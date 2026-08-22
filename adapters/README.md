# 工具适配示例

这些文件只提供入口映射，不是独立的规则系统。复制适配文件到目标仓库后，始终以根目录的 `AGENTS.md` 为事实源，并从仓库根目录读取 `templates/`、`checklists/` 和 `docs/`。

## 复制位置

| 示例文件 | 复制到目标仓库 |
| --- | --- |
| `CLAUDE.md` | `CLAUDE.md` |
| `cursor/rules/ai-workflow.mdc` | `.cursor/rules/ai-workflow.mdc` |
| `copilot/copilot-instructions.md` | `.github/copilot-instructions.md` |

只复制正在使用的工具适配。目标仓库已有对应文件时，合并入口指令并保留一个政策来源；不要把 `AGENTS.md` 的完整内容复制三遍。适配文件变更也应经过正常评审。

