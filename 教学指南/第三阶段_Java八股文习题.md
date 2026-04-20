# 知光项目 - Java 八股知识点总结（第三阶段）

> 基于项目实际使用的技术栈生成，共 12 道题目，覆盖 5 个技术领域。

---

## 第一部分：自测习题

以下为项目涉及核心知识点习题，可自行书写答案。

---

### 问题 1 【阿里云OSS】难度：初级
**项目来源**：`src/main/java/zhiguang/nauy/storage/OssStorageService.java:39-65`

**问题描述**：分析 `OssStorageService.uploadAvatar()` 方法的实现，说明如何上传头像文件到阿里云OSS？该方法涉及哪些关键步骤？

---
答案：


---

### 问题 2 【Spring配置属性】难度：中级
**项目来源**：`src/main/java/zhiguang/nauy/storage/OssProperties.java:14-24`

**问题描述**：`OssProperties` 类使用了哪些注解？请解释这些注解的作用以及它们如何配合工作？

---
答案：


---

### 问题 3 【分布式ID - 雪花算法】难度：中级
**项目来源**：参考 `教学指南/第三阶段.md` 第 1.1 节

**问题描述**：雪花算法生成的64位ID包含哪些部分？请说明各占多少位，并解释时间戳部分为什么可以使用41位？

---
答案：


---

### 问题 4 【分布式ID - 线程安全】难度：高级
**项目来源**：参考 `教学指南/第三阶段.md` 第 1.1 节

**问题描述**：在单机部署环境下，雪花算法如何保证生成的ID是唯一的？如果要扩展到多节点部署，需要考虑哪些问题？

---
答案：


---

### 问题 5 【Redis键命名规范】难度：初级
**项目来源**：参考 `教学指南/双令牌认证学习指南.md`

**问题描述**：项目中Redis键的命名规范是什么？请举例说明 auth:rt:{userId}:{jti} 这样的键名设计有什么优点？

---
答案：


---

### 问题 6 【Java Stream API】难度：中级
**项目来源**：`src/main/java/zhiguang/nauy/storage/OssStorageService.java:74-81`

**问题描述**：分析 `publicUrl()` 方法中 `replaceAll("/$", "")` 的作用，这里使用正则表达式要解决什么问题？

---
答案：


---

### 问题 7 【异常处理设计】难度：中级
**项目来源**：`src/main/java/zhiguang/nauy/storage/OssStorageService.java:58-62`

**问题描述**：为什么在 `catch(IOException e)` 后要 `throw new BusinessException`？这种异常转换说明了什么设计原则？

---
答案：


---

### 问题 8 【配置文件绑定】难度：中级
**项目来源**：`src/main/java/zhiguang/nauy/storage/OssProperties.java`

**问题描述**：`@ConfigurationProperties(prefix = "oss")` 中的 prefix 配置对应什么位置？如果需要在 application.yml 中配置，应该怎么写？

---
答案：


---

### 问题 9 【前端直传架构】难度：高级
**项目来源**：参考 `教学指南/第三阶段.md` 第 1.6-1.7 节

**问题描述**：项目采用"前端直传OSS"架构，请解释这种设计相比"后端代理上传"的优势和缺点分别是什么？

---
答案：


---

### 问题 10 【预签名URL】难度：高级
**项目来源**：`src/main/java/zhiguang/nauy/storage/OssStorageService.java:92-109`

**问题描述**：`generatePresignedPutUrl()` 方法生成的预签名URL包含哪些关键信息？客户端上传时必须保证什么一致性？

---
答案：


---

### 问题 11 【唯一性校验】难度：中级
**项目来源**：`src/main/java/zhiguang/nauy/profile/service/ProfileServiceImpl.java:51-58`

**问题描述**：知光号(zgId)唯一性校验时，为什��需要排除当前用户自身的ID？这体现了什么业务逻辑？

---
答案：


---

### 问题 12 【Lambda更新构造】难度：中级
**项目来源**：`src/main/java/zhiguang/nauy/profile/service/ProfileServiceImpl.java:62-71`

**问题描述**：分析 `LambdaUpdateWrapper` 中 `.set(StrUtil.isNotBlank(...), ...)` 的作用，这种写法相比普通 update 有什么优势？

