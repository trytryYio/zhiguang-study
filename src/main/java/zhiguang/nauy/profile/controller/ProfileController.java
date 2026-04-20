package zhiguang.nauy.profile.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import zhiguang.nauy.auth.token.JwtService;
import zhiguang.nauy.common.BaseResponse;
import zhiguang.nauy.common.ResultUtils;
import zhiguang.nauy.exception.ErrorCode;
import zhiguang.nauy.exception.ThrowUtils;
import zhiguang.nauy.profile.dto.ProfilePatchRequest;
import zhiguang.nauy.profile.dto.ProfileResponse;
import zhiguang.nauy.profile.service.ProfileService;
import zhiguang.nauy.storage.OssStorageService;

/**
 * @Description: // 类说明，在创建类时要填写
 * @ClassName: ProfileController // 类名，会自动填充
 * @Author: oyy // 创建者
 * @Date: 2026/4/17 15:28 // 时间
 * @Version: 1.0 // 版本
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Validated
public class ProfileController {
        @Resource
        private OssStorageService ossStorageService;
        @Resource
        private ProfileService profileService;
        @Resource
        private JwtService jwtService;

        /**
         * 更新用户资料
         *
         * @param jwt
         * @param request
         * @return
         */
        @PatchMapping
        public BaseResponse<ProfileResponse> patch(@AuthenticationPrincipal Jwt jwt,
                        @Valid @RequestBody ProfilePatchRequest request) {
                // 从 JWT 中提取用户 ID。
                long userId = jwtService.extractUserId(jwt);

                // 更新这个用户的资料
                return ResultUtils.success(profileService.updateProfile(userId, request));
        }

        /**
         * 上传头像图片
         *
         * @param jwt
         * @param file
         * @return
         */
        @PostMapping("/avatar")
        public BaseResponse<ProfileResponse> uploadAvatar(@AuthenticationPrincipal Jwt jwt,
                        @RequestPart("file") MultipartFile file) {
                // 0.校验参数
                if (file.isEmpty()) {
                        ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "上传文件不能为空");

                }
                // 文件非空

                // 1.从 JWT 中提取用户 ID。
                long userId = jwtService.extractUserId(jwt);
                // 2.上传到阿里云存储
                String uploadAvatar = ossStorageService.uploadAvatar(userId, file);
                ThrowUtils.throwIf(uploadAvatar == null, ErrorCode.SYSTEM_ERROR, "上传头像失败");
                // 从存储中获取图片的 URL
                ProfileResponse profileResponse = profileService.updateAvatar(userId, uploadAvatar);
                // 设置图片的地址到数据库中
                return ResultUtils.success(profileResponse);
        }
}
