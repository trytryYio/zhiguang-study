package zhiguang.nauy.user.service.impl;

import java.time.Instant;
import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.internal.ObjectUtil;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import zhiguang.nauy.exception.ErrorCode;
import zhiguang.nauy.exception.ThrowUtils;
import zhiguang.nauy.user.domain.User;
import zhiguang.nauy.user.service.UserService;
import zhiguang.nauy.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * @author yuan
 * @description 针对表【users】的数据库操作Service实现
 * @createDate 2026-04-13 18:44:29
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public User findByPhone(String phone) {
        ThrowUtils.throwIf(StringUtil.isEmpty(phone), ErrorCode.PARAMS_ERROR, "手机号不能为空");
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        User one = getOne(queryWrapper);
        ThrowUtils.throwIf(ObjectUtils.isEmpty(one), ErrorCode.NOT_FOUND_ERROR, "该用户不存在");
        return one;
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        ThrowUtils.throwIf(StringUtil.isEmpty(email), ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<User>().eq(User::getEmail, email);
        User one = getOne(lambdaQueryWrapper);
        ThrowUtils.throwIf(ObjectUtils.isEmpty(one), ErrorCode.NOT_FOUND_ERROR, "该用户不存在");
        return one;
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能小于0");
        User user = this.getById(id);
        return user;
    }

    @Override

    @Transactional(readOnly = true)
    public boolean existsByPhone(String phone) {
        ThrowUtils.throwIf(StringUtil.isEmpty(phone), ErrorCode.PARAMS_ERROR, "手机号不能为空");
        boolean exists = userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        return exists;
    }

    @Override

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        ThrowUtils.throwIf(StringUtil.isEmpty(email), ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        boolean exists = userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getEmail, email));

        return exists;
    }


    @Override
    @Transactional
    public User createUser(User user) {
        // ✅ 直接设置时间戳
        Instant now = Instant.now();
        user.setCreatedAt(Date.from(now));
        user.setUpdatedAt(Date.from(now));

        // ✅ 直接插入原对象
        userMapper.insert(user);
        return user;
    }

}

    @Transactional
    @Override
    public void updatePassword(User user) {
        User existsUser = this.getById(user.getId());
        ThrowUtils.throwIf(existsUser == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        this.lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getPasswordHash, user.getPasswordHash())
                .set(User::getUpdatedAt, new Date())
                .update();
    }

    @Override
    @Transactional
    public void updateUser(User user) {
        User existsUser = this.getById(user.getId());
        ThrowUtils.throwIf(existsUser == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        this.lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(user.getPhone() != null, User::getPhone, user.getPhone())
                .set(user.getEmail() != null, User::getEmail, user.getEmail())
                .set(user.getPasswordHash() != null, User::getPasswordHash, user.getPasswordHash())
                .set(user.getNickname() != null, User::getNickname, user.getNickname())
                .set(user.getAvatar() != null, User::getAvatar, user.getAvatar())
                .set(user.getBio() != null, User::getBio, user.getBio())
                .set(user.getZgId() != null, User::getZgId, user.getZgId())
                .set(user.getGender() != null, User::getGender, user.getGender())
                .set(user.getBirthday() != null, User::getBirthday, user.getBirthday())
                .set(user.getSchool() != null, User::getSchool, user.getSchool())
                .set(user.getTagsJson() != null, User::getTagsJson, user.getTagsJson())
                .set(User::getUpdatedAt, new Date())
                .update();
    }


}




