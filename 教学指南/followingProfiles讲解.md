# followingProfiles 方法级讲解

> 从 Controller 入口到 Redis ZSet 查询，逐层拆解每个方法，重点讲解少见/高级用法。

---

## 完整调用链路图

```
HTTP GET /api/v1/relation/following?userId=123&limit=20&cursor=1714000000000
        │
        ▼
RelationController.following()                    ← 入口，参数校验
        │
        ▼
RelationServiceImpl.followingProfiles()           ← 路由：游标 vs 偏移
        │
        ├─ cursor != null ──→ followingCursor()   ← 游标分页
        │                         │
        │                         ▼
        │                    getListWithCursor()   ← 核心：Redis ZSet 游标查询
        │                         │
        │                         ├─ Redis 命中 → 直接返回
        │                         └─ Redis 未命中 → DB 回填 → 再查 Redis
        │
        └─ cursor == null ──→ following()          ← 偏移分页
                                  │
                                  ▼
                             getListWithOffset()   ← 核心：Redis ZSet 偏移查询
                                  │
                                  ├─ L1 Caffeine 本地缓存（大V用户）
                                  ├─ L2 Redis ZSet
                                  └─ L3 DB 回填
        │
        ▼
toProfiles(ids)                                   ← ID → 用户资料映射
        │
        ├─ userMapper.listByIds()                 ← 批量查 MySQL
        └─ LinkedHashMap 保持顺序 → ProfileResponse
```

---

## 第一层：Controller 入口

### `RelationController.following()`

```java
@GetMapping("/following")
public List<ProfileResponse> following(
        @RequestParam("userId") long userId,
        @RequestParam(value = "limit", defaultValue = "20") int limit,
        @RequestParam(value = "offset", defaultValue = "0") int offset,
        @RequestParam(value = "cursor", required = false) Long cursor) {
    int l = Math.min(Math.max(limit, 1), 100);   // 限制 [1, 100]
    return relationService.followingProfiles(userId, l, Math.max(offset, 0), cursor);
}
```

**做了什么：**
- 接收 HTTP 请求参数
- 对 `limit` 做边界裁剪：最小 1，最大 100（防止恶意请求拉全量）
- `cursor` 是 `required = false`，没传就是 `null`

**少见点：** `@RequestParam(value = "cursor", required = false) Long cursor`
- 用 `Long`（包装类）而不是 `long`（基本类型），因为 `required = false` 时可能为 `null`
- 如果用 `long`，没传参会直接报 400 错误

---

## 第二层：路由分发

### `RelationServiceImpl.followingProfiles()`

```java
@Override
public List<ProfileResponse> followingProfiles(long userId, int limit, int offset, Long cursor) {
    List<Long> ids = cursor != null ? followingCursor(userId, limit, cursor)
                                    : following(userId, limit, offset);
    return toProfiles(ids);
}
```

**做了什么：**
- 根据 `cursor` 是否为空，决定走 **游标分页** 还是 **偏移分页**
- 拿到 ID 列表后，统一转换为用户资料

**设计思路：** 一个方法同时支持两种分页模式，前端不需要两个接口。

---

## 第三层：两种分页方式

### 方式 A：游标分页 — `followingCursor()`

```java
@Override
public List<Long> followingCursor(long userId, int limit, Long cursor) {
    String key = "uf:flws:" + userId;          // Redis key
    return getListWithCursor(
            key, limit, cursor,
            need -> mapper.listFollowingRows(userId, need, 0),  // DB 回填函数
            "toUserId", "createdAt"
    );
}
```

**做了什么：**
- 构造 Redis key：`uf:flws:{userId}`（User Following Set）
- 传入一个 **lambda** 作为 DB 回填函数（延迟加载，Redis 没命中才执行）

**少见点：** `IntFunction<Map<Long, Map<String, Object>>>` 参数类型
- `IntFunction` 是函数式接口：接受 `int`，返回 `T`
- 这里传入 `need -> mapper.listFollowingRows(userId, need, 0)`
- 作用：**延迟加载** — 只有 Redis 没命中时才查数据库
- 如果直接传查询结果，每次都会查 DB，浪费性能

