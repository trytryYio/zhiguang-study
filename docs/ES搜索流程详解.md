# Elasticsearch 搜索与自动补全流程详解

## 一、项目中用到的两个 ES 索引

本项目使用了两个独立的 Elasticsearch 索引，分别服务于不同场景：

| 索引名 | 用途 | 客户端 |
|---|---|---|
| `zhiguang_content_index` | 关键词全文搜索 + 自动补全 | `ElasticsearchClient`（直连） |
| `zhiguang-ai-index` | 语义向量搜索（RAG 问答） | Spring AI `VectorStore`（抽象层） |

本文档重点讲解 **搜索与自动补全**（即 `zhiguang_content_index` 的完整流程）。

---

## 二、索引结构定义

索引在应用启动时自动创建，映射定义如下（`SearchIndexInitializer.java`）：

```
zhiguang_content_index
├── title           (text, ik_max_word分词, ik_smart搜索, copyTo → title_suggest)
├── body            (text, ik_max_word分词, ik_smart搜索)
├── title_suggest   (completion, 自动从title复制)
├── postId          (keyword, 精确匹配)
├── likeCount       (动态映射, 用于排序)
├── favCount        (动态映射, 用于展示)
└── creatorId       (动态映射)
```

**字段说明：**
- `title` 和 `body` 使用 IK 中文分词器：`ik_max_word`（索引时最细粒度切分）、`ik_smart`（搜索时智能切分）
- `title_suggest` 是 completion 类型，通过 `copyTo` 从 `title` 自动填充，专门用于自动补全
- 1个分片，0个副本（单机环境）

---

## 三、数据写入流程：文档如何进入 ES

### 写入时机

文档**不是**在知文创建时就写入 ES，而是在**发布后**通过 Canal 监听 MySQL binlog 异步写入。

### 完整写入链路

```
步骤1: 用户创建知文
  POST /api/v1/knowposts/drafts → 创建草稿 (status=DRAFT)

步骤2: 上传内容到 OSS
  presign → 上传 → 确认

步骤3: 发布知文
  POST /api/v1/knowposts/{id}/publish
  → MySQL: UPDATE know_posts SET status='PUBLISHED' ...

步骤4: Canal 监听 binlog
  Canal Server 订阅 MySQL binlog，过滤条件：
    zhiguang\.outbox | zhiguang\.know_posts
  → 捕获到 know_posts 表的 UPDATE 事件

步骤5: CanalKafkaBridge 转发到 Kafka
  → 从 Canal 消息中提取 payload
  → 构造 JSON 信封: {"table":"outbox","type":"UPDATE","data":[{"payload":"..."}]}
  → kafka.send("canal-outbox", json)

步骤6: CanalOutboxConsumerSearch 消费消息
  → 解析 JSON，检查 aggregateType == "Knowpost"
  → 调用 SearchIndexService.upsertKnowPost(postId)

步骤7: SearchIndexService 写入 ES
  → 从 MySQL 加载知文元数据
  → 从 OSS 下载 body 内容（RestTemplate）
  → 从 CounterService 读取点赞/收藏数
  → 构造 ES 文档: {postId, title, body, creatorId, likeCount, favCount}
  → 写入 zhiguang_content_index，Refresh.WaitFor（确保可搜索）
```

### 关键代码位置

- Canal 监听：`CanalKafkaBridge.java` — 订阅 binlog，发送到 Kafka
- 消费触发：`CanalOutboxConsumerSearch.java:43` — `searchIndexService.upsertKnowPost(postId)`
- ES 写入：`SearchIndexService.java` — 构造文档并 upsert

---

## 四、搜索流程：关键词全文搜索

### 前端调用

```
用户输入关键词 → SearchPage.tsx
  → 按回车或点击搜索按钮
  → 调用 search({ q: "关键词", size: 20 })
  → GET /api/v1/search?q=关键词&page=1&size=20
```

### 后端处理（SearchServiceImpl）

```
SearchController.search()
  → SearchServiceImpl.search()
    → 构造 multi_match 查询:
        query: "关键词"
        fields: ["title^3", "body"]    ← title 权重是 body 的3倍
    → 排序: likeCount 降序
    → 返回: List<SearchResult>
        SearchResult = { postId, title, snippet(截取前200字), likeCount, favCount }
```

### ES 查询 DSL

```json
{
  "query": {
    "multi_match": {
      "query": "关键词",
      "fields": ["title^3", "body"]
    }
  },
  "sort": [
    { "likeCount": { "order": "desc" } }
  ]
}
```

---

## 五、自动补全流程：联想词如何弹出

### 前端触发机制

```
用户在搜索框输入文字
  → SearchBar.tsx 监听 onChange
  → 300ms 防抖（debounce）
  → 调用 suggest({ q: "输入前缀", size: 10 })
  → GET /api/v1/search/suggest?q=输入前缀&size=10
  → 返回 string[]（如 ["Java入门", "Java进阶", "JavaScript笔记"]）
  → 下拉框展示联想词列表
```

### 后端处理（SearchServiceImpl）

