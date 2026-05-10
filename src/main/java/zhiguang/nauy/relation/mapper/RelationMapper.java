package zhiguang.nauy.relation.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 关系表数据访问层。
 * 职责：维护关注/粉丝关系的插入与逻辑取消，分页读取与行数据回填，统计有效关系计数。
 */
@Mapper
public interface RelationMapper {
    /**
     * 插入关注关系。
     * @param id 主键ID
     * @param fromUserId 发起关注的用户ID
     * @param toUserId 被关注的用户ID
     * @param relStatus 关系状态
     * @return 影响行数
     */
    @Insert("INSERT INTO following(id, from_user_id, to_user_id, rel_status, created_at, updated_at) " +
        "VALUES(#{id}, #{fromUserId}, #{toUserId}, #{relStatus}, NOW(3), NOW(3)) " +
        "ON DUPLICATE KEY UPDATE rel_status=VALUES(rel_status), updated_at=VALUES(updated_at)")
    int insertFollowing(@Param("id") Long id,
                        @Param("fromUserId") Long fromUserId,
                        @Param("toUserId") Long toUserId,
                        @Param("relStatus") Integer relStatus);


    /**
     * 取消关注关系。
     * @param uid
     * @param toUserId
     * @return
     */
    @Update("   UPDATE following SET rel_status=0, updated_at=NOW(3)\n" +
        "        WHERE from_user_id=#{fromUserId} AND to_user_id=#{toUserId} ")
    int cancelFollowing(@Param("fromUserId") long uid, @Param("toUserId") long toUserId);

    /**
     * 判断关系是否存在。
     * @param fromUserId
     * @param toUserId
     * @return
     */
    @Select("SELECT EXISTS(SELECT 1 FROM following WHERE from_user_id=#{fromUserId} AND to_user_id=#{toUserId} AND rel_status=1)")
    boolean existsFollowing(@Param("fromUserId") long fromUserId, @Param("toUserId") long toUserId);
    /**
     * 获取关注列表。 也就是我的关注
     * @param userId
     * @param limit
     * @param offset
     * @return
     */
    @MapKey("toUserId")
    Map<Long, Map<String, Object>> listFollowingRows(@Param("fromUserId")long userId, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 获取关注列表。 也就是我的关注
     * @param userId
     * @param limit
     * @param offset
     * @return
     */
    @Select("SELECT to_user_id FROM following WHERE from_user_id=#{userId} LIMIT #{limit} OFFSET #{offset}")
    List<Long> following(@Param("userId") long userId, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 获取粉丝列表。
     * @param userId
     * @param limit
     * @param offset
     * @return
     */

    @Select("select from_user_id  from follower where to_user_id = #{userId} limit #{limit} offset #{offset};")
    List<Long> followers(@Param("userId") long userId, @Param("limit") int limit, @Param("offset") int offset);


    /**
     * 获取粉丝列表。
     * @param userId
     * @param limit
     * @param offset
     * @return
     */
    @MapKey("toUserId")
    Map<Long, Map<String, Object>> listFollowerRows(@Param("userId") long userId, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 统计关注列表
     * @param userId
     * @return
     */
    @Select("  SELECT COUNT(*) FROM following WHERE from_user_id=#{userId} AND rel_status=1")
    int countFollowingActive(@Param("userId") long userId);

    /**
     * 统计粉丝列表
     * @param userId
     * @return
     */
    @Select("  SELECT COUNT(*) FROM follower WHERE to_user_id=#{userId} AND rel_status=1")
    int countFollowerActive(long userId);





    /**
     * 写入粉丝关系（异步消费时使用）。
     * @param id 主键ID
     * @param toUserId 被关注者ID
     * @param fromUserId 关注者ID
     * @param relStatus 关系状态
     * @return 影响行数
     */
    @Insert("INSERT INTO follower(id, to_user_id, from_user_id, rel_status, created_at, updated_at) " +
        "VALUES(#{id}, #{toUserId}, #{fromUserId}, #{relStatus}, NOW(3), NOW(3)) " +
        "ON DUPLICATE KEY UPDATE rel_status=VALUES(rel_status), updated_at=VALUES(updated_at)")
    int insertFollower(@Param("id") Long id,
                       @Param("toUserId") Long toUserId,
                       @Param("fromUserId") Long fromUserId,
                       @Param("relStatus") Integer relStatus);

    /**
     * 取消粉丝关系（异步消费时使用）。
     * @param toUserId 被关注者ID
     * @param fromUserId 关注者ID
     * @return 影响行数
     */
    @Update("UPDATE follower SET rel_status=0, updated_at=NOW(3) " +
        "WHERE to_user_id=#{toUserId} AND from_user_id=#{fromUserId}")
    int cancelFollower(@Param("toUserId") Long toUserId,
                       @Param("fromUserId") Long fromUserId);
}

