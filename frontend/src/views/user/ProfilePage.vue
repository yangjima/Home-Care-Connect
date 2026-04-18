<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="profile-avatar">{{ avatarLetter }}</div>
      <div class="profile-info">
        <div class="profile-name">{{ displayName }}</div>
        <div class="profile-phone">📱 {{ maskedPhone }}</div>
        <div class="badges">
          <span v-for="b in badges" :key="b" class="profile-badge">{{ b }}</span>
        </div>
      </div>
      <div class="profile-actions">
        <router-link v-if="canAdmin" to="/admin/dashboard" class="btn-edit">🏪 系统后台</router-link>
        <el-button class="btn-edit" @click="editOpen = true">✏️ 编辑资料</el-button>
      </div>
    </div>

    <div class="user-center-nav card">
      <div class="nav-title">我的服务</div>
      <el-segmented v-model="activeKey" :options="navOptions" size="large" @change="handleNavChange" />
    </div>

    <div class="section">
      <div class="section-header">
        <h2 class="section-title">我的服务</h2>
      </div>
      <ul class="menu-list">
        <li class="menu-item" @click="router.push('/user/orders')">
          <span class="menu-icon">📦</span>
          <span class="menu-text">我的本地商城订单</span>
          <span class="menu-arrow">›</span>
        </li>
        <li class="menu-item" @click="router.push('/user/secondhand')">
          <span class="menu-icon">🔄</span>
          <span class="menu-text">我的跳蚤市场发布</span>
          <span class="menu-arrow">›</span>
        </li>
        <li class="menu-item">
          <span class="menu-icon">💳</span>
          <span class="menu-text">支付方式管理</span>
          <span class="menu-arrow">›</span>
        </li>
        <li class="menu-item">
          <span class="menu-icon">🔔</span>
          <span class="menu-text">消息通知</span>
          <span class="menu-arrow">›</span>
        </li>
        <li class="menu-item" @click="pwdOpen = true">
          <span class="menu-icon">🛡️</span>
          <span class="menu-text">账户安全（修改密码）</span>
          <span class="menu-arrow">›</span>
        </li>
        <li class="menu-item">
          <span class="menu-icon">📖</span>
          <span class="menu-text">帮助与反馈</span>
          <span class="menu-arrow">›</span>
        </li>
        <li class="menu-item">
          <span class="menu-icon">⚙️</span>
          <span class="menu-text">系统设置</span>
          <span class="menu-arrow">›</span>
        </li>
      </ul>
    </div>

    <div class="section logout-wrap">
      <el-button text type="danger" class="logout-btn" @click="doLogout">退出登录</el-button>
    </div>

    <el-dialog v-model="editOpen" title="编辑资料" width="520px" destroy-on-close @opened="syncForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <div class="avatar-row">
          <el-avatar :size="56" :src="form.avatar">{{ avatarLetter }}</el-avatar>
          <el-upload action="#" :show-file-list="false" :before-upload="handleAvatarUpload">
            <el-button size="small" :loading="avatarUploading">更换头像</el-button>
          </el-upload>
        </div>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" maxlength="20" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" maxlength="30" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" disabled />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdOpen" title="修改密码" width="480px">
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
        <el-button @click="pwdOpen = false">取消</el-button>
        <el-button type="primary" :loading="changingPwd" @click="handleChangePwd">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { updatePassword, updateUser, uploadAvatar } from '@/api/auth'
import { encryptLoginPassword } from '@/utils/passwordCrypto'
import type { User } from '@/types'
import {
  ROLE_DISTRIBUTOR,
  ROLE_MERCHANT,
  canAccessAdmin,
  isPlatformAdmin,
  roleDisplayName,
} from '@/constants/roles'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const editOpen = ref(false)
const pwdOpen = ref(false)
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
  username: [],
  email: [],
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

const displayName = computed(
  () => authStore.userInfo?.nickname || authStore.userInfo?.username || '用户',
)
const avatarLetter = computed(() => (displayName.value[0] || 'U').toUpperCase())

const maskedPhone = computed(() => {
  const p = authStore.userInfo?.phone
  if (!p || p.length < 7) return '未绑定手机'
  return `${p.slice(0, 3)}****${p.slice(-4)}`
})

const badges = computed(() => {
  const r = authStore.userInfo?.role
  const list: string[] = [roleDisplayName(r)]
  if (r === ROLE_MERCHANT) {
    list.push('认证商家')
  }
  if (isPlatformAdmin(r)) {
    list.push('平台管理')
  }
  return list
})

const canAdmin = computed(() => canAccessAdmin(authStore.userInfo?.role))
const showCommission = computed(() => authStore.userInfo?.role === ROLE_DISTRIBUTOR)
const canListProperty = computed(() => {
  const r = authStore.userInfo?.role
  return isPlatformAdmin(r) || r === ROLE_MERCHANT
})

type NavKey = 'orders' | 'viewings' | 'secondhand' | 'properties'
const activeKey = ref<NavKey>('orders')

