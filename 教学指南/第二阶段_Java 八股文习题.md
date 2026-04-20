# 项目 Java 八股知识点总结

> 基于知光项目用户认证系统实际使用的技术栈生成，共 15 道题目，覆盖 Spring Security、JWT、Redis、MyBatis、密码学等领域。

---

## 第一部分：自测习题（仅问题，预留答题区域）

以下为项目涉及核心知识点习题，可自行书写答案。

---

### 问题 1【Spring Security】中级
**项目来源：** `src/main/java/zhiguang/nauy/auth/config/SecurityConfig.java:47-68` — SecurityFilterChain 配置方法

**问题描述：** 
项目中配置了 OAuth2 Resource Server + JWT 验签，请解释：
1. `.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))` 这行代码的作用是什么？
2. Spring Security 如何处理携带 `Authorization: Bearer <token>` 的请求？
3. `@AuthenticationPrincipal Jwt jwt` 参数是如何被解析的？

---
答案：




---

### 问题 2【JWT/令牌安全】高级
**项目来源：** `src/main/java/zhiguang/nauy/auth/token/JwtService.java:39-48` — issueTokenPair 方法

**问题描述：**
项目采用 Access Token(15 分钟) + Refresh Token(7 天) 的双令牌机制：
1. 为什么需要双令牌设计？只用一个长期有效的 Access Token 有什么问题？
2. Refresh Token 为什么要设计白名单机制（Redis 存储）？
3. 如果 Access Token 被盗用，攻击窗口期有多长？如何缩短这个窗口？

---
答案：




---

### 问题 3【Redis 应用】中级
**项目来源：** `src/main/java/zhiguang/nauy/auth/token/RedisRefreshTokenStoreImpl.java:30-34` — storeToken 方法

**问题描述：**
```java
redisTemplate.opsForValue().set(key, "1", ttl);
```
1. Redis 的 Key 设计为 `auth:rt:{userId}:{tokenId}` 有什么考虑？
2. 使用 `ttl` 参数设置过期时间与依赖 Redis 键空间通知 (Keyspace Notifications) 有什么区别？
3. 如果 Redis 宕机，对认证系统有什么影响？如何容灾？

---
答案：




---

### 问题 4【密码学】中级
**项目来源：** `src/main/java/zhiguang/nauy/auth/config/PemUtils.java:36-48` — readPrivateKey 方法

**问题描述：**
1. 项目使用 RSA-2048 非对称加密签名 JWT，为什么不用 HMAC-SHA256 对称加密？
2. PKCS#8 格式 (`-----BEGIN PRIVATE KEY-----`) 与传统的 PKCS#1 格式 (`-----BEGIN RSA PRIVATE KEY-----`) 有什么区别？
3. `Base64.getDecoder().decode()` 解码后得到的字节数组，如何通过 `PKCS8EncodedKeySpec` 还原成 `RSAPrivateKey` 对象？

---
答案：




---

### 问题 5【Spring Boot】初级
**项目来源：** `src/main/java/zhiguang/nauy/auth/config/AuthConfiguration.java:16-20` — @EnableConfigurationProperties

**问题描述：**
```java
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
@RequiredArgsConstructor
public class AuthConfiguration {
    private final AuthProperties properties;
}
```
1. `@EnableConfigurationProperties` 的作用是什么？不写这个注解会发生什么？
2. `AuthProperties` 类上的 `@ConfigurationProperties(prefix = "auth")` 是如何将 `application.yml` 中的配置绑定到 Java 对象的？
3. Lombok 的 `@RequiredArgsConstructor` 在这里生成了什么样的构造函数？

---
答案：




---

### 问题 6【并发编程】高级
**项目来源：** `src/main/java/zhiguang/nauy/auth/verdication/VerificationService.java:103-118` — enforceDailyLimit 方法

**问题描述：**
```java
Long count = redisTemplate.opsForValue().increment(key);
if (count != null && count == 1L) {
    redisTemplate.expire(key, Duration.ofDays(1));
}
```
1. `increment()` 操作是原子的吗？为什么不需要加锁？
2. 如果先 `increment()` 再 `expire()`，是否存在并发问题？如果有，如何解决？
3. Redis 的原子操作还有哪些？（至少列举 3 个）

---
答案：




---

### 问题 7【MyBatis】中级
**项目来源：** `src/main/resources/mapper/UserMapper.xml:297-327` — insert 语句

**问题描述：**
```xml
<insert id="insert" parameterType="com.tongji.user.domain.User"
        useGeneratedKeys="true" keyProperty="id">
```
1. `useGeneratedKeys="true"` 和 `keyProperty="id"` 的作用分别是什么？
2. MySQL 的自增主键是如何返回并赋值给 User 对象的 id 字段的？
3. 如果使用 Oracle（使用序列），这段 XML 应该如何修改？

---
答案：




---

