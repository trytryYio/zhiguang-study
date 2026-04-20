package zhiguang.nauy.user.domain;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import zhiguang.nauy.profile.dto.ProfileResponse;

/**
 * @TableName users
 */
@TableName(value = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    /**
     *
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     *
     */
    private String phone;

    /**
     *
     */
    private String email;

    /**
     *
     */
    private String passwordHash;

    /**
     *
     */
    private String nickname;

    /**
     *
     */
    private String avatar;

    /**
     *
     */
    private String bio;

    /**
     *
     */
    private String zgId;

    /**
     *
     */
    private String gender;

    /**
     *
     */
    private Date birthday;

    /**
     *
     */
    private String school;

    /**
     *
     */
    private Object tagsJson;

    /**
     *
     */
    private Date createdAt;

    /**
     *
     */
    private Date updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        User other = (User) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getPhone() == null ? other.getPhone() == null : this.getPhone().equals(other.getPhone()))
                && (this.getEmail() == null ? other.getEmail() == null : this.getEmail().equals(other.getEmail()))
                && (this.getPasswordHash() == null ? other.getPasswordHash() == null : this.getPasswordHash().equals(other.getPasswordHash()))
                && (this.getNickname() == null ? other.getNickname() == null : this.getNickname().equals(other.getNickname()))
                && (this.getAvatar() == null ? other.getAvatar() == null : this.getAvatar().equals(other.getAvatar()))
                && (this.getBio() == null ? other.getBio() == null : this.getBio().equals(other.getBio()))
                && (this.getZgId() == null ? other.getZgId() == null : this.getZgId().equals(other.getZgId()))
                && (this.getGender() == null ? other.getGender() == null : this.getGender().equals(other.getGender()))
                && (this.getBirthday() == null ? other.getBirthday() == null : this.getBirthday().equals(other.getBirthday()))
                && (this.getSchool() == null ? other.getSchool() == null : this.getSchool().equals(other.getSchool()))
                && (this.getTagsJson() == null ? other.getTagsJson() == null : this.getTagsJson().equals(other.getTagsJson()))
                && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
                && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getPhone() == null) ? 0 : getPhone().hashCode());
        result = prime * result + ((getEmail() == null) ? 0 : getEmail().hashCode());
        result = prime * result + ((getPasswordHash() == null) ? 0 : getPasswordHash().hashCode());
        result = prime * result + ((getNickname() == null) ? 0 : getNickname().hashCode());
        result = prime * result + ((getAvatar() == null) ? 0 : getAvatar().hashCode());
        result = prime * result + ((getBio() == null) ? 0 : getBio().hashCode());
        result = prime * result + ((getZgId() == null) ? 0 : getZgId().hashCode());
        result = prime * result + ((getGender() == null) ? 0 : getGender().hashCode());
        result = prime * result + ((getBirthday() == null) ? 0 : getBirthday().hashCode());
        result = prime * result + ((getSchool() == null) ? 0 : getSchool().hashCode());
        result = prime * result + ((getTagsJson() == null) ? 0 : getTagsJson().hashCode());
        result = prime * result + ((getCreatedAt() == null) ? 0 : getCreatedAt().hashCode());
        result = prime * result + ((getUpdatedAt() == null) ? 0 : getUpdatedAt().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", phone=").append(phone);
        sb.append(", email=").append(email);
        sb.append(", passwordHash=").append(passwordHash);
        sb.append(", nickname=").append(nickname);
        sb.append(", avatar=").append(avatar);
        sb.append(", bio=").append(bio);
        sb.append(", zgId=").append(zgId);
        sb.append(", gender=").append(gender);
        sb.append(", birthday=").append(birthday);
        sb.append(", school=").append(school);
        sb.append(", tagsJson=").append(tagsJson);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }


    /**
     * 将 User 实体转换为 ProfileResponse
     */
    public ProfileResponse convertToProfileResponse() {
        return new ProfileResponse(
                this.getId(),
                this.getNickname(),
                this.getAvatar(),
                this.getBio(),
                this.getZgId(),
                this.getGender(),
                LocalDateTimeUtil.of(this.getBirthday()).toLocalDate(),
                this.getSchool(),
                this.getPhone(),
                this.getEmail(),
                this.getTagsJson().toString()

        );
    }
}