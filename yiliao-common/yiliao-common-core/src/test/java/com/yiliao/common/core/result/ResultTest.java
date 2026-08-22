package com.yiliao.common.core.result;

import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @Test
    void okShouldHaveZeroCode() {
        Result<String> r = Result.ok("data");
        assertEquals(0, r.code());
        assertEquals("data", r.data());
        assertTrue(r.isSuccess());
    }

    @Test
    void failFromErrorCode() {
        Result<Void> r = Result.fail(CommonErrorCode.UNAUTHORIZED);
        assertEquals(401, r.code());
        assertEquals("未认证或登录已过期", r.msg());
        assertNull(r.data());
        assertFalse(r.isSuccess());
    }

    @Test
    void failFromBizExceptionKeepsCustomMsg() {
        BizException e = new BizException(CommonErrorCode.PARAM_INVALID, "手机号格式不正确");
        Result<Void> r = Result.fail(e.getErrorCode());
        assertEquals(400, r.code());
        assertEquals("手机号格式不正确", r.msg());
    }
}
