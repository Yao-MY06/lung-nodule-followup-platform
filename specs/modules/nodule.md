# 结节与检查 nodule（yiliao-nodule / 8083）

> 文档类型：业务模块文档　所属模块：nodule　依赖文档：[global/10, global/20, global/30, flows/F1, flows/F3]
> 版本：v1.0（2026-08-22）　阅读优先级：高　风险车道：黄区（对比判断影响临床决策）

## 1. 职责边界与能力清单

- 结节登记与全生命周期状态（随访中/已手术/排除）；每次复查生成快照，支持**纵向对比**。
- 检查检验记录：CT/PET-CT/MRI/标志物/病理/基因报告；报告原文存档并对接 AI 抽取（AiApi）。
- 快照对比判断（≥2mm 规则）并发布进展事件。
- **不做**：随访计划（归 followup）、AI 推理（归 ai-service）。

## 2. 数据模型

库 `yiliao_nodule`，完整 DDL 见 make/技术设计文档 §8。

| 表 | 关键点 |
|---|---|
| nodule | 患者内编号 N1/N2…；`nodule_type` 决定随访规则分支 |
| nodule_snapshot | 每次复查一条；`max_diameter_mm`/`solid_diameter_mm` 是对比核心字段；`is_new` 新发结节单独标记 |
| exam_report | `raw_text` 为 AI 抽取输入；`structured_json` 存抽取结果；`extract_status` 见枚举（0未抽取 1已抽取 2已确认） |
| lab_result | 肿瘤标志物趋势（CEA 等 5 项） |

## 3. 接口定义

REST（/api/nodule、/api/exam）：

| 方法路径 | 说明 |
|---|---|
| POST /nodules · POST /snapshots | 登记结节 / 录入复查快照（**触发对比分析**） |
| GET /nodules/{id}/trend | 纵向趋势序列（直径/体积/密度，前端折线图） |
| GET /nodules/{id}/compare | 最近两次快照对比（变化量、是否≥2mm、建议动作） |
| POST /exam/reports · GET /exam/reports | 报告录入（可触发 AI 抽取）/ 列表 |
| POST /exam/reports/{id}/confirm | 医生确认 AI 抽取结果入库（extract_status→2） |

Feign 契约 `NoduleApi`：`getTrend(patientId)`、`latestSnapshot(noduleId)`、`reportSummary(patientId)`。

MQ：生产 `YILIAO_EXTRACT_CONFIRMED`（确认入库→followup 生成/更新计划）、`YILIAO_NODULE_PROGRESS`（进展→followup 升级评估 + notification 预警）。

## 4. 业务规则

1. **对比规则（核心）**：新快照与上一条比较 `max_diameter_mm` 与 `solid_diameter_mm`；任一增大 **≥2mm** → 判定进展，发 `YILIAO_NODULE_PROGRESS`；compare 接口同时返回"稳定/增大/新发"结论与建议动作文案。
2. AI 抽取流程：报告录入（raw_text）→ 调 `AiApi.extractReport` → 草稿写 `structured_json`（extract_status=1）→ 前端回填表单 → 医生确认（=2）才写 nodule/snapshot。**AI 只起草、人确认**（不变量，AGENTS.md）。
3. 新发结节 `is_new=1`：标记并在趋势接口高亮，随访策略匹配时作为高危因子传给 followup。
4. 报告附件上传走 `FileApi.presignUpload`，`file_url` 存 MinIO 对象键。

## 5. 异常场景

- AiApi 熔断/超时：报告正常入库（extract_status=0），前端提示"AI 抽取暂不可用，可手动录入"；不阻塞建档。
- 同日重复快照：允许（以 exam_date+report_id 区分），compare 默认取最新非同源快照。
- 快照缺少基线（首条）：compare 返回 `无基线`，不发生成进展事件。

## 6. 禁止行为

1. 禁止 AI 抽取结果未经确认直接写 nodule/nodule_snapshot。
2. 禁止在本服务内做随访间隔计算（规则归 followup 的 decision_rule）。
3. 禁止拿未脱敏 raw_text 直接调外部 LLM（走 ai-service 的脱敏过滤）。

## 7. 依赖声明

- 公共能力：common-data、common-web、common-feign。
- 下游调用：`AiApi`（抽取）、`FileApi`（附件）。
- 中间件：MySQL(yiliao_nodule)、MinIO、RocketMQ。

## 8. 最小调用示例

```bash
curl -X POST http://localhost:8080/api/nodule/snapshots \
  -H "Authorization: Bearer {token}" -H "Content-Type: application/json" \
  -d '{"noduleId":101,"examDate":"2026-09-10","maxDiameterMm":8.5,"solidDiameterMm":6.0,"reportId":9001}'
# → data.compare: {"deltaMax":2.5,"progress":true,"advice":"建议升级随访策略，已通知医生"}
```

## 变更记录

| 版本 | 日期 | 变更 | 原因 |
|---|---|---|---|
| v1.0 | 2026-08-22 | 首版，收敛自 make/技术设计文档 §8/§14/§18 | 分层文档体系建立 |
