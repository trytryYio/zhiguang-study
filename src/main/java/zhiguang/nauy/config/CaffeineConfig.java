package zhiguang.nauy.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置类
 * <p>提供基于 Caffeine 的高性能本地缓存，适用于热点数据缓存</p>
 * <p>缓存特性：</p>
 * <ul>
 *   <li>初始容量：1024 个条目</li>
 *   <li>最大容量：10000 个条目</li>
 *   <li>过期策略：写入后 5 分钟自动过期</li>
 * </ul>
 */
@Deprecated
public class CaffeineConfig {

    /**
     * 创建本地缓存 Bean
     * <p>该缓存适用于存储访问频繁但更新较少的数据，如用户信息、配置项等</p>
     *
     * @return Caffeine 缓存实例，键和值均为 String 类型
     */
    @Bean
    public Cache<String, String> localCache() {
        return Caffeine.newBuilder()
                .initialCapacity(1024)
                .maximumSize(10000L)
                .expireAfterWrite(5L, TimeUnit.MINUTES)
                .build();
    }


}
