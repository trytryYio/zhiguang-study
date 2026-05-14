package zhiguang.nauy.search.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import zhiguang.nauy.counter.service.CounterService;
import zhiguang.nauy.knowpost.domain.KnowPosts;
import zhiguang.nauy.knowpost.mapper.KnowPostsMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ES 索引写入服务。
 *
 * <p>负责知文的索引写入、更新和软删除。
 * 启动时自动回填已有帖子。</p>
 */
@Service
@Slf4j

public class SearchIndexService  {
    @Resource
    private KnowPostsMapper knowPostsMapper;

    @Resource
    private ElasticsearchClient esClient;

    private static final String INDEX_NAME = "zhiguang_content_index";
    @Autowired
    private RestTemplate restTemplate;
    @Resource
    private CounterService counterService;


    /**
     * 启动时自动回填已有帖子。
     */
    @PostConstruct //应用启动时自动执行
    public void ensureBackFill() throws IOException {
        //TODO: 启动时自动回填已有帖子
        //1.查询有多少条数据
        long count = esClient.count(c -> c.index(INDEX_NAME)).count();
        //2.如果大于0 说明已经回填过了 跳过
        if (count>0) return;
        //3.如果=0 说明没有回填过 ,调用backfillALl方法
        backfillAll();
    }

    /**
     * 批量回填所有帖子。
     *
     * <p>当启动时自动回填已有帖子时调用。</p>
     */
    private void backfillAll() {
        log.info("Starting full backfill...");
        // 1. 从 MySQL 查所有已发布的帖子
        LambdaQueryWrapper<KnowPosts> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowPosts::getStatus, "published");
        List<KnowPosts> knowPosts = knowPostsMapper.selectList(wrapper);
        int indexed =0;
        for (KnowPosts post : knowPosts){
            try {
//                2. 遍历每个帖子，调 upsertKnowPost() 写入 ES
                upsertKnowPost(post.getId());
                indexed++;

            } catch (Exception e) {
                log.warn("Failed to index post {}: {}", post.getId(), e.getMessage());
            }
        }
        log.info("Backfill completed: {}/{} posts indexed", indexed, knowPosts.size());
    }

    /**
     * 写入/更新帖子。
     *
     * <p>更新索引，当知文被修改时调用。</p>
     *
     * @param postId 帖子 ID
     */
    public void upsertKnowPost(Long postId) throws IOException {
        // 1. 从数据库查询帖子信息
        KnowPosts knowPosts = knowPostsMapper.selectById(postId);
        // 2. 校验：帖子不存在或未发布则跳过
        if (knowPosts == null||!"published".equals(knowPosts.getStatus())){
            return;
        }
        // 3. 提取标题（空值保护）
        String title = knowPosts.getTitle()!=null?knowPosts.getTitle():"";
        // 4. 获取正文内容
        String body="";
        if(knowPosts.getContentUrl() != null && !knowPosts.getContentUrl().isBlank()){
            try {
                // 从 OSS URL 下载正文内容
                //有内容
                body = restTemplate.getForObject(knowPosts.getContentUrl(), String.class);
            } catch (RestClientException e) {
                log.warn("Failed to fetch content for post {}: {}", postId, e.getMessage());
            }
            // 5. 构建 ES 文档数据
                HashMap<String, Object> map = new HashMap<>();
                map.put("postId", String.valueOf(postId));
                map.put("title", title);
                map.put("body", body != null ? body : "");
                map.put("creatorId", String.valueOf(knowPosts.getCreatorId()));
            // 6. 获取计数数据（点赞数、收藏数）
                //获取计数数据
            try {
                Map<String, Long> counts = counterService.getCounts("knowpost", String.valueOf(postId),List.of("fav","like"));
                Long like = counts.get("like");
                Long fav = counts.get("fav");
                map.put("likeCount", like != null ? like : 0);
                    map.put("favCount", fav != null ? fav : 0);
            } catch (Exception e) {
                // 计数获取失败时使用默认值 0，不影响索引写入
                map.put("likeCount", 0);
                map.put("favCount", 0);

            }
            // 7. 写入/更新 ES 索引
            esClient.index(i -> i
                .index(INDEX_NAME)
                .id(String.valueOf(postId))
                .document( map)
                .refresh(Refresh.WaitFor));
        }

    }


    /**
     * 软删除帖子。
     *
     * <p>从索引中删除帖子，当知文被删除时调用。</p>
     *
     * @param postId 帖子 ID
     */
    public void softDeleteKnowPost(Long postId) throws IOException {

        esClient.delete(d->d.index(INDEX_NAME)
            .id(String.valueOf(postId)));


    }

}
