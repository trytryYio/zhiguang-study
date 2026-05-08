package zhiguang.nauy.knowpost.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zhiguang.nauy.cache.config.HotKeyDetector;
import zhiguang.nauy.exception.BusinessException;
import zhiguang.nauy.exception.ErrorCode;
import zhiguang.nauy.exception.ThrowUtils;
import zhiguang.nauy.knowpost.api.dto.KnowPostDetailResponse;
import zhiguang.nauy.knowpost.domain.KnowPostDetailRow;
import zhiguang.nauy.knowpost.domain.KnowPostFeedRow;
import zhiguang.nauy.knowpost.domain.KnowPosts;
import zhiguang.nauy.knowpost.domain.id.SnowflakeIdGenerator;
import zhiguang.nauy.knowpost.mapper.KnowPostsMapper;
import zhiguang.nauy.knowpost.service.KnowPostFeedService;
import zhiguang.nauy.knowpost.service.KnowPostsService;
import zhiguang.nauy.storage.OssStorageService;
import zhiguang.nauy.user.domain.User;
import zhiguang.nauy.user.service.UserService;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 知文服务实现类
 * 详情页用 toKnowPostDetailRow - 需要完整信息来显示内容和做权限控制
 *
 * <p>提供知文的创建、编辑、发布、删除等核心业务逻辑</p>
 *
 * @author yuan
 * @description 针对表【know_posts(知文主表-存储文章/帖子的核心元数据)】的数据库操作Service实现
 * @createDate 2026-04-17 15:01:16
 */
