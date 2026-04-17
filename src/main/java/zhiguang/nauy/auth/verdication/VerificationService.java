package zhiguang.nauy.auth.verdication;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import zhiguang.nauy.auth.api.dto.SendCodeRequest;
import zhiguang.nauy.auth.api.dto.SendCodeResponse;
import zhiguang.nauy.auth.config.AuthProperties;
import zhiguang.nauy.auth.model.IdentifierType;
import zhiguang.nauy.auth.service.AuthService;
import zhiguang.nauy.exception.BusinessException;
import zhiguang.nauy.exception.ErrorCode;
import zhiguang.nauy.exception.ThrowUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * 验证码业务服务。
 * <p>
 * 负责发送与校验验证码：
 * - 速率限制与日限额；
 * - 随机码生成与存储；
 * - 调用发送器进行实际发送；
 * 配置来源于 `AuthProperties.Verification`。
 */
@Service
@RequiredArgsConstructor
public class VerificationService {
    private final VerificationCodeStore verificationCodeStore;
    private final StringRedisTemplate redisTemplate;
    private final AuthProperties properties;
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final CodeSender codeSender;

    /**
     * 校验验证码是否正确且未超限。
     *
     * @param scene      验证码场景。
     * @param identifier 标识（手机号或邮箱）。
     * @param code       用户输入的验证码。
     * @return 校验结果，包含状态与尝试次数统计。
     * @throws BusinessException 参数不完整时抛出。
     */
    public VerificationCheckResult verify(VerificationScene scene, String identifier, String code) {
//        0.判空
        ThrowUtils.throwIf(identifier == null || code == null, ErrorCode.BAD_REQUEST, "请提供正确的验证码校验参数");
        ThrowUtils.throwIf(ObjUtil.isNull( scene), ErrorCode.PARAMS_ERROR, "请提供正确的验证码校验参数");
        return verificationCodeStore.verify(scene.name(), identifier, code);
    }
    /**
     * 发送验证码到指定标识。
     * <p>
     * 执行发送间隔与日次数限制，生成随机数字验证码，保存到存储并调用发送器。
     *
     * @param request 发送验证码请求参数。
     * @return 发送结果，包含标识、场景与过期秒数。
     * @throws BusinessException 验证码场景不支持或参数不完整或触发速率/日限额时抛出。
     */
    public SendCodeResponse sendCode(SendCodeRequest request) {
        // ① 参数校验
        ThrowUtils.throwIf(request==null, ErrorCode.PARAMS_ERROR, "请提供正确的验证码发送参数");
//    获取验证码相关配置
        AuthProperties.Verification cfg = properties.getVerification();
        VerificationScene scene = request.scene();
        String identifier = request.identifier();

        // ② 发送间隔限制（60 秒）
        enforceSendInterval(scene, identifier, cfg.getSendInterval());

        // ③ 每日次数限制（10 次）
        enforceDailyLimit(scene, identifier, cfg.getDailyLimit());
        // ④ 生成 6 位随机数字验证码
        String code = generateNumericCode(cfg.getCodeLength());
        // ⑤ 保存到 Redis Hash
        verificationCodeStore.saveCode(scene.name(), identifier, code, cfg.getTtl(), cfg.getMaxAttempts());        // ⑥ 调用发送器（短信/邮件/日志）
//    // ⑥ 调用发送器（短信/邮件/日志）
        codeSender.sendCode(scene, identifier, code, (int) cfg.getTtl().toMinutes());

//        保存日志
        // ⑦ 返回结果
      return   new SendCodeResponse(identifier,scene, (int) cfg.getTtl().toSeconds());

    }

    /**
     * 生成指定长度的随机数字验证码
     * @param codeLength
     * @return
     */
    private String generateNumericCode(int codeLength) {
        ThrowUtils.throwIf(codeLength<=0, ErrorCode.PARAMS_ERROR, "请提供正确的验证码发送参数");
        String s = RandomUtil.randomNumbers(codeLength);
        return s;
    }

    /**
     * 验证码日次数限制
     * @param scene
     * @param identifier
     * @param dailyLimit
     */
    private void enforceDailyLimit(VerificationScene scene, String identifier, int dailyLimit) {
        // 1️界检查：如果间隔为0或负数，直接返回（不限制）
        if (dailyLimit<=0) {
            return;
        }
        String date = DAY_FORMAT.format(LocalDate.now());// 获取当前日期 格式 : yyyyMMdd


//        auth:code:count:{scene}:{identifier}:{yyyyMMdd} = 计数
        String key = "auth:code:count:" + scene.name() + ":" + identifier + ":" + date;

//        如果 key 不存在 → 创建 key，值设为 1，返回 1
//        如果 key 已存在 → 值 +1，返回新值
        Long count = redisTemplate.opsForValue().increment(key);
        //查询该用户上次发验证码的事件

        // 3️⃣ 如果是第一次调用（count=1），设置过期时间为 1 天
         if (count != null && count == 1L) {
             //expire : 1天 //设置过期事件
            redisTemplate.expire(key, Duration.ofDays(1));
        }
         //如果超出过期事件，则抛出异常
        if (count != null && count > dailyLimit) {
            throw new BusinessException(ErrorCode.VERIFICATION_DAILY_LIMIT);
        }
    }

