package zhiguang.nauy.profile.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zhiguang.nauy.auth.token.JwtService;
import zhiguang.nauy.common.BaseResponse;
import zhiguang.nauy.common.ResultUtils;
import zhiguang.nauy.profile.dto.ProfilePatchRequest;
import zhiguang.nauy.profile.dto.ProfileResponse;
import zhiguang.nauy.profile.service.ProfileService;

/**
 * @Description: // 类说明，在创建类时要填写
 * @ClassName: ProfileController    // 类名，会自动填充
 * @Author: oyy         // 创建者
 * @Date: 2026/4/17 15:28   // 时间
 * @Version: 1.0     // 版本
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Validated
public class ProfileController {
    @Resource
    private ProfileService profileService;
    @Resource
    private JwtService jwtService;

    @PatchMapping
    public BaseResponse<ProfileResponse> patch(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody ProfilePatchRequest request) {
//        从 JWT 中提取用户 ID。
        long userId = jwtService.extractUserId(jwt);

//        更新这个用户的资料
        return ResultUtils.success(profileService.updateProfile(userId, request));
    }


}
