package com.yiliao.followup.controller;

import com.yiliao.api.followup.dto.NextFollowupDTO;
import com.yiliao.common.core.page.PageQuery;
import com.yiliao.common.core.page.PageResult;
import com.yiliao.common.core.result.Result;
import com.yiliao.followup.entity.FollowupTask;
import com.yiliao.followup.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 随访任务/工作台接口（specs/modules/followup.md §3）。
 */
@RestController
@RequestMapping("/api/followup")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "随访工作台（状态筛选：0未到期 1临期 2逾期 3已完成 4失访）")
    @GetMapping("/workbench")
    public Result<PageResult<FollowupTask>> workbench(@RequestParam(required = false) Integer status,
                                                      @Valid PageQuery query) {
        return Result.ok(taskService.workbench(status, query));
    }

    @Operation(summary = "下次随访任务")
    @GetMapping("/patients/{patientId}/next")
    public Result<NextFollowupDTO> next(@PathVariable Long patientId) {
        return Result.ok(taskService.nextFollowup(patientId));
    }

    @Operation(summary = "调整单次任务日期/项目")
    @PutMapping("/tasks/{id}/adjust")
    public Result<Void> adjust(@PathVariable Long id,
                               @RequestParam(required = false) LocalDate planDate,
                               @RequestParam(required = false) String itemsJson) {
        taskService.adjust(id, planDate, itemsJson);
        return Result.ok();
    }

    @Operation(summary = "提交随访记录（完成任务）")
    @PutMapping("/tasks/{id}/record")
    public Result<Void> record(@PathVariable Long id, @Valid @RequestBody RecordRequest request) {
        taskService.completeTask(id, request.followupType(), request.content(),
                request.resultSummary(), request.nextAdvice(), request.operatorId());
        return Result.ok();
    }

    public record RecordRequest(
            @NotNull Integer followupType,
            @NotBlank String content,
            String resultSummary,
            String nextAdvice,
            Long operatorId
    ) {
    }
}