---
答案：


---

## 第二部分：参考答案详解

---

### 问题 1

**参考答案：**

`uploadAvatar()` 方法的核心步骤：
1. **参数校验**：调用 `ensureConfigured()` 检查 OSS 配置是否完整
2. **文件名处理**：提取原始文件扩展名（如 .jpg）
3. **构造对象键**：格式为 `{folder}/{userId}-{timestamp}.ext`，如 `avatars/123456-1704067200000.jpg`
4. **创建OSS Client**：`new OSSClientBuilder().build(...)` 
5. **上传文件**：`client.putObject(request)` 
6. **返回外链**：调用 `publicUrl()` 生成可访问的URL

项目中 avatar 存储在 OSS 的 `avatars/` 目录下，使用时间戳保证文件名唯一。

---

### 问题 2

**参考答案：**

`OssProperties` 使用了三个关键注解：

| 注解 | 作用 |
|------|------|
| `@Data` | Lombok 注解，自动生成 getter/setter/equals/toString 等方法 |
| `@Component` | 将类注册为 Spring Bean，让 Spring 管理其生命周期 |
| `@ConfigurationProperties(prefix = "oss")` | 将 application.yml 中 `oss.*` 属性自动绑定到该类的字段 |

三者配合：`@Component` 使类可被 Spring 扫描注入，`@ConfigurationProperties` 指定配置前缀，`@Data` 自动生成访问方法。

---

### 问题 3

**参考答案：**

64位雪花ID结构分布：

| 部分 | 位数 | 说明 |
|------|------|------|
| 时间戳 | 41位 | 距离 EPOCH 的毫秒数，支持 ~69 年 |
| 数据中心ID | 5位 | 支持 32 个数据中心 |
| 工作节点ID | 5位 | 支持 32 个工作节点 |
| 序列号 | 12位 | 每毫秒支持 4096 个ID |

41位时间戳：2^41 ≈ 2.2×10^12 毫秒 ≈ 69 年，减去2024年开始的偏移，实际可用约60+年。

---

### 问题 4

**参考答案：**

**单机部署**：默认构造器 `new SnowflakeIdGenerator()` 使用固定的 workerId=0，保证同一进程内线程安全（通过 synchronized 或 AtomicLong）。

**多节点部署需要考虑**：
1. **工作节点分配**：需要为每个节点分配唯一的 workerId（0-31）
   - 方案A：配置文件指定
   - 方案B：使用 ZooKeeper/Etcd 协调分配
   - 方案C：机器 MAC 地址/IP 哈希取模
2. **时钟同步**：多节点时钟必须同步（否则可能产生重复ID）
3. **心跳检测**：检测节点存活，避免 workerId 冲突

知光项目当前阶段使用单机部署，默认构造器即可。

---

### 问题 5

**参考答案：**

**命名规范**：采用 `{业务}:{子类型}:{用户ID}:{序列化标识}` 格式，如 `auth:rt:123:abc-def-ghi`

**优点**：
1. **语义清晰**：通过冒号分隔便于阅读和管理
2. **便于批量操作**：可使用 `auth:*` 匹配所有认证相关键
3. **用户隔离**：包含 userId 避免数据混淆
4. **类型区分**：区分 access token 和 refresh token

Redis 键过期通过 TTL 自动管理，无需显式删除。

---

### 问题 6

**参考答案：**

`replaceAll("/$", "")` 是为了处理路径末尾可能存在的斜杠：

```java
// 如果 publicDomain = "https://cdn.example.com/" 
// 不处理直接拼接：https://cdn.example.com//avatars/123.jpg ❌
// 处理后：https://cdn.example.com/avatars/123.jpg ✅
return props.getPublicDomain().replaceAll("/$", "") + "/" + objectKey;
```

正则 `/` + `$` 表示"末尾的斜杠"，`replaceAll` 会将末尾的所有斜杠替换为空字符串。注意这里不是 `replaceAll("/$", "")` 而是 `replaceAll("/$", "")`，正则中 `/` 需要转义。

---

### 问题 7

**参考答案：**

