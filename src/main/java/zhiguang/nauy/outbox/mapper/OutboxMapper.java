package zhiguang.nauy.outbox.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import zhiguang.nauy.outbox.domain.Outbox;

/**
* @author yuan
* @description 针对表【outbox(发件箱表 - 分布式事务支持)】的数据库操作Mapper
* @createDate 2026-04-29 10:45:50
* @Entity .domain.Outbox
*/
@Mapper
public interface OutboxMapper extends BaseMapper<Outbox> {

    /**
     * 写入 Outbox 事件。
     * @param id 事件ID
     * @param aggregateType 聚合类型
     * @param aggregateId 聚合ID
     * @param type 事件类型
     * @param payload 事件负载（JSON）
     * @return 影响行数
     */
    int insert(@Param("id") Long id,
               @Param("aggregateType") String aggregateType,
               @Param("aggregateId") Long aggregateId,
               @Param("type") String type,
               @Param("payload") String payload);
}




