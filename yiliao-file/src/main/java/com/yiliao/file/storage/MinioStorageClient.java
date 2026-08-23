package com.yiliao.file.storage;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * MinIO 客户端封装（specs/modules/file.md §1）：仅预签名能力，客户端在构造时创建、不预连接。
 * 异常不在此层捕获，统一向上抛由 service 层转译为业务错误。
 */
@Component
public class MinioStorageClient {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioStorageClient(@Value("${yiliao.file.endpoint}") String endpoint,
                              @Value("${yiliao.file.access-key}") String accessKey,
                              @Value("${yiliao.file.secret-key}") String secretKey,
                              @Value("${yiliao.file.bucket}") String bucket) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
    }

    /** 生成限时上传预签名 URL（specs §4.1：上传 600 秒，禁止永久链接）。 */
    public String presignPut(String objectKey, int expirySeconds) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(bucket)
                .object(objectKey)
                .expiry(expirySeconds, TimeUnit.SECONDS)
                .build());
    }

    /** 生成限时下载预签名 URL（specs §4.1：下载 1800 秒，仅私有桶限时访问）。 */
    public String presignGet(String objectKey, int expirySeconds) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectKey)
                .expiry(expirySeconds, TimeUnit.SECONDS)
                .build());
    }
}
