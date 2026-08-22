# GitHub Copilot 仓库入口

请把仓库根目录的 `AGENTS.md` 作为项目上下文和 AI 工作流的唯一事实源，不要在本文件维护第二份完整规则。

开始任务时：

- 先只读探索，并读取 `templates/task-brief.md` 与相关项目规则。
- 绿区小任务可以使用轻量计划；黄区、红区和跨会话任务先创建 `templates/specification.md`。
- 根据规格生成 `templates/implementation-plan.md`，等待人工批准后再实现。
- 每个任务使用 `templates/task.md`，先定义测试或验证，再生成实现。
- 将 lint、类型、测试、构建、安全和依赖核验结果记录到 `templates/verification-report.md`。
- 合并前执行 `checklists/before-start.md`、`before-submit.md` 和 `before-merge.md` 中适用的检查。
- 红区变更必须经过资深工程师和安全角色双审；会话结束后评估是否更新 `AGENTS.md` 或 Skill。

