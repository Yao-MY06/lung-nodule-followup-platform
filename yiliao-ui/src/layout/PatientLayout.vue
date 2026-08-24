<template>
  <el-container class="patient-layout">
    <el-header class="patient-header" height="72px">
      <span class="patient-logo">
        <span class="logo-mark"><span class="logo-dot"></span></span>
        肺结节患者管理平台
      </span>
      <el-menu :default-active="activePath" router mode="horizontal" class="patient-menu" :ellipsis="false">
        <el-menu-item index="/portal/plan">我的随访计划</el-menu-item>
        <el-menu-item index="/portal/chat">AI 助手</el-menu-item>
        <el-menu-item index="/portal/interpret">报告解读</el-menu-item>
      </el-menu>
      <el-dropdown trigger="click" @command="onCommand">
        <span class="patient-user">
          <span class="avatar">{{ avatarChar }}</span>
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
const avatarChar = computed(() => (auth.user?.realName || '用').slice(0, 1))

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

/* 患者端顶栏：白色 + 青蓝下划线菜单，中老年适配大字号 */
.patient-header {
  display: flex;
  align-items: center;
  gap: 40px;
  background: #fff;
  border-bottom: 1px solid var(--yl-line);
  box-shadow: 0 1px 8px rgba(23, 43, 77, 0.05);
}
.patient-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: var(--yl-ink);
  white-space: nowrap;
}
.logo-mark {
  position: relative;
  width: 28px;
  height: 28px;
  border: 3px solid var(--yl-primary);
  border-radius: 50%;
  flex: none;
}
.logo-dot {
  position: absolute;
  inset: 6px;
  background: var(--yl-primary);
  border-radius: 50%;
}
.patient-menu {
  flex: 1;
  border-bottom: none;
  --el-menu-horizontal-height: 72px;
  --el-menu-hover-bg-color: transparent;
}
.patient-menu :deep(.el-menu-item) {
  font-size: 17px;
  padding: 0 24px;
  color: var(--yl-ink-2);
}
.patient-menu :deep(.el-menu-item.is-active) {
  color: var(--yl-primary);
  font-weight: 600;
  border-bottom: 3px solid var(--yl-primary);
}
.patient-user {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  cursor: pointer;
  color: var(--yl-ink);
  outline: none;
  white-space: nowrap;
}
.avatar {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--el-color-primary-light-9);
  color: var(--yl-primary);
  font-size: 15px;
  font-weight: 600;
}

/* 中老年用户适配：正文与控件加大 */
.patient-main {
  background: linear-gradient(180deg, #f2f7fa 0%, #eef3f7 100%);
  font-size: 16px;
  padding: 24px;
}
.patient-main :deep(.el-button) {
  height: 44px;
  font-size: 16px;
  padding: 10px 24px;
}
</style>
