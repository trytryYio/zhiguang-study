package zhiguang.nauy.relation.service.impl;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zhiguang.nauy.counter.service.UserCounterService;
import zhiguang.nauy.exception.ErrorCode;
import zhiguang.nauy.exception.ThrowUtils;
import zhiguang.nauy.outbox.mapper.OutboxMapper;
import zhiguang.nauy.profile.dto.ProfileResponse;
import zhiguang.nauy.relation.event.RelationEvent;
import zhiguang.nauy.relation.mapper.RelationMapper;
import zhiguang.nauy.relation.service.RelationService;
import zhiguang.nauy.user.domain.User;
import zhiguang.nauy.user.service.UserService;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntFunction;
import java.util.stream.Collectors;


/**
 * @Description: // 类说明，在创建类时要填写
 * @ClassName: RelationServiceImpl    // 类名，会自动填充
 * @Author: oyy         // 创建者
 * @Date: 2026/4/28 11:22   // 时间
 * @Version: 1.0     // 版本
 */
@Service

public class RelationServiceImpl implements RelationService {
    @Lazy
    @Resource
    private UserCounterService userCounterService;
    @Resource
    private UserService userService;

    @Resource
    private OutboxMapper outboxMapper;
    private final DefaultRedisScript<Long> tokenScript;
    @Resource
    private RelationMapper mapper;
    @Resource
    private StringRedisTemplate redis;
    private final Cache<Long, List<Long>> flwsTopCache;

    public RelationServiceImpl(Cache<Long, List<Long>> flwsTopCache) {
        this.flwsTopCache = flwsTopCache;
        this.tokenScript = new DefaultRedisScript<>();
        this.tokenScript.setResultType(Long.class);
        this.tokenScript.setScriptText(TOKEN_BUCKET_LUA);
    }

