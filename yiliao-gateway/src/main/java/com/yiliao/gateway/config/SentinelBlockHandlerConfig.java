package com.yiliao.gateway.config;

import com.alibaba.csp.sentinel.adapter.spring.webflux.callback.WebFluxCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Sentinel 限流响应统一为 Result{code:11001}（specs/modules/gateway.md §4.2）。
 * 默认实现返回裸 429 文本，这里替换为统一返回体 JSON。
 */
@Configuration
public class SentinelBlockHandlerConfig {

    @PostConstruct
    public void registerBlockHandler() {
        WebFluxCallbackManager.setBlockHandler((exchange, throwable) ->
                ServerResponse.status(429)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{\"code\":11001,\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}"));
    }
}
