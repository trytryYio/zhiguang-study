package zhiguang.nauy.knowpost.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import zhiguang.nauy.cache.config.HotKeyDetector;
import zhiguang.nauy.counter.service.CounterService;
import zhiguang.nauy.exception.ErrorCode;
import zhiguang.nauy.exception.ThrowUtils;
import zhiguang.nauy.knowpost.api.dto.FeedItemResponse;
import zhiguang.nauy.knowpost.api.dto.FeedPageResponse;
import zhiguang.nauy.knowpost.domain.KnowPostFeedRow;
import zhiguang.nauy.knowpost.domain.KnowPosts;
import zhiguang.nauy.knowpost.mapper.KnowPostsMapper;
import zhiguang.nauy.knowpost.service.KnowPostFeedService;
import zhiguang.nauy.relation.service.RelationService;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * FeedServiceImpl 列表页用 toKnowPostFeedRow - 只需要基本信息来展示列表项，提高性能
 */

@AllArgsConstructor
@Slf4j
@Service
public class KnowPostFeedServiceImpl extends ServiceImpl<KnowPostsMapper, KnowPosts> implements KnowPostFeedService {
    @Resource
    private Cache<String, FeedPageResponse> feedPublicCache;

    @Resource
    private CounterService counterService;
    @Resource
    private RelationService relationService;
    @Resource
    private Cache<String, FeedPageResponse> feedMineCache;
    private final HotKeyDetector hotKey;
    private final StringRedisTemplate redis;

    @Resource
    private KnowPostsMapper knowPostsMapper;
    private final ConcurrentHashMap<String, Object> singleFlight = new ConcurrentHashMap<>();

    /**
     * 获取公开 Feed
     *
     * @param page   页码
     * @param size   每页大小
     * @param userId 用户ID
     * @return FeedPageResponse
     */
    @Override
    public FeedPageResponse getPublicFeed(int page, int size, Long userId) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 1);
        // 构建缓存 key
        String localPageKey = cacheKey(safePage, safeSize, 0);
//        缓存公共信息流的分页数据（所有用户可见的内容）

        // L1: 先从caffeine
        // 缓存拿数据，高并发时抗 80% 流量

        FeedPageResponse local = feedPublicCache.getIfPresent(localPageKey);
        if (local != null && local.items() != null) {
//            L1 命中缓存
//            对返回列表中的每个条目进行热度统计
            local.items().forEach(item -> recordItemHotKey(item.id()));
//            打印日志
            log.info("feed.public source=local localPageKey={} page={} size={}", localPageKey, safePage, safeSize);
            List<FeedItemResponse> enrichedLocal = enrichedLocals(local.items(), userId);

            return new FeedPageResponse(enrichedLocal, local.page(), local.size(), local.hasMore());

        }


        //        为什么按小时分片？
//        - 降低跨小时内容更新导致的大面积失效风险
//                - 避免热门页在整站失效时同时回源
        // 生成 Redis 片段缓存 Key（按小时分片）

        long hourSlot = System.currentTimeMillis() / 3600000L;
        String idsKey = "feed:public:ids:" + safeSize + ":" + hourSlot + ":" + safePage;
        String hasMoreKey = "feed:public:ids:" + safeSize + ":" + hourSlot + ":" + safePage + ":hasMore";


        // L2: 二级缓存，Redis 片段缓存，组装

        FeedPageResponse fromCache = assembleFromCache(idsKey, hasMoreKey, safePage, safeSize, userId);
        if (fromCache != null) {
            // 写入缓存
            feedPublicCache.put(localPageKey, fromCache);
            if (fromCache.items() != null) {
                // 对返回列表中的每个条目进行热度统计
                for (FeedItemResponse item : fromCache.items()) {
                    recordItemHotKey(item.id());
                }
            }
            log.info("feed.public source=3tier localPageKey={} page={} size={}", localPageKey, safePage, safeSize);
            return fromCache;
        }