### 问题 8【数据库事务】中级
**项目来源：** `src/main/java/zhiguang/nauy/user/service/impl/UserServiceImpl.java:66-72` — createUser 方法

**问题描述：**
```java
@Transactional
public User createUser(User user) {
    Instant now = Instant.now();
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    userMapper.insert(user);
    return user;
}
```
1. `@Transactional` 不加任何参数时，默认的事务传播行为和隔离级别是什么？
2. 如果在 `userMapper.insert(user)` 之后抛出了 RuntimeException，会发生什么？
3. 如果将方法改为 `@Transactional(readOnly = true)`，`insert` 操作还能执行成功吗？为什么？

---
答案：




---

### 问题 9【异常处理】初级
**项目来源：** `src/main/java/zhiguang/nauy/auth/service/AuthService.java:78-81` — validatePassword 方法

**问题描述：**
```java
ThrowUtils.throwIf(!hasLetter || !hasDigit, ErrorCode.PASSWORD_POLICY_VIOLATION, "密码必须同时包含字母和数字");
```
1. `ThrowUtils` 是一个工具类，它的设计模式是什么？相比直接 `if (...) throw new BusinessException(...)` 有什么优势？
2. `ErrorCode` 枚举通常应该包含哪些字段？（提示：code、message、httpStatus）
3. 全局异常处理器 (`@RestControllerAdvice`) 如何捕获 `BusinessException` 并返回统一的 JSON 格式？

---
答案：




---

### 问题 10【Spring AOP】高级
**项目来源：** `src/main/java/zhiguang/nauy/auth/api/AuthController.java:58-62` — me 方法

**问题描述：**
```java
@GetMapping("/me")
public BaseResponse<AuthUserResponse> me(@AuthenticationPrincipal Jwt jwt) {
    long userId = jwtService.extractUserId(jwt);
    return ResultUtils.success(authService.me(userId));
}
```
1. `@AuthenticationPrincipal` 注解背后的 HandlerMethodArgumentResolver 是如何工作的？
2. Spring Security 如何在控制器方法执行前完成 JWT 的解析和用户信息的提取？
3. 如果要自定义一个 `@RequireRole("ADMIN")` 注解实现角色校验，应该使用什么技术？（拦截器？过滤器？AOP？）

---
答案：




---

### 问题 11【设计模式】中级
**项目来源：** `src/main/java/zhiguang/nauy/auth/token/RefreshTokenStore.java` — 接口定义

**问题描述：**
```java
public interface RefreshTokenStore {
    void storeToken(long userId, String tokenId, Duration ttl);
    boolean isTokenValid(long userId, String tokenId);
    void revokeToken(long userId, String tokenId);
    void revokeAll(long userId);
}
```
1. 为什么要定义接口而不是直接在 Controller 中调用 `RedisRefreshTokenStoreImpl`？
2. 如果将来要支持"将所有刷新令牌存入 MySQL"作为持久化备份，应该如何设计？
3. 这体现了什么设计原则？（提示：SOLID 中的哪一个？）

---
答案：




---

### 问题 12【HTTP/REST】初级
**项目来源：** `src/main/java/zhiguang/nauy/auth/api/AuthController.java:29-36` — 类注解

**问题描述：**
```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController { ... }
```
1. `@RestController` 和 `@Controller` 的区别是什么？
2. `@Validated` 注解的作用是什么？它与 `@Valid` 有什么区别？
3. 如果 `@RequestBody RegisterRequest request` 校验失败，Spring 会抛出什么异常？如何统一处理？

---
答案：




---

### 问题 13【JVM/性能调优】高级
**项目来源：** 整个认证模块

**问题描述：**
假设系统上线后 QPS 达到 5000，认证接口平均响应时间从 50ms 上升到 200ms：
1. 如何使用 Arthas 或 JProfiler 定位瓶颈是在 BCrypt 加密、Redis 操作还是 JWT 签发？
2. BCrypt 的 `strength` 参数从 12 降到 10 会对安全性和性能产生什么影响？
3. 如果 Redis 成为瓶颈，有哪些优化手段？（至少列举 3 种）

---
答案：




---

### 问题 14【安全攻防】高级
**项目来源：** 整个认证流程

**问题描述：**
1. **暴力破解**：攻击者每秒尝试 1000 次验证码，项目的限流策略能否防御？如何加强？
2. **重放攻击**：攻击者截获了一个合法的 Access Token，在 15 分钟内反复使用，如何防御？
3. **JWT 伪造**：如果攻击者将算法从 RS256 改为 HS256，并使用公钥作为 HMAC 密钥，Spring Security 能否识别？为什么？

---
答案：




---

### 问题 15【分布式系统】高级
**项目来源：** `src/main/java/zhiguang/nauy/auth/verdication/VerificationService.java:95-100` — sendCode 方法

