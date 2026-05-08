package zhiguang.nauy.counter.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import zhiguang.nauy.counter.dto.UserCounterDTO;
import zhiguang.nauy.counter.schema.CounterSchema;
import zhiguang.nauy.counter.schema.SdsUtils;
import zhiguang.nauy.counter.schema.UserCounterKeys;
import zhiguang.nauy.counter.schema.UserCounterSchema;
import zhiguang.nauy.counter.service.CounterService;
import zhiguang.nauy.counter.service.UserCounterService;
import zhiguang.nauy.knowpost.service.KnowPostsService;
import zhiguang.nauy.relation.service.RelationService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户维度计数服务实现。
 *
 * <p>实现用户维度的计数操作。</p>
 * <p>包含：</p>
 * <ul>
 *   <li>关注数、粉丝数、发帖数</li>
 *   <li>获赞数、获收藏数</li>
 *   <li>从数据库重建计数</li>
 * </ul>
 */
@Service
@Slf4j
public class UserCounterServiceImpl implements UserCounterService {
    @Resource
    private RelationService relationService;
    @Resource
    private CounterService counterService;
    @Resource
    private KnowPostsService knowPostsService;

    @Autowired
    private StringRedisTemplate redis;

    @Resource
    private RedissonClient redisson;

    // 用户计数SDS结构长度
    int expectedLen = CounterSchema.SCHEMA_LEN * CounterSchema.BYTES_PER_METRIC;

    /**
     * 增加用户关注数。
     *
     * @param userId 用户ID
     * @param delta  增量
     */
    @Override
    public void incrementFollowings(long userId, int delta) {
        incrementUserCounterField(userId, UserCounterSchema.IDX_FOLLOWINGS, delta);
    }

    /**
     * 增加用户粉丝数。
     *
     * @param userId 用户ID
     * @param delta  增量
     */
    @Override
    public void incrementFollowers(long userId, int delta) {
        incrementUserCounterField(userId, UserCounterSchema.IDX_FOLLOWERS, delta);
    }
    
    /**
     * 增加用户发帖数。
     *
     * @param userId 用户ID
     * @param delta  增量
     */
    @Override
    public void incrementPosts(long userId, int delta) {
        incrementUserCounterField(userId, UserCounterSchema.IDX_POSTS, delta);
    }
    
    /**
     * 增加用户获赞数。
     *
     * @param userId 用户ID
     * @param delta  增量
     */
    @Override
    public void incrementLikesReceived(long userId, int delta) {
        incrementUserCounterField(userId, UserCounterSchema.IDX_LIKES_RECEIVED, delta);
    }
    
    /**
     * 增加用户获收藏数。
     *
     * @param userId 用户ID
     * @param delta  增量
     */
    @Override
    public void incrementFavsReceived(long userId, int delta) {
        incrementUserCounterField(userId, UserCounterSchema.IDX_FAVS_RECEIVED, delta);
    }

