package com.yiliao.api.notify;

import com.yiliao.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * notification 服务对外契约（specs/global/30 §5）：即时站内信，同样过 bizKey 幂等。
 */
@FeignClient(name = "yiliao-notification", contextId = "notifyApi")
public interface NotifyApi {

    @PostMapping("/api/notify/internal/messages")
    Result<Long> sendInternalMessage(@RequestBody InternalMessageRequest request);

    record InternalMessageRequest(
            @NotNull Long receiverId,
            @NotBlank String title,
            @NotBlank String content,
            @NotBlank String bizKey
    ) {
    }
}
