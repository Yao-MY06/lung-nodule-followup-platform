# 消息中心 notification（yiliao-notification / 8085）

> 文档类型：业务模块文档　所属模块：notification　依赖文档：[global/10, global/20, global/30, flows/F2]
> 版本：v1.0（2026-08-22）　阅读优先级：中　风险车道：黄区（幂等/可靠性设计点）

## 1. 职责边界与能力清单

- 消费提醒事件并发送：站内信（必做）、短信（对接云厂商 SDK，可配置开关）、模板消息（扩展预留）。
- 消息模板管理、发送记录与送达状态、失败重试与死信兜底。
- 健康宣教内容管理与定向推送、随访问卷模板与答卷管理。
- **不做**：决定"何时提醒"（排程归 followup），只负责"到点发送"。

## 2. 数据模型

库 `yiliao_notify`，完整 DDL 见 make/技术设计文档 §10。

| 表 | 关键点 |
|---|---|
| message_record | `biz_key` 唯一（幂等键=taskId+remindType 或事件标识）；send_status 见 SendStatus |
| education_article | 分类/标签定向推送（绿区 CRUD） |
| questionnaire / questionnaire_answer | 量表模板（questions_json+score_rule_json）与答卷 |

## 3. 接口定义

REST（/api/notify）：`GET /messages`（我的消息）、`PUT /messages/{id}/read`、`POST /test-sms`（管理员通道自检）。

Feign 契约 `NotifyApi`：`sendInternalMessage(receiverId, title, content, bizKey)`（各服务即时站内信，同样过 biz_key 幂等）。

MQ 消费：`YILIAO_REMIND_DUE`（患者复查提醒，主链路）、`YILIAO_SYMPTOM_ALERT`（推送主管医生）、`YILIAO_NODULE_PROGRESS`（预警医生，与 followup 各自独立消费）。

## 4. 业务规则

1. **幂等三保险**（不变量）：消费入口 Redis SETNX(bizKey) → 写 message_record(`uk biz_key`) → 发送渠道。重复投递直接 ACK 丢弃。
2. 发送顺序：先站内信（必达，本地事务写库即算成功），短信异步可选渠道；短信失败不影响站内信结果。
3. 失败处理：渠道发送失败重试 3 次（间隔 1/5/30 分钟，延迟消息实现）→ 仍失败标记 send_status=2 并进死信告警，供人工补发。
4. 提醒文案模板化：`{{患者姓名}}，您预约{{日期}}的{{项目}}复查将至…`；模板变量缺失时降级通用文案并发开发告警。

## 5. 异常场景

- 短信厂商不可用：熔断（Sentinel）→ 只发站内信 + 待发队列，厂商恢复后补发（幂等键防重）。
- 消费异常（反序列化/数据缺失）：重试后进死信，**不阻塞**后续消息；死信每日汇总告警。
- Redis 不可用：退化为仅 DB 唯一键幂等（性能下降但正确性保持）。

## 6. 禁止行为

1. 禁止绕过 biz_key 幂等直接调短信 SDK（重复短信是事故）。
2. 禁止在短信/站内信中携带身份证号、诊断细节等敏感临床信息（只给时间/项目/复诊提示）。
3. 禁止消费逻辑里做业务决策（如改随访状态）——只做通知投递。

## 7. 依赖声明

- 公共能力：common-mq（幂等基类）、common-web、common-data。
- 上游事件：followup（REMIND_DUE/SYMPTOM_ALERT）、nodule（PROGRESS）。
- 中间件：MySQL(yiliao_notify)、RocketMQ、Redis、短信 SDK（Nacos 密钥）。

## 8. 最小调用示例

```
# 消费 YILIAO_REMIND_DUE 事件
BaseEvent{eventType=YILIAO_REMIND_DUE, bizKey="T1001:REMIND_T3",
          payload={patientId:1001, planDate:"2026-09-10", items:["胸部CT"]}}
→ SETNX yiliao:mq:consumed:...:T1001:REMIND_T3  # 首次成功
→ 站内信落库 + 短信异步发送 → message_record(biz_key="T1001:REMIND_T3")
```

## 变更记录

| 版本 | 日期 | 变更 | 原因 |
|---|---|---|---|
| v1.0 | 2026-08-22 | 首版，收敛自 make/技术设计文档 §10/§17/§23 | 分层文档体系建立 |