@Service
@Slf4j
@Transactional
public class KnowPostServiceImpl extends ServiceImpl<KnowPostsMapper, KnowPosts>
        implements KnowPostsService {

    @Resource
    private UserService userService;
    @Resource
    private KnowPostFeedService knowPostFeedService;

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private StringRedisTemplate redis;
    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Resource
    private OssStorageService ossStorageService;

    @Resource
    private HotKeyDetector hotKey;
    @Qualifier("knowPostDetailCache")
    @Resource
    private Cache<String, KnowPostDetailResponse> knowPostDetailCache;

    private final ConcurrentHashMap<String, Object> singleFlight = new ConcurrentHashMap<>();
    @Autowired
    private KnowPostsMapper knowPostsMapper;

    /**
     * 创建草稿
     * <p>初始化一个知文草稿，设置默认状态为 DRAFT，返回草稿 ID</p>
     * <p>业务流程：</p>
     * <ul>
     *
     * </ul>
     *
     * @param creatorId 创作者用户ID
     * @return 草稿的知文ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createDraft(long creatorId) {
//         <li>1. 创建 KnowPosts 实体，设置 creatorId</li>
        KnowPosts knowPosts = new KnowPosts();
//        贴文id ->雪花 id
        long id = snowflakeIdGenerator.nextId();
        knowPosts.setId(id);
        knowPosts.setCreatorId(creatorId);

        //     *   <li>2. 设置 status = "DRAFT"（草稿状态）</li>
        knowPosts.setStatus("DRAFT");
//     *   <li>3. 设置 visible = "PRIVATE"（默认可见性为私密）</li>
        knowPosts.setVisible("PRIVATE");
//     *   <li>4. 设置 isTop = 0（默认不置顶）</li>
        knowPosts.setIsTop(0);
        knowPosts.setCreateTime(new Date());
        knowPosts.setUpdateTime(new Date());
//     *   <li>5. 插入数据库，返回生成的 ID</li>
        this.save(knowPosts);
        return id;
    }

    /**
     * 确认内容上传完成
     * <p>在用户上传内容到 OSS 后，记录文件的元数据信息</p>
     * <p>业务流程：</p>
     * <ul>
     *   <li>1. 根据 id 和 creatorId 查询知文，校验权限</li>
     *   <li>2. 更新 contentObjectKey、contentEtag、contentSize、contentSha256 字段</li>
     *   <li>3. 可选：生成预签名 URL 并保存到 contentUrl</li>
     *   <li>4. 更新 updateTime</li>
     * </ul>
     *
     * @param creatorId 创作者用户ID
     * @param id        知文ID
     * @param objectKey OSS 对象键
     * @param etag      文件 ETag（用于校验文件完整性）
     * @param size      文件大小（字节）
     * @param sha256    文件 SHA256 哈希值
     */
    @Override
    public void confirmContent(long creatorId, long id, String objectKey, String etag, Long size, String sha256) {
        KnowPosts knowPosts = validteKnowPostAuth(creatorId, id);

        // 更新元数据
        // 2. 使用 UpdateWrapper 只更新指定字段
        LambdaUpdateWrapper<KnowPosts> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(KnowPosts::getId, id)
                .set(KnowPosts::getContentObjectKey, objectKey)
                .set(KnowPosts::getContentEtag, etag)
                .set(KnowPosts::getContentSize, size)
                .set(KnowPosts::getContentSha256, sha256)
                .set(KnowPosts::getContentUrl, ossStorageService.publicUrl(objectKey))
                .set(KnowPosts::getUpdateTime, new Date());
        this.updateById(knowPosts);
    }


    /**
     * 更新知文元数据
     * <p>更新知文的标题、标签、图片、可见性、置顶状态和描述等信息</p>
     * <p>业务流程：</p>
     * <ul>
     *
     * </ul>
     *
     * @param creatorId   创作者用户ID（用于权限校验）
     * @param id          知文ID
     * @param title       标题
     * @param tagId       主标签ID
     * @param tags        标签列表
     * @param imgUrls     图片URL列表
     * @param visible     可见性（PUBLIC-公开, PRIVATE-私密, FRIENDS-仅好友可见）
     * @param isTop       是否置顶
     * @param description 描述/摘要
     */
    @Override
    public void updateMetadata(long creatorId, long id, String title, Long tagId, List<String> tags, List<String> imgUrls, String visible, Boolean isTop, String description) {

//             *   <li>1. 根据 id 和 creatorId 查询知文，校验权限</li>
        KnowPosts knowPosts = validteKnowPostAuth(creatorId, id);
//     *   <li>2. 校验参数合法性（标题长度、可见性枚举值等）</li>
        ThrowUtils.throwIf(title.length() > 50, ErrorCode.PARAMS_ERROR, "标题长度不能超过 50 个字符");
//     *   <li>3. 更新 title、tagId、tags、imgUrls、visible、isTop、description 字段</li>
//     *   <li>4. tags 和 imgUrls 需要序列化为 JSON 字符串存储</li>
        String tagJson = tags != null ? JSONUtil.toJsonStr(tags) : null;
        String imgJson = imgUrls != null ? JSONUtil.toJsonStr(imgUrls) : null;

//     *   <li>5. 更新 updateTime</li>
        LambdaUpdateWrapper<KnowPosts> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(KnowPosts::getId, id)
                .set(KnowPosts::getTitle, title)
                .set(KnowPosts::getTagId, tagId)
                .set(KnowPosts::getTags, tagJson)
                .set(KnowPosts::getImgUrls, imgJson)
                .set(KnowPosts::getVisible, visible)
                .set(KnowPosts::getDescription, description)
                .set(KnowPosts::getIsTop, isTop)
                .set(KnowPosts::getUpdateTime, new Date());
        boolean update = this.update(updateWrapper);
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR);

    }

    /**
     * 发布知文
     * <p>将草稿状态的知文变更为已发布状态（PUBLISHED），设置发布时间</p>
     * <p>业务流程：</p>
     * <ul>
     *   <li>1. 根据 id 和 creatorId 查询知文，校验权限</li>
     *   <li>2. 校验知文状态是否为 DRAFT（只有草稿可以发布）</li>
     *   <li>3. 校验必要字段是否完整（title、contentObjectKey 等）</li>
     *   <li>4. 更新 status = "PUBLISHED"</li>
     *   <li>5. 设置 publishTime = 当前时间</li>
     *   <li>6. 更新 updateTime</li>
     * </ul>
     *
     * @param creatorId 创作者用户ID（用于权限校验）
     * @param id        知文ID
     */
    @Override
    public void publish(long creatorId, long id) {
        //A.更新状态
//        0.校验参数
        KnowPosts knowPosts = validteKnowPostAuth(creatorId, id);
//        1.参数设置
        knowPosts.setVisible("PUBLISHED");
        knowPosts.setUpdateTime(new Date());
        knowPosts.setPublishTime(new Date());
//        2.构造lambudaupdatewrapper
        LambdaUpdateWrapper<KnowPosts> updateWrapper = createLambdaUpdateWrapper(knowPosts);
        boolean update = this.update(knowPosts, updateWrapper);
//        3.判断是否更新成功
        ThrowUtils.throwIf(!update, ErrorCode.BAD_REQUEST, "草稿不存在或无权限");

    }

    /**
     * 更新置顶状态
     * <p>设置或取消知文的置顶状态</p>
     * <p>业务流程：</p>
     * <ul>
     *   <li>1. 根据 id 和 creatorId 查询知文，校验权限</li>
     *   <li>2. 更新 isTop 字段（1-置顶, 0-取消置顶）</li>
     *   <li>3. 更新 updateTime</li>
     * </ul>
     *
     * @param creatorId 创作者用户ID（用于权限校验）
     * @param id        知文ID
     * @param isTop     是否置顶（true-置顶, false-取消置顶）
     */
    @Override
    public void updateTop(long creatorId, long id, boolean isTop) {
        KnowPosts knowPosts = validteKnowPostAuth(creatorId, id);
        knowPosts.setIsTop(isTop ? 1 : 0);
        boolean update = this.update(knowPosts, createLambdaUpdateWrapper(knowPosts));
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 更新可见性
     * <p>修改知文的可见范围（公开/私密/仅好友可见）</p>
     * <p>业务流程：</p>
     * <ul>
     *   <li>1. 根据 id 和 creatorId 查询知文，校验权限</li>
     *   <li>2. 校验 visible 参数是否为合法枚举值（PUBLIC/PRIVATE/FRIENDS）</li>
     *   <li>3. 更新 visible 字段</li>
     *   <li>4. 更新 updateTime</li>
     * </ul>
     *
     * @param creatorId 创作者用户ID（用于权限校验）
     * @param id        知文ID
     * @param visible   可见性（PUBLIC-公开, PRIVATE-私密, FRIENDS-仅好友可见）
     */
    @Override
    public void updateVisibility(long creatorId, long id, String visible) {
        KnowPosts knowPosts = validteKnowPostAuth(creatorId, id);
        knowPosts.setVisible(visible);
        boolean update = this.update(knowPosts, createLambdaUpdateWrapper(knowPosts));
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR);


    }

    /**
     * 删除知文
     * <p>逻辑删除知文（设置 deleted=1），不会物理删除数据库记录</p>
     * <p>业务流程：</p>
     * <ul>
     *   <li>1. 根据 id 和 creatorId 查询知文，校验权限</li>
     *   <li>2. 使用 MyBatis-Plus 的逻辑删除功能（如果配置了 @TableLogic）</li>
     *   <li>3. 或者手动更新 status = "DELETED"</li>
     *   <li>4. 更新 updateTime</li>
     * </ul>
     *
     * @param creatorId 创作者用户ID（用于权限校验）
     * @param id        知文ID
     */
    @Override
    public void delete(long creatorId, long id) {
//        0.校验参数
        KnowPosts knowPosts = validteKnowPostAuth(creatorId, id);
        boolean b = this.removeById(knowPosts);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 获取知文详情（三级缓存架构）
     * <p>采用 L1(Caffeine) -> L2(Redis) -> L3(Database) 的三级缓存策略，配合 SingleFlight 防止缓存击穿</p>
     * <p>业务流程：</p>
     * <ol>
     *   <li>L1 缓存查询：从 Caffeine 本地缓存中快速获取热点数据</li>
     *   <li>L2 缓存查询：从 Redis 分布式缓存中获取数据</li>
     *   <li>SingleFlight 合并请求：对同一 key 的请求加锁，防止缓存击穿</li>
     *   <li>双重检查：获取锁后再次检查缓存，避免重复查询数据库</li>
     *   <li>L3 数据库查询：缓存未命中时从数据库查询</li>
     *   <li>空值缓存：对于不存在的数据写入 "NULL" 防止缓存穿透</li>
     *   <li>权限校验：根据可见性和用户身份判断是否有访问权限</li>
     *   <li>组装响应：将数据库记录转换为响应对象返回</li>
     * </ol>
     *
     * @param id                    知文ID
     * @param currentUserIdNullable 当前登录用户ID（可为 null，表示未登录用户）
     * @return 知文详情响应对象
     * @throws BusinessException 当内容不存在或无权限访问时抛出异常
     */
    @Override
    public KnowPostDetailResponse getDetail(long id, Long currentUserIdNullable) {
        log.info("获取帖子详情，id={}, currentUserId={}", id, currentUserIdNullable);

        // ========== 第一步：L1 缓存查询（Caffeine 本地缓存）==========
        String pageKey = "knowPost:detail:" + id + ":v" + 0;
        KnowPostDetailResponse local = knowPostDetailCache.getIfPresent(pageKey);
        if (local != null) {
            // 命中 L1 缓存，记录热度并延长 TTL
            recordHotKeyAndExtendTtl(id, pageKey);
            log.info("L1缓存命中，id={}, currentUserId={}", id, currentUserIdNullable);
            return local;
        }

        // ========== 第二步：L2 缓存查询（Redis 分布式缓存）==========
        String cached = redis.opsForValue().get(pageKey);
        KnowPostDetailResponse resp = tryProcessCacheHit(cached, id, pageKey, currentUserIdNullable, "page");
        if (resp != null) {
            // 命中 L2 缓存，直接返回
            return resp;
        }

        // ========== 第三步：SingleFlight 防止缓存击穿 ==========
        // 对同一个 pageKey 加锁，防止高并发下大量请求同时打到数据库
        Object lock = singleFlight.computeIfAbsent(pageKey, k -> new Object());
        synchronized (lock) {
            try {
                // 双重检查：获取锁后再次检查缓存，因为等待锁期间其他线程可能已写入缓存
                String again = redis.opsForValue().get(pageKey);
                resp = tryProcessCacheHit(again, id, pageKey, currentUserIdNullable, "page(after-flight)");
                if (resp != null) {
                    // 缓存已由其他线程填充，直接返回
                    return resp;
                }

                // ========== 第四步：L3 数据库查询（兜底查询）==========
                KnowPosts knowPosts = this.getById(id);
                ThrowUtils.throwIf(knowPosts == null, ErrorCode.NOT_FOUND, "内容不存在");

                KnowPostDetailRow row = toKnowPostDetailRow(knowPosts);

                // 处理内容不存在或已删除的情况，写入空值缓存防止缓存穿透
                if (row == null || "deleted".equals(row.getStatus())) {
                    long ttl = 30 + RandomUtil.randomInt(31);
                    redis.opsForValue().set(pageKey, "NULL", Duration.ofSeconds(ttl));
                    log.warn("帖子不存在或已删除，设置空值缓存，id={}", id);
                    throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在");
                }

                // ========== 第五步：权限校验 ==========
                boolean isPublic = "published".equals(row.getStatus()) && "public".equals(row.getVisible());
                boolean isOwner = currentUserIdNullable != null && currentUserIdNullable.equals(row.getCreatorId());
                if (!isPublic && !isOwner) {
                    log.warn("无权限查看，id={}, currentUserId={}", id, currentUserIdNullable);
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看");
                }

                // ========== 第六步：组装响应对象 ==========
                // TODO: 后续补充实时数据（点赞数、收藏数、用户互动状态等）
                KnowPostDetailResponse response = fillKnowPostDetailResponse(row, null, null, null, null);
                return response;

            } finally {
                // 释放 SingleFlight 锁，允许后续请求进入
                singleFlight.remove(pageKey);
            }
        }
    }

    /**
     * 尝试处理缓存命中
     * @param cached
     * @param id
     * @param pageKey

     * @return
     */
    private KnowPostDetailResponse tryProcessCacheHit(String cached, long id, String pageKey, Long uid, String sourceLog) {
//        0.校验参数
        if (cached == null) {
            return null;
        }
        // 1. 命中空值缓存（防止穿透）
        if ("NULL".equals(cached)){
            ThrowUtils.throwIf(true, ErrorCode.NOT_FOUND, "内容不存在");
        }
//        2.命中
        try {
            // 3. 反序列化缓存数据
            KnowPostDetailResponse bean = JSONUtil.toBean(cached, KnowPostDetailResponse.class);

            //   填充caffeine
            knowPostDetailCache.put(pageKey, bean);
            // 4. 记录热度并尝试续期
            // 如果该内容正在被高频访问，自动延长其缓存 TTL
            recordHotKeyAndExtendTtl(id, pageKey);
            log.info("detail source={} key={}", sourceLog, pageKey);

            // todo 后续完成5. 叠加实时数据（计数与用户状态）并返回
//            return enrichDetailResponse(base, uid, true);
            return bean;
        } catch (Exception ignored) {
            // 反序列化失败等异常情况，视为未命中，回源修复
            return null;
        }

    }

    private KnowPostDetailResponse fillKnowPostDetailResponse(KnowPostDetailRow row,Long likeCount,Long favoriteCount,Boolean liked ,Boolean faved ){
        List<String> images = JSONUtil.toList(row.getImgUrls(),String.class);
        List<String> tags = JSONUtil.toList(row.getTags(),String.class);

        return  new KnowPostDetailResponse(
                String.valueOf(row.getId()),
                row.getTitle(),
                row.getDescription(),
                row.getContentUrl(),
                images,
                tags,
                String.valueOf(row.getCreatorId()),
                row.getAuthorAvatar(),
                row.getAuthorNickname(),
                row.getAuthorTagJson(),
                likeCount,
                favoriteCount,
                liked, // liked 状态暂时留空，由 enrich 填充
                faved, // faved 状态暂时留空，由 enrich 填充
                row.getIsTop(),
                row.getVisible(),
                row.getType(),
                row.getPublishTime()
        );
    }
    @Override
    public KnowPostDetailRow toKnowPostDetailRow(KnowPosts knowPosts) {
        KnowPostDetailRow knowPostDetailRow = new KnowPostDetailRow();
        knowPostDetailRow.setId(knowPosts.getId());
        knowPostDetailRow.setCreatorId(knowPosts.getCreatorId());
        knowPostDetailRow.setTitle(knowPosts.getTitle() != null ? knowPosts.getTitle() : "");
        knowPostDetailRow.setDescription(knowPosts.getDescription() != null ? knowPosts.getDescription() : "");
        knowPostDetailRow.setTags(knowPosts.getTags() != null ? knowPosts.getTags().toString() : "");
        knowPostDetailRow.setImgUrls(knowPosts.getImgUrls() != null ? knowPosts.getImgUrls().toString() : "");
        knowPostDetailRow.setContentUrl(knowPosts.getContentUrl() != null ? knowPosts.getContentUrl() : "");
        knowPostDetailRow.setContentEtag(knowPosts.getContentEtag() != null ? knowPosts.getContentEtag() : "");
        knowPostDetailRow.setContentSha256(knowPosts.getContentSha256() != null ? knowPosts.getContentSha256() : "");
        User user = userService.findById(knowPosts.getCreatorId());
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND);
        knowPostDetailRow.setAuthorAvatar(user.getAvatar());
        knowPostDetailRow.setAuthorNickname(user.getNickname());
        knowPostDetailRow.setAuthorTagJson(JSONUtil.toJsonStr(user.getTagsJson()));
        knowPostDetailRow.setPublishTime(knowPosts.getPublishTime() != null ? knowPosts.getPublishTime().toInstant() : null);
        knowPostDetailRow.setIsTop(knowPosts.getIsTop() != null ? knowPosts.getIsTop() == 1 : false);
        knowPostDetailRow.setVisible(knowPosts.getVisible() != null ? knowPosts.getVisible() : "");
        knowPostDetailRow.setType(knowPosts.getType() != null ? knowPosts.getType() : "");
        knowPostDetailRow.setStatus(knowPosts.getStatus() != null ? knowPosts.getStatus() : "");
        return knowPostDetailRow;
    }
    @Override
    public KnowPostFeedRow toKnowPostFeedRow(KnowPosts knowPosts) {
        KnowPostFeedRow knowPostFeedRow = new KnowPostFeedRow();
        knowPostFeedRow.setId(knowPosts.getId() != null ? knowPosts.getId() : 0L);
        knowPostFeedRow.setTitle(knowPosts.getTitle() != null ? knowPosts.getTitle() : "");
        knowPostFeedRow.setDescription(knowPosts.getDescription() != null ? knowPosts.getDescription() : "");
        knowPostFeedRow.setTags(knowPosts.getTags() != null ? knowPosts.getTags().toString() : "");
        knowPostFeedRow.setImgUrls(knowPosts.getImgUrls() != null ? knowPosts.getImgUrls().toString() : "");
        User user = userService.findById(knowPosts.getCreatorId());
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND);
        knowPostFeedRow.setAuthorAvatar(user.getAvatar());
        knowPostFeedRow.setAuthorNickname(user.getNickname());
        knowPostFeedRow.setAuthorTagJson(JSONUtil.toJsonStr(user.getTagsJson()));
        knowPostFeedRow.setPublishTime(knowPosts.getPublishTime() != null ? knowPosts.getPublishTime().toInstant() : null);
        knowPostFeedRow.setIsTop(knowPosts.getIsTop() != null ? knowPosts.getIsTop() == 1 : false);
        return knowPostFeedRow;
    }

    /**
     * 获取创作者内容数量
     * @param creatorId 创作者
     * @return
     */
    @Override
    public List<Long> countUserPosts(long creatorId) {
        LambdaQueryWrapper<KnowPosts> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowPosts::getCreatorId, creatorId)
            .eq(KnowPosts::getIsDelete,false)
            .select(KnowPosts::getId);
        List<Object> idObj = knowPostsMapper.selectObjs(queryWrapper);
        List<Long> collect = idObj.stream().map(obj -> Long.parseLong(obj.toString()))
            .distinct()//去重
            .collect(Collectors.toList());



        return collect;
    }


    /**
     * 记录内容热度，并根据热度等级延长相关缓存的 TTL。
     * 延长的缓存包括：
     * 1. 详情页整页缓存 (knowpost:detail:{id})
     * 2. Feed 流内容片段缓存 (feed:item:{id})
     * 这样可以确保热点内容在 Feed 流中也不会轻易过期，避免 Feed 流回源。
     * @param id 内容 ID
     * @param detailPageKey 详情页缓存 Key
     */
    private void recordHotKeyAndExtendTtl(long id, String detailPageKey) {
        // 统一使用 knowpost:{id} 作为热度统计 Key
        String hotKeyId = "knowpost:" + id;
        hotKey.record(hotKeyId);

        int baseTtl = 60;
        int target = hotKey.ttlForPublic(baseTtl, hotKeyId);

        // 1. 延长详情页缓存
        Long detailTtl = redis.getExpire(detailPageKey);
        if (detailTtl < target) {
            redis.expire(detailPageKey, java.time.Duration.ofSeconds(target));
        }

        // 2. 延长 Feed 流内容片段缓存
        String itemKey = "feed:item:" + id;
        Long itemTtl = redis.getExpire(itemKey);
        if (itemTtl < target) {
            redis.expire(itemKey, java.time.Duration.ofSeconds(target));
        }
    }
    /**
     * 校验权限
     *
     * @param creatorId
     * @param id
     * @return
     */
    public KnowPosts validteKnowPostAuth(long creatorId, long id) {
        //        0.查询知文，校验权限
//            判断creatorId 是不是 这个知文的创作者
        // 1. 构建查询条件
        LambdaQueryWrapper<KnowPosts> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowPosts::getId, id)
                .eq(KnowPosts::getCreatorId, creatorId);
        KnowPosts knowPosts = this.getOne(wrapper);

        ThrowUtils.throwIf(knowPosts == null, ErrorCode.NOT_FOUND_ERROR);
