package zhiguang.nauy.auth.audit.service;

import org.springframework.transaction.annotation.Transactional;
import zhiguang.nauy.auth.audit.domain.LoginLogs;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.Instant;

/**
* @author yuan
* @description 针对表【login_logs(登录日志表-记录用户的每一次登录尝试，用于安全审计)】的数据库操作Service
* @createDate 2026-04-15 11:09:15
*/
public interface LoginLogsService extends IService<LoginLogs> {
    /**
     * 记录登录日志
     *
     * @param userId
     * @param identifier
     * @param channel
     * @param ip
     * @param userAgent
     * @param status
     */
    void record(Long userId, String identifier, String channel, String ip, String userAgent, String status);

}
