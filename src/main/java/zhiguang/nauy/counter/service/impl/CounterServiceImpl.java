package zhiguang.nauy.counter.service.impl;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import zhiguang.nauy.counter.event.CounterEvent;
import zhiguang.nauy.counter.event.CounterEventProducer;
import zhiguang.nauy.counter.schema.BitmapShard;
import zhiguang.nauy.counter.schema.CounterKeys;
import zhiguang.nauy.counter.schema.CounterSchema;
import zhiguang.nauy.counter.schema.SdsUtils;
import zhiguang.nauy.counter.service.CounterService;
import zhiguang.nauy.exception.ErrorCode;
import zhiguang.nauy.exception.ThrowUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 内容实体计数服务实现。
 *
 * <p>实现点赞、收藏、查询计数等核心逻辑。</p>
 * <p>包含：</p>
 * <ul>
 *   <li>Fact层：位图原子操作（Lua脚本）</li>
 *   <li>Event层：发布计数事件到Kafka</li>
 *   <li>Summary层：SDS读写（大端32位）</li>
 *   <li>重建机制：分布式锁+限流+指数退避</li>
 * </ul>
 */
@Service
@Slf4j
public class CounterServiceImpl implements CounterService {
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> toggleScript;
    private final CounterEventProducer eventProducer;
    private final ApplicationEventPublisher eventPublisher;
    private final RedissonClient redisson;

    public CounterServiceImpl(StringRedisTemplate redis, CounterEventProducer eventProducer, ApplicationEventPublisher eventPublisher, RedissonClient redisson) {
        this.redis = redis;
        this.eventProducer = eventProducer;
        this.eventPublisher = eventPublisher;
        this.redisson = redisson;
        this.toggleScript = new DefaultRedisScript<>();
        this.toggleScript.setResultType(Long.class);
        // 位图状态原子切换，仅在状态变化时返回 1
        this.toggleScript.setScriptText(TOGGLE_LUA);
    }

    @Value("${counter.rebuild.lock.ttl-ms:5000}")
    private long lockTtlMs;
    @Value("${counter.rebuild.rate.permits:3}")
    private int ratePermits;
    @Value("${counter.rebuild.rate.window-seconds:10}")
    private int rateWindowSeconds;
    @Value("${counter.rebuild.backoff.base-ms:500}")
    private long backoffBaseMs;
    @Value("${counter.rebuild.backoff.max-ms:30000}")
    private long backoffMaxMs;

    /**
     * redis 位图 ，点赞
     *
     * @param entityType
     * @param entityId
     * @param userId
     * @return
     */
    @Override
    public boolean like(String entityType, String entityId, long userId) {

        return toggle(entityType, entityId, userId, "like", CounterSchema.IDX_LIKE, true);
    }

    /**
     * 取消点赞
     *
     * @param entityType
     * @param entityId
     * @param userId
     * @return
     */
    @Override
    public boolean unlike(String entityType, String entityId, long userId) {

        return toggle(entityType, entityId, userId, "like", CounterSchema.IDX_LIKE, false);

    }

    /**
     * 收藏
     *
     * @param entityType
     * @param entityId
     * @param userId
     * @return
     */
    @Override
    public boolean fav(String entityType, String entityId, long userId) {
        return toggle(entityType, entityId, userId, "fav", CounterSchema.IDX_FAV, true);
    }

    /**
     * 取消收藏
     *
     * @param entityType
     * @param entityId
     * @param userId
     * @return
     */
    @Override
    public boolean unfav(String entityType, String entityId, long userId) {
        return toggle(entityType, entityId, userId, "fav", CounterSchema.IDX_FAV, false);
    }

