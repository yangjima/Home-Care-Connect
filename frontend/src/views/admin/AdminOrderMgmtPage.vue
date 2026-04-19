<template>
  <div>
    <div class="content">
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon">📋</div>
          <div class="stat-value">{{ total }}</div>
          <div class="stat-label">总订单数</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">⏳</div>
          <div class="stat-value">{{ pendingCount }}</div>
          <div class="stat-label">待处理</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🔄</div>
          <div class="stat-value">{{ inProgressCount }}</div>
          <div class="stat-label">进行中</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">💰</div>
          <div class="stat-value">¥528,420</div>
          <div class="stat-label">本月收入</div>
        </div>
      </div>

      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索订单号、用户..." clearable style="max-width: 320px" />
        <el-select v-model="status" placeholder="订单状态" clearable style="width: 140px" @change="load">
          <el-option label="全部状态" value="" />
          <el-option label="待处理" value="pending" />
          <el-option label="已派单" value="assigned" />
          <el-option label="已接单" value="accepted" />
          <el-option label="进行中" value="in_progress" />
          <el-option label="已完成" value="completed" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
        <el-button type="primary" @click="load">筛选</el-button>
      </div>

      <div class="table-card">
        <el-table v-loading="loading" :data="filteredRows" stripe>
          <el-table-column prop="orderNo" label="订单号" width="170" />
          <el-table-column label="订单类型" width="120">
            <template #default>
              <span class="order-type type-service">🛠️ 服务订单</span>
            </template>
          </el-table-column>
          <el-table-column prop="serviceTypeName" label="服务/商品" min-width="140" />
          <el-table-column prop="userId" label="用户" width="80" />
          <el-table-column label="金额" width="100">
            <template #default="{ row }">
              <span class="price">¥{{ row.totalAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <span class="status-tag" :class="orderStatusClass(row.status)">{{ orderStatusText(row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" class="btn-view" @click="openDetail(row)">查看</el-button>
              <template v-if="isPlatformAdmin(role)">
                <el-button
                  v-if="toOrderStatusKey(String(row.status)) === 'pending'"
                  size="small"
                  type="warning"
                  @click="doConfirm(row)"
                >
                  处理
                </el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <div class="pagination-info">共 {{ total }} 条</div>
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            layout="prev, pager, next"
            :total="total"
            @current-change="load"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { confirmOrder, getOrderList } from '@/api/service'
import { isPlatformAdmin } from '@/constants/roles'
import { useAuthStore } from '@/stores/auth'
import { useAdminActions } from '@/composables/useAdminActions'
import {
  orderStatusClass as commonOrderStatusClass,
  orderStatusLabelForAdmin,
  toOrderStatusKey,
} from '@/utils/status'

const authStore = useAuthStore()
const role = computed(() => authStore.userInfo?.role || '')
const { runConfirmActionSafe } = useAdminActions()

const loading = ref(false)
const rows = ref<Record<string, unknown>[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const status = ref('')
const keyword = ref('')

const pendingCount = computed(() => rows.value.filter((r) => r.status === 'pending').length)
const inProgressCount = computed(() =>
  rows.value.filter((r) => ['assigned', 'accepted', 'in_progress'].includes(String(r.status))).length,
)

const filteredRows = computed(() => {
  if (!keyword.value) return rows.value
  const k = keyword.value.trim()
  return rows.value.filter((r) => String(r.orderNo || '').includes(k) || String(r.userId || '').includes(k))
})

function orderStatusText(s: string) {
  return orderStatusLabelForAdmin(s)
}

function orderStatusClass(s: string) {
  const base = commonOrderStatusClass(s)
  if (base === 'completed') return 'status-completed'
  if (base === 'cancelled') return 'status-cancelled'
  if (base === 'confirmed') return 'status-paid'
  return 'status-pending'
}

async function load() {
  loading.value = true
  try {
    const res = await getOrderList({
      page: page.value,
      size: pageSize.value,
      status: status.value || undefined,
    })
    const data = res as { records?: unknown[]; list?: unknown[]; total?: number }
    rows.value = (data.records || data.list || []) as Record<string, unknown>[]
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

function openDetail(row: Record<string, unknown>) {
  ElMessageBox.alert(JSON.stringify(row, null, 2), `订单 ${row.orderNo}`, { customClass: 'wide-msgbox' })
}

async function doConfirm(row: Record<string, unknown>) {
  await runConfirmActionSafe({
    confirmMessage: '确认将该订单设为已派单？',
    confirmTitle: '处理订单',
    action: async () => {
      await confirmOrder(Number(row.id))
      await load()
    },
    successMessage: '已确认',
  })
}

onMounted(load)
</script>

<style scoped lang="scss">
.topbar {
  background: #fff;
  padding: 15px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}
.topbar-title {
  font-size: 20px;
  font-weight: 600;
}
.topbar-breadcrumb {
  font-size: 14px;
  color: #999;
  margin-top: 3px;
}
.content {
  padding: 25px 30px;
}
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}
@media (max-width: 900px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.stat-icon {
  font-size: 32px;
  margin-bottom: 10px;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 5px;
}
.stat-label {
  font-size: 14px;
  color: #999;
}
.toolbar {
  background: #fff;
  border-radius: 12px;
  padding: 20px 25px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}
.table-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}
.order-type {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
}
.type-service {
  background: #e8f5e9;
  color: #4caf50;
}
.price {
  font-weight: bold;
  color: #ffa500;
}
.status-tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
}
.status-pending {
  background: #fff3e0;
  color: #ff9800;
}
.status-paid {
  background: #e3f2fd;
  color: #2c7be5;
}
.status-completed {
  background: #e8f5e9;
  color: #4caf50;
}
.status-cancelled {
  background: #f5f5f5;
  color: #999;
}
.btn-view {
  color: #2c7be5;
}
.pagination {
  display: flex;
  justify-content: space-between;
  padding: 16px 20px;
}
.pagination-info {
  font-size: 14px;
  color: #999;
}
</style>