---

### 方式 B：偏移分页 — `following()`

```java
@Override
public List<Long> following(long userId, int limit, int offset) {
    String key = "uf:flws:" + userId;
    return getListWithOffset(
            key, offset, limit,
            need -> mapper.listFollowingRows(userId, need, 0),
            "toUserId", "createdAt",
            flwsTopCache,     // Caffeine 本地缓存（大V用）
            userId
    );
}
```

**与游标分页的区别：**
| | 游标分页 | 偏移分页 |
|---|---|---|
| 参数 | `cursor`（时间戳） | `offset`（数字） |
| 适合 | 无限滚动（加载更多） | 传统分页（第1、2、3页） |
| 性能 | 稳定（不受数据量影响） | 大 offset 时慢 |
| Redis 命令 | `ZREVRANGEBYSCORE` | `ZREVRANGE` |

---

## 第四层：核心方法详解

### `getListWithCursor()` — 游标分页核心

```java
private List<Long> getListWithCursor(String key,
                                     int limit,
                                     Long cursor,
                                     IntFunction<Map<Long, Map<String, Object>>> rowsFetcher,
                                     String idField,
                                     String tsField) {

    // 1. 计算游标上界
    double max = cursor == null ? Double.POSITIVE_INFINITY : cursor.doubleValue();

    // 2. 从 Redis ZSet 查询
    Set<String> cached = redis.opsForZSet()
            .reverseRangeByScore(key, Double.NEGATIVE_INFINITY, max, 0, limit);

    // 3. Redis 命中 → 直接返回
    if (cached != null && !cached.isEmpty()) {
        return toLongList(cached);
    }

    // 4. Redis 未命中 → 从 DB 回填
    int need = Math.max(limit, 100);
    Map<Long, Map<String, Object>> rows = rowsFetcher.apply(Math.min(need, 1000));

    // 5. 写入 Redis ZSet
    if (rows != null && !rows.isEmpty()) {
        fillZSet(key, rows, idField, tsField, cursor);
        redis.expire(key, Duration.ofHours(2));   // 设置 2 小时 TTL

        // 6. 回填后再查一次 Redis
        Set<String> filled = redis.opsForZSet()
                .reverseRangeByScore(key, Double.NEGATIVE_INFINITY, max, 0, limit);
        return filled == null ? Collections.emptyList() : toLongList(filled);
    }
    return Collections.emptyList();
}
```

**逐步拆解：**

#### ① `Double.POSITIVE_INFINITY` / `Double.NEGATIVE_INFINITY`
```java
double max = cursor == null ? Double.POSITIVE_INFINITY : cursor.doubleValue();
```
- **第一页**：`cursor = null` → `max = +∞` → 取所有数据（按时间倒序，最新的在前）
- **后续页**：`max = 上一页最小 score` → 只取比它更新的数据
- 这是 **游标分页的标准模式**：用 `±∞` 作为边界哨兵

#### ② `reverseRangeByScore` — 核心 Redis 命令
```java
redis.opsForZSet().reverseRangeByScore(key, Double.NEGATIVE_INFINITY, max, 0, limit)
```
等价于 Redis 命令：
```
ZREVRANGEBYSCORE key +∞ max LIMIT 0 limit
```
- `reverse` = 降序（score 大的在前，即最新的在前）
- 范围 `(-∞, max]` = 取所有 score ≤ max 的元素
- `LIMIT 0 limit` = 跳过 0 条，取 limit 条

**为什么用 `Double`？** ZSet 的 score 是 `double` 类型，毫秒时间戳（如 `1714000000000`）可以直接作为 score。