    /**
     * 发起关注。
     *
     * @param fromUserId
     * @param toUserId
     * @return
     */
    @Override
    @Transactional
    public Boolean follow(long fromUserId, long toUserId) {
//        1.先限流 redis 执行 lua脚本 实现限流功能
        Long ok = redis.execute(tokenScript, List.of("rl:follow:" + fromUserId), "100", "1");
        if (ok == 0L) {
            return false;
        }

        // 2.写入数据库
        //每个线程自己拿自己的随机数生成器，生成一个 0 ~ Long.MAX_VALUE 之间的随机 long
        long id = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
//
        int inserted = mapper.insertFollowing(id, fromUserId, toUserId, 1);
        if (inserted > 0) {
            //数据库写入成功
            try {
                // 主表写成功后，写入 outbox 事件，供后续异步更新粉丝表、缓存和计数
                Long outId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
                String payload = JSONUtil.toJsonStr(new RelationEvent("FollowCreated", toUserId, fromUserId, id));

                outboxMapper.insert(outId, "following", id, "FollowCreated", payload);
            } catch (Exception ignored) {
                ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "数据库写入失败");
            }

            return true;
        }
        return false;
    }

    /**
     * 取消关注
     *
     * @param uid
     * @param toUserId
     * @return
     */
    @Override
    @Transactional
    public Boolean unfollow(long uid, long toUserId) {
//  1.：取消 following
        int updated = mapper.cancelFollowing(uid, toUserId);
//  2.取消成功
        if (updated > 0) {
//            取消
            try {
                Long outId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
                String payload = JSONUtil.toJsonStr(
                    new RelationEvent("FollowCanceled", toUserId, uid, null)
                );
                outboxMapper.insert(outId, "following", null, "FollowCanceled", payload);
            } catch (Exception ignored) {
                ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "数据库写入失败");

            }
            return true;
        }
        return false;
    }

    @Override

    public Map<String, Boolean> relationStatus(long uid, long toUserId) {

//我有没有关注他
        boolean following = isFollowing(uid, toUserId);
//        他有没有回关我
        boolean followedBy = isFollowing(toUserId, uid);
        //互相关注
        boolean mutual = following && followedBy;
        Map<String, Boolean> m = new LinkedHashMap<>();
        m.put("following", following);
        m.put("followedBy", followedBy);
        m.put("mutual", mutual);
        return m;
    }

    /**
     * 获取关注列表，支持偏移或游标分页。
     *
     * @param userId 用户ID
     * @param limit  返回数量上限
     * @param offset 偏移量（当 cursor 为空时生效）
     * @param cursor 游标（毫秒时间戳）
     * @return 关注用户ID列表
     */
    @Override

    public List<ProfileResponse> followingProfiles(long userId, int limit, int offset, Long cursor) {
//        0.校验参数
        ThrowUtils.throwIf(userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能小于0");
        ThrowUtils.throwIf(limit < 0, ErrorCode.PARAMS_ERROR, "limit不能小于0");
        ThrowUtils.throwIf(offset < 0, ErrorCode.PARAMS_ERROR, "offset不能小于0");
        ThrowUtils.throwIf(cursor == null || cursor < 0, ErrorCode.PARAMS_ERROR, "cursor不能小于0");
//        1.获取关注列表
//        1.1 根据userID 来查询这个用户的关注列表 --follower
        List<Long> ids = cursor != null ? followingCursor(userId, limit, cursor)//游标分页
            : following(userId, limit, offset);//偏移量分页
        return toProfiles(ids);
    }
    /**
     * 获取粉丝列表的用户资料。
     * <p>支持偏移量分页和游标分页两种模式：</p>
     * <ul>
     *   <li>当 cursor 为 null 时，使用 offset 进行偏移量分页</li>
     *   <li>当 cursor 不为 null 时，使用 cursor 进行游标分页（基于时间戳）</li>
     * </ul>
     *
     * @param userId 目标用户ID，查询该用户的粉丝列表
     * @param limit  返回数量上限
     * @param offset 偏移量（仅在 cursor 为 null 时生效）
     * @param cursor 游标（毫秒时间戳，用于深度分页优化）
     * @return 粉丝用户的资料响应列表
     */

    @Override
    public List<ProfileResponse> followersProfiles(long userId, int limit, int offset, Long cursor) {
        //        0.校验参数
        ThrowUtils.throwIf(userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能小于0");
        ThrowUtils.throwIf(limit < 0, ErrorCode.PARAMS_ERROR, "limit不能小于0");
        ThrowUtils.throwIf(offset < 0, ErrorCode.PARAMS_ERROR, "offset不能小于0");
        ThrowUtils.throwIf(cursor == null || cursor < 0, ErrorCode.PARAMS_ERROR, "cursor不能小于0");
//        1.获取关注列表
//        1.1 根据userID 来查询这个用户的关注列表 --follower
        List<Long> ids = cursor != null ?
            followersCursor(userId, limit, cursor)
            : followers(userId, limit, offset);
        return toProfiles(ids);
    }

    /**
     * SDS 由 5 个 4 字节段组成（关注/粉丝/发文/获赞/获藏）
     *
     * @param userId
     * @return
     */
    @Override
    public Map<String, Long> counter(long userId) {
//        1.从redis读取用户计数字符串 SDS 键 ucnt :{userId}
        //stringCommands 获取字符串
        //
        byte[] raw = redis.execute((RedisCallback<byte[]>)
            c -> c.stringCommands().get(("ucnt:" + userId).getBytes(StandardCharsets.UTF_8)));
//        拼接计数结果
        Map<String, Long> m = new LinkedHashMap<>();

//        2.    缺失或结构异常 (少于 5段 * 每段4字节 时)尝试重建
        if (raw == null || raw.length < 20) {
            try {
                userCounterService.rebuildAllCounters(userId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
//            重建后二次读取
            raw = redis.execute((RedisCallback<byte[]>)
                c -> c.stringCommands().get(("ucnt:" + userId).getBytes(StandardCharsets.UTF_8)));
            //任然失败则返回0 ,保证接口可用
            if (raw == null || raw.length < 20) {

                m.put("following", 0L);
                m.put("followers", 0L);
                m.put("posts", 0L);
                m.put("likes", 0L);
                m.put("favors", 0L);

                return m;
            }
        }

//        3.解码二进制数据（大端序，每4字节一个字段）
        final byte[] buf = raw;
        // 段数（每段 4 字节，按大端 32 位整型编码）

        final int seg = buf.length / 4;
//        读取第idx 段 的计数 (1基座标 )大端拼接为long
        IntFunction<Long> read = idx -> {
            if (idx > seg || idx <= 1) return 0L;
            int off = (idx - 1) * 4;
            long n = 0;
            for (int i = 0; i < 4; i++) {

                n = (n << 8) | (buf[off + i] & 0xFFL);
            }
            return n;
        };
        long sdsFollowings = read.apply(1);
        long sdsFollowers = read.apply(2);

        // 4. 采样校验：每300秒对比一次数据库（限流控制）
        // 采样校验：使用 Redis 锁限流，每用户 300s 触发一次
        String chkKey = "ucnt:chk:" + userId;
        Boolean doCheck = redis.opsForValue().setIfAbsent(chkKey, "1", java.time.Duration.ofSeconds(300));

        if (Boolean.TRUE.equals(doCheck)) {
            int dbFollowings = 0;
            int dbFollowers = 0;

            // 仅校验关注/粉丝的有效关系计数，与 SDS 值对比
            try {
                dbFollowings = mapper.countFollowingActive(userId);
            } catch (Exception ignored) {
            }
            try {
                dbFollowers = mapper.countFollowerActive(userId);
            } catch (Exception ignored) {
            }

            // 段数异常或值不一致则触发全量重建
            if ((seg != 5) || sdsFollowings != (long) dbFollowings || sdsFollowers != (long) dbFollowers) {
                try {
                    userCounterService.rebuildAllCounters(userId);
                } catch (Exception ignored) {
                }

                // 重建后读取并直接返回最新值
                byte[] raw2 = redis.execute((RedisCallback<byte[]>)
                    c -> c.stringCommands().get(("ucnt:" + userId).getBytes(StandardCharsets.UTF_8)));
                if (raw2 != null && raw2.length >= 20) {
                    final byte[] buf2 = raw2;
                    // 二次读取函数：同样按大端 32 位读取
                    IntFunction<Long> r2 = idx -> {
                        int off = (idx - 1) * 4;
                        long n = 0;
                        for (int i = 0; i < 4; i++) {
                            n = (n << 8) | (buf2[off + i] & 0xFFL);
                        }
                        return n;
                    };
                    m.put("followings", r2.apply(1));
                    m.put("followers", r2.apply(2));
                    m.put("posts", r2.apply(3));
                    m.put("likedPosts", r2.apply(4));
                    m.put("favedPosts", r2.apply(5));
                    return m;
                }
            }
        }

        // 正常路径：直接返回 SDS 中的计数值
        m.put("followings", sdsFollowings);
        m.put("followers", sdsFollowers);
        m.put("posts", read.apply(3));
        m.put("likedPosts", read.apply(4));
        m.put("favedPosts", read.apply(5));
        return m;
    }

    @Override
    public long followingCount(long userId) {
        return mapper.countFollowingActive(userId);
    }

    @Override
    public long followersCount(long userId) {
        return mapper.countFollowerActive(userId);
    }

    /**
     * 获取粉丝列表，支持偏移
     *
     * @param userId
     * @param limit
     * @param offset
     * @return
     */
    private List<Long> followers(long userId, int limit, int offset) {

        List<Long> followers = mapper.followers(userId, limit, offset);
        return followers;
    }

    /**
     * 获取粉丝列表，支持游标
     *
     * @param userId
     * @param limit
     * @param cursor
     * @return
     */
    private List<Long> followersCursor(long userId, int limit, Long cursor) {
        String key = "uf:fans:" + userId;
        return getListWithCursor(
            key,
            limit,
            cursor,
            need -> mapper.listFollowerRows(userId, need, 0),
            "fromUserId",
            "createdAt"
        );

    }

    /**
     * 通过偏移量获取关注列表
     *
     * @param userId
     * @param limit
     * @param offset
     * @return
     */
    private List<Long> following(long userId, int limit, int offset) {
        String key = "uf:flws:" + userId;

        return getListWithOffset(
            key,
            offset,
            limit,
            need -> mapper.listFollowingRows(userId, limit, 0),
            "toUserId",
            "createdAt",
            flwsTopCache,
            userId
        );

    }

    /**
     * 偏移分页读取：优先命中 ZSet，未命中时从 DB 回填并设置 TTL；大V用户维护本地 Top 缓存以降低冷启动开销。
     *
     * @param key          缓存键
     * @param offset       偏移量
     * @param limit        返回数量
     * @param rowsFetcher  行数据获取函数
     * @param idField      ID字段
     * @param tsField      时间戳字段
     * @param flwsTopCache 缓存
     * @param userId       用户ID
     * @return
     */
    private List<Long> getListWithOffset(
        String key, int offset,
        int limit,
        IntFunction<Map<Long, Map<String, Object>>> rowsFetcher,
        String idField,
        String tsField
        , Cache<Long, List<Long>> flwsTopCache,
        long userId) {
//        三级缓存

//        caffeine
        List<Long> top = flwsTopCache != null ? flwsTopCache.getIfPresent(userId) : null;
        if (top != null && !top.isEmpty()) {
            // 本地缓存通常只存 Top N (例如前500)，如果 offset 在范围内则直接返回
            if (offset < top.size()) {
                int to = Math.min(offset + limit, top.size());
                return new ArrayList<>(top.subList(offset, to));
            }
            // 如果请求的 offset 超过了本地缓存范围，继续查 Redis
        }

//        redis
        Set<String> cached = redis.opsForZSet().reverseRange(key, offset, offset + limit - 1L);
        if (cached != null && !cached.isEmpty()) {
            return toLongList(cached);
        }
//        数据库
        int need = Math.max(1, limit + offset);
        Map<Long, Map<String, Object>> rows = rowsFetcher.apply(Math.min(need, 1000));
        if (rows != null && !rows.isEmpty()) {
            //更新redis
            fillZSet(key, rows, idField, tsField, null);
            redis.expire(key, Duration.ofHours(2));

            // 回填后尝试更新本地缓存（仅针对大V）
            if (flwsTopCache != null && isBigV(userId)) {
                maybeUpdateTopCache(userId, key, flwsTopCache);
            }

            Set<String> filled = redis.opsForZSet().reverseRange(key, offset, offset + limit - 1L);
            return filled == null ? Collections.emptyList() : toLongList(filled);
        }
        return Collections.emptyList();
    }

    /**
     * 可能更新本地缓存
     *
     * @param userId
     * @param key
     * @param flwsTopCache
     */
    private void maybeUpdateTopCache(long userId, String key, Cache<Long, List<Long>> flwsTopCache) {
        // 取前 500 名
        Set<String> allSet = redis.opsForZSet().reverseRange(key, 0, 499);
        if (allSet == null || allSet.isEmpty()) return;
        List<Long> all = new ArrayList<>(allSet.size());
        for (String s : allSet) {
            all.add(Long.parseLong(s));
        }
        // 缓存前 500 名到 Caffeine
        flwsTopCache.put(userId, all);
    }

    /**
     * 如果是大v
     *
     * @param userId
     * @return
     */
    private boolean isBigV(long userId) {

//        redis.execute(...): 让 Java 程序直接命令 Redis 数据库执行一个操作。
//        c.stringCommands().get(...): 具体执行的是“读取字符串”操作。
//        "ucnt:" + userId: 这是 Redis 里的“钥匙”（Key）。
//        比如用户 ID 是 10086，钥匙就是 ucnt:10086。通常这里存的是该用户的粉丝总数或关注总数。
//.getBytes(...): 因为网络传输需要二进制，所以把钥匙字符串转换成字节数组。
//        byte[] raw = ...: 把 Redis 返回的结果（也就是那个数量值）存到 raw 变量里。
//        目前它是字节格式，后续通常需要转换成数字（如 Integer 或 Long）来进行比较
//        （例如：如果粉丝数 > 10万，则是大V）。
        byte[] raw = redis.execute((RedisCallback<byte[]>) c ->
            c.stringCommands().get(("ucnt:" + userId).getBytes(StandardCharsets.UTF_8)));
        if (raw == null || raw.length < 20) return false;
        long n = 0;
        // 第 2 段（followers 计数）
        int off = 2 * 4;
        for (int i = 0; i < 4; i++) {
            n = (n << 8) | (raw[off + i] & 0xFFL);
        }
        return n >= 500_000L;  // 粉丝 ≥ 50 万
    }

    /**
     * 批量转换成 ProfileResponse
     *
     * @param ids
     * @return
     */
    private List<ProfileResponse> toProfiles(List<Long> ids) {
/*
* 方案一
*   List<User> users = userService.listByIds(ids);
        List<ProfileResponse> collect = users.stream().map(User::convertToProfileResponse).collect(Collectors.toList());

        return collect;
*
* 顺序通常由数据库决定，可能与 ids 传入顺序不一致
* */
//方案二
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 批量查询用户 (建议使用 userService 以利用其内部的缓存或逻辑)
        List<User> users = userService.listByIds(ids);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 用 LinkedHashMap 保持顺序并快速查找
        Map<Long, User> userMap = new LinkedHashMap<>(users.size());
        for (User u : users) {
            userMap.put(u.getId(), u);
        }

        // 3. 按原始 ID 顺序映射为 ProfileResponse
        List<ProfileResponse> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            User u = userMap.get(id);
            if (u != null) {
                // 假设 User 类里有 convertToProfileResponse 方法，或者手动构造
                result.add(u.convertToProfileResponse());
            }
        }
        return result;
    }


    /**
     * 通过游标获取关注列表
     *
     * @param userId
     * @param limit
     * @param cursor
     * @return
     */
    private List<Long> followingCursor(long userId, int limit, Long cursor) {
        String key = "uf:flws:" + userId;
        return getListWithCursor(
            key, limit, cursor,
            //**延迟加载** — 只有 Redis 没命中时才查数据库
            need -> mapper.listFollowingRows(userId, need, 0),
            "toUserId", "createdAt"
        );
    }

    /**
     * 获取关注列表，支持偏移或游标分页。
     * 游标分页核心
     *
     * @param key         缓存键
     * @param limit       返回数量上限
     * @param cursor      游标（毫秒时间戳）
     * @param rowsFetcher 行数据获取函数
     * @param idField     ID字段名
     * @param tsField     时间戳字段名
     * @return
     */
    private List<Long> getListWithCursor(String key,
                                         int limit,
                                         Long cursor,
                                         IntFunction<Map<Long, Map<String, Object>>> rowsFetcher,
                                         String idField,
                                         String tsField) {

        // 1. 确定游标上限：如果未提供游标，则设为正无穷（即从头开始）；否则使用提供的游标值
        double maxScore = (cursor == null) ? Double.POSITIVE_INFINITY : cursor.doubleValue();

        // 2. 尝试从 Redis ZSet 缓存中读取数据
        // 使用 reverseRangeByScore 按分数倒序获取 [负无穷, maxScore] 范围内的前 limit 个元素
        Set<String> cachedIds = redis.opsForZSet().reverseRangeByScore(key, Double.NEGATIVE_INFINITY, maxScore, 0, limit);

        // 3. 如果缓存命中且不为空，直接转换并返回结果
        if (cachedIds != null && !cachedIds.isEmpty()) {
            return toLongList(cachedIds);
        }

        // 4. 缓存未命中，需要从数据库加载数据以填充缓存
        // 计算需要加载的数据量：至少加载 limit 条，但为了减少频繁回源，最少加载 100 条
        int loadLimit = Math.max(limit, 100);
        // 限制单次最大加载量为 1000，防止一次性加载过多数据
        Map<Long, Map<String, Object>> dbRows = rowsFetcher.apply(Math.min(loadLimit, 1000));
//            456L: {                    // ← 外层 Key：toUserId 的值
//        "toUserId": 456,       // ← 内层 Map：列名 → 值
//        "createdAt": 1714387200000
//    },

        // 5. 如果数据库中有数据
        if (dbRows != null && !dbRows.isEmpty()) {
            // 将数据库查询结果回填到 Redis ZSet 中  redis  也有数据了
            fillZSet(key, dbRows, idField, tsField, cursor);
            // 设置缓存过期时间为 2 小时
            redis.expire(key, Duration.ofHours(2));

            // 6. 回填后，再次从 Redis 中读取所需范围的数据
            Set<String> refreshedIds = redis.opsForZSet().reverseRangeByScore(key, Double.NEGATIVE_INFINITY, maxScore, 0, limit);
            // 返回转换后的列表，如果为空则返回空列表
            return (refreshedIds == null) ? Collections.emptyList() : toLongList(refreshedIds);
        }

        // 7. 如果数据库中也没有数据，返回空列表
        return Collections.emptyList();
    }

    /**
     * 将数据库查询结果回填到 Redis ZSet 中
     *
     * @param key
     * @param dbRows
     * @param idField
     * @param tsField
     * @param cursor
     */
    private void fillZSet(String key, Map<Long, Map<String, Object>> dbRows, String idField, String tsField, Long cursor) {
        for (Map<String, Object> r : dbRows.values()) {
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

    /**
     * 多类型时间戳转换
     *
     * @param tsObj
     * @return
     */
    private long tsScore(Object tsObj) {
        if (tsObj instanceof Timestamp ts) {
            return ts.getTime();           // java.sql.Timestamp → 毫秒
        }
        if (tsObj instanceof Date d) {
            return d.getTime();            // java.util.Date → 毫秒
        }
        return System.currentTimeMillis(); // 兜底：当前时间
    }

    /**
     * 缓存结果转换成 Long 列表
     *
     * @param cachedIds
     * @return
     */
    private List<Long> toLongList(Set<String> cachedIds) {
        return cachedIds.stream().map(Long::parseLong).collect(Collectors.toList());
    }

    private boolean isFollowing(long fromUserId, long toUserId) {
        return mapper.existsFollowing(fromUserId, toUserId);

    }

    /**
     * 令牌桶限流 lua脚本
     */
    /**
     * java 的伪代码和上面等效的
     * public boolean tryConsumeFollowToken(long fromUserId) {
     * String key = "rl:follow:" + fromUserId;
     * int capacity = 100; // 桶最大容量
     * int rate = 1;       // 每秒恢复 1 个令牌
     * long now = getRedisServerTimeInSeconds();
     * // 从 Redis 里拿当前桶状态
     * BucketState state = redis.getBucketState(key);
     * // state.last: 上次更新时间
     * // state.tokens: 当前令牌数
     * // 第一次访问，直接初始化为满桶
     * if (state == null) {
     * state = new BucketState(now, capacity);
     * }
     * // 先按时间恢复令牌
     * long elapsed = now - state.last;
     * double recoveredTokens = elapsed * rate;
     * state.tokens = Math.min(capacity, state.tokens + recoveredTokens);
     * // 如果令牌不够，就拒绝 follow
     * if (state.tokens < 1) {
     * state.last = now;
     * redis.saveBucketState(key, state);
     * redis.expire(key, 60_000);
     * return false;
     * }
     * // 令牌够，扣掉 1 个
     * state.tokens = state.tokens - 1;
     * state.last = now;
     * // 回写 Redis
     * redis.saveBucketState(key, state);
     * redis.expire(key, 60_000);
     * return true;
     **/
    private static final String TOKEN_BUCKET_LUA = """

        local key = KEYS[1]
        local capacity = tonumber(ARGV[1])
        local rate = tonumber(ARGV[2])
        local now = redis.call('TIME')[1]
        local last = redis.call('HGET', key, 'last')
        local tokens = redis.call('HGET', key, 'tokens')
        if not last then last = now; tokens = capacity end
        local elapsed = tonumber(now) - tonumber(last)
        local add = elapsed * rate
        tokens = math.min(capacity, tonumber(tokens) + add)
        if tokens < 1 then redis.call('HSET', key, 'last', now); redis.call('HSET', key, 'tokens', tokens); return 0 end
        tokens = tokens - 1
        redis.call('HSET', key, 'last', now)
        redis.call('HSET', key, 'tokens', tokens)
        redis.call('PEXPIRE', key, 60000)
        return 1
        """;


}
