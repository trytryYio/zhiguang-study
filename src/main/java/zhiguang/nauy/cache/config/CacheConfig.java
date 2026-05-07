package zhiguang.nauy.cache.config;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zhiguang.nauy.knowpost.api.dto.FeedPageResponse;
import zhiguang.nauy.knowpost.api.dto.KnowPostDetailResponse;

/**
 * Caffeine 本地缓存配置（L2 缓存）。
 *
 * <p>用于在应用进程内缓存分页结果和详情数据，降低数据库压力。</p>
 */
@Configuration
public class CacheConfig {

    /**
     * 公共信息流分页缓存。
     *
     * <p>TTL 和最大条目由 application.yml 中的 cache.l2.public-cfg 控制。</p>
     */
    @Bean("feedPublicCache")
    public Cache<String, FeedPageResponse> feedPublicCache(CacheProperties props) {
        return Caffeine.newBuilder()

                // 最大条目数
                .maximumSize(props.getL2().getPublicCfg().getMaxSize())
                //ttl
                .expireAfterWrite(Duration.ofSeconds(props.getL2().getPublicCfg().getTtlSeconds()))
                .build();
    }

    /**
     * 个人信息流分页缓存。
     */
    @Bean("feedMineCache")
    public Cache<String, FeedPageResponse> feedMineCache(CacheProperties props) {
        return Caffeine.newBuilder()
                // 最大条目数
                .maximumSize(props.getL2().getMineCfg().getMaxSize())
                // ttl
                .expireAfterWrite(Duration.ofSeconds(props.getL2().getMineCfg().getTtlSeconds()))
                .build();
    }

    /**
     * 知文详情本地缓存。
     */
    @Bean("knowPostDetailCache")
    public Cache<String, KnowPostDetailResponse> knowPostDetailCache(CacheProperties props) {
        return Caffeine.newBuilder()
                // 最大条目数

                .maximumSize(props.getL2().getDetailCfg().getMaxSize())
                // ttl

                .expireAfterWrite(Duration.ofSeconds(props.getL2().getDetailCfg().getTtlSeconds()))
                .build();
    }
    /**
     * 关注列表 Top 缓存（用于大V用户）。
     *
     * <p>缓存每个用户的前 N 个关注者 ID，降低冷启动时的数据库压力。</p>
     */
    @Bean("flwsTopCache")
    public Cache<Long, List<Long>> flwsTopCache() {
        return Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();
    }
}
