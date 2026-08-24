package com.yiliao.api.nodule;

import com.yiliao.api.nodule.dto.SnapshotDTO;
import com.yiliao.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * nodule 服务对外契约（specs/global/30 §5）。
 */
@FeignClient(name = "yiliao-nodule", url = "${yiliao.feign.nodule-url:http://localhost:8083}", contextId = "noduleApi")
public interface NoduleApi {

    @GetMapping("/api/nodule/internal/nodules/patient/{patientId}/trend")
    Result<List<SnapshotDTO>> getTrend(@PathVariable("patientId") Long patientId,
                                       @RequestParam(value = "noduleId", required = false) Long noduleId);

    @GetMapping("/api/nodule/internal/reports/{patientId}/summary")
    Result<String> reportSummary(@PathVariable("patientId") Long patientId);
}