**问题描述：**
```java
// ③ 每日次数限制（10 次）
enforceDailyLimit(scene, identifier, cfg.getDailyLimit());
// ④ 生成 6 位随机数字验证码
String code = generateNumericCode(cfg.getCodeLength());
// ⑤ 保存到 Redis Hash
verificationCodeStore.saveCode(scene.name(), identifier, code, cfg.getTtl(), cfg.getMaxAttempts());
```
1. 上述代码在分布式环境下（多实例部署）是否存在竞态条件？如果有，请描述场景。
2. 如何使用 Redis Lua 脚本或 Redlock 保证"检查限额 → 生成验证码 → 保存"的原子性？
3. 如果要求"同一手机号在全局 1 分钟内只能发送 1 次验证码"，Key 应该如何设计？

---
答案：




---

## 第二部分：参考答案详解

---

### 问题 1

**参考答案：**

**1. `.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))` 的作用：**

这行代码启用了 OAuth2 资源服务器功能，并配置使用 JWT 进行令牌验证。具体来说：
- Spring Security 会自动配置一个 `JwtAuthenticationConverter`
- 从 HTTP 请求头的 `Authorization: Bearer <token>` 中提取 JWT
- 使用配置的 `JwtDecoder` 进行解码和验签
- 将解析出的 Claims 转换为 `Authentication` 对象存入 `SecurityContext`

**2. Spring Security 处理Bearer Token 的流程：**

```
请求进入 → BearerTokenAuthenticationFilter 拦截 
       → 提取 Authorization Header 中的 token 
       → JwtDecoder.decode(token) 解码并验签 
       → JwtAuthenticationToken 创建并放入 SecurityContext 
       → FilterChain.doFilter() 继续执行到 Controller
```

关键组件：
- `BearerTokenAuthenticationFilter`: 负责提取和初步验证 token
- `JwtDecoder`: 负责解码和验签（项目中使用 NimbusJwtDecoder + RSA 公钥）
- `JwtAuthenticationConverter`: 将 Jwt 转换为 Authentication

**3. `@AuthenticationPrincipal Jwt jwt` 的解析机制：**

这是通过 `HandlerMethodArgumentResolver` 实现的：
- Spring MVC 在处理 Controller 方法参数时，会遍历所有注册的 ArgumentResolver
- `AuthenticationPrincipalArgumentResolver` 检测到 `@AuthenticationPrincipal` 注解
- 从 `SecurityContextHolder.getContext().getAuthentication()` 获取认证信息
- 由于之前 JWT 过滤器已经将 `JwtAuthenticationToken` 放入 Context，所以可以直接提取
- 最终将 Jwt 对象注入到方法参数中

---

### 问题 2

**参考答案：**

**1. 双令牌设计的必要性：**

| 对比项 | 单令牌 (长期 Access Token) | 双令牌机制 |
|--------|---------------------------|------------|
| 泄露风险 | Token 泄露后，整个有效期内都可被滥用 | Access Token 只有 15 分钟窗口期 |
| 撤销难度 | 无法主动撤销，只能等待过期 | 可通过撤销 Refresh Token 阻止新 Access Token 签发 |
| 用户体验 | 频繁重新登录 | 无感刷新 |

**核心思想：** Access Token 短期有效减少暴露窗口，Refresh Token 长期有效但存储在白名单中便于撤销。

**2. Refresh Token 白名单的原因：**

- **可撤销性**：当用户修改密码、登出、或发现异常登录时，可以立即撤销 Refresh Token，阻止攻击者获取新的 Access Token
- **设备管理**：每个 Refresh Token 有唯一的 `jti`，可以实现"踢下线"特定设备
- **审计追踪**：可以记录每个 Refresh Token 的签发时间、IP、设备信息

如果 Refresh Token 像 Access Token 一样 Stateless，那么一旦签发就无法中途作废，只能等待 7 天后过期。

**3. Access Token 盗用的攻击窗口及优化：**

- **当前窗口期**：15 分钟
- **缩短窗口的方法**：
  1. 将 Access Token TTL 从 15 分钟降到 5 分钟（但会增加 Refresh 频率）
  2. 引入 Refresh Token 轮换机制：每次刷新后废弃旧的 Refresh Token，攻击者用旧 Token 刷新时会触发告警
  3. 绑定 Access Token 到客户端指纹（IP + UserAgent 哈希），服务端校验一致性

---

### 问题 3

**参考答案：**

**1. Key 设计的考虑：**

```
auth:rt:{userId}:{tokenId}
  │     │      │         │
  │     │      │         └─ Refresh Token 的 jti，全局唯一
  │     │      └─────────── 用户 ID，支持按用户维度撤销
  │     └────────────────── refresh token 标识
  └──────────────────────── 认证域命名空间
```

- **命名空间隔离**：`auth:` 前缀避免与其他业务 Key 冲突
- **按用户聚合**：`{userId}`使得`revokeAll(userId)`可以用 pattern `auth:rt:{userId}:*`批量删除
- **唯一索引**：`{tokenId}`确保每个 Refresh Token 独立管理

