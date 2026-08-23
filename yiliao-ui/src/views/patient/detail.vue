<template>
  <div class="patient-detail">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="header-bar">
          <span>患者详情</span>
          <div v-if="archive" class="actions">
            <el-button type="primary" plain @click="goTrend">结节趋势</el-button>
            <el-button type="primary" plain @click="goTimeline">随访时间轴</el-button>
            <el-dropdown trigger="click" @command="onChangeStage">
              <el-button type="warning">
                变更阶段<el-icon style="margin-left: 4px"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="s in stageLabels"
                    :key="s"
                    :command="s"
                    :disabled="s === archive.stageLabel"
                  >
                    {{ s }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </template>

      <template v-if="archive">
        <el-descriptions :column="2" border title="基本信息">
          <el-descriptions-item label="患者编号">{{ archive.patientNo }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ archive.name }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ genderText(archive.gender) }}</el-descriptions-item>
          <el-descriptions-item label="出生日期">{{ archive.birthDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="证件号">{{ archive.idCardMasked || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ archive.phoneMasked || '-' }}</el-descriptions-item>
          <el-descriptions-item label="阶段">
            <el-tag :type="stageTagType(archive.stageLabel)">{{ archive.stageLabel || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="来源">{{ sourceText(archive.sourceType) }}</el-descriptions-item>
          <el-descriptions-item label="主管医生">{{ archive.doctorId ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ archive.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions :column="2" border title="危险因素" style="margin-top: 20px">
          <el-descriptions-item label="吸烟包年">{{ archive.riskFactor?.smokingPackYear ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="肺癌家族史">{{ boolText(archive.riskFactor?.familyHistory) }}</el-descriptions-item>
          <el-descriptions-item label="职业暴露史">{{ boolText(archive.riskFactor?.occupationalExposure) }}</el-descriptions-item>
          <el-descriptions-item label="既往肿瘤史">{{ boolText(archive.riskFactor?.priorCancer) }}</el-descriptions-item>
          <el-descriptions-item label="合并症" :span="2">{{ archive.riskFactor?.comorbidity || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="nodule-block">
          <div class="block-title">结节列表</div>
          <el-table v-loading="noduleLoading" :data="nodules" border empty-text="暂无结节登记">
            <el-table-column prop="noduleNo" label="结节编号" width="150" />
            <el-table-column prop="location" label="部位" min-width="140" />
            <el-table-column label="类型" width="110">
              <template #default="{ row }">{{ noduleTypeText(row.noduleType) }}</template>
            </el-table-column>
            <el-table-column prop="firstFoundDate" label="首次发现日期" width="130" />
          </el-table>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="档案加载失败或不存在">
        <el-button type="primary" @click="load">重新加载</el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { getArchive, changeStage } from '../../api/patient'
import { listNodules } from '../../api/nodule'

const route = useRoute()
const router = useRouter()
const id = route.params.id

const stageLabels = ['结节随访中', '术后随访中', '治疗中', 'MDT中', '失访', '结案']

const archive = ref(null)
const loading = ref(false)
const nodules = ref([])
const noduleLoading = ref(false)

const genderText = (g) => (g === 1 ? '男' : g === 2 ? '女' : '-')
const sourceText = (s) => ({ 1: '体检', 2: '门诊', 3: '住院' }[s] || '-')
const boolText = (v) => (v === 1 || v === true ? '是' : v === 0 || v === false ? '否' : '-')
const noduleTypeText = (t) => ({ 1: '实性', 2: '部分实性', 3: '纯磨玻璃' }[t] || '-')
const stageTagType = (label) => {
  if (label === '失访') return 'danger'
  if (label === 'MDT中') return 'warning'
  if (label === '结案') return 'info'
  return 'primary'
}

async function load() {
  loading.value = true
  try {
    archive.value = await getArchive(id)
  } catch {
    // 业务错误已由拦截器弹提示，兜底为空态
    archive.value = null
  } finally {
    loading.value = false
  }
}

async function loadNodules() {
  noduleLoading.value = true
  try {
    nodules.value = (await listNodules(id)) || []
  } catch {
    nodules.value = []
  } finally {
    noduleLoading.value = false
  }
}

function goTrend() {
  router.push('/nodule/trend/' + id)
}

function goTimeline() {
  router.push('/followup/timeline/' + id)
}

async function onChangeStage(label) {
  try {
    await changeStage(id, label)
    ElMessage.success('阶段已变更为：' + label)
    load()
  } catch {
    // 业务错误（如非法流转）已由拦截器弹提示
  }
}

onMounted(() => {
  load()
  loadNodules()
})
</script>

<style scoped>
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
.nodule-block {
  margin-top: 20px;
}
.block-title {
  font-weight: 600;
  margin-bottom: 10px;
}
</style>
