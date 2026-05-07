package zhiguang.nauy.knowpost.api.dto;

import java.util.List;

/**
 * 首页 Feed 单条记录响应DTO
 *
 * @param id             知文ID
 * @param title          标题
 * @param description    描述
 * @param coverImage     封面图片URL
 * @param tags           标签列表
 * @param authorAvatar   作者头像URL
 * @param authorNickname 作者昵称
 * @param tagJson        标签JSON字符串
 * @param likeCount      点赞数
 * @param favoriteCount  收藏数
 * @param liked          当前用户是否已点赞
 * @param faved          当前用户是否已收藏
 * @param isTop          是否置顶
 */
public record FeedItemResponse(
        String id,
        String title,
        String description,
        String coverImage,
        List<String> tags,
        String authorAvatar,
        String authorNickname,
        String tagJson,
        Long likeCount,
        Long favoriteCount,
        Boolean liked,
        Boolean faved,
        Boolean isTop
) {
}