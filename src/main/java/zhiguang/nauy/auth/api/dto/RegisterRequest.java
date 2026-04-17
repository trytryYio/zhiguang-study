package zhiguang.nauy.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import zhiguang.nauy.auth.model.IdentifierType;

/**
 * 注册请求。
 */
public record RegisterRequest(
        // 账号类型
        //    PHONE,
        //    EMAIL;
        @NotNull(message = "账号类型不能为空") IdentifierType identifierType,
//        账号
        @NotBlank(message = "账号不能为空") String identifier,
//        验证码
        @NotBlank(message = "验证码不能为空") String code,
        // 密码
        String password,
        // 是否同意协议
        boolean agreeTerms
) {
}