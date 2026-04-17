package zhiguang.nauy.auth.audit.mapper;

import zhiguang.nauy.auth.audit.domain.LoginLogs;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author yuan
* @description 针对表【login_logs(登录日志表-记录用户的每一次登录尝试，用于安全审计)】的数据库操作Mapper
* @createDate 2026-04-15 11:09:15
* @Entity generator.domain.LoginLogs
*/
public interface LoginLogsMapper extends BaseMapper<LoginLogs> {

}




