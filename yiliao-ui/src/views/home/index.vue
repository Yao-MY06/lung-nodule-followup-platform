<template>
  <div class="home-page">
    <!-- 欢迎条：青蓝渐变 hero -->
    <section class="hero">
      <div>
        <h2>{{ greeting }}，{{ auth.user?.realName || '用户' }}</h2>
        <p>{{ todayStr }} · 欢迎使用肺结节/肺癌患者管理系统</p>
      </div>
      <div class="hero-mark" aria-hidden="true"></div>
    </section>

    <!-- 统计卡：图标圆 + 强调色 -->
    <el-row :gutter="20" class="stat-row" v-loading="loading">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <div class="stat-card" :class="card.tone">
          <div class="stat-icon"><el-icon><component :is="card.icon" /></el-icon></div>
          <div>
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷入口：大色块卡片 -->
    <section class="shortcut-card">
      <div class="shortcut-head">快捷入口</div>
      <div class="shortcut-list">
        <button v-for="item in shortcuts" :key="item.path" class="shortcut-tile" @click="go(item.path)">
          <el-icon class="shortcut-icon"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </div>
    </section>
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

const hour = new Date().getHours()
const greeting = hour < 12 ? '上午好' : hour < 18 ? '下午好' : '晚上好'
const todayStr = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

const statCards = computed(() => [
  { label: '在管患者数', value: stats.value?.managingCount ?? '-', icon: UserFilled, tone: 'tone-primary' },
  { label: '进行中计划', value: stats.value?.activePlanCount ?? '-', icon: Calendar, tone: 'tone-success' },
  { label: '逾期任务', value: stats.value?.overdueTaskCount ?? '-', icon: Document, tone: 'tone-danger' },
  { label: '预警', value: stats.value?.alertCount ?? '-', icon: DataAnalysis, tone: 'tone-warning' }
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
    stats.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home-page { padding: 4px; }

/* Hero 欢迎条 */
.hero {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px 32px;
  margin-bottom: 20px;
  border-radius: var(--yl-radius);
  color: #eaf6f5;
  background: linear-gradient(120deg, #0a5a55 0%, #0d7e76 55%, #15648f 100%);
  box-shadow: var(--yl-shadow);
}
.hero h2 { margin: 0 0 6px; font-size: 24px; font-weight: 700; letter-spacing: 1px; }
.hero p { margin: 0; font-size: 13px; color: rgba(234, 246, 245, 0.85); }
.hero-mark {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background:
    radial-gradient(circle, transparent 40%, rgba(255, 255, 255, 0.12) 41% 44%, transparent 45%),
    radial-gradient(circle, transparent 62%, rgba(255, 255, 255, 0.09) 63% 66%, transparent 67%);
  flex: none;
}

/* 统计卡 */
.stat-row { margin-bottom: 20px; }
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
  background: #fff;
  border: 1px solid var(--yl-line);
  border-radius: var(--yl-radius);
  box-shadow: var(--yl-shadow);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--yl-shadow-hover); }
.stat-icon {
  width: 48px;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 22px;
  flex: none;
}
.stat-value { font-size: 30px; font-weight: 700; font-variant-numeric: tabular-nums; line-height: 1.2; color: var(--yl-ink); }
.stat-label { margin-top: 2px; font-size: 13px; color: var(--yl-muted); }
.tone-primary .stat-icon { background: var(--el-color-primary-light-9); color: var(--yl-primary); }
.tone-success .stat-icon { background: #e8f6ef; color: var(--yl-success); }
.tone-danger  .stat-icon { background: #fbeaea; color: var(--yl-danger); }
.tone-warning .stat-icon { background: #fdf3e4; color: var(--yl-warning); }
.tone-danger  .stat-value { color: var(--yl-danger); }

/* 快捷入口 */
.shortcut-card {
  background: #fff;
  border: 1px solid var(--yl-line);
  border-radius: var(--yl-radius);
  box-shadow: var(--yl-shadow);
  padding: 20px 22px 24px;
}
.shortcut-head { font-size: 16px; font-weight: 600; color: var(--yl-ink); margin-bottom: 16px; }
.shortcut-list { display: flex; gap: 16px; flex-wrap: wrap; }
.shortcut-tile {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 22px;
  border: 1px solid var(--yl-line);
  border-radius: 12px;
  background: #fafcfd;
  font-size: 15px;
  color: var(--yl-ink-2);
  cursor: pointer;
  transition: all 0.2s ease;
}
.shortcut-tile:hover {
  border-color: var(--el-color-primary-light-7);
  background: var(--el-color-primary-light-9);
  color: var(--yl-primary);
  transform: translateY(-2px);
}
.shortcut-icon { font-size: 20px; }
</style>
