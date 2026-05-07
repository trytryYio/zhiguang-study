package zhiguang.nauy.knowpost.domain;
import java.time.Instant;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 知文主表-存储文章/帖子的核心元数据
 * <p>该实体类对应数据库中的 know_posts 表，用于存储用户发布的文章、帖子等内容的核心信息</p>
 * <p>主要功能：</p>
 * <ul>
 *   <li>存储知文的基本信息（标题、描述、分类等）</li>
 *   <li>管理内容文件的 OSS 存储元数据（对象键、ETag、大小、哈希值等）</li>
 *   <li>控制知文的可见性、置顶状态和发布状态</li>
 *   <li>记录创建时间、更新时间和发布时间</li>
 * </ul>
 *
 * @TableName know_posts
 */
@TableName(value ="know_posts")
@Data
public class KnowPosts implements Serializable {
    /**
     * 知文唯一标识ID
     * <p>使用雪花算法生成的分布式ID，作为主键</p>
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 主分类/内容分类ID
     * <p>关联到标签表的主标签ID，用于对知文进行分类</p>
     */
    private Long tagId;

    /**
     * 标签名数组
     * <p>存储多个标签名称，例如 ["java","编程"]，以 JSON 格式存储</p>
     */
    private Object tags;

    /**
     * 知文标题
     * <p>文章的标题，建议长度不超过50个字符</p>
     */
    private String title;

    /**
     * 摘要/描述
     * <p>文章的简短描述或摘要，最多50字，用于列表展示和搜索</p>
     */
    private String description;

    /**
     * 正文访问URL
     * <p>存储在OSS中的正文内容的访问URL或预签名URL，用户可通过此URL访问完整内容</p>
     */
    private String contentUrl;

    /**
     * OSS对象Key
     * <p>存储在阿里云OSS中的对象键，用于生成访问URL和管理文件</p>
     */
    private String contentObjectKey;

    /**
     * OSS ETag（用于校验）
     */
    private String contentEtag;

    /**
     * 正文字节大小
     */
    private Long contentSize;

    /**
     * 正文SHA-256哈希（hex）
     */
    private String contentSha256;

    /**
     * 
     */
    private Long creatorId;

    /**
     * 
     */
    private Integer isTop;

    /**
     * 
     */
    private String type;

    /**
     * 
     */
    private String visible;

    /**
     * 图片URL数组或对象数组
     */
    private Object imgUrls;

    /**
     * 视频URL（一期不使用）
     */
    private String videoUrl;

    /**
     * 
     */
    private String status;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    /**
     * 
     */
    private Date publishTime;

    @TableLogic
    private Boolean isDelete;

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
        KnowPosts other = (KnowPosts) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getTagId() == null ? other.getTagId() == null : this.getTagId().equals(other.getTagId()))
            && (this.getTags() == null ? other.getTags() == null : this.getTags().equals(other.getTags()))
            && (this.getTitle() == null ? other.getTitle() == null : this.getTitle().equals(other.getTitle()))
            && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
            && (this.getContentUrl() == null ? other.getContentUrl() == null : this.getContentUrl().equals(other.getContentUrl()))
            && (this.getContentObjectKey() == null ? other.getContentObjectKey() == null : this.getContentObjectKey().equals(other.getContentObjectKey()))
            && (this.getContentEtag() == null ? other.getContentEtag() == null : this.getContentEtag().equals(other.getContentEtag()))
            && (this.getContentSize() == null ? other.getContentSize() == null : this.getContentSize().equals(other.getContentSize()))
            && (this.getContentSha256() == null ? other.getContentSha256() == null : this.getContentSha256().equals(other.getContentSha256()))
            && (this.getCreatorId() == null ? other.getCreatorId() == null : this.getCreatorId().equals(other.getCreatorId()))
            && (this.getIsTop() == null ? other.getIsTop() == null : this.getIsTop().equals(other.getIsTop()))
            && (this.getType() == null ? other.getType() == null : this.getType().equals(other.getType()))
            && (this.getVisible() == null ? other.getVisible() == null : this.getVisible().equals(other.getVisible()))
            && (this.getImgUrls() == null ? other.getImgUrls() == null : this.getImgUrls().equals(other.getImgUrls()))
            && (this.getVideoUrl() == null ? other.getVideoUrl() == null : this.getVideoUrl().equals(other.getVideoUrl()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (this.getPublishTime() == null ? other.getPublishTime() == null : this.getPublishTime().equals(other.getPublishTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getTagId() == null) ? 0 : getTagId().hashCode());
        result = prime * result + ((getTags() == null) ? 0 : getTags().hashCode());
        result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getContentUrl() == null) ? 0 : getContentUrl().hashCode());
        result = prime * result + ((getContentObjectKey() == null) ? 0 : getContentObjectKey().hashCode());
        result = prime * result + ((getContentEtag() == null) ? 0 : getContentEtag().hashCode());
        result = prime * result + ((getContentSize() == null) ? 0 : getContentSize().hashCode());
        result = prime * result + ((getContentSha256() == null) ? 0 : getContentSha256().hashCode());
        result = prime * result + ((getCreatorId() == null) ? 0 : getCreatorId().hashCode());
        result = prime * result + ((getIsTop() == null) ? 0 : getIsTop().hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((getVisible() == null) ? 0 : getVisible().hashCode());
        result = prime * result + ((getImgUrls() == null) ? 0 : getImgUrls().hashCode());
        result = prime * result + ((getVideoUrl() == null) ? 0 : getVideoUrl().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + ((getPublishTime() == null) ? 0 : getPublishTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", tagId=").append(tagId);
        sb.append(", tags=").append(tags);
        sb.append(", title=").append(title);
        sb.append(", description=").append(description);
        sb.append(", contentUrl=").append(contentUrl);
        sb.append(", contentObjectKey=").append(contentObjectKey);
        sb.append(", contentEtag=").append(contentEtag);
        sb.append(", contentSize=").append(contentSize);
        sb.append(", contentSha256=").append(contentSha256);
        sb.append(", creatorId=").append(creatorId);
        sb.append(", isTop=").append(isTop);
        sb.append(", type=").append(type);
        sb.append(", visible=").append(visible);
        sb.append(", imgUrls=").append(imgUrls);
        sb.append(", videoUrl=").append(videoUrl);
        sb.append(", status=").append(status);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", publishTime=").append(publishTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }


}