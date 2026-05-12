package zhiguang.nauy.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zhiguang.nauy.search.service.dto.SearchResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static jodd.util.StringUtil.truncate;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {
    private static final String INDEX_NAME = "zhiguang_content_index";

    @Resource
    private ElasticsearchClient client;


    /**
     * 执行全文搜索。
     *
     * <p>使用 multi_match 在标题和正文中检索关键词，
     * 按点赞数降序排列并返回分页结果。</p>
     *
     * @param query 搜索关键词
     * @param page  页码（从1开始）
     * @param size  每页数量
     * @return 搜索结果列表，失败时返回空列表
     */
    @Override
    public List<SearchResult> search(String query, int page, int size) {
//        用户输入关键词 → ES multi_match 查询 → 相关性排序 + 业务加权 → 返回结果
        try {
            // 1. 计算分页偏移量（from = (页码-1) * 每页数量）
            int from = (page - 1) * size;
            // 2. 执行 Elasticsearch 搜索请求
            SearchResponse<Map> response = client.search(s -> s
                // 2.1 指定要搜索的索引名称
                .index(INDEX_NAME)
                // 2.2 构建查询条件：使用 multi_match 在多个字段中搜索
                .query(q -> q.multiMatch(m -> m
                    // 搜索字段：标题和正文
                    .fields("title", "body")
                    // 用户输入的搜索关键词
                    .query(query)))
                // 2.3 分页参数：起始位置
                .from(from)
                // 2.4 分页参数：返回数量
                .size(size)
                // 2.5 排序规则：按点赞数降序排列
                .sort(so -> so.field(f -> f.field("likeCount")
                    .order(SortOrder.Desc))), Map.class
            );
            // 3. 初始化结果列表
            List<SearchResult> results = new ArrayList<>();
            // 4. 遍历 ES 返回的命中结果
            for (Hit<Map> hit : response.hits().hits()) {
                // 4.1 获取文档源数据（_source 字段）
                Map<String, Object> doc = hit.source();
                // 4.2 空值保护：如果文档为空则跳过
                if (doc == null) continue;
                results.add(new SearchResult(
                    // 帖子 ID：从 String 转为 Long
                    Long.parseLong((String) doc.get("postId")),
                    // 标题
                    (String) doc.get("title"),
                    // 摘要：截取正文前 200 字符
                    truncate((String) doc.get("body"), 200),
                    // 点赞数
                    doc.get("likeCount") != null ? ((Number) doc.get("likeCount")).longValue() : 0,
                    // 收藏数
                    doc.get("favCount") != null ? ((Number) doc.get("favCount")).longValue() : 0

                ));
            }
            // 5. 返回搜索结果列表
            return results;
        } catch (Exception e) {

            return List.of();            // 6. 异常处理：搜索失败时返回空列表（避免接口崩溃）
        }

    }

    /**
     * 获取补全建议。
     *
     * <p>使用 Completion Suggester 获取用户输入的前缀的补全建议。</p>
     *
     *
     类比：你在百度搜索框输入 "red"，下面自动弹出：
     redis
     redis 分布式锁
     redis 集群搭建
     这就是搜索联想/补全。suggest 方法做的就是这件事。
     *
     *
     * @param prefix 用户输入的前缀
     * @param size   返回数量
     * @return 补全建议列表，失败时返回空列表
     */
    @Override
    public List<String> suggest(String prefix, int size) {
        try {
            //1.发请求给  ES
            SearchResponse<Map> response = client.search(s -> s
                .index(INDEX_NAME) // 指定索引名称 ->搜索哪一张表格
                .suggest(suggest -> suggest //启用联想功能
                    .suggesters("title_suggest", sg -> sg // 指定联想器名称
                        .prefix(prefix) // 输入的前缀
                        .completion(c -> c
                            .field("title_suggest") // 搜ES 里 哪个联想字段
                            .size(size) // 返回数量
                        ))), Map.class);

            if (response.suggest() == null) {
                return List.of();
            }
//{
//  "suggest": {                   // 联想查询的总包裹
//    "title_suggest": [           // 具体的联想器名称（与查询请求中的 name 对应）
//      {
//        "text": "red",           // 用户输入的原始前缀
//        "options": [             // 联想补全的候选结果列表
//          { "text": "Redis 基础" },
//          { "text": "Redis 分布式锁" },
//          { "text": "React 入门" }
//        ]
//      }
//    ]
//  }
//}

            List<String> suggestions = new ArrayList<>();

            //取出联系的结果
            Collection<List<Suggestion<Map>>> values = response.suggest().values();
            // 遍历所有联想器的结果
            // response.suggest() 返回 Map<String, List<Suggestion<Map>>>
            for (var suggestionGroup : values) {
                //     .values() 取出 Map 的所有 value：
                //     // values() 返回：
                //     [
                //       {                         // ← 这是第一个 value
                //         "text": "red",
                //         "options": [...]
                //       },
                //       {...}  // ← 这是第二个 value
                //     ]
// suggestionGroup = [Suggestion对象]
                // 相当于取 Map 里所有的 value
                for (var suggestion : suggestionGroup) {
                    //  遍历数组里的每个元素



                    // suggestion = 一个 Suggestion 对象
                    // ES 9.x 里它是个"标签联合体"，可能是 completion/phrase/term 三种类型


                    if (suggestion.isCompletion()) {
                        // 判断：这是 completion 类型吗？→ 是的，我们用的就是 completion
                        // completion() 返回 CompletionSuggest，再调 options()
                        for (var option : suggestion.completion().options()) {
                            //                        ↑ 取出 completion 类型的数据
                            //                                     ↑ 取出 options 数组

                            // option.text() = "Redis 基础"
                            // option.text() = "Redis 分布式锁"
                            // option.text() 获取联想文本
                            suggestions.add(option.text());
                        }
                    }
                }
            }
            return suggestions;
        } catch (Exception e) {
            return List.of();
        }
//            完整对应图                                                                                                                                                                                       ▶ Modified Files
//
//     response.suggest()                          →  整个 suggest JSON
//       ↓
//     .values()                                   →  [{ "text":"red", "options":[...] }]
//       ↓
//     suggestionGroup (List)                      →  [{ "text":"red", "options":[...] }]
//       ↓
//     suggestion (单个)                            →  { "text":"red", "options":[...] }
//       ↓
//     suggestion.completion().options()           →  [{ "text":"Redis基础" }, { "text":"Redis分布式锁" }, ...]
//       ↓
//     option.text()                               →  "Redis基础"
    }

}

