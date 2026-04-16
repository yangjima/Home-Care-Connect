<template>
  <div class="register-page">
    <h2 class="title">创建账号</h2>
    <p class="subtitle">加入居服通，享受智慧社区服务</p>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="register-form">
      <el-form-item label="邮箱" prop="email">
        <el-input
          v-model="form.email"
          placeholder="请输入邮箱"
          size="large"
          :prefix-icon="Message"
        />
      </el-form-item>

      <el-form-item label="验证码" prop="captcha">
        <div class="captcha-row">
          <el-input
            v-model="form.captcha"
            placeholder="请输入验证码"
            size="large"
            :prefix-icon="CircleCheck"
            style="flex: 1"
          />
          <el-button
            size="large"
            :disabled="captchaCooldown > 0"
            class="captcha-btn"
            @click="sendCaptcha"
          >
            {{ captchaCooldown > 0 ? `${captchaCooldown}s` : '发送验证码' }}
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请设置密码（至少 8 位，含大小写字母和数字）"
          size="large"
          show-password
          :prefix-icon="Lock"
        />
        <div class="password-strength">
          <div class="strength-bar" :style="{ width: passwordStrength.width }" :class="passwordStrength.class"></div>
        </div>
        <div class="strength-text" :class="passwordStrength.class">
          {{ passwordStrength.text }}
        </div>
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="请再次输入密码"
          size="large"
          show-password
          :prefix-icon="Lock"
        />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          class="submit-btn"
          @click="handleRegister"
        >
          立即注册
        </el-button>
      </el-form-item>
    </el-form>

    <div class="auth-switch">
      已有账号？
      <router-link to="/auth/login">立即登录</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Message, Lock, CircleCheck } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { sendCaptcha as apiSendCaptcha } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const loading = ref(false)
const captchaCooldown = ref(0)

const form = reactive({
  email: '',
  captcha: '',
  password: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' },
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码为 4 位数字', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const passwordStrength = computed(() => {
  const p = form.password
  if (!p) return { width: '0%', class: '', text: '' }

  let score = 0
  if (p.length >= 8) score++
  if (/[a-z]/.test(p)) score++
  if (/[A-Z]/.test(p)) score++
  if (/\d/.test(p)) score++
  if (/[!@#$%^&*(),.?":{}|<>]/.test(p)) score++

  if (score <= 2) return { width: '33%', class: 'weak', text: '密码强度：弱' }
  if (score <= 3) return { width: '66%', class: 'medium', text: '密码强度：中等' }
  return { width: '100%', class: 'strong', text: '密码强度：强' }
})

async function sendCaptcha() {
  if (!form.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  if (!/^\S+@\S+\.\S+$/.test(form.email)) {
    ElMessage.warning('请输入有效的邮箱地址')
    return
  }

  await apiSendCaptcha(form.email)
  ElMessage.success('验证码已发送')

  captchaCooldown.value = 60
  const timer = setInterval(() => {
    captchaCooldown.value--
    if (captchaCooldown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.register(form.email, form.password, form.confirmPassword)
    ElMessage.success('注册成功，欢迎加入居服通！')
    router.push('/home')
  } catch (e) {
    ElMessage.error('注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.register-page {
  width: 100%;
  max-width: 400px;
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

.captcha-row {
  display: flex;
  gap: 8px;
}

.captcha-btn {
  width: 120px;
  flex-shrink: 0;
}

.password-strength {
  height: 3px;
  background: var(--border-color-light);
  border-radius: 2px;
  margin-top: 6px;
  overflow: hidden;
}

.strength-bar {
  height: 100%;
  transition: all 0.3s;
  border-radius: 2px;

  &.weak { background: var(--color-danger); }
  &.medium { background: var(--color-warning); }
  &.strong { background: var(--color-success); }
}

.strength-text {
  font-size: 12px;
  margin-top: 4px;

  &.weak { color: var(--color-danger); }
  &.medium { color: var(--color-warning); }
  &.strong { color: var(--color-success); }
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
