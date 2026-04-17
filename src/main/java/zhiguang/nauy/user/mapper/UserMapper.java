package zhiguang.nauy.user.mapper;

import org.apache.ibatis.annotations.Mapper;
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

}




