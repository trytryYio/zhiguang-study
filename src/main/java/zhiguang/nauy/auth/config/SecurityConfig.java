package zhiguang.nauy.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * @Description: pring Security 安全配置。
 * *
 * * <p>第一阶段先配置基础安全策略，第二阶段会加入 JWT 过滤器。</p>
 * @ClassName: SecutityConfig    // 类名，会自动填充
 * @Author: oyy         // 创建者
 * @Date: 2026/4/12 16:29   // 时间
 * @Version: 1.0     // 版本
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 启用方法级安全
public class SecurityConfig {
    /**
     * //    禁用CSRF防护，启用CORS
     * //    设置无状态会话管理（STATELESS）
     * //    公开部分接口：健康检查、Feed流、知文详情、RAG问答、认证相关接口
     * //            其他所有请求都需要身份认证
     * //    启用OAuth2资源服务器和JWT验证
     *
     * @param httpSecurity
     * @return
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity)

            throws Exception {
        httpSecurity
                .csrf(csrf -> csrf.disable())// 关闭 CSRF 防护
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/info").permitAll()
                                //公开内容  首页Feed 不需要登录

                                .requestMatchers("/api/v1/knoposts/feed").permitAll()
//                        知文详情 (公开已发布的内容)
                                .requestMatchers(HttpMethod.GET, "/api/v1/knowposts/detail/*").permitAll()
//                        RAG 回答允许匿名
                                .requestMatchers(HttpMethod.GET, "/api/v1/knowposts/*/qa/stream").permitAll()
// 认证接口公开
                                .requestMatchers(
                                        "/api/v1/auth/send-code",
                                        "/api/v1/auth/register",
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/token/refresh",
                                        "/api/v1/auth/logout",
                                        "/api/v1/auth/password/reset"
                                ).permitAll()
                                .anyRequest().authenticated()

                )
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
//     ，第二阶段会加入 JWT 过滤器


        return httpSecurity.build();

    }

    /**
     * 这段代码配置了CORS跨域策略：
     * 允许所有来源访问（*，待优化为白名单）
     * 支持GET、POST、PUT、DELETE、OPTIONS五种HTTP方法
     * 允许Authorization、Content-Type、X-Requested-With请求头
     * 不携带凭证（allowCredentials=false）
     * 应用于所有路径（/**）
     *
     * @return
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // TODO: 后续替换为产品白名单
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