**2. TTL 机制 vs Keyspace Notifications：**

| 方案 | 原理 | 优缺点 |
|------|------|--------|
| 设置 TTL | Redis 服务端自动倒计时，到期删除 | 简单可靠，无需额外配置；但无法实时感知过期事件 |
| Keyspace Notifications | Redis 发布 `__keyevent@0__:expired` 事件，客户端订阅 | 可实时感知，但增加复杂度，生产环境通常关闭此功能 |

项目选择 TTL 是因为：刷新令牌只需在**使用时**验证有效性，不需要实时感知过期。

**3. Redis 宕机的影响及容灾：**

**影响：**
- 新用户无法登录（无法写入 Refresh Token 白名单）
- 已有用户刷新 Token 失败（无法验证 Refresh Token）
- 登出功能失效（无法撤销 Token）

**容灾方案：**
1. **Redis Cluster**：多节点高可用，主从切换
2. **本地缓存降级**：短暂宕机时，用 Caffeine 缓存已验证过的 Token，允许有限次数的刷新
3. **MySQL 持久化备份**：将白名单同步写入 MySQL，Redis 恢复后从 MySQL 重建

---

### 问题 4

**参考答案：**

**1. RSA 非对称加密 vs HMAC 对称加密：**

| 对比 | RSA-2048 (RS256) | HMAC-SHA256 (HS256) |
|------|------------------|---------------------|
| 密钥管理 | 私钥签名、公钥验签，公钥可公开分发 | 单一密钥，所有服务都需持有 |
| 密钥泄露风险 | 私钥泄露才可伪造 Token | 密钥泄露即可伪造任意 Token |
| 适用场景 | 微服务架构、多团队、跨系统 | 单体应用、单一信任域 |

**为什么选 RSA：**
- 未来扩展性：当系统拆分为多个微服务时，各服务只需公钥验签，私钥由认证中心保管
- 安全性：即使某个服务的公钥泄露，也无法伪造 Token

**2. PKCS#8 vs PKCS#1：**

| 格式 | OID 标识 | 编码内容 | OpenSSL 命令 |
|------|----------|----------|--------------|
| PKCS#1 | `1.2.840.113549.1.1.1` | 仅 RSA 密钥参数 | `openssl genrsa` |
| PKCS#8 | `1.2.840.113549.1.5.13` | 算法标识符 + 加密密钥 | `openssl genpkey -algorithm RSA` |

**Java 推荐 PKCS#8** 因为：
- `PKCS8EncodedKeySpec` 原生支持
- 更通用，支持多种算法（不只是 RSA）

**3. 从字节数组还原 RSAPrivateKey：**

```java
byte[] keyBytes = Base64.getDecoder().decode(keyData);
// 使用 PKCS#8 规范封装字节数组
PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
// 获取 RSA 算法的 KeyFactory
KeyFactory kf = KeyFactory.getInstance("RSA");
// 生成私钥对象
return (RSAPrivateKey) kf.generatePrivate(spec);
```

底层原理：
- `PKCS8EncodedKeySpec` 是 ASN.1 DER 编码的包装类
- `KeyFactory` 使用 SPI 机制调用 `sun.security.rsa.RSAKeyFactory`
- 解析 DER 结构，提取模数 n、私有指数 d 等参数

---

### 问题 5

**参考答案：**

**1. `@EnableConfigurationProperties` 的作用：**

- 启用配置属性绑定功能，将 `@ConfigurationProperties` 标注的类注册为 Spring Bean
- 不写这个注解的后果：
  - 如果 `AuthProperties` 上没有 `@Component`，则不会成为 Bean
  - `AuthConfiguration` 构造函数注入 `AuthProperties` 时会失败（NoSuchBeanDefinitionException）

**最佳实践：** 配置属性类通常不加 `@Component`，而是通过 `@EnableConfigurationProperties`显式启用，避免误扫描。

**2. 配置绑定的内部机制：**

```yaml
auth:
  jwt:
    issuer: zhiguang
    access-token-ttl: 15m
```

绑定过程：
1. Spring Boot 启动时，`ConfigurationPropertiesBindingPostProcessor` 扫描所有 `@ConfigurationProperties` 类
2. 从 Environment 中读取 `auth.*` 的所有属性
3. 使用 `Binder` API 将 kebab-case (`access-token-ttl`) 转为 camelCase (`accessTokenTtl`)
4. 通过 setter 方法或字段反射赋值
5. `Duration` 类型有特殊转换器，支持 `15m` → `Duration.ofMinutes(15)`

**3. `@RequiredArgsConstructor` 生成的构造函数：**

```java
// Lombok 生成
public AuthConfiguration(final AuthProperties properties) {
    this.properties = properties;
}
```

只初始化 `final` 字段和 `@NonNull` 字段，这里是 `properties`。这使得构造函数注入更简洁，无需手写 `@Autowired`。

---

### 问题 6

**参考答案：**

