package zhiguang.nauy.counter.schema;

import java.util.Map;
import java.util.Set;

/**
 * 定义计数指标的索引映射和SDS结构常量。
 *
 * <p>计数指标（Metric）：就是你要统计的东西，比如：
 * <ul>
 *   <li>like - 点赞数</li>
 *   <li>fav - 收藏数</li>
 *   <li>comment - 评论数（预留）</li>
 *   <li>read - 阅读数（预留）</li>
 * </ul>
 *
 * <p>索引映射（Index Mapping）：给每个指标分配一个数字ID，方便在二进制数据中定位。
 *
 * <p>SDS（Simple Data Structure）结构：SDS是一种固定结构的二进制数据，用来存储多个指标的计数值。
 * 假设有5个指标，每个占4字节（Int32）：
 * <pre>
 * ┌─────────┬─────────┬─────────┬─────────┬─────────┐
 * │ 索引0    │ 索引1    │ 索引2    │ 索引3    │ 索引4   │
 * │ (read)   │ (like)  │ (fav)   │(comment)│(repost) │
 * │ 4字节     │ 4字节    │ 4字节    │ 4字节    │ 4字节   │
 * └─────────┴─────────┴─────────┴─────────┴─────────┘
 *   0-3字节   4-7字节   8-11字节  12-15字节 16-19字节
 * </pre>
 *
 * @author nauy
 */
public final class CounterSchema {
    // Schema 版本标识
    public static final String SCHEMA_ID = "V1";

    // 每个指标占用的字节数（4字节=Int32，范围大约 +-21亿）
    public static final int BYTES_PER_METRIC = 4;

    // 预留的指标位数（目前只用like和fav，其他预留）
    public static final int SCHEMA_LEN = 5;

    // 指标索引映射（用于SDS二进制结构中的偏移计算）
    public static final int IDX_READ = 0;    // 阅读数（预留）
    public static final int IDX_LIKE = 1;    // 点赞数
    public static final int IDX_FAV = 2;     // 收藏数
    public static final int IDX_COMMENT = 3; // 评论数（预留）
    public static final int IDX_REPOST = 4;  // 转发数（预留）

    // 指标名称 → 索引的映射（用于事件处理时查找）
    public static final Map<String, Integer> NAME_TO_IDX = Map.of(
        "like", IDX_LIKE,
        "fav", IDX_FAV
    );

    // 对外暴露支持的指标集合（API层校验用）
    public static final Set<String> SUPPORTED_METRICS = NAME_TO_IDX.keySet();

    // 私有构造函数，防止实例化
    private CounterSchema() {}
}
