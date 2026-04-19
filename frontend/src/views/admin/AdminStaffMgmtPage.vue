<template>
  <div>
    <div class="content">
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon">👥</div>
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">用户总数</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🔧</div>
          <div class="stat-value">{{ stats.serviceStaff }}</div>
          <div class="stat-label">服务人员</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">📢</div>
          <div class="stat-value">{{ stats.distributor }}</div>
          <div class="stat-label">分销员</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">✅</div>
          <div class="stat-value">{{ stats.online }}</div>
          <div class="stat-label">在线</div>
          <div class="stat-hint">最近 {{ onlineHintMinutes }} 分钟内有登录</div>
        </div>
      </div>

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

    <el-dialog v-model="addVisible" title="添加用户" width="480px" destroy-on-close @closed="resetAddForm">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="88px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addForm.username" placeholder="登录账号" autocomplete="off" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="addForm.password" type="password" placeholder="6–20 位" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="addForm.role" placeholder="选择角色" style="width: 100%">
            <el-option v-for="opt in assignableRoleOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="addForm.realName" placeholder="选填" />
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="addForm.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="addForm.email" placeholder="选填" />
        </el-form-item>
        <el-form-item v-if="showStorePickerAdd" label="所属门店" prop="storeId">
          <el-select v-model="addForm.storeId" placeholder="请选择门店" filterable style="width: 100%">
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addSubmitting" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewVisible" title="用户详情" width="560px" destroy-on-close @closed="viewDetail = null">
      <div v-loading="viewLoading" class="dialog-body">
        <template v-if="viewDetail">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{ viewDetail.username || '—' }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ viewDetail.realName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ genderText(viewDetail.gender) }}</el-descriptions-item>
            <el-descriptions-item label="手机">{{ viewDetail.phone || '—' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ viewDetail.email || '—' }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ roleText(String(viewDetail.role || '')) }}</el-descriptions-item>
            <el-descriptions-item label="门店">
              {{ viewDetail.storeName || (viewDetail.storeId != null ? `#${viewDetail.storeId}` : '—') }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">{{ accountStatusText(viewDetail.status) }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ formatDateTime(viewDetail.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="最近登录">{{ formatDateTime(viewDetail.lastLoginAt) }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑用户" width="480px" destroy-on-close @closed="resetEditForm">
      <div v-loading="editLoading" class="dialog-body">
        <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="88px">
          <el-form-item label="用户名">
            <el-input :model-value="editForm.username" disabled />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="editForm.realName" placeholder="真实姓名" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-select v-model="editForm.gender" placeholder="选填" clearable style="width: 100%">
              <el-option label="男" value="male" />
              <el-option label="女" value="female" />
            </el-select>
          </el-form-item>
          <el-form-item label="手机" prop="phone">
            <el-input v-model="editForm.phone" placeholder="选填" />
          </el-form-item>
          <el-form-item label="角色" prop="role">
            <el-select v-model="editForm.role" placeholder="选择角色" style="width: 100%">
              <el-option v-for="opt in roleEditOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="editForm.status" style="width: 100%">
              <el-option label="正常" value="active" />
              <el-option label="已停用" value="inactive" />
              <el-option label="已封禁" value="banned" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="showStorePickerEdit" label="所属门店">
            <el-select v-model="editForm.storeId" placeholder="未绑定" clearable filterable style="width: 100%">
              <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createUserByAdmin,
  deleteUser,
  getUserById,
  getUserStats,
  listUsers,
  updateUserProfile,
  updateUserRole,
  updateUserStatus,
  updateUserStore,
} from '@/api/users'
import { listStores, type Store } from '@/api/stores'
import {
  ROLE_ADMIN,
  ROLE_STORE_MANAGER,
} from '@/constants/roles'
import { useAuthStore } from '@/stores/auth'
import { encryptLoginPassword } from '@/utils/passwordCrypto'
import type { User } from '@/types'

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
const editLoading = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref<FormInstance>()
const editId = ref<number | null>(null)
const editOriginal = ref<User | null>(null)
const editForm = reactive({
  username: '',
  realName: '',
  gender: '' as string,
  phone: '',
  role: '',
  status: 'active',
  storeId: null as number | null,
})

const addVisible = ref(false)
const addSubmitting = ref(false)
const addFormRef = ref<FormInstance>()
const addForm = reactive({
  username: '',
  password: '',
  role: '',
  realName: '',
  phone: '',
  email: '',
  storeId: undefined as number | undefined,
})

const showStorePickerAdd = computed(
  () => authStore.userInfo?.role === ROLE_ADMIN && roleNeedsStore(addForm.role),
)
const showStorePickerEdit = computed(() => authStore.userInfo?.role === ROLE_ADMIN)

const addRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

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

const roleEditOptions = computed(() => {
  const base = assignableRoleOptions.value
  const r = editForm.role
  if (r && !base.some((o) => o.value === r)) {
    return [{ value: r, label: roleText(r) }, ...base]
  }
  return base
})

