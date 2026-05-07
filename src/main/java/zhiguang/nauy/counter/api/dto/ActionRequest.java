package zhiguang.nauy.counter.api.dto;

import lombok.Data;

/**
 * 行为请求体。
 * 用于接收点赞/收藏等操作的请求参数。
 */
@Data
public class ActionRequest {
    
    /**
     * 实体类型（如 knowpost）
     */
    private String entityType;
    
    /**
     * 实体ID
     */
    private String entityId;
}
