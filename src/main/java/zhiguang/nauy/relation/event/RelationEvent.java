package zhiguang.nauy.relation.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
/**
 * 构造关系事件。
 *

 */
@Schema(title = "RelationEventSchema")
@Data
@AllArgsConstructor
public class RelationEvent {

    /**
     * 事件类型
     */
    @Schema(title = "type")
    private String type;

    /**
     * 目标用户ID
     */
    @Schema(title = "toUserId")
    private Long toUserId;

    /**
     * 来源用户ID
     */
    @Schema(title = "fromUserId")
    private Long fromUserId;

    /**
     * 关系ID
     */
    @Schema(title = "id")
    private Long id;
}