    /**
     * 获取实体计数汇总（SDS）。
     * <p>从 Summary 层读取指定指标的计数值。若 SDS 结构缺失或长度异常，则标记为需要重建（当前实现中重建逻辑待完善）。</p>
     *
     * @param entityType 实体类型（如 "knowpost"）
     * @param entityId   实体ID（如帖子ID "123"）
     * @param metrics    需要查询的指标名称列表（如 ["like", "fav"]）
     * @return 指标名称到计数值的映射（如 {"like": 100, "fav": 50}）
     */
    @Override
    public Map<String, Long> getCounts(String entityType, String entityId, List<String> metrics) {
//        0.校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(entityType) || StrUtil.isBlank(entityId), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(metrics == null || metrics.isEmpty(), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(metrics.size() > CounterSchema.SCHEMA_LEN, ErrorCode.PARAMS_ERROR);
        //1.生成SDS 键 并且读取
        String sdsKey = CounterKeys.sdsKey(entityType, entityId);
        byte[] raw = SdsUtils.getRaw(redis,sdsKey);
        //2.检查是否需要重建
        //字节长度
        int expectedLen = CounterSchema.SCHEMA_LEN * CounterSchema.BYTES_PER_METRIC;
        //3.判断是否需要重建
        boolean needRebuild = (raw == null || raw.length != expectedLen);
        LinkedHashMap<String, Long> result = new LinkedHashMap<>();

        if (needRebuild) {
            log.info("计数结构不存在，需要重建");
            //3.1 获取分布式锁
            String lockKey = String.format("lock:sds-rebuild:%s:%s", entityType, entityId);
            RLock lock = redisson.getLock(lockKey);
            boolean locked = false;

            try {
                //尝试获取锁，最多等待5秒
                //使用redisson看门狗机制 ；不指定租期 自动续约

                locked = lock.tryLock(lockTtlMs, TimeUnit.MILLISECONDS);
                if (!locked) {
                    //增加退避级别并设置下次允许尝试的时间
                    escalateBackoff(entityType, entityId);
                    //没有获取到锁
                    log.warn("获取重建锁失败，放弃重建");
                    return result;
                }
                //3.2 限流检查
                if (!allowedByRateLimiter(entityType, entityId)) {
//                    超出限流阈值
                    log.warn("超出限流阈值，放弃重建");
                    return result;

                }
//                3.3 退避检查
                if (inBackoff(entityType, entityId)) {
                    log.warn("处于退避状态，放弃重建");
                    return result;
                }
                //3.4双重检查
                byte[] recheckRaw = SdsUtils.getRaw(redis,sdsKey);
                if (recheckRaw != null && recheckRaw.length == expectedLen) {
                    log.info("双重检查：SDS已被其他线程重建，直接使用");
                    raw = recheckRaw; // 用新的raw
                } else {
                    //3.5 尝试重建
                    rebuildFromBitmap(entityType, entityId, sdsKey, expectedLen);
                    //重建后再获取
                    raw = SdsUtils.getRaw(redis,sdsKey);
                }
                resetBackoff(entityType, entityId);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if (locked) {
                    lock.unlock();
                }
            }

        }
        //不需要重建
        for (String metric : metrics) {
            //遍历 “like” “fav”
            //"like" ->1
            //"fav"->2
            Integer index = CounterSchema.NAME_TO_IDX.get(metric);
            if (index != null && raw != null) {
                int offset = index * CounterSchema.BYTES_PER_METRIC;
                // like: 1 * 4 = 4（偏移量4）
                // fav:  2 * 4 = 8（偏移量8）
                //从raw 的第offset开始读取4个字节 转化为int32
                long value = SdsUtils.readInt32BE(raw, offset);

                result.put(metric, value);
            } else {
                result.put(metric, 0L);
            }
        }


        return result;
    }

    private void resetBackoff(String entityType, String entityId) {
        //重置退避
        //        eKey (Exp Key): 存储当前的退避指数（即已经重试/失败了多少次）。
        String eKey = String.format("backoff:sds-rebuild:exp:%s:%s", entityType, entityId);
//uKey (Until Key): 存储退避截止时间（毫秒时间戳），在这个时间点之前禁止重试。
        String uKey = String.format("backoff:sds-rebuild:until:%s:%s", entityType, entityId);
        redisson.getBucket(eKey).delete();
        redisson.getBucket(uKey).delete();
    }

