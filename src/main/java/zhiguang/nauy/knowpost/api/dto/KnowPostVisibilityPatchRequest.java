package zhiguang.nauy.knowpost.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 帖子可见性更新请求DTO
 *
 * @param visible 可见性（PRIVATE/PUBLIC/FOLLOWERS）
 */
public record KnowPostVisibilityPatchRequest(
        @NotBlank String visible
) {
}