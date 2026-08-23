<template>
  <div class="dashboard-page">
    <el-row :gutter="16" class="stat-row" v-loading="overviewLoading">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :class="{ danger: card.danger }">{{ card.value }}</div>
          <div class="stat-footer">更新于 {{ overviewData?.updatedAt || '-' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12" class="chart-col">
        <el-card shadow="never">
          <template #header>结节类型分布</template>
          <div ref="noduleChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12" class="chart-col">
        <el-card shadow="never">
          <template #header>随访完成率（近 6 个月）</template>
          <div ref="followupChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12" class="chart-col">
        <el-card shadow="never">
          <template #header>医生工作量</template>
          <div ref="workloadChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12" class="chart-col">
        <el-card shadow="never">
          <template #header>风险等级分布</template>
          <div ref="riskChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { overview, noduleDistribution, followupRate, doctorWorkload } from '../../api/stats'

const overviewData = ref(null)
const overviewLoading = ref(false)

const statCards = computed(() => [
  { label: '在管患者数', value: overviewData.value?.managingCount ?? '-' },
  { label: '进行中计划', value: overviewData.value?.activePlanCount ?? '-' },
  { label: '逾期任务', value: overviewData.value?.overdueTaskCount ?? '-', danger: true },
  { label: '预警', value: overviewData.value?.alertCount ?? '-' }
])

const noduleChartRef = ref(null)
const followupChartRef = ref(null)
const workloadChartRef = ref(null)
const riskChartRef = ref(null)

// ECharts 生命周期统一封装：init 于 mounted+nextTick、resize 监听、onUnmounted dispose
const charts = []
function initChart(el) {
  const chart = echarts.init(el)
  charts.push(chart)
  return chart
}
function handleResize() {
  charts.forEach((chart) => chart.resize())
}

const percentFormatter = (v) => `${(v * 100).toFixed(0)}%`

function pieOption(data) {
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        data
      }
    ]
  }
}

async function loadNoduleChart() {
  const chart = initChart(noduleChartRef.value)
  try {
    const list = await noduleDistribution()
    const data = (list || [])
      .filter((item) => item.dimension === 'nodule_type')
      .map((item) => ({ name: item.label, value: item.cnt }))
    chart.setOption(pieOption(data))
  } catch {
    chart.setOption(pieOption([]))
  }
}

async function loadFollowupChart() {
  const chart = initChart(followupChartRef.value)
  let list = []
  try {
    list = (await followupRate(6)) || []
  } catch {
    list = []
  }
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v) => (typeof v === 'number' ? percentFormatter(v) : '-')
    },
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: list.map((item) => item.statMonth) },
    yAxis: { type: 'value', max: 1, axisLabel: { formatter: (v) => percentFormatter(v) } },
    series: [
      { name: '完成率', type: 'line', smooth: true, data: list.map((item) => item.completionRate) },
      { name: '及时率', type: 'line', smooth: true, data: list.map((item) => item.timelyRate) }
    ]
  })
}

async function loadWorkloadChart() {
  const chart = initChart(workloadChartRef.value)
  let list = []
  try {
    list = (await doctorWorkload()) || []
  } catch {
    list = []
  }
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: list.map((item) => item.doctorName) },
    yAxis: { type: 'value' },
    series: [
      { name: '建档数', type: 'bar', data: list.map((item) => item.archiveCount) },
      { name: '完成随访数', type: 'bar', data: list.map((item) => item.doneCount) }
    ]
  })
}

async function loadRiskChart() {
  const chart = initChart(riskChartRef.value)
  try {
    const list = await noduleDistribution()
    const data = (list || [])
      .filter((item) => item.dimension === 'risk_level')
      .map((item) => ({ name: item.label, value: item.cnt }))
    chart.setOption(pieOption(data))
  } catch {
    chart.setOption(pieOption([]))
  }
}

onMounted(async () => {
  overviewLoading.value = true
  try {
    overviewData.value = await overview()
  } catch {
    // 接口失败已由拦截器弹错，统计卡显示 "-" 兜底
    overviewData.value = null
  } finally {
    overviewLoading.value = false
  }

  await nextTick()
  window.addEventListener('resize', handleResize)
  loadNoduleChart()
  loadFollowupChart()
  loadWorkloadChart()
  loadRiskChart()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach((chart) => chart.dispose())
})
</script>

<style scoped>
.dashboard-page {
  padding: 4px;
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
.stat-footer {
  margin-top: 8px;
  font-size: 12px;
  color: #c0c4cc;
}
.chart-col {
  margin-bottom: 16px;
}
.chart-box {
  height: 320px;
}
</style>
