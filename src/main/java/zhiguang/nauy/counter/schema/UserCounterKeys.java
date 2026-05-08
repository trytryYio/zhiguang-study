package zhiguang.nauy.counter.schema;

import lombok.NoArgsConstructor;

/**
 * 用户维度计数键生成工具。
 *
 * 用于存储用户的统计信息：
 * - 关注数（followings）
 * - 粉丝数（followers）
 * - 发帖数（posts）
 * - 获赞数（likesReceived）
 * - 获收藏数（favsReceived）
 */
@NoArgsConstructor
public class UserCounterKeys {
    /**
     * 用户维度SDS键
     * @param userId 用户ID
     * @return 键名，如 ucnt:123
     */
    public static String sdsKey(long userId) {
        return "ucnt:" + userId;
    }



}
