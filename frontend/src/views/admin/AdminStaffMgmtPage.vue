<template>
  <div>
    <div class="content">
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon">👥</div>
          <div class="stat-value">{{ total }}</div>
          <div class="stat-label">员工总数</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🔧</div>
          <div class="stat-value">{{ staffCount }}</div>
          <div class="stat-label">服务人员</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">📢</div>
          <div class="stat-value">{{ distributorCount }}</div>
          <div class="stat-label">分销员</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">✅</div>
          <div class="stat-value">{{ onlineCount }}</div>
          <div class="stat-label">在线</div>
        </div>
      </div>

      <div class="tabs">
        <div class="tab" :class="{ active: tab === 'all' }" @click="setTab('all')">全部员工</div>
        <div class="tab" :class="{ active: tab === 'service_staff' }" @click="setTab('service_staff')">服务人员</div>
        <div class="tab" :class="{ active: tab === 'distributor' }" @click="setTab('distributor')">分销员</div>
      </div>

      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索姓名、手机号..." clearable style="max-width: 360px" />
        <el-button type="primary" @click="load">搜索</el-button>
      </div>

      <div class="table-card">
        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column label="员工信息" min-width="200">
            <template #default="{ row }">
              <div class="staff-info">
                <div class="staff-avatar">{{ displayName(row)[0] }}</div>
                <div>
                  <div class="staff-name">{{ displayName(row) }}</div>
                  <div class="staff-phone">{{ row.phone || row.email || '—' }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="角色" width="130">
            <template #default="{ row }">
              <span class="role-tag" :class="roleClass(String(row.role))">{{ roleText(String(row.role)) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span class="status-tag" :class="row.status === 'active' ? 'status-online' : 'status-offline'">
                {{ row.status === 'active' ? '在线' : '离线' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="入职时间" width="120" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default>
              <el-button size="small" type="primary" plain>编辑</el-button>
              <el-button size="small">查看</el-button>
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
import { computed, onMounted, ref, watch } from 'vue'
import { listUsers } from '@/api/users'
import type { User } from '@/types'

const tab = ref<'all' | 'service_staff' | 'distributor'>('all')
const loading = ref(false)
const rows = ref<User[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const staffCount = computed(() => rows.value.filter((u) => u.role === 'service_staff').length)
const distributorCount = computed(() => rows.value.filter((u) => u.role === 'distributor').length)
const onlineCount = computed(() => rows.value.filter((u) => u.status === 'active').length)

function displayName(row: User) {
  return row.nickname || row.username || `用户${row.id}`
}

function roleText(r: string) {
  const m: Record<string, string> = {
    admin: '管理员',
    store_manager: '店长',
    supplier: '商家',
    service_staff: '服务人员',
    distributor: '分销员',
    tenant: '用户',
    user: '用户',
  }
  return m[r] || r
}

function roleClass(r: string) {
  if (r === 'admin' || r === 'store_manager') return 'role-admin'
  if (r === 'distributor') return 'role-distributor'
  return 'role-staff'
}

function setTab(t: typeof tab.value) {
  tab.value = t
  page.value = 1
  load()
}

watch(keyword, () => {
  page.value = 1
})

async function load() {
  loading.value = true
  try {
    const roleFilter = tab.value === 'all' ? undefined : tab.value
    const res = await listUsers({
      page: page.value,
      size: pageSize.value,
      role: roleFilter,
      keyword: keyword.value || undefined,
    })
    const data = res as { records?: User[]; list?: User[]; total?: number }
    rows.value = (data.records || data.list || []) as User[]
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
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
  text-align: center;
}
.stat-icon {
  font-size: 36px;
  margin-bottom: 10px;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
}
.stat-label {
  font-size: 14px;
  color: #999;
}
.tabs {
  background: #fff;
  border-radius: 12px;
  padding: 0 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
}
.tab {
  padding: 15px 25px;
  font-size: 15px;
  color: #666;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  transition: all 0.3s;
}
.tab:hover {
  color: #2c7be5;
}
.tab.active {
  color: #2c7be5;
  border-bottom-color: #2c7be5;
  font-weight: 600;
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
}
.table-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}
.staff-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.staff-avatar {
  width: 42px;
  height: 42px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  flex-shrink: 0;
}
.staff-name {
  font-weight: 600;
  margin-bottom: 3px;
}
.staff-phone {
  font-size: 13px;
  color: #999;
}
.role-tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
}
.role-admin {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
}
.role-staff {
  background: #e3f2fd;
  color: #2c7be5;
}
.role-distributor {
  background: #fff3e0;
  color: #ff9800;
}
.status-tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
}
.status-online {
  background: #e8f5e9;
  color: #4caf50;
}
.status-offline {
  background: #f5f5f5;
  color: #999;
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
