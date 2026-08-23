<template>
  <div class="followup-workbench">
    <div class="toolbar">
      <el-tabs v-model="query.status" class="status-tabs" @tab-change="onTabChange">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="未到期" name="0" />
        <el-tab-pane label="临期" name="1" />
        <el-tab-pane name="2">
          <template #label>
            <span class="tab-overdue">逾期</span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="已完成" name="3" />
        <el-tab-pane label="失访" name="4" />
      </el-tabs>
      <el-button-group class="toolbar-actions">
        <el-button type="primary" :icon="Plus" @click="openGenerate">生成计划</el-button>
        <el-button :icon="Refresh" @click="openRegenerate">重新生成（规则演示）</el-button>
      </el-button-group>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="seq" label="第N次" width="80" align="center">
        <template #default="{ row }">第{{ row.seq }}次</template>
      </el-table-column>
      <el-table-column prop="patientId" label="患者ID" width="100" align="center" />
      <el-table-column prop="planDate" label="计划日期" width="120" />
      <el-table-column label="随访事项" min-width="220">
        <template #default="{ row }">
          <el-tooltip :content="row.items" placement="top" :disabled="!row.items || row.items.length <= 30">
            <span>{{ truncate(row.items) }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="doneDate" label="完成日期" width="120">
        <template #default="{ row }">{{ row.doneDate || '—' }}</template>
      </el-table-column>
      <el-table-column prop="remindSent" label="提醒次数" width="90" align="center">
        <template #default="{ row }">{{ row.remindSent ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            type="primary"
            :disabled="!operable(row.status)"
            @click="openRecord(row)"
          >完成随访</el-button>
          <el-button
            size="small"
            :disabled="!operable(row.status)"
            @click="openAdjust(row)"
          >调整日期</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无随访任务" />
      </template>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="loadData"
        @size-change="onSizeChange"
      />
    </div>

    <!-- 完成随访 -->
    <el-dialog v-model="recordDialog.visible" title="完成随访" width="520px" @closed="resetRecordForm">
      <el-form ref="recordFormRef" :model="recordDialog.form" :rules="recordRules" label-width="90px">
        <el-form-item label="随访方式" prop="followupType">
          <el-select v-model="recordDialog.form.followupType" placeholder="请选择" style="width: 100%">
            <el-option label="电话" :value="1" />
            <el-option label="门诊" :value="2" />
            <el-option label="线上问卷" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="随访内容" prop="content">
          <el-input v-model="recordDialog.form.content" type="textarea" :rows="4" placeholder="随访过程与患者反馈" />
        </el-form-item>
        <el-form-item label="结果小结">
          <el-input v-model="recordDialog.form.resultSummary" placeholder="选填" />
        </el-form-item>
        <el-form-item label="后续建议">
          <el-input v-model="recordDialog.form.nextAdvice" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="recordDialog.loading" @click="doSubmitRecord">提交</el-button>
      </template>
    </el-dialog>

    <!-- 调整日期 -->
    <el-dialog v-model="adjustDialog.visible" title="调整计划日期" width="420px" @closed="adjustDialog.planDate = null">
      <el-form label-width="90px">
        <el-form-item label="计划日期" required>
          <el-date-picker
            v-model="adjustDialog.planDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="adjustDialog.loading" :disabled="!adjustDialog.planDate" @click="doAdjust">确定</el-button>
      </template>
    </el-dialog>

    <!-- 生成计划 -->
    <el-dialog v-model="planDialog.visible" title="生成随访计划" width="480px" @closed="resetPlanForm">
      <el-form ref="planFormRef" :model="planDialog.form" :rules="planRules" label-width="100px">
        <el-form-item label="患者ID" prop="patientId">
          <el-input-number v-model="planDialog.form.patientId" :min="1" :controls="false" style="width: 100%" placeholder="请输入患者ID" />
        </el-form-item>
        <el-form-item label="结节类型">
          <el-select v-model="planDialog.form.noduleType" style="width: 100%">
            <el-option label="实性结节" :value="1" />
            <el-option label="部分实性结节" :value="2" />
            <el-option label="纯磨玻璃结节" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大径(mm)">
          <el-input-number v-model="planDialog.form.maxDiaMm" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="危险因素">
          <el-switch v-model="planDialog.form.riskFactor" active-text="有" inactive-text="无" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="planDialog.loading" @click="doGenerate">生成</el-button>
      </template>
    </el-dialog>

    <!-- 重新生成（规则演示） -->
    <el-dialog v-model="regenDialog.visible" title="重新生成随访计划" width="480px" @closed="resetRegenForm">
      <el-alert
        type="info"
        :closable="false"
        title="修改 decision_rule 表数据后点此重新生成——表驱动规则引擎即时生效演示"
        style="margin-bottom: 16px"
      />
      <el-form ref="regenFormRef" :model="regenDialog.form" :rules="planRules" label-width="100px">
        <el-form-item label="患者ID" prop="patientId">
          <el-input-number v-model="regenDialog.form.patientId" :min="1" :controls="false" style="width: 100%" placeholder="请输入患者ID" />
        </el-form-item>
        <el-form-item label="结节类型">
          <el-select v-model="regenDialog.form.noduleType" style="width: 100%">
            <el-option label="实性结节" :value="1" />
            <el-option label="部分实性结节" :value="2" />
            <el-option label="纯磨玻璃结节" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大径(mm)">
          <el-input-number v-model="regenDialog.form.maxDiaMm" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="危险因素">
          <el-switch v-model="regenDialog.form.riskFactor" active-text="有" inactive-text="无" />
        </el-form-item>
        <el-form-item label="重新生成原因">
          <el-input v-model="regenDialog.form.reason" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="regenDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="regenDialog.loading" @click="doRegenerate">重新生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { workbench, submitRecord, adjustTask, generatePlan, regeneratePlan } from '../../api/followup'

// 任务状态：0未到期 1临期 2逾期 3已完成 4失访 5已取消（specs/global/20）
const STATUS_TEXT = { 0: '未到期', 1: '临期', 2: '逾期', 3: '已完成', 4: '失访', 5: '已取消' }
const STATUS_TAG = { 0: 'info', 1: 'warning', 2: 'danger', 3: 'success', 4: 'danger', 5: 'info' }
const statusText = (s) => STATUS_TEXT[s] ?? `未知(${s})`
const statusTagType = (s) => STATUS_TAG[s] ?? 'info'
const operable = (s) => s === 0 || s === 1 || s === 2
const truncate = (text) => (text && text.length > 30 ? `${text.slice(0, 30)}…` : text || '—')

const query = reactive({ status: '', page: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.status !== '') params.status = Number(query.status)
    const data = await workbench(params)
    rows.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    // 业务错误已由拦截器弹窗，这里只做兜底显示
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  query.page = 1
  loadData()
}

function onSizeChange() {
  query.page = 1
  loadData()
}

// ---- 完成随访 ----
const recordFormRef = ref()
const recordDialog = reactive({
  visible: false,
  loading: false,
  taskId: null,
  form: { followupType: null, content: '', resultSummary: '', nextAdvice: '' }
})
const recordRules = {
  followupType: [{ required: true, message: '请选择随访方式', trigger: 'change' }],
  content: [{ required: true, message: '请填写随访内容', trigger: 'blur' }]
}

function openRecord(row) {
  recordDialog.taskId = row.id
  recordDialog.visible = true
}

function resetRecordForm() {
  recordDialog.taskId = null
  recordDialog.form = { followupType: null, content: '', resultSummary: '', nextAdvice: '' }
  recordFormRef.value?.clearValidate()
}

async function doSubmitRecord() {
  await recordFormRef.value.validate()
  recordDialog.loading = true
  try {
    await submitRecord(recordDialog.taskId, { ...recordDialog.form })
    ElMessage.success('随访记录已提交')
    recordDialog.visible = false
    loadData()
  } catch {
    // 拦截器已提示
  } finally {
    recordDialog.loading = false
  }
}

// ---- 调整日期 ----
const adjustDialog = reactive({ visible: false, loading: false, taskId: null, planDate: null })

function openAdjust(row) {
  adjustDialog.taskId = row.id
  adjustDialog.planDate = row.planDate || null
  adjustDialog.visible = true
}

async function doAdjust() {
  adjustDialog.loading = true
  try {
    await adjustTask(adjustDialog.taskId, { planDate: adjustDialog.planDate })
    ElMessage.success('计划日期已调整')
    adjustDialog.visible = false
    loadData()
  } catch {
    // 拦截器已提示
  } finally {
    adjustDialog.loading = false
  }
}

// ---- 生成 / 重新生成计划（scene 固定 1=结节随访） ----
const planFormRef = ref()
const regenFormRef = ref()
const planRules = {
  patientId: [{ required: true, message: '请输入患者ID', trigger: 'blur' }]
}

const planDialog = reactive({
  visible: false,
  loading: false,
  form: { patientId: null, noduleType: 1, maxDiaMm: 8, riskFactor: false }
})
const regenDialog = reactive({
  visible: false,
  loading: false,
  form: { patientId: null, noduleType: 1, maxDiaMm: 8, riskFactor: false, reason: '规则变更后重新生成' }
})

function openGenerate() {
  planDialog.visible = true
}

function openRegenerate() {
  regenDialog.visible = true
}

function resetPlanForm() {
  planDialog.form = { patientId: null, noduleType: 1, maxDiaMm: 8, riskFactor: false }
  planFormRef.value?.clearValidate()
}

function resetRegenForm() {
  regenDialog.form = { patientId: null, noduleType: 1, maxDiaMm: 8, riskFactor: false, reason: '规则变更后重新生成' }
  regenFormRef.value?.clearValidate()
}

function buildInput(form) {
  return { noduleType: form.noduleType, maxDiaMm: form.maxDiaMm, riskFactor: form.riskFactor }
}

async function doGenerate() {
  await planFormRef.value.validate()
  planDialog.loading = true
  try {
    const res = await generatePlan({
      patientId: planDialog.form.patientId,
      scene: 1,
      input: buildInput(planDialog.form)
    })
    const planId = res?.planId ?? res
    ElMessage.success(`计划 #${planId} 已生成`)
    planDialog.visible = false
    loadData()
  } catch {
    // 拦截器已提示
  } finally {
    planDialog.loading = false
  }
}

async function doRegenerate() {
  await regenFormRef.value.validate()
  regenDialog.loading = true
  try {
    const res = await regeneratePlan(
      regenDialog.form.patientId,
      1,
      buildInput(regenDialog.form),
      regenDialog.form.reason
    )
    const planId = res?.planId ?? res
    ElMessage.success(`计划 #${planId} 已重新生成`)
    regenDialog.visible = false
    loadData()
  } catch {
    // 拦截器已提示
  } finally {
    regenDialog.loading = false
  }
}

loadData()
</script>

<style scoped>
.followup-workbench {
  padding: 16px;
}
.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.status-tabs {
  flex: 1;
}
.tab-overdue {
  color: var(--el-color-danger);
}
.toolbar-actions {
  flex-shrink: 0;
  margin-top: 4px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
