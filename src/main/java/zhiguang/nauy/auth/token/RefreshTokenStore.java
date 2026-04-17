package zhiguang.nauy.auth.token;

import java.time.Duration;

/**
 * 刷新令牌白名单存储接口。
 */
public interface RefreshTokenStore {

    /**
     * 存储刷新令牌。
     *
     * @param userId 用户ID
     * @param tokenId 令牌ID
     * @param ttl 令牌的有效期
     */
    void storeToken(long userId, String tokenId, Duration ttl);

    /**
     * 检查刷新令牌是否有效。
     *
     * @param userId 用户ID
     * @param tokenId 令牌ID
     * @return 如果令牌有效则返回 true，否则返回 false
     */
    boolean isTokenValid(long userId, String tokenId);

    /**
     * 撤销指定的刷新令牌。
     *
     * @param userId 用户ID
     * @param tokenId 令牌ID
     */
    void revokeToken(long userId, String tokenId);

    /**
     * 撤销指定用户的所有刷新令牌。
     *
     * @param userId 用户ID
     */
    void revokeAll(long userId);
}