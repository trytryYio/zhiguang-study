package zhiguang.nauy.knowpost.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 帖子置顶状态更新请求DTO
 *
 * @param isTop 是否置顶
 */
public record KnowPostTopPatchRequest(
        @NotNull Boolean isTop
) {
}