package zhiguang.nauy.auth.api.dto;

/**
 * 认证响应。登录/注册成功后返回用户信息与令牌。
 */
public record AuthResponse(
        // 用户信息
        AuthUserResponse user,
        // 令牌信息
        TokenResponse token
) {
}