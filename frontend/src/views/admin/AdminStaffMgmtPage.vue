<template>
  <div>
    <div class="content">
      <UserStatsCard :stats="stats" :online-hint-minutes="onlineHintMinutes" />

      <div class="tabs">
        <div class="tab" :class="{ active: tab === 'all' }" @click="setTab('all')">全部用户</div>
        <div class="tab" :class="{ active: tab === 'service_staff' }" @click="setTab('service_staff')">服务人员</div>
        <div class="tab" :class="{ active: tab === 'distributor' }" @click="setTab('distributor')">分销员</div>
      </div>

      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索姓名、手机号..." clearable style="max-width: 360px" />
        <el-button type="primary" @click="load">搜索</el-button>
        <el-button type="success" @click="openAddDialog">添加用户</el-button>
      </div>

      <div class="table-card">
        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column label="用户信息" min-width="200">
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
              <span class="status-tag" :class="accountStatusClass(row.status)">
                {{ accountStatusText(row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="账号注册时间" width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="最近登录时间" width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.lastLoginAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" plain @click="openEdit(row)">编辑</el-button>
              <el-button size="small" @click="openView(row)">查看</el-button>
              <el-button
                v-if="canDeleteRow(row)"
                size="small"
                type="danger"
                plain
                @click="confirmDelete(row)"
              >
                删除
              </el-button>
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

    <AddUserDialog
      v-model="addVisible"
      :assignable-role-options="assignableRoleOptions"
      :store-options="storeOptions"
      :store-picker-for="storePickerForRole"
      @created="onUserChanged"
    />

    <UserDetailDialog v-model="viewVisible" :loading="viewLoading" :detail="viewDetail" />

    <UserEditDialog
      v-model="editVisible"
      :user-id="editId"
      :assignable-role-options="assignableRoleOptions"
      :store-options="storeOptions"
      :show-store-picker="showStorePickerEdit"
      @saved="onUserChanged"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteUser, getUserById, getUserStats, listUsers } from '@/api/users'
import { listStores, type Store } from '@/api/stores'
import { ROLE_ADMIN, ROLE_STORE_MANAGER } from '@/constants/roles'
import { useAuthStore } from '@/stores/auth'
import type { User } from '@/types'
import UserStatsCard from '@/components/admin/UserStatsCard.vue'
import AddUserDialog from '@/components/admin/AddUserDialog.vue'
import UserDetailDialog from '@/components/admin/UserDetailDialog.vue'
import UserEditDialog from '@/components/admin/UserEditDialog.vue'
import {
  accountStatusClass,
  accountStatusText,
  displayName,
  formatDateTime,
  roleClass,
  roleText,
} from '@/utils/userFormat'

const authStore = useAuthStore()
const storeOptions = ref<Store[]>([])
const onlineHintMinutes = 15

function roleNeedsStore(role: string) {
  return role === 'store_manager' || role === 'service_staff'
}

const tab = ref<'all' | 'service_staff' | 'distributor'>('all')
const loading = ref(false)
const rows = ref<User[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const stats = reactive({ total: 0, serviceStaff: 0, distributor: 0, online: 0 })

const viewVisible = ref(false)
const viewLoading = ref(false)
const viewDetail = ref<User | null>(null)

const editVisible = ref(false)
const editId = ref<number | null>(null)

const addVisible = ref(false)

const assignableRoleOptions = computed(() => {
  const r = authStore.userInfo?.role
  const opts: { value: string; label: string }[] = []
  const push = (value: string, label: string) => opts.push({ value, label })
  if (r === ROLE_ADMIN) {
    push('admin', '管理员')
    push('store_manager', '店长')
    push('supplier', '商家')
    push('service_staff', '服务人员')
    push('distributor', '分销员')
    push('user', '用户')
  } else if (r === ROLE_STORE_MANAGER) {
    push('store_manager', '店长')
    push('service_staff', '服务人员')
    push('distributor', '分销员')
    push('user', '用户')
  }
  return opts
})

function storePickerForRole(role: string) {
  return authStore.userInfo?.role === ROLE_ADMIN && roleNeedsStore(role)
}
const showStorePickerEdit = computed(() => authStore.userInfo?.role === ROLE_ADMIN)

/** 店长仅能删除一线角色账号；超级管理员可删非 admin 账号（与后端一致） */
function canDeleteRow(row: User) {
  const op = authStore.userInfo?.role
  const uid = authStore.userInfo?.id
  if (!op || row.id === uid) return false
  const tr = String(row.role || '')
  if (op === ROLE_ADMIN) {
    return tr !== 'admin'
  }
  if (op === ROLE_STORE_MANAGER) {
    return ['service_staff', 'distributor', 'tenant', 'user'].includes(tr)
  }
  return false
}

async function confirmDelete(row: User) {
  const name = displayName(row)
  try {
    await ElMessageBox.confirm(`确定删除用户「${name}」吗？此操作不可恢复。`, '删除用户', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteUser(row.id)
    ElMessage.success('已删除')
    await loadStats()
    await load()
  } catch {
    /* 拦截器已提示 */
  }
}

function setTab(t: typeof tab.value) {
  tab.value = t
  page.value = 1
  load()
}

watch(keyword, () => {
  page.value = 1
})

async function loadStats() {
  try {
    const s = await getUserStats()
    stats.total = s.total
    stats.serviceStaff = s.serviceStaff
    stats.distributor = s.distributor
    stats.online = s.online
  } catch {
    /* 拦截器已提示 */
  }
}

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
    const list = (data.records ?? data.list ?? []) as User[]
    rows.value = list
    const t = data.total
    let parsed = typeof t === 'number' && !Number.isNaN(t) ? t : list.length
    if (list.length > 0 && parsed === 0) parsed = list.length
    total.value = parsed
  } finally {
    loading.value = false
  }
}

async function openView(row: User) {
  viewVisible.value = true
  viewLoading.value = true
  viewDetail.value = null
  try {
    viewDetail.value = await getUserById(row.id)
  } catch {
    viewDetail.value = { ...row }
  } finally {
    viewLoading.value = false
  }
}

function openEdit(row: User) {
  editId.value = row.id
  editVisible.value = true
}

async function ensureStoreOptions() {
  if (authStore.userInfo?.role !== ROLE_ADMIN) return
  try {
    storeOptions.value = await listStores()
  } catch {
    storeOptions.value = []
  }
}

async function openAddDialog() {
  if (assignableRoleOptions.value.length === 0) {
    ElMessage.warning('当前账号无可分配角色')
    return
  }
  await ensureStoreOptions()
  addVisible.value = true
}

async function onUserChanged() {
  await loadStats()
  await load()
}

onMounted(async () => {
  await ensureStoreOptions()
  void loadStats()
  void load()
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
.content {
  padding: 25px 30px;
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
.status-banned {
  background: #ffebee;
  color: #e53935;
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
.table-card :deep(.el-table .el-button) {
  text-decoration: none;
}
</style>