    /**
     * 从位图分片重建 SDS 计数结构。
     * <p>当 Summary 层数据缺失或损坏时，
     * 通过扫描 Fact 层的所有位图分片，统计各指标的真实计数值，并回写到 SDS 中。</p>
     *
     * @param entityType  实体类型（如 "knowpost"）
     * @param entityId    实体ID（如帖子ID "123"）
     * @param sdsKey      SDS 计数器的 Redis Key
     * @param expectedLen SDS 结构的预期字节长度
     */
    private void rebuildFromBitmap(String entityType, String entityId, String sdsKey, int expectedLen) {
        log.info("开始重建计数：entityType={}, entityId={}", entityType, entityId);
        //1.统计所有指标
        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        for (String metric : CounterSchema.SUPPORTED_METRICS) {
            long count = bitCountShardsPipelined(metric, entityType, entityId);
            counts.put(metric, count);
        }
        //2.构建新的SDS
        byte[] newSds = new byte[expectedLen];
        counts.entrySet().forEach(entry -> {
            Integer idx = CounterSchema.NAME_TO_IDX.get(entry.getKey());
            if (idx != null) {
                int offset = idx * CounterSchema.BYTES_PER_METRIC;
                SdsUtils.writeInt32BE(newSds, offset, entry.getValue());
            }

        });
        //3.写入Redis
        SdsUtils.setRaw(redis,sdsKey, newSds);
        log.info("重建完成：entityType={}, entityId={}", entityType, entityId);

    }

