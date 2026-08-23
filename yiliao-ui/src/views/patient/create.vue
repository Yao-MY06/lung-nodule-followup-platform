<template>
  <div class="patient-create">
    <el-card shadow="never">
      <template #header>患者建档</template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 720px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="患者姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期" prop="birthDate">
          <el-date-picker
            v-model="form.birthDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
          />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="18 位身份证号（加密存储）" maxlength="18" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="住址" prop="address">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="紧急联系人" prop="emergencyContact">
          <el-input v-model="form.emergencyContact" />
        </el-form-item>
        <el-form-item label="紧急联系电话" prop="emergencyPhone">
          <el-input v-model="form.emergencyPhone" maxlength="11" />
        </el-form-item>
        <el-form-item label="主管医生 ID" prop="doctorId">
          <el-input-number v-model="form.doctorId" :min="1" :controls="false" placeholder="医生用户 ID" style="width: 180px" />
        </el-form-item>
        <el-form-item label="来源" prop="sourceType">
          <el-select v-model="form.sourceType" placeholder="请选择来源" style="width: 180px">
            <el-option label="体检" :value="1" />
            <el-option label="门诊" :value="2" />
            <el-option label="住院" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>

        <el-collapse v-model="activePanels">
          <el-collapse-item title="危险因素" name="risk">
            <el-form-item label="吸烟包年">
              <el-input-number v-model="form.riskFactor.smokingPackYear" :min="0" :max="200" style="width: 180px" />
            </el-form-item>
            <el-form-item label="肺癌家族史">
              <el-switch v-model="form.riskFactor.familyHistory" />
            </el-form-item>
            <el-form-item label="职业暴露史">
              <el-switch v-model="form.riskFactor.occupationalExposure" />
            </el-form-item>
            <el-form-item label="既往肿瘤史">
              <el-switch v-model="form.riskFactor.priorCancer" />
            </el-form-item>
            <el-form-item label="合并症">
              <el-input v-model="form.riskFactor.comorbidity" placeholder="如 COPD、糖尿病等" />
            </el-form-item>
          </el-collapse-item>

          <el-collapse-item title="自动生成随访计划" name="plan">
            <el-form-item label="自动生成">
              <el-switch v-model="planEnabled" />
              <span class="tip">开启后建档成功自动生成随访计划（场景：结节随访）</span>
            </el-form-item>
            <template v-if="planEnabled">
              <el-form-item label="结节类型">
                <el-select v-model="form.plan.input.noduleType" placeholder="请选择" clearable style="width: 180px">
                  <el-option label="实性" :value="1" />
                  <el-option label="部分实性" :value="2" />
                  <el-option label="纯磨玻璃" :value="3" />
                </el-select>
              </el-form-item>
              <el-form-item label="最大径 (mm)">
                <el-input-number v-model="form.plan.input.maxDiaMm" :min="0" :max="100" :precision="1" style="width: 180px" />
              </el-form-item>
              <el-form-item label="危险因素">
                <el-select
                  v-model="form.plan.input.riskFactor"
                  placeholder="不填（按危险因素档案推断）"
                  clearable
                  style="width: 240px"
                >
                  <el-option label="有危险因素" :value="true" />
                  <el-option label="无危险因素" :value="false" />
                </el-select>
              </el-form-item>
            </template>
          </el-collapse-item>
        </el-collapse>

        <el-form-item style="margin-top: 20px">
          <el-button type="primary" :loading="submitting" @click="onSubmit">提交建档</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createArchive } from '../../api/patient'

const router = useRouter()

const formRef = ref()
const submitting = ref(false)
const planEnabled = ref(true)
const activePanels = ref(['plan'])

const emptyForm = () => ({
  name: '',
  gender: 1,
  birthDate: null,
  idCard: '',
  phone: '',
  address: '',
  emergencyContact: '',
  emergencyPhone: '',
  doctorId: null,
  sourceType: null,
  remark: '',
  riskFactor: {
    smokingPackYear: null,
    familyHistory: false,
    occupationalExposure: false,
    priorCancer: false,
    comorbidity: ''
  },
  plan: {
    input: {
      noduleType: null,
      maxDiaMm: null,
      riskFactor: null
    }
  }
})

const form = reactive(emptyForm())

const rules = {
  name: [{ required: true, message: '请输入患者姓名', trigger: 'blur' }],
  sourceType: [{ required: true, message: '请选择来源', trigger: 'change' }]
}

function buildPayload() {
  const payload = {
    name: form.name,
    gender: form.gender,
    birthDate: form.birthDate,
    idCard: form.idCard || null,
    phone: form.phone || null,
    address: form.address || null,
    emergencyContact: form.emergencyContact || null,
    emergencyPhone: form.emergencyPhone || null,
    doctorId: form.doctorId,
    sourceType: form.sourceType,
    remark: form.remark || null,
    // 后端 RiskFactorRequest 三个开关字段为 Integer（1/0），表单内绑定布尔，提交时转换
    riskFactor: {
      smokingPackYear: form.riskFactor.smokingPackYear,
      familyHistory: form.riskFactor.familyHistory ? 1 : 0,
      occupationalExposure: form.riskFactor.occupationalExposure ? 1 : 0,
      priorCancer: form.riskFactor.priorCancer ? 1 : 0,
      comorbidity: form.riskFactor.comorbidity || null
    }
  }
  if (planEnabled.value) {
    payload.plan = {
      scene: 1,
      input: {
        noduleType: form.plan.input.noduleType,
        maxDiaMm: form.plan.input.maxDiaMm,
        riskFactor: form.plan.input.riskFactor
      }
    }
  }
  return payload
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return // 校验未通过，Element Plus 已在表单项上提示
  }
  submitting.value = true
  try {
    const data = await createArchive(buildPayload())
    ElMessage.success('建档成功：' + data.patientNo)
    if (data.autoPlanError) {
      await ElMessageBox.alert(
        `随访计划自动生成失败：${data.autoPlanError}，可稍后在随访工作台手动生成`,
        '提示',
        { type: 'warning', confirmButtonText: '知道了' }
      )
      router.push('/patient/detail/' + data.archiveId)
    } else if (data.planId) {
      ElMessageBox.confirm('随访计划已自动生成，是否查看时间轴？', '提示', {
        type: 'success',
        confirmButtonText: '查看时间轴',
        cancelButtonText: '查看档案'
      })
        .then(() => router.push('/followup/timeline/' + data.archiveId))
        .catch(() => router.push('/patient/detail/' + data.archiveId))
    } else {
      router.push('/patient/detail/' + data.archiveId)
    }
  } catch {
    // 业务错误已由拦截器弹提示
  } finally {
    submitting.value = false
  }
}

function onReset() {
  Object.assign(form, emptyForm())
  planEnabled.value = true
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
</style>
