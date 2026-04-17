package zhiguang.nauy.auth.model;

/**
 * 客户端信息。记录客户端 IP 与 User-Agent，用于登录审计。
 */
public record ClientInfo(
//        ip地址
        String ip,
        // 用户代理 : User-Agent
//        操作系统：Windows 10 (64位)
//浏览器引擎：AppleWebKit 537.36
//浏览器：Chrome 120.0.0.0
//兼容标识：Safari 537.36
        String userAgent) {
}