package zhiguang.nauy.knowpost.api.dto;

import java.util.List;

/**
 * 首页 Feed 分页响应DTO
 *
 * @param items   Feed列表
 * @param page    当前页码
 * @param size    每页大小
 * @param hasMore 是否还有更多数据
 */
public record FeedPageResponse(
        List<FeedItemResponse> items,
        int page,
        int size,
        boolean hasMore
) {
}