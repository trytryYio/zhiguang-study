package com.tongji.knowpost.model;

import lombok.Data;

import java.time.Instant;

/**
 * Mapper 原始行映射（从 DB 读取）。
 * <p>用于信息流列表展示的精简数据模型，仅包含展示必需的字段。</p>
 * <p>设计目的：</p>
 * <ul>
 *   <li>减少网络传输：避免加载 contentUrl、contentEtag 等大字段</li>
 *   <li>降低内存占用：在大量列表项场景下节省内存</li>
 *   <li>提高查询速度：SELECT 语句只查询必要字段</li>
 * </ul>
 * <p>使用场景：首页信息流、搜索结果页、用户主页知文列表等高性能要求的列表展示场景。</p>
 *
 * @author tongji
 */
@Data
public class KnowPostFeedRow {
    /**
     * 知文ID
     */
    private Long id;
    
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
}