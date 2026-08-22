# AI 服务 ai（yiliao-ai / 8086）

> 文档类型：业务模块文档　所属模块：ai　依赖文档：[global/10, global/20, global/30, flows/F1, flows/F4]
> 版本：v1.0（2026-08-22）　阅读优先级：高　风险车道：红区（PII 出网 / 输出安全）

## 1. 职责边界与能力清单

- A3 报告结构化抽取：CT 报告原文 → `NoduleExtract` JSON 草稿（供 nodule 人工确认）。
- A1 报告通俗解读：四段式输出，SSE 流式。
- A2 RAG 指南问答：混合检索 + Rerank + 引用溯源。
- Tool Calling 智能助手：注册 4 个工具，自然语言查真实数据/上报症状（见 F4）。
- **不做**：抽取结果直接入库（确认归 nodule）、业务决策（升级随访归 followup）。

## 2. 数据模型

库 `yiliao_ai` + PgVector，完整 DDL 见 make/技术设计文档 §11。

| 表 | 关键点 |
|---|---|
| ai_chat_session / ai_chat_message | 会话与消息；`tool_calls_json` 工具调用审计；`citations_json` RAG 出处 |
| knowledge_doc | 指南文档登记（名称/版本/切片数） |
| PgVector 切片表 | id/doc_id/chunk_text/embedding/metadata(文档名+章节) |

## 3. 接口定义

REST（/api/ai）：

| 方法路径 | 说明 |
|---|---|
| POST /report/extract | raw_text → `NoduleExtract`（结构化输出，非流式） |
| POST /report/interpret | 报告解读（SSE 流式） |
| POST /chat | 智能问答/助手（RAG+Tool Calling，SSE 流式） |
| GET /chat/sessions/{id} | 会话历史 |
| POST /knowledge/import · GET /knowledge/docs | 指南切片入库（管理员）/ 文档列表 |

Feign 契约 `AiApi`：`extractReport(rawText)`。

DTO（契约的一部分）：

```java
public record NoduleExtract(String location, String noduleType,   // SOLID/PART_SOLID/PURE_GG
        BigDecimal maxDiameterMm, BigDecimal solidDiameterMm,
        Integer count, List<String> signs, String impression) {}
```

## 4. 业务规则

1. **出网前脱敏（不变量）**：任何文本进 LLM 前经过滤器去除姓名/证件号/手机号/住院号；脱敏命中记审计不记原文。
2. 抽取 Prompt：角色=放射科结构化助手；**仅抽取原文存在的信息，缺失字段返回 null 禁止臆造**；输出严格 JSON 绑定 `NoduleExtract`。
3. 解读输出固定四段：`这是什么 / 风险怎么看 / 下一步建议 / 注意事项`，末尾固定免责语"仅供参考，以主治医生意见为准"。
4. RAG 管道：指南按章节语义切分（500~800 字，重叠 100，元数据保留文档名+章节）→ 混合检索（BGE 向量 + BM25，RRF 融合 Top8）→ bge-reranker 精排 Top3 → 拼 Prompt。回答**必须附引用出处**；知识库不可用时明确告知"知识库暂不可用"，禁止自由发挥。
5. 降级链：Rerank 挂 → 回退混合检索原始序；向量库挂 → 拒答提示；LLM 挂/超时 → Sentinel 熔断（超时 30s/异常比例 50%）返回 `24001 LLM 服务熔断降级`。
6. Tool Calling 工具注册（详见 F4）：`queryNextFollowup`、`queryNoduleTrend`、`reportSymptom`、`queryReportSummary`；**工具内部强制以当前登录患者身份取数**（从 X-User-Id 解析，不信任 LLM 传入的 patientId）；每次调用落 `tool_calls_json` 审计。
7. 模型接入：Spring AI + OpenAI 兼容协议，模型名/BaseURL/Key 全走 Nacos；换模型只改配置。
8. 会话记忆：Redis 存最近 10 轮（TTL 2h）+ 落库持久化。

## 5. 异常场景

- LLM 返回非法 JSON（抽取）：重试 1 次（附加纠错提示）→ 仍失败返回"抽取失败请手动录入"，不阻塞 nodule 建档。
- 工具调用 Feign 失败：该轮回答标注"实时数据暂不可用"，仅基于 RAG 作答。
- SSE 中断：客户端重连带 `Last-Event-ID` 续传；服务端会话缓冲保留 5 分钟。

## 6. 禁止行为

1. 禁止向 LLM 发送未脱敏文本；禁止把 LLM API Key 写代码/日志/异常信息。
2. 禁止 AI 输出替代诊断的确定性表述；禁止解读/问答缺免责语。
3. 禁止 RAG 无引用作答；禁止工具方法接受 LLM 指定的任意 patientId（防越权）。
4. 禁止在本服务写结节/随访业务表。

## 7. 依赖声明

- 公共能力：common-web（SSE）、common-feign。
- 下游调用：`FollowupApi`、`NoduleApi`、`PatientApi`（工具）；被 nodule 经 `AiApi` 调用。
- 中间件：MySQL(yiliao_ai)、PgVector、Redis、LLM API（OpenAI 兼容）、Sentinel。

## 8. 最小调用示例

```bash
curl -N -X POST http://localhost:8080/api/ai/chat \
  -H "Authorization: Bearer {patientToken}" -H "Content-Type: application/json" \
  -d '{"sessionId":"S1","message":"我下次什么时候复查？"}'
# SSE 流：data:{"delta":"根据您的随访计划"} → data:{"tool":"queryNextFollowup"} → ...
#        data:{"delta":"下一次复查为 2026-09-10（胸部CT）","citation":"肺结节诊治中国专家共识(2024) §随访"}
```

## 变更记录

| 版本 | 日期 | 变更 | 原因 |
|---|---|---|---|
| v1.0 | 2026-08-22 | 首版，收敛自 make/技术设计文档 §11/§16/§18~§21 | 分层文档体系建立 |
