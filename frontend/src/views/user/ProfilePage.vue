<template>
  <div class="profile-page">
    <div class="page-header">
      <h1>个人资料</h1>
    </div>

    <div class="profile-content card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <div class="avatar-section">
          <div class="avatar-preview">
            <img v-if="form.avatar" :src="form.avatar" alt="头像" />
            <el-icon v-else :size="48"><UserFilled /></el-icon>
          </div>
          <el-upload action="#" :show-file-list="false" :before-upload="handleAvatarUpload">
            <el-button size="small" :loading="avatarUploading">更换头像</el-button>
          </el-upload>
        </div>

        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" maxlength="20" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="30" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" disabled />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card mt-lg">
      <h3>修改密码</h3>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px" class="mt-md">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="changingPwd" @click="handleChangePwd">修改密码</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { FormInstance, FormRules } from 'element-plus'
import { updatePassword, updateUser, uploadAvatar } from '@/api/auth'
import type { User } from '@/types'

const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const pwdFormRef = ref<FormInstance>()
const saving = ref(false)
const changingPwd = ref(false)
const avatarUploading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
}

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

onMounted(async () => {
  await authStore.fetchUserInfo()
  handleReset()
})

function handleReset() {
  const user = authStore.userInfo
  if (user) {
    form.username = user.username || ''
    form.nickname = (user as any).realName || user.nickname || ''
    form.email = user.email || ''
    form.phone = user.phone || ''
    form.avatar = user.avatar || ''
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!authStore.userInfo?.id) {
    ElMessage.error('未获取到用户信息，请重新登录后再试')
    return
  }

  saving.value = true
  try {
    const updated = await updateUser(authStore.userInfo.id, {
      username: form.username,
      realName: form.nickname,
      email: form.email,
      phone: form.phone,
      avatar: form.avatar,
    })
    const mergedUser: User = {
      ...(authStore.userInfo as User),
      ...(updated as Partial<User>),
      username: (updated as Partial<User>)?.username || form.username,
      email: (updated as Partial<User>)?.email || form.email,
      phone: (updated as Partial<User>)?.phone || form.phone,
      avatar: (updated as Partial<User>)?.avatar || form.avatar,
      nickname: (updated as any)?.realName || form.nickname,
    }
    authStore.setUserInfo(mergedUser)
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function handleChangePwd() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!authStore.userInfo?.id) {
    ElMessage.error('未获取到用户信息，请重新登录后再试')
    return
  }

  changingPwd.value = true
  try {
    await updatePassword(authStore.userInfo.id, {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch {
    ElMessage.error('密码修改失败，请稍后重试')
  } finally {
    changingPwd.value = false
  }
}

async function handleAvatarUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }

  avatarUploading.value = true
  try {
    const result = await uploadAvatar(file)
    form.avatar = (result as any)?.avatar || ''

    if ((result as any)?.user) {
      authStore.setUserInfo((result as any).user as User)
    } else if (authStore.userInfo) {
      authStore.setUserInfo({
        ...(authStore.userInfo as User),
        avatar: form.avatar,
      })
    }
    ElMessage.success('头像上传成功')
  } catch {
    ElMessage.error('头像上传失败，请稍后重试')
  } finally {
    avatarUploading.value = false
  }
  return false
}
</script>

<style scoped lang="scss">
.profile-page {
  .page-header h1 {
    font-size: 20px;
    font-weight: 700;
    margin-bottom: var(--spacing-lg);
  }
}

.profile-content {
  padding: var(--spacing-xl);
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-light);
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--color-bg-page);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: var(--color-text-secondary);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

h3 {
  font-size: 16px;
  font-weight: 600;
}
</style>
