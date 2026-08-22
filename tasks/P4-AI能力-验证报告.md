# 验证报告：P4 AI 能力（ai-service + 确认排期主线）

- 任务简报：`tasks/P4-AI能力.md`
- 风险车道：红区（PII 出网、输出免责、工具越权）
- 验证人：本人（AI 辅助实现 + 本地执行验证）
- 日期：2026-08-22

## 自动化检查

| 检查 | 命令 | 结果 | 证据摘要 |
|---|---|---|---|
| 编译安装 | `mvn install` | **通过** | Reactor 16 项全 SUCCESS（新增 yiliao-ai；nodule 扩确认链路） |
| 脱敏单测 | ai `SensitiveDataFilterTest`（5） | **通过** | 手机号/身份证/邮箱/住院号替换；正常医学文本不误伤（"5mm"保留） |
| 抽取单测 | `ReportExtractServiceTest`（5） | **通过** | JSON→DTO 映射（缺失=null）；**出网 Prompt 已脱敏**（捕获断言）；坏 JSON 重试 1 次后 24002；第二次恢复；代码块围栏剥离 |
| RAG 单测 | `RagPipelineTest`（6） | **通过** | 混合检索把相关章节排首位；章节切分（#标题）；长文滑窗重叠；非法配置拒绝；RRF 双通道命中优先 |
| 工具安全单测 | `PatientToolsTest`（4） | **通过** | 工具内部取 X-User-Id 当前患者（**方法签名无 patientId，LLM 无法传参越权**）；severity 1-5 服务端校验；≥3 提示已通知医生；无登录上下文拒绝 |
| 确认链路单测 | nodule `ConfirmServiceTest`（4） | **通过** | 确认建 N1 结节+基线快照+状态 1→2+自动排期（risk 取 PatientApi 真实值）；**排期失败不回滚确认**（返 planError）；重复确认 21004；无抽取结果 21003 |

**全项目累计 106 个测试通过。**

## 红区自查（供人工复核）

1. 出网脱敏：`extract/interpret` 双路径都过 `SensitiveDataFilter`，脱敏后残留自检失败即终止（24002）——三处安全断言在测试中固化。
2. 免责语：解读与问答的免责语**由服务端强制追加**（SSE done 事件前），不依赖模型自觉。
3. 工具越权：见工具安全单测；Prompt 注入"查询患者1002"类攻击因方法签名无参而天然无效。
4. API Key：`spring.ai.openai.api-key` 走环境变量占位 `${YILIAO_LLM_API_KEY:sk-dev-placeholder}`，仓库无真实密钥。

## 关键实现决策

1. **LlmClient 抽象层**：Spring AI 实现只在一个类里（`SpringAiLlmClient`），业务/单测全部面向接口（Fake 实现离线可测）——模型可插拔 + 可测性双赢。
2. **RAG 离线可演示**：`DeterministicEmbedding`（bigram 哈希伪向量）让混合检索+RRF 全链路无 API Key 可跑通；P4b 换 BGE 真向量接口不变（诚实标注：伪向量退化为词频相似，消融实验前必须换真模型）。
3. 确认→排期：risk 经 `PatientApi.hasRiskFactor` 契约取真实危险因素（比 P2 的 request 内推断更准确），失败不回滚确认（与 F1 的 Seata 演进一致）。
4. nodule 报告录入增加 `POST /exam/reports/{id}/extract`（0→1 草稿态）+ `confirm`（1→2）两段式，体现"AI 只起草、人确认"。

## 挂起（P4b，依赖中间件/密钥）

- [ ] 真实 LLM 联调（抽取/解读/问答 SSE 端到端）——需 API Key
- [ ] Seata @GlobalTransactional 建档强化（当前同步 Feign 容错模式）
- [ ] PgVector 切片迁移 + BGE Embedding + Rerank
- [ ] 会话 Redis 记忆 + ai_chat_message 落库审计；Sentinel 熔断规则

## 失败与处理

- 文本块内嵌中文引号语法错误（普通字符串嵌 `"` 未转义）→ 全角引号。
- 脱敏规则顺序缺陷：身份证内 11 位子串先被手机号规则命中 → **长模式优先**重排（规则顺序即优先级，已注释说明）。

## 结论

- [x] 适用检查通过、证据可复现；红区三条底线（脱敏/免责/越权）均有测试固化
- [x] F1 主线（报告→抽取→确认→自动排期）代码闭环 + F4 工具层完成
- [ ] 运行时演示挂起（API Key + 中间件），与 P0~P3 合并

结论：P4 代码可进入人工审查（重点：SensitiveDataFilter、PatientTools、ConfirmService）
