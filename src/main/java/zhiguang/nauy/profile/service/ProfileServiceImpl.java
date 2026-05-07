package zhiguang.nauy.profile.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zhiguang.nauy.exception.ErrorCode;
import zhiguang.nauy.exception.ThrowUtils;
import zhiguang.nauy.profile.dto.ProfilePatchRequest;
import zhiguang.nauy.profile.dto.ProfileResponse;
import zhiguang.nauy.storage.OssProperties;
import zhiguang.nauy.user.domain.User;
import zhiguang.nauy.user.mapper.UserMapper;
import zhiguang.nauy.user.service.UserService;

import java.beans.ConstructorProperties;

/**
 * @Description: // 类说明，在创建类时要填写
 * @ClassName: ProfileServiceImpl    // 类名，会自动填充
 * @Author: oyy         // 创建者
 * @Date: 2026/4/17 15:30   // 时间
 * @Version: 1.0     // 版本
 */
@Service
@AllArgsConstructor
public class ProfileServiceImpl implements ProfileService{
    // 属性
    private final OssProperties props;

    @Resource
    private UserService userService;
    @Resource
    private UserMapper userMapper;

    /**
     * 更新用户资料
     * @param userId
     * @param request
     * @return
     */
    @Transactional
    @Override
    public ProfileResponse updateProfile(long userId, ProfilePatchRequest request) {
        // 读取当前用户，作为更新与唯一性校验的基准
        User current = userService.findById(userId);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_FOUND);
        // 至少要提交一个字段，否则属于无效请求
        boolean hasAnyField = request.nickname() != null || request.bio() != null || request.gender() != null
                || request.birthday() != null || request.zgId() != null || request.school() != null
                || request.tagJson() != null;
        ThrowUtils.throwIf(!hasAnyField, ErrorCode.BAD_REQUEST,"未提交任何更新字段");

// 知光号唯一性校验：仅在提交且非空时检查（排除自己）

        // 知光号唯一性校验：仅在提交且非空时检查（排除自己）
//        查有没有**其他人**（excludeId 除外）已经用了这个 zgId。
        if (request.zgId() != null && !request.zgId().isBlank()) {
            boolean exists = userMapper.existsByZgIdExceptId(request.zgId(), current.getId());
            ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "知光号已存在");
        }

        // 仅写入非空字段，避免把未提交字段覆盖成 null

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                .set(StrUtil.isNotBlank(request.nickname()), User::getNickname, request.nickname())
                .set(StrUtil.isNotBlank(request.bio()), User::getBio, request.bio())
                .set(StrUtil.isNotBlank(request.gender()), User::getGender, request.gender())
                .set(request.birthday() != null, User::getBirthday, request.birthday())
                .set(StrUtil.isNotBlank(request.zgId()), User::getZgId, request.zgId())
                .set(StrUtil.isNotBlank(request.school()), User::getSchool, request.school())
                .set(request.tagJson() != null, User::getTagsJson, request.tagJson())
                .set(User::getUpdatedAt, new java.util.Date());
        boolean update = userService.update(updateWrapper);
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR);
        // 更新后回读，保证返回数据为最新快照
        User user = userService.findById(userId);
        return user.convertToProfileResponse();
    }

    /**
     * 更新用户头像
     * @param userId
     * @param uploadAvatar
     * @return
     */
    @Override
    public ProfileResponse updateAvatar(long userId, String uploadAvatar) {
//      0.判空
        ThrowUtils.throwIf(StrUtil.isBlank(uploadAvatar), ErrorCode.BAD_REQUEST);
        User currentUser = userService.findById(userId);
        ThrowUtils.throwIf(currentUser == null, ErrorCode.NOT_FOUND);
        User user = new User();
        BeanUtils.copyProperties(currentUser, user);
        user.setId(userId);
        user.setAvatar(uploadAvatar);
        User update = userService.update(user);
        return update.convertToProfileResponse();
    }

    @Override
    public String publicUrl(String objectKey) {
        // 构建对象存储的公开访问URL
        // 优先使用自定义域名，否则使用默认Bucket域名格式
        if (props.getPublicDomain() != null && !props.getPublicDomain().isBlank()) {
            return props.getPublicDomain().replaceAll("/$", "") + "/" + objectKey;
        }
        //默认域名
        return "https://" + props.getBucket() + "." + props.getEndpoint() + "/" + objectKey;

    }
}
