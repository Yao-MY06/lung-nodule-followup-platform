package com.yiliao.ai.feign;

import com.yiliao.ai.extract.ReportExtractService;
import com.yiliao.api.ai.AiApi;
import com.yiliao.api.ai.dto.NoduleExtract;
import com.yiliao.common.core.result.Result;
import org.springframework.web.bind.annotation.RestController;

/**
 * AiApi 契约实现（specs/global/30 §5）：nodule 报告录入后同步调用。
 */
@RestController
public class AiApiController implements AiApi {

    private final ReportExtractService extractService;

    public AiApiController(ReportExtractService extractService) {
        this.extractService = extractService;
    }

    @Override
    public Result<NoduleExtract> extractReport(ExtractRequest request) {
        return Result.ok(extractService.extract(request.rawText()));
    }
}
