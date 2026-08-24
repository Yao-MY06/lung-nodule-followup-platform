<template>
  <el-container class="admin-layout">
    <el-aside width="224px" class="admin-aside">
      <div class="admin-logo">
        <span class="logo-mark"><span class="logo-dot"></span></span>
        <span class="logo-text">肺结节患者管理</span>
      </div>
      <el-menu :default-active="activePath" router class="admin-menu">
        <el-menu-item index="/home">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/patient/list">
          <el-icon><UserFilled /></el-icon>
          <span>患者管理</span>
        </el-menu-item>
        <el-menu-item index="/nodule/report">
          <el-icon><Document /></el-icon>
          <span>报告与AI抽取</span>
        </el-menu-item>
        <el-menu-item index="/followup/workbench">
          <el-icon><Calendar /></el-icon>
          <span>随访工作台</span>
        </el-menu-item>
        <el-menu-item index="/stats/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>统计驾驶舱</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">闭环管理 · 从建档到预警</div>
    </el-aside>
    <el-container>
      <el-header class="admin-header" height="64px">
        <span class="admin-header-title">{{ route.meta.title || '首页' }}</span>
        <el-dropdown trigger="click" @command="onCommand">
          <span class="admin-user">
            <span class="avatar">{{ avatarChar }}</span>
            <span class="name">{{ auth.user?.realName || '用户' }}</span>
            <el-tag size="small" effect="plain" class="admin-role-tag">{{ roleName }}</el-tag>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, UserFilled, Document, Calendar, DataAnalysis, ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 二级页面（建档/详情等）高亮归属菜单
const activePath = computed(() => {
  const p = route.path
  if (p.startsWith('/patient')) return '/patient/list'
  if (p.startsWith('/nodule')) return '/nodule/report'
  if (p.startsWith('/followup')) return '/followup/workbench'
  if (p.startsWith('/stats')) return '/stats/dashboard'
  return '/home'
})

const ROLE_NAMES = { 1: '管理员', 2: '医生', 3: '随访专员' }
const roleName = computed(() => ROLE_NAMES[auth.user?.userType] || '用户')
const avatarChar = computed(() => (auth.user?.realName || '用').slice(0, 1))

async function onCommand(command) {
  if (command !== 'logout') return
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

/* 浅色侧边栏：医疗洁净感；当前项左侧色条 + 浅青底 */
.admin-aside {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid var(--yl-line);
}
.admin-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 64px;
  padding: 0 20px;
  border-bottom: 1px solid var(--yl-line);
}
.logo-mark {
  position: relative;
  width: 30px;
  height: 30px;
  border: 3px solid var(--yl-primary);
  border-radius: 50%;
}
.logo-dot {
  position: absolute;
  inset: 7px;
  background: var(--yl-primary);
  border-radius: 50%;
}
.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--yl-ink);
  letter-spacing: 1px;
}
.admin-menu {
  flex: 1;
  border-right: none;
  padding: 10px 12px;
  --el-menu-hover-bg-color: var(--el-color-primary-light-9);
}
.admin-menu :deep(.el-menu-item) {
  height: 46px;
  margin: 4px 0;
  border-radius: 10px;
  color: var(--yl-ink-2);
  transition: background 0.2s ease, color 0.2s ease;
}
.admin-menu :deep(.el-menu-item.is-active) {
  color: var(--yl-primary);
  font-weight: 600;
  background: var(--el-color-primary-light-9);
  box-shadow: inset 3px 0 0 var(--yl-primary);
}
.aside-footer {
  padding: 14px 20px;
  font-size: 12px;
  color: var(--yl-muted);
  border-top: 1px solid var(--yl-line);
}

/* 顶栏 */
.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(6px);
  border-bottom: 1px solid var(--yl-line);
}
.admin-header-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--yl-ink);
  letter-spacing: 0.5px;
}
.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--yl-ink-2);
  outline: none;
}
.avatar {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--el-color-primary-light-9);
  color: var(--yl-primary);
  font-size: 13px;
  font-weight: 600;
}
.admin-user .name {
  font-size: 14px;
  font-weight: 500;
}

.admin-main {
  background: linear-gradient(180deg, #f2f7fa 0%, #eef3f7 100%);
  padding: 20px 24px;
}
</style>