**1. `increment()` 的原子性：**

是的，`increment()` 是原子的。原因：
- Redis 是单线程处理命令（指命令执行阶段，不包括 I/O）
- `INCR` 命令在 Redis 内部是原子操作
- 即使是多个客户端并发调用，也不会出现竞态

**2. `increment()` + `expire()` 的并发问题：**

存在极端情况下的问题：
```
线程 A: increment() → 返回 1
线程 B: increment() → 返回 2
线程 A: expire(key, 1d)  // 设置过期
线程 B: expire(key, 1d)  // 重复设置，无害但冗余
```

但如果考虑**边界竞争**：
```
t=0: key 不存在
t=1: A increment() → 1
t=2: A expire() 之前发生上下文切换
t=3: B increment() → 2
t=4: A expire()  // 此时 count=2，但 A 认为是 1
```

虽然不影响正确性（最终都有过期时间），但更好的做法是使用 Lua 脚本：
```lua
local count = redis.call('INCR', KEYS[1])
if count == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[1])
end
return count
```

**3. Redis 其他原子操作：**
- `decr()` / `decrby()`：原子递减
- `append()`：原子追加字符串
- `hincrby()` / `hincrbyfloat()`：Hash 字段原子递增
- `sadd()` / `srem()`：Set 集合原子添加/删除
- `lpush()` / `rpop()`：List 原子入队/出队
- `setnx()`：原子设置（不存在时才设置）

---

### 问题 7

**参考答案：**

**1. `useGeneratedKeys`和`keyProperty` 的作用：**

- `useGeneratedKeys="true"`: 告诉 MyBatis 使用 JDBC 的 `getGeneratedKeys()` 方法获取数据库生成的主键（如 MySQL 的 AUTO_INCREMENT）
- `keyProperty="id"`: 将获取到的主键值赋给参数对象的 `id` 属性

执行流程：
```java
// MyBatis 底层执行
PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
stmt.executeUpdate();
ResultSet keys = stmt.getGeneratedKeys(); // 获取生成的主键
if (keys.next()) {
    user.setId(keys.getLong(1)); // 回填到 user.id
}
```

**2. MySQL 自增主键返回机制：**

MySQL 在执行 INSERT 后，会在连接级别保存最后插入的自增值，JDBC 驱动通过 `SHOW LAST_INSERT_ID()`或`getGeneratedKeys()` 获取。

**3. Oracle 序列的改写：**

```xml
<!-- Oracle 使用序列 -->
<insert id="insert" parameterType="com.tongji.user.domain.User">
    <selectKey keyProperty="id" resultType="long" order="BEFORE">
        SELECT user_seq.NEXTVAL FROM DUAL
    </selectKey>
    INSERT INTO users (id, phone, email, ...)
    VALUES (#{id}, #{phone}, #{email}, ...)
</insert>
```

- `<selectKey>`: 在主 SQL 执行前先查询序列
- `order="BEFORE"`: 先执行 selectKey，再执行 insert
- `keyProperty="id"`: 将序列值赋给 id

---

### 问题 8

**参考答案：**

**1. 默认事务传播和隔离级别：**

- **传播行为**：`Propagation.REQUIRED`（如果当前存在事务，则加入该事务；否则创建一个新事务）
- **隔离级别**：`Isolation.DEFAULT`（使用底层数据库的默认隔离级别，MySQL 是 REPEATABLE_READ）

**2. RuntimeException 抛出后的行为：**

- Spring 的 `TransactionInterceptor` 捕获到 RuntimeException
- 调用 `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()`
- 事务提交时发现是 rollbackOnly 状态，执行 ROLLBACK
- `userMapper.insert(user)` 的 SQL 被回滚，数据库中不会有这条记录

**注意：** 如果是 Checked Exception（非 RuntimeException），默认不会回滚，除非配置 `@Transactional(rollbackFor = Exception.class)`

**3. `readOnly = true` 时能否执行 insert？**

**能执行成功！** 原因：
- `readOnly = true` 只是一个**提示**，不是强制约束
- Spring 会将它传递给底层数据库连接（`Connection.setReadOnly(true)`）
- 但 MySQL 驱动通常忽略这个标志
- 真正阻止写操作的是数据库权限，而非 Spring

**最佳实践：** 查询方法加上 `readOnly = true` 可以优化性能（如 Hibernate 不会跟踪实体变化），但不要依赖它做数据完整性保护。

---

### 问题 9

**参考答案：**

**1. `ThrowUtils` 的设计模式：**

这是**Specification 模式**的简化版，也称为"断言工具类"。

```java
// 传统写法
if (!hasLetter || !hasDigit) {
    throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "密码必须同时包含字母和数字");
}

// ThrowUtils 写法
ThrowUtils.throwIf(!hasLetter || !hasDigit, ErrorCode.PASSWORD_POLICY_VIOLATION, "密码必须同时包含字母和数字");
```

