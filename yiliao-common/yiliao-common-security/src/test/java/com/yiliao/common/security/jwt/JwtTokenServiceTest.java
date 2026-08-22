package com.yiliao.common.security.jwt;

import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceTest {

    private static final String SECRET = "unit-test-secret-key-0123456789abcdef0123456789";

    private JwtTokenService service() {
        return new JwtTokenService(new JwtProperties(SECRET, "yiliao", 7200, 604800));
    }

    @Test
    void accessTokenRoundTripKeepsClaims() {
        String token = service().issueAccessToken(7L, "doctor01", 2, List.of("DOCTOR")).token();
        JwtTokenService.TokenPayload payload = service().parse(token);
        assertEquals(7L, payload.userId());
        assertEquals("doctor01", payload.username());
        assertEquals(2, payload.userType());
        assertEquals(List.of("DOCTOR"), payload.roles());
        assertEquals(JwtTokenService.TYPE_ACCESS, payload.tokenType());
        assertTrue(payload.expiresAt().getTime() > System.currentTimeMillis());
    }

    @Test
    void jtiIsUniquePerIssue() {
        JwtTokenService service = service();
        JwtTokenService.TokenPayload a = service.issueAccessToken(1L, "u", 1, List.of());
        JwtTokenService.TokenPayload b = service.issueAccessToken(1L, "u", 1, List.of());
        assertNotEquals(a.token(), b.token());
        assertNotEquals(a.jti(), b.jti());
    }

    @Test
    void expiredTokenRejectedAs401() {
        JwtTokenService expiredService = new JwtTokenService(
                new JwtProperties(SECRET, "yiliao", -60, -60));
        String token = expiredService.issueAccessToken(1L, "u", 1, List.of()).token();
        BizException e = assertThrows(BizException.class, () -> service().parse(token));
        assertEquals(CommonErrorCode.UNAUTHORIZED.getCode(), e.getCode());
    }

    @Test
    void wrongSignatureRejected() {
        String token = service().issueAccessToken(1L, "u", 1, List.of()).token();
        JwtTokenService other = new JwtTokenService(new JwtProperties(
                "another-secret-key-0123456789abcdef0123456789xyz", "yiliao", 7200, 604800));
        assertThrows(BizException.class, () -> other.parse(token));
    }

    @Test
    void accessTokenRejectedWhereRefreshExpected() {
        String accessToken = service().issueAccessToken(1L, "u", 1, List.of()).token();
        BizException e = assertThrows(BizException.class,
                () -> service().parse(accessToken, JwtTokenService.TYPE_REFRESH));
        assertEquals(CommonErrorCode.UNAUTHORIZED.getCode(), e.getCode());
    }

    @Test
    void secretTooShortRejectedAtConstruction() {
        assertThrows(IllegalStateException.class,
                () -> new JwtTokenService(new JwtProperties("short", "yiliao", 1, 1)));
    }

    @Test
    void remainingSecondsNeverNegative() {
        JwtTokenService.TokenPayload payload = service().issueAccessToken(1L, "u", 1, List.of());
        assertTrue(service().remainingSeconds(payload) > 0);
    }
}
