<template>
  <div>
    <div class="content">
      <div class="top-actions">
        <el-button type="success" @click="goCreate">➕ 新增房源</el-button>
      </div>
      <div class="toolbar">
        <div class="toolbar-search">
          <el-input v-model="keyword" placeholder="🔍 搜索房源名称、地址..." clearable @keyup.enter="load" />
          <el-button type="primary" @click="load">搜索</el-button>
        </div>
        <el-select v-model="district" placeholder="全部区域" clearable style="width: 140px" @change="load">
          <el-option label="全部区域" value="" />
          <el-option v-for="d in districts" :key="d" :label="d" :value="d" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="load">
          <el-option label="全部状态" value="" />
          <el-option label="待审核" value="pending" />
          <el-option label="已驳回" value="rejected" />
          <el-option label="待出租" value="vacant" />
          <el-option label="已出租" value="occupied" />
          <el-option label="预定中" value="reserved" />
        </el-select>
      </div>

      <div class="table-card">
        <el-table v-loading="loading" :data="rows" stripe style="width: 100%">
          <el-table-column label="房源信息" min-width="220">
            <template #default="{ row }">
              <div class="property-info">
                <div class="property-thumb">{{ thumbEmoji(row) }}</div>
                <div>
                  <div class="property-name">{{ row.title }}</div>
                  <div class="property-id">#P{{ row.id }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="district" label="区域" width="100" />
          <el-table-column label="面积" width="90">
            <template #default="{ row }">{{ row.area }}㎡</template>
          </el-table-column>
          <el-table-column label="租金" width="120">
            <template #default="{ row }">
              <span class="price">¥{{ row.rentPrice }}/月</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <span class="status-tag" :class="statusClass(row.status)">{{ statusText(row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="发布者" width="100">
            <template #default="{ row }">{{ row.ownerId ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="更新时间" width="120" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button size="small" class="btn-edit" @click="goEdit(row)">编辑</el-button>
              <el-button size="small" @click="goView(row)">查看</el-button>
              <template v-if="isPlatformAdmin(role) && row.status === 'pending'">
                <el-button size="small" type="success" link @click="approveRow(row)">通过上架</el-button>
                <el-button size="small" type="danger" link @click="rejectRow(row)">驳回</el-button>
              </template>
              <template v-if="isPlatformAdmin(role)">
                <el-button size="small" link type="primary" @click="toggleRecommend(row)">
                  {{ row.isRecommended ? '取消推荐' : '推荐' }}
                </el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <div class="pagination-info">共 {{ total }} 条记录，第 {{ page }}/{{ totalPages }} 页</div>
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPropertyList, recommendProperty, approvePropertyListing, rejectPropertyListing } from '@/api/property'
import { isPlatformAdmin } from '@/constants/roles'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const role = computed(() => authStore.userInfo?.role || '')

const loading = ref(false)
const rows = ref<Record<string, unknown>[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const district = ref('')
const statusFilter = ref('')
const districts = ['朝阳区', '海淀区', '东城区', '西城区', '丰台区']

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

function statusText(s: string) {
  if (s === 'pending') return '待审核'
  if (s === 'rejected') return '已驳回'
  if (s === 'vacant') return '待出租'
  if (s === 'occupied') return '已出租'
  if (s === 'reserved') return '预定中'
  return String(s || '')
}

function statusClass(s: string) {
  if (s === 'occupied') return 'status-rented'
  if (s === 'reserved') return 'status-pending'
  return 'status-available'
}

function thumbEmoji(row: Record<string, unknown>) {
  const t = String(row.propertyType || row.type || '')
  if (t.includes('villa')) return '🏡'
  if (t.includes('studio')) return '🏠'
  return '🏠'
}

async function load() {
  loading.value = true
  try {
    const ownerId = role.value === 'supplier' ? authStore.userInfo?.id : undefined
    const statuses = statusFilter.value
      ? [statusFilter.value]
      : ['pending', 'rejected', 'vacant', 'occupied', 'reserved']
    const res = await getPropertyList({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      district: district.value || undefined,
      statuses,
      ownerId,
    })
    const data = res as { records?: unknown[]; list?: unknown[]; total?: number }
    rows.value = (data.records || data.list || []) as Record<string, unknown>[]
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

function goCreate() {
  router.push({ path: '/user/properties', query: { new: '1' } })
}

function goView(row: Record<string, unknown>) {
  router.push(`/properties/${row.id}`)
}

function goEdit(row: Record<string, unknown>) {
  router.push({ path: '/user/properties', query: { edit: String(row.id) } })
}

async function approveRow(row: Record<string, unknown>) {
  try {
    await approvePropertyListing(Number(row.id))
    ElMessage.success('已通过上架审核')
    await load()
  } catch {
    /* */
  }
}

async function rejectRow(row: Record<string, unknown>) {
  try {
    await ElMessageBox.confirm('确定驳回该房源上架申请？', '驳回', { type: 'warning' })
    await rejectPropertyListing(Number(row.id))
    ElMessage.success('已驳回')
    await load()
  } catch {
    /* */
  }
}

async function toggleRecommend(row: Record<string, unknown>) {
  try {
    const id = Number(row.id)
    const next = !row.isRecommended
    await recommendProperty(id, next)
    ElMessage.success(next ? '已推荐' : '已取消推荐')
    await load()
  } catch {
    /* request 已提示 */
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
  padding: 0;
}
.top-actions {
  margin-bottom: 16px;
}
.toolbar {
  background: #fff;
  border-radius: 12px;
  padding: 20px 25px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  gap: 15px;
  align-items: center;
  flex-wrap: wrap;
}
.toolbar-search {
  flex: 1;
  min-width: 200px;
  display: flex;
  gap: 10px;
}
.table-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}
.property-info {
  display: flex;
  align-items: center;
  gap: 15px;
}
.property-thumb {
  width: 80px;
  height: 60px;
  background: linear-gradient(135deg, #e0e0e0, #f5f5f5);
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}
.property-name {
  font-weight: 600;
  margin-bottom: 3px;
}
.property-id {
  font-size: 12px;
  color: #999;
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
.status-rented {
  background: #e8f5e9;
  color: #4caf50;
}
.status-available {
  background: #e3f2fd;
  color: #2c7be5;
}
.status-pending {
  background: #fff3e0;
  color: #ff9800;
}
.btn-edit {
  color: #2c7be5;
}
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.pagination-info {
  font-size: 14px;
  color: #999;
}
</style>
