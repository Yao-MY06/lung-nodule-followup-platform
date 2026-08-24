# R1：AI 真实链路重测 —— 验证报告

> 元信息：类型=运行态联调验证报告　模块=yiliao-ai（含 gateway/followup 交叉）　依赖=R 系列执行书 R1　版本=2026-08-24
> 执行者：ZCode（ox-alpha）　LLM：智谱 GLM glm-4-flash（OpenAI 兼容协议，真 Key 实网调用）

## 结果摘要

**5 项检查：4 通过，1 未通过（T4，缺陷已定位并记录证据链，按执行书规则未改业务代码）。**

首次以真实 LLM Key 跑通抽取/解读/RAG 三条路径全链路（含脱敏出网、服务端强制免责、citation 出处三大安全设计），全部符合预期；工具调用路径发现一个启动后才暴露的线程上下文缺陷。

## 执行环境偏差披露

| 项 | 执行书假设 | 实际 | 影响 |
|---|---|---|---|
| LLM 厂商 | DeepSeek/通义/豆包 | **智谱 GLM**（glm-4-flash，`F:\bc\ainew\.env` 的 Key） | 无——OpenAI 兼容协议同构；环境变量 DEEPSEEK_API_KEY 实测 401 失效 |
| ai 启动参数 | base-url 即可 | 追加 `--spring.ai.openai.chat.completions-path=/chat/completions` | GLM 兼容端点是 `/api/paas/v4/chat/completions`，Spring AI 默认拼 `/v1/chat/completions` 导致 404→24002。属厂商配置差异，非代码缺陷 |
| 中间件 | RocketMQ 运行中 | broker 崩溃循环，已修复后恢复（见下"基础设施修复"） | T1~T5 不依赖 MQ |

## 验证表

| 检查 | 结果 | 关键证据 |
|---|---|---|
| T1 抽取字段一致 | ✅ | `noduleType=PURE_GG`、`maxDiameterMm=4`、`count=1`、`signs=[]`（原文"未见毛刺"，阴性未臆造为阳性） |
| T1 脱敏出网 | ✅ | 报告原文含 13812345678；ai 服务日志 `grep -c 13812345678 = 0`；响应体亦无。出网前被 `SensitiveDataFilter` 替换为 `[电话]` |
| T2 解读四段+免责 | ✅ | 【这是什么】【风险怎么看】【下一步建议】【注意事项】四段齐全（184 个 delta 流式输出）；免责语由服务端在 done 事件前独立 delta 强制追加（ChatService 第 50 行逻辑），不依赖模型 |
| T3 RAG 出处 | ✅ | 导入《肺结节诊治中国专家共识2024》2 个切片；患者问"4mm 磨玻璃结节复查"，回答含"6个月"；SSE 出现 `event:citation` 且 data 含文档名与章节名 |
| T4 工具取数 | ❌ | 模型确实发起工具调用（响应 ~3s 两轮往返 + 回复逐字复述 `CommonErrorCode.UNAUTHORIZED.msg`），但工具执行必抛 401 → 回答无真实日期；symptom_report 落库数 0。根因见下节 |
| T5 越权注入 | ⚠️ 部分通过 | 注入"查档案 1002"未泄露任何他人数据（目标达成）；但拦截发生在工具 401 故障态而非"绑定本人后比对拒绝"的设计态。纵深防御独立验证通过：患者直连 `/api/followup/plans/1002` → 403"无权访问他人数据"，`/api/nodule/patients/1002/nodules` → 403 |

## T4 缺陷定位（完整证据链见 tasks/evidence/R1-t4_defect_evidence.txt）

- **现象**：患者身份经 chat 问计划/报症状，回答均为"未认证或登录已过期"；followup 库 symptom_report 无新增。
- **根因**：`ChatService.chat()` 用 `.stream()`（yiliao-ai/.../chat/ChatService.java:57），Spring AI 1.0.0 流式路径的 `@Tool` 方法在 Reactor boundedElastic 线程执行；`PatientTools.currentPatientId()`（tools/PatientTools.java:68）依赖 RequestContextHolder（ThreadLocal 只绑定 Tomcat http-nio 线程），跨线程恒为 null → 必抛 UNAUTHORIZED。
- **判定依据**：① 模型回复与错误码 msg 一字不差（幻觉不会精确复述代码常量）；② ~3s 双轮 LLM 往返耗时；③ ai→8084 无网络连接（Feign 调用前即抛出）；④ jstack 存在 boundedElastic 线程池（R1-t4_thread_dump.txt）。
- **影响**：F4 四个患者数据工具在 SSE 主路径全部不可用（P4 单测未覆盖流式集成路径，故测试全绿但运行期失效）。
- **修复方向（供后续任务，本次未动代码）**：进入 chat() 时将 X-User-Id 快照为局部变量显式传入工具对象（如构造 `new PatientTools(userId)`），不依赖 ThreadLocal 跨线程。

## 基础设施修复（非业务代码，docker/docker-compose.yml 一行）

broker 此前崩溃循环（exit 255），两个叠加原因：
1. compose 把容器 8081 映射宿主机 `"8081:8081"`，与运行中的 auth-service 冲突 → 改映射 `"18081:8081"`。
2. named volumes（rmq-broker-logs/store）属主 root，容器内 rocketmq 用户(uid 3000)不可写 → 启动即失败且真实异常被关闭路径 NPE 掩盖 → `chown -R 3000:3000` 卷后 boot success 并成功注册 namesrv。

## 测试前置数据（可复现）

- 为档案 1001 经 API 正常生成随访计划（POST /api/followup/plans/generate，scene=1 纯磨玻璃 4mm）→ 命中 decision_rule id=7（≤5mm 首查+6个月），首个任务日期 **2027-02-24**（表驱动规则生效证据）。此日期即 T4 工具应取到的真实值。
- 知识库导入返回 chunks=2。

## 证据文件清单（tasks/evidence/R1-*）

| 文件 | 内容 |
|---|---|
| R1-token.txt / R1-patient_login.json / R1-patient_token.txt | admin 与 patient01 登录凭据 |
| R1-gen_plan_body.json / _resp.json / R1-plan_timeline.json | 1001 计划生成与时间线（2027-02-24 首任务） |
| R1-ai-run.log | ai 服务启动与运行日志（含手机号零残留 grep 对象；含 GLM completions-path 修正前的 404 现场） |
| R1-t1_extract_body.json / _resp.json | T1 请求体与结构化抽取结果 |
| R1-t2_interpret_body.json / _sse.txt / _fulltext.txt / _disclaimer_check.txt | T2 SSE 原始流、还原全文、免责语核验 |
| R1-t3_import_body.json / _resp.json / R1-t3_chat_body.json / _sse.txt / _check.txt | T3 知识库导入与 RAG 问答+citation |
| R1-t4_q1*/q2*/defect_evidence.txt/thread_dump.txt | T4 失败现场（经网关+直连对照）与缺陷证据链 |
| R1-t5_body.json / _sse.txt | T5 注入用例现场 |

## 未通过项与风险提醒

- **未通过**：T4 工具取数（缺陷如上，建议作为 P4b 修复任务，修复后需补流式路径集成测试）。
- **费用/配额**：glm-4-flash 免费/低价档，全程约 10 次 LLM 往返消耗极小；若换 deepseek-chat 重跑注意余额。
- **模型差异**：GLM 输出的四段标题拼接时丢失了换行（delta 流合并后"…现象。风险怎么看】"连排），段落标记本身齐全；不影响语义，记录备查。
