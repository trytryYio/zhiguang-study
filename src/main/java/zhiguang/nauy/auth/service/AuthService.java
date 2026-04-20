package zhiguang.nauy.auth.service;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import zhiguang.nauy.auth.api.dto.*;
import zhiguang.nauy.auth.audit.service.LoginLogsService;
import zhiguang.nauy.auth.config.AuthProperties;
import zhiguang.nauy.auth.model.ClientInfo;
import zhiguang.nauy.auth.model.IdentifierType;
import zhiguang.nauy.auth.token.JwtService;
import zhiguang.nauy.auth.token.RefreshTokenStore;
import zhiguang.nauy.auth.token.TokenPair;
import zhiguang.nauy.auth.verdication.VerificationCheckResult;
import zhiguang.nauy.auth.verdication.VerificationCodeStatus;
import zhiguang.nauy.auth.verdication.VerificationScene;
import zhiguang.nauy.auth.verdication.VerificationService;
import zhiguang.nauy.common.BaseResponse;
import zhiguang.nauy.exception.BusinessException;
import zhiguang.nauy.exception.ErrorCode;
import zhiguang.nauy.exception.ThrowUtils;
import zhiguang.nauy.user.domain.User;
import zhiguang.nauy.user.service.UserService;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 认证业务服务。
 * 职责：发送验证码、注册、登录、刷新令牌、登出、重置密码、查询当前用户信息。
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    @Resource
    private LoginLogsService loginLogsService;
    @Resource
    private JwtService jwtService;
    @Resource
    private BCryptPasswordEncoder passwordEncoder;
    @Resource
    private AuthProperties authProperties;
    @Resource
    private VerificationService verificationService;
    @Resource
    private UserService userService;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    @Resource
    private RefreshTokenStore refreshTokenStore;

    /**
     * 注册用户
     * 创建用户
     * → 签发双令牌
     * → Refresh Token 写入白名单
     * → 返回 user + token
     *
     * @param request
     * @param clientInfo
     * @return
     */
    public AuthResponse register(RegisterRequest request, ClientInfo clientInfo) {
        String identifier = request.identifier();
        //    ├─ ① 检查是否同意协议
        ThrowUtils.throwIf(!request.agreeTerms(), ErrorCode.TERMS_NOT_ACCEPTED);
//    ├─ ② 校验标识格式（手机号/邮箱正则）
        validateIdentifier(request.identifierType(), identifier);

//    ├─ ③ 检查标识是否已存在 → IDENTIFIER_EXISTS
//        首先根据类型查询
        IDENTIFIER_EXISTS(request.identifierType(), identifier);

//    ├─ ④ 校验验证码 → VerificationService.verify()
//    │   └─ Redis Hash: auth:code:{scene}:{identifier}
//    │       fields: code, maxAttempts, attempts
        String code = request.code();

        VerificationScene register = VerificationScene.REGISTER;
        // 校验验证码
        VerificationCheckResult verificationCheckResult = verificationService.verify(register, identifier, code);
        // 确保验证码成功
        ensureVerificationSuccess(verificationCheckResult);

//    ├─ ⑤ 构建 User 对象（nickname="知光用户"+8位UUID）
        User user = User.builder().nickname("知光用户" + UUID.randomUUID().toString().substring(0, 8)).build();
        switch (request.identifierType()) {
            case PHONE -> {
                user.setPhone(identifier);
            }
            case EMAIL -> {
                user.setEmail(identifier);
            }
        }


//    ├─ ⑥ 如有密码 → validatePassword() → BCrypt 编码
        String password = request.password();
        if (StrUtil.isNotBlank(password)) {
            validatePassword(password);
            user.setPasswordHash(passwordEncoder.encode(password.trim()));
        }
//    ├─ ⑦ userService.createUser(user) → 写 MySQL
        //没有密码 则直接创建user用户
        User user1 = userService.createUser(user);

//    ├─ ⑧ jwtService.issueTokenPair(user) → ★ 签发双令牌
        TokenPair tokenPair = jwtService.issueTokenPair(user1);
//    │   ├─ Access Token: 15min, claims: {uid, token_type="access", nickname}
//    │   └─ Refresh Token: 7d, claims: {uid, token_type="refresh"}
//    │       jti = UUID.randomUUID() ← 这个 ID 用作白名单键
//    ├─ ⑨ storeRefreshToken(userId, tokenPair) → 写入 Redis
//    │   └─ Redis Key: auth:rt:{userId}:{jti} = "1", TTL=7d
        storeRefreshToken(user1.getId(), tokenPair);
//    ├─ ⑩ loginLogService.record() → 写审计日志
//        把这次注册行为留下审计记录。
        loginLogsService.record(user1.getId(), identifier, "REGISTER", clientInfo.ip(), clientInfo.userAgent(), "SUCCESS");

//    └─ ⑪ 返回 AuthResponse(user + tokens)

        return new AuthResponse(mapUser(user1), mapToken(tokenPair));
    }

    /**
     * 查询用户概要信息。
     *
     * @param userId 用户 ID。
     * @return 用户概要响应。
     * @throws BusinessException 当用户不存在时抛出。
     */
    public AuthUserResponse me(long userId) {
        User user = findUserById(userId);
        return mapUser(user);
    }

    /**
     * 根据 ID 查找用户。
     *
     * @param userId 用户 ID。
     * @return 用户 Optional。
     */
    public User findUserById(long userId) {
        return userService.findById(userId);
    }

    /**
     * 验证密码 (密码格式化,包含小写字母 和数字)
     *
     * @param password
     * @return
     */
    public void validatePassword(String password) {
        //0.判空
        ThrowUtils.throwIf(StrUtil.isBlank(password), ErrorCode.PARAMS_ERROR, "密码不能为空");
//        1.密码格式化
        String trim = password.trim();//去空格
        if (trim.length() < authProperties.getPassword().getMinLength()) {
            ThrowUtils.throwIf(true, ErrorCode.PASSWORD_POLICY_VIOLATION, "密码长度不能小于" + authProperties.getPassword().getMinLength());
        }
//        2.密码必须同时包含字母和数字
        boolean hasLetter = trim.chars().anyMatch(Character::isLetter);//匹配字母
        boolean hasDigit = trim.chars().anyMatch(Character::isDigit);//匹配数字
        ThrowUtils.throwIf(!hasLetter || !hasDigit, ErrorCode.PASSWORD_POLICY_VIOLATION, "密码必须同时包含字母和数字");

    }

    /**
     * 查询标识值是否已被绑定
     *
     * @param identifierType
     * @param identifier
     * @return
     */
    public Boolean IDENTIFIER_EXISTS(IdentifierType identifierType, String identifier) {
        //        判空
        ThrowUtils.throwIf(ObjUtil.isNull(identifierType), ErrorCode.PARAMS_ERROR, "标识类型不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(identifier), ErrorCode.PARAMS_ERROR, "标识值不能为空");

        Boolean exists = switch (identifierType) {
            case PHONE -> userService.existsByPhone(identifier);
            case EMAIL -> userService.existsByEmail(identifier);
        };
        ThrowUtils.throwIf(exists, ErrorCode.IDENTIFIER_EXISTS);
        return exists;

    }

    /**
     * 校验标识（手机号/邮箱）的格式。
     *
     * @param identifierType 标识类型：PHONE 或 EMAIL。
     * @param identifier     标识值。
     * @throws BusinessException 当格式不合法时抛出。
     */
    public void validateIdentifier(IdentifierType identifierType, String identifier) {
//        判空
        ThrowUtils.throwIf(ObjUtil.isNull(identifierType), ErrorCode.PARAMS_ERROR, "标识类型不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(identifier), ErrorCode.PARAMS_ERROR, "标识值不能为空");

//        校验标识格式（手机号/邮箱正则）
        boolean isValid = switch (identifierType) {
            case PHONE -> PHONE_PATTERN.matcher(identifier).matches();
            case EMAIL -> EMAIL_PATTERN.matcher(identifier).matches();
        };
        ThrowUtils.throwIf(!isValid, ErrorCode.PARAMS_ERROR, "标识格式错误");
    }

    /**
     * 保证验证码校验成功，否则按状态抛出对应业务异常。
     *
     * @param result 验证码校验结果。
     */
    public Boolean ensureVerificationSuccess(VerificationCheckResult result) {
//        验证码校验成功
        if (result.isSuccess()) {
            return true;
        }
        //不成功的状态
        VerificationCodeStatus status = result.status();
        switch (status) {
            case NOT_FOUND, EXPIRED -> {
                //验证码不存在或已过期
                ThrowUtils.throwIf(true, ErrorCode.VERIFICATION_NOT_FOUND);
            }
            case MISMATCH -> {
                //验证码错误
                ThrowUtils.throwIf(true, ErrorCode.VERIFICATION_MISMATCH);
            }
            case TOO_MANY_ATTEMPTS -> {
                //验证码尝试次数过多
                ThrowUtils.throwIf(true, ErrorCode.VERIFICATION_TOO_MANY_ATTEMPTS);
            }
        }
        return false;

    }

    /**
     * 存储 RefreshToken
     *
     * @param userId
     * @param tokenPair
     */
    public void storeRefreshToken(Long userId, TokenPair tokenPair) {
//        先算Redis TTL -动态计算剩余有效期
        Duration ttl = Duration.between(Instant.now(), tokenPair.refreshTokenExpiresAt());
//        如果 TTL 已经是负数，就归零
        if (ttl.isNegative()) {
            ttl = Duration.ZERO;
        }
        refreshTokenStore.storeToken(userId, tokenPair.refreshTokenId(), ttl);
    }

    /**
     * 映射用户信息
     *
     * @param user
     * @return
     */
    public AuthUserResponse mapUser(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatar(),
                user.getPhone(),
                user.getZgId(),
                user.getBirthday() != null ? LocalDateTimeUtil.of(user.getBirthday()).toLocalDate() : null,
                user.getSchool(),
                user.getBio(),
                user.getGender(),
                JSONUtil.toJsonStr(user.getTagsJson())
        );
    }

    /**
     * 把内部令牌对象转化为接口响应
     *
     * @param tokenPair
     * @return
     */
    public TokenResponse mapToken(TokenPair tokenPair) {
        return new TokenResponse(
                tokenPair.accessToken(),
                tokenPair.accessTokenExpiresAt(),
                tokenPair.refreshToken(),
                tokenPair.refreshTokenExpiresAt()
        );
    }

    /**
     * 刷新令牌
     *
     * @param refreshToken
     * @return TokenResponse
     */
    public TokenResponse refresh(TokenRefreshRequest refreshToken) {
        // ① 解码 Refresh Token
//        通过 jwt令牌加密的 也需要通过jwt来解码
//        为什么我能解码 -> jwtConfig 中重新配置了JwtDecoder 这个bean 所以 可以解码
        Jwt jwt = decodeRefreshToken(refreshToken.refreshToken());

        // ② 类型检查：必须是 refresh token 而不是 access token
        if (!Objects.equals("refresh", jwtService.extractTokenType(jwt))) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        // ③ 从载荷中提取身份信息
        long userId = jwtService.extractUserId(jwt);
        String tokenId = jwtService.extractTokenId(jwt);

        // ④ 白名单校验（Redis）
//            不在白名单中
        ThrowUtils.throwIf(!refreshTokenStore.isTokenValid(userId, tokenId), ErrorCode.REFRESH_TOKEN_INVALID);
        // ⑤ 查数据库确认用户仍存在
        User user = userService.findById(userId);
        ThrowUtils.throwIf(ObjUtil.isNull(user), ErrorCode.NOT_FOUND_ERROR);

        // ⑥ 签发新令牌对
        TokenPair tokenPair = jwtService.issueTokenPair(user);
        // ⑦ 轮换：撤销旧的，写入新的
//        删除旧的
        refreshTokenStore.revokeToken(userId, tokenId);
//      写入新的
        storeRefreshToken(userId, tokenPair);
        // ⑧ 返回响应

        return mapToken(tokenPair);

    }

    /**
     * 解码刷新令牌，失败时抛业务异常。
     *
     * @param refreshToken 刷新令牌字符串。
     * @return 解析得到的 JWT。
     * @throws BusinessException 当刷新令牌无法解析时抛出。
     */
    public Jwt decodeRefreshToken(String refreshToken) {
        try {
            return jwtService.decode(refreshToken);
        } catch (JwtException ex) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    /**
     * 重置密码
     *
     * @param request
     */

    public void resetPassword(PasswordResetRequest request) {
        //1.校验输入的格式+
        validateIdentifier(request.identifierType(), request.identifier());
        validatePassword(request.newPassword());
//        2.标准化标识 把手机号/邮箱统一 格式
        String identifier = normalizeIdentifier(request.identifierType(), request.identifier());
//        3.查找用户
        User user = findUserByIdentifier(request.identifierType(), identifier);
//        4.校验验证码
        ensureVerificationSuccess(
                verificationService.verify(VerificationScene.RESET_PASSWORD, identifier, request.code())
        );
        //        5.更新密码hash值
        String newPassword = request.newPassword();
        user.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
//        6.持久化
        userService.updateById(user);
//        7.★ 撤销所有 Refresh Token（强制所有设备下线）
        refreshTokenStore.revokeAll(user.getId());
    }

    /**
     * 根据标识查找用户
     *
     * @param identifierType
     * @param identifier
     * @return
     */
    public User findUserByIdentifier(IdentifierType identifierType, String identifier) {
        ThrowUtils.throwIf(identifierType == null, ErrorCode.PARAMS_ERROR, "标识不能为空");
        ThrowUtils.throwIf(identifier.isEmpty(), ErrorCode.PARAMS_ERROR, "标识不能为空");
//        判断类别
        User user = null;
        switch (identifierType) {

            case PHONE -> {
                user = userService.findByPhone(identifier);
            }
            case EMAIL -> {
                user = userService.findByEmail(identifier);
            }
        }
        ThrowUtils.throwIf(ObjUtil.isNull(user), ErrorCode.IDENTIFIER_NOT_FOUND);
        return user;
    }

    /**
     * 标准化标识
     *
     * @param type
     * @param identifier
     * @return
     */
    public String normalizeIdentifier(IdentifierType type, String identifier) {
        return switch (type) {
            case PHONE -> identifier.trim();
            case EMAIL -> identifier.trim().toLowerCase(Locale.ROOT);
        };
    }


    /**
     * 登出
     * }
     *
     * @param refreshToken
     */
    public void logout(String refreshToken) {
        //登出时撤销 Refresh Token
        decodeRefreshTokenSafely(refreshToken).ifPresent(jwt -> {
            // 仅处理刷新令牌
            if (Objects.equals("refresh", jwtService.extractTokenType(jwt))) {
//                获取用户在jwt 中的id
                long userId = jwtService.extractUserId(jwt);
                String tokenId = jwtService.extractTokenId(jwt);
//                撤销刷新令牌
                refreshTokenStore.revokeToken(userId, tokenId);
            }

        });

    }

    /**
     * 安全解码 Refresh Token，并返回 JWT 对象。
     *
     * @param refreshToken
     * @return
     */
    private Optional<Jwt> decodeRefreshTokenSafely(String refreshToken) {
        try {
            return Optional.of(jwtService.decode(refreshToken));
        } catch (JwtException ex) {
            return Optional.empty();
        }

    }

    /**
     * 登录
     *
     * @param request
     * @param clientInfo
     * @return
     */
    public AuthResponse login(LoginRequest request, ClientInfo clientInfo) {
//        登录的dto
        String identifier = request.identifier();
        IdentifierType identifierType = request.identifierType();
        String code = request.code();
//    客户端的dto

//        1.校验标识格式（手机号/邮箱）
        validateIdentifier(identifierType, identifier);
        //        2.标准化处理（去除空格、统一格式）
        String password = request.password();
        if (password!=null) {
            validatePassword(password);
        }
        //标准化处理（去除空格、统一格式）
        identifier = normalizeIdentifier(request.identifierType(), request.identifier());

//        3.查询用户是否存在
        User user = findUserByIdentifier(identifierType, identifier);
        if (user == null) {
            ThrowUtils.throwIf(true, ErrorCode.IDENTIFIER_NOT_FOUND);
        }
//        4.判断认证方式并验证
        String channel;
//        4.1
        //channel : 密码
        if (StrUtil.isNotBlank(request.password())) {
            channel = "PASSWORD";

            if (!StringUtils.hasText(user.getPasswordHash()) ||
                    !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                {
                    // 记录失败的登录日志
                    loginLogsService.record(user.getId(), identifier, channel, clientInfo.ip(), clientInfo.userAgent(), "FAILED");
                    throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);

                }
            }
        }else if (StrUtil.isNotBlank(request.code())){
//            4.2
            channel="CODE";
//            验证验证码是否正确
           Boolean result = ensureVerificationSuccess(verificationService.verify(VerificationScene.LOGIN, identifier, code));
           if (!result) {
               loginLogsService.record(user.getId(), identifier, channel, clientInfo.ip(), clientInfo.userAgent(), "FAILED");
               ThrowUtils.throwIf(true, ErrorCode.VERIFICATION_MISMATCH);
           }
        }
        else {
            // 4.3 既没有密码也没有验证码，参数不完整
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请提供验证码或密码");
        }
//        5.签发 jwt token对
        TokenPair tokenPair = jwtService.issueTokenPair(user);
        // 6. 将 Refresh Token 存入 Redis 白名单
        storeRefreshToken(user.getId(), tokenPair);
        // 7. 记录成功的登录日志（包含 IP、UA、认证渠道等信息）
        loginLogsService.record(user.getId(), identifier, channel, clientInfo.ip(), clientInfo.userAgent(), "SUCCESS");
        // 8. 返回认证响应（用户信息 + Token 信息）
        return new AuthResponse(mapUser(user), mapToken(tokenPair));
    }
}
