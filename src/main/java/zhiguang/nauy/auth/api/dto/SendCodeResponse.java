package zhiguang.nauy.auth.api.dto;


import zhiguang.nauy.auth.verdication.VerificationScene;

/**
 * 发送验证码响应。
 * <p>
 * 返回规范化后的账号、场景，以及验证码有效期（秒）。
 */
public record SendCodeResponse(
        /**
         * 账号。
         */
        String identifier,
        /**
         * 验证码场景。
         */
        VerificationScene scene,
        /**
         * 验证码有效期（秒）。
         */
        int expireSeconds
) {
}
