package com.yiliao.common.feign;

import com.yiliao.common.core.constant.YiliaoConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 内部头透传（specs/global/30 §7）：把网关注入的用户头原样带给下游，
 * 保证跨服务调用链上登录态不丢。无请求上下文（MQ 消费/定时任务）时跳过。
 */
public class HeaderRelayInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        relay(template, request, YiliaoConstants.HEADER_USER_ID);
        relay(template, request, YiliaoConstants.HEADER_USER_ROLES);
        relay(template, request, YiliaoConstants.HEADER_TRACE_ID);
    }

    private void relay(RequestTemplate template, HttpServletRequest request, String header) {
        String value = request.getHeader(header);
        if (value != null && !value.isBlank()) {
            template.header(header, value);
        }
    }
}
