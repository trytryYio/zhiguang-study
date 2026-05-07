package zhiguang.nauy.relation.service;

import zhiguang.nauy.profile.dto.ProfileResponse;

import java.util.List;
import java.util.Map;

/**
 * @Description: // 类说明，在创建类时要填写
 * @ClassName: RelationService    // 类名，会自动填充
 * @Author: oyy         // 创建者
 * @Date: 2026/4/28 11:22   // 时间
 * @Version: 1.0     // 版本
 */
public interface RelationService {
    /**
     * 关注
     * @param uid
     * @param toUserId
     * @return
     */
    Boolean follow(long uid, long toUserId);

    /**
     * 取消关注
     * @param uid
     * @param toUserId
     * @return
     */
    Boolean unfollow(long uid, long toUserId);

    /**
     * 关注状态
     * @param uid
     * @param toUserId
     * @return
     */
    Map<String, Boolean> relationStatus(long uid, long toUserId);

    /**
     * 获取关注列表
     * @param userId
     * @param l
     * @param max
     * @param cursor
     * @return
     */
    List<ProfileResponse> followingProfiles(long userId, int l, int max, Long cursor);

    /**
     * 获取粉丝列表
     * @param userId
     * @param l
     * @param max
     * @param cursor
     * @return
     */
    List<ProfileResponse> followersProfiles(long userId, int l, int max, Long cursor);
    /**
     * SDS 由 5 个 4 字节段组成（关注/粉丝/发文/获赞/获藏）
     * @param userId
     * @return
     */
    Map<String, Long> counter(long userId);
}
