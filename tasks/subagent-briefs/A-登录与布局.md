# 分工 A：登录页 + 布局 + 首页

> 任务书 = 00-通用约定.md + 本文件。允许覆盖的文件（仅此 4 个）：
> `src/views/login/index.vue`、`src/layout/AdminLayout.vue`、`src/layout/PatientLayout.vue`、`src/views/home/index.vue`

## 1. views/login/index.vue

- 居中卡片（系统名"肺结节/肺癌患者管理系统"），el-form：用户名/密码（必填校验）、登录按钮（loading 态）。
- 提交调 `useAuthStore().login(username, password)`；成功 `ElMessage.success` 后跳转：`route.query.redirect` 存在则跳它；否则患者（userType===4）→ `/portal/plan`，其他 → `/home`。
- 底部灰字：开发账号 admin/admin123、doctor01/doctor123。

## 2. layout/AdminLayout.vue（当前是占位壳，整体重写）

- el-container 全屏：侧边 el-menu（router 模式，default-active=当前路由 path）：首页 /home、患者管理 /patient/list、报告与AI抽取 /nodule/report、随访工作台 /followup/workbench、统计驾驶舱 /stats/dashboard；配 @element-plus/icons-vue 图标。
- 顶栏右侧 el-dropdown：显示 `auth.user.realName` + 角色名（1管理员/2医生/3随访专员映射）；下拉"退出登录"→ `auth.logout()` → 跳 /login。
- 主体 `<router-view />`。

## 3. layout/PatientLayout.vue（重写）

- 简洁顶栏：系统名 + 横向 el-menu（我的随访计划 /portal/plan、AI 助手 /portal/chat、报告解读 /portal/interpret）+ 右上用户名/退出。
- 适配中老年用户：正文 ≥16px、按钮 large。主体 router-view。

## 4. views/home/index.vue

- 欢迎卡片（`auth.user.realName`）。
- 一排 4 个统计卡：调 `src/api/stats.js` 的 `overview()`——在管患者数 managingCount、进行中计划 activePlanCount、逾期任务 overdueTaskCount（红色）、预警 alertCount；loading；接口失败显示 "-"（不阻塞页面，ElMessage 已由拦截器弹出）。
- 快捷入口卡片：跳转 /patient/create、/nodule/report、/followup/workbench、/stats/dashboard。

## 验证自查

菜单 path 与 router/index.js 完全一致；auth store 字段名（realName/userType/roles）核对 stores/auth.js；图标组件先 `import { ... } from '@element-plus/icons-vue'` 再使用。
