package zhiguang.nauy.profile.service;

import zhiguang.nauy.common.BaseResponse;
import zhiguang.nauy.profile.dto.ProfilePatchRequest;
import zhiguang.nauy.profile.dto.ProfileResponse;

/**
 * @Description: // 类说明，在创建类时要填写
 * @ClassName: ProfileService    // 类名，会自动填充
 * @Author: oyy         // 创建者
 * @Date: 2026/4/17 15:30   // 时间
 * @Version: 1.0     // 版本
 */
public interface ProfileService {
    /**
     * 更新用户资料
     * @param userId
     * @param request
     * @return
     */
    ProfileResponse updateProfile(long userId, ProfilePatchRequest request);

    /**
     * 更新用户头像
     * @param userId
     * @param uploadAvatar
     * @return
     */

    ProfileResponse updateAvatar(long userId, String uploadAvatar);

    String publicUrl(String objectKey);
}
