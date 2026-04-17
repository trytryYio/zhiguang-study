package zhiguang.nauy.cache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
// 以下两个类型在第三阶段才会用到，但这里先声明，避免编译报错
//import com.tongji.knowpost.api.dto.FeedPageResponse;       // 第三阶段创建
//import com.tongji.knowpost.api.dto.KnowPostDetailResponse;  // 第三阶段创建

import java.time.Duration;

import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caffeine 本地缓存配置（L2 缓存）。
 *
 * <p>用于在应用进程内缓存分页结果和详情数据，降低数据库压力。</p>
 */
@Configuration
public class CacheConfig {

//    /**
//     * 公共信息流分页缓存。
//     *
//     * <p>TTL 和最大条目由 application.yml 中的 cache.l2.public-cfg 控制。</p>
//     */
//    @Bean("feedPublicCache")
//    public Cache<String, FeedPageResponse> feedPublicCache(CacheProperties props) {
//        return Caffeine.newBuilder()
//                .maximumSize(props.getL2().getPublicCfg().getMaxSize())
//                .expireAfterWrite(Duration.ofSeconds(props.getL2().getPublicCfg().getTtlSeconds()))
//                .build();
//    }
//
//    /**
//     * 个人信息流分页缓存。
//     */
//    @Bean("feedMineCache")
//    public Cache<String, FeedPageResponse> feedMineCache(CacheProperties props) {
//        return Caffeine.newBuilder()
//                .maximumSize(props.getL2().getMineCfg().getMaxSize())
//                .expireAfterWrite(Duration.ofSeconds(props.getL2().getMineCfg().getTtlSeconds()))
//                .build();
//    }
//
//    /**
//     * 知文详情本地缓存。
//     */
//    @Bean("knowPostDetailCache")
//    public Cache<String, KnowPostDetailResponse> knowPostDetailCache(CacheProperties props) {
//        return Caffeine.newBuilder()
//                .maximumSize(props.getL2().getDetailCfg().getMaxSize())
//                .expireAfterWrite(Duration.ofSeconds(props.getL2().getDetailCfg().getTtlSeconds()))
//                .build();
//    }
}