#### ③ `rowsFetcher.apply(need)` — 延迟加载
```java
int need = Math.max(limit, 100);  // 至少查 100 条
Map<Long, Map<String, Object>> rows = rowsFetcher.apply(Math.min(need, 1000));
```
- `rowsFetcher` 是 lambda：`need -> mapper.listFollowingRows(userId, need, 0)`
- 只有 Redis 没命中时才执行（延迟加载）
- 最多查 1000 条（防止 DB 压力过大）

#### ④ `fillZSet()` — 写入 Redis
```java
fillZSet(key, rows, idField, tsField, cursor);
```
将 DB 查出的数据写入 Redis ZSet，下次直接从 Redis 读。

---

### `getListWithOffset()` — 偏移分页核心（三级缓存）

```java
private List<Long> getListWithOffset(
        String key, int offset, int limit,
        IntFunction<Map<Long, Map<String, Object>>> rowsFetcher,
        String idField, String tsField,
        Cache<Long, List<Long>> localCache,
        long userId) {

    // L1: Caffeine 本地缓存（仅大V用户）
    List<Long> top = localCache != null ? localCache.getIfPresent(userId) : null;
    if (top != null && !top.isEmpty()) {
        if (offset < top.size()) {              // offset 在本地缓存范围内
            int to = Math.min(offset + limit, top.size());
            return new ArrayList<>(top.subList(offset, to));
        }
        // offset 超出本地缓存 → 继续查 Redis
    }

    // L2: Redis ZSet
    Set<String> cached = redis.opsForZSet().reverseRange(key, offset, offset + limit - 1L);
    if (cached != null && !cached.isEmpty()) {
        return toLongList(cached);
    }

    // L3: DB 回填
    int need = Math.max(1, limit + offset);
    Map<Long, Map<String, Object>> rows = rowsFetcher.apply(Math.min(need, 1000));
    if (rows != null && !rows.isEmpty()) {
        fillZSet(key, rows, idField, tsField, null);
        redis.expire(key, Duration.ofHours(2));

        // 大V用户：更新本地缓存
        if (localCache != null && isBigV(userId)) {
            maybeUpdateTopCache(userId, key, localCache);
        }

        Set<String> filled = redis.opsForZSet().reverseRange(key, offset, offset + limit - 1L);
        return filled == null ? Collections.emptyList() : toLongList(filled);
    }
    return Collections.emptyList();
}
```

**少见点 ①：Caffeine 本地缓存**
```java
Cache<Long, List<Long>> localCache  // L1 缓存
```
- **Caffeine** 是 Java 本地缓存库（类似 Guava Cache，但性能更好）
- 这里只对 **大V 用户**（粉丝 ≥ 50 万）启用
- 大V 的关注列表被访问频率极高，本地缓存可以拦截 80% 的 Redis 请求

**少见点 ②：`isBigV()` 判断**
```java
private boolean isBigV(long userId) {
    byte[] raw = redis.execute((RedisCallback<byte[]>) c ->
        c.stringCommands().get(("ucnt:" + userId).getBytes(StandardCharsets.UTF_8)));
    if (raw == null || raw.length < 20) return false;
    long n = 0;
    int off = 2 * 4;  // 第 2 段（followers 计数）
    for (int i = 0; i < 4; i++) {
        n = (n << 8) | (raw[off + i] & 0xFFL);
    }
    return n >= 500_000L;  // 粉丝 ≥ 50 万
}
```
- 用户计数存在 Redis 的 **SDS（Simple Dynamic String）** 结构中
- SDS 是 **二进制编码**：5 个 4 字节段（关注/粉丝/发文/获赞/获藏），大端序
- 这里手动解析第 2 段（followers），判断是否为大V

---

## 第五层：ID → 用户资料映射

### `toProfiles()`

