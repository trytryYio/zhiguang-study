package zhiguang.nauy.auth.verdication;

import java.time.Duration;

/**
 * 验证码存储接口。
 * <p>
 * 抽象验证码的保存、校验与失效操作，允许使用 Redis 等实现。
 * 需支持最大尝试次数与 TTL 以保证安全性。
 */
public interface VerificationCodeStore {


    /**
     * 校验验证码。
     *
     * @param scene      场景名称。
     * @param identifier 标识（手机号或邮箱）。
     * @param code       用户输入的验证码。
     * @return 校验结果，包含状态与尝试次数统计。
     */
    VerificationCheckResult verify(String scene, String identifier, String code);

    /**
     * 保存验证码。
     * @param name
     * @param identifier
     * @param code
     * @param ttl
     * @param maxAttempts
     */
    void saveCode(String name, String identifier, String code, Duration ttl, int maxAttempts);

}

