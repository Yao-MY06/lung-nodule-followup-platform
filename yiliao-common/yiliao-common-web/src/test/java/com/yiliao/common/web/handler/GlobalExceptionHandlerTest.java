package com.yiliao.common.web.handler;

import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.common.core.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void bizExceptionMapsToErrorCode() {
        Result<Void> r = handler.handleBiz(new BizException(CommonErrorCode.FORBIDDEN));
        assertEquals(403, r.code());
    }

    @Test
    void validationExceptionReturnsFieldDetail() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "req");
        binding.addError(new FieldError("req", "phone", "手机号格式不正确"));
        MethodArgumentNotValidException e = new MethodArgumentNotValidException(null, binding);
        Result<Void> r = handler.handleValidation(e);
        assertEquals(400, r.code());
        assertEquals("phone: 手机号格式不正确", r.msg());
    }

    @Test
    void noResourceMapsTo404() {
        Result<Void> r = handler.handleNotFound(
                new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/missing"));
        assertEquals(404, r.code());
    }

    @Test
    void unknownExceptionHidesDetails() {
        Result<Void> r = handler.handleUnknown(new IllegalStateException("db password is xxx"));
        assertEquals(500, r.code());
        assertEquals("系统繁忙，请稍后重试", r.msg());
    }
}
