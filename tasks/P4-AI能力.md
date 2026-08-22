# 任务简报：P4 AI 能力（ai-service + 确认排期主线）

## 基本信息

- 负责人：本人（AI 辅助）
- 日期：2026-08-22
- 关联需求：specs/modules/ai.md；flows/F1（抽取前置）+ F4（AI 助手）；references/03-AI能力参考.md
- 风险车道：**红区**（PII 出网过滤、输出免责、工具越权防护）
- AI 使用方式：委托代理

## 目标与边界

**目标**

1. yiliao-ai（8086）编译 + 核心单测：
   - **A3 抽取**：raw_text → 脱敏 → LLM → `NoduleExtract`（缺失=null 禁臆造；解析失败重试 1 次→24002）；`AiApi.extractReport` 契约。
   - **A1 解读**：四段式 Prompt + 服务端强制免责语，SSE 流式；LLM 异常→优雅降级（24001 文案）。
   - **A2 RAG 管道**：章节切分（500~800 字/重叠 100）+ **混合检索（关键词 bigram + 向量余弦，RRF 融合）** + 引用溯源组装——纯 Java 可测，Embedding 默认确定性伪向量（离线可演示），真模型/PgVector P4b 换配。
   - **F4 工具**：`queryNextFollowup/queryNoduleTrend/reportSymptom/queryReportSummary` 四个 @Tool 方法，**patientId 强制取当前登录用户，不信任 LLM 参数**；会话 SSE。
2. nodule 确认链路：`POST /exam/reports/{id}/confirm` → structured_json 建结节+首快照 → Feign followup 自动排期（risk 经 PatientApi 新契约取真实危险因素）。
3. 契约扩展（黄区）：AiApi、PatientApi.getRiskFactor、FollowupApi.reportSymptom。
4. yiliao_ai 库 DDL。

**不在范围内（P4b）**：Seata @GlobalTransactional 强化（当前同步 Feign 容错模式继续）、PgVector 实装、Redis 会话记忆、Sentinel 规则、Rerank 服务、file-service。

## 验收标准

- [ ] `mvn install` 全绿（16 模块）
- [ ] 脱敏单测：手机号/身份证/邮箱被替换
- [ ] 抽取单测：fake LLM 返回 JSON→DTO 映射；坏 JSON 重试后 24002；Prompt 含脱敏后文本
- [ ] RAG 单测：混合检索把相关章节排首位、RRF 融合、无关查询不召回爆炸
- [ ] 工具安全单测：工具内部用当前用户 id（LLM 无法越权）
- [ ] nodule 确认单测：状态流转 1→2、建结节+快照、自动生成计划被调用

## 开工批准

- 计划审阅者：本人（用户指示"开始做P4"）
- 批准日期：2026-08-22
