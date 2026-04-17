package zhiguang.nauy.auth.token;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * 基于 Redis 的刷新令牌白名单存储。
 * 键空间：`auth:rt:{userId}:{tokenId}`，值固定为 "1"。
 */
@Component
public class RedisRefreshTokenStoreImpl implements RefreshTokenStore {

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenStoreImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    /**
     * 将刷新令牌存入 Redis，设置过期时间。
     *
     * @param userId  用户 ID
     * @param tokenId 刷新令牌的唯一标识（jti）
     * @param ttl     令牌的有效期时长
     */
    @Override
    public void storeToken(long userId, String tokenId, Duration ttl) {
        // Redis Key: auth:rt:{userId}:{tokenId}

        String key = key(userId, tokenId);
//        Redis 服务端会自动根据设置的 TTL 进行倒计时，到期后自动删除该键。
        redisTemplate.opsForValue().set(key, "1", ttl);

    }
    /**
     * 检查刷新令牌是否有效（是否存在于 Redis 中）。
     *
     * @param userId  用户 ID
     * @param tokenId 刷新令牌的唯一标识（jti）
     * @return true 表示令牌有效，false 表示令牌不存在或已过期
     */
    @Override
    public boolean isTokenValid(long userId, String tokenId) {
        String key = key(userId, tokenId);
//         有效（key 存在且值为 "1"）
        return Objects.equals("1", redisTemplate.opsForValue().get(key));
    }
    /**
     * 撤销指定的刷新令牌（从 Redis 中删除）。
     *
     * @param userId  用户 ID
     * @param tokenId 要撤销的刷新令牌的唯一标识（jti）
     */
    @Override
    public void revokeToken(long userId, String tokenId) {
        redisTemplate.delete(key(userId, tokenId));
    }
    /**
     * 撤销指定用户的所有刷新令牌（批量删除）。
     * 用于用户登出、修改密码等需要使所有设备失效的场景。
     *
     * @param userId 用户 ID
     */
    @Override
    public void revokeAll(long userId) {
        String pattern = "auth:rt:%d:*".formatted(userId);
        var keys = redisTemplate.keys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    /**
     * 生成 Redis 键名，格式为 auth:rt:{userId}:{tokenId}。
     *
     * @param userId  用户 ID
     * @param tokenId 刷新令牌的唯一标识（jti）
     * @return Redis 键名字符串
     */
    private static String key(long userId, String tokenId) {
        // Redis Key: auth:rt:{userId}:{tokenId}
        return "auth:rt:%d:%s".formatted(userId, tokenId);
    }
}