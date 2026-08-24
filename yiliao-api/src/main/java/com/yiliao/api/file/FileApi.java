package com.yiliao.api.file;

import com.yiliao.api.file.dto.PresignUploadRequest;
import com.yiliao.api.file.dto.PresignVO;
import com.yiliao.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * file 服务对外契约（specs/global/30 §5）。
 * 方法路径为绝对路径（服务端实现类直接映射）；internal 路径仅供服务间调用，网关拒绝外部访问。
 */
@FeignClient(name = "yiliao-file", url = "${yiliao.feign.file-url:http://localhost:8088}", contextId = "fileApi")
public interface FileApi {

    @PostMapping("/api/file/internal/presign/upload")
    Result<PresignVO> presignUpload(@RequestBody PresignUploadRequest request);

    @GetMapping("/api/file/internal/presign/download")
    Result<PresignVO> presignDownload(@RequestParam("objectKey") String objectKey);
}
