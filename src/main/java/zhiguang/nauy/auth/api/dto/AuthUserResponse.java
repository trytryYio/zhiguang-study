package zhiguang.nauy.auth.api.dto;

import java.time.LocalDate;

/**
 * 认证用户响应。
 */
public record AuthUserResponse(
        Long id,
        String nickname,
        String avatar,
        String phone,
        String zhId,
        LocalDate birthday,
        String school,
        String bio,
        String gender,
        String tagJson
) {
}