package zhiguang.nauy.knowpost.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户帖子统计 DTO。
 *
 * <p>封装用户的帖子总数和 ID 列表。</p>
 */
@Data
public class UserPostsStatsDTO {
    /**
     * 帖子总数
     */
    private Long count;

    /**
     * 去重后的帖子 ID 列表
     */
    private List<Long> ids;
}
