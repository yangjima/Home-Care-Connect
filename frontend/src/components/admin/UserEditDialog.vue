<template>
  <el-dialog :model-value="modelValue" title="编辑用户" width="480px" destroy-on-close
    @update:model-value="emit('update:modelValue', $event as boolean)" @closed="resetForm">
    <div v-loading="loading" class="dialog-body">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="用户名">
          <el-input :model-value="form.username" disabled />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="form.gender" placeholder="选填" clearable style="width: 100%">
            <el-option label="男" value="male" />
            <el-option label="女" value="female" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="选择角色" style="width: 100%">
            <el-option v-for="opt in roleEditOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="正常" value="active" />
            <el-option label="已停用" value="inactive" />
            <el-option label="已封禁" value="banned" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="showStorePicker" label="所属门店">
          <el-select v-model="form.storeId" placeholder="未绑定" clearable filterable style="width: 100%">
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import {
  getUserById,
  updateUserProfile,
  updateUserRole,
  updateUserStatus,
  updateUserStore,
} from '@/api/users'
import type { Store } from '@/api/stores'
import type { User } from '@/types'
import { ROLE_ADMIN } from '@/constants/roles'
import { useAuthStore } from '@/stores/auth'
import { roleText } from '@/utils/userFormat'

type RoleOption = { value: string; label: string }

const props = defineProps<{
  modelValue: boolean
  userId: number | null
  assignableRoleOptions: RoleOption[]
  storeOptions: Store[]
  showStorePicker: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved'): void
}>()

const authStore = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const original = ref<User | null>(null)

const form = reactive({
  username: '',
  realName: '',
  gender: '' as string,
  phone: '',
  role: '',
  status: 'active',
  storeId: null as number | null,
})

const rules: FormRules = {
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const roleEditOptions = computed(() => {
  const base = props.assignableRoleOptions
  const r = form.role
  if (r && !base.some((o) => o.value === r)) {
    return [{ value: r, label: roleText(r) }, ...base]
  }
  return base
})

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    if (props.userId == null) return
    resetForm()
    loading.value = true
    try {
      const u = await getUserById(props.userId)
      original.value = u
      applyUser(u)
    } catch {
      /* interceptor already toasted */
    } finally {
      loading.value = false
    }
  },
)

function applyUser(u: User) {
  form.username = u.username || ''
  form.realName = u.realName || ''
  form.gender = u.gender || ''
  form.phone = u.phone || ''
  form.role = u.role || ''
  form.status = u.status || 'active'
  form.storeId = u.storeId ?? null
}

function resetForm() {
  original.value = null
  form.username = ''
  form.realName = ''
  form.gender = ''
  form.phone = ''
  form.role = ''
  form.status = 'active'
  form.storeId = null
  formRef.value?.clearValidate()
}

function close() {
  emit('update:modelValue', false)
}

async function submit() {
  if (!formRef.value || props.userId == null) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  const id = props.userId
  const orig = original.value
  submitting.value = true
  try {
    const profile: { realName?: string; gender?: string; phone?: string } = {}
    const rn = form.realName.trim()
    const origRn = (orig?.realName || '').trim()
    if (rn !== origRn) profile.realName = rn || undefined
    const g = form.gender || ''
    const og = orig?.gender || ''
    if (g !== og) profile.gender = g || undefined
    const ph = form.phone.trim()
    const oph = (orig?.phone || '').trim()
    if (ph !== oph) profile.phone = ph || undefined

    if (Object.keys(profile).length > 0) {
      await updateUserProfile(id, profile)
    }
    if (orig && form.role !== orig.role) {
      await updateUserRole(id, form.role)
    }
    if (orig && form.status !== orig.status) {
      await updateUserStatus(id, form.status)
    }
    if (
      authStore.userInfo?.role === ROLE_ADMIN &&
      orig &&
      (form.storeId ?? null) !== (orig.storeId ?? null)
    ) {
      await updateUserStore(id, form.storeId ?? null)
    }
    ElMessage.success('保存成功')
    emit('update:modelValue', false)
    emit('saved')
  } catch {
    /* interceptor already toasted */
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.dialog-body {
  min-height: 80px;
}
</style>
