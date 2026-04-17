package zhiguang.nauy.auth.token;

import java.time.Instant;

/**
 * 访问令牌与刷新令牌的组合。
 */
public record TokenPair(
        /** 访问令牌 */
        String accessToken,
        /** 访问令牌过期时间 */
        Instant accessTokenExpiresAt,
        /** 刷新令牌 */
        String refreshToken,
        /** 刷新令牌过期时间 */
        Instant refreshTokenExpiresAt,
        /** 刷新令牌ID */
        String refreshTokenId
) {
}