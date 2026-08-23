package com.yiliao.api.file.dto;

/**
 * 预签名结果（specs/modules/file.md §4.1）：url 限时有效（上传 600 秒 / 下载 1800 秒），
 * 禁止生成永久公开链接；objectKey 由业务方持久化到附件归属方（如 exam_report）。
 */
public record PresignVO(
        String objectKey,
        String url,
        int expireSeconds
) {
}
