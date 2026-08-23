package com.yiliao.file.controller.feign;

import com.yiliao.api.file.FileApi;
import com.yiliao.api.file.dto.PresignUploadRequest;
import com.yiliao.api.file.dto.PresignVO;
import com.yiliao.common.core.result.Result;
import com.yiliao.file.service.FileService;
import org.springframework.web.bind.annotation.RestController;

/**
 * FileApi 契约实现（specs/global/30 §5）：internal 路径仅供服务间调用。
 * 契约接口方法已写绝对路径映射，本类禁止类级 @RequestMapping（否则路径叠加出错）。
 */
@RestController
public class FileApiController implements FileApi {

    private final FileService fileService;

    public FileApiController(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public Result<PresignVO> presignUpload(PresignUploadRequest request) {
        return Result.ok(fileService.presignUpload(request));
    }

    @Override
    public Result<PresignVO> presignDownload(String objectKey) {
        return Result.ok(fileService.presignDownload(objectKey));
    }
}