const editRules: FormRules = {
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

function genderText(g?: string | null) {
  if (g === 'male') return '男'
  if (g === 'female') return '女'
  return '—'
}

function displayName(row: User) {
  return row.nickname || row.realName || row.username || `用户${row.id}`
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

function accountStatusText(s?: string) {
  if (s === 'active') return '正常'
  if (s === 'inactive') return '已停用'
  if (s === 'banned') return '已封禁'
  return s || '—'
}

function accountStatusClass(s?: string) {
  if (s === 'active') return 'status-online'
  if (s === 'inactive') return 'status-offline'
  if (s === 'banned') return 'status-banned'
  return 'status-offline'
}

function formatDateTime(s?: string | (number | string)[] | null) {
  if (s == null || s === '') return '—'
  if (Array.isArray(s) && s.length >= 3) {
    const y = s[0]
    const mo = String(s[1]).padStart(2, '0')
    const d = String(s[2]).padStart(2, '0')
    const h = s.length > 3 ? String(s[3] ?? 0).padStart(2, '0') : '00'
    const mi = s.length > 4 ? String(s[4] ?? 0).padStart(2, '0') : '00'
    const se = s.length > 5 ? String(s[5] ?? 0).padStart(2, '0') : '00'
    return `${y}-${mo}-${d} ${h}:${mi}:${se}`
  }
  if (typeof s === 'string') {
    return s.replace('T', ' ').slice(0, 19)
  }
  return '—'
}

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
    /* ElMessage 已在拦截器提示 */
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

function resetEditForm() {
  editId.value = null
  editOriginal.value = null
  editForm.username = ''
  editForm.realName = ''
  editForm.gender = ''
  editForm.phone = ''
  editForm.role = ''
  editForm.status = 'active'
  editForm.storeId = null
  editFormRef.value?.clearValidate()
}

async function openEdit(row: User) {
  editVisible.value = true
  editLoading.value = true
  resetEditForm()
  await ensureStoreOptions()
  try {
    const u = await getUserById(row.id)
    editId.value = u.id
    editOriginal.value = u
    editForm.username = u.username || ''
    editForm.realName = u.realName || ''
    editForm.gender = u.gender || ''
    editForm.phone = u.phone || ''
    editForm.role = u.role || ''
    editForm.status = u.status || 'active'
    editForm.storeId = u.storeId ?? null
  } catch {
    editId.value = row.id
    editOriginal.value = { ...row }
    editForm.username = row.username || ''
    editForm.realName = row.realName || ''
    editForm.gender = row.gender || ''
    editForm.phone = row.phone || ''
    editForm.role = row.role || ''
    editForm.status = row.status || 'active'
    editForm.storeId = row.storeId ?? null
  } finally {
    editLoading.value = false
  }
}

async function submitEdit() {
  if (!editFormRef.value || editId.value == null) return
  try {
    await editFormRef.value.validate()
  } catch {
    return
  }
  const id = editId.value
  const orig = editOriginal.value
  editSubmitting.value = true
  try {
    const profile: { realName?: string; gender?: string; phone?: string } = {}
    const rn = editForm.realName.trim()
    const origRn = (orig?.realName || '').trim()
    if (rn !== origRn) profile.realName = rn || undefined
    const g = editForm.gender || ''
    const og = orig?.gender || ''
    if (g !== og) profile.gender = g || undefined
    const ph = editForm.phone.trim()
    const oph = (orig?.phone || '').trim()
    if (ph !== oph) profile.phone = ph || undefined

    if (Object.keys(profile).length > 0) {
      await updateUserProfile(id, profile)
    }
    if (orig && editForm.role !== orig.role) {
      await updateUserRole(id, editForm.role)
    }
    if (orig && editForm.status !== orig.status) {
      await updateUserStatus(id, editForm.status)
    }
    if (
      authStore.userInfo?.role === ROLE_ADMIN &&
      orig &&
      (editForm.storeId ?? null) !== (orig.storeId ?? null)
    ) {
      await updateUserStore(id, editForm.storeId ?? null)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    await loadStats()
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    editSubmitting.value = false
  }
}

async function openAddDialog() {
  if (assignableRoleOptions.value.length === 0) {
    ElMessage.warning('当前账号无可分配角色')
    return
  }
  resetAddForm()
  await ensureStoreOptions()
  addForm.role = assignableRoleOptions.value[0]?.value || ''
  addVisible.value = true
}

function resetAddForm() {
  addForm.username = ''
  addForm.password = ''
  addForm.role = ''
  addForm.realName = ''
  addForm.phone = ''
  addForm.email = ''
  addForm.storeId = undefined
  addFormRef.value?.clearValidate()
}

async function ensureStoreOptions() {
  if (authStore.userInfo?.role !== ROLE_ADMIN) return
  try {
    storeOptions.value = await listStores()
  } catch {
    storeOptions.value = []
  }
}

async function submitAdd() {
  if (!addFormRef.value) return
  try {
    await addFormRef.value.validate()
  } catch {
    return
  }
  if (showStorePickerAdd.value && (addForm.storeId == null || Number.isNaN(addForm.storeId))) {
    ElMessage.warning('请选择所属门店')
    return
  }
  addSubmitting.value = true
  try {
    const enc = await encryptLoginPassword(addForm.password)
    await createUserByAdmin({
      username: addForm.username.trim(),
      password: enc,
      role: addForm.role,
      realName: addForm.realName.trim() || undefined,
      phone: addForm.phone.trim() || undefined,
      email: addForm.email.trim() || undefined,
      storeId: showStorePickerAdd.value ? addForm.storeId : undefined,
    })
    ElMessage.success('创建成功')
    addVisible.value = false
    await loadStats()
    await load()
  } finally {
    addSubmitting.value = false
  }
}

onMounted(() => {
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
.stat-hint {
  font-size: 12px;
  color: #bbb;
  margin-top: 6px;
  line-height: 1.3;
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
.dialog-body {
  min-height: 80px;
}
.table-card :deep(.el-table .el-button) {
  text-decoration: none;
}
</style>
