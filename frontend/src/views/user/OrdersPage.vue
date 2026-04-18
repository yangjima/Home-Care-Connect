<template>
  <div class="orders-page">
    <UserSubpageHeader title="我的订单" />

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
          <el-tag :type="statusType(order.status)" size="small">
            {{ statusLabel(order.status) }}
          </el-tag>
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
            <el-tag :type="payStatusType(order.payStatus)" size="small">
              {{ payStatusLabel(order.payStatus) }}
            </el-tag>
          </div>
        </div>

        <div class="order-actions">
          <el-button v-if="canPay(order)" type="primary" size="small" @click="handlePay(order.id)">
            去支付
          </el-button>
          <el-button v-if="canCancel(order)" size="small" @click="handleCancel(order.id)">取消订单</el-button>
          <el-button v-if="toStatusKey(order.status) === 'completed'" size="small" @click="handleReview(order.id)">评价</el-button>
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
import UserSubpageHeader from '@/components/user/UserSubpageHeader.vue'
import { useServiceStore } from '@/stores/service'
import type { ServiceOrder } from '@/types'
import { createReview, getOrderDetail } from '@/api/service'

const serviceStore = useServiceStore()

const orders = ref<ServiceOrder[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref('')

const statusMap: Record<string, string> = {
  pending: '待处理',
  assigned: '已确认',
  accepted: '已接单',
  in_progress: '服务中',
  completed: '已完成',
  cancelled: '已取消',
}

const payStatusMap: Record<string, string> = {
  pending: '待支付',
  paid: '已支付',
  refunded: '已退款',
}

function toStatusKey(status: number | string): string {
  if (typeof status === 'number') {
    if (status === 0) return 'pending'
    if (status === 1) return 'assigned'
    if (status === 2) return 'in_progress'
    if (status === 3) return 'completed'
    if (status === 4) return 'cancelled'
  }
  return String(status || '')
}

function statusLabel(status: number | string): string {
  return statusMap[toStatusKey(status)] || '未知'
}

function statusClass(status: number | string): string {
  const map: Record<string, string> = {
    pending: 'pending',
    assigned: 'confirmed',
    accepted: 'confirmed',
    in_progress: 'processing',
    completed: 'completed',
    cancelled: 'cancelled',
  }
  return map[toStatusKey(status)] || ''
}

function statusType(status: number | string) {
  const map: Record<string, 'warning' | 'success' | 'info' | 'primary'> = {
    pending: 'warning',
    assigned: 'primary',
    accepted: 'primary',
    in_progress: 'primary',
    completed: 'success',
    cancelled: 'info',
  }
  return map[toStatusKey(status)] || 'info'
}

function toPayStatusKey(status: number | string | null): string {
  if (status === null || status === undefined || status === '') return 'pending'
  if (typeof status === 'number') {
    if (status === 1) return 'paid'
    if (status === 2) return 'refunded'
    return 'pending'
  }
  return String(status)
}

function payStatusLabel(status: number | string | null): string {
  return payStatusMap[toPayStatusKey(status)] || '待支付'
}

function payStatusClass(status: number | string | null): string {
  return toPayStatusKey(status) === 'paid' ? 'paid' : 'unpaid'
}

function payStatusType(status: number | string | null) {
  return toPayStatusKey(status) === 'paid' ? 'success' : 'warning'
}

function formatDate(date?: string): string {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

function getStatusParam(status: string): string | undefined {
  if (!status) return undefined
  if (status === 'confirmed') return 'assigned'
  if (status === 'pending' || status === 'completed' || status === 'cancelled') return status
  return undefined
}

function canPay(order: ServiceOrder): boolean {
  return toStatusKey(order.status) === 'pending'
}

function canCancel(order: ServiceOrder): boolean {
  const status = toStatusKey(order.status)
  return ['pending', 'assigned', 'accepted', 'in_progress'].includes(status)
}

async function fetchOrders() {
  loading.value = true
  try {
    const result = await serviceStore.fetchMyOrders({
      page: page.value,
      size: pageSize.value,
      status: getStatusParam(filterStatus.value),
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

async function handleReview(id: number) {
  try {
    const { value } = await ElMessageBox.prompt('请输入评价内容（可选）', '订单评价', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValue: '',
    })
    await createReview({
      orderId: id,
      rating: 5,
      content: value || '',
      isAnonymous: false,
    })
    ElMessage.success('评价提交成功')
  } catch {
    // 用户取消
  }
}

async function viewDetail(id: number) {
  try {
    const detail = await getOrderDetail(id)
    const data = detail as any
    await ElMessageBox.alert(
      [
        `订单号：${data.orderNo || '-'}`,
        `服务：${data.serviceTypeName || '-'}`,
        `时间：${formatDate(data.serviceTime)}`,
        `地址：${data.serviceAddress || '-'}`,
        `金额：¥${data.totalAmount || 0}`,
        `状态：${statusLabel(data.status)}`,
      ].join('\n'),
      '订单详情',
      { confirmButtonText: '我知道了' },
    )
  } catch {
    ElMessage.error('获取订单详情失败')
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped lang="scss">
.orders-page {
  // header is unified by UserSubpageHeader
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
  // legacy (replaced by el-tag)
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
    // legacy (replaced by el-tag)
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
