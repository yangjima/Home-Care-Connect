<template>
  <div class="content">
    <AdminCrudToolbar
      v-model:keyword="keyword"
      v-model:status="statusFilter"
      create-path="/admin/services/new"
      create-text="新增服务"
      create-button-type="success"
      keyword-placeholder="搜索服务名称/描述"
      @search="load"
    />

    <div class="card">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="服务名称" min-width="180" />
        <el-table-column prop="category" label="分类" width="130" />
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

    <AdminEditDialog v-model="editOpen" title="编辑服务" :loading="saving" @confirm="saveEdit">
      <el-form :model="form" label-width="88px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.category" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
    </AdminEditDialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AdminCrudToolbar from '@/components/admin/AdminCrudToolbar.vue'
import AdminEditDialog from '@/components/admin/AdminEditDialog.vue'
import { adminStatusTagType, adminStatusText, normalizeAdminStatus } from '@/utils/adminCrud'
import { useAdminActions } from '@/composables/useAdminActions'
import { useAdminList } from '@/composables/useAdminList'
import {
  approveServiceTypeListing,
  deleteServiceType,
  getServiceTypes,
  rejectServiceTypeListing,
  submitServiceTypeListing,
  updateServiceType,
} from '@/api/service'

const { runAction, runConfirmAction } = useAdminActions()
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
  category: '',
  price: 0,
  unit: '次',
  icon: '',
  description: '',
})

function openEdit(row: Record<string, unknown>) {
  form.id = Number(row.id)
  form.name = String(row.name || '')
  form.category = String(row.category || '')
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
