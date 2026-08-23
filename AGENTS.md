# 项目 AI 上下文

> 本文件记录代理无法从代码可靠推断的项目事实，事实源为 `make/` 下的需求分析与技术设计文档。控制在 150 行以内，变更按普通代码变更审查。

## 项目身份

- 项目名称：肺结节/肺癌患者管理系统
- 一句话目的：覆盖"建档 → 风险评估 → 随访计划自动生成 → 智能提醒 → 复查对比 → 预警干预"闭环的医院患者管理平台（毕业设计）
- 主要维护者：学生团队（本人负责，指导老师验收）
- 默认分支：`main`
- 事实源文档：**`specs/` 分层文档体系（实现事实源）** = README（地图）+ global/ 三份全局规范 + modules/ 九份模块文档 + flows/ 四份流程文档；`make/` 三份大文档为需求调研与论文素材快照。两者冲突以 `specs/` 为准并回写 `make/`。AI 阅读顺序：本文件 → specs/README → global/ 三份 → 目标模块文档 → 相关流程文档。

## 技术栈（已定，不重新选型）

- Java 17 + Spring Boot 3.4.x + Spring Cloud 2023.0.x + Spring Cloud Alibaba 2023.0.3.x（修正原设计文档的 2022.x 配对：SC 2022.x 仅支持 Boot 3.0，且 Spring AI 1.0 基线要求 Boot 3.4+）
- Nacos（注册+配置）、Gateway、Sentinel、OpenFeign
- MyBatis-Plus + MySQL 8、Redis + Caffeine、Redisson、RocketMQ 5、XXL-Job、MinIO、Seata AT
- Spring AI 1.0.x（OpenAI 兼容协议，可切换 DeepSeek/通义/豆包）+ PgVector
- 前端 Vue 3 + Element Plus + ECharts；部署 Docker Compose

## 常用命令

| 目的 | 命令 |
| --- | --- |
| 安装依赖 | `mvn install`（根 POM 聚合，全部模块） |
| 本地开发 | 中间件：`cd docker && docker compose up -d`；应用：各服务 IDE 启动 |
| 单元测试 | `mvn test -pl {模块}` |
| 全量测试 | `mvn test` |
| lint/格式化 | 待接入（spotless，P1 候选） |
| 类型检查 | Java 编译即类型检查：`mvn compile` |
| 构建 | `mvn clean package -DskipTests` |
| 安全扫描 | 待接入 |

> 注意：本机 JDK 为 25，编译目标 17（parent `maven.compiler.release`）；Lombok 暂未引入（JDK 25 兼容未验证，P1 处理）。

## 仓库地图（目标结构，骨架未建）

| 服务 | 端口 | 职责 | 状态 |
| --- | --- | --- | --- |
| gateway | 8080 | 路由、JWT 校验、限流、CORS | ✅ P1 |
| auth-service | 8081 | 登录、Token、用户/角色/菜单 RBAC | ✅ P1（菜单/用户管理 P1b） |
| patient-service | 8082 | 患者档案、危险因素、诊断/分期、医患绑定 | ✅ P2 |
| nodule-service | 8083 | 结节登记、复查快照、纵向对比、检查报告 | ✅ P2 |
| followup-service | 8084 | 规则引擎、计划生成、任务调度、随访记录、失访管理（核心） | ✅ P2（MQ/XXL-Job P3） |
| notification-service | 8085 | 站内信/短信/模板消息、延迟消息消费 | ✅ P3（宣教/问卷 CRUD 与医生侧通知 P3b） |
| ai-service | 8086 | 报告解读、RAG 问答、结构化抽取、Tool Calling | ✅ P4（Seata/PgVector/会话记忆 P4b） |
| statistics-service | 8087 | 统计驾驶舱 | ✅ P5（数据源接 Feign P5b） |
| file-service | 8088 | MinIO 附件上传下载 | ✅ P5 |

- 服务间通信：同步查询走 OpenFeign（带 Sentinel 降级）；状态变更事件走 RocketMQ（最终一致）。
- 工程结构事实源：`specs/global/30-架构与工程约束.md`（Maven 单仓多模块：6 个 common + yiliao-api + 9 服务 + yiliao-ui；每服务独立数据库，禁止跨库直连；Feign/MQ 契约、事务/缓存/幂等规范均在其中）。
- `references/`：参考项目库（同类开源项目调研，只学思路不抄代码，借鉴需登记"已借鉴点"，详见其 README）。
- `tasks/`：各阶段任务简报与验证报告。
- 文档编写规则：specs/ 内新增文档必须带元信息头（类型/模块/依赖/版本/优先级）、禁止复制全局规范只引用、单文档 ≤300 行，规则见 `specs/README.md`。
- 数据库 DDL 事实源：技术设计文档第二部分（§6~§11）。

## 不可见约定

- 表命名：小写下划线、服务前缀（如 `followup_task`）；所有表含 `create_time/update_time/create_by/update_by/deleted`，逻辑删除不物理删除。
- 接口约定：统一前缀 `/api/{服务名}`，统一返回体 `Result<T>{code,msg,data}`，分页用 MyBatis-Plus `Page`，接口文档 Knife4j。
- 同步查询走 Feign、状态变更走 MQ，不混用；跨服务写操作（建档链路）用 Seata AT。
- 随访规则是数据不是代码：指南规则存 `decision_rule` 表（表驱动），禁止在 Java 里写 if-else 硬编码随访间隔。
- AI 模型通过 OpenAI 兼容协议接入，模型可插拔，只改配置不改代码。

