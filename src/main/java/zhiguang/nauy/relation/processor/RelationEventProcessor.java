package zhiguang.nauy.relation.processor;


import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import zhiguang.nauy.counter.service.UserCounterService;
import zhiguang.nauy.relation.event.RelationEvent;
import zhiguang.nauy.relation.mapper.RelationMapper;

import java.time.Duration;

/**
 * 关系事件处理器。
 *
 * <p>职责：对 关注/取关 事件进行去重、防抖与幂等处理，
 * 落库更新粉丝表，维护关注/粉丝 ZSet 缓存与 TTL，并原子更新用户维度计数（SDS）。</p>
 */
@Service
public class RelationEventProcessor {

    @Resource
    private StringRedisTemplate redis;

    @Resource
    private RelationMapper relationMapper;

    @Resource
    private UserCounterService userCounterService;
    /**
     * 处理关系事件：入库、更新缓存、刷新计数，并进行幂等去重。
     *
     * @param evt 关系事件
     */
    public void process(RelationEvent evt) {
        //1. 幂等去重 Redis SETNX，10 分钟过期
//        SETNX SETNX = SET if Not eXists
        //构造缓存键 dedup:rel:{type}:{fromUserId}:{toUserId}:{id}
        String dk = "dedup:rel:" + evt.getType() + ":"
            + evt.getFromUserId() + ":" + evt.getToUserId() + ":"
            + (evt.getId() == null ? "0" : String.valueOf(evt.getId()));
        Boolean first = redis.opsForValue().setIfAbsent(dk, "1", Duration.ofMinutes(10));

        if (first == null || !first) {
            return;  // 非首次，直接返回
        }


        // 2. 根据事件类型执行业务逻辑
        switch (evt.getType()){
            case "FollowCreated":
                //新增关注
                // 写入 follower 表
                relationMapper.insertFollower(evt.getId(), evt.getToUserId(), evt.getFromUserId(), 1);
                long now = System.currentTimeMillis();
                // 更新 Redis ZSet 缓存
//                更新关注列表缓存：`uf:flws:{userId}` ZSet，分数为时间戳。这样查询"我的关注"时可以直接从 Redis 读取，不用查数据库。
                //关注列表
                redis.opsForZSet().add("uf:flws:" + evt.getFromUserId(),
                    String.valueOf(evt.getToUserId()), now);
                //粉丝列表
                redis.opsForZSet().add("uf:fans:" + evt.getToUserId(),
                    String.valueOf(evt.getFromUserId()), now);
                //过期时间
                redis.expire("uf:flws:" + evt.getFromUserId(), Duration.ofHours(2));
                redis.expire("uf:fans:" + evt.getToUserId(), Duration.ofHours(2));


                // 同步用户维度的计数器（SDS 结构），增加关注数和粉丝数。
                userCounterService.incrementFollowings(evt.getFromUserId(), 1);
                userCounterService.incrementFollowers(evt.getToUserId(), 1);


                break;
            case  "FollowCanceled":
                //取消关注
                // 取消粉丝关系
                relationMapper.cancelFollower(evt.getToUserId(), evt.getFromUserId());

                // 更新 Redis ZSet 缓存
                redis.opsForZSet().remove("uf:flws:" + evt.getFromUserId(),
                    String.valueOf(evt.getToUserId()));
                redis.opsForZSet().remove("uf:fans:" + evt.getToUserId(),
                    String.valueOf(evt.getFromUserId()));
                redis.expire("uf:flws:" + evt.getFromUserId(), Duration.ofHours(2));
                redis.expire("uf:fans:" + evt.getToUserId(), Duration.ofHours(2));

                // 同步用户计数器
                userCounterService.incrementFollowings(evt.getFromUserId(), -1);
                userCounterService.incrementFollowers(evt.getToUserId(), -1);
                break;
        }



    }
}
