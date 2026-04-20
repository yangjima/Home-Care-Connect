<template>
  <el-dialog :model-value="modelValue" title="添加用户" width="480px" destroy-on-close
    @update:model-value="emit('update:modelValue', $event as boolean)" @closed="resetForm">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="登录账号" autocomplete="off" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" placeholder="6–20 位" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-select v-model="form.role" placeholder="选择角色" style="width: 100%">
          <el-option v-for="opt in assignableRoleOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="姓名" prop="realName">
        <el-input v-model="form.realName" placeholder="选填" />
      </el-form-item>
      <el-form-item label="手机" prop="phone">
        <el-input v-model="form.phone" placeholder="选填" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="选填" />
      </el-form-item>
      <el-form-item v-if="showStorePicker" label="所属门店" prop="storeId">
        <el-select v-model="form.storeId" placeholder="请选择门店" filterable style="width: 100%">
          <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { createUserByAdmin } from '@/api/users'
import type { Store } from '@/api/stores'
import { encryptLoginPassword } from '@/utils/passwordCrypto'

type RoleOption = { value: string; label: string }

const props = defineProps<{
  modelValue: boolean
  assignableRoleOptions: RoleOption[]
  storeOptions: Store[]
  /** 判断当前选中角色是否需要绑定门店（由父组件根据当前操作者角色决定） */
  storePickerFor: (role: string) => boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'created'): void
}>()

const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  password: '',
  role: '',
  realName: '',
  phone: '',
  email: '',
  storeId: undefined as number | undefined,
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const showStorePicker = computed(() => props.storePickerFor(form.role))

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      resetForm()
      form.role = props.assignableRoleOptions[0]?.value || ''
    }
  },
)

function resetForm() {
  form.username = ''
  form.password = ''
  form.role = ''
  form.realName = ''
  form.phone = ''
  form.email = ''
  form.storeId = undefined
  formRef.value?.clearValidate()
}

function close() {
  emit('update:modelValue', false)
}

async function submit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (showStorePicker.value && (form.storeId == null || Number.isNaN(form.storeId))) {
    ElMessage.warning('请选择所属门店')
    return
  }
  submitting.value = true
  try {
    const enc = await encryptLoginPassword(form.password)
    await createUserByAdmin({
      username: form.username.trim(),
      password: enc,
      role: form.role,
      realName: form.realName.trim() || undefined,
      phone: form.phone.trim() || undefined,
      email: form.email.trim() || undefined,
      storeId: showStorePicker.value ? form.storeId : undefined,
    })
    ElMessage.success('创建成功')
    emit('update:modelValue', false)
    emit('created')
  } finally {
    submitting.value = false
  }
}
</script>
