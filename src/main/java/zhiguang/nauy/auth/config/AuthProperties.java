package zhiguang.nauy.auth.config;

/**
 * @Description: 认证配置  // 类说明，在创建类时要填写
 * @ClassName: AuthProperties    // 类名，会自动填充
 * @Author: oyy         // 创建者
 * @Date: 2026/4/13 23:42   // 时间
 * @Version: 1.0     // 版本
 */

import lombok.Data;
import org.apache.kafka.common.config.types.Password;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.ST;

import java.time.Duration;

/**
 * 认证相关配置属性，绑定前缀 {@code auth.*}。
 */
@Data
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /**
     * JWT 相关配置
     */
    private final Jwt jwt = new Jwt();

    /**
     * 验证码相关配置
     */
    private final Verification verification = new Verification();

    /**
     * 密码相关配置
     */
    private final Password password = new Password();

    /**
     * JWT配置内部类
     */
    @Data
    public static class Jwt {

        /**
         * JWT 签发者
         */
        private String issuer = "zhiguang";

        /**
         * JWT 密钥 ID
         */
        private String keyId = "zhiguang-key";

        /**
         * 私钥文件路径
         */
        private Resource privateKey;

        /**
         * 公钥文件路径
         */
        private Resource publicKey;

        /**
         * Access Token 有效期（Duration 类型，如 PT15M 表示15分钟）
         */
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        /**
         * Refresh Token 有效期（Duration 类型，如 P7D 表示7天）
         */
        private Duration refreshTokenTtl = Duration.ofDays(7);
    }

    /**
     * 验证码相关配置属性
     */
    @Data
    public static class Verification {
        /**
         * 验证码长度
         */
        private int codeLength = 6;

        /**
         * 验证码有效期
         */
        private Duration ttl = Duration.ofMinutes(5);
        /**
         * 最大尝试次数
         */
        private int maxAttempts = 5;
        /**
         * 发送间隔时间
         */
        private Duration sendInterval = Duration.ofSeconds(60);
        /**
         * 每日发送限制次数
         */
        private int dailyLimit = 10;
    }
    /**
     * 密码相关配置属性
     */
    @Data
    public static class Password {
        /**
         * BCrypt 加密强度
         */
        private int bcryptStrength = 12;
        /**
         * 密码最小长度
         */
        private int minLength = 8;
    }
}