//      单次航班锁
        Object lock = singleFlight.computeIfAbsent(idsKey, k -> new Object());
        FeedPageResponse pageResponse;
        synchronized (lock) {
            // 重查 L2 缓存，避免重复回源
            try {
                FeedPageResponse feedPageResponse = assembleFromCache(idsKey, hasMoreKey, safePage, safeSize, userId);
                if (feedPageResponse != null) {
                    // 写入缓存
                    feedPublicCache.put(localPageKey, feedPageResponse);
                    if (feedPageResponse.items() != null) {
                        // 对返回列表中的每个条目进行热度统计
                        for (FeedItemResponse item : feedPageResponse.items()) {
                            recordItemHotKey(item.id());
                        }
                    }
                    log.info("feed.public source=3tier(after-flight) localPageKey={} page={} size={}", localPageKey, safePage, safeSize);
                    return feedPageResponse;
                }


                // 2. 数据库查询（L3 降级）

                //offset 是页码的偏移量，比如第1页的offset是0，第2页的offset是10，第三页是20 ...
                int offset = (safePage - 1) * safeSize;
                List<KnowPostFeedRow> rows = knowPostsMapper.listFeedPublic(safeSize + 1, offset);
                boolean hasMore = rows.size() > safeSize;
                if (hasMore) {
                    //从列表 rows 中截取前 safeSize 个元素，生成一个新的子列表
                    rows = rows.subList(0, safeSize);
                }

                //这个方法对喜欢数和收藏数都进行了填充
                List<FeedItemResponse> items = mapRowsToItems(rows, userId, false);

                pageResponse = new FeedPageResponse(
                    items,
                    safePage,
                    safeSize,
                    hasMore
                );
                // 3. 写入缓存
                // 写入片段缓存与本地缓存
//            含义：片段缓存的过期时间（带随机抖动）
//取值范围：60 ~ 89 秒（60秒基础值 + 0~29秒随机抖动）
//用途：
//为 Redis 中的 IDs、items、hasMore 设置统一的过期时间
//加入随机抖动避免大量缓存同时过期导致缓存雪崩
                int baseTtl = 60;
                int jitter = ThreadLocalRandom.current().nextInt(30);
                Duration frTtl = Duration.ofSeconds(baseTtl + jitter);
                feedPublicCache.put(localPageKey, pageResponse);
                writeCaches(localPageKey, idsKey, hasMoreKey, safeSize, items, hasMore, frTtl);


                // 3. 返回结果


                return pageResponse;
            } finally {
                singleFlight.remove(idsKey, lock);
            }
        }

    }

    private List<FeedItemResponse> enrichedLocals(List<FeedItemResponse> items, Long userId) {

        // 1. 提取所有 ID
        List<String> ids = items.stream().map(FeedItemResponse::id).collect(Collectors.toList());

        // 2. 批量获取计数
        Map<String, Map<String, Long>> countsBatch = counterService.getCountsBatch("knowpost", ids, List.of("like", "fav"));
        // 3. 构建增强后的列表
        List<FeedItemResponse> enrichedItems = new ArrayList<>();


        for (FeedItemResponse item : items) {
            // 安全地获取计数，防止 NPE
            Map<String, Long> counts = countsBatch.getOrDefault(item.id(), Collections.emptyMap());
            Long likeCount = counts.getOrDefault("like", 0L);
            Long favCount = counts.getOrDefault("fav", 0L);
            boolean faved = counterService.isFaved("knowpost", item.id(), userId);
            boolean liked = counterService.isLiked("knowpost", item.id(), userId);
// 4. 如果是 Record，需要创建新对象；如果是普通类且有 setter，则使用 setter
            // 假设 FeedItemResponse 是 Record，我们需要用新的计数值重建它
            // 注意：这里需要根据你 FeedItemResponse 的实际构造函数参数进行调整
            FeedItemResponse enrichedItem = new FeedItemResponse(
                item.id(),
                item.title(),
                item.description(),
                item.coverImage(),
                item.tags(),
                item.authorAvatar(),
                item.authorNickname(),
                item.tagJson(),
                likeCount, // 更新点赞数
                favCount,  // 更新收藏数
                liked,
                faved,
                item.isTop()
                );
            enrichedItems.add(enrichedItem);
        }
        return enrichedItems;
    }


    /**
     * 将数据库行映射为响应条目。
     * 计数通过计数服务填充；liked/faved 按需计算；isTop 仅在个人列表返回。
     *
     * @param rows           查询结果行
     * @param userIdNullable 当前用户 ID（可空）
     * @param includeIsTop   是否在响应中包含 isTop
     * @return 条目列表
     */
    private List<FeedItemResponse> mapRowsToItems(List<KnowPostFeedRow> rows, Long userIdNullable, boolean includeIsTop) {
        List<FeedItemResponse> items = new ArrayList<>(rows.size());

        for (KnowPostFeedRow r : rows) {
            List<String> tags = JSONUtil.toList(r.getTags(), String.class);
            List<String> imgs = JSONUtil.toList(r.getImgUrls(), String.class);

            String cover = imgs.isEmpty() ? null : imgs.get(0);

            Map<String, Long> counts = counterService.getCounts("knowpost", String.valueOf(r.getId()), List.of("like", "fav"));
            Long likeCount = counts.get("like");
            Long favCount = counts.get("fav");
            ThrowUtils.throwIf(likeCount == null, ErrorCode.OPERATION_ERROR);
            ThrowUtils.throwIf(favCount == null, ErrorCode.OPERATION_ERROR);


            Boolean liked = userIdNullable != null && counterService.isLiked("knowpost", String.valueOf(r.getId()), userIdNullable);
            Boolean faved = userIdNullable != null && counterService.isFaved("knowpost", String.valueOf(r.getId()), userIdNullable);
            Boolean isTop = includeIsTop ? r.getIsTop() : null;

            items.add(new FeedItemResponse(
                String.valueOf(r.getId()),
                r.getTitle(),
                r.getDescription(),
                cover,
                tags,
                r.getAuthorAvatar(),
                r.getAuthorNickname(),
                r.getAuthorTagJson(),
                likeCount,
                favCount,
                liked,
                faved,
                isTop
            ));
        }
        return items;
    }


    /**
     * 写入片段缓存与软缓存：
     * - idsKey：ID 列表（中 TTL）
     * - item：条目片段（中 TTL）
     * - hasMore：软缓存，满页时缓存 true 10~20s，否则 10s
     * 注意：不再写入 Redis 整页缓存 (pageKey)，避免双重存储。
     *
     * @param pageKey    页面缓存 Key (用于反向索引引用)
     * @param idsKey     ID 列表 Key
     * @param hasMoreKey 软缓存 Key
     * @param size       每页大小
     * @param items      条目列表（计数已填充，liked/faved 为空）
     * @param hasMore    是否还有更多
     * @param frTtl      片段缓存 TTL
     */
    private void writeCaches(String pageKey, String idsKey, String hasMoreKey, int size, List<FeedItemResponse> items, boolean hasMore, Duration frTtl) {
        // ID 列表
        List<String> idVals = new ArrayList<>();

        for (FeedItemResponse r : items) {
            idVals.add(r.id());
        }

        if (!idVals.isEmpty()) {
//            1️⃣ 存储 ID 列表（idsKey）
//            leftPushAll ，从（头部） 依次插入到 Redis 的列表中。
            redis.opsForList().leftPushAll(idsKey, idVals);
            redis.expire(idsKey, frTtl);
            // 软缓存 hasMore：仅在满页时缓存 true，TTL 很短
            if (idVals.size() == size && hasMore) {
                // 软缓存 hasMore：TTL 10~20s
                redis.opsForValue().set(hasMoreKey, "1", Duration.ofSeconds(10 + ThreadLocalRandom.current().nextInt(11)));
            } else {
//                处理其他情况（页面没满，或者虽然满了但没有更多数据了）。
//                如果 hasMore 是 true（但页面没满？这逻辑上通常不会发生，除非查询逻辑特殊），存 "1"。
//                如果 hasMore 是 false，存 "0"。
                redis.opsForValue().set(hasMoreKey, hasMore ? "1" : "0", Duration.ofSeconds(10));
            }
        }

        // 页面键集合索引，用于按页面维度批量失效与清理（即使没有 Redis 整页缓存，依然保留反向索引用于本地缓存通知或其他用途）
        redis.opsForSet().add("feed:public:pages", pageKey);

        for (FeedItemResponse it : items) {
            // 反向索引：按小时为每个内容建立“页面引用关系”，支持内容更新时快速定位受影响页面
            long hourSlot = System.currentTimeMillis() / 3600000L;
            String idxKey = "feed:public:index:" + it.id() + ":" + hourSlot;
            redis.opsForSet().add(idxKey, pageKey);
            redis.expire(idxKey, frTtl);

            try {
                String itemKey = "feed:item:" + it.id();
                String itemJson = JSONUtil.toJsonStr(it);
                redis.opsForValue().set(itemKey, itemJson, frTtl);
            } catch (Exception ignored) {
            }
        }
    }


    /**
     * 从 Redis 片段缓存组装页面：
     * - idsKey：列表 ID 顺序
     * - itemKey：每个条目基础信息
     * - countKey：点赞/收藏计数
     * 若缺片段则回源修补并写回软缓存。
     *
     * @param idsKey     Redis 列表 Key
     * @param hasMoreKey Redis 软缓存 hasMore Key
     * @param safePage   页码
     * @param safeSize   每页大小
     * @param userId     ID（用于 liked/faved）
     * @return 组装完成的页面；不存在时返回 null
     */
    private FeedPageResponse assembleFromCache(@NotNull String idsKey, @NotNull String hasMoreKey, @NotNull int safePage, @NotNull int safeSize, Long userId) {

//        1.读取idsKey 中的ids列表
//        range 是 Redis List（列表）数据类型的一个操作命令，用于获取列表中指定范围内的元素
        List<String> idList = redis.opsForList().range(idsKey, 0, safeSize - 1);
        String hasMoreStr = redis.opsForValue().get(hasMoreKey);
        //没有缓存
        if (idList == null || idList.isEmpty()) return null;

//        2.批量读取每个文章的详情
        List<String> itemKeys = new ArrayList<>(idList.size());
        for (String id : idList) {
            itemKeys.add("feed:item:" + id);
        }
        // 批量获取（一次网络请求，比循环 N 次快很多）
//        multiGet 是 Spring Data Redis 中用于批量获取多个 Key 的值的操作方法。
        List<String> itemJsons = redis.opsForValue().multiGet(itemKeys);
        List<FeedItemResponse> items = new ArrayList<>(idList.size());
//         如果任何一个文章详情缺失，整个页面缓存失效，回源
        if (itemJsons.size() != idList.size() || idList.isEmpty()) return null;
        if (itemJsons.stream().anyMatch(json -> json == null || json.isBlank())) {
            return null;
        }

        try {
            items = itemJsons.stream()
                .map(json -> JSONUtil.toBean(json, FeedItemResponse.class))
                .collect(Collectors.toList());
        } catch (Exception e) {
// JSON 解析失败，同样回源
            return null;
        }


//        4.组装返回
        // hasMore 优先使用软缓存值；若缺失，则以"满页"作为兜底判断
        boolean hasMore = hasMoreStr != null ? "1".equals(hasMoreStr) : (idList.size() == safeSize);
        return new FeedPageResponse(items, safePage, safeSize, hasMore);
    }


    //    获取我的个人文章
    @Override
    public FeedPageResponse getMyPublished(long userId, int page, int size) {

        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 1);
        String key = myCacheKey(userId, safePage, safeSize);

