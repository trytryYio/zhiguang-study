package zhiguang.nauy.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import zhiguang.nauy.user.domain.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author yuan
 * @description 针对表【users】的数据库操作Mapper
 * @createDate 2026-04-13 18:44:29
 * @Entity generator.domain.Users
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 根据知光号查询用户是否存在
     *
     * @param zgId
     * @param userId
     * @return
     */
    @Select("SELECT EXISTS (SELECT 1 FROM users WHERE zg_id = #{zgId} AND id != #{userId})")
    boolean existsByZgIdExceptId(String zgId, Long userId);
}




