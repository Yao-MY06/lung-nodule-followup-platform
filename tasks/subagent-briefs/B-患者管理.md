# 分工 B：患者管理三页

> 任务书 = 00-通用约定.md + 本文件。允许覆盖的文件（仅此 3 个）：
> `src/views/patient/list.vue`、`src/views/patient/create.vue`、`src/views/patient/detail.vue`
> API 仅用 `src/api/patient.js`（详情页另可用 `src/api/nodule.js` 的 `listNodules`）。

## 1. list.vue 患者列表

- 筛选区：关键字 el-input（name/patientNo 模糊）、阶段标签 el-select（六个标签）+ 查询按钮。
- el-table 分页（el-pagination）：patientNo / name / gender（1男 2女）/ stageLabel（el-tag：失访 danger、MDT中 warning、结案 info，其余 primary）/ sourceType 映射 / createTime。
- 操作列：详情 → `router.push('/patient/detail/' + row.id)`。
- 查询调 `pageArchives({ page, size, keyword, stageLabel })`；返回 `{records,total,page,size}`，total 驱动分页。

## 2. create.vue 建档

el-form（label-width 120px）+ 重置按钮：

- 基本信息：name 必填、gender 单选、birthDate（el-date-picker）、idCard、phone、address、emergencyContact、emergencyPhone、doctorId（el-input-number）、sourceType 下拉必填、remark。
- el-collapse"危险因素"：smokingPackYear 数字、familyHistory/occupationalExposure/priorCancer 用 el-switch（false/true）、comorbidity。
- el-collapse"自动生成随访计划"（默认展开）：scene 固定 1；结节类型下拉（1实性 2部分实性 3纯磨玻璃）、最大径 mm（el-input-number）、危险因素开关（可不填——后端会按危险因素档案推断）。
- 提交调 `createArchive(payload)`（payload 结构：基本信息平铺 + riskFactor:{...} + plan:{scene, input:{noduleType, maxDiaMm, riskFactor}}，对照 api/patient.js 与后端 CreateArchiveRequest；后端字段是 maxDiaMm/riskFactor 蛇形转驼峰已对齐，以 api 层为准）。
- 成功：`ElMessage.success('建档成功：' + data.patientNo)`；若 `data.autoPlanError` 非空 → `ElMessageBox.alert` warning"随访计划自动生成失败：{autoPlanError}，可稍后在随访工作台手动生成"；若 `data.planId` 非空 → 提示"随访计划已自动生成"，附"查看时间轴"链接（/followup/timeline/{创建返回的 archiveId}）。

## 3. detail.vue 患者详情

- 路由参数 id，调 `getArchive(id)`（返回已脱敏：idCardMasked/phoneMasked/riskFactor）。
- el-descriptions：编号/姓名/性别/出生日期/证件号（脱敏值）/电话（脱敏）/阶段/来源/主管医生/备注；危险因素子块（吸烟包年/家族史/职业暴露/既往肿瘤/合并症）。
- 顶部操作：跳转"结节趋势"（/nodule/trend/{id}）、"随访时间轴"（/followup/timeline/{id}）、「变更阶段」el-dropdown 六标签 → `changeStage(id, label)` 成功后重新加载。

## 验证自查

函数名与 api/patient.js 逐字核对（pageArchives/getArchive/createArchive/changeStage/changeDoctor/listNodules）；分页切换重查；switch 绑定布尔。
