package com.yiliao.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiliao.auth.dto.LoginRequest;
import com.yiliao.auth.dto.LoginResponse;
import com.yiliao.auth.entity.SysUser;
import com.yiliao.auth.error.AuthErrorCode;
import com.yiliao.auth.mapper.SysRoleMapper;
import com.yiliao.auth.mapper.SysUserMapper;
import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.common.security.jwt.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 登录认证（specs/modules/auth.md §4）：
 * 失败统一 10001 防枚举；连续失败 5 次锁定 10 分钟；停用账号拒绝登录。
 */
@Service
public class AuthService {

    static final int MAX_FAIL_COUNT = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final JwtTokenService jwtTokenService;
    private final TokenBlacklistService blacklistService;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(SysUserMapper userMapper, SysRoleMapper roleMapper,
                       JwtTokenService jwtTokenService, TokenBlacklistService blacklistService,
                       StringRedisTemplate redisTemplate, BCryptPasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.jwtTokenService = jwtTokenService;
        this.blacklistService = blacklistService;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        String failKey = failKey(request.username());
        checkLocked(failKey);

        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username()));

        // 账号不存在与密码错误统一提示，防枚举
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            recordFailure(failKey);
            throw new BizException(AuthErrorCode.BAD_CREDENTIALS);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(AuthErrorCode.ACCOUNT_DISABLED);
        }

        redisTemplate.delete(failKey);
        List<String> roles = loadRoles(user.getId());
        return buildResponse(user, roles);
    }

    /** 刷新：校验 refresh 类型 Token（非法一律 10005），轮换签发新 Token 对。 */
    public LoginResponse refresh(String refreshToken) {
        JwtTokenService.TokenPayload payload;
        try {
            payload = jwtTokenService.parse(refreshToken, JwtTokenService.TYPE_REFRESH);
        } catch (BizException e) {
            throw new BizException(AuthErrorCode.REFRESH_INVALID);
        }
        if (blacklistService.isBlacklisted(payload.jti())) {
            throw new BizException(AuthErrorCode.REFRESH_INVALID);
        }
        SysUser user = userMapper.selectById(payload.userId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(AuthErrorCode.ACCOUNT_DISABLED);
        }
        blacklistService.blacklist(payload);
        List<String> roles = loadRoles(user.getId());
        return buildResponse(user, roles);
    }

    /** 注销：把传入 Token 对拉黑（已过期/非法的静默忽略，注销必须幂等成功）。 */
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            blacklistQuietly(accessToken);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            blacklistQuietly(refreshToken);
        }
    }

    public LoginResponse issueForUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(AuthErrorCode.TOKEN_INVALID);
        }
        return buildResponse(user, loadRoles(userId));
    }

    void checkLocked(String failKey) {
        String count = redisTemplate.opsForValue().get(failKey);
        if (count != null && Integer.parseInt(count) >= MAX_FAIL_COUNT) {
            throw new BizException(AuthErrorCode.ACCOUNT_LOCKED);
        }
    }

    void recordFailure(String failKey) {
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1) {
            redisTemplate.expire(failKey, LOCK_DURATION);
        }
        log.warn("登录失败 failKey={} count={}", failKey, count);
    }

    private void blacklistQuietly(String token) {
        try {
            blacklistService.blacklist(jwtTokenService.parse(token));
        } catch (BizException e) {
            // 非法/过期 Token 无需拉黑
        }
    }

    private List<String> loadRoles(Long userId) {
        List<String> roles = roleMapper.selectRoleCodes(userId);
        return roles == null ? List.of() : roles;
    }

    private LoginResponse buildResponse(SysUser user, List<String> roles) {
        JwtTokenService.TokenPayload access = jwtTokenService.issueAccessToken(
                user.getId(), user.getUsername(), user.getUserType(), roles);
        JwtTokenService.TokenPayload refresh = jwtTokenService.issueRefreshToken(
                user.getId(), user.getUsername(), user.getUserType(), roles);
        return new LoginResponse(
                access.token(), refresh.token(), "Bearer",
                (access.expiresAt().getTime() - System.currentTimeMillis()) / 1000,
                user.getId(), user.getRealName(), user.getUserType(), roles);
    }

    static String failKey(String username) {
        return YiliaoConstants.CACHE_PREFIX + "auth:fail:" + username;
    }
}
