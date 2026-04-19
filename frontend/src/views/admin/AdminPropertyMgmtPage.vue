<template>
  <div>
    <div class="content">
      <PropertyFormDialog
        v-model:visible="propertyFormVisible"
        :property-id="formPropertyId"
        @saved="load"
      />
      <el-dialog v-model="viewDialogVisible" title="房源详情" width="720px" destroy-on-close @closed="onViewClosed">
        <div v-loading="viewLoading" class="view-dialog-body">
          <template v-if="viewDetail">
            <div v-if="viewCover" class="view-cover">
              <img :src="viewCover" alt="">
            </div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="标题" :span="2">{{ viewDetail.title }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ statusText(String(viewDetail.status || '')) }}</el-descriptions-item>
              <el-descriptions-item label="区域">{{ viewDetail.district || '—' }}</el-descriptions-item>
              <el-descriptions-item label="租金">¥{{ viewDetail.rentPrice }}/月</el-descriptions-item>
              <el-descriptions-item label="面积">{{ viewDetail.area }}㎡</el-descriptions-item>
              <el-descriptions-item label="房型">{{ viewDetail.propertyType || '—' }}</el-descriptions-item>
              <el-descriptions-item label="户型">{{ viewDetail.layout || '—' }}</el-descriptions-item>
              <el-descriptions-item label="楼层">
                {{ viewDetail.floor ?? '—' }} / {{ viewDetail.totalFloor ?? '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="地址" :span="2">{{ viewDetail.address || '—' }}</el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">
                <div class="view-desc">{{ viewDetail.description || '—' }}</div>
              </el-descriptions-item>
            </el-descriptions>
            <div v-if="viewGallery.length" class="view-gallery">
              <div v-for="(u, i) in viewGallery" :key="`${u}-${i}`" class="view-gallery-item">
                <img :src="u" alt="">
              </div>
            </div>
          </template>
        </div>
      </el-dialog>

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
          <el-table-column label="操作" width="320" fixed="right">
            <template #default="{ row }">
              <el-button size="small" class="btn-edit" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" @click="openView(row)">查看</el-button>
              <el-button size="small" type="danger" plain @click="deleteRow(row)">删除</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import PropertyFormDialog from '@/components/property/PropertyFormDialog.vue'
import {
  getPropertyList,
  getPropertyDetail,
  deletePropertyById,
  recommendProperty,
  approvePropertyListing,
  rejectPropertyListing,
} from '@/api/property'
import { isPlatformAdmin } from '@/constants/roles'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const role = computed(() => authStore.userInfo?.role || '')

const propertyFormVisible = ref(false)
const formPropertyId = ref<number | null>(null)
const viewDialogVisible = ref(false)
const viewLoading = ref(false)
const viewDetail = ref<Record<string, unknown> | null>(null)

const viewCover = computed(() => {
  const d = viewDetail.value
  if (!d) return ''
  const cover = d.coverImage as string | undefined
  const imgs = d.images as string[] | undefined
  return cover || imgs?.[0] || ''
})

const viewGallery = computed(() => {
  const d = viewDetail.value
  if (!d) return [] as string[]
  const imgs = (d.images as string[] | undefined) || []
  return imgs.length ? imgs : viewCover.value ? [viewCover.value] : []
})

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
  formPropertyId.value = null
  propertyFormVisible.value = true
}

async function openView(row: Record<string, unknown>) {
  viewDialogVisible.value = true
  viewDetail.value = null
  viewLoading.value = true
  try {
    const raw = await getPropertyDetail(Number(row.id))
    viewDetail.value = raw as Record<string, unknown>
  } catch {
    viewDialogVisible.value = false
  } finally {
    viewLoading.value = false
  }
}

function openEdit(row: Record<string, unknown>) {
  formPropertyId.value = Number(row.id)
  propertyFormVisible.value = true
}

function onViewClosed() {
  viewDetail.value = null
}

async function deleteRow(row: Record<string, unknown>) {
  try {
    await ElMessageBox.confirm('确定删除该房源？此操作不可恢复。', '删除房源', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deletePropertyById(Number(row.id))
    ElMessage.success('已删除')
    await load()
  } catch {
    /* 取消或失败 */
  }
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
.view-dialog-body {
  min-height: 120px;
}
.view-cover {
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
  max-height: 220px;
  background: #f3f4f6;
  img {
    width: 100%;
    max-height: 220px;
    object-fit: cover;
    display: block;
  }
}
.view-desc {
  white-space: pre-wrap;
  line-height: 1.5;
  max-height: 160px;
  overflow-y: auto;
}
.view-gallery {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.view-gallery-item {
  width: 88px;
  height: 66px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}
</style>
