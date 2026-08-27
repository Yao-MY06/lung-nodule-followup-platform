# Lung Nodule Follow-up Platform · 肺结节/肺癌患者全周期随访管理平台

基于 **Spring Cloud 2023 + Spring AI 1.0** 的微服务医疗随访平台，覆盖「建档 → 风险评估 → 随访计划自动生成 → 智能提醒 → 复查对比 → 预警干预」全闭环。毕业设计作品，含管理端与患者端 Web 应用。

> 依据《肺结节诊治中国专家共识（2024）》等指南内置随访规则引擎——**规则是数据不是代码**，修改规则表即时生效。

## 核心特性

- **表驱动随访规则引擎**：指南规则固化于 `decision_rule` 表（16 条种子规则），按结节特征（类型/大小/危险因素）或术后分期自动匹配模板生成随访时间轴，零 if-else 硬编码
- **AI 报告结构化抽取**（医生确认制）：CT 报告原文 → 脱敏出网 → LLM 结构化 JSON 草稿 → **医生逐项核对确认后**才写入结节档案——AI 只起草、人决策
- **RAG 指南问答**：章节切分 + 关键词/向量**混合检索（RRF 融合）**，回答附指南出处，知识库不可用时明确告知而非自由发挥
- **Tool Calling 患者助手**：自然语言查本人随访计划/结节趋势、上报症状——工具在 Java 层强制绑定当前登录患者，Prompt 注入无法越权
- **消息可靠性三保险**：Redis SETNX + DB 唯一键 + 渠道幂等，到点复查提醒不丢不重；扫描派发 + 对账补投兜底
- **纵向对比与升级预警**：复查快照自动对比，增大或实性成分增加 ≥2mm 触发随访级别升级评估并预警主管医生
- **患者数据权限双层防线**：网关 PATIENT 路径白名单 + 服务侧档案归属校验，越权访问实测 403

## 技术栈

| 层 | 技术 |
|---|---|
| 语言/框架 | Java 17 · Spring Boot 3.4.5 · Spring Cloud 2023.0.3 · Spring Cloud Alibaba 2023.0.3.2 |
| 服务治理 | Nacos（注册/配置）· Spring Cloud Gateway · Sentinel（限流熔断）· OpenFeign |
| 数据 | MySQL 8（每服务一库）· MyBatis-Plus · Redis + Caffeine · Redisson |
| 消息/调度 | RocketMQ 5 · XXL-Job（分片扫描） |
| AI | Spring AI 1.0（OpenAI 兼容协议，模型可插拔）· 混合检索 RRF · SSE 流式 |
| 其他 | Seata AT（建档链路）· MinIO（附件预签名）· Knife4j |
| 前端 | Vue 3.5 · Element Plus · ECharts · Pinia |

## 系统架构

```
 管理端 Web ─┐                       ┌─ auth(8081)   认证/JWT/RBAC
             ├─► Gateway(8080) ──┬──► patient(8082) 档案/危险因素/PII加密脱敏
 患者端 H5  ─┘   鉴权·白名单·限流  ├──► nodule(8083)  结节/快照/≥2mm对比
                                   ├──► followup(8084) 规则引擎/计划/工作台 ★核心
                                   ├──► notification(8085) 提醒/幂等消费
                                   ├──► ai(8086)  抽取/解读/RAG/工具调用
                                   ├──► statistics(8087) 驾驶舱
                                   └──► file(8088)  MinIO 预签名
 中间件：MySQL · Redis · RocketMQ · Nacos · MinIO · XXL-Job · Sentinel
```

同步查询走 Feign（带降级），状态变更走 RocketMQ（最终一致 + 幂等）；建档链路预留 Seata AT 强化。

## 快速开始

```bash
# 1. 中间件（MySQL 映射 3307，避免与本机 3306 冲突）
cd docker && docker compose -f docker-compose.yml -f docker-compose.local.yml up -d
#    首次启动自动建 7 个业务库并导入种子数据（16 条指南规则/角色/账号）

# 2. 后端服务（任一目录执行，数据源指向 3307）
mvn -q -pl yiliao-auth spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:mysql://localhost:3307/yiliao_auth?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
#    依次启动 auth → gateway → patient → nodule → followup → notification → ai

# 3. 前端
cd yiliao-ui && npm install && npm run dev   # http://localhost:5173
```