//        L1  caffeine缓存
        FeedPageResponse local = feedMineCache.getIfPresent(key);
        if (local != null) {
            hotKey.record(key);
            maybeExtendTtlMine(key);
            log.info("feed.mine source=local key={} page={} size={} user={}", key, safePage, safeSize, userId);
            return local;
        }


//      L2
//        redis 缓存
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            try {
                FeedPageResponse cachedResp = JSONUtil.toBean(cached, FeedPageResponse.class);
                // 老缓存可能没有点赞数
                boolean hasCounts = cachedResp.items() != null && cachedResp.items().stream()
//                    allMatch 所有都满足才为真
                    .allMatch(it -> it.likeCount() != null && it.favoriteCount() != null);

                if (hasCounts) {
//                    如果有点赞数
                    // 覆盖 liked/faved，确保老缓存也能返回用户维度状态
                    feedMineCache.put(key, cachedResp);
                    hotKey.record(key);
                    maybeExtendTtlMine(key);
                    log.info("feed.mine source=page key={} page={} size={} user={}", key, safePage, safeSize, userId);
                    List<FeedItemResponse> enriched = enrichedLocals(cachedResp.items(), userId);

                    return new FeedPageResponse(cachedResp.items(), cachedResp.page(), cachedResp.size(), cachedResp.hasMore());
                }
            } catch (Exception ignored) {
            }
        }


        int offset = (safePage - 1) * safeSize;
        List<KnowPostFeedRow> rows = knowPostsMapper.listMyPublished(userId, safeSize + 1, offset);
        boolean hasMore = rows.size() > safeSize;
        if (hasMore) rows = rows.subList(0, safeSize);
        //
        List<FeedItemResponse> items = mapRowsToItems(rows, userId, true);
        FeedPageResponse resp = new FeedPageResponse(items, safePage, safeSize, hasMore);
        log.info("feed.mine source=db key={} page={} size={} user={} hasMore={}", key, safePage, safeSize, userId, hasMore);
        return resp;
    }


    /**
     * 根据热点级别动态延长“我的发布”页面缓存 TTL。
     *
     * @param key 页面缓存 Key
     */
    private void maybeExtendTtlMine(String key) {
        int baseTtl = 30;
        int target = hotKey.ttlForMine(baseTtl, key);
        Long currentTtl = redis.getExpire(key);
        if (currentTtl < target) {
            redis.expire(key, Duration.ofSeconds(target));
        }
    }

    /**
     * 生成“我的发布”列表的缓存 Key（用户维度）。
     *
     * @param userId 用户 ID
     * @param page   页码
     * @param size   每页大小
     * @return Redis 页面缓存 Key
     */
    private String myCacheKey(long userId, int page, int size) {
        // Redis Key: feed:mine:{userId}:{size}:{page}
        return "feed:mine:" + userId + ":" + size + ":" + page;
    }

    private FeedItemResponse toFeedItemResponse(KnowPosts post, String authorAvatar, String authorNickname, Boolean liked, Boolean faved, Long likeCount, Long favoriteCount) {
        // ★ 安全解析 tags：null / 空值 → 空列表，避免 JSONUtil.toList 空指针
        List<String> tags;
        if (post.getTags() == null) {
            tags = List.of();
        } else if (post.getTags() instanceof List) {
            tags = (List<String>) post.getTags();
        } else {
            tags = JSONUtil.toList(post.getTags().toString(), String.class);
        }

        // ★ 安全解析 imgUrls：null / 空数组 → 空列表，避免 img.get(0) 越界
        List<String> img;
        if (post.getImgUrls() == null) {
            img = List.of();
        } else {
            img = JSONUtil.toList(post.getImgUrls().toString(), String.class);
        }

        return new FeedItemResponse(
            String.valueOf(post.getId()),
            post.getTitle(),
            post.getDescription(),
            // coverImage - 取第一张图片作为封面，列表为空则返回 null
            !img.isEmpty() ? img.get(0) : null,
            tags,
            // authorAvatar - 这里暂时为空，实际需要关联用户表获取
            authorAvatar,
            // authorNickname - 这里暂时为空，实际需要关联用户表获取
            authorNickname,
            post.getTags() != null ? post.getTags().toString() : null,
            // likeCount和favoriteCount - 这里暂时为0，实际需要从计数服务获取
            likeCount,
            favoriteCount,
            // liked和faved - 这里暂时为false，实际需要根据userId判断
            liked,
            faved,
            post.getIsTop() != null && post.getIsTop() == 1
        );
    }

    /**
     * 记录单个内容条目的热度，并尝试延长其相关片段缓存的 TTL。
     *
     * @param itemId 内容 ID
     */
    private void recordItemHotKey(String itemId) {
        //0. 使用内容 ID 作为热点统计 Key，而不是页面 Key
        String hotspotKey = "knowpost:" + itemId;
        //1. 尝试记录该内容
//        10:01 - 用户访问文章A
//  → record("文章A")
//  → counters = {"文章A": [1,0,0,0,0,0]}
//10:02 - 用户访问文章A
//  → record("文章A")
//  → counters = {"文章A": [2,0,0,0,0,0]}
//10:03 - 用户访问文章A
//  → record("文章A")
//  → counters = {"文章A": [3,0,0,0,0,0]}
//10:04 - 用户访问文章B
//  → record("文章B")
//  → counters = {
//      "文章A": [3,0,0,0,0,0],
//      "文章B": [1,0,0,0,0,0]
//    }
//10:05 - 用户访问文章A
//  → record("文章A")
//  → counters = {
//      "文章A": [4,0,0,0,0,0],
//      "文章B": [1,0,0,0,0,0]
//    }
//---
        hotKey.record(hotspotKey);
        int baseTtl = 60;
        //计算ttl
        int target = hotKey.ttlForPublic(baseTtl, hotspotKey);

//         延长该内容的详情片段缓存
        String itemKey = "feed:item:" + itemId;
        //从redis中获取过期时间
        Long itemTtl = redis.getExpire(itemKey);
        if (itemTtl < target) {
//            重置过期时间
            redis.expire(itemKey, Duration.ofSeconds(target));
        }

    }

    /**
     * 生成缓存键
     * <p>根据页码、每页大小和布局版本生成唯一的缓存键</p>
     * <p>缓存键格式：feed:public:{size}:{page}:v{layoutVer}</p>
     * <p>示例：feed:public:10:1:v1 表示第1页，每页10条，布局版本1</p>
     *
     * @param safePage 安全的页码（已校验 >= 1）
     * @param safeSize 安全的每页大小（已校验 1~50）
     * @return 缓存键字符串
     */
    private String cacheKey(int safePage, int safeSize, int LAYOUT_VERSION) {
        return String.format("feed:public:%d:%d:v%d", safeSize, safePage, LAYOUT_VERSION);
    }


    /**
     * 叠加用户维度状态，将 liked/faved  已点赞/已收藏 根据用户计算覆盖到列表上。
     * 不改写底层缓存，避免不同用户状态互相污染。
     * @param base 基础列表（含计数）
     * @param uid 用户 ID（可空）
     * @return 叠加 liked/faved 的列表
     */
//    private List<FeedItemResponse> enrich(List<FeedItemResponse> base, Long uid) {
//        List<FeedItemResponse> out = new ArrayList<>(base.size());
//
//        for (FeedItemResponse it : base) {
//            boolean liked = uid != null && counterService.isLiked("zhiguang/nauy/knowpost", it.id(), uid);
//            boolean faved = uid != null && counterService.isFaved("zhiguang/nauy/knowpost", it.id(), uid);
//            out.add(new FeedItemResponse(
//                    it.id(),
//                    it.title(),
//                    it.description(),
//                    it.coverImage(),
//                    it.tags(),
//                    it.authorAvatar(),
//                    it.authorNickname(),
//                    it.tagJson(),
//                    it.likeCount(),
//                    it.favoriteCount(),
//                    liked,
//                    faved,
//                    it.isTop()
//            ));
//        }
//        return out;
//    }
}
