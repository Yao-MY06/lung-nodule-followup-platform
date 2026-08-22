# 文件服务 file（yiliao-file / 8088）

> 文档类型：业务模块文档　所属模块：file　依赖文档：[global/10, global/20, global/30]
> 版本：v1.0（2026-08-22）　阅读优先级：低　风险车道：绿区

## 1. 职责边界与能力清单

- MinIO 封装：预签名上传/下载 URL、对象元数据登记、脱敏预览（后缀白名单）。
- 最薄服务：三个能力，无业务规则；P5 阶段实现。
- **不做**：业务附件归属管理（归属方如 exam_report 只存对象键）。

## 2. 数据模型

无业务库（可选 Redis 存预签名状态）。MinIO 桶规划：`yiliao-report`（报告附件，私有）、`yiliao-public`（宣教图片，公开读）。

## 3. 接口定义

REST（/api/file）：`POST /presign/upload`（入参 bizType/文件名/大小→返回 URL+对象键）、`GET /presign/download/{objectKey}`（限时 URL）。

Feign 契约 `FileApi`：`presignUpload(bizType)`、`presignDownload(objectKey)`。

## 4. 业务规则

1. 预签名有效期：上传 10 分钟、下载 30 分钟；**禁止**生成永久公开链接访问私有桶。
2. 上传白名单：pdf/jpg/png/dcm（DICOM 预留）；单文件 ≤100MB。
3. 对象键规范：`{bizType}/{patientId}/{yyyyMM}/{uuid}.{ext}`，不含患者姓名等明文信息。

## 5. 异常场景

- MinIO 不可用：预签名接口返回 500 + 建议稍后重试；不影响报告文本录入（附件为可选）。

## 6. 禁止行为

1. 禁止任何绕过预签名的直传/直下接口。
2. 禁止对象键包含姓名/身份证等明文；禁止日志记录完整签名 URL。

## 7. 依赖声明

- 中间件：MinIO；无数据库。

## 8. 最小调用示例

```bash
curl -H "Authorization: Bearer {token}" \
     -X POST http://localhost:8080/api/file/presign/upload \
     -d '{"bizType":"EXAM_REPORT","filename":"ct-20260910.pdf","sizeBytes":1048576}'
# → data:{"objectKey":"EXAM_REPORT/1001/202609/3f2a....pdf","uploadUrl":"https://minio..."}
```

## 变更记录

| 版本 | 日期 | 变更 | 原因 |
|---|---|---|---|
| v1.0 | 2026-08-22 | 首版 | 分层文档体系建立 |
