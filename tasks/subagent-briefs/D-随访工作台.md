# 分工 D：随访工作台 + 计划时间轴

> 任务书 = 00-通用约定.md + 本文件。允许覆盖的文件（仅此 2 个）：
> `src/views/followup/workbench.vue`、`src/views/followup/timeline.vue`
> API 仅用 `src/api/followup.js`（workbench/submitRecord/adjustTask/generatePlan/regeneratePlan/timeline）。

## 1. workbench.vue 随访工作台

- 顶部 el-tabs 状态筛选：全部/未到期(0)/临期(1)/逾期(2)/已完成(3)/失访(4)；切换重置 page=1 并重查。tab"逾期(2)"label 红色。
- el-table + el-pagination：seq（第N次）、patientId、planDate、items（itemsJson 截断 30 字 tooltip 全文）、status el-tag（0 info/1 warning/2 danger/3 success/4 danger/5 info）、doneDate、remindSent（提醒次数）。
- 操作列：
  - 【完成随访】（status 为 0/1/2 可用，其余禁用）：el-dialog 表单——followupType 下拉（1电话 2门诊 3线上问卷）必填、content textarea 必填、resultSummary、nextAdvice → `submitRecord(id, form)` 成功 ElMessage + 刷新；dialog 关闭重置表单。
  - 【调整日期】（同上可用）：el-dialog 内 el-date-picker → `adjustTask(id, { planDate })`。
- 页面右上工具组（el-button-group）：
  - 【生成计划】el-dialog：patientId（el-input-number 必填）、scene 固定 1、结节类型下拉/最大径/危险因素开关 → `generatePlan({ patientId, scene, input: { noduleType, maxDiaMm, riskFactor } })` 成功提示"计划 #planId 已生成"。
  - 【重新生成（规则演示）】el-dialog：说明文字"修改 decision_rule 表数据后点此重新生成——表驱动规则引擎即时生效演示"；patientId/scene/输入同上/reason 默认"规则变更后重新生成" → `regeneratePlan(patientId, scene, input, reason)`。
- 查询调 `workbench({ status, page, size })`（status 为"全部"时不传该参数）。

## 2. timeline.vue 计划时间轴

- 路由参数 patientId，调 `timeline(patientId)`（可能返回 null——`el-empty` "该患者暂无随访计划"）。
- 头部 el-descriptions：templateName / startDate / endDate / status（1进行中 primary 2已完成 success 3已终止 info）/ adjustReason（有值用 el-alert warning 展示）。
- 主体 el-timeline：每项——`第{seq}次 · {planDate}`（加粗）、items 内容、status el-tag 同上映射、已完成显示 doneDate；已完成项 timeline-item type=success、逾期 danger。
- 数据字段名对照 api/followup.js 返回的 PlanTimelineDTO：{planId, templateName, startDate, endDate, status, adjustReason, tasks:[{id, seq, planDate, items, status, doneDate}]}。

## 验证自查

tabs 切换重置分页；两个 dialog 的 loading/关闭重置；按钮状态机；"全部" tab 不传 status。
