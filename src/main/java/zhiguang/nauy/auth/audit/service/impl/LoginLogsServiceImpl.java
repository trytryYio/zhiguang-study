package zhiguang.nauy.auth.audit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import zhiguang.nauy.auth.audit.domain.LoginLogs;
import zhiguang.nauy.auth.audit.service.LoginLogsService;
import zhiguang.nauy.auth.audit.mapper.LoginLogsMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

/**
* @author yuan
* @description 针对表【login_logs(登录日志表-记录用户的每一次登录尝试，用于安全审计)】的数据库操作Service实现
* @createDate 2026-04-15 11:09:15
*/
@Service
public class LoginLogsServiceImpl extends ServiceImpl<LoginLogsMapper, LoginLogs>
    implements LoginLogsService{
    @Resource
    private LoginLogsMapper loginLogsMapper;

    /**
     * 记录日志
     * @param userId
     * @param identifier
     * @param channel
     * @param ip
     * @param userAgent
     * @param status
     */
    @Transactional
    public void record(Long userId, String identifier, String channel, String ip, String userAgent, String status) {
        LoginLogs log = LoginLogs.builder()
                .userId(userId)
                .identifier(identifier)
                .channel(channel)
                .ip(ip)
                .userAgent(userAgent)
                .status(status)
                .createdAt(Date.from(Instant.now()))
                .build();
        loginLogsMapper.insert(log);
    }
}




