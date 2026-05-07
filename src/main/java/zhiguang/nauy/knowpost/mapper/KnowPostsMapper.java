package zhiguang.nauy.knowpost.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import zhiguang.nauy.knowpost.domain.KnowPostFeedRow;
import zhiguang.nauy.knowpost.domain.KnowPosts;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author yuan
* @description 针对表【know_posts(知文主表-存储文章/帖子的核心元数据)】的数据库操作Mapper
* @createDate 2026-04-17 15:01:16
* @Entity knowpost.domain.KnowPosts
*/
@Mapper
public interface KnowPostsMapper extends BaseMapper<KnowPosts> {

    @Select("SELECT p.id, p.title, p.description, p.tags, p.img_urls AS imgUrls, " +
        "u.avatar AS authorAvatar, u.nickname AS authorNickname, u.tags_json AS authorTagJson, " +
        "p.publish_time AS publishTime, p.is_top AS isTop " +
        "FROM know_posts p " +
        "JOIN users u ON p.creator_id = u.id " +
        "WHERE p.status = 'published' AND p.visible = 'public' " +
        "ORDER BY p.publish_time DESC " +
        "LIMIT #{limit} OFFSET #{offset}")
    // OFFSET #{offset} —— “跳过多少条”
    //. LIMIT #{limit} —— “拿多少条”
    List<KnowPostFeedRow> listFeedPublic(@Param("limit")int i,@Param("offset") int offset);

    @Select("SELECT p.id, p.title, p.description, p.tags, p.img_urls AS imgUrls, " +
        "u.avatar AS authorAvatar, u.nickname AS authorNickname, u.tags_json AS authorTagJson, " +
        "p.publish_time AS publishTime, p.is_top AS isTop " +
        "from know_posts p join users u on p.creator_id = u.id" +
        " where p.creator_id=#{creatorId} and p.status ='published'" +
        " order by p.is_top desc ,p.publish_time desc" +
        " limit #{limit} offset #{offset}")
    List<KnowPostFeedRow> listMyPublished(@Param("creatorId") long userId, @Param("limit") int limit, @Param("offset") int offset);
}




