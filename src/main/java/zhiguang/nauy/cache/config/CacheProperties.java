package zhiguang.nauy.cache.config;

/**
 * @Description: // 类说明，在创建类时要填写
 * @ClassName: CacheProperties    // 类名，会自动填充
 * @Author: oyy         // 创建者
 * @Date: 2026/4/12 21:44   // 时间
 * @Version: 1.0     // 版本
 */

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


/**
 * 缓存相关配置项，绑定 application.yml 中的 cache.*。
 */
@Component
@ConfigurationProperties(prefix = "cache")
@Data
public class CacheProperties {
    private L2 l2 = new L2();
    private Hotkey hotkey = new Hotkey();

    @Data
    public static class L2 {
        private PublicCfg publicCfg = new PublicCfg();
        private MineCfg mineCfg = new MineCfg();
        private DetailCfg detailCfg = new DetailCfg();
    }

    @Data
    public static class PublicCfg {
        private int ttlSeconds = 15;
        private long maxSize = 1000;
    }

    @Data
    public static class MineCfg {
        private int ttlSeconds = 10;
        private long maxSize = 1000;
    }

    @Data
    public static class DetailCfg {
        private int ttlSeconds = 30;
        private long maxSize = 5000;
    }

    @Data
    public static class Hotkey {
        private int windowSeconds = 60;
        private int segmentSeconds = 10;
        private int levelLow = 50;
        private int levelMedium = 200;
        private int levelHigh = 500;
        private int extendLowSeconds = 20;
        private int extendMediumSeconds = 60;
        private int extendHighSeconds = 120;
    }
}
