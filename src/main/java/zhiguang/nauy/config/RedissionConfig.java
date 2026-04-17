package zhiguang.nauy.config;

import org.redisson.config.Config;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description: Redisson 客户端配置。
 * <p>用于分布式锁（如第四阶段计数重建锁）。</p>
 * @ClassName: RedissionConfig    // 类名，会自动填充
 * @Author: oyy         // 创建者
 * @Date: 2026/4/12 16:47   // 时间
 * @Version: 1.0     // 版本
 */
@Configuration
public class RedissionConfig {

    @Value("${counter.rebuild.lock.watchdog-ms:30000}")
    private long lockWatchdogMs;

    @Bean
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        config.setLockWatchdogTimeout(lockWatchdogMs);// 锁超时时间，防止死锁
        String address = "redis://" + redisProperties.getHost() + ":" + redisProperties.getPort();
        // 单机模式
        SingleServerConfig singleServerConfig = config.useSingleServer().setAddress(address);
        if (redisProperties.getPassword() != null && redisProperties.getPassword().length() > 0)
            singleServerConfig.setPassword(redisProperties.getPassword());
        singleServerConfig.setDatabase(redisProperties.getDatabase());
        return Redisson.create(config);


    }
}