//        判断权限
        ThrowUtils.throwIf(!knowPosts.getCreatorId().equals(creatorId), ErrorCode.NO_AUTH_ERROR);
        return knowPosts;
    }

    /**
     * 构建更新条件
     *
     * @param knowPosts
     * @return
     */
    public LambdaUpdateWrapper<KnowPosts> createLambdaUpdateWrapper(KnowPosts knowPosts) {
        return new LambdaUpdateWrapper<KnowPosts>()
                .eq(KnowPosts::getId, knowPosts.getId())
                .set(knowPosts.getTitle() != null, KnowPosts::getTitle, knowPosts.getTitle())
                .set(knowPosts.getDescription() != null, KnowPosts::getDescription, knowPosts.getDescription())
                .set(knowPosts.getTagId() != null, KnowPosts::getTagId, knowPosts.getTagId())
                .set(knowPosts.getTags() != null, KnowPosts::getTags, knowPosts.getTags())
                .set(knowPosts.getImgUrls() != null, KnowPosts::getImgUrls, knowPosts.getImgUrls())
                .set(knowPosts.getVisible() != null, KnowPosts::getVisible, knowPosts.getVisible())
                .set(knowPosts.getIsTop() != null, KnowPosts::getIsTop, knowPosts.getIsTop())
                .set(knowPosts.getStatus() != null, KnowPosts::getStatus, knowPosts.getStatus())
                .set(knowPosts.getType() != null, KnowPosts::getType, knowPosts.getType())
                .set(knowPosts.getContentUrl() != null, KnowPosts::getContentUrl, knowPosts.getContentUrl())
                .set(knowPosts.getContentObjectKey() != null, KnowPosts::getContentObjectKey, knowPosts.getContentObjectKey())
                .set(knowPosts.getContentEtag() != null, KnowPosts::getContentEtag, knowPosts.getContentEtag())
                .set(knowPosts.getContentSize() != null, KnowPosts::getContentSize, knowPosts.getContentSize())
                .set(knowPosts.getContentSha256() != null, KnowPosts::getContentSha256, knowPosts.getContentSha256())
                .set(knowPosts.getVideoUrl() != null, KnowPosts::getVideoUrl, knowPosts.getVideoUrl())
                .set(KnowPosts::getUpdateTime, new Date());
    }

}