## 关键不变量

- **AI 只起草、人确认**：AI 结构化抽取结果只是草稿，必须经医生人工确认后才写入 `nodule`/`nodule_snapshot`——医疗系统安全底线，违反会导致误诊风险。
- **AI 输出必须标注"辅助参考"**：报告解读、RAG 问答末尾附免责语，不替代诊断；RAG 回答必须附指南出处，知识库不可用时明确告知而非自由发挥。
- **患者数据权限**：患者端接口仅可访问本人数据，服务端强制校验（不能只靠前端隐藏）；Tool Calling 工具内部以当前登录患者身份查询，防越权。
- **消息幂等**：提醒消息 `biz_key = taskId + remindType` 唯一键 + Redis SETNX，重复投递不得重复发短信。
- **防重生成**：随访计划生成用 Redisson 锁（key=patientId）+ `followup_plan` 唯一索引兜底，患者同一时刻只有一个进行中计划。
- **随访升级规则**：结节增大或实性成分增加 ≥2mm 必须触发随访级别升级评估（来自 2024 中国专家共识）。

## 测试与验证

- 测试数据：构造模拟患者数据演示全流程（毕设无真实数据，禁止编造宣称真实）。
- 规则引擎验证：修改 `decision_rule` 表数据 → 重新生成计划即时生效，作为非硬编码的证据。
- 消息可靠性验证：模拟消费者宕机/重复投递，展示幂等与死信兜底。
- 论文证据项（实现时同步产出）：RAG 消融实验表、JMeter 压测对比图、熔断降级演示。
- 必须人工验证：登录鉴权全链路、患者端越权访问拦截、AI 抽取确认流程。

## 安全边界

- PII/敏感数据：`id_card` 加密存储、展示脱敏；`phone` 出参脱敏（`138****5678`）；发给 LLM 前的文本必须过脱敏过滤器。
- 认证：JWT（access 2h + refresh 7d），注销/改密后旧 Token 入 Redis 黑名单；密码 BCrypt。
- 附件：MinIO 预签名 URL 限时访问，不暴露直链。
- 审计：登录、建档、计划调整、规则修改必须写操作日志。
- 密钥来源：LLM API Key、短信密钥等走 Nacos 配置中心加密配置/环境变量，禁止提交到仓库。
- 禁止事项：不得提交真实患者数据、真实凭据、未经核验的新依赖。
- 红区范围（双审）：auth-service 全部、PII 处理、`decision_rule` 数据变更、网关鉴权逻辑。
- 红区变更审批人：本人 + 指导老师（学生项目角色合并，签核记录仍要留）。

## AI 工作流规则

1. 开始任务先读取本文件；默认只读探索，不在没有批准的计划前修改文件。
2. 使用 `templates/` 下的任务简报、规格、计划和验证报告；实现以 `specs/` 分层文档为事实源（先读全局三份再读模块文档），业务规则冲突时以 `specs/` 为准并显式记录决策。
3. AI 不为自己的实现单独定义唯一测试；测试由人或独立流程先提出，AI 对着验证标准实现。
4. 每个任务保持小而可验证；引用确切文件和既有模式，描述症状时不要预设未验证的方案。
5. 黄区变更（跨服务、数据库结构、MQ 消息契约）需人工复核签核；红区变更（见上）双审。
6. 合并前逐行阅读完整 diff，保存测试、构建、手工验证证据到验证报告。
7. 会话结束后用 `templates/retrospective.md` 复盘，反复出现的约定或陷阱回写本文件。

## 已知陷阱

- **Feign 契约路径**：`@FeignClient(path=...)` 不作用于服务端实现；契约方法必须写绝对路径，实现类禁止类级 `@RequestMapping`（specs/global/30 §5）。
- **xxl-job-core 2.4.1**：已删除 `ShardingUtil`，分片参数用 `XxlJobHelper.getShardIndex()/getShardTotal()`。
- **RocketMQ 延迟级别**：rocketmq-spring 2.3.1 的 delayLevel 上限 2 小时，天级定时不可用——提醒采用扫描派发（specs/flows/F2 决策表）。
- **MyBatis-Plus 3.5.7**：`insert` 有单条/集合重载，测试中 `any()` 歧义需显式 `any(Entity.class)`；Mock `insert` 不会回填自增 id，需 `thenAnswer`。
- **record**：作为返回体时访问器是 `data()/code()`，不是 `getXxx()`。
- **JDK 25**：Mockito/ByteBuddy 需 `-Dnet.bytebuddy.experimental=true`（parent surefire 已配）；Lombok 兼容未验证，暂不引入。
- **AES-256**：密钥必须恰好 32 字节（base64 解码后），开发占位密钥曾在长度上出错。

## 更新记录

- 最近更新：2026-08-22
- 更新原因：基于 `make/` 设计文档首次起草
- 审阅者：待指导老师/本人确认
