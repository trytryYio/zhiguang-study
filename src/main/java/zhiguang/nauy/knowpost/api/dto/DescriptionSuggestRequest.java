package zhiguang.nauy.knowpost.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * AI描述生成请求DTO
 *
 * @param content 需要生成描述的原始内容
 */
public record DescriptionSuggestRequest(
        @NotBlank(message = "content 不能为空") String content
) {
}