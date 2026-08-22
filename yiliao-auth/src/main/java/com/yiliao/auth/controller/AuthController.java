package com.yiliao.auth.controller;

import com.yiliao.auth.dto.LoginRequest;
import com.yiliao.auth.dto.LoginResponse;
import com.yiliao.auth.dto.RefreshRequest;
import com.yiliao.auth.error.AuthErrorCode;
import com.yiliao.auth.service.AuthService;
import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.common.core.result.Result;
import com.yiliao.common.security.jwt.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（specs/modules/auth.md §3）。/me 依赖网关注入的 X-User-Id。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenService jwtTokenService;

    public AuthController(AuthService authService, JwtTokenService jwtTokenService) {
        this.authService = authService;
        this.jwtTokenService = jwtTokenService;
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.ok(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "注销（Token 入黑名单）")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @RequestBody(required = false) RefreshRequest request) {
        String accessToken = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            accessToken = authorization.substring(7);
        }
        authService.logout(accessToken, request == null ? null : request.refreshToken());
        return Result.ok();
    }

    @Operation(summary = "当前登录用户信息")
    @PostMapping("/me")
    public Result<LoginResponse> me(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestHeader(value = YiliaoConstants.HEADER_USER_ID, required = false) String headerUserId) {
        Long userId = resolveUserId(authorization, headerUserId);
        return Result.ok(authService.issueForUser(userId));
    }

    /** 优先信任网关透传头；直连服务（本地调试）时回退解析 Bearer Token。 */
    private Long resolveUserId(String authorization, String headerUserId) {
        if (headerUserId != null && !headerUserId.isBlank()) {
            return Long.valueOf(headerUserId);
        }
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return jwtTokenService.parse(authorization.substring(7)).userId();
        }
        throw new BizException(AuthErrorCode.TOKEN_INVALID);
    }
}
