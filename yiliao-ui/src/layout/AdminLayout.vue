<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="admin-logo">肺结节患者管理</div>
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
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <span class="admin-header-title">{{ route.meta.title || '肺结节/肺癌患者管理系统' }}</span>
        <el-dropdown trigger="click" @command="onCommand">
          <span class="admin-user">
            {{ auth.user?.realName || '用户' }}
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
.admin-aside {
  background: #001529;
}
.admin-logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}
.admin-menu {
  border-right: none;
  background: #001529;
}
.admin-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.75);
}
.admin-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: #1890ff;
}
.admin-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
}
.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}
.admin-header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #303133;
  outline: none;
}
.admin-role-tag {
  margin-left: 2px;
}
.admin-main {
  background: #f5f7fa;
}
</style>
