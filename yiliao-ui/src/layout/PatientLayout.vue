<template>
  <el-container class="patient-layout">
    <el-header class="patient-header" height="64px">
      <span class="patient-logo">肺结节/肺癌患者管理系统</span>
      <el-menu :default-active="activePath" router mode="horizontal" class="patient-menu" :ellipsis="false">
        <el-menu-item index="/portal/plan">我的随访计划</el-menu-item>
        <el-menu-item index="/portal/chat">AI 助手</el-menu-item>
        <el-menu-item index="/portal/interpret">报告解读</el-menu-item>
      </el-menu>
      <el-dropdown trigger="click" @command="onCommand">
        <span class="patient-user">
          {{ auth.user?.realName || '用户' }}
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </el-header>
    <el-main class="patient-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activePath = computed(() => route.path)

async function onCommand(command) {
  if (command !== 'logout') return
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.patient-layout {
  min-height: 100vh;
}
.patient-header {
  display: flex;
  align-items: center;
  gap: 32px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}
.patient-logo {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}
.patient-menu {
  flex: 1;
  border-bottom: none;
}
.patient-menu :deep(.el-menu-item) {
  font-size: 16px;
}
.patient-user {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  cursor: pointer;
  color: #303133;
  outline: none;
  white-space: nowrap;
}
/* 中老年用户适配：正文与控件加大 */
.patient-main {
  background: #f5f7fa;
  font-size: 16px;
}
.patient-main :deep(.el-button) {
  height: 44px;
  font-size: 16px;
  padding: 10px 24px;
}
</style>
