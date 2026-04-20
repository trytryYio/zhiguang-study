package zhiguang.nauy.auth.verdication;


import cn.hutool.core.map.MapUtil;
import jakarta.annotation.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.Integer.parseInt;


/**
 * 基于 Redis 的验证码存储实现。
 * <p>
 * 使用 Hash 结构保存 `code`、`maxAttempts` 与 `attempts`，TTL 控制有效期。
 * 校验时支持尝试计数与错误状态返回，成功后删除键以防重用。
 */
@Component
public class RedisVerificationCodeStoreImpl implements VerificationCodeStore {
    private static final String FIELD_CODE = "code";//验证码字段
    private static final String FIELD_MAX_ATTEMPTS = "maxAttempts";//最大尝试次数字段
    private static final String FIELD_ATTEMPTS = "attempts";//尝试次数字段

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 校验验证码是否匹配，更新尝试计数并在成功时删除记录。
     *
     * @param scene      场景名称。
     * @param identifier 标识（手机号或邮箱）。
     * @param code       用户输入的验证码。
     * @return 校验结果（成功、未找到、错误、尝试过多）。
     */
    @Override
    public VerificationCheckResult verify(String scene, String identifier, String code) {
//        1. 构建 Redis Key
//        根据场景名称和标识（手机号/邮箱）生成唯一的 Redis Key
        String key = buildKey(scene, identifier);
//        2. 从 Redis 获取数据
//        读取 Hash 结构中存储的验证码信息（包括正确验证码、最大尝试次数、已尝试次数）
        HashOperations<String, String, String> ops = stringRedisTemplate.opsForHash();
//        Key: "auth:code:REGISTER:13812345678"
        //├── "code" → "123456"
        //├── "maxAttempts" → "5"
        //└── "attempts" → "2"
        Map<String, String> data = ops.entries(key);// 获取指定 Key 的所有字段和值
//        Map<String, String> data = {
        //    "code": "123456",
        //    "maxAttempts": "5",
        //    "attempts": "2"
//                          }

//        3. 判断验证码是否存在
//        如果 Redis 中没有数据，说明验证码不存在或已过期，直接返回"未找到"
        if (MapUtil.isEmpty(data)) {
            return new VerificationCheckResult(VerificationCodeStatus.NOT_FOUND, 0, 0);
        }
//        4. 检查是否超限
//        如果已尝试次数已经达到或超过最大允许次数，直接返回"尝试次数过多"，不允许继续验证
        String storedCode = data.get(FIELD_CODE);
//        获取 maxAttempts 字段，如果不存在或格式错误，默认为 5
        int maxAttempts =  safeParseInt(data.get(FIELD_MAX_ATTEMPTS), 5);
        int attempts = safeParseInt(data.get(FIELD_ATTEMPTS), 0);
        if (attempts >= maxAttempts) {
            return new VerificationCheckResult(VerificationCodeStatus.TOO_MANY_ATTEMPTS, attempts, maxAttempts);
        }

//        5. 比对验证码
//        如果匹配：删除 Redis 中的记录（防止重用），返回"成功"
//        如果不匹配：进入下一步
        if (Objects.equals(storedCode, code)) {
            stringRedisTemplate.delete(key);
            return new VerificationCheckResult(VerificationCodeStatus.SUCCESS, attempts, maxAttempts);
        }
//        6. 更新尝试计数
//        将已尝试次数加 1，并更新到 Redis
        ops.put(key, FIELD_ATTEMPTS, String.valueOf(++attempts));
//        7. 再次检查是否达到上限
//        如果更新后的次数达到上限： Redis 过期时间 5分，返回"尝试次数过多"
//        如果还没达到上限：返回"验证码错误"，允许继续尝试
        if (attempts >= maxAttempts) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(5));
            // 设置 Redis 的过期时间为 30 分钟
            return new VerificationCheckResult(VerificationCodeStatus.TOO_MANY_ATTEMPTS, attempts, maxAttempts);
        }

//        8. 返回结果
        return new VerificationCheckResult(VerificationCodeStatus.MISMATCH, attempts, maxAttempts);
    }

    /**
     * 保存验证码。
     *
     * @param name        场景名称。
     * @param identifier  标识（手机号或邮箱）。
     * @param code        验证码。
     * @param ttl         验证码有效期。
     * @param maxAttempts 最大尝试次数。
     */
    @Override
    public void saveCode(String name, String identifier, String code, Duration ttl, int maxAttempts) {

//Key: auth:code:REGISTER:13800138000
//Type: hash
//Fields:
//  ├─ code: "123456"          ← 验证码
//  ├─ maxAttempts: "5"        ← 最大允许尝试次数
//  └─ attempts: "0"           ← 当前已尝试次数
//
//TTL: 300 seconds (5分钟)
//      0.  要保存到redis
//        1. 构造key
        // 格式：auth:code:{场景}:{标识}
        String key= buildKey(name, identifier);
        HashOperations<String, String, String> ops = stringRedisTemplate.opsForHash();
        try {
//        2.把生成出来的验证码也存入redis
            ops.put(key, FIELD_CODE, code);
            ops.put(key, FIELD_MAX_ATTEMPTS, String.valueOf(maxAttempts));
            ops.put(key, FIELD_ATTEMPTS, "0");
//        3.设置过期时间
            stringRedisTemplate.expire(key, ttl);
        }catch (DataAccessException ex){
            throw new RedisSystemException("Failed to save verification code", ex);
        }
    }

    /**
     * 删除验证码。
     * @param scene
     * @param identifier
     */
    @Override
    public void invalidate(String scene, String identifier) {
        stringRedisTemplate.delete(buildKey(scene, identifier));

    }


    /**
     * 生成验证码的 Redis 键名。
     *
     * @param scene      场景名称。
     * @param identifier 标识（手机号或邮箱）。
     * @return 键名字符串。
     */
    private static String buildKey(String scene, String identifier) {
        return "auth:code:%s:%s".formatted(scene, identifier);
    }


    /**
     * 安全地解析整数，如果解析失败则返回默认值。
     *
     * @param str          要解析的字符串。
     * @param defaultValue 默认值。
     * @return 解析后的整数，或默认值。
     */
    private static int safeParseInt(String str, int defaultValue) {
        if (str == null || str.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