```java
private List<ProfileResponse> toProfiles(List<Long> ids) {
    if (ids == null || ids.isEmpty()) return List.of();

    // 1. 批量查询用户
    List<User> users = userMapper.listByIds(ids);

    // 2. 用 LinkedHashMap 保持顺序
    Map<Long, User> m = new LinkedHashMap<>(users.size());
    for (User u : users) m.put(u.getId(), u);

    // 3. 按原始 ID 顺序映射为 ProfileResponse
    List<ProfileResponse> out = new ArrayList<>(ids.size());
    for (Long id : ids) {
        User u = m.get(id);
        if (u == null) continue;  // 用户已注销，跳过
        out.add(new ProfileResponse(u.getId(), u.getNickname(), u.getAvatar(),
                u.getBio(), u.getZgId(), u.getGender(), u.getBirthday(),
                u.getSchool(), u.getPhone(), u.getEmail(), u.getTagsJson()));
    }
    return out;
}
```

**少见点 ①：`LinkedHashMap` 保持插入顺序**
```java
Map<Long, User> m = new LinkedHashMap<>(users.size());
```
- `HashMap` 不保证顺序
- `LinkedHashMap` 按插入顺序迭代
- 这样遍历 `ids` 时，输出顺序与 Redis 查询结果一致

**少见点 ②：批量查询而非逐个查询**
```java
List<User> users = userMapper.listByIds(ids);
```
- 如果用 `findById(id)` 逐个查，N 个用户需要 N 次 DB 查询
- 用 `listByIds` 只需要 **1 次** DB 查询（`WHERE id IN (...)`）
- 性能差距：20 个用户 = 20 次查询 vs 1 次查询

**少见点 ③：`List.of()` 返回不可变空集合**
```java
if (ids == null || ids.isEmpty()) return List.of();
```
- `List.of()` 是 Java 9+ 的工厂方法，返回不可变空集合
- 比 `Collections.emptyList()` 更简洁
- 不可变：调用方不能 add/remove，避免 NPE

---

## 第六层：辅助方法

### `fillZSet()` — Redis ZSet 回填

```java
private void fillZSet(String key,
                      Map<Long, Map<String, Object>> rows,
                      String idField,
                      String tsField,
                      Long cursor) {
    for (Map<String, Object> r : rows.values()) {
        Object idObj = r.get(idField);      // 取用户 ID
        Object tsObj = r.get(tsField);      // 取时间戳
        if (idObj == null || tsObj == null) continue;

        long score = tsScore(tsObj);        // 转为毫秒时间戳

        // 如果有游标，只填充 score ≤ cursor 的记录
        if (cursor == null || score <= cursor) {
            redis.opsForZSet().add(key, String.valueOf(idObj), score);
        }
    }
}
```

**做了什么：**
- 遍历 DB 查出的每一行，写入 Redis ZSet
- `score` = 关注时间的毫秒时间戳
- 如果是游标分页的回填，只写入 `score ≤ cursor` 的记录（避免写入太新的数据干扰分页）

---

### `tsScore()` — 多类型时间戳转换

```java
private long tsScore(Object tsObj) {
    if (tsObj instanceof Timestamp ts) {
        return ts.getTime();           // java.sql.Timestamp → 毫秒
    }
    if (tsObj instanceof Date d) {
        return d.getTime();            // java.util.Date → 毫秒
    }
    return System.currentTimeMillis(); // 兜底：当前时间
}
```

**少见点：`instanceof` 模式匹配（Java 16+）**
```java
if (tsObj instanceof Timestamp ts)  // 直接绑定变量，无需强转
```
- 传统写法：`if (tsObj instanceof Timestamp) { Timestamp ts = (Timestamp) tsObj; ... }`
- Java 16+ 写法：`if (tsObj instanceof Timestamp ts) { ... }` — 更简洁

---

### `toLongList()` — Set → List 类型转换

```java
private List<Long> toLongList(Set<String> set) {
    List<Long> out = new ArrayList<>(set.size());
    for (String s : set) out.add(Long.valueOf(s));
    return out;
}
```

**为什么需要这个？**
- Redis ZSet 返回 `Set<String>`（Redis 一切都是字符串）
- 业务需要 `List<Long>`（有序 + 数字类型）
- 所以需要手动转换

---

### `maybeUpdateTopCache()` — 大V 本地缓存更新