**异常转换的设计原则**：

1. **统一异常类型**：将检查型 `IOException` 转换为业务异常 `BusinessException`
2. **隐蔽实现细节**：避免暴露底层存储技术细节给上层
3. **提供友好信息**：将"文件读取失败"转换为用户可理解的错误信息
4. **统一错误码**：使用项目自定义的错误码体系（ErrorCode.BAD_REQUEST）

这体现了**异常封装**原则：底层异常不应直接传播给上层，而应包装为上层可理解和处理的类型。

---

### 问题 8

**参考答案：**

在 `application.yml` 中的配置位置：

```yaml
oss:
  endpoint: oss-cn-shanghai.aliyuncs.com
  access-key-id: LTAIxxxxx
  access-key-secret: yyyyy
  bucket: zhiguang-files
  public-domain: https://cdn.example.com
  folder: avatars
```

Spring Boot 会自动将 `oss.endpoint` 绑定到 `OssProperties.endpoint` 字段，注意属性名使用 kebab-case（短横线命名）。

---

### 问题 9

**参考答案：**

**优势**：
1. **节省带宽**：大文件不经过后端服务器，减少后端流量和成本
2. **降低延迟**：客户端直传 OSS，距离更近，响应更快
3. **后端解耦**：后端只需存储最终 URL，无需处理上传逻辑
4. **扩展性好**：OSS 可水平扩展承受海量并发

**缺点**：
1. **架构复杂**：需要预签名、回调确认等多步骤
2. **前端工作量增加**：需要实现文件直传逻辑
3. **安全性管理**：预签名 URL 有效期需要合理设置
4. **调试困难**：直传失败较难追踪

知光项目采用此架构是因为 AI 生成的 Markdown 内容可能较大，直传可节省后端流量成本。

---

### 问题 10

**参考答案：**

预签名URL包含的关键信息：
1. **目标Bucket**：哪个 OSS 桶
2. **ObjectKey**：存储路径
3. **过期时间**：URL 有效期（建议 300-900 秒）
4. **Content-Type**：上传内容的 MIME 类型
5. **签名信息**：阿里云生成的验证签名

**客户端必须保证的一致性**：
- `Content-Type` 必须与生成签名时一致（否则403签名错误）
- HTTP 方法必须是 PUT
- 请求体内容必须一致（ETag 依赖内容计算）

```javascript
// 前端上传示例
fetch(putUrl, {
    method: 'PUT',
    headers: {'Content-Type': 'text/markdown'},
    body: fileContent
})
```

---

### 问题 11

**参考答案：**

**排除自身的原因**：

用户修改资料时，提交的 zgId 可能是自己原来正在使用的，如果不排除自己，每次保存都会报"知光号已存在"错误。

```java
// 排除当前用户后再查询是否存在重复
boolean exists = userMapper.existsByZgIdExceptId(req.getZgId(), current.getId());
// 这样：用户保持原有 zgId → 不报错
//       用户改为自己已占用的 zgId → 报错
```

这体现了**业务逻辑的完整性**：唯一性校验是针对"除自己外的其他用户"，而不是"全局唯一"。

---

### 问题 12

**参考答案：**

使用条件 `.set(condition, field, value)` 的优势：

1. **仅更新非空字段**：不会将 null 值覆盖掉已有数据
2. **避免显式判空**：不需要逐个 if 判断后再 set
3. **代码更简洁**：链式调用易于阅读

```java
// 传统写法
if (StrUtil.isNotBlank(req.getNickname())) {
    user.setNickname(req.getNickname());
}
if (StrUtil.isNotBlank(req.getBio())) {
    user.setBio(req.getBio());
}
// ...
// LambdaWrapper 写法
.set(StrUtil.isNotBlank(req.getNickname()), User::getNickname, req.getNickname())
.set(StrUtil.isNotBlank(req.getBio()), User::getBio, req.getBio())
```

这种方式允许用户仅更新部分字段，其他字段保持不变。

---

**��三��段学习指南完成！**

建议继续完成第四阶段后，结合以下知识点复习：
- Redis Bitmap 计数
- Lua 脚本原子操作
- 热键检测
- Kafka 消息队列