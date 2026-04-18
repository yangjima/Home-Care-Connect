<template>
  <div class="admin-back-entry">
    <div class="inner">
      <div class="left">
        <el-button text @click="goProfile">返回个人中心</el-button>
        <el-divider direction="vertical" />
        <el-button text @click="pwdOpen = true">修改密码</el-button>
        <el-divider direction="vertical" />
        <el-button v-if="showBackToDashboard" text @click="goDashboard">返回数据看板</el-button>
      </div>
    </div>

    <el-dialog v-model="pwdOpen" title="修改密码" width="480px" destroy-on-close>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="96px">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closePwdDialog">取消</el-button>
        <el-button type="primary" :loading="changingPwd" @click="handleChangePwd">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { updatePassword } from '@/api/auth'
import { encryptLoginPassword } from '@/utils/passwordCrypto'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const showBackToDashboard = computed(() => String(route.path || '') !== '/admin/dashboard')
const pwdOpen = ref(false)
const changingPwd = ref(false)
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirm = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

function goProfile() {
  router.push('/user/profile')
}

function goDashboard() {
  router.push('/admin/dashboard')
}

function closePwdDialog() {
  pwdOpen.value = false
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
}

async function handleChangePwd() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!authStore.userInfo?.id) {
    ElMessage.error('未获取到用户信息，请刷新后重试')
    return
  }

  changingPwd.value = true
  try {
    const oldPassword = await encryptLoginPassword(pwdForm.oldPassword)
    const newPassword = await encryptLoginPassword(pwdForm.newPassword)
    await updatePassword(authStore.userInfo.id, { oldPassword, newPassword })
    ElMessage.success('密码修改成功')
    closePwdDialog()
  } catch (error) {
    const message = (error as { message?: string })?.message
    if (message) {
      ElMessage.error(message)
    }
  } finally {
    changingPwd.value = false
  }
}
</script>

<style scoped lang="scss">
.admin-back-entry {
  padding: 14px 24px 0;
}

.inner {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 10px 12px;
}

.left {
  display: flex;
  align-items: center;
}
</style>

