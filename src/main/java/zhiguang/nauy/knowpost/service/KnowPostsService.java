package zhiguang.nauy.knowpost.service;

import zhiguang.nauy.knowpost.api.dto.KnowPostDetailResponse;
import zhiguang.nauy.knowpost.domain.KnowPostDetailRow;
import zhiguang.nauy.knowpost.domain.KnowPostFeedRow;
import zhiguang.nauy.knowpost.domain.KnowPosts;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author yuan
* @description 针对表【know_posts(知文主表-存储文章/帖子的核心元数据)】的数据库操作Service
* @createDate 2026-04-17 15:01:16
*/
// ... existing code ...
public interface KnowPostsService extends IService<KnowPosts> {
    /**
     * 创建草稿
     * <p>初始化一个知文草稿，设置默认状态为 DRAFT，返回草稿 ID</p>
     *
     * @param creatorId 创作者用户ID
     * @return 草稿的知文ID
     */
    long createDraft(long creatorId);

    /**
     * 确认内容上传完成
     * <p>在用户上传内容到 OSS 后，记录文件的元数据信息（对象键、ETag、大小、SHA256）</p>
     *
     * @param creatorId 创作者用户ID
     * @param id        知文ID
     * @param objectKey OSS 对象键
     * @param etag      文件 ETag（用于校验文件完整性）
     * @param size      文件大小（字节）
     * @param sha256    文件 SHA256 哈希值
     */
    void confirmContent(long creatorId, long id, String objectKey, String etag, Long size, String sha256);

    /**
     * 更新知文元数据
     * <p>更新知文的标题、标签、图片、可见性、置顶状态和描述等信息</p>
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
    void updateMetadata(long creatorId, long id, String title, Long tagId, List<String> tags, List<String> imgUrls, String visible, Boolean isTop, String description);

    /**
     * 发布知文
     * <p>将草稿状态的知文变更为已发布状态（PUBLISHED），设置发布时间</p>
     *
     * @param creatorId 创作者用户ID（用于权限校验）
     * @param id        知文ID
     */
    void publish(long creatorId, long id);

    /**
     * 更新置顶状态
     * <p>设置或取消知文的置顶状态</p>
     *
     * @param creatorId 创作者用户ID（用于权限校验）
     * @param id        知文ID
     * @param isTop     是否置顶（true-置顶, false-取消置顶）
     */
    void updateTop(long creatorId, long id, boolean isTop);

    /**
     * 更新可见性
     * <p>修改知文的可见范围（公开/私密/仅好友可见）</p>
     *
     * @param creatorId 创作者用户ID（用于权限校验）
     * @param id        知文ID
     * @param visible   可见性（PUBLIC-公开, PRIVATE-私密, FRIENDS-仅好友可见）
     */
    void updateVisibility(long creatorId, long id, String visible);

    /**
     * 删除知文
     * <p>逻辑删除知文（设置 deleted=1），不会物理删除数据库记录</p>
     *
     * @param creatorId 创作者用户ID（用于权限校验）
     * @param id        知文ID
     */
    void delete(long creatorId, long id);

    /**
     * 获取知文详情
     * <p>根据知文ID查询详细信息，包括作者信息、点赞数、收藏数、评论数等</p>
     * <p>如果传入当前用户ID，会同时返回该用户对知文的互动状态（是否点赞、是否收藏、是否关注作者）</p>
     *
     * @param id                    知文ID
     * @param currentUserIdNullable 当前登录用户ID（可为 null，表示未登录用户）
     * @return 知文详情响应对象
     */
    KnowPostDetailResponse getDetail(long id, Long currentUserIdNullable);



    KnowPostDetailRow toKnowPostDetailRow(KnowPosts knowPosts);

    // ... existing code ...
    KnowPostFeedRow toKnowPostFeedRow(KnowPosts knowPosts);
}

