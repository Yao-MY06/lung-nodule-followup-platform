<template>
  <div class="portal-plan">
    <el-card v-if="loading" shadow="never">
      <el-skeleton :rows="4" animated />
    </el-card>

    <el-empty v-else-if="!archive" description="暂未绑定健康档案，请联系您的主管医生完成建档" />

    <template v-else>
      <el-card shadow="never" class="head-card">
        <div class="head">
          <div>
            <div class="name">{{ archive.name }}<el-tag size="small" style="margin-left: 8px">{{ archive.stageLabel }}</el-tag></div>
            <div class="no">档案号 {{ archive.patientNo }}</div>
          </div>
          <div v-if="next" class="next">
            <div class="next-label">下次复查</div>
            <div class="next-date">{{ next.planDate }}</div>
            <div class="next-items">{{ briefItems(next.items) }}</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>随访计划时间轴</template>
        <el-empty v-if="!plan" description="暂无随访计划，医生确认报告后将自动生成" />
        <template v-else>
          <div class="plan-meta">
            方案：{{ plan.templateName || '-' }}　{{ plan.startDate }} ~ {{ plan.endDate || '长期' }}
          </div>
          <el-timeline style="margin-top: 16px; padding-left: 4px">
            <el-timeline-item
              v-for="t in plan.tasks || []"
              :key="t.id"
              :type="timelineType(t.status)"
              :timestamp="t.planDate + (t.doneDate ? '（已完成 ' + t.doneDate + '）' : '')"
            >
              <div class="task-title">第 {{ t.seq }} 次随访</div>
              <div class="task-items">{{ briefItems(t.items) }}</div>
            </el-timeline-item>
          </el-timeline>
        </template>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyArchive } from '../../api/patient'
import { timeline, nextFollowup } from '../../api/followup'

const loading = ref(true)
const archive = ref(null)
const plan = ref(null)
const next = ref(null)

const briefItems = (items) => {
  if (!items) return '复查项目以医嘱为准'
  try {
    const parsed = JSON.parse(items)
    const list = parsed?.items || parsed
    return Array.isArray(list) && list.length ? list.join('、') : items
  } catch {
    return items
  }
}
const timelineType = (status) =>
  ({ 0: 'primary', 1: 'warning', 2: 'danger', 3: 'success', 4: 'danger', 5: 'info' }[status] || 'primary')

onMounted(async () => {
  try {
    archive.value = await getMyArchive()
    if (archive.value) {
      const [planData, nextData] = await Promise.allSettled([
        timeline(archive.value.id),
        nextFollowup(archive.value.id)
      ])
      plan.value = planData.status === 'fulfilled' ? planData.value : null
      next.value = nextData.status === 'fulfilled' ? nextData.value : null
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.portal-plan { max-width: 860px; margin: 0 auto; font-size: 16px; }
.head { display: flex; justify-content: space-between; align-items: center; }
.name { font-size: 20px; font-weight: 600; }
.no { color: #909399; margin-top: 6px; }
.next { text-align: center; }
.next-label { color: #909399; }
.next-date { font-size: 24px; font-weight: 700; color: #409eff; margin: 4px 0; }
.next-items { color: #606266; }
.plan-meta { color: #606266; }
.task-title { font-weight: 600; }
.task-items { color: #606266; margin-top: 4px; }
</style>
