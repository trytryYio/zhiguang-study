package zhiguang.nauy.auth.verdication;

/**
 * 验证码类型
 * 分别对应：成功、找不到、过期、不匹配、尝试次数过多
  */

public enum VerificationCodeStatus {
    /** 验证成功 */
    SUCCESS,
    /** 验证码不存在 */
    NOT_FOUND,
    /** 验证码已过期 */
    EXPIRED,
    /** 验证码不匹配 */
    MISMATCH,
    /** 尝试次数过多 */
    TOO_MANY_ATTEMPTS
}
