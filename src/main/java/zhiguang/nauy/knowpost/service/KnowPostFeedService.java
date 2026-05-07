package zhiguang.nauy.knowpost.service;

import zhiguang.nauy.knowpost.api.dto.FeedPageResponse;

/**
 * @Description: // 类说明，在创建类时要填写
 * @ClassName: KnowPostFeedService    // 类名，会自动填充
 * @Author: oyy         // 创建者
 * @Date: 2026/4/21 15:04   // 时间
 * @Version: 1.0     // 版本
 */
public interface KnowPostFeedService {
    /**
     * 获取公开动态
     * @param page
     * @param size
     * @param userId
     * @return
     */
    FeedPageResponse getPublicFeed(int page, int size, Long userId);

    FeedPageResponse getMyPublished(long userId, int page, int size);
}
