<template>
  <div class="login-page">
    <!-- 左：品牌面（青蓝渐变 + CT 扫描环装饰 + 闭环卖点） -->
    <aside class="brand-panel">
      <div class="brand-rings" aria-hidden="true"></div>
      <div class="brand-content">
        <div class="brand-badge">PULMONARY CARE</div>
        <h1 class="brand-title">肺结节 / 肺癌<br />患者管理系统</h1>
        <p class="brand-sub">建档 · 风险评估 · 随访计划 · 智能提醒 · 复查对比 · 预警干预</p>
        <ul class="brand-points">
          <li>指南规则引擎，随访计划自动生成</li>
          <li>AI 报告抽取与解读，医生确认后入库</li>
          <li>到点多级提醒，失访闭环追踪</li>
        </ul>
      </div>
    </aside>

    <!-- 右：登录表单 -->
    <main class="form-panel">
      <div class="form-box">
        <div class="form-mark">
          <span class="mark-ring"></span>
        </div>
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-sub">请使用工号账号登录</p>
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
          <el-form-item class="form-submit">
            <el-button type="primary" class="login-button" :loading="loading" @click="onSubmit">登 录</el-button>
          </el-form-item>
        </el-form>
        <p class="login-tip">开发账号：admin / admin123 · doctor01 / doctor123</p>
      </div>
    </main>
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
  display: flex;
  min-height: 100vh;
  background: var(--yl-bg);
}

/* ── 品牌面 ─────────────────────────────── */
.brand-panel {
  position: relative;
  flex: 1 1 46%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #eaf6f5;
  background: linear-gradient(160deg, #0a5a55 0%, #0d7e76 45%, #123a5c 100%);
}
.brand-rings {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 78% 18%, rgba(255, 255, 255, 0.14) 0 2px, transparent 3px),
    repeating-radial-gradient(circle at 78% 18%, transparent 0 38px, rgba(255, 255, 255, 0.07) 38px 39px),
    repeating-radial-gradient(circle at 12% 88%, transparent 0 30px, rgba(255, 255, 255, 0.05) 30px 31px);
}
.brand-content {
  position: relative;
  max-width: 480px;
  padding: 0 48px;
}
.brand-badge {
  display: inline-block;
  padding: 4px 12px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 999px;
  font-size: 11px;
  letter-spacing: 3px;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 24px;
}
.brand-title {
  margin: 0 0 16px;
  font-size: 34px;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: 1px;
}
.brand-sub {
  margin: 0 0 32px;
  font-size: 15px;
  line-height: 1.8;
  color: rgba(234, 246, 245, 0.82);
}
.brand-points {
  margin: 0;
  padding: 0;
  list-style: none;
}
.brand-points li {
  position: relative;
  padding: 8px 0 8px 26px;
  font-size: 14px;
  color: rgba(234, 246, 245, 0.9);
}
.brand-points li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 14px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.75);
}

/* ── 表单面 ─────────────────────────────── */
.form-panel {
  flex: 1 1 54%;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(900px 500px at 90% -10%, rgba(13, 126, 118, 0.06), transparent 60%),
    var(--yl-bg);
}
.form-box {
  width: 380px;
  padding: 40px 40px 28px;
  background: #fff;
  border: 1px solid var(--yl-line);
  border-radius: 16px;
  box-shadow: var(--yl-shadow);
}
.form-mark {
  display: flex;
  justify-content: center;
  margin-bottom: 14px;
}
.mark-ring {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 4px solid var(--yl-primary);
  box-shadow: inset 0 0 0 4px var(--el-color-primary-light-9);
}
.form-title {
  margin: 0 0 4px;
  text-align: center;
  font-size: 24px;
  font-weight: 700;
}
.form-sub {
  margin: 0 0 28px;
  text-align: center;
  font-size: 13px;
  color: var(--yl-muted);
}
.form-submit {
  margin-top: 8px;
  margin-bottom: 10px;
}
.login-button {
  width: 100%;
  font-size: 16px;
  letter-spacing: 6px;
}
.login-tip {
  margin: 4px 0 0;
  text-align: center;
  font-size: 12px;
  color: var(--yl-muted);
}

@media (max-width: 900px) {
  .brand-panel { display: none; }
  .form-panel { flex: 1 1 100%; }
}
</style>
