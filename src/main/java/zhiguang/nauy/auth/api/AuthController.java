package zhiguang.nauy.auth.api;

import cn.hutool.core.util.ObjUtil;
import io.micrometer.core.instrument.binder.http.HttpServletRequestTagsProvider;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.BindingResultUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import zhiguang.nauy.auth.api.dto.*;
import zhiguang.nauy.auth.model.ClientInfo;
import zhiguang.nauy.auth.service.AuthService;
import zhiguang.nauy.auth.token.JwtService;
import zhiguang.nauy.auth.token.RefreshTokenStore;
import zhiguang.nauy.auth.verdication.VerificationService;
import zhiguang.nauy.common.BaseResponse;
import zhiguang.nauy.common.ResultUtils;
import zhiguang.nauy.exception.ErrorCode;
import zhiguang.nauy.exception.ThrowUtils;

import javax.validation.Valid;
import java.util.Objects;
import java.util.Optional;

/**
 * 认证 API 控制器。
 * <p>
 * 暴露 REST 接口：发送验证码、注册、登录、刷新令牌、登出、重置密码、查询当前用户信息。
 * 集成：使用 Spring Security 的资源服务器能力，`/me` 通过 `@AuthenticationPrincipal Jwt` 提取用户。
 * 客户端信息：从请求头解析 IP 与 UA，用于审计登录日志。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    @Resource
    private VerificationService verificationService;
    @Resource
    private RefreshTokenStore refreshTokenStore;
    @Resource
    private JwtService jwtService;
    @Resource
    private AuthService authService;

    /**
     * 注册新用户并自动登录
     * <p>
     * 验证标识与验证码后创建用户，若提供密码则进行复杂度校验并保存密码哈希；成功后签发 Access/Refresh Token。
     *
     * @param request     请求体，包含：标识类型与值、验证码、可选密码、是否同意协议。
     * @param httpRequest 用于解析客户端信息（IP 与 User-Agent），记录审计日志。
     * @return 认证响应，包含用户信息与令牌对。
     */
    @PostMapping("/register")
    public BaseResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                               HttpServletRequest httpRequest) {
        AuthResponse authResponse = authService.register(request, resolveClient(httpRequest));
        return ResultUtils.success(authResponse);
    }

    /**
     * 获取当前用户信息
     *
     * @param jwt
     * @return
     */
    @GetMapping("/me")
    public BaseResponse<AuthUserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        return ResultUtils.success(authService.me(userId));
    }

    /**
     * 使用 Refresh Token 刷新令牌。
     * <p>
     * 校验刷新令牌的合法性与白名单状态，签发新的令牌对，并撤销旧刷新令牌。
     *
     * @param refreshToken 请求体，包含：refreshToken（刷新令牌）。
     * @return 新的令牌响应（accessToken/refreshToken 及其过期时间）。
     */
    @PostMapping("/token/refresh")
    public BaseResponse<TokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest refreshToken) {

        TokenResponse authResponse = authService.refresh(refreshToken);
        return ResultUtils.success(authResponse);
    }

    /**
     * 登出
     *
     * @param logoutRequest
     * @return
     */
    @PostMapping("/logout")
    public void logout(@RequestBody @Valid  LogoutRequest logoutRequest) {
      authService.logout(logoutRequest.refreshToken());
    }

    /**
     * 重置密码
     *
     * @param request
     * @return
     */
    @PostMapping("/password/reset")
    public  void resetPassword(PasswordResetRequest request){
//        校验参数
        ThrowUtils.throwIf(ObjUtil.isNull(request), ErrorCode.PARAMS_ERROR);
        authService.resetPassword(request);

    }

    /**
     * 发送验证码
     * @param request
     * @return
     */
    @PostMapping("/send-code")
    public BaseResponse<SendCodeResponse> sendCode(@Valid @RequestBody SendCodeRequest request){
        SendCodeResponse sendCodeResponse =verificationService.sendCode(request);
        return ResultUtils.success(sendCodeResponse);
    }

    /**
     * 登录
      * @param request
     * @param httpRequest
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResultUtils.success(authService.login(request, resolveClient(httpRequest)));
    }


    /**
     * 从请求中解析客户端信息。
     *
     * @param request HTTP 请求对象。
     * @return 客户端信息（IP 与 User-Agent）。
     */
    private ClientInfo resolveClient(HttpServletRequest request) {
        String ip = extractClientIp(request);
        String header = request.getHeader("User-Agent");
        return new ClientInfo(ip, header);
    }

    /**
     * 从请求中获取客户端 IP
     *
     * @param request
     * @return
     */
    private String extractClientIp(HttpServletRequest request) {
//        `request.getRemoteAddr()` 拿到的可能是代理服务器 IP
//        所以有三层兜底模式
//       第 1 层：`X-Forwarded-For`
//
//        这通常是最常见的真实客户端 IP 头。
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
//            第一个才是最初的客户端 IP，所以要：
            return forwarded.split(",")[0].trim();
        }
//        第 2 层：`X-Real-IP`
//有些代理不会传 `X-Forwarded-For`，只传 `X-Real-IP`。
//
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
//        第 3 层：`getRemoteAddr()` //前两个都没有 没招了

        return request.getRemoteAddr();
    }









}