```
SearchController.suggest()
  → SearchServiceImpl.suggest()
    → 构造 completion suggester 查询:
        suggest: {
          title_suggest: {
            prefix: "输入前缀",
            completion: {
              field: "title_suggest",
              size: 10
            }
          }
        }
    → 从 suggest 结果中提取 text 列表
    → 返回: List<String>（纯字符串数组）
```

### ES 查询 DSL

```json
{
  "suggest": {
    "title_suggest": {
      "prefix": "Java",
      "completion": {
        "field": "title_suggest",
        "size": 10
      }
    }
  }
}
```

### 自动补全的工作原理

1. **title → title_suggest 自动复制**：在索引映射中，`title` 字段配置了 `copy_to: title_suggest`，所以每当文档被索引时，`title` 的内容会自动复制到 `title_suggest` 字段
2. **completion 类型**：`title_suggest` 是 ES 的 completion suggester 类型，内部使用 FST（Finite State Transducer）数据结构，查询速度极快（O(输入长度)）
3. **前缀匹配**：用户输入 "Java" 时，ES 查找所有以 "Java" 开头的 title_suggest 值

---

## 六、为什么新建的知文没有出现在联想词中？

### 原因分析

新建的知文**没有出现在联想词中**，有以下几种可能：

#### 原因1：Canal 没有捕获到 binlog 更新

Canal 订阅的过滤条件是 `zhiguang\.outbox | zhiguang\.know_posts`。只有当 `know_posts` 表的记录被 UPDATE 时（特别是 `status` 从 DRAFT 变为 PUBLISHED），Canal 才会捕获到变更。

**检查点：**
- Canal Server 是否在运行？
- Canal 的 `destination` 是否配置正确（`example`）？
- `canal.enabled` 是否为 `true`？

#### 原因2：CanalKafkaBridge 没有发送到 Kafka

Canal 捕获 binlog 后，由 `CanalKafkaBridge` 负责发送到 Kafka。如果 CanalKafkaBridge 没有运行或者解析失败，消息不会进入 Kafka。

**检查点：**
- CanalKafkaBridge 是否在运行？
- Kafka broker 是否可达（`localhost:9092`）？

#### 原因3：Consumer 没有消费或处理失败

`CanalOutboxConsumerSearch` 消费 Kafka 消息后，需要：
1. 解析 payload，检查 `aggregateType == "Knowpost"`
2. 调用 `SearchIndexService.upsertKnowPost(postId)` 写入 ES

如果 payload 格式不对、或者 ES 连接失败，写入会失败。

**检查点：**
- Kafka 消费者组 `search-index-consumer` 是否在消费？
- ES 是否可达（`localhost:9200`）？
- 应用日志是否有 `upsertKnowPost` 相关的错误？

#### 原因4：ES 的 Refresh 策略

写入 ES 时使用了 `Refresh.WaitFor`，这意味着写入后 ES 会等待下一次 refresh（默认1秒间隔）后数据才可搜索。如果是立即查询，可能还没 refresh。

**解决方案：** 等待1-2秒后重试搜索。

#### 原因5：应用重启后没有触发回填

应用启动时会检查 ES 索引是否为空，如果为空会回填所有已发布的知文。但如果 ES 中已有数据，回填不会执行。如果中间有知文漏写入，需要手动触发或重启应用。

---

## 七、完整数据流图

```
用户操作                     MySQL                  Canal         Kafka           ES
───────                     ──────                  ─────         ─────           ──
创建草稿 ──→ INSERT know_posts (status=DRAFT)
                                          (不触发，因为不是PUBLISHED)
上传内容 ──→ OSS
发布知文 ──→ UPDATE know_posts (status=PUBLISHED) ──→ 捕获binlog ──→ canal-outbox ──→ Consumer ──→ ES upsert
                                                                    topic          搜索索引

用户输入 ──→ 300ms防抖 ──→ suggest API ──→ ES completion suggester ──→ 联想词列表 ──→ 下拉框展示
回车搜索 ──→ search API ──→ ES multi_match ──→ 搜索结果列表 ──→ 渲染CourseCard
```

---

## 八、RAG 语义搜索（补充说明）

除了关键词搜索，本项目还支持基于向量的语义搜索（RAG），用于知文详情页的 AI 问答：

| 特性 | 关键词搜索 | 语义搜索（RAG） |
|---|---|---|
| 索引 | `zhiguang_content_index` | `zhiguang-ai-index` |
| 分词/向量化 | IK 中文分词器 | 1536维向量嵌入 |
| 查询方式 | multi_match + completion | 向量相似度搜索 |
| 触发场景 | 搜索页 | 知文详情页的 AI 问答 |
| 索引触发 | Canal binlog 事件驱动 | 手动 reindex 或 QA 时自动 reindex |
| 内容来源 | MySQL + OSS body | OSS markdown 全文（分块后向量化） |

**RAG 索引流程：**
1. 调用 `POST /api/v1/knowposts/{id}/rag/reindex`
2. 从 OSS 获取 markdown 内容
3. SHA-256 指纹校验（内容未变则跳过）
4. 按 heading 分块 + 滑动窗口（800字符，100重叠）
5. 调用 Embedding API 生成 1536 维向量
6. 写入 `zhiguang-ai-index`
