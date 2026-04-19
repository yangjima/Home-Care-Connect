<template>
  <div>
    <div class="content">
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-icon">🏠</div>
            <span class="stat-trend trend-up">↑ {{ rentTrend }}%</span>
          </div>
          <div class="stat-value">{{ formatNum(stats.rentCount) }}</div>
          <div class="stat-label">在租房源</div>
          <div class="stat-sub">含全部状态房源</div>
        </div>
        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-icon">📋</div>
            <span class="stat-trend trend-up">↑ {{ orderTrend }}%</span>
          </div>
          <div class="stat-value">{{ formatNum(stats.orderCount) }}</div>
          <div class="stat-label">总订单数</div>
          <div class="stat-sub">服务订单</div>
        </div>
        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-icon">💰</div>
            <span class="stat-trend trend-up">↑ 15.7%</span>
          </div>
          <div class="stat-value">¥528,420</div>
          <div class="stat-label">本月收入</div>
          <div class="stat-sub">日均 ¥17,614</div>
        </div>
        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-icon">👥</div>
            <span class="stat-trend trend-down">↓ 2.1%</span>
          </div>
          <div class="stat-value">{{ formatNum(stats.userCount) }}</div>
          <div class="stat-label">用户（本页样本）</div>
          <div class="stat-sub">用户列表第 1 页</div>
        </div>
      </div>

      <div class="charts-grid">
        <div class="chart-card">
          <div class="chart-header">
            <div class="chart-title">📈 收入趋势（近30天）</div>
            <div class="chart-actions">
              <span class="chart-tab active">日</span>
              <span class="chart-tab">周</span>
              <span class="chart-tab">月</span>
            </div>
          </div>
          <div class="chart-placeholder">
            <div class="chart-placeholder-icon">📊</div>
            <div class="chart-placeholder-text">收入趋势折线图（ECharts）</div>
          </div>
        </div>
        <div class="chart-card">
          <div class="chart-header">
            <div class="chart-title">🏆 分销员业绩排行</div>
            <span class="chart-tab active">本月</span>
          </div>
          <ul class="rank-list">
            <li v-for="(r, i) in rankDemo" :key="r.name" class="rank-item">
              <span class="rank-num" :class="rankClass(i)">{{ i + 1 }}</span>
              <div class="rank-avatar">{{ r.name[0] }}</div>
              <span class="rank-name">{{ r.name }}</span>
              <span class="rank-value">{{ r.amount }}</span>
            </li>
          </ul>
        </div>
      </div>

      <div class="bottom-grid">
        <div class="card">
          <div class="card-title">📌 待办事项 <span class="muted">（共4项）</span></div>
          <ul class="todo-list">
            <li class="todo-item">
              <span class="todo-icon">⚠️</span>
              <span class="todo-text">3条房源信息待审核</span>
              <span class="todo-time">今天</span>
              <span class="todo-tag tag-urgent">紧急</span>
            </li>
            <li class="todo-item">
              <span class="todo-icon">💰</span>
              <span class="todo-text">5笔佣金待结算</span>
              <span class="todo-time">今天</span>
              <span class="todo-tag tag-urgent">紧急</span>
            </li>
            <li class="todo-item">
              <span class="todo-icon">📞</span>
              <span class="todo-text">2条用户投诉待处理</span>
              <span class="todo-time">本周</span>
              <span class="todo-tag tag-normal">一般</span>
            </li>
            <li class="todo-item">
              <span class="todo-icon">📅</span>
              <span class="todo-text">下周服务排班待安排</span>
              <span class="todo-time">本周</span>
              <span class="todo-tag tag-normal">一般</span>
            </li>
          </ul>
        </div>
        <div class="card">
          <div class="card-title">⚡ 快捷操作</div>
          <div class="quick-actions">
            <router-link to="/admin/properties" class="quick-action">
              <span class="quick-action-icon">🏘️</span><span>添加房源</span>
            </router-link>
            <router-link to="/admin/orders" class="quick-action">
              <span class="quick-action-icon">📋</span><span>处理订单</span>
            </router-link>
            <router-link to="/admin/services" class="quick-action">
              <span class="quick-action-icon">🛠️</span><span>配置服务</span>
            </router-link>
            <router-link to="/admin/staff" class="quick-action">
              <span class="quick-action-icon">👥</span><span>管理用户</span>
            </router-link>
            <a href="#" class="quick-action" @click.prevent><span class="quick-action-icon">💰</span><span>发放佣金</span></a>
            <a href="#" class="quick-action" @click.prevent><span class="quick-action-icon">📊</span><span>查看报表</span></a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getPropertyList } from '@/api/property'
import { getOrderList } from '@/api/service'
import { listUsers } from '@/api/users'
import { isPlatformAdmin } from '@/constants/roles'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const stats = reactive({ rentCount: 0, orderCount: 0, userCount: 0 })
const rentTrend = ref(12.5)
const orderTrend = ref(8.3)

