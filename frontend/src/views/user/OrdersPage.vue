<template>
  <div class="orders-page">
    <div class="page-header">
      <h1>我的订单</h1>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="filterStatus" @change="handleStatusChange">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="pending">待处理</el-radio-button>
        <el-radio-button value="confirmed">已确认</el-radio-button>
        <el-radio-button value="completed">已完成</el-radio-button>
        <el-radio-button value="cancelled">已取消</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="order-list">
      <div v-for="order in orders" :key="order.id" class="order-card card">
        <div class="order-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <span class="order-status" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
        </div>

        <div class="order-body">
          <div class="order-service">
            <h3>{{ order.serviceTypeName || '服务项目' }}</h3>
            <p class="service-info">
              <span><el-icon><Clock /></el-icon> {{ formatDate(order.serviceTime) }}</span>
              <span><el-icon><Location /></el-icon> {{ order.serviceAddress }}</span>
            </p>
          </div>
          <div class="order-price">
            <span class="amount">¥{{ order.totalAmount }}</span>
            <span class="pay-status" :class="payStatusClass(order.payStatus)">{{ payStatusLabel(order.payStatus) }}</span>
          </div>
        </div>

        <div class="order-actions">
          <el-button v-if="order.status === 1 && order.payStatus === 0" type="primary" size="small" @click="handlePay(order.id)">
            去支付
          </el-button>
          <el-button v-if="order.status === 1" size="small" @click="handleCancel(order.id)">取消订单</el-button>
          <el-button v-if="order.status === 3" size="small" @click="handleReview(order.id)">评价</el-button>
          <el-button size="small" @click="viewDetail(order.id)">查看详情</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && orders.length === 0" description="暂无订单记录" />
    </div>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="fetchOrders"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Clock, Location } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useServiceStore } from '@/stores/service'
import type { ServiceOrder } from '@/types'

const serviceStore = useServiceStore()

const orders = ref<ServiceOrder[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref('')

const statusMap: Record<number, string> = {
  0: '待处理',
  1: '已确认',
  2: '服务中',
  3: '已完成',
  4: '已取消',
}

const payStatusMap: Record<number, string> = {
  0: '待支付',
  1: '已支付',
  2: '已退款',
}

function statusLabel(status: number): string {
  return statusMap[status] || '未知'
}

function statusClass(status: number): string {
  const map: Record<number, string> = {
    0: 'pending',
    1: 'confirmed',
    2: 'processing',
    3: 'completed',
    4: 'cancelled',
  }
  return map[status] || ''
}

function payStatusLabel(status: number): string {
  return payStatusMap[status] || '未知'
}

function payStatusClass(status: number): string {
  return status === 1 ? 'paid' : 'unpaid'
}

function formatDate(date?: string): string {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

function getStatusParam(status: string): number | undefined {
  if (status === 'pending') return 0
  if (status === 'confirmed') return 1
  if (status === 'completed') return 3
  if (status === 'cancelled') return 4
  return undefined
}

async function fetchOrders() {
  loading.value = true
  try {
    const result = await serviceStore.fetchMyOrders({
      page: page.value,
      size: pageSize.value,
    })
    orders.value = serviceStore.myOrders
    total.value = (result as any)?.total || 0
  } finally {
    loading.value = false
  }
}

function handleStatusChange() {
  page.value = 1
  fetchOrders()
}

async function handlePay(id: number) {
  try {
    await serviceStore.pay(id)
    ElMessage.success('支付成功')
    fetchOrders()
  } catch {
    ElMessage.error('支付失败，请稍后重试')
  }
}

async function handleCancel(id: number) {
  try {
    await ElMessageBox.confirm('确定要取消此订单吗？', '取消订单', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await serviceStore.cancel(id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch {
    // 用户取消
  }
}

function handleReview(id: number) {
  ElMessage.info(`评价功能开发中，订单ID: ${id}`)
}

function viewDetail(id: number) {
  ElMessage.info(`订单详情功能开发中，订单ID: ${id}`)
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped lang="scss">
.orders-page {
  .page-header h1 {
    font-size: 20px;
    font-weight: 700;
    margin-bottom: var(--spacing-lg);
  }
}

.filter-bar {
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md);
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-light);
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.order-card {
  padding: var(--spacing-lg);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--color-border-light);

  .order-no {
    font-size: 13px;
    color: var(--color-text-secondary);
  }
}

.order-status {
  font-size: 13px;
  font-weight: 500;

  &.pending { color: #e6a23c; }
  &.confirmed { color: #409eff; }
  &.processing { color: #409eff; }
  &.completed { color: #67c23a; }
  &.cancelled { color: #909399; }
}

.order-body {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-md);
}

.order-service {
  h3 {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: var(--spacing-sm);
  }

  .service-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
    color: var(--color-text-secondary);
    font-size: 13px;

    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.order-price {
  text-align: right;

  .amount {
    font-size: 20px;
    font-weight: 700;
    color: #f56c6c;
    display: block;
    margin-bottom: 4px;
  }

  .pay-status {
    font-size: 12px;

    &.paid { color: #67c23a; }
    &.unpaid { color: #e6a23c; }
  }
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-xl);
}
</style>
