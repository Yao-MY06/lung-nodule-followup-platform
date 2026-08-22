package com.yiliao.auth.service;

import com.yiliao.auth.dto.LoginRequest;
import com.yiliao.auth.dto.LoginResponse;
import com.yiliao.auth.entity.SysUser;
import com.yiliao.auth.error.AuthErrorCode;
import com.yiliao.auth.mapper.SysRoleMapper;
import com.yiliao.auth.mapper.SysUserMapper;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.common.security.jwt.JwtProperties;
import com.yiliao.common.security.jwt.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "unit-test-secret-key-0123456789abcdef0123456789";

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private TokenBlacklistService blacklistService;
    @Mock
    private ValueOperations<String, String> valueOps;

    private AuthService service;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        JwtTokenService jwt = new JwtTokenService(new JwtProperties(SECRET, "yiliao", 7200, 604800));
        service = new AuthService(userMapper, roleMapper, jwt, blacklistService, redisTemplate, encoder);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private SysUser user(String rawPassword, int status) {
        SysUser user = new SysUser();
        user.setId(7L);
        user.setUsername("doctor01");
        user.setPassword(encoder.encode(rawPassword));
        user.setRealName("测试医生");
        user.setUserType(2);
        user.setStatus(status);
        return user;
    }

    @Test
    void loginSuccessReturnsTokensAndRoles() {
        when(userMapper.selectOne(any())).thenReturn(user("right-pass", 1));
        when(roleMapper.selectRoleCodes(7L)).thenReturn(List.of("DOCTOR"));
        when(valueOps.get(anyString())).thenReturn(null);

        LoginResponse response = service.login(new LoginRequest("doctor01", "right-pass"));

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals(List.of("DOCTOR"), response.roles());
        verify(redisTemplate).delete(AuthService.failKey("doctor01"));
    }

    @Test
    void wrongPasswordThrowsUnifiedErrorAndCountsFailure() {
        when(userMapper.selectOne(any())).thenReturn(user("right-pass", 1));
        when(valueOps.get(anyString())).thenReturn(null);
        when(valueOps.increment(anyString())).thenReturn(1L);

        BizException e = assertThrows(BizException.class,
                () -> service.login(new LoginRequest("doctor01", "wrong-pass")));
        assertEquals(AuthErrorCode.BAD_CREDENTIALS.getCode(), e.getCode());
        verify(valueOps).increment(AuthService.failKey("doctor01"));
    }

    @Test
    void unknownUserSameErrorAsWrongPassword() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(valueOps.get(anyString())).thenReturn(null);
        when(valueOps.increment(anyString())).thenReturn(1L);

        BizException e = assertThrows(BizException.class,
                () -> service.login(new LoginRequest("ghost", "whatever")));
        assertEquals(AuthErrorCode.BAD_CREDENTIALS.getCode(), e.getCode());
    }

    @Test
    void lockedAfterFiveFailures() {
        when(valueOps.get(AuthService.failKey("doctor01"))).thenReturn("5");

        BizException e = assertThrows(BizException.class,
                () -> service.login(new LoginRequest("doctor01", "right-pass")));
        assertEquals(AuthErrorCode.ACCOUNT_LOCKED.getCode(), e.getCode());
        verify(userMapper, never()).selectOne(any());
    }

    @Test
    void disabledUserRejected() {
        when(userMapper.selectOne(any())).thenReturn(user("right-pass", 0));
        when(valueOps.get(anyString())).thenReturn(null);

        BizException e = assertThrows(BizException.class,
                () -> service.login(new LoginRequest("doctor01", "right-pass")));
        assertEquals(AuthErrorCode.ACCOUNT_DISABLED.getCode(), e.getCode());
    }
}
