package com.yiliao.common.web.handler;

import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.common.core.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理（specs/global/10 §7）：BizException→业务码；校验失败→400+字段信息；兜底→500 不暴露堆栈。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常 code={} msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.fail(CommonErrorCode.PARAM_INVALID.getCode(), detail);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        return Result.fail(CommonErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("未处理异常", e);
        return Result.fail(CommonErrorCode.SYSTEM_ERROR);
    }
}
