# 随访引擎 followup（yiliao-followup / 8084）—— 系统核心

> 文档类型：业务模块文档　所属模块：followup　依赖文档：[global/10, global/20, global/30, flows/F1, flows/F2, flows/F3]
> 版本：v1.0（2026-08-22）　阅读优先级：高　风险车道：黄区（规则引擎/契约）

## 1. 职责边界与能力清单

- **表驱动规则引擎**：`decision_rule` 条件匹配 → `followup_template` 方案 → 生成计划与任务时间轴。
- 随访工作台（临期/逾期/失访待办）、随访记录、任务调整。
- 复查结果驱动的计划自适应（升级/续期）；ePRO 症状上报与阈值预警。
- XXL-Job 逾期/失访扫描；提醒排程（发延迟消息给 notification）。
- **不做**：结节数据（归 nodule）、消息发送（归 notification）。

## 2. 数据模型

库 `yiliao_followup`，完整 DDL 见 make/技术设计文档 §9。

| 表 | 关键点 |
|---|---|
| followup_template | 指南方案模板；`items_json` 每次随访项目清单；`scene` 见 SceneType |
| decision_rule | `cond_json` 条件（nodule_type/max_dia/risk/stage/gene…）；`result_template_id` + 首次/重复间隔 + 总年限 + `priority`（多规则命中取最高） |
| followup_plan | 每患者同时仅一个进行中（`uk_patient_active(patient_id,status)`） |
| followup_task | 高频表；`status` 见 TaskStatus；`idx_scan(status,plan_date)` 供 XXL-Job 扫描 |
| followup_record | 电话/门诊/问卷执行记录，支持复制上次 |
| symptom_report | ePRO；severity≥3 置 alert_flag 并发预警事件 |

种子数据：第三章指南规则表（Lung-RADS / 2024 共识 / 术后随访）固化为 `docker/init-sql/yiliao_followup.sql` 的 rule+template 初始数据。

## 3. 接口定义

REST（/api/followup）：

| 方法路径 | 说明 |
|---|---|
| GET /templates · GET/POST/PUT /rules | 模板查询 / 决策规则维护（管理员，**红区接口**） |
| POST /plans/generate | 手动触发计划生成（规则匹配 + Redisson 防重） |
| GET /plans/{patientId} | 患者计划 + 任务时间轴 |
| PUT /tasks/{id}/adjust | 调整单次任务日期/项目（记录 adjust_reason） |
| GET /workbench | 工作台（状态筛选，按 plan_date 排序） |
| POST /records | 提交随访记录（完成任务 → 触发下一期计算） |
| POST /symptoms | 症状上报（阈值判断 → 预警） |
| GET /lost | 失访名单与追踪记录 |

Feign 契约 `FollowupApi`：`nextFollowup(patientId)`、`planTimeline(patientId)`。

MQ：消费 `YILIAO_ARCHIVE_CREATED`（建档兜底）、`YILIAO_EXTRACT_CONFIRMED`（确认→生成计划）、`YILIAO_NODULE_PROGRESS`（进展→升级评估）；生产 `YILIAO_PLAN_CREATED`、`YILIAO_REMIND_DUE`（定时消息）、`YILIAO_SYMPTOM_ALERT`。

## 4. 业务规则

1. **规则匹配算法**：输入 = 结节特征（nodule_type/max_dia/risk）或术后特征（分期/基因/辅助治疗）→ 加载启用规则（两级缓存）→ 按条件求值 → 命中多条取 `priority` 最高 → 取模板生成 `followup_plan` + 按 `first_interval_month/repeat_interval_month` 铺未来任务直至 `total_years`。**全程无 if-else 硬编码间隔**。
2. 防重生成：Redisson 锁 `yiliao:lock:plan:{patientId}` + `uk_patient_active` 兜底；重复请求返回 `22001 该患者已有进行中计划`。
3. 提醒排程：任务生成时计算 T-7/T-3/T-1 三个提醒点 → 投递 RocketMQ 5.x 定时消息（bizKey=`taskId:REMIND_T7` 等），由 notification 消费。
4. 升级评估：消费 `YILIAO_NODULE_PROGRESS` → 以更高优先级规则（更短间隔模板）重算**剩余未完成任务**，保留历史任务记录；adjust_reason 记录"结节进展自动升级"。
5. 逾期/失访扫描（XXL-Job 每日 02:00，分片广播按 patient_id 取模）：未完成且 plan_date < today-7 → 逾期并推送主管医生；逾期且 < today-30 → 疑似失访；提醒≥3 次未响应 → 失访名单。
6. 任务完成：提交 followup_record → task.status=3 → 按"当前最新快照特征"重新匹配规则生成下一期任务（支持稳定后降级随访）。

## 5. 异常场景

- 规则匹配无命中：计划生成失败并返回"无可用随访规则，请维护 decision_rule"，**禁止**写默认间隔兜底（避免无指南依据的计划）。
- 模板/规则缓存与 DB 不一致：接受本地缓存 ≤5min 延迟（【global/30 §8】取舍）；规则维护接口保存后主动失效 Redis 层。
- 定时消息投递失败：本地 `remind_outbox` 思路——task.remind_sent 计数 + XXL-Job 每小时对账补投（bizKey 幂等保证不重复）。

## 6. 禁止行为

1. 禁止任何随访间隔/年限硬编码进 Java（含"默认 6 个月"之类兜底）。
2. 禁止直接改 followup_task.status 绕过状态机（未到期→临期→逾期/完成→失访/取消）。
3. 禁止 decision_rule 维护接口暴露给非管理员；规则变更必须留操作日志。
4. 禁止在本服务发送短信/站内信（只发事件，由 notification 统一执行）。

## 7. 依赖声明

- 公共能力：common-data、common-web、common-feign、common-mq（幂等基类）。
- 下游调用：`PatientApi`、`NoduleApi`（取最新快照特征）。
- 中间件：MySQL(yiliao_followup)、Redis+Caffeine、Redisson、RocketMQ、XXL-Job。

## 8. 最小调用示例

```bash
curl -X POST http://localhost:8080/api/followup/plans/generate \
  -H "Authorization: Bearer {token}" -H "Content-Type: application/json" \
  -d '{"patientId":1001,"scene":1,"input":{"noduleType":3,"maxDiaMm":4.0,"risk":false}}'
# → data: {"planId":5,"template":"pGGN≤5mm：首6mo后年度","tasks":[...未来任务时间轴...]}
```

## 变更记录

| 版本 | 日期 | 变更 | 原因 |
|---|---|---|---|
| v1.0 | 2026-08-22 | 首版，收敛自 make/技术设计文档 §9/§15 | 分层文档体系建立 |
