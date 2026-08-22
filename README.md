# AI 工作流仓库模板

这是一套不绑定语言、框架、CI 平台或 AI 厂商的 AI 辅助开发模板。它把任务上下文、规格驱动开发（SDD）、小步实现、证据验证、风险分级审查和知识沉淀放进同一套可复制的仓库结构。

## 快速开始

1. 将本目录内容复制到目标仓库根目录，或把需要的子目录复制到已有的工作流目录。
2. 先填写 `AGENTS.md`，只写代码无法可靠推断的项目事实。
3. 从 `templates/task-brief.md` 创建任务简报，分配绿、黄或红风险车道。
4. 微小任务使用轻量计划；中大型任务依次创建 `specification.md`、`implementation-plan.md` 和 `task.md`。
5. 按计划实现，每个任务保持小而可验证，并保存 `templates/verification-report.md`。
6. 通过 `checklists/before-start.md`、`before-submit.md` 和 `before-merge.md` 完成人工关卡。
7. 会话结束时使用 `templates/retrospective.md`，把反复出现的约定或陷阱回写到 `AGENTS.md` 或团队 Skill。

## 任务分级

| 车道 | 典型范围 | 最低审查要求 |
| --- | --- | --- |
| 绿区 | 文档、测试、样板、局部无状态逻辑 | 作者逐行理解 diff，项目质量检查通过 |
| 黄区 | 跨模块逻辑、数据访问、依赖变更、公共行为调整 | 绿区要求 + 资深工程师签核 |
| 红区 | 鉴权、支付、PII、基础设施、迁移、公开 API、安全边界 | 黄区要求 + 资深工程师与安全角色双审 |

风险车道不决定是否使用 AI，只决定所需证据和人工审查强度。

## 轻量路径与完整 SDD

对于几百行以内、边界清楚、无高风险影响的任务，可以使用：

```text
task-brief → implementation-plan → task → verification-report → review
```

对于跨会话、多步骤、跨模块或黄/红区任务，使用完整路径：

```text
task-brief → specification → implementation-plan → task → verification-report → review → retrospective
```

规格是事实源。计划必须经过人工批准后才能开始实现；AI 生成的代码不能替代对需求、边界和验证证据的理解。

## 目录说明

- `AGENTS.md`：项目上下文骨架，目标是少于 150 行。
- `docs/`：工作流、团队治理和度量说明。
- `templates/`：任务、规格、计划、验证和复盘产物模板。
- `checklists/`：开工前、提交前、合并前三道人工检查。
- `adapters/`：Claude、Cursor、Copilot 的薄适配示例。

## 三道人工关卡

- **开工前**：目标、范围、车道、验收标准和计划是否清楚并获批准。
- **提交前**：是否理解完整 diff，测试、质量检查、安全检查和依赖核验是否有证据。
- **合并前**：审查角色是否匹配风险，自动化证据是否完整，是否需要回写上下文。

## 工具适配

`adapters/` 中的文件是可选示例，不是新的规则来源。复制适配文件到对应工具的位置后，仍以项目根目录的 `AGENTS.md`、`templates/` 和 `checklists/` 为准：

- `adapters/CLAUDE.md` → 目标仓库根目录的 `CLAUDE.md`
- `adapters/cursor/rules/ai-workflow.mdc` → `.cursor/rules/ai-workflow.mdc`
- `adapters/copilot/copilot-instructions.md` → `.github/copilot-instructions.md`

如果目标仓库已有规则文件，请合并入口指令，不要维护两份完整政策。适配说明见 `adapters/README.md`。

## 使用边界

模板不预置具体 CI、密钥扫描、SAST 或依赖管理实现。确定目标仓库的语言和平台后，再把对应的自动化门禁接入 `verification-report.md`。不要用代码行数、token 数量或 PR 数量单独衡量生产力；优先观察交付稳定性、评审队列、缺陷和返工。

