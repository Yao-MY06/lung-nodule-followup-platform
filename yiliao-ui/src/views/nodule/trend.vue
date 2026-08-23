<template>
  <div class="trend-page">
    <!-- 顶部：结节选择 -->
    <el-card shadow="never">
      <template #header>结节趋势（患者ID：{{ patientId }}）</template>
      <div class="select-bar">
        <el-select
          v-model="currentNoduleId"
          placeholder="选择结节"
          style="width: 360px"
          :loading="loadingNodules"
          @change="onNoduleChange"
        >
          <el-option
            v-for="n in nodules"
            :key="n.id"
            :value="n.id"
            :label="noduleLabel(n)"
          />
        </el-select>
        <span v-if="!loadingNodules && !nodules.length" class="empty-tip">该患者暂无结节登记</span>
      </div>
    </el-card>

    <!-- 中部：趋势折线图 -->
    <el-card shadow="never" class="chart-card">
      <template #header>直径变化趋势</template>
      <div v-if="currentNoduleId" ref="chartRef" class="trend-chart"></div>
      <el-empty v-else description="请先选择结节" />
    </el-card>

    <!-- 最近对比 -->
    <el-card v-if="compare" shadow="never" class="compare-card">
      <template #header>
        <div class="compare-header">
          <span>最近对比（{{ compare.previousExamDate || '无基线' }} → {{ compare.currentExamDate || '-' }}）</span>
          <el-tag v-if="compare.progress" type="danger" effect="dark">结节进展（≥2mm）</el-tag>
          <el-tag v-else-if="compare.newNodule" type="primary" effect="dark">新发结节</el-tag>
          <el-tag v-else type="success" effect="dark">稳定</el-tag>
        </div>
      </template>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="Δ最大径">{{ fmtDelta(compare.deltaMaxMm, 'mm') }}</el-descriptions-item>
        <el-descriptions-item label="Δ实性成分">{{ fmtDelta(compare.deltaSolidMm, 'mm') }}</el-descriptions-item>
        <el-descriptions-item label="Δ密度">{{ fmtDelta(compare.deltaDensityHu, 'HU') }}</el-descriptions-item>
      </el-descriptions>
      <el-alert
        :type="compare.progress ? 'error' : compare.newNodule ? 'info' : 'success'"
        :closable="false"
        class="advice-alert"
        :title="compare.advice || ''"
      />
    </el-card>

    <!-- 底部：录入复查快照 -->
    <el-card v-if="currentNoduleId" shadow="never" class="snapshot-card">
      <el-collapse>
        <el-collapse-item title="录入复查快照" name="snapshot">
          <el-form ref="snapshotFormRef" :model="snapshotForm" :rules="snapshotRules" label-width="100px" class="snapshot-form">
            <el-form-item label="检查日期" prop="examDate">
              <el-date-picker v-model="snapshotForm.examDate" type="date" value-format="YYYY-MM-DD" placeholder="选择检查日期" />
            </el-form-item>
            <el-form-item label="最大径 (mm)">
              <el-input-number v-model="snapshotForm.maxDiameterMm" :min="0" :precision="1" :step="0.5" />
            </el-form-item>
            <el-form-item label="实性成分 (mm)">
              <el-input-number v-model="snapshotForm.solidDiameterMm" :min="0" :precision="1" :step="0.5" />
            </el-form-item>
            <el-form-item label="新发结节">
              <el-switch v-model="snapshotForm.isNew" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submittingSnapshot" @click="submitSnapshot">提交快照</el-button>
            </el-form-item>
          </el-form>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { listNodules, getTrend, getCompare, createSnapshot } from '../../api/nodule'

const route = useRoute()
const patientId = route.params.patientId

// ── 结节选择 ──
const nodules = ref([])
const loadingNodules = ref(false)
const currentNoduleId = ref(null)

const TYPE_TEXT = { 1: '实性', 2: '部分实性', 3: '纯磨玻璃' }