const navOptions = computed(() => {
  const base = [
    { label: '我的订单', value: 'orders' },
    { label: '我的看房', value: 'viewings' },
    { label: '跳蚤市场', value: 'secondhand' },
  ] as Array<{ label: string; value: NavKey }>
  if (canListProperty.value) {
    base.push({ label: '我的房源', value: 'properties' })
  }
  return base
})

function syncActiveFromRoute() {
  const p = String(route.path || '')
  if (p.includes('/user/viewings')) activeKey.value = 'viewings'
  else if (p.includes('/user/secondhand')) activeKey.value = 'secondhand'
  else if (p.includes('/user/properties')) activeKey.value = 'properties'
  else activeKey.value = 'orders'
}

function handleNavChange(v: string | number | boolean) {
  const key = String(v) as NavKey
  activeKey.value = key
  router.push(`/user/${key}`)
}

onMounted(async () => {
  await authStore.fetchUserInfo()
  syncForm()
  syncActiveFromRoute()
})

function syncForm() {
  const user = authStore.userInfo
  if (user) {
    form.username = user.username || ''
    form.nickname = (user as { realName?: string }).realName || user.nickname || ''
    form.email = user.email || ''
    form.phone = user.phone || ''
    form.avatar = user.avatar || ''
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!authStore.userInfo?.id) return
  saving.value = true
  try {
    const updated = await updateUser(authStore.userInfo.id, {
      realName: form.nickname,
      phone: form.phone,
      avatar: form.avatar,
    })
    const mergedUser: User = { ...(authStore.userInfo as User), ...(updated as Partial<User>) }
    authStore.setUserInfo(mergedUser)
    ElMessage.success('保存成功')
    editOpen.value = false
  } catch {
    /* toast */
  } finally {
    saving.value = false
  }
}

async function handleChangePwd() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!authStore.userInfo?.id) return
  changingPwd.value = true
  try {
    const oldPassword = await encryptLoginPassword(pwdForm.oldPassword)
    const newPassword = await encryptLoginPassword(pwdForm.newPassword)
    await updatePassword(authStore.userInfo.id, {
      oldPassword,
      newPassword,
    })
    ElMessage.success('密码修改成功')
    pwdOpen.value = false
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } finally {
    changingPwd.value = false
  }
}

async function handleAvatarUpload(file: File) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片')
    return false
  }
  if (file.size / 1024 / 1024 >= 2) {
    ElMessage.error('图片不能超过 2MB')
    return false
  }
  avatarUploading.value = true
  try {
    const result = await uploadAvatar(file)
    form.avatar = (result as { avatar?: string })?.avatar || form.avatar
    if ((result as { user?: User })?.user) {
      authStore.setUserInfo((result as { user: User }).user)
    }
    ElMessage.success('头像已更新')
  } finally {
    avatarUploading.value = false
  }
  return false
}

async function doLogout() {
  await ElMessageBox.confirm('确定要退出登录吗？', '提示')
  authStore.logout()
  ElMessage.success('已退出')
  router.push('/home')
}
</script>

<style scoped lang="scss">
.profile-page {
  width: 100%;
}

.profile-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 35px;
  margin-bottom: 30px;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 25px;
  flex-wrap: wrap;
}

.profile-avatar {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  flex-shrink: 0;
}

.profile-info {
  flex: 1;
  min-width: 200px;
}

.profile-name {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 8px;
}

.profile-phone {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.profile-badge {
  display: inline-block;
  padding: 3px 12px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  font-size: 12px;
}

.profile-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.btn-edit {
  padding: 10px 24px;
  background: #fff;
  color: #2c7be5;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-button.btn-edit) {
  padding: 10px 24px;
  height: auto;
}

.user-center-nav {
  margin-bottom: 24px;
  padding: var(--spacing-lg);
}

.nav-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: var(--spacing-md);
  color: var(--color-text-regular);
}

/* 移动端：segmented 横向滚动，避免拥挤换行导致跳动 */
.user-center-nav {
  :deep(.el-segmented) {
    width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  :deep(.el-segmented__group) {
    flex-wrap: nowrap;
  }
}

.section {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
  overflow: hidden;
}

.section-header {
  padding: 20px 30px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
}

.menu-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  padding: 16px;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 16px 18px;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  color: #333;
  transition: all 0.3s;
  cursor: pointer;
  text-decoration: none;
  background: #fff;
}

.menu-item:hover {
  background: #f8f9fa;
  border-color: #e9ecef;
  transform: translateY(-1px);
}

.menu-icon {
  font-size: 24px;
  width: 36px;
  text-align: center;
}

.menu-text {
  flex: 1;
  font-size: 15px;
}

.menu-arrow {
  font-size: 18px;
  color: #ccc;
}

@media (min-width: 992px) {
  .menu-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
    padding: 18px;
  }
}

.logout-wrap {
  text-align: center;
  padding: 30px;
}

.logout-btn {
  font-size: 16px;
  font-weight: 500;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
</style>
