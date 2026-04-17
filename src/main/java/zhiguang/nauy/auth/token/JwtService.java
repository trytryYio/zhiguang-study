package zhiguang.nauy.auth.token;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import zhiguang.nauy.auth.config.AuthProperties;
import zhiguang.nauy.user.domain.User;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * JWT 令牌服务。
 * 功能：签发 Access/Refresh Token（RS256），解码 JWT，提取用户 ID、令牌类型与令牌 ID。
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String CLAIM_USER_ID = "uid";

    /** JWT 编码器- 把你构造好的 claims 真正编码成 JWT 字符串，并用 RSA 私钥做 RS256 签名。**/
    private final JwtEncoder jwtEncoder;
    /** J把 JWT 字符串解码并验签。**/
    private final JwtDecoder jwtDecoder;
    /** 这里拿的是 `AuthProperties`。
     它提供：

     - `issuer = "zhiguang"`
     - `accessTokenTtl = 15 分钟`
     - `refreshTokenTtl = 7 天`。**/
    private final AuthProperties properties;
//    将来写测试时，可以替换成固定时间，方便验证 token 的过期逻辑。
    private final Clock clock = Clock.systemUTC();


    /**
     * 为指定用户签发访问令牌（Access Token）和刷新令牌（Refresh Token）。
     *
     * @param user 当前登录用户信息
     * @return 包含访问令牌、刷新令牌及其过期时间和ID的 TokenPair 对象
     */
    public TokenPair issueTokenPair(User user) {
        // 生成刷新令牌的唯一标识 ID
        String refreshTokenId = UUID.randomUUID().toString();
        // 获取当前时间作为令牌签发时间
        Instant issuedAt = Instant.now(clock);
        // 计算访问令牌的过期时间
        Instant accessExpiresAt = issuedAt.plus(properties.getJwt().getAccessTokenTtl());
        // 计算刷新令牌的过期时间
        Instant refreshExpiresAt = issuedAt.plus(properties.getJwt().getRefreshTokenTtl());

        // 编码生成访问令牌，类型为 "access"，并分配唯一的令牌 ID
        String accessToken = encodeToken(user, issuedAt, accessExpiresAt, "access", UUID.randomUUID().toString());
        // 编码生成刷新令牌，类型为 "refresh"，使用预生成的 refreshTokenId
        String refreshToken = encodeRefreshToken(user, issuedAt, refreshExpiresAt, refreshTokenId);

        // 返回包含令牌字符串、过期时间及刷新令牌 ID 的结果对象
//        TokenPair 一个把“双令牌字符串 + 双过期时间 + refreshTokenId”打包起来的返回载体
        return new TokenPair(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt, refreshTokenId);
    }

    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    private String encodeToken(User user, Instant issuedAt, Instant expiresAt, String tokenType, String tokenId) {

        /*
         * 构建 JWT Claims（载荷部分）。
         * 包含标准声明和自定义声明：
         * - issuer: 签发者标识（来自配置）
         * - issuedAt/expiresAt: 令牌生效和过期时间
         * - subject: 用户 ID（字符串形式，符合 JWT 规范）
         * - id: 令牌的唯一标识 jti，用于刷新令牌追踪
         * - claim(CLAIM_TOKEN_TYPE): 区分 access/refresh 令牌类型
         * - claim(CLAIM_USER_ID): 数值型用户 ID，方便解析后直接使用
         * - claim("nickname"): 用户昵称，减少额外查询
         */
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .id(tokenId)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .claim(CLAIM_USER_ID, user.getId())
                .claim("nickname", user.getNickname())
                .build();
        // 编码生成 JWT 令牌 Jwt令牌结果大概是:
//        伪代码:JWT 令牌由三部分组成，用 . 分隔：
//        头部 Header{ }.荷载 Payload{ }.签名 Signature{ }
//        荷载有:{
//  "iss": "zhiguang",           // 签发者
//  "iat": 1713168000,           // 签发时间 (Unix时间戳)
//  "exp": 1713168900,           // 过期时间 (15分钟后)
//  "sub": "123",                // 主题 (用户ID字符串)
//  "jti": "uuid-string",        // JWT ID (唯一标识)
//  "token_type": "access",      // 令牌类型
//  "uid": 123,                  // 用户ID (数值型)
//  "nickname": "张三"            // 用户昵称
//}
//        组合后在最终:
//        头部
//        eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.
//        荷载
//eyJpc3MiOiJ6aGlndWFuZyIsImlhdCI6MTcxMzE2ODAwMCwiZXhwIjoxNzEzMTY4OTAwLCJzdWIiOiIxMjMiLCJqdGkiOiJhMWIyYzNkNC1lNWY2LTc4OTAiLCJ0b2tlbl90eXBlIjoiYWNjZXNzIiwidWlkIjoxMjMsIm5pY2tuYW1lIjoi5Lit5Zu9In0.
//        签名
//MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
//        把 claims 变成 JWT 字符串。并且底层会用 RSA 私钥完成签名。
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private String encodeRefreshToken(User user, Instant issuedAt, Instant expiresAt, String tokenId) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .id(tokenId)
                .claim(CLAIM_TOKEN_TYPE, "refresh")
                .claim(CLAIM_USER_ID, user.getId())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * 从 JWT 中提取用户 ID。
     * @param jwt
     * @return
     */
    public long extractUserId(Jwt jwt) {
//        从荷载中获取用户 id 好处 :不用在查询数据库
        Object claim = jwt.getClaims().get(CLAIM_USER_ID);
//        判断 类型 是否 数字
        if (claim instanceof Number number) {
            return number.longValue();
        }
        if (claim instanceof String text) {
            return Long.parseLong(text);
        }
//        既不是数字 也不是字符串 报错
        throw new IllegalArgumentException("Invalid user id in token");
    }

    /**
     * 从 JWT 中提取令牌类型。  access /refresh
     * @param jwt
     * @return
     */
    public String extractTokenType(Jwt jwt) {
        Object claim = jwt.getClaims().get(CLAIM_TOKEN_TYPE);
        return claim != null ? claim.toString() : "";
    }

    /**
     * 从 JWT 中提取令牌 ID。
     * @param jwt
     * @return
     */
    public String extractTokenId(Jwt jwt) {
        return jwt.getId();
    }
}