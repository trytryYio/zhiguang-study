package zhiguang.nauy.counter.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
    @Lazy
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

    private final DefaultRedisScript<Long> incrScript;

    // 用户计数SDS结构长度
    int expectedLen = UserCounterSchema.SCHEMA_LEN * UserCounterSchema.BYTES_PER_METRIC;

    public UserCounterServiceImpl() {
        this.incrScript = new DefaultRedisScript<>();
        this.incrScript.setResultType(Long.class);
        this.incrScript.setScriptText(INCR_FIELD_LUA);
    }

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
     * 原子增加用户计数字段（Lua 脚本，单次 Redis 交互）。
     *
     * @param userId     用户ID
     * @param fieldIndex 字段索引（0-based: 0=关注数, 1=粉丝数, 2=发帖数, 3=获赞数, 4=获收藏数）
     * @param delta      增量值
     */
    private void incrementUserCounterField(long userId, int fieldIndex, int delta) {
        String key = UserCounterKeys.sdsKey(userId);
        // Lua 脚本使用 1-based 索引，所以 +1
        redis.execute(incrScript, List.of(key),
            String.valueOf(UserCounterSchema.SCHEMA_LEN),
            String.valueOf(UserCounterSchema.BYTES_PER_METRIC),
            String.valueOf(fieldIndex + 1),
            String.valueOf(delta));
    }

    /**
     * 用户维度计数原子折叠 Lua 脚本（1-based 索引）。
     * <p>在 Redis 内部原子执行 GET → 修改指定字段 → SET，避免 GET+SET 竞态。</p>
     */
    private static final String INCR_FIELD_LUA = """
        local cntKey = KEYS[1]
        local schemaLen = tonumber(ARGV[1])
        local fieldSize = tonumber(ARGV[2])
        local idx = tonumber(ARGV[3])
        local delta = tonumber(ARGV[4])
        local function read32be(s, off)
          local b = {string.byte(s, off+1, off+4)}
          local n = 0
          for i=1,4 do n = n * 256 + b[i] end
          return n
        end
        local function write32be(n)
          local t = {}
          for i=4,1,-1 do t[i] = n % 256; n = math.floor(n/256) end
          return string.char(unpack(t))
        end
        local cnt = redis.call('GET', cntKey)
        if not cnt then cnt = string.rep(string.char(0), schemaLen * fieldSize) end
        local off = (idx - 1) * fieldSize
        local v = read32be(cnt, off) + delta
        if v < 0 then v = 0 end
        local seg = write32be(v)
        cnt = string.sub(cnt, 1, off) .. seg .. string.sub(cnt, off+fieldSize+1)
        redis.call('SET', cntKey, cnt)
        return 1
        """;
}
