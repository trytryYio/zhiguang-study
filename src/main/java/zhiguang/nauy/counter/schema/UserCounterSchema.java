package zhiguang.nauy.counter.schema;

import java.util.Map;
import java.util.Set;

/**
 * 用户维度计数指标的索引映射和SDS结构常量。
 *
 * <p>用户维度计数指标：</p>
 * <ul>
 *   <li>followings - 关注数</li>
 *   <li>followers - 粉丝数</li>
 *   <li>posts - 发帖数</li>
 *   <li>likesReceived - 获赞数</li>
 *   <li>favsReceived - 获收藏数</li>
 * </ul>
 *
 * <p>SDS结构：5个指标 × 4字节 = 20字节</p>
 */
public final class UserCounterSchema {
    public static final String SCHEMA_ID = "V1";
    public static final int BYTES_PER_METRIC = 4;
    public static final int SCHEMA_LEN = 5;

    // 指标索引
    public static final int IDX_FOLLOWINGS = 0;
    public static final int IDX_FOLLOWERS = 1;
    public static final int IDX_POSTS = 2;
    public static final int IDX_LIKES_RECEIVED = 3;
    public static final int IDX_FAVS_RECEIVED = 4;

    // 指标名称 → 索引映射
    public static final Map<String, Integer> NAME_TO_IDX = Map.of(
        "followings", IDX_FOLLOWINGS,
        "followers", IDX_FOLLOWERS,
        "posts", IDX_POSTS,
        "likesReceived", IDX_LIKES_RECEIVED,
        "favsReceived", IDX_FAVS_RECEIVED
    );

    public static final Set<String> SUPPORTED_METRICS = NAME_TO_IDX.keySet();

    private UserCounterSchema() {}
}
