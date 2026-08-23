package com.yiliao.api.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 预签名上传请求（specs/modules/file.md §3）：bizType 限定业务类型白名单，
 * filename 仅取后缀做白名单校验，sizeBytes 由服务端校验上限，均不落库。
 */
public record PresignUploadRequest(
        @NotBlank String bizType,
        @NotBlank String filename,
        @NotNull Long sizeBytes
) {
}
