package zhiguang.nauy.counter.schema;

import lombok.NoArgsConstructor;

/**
 * Redis Key 生成工具。
 *
 * <p>三层计数架构的键模式：
 * <ol>
 *   <li>Fact层（事实层）：分片位图，存储用户行为（谁点了赞）</li>
 *   <li>Aggregation层（聚合层）：Redis Hash，临时存储增量</li>
 *   <li>Summary层（汇总层）：SDS二进制，快速读取总数</li>
 * </ol>
 */
@NoArgsConstructor
public class CounterKeys {
    /**
     * Fact层键（分片位图）
     * 存储：位图，第N位=1表示用户ID=N点赞了
     * 用途：判断用户是否点赞过、去重计数
     *
     * @param metric     计数指标名称，例如 "like"（点赞）、"fav"（收藏）
     * @param entityType 实体类型，例如 "knowpost"（知识帖子）
     * @param entityId   实体的唯一标识符，例如帖子 ID "123"
     * @param chunk      位图分片编号，由用户 ID 通过 {(long)} 计算得出
     * @return 格式化的 Redis Key，例如 "bm:like:knowpost:123:3"
     */
    public static String bitmapKey(String metric, String entityType, String entityId, long chunk) {
        return String.format("bm:%s:%s:%s:%d", metric, entityType, entityId, chunk);
    }

    /**
     * Aggregation层键（聚合增量桶）
     * 存储：Redis Hash，field=metric, value=增量
     * 用途：临时存储增量，定时刷写到Summary层
     *
     * @param entityType 实体类型，例如 "knowpost"（知识帖子）
     * @param entityId   实体的唯一标识符，例如帖子 ID "123"
     * @return 格式化的 Redis Key，例如 "agg:V1:knowpost:123"
     */
    public static String aggKey(String entityType, String entityId) {
        return String.format("agg:%s:%s:%s", CounterSchema.SCHEMA_ID, entityType, entityId);
    }

    /**
     * Summary层键（SDS固定结构计数）
     * 存储：二进制数据，每个指标4字节Int32
     * 用途：快速读取实体（如帖子）的总点赞数、总收藏数
     *
     * @param entityType 实体类型（如knowpost）
     * @param entityId   实体ID
     * @return 键名，如 cnt:v1:knowpost:123
     */
    public static String sdsKey(String entityType, String entityId) {
        return String.format("cnt:%s:%s:%s", CounterSchema.SCHEMA_ID, entityType, entityId);
    }
}