function noduleLabel(n) {
  return [n.noduleNo, n.location || '位置未知', TYPE_TEXT[n.noduleType] || '类型未知'].join(' · ')
}

async function loadNodules() {
  loadingNodules.value = true
  try {
    nodules.value = (await listNodules(patientId)) || []
    if (nodules.value.length) {
      currentNoduleId.value = nodules.value[0].id
      await onNoduleChange()
    }
  } catch (e) {
    nodules.value = []
  } finally {
    loadingNodules.value = false
  }
}

async function onNoduleChange() {
  compare.value = null
  await Promise.all([loadTrend(), loadCompare()])
}

// ── ECharts 趋势图 ──
const chartRef = ref()
let chart = null

function ensureChart() {
  if (!chart && chartRef.value) {
    chart = echarts.init(chartRef.value)
  }
  return chart
}

async function loadTrend() {
  if (!currentNoduleId.value) return
  try {
    const list = (await getTrend(currentNoduleId.value)) || []
    await nextTick()
    const instance = ensureChart()
    if (!instance) return
    instance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['最大径mm', '实性成分mm'] },
      grid: { left: 48, right: 24, top: 40, bottom: 32 },
      xAxis: { type: 'category', data: list.map((s) => s.examDate) },
      yAxis: { type: 'value', name: 'mm' },
      series: [
        {
          name: '最大径mm',
          type: 'line',
          data: list.map((s) => s.maxDiameterMm ?? null)
        },
        {
          name: '实性成分mm',
          type: 'line',
          data: list.map((s) => s.solidDiameterMm ?? null)
        }
      ]
    }, true)
  } catch (e) {
    const instance = ensureChart()
    if (instance) {
      instance.setOption({ xAxis: { data: [] }, series: [{ data: [] }, { data: [] }] }, true)
    }
  }
}

function onResize() {
  chart?.resize()
}

// ── 最近对比 ──
const compare = ref(null)

async function loadCompare() {
  if (!currentNoduleId.value) return
  try {
    compare.value = await getCompare(currentNoduleId.value)
  } catch (e) {
    compare.value = null
  }
}

function fmtDelta(value, unit) {
  if (value == null) return '-'
  const sign = value > 0 ? '+' : ''
  return `${sign}${value} ${unit}`
}

// ── 录入复查快照 ──
const snapshotFormRef = ref()
const snapshotForm = reactive({
  examDate: '',
  maxDiameterMm: null,
  solidDiameterMm: null,
  isNew: false
})
const snapshotRules = {
  examDate: [{ required: true, message: '请选择检查日期', trigger: 'change' }]
}
const submittingSnapshot = ref(false)

async function submitSnapshot() {
  await snapshotFormRef.value.validate()
  submittingSnapshot.value = true
  try {
    const result = await createSnapshot({
      noduleId: currentNoduleId.value,
      examDate: snapshotForm.examDate,
      maxDiameterMm: snapshotForm.maxDiameterMm,
      solidDiameterMm: snapshotForm.solidDiameterMm,
      isNew: snapshotForm.isNew ? 1 : 0
    })
    ElMessage.success('快照已录入')
    // 返回体即最新对比结果（CompareVO），直接更新对比卡并重拉趋势
    compare.value = result || null
    snapshotFormRef.value.resetFields()
    await loadTrend()
  } catch (e) {
    // 业务错误已由拦截器统一弹错
  } finally {
    submittingSnapshot.value = false
  }
}

// ── 生命周期 ──
onMounted(async () => {
  await nextTick()
  window.addEventListener('resize', onResize)
  await loadNodules()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.trend-page {
  padding: 16px;
}
.select-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}
.empty-tip {
  color: #909399;
  font-size: 13px;
}
.chart-card {
  margin-top: 16px;
}
.trend-chart {
  width: 100%;
  height: 320px;
}
.compare-card {
  margin-top: 16px;
}
.compare-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.advice-alert {
  margin-top: 12px;
}
.snapshot-card {
  margin-top: 16px;
}
.snapshot-form {
  max-width: 420px;
}
</style>
