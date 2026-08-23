# 分工 C：报告录入 → AI 抽取 → 确认（F1 演示主线）+ 结节趋势

> 任务书 = 00-通用约定.md + 本文件。允许覆盖的文件（仅此 2 个）：
> `src/views/nodule/report.vue`、`src/views/nodule/trend.vue`
> API 仅用 `src/api/nodule.js`（createReport/pageReports/extractReport/confirmReport/listNodules/getTrend/getCompare/createSnapshot）。
> **这是答辩演示主线页面，交互做扎实。**

## 1. report.vue 左右两栏（el-row :gutter）

**左栏"报告录入"**：el-form——patientId（el-input-number 必填）、reportType 固定 1（CT，禁用选择）、examDate（必填）、orgName、raw_text（type=textarea rows=8 必填）、conclusion。提交 `createReport(form)` 成功 ElMessage + 清空 + 刷新右栏。

**右栏"报告列表与 AI 抽取"**：

- 顶部 patientId 同步查询：`pageReports({ patientId, page:1, size:10 })` 表格：examDate/orgName/extract_status（0未选取灰 1已抽取蓝 2已确认绿 el-tag）；行点击选中（highlight-current-row）。
- 选中后启用两个按钮：
  - 【AI 抽取】→ `extractReport(id)`（按钮 loading，文案"AI 解析中…"）→ 下方草稿卡片展示：位置、类型映射（SOLID实性/PART_SOLID部分实性/PURE_GG纯磨玻璃）、最大径 mm、实性成分 mm（null 显示"原文未提及"）、数量、恶性征象 el-tag 列表、影像结论。**卡片头固定 el-alert warning："AI 结果仅为草稿，须医生逐项核对后确认方可入库"**。
  - 【确认入库】→ `ElMessageBox.confirm`（正文复述上述安全提示）→ `confirmReport(id)`：成功结果卡片——noduleId、snapshotId；`planId` 有值 → success 提示"随访计划已自动生成" + 链接 /followup/timeline/{patientId}；`planError` 有值 → warning"自动排期失败，请到随访工作台手动生成"。
- extract_status===2 的行：两个按钮禁用（tooltip"已确认"）。

## 2. trend.vue 结节趋势

- 路由参数 patientId。顶部：`listNodules(patientId)` → el-select（label：`noduleNo · 位置 · 类型中文`）。
- 中部 el-card ECharts 折线：`getTrend(noduleId)`（返回快照数组，字段 examDate/maxDiameterMm/solidDiameterMm）；x=examDate，双系列"最大径mm"、"实性成分mm"（实性成分可能为 null，系列允许断点）。ECharts 生命周期：onMounted+nextTick init、window resize 监听、onUnmounted dispose 与移除监听。
- 图下"最近对比"卡：`getCompare(noduleId)` 展示 Δ最大径/Δ实性成分/Δ密度（null 显示 -）；`progress===true` → 红色高亮"结节进展（≥2mm）"+ advice；否则绿色"稳定"+ advice；`newNodule` → 蓝色"新发结节"。
- 底部 el-collapse"录入复查快照"：examDate 必填/maxDiameterMm/solidDiameterMm（el-input-number）/isNew（el-switch）→ `createSnapshot(...)` 提交后**用返回的 compare 结果直接更新对比卡并重拉趋势图**。

## 验证自查

按钮状态机（未选行禁用/已确认禁用/抽取中禁确认）；ECharts 空数组不报错；null 字段兜底显示。