    /**
     * 验证码发送频率限
     * @param scene
     * @param identifier
     * @param interval
     */
    private void enforceSendInterval(VerificationScene scene, String identifier, Duration interval) {

        // 1️界检查：如果间隔为0或负数，直接返回（不限制）
        if (interval.isZero() || interval.isNegative()) {
            return;
        }

        String key = "auth:code:last:" + scene.name() + ":" + identifier;
        //key : auth:code:last:REGISTER:phone:1787879787897

        //查询该用户上次发验证码的事件
        String existing = redisTemplate.opsForValue().get(key);
        if (existing != null) {
//            4️⃣ 如果存在记录 → 说明还在冷却期内 → 抛出异常
            throw new BusinessException(ErrorCode.VERIFICATION_RATE_LIMIT);
        }
        //interval : 60s 到时间redis自动删除这条信息
        redisTemplate.opsForValue().set(key, "1", interval);
    }


//    /**
//     * 发送验证码到指定标识。
//     * <p>
//     * 执行发送间隔与日次数限制，生成随机数字验证码，保存到存储并调用发送器。
//     *
//     * @param scene      验证码场景（REGISTER/LOGIN/RESET_PASSWORD）。
//     * @param identifier 标识（手机号或邮箱）。
//     * @return 发送结果，包含标识、场景与过期秒数。
//     * @throws BusinessException 参数不完整或触发速率/日限额时抛出。
//     */
//    public SendCodeResult sendCode(VerificationScene scene, String identifier) {
//        if (scene == null || !StringUtils.hasText(identifier)) {
//            throw new BusinessException(ErrorCode.BAD_REQUEST, "请提供正确的验证码发送参数");
//        }
//        AuthProperties.Verification cfg = properties.getVerification();
//        enforceSendInterval(scene, identifier, cfg.getSendInterval());
//        enforceDailyLimit(scene, identifier, cfg.getDailyLimit());
//
//        String code = generateNumericCode(cfg.getCodeLength());
//        codeStore.saveCode(scene.name(), identifier, code, cfg.getTtl(), cfg.getMaxAttempts());
//        codeSender.sendCode(scene, identifier, code, (int) cfg.getTtl().toMinutes());
//        return new SendCodeResult(identifier, scene, (int) cfg.getTtl().toSeconds());
//    }
//
//    /**
//     * 校验验证码是否正确且未超限。
//     *
//     * @param scene      验证码场景。
//     * @param identifier 标识（手机号或邮箱）。
//     * @param code       用户输入的验证码。
//     * @return 校验结果，包含状态与尝试次数统计。
//     * @throws BusinessException 参数不完整时抛出。
//     */
//    public VerificationCheckResult verify(VerificationScene scene, String identifier, String code) {
//        if (scene == null || !StringUtils.hasText(identifier) || !StringUtils.hasText(code)) {
//            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码校验参数不完整");
//        }
//        return codeStore.verify(scene.name(), identifier, code);
//    }
//
//    /**
//     * 使验证码失效（删除存储记录）。
//     *
//     * @param scene      验证码场景。
//     * @param identifier 标识（手机号或邮箱）。
//     */
//    public void invalidate(VerificationScene scene, String identifier) {
//        codeStore.invalidate(scene.name(), identifier);
//    }
//
//    /**
//     * 发送间隔限制：同一标识在指定间隔内只能发送一次。
//     *
//     * @param scene      验证码场景。
//     * @param identifier 标识（手机号或邮箱）。
//     * @param interval   发送间隔。
//     */
//    private void enforceSendInterval(VerificationScene scene, String identifier, Duration interval) {
//        if (interval.isZero() || interval.isNegative()) {
//            return;
//        }
//        String key = "auth:code:last:" + scene.name() + ":" + identifier;
//        String existing = stringRedisTemplate.opsForValue().get(key);
//        if (existing != null) {
//            throw new BusinessException(ErrorCode.VERIFICATION_RATE_LIMIT);
//        }
//        stringRedisTemplate.opsForValue().set(key, "1", interval);
//    }
//
//    /**
//     * 每日发送次数限制：超过上限则抛出限额异常。
//     *
//     * @param scene      验证码场景。
//     * @param identifier 标识（手机号或邮箱）。
//     * @param limit      每日上限次数。
//     */
//    private void enforceDailyLimit(VerificationScene scene, String identifier, int limit) {
//        if (limit <= 0) {
//            return;
//        }
//        String date = DAY_FORMAT.format(LocalDate.now());
//        String key = "auth:code:count:" + scene.name() + ":" + identifier + ":" + date;
//        Long count = stringRedisTemplate.opsForValue().increment(key);
//        if (count != null && count == 1L) {
//            stringRedisTemplate.expire(key, Duration.ofDays(1));
//        }
//        if (count != null && count > limit) {
//            throw new BusinessException(ErrorCode.VERIFICATION_DAILY_LIMIT);
//        }
//    }
//
//    /**
//     * 生成指定长度的纯数字验证码。
//     *
//     * @param length 验证码长度。
//     * @return 数字字符串。
//     */
//    private static String generateNumericCode(int length) {
//        StringBuilder builder = new StringBuilder(length);
//        for (int i = 0; i < length; i++) {
//            builder.append(RANDOM.nextInt(10));
//        }
//        return builder.toString();
//    }
}
