<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <h1 class="login-title">肺结节/肺癌患者管理系统</h1>
      <p class="login-subtitle">建档 · 随访 · 预警 一体化管理平台</p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        label-position="top"
        @keyup.enter="onSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            :prefix-icon="Lock"
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-button" :loading="loading" @click="onSubmit">登 录</el-button>
        </el-form-item>
      </el-form>
      <p class="login-tip">开发账号：admin/admin123、doctor01/doctor123</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    if (route.query.redirect) {
      router.push(String(route.query.redirect))
    } else if (auth.user?.userType === 4) {
      router.push('/portal/plan')
    } else {
      router.push('/home')
    }
  } catch {
    // 业务错误提示已由 request 拦截器统一弹出，这里仅复位 loading
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
}
.login-card {
  width: 420px;
  border-radius: 12px;
}
.login-title {
  margin: 8px 0 4px;
  text-align: center;
  font-size: 22px;
  color: #303133;
}
.login-subtitle {
  margin: 0 0 24px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}
.login-button {
  width: 100%;
}
.login-tip {
  margin: 8px 0 0;
  text-align: center;
  font-size: 12px;
  color: #909399;
}
</style>