    /**
     * * 管道化统计所有分片位图置位总数（和参考项目逻辑一致）
     * * 用 KEYS 命令找所有存在的分片，批量 BITCOUNT 提升性能
     *
     * @param metric     计数指标名称（如 "like"、"fav"）
     * @param entityType 实体类型（如 "knowpost"）
     * @param entityId   实体ID（如帖子ID "123"）
     * @return 该指标在所有分片中的总计数值
     */
    private long bitCountShardsPipelined(String metric, String entityType, String entityId) {
        //根据 metric 获取对应的分片 Key -》统计置位总数-》返回结果
        // 1. 构建KEYS匹配模式，找所有分片（比如 bm:like:knowpost:123:*）
        String pattern = String.format("bm:%s:%s:%s:*", metric, entityType, entityId);
        // 是模糊匹配，根据你提供的通配符模式，找出 Redis 中所有符合要求的 Key。
        Set<String> keys = redis.keys(pattern); // 返回所有存在的分片键 //全表扫描 o(N)

        if (keys.isEmpty() || keys == null) {
            return 0L;
        }
        //2.管道批量执行Bitcount (1次网络往返 替代循环32次)
        List<Object> result = redis.executePipelined(new RedisCallback<Object>() {

            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                for (String key : keys) {
                    connection.stringCommands().bitCount(key.getBytes(StandardCharsets.UTF_8));
                }
                return null;
            }
        });
        //3.累加所有分片的计数
        long sum = 0;
        for (Object o : result) {
            if (o instanceof Number) {
                sum += ((Number) o).longValue();
            }
        }
        return sum;
    }

    /**
     * 增加退避级别并设置下次允许尝试的时间（指数递增，封顶）。
     */
    private void escalateBackoff(String entityType, String entityId) {
        //1.准备redis key
//        eKey (Exp Key): 存储当前的退避指数（即已经重试/失败了多少次）。
        String eKey = String.format("backoff:sds-rebuild:exp:%s:%s", entityType, entityId);
//uKey (Until Key): 存储退避截止时间（毫秒时间戳），在这个时间点之前禁止重试。
        String uKey = String.format("backoff:sds-rebuild:until:%s:%s", entityType, entityId);
//        2.获取分布式对象 (RBucket)
        RBucket<Integer> expB = redisson.getBucket(eKey);
        RBucket<Long> untilB = redisson.getBucket(uKey);
        // 读取当前实体已经累积的退避指数。如果是第一次失败，这里会是 null。
        Integer exp = expB.get();
        //3.计算下一个退避指数 最大不超过10
        int nextExp = Math.min(exp == null ? 0 : exp + 1, 10);
        // 4.基础延迟 × 2的指数次方
        long delay = Math.min(backoffBaseMs * (1L << nextExp), backoffMaxMs);
//        5. 计算截止时间 (until)
        long until = System.currentTimeMillis() + delay;


        //6. 设置过期时间
        // 设置过期时间，避免长时间残留
        expB.set(nextExp);
//        expB.set(nextExp): 更新退避指数。注意这里没有设置过期时间，
//        因为它会随着 untilB 的清理而被间接管理，或者在成功重建时被显式删除

//        为什么加 1000ms? 为了确保在退避期结束后，这个 Key 还能多存活 1 秒，
//        方便调试或防止极端情况下的竞态条件，之后 Redis 会自动删除它，避免垃圾数据堆积。
        untilB.set(until, Duration.ofMillis(delay + 1000));

    }

    /**
     * 是否处于指数退避期：期间跳过重建并返回降级结果。
     */
    private boolean inBackoff(String entityType, String entityId) {
        String bKey = String.format("backoff:sds-rebuild:until:%s:%s", entityType, entityId);
        //获取限流bucket
        RBucket<Long> bucket = redisson.getBucket(bKey);
        //获取限流bucket的值
        Long until = bucket.get();
        //判断是否处于退避期
//         如果还没到重试时间，返回
        return until != null && System.currentTimeMillis() < until;
    }

    /**
     * 限流判断：单位窗口可重建次数，防止抖动与风暴。
     * <p>使用 Redisson 的 RateLimiter 实现分布式限流，确保同一实体在指定时间窗口内最多触发指定次数的重建。</p>
     *
     * @param entityType 实体类型（如 "knowpost"）
     * @param entityId   实体ID（如帖子ID "123"）
     * @return true=允许重建，false=超出限流阈值
     */
    private boolean allowedByRateLimiter(String entityType, String entityId) {
        //生成限流key
        String lockKey = String.format("rl:sds-rebuild:%s:%s", entityType, entityId);

        // 获取 Redisson 速率限制器
        RRateLimiter limiter = redisson.getRateLimiter(lockKey);

        //初始化速率限制器 （如果不存在则设置）
//        例如 10秒内最多允许3次
        limiter.trySetRate(RateType.OVERALL, ratePermits, Duration.ofSeconds(rateWindowSeconds));
        // 尝试获取一个许可
        return limiter.tryAcquire(1);
    }

    /**
     * 批量获取实体计数汇总（SDS）。
     * <p>利用 Redis 管道（Pipeline）一次性读取多个实体的 SDS 二进制数据，并在内存中解析，显著降低网络 RTT。</p>
     *
     * @param entityType 实体类型（如 "knowpost"）
     * @param entityIds  实体ID列表（如 ["123", "456"]）
     * @param metrics    需要查询的指标名称列表（如 ["like", "fav"]）
     * @return 嵌套映射：外层 Key 为 entityId，内层 Key 为 metric，Value 为计数值
     */
    @Override
    public Map<String, Map<String, Long>> getCountsBatch(String entityType, List<String> entityIds, List<String> metrics) {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        if (entityIds == null || entityIds.isEmpty()) {
            return result;
        }
        //1.准备所有实体的sds key
        List<String> sdsKeys = entityIds.stream().map(entityId -> CounterKeys.sdsKey(entityType, entityId)).toList();

        //2.批量获取所有实体的sds数据
        List<Object> rawList = redis.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : sdsKeys) {
                connection.stringCommands().get(key.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });
        // 3. 解析每个实体的 SDS 数据
        int expectedLen = CounterSchema.SCHEMA_LEN*CounterSchema.BYTES_PER_METRIC;

        for (int i = 0; i < entityIds.size(); i++) {
            Map<String, Long> counts = new LinkedHashMap<>(); // 放在循环体里面 每一个实体 对应一个map

            String entityId = entityIds.get(i);
            //获取实体的 SDS 数据
            Object rawObj = (i < rawList.size()) ? rawList.get(i) : null;
            byte[] raw = (rawObj instanceof byte[]) ? (byte[]) rawObj : null;

             // 如果数据长度符合预期，则复用 readInt32BE 进行解析
             if (raw!=null&&raw.length==expectedLen){
                 for (String metric : metrics) {
                     Integer idx = CounterSchema.NAME_TO_IDX.get(metric);
                     if (idx != null) {
                         int offset = idx * CounterSchema.BYTES_PER_METRIC;
                         // 使用 SdsUtils 解析方法
                         counts.put(metric, SdsUtils.readInt32BE(raw, offset));
                     }
                 }
            }else {
                //数据损坏或者丢失返回0
                for (String metric : metrics)
                    counts.put(metric, 0L);
            }
            result.put(entityId, counts);

        }
        return result;
    }


    @Override
    public boolean isLiked(String entityType, String entityId, long userId) {
        return checkBitmapState(entityType, entityId, userId, "like");
    }



    @Override
    public boolean isFaved(String entityType, String entityId, long userId) {
        return checkBitmapState(entityType, entityId, userId, "fav");
    }
    /**
     * 检查用户在位图中的状态。
     *
     * @param entityType 实体类型（如 "knowpost"）
     * @param entityId   实体ID（如帖子ID "123"）
     * @param userId     用户ID
     * @param metric     指标名称（如 "like"、"fav"）
     * @return true 表示用户已执行该操作，false 表示未执行
     */
    private boolean checkBitmapState(String entityType, String entityId, long userId, String metric) {

        // 0. 校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(entityType) || StrUtil.isBlank(entityId), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!CounterSchema.SUPPORTED_METRICS.contains(metric), ErrorCode.PARAMS_ERROR);

        //1.计算分片位置
        long chunk = BitmapShard.chunkOf(userId);
        long bit = BitmapShard.bitOf(userId);
        String bitmapKey = CounterKeys.bitmapKey(metric, entityType, entityId, chunk);

        //从bitmapkey 中获取bit 判断是否为1 -> 返回true
        Boolean aBoolean = redis.opsForValue().getBit(bitmapKey, bit);
        return Boolean.TRUE.equals(aBoolean);
    }
    /**
     * 实际处理 点赞和取消点赞的函数
     *
     * @param entityType 实体类型（如 "knowpost"）
     * @param entityId   实体ID（如帖子ID "123"）
     * @param userId     用户ID
     * @param metric     计数指标名称（如 "like"、"fav"）
     * @param metricIdx  指标在 SDS 结构中的索引位置（如 CounterSchema.IDX_LIKE）
     * @param add        操作方向：true=置位（点赞/收藏），false=清位（取消）
     * @return 是否发生状态变化（true 表示本次操作生效）
     */

    private boolean toggle(String entityType, String entityId, long userId, String metric, int metricIdx, boolean add) {
        //0.校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(entityType) || StrUtil.isBlank(entityId), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!CounterSchema.SUPPORTED_METRICS.contains(metric), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(metricIdx < 0 || metricIdx >= CounterSchema.SCHEMA_LEN, ErrorCode.PARAMS_ERROR);
        //1.计算分片位置
        long chunk = BitmapShard.chunkOf(userId);
        long bit = BitmapShard.bitOf(userId);
        //2.生成键名
        // bm:like:knowpost:123:3
        String bitmapKey = CounterKeys.bitmapKey(metric, entityType, entityId, chunk);
        // 3.生成参数
        List<String> keys = List.of(bitmapKey);
        List<String> args = List.of(String.valueOf(bit), add ? "add" : "remove");
        //4.执行lua脚本 （原子操作）
        //不管在固定时间内该用户点了多少下 点赞都视为一次点赞信息
        Long result = redis.execute(toggleScript, keys, args.toArray(new String[0]));
        //5.发布事件
        boolean ok = result == 1L;
        if (ok) {
            //点赞还是取消
            int delta = add ? 1 : -1;
            //发布事件到kafka （跨进程持久化）
            eventProducer.publish(CounterEvent.of(entityType, entityId, metric, metricIdx, userId, delta));
            //发布到spring本地
            eventPublisher.publishEvent(CounterEvent.of(entityType, entityId, metric, metricIdx, userId, delta));

        }
        return ok;
    }



    // Redis 内嵌 Lua（Redis 5/6 的 Lua 5.1），位图原子切换（分片内偏移）
    private static final String TOGGLE_LUA = """
        local bmKey = KEYS[1]
        local offset = tonumber(ARGV[1])
        local op = ARGV[2] -- 'add' or 'remove'
        local prev = redis.call('GETBIT', bmKey, offset)
        if op == 'add' then
          if prev == 1 then return 0 end
          redis.call('SETBIT', bmKey, offset, 1)
          return 1
        elseif op == 'remove' then
          if prev == 0 then return 0 end
          redis.call('SETBIT', bmKey, offset, 0)
          return 1
        end
        return -1
        """;


}