**优势：**
- 减少样板代码，一行搞定校验 + 抛异常
- 统一异常构造逻辑，便于后续修改（如添加日志）
- 可读性更强："如果...就抛出..."

**2. `ErrorCode` 枚举应包含的字段：**

```java
public enum ErrorCode {
    SUCCESS(20000, "操作成功", HttpStatus.OK),
    PASSWORD_POLICY_VIOLATION(40001, "密码不符合安全策略", HttpStatus.BAD_REQUEST),
    IDENTIFIER_EXISTS(40002, "该标识已被注册", HttpStatus.BAD_REQUEST),
    VERIFICATION_DAILY_LIMIT(42901, "今日发送次数已达上限", HttpStatus.TOO_MANY_REQUESTS);
    
    private final Integer code;      // 业务错误码（便于前端国际化）
    private final String message;    // 默认提示信息
    private final HttpStatus httpStatus; // HTTP 状态码
}
```

**3. 全局异常处理器：**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return BaseResponse.error(errorCode.getCode(), errorCode.getMessage());
    }
}
```

返回统一格式：
```json
{
  "code": 40001,
  "message": "密码不符合安全策略",
  "data": null
}
```

---

### 问题 10

**参考答案：**

**1. `@AuthenticationPrincipal` 的工作原理：**

这是通过 `HandlerMethodArgumentResolver` 接口实现的：

```java
// AuthenticationPrincipalArgumentResolver 核心逻辑
@Override
public Object resolveArgument(MethodParameter parameter, ...) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
        return null;
    }
    // 如果参数类型是 Jwt，返回 authentication.getPrincipal()
    // 如果参数类型是 UserDetails，返回 authentication.getPrincipal()
    // 如果参数类型是 Long/Integer，尝试从 principal 中提取 uid
    return extractPrincipal(authentication, parameter.getParameterType());
}
```

注册时机：`SecurityConfigurationClassSetup` 在启动时将 resolver 添加到 `RequestMappingHandlerAdapter`

**2. JWT 解析时机：**

```
DispatcherServlet
    ↓
BearerTokenAuthenticationFilter (提取并验证 JWT)
    ↓ 成功后设置 SecurityContext
FilterSecurityInterceptor (校验授权)
    ↓
Controller (此时@AuthenticationPrincipal 才能拿到数据)
```

**关键点：** JWT 解析在 Filter 链的早期完成，Controller 执行时 `SecurityContext` 已经 populated。

**3. 自定义 `@RequireRole("ADMIN")` 的技术选型：**

**推荐方案：AOP + 自定义注解**

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String value();
}

@Aspect
@Component
public class RoleAspect {
    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority().equals("ROLE_" + requireRole.value()))) {
            throw new AccessDeniedException("需要 " + requireRole.value() + " 角色");
        }
    }
}
```

**为什么不选拦截器？** 拦截器无法方便地获取注解参数，且执行时机过早（Security Filter 之后，Controller 之前）。

---

### 问题 11

**参考答案：**

**1. 定义接口的原因：**

- **依赖倒置**：Controller/Service 依赖抽象接口，而非具体实现
- **可测试性**：单元测试时可以用 `InMemoryRefreshTokenStore` 替代 Redis 实现
- **可扩展性**：未来可以轻松添加缓存层、持久化层

```java
// 没有接口时
@Autowired
private RedisRefreshTokenStore store; // 强耦合 Redis

// 有接口时
@Autowired
private RefreshTokenStore store; // 可以是任何实现
```

**2. 支持 MySQL 持久化备份的设计：**

**方案一：装饰器模式**
```java
public class CachedRefreshTokenStore implements RefreshTokenStore {
    private final RefreshTokenStore redisDelegate;
    private final RefreshTokenStore mysqlDelegate;
    
    @Override
    public void storeToken(long userId, String tokenId, Duration ttl) {
        redisDelegate.storeToken(userId, tokenId, ttl);
        // 异步写入 MySQL 做备份
        CompletableFuture.runAsync(() -> 
            mysqlDelegate.storeToken(userId, tokenId, Duration.ofDays(30))
        );
    }
}
```

**方案二：组合模式**
```java
@Component
@Primary
public class CompositeRefreshTokenStore implements RefreshTokenStore {
    private final List<RefreshTokenStore> stores;
    
    @Override
    public void storeToken(...) {
        for (RefreshTokenStore store : stores) {
            store.storeToken(...)
        }
    }
}
```

**3. 体现的设计原则：**

**依赖倒置原则 (Dependency Inversion Principle, DIP)**：
- 高层模块（AuthService）不应依赖低层模块（RedisRefreshTokenStore），两者都应依赖抽象（RefreshTokenStore 接口）
- 面向接口编程，而非面向实现编程

---

### 问题 12

**参考答案：**

**1. `@RestController` vs `@Controller`：**

