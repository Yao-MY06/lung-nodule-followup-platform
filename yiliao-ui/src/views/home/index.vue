<template>
  <div class="home-page">
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-text">
        <h2>你好，{{ auth.user?.realName || '用户' }}</h2>
        <p>欢迎使用肺结节/肺癌患者管理系统</p>
      </div>
    </el-card>

    <el-row :gutter="16" class="stat-row" v-loading="loading">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :class="{ danger: card.danger }">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="shortcut-card" shadow="never">
      <template #header>快捷入口</template>
      <div class="shortcut-list">
        <el-button v-for="item in shortcuts" :key="item.path" size="large" @click="go(item.path)">
          <el-icon class="shortcut-icon"><component :is="item.icon" /></el-icon>
          {{ item.label }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { UserFilled, Document, Calendar, DataAnalysis } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { overview } from '../../api/stats'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const stats = ref(null)

const statCards = computed(() => [
  { label: '在管患者数', value: stats.value?.managingCount ?? '-' },
  { label: '进行中计划', value: stats.value?.activePlanCount ?? '-' },
  { label: '逾期任务', value: stats.value?.overdueTaskCount ?? '-', danger: true },
  { label: '预警', value: stats.value?.alertCount ?? '-' }
])

const shortcuts = [
  { label: '患者建档', path: '/patient/create', icon: UserFilled },
  { label: '报告录入与AI抽取', path: '/nodule/report', icon: Document },
  { label: '随访工作台', path: '/followup/workbench', icon: Calendar },
  { label: '统计驾驶舱', path: '/stats/dashboard', icon: DataAnalysis }
]

function go(path) {
  router.push(path)
}

onMounted(async () => {
  loading.value = true
  try {
    stats.value = await overview()
  } catch {
    // 接口失败已由拦截器弹错，统计卡显示 "-" 兜底
    stats.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home-page {
  padding: 4px;
}
.welcome-card {
  margin-bottom: 16px;
}
.welcome-text h2 {
  margin: 0 0 4px;
  font-size: 20px;
  color: #303133;
}
.welcome-text p {
  margin: 0;
  color: #909399;
}
.stat-row {
  margin-bottom: 16px;
}
.stat-card {
  text-align: center;
}
.stat-label {
  font-size: 14px;
  color: #909399;
}
.stat-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}
.stat-value.danger {
  color: #f56c6c;
}
.shortcut-list {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.shortcut-list .el-button {
  margin-left: 0;
}
.shortcut-icon {
  margin-right: 4px;
}
</style>
