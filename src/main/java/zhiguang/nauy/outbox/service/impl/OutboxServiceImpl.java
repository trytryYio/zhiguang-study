package zhiguang.nauy.outbox.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;
import zhiguang.nauy.outbox.domain.Outbox;
import zhiguang.nauy.outbox.mapper.OutboxMapper;
import zhiguang.nauy.outbox.service.OutboxService;

/**
* @author yuan
* @description 针对表【outbox(发件箱表 - 分布式事务支持)】的数据库操作Service实现
* @createDate 2026-04-29 10:45:50
*/
@Service
public class OutboxServiceImpl extends ServiceImpl<OutboxMapper, Outbox>
    implements OutboxService {

}




