<template>
  <div class="login-page">
    <h2 class="title">欢迎回来</h2>
    <p class="subtitle">登录居服通，开启智慧社区生活</p>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="login-form">
      <el-form-item label="用户名或邮箱" prop="account">
        <el-input
          v-model="form.account"
          placeholder="注册邮箱，或后台创建账号的用户名"
          size="large"
          :prefix-icon="Message"
        />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          size="large"
          show-password
          :prefix-icon="Lock"
          @keyup.enter="handleLogin"
        />
      </el-form-item>

      <div class="form-options">
        <el-checkbox v-model="form.remember">记住我</el-checkbox>
        <el-link type="primary" :underline="false">忘记密码？</el-link>
      </div>

      <el-form-item>
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          class="submit-btn"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form-item>
    </el-form>

    <div class="auth-switch">
      还没有账号？
      <router-link to="/auth/register">立即注册</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Message, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  account: '',
  password: '',
  remember: false,
})

function validateAccount(_rule: unknown, value: string, callback: (e?: Error) => void) {
  const v = (value || '').trim()
  if (!v) {
    callback(new Error('请输入用户名或邮箱'))
    return
  }
  if (v.includes('@')) {
    const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)
    if (!emailOk) {
      callback(new Error('请输入有效的邮箱地址'))
      return
    }
  } else if (v.length < 2) {
    callback(new Error('用户名至少 2 个字符'))
    return
  }
  callback()
}

const rules = {
  account: [{ required: true, validator: validateAccount, trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' },
  ],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.account.trim(), form.password)
    ElMessage.success('登录成功')

    const redirect = route.query.redirect as string || '/home'
    router.push(redirect)
  } catch (e) {
    ElMessage.error('登录失败，请检查账号和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  width: 100%;
  max-width: 360px;
  margin: 0 auto;
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.subtitle {
  color: var(--color-text-secondary);
  margin-bottom: 32px;
}

.login-form {
  :deep(.el-form-item__label) {
    font-weight: 500;
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.auth-switch {
  text-align: center;
  margin-top: 24px;
  color: var(--color-text-secondary);

  a {
    color: var(--color-primary);
    font-weight: 500;
    margin-left: 4px;
  }
}
</style>
