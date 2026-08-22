# 统计驾驶舱 statistics（yiliao-statistics / 8087）

> 文档类型：业务模块文档　所属模块：statistics　依赖文档：[global/10, global/20, global/30]
> 版本：v1.0（2026-08-22）　阅读优先级：中　风险车道：绿区

## 1. 职责边界与能力清单

- 数据驾驶舱：在管人数、随访完成率、失访率、预警数、结节类型/风险分布、随访及时率趋势、医生工作量。
- 数据来源：**XXL-Job 每小时经 Feign 分页拉取各服务数据 → 预聚合写入本库快照表 → 接口直查快照**（【global/30 §4】禁止跨库直连）。
- **不做**：任何写操作业务数据；实时明细查询（明细回各服务页面）。

## 2. 数据模型

库 `yiliao_stats`（自建快照表，DDL 本服务定义并登记于此）：

| 表 | 粒度 | 关键字段 |
|---|---|---|
| stats_overview_daily | 日 | 在管患者数/进行中计划数/逾期任务数/预警数 |
| stats_followup_monthly | 月 | 应随访数/完成数/及时数 → 完成率、及时率 |
| stats_nodule_dist | 快照 | nodule_type × risk 分布计数 |
| stats_doctor_workload | 日×医生 | 建档数/完成任务数/预警处理数 |

## 3. 接口定义

REST（/api/stats）：`GET /overview`、`GET /nodule-distribution`、`GET /followup-rate?months=6`、`GET /doctor-workload`。全部只读、走快照表。

## 4. 业务规则

1. 预聚合 Job：每小时整点分页拉 `PatientApi/FollowupApi/NoduleApi`，按 bizKey 幂等 upsert 快照（`stats:{table}:{dim}:{date}`）。
2. 指标口径固化在 SQL/服务层并注释来源：完成率=已完成任务/应完成任务（按计划日期 <= 今日）；及时率=done_date<=plan_date 的占比；失访率=失访档案/在管档案。
3. 前端图表（ECharts）由本服务直接出聚合 JSON，格式与前端组件约定（series/categories）。

## 5. 异常场景

- 上游 Feign 拉取失败：跳过本轮，保留上一轮快照；连续 3 轮失败告警。驾驶舱展示"数据更新于 {上一轮时间}"。
- 快照表空（首次启动）：接口返回空集合+提示，不报错。

## 6. 禁止行为

1. 禁止直连其他服务数据库或做跨库 JOIN。
2. 禁止在统计接口做实时全量聚合查询（必须走快照）。
3. 禁止输出含 PII 的明细统计（一律聚合维度；医生工作量仅对 ADMIN/科室维度可见）。

## 7. 依赖声明

- 公共能力：common-data、common-web、common-feign。
- 下游调用：`PatientApi`、`FollowupApi`、`NoduleApi`、`AuthApi`。
- 中间件：MySQL(yiliao_stats)、XXL-Job、Caffeine（快照缓存 10min）。

## 8. 最小调用示例

```bash
curl -H "Authorization: Bearer {adminToken}" \
     "http://localhost:8080/api/stats/followup-rate?months=6"
# → data: {"months":["2026-03",...],"completionRate":[0.91,...],"timelyRate":[0.84,...]}
```

## 变更记录

| 版本 | 日期 | 变更 | 原因 |
|---|---|---|---|
| v1.0 | 2026-08-22 | 首版；明确预聚合取数路径（修正原设计"MySQL 聚合"在分库后不可行） | 分层文档体系建立 |
