package com.yiliao.ai.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.common.core.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.charset.StandardCharsets;

/**
 * SSE 端点业务异常的传输层处理（T4 遗留决策一，2026-09-05 定案落地，仅作用于本服务）。
 *
 * <p>背景：{@code /api/ai/chat} 与 {@code /api/ai/report/interpret} 均 produces=text/event-stream，
 * 抛出 BizException 时 common-web 的 {@code GlobalExceptionHandler} 返回 JSON Result 体，
 * 与请求 Accept: text/event-stream 协商失败 → HttpMediaTypeNotAcceptableException → HTTP 500 空体，
 * 客户端拿不到真实业务码（T4 验证报告 P1 用例：守卫已拦截但传输层 403 不可达）。
 *
 * <p>契约：<b>真实状态码 + 事件体</b>——
 * <ul>
 *   <li>Accept 含 text/event-stream：按业务码映射 HTTP 状态（401→401、403→403、其余→500），
 *       Content-Type 为 text/event-stream;charset=UTF-8，body 为可直接被 EventSource/curl
 *       解析的错误事件：{@code event:error} + {@code data:{"code":<业务码>,"msg":"<msg>"}} + 空行结尾；</li>
 *   <li>Accept 不含 text/event-stream：不处理该请求形态，按平台"HTTP 200 + 业务码"惯例
 *       返回与全局处理器一致的 Result JSON（Result.fail）。</li>
 * </ul>
 *
 * <p>限定 basePackages=com.yiliao.ai 且 @Order 高于全局处理器（后者无 @Order，为最低优先级），
 * 仅接管本服务控制器的 BizException；其余异常类型仍走 common-web 全局处理器。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.yiliao.ai")
public class SseBizExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SseBizExceptionHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(BizException.class)
    public ResponseEntity<?> handleBiz(BizException e, HttpServletRequest request) {
        log.warn("业务异常 code={} msg={} accept={}", e.getCode(), e.getMessage(), request.getHeader("Accept"));
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
            return ResponseEntity.status(resolveHttpStatus(e.getCode()))
                    .contentType(new MediaType(MediaType.TEXT_EVENT_STREAM, StandardCharsets.UTF_8))
                    .body(sseErrorBody(e.getCode(), e.getMessage()));
        }
        // 非 SSE 形态：与 GlobalExceptionHandler.handleBiz 行为一致（HTTP 200 + Result 业务码）
        return ResponseEntity.ok(Result.fail(e.getErrorCode()));
    }

    /** 业务码 → HTTP 状态码映射：401/403 如实透传，其余统一 500（不对外暴露内部码语义）。 */
    private HttpStatus resolveHttpStatus(int bizCode) {
        if (bizCode == CommonErrorCode.UNAUTHORIZED.getCode()) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (bizCode == CommonErrorCode.FORBIDDEN.getCode()) {
            return HttpStatus.FORBIDDEN;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /** 构造 SSE 错误事件体：event:error + data JSON + 空行结尾（SSE 事件终止符）。 */
    private String sseErrorBody(int bizCode, String msg) {
        String data;
        try {
            data = objectMapper.writeValueAsString(new ErrorPayload(bizCode, msg));
        } catch (Exception ex) {
            // JSON 序列化失败时退化为转义后的极简体，保证事件格式仍合法
            data = "{\"code\":" + bizCode + ",\"msg\":\"\"}";
        }
        return "event:error\ndata:" + data + "\n\n";
    }

    /** 错误事件 data 载荷（字段与平台统一返回体的 code/msg 语义对齐）。 */
    record ErrorPayload(int code, String msg) {
    }
}
