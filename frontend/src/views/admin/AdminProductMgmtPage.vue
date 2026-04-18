<template>
  <div class="page">
    <AdminCrudToolbar
      v-model:keyword="keyword"
      v-model:status="statusFilter"
      create-path="/admin/products/new"
      create-text="添加商品"
      keyword-placeholder="搜索名称/标题"
      @search="reloadActive"
    />

    <div class="card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="本地商城商品" name="procurement">
          <el-table v-loading="loadingProcurement" :data="procurementRows" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="商品名称" min-width="180" />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column prop="stock" label="库存" width="90" />
            <el-table-column label="价格" width="120">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="adminStatusTagType(row.status)">{{ adminStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="290" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openProcurementEdit(row)">编辑</el-button>
                <el-button
                  v-if="normalizeAdminStatus(row.status) === '2'"
                  size="small"
                  type="success"
                  @click="approveProcurement(row)"
                >
                  通过
                </el-button>
                <el-button
                  v-if="normalizeAdminStatus(row.status) === '2'"
                  size="small"
                  type="warning"
                  @click="rejectProcurement(row)"
                >
                  驳回
                </el-button>
                <el-button size="small" type="danger" plain @click="removeProcurement(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="跳蚤市场商品" name="secondhand">
          <el-table v-loading="loadingSecondhand" :data="secondhandRows" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="商品标题" min-width="180" />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column label="价格" width="120">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="adminStatusTagType(row.status)">{{ adminStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="320" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openSecondhandEdit(row)">编辑</el-button>
                <el-button
                  v-if="normalizeAdminStatus(row.status) === '2'"
                  size="small"
                  type="success"
                  @click="approveSecondhand(row)"
                >
                  通过
                </el-button>
                <el-button
                  v-if="normalizeAdminStatus(row.status) === '2'"
                  size="small"
                  type="warning"
                  @click="rejectSecondhand(row)"
                >
                  驳回
                </el-button>
                <el-button
                  v-if="normalizeAdminStatus(row.status) === '0'"
                  size="small"
                  type="primary"
                  plain
                  @click="submitSecondhand(row)"
                >
                  重新提交
                </el-button>
                <el-button size="small" type="danger" plain @click="removeSecondhand(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <AdminEditDialog v-model="procurementEditOpen" title="编辑本地商城商品" :loading="savingProcurement" @confirm="saveProcurement">
      <el-form :model="procurementForm" label-width="90px">
        <el-form-item label="名称"><el-input v-model="procurementForm.name" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="procurementForm.category" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="procurementForm.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="procurementForm.stock" :min="0" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="procurementForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
    </AdminEditDialog>

    <AdminEditDialog v-model="secondhandEditOpen" title="编辑跳蚤市场商品" :loading="savingSecondhand" @confirm="saveSecondhand">
      <el-form :model="secondhandForm" label-width="90px">
        <el-form-item label="标题"><el-input v-model="secondhandForm.title" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="secondhandForm.category" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="secondhandForm.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="成色">
          <el-select v-model="secondhandForm.condition" style="width: 220px">
            <el-option label="几乎全新" value="like_new" />
            <el-option label="良好" value="good" />
            <el-option label="一般" value="fair" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="secondhandForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
    </AdminEditDialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminCrudToolbar from '@/components/admin/AdminCrudToolbar.vue'
import AdminEditDialog from '@/components/admin/AdminEditDialog.vue'
import { adminStatusTagType, adminStatusText, normalizeAdminStatus } from '@/utils/adminCrud'
import { useAdminActions } from '@/composables/useAdminActions'
import { useAdminList } from '@/composables/useAdminList'
import {
  approveProcurementListing,
  approveSecondhandListing,
  deleteProcurementProduct,
  deleteSecondhandItem,
  getProcurementList,
  getSecondhandList,
  rejectProcurementListing,
  rejectSecondhandListing,
  submitSecondhandListing,
  updateProcurementProduct,
  updateSecondhandItem,
} from '@/api/asset'

const { runAction, runConfirmAction } = useAdminActions()
const route = useRoute()
const router = useRouter()
const activeTab = ref('procurement')
const keyword = ref('')
const statusFilter = ref('')
const {
  loading: loadingProcurement,
  rows: procurementRows,
  load: loadProcurement,
} = useAdminList<Record<string, unknown>>(
  async ({ keyword, status }) => {
    const res = await getProcurementList({
      page: 1,
      size: 100,
      status: status || undefined,
      keyword: keyword || undefined,
    })
    const data = res as { records?: unknown[]; list?: unknown[] }
    return (data.records || data.list || []) as Record<string, unknown>[]
  },
  { keywordRef: keyword, statusRef: statusFilter },
)
const {
  loading: loadingSecondhand,
  rows: secondhandRows,
  load: loadSecondhand,
} = useAdminList<Record<string, unknown>>(
  async ({ keyword, status }) => {
    const res = await getSecondhandList({
      page: 1,
      size: 100,
      status: status || undefined,
      keyword: keyword || undefined,
    })
    const data = res as { records?: unknown[]; list?: unknown[] }
    return (data.records || data.list || []) as Record<string, unknown>[]
  },
  { keywordRef: keyword, statusRef: statusFilter },
)
const procurementEditOpen = ref(false)
const secondhandEditOpen = ref(false)
const savingProcurement = ref(false)
const savingSecondhand = ref(false)

const procurementForm = reactive({
  id: 0,
  name: '',
  category: '',
  price: 0,
  stock: 0,
  description: '',
})

const secondhandForm = reactive({
  id: 0,
  title: '',
  category: '',
  price: 0,
  condition: 'good',
  description: '',
})

function openProcurementEdit(row: Record<string, unknown>) {
  procurementForm.id = Number(row.id)
  procurementForm.name = String(row.name || '')
  procurementForm.category = String(row.category || '')
  procurementForm.price = Number(row.price || 0)
  procurementForm.stock = Number(row.stock || 0)
  procurementForm.description = String(row.description || '')
  procurementEditOpen.value = true
}

async function saveProcurement() {
  savingProcurement.value = true
  try {
    await runAction(async () => {
      await updateProcurementProduct(procurementForm.id, {
        name: procurementForm.name,
        category: procurementForm.category,
        price: procurementForm.price,
        stock: procurementForm.stock,
        description: procurementForm.description || undefined,
      })
    }, '商品已更新')
    procurementEditOpen.value = false
    await loadProcurement()
  } finally {
    savingProcurement.value = false
  }
}

function openSecondhandEdit(row: Record<string, unknown>) {
  secondhandForm.id = Number(row.id)
  secondhandForm.title = String(row.title || '')
  secondhandForm.category = String(row.category || '')
  secondhandForm.price = Number(row.price || 0)
  secondhandForm.condition = String(row.condition || 'good')
  secondhandForm.description = String(row.description || '')
  secondhandEditOpen.value = true
}

async function saveSecondhand() {
  savingSecondhand.value = true
  try {
    await runAction(async () => {
      await updateSecondhandItem(secondhandForm.id, {
        title: secondhandForm.title,
        category: secondhandForm.category,
        price: secondhandForm.price,
        condition: secondhandForm.condition,
        description: secondhandForm.description || undefined,
      })
    }, '商品已更新')
    secondhandEditOpen.value = false
    await loadSecondhand()
  } finally {
    savingSecondhand.value = false
  }
}

async function approveProcurement(row: Record<string, unknown>) {
  await runAction(async () => {
    await approveProcurementListing(Number(row.id))
  }, '已通过')
  await loadProcurement()
}

async function rejectProcurement(row: Record<string, unknown>) {
  await runConfirmAction({
    confirmMessage: '确认驳回该商品上架申请？',
    action: async () => {
      await rejectProcurementListing(Number(row.id))
    },
    successMessage: '已驳回',
  })
  await loadProcurement()
}

async function removeProcurement(row: Record<string, unknown>) {
  await runConfirmAction({
    confirmMessage: '确认删除该本地商城商品？删除后不可恢复。',
    confirmTitle: '危险操作',
    action: async () => {
      await deleteProcurementProduct(Number(row.id))
    },
    successMessage: '已删除',
  })
  await loadProcurement()
}

async function approveSecondhand(row: Record<string, unknown>) {
  await runAction(async () => {
    await approveSecondhandListing(Number(row.id))
  }, '已通过')
  await loadSecondhand()
}

async function rejectSecondhand(row: Record<string, unknown>) {
  await runConfirmAction({
    confirmMessage: '确认驳回该跳蚤市场商品？',
    action: async () => {
      await rejectSecondhandListing(Number(row.id))
    },
    successMessage: '已驳回',
  })
  await loadSecondhand()
}

async function submitSecondhand(row: Record<string, unknown>) {
  await runAction(async () => {
    await submitSecondhandListing(Number(row.id))
  }, '已重新提交审核')
  await loadSecondhand()
}

async function removeSecondhand(row: Record<string, unknown>) {
  await runConfirmAction({
    confirmMessage: '确认删除该跳蚤市场商品？删除后不可恢复。',
    confirmTitle: '危险操作',
    action: async () => {
      await deleteSecondhandItem(Number(row.id))
    },
    successMessage: '已删除',
  })
  await loadSecondhand()
}

async function reloadActive() {
  if (activeTab.value === 'procurement') {
    await loadProcurement()
    return
  }
  await loadSecondhand()
}

function syncTabFromRoute() {
  if (route.path.endsWith('/products/secondhand')) {
    activeTab.value = 'secondhand'
    return
  }
  activeTab.value = 'procurement'
}

function syncRouteFromTab() {
  const target = activeTab.value === 'secondhand' ? '/admin/products/secondhand' : '/admin/products/procurement'
  if (route.path !== target) {
    void router.replace(target)
  }
}

watch(activeTab, () => {
  syncRouteFromTab()
  void reloadActive()
})

watch(() => route.path, () => {
  syncTabFromRoute()
})

onMounted(async () => {
  syncTabFromRoute()
  syncRouteFromTab()
  await Promise.all([loadProcurement(), loadSecondhand()])
})
</script>

<style scoped lang="scss">
.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 20px;
}
</style>
