package zhiguang.nauy.outbox.domain;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 发件箱表 - 分布式事务支持
 * @TableName outbox
 */
@TableName(value ="outbox")
@Data
public class Outbox implements Serializable {
    /**
     * Outbox事件ID
     */
    @TableId
    private Long id;

    /**
     * 聚合根类型
     */
    private String aggregateType;

    /**
     * 聚合根ID
     */
    private Long aggregateId;

    /**
     * 事件类型
     */
    private String type;

    /**
     * 事件负载数据
     */
    private Object payload;

    /**
     * 创建时间
     */
    private Date createdAt;

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
        Outbox other = (Outbox) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getAggregateType() == null ? other.getAggregateType() == null : this.getAggregateType().equals(other.getAggregateType()))
            && (this.getAggregateId() == null ? other.getAggregateId() == null : this.getAggregateId().equals(other.getAggregateId()))
            && (this.getType() == null ? other.getType() == null : this.getType().equals(other.getType()))
            && (this.getPayload() == null ? other.getPayload() == null : this.getPayload().equals(other.getPayload()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getAggregateType() == null) ? 0 : getAggregateType().hashCode());
        result = prime * result + ((getAggregateId() == null) ? 0 : getAggregateId().hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((getPayload() == null) ? 0 : getPayload().hashCode());
        result = prime * result + ((getCreatedAt() == null) ? 0 : getCreatedAt().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", aggregateType=").append(aggregateType);
        sb.append(", aggregateId=").append(aggregateId);
        sb.append(", type=").append(type);
        sb.append(", payload=").append(payload);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
