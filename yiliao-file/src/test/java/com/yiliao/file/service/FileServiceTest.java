package com.yiliao.file.service;

import com.yiliao.api.file.dto.PresignUploadRequest;
import com.yiliao.api.file.dto.PresignVO;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.file.storage.MinioStorageClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * FileService 校验规则测试（specs/modules/file.md §4）：白名单/大小上限/对象键规范/路径穿越。
 * MinIO 客户端 mock 掉，仅验证业务校验与对象键生成。
 */
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    private static final long MB = 1024L * 1024;

    @Mock
    private MinioStorageClient storageClient;

    @InjectMocks
    private FileService fileService;

    @Test
    void 合法上传_pdf_通过且对象键符合规范() throws Exception {
        when(storageClient.presignPut(anyString(), anyInt())).thenReturn("http://minio/fake-put-url");
        PresignUploadRequest request = new PresignUploadRequest("EXAM_REPORT", "ct-20260910.pdf", MB);

        PresignVO vo = fileService.presignUpload(request);

        // 对象键 = bizType/yyyyMM/uuid.pdf：前缀、6 位年月、36 位 UUID 段
        assertTrue(vo.objectKey().matches("EXAM_REPORT/\\d{6}/[0-9a-f-]{36}\\.pdf"),
                "对象键不符合规范: " + vo.objectKey());
        assertEquals("http://minio/fake-put-url", vo.url());
        assertEquals(600, vo.expireSeconds());
    }

    @Test
    void 上传_非法后缀exe_拒绝26001() {
        PresignUploadRequest request = new PresignUploadRequest("EXAM_REPORT", "virus.exe", MB);

        BizException e = assertThrows(BizException.class, () -> fileService.presignUpload(request));
        assertEquals(26001, e.getCode());
    }

    @Test
    void 上传_超大文件200MB_拒绝26002() {
        PresignUploadRequest request = new PresignUploadRequest("EXAM_REPORT", "huge.pdf", 200 * MB);

        BizException e = assertThrows(BizException.class, () -> fileService.presignUpload(request));
        assertEquals(26002, e.getCode());
    }

    @Test
    void 下载_对象键含路径穿越_拒绝26003() {
        BizException e = assertThrows(BizException.class,
                () -> fileService.presignDownload("EXAM_REPORT/../../etc/passwd"));
        assertEquals(26003, e.getCode());
    }

    @Test
    void 下载_非法前缀tmp_拒绝26003() {
        BizException e = assertThrows(BizException.class,
                () -> fileService.presignDownload("tmp/202609/xxx.pdf"));
        assertEquals(26003, e.getCode());
    }

    @Test
    void 合法下载_返回预签名URL() throws Exception {
        when(storageClient.presignGet(anyString(), anyInt())).thenReturn("http://minio/fake-get-url");

        PresignVO vo = fileService.presignDownload("EXAM_REPORT/202609/abc-123.pdf");

        assertEquals("EXAM_REPORT/202609/abc-123.pdf", vo.objectKey());
        assertEquals("http://minio/fake-get-url", vo.url());
        assertEquals(1800, vo.expireSeconds());
    }
}
