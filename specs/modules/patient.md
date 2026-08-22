# 患者档案 patient（yiliao-patient / 8082）

> 文档类型：业务模块文档　所属模块：patient　依赖文档：[global/10, global/20, global/30, flows/F1]
> 版本：v1.0（2026-08-22）　阅读优先级：高　风险车道：黄区（含 PII）

## 1. 职责边界与能力清单

- 患者档案 CRUD：基本信息、危险因素、肺癌诊断（分期/病理/基因/手术）。
- 档案阶段标签（StageLabel）流转：结节随访中 → 术后随访中/治疗中/MDT中/失访/结案。
- 主管医生绑定与变更；患者全景时间轴聚合（Feign 组装）。
- **不做**：结节与检查数据（归 nodule）、随访计划（归 followup）。

## 2. 数据模型

库 `yiliao_patient`，完整 DDL 见 make/技术设计文档 §7。

| 表 | 关键点 |
|---|---|
| patient_archive | `patient_no` 唯一；`id_card` 加密存储、出参脱敏；`stage_label` 见枚举；索引 idx_doctor/idx_stage |
| patient_risk_factor | 每患者一条（uk_patient）；吸烟包年/家族史/职业暴露/既往肿瘤史/合并症——**直接影响随访策略选择** |
| patient_diagnosis | 一患者多条；TNM/临床分期/病理/基因/是否辅助治疗——随访模板匹配输入 |

## 3. 接口定义

REST（/api/patient）：

| 方法路径 | 说明 |
|---|---|
| POST /archives | 建档（**Seata 全局事务 TM**，串联 nodule 快照与 followup 计划，见 F1） |
| GET /archives | 分页（按主管医生/阶段标签/关键字） |
| GET /archives/{id} | 详情（含危险因素、诊断） |
| PUT /archives/{id} · /{id}/stage · /{id}/doctor | 修改 / 阶段变更 / 换主管医生 |
| GET /archives/{id}/timeline | 全景时间轴（结节快照+随访+报告，Feign 聚合 NoduleApi/FollowupApi） |

Feign 契约 `PatientApi`：`getArchive(id)`、`pageArchives(query)`。

MQ：生产 `YILIAO_ARCHIVE_CREATED`（建档成功后，followup 兜底对账）。

## 4. 业务规则

1. 建档唯一性：`patient_no` 唯一索引 + Redisson 锁 `yiliao:lock:patient:create:{idCardHash}` 防重复提交。
2. 阶段变更只允许合法流转（状态机）：失访→随访中需随访专员操作；结案不可直接回退（需重建档案，毕设简化）。
3. PII：身份证 AES 加密存库（密钥 Nacos）；出参经 `@Sensitive` 脱敏（【global/10 §9】）。
4. 危险因素/诊断变更后发领域事件（后续可扩展），当前版本只在新诊断录入时提示医生"建议重新匹配随访方案"。

## 5. 异常场景

- Seata 链路失败：整体回滚，返回明确错误码与"请重试或联系管理员"；不做部分成功。
- 时间轴聚合部分下游超时：返回已有片段 + 标注 `partial:true`（Sentinel 降级），不整页失败。

## 6. 禁止行为

1. 禁止未脱敏返回身份证/手机号；禁止把 PII 写入日志。
2. 禁止绕过阶段状态机直接 update stage_label。
3. 禁止在 patient 库存结节/随访数据（跨库直连违反【global/30 §4】）。

## 7. 依赖声明

- 公共能力：common-data、common-web、common-feign；Seata。
- 下游调用：`NoduleApi`、`FollowupApi`、`AuthApi`。
- 中间件：MySQL(yiliao_patient)、Redis、RocketMQ、Seata。

## 8. 最小调用示例

```bash
curl -X POST http://localhost:8080/api/patient/archives \
  -H "Authorization: Bearer {token}" -H "Content-Type: application/json" \
  -d '{"name":"张三","gender":1,"birthDate":"1965-03-12","phone":"138****5678",
       "riskFactor":{"smokingPackYear":30,"familyHistory":1},"sourceType":1}'
```

## 变更记录

| 版本 | 日期 | 变更 | 原因 |
|---|---|---|---|
| v1.0 | 2026-08-22 | 首版，收敛自 make/技术设计文档 §7/§13 | 分层文档体系建立 |
