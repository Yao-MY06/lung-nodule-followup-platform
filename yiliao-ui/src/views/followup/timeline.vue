<template>
  <div v-loading="loading" class="followup-timeline">
    <template v-if="plan">
      <el-descriptions :title="`随访计划 #${plan.planId}`" :column="3" border class="plan-header">
        <el-descriptions-item label="计划模板">{{ plan.templateName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ plan.startDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ plan.endDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="计划状态">
          <el-tag :type="planStatusTagType(plan.status)">{{ planStatusText(plan.status) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="plan.adjustReason"
        type="warning"
        :closable="false"
        :title="`调整原因：${plan.adjustReason}`"
        class="adjust-alert"
      />

      <el-timeline class="task-timeline">
        <el-timeline-item
          v-for="task in plan.tasks"
          :key="task.id"
          :type="timelineItemType(task.status)"
          :timestamp="task.planDate"
          placement="top"
        >
          <div class="task-title">
            <strong>第{{ task.seq }}次 · {{ task.planDate }}</strong>
            <el-tag :type="taskStatusTagType(task.status)" size="small">{{ taskStatusText(task.status) }}</el-tag>
          </div>
          <div class="task-items">{{ task.items }}</div>
          <div v-if="task.doneDate" class="task-done">完成日期：{{ task.doneDate }}</div>
        </el-timeline-item>
      </el-timeline>
    </template>

    <el-empty v-else-if="!loading" description="该患者暂无随访计划" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { timeline } from '../../api/followup'

// 任务状态：0未到期 1临期 2逾期 3已完成 4失访 5已取消；计划状态：1进行中 2已完成 3已终止（specs/global/20）
const TASK_STATUS_TEXT = { 0: '未到期', 1: '临期', 2: '逾期', 3: '已完成', 4: '失访', 5: '已取消' }
const TASK_STATUS_TAG = { 0: 'info', 1: 'warning', 2: 'danger', 3: 'success', 4: 'danger', 5: 'info' }
const PLAN_STATUS_TEXT = { 1: '进行中', 2: '已完成', 3: '已终止' }
const PLAN_STATUS_TAG = { 1: 'primary', 2: 'success', 3: 'info' }

const taskStatusText = (s) => TASK_STATUS_TEXT[s] ?? `未知(${s})`
const taskStatusTagType = (s) => TASK_STATUS_TAG[s] ?? 'info'
const planStatusText = (s) => PLAN_STATUS_TEXT[s] ?? `未知(${s})`
const planStatusTagType = (s) => PLAN_STATUS_TAG[s] ?? 'info'
// 已完成绿色、逾期红色，其余默认
const timelineItemType = (s) => (s === 3 ? 'success' : s === 2 ? 'danger' : '')

const route = useRoute()
const plan = ref(null)
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    plan.value = await timeline(route.params.patientId)
  } catch {
    // 业务错误已由拦截器弹窗，这里兜底为空态
    plan.value = null
  } finally {
    loading.value = false
  }
}

loadData()
</script>

<style scoped>
.followup-timeline {
  padding: 16px;
}
.plan-header {
  margin-bottom: 16px;
}
.adjust-alert {
  margin-bottom: 16px;
}
.task-timeline {
  margin-top: 8px;
  padding-left: 8px;
}
.task-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.task-items {
  color: var(--el-text-color-regular);
  line-height: 1.6;
}
.task-done {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
