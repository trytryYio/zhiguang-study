package zhiguang.nauy.search.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.CompletionProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ES 搜索索引初始化器。
 *
 * <p>应用启动时自动创建 zhiguang_content_index 索引，
 * 配置 IK 分词器和 completion suggester。</p>
 */
@Slf4j
@Component
public class SearchIndexInitializer {
    @Resource
    private ElasticsearchClient elasticsearchClient;
    //创建index索引

    private static final String INDEX_NAME = "zhiguang_content_index";

    /**
     * 创建索引
     */
    @PostConstruct
    public void init() {
        try {

            //elasticsearchClient - Elasticsearch 客户端实例
            //.indices() - 获取索引管理 API 的入口点
            //.exists() - 检查指定索引是否存在的方法
            boolean exists = elasticsearchClient.indices()
                .exists(e -> e.index(INDEX_NAME))
                .value();

            if (!exists) {
                createIndex();
                log.info("索引不存在，正在创建索引...:{}", INDEX_NAME);
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    /**
     * 创建索引
     */
    private void createIndex() throws IOException {
//        调用 ES 客户端的索引管理 API，使用函数式编程方式构建创建索引请求
        elasticsearchClient.indices().create(CreateIndexRequest.of(c -> c
            //指定要创建的索引名称为 zhiguang_content_index
            .index(INDEX_NAME)
            //开始定义索引的映射（mapping），即字段结构
            .mappings(m -> m
                //：定义 title 字段
                //类型：text（全文本）
                //分词器：ik_max_word（最细粒度分词，用于索引）
                //搜索分词器：ik_smart（较粗粒度分词，用于搜索）
                //复制到：title_suggest 字段（用于自动补全）
                .properties("title", Property.of(p -> p.text(TextProperty.of(t -> t
                    .analyzer("ik_max_word")
                    .searchAnalyzer("ik_smart")
                    .copyTo("title_suggest")
                ))))
//                类型：text（全文本）
//                分词器：同上，支持中文分词
                .properties("body", Property.of(p -> p.text(TextProperty.of(b -> b
                    .analyzer("ik_max_word")
                    .searchAnalyzer("ik_smart")

                ))))

//                定义 title_suggest 字段
//                类型：completion（自动补全类型）
//                用途：支持搜索建议功能
                .properties("title_suggest", Property.of(p -> p
                    .completion(CompletionProperty.of(cp -> cp))

                ))
                //定义 postId 字段
                    //类型：keyword（精确匹配）
                    //用途：存储帖子 ID，用于精确查询和关联
                .properties("postId", Property.of(p -> p
                    .keyword(KeywordProperty.of(k -> k))

                ))


            )
            //定义索引设置
            //分片数：1 个主分片
            //副本数：0 个副本（开发环境节省资源）
            .settings(s -> s
                .numberOfShards("1")
                .numberOfReplicas("0"))

        ));
    }

}
