package zhiguang.nauy.knowpost.api.dto;

/**
 * AI描述生成响应DTO
 *
 * @param description AI生成的描述文本
 */
public record DescriptionSuggestResponse(
        String description
) {
}