package com.tongji.knowpost.model;

import lombok.Data;

import java.time.Instant;

/**
 * 知文详情查询的行映射（含作者信息）。
 * <p>用于知文详情页展示的完整数据模型，包含所有业务字段。</p>
 * <p>设计目的：</p>
 * <ul>
 *   <li>完整性：包含权限校验、编辑操作所需的所有字段</li>
 *   <li>权限控制：包含 creatorId、visible、status 等状态字段用于访问控制</li>
 *   <li>内容获取：包含 contentUrl、contentEtag、contentSha256 等内容引用信息</li>
 * </ul>
 * <p>使用场景：知文详情页、编辑页面、管理后台等需要完整业务信息的场景。</p>
 *
 * @author tongji
 */
@Data
public class KnowPostDetailRow {
    /**
     * 知文ID
     */
    private Long id;
    
    /**
     * 创作者用户ID
     * <p>用于权限校验，判断当前用户是否有编辑/删除权限</p>
     */
    private Long creatorId;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 描述/摘要
     */
    private String description;
    
    /**
     * 标签列表（JSON字符串格式）
     * <p>示例：["Java", "Spring Boot", "微服务"]</p>
     */
    private String tags;
    
    /**
     * 图片URL列表（JSON字符串格式）
     * <p>示例：["https://oss.example.com/img1.jpg", "https://oss.example.com/img2.jpg"]</p>
     */
    private String imgUrls;
    
    /**
     * 内容文件URL
     * <p>指向存储在 OSS 中的完整内容文件（Markdown/HTML等）</p>
     */
    private String contentUrl;
    
    /**
     * 内容文件ETag
     * <p>用于校验文件完整性，确保下载的内容未被篡改</p>
     */
    private String contentEtag;
    
    /**
     * 内容文件SHA256哈希值
     * <p>用于二次校验文件完整性，提供更高的安全性保障</p>
     */
    private String contentSha256;
    
    /**
     * 作者头像URL
     */
    private String authorAvatar;
    
    /**
     * 作者昵称
     */
    private String authorNickname;
    
    /**
     * 作者的领域标签（JSON字符串格式）
     * <p>示例：["后端开发", "分布式系统"]</p>
     */
    private String authorTagJson;
    
    /**
     * 发布时间
     */
    private Instant publishTime;
    
    /**
     * 是否置顶
     * <p>true-置顶，false-普通排序</p>
     */
    private Boolean isTop;
    
    /**
     * 可见性
     * <p>枚举值：PUBLIC-公开，PRIVATE-私密，FRIENDS-仅好友可见</p>
     */
    private String visible;
    
    /**
     * 知文类型
     * <p>枚举值：ARTICLE-文章，VIDEO-视频，COURSE-课程等</p>
     */
    private String type;
    
    /**
     * 状态
     * <p>枚举值：DRAFT-草稿，PUBLISHED-已发布，DELETED-已删除</p>
     */
    private String status;
}