    /**
     * 重建用户所有计数（从数据库回源）。
     *
     * @param userId 用户ID
     */
    @Override
    public void rebuildAllCounters(long userId) {
        log.info("开始重建计数：userId={}", userId);
        String lockKey = "lock:ucnt-rebuild:" + userId;
        //1.看门狗
        RLock lock = redisson.getLock(lockKey);
        boolean locked = false;
//        分布式锁
        try {
            //尝试获取锁，最多等待5秒
            //使用redisson看门狗机制 ；不指定租期 自动续约
            locked = lock.tryLock(5, TimeUnit.MILLISECONDS);
            if (!locked) {
                log.warn("获取锁失败：userId={}", userId);
                return;
            }
            // 步骤2：创建新的 SDS 缓冲区（20字节）
            UserCounterDTO dto = new UserCounterDTO();
            //获取用户发帖数
            dto.setPosts((long) knowPostsService.countUserPosts(userId).size());
            //获取用户粉丝数
            dto.setFollowers(relationService.followersCount(userId));
            //获取用户关注数
            dto.setFollowings(relationService.followingCount(userId));

//            2.1获赞数
            //先获取作者的所有贴文 ids
            List<Long> postIds = knowPostsService.countUserPosts(userId);
            if (!postIds.isEmpty()) {
                List<String> collect = postIds.stream().map(String::valueOf).collect(Collectors.toList());
                //for循环 遍历每一个贴文内 所有的赞数->在对应的帖子sds中有
                Map<String, Map<String, Long>> batch = counterService.getCountsBatch("knowpost", collect, List.of("like", "fav"));
                // 2. 聚合计数：获赞数与获收藏数
                long likesReceived = 0L;
                long favsReceived = 0L;

                for (Map<String, Long> map : batch.values()) {
                    likesReceived += map.getOrDefault("like", 0L);
                    favsReceived += map.getOrDefault("fav", 0L);
                }
                //获取用户获赞数
                dto.setLikesReceived(likesReceived);
                dto.setFavsReceived(favsReceived);
            }

            //3.创建新的 SDS 缓冲区（20字节）
            byte[] newSds = new byte[expectedLen];
            SdsUtils.writeIntBE(newSds, UserCounterSchema.IDX_FOLLOWINGS * CounterSchema.BYTES_PER_METRIC, dto.getFollowings(), CounterSchema.BYTES_PER_METRIC);
            SdsUtils.writeIntBE(newSds, UserCounterSchema.IDX_FOLLOWERS * CounterSchema.BYTES_PER_METRIC, dto.getFollowers(), CounterSchema.BYTES_PER_METRIC);
            SdsUtils.writeIntBE(newSds, UserCounterSchema.IDX_POSTS * CounterSchema.BYTES_PER_METRIC, dto.getPosts(), CounterSchema.BYTES_PER_METRIC);
            SdsUtils.writeIntBE(newSds, UserCounterSchema.IDX_LIKES_RECEIVED * CounterSchema.BYTES_PER_METRIC, dto.getLikesReceived(), CounterSchema.BYTES_PER_METRIC);
            SdsUtils.writeIntBE(newSds, UserCounterSchema.IDX_FAVS_RECEIVED * CounterSchema.BYTES_PER_METRIC, dto.getFavsReceived(), CounterSchema.BYTES_PER_METRIC);
            // 4. 存入 Redis
            String sdsKey = UserCounterKeys.sdsKey(userId);
            SdsUtils.setRaw(redis, sdsKey, newSds);
            log.info("用户计数重建完成: userId={}, followings={}, followers={}, posts={}, likes={}, favs={}",
                userId, dto.getFollowings(), dto.getFollowers(), dto.getPosts(), dto.getLikesReceived(), dto.getFavsReceived());

        } catch (InterruptedException e) {
            log.error("获取锁失败：userId={}", userId, e);
            return;
        } finally {
            if (locked) {
                lock.unlock();
            }
        }


    }

    /**
     * 原子增加用户计数字段。
     *
     * @param userId     用户ID
     * @param fieldIndex 字段索引（0=关注数, 1=粉丝数, 2=发帖数, 3=获赞数, 4=获收藏数）
     * @param delta      增量值
     */
    private void incrementUserCounterField(long userId, int fieldIndex, int delta) {
        String sdsKey = UserCounterKeys.sdsKey(userId);
        synchronized (this) {
            byte[] raw = SdsUtils.getRaw(redis, sdsKey);
            //字节长度
            if (raw == null || raw.length != expectedLen) {
                log.warn("用户计数SDS结构异常，触发重建: userId={}, 当前长度={}", userId, raw == null ? 0 : raw.length);

                // 触发重建
                rebuildAllCounters(userId);

                // 重建后重新读取
                raw = SdsUtils.getRaw(redis, sdsKey);

                // 如果重建后仍然异常，初始化为空结构
                if (raw == null || raw.length != expectedLen) {
                    log.error("重建后SDS仍然异常，初始化为空结构: userId={}", userId);
                    raw = new byte[expectedLen];
                }
            }

            //不需要重建

            //计算新值
            //偏移量
            int offset = fieldIndex * UserCounterSchema.BYTES_PER_METRIC;
            //当前值
            long currentValue = SdsUtils.readIntBE(raw, offset, UserCounterSchema.BYTES_PER_METRIC);
            long newValue = currentValue + delta;

            // 边界检查：
            //下溢
            if (newValue < 0) {
                newValue = 0;
                log.warn("Invalid delta for user counter field: {}", delta);
            }
            //上溢（根据字节数）
            long maxValue = (1L << (8 * 8)) - 1;  // 1L << 64
            if (newValue > maxValue) {
                newValue = Integer.MAX_VALUE;
                log.warn("Delta too large for user counter field: {}", delta);
            }
            // 步骤7：写入新值
            SdsUtils.writeIntBE(raw, offset, newValue, UserCounterSchema.BYTES_PER_METRIC);

            // 步骤8：保存回 Redis
            SdsUtils.setRaw(redis, sdsKey, raw);
            log.debug("用户计数更新成功: userId={}, fieldIndex={}, oldValue={}, newValue={}",
                userId, fieldIndex, currentValue, newValue);
        }


    }
}
