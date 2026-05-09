package zhiguang.nauy.counter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户维度计数 DTO。
 *
 * <p>包含：关注数、粉丝数、发帖数、获赞数、获收藏数</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCounterDTO {
    /**
     * 关注数
     */
    private Long followings;

    /**
     * 粉丝数
     */
    private Long followers;

    /**
     * 发帖数
     */
    private Long posts;

    /**
     * 获赞数
     */
    private Long likesReceived;

    /**
     * 获收藏数
     */
    private Long favsReceived;
}
