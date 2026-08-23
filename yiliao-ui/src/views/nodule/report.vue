<template>
  <div class="report-page">
    <el-row :gutter="16">
      <!-- 左栏：报告录入 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>报告录入</template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
            <el-form-item label="患者ID" prop="patientId">
              <el-input-number v-model="form.patientId" :min="1" :controls="false" placeholder="请输入患者ID" style="width: 100%" />
            </el-form-item>
            <el-form-item label="报告类型">
              <el-select :model-value="1" disabled style="width: 100%">
                <el-option :value="1" label="CT" />
              </el-select>
            </el-form-item>
            <el-form-item label="检查日期" prop="examDate">
              <el-date-picker v-model="form.examDate" type="date" value-format="YYYY-MM-DD" placeholder="选择检查日期" style="width: 100%" />
            </el-form-item>
            <el-form-item label="检查机构">
              <el-input v-model="form.orgName" placeholder="如：XX医院影像科" />
            </el-form-item>
            <el-form-item label="报告原文" prop="rawText">
              <el-input v-model="form.rawText" type="textarea" :rows="8" placeholder="粘贴 CT 报告原文，作为 AI 抽取输入" />
            </el-form-item>
            <el-form-item label="影像结论">
              <el-input v-model="form.conclusion" type="textarea" :rows="2" placeholder="选填" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="creating" @click="submitCreate">提交录入</el-button>
              <el-button @click="resetForm">清空</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右栏：报告列表与 AI 抽取 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>报告列表与 AI 抽取</template>
          <div class="query-bar">
            <el-input-number v-model="queryPatientId" :min="1" :controls="false" placeholder="患者ID" style="width: 180px" />
            <el-button type="primary" :loading="loadingList" @click="onQuery">查询</el-button>
          </div>

          <el-table
            ref="tableRef"
            v-loading="loadingList"
            :data="reports"
            highlight-current-row
            height="260"
            @current-change="onRowSelect"
          >
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="examDate" label="检查日期" width="110" />
            <el-table-column prop="orgName" label="检查机构" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.orgName || '-' }}</template>
            </el-table-column>
            <el-table-column label="抽取状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.extractStatus)">{{ statusText(row.extractStatus) }}</el-tag>
              </template>
            </el-table-column>
            <template #empty>暂无报告，请先在左侧录入或输入患者ID查询</template>
          </el-table>

          <div class="action-bar">
            <el-tooltip :disabled="!actionDisabledReason" :content="actionDisabledReason" placement="top">
              <span>
                <el-button type="primary" :disabled="!canExtract" :loading="extracting" @click="doExtract">
                  {{ extracting ? 'AI 解析中…' : 'AI 抽取' }}
                </el-button>
              </span>
            </el-tooltip>
            <el-tooltip :disabled="!confirmDisabledReason" :content="confirmDisabledReason" placement="top">
              <span>
                <el-button type="success" :disabled="!canConfirm" :loading="confirming" @click="doConfirm">
                  确认入库
                </el-button>
              </span>
            </el-tooltip>
          </div>

          <!-- AI 抽取草稿 -->
          <el-card v-if="draft" class="draft-card" shadow="never">
            <template #header>
              <el-alert
                type="warning"
                :closable="false"
                title="AI 结果仅为草稿，须医生逐项核对后确认方可入库"
              />
            </template>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="位置">{{ draft.location || '原文未提及' }}</el-descriptions-item>
              <el-descriptions-item label="结节类型">
                <el-tag size="small">{{ extractTypeText(draft.noduleType) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="最大径">
                {{ draft.maxDiameterMm != null ? draft.maxDiameterMm + ' mm' : '原文未提及' }}
              </el-descriptions-item>
              <el-descriptions-item label="实性成分">
                {{ draft.solidDiameterMm != null ? draft.solidDiameterMm + ' mm' : '原文未提及' }}
              </el-descriptions-item>
              <el-descriptions-item label="数量">{{ draft.count != null ? draft.count : '原文未提及' }}</el-descriptions-item>
              <el-descriptions-item label="恶性征象">
                <template v-if="draft.signs && draft.signs.length">
                  <el-tag v-for="s in draft.signs" :key="s" type="danger" size="small" class="sign-tag">{{ s }}</el-tag>
                </template>
                <template v-else>原文未提及</template>
              </el-descriptions-item>
              <el-descriptions-item label="影像结论" :span="2">{{ draft.impression || '原文未提及' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 确认入库结果 -->
          <el-card v-if="confirmResult" class="result-card" shadow="never">
            <template #header>确认结果</template>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="结节ID">{{ confirmResult.noduleId ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="快照ID">{{ confirmResult.snapshotId ?? '-' }}</el-descriptions-item>
            </el-descriptions>
            <el-alert
              v-if="confirmResult.planId"
              type="success"
              :closable="false"
              class="result-alert"
              title="随访计划已自动生成"
            >
              <template #default>
                计划ID：{{ confirmResult.planId }}，
                <el-link type="primary" @click="goTimeline">查看随访计划时间轴</el-link>
              </template>
            </el-alert>
            <el-alert
              v-else-if="confirmResult.planError"
              type="warning"
              :closable="false"
              class="result-alert"
              :title="'自动排期失败，请到随访工作台手动生成（' + confirmResult.planError + '）'"
            />
          </el-card>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createReport, pageReports, extractReport, confirmReport } from '../../api/nodule'

const router = useRouter()

// ── 左栏：报告录入 ──
const formRef = ref()
const form = reactive({
  patientId: null,
  reportType: 1,
  examDate: '',
  orgName: '',
  rawText: '',
  conclusion: ''
})
const rules = {
  patientId: [{ required: true, message: '请输入患者ID', trigger: 'blur' }],
  examDate: [{ required: true, message: '请选择检查日期', trigger: 'change' }],
  rawText: [{ required: true, message: '请粘贴报告原文', trigger: 'blur' }]
}
const creating = ref(false)

async function submitCreate() {
  await formRef.value.validate()
  creating.value = true
  try {
    await createReport({ ...form })
    ElMessage.success('报告录入成功')
    queryPatientId.value = form.patientId
    resetForm()
    await loadReports()
  } catch (e) {
    // 业务错误已由 request 拦截器统一弹错，此处仅兜底不重复提示
  } finally {
    creating.value = false
  }
}

function resetForm() {
  formRef.value?.resetFields()
  form.reportType = 1
}

// ── 右栏：报告列表 ──
const queryPatientId = ref(null)
const reports = ref([])
const loadingList = ref(false)
const currentRow = ref(null)
const tableRef = ref()

async function loadReports() {
  if (!queryPatientId.value) {
    ElMessage.warning('请先输入患者ID')
    return
  }
  loadingList.value = true
  try {
    const data = await pageReports({ patientId: queryPatientId.value, page: 1, size: 10 })
    reports.value = data?.records || []
    // 抽取/确认后的内部刷新：按 id 重新定位选中行（拿到最新 extractStatus），不清空草稿与结果卡
    if (currentRow.value) {
      currentRow.value = reports.value.find((r) => r.id === currentRow.value.id) || null
      await nextTick()
      if (currentRow.value) {
        tableRef.value?.setCurrentRow(currentRow.value)
      }
    }
  } catch (e) {
    reports.value = []
  } finally {
    loadingList.value = false
  }
}

// 手动点"查询"：清空选中与草稿/结果后重新加载
function onQuery() {
  currentRow.value = null
  draft.value = null
  confirmResult.value = null
  loadReports()
}

function onRowSelect(row) {
  currentRow.value = row || null
  draft.value = null
  confirmResult.value = null
}

function statusText(status) {
  return { 0: '未抽取', 1: '已抽取', 2: '已确认' }[status] ?? '未知'
}

function statusTagType(status) {
  return { 0: 'info', 1: 'primary', 2: 'success' }[status] ?? 'info'
}

// ── 按钮状态机 ──
const extracting = ref(false)
const confirming = ref(false)
const draft = ref(null)
const confirmResult = ref(null)

const actionDisabledReason = computed(() => {
  if (!currentRow.value) return '请先选中一行报告'
  if (currentRow.value.extractStatus === 2) return '已确认'
  return ''
})
const canExtract = computed(() => !actionDisabledReason.value && !extracting.value && !confirming.value)

const confirmDisabledReason = computed(() => {
  if (!currentRow.value) return '请先选中一行报告'
  if (currentRow.value.extractStatus === 2) return '已确认'
  if (extracting.value) return 'AI 解析中，请稍候'
  if (currentRow.value.extractStatus !== 1) return '请先执行 AI 抽取'
  return ''
})
const canConfirm = computed(() => !confirmDisabledReason.value && !confirming.value)

// ── AI 抽取 ──
async function doExtract() {
  if (!currentRow.value) return
  extracting.value = true
  try {
    const result = await extractReport(currentRow.value.id)
    ElMessage.success('AI 抽取完成，请逐项核对草稿')
    await loadReports()
    draft.value = result
  } catch (e) {
    draft.value = null
  } finally {
    extracting.value = false
  }
}

// ── 确认入库 ──
async function doConfirm() {
  if (!currentRow.value) return
  try {
    await ElMessageBox.confirm(
      'AI 结果仅为草稿，须医生逐项核对后确认方可入库。确认后将生成结节登记与复查快照，并尝试自动生成随访计划。是否确认？',
      '确认入库',
      { type: 'warning', confirmButtonText: '确认入库', cancelButtonText: '再核对一下' }
    )
  } catch (e) {
    return // 用户取消
  }
  confirming.value = true
  try {
    const result = await confirmReport(currentRow.value.id)
    ElMessage.success('已确认入库')
    await loadReports()
    draft.value = null
    confirmResult.value = result || {}
  } catch (e) {
    confirmResult.value = null
  } finally {
    confirming.value = false
  }
}

// ── 抽取结果类型映射（SOLID/PART_SOLID/PURE_GG） ──
function extractTypeText(type) {
  return { SOLID: '实性', PART_SOLID: '部分实性', PURE_GG: '纯磨玻璃' }[type] || '原文未提及'
}

function goTimeline() {
  if (queryPatientId.value) {
    router.push(`/followup/timeline/${queryPatientId.value}`)
  }
}
</script>

<style scoped>
.report-page {
  padding: 16px;
}
.query-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.action-bar {
  display: flex;
  gap: 8px;
  margin: 12px 0;
}
.draft-card,
.result-card {
  margin-top: 8px;
}
.sign-tag {
  margin-right: 4px;
}
.result-alert {
  margin-top: 12px;
}
</style>
