package zhiguang.nauy.counter.service;

import java.util.List;
import java.util.Map;

/**
 * 内容实体计数服务接口。
 *
 * <p>定义点赞、收藏、查询计数等操作。</p>
 */
public interface CounterService {

    /**
     * 点赞：仅在之前未点赞时置位并 +1。
     * @return 是否发生状态变化（true 表示这次操作生效）
     */
    boolean like(String entityType, String entityId, long userId);

    /**
     * 取消点赞：仅在之前已点赞时清位并 -1。
     * @return 是否发生状态变化（true 表示这次操作生效）
     */
    boolean unlike(String entityType, String entityId, long userId);

    /**
     * 收藏：仅在之前未收藏时置位并 +1。
     */
    boolean fav(String entityType, String entityId, long userId);

    /**
     * 取消收藏：仅在之前已收藏时清位并 -1。
     */
    boolean unfav(String entityType, String entityId, long userId);

    /**
     * 获取单个实体的计数汇总。
     * <p>从 Summary 层（SDS）读取指定指标的计数值。若 SDS 结构缺失或损坏，会自动触发基于 Fact 层（位图）的重建机制。</p>
     *
     * @param entityType 实体类型（如 "knowpost"）
     * @param entityId   实体ID（如帖子ID "123"）
     * @param metrics    需要查询的指标名称列表（如 ["like", "fav"]）
     * @return 指标名称到计数值的映射（如 {"like": 100, "fav": 50}）
     */
    Map<String, Long> getCounts(String entityType, String entityId, List<String> metrics);

    /**
     * 批量获取多个实体的计数汇总。
     * <p>利用 Redis 管道（Pipeline）一次性读取多个实体的 SDS 数据，显著降低网络 RTT，适用于 Feed 流等场景。</p>
     *
     * @param entityType 实体类型（如 "knowpost"）
     * @param entityIds  实体ID列表（如 ["123", "456"]）
     * @param metrics    需要查询的指标名称列表（如 ["like", "fav"]）
     * @return 嵌套映射：外层 Key 为 entityId，内层 Key 为 metric，Value 为计数值
     */
    Map<String, Map<String, Long>> getCountsBatch(String entityType, List<String> entityIds, List<String> metrics);

    /**
     * 判断是否点赞/收藏（位图）。
     */
    boolean isLiked(String entityType, String entityId, long userId);
    boolean isFaved(String entityType, String entityId, long userId);
}
