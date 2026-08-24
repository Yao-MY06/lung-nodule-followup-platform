# tasks/run-briefs/ —— 联调与深化执行书（R 系列）

> 用途：交给外部模型在**主仓 F:\bc\yiliao + 本机运行中的中间件**上执行的联调/深化任务。
> 与 external-briefs（全新空目录重放）不同：**R 系列直接操作主仓与运行环境**。

## 通用前提（每份执行书先自查）

- 中间件运行中：MySQL(3307) / Redis(6379) / RocketMQ(9876)。服务按 auth(8081)→gateway(8080)→patient(8082)→nodule(8083)→followup(8084)→notification(8085)→ai(8086) 顺序本地启动（数据源 url 参数指向 3307，命令见 `tasks/全链路运行验证报告.md` 启动清单）。
- 开发账号：admin/admin123（管理员）、doctor01/doctor123（医生）、patient01/admin123（患者，档案 1001）。
- 测试含中文的 JSON 一律用 UTF-8 文件体 `--data-binary @file.json`（Git Bash curl 直传中文字符串会按 GBK 编码报错）。
- 证据输出统一放 `tasks/evidence/`（截图/日志/jtl/json），文件名前缀 R{编号}-。
- **禁止修改既有业务代码**（R6 除外，它是唯一写代码的任务，白名单见该文件）；发现缺陷先记录，不擅自改。

## 清单

| 文件 | 任务 | 前置 |
|---|---|---|
| R1-AI真实链路重测.md | 配真 LLM Key 后抽取/解读/RAG/工具调用正常路径 | LLM Key（执行者提供） |
| R2-前端E2E冒烟.md | Playwright 跑通管理端+患者端演示主线截图 | 后端+前端 dev 全部运行 |
| R3-RocketMQ提醒灌数实测.md | XXL-Job 启用 + 提醒派发+对账+幂等实测 | RocketMQ/Redis/MySQL |
| R4-RAG消融实验.md | 评测集 + 纯向量/混合/混合+Rerank 三组对比出表 | 与 R1 同源知识库 |
| R5-JMeter压测与限流.md | 三接口三档并发压测出图 + Sentinel 限流 11001 演示 | 服务链运行 |
| R6-P1b权限细化.md | 菜单/用户 CRUD + 菜单树 + 管理员角色守卫（写代码） | 读 specs/modules/auth.md |

执行顺序建议：R1 → R3 → R5（运行态）→ R2（E2E 收尾）→ R4（消融）→ R6（代码）。
