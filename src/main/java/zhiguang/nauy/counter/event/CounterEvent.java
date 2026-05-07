package zhiguang.nauy.counter.event;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 计数事件。
 *
 * <p>定义了计数事件结构，包含实体类型、实体ID、指标类型、分片索引、用户ID和变化量。</p>
 */

@Data
@AllArgsConstructor

public class CounterEvent {
    /**
     * 实体类型 (如: post, comment)
     */
    private String entityType;

    /**
     * 实体ID
     */
    private String entityId;

    /**
     * 指标类型 (如: like, favorite)
     */
    private String metric;

    /**
     * 分片索引
     */
    private int idx;

    /**
     * 用户ID
     */
    private long userId;

    /**
     * 变化量 (1 或 -1)
     */
    private int delta;

    // 静态工厂方法（可选，方便创建对象）
    public static CounterEvent of(String entityType, String entityId, String metric, int idx, long userId, int delta) {
        return new CounterEvent(entityType, entityId, metric, idx, userId, delta);
    }


}
