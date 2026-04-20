package zhiguang.nauy.profile.dto;

import java.time.LocalDate;

public record ProfileResponse(
        /** 用户ID */
        Long id,
        /** 昵称 */
        String nickname,
        /** 头像URL */
        String avatar,
        /** 个人简介 */
        String bio,
        /** 智光ID */
        String zgId,
        /** 性别 */
        String gender,
        /** 生日 */
        LocalDate birthday,
        /** 学校 */
        String school,
        /** 手机号 */
        String phone,
        /** 邮箱 */
        String email,
        /** 标签JSON字符串 */
        String tagJson
) {}