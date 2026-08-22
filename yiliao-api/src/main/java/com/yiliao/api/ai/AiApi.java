package com.yiliao.api.ai;

import com.yiliao.api.ai.dto.NoduleExtract;
import com.yiliao.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.constraints.NotBlank;

/**
 * ai 服务对外契约（specs/global/30 §5）：报告结构化抽取，供 nodule 调用。
 */
@FeignClient(name = "yiliao-ai", contextId = "aiApi")
public interface AiApi {

    @PostMapping("/api/ai/internal/report/extract")
    Result<NoduleExtract> extractReport(@RequestBody ExtractRequest request);

    record ExtractRequest(@NotBlank String rawText) {
    }
}