const rankDemo = [
  { name: '李明', amount: '¥28,450' },
  { name: '张伟', amount: '¥22,180' },
  { name: '陈静', amount: '¥19,620' },
  { name: '刘洋', amount: '¥15,300' },
  { name: '赵敏', amount: '¥12,890' },
]

function rankClass(i: number) {
  if (i === 0) return 'top1'
  if (i === 1) return 'top2'
  if (i === 2) return 'top3'
  return ''
}

function formatNum(n: number) {
  return n.toLocaleString('zh-CN')
}

onMounted(async () => {
  try {
    const role = authStore.userInfo?.role
    const ownerFilter = role === 'supplier' ? { ownerId: authStore.userInfo?.id } : {}
    const [pRes, oRes] = await Promise.all([
      getPropertyList({
        page: 1,
        size: 1,
        statuses: ['vacant', 'occupied', 'reserved'],
        ...ownerFilter,
      }),
      isPlatformAdmin(role)
        ? getOrderList({ page: 1, size: 1 })
        : Promise.resolve({ total: 0 }),
    ])
    stats.rentCount = (pRes as { total?: number }).total ?? 0
    stats.orderCount = (oRes as { total?: number }).total ?? 0
    if (isPlatformAdmin(role)) {
      const u = await listUsers({ page: 1, size: 10 })
      stats.userCount = (u as { total?: number }).total ?? 0
    }
  } catch {
    stats.rentCount = 1286
    stats.orderCount = 3542
    stats.userCount = 28
  }
})
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
.topbar-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}
.topbar-time {
  font-size: 14px;
  color: #999;
}
.content {
  padding: 25px 30px;
}
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 25px;
}
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 768px) {
  .content {
    padding: 12px;
  }
  .stats-row {
    grid-template-columns: 1fr;
    gap: 12px;
    margin-bottom: 16px;
  }
  .stat-card {
    padding: 16px;
  }
  .stat-value {
    font-size: 24px;
  }
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.stat-icon {
  font-size: 36px;
}
.stat-trend {
  font-size: 13px;
  padding: 3px 8px;
  border-radius: 4px;
}
.trend-up {
  background: #e8f5e9;
  color: #4caf50;
}
.trend-down {
  background: #ffebee;
  color: #f44336;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}
.stat-label {
  font-size: 14px;
  color: #999;
}
.stat-sub {
  font-size: 13px;
  color: #bbb;
  margin-top: 3px;
}
.charts-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 25px;
}
@media (max-width: 992px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}
.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.chart-title {
  font-size: 16px;
  font-weight: 600;
}
.chart-actions {
  display: flex;
  gap: 8px;
}
.chart-tab {
  padding: 5px 12px;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  border-radius: 4px;
}
.chart-tab.active {
  background: #f0f7ff;
  color: #2c7be5;
  font-weight: 600;
}
.chart-placeholder {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 8px;
  height: 250px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  color: #999;
}
.chart-placeholder-icon {
  font-size: 48px;
}
.rank-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.rank-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.rank-item:last-child {
  border-bottom: none;
}
.rank-num {
  width: 28px;
  height: 28px;
  background: #f0f0f0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: bold;
  margin-right: 12px;
  color: #666;
}
.rank-num.top1 {
  background: linear-gradient(135deg, #ffd700, #ffb300);
  color: #fff;
}
.rank-num.top2 {
  background: linear-gradient(135deg, #c0c0c0, #a0a0a0);
  color: #fff;
}
.rank-num.top3 {
  background: linear-gradient(135deg, #cd7f32, #b8702d);
  color: #fff;
}
.rank-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  margin-right: 10px;
}
.rank-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
}
.rank-value {
  font-size: 14px;
  font-weight: bold;
  color: #2c7be5;
}
.bottom-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}
@media (max-width: 768px) {
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.muted {
  font-size: 13px;
  color: #999;
  font-weight: normal;
}
.todo-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.todo-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.todo-item:last-child {
  border-bottom: none;
}
.todo-icon {
  font-size: 20px;
  margin-right: 12px;
}
.todo-text {
  flex: 1;
  font-size: 14px;
}
.todo-time {
  font-size: 13px;
  color: #999;
  margin-right: 10px;
}
.todo-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}
.tag-urgent {
  background: #ffebee;
  color: #f44336;
}
.tag-normal {
  background: #e3f2fd;
  color: #2c7be5;
}
.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.quick-action {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px;
  border-radius: 8px;
  background: #f8f9fa;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
  color: #333;
  text-decoration: none;
}
.quick-action:hover {
  background: #e9ecef;
  transform: translateX(5px);
}
.quick-action-icon {
  font-size: 24px;
}
</style>
