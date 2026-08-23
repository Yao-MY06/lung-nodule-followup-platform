package com.yiliao.file.service;

import com.yiliao.api.file.dto.PresignUploadRequest;
import com.yiliao.api.file.dto.PresignVO;
import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.file.error.FileErrorCode;
import com.yiliao.file.storage.MinioStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 预签名服务（specs/modules/file.md §4）：
 * 白名单（pdf/jpg/jpeg/png/dcm）+ 大小上限（100MB）+ 对象键规范（{bizType}/{yyyyMM}/{uuid}.{ext}）。
 * 对象键不含患者姓名等明文；签名 URL 有效期上传 600 秒 / 下载 1800 秒。
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    /** 上传后缀白名单（specs §4.2：dcm 为 DICOM 预留；校验忽略大小写）。 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "dcm");

    /** 单文件大小上限 100MB（specs §4.2）。 */
    private static final long MAX_SIZE_BYTES = 100L * 1024 * 1024;

    /** 对象键允许的业务前缀白名单：检查报告附件 / 宣教素材（specs §4.3）。 */
    private static final List<String> ALLOWED_BIZ_PREFIXES = List.of("EXAM_REPORT/", "EDUCATION/");

    /** 对象键字符白名单：字母/数字/下划线/连字符/斜杠/点，其余一律拒绝（防注入与穿越）。 */
    private static final Pattern OBJECT_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-/\\.]+$");

    private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

    /** 预签名有效期：上传 10 分钟 / 下载 30 分钟（specs §4.1）。 */
    static final int UPLOAD_EXPIRE_SECONDS = 600;
    static final int DOWNLOAD_EXPIRE_SECONDS = 1800;

    private final MinioStorageClient storageClient;

    public FileService(MinioStorageClient storageClient) {
        this.storageClient = storageClient;
    }

    /** 预签名上传：校验通过后按对象键规范生成键并返回限时 PUT URL。 */
    public PresignVO presignUpload(PresignUploadRequest request) {
        String ext = extensionOf(request.filename());
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BizException(FileErrorCode.FILE_TYPE_REJECTED.withMsg(
                    "仅支持 " + String.join("/", ALLOWED_EXTENSIONS) + " 格式"));
        }
        long sizeBytes = request.sizeBytes() == null ? -1 : request.sizeBytes();
        if (sizeBytes <= 0 || sizeBytes > MAX_SIZE_BYTES) {
            throw new BizException(FileErrorCode.FILE_TOO_LARGE.withMsg("单文件不得超过 100MB"));
        }
        String bizType = request.bizType().trim();
        // bizType 会拼入对象键，必须命中前缀白名单，否则下载侧校验永远无法通过（26003）
        if (ALLOWED_BIZ_PREFIXES.stream().noneMatch(p -> p.equalsIgnoreCase(bizType + "/"))) {
            throw new BizException(FileErrorCode.OBJECT_KEY_INVALID.withMsg(
                    "业务类型必须在 " + ALLOWED_BIZ_PREFIXES + " 范围内"));
        }
        // 对象键不含患者姓名等明文（specs §4.3），UUID 规避文件名猜测与覆盖
        String objectKey = bizType.toUpperCase(Locale.ROOT) + "/"
                + YearMonth.now().format(YYYYMM) + "/"
                + UUID.randomUUID() + "." + ext;
        return new PresignVO(objectKey, presign(() -> storageClient.presignPut(objectKey, UPLOAD_EXPIRE_SECONDS)),
                UPLOAD_EXPIRE_SECONDS);
    }

    /** 预签名下载：校验对象键格式与前缀白名单，防路径穿越，返回限时 GET URL。 */
    public PresignVO presignDownload(String objectKey) {
        validateObjectKey(objectKey);
        return new PresignVO(objectKey, presign(() -> storageClient.presignGet(objectKey, DOWNLOAD_EXPIRE_SECONDS)),
                DOWNLOAD_EXPIRE_SECONDS);
    }

    /** 对象键合法性：字符白名单 + 禁止 ".." 路径穿越 + 业务前缀白名单。 */
    private void validateObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()
                || !OBJECT_KEY_PATTERN.matcher(objectKey).matches()) {
            throw new BizException(FileErrorCode.OBJECT_KEY_INVALID);
        }
        if (objectKey.contains("..")) {
            throw new BizException(FileErrorCode.OBJECT_KEY_INVALID.withMsg("对象键禁止路径穿越"));
        }
        if (ALLOWED_BIZ_PREFIXES.stream().noneMatch(objectKey::startsWith)) {
            throw new BizException(FileErrorCode.OBJECT_KEY_INVALID.withMsg("对象键前缀不在白名单内"));
        }
    }

    /** 提取小写后缀；无后缀返回 null。 */
    private static String extensionOf(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        // 无点或点在末尾（如 "a."）均视为无合法后缀；点在首位（如 ".pdf"）为纯后缀文件名，同样拒绝
        if (dot <= 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** MinIO 调用统一转译（specs §5：MinIO 不可用返回 500+建议稍后重试）；日志禁止记录完整签名 URL。 */
    private String presign(PresignCall call) {
        try {
            return call.get();
        } catch (Exception e) {
            log.warn("MinIO 预签名失败：{}", e.getMessage());
            throw new BizException(CommonErrorCode.SYSTEM_ERROR.withMsg("存储服务暂不可用，请稍后重试"));
        }
    }

    @FunctionalInterface
    private interface PresignCall {
        String get() throws Exception;
    }
}
