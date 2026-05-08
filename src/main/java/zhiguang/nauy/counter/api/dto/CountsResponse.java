package zhiguang.nauy.counter.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 计数响应体。
 * 返回实体各指标的计数值。
 */
@Data
@AllArgsConstructor
public class CountsResponse {

    /**
     * 实体类型
     */
    private String entityType;

    /**
     * 实体ID
     */
    private String entityId;

    /**
     * 各指标的计数值映射（如 {"like": 100, "fav": 50}）
     */
    private Map<String, Long> counts;
}
