package zhiguang.nauy.user.service;

import org.springframework.transaction.annotation.Transactional;
import zhiguang.nauy.user.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * @author yuan
 * @description 针对表【users】的数据库操作Service
 * @createDate 2026-04-13 18:44:29
 */
public interface UserService extends IService<User> {
    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息，如果不存在则返回空 User
     */
    User findByPhone(String phone);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱地址
     * @return 用户信息，如果不存在则返回空 User
     */
    User findByEmail(String email);

    /**
     * 根据用户ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息，如果不存在则返回空 User
     */
    User findById(long id);

    /**
     * 检查手机号是否已存在
     *
     * @param phone 手机号
     * @return 如果存在返回 true，否则返回 false
     */
    boolean existsByPhone(String phone);

    /**
     * 检查邮箱是否已存在
     *
     * @param email 邮箱地址
     * @return 如果存在返回 true，否则返回 false
     */
    boolean existsByEmail(String email);

    /**
     * 创建新用户
     *
     * @param user 待创建的用户对象
     * @return 创建成功后的用户对象
     */
    User createUser(User user);

    /**
     * 更新用户密码
     *
     * @param user 包含新密码的用户对象
     */
    void updatePassword(User user);

    /**
     * 更新用户信息
     *
     * @param user
     */
    void updateUser(User user);

    /**
     * 更新用户信息并且返回 User
     * @param user
     * @return
     */

    @Transactional
    User update(User user);
}
