package zhiguang.nauy.knowpost.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * 知文详情响应DTO
 *
 * @param id             知文ID
 * @param title          标题
 * @param description    描述
 * @param contentUrl     内容URL（OSS地址）
 * @param images         图片URL列表
 * @param tags           标签列表
 * @param authorId       作者ID
 * @param authorAvatar   作者头像URL
 * @param authorNickname 作者昵称
 * @param authorTagJson  作者标签JSON字符串
 * @param likeCount      点赞数
 * @param favoriteCount  收藏数
 * @param liked          当前用户是否已点赞
 * @param faved          当前用户是否已收藏
 * @param isTop          是否置顶
 * @param visible        可见性（PRIVATE/PUBLIC/FOLLOWERS）
 * @param type           类型（DRAFT/PUBLISHED等）
 * @param publishTime    发布时间
 */
public record KnowPostDetailResponse(
        String id,
        String title,
        String description,
        String contentUrl,
        List<String> images,
        List<String> tags,
        String authorId,
        String authorAvatar,
        String authorNickname,
        String authorTagJson,
        Long likeCount,
        Long favoriteCount,
        Boolean liked,
        Boolean faved,
        Boolean isTop,
        String visible,
        String type,
        Instant publishTime
) {
}