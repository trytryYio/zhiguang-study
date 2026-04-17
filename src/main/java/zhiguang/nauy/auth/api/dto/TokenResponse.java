package zhiguang.nauy.auth.api.dto;

import java.time.Instant;

/**
 * 令牌响应。
 * <p>
 * 返回访问令牌与刷新令牌及其过期时间，供客户端持久化与后续调用使用。
 */
public record TokenResponse(
        // 访问令牌
        String accessToken,
        // 访问令牌过期时间
        Instant accessTokenExpiresAt,
        // 刷新令牌
        String refreshToken,
        // 刷新令牌过期时间
        Instant refreshTokenExpiresAt
) {
}
