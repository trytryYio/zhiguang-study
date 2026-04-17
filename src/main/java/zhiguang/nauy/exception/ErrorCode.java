package zhiguang.nauy.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    TERMS_NOT_ACCEPTED(50002, "未同意授权"),
    IDENTIFIER_EXISTS(50003, "该账户已存在"),
    IDENTIFIER_NOT_FOUND(50004, "账号不存在"),
    ZGID_EXISTS(50005, "知光号已存在"),
    VERIFICATION_RATE_LIMIT(50006, "验证码发送过于频繁"),
    VERIFICATION_DAILY_LIMIT(50007, "验证码发送次数超限"),
    VERIFICATION_NOT_FOUND(50008, "验证码不存在或已过期"),
    VERIFICATION_MISMATCH(50009, "验证码错误"),
    VERIFICATION_TOO_MANY_ATTEMPTS(50010, "验证码尝试次数过多"),
    INVALID_CREDENTIALS(50011, "登录凭证错误"),
    PASSWORD_POLICY_VIOLATION(50012, "密码强度不足"),
    REFRESH_TOKEN_INVALID(50013, "刷新令牌无效"),
    BAD_REQUEST(40001, "请求参数错误"),
    INTERNAL_ERROR(50001, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
