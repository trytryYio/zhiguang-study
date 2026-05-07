package zhiguang.nauy.knowpost.api.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 帖子元数据更新请求DTO（部分字段可选）
 *
 * @param title       标题
 * @param tagId       标签ID
 * @param tags        标签列表（最多20个）
 * @param imgUrls     图片URL列表（最多20个）
 * @param visible     可见性（PRIVATE/PUBLIC/FOLLOWERS）
 * @param isTop       是否置顶
 * @param description 描述
 */
public record KnowPostPatchRequest(
        String title,
        Long tagId,
        @Size(max = 20) List<String> tags,
        @Size(max = 20) List<String> imgUrls,
        String visible,
        Boolean isTop,
        String description
) {
}