| 注解 | 返回值处理 | 典型使用场景 |
|------|------------|--------------|
| `@Controller` | 返回视图名称，配合 `ViewResolver` 渲染 HTML | Spring MVC 传统 Web 应用 |
| `@RestController` = `@Controller` + `@ResponseBody` | 返回值直接序列化为 JSON/XML | RESTful API |

项目是前后端分离架构，所以使用 `@RestController`。

**2. `@Validated` vs `@Valid`：**

| 对比 | `@Validated` (Spring) | `@Valid` (JSR-303) |
|------|----------------------|---------------------|
| 作用范围 | 类级别 + 方法参数 | 仅方法参数 |
| 分组校验 | 支持 `@Validated(Group.class)` | 不支持 |
| 嵌套校验 | 需要配合 `@Valid` | 自身支持 |

项目中：
- 类级别 `@Validated`：启用方法参数校验
- 参数级别`@Valid`：触发 RegisterRequest 字段的约束注解（如`@NotNull`）

**3. 校验失败抛出的异常：**

`MethodArgumentNotValidException`，包含详细的字段错误信息。

统一处理：
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError firstError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = firstError != null ? firstError.getDefaultMessage() : "参数校验失败";
        return BaseResponse.error(ErrorCode.PARAMS_ERROR.getCode(), message);
    }
}
```

---

### 问题 13

**参考答案：**

**1. 使用 Arthas 定位瓶颈：**

```bash
# 1. 查看方法调用耗时
trace com.tongji.auth.service.AuthService register --skipJDKMethod false

# 2. 监控 Redis 操作耗时
monitor com.tongji.nauy.auth.token.RedisRefreshTokenStore storeToken

# 3. 查看 BCrypt 加密耗时
trace org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encode

# 4. 查看 JVM 整体性能
dashboard  # CPU、内存、GC 情况
thread -n 3  # 最忙的前 3 个线程
```

**性能分析结论：**
- 如果 BCrypt 耗时 > 50ms：降低 strength 参数
- 如果 Redis 操作 > 30ms：检查网络延迟或 Redis 负载
- 如果 JWT 签发 > 10ms：RSA-2048 正常范围，可考虑升级到 RSA-3072 或切换到 EdDSA

**2. BCrypt strength 从 12 降到 10 的影响：**

| strength | 计算轮数 | 相对耗时 | 安全性 |
|----------|----------|----------|--------|
| 12 | 2^12 = 4096 轮 | 基准 | 推荐 |
| 10 | 2^10 = 1024 轮 | 约 1/4 | 仍可接受 |

**权衡建议：**
- 如果 QPS 翻倍导致 CPU 瓶颈，可以临时降到 10
- 长期方案：将密码哈希移到异步队列（注册时不立即返回，但这会影响用户体验）

**3. Redis 瓶颈优化手段：**

1. **连接池优化**：增加 `maxTotal`、`maxIdle`，减少连接获取等待
2. **Pipeline/Batching**：多个 Redis 命令批量发送（但项目中使用 Spring Data Redis，需要手动封装 Pipeline）
3. **本地缓存**：热点数据（如频繁验证的 Refresh Token）用 Caffeine 缓存，减少 Redis 访问
4. **Redis Cluster**：分片扩容，将 `auth:rt:*` 分散到多个节点
5. **读写分离**：验证 Token 走从节点，写入走主节点

---

### 问题 14

**参考答案：**

**1. 暴力破解验证码的防御：**

**当前防护：**
- 速率限制：60 秒内只能发送一次（`sendInterval`）
- 日限额：每天最多 10 次（`dailyLimit`）

**能否防御 1000 次/秒？**
- 不能！因为限流是针对**发送验证码**，而不是**尝试验证码**
- 攻击者可以先正常获取验证码，然后用脚本快速尝试不同验证码组合

**加强方案：**
```java
// 验证码尝试次数限制
public VerificationCheckResult verify(...) {
    int attempts = verificationCodeStore.incrementAttempts(scene, identifier);
    if (attempts > MAX_ATTEMPTS) {
        // 锁定该标识，1 小时内不允许再尝试
        verificationCodeStore.lockIdentifier(identifier, Duration.ofHours(1));
        throw new BusinessException(ErrorCode.VERIFICATION_TOO_MANY_ATTEMPTS);
    }
    // ...
}
```

**2. 重放攻击防御：**

**当前状态：** Access Token 在 15 分钟内可重复使用，这是 JWT 的固有特性。

**防御方案：**
- **短 TTL**：将 Access Token 降到 5 分钟
- **一次性 Token**：服务端缓存已使用的 Access Token 的 jti，拒绝重复使用（但这破坏了 JWT 的 Statelessness 优势）
- **绑定客户端指纹**：签发时将 IP+UA 的哈希写入 Token，每次请求校验一致性

```java
// 签发时
String clientFingerprint = hash(ip + ua);
claims.claim("fingerprint", clientFingerprint);

// 校验时
String storedFingerprint = jwt.getClaims().get("fingerprint").toString();
String currentFingerprint = hash(currentIp + currentUa);
if (!storedFingerprint.equals(currentFingerprint)) {
    throw new TokenInvalidException("Token 可能被劫持");
}
```

**3. JWT 算法混淆攻击（HS256 vs RS256）：**

**攻击原理：**
1. 攻击者获取公钥（公开的）
2. 将 JWT Header 的 `alg` 从 `RS256` 改为 `HS256`
3. 使用公钥内容作为 HMAC 密钥签名
4. 如果服务端没有强制校验算法，会用公钥做 HMAC 验签，恰好能通过

**Spring Security 是否免疫？**

**是的，默认免疫。** 原因：
- `NimbusJwtDecoder` 在构造时明确指定使用 `SignatureAlgorithm.RS256`
- 解码时会检查 Header 中的 `alg` 是否为 RS256，不匹配则抛出 `JwtException`

```java
// NimbusJwtDecoder 内部
if (!SignatureAlgorithm.RS256.equals(jwsHeader.getAlgorithm())) {
    throw new JwtException("Invalid algorithm: " + jwsHeader.getAlgorithm());
}
```

**最佳实践：** 显式指定 JWS 类型选择器
```java
JwtDecoders.fromIssuerLocation(issuer)
    .jwsTypeSelector(JwsHeader::getAlgorithm) // 强制校验 alg
    .build();
```

---

### 问题 15

**参考答案：**

**1. 分布式环境下的竞态条件：**

**场景描述：**
```yaml
verification:
  daily-limit: 10
```

假设有两个服务实例 A 和 B：
```
t=0: 用户已发送 9 次，Redis count=9
t=1: 请求到达实例 A，A 检查 count=9 < 10 ✓
t=2: 请求同时到达实例 B，B 检查 count=9 < 10 ✓
t=3: A 执行 increment() → count=10，允许发送
t=4: B 执行 increment() → count=11，也允许发送（超限！）
```

**根本原因：** "检查限额 → increment" 不是原子操作，存在 Check-Then-Act 竞态。

**2. 使用 Lua 脚本保证原子性：**

```lua
-- 按键：KEYS[1] = auth:code:count:{scene}:{identifier}:{yyyyMMdd}
-- 阈值：ARGV[1] = dailyLimit

local current = redis.call('GET', KEYS[1])
if current == false then
    current = 0
else
    current = tonumber(current)
end

if current >= tonumber(ARGV[1]) then
    return -1  -- 超过限额
end

-- 原子递增并设置过期时间
local newCount = redis.call('INCR', KEYS[1])
if newCount == 1 then
    redis.call('EXPIRE', KEYS[1], 86400)  -- 1 天
end

return newCount
```

Java 调用：
```java
DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
Long result = redisTemplate.execute(script, Collections.singletonList(key), String.valueOf(dailyLimit));
if (result == -1) {
    throw new BusinessException(ErrorCode.VERIFICATION_DAILY_LIMIT);
}
```

**3. 全局 1 分钟限流的 Key 设计：**

```
auth:code:rate:{scene}:{identifier}
```

使用 Redis 的**滑动窗口限流**：
```lua
-- 使用 ZSET 实现滑动窗口
local key = "auth:code:rate:" .. scene .. ":" .. identifier
local now = redis.call('TIME')
local windowStart = now[1] - 60  -- 60 秒窗口

-- 移除窗口外的记录
redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)

-- 统计窗口内请求数
local count = redis.call('ZCARD', key)
if count >= limit then
    return 0  -- 拒绝
end

-- 添加当前请求
redis.call('ZADD', key, now[1], now[1] .. math.random())
redis.call('EXPIRE', key, 60)

return 1  -- 允许
```

---

## 总结

本习题集覆盖了知光项目第二阶段（用户认证系统）涉及的核心知识点：

| 领域 | 题目数 | 难度分布 |
|------|--------|----------|
| Spring Security | 2 题 | 中级、高级 |
| JWT/令牌安全 | 2 题 | 高级、高级 |
| Redis | 3 题 | 中级、高级、高级 |
| 密码学 | 1 题 | 中级 |
| Spring Boot | 1 题 | 初级 |
| 并发编程 | 1 题 | 高级 |
| MyBatis | 1 题 | 中级 |
| 数据库事务 | 1 题 | 中级 |
| 异常处理 | 1 题 | 初级 |
| Spring AOP | 1 题 | 高级 |
| 设计模式 | 1 题 | 中级 |
| HTTP/REST | 1 题 | 初级 |
| JVM 调优 | 1 题 | 高级 |
| 安全攻防 | 1 题 | 高级 |
| 分布式系统 | 1 题 | 高级 |

**建议学习路径：**
1. 先掌握初级题目（Spring Boot、异常处理、HTTP）
2. 再攻克中级题目（MyBatis、事务、Redis 基础、密码学）
3. 最后挑战高级题目（分布式、安全攻防、JVM 调优）