```java
private void maybeUpdateTopCache(long userId, String key, Cache<Long, List<Long>> cache) {
    Set<String> allSet = redis.opsForZSet().reverseRange(key, 0, 499);  // 取前 500 名
    if (allSet == null || allSet.isEmpty()) return;
    List<Long> all = new ArrayList<>(allSet.size());
    for (String s : allSet) all.add(Long.valueOf(s));
    cache.put(userId, all);  // 缓存前 500 名到 Caffeine
}
```

**设计思路：**
- 大V 用户可能有几十万粉丝，但前端通常只显示前几百个
- 把前 500 名缓存到本地（Caffeine），后续请求直接从内存读
- Caffeine 配置：`maximumSize(1000)` + `expireAfterWrite(10分钟)`

---

## 第七层：Mapper 层

### `RelationMapper.listFollowingRows()`

```java
@MapKey("toUserId")
Map<Long, Map<String, Object>> listFollowingRows(
        @Param("fromUserId") Long fromUserId,
        @Param("limit") int limit,
        @Param("offset") int offset);
```

对应 XML：
```xml
<select id="listFollowingRows" resultType="map">
    SELECT to_user_id AS toUserId, created_at AS createdAt
    FROM following
    WHERE from_user_id=#{fromUserId} AND rel_status=1
    ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}
</select>
```

**少见点 ①：`@MapKey` 注解**
```java
@MapKey("toUserId")
Map<Long, Map<String, Object>> listFollowingRows(...)
```
- 普通 `resultType="map"` 返回 `List<Map<String, Object>>`
- 加了 `@MapKey("toUserId")` 后，返回 `Map<Long, Map<String, Object>>`
- 即：**以 `toUserId` 为 key 的二级 Map**
- 好处：可以直接用 `rows.get(userId)` 快速查找，不需要遍历

**少见点 ②：双表设计**
- `following` 表：存"我关注了谁"（from_user_id → to_user_id）
- `follower` 表：存"谁关注了我"（to_user_id → from_user_id）
- 两张表数据冗余，但查询效率高（不需要 JOIN）

---

## 关键数据结构总结

### Redis ZSet 存储结构
```
Key:   uf:flws:{userId}          # User Following Set
Field: 用户ID（字符串）            # member
Score: 关注时间的毫秒时间戳        # score
```

示例：
```
uf:flws:123
├── "456" → 1714000000000    # 2024-04-25 关注用户 456
├── "789" → 1714100000000    # 2024-04-26 关注用户 789
└── "012" → 1714200000000    # 2024-04-27 关注用户 012
```

### 三级缓存架构
```
L1: Caffeine（本地内存）  → 仅大V用户，前 500 名，10 分钟过期
L2: Redis ZSet            → 所有用户，2 小时 TTL
L3: MySQL                 → 永久存储，按需回填
```

---

## 常见面试追问

### Q1: 为什么用游标分页而不是 OFFSET 分页？
**A:** OFFSET 分页在深分页时（如第 1000 页）需要跳过大量数据，性能差。游标分页用 `score ≤ cursor` 做范围查询，无论第几页性能都稳定。

### Q2: Redis ZSet 没命中怎么办？
**A:** 从 DB 查出数据 → 写入 Redis ZSet → 设置 2 小时 TTL → 再从 Redis 查。下次请求直接命中 Redis。

### Q3: 大V 用户怎么优化？
**A:** 用 Caffeine 本地缓存前 500 名，拦截 80% 的 Redis 请求。本地缓存 10 分钟过期，保证数据不会太旧。

### Q4: `@MapKey` 和普通 `List<Map>` 有什么区别？
**A:** `@MapKey` 把 `List<Map>` 转成 `Map<K, Map>`，用指定字段做 key，查找时 O(1) 而不是 O(n)。

### Q5: 为什么用双表设计？
**A:** 查询"我关注了谁"和"谁关注了我"是两个高频场景。单表需要 JOIN 或两次索引查询，双表各自维护，查询更快。
