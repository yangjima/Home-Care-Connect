<template>
  <div class="content">
    <AdminCrudToolbar
      v-model:keyword="keyword"
      v-model:status="statusFilter"
      create-text="新增服务"
      create-button-type="success"
      keyword-placeholder="搜索服务名称/描述"
      @search="load"
      @create="openCreate"
    />

    <div class="card">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="服务名称" min-width="180" />
        <el-table-column label="分类" width="130">
          <template #default="{ row }">{{ serviceCategoryLabel(row.category) }}</template>
        </el-table-column>
        <el-table-column label="基础价" width="110">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="adminStatusTagType(row.status)">{{ adminStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="320" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="normalizeAdminStatus(row.status) === '2'"
              size="small"
              type="success"
              @click="approveRow(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="normalizeAdminStatus(row.status) === '2'"
              size="small"
              type="warning"
              @click="rejectRow(row)"
            >
              驳回
            </el-button>
            <el-button
              v-if="normalizeAdminStatus(row.status) === '0'"
              size="small"
              type="primary"
              plain
              @click="submitRow(row)"
            >
              重新提交
            </el-button>
            <el-button size="small" type="danger" plain @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <AdminEditDialog
      v-model="createOpen"
      title="新增服务"
      width="640px"
      confirm-text="提交"
      :loading="creating"
      @confirm="saveCreate"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="96px">
        <el-form-item label="服务名称" prop="name">
          <el-input v-model="createForm.name" placeholder="例如：深度保洁" />
        </el-form-item>
        <el-form-item label="服务类型" prop="category">
          <el-select v-model="createForm.category" placeholder="请选择服务类型" style="width: 100%">
            <el-option
              v-for="opt in SERVICE_CATEGORY_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="基础价格" prop="price">
          <el-input-number v-model="createForm.price" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计价单位" prop="unit">
          <el-input v-model="createForm.unit" placeholder="次、小时等" />
        </el-form-item>
        <el-form-item label="服务图标" prop="icon">
          <ServiceIconPicker v-model="createForm.icon" />
        </el-form-item>
        <el-form-item label="服务描述" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
    </AdminEditDialog>

    <AdminEditDialog v-model="editOpen" title="编辑服务" :loading="saving" @confirm="saveEdit">
      <el-form :model="form" label-width="88px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="服务类型">
          <el-select v-model="form.category" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="opt in SERVICE_CATEGORY_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item label="服务图标">
          <ServiceIconPicker v-model="form.icon" />
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
    </AdminEditDialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import AdminCrudToolbar from '@/components/admin/AdminCrudToolbar.vue'
import AdminEditDialog from '@/components/admin/AdminEditDialog.vue'
import ServiceIconPicker from '@/components/admin/ServiceIconPicker.vue'
import { adminStatusTagType, adminStatusText, normalizeAdminStatus } from '@/utils/adminCrud'
import { useAdminActions } from '@/composables/useAdminActions'
import { useAdminList } from '@/composables/useAdminList'
import {
  approveServiceTypeListing,
  createServiceType,
  deleteServiceType,
  getServiceTypes,
  rejectServiceTypeListing,
  submitServiceTypeListing,
  updateServiceType,
} from '@/api/service'

const SERVICE_CATEGORY_OPTIONS = [
  { label: '保洁服务', value: 'cleaning' },
  { label: '维修服务', value: 'repair' },
  { label: '其他服务', value: 'other' },
] as const

function serviceCategoryLabel(cat: unknown): string {
  const s = String(cat ?? '')
  const found = SERVICE_CATEGORY_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s || '—'
}

const { runAction, runConfirmAction } = useAdminActions()
const createOpen = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  name: '',
  category: 'cleaning' as string,
  price: 0,
  unit: '次',
  icon: '',
  description: '',
})

const createRules: FormRules = {
  name: [{ required: true, message: '请输入服务名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择服务类型', trigger: 'change' }],
  price: [{ required: true, message: '请输入基础价格', trigger: 'change' }],
}

const editOpen = ref(false)
const saving = ref(false)
const {
  loading,
  rows,
  keyword,
  statusFilter,
  load,
} = useAdminList<Record<string, unknown>>(async ({ keyword, status }) => {
  const list = await getServiceTypes({
    activeOnly: false,
    keyword: keyword || undefined,
  })
  const allRows = Array.isArray(list) ? (list as Record<string, unknown>[]) : []
  if (!status) return allRows
  return allRows.filter((r) => normalizeAdminStatus(r.status) === status)
})

const form = reactive({
  id: 0,
  name: '',
  category: 'cleaning',
  price: 0,
  unit: '次',
  icon: '',
  description: '',
})

function normalizeCategory(raw: unknown): string {
  const s = String(raw ?? '').trim()
  if (SERVICE_CATEGORY_OPTIONS.some((o) => o.value === s)) return s
  return 'other'
}

function resetCreateForm() {
  createForm.name = ''
  createForm.category = 'cleaning'
  createForm.price = 0
  createForm.unit = '次'
  createForm.icon = ''
  createForm.description = ''
}

function openCreate() {
  resetCreateForm()
  createOpen.value = true
}

async function saveCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    await runAction(async () => {
      await createServiceType({
        name: createForm.name,
        category: createForm.category,
        price: Number(createForm.price),
        unit: createForm.unit || '次',
        icon: createForm.icon || undefined,
        description: createForm.description || undefined,
      })
    }, '服务创建成功')
    createOpen.value = false
    await load()
  } finally {
    creating.value = false
  }
}

function openEdit(row: Record<string, unknown>) {
  form.id = Number(row.id)
  form.name = String(row.name || '')
  form.category = normalizeCategory(row.category)
  form.price = Number(row.price || 0)
  form.unit = String(row.unit || '次')
  form.icon = String(row.icon || '')
  form.description = String(row.description || '')
  editOpen.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    await runAction(async () => {
      await updateServiceType(form.id, {
        name: form.name,
        category: form.category,
        price: form.price,
        unit: form.unit || '次',
        icon: form.icon || undefined,
        description: form.description || undefined,
      })
    }, '服务已更新')
    editOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function approveRow(row: Record<string, unknown>) {
  await runAction(async () => {
    await approveServiceTypeListing(Number(row.id))
  }, '已通过')
  await load()
}

async function rejectRow(row: Record<string, unknown>) {
  await runConfirmAction({
    confirmMessage: '确定驳回该服务类型？',
    action: async () => {
      await rejectServiceTypeListing(Number(row.id))
    },
    successMessage: '已驳回',
  })
  await load()
}

async function submitRow(row: Record<string, unknown>) {
  await runAction(async () => {
    await submitServiceTypeListing(Number(row.id))
  }, '已重新提交审核')
  await load()
}

async function removeRow(row: Record<string, unknown>) {
  await runConfirmAction({
    confirmMessage: '确认删除该服务类型？删除后不可恢复。',
    confirmTitle: '危险操作',
    action: async () => {
      await deleteServiceType(Number(row.id))
    },
    successMessage: '已删除',
  })
  await load()
}

onMounted(() => {
  void load()
})
</script>

<style scoped lang="scss">
.content {
  padding: 0;
}

.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 20px;
}
</style>
