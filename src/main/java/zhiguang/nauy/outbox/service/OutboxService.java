package zhiguang.nauy.outbox.service;

import com.baomidou.mybatisplus.extension.service.IService;
import zhiguang.nauy.outbox.domain.Outbox;

/**
* @author yuan
* @description 针对表【outbox(发件箱表 - 分布式事务支持)】的数据库操作Service
* @createDate 2026-04-29 10:45:50
*/
public interface OutboxService extends IService<Outbox> {

}
