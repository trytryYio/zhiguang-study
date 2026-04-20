package zhiguang.nauy.knowpost.mapper;

import org.apache.ibatis.annotations.Mapper;
import zhiguang.nauy.knowpost.domain.KnowPosts;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author yuan
* @description 针对表【know_posts(知文主表-存储文章/帖子的核心元数据)】的数据库操作Mapper
* @createDate 2026-04-17 15:01:16
* @Entity knowpost.domain.KnowPosts
*/
@Mapper
public interface KnowPostsMapper extends BaseMapper<KnowPosts> {

}




