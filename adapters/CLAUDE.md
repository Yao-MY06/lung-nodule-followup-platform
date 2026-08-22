# Claude Code 项目入口

请先读取仓库根目录的 `AGENTS.md`。它是项目上下文和 AI 工作流规则的唯一事实源；不要在本文件复制完整政策。

## 工作顺序

1. 先进行只读探索，阅读任务简报和相关代码。
2. 微小绿区任务使用 `templates/implementation-plan.md`；黄区、红区或多步骤任务先使用 `templates/specification.md`。
3. 将计划写入或链接到 `templates/implementation-plan.md`，等待人工逐条批准。
4. 批准后按 `templates/task.md` 小步实现，测试或验证条件必须先确定。
5. 使用 `templates/verification-report.md` 保存检查证据，再执行 `checklists/before-submit.md` 和 `checklists/before-merge.md`。
6. 会话结束时使用 `templates/retrospective.md`，只把稳定的项目事实回写到 `AGENTS.md` 或 Skill。

修改文件前必须明确当前风险车道；红区变更需要资深工程师和安全角色双审。

