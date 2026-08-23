package com.yiliao.file.controller;

import com.yiliao.api.file.dto.PresignUploadRequest;
import com.yiliao.api.file.dto.PresignVO;
import com.yiliao.common.core.result.Result;
import com.yiliao.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 附件 REST 接口（specs/modules/file.md §3）：预签名上传/下载，不提供直传/直下。
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "预签名上传（返回对象键+限时 PUT URL）")
    @PostMapping("/presign/upload")
    public Result<PresignVO> upload(@Valid @RequestBody PresignUploadRequest request) {
        return Result.ok(fileService.presignUpload(request));
    }

    @Operation(summary = "预签名下载（返回限时 GET URL）")
    @GetMapping("/presign/download")
    public Result<PresignVO> download(@RequestParam("objectKey") String objectKey) {
        return Result.ok(fileService.presignDownload(objectKey));
    }
}
