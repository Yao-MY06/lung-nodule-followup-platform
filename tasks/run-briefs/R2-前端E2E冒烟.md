# R2：前端浏览器 E2E 冒烟（Playwright）

> 类型：运行态联调　前置：后端服务链运行中 + `cd yiliao-ui && npm run dev`（5173，/api 代理→8080 已配）。

## 目标

用 Playwright 在真实浏览器跑通演示主线并留截图证据（论文/答辩用）。产出为可重复执行的脚本 + 截图集，不要求全量覆盖。

## 步骤

### S0 环境

在 `tasks/e2e/` 新建独立 Node 工程（**不写进 yiliao-ui**，避免污染构建）：
```bash
cd tasks/e2e && npm init -y && npm i -D @playwright/test && npx playwright install chromium
```
playwright.config.js：baseURL `http://localhost:5173`，截图 `tasks/evidence/R2-{场景}.png`，中文默认。

### S1 演示主线用例（按序，每步截图）

1. 登录：访问 /login → admin/admin123 → 断言跳转 /home，统计卡渲染（允许数据为 -）
2. 建档：/patient/create 填"测试患者A"（含危险因素与计划输入：结节类型=纯磨玻璃、最大径=4）→ 提交 → 断言出现"建档成功"且提示"随访计划已自动生成"
3. 患者详情：列表第一条进详情 → 断言证件/手机号显示脱敏形式（含 ****）
4. 报告抽取主线：/nodule/report → 录入一条 CT 报告（R1 T1 原文）→ 点【AI 抽取】→ 断言草稿卡展示 + 黄色"AI 结果仅为草稿"提示 → 点【确认入库】→ 断言出现计划链接
5. 随访工作台：/followup/workbench → tabs 切换"逾期"→ 断言表格渲染
6. 时间轴：从详情跳 /followup/timeline/{id} → 断言 el-timeline 至少 1 项
7. 驾驶舱：/stats/dashboard → 断言 4 统计卡 + 至少 2 个 canvas 渲染
8. 越权：患者账号 patient01/admin123 登录 → 断言被强制导向 /portal/plan；直接访问 /stats/dashboard → 断言被重定向回 portal（前端守卫）+ 后端返回 403（可经 page.evaluate fetch 断言）
9. 患者端 AI 助手：/portal/chat 输入"我下次什么时候复查？" → 断言出现流式文字（等待 done 事件或超时 20s 截断）+ 若后端有 citation 事件则断言引用块渲染

### S2 输出

- `tasks/e2e/tests/mainline.spec.js`（9 用例）
- `tasks/evidence/R2-report/`（Playwright HTML 报告：npx playwright test --reporter=html）
- 截图 PNG 若干

## 验证表

| 检查 | 命令 | 通过标准 |
|---|---|---|
| 全套用例 | `npx playwright test` | 9/9 通过（允许 AI 类用例在假 Key 时断言降级文案而非真内容） |
| 证据完整 | 查看报告目录 | 9 张截图齐 |

## 已知陷阱

- 后端未运行时 Playwright 全部超时——先按 README 前提确认服务链。
- Element Plus 的 el-select 下拉选项在 shadow/teleport 层，用 `page.getByRole('option', ...)` 或可见文本定位。
- AI 用例在无真 Key 环境断言降级文案（"AI 服务暂时不可用"），不要断言具体内容。

## 禁止

不改 yiliao-ui 源码；发现页面 bug 只记录进"未决问题"清单。