**演示账号**：`admin / admin123`（管理员）· `doctor01 / doctor123`（医生）· `patient01 / admin123`（患者，已绑定演示档案）

LLM 配置（AI 功能需要）：启动 ai 服务时设置环境变量 `YILIAO_LLM_API_KEY` / `YILIAO_LLM_BASE_URL` / `YILIAO_LLM_MODEL`（OpenAI 兼容协议，DeepSeek/通义/豆包均可）。

## 演示主线（8 步）

1. 医生粘贴 CT 报告 → **AI 抽取**结构化草稿（脱敏出网，缺失字段不臆造）
2. 医生核对确认入库 → 自动建结节 + 基线快照
3. **规则引擎**按 pGGN≤5mm 匹配模板 → 生成 3~5 年随访时间轴
4. 修改 `decision_rule` 表间隔 → 重新生成**即时生效**（表驱动证据）
5. 到点任务 → 扫描派发 MQ → **幂等消费**推送复查提醒（重复投递不重发）
6. 录入复查快照 → 结节增大 ≥2mm → 自动升级随访级别 + 预警医生
7. 患者端问“我下次什么时候复查” → AI **工具调用**查真实计划；上报咳嗽 3 级 → 触发医生预警
8. 断开 LLM → **优雅降级**（SSE 固定文案，服务不雪崩）；患者 token 越权访问他人数据 → **403**

## 测试与验证

- **143 个单元测试**全绿：规则引擎条件求值、消息幂等、快照对比、JWT/网关过滤器、AI 工具越权防护、PII 加密脱敏等
- **首次实机全链路验证通过**（登录鉴权/越权防线/建档联动/规则热改/幂等/降级），见 [`tasks/全链路运行验证报告.md`](tasks/全链路运行验证报告.md)
- 各阶段均有任务简报与验证报告（[`tasks/`](tasks/)），外部模型重放与五维度评审材料（[`tasks/external-briefs/`](tasks/external-briefs/)）

## 文档地图

| 目录 | 内容 |
|---|---|
| [specs/](specs/) | 分层设计文档（事实源）：全局规范 ×3 + 九模块文档 + 四跨服务流程 |
| [tasks/](tasks/) | 阶段任务简报/验证报告/联调执行书（R 系列） |
| [docker/](docker/) | 中间件编排 + 全部 DDL 与种子 SQL |
| [references/](references/) | 参考项目调研与借鉴登记 |

## 目录结构

```
yiliao-common/     6 个公共模块（core/web/data/security/mq/feign）
yiliao-api/        服务间 Feign 契约 + DTO
yiliao-gateway/    网关：JWT 校验/PATIENT 白名单/限流
yiliao-auth/       认证：双 Token/黑名单/防枚举/锁定
yiliao-patient/    档案：AES-GCM 身份证加密/阶段状态机
yiliao-nodule/     结节：快照/纵向对比/AI 抽取确认链路
yiliao-followup/   ★ 随访引擎：表驱动规则/计划生成/工作台/提醒派发
yiliao-notification/ 消息：幂等三保险/站内信/短信通道
yiliao-ai/         AI：抽取/解读 SSE/RAG/Tool Calling
yiliao-statistics/ 驾驶舱：快照预聚合 + 四只读接口
yiliao-file/       MinIO 预签名：白名单/防路径穿越
yiliao-ui/         Vue3 前端（管理端 + 患者端）
```

## 声明

本项目为毕业设计，**全部使用构造的模拟数据**演示（无任何真实患者数据）。AI 输出仅作辅助参考并强制附免责语，不构成诊断意见；AI 结构化抽取结果必须经医生人工确认后方可入库。开发过程采用 AI 辅助工作流（分层文档事实源 + 任务简报 + 验证报告 + 子代理并行），各提交信息含 AI 使用披露。

安全基线：PII 加密存储与出参脱敏、发 LLM 前强制脱敏过滤、密钥走环境变量不入库、逻辑删除、操作可审计。
