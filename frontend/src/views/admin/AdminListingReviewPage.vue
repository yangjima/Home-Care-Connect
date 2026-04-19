<template>
  <div class="listing-review">
    <div class="content">
      <div class="actions">
        <div class="type-filter">
          <span class="type-filter-label">类型筛选：</span>
          <el-select v-model="selectedType" placeholder="全部类型" style="width: 220px">
            <el-option label="全部" value="all" />
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </div>
        <el-button type="primary" plain @click="reloadAll">刷新全部</el-button>
      </div>
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane v-if="enabledTypeSet.has('property')" label="房源" name="property">
          <el-table v-loading="loadingProperty" :data="propertyRows" stripe empty-text="暂无待审房源">
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column prop="title" label="标题" min-width="160" />
            <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
            <el-table-column label="租金" width="100">
              <template #default="{ row }">¥{{ row.rentPrice }}/月</template>
            </el-table-column>
            <el-table-column prop="ownerId" label="发布者ID" width="100" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="approveProperty(row.id)">通过</el-button>
                <el-button type="danger" size="small" plain @click="rejectProperty(row.id)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane v-if="enabledTypeSet.has('procurement')" label="本地商城商品" name="procurement">
          <div class="filters">
            <el-select v-model="procurementCategory" clearable placeholder="按分类筛选（全部）" style="width: 220px">
              <el-option v-for="c in procurementCategories" :key="c" :label="c" :value="c" />
            </el-select>
          </div>
          <el-table v-loading="loadingProcurement" :data="filteredProcurementRows" stripe empty-text="暂无待审商品">
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column prop="name" label="名称" min-width="160" />
            <el-table-column prop="category" label="分类" width="100" />
            <el-table-column label="价格" width="90">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="storeId" label="商家ID" width="90" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="approveProcurement(row.id)">通过</el-button>
                <el-button type="danger" size="small" plain @click="rejectProcurement(row.id)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane v-if="enabledTypeSet.has('service')" label="服务类型" name="service">
          <div class="filters">
            <el-select v-model="serviceCategory" clearable placeholder="按分类筛选（全部）" style="width: 220px">
              <el-option v-for="c in serviceCategories" :key="c" :label="c" :value="c" />
            </el-select>
          </div>
          <el-table v-loading="loadingService" :data="filteredServiceRows" stripe empty-text="暂无待审服务">
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column prop="name" label="名称" min-width="140" />
            <el-table-column prop="category" label="分类" width="100" />
            <el-table-column label="基础价" width="100">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="approveService(row.id)">通过</el-button>
                <el-button type="danger" size="small" plain @click="rejectService(row.id)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane v-if="enabledTypeSet.has('secondhand')" label="跳蚤市场" name="secondhand">
          <div class="filters">
            <el-select v-model="secondhandCategory" clearable placeholder="按分类筛选（全部）" style="width: 220px">
              <el-option v-for="c in secondhandCategories" :key="c" :label="c" :value="c" />
            </el-select>
          </div>
          <el-table v-loading="loadingSecondhand" :data="filteredSecondhandRows" stripe empty-text="暂无待审闲置">
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column prop="title" label="标题" min-width="160" />
            <el-table-column prop="category" label="分类" width="100" />
            <el-table-column label="价格" width="90">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="userId" label="发布者ID" width="100" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="approveSecondhand(row.id)">通过</el-button>
                <el-button type="danger" size="small" plain @click="rejectSecondhand(row.id)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getPropertyList, approvePropertyListing, rejectPropertyListing } from '@/api/property'
import {
  getProcurementList,
  approveProcurementListing,
  rejectProcurementListing,
  getSecondhandList,
  approveSecondhandListing,
  rejectSecondhandListing,
} from '@/api/asset'
import { getPendingServiceTypes, approveServiceTypeListing, rejectServiceTypeListing } from '@/api/service'
import { useAdminActions } from '@/composables/useAdminActions'

const { runAction, runConfirmActionSafe } = useAdminActions()

const activeTab = ref('property')
const typeOptions = [
  { label: '房源', value: 'property' },
  { label: '本地商城商品', value: 'procurement' },
  { label: '服务类型', value: 'service' },
  { label: '跳蚤市场', value: 'secondhand' },
] as const
const selectedType = ref<'all' | 'property' | 'procurement' | 'service' | 'secondhand'>('all')
const enabledTypes = computed<string[]>(() => {
  if (selectedType.value === 'all') {
    return typeOptions.map((item) => item.value)
  }
  return [selectedType.value]
})
const enabledTypeSet = computed(() => new Set(enabledTypes.value))

const loadingProperty = ref(false)
const loadingProcurement = ref(false)
const loadingService = ref(false)
const loadingSecondhand = ref(false)

const propertyRows = ref<Record<string, unknown>[]>([])
const procurementRows = ref<Record<string, unknown>[]>([])
const serviceRows = ref<Record<string, unknown>[]>([])
const secondhandRows = ref<Record<string, unknown>[]>([])

const procurementCategory = ref<string | undefined>()
const serviceCategory = ref<string | undefined>()
const secondhandCategory = ref<string | undefined>()

function normalizeCategory(value: unknown): string | undefined {
  if (typeof value !== 'string') return undefined
  const s = value.trim()
  return s ? s : undefined
}

function uniqueCategories(rows: Record<string, unknown>[]) {
  const set = new Set<string>()
  for (const r of rows) {
    const c = normalizeCategory((r as Record<string, unknown>).category)
    if (c) set.add(c)
  }
  return Array.from(set).sort((a, b) => a.localeCompare(b, 'zh-Hans-CN'))
}

const procurementCategories = computed(() => uniqueCategories(procurementRows.value))
const serviceCategories = computed(() => uniqueCategories(serviceRows.value))
const secondhandCategories = computed(() => uniqueCategories(secondhandRows.value))

const filteredProcurementRows = computed(() => {
  const c = procurementCategory.value
  if (!c) return procurementRows.value
  return procurementRows.value.filter((r) => normalizeCategory(r.category) === c)
})

const filteredServiceRows = computed(() => {
  const c = serviceCategory.value
  if (!c) return serviceRows.value
  return serviceRows.value.filter((r) => normalizeCategory(r.category) === c)
})

const filteredSecondhandRows = computed(() => {
  const c = secondhandCategory.value
  if (!c) return secondhandRows.value
  return secondhandRows.value.filter((r) => normalizeCategory(r.category) === c)
})

async function loadProperties() {
  loadingProperty.value = true
  try {
    const res = await getPropertyList({ page: 1, size: 50, statuses: ['pending'] })
    const data = res as { records?: unknown[]; list?: unknown[] }
    propertyRows.value = (data.records || data.list || []) as Record<string, unknown>[]
  } catch {
    propertyRows.value = []
    ElMessage.error('加载待审房源失败')
  } finally {
    loadingProperty.value = false
  }
}

async function loadProcurement() {
  loadingProcurement.value = true
  try {
    const res = await getProcurementList({ page: 1, size: 50, status: '2' })
    const data = res as { records?: unknown[]; list?: unknown[] }
    procurementRows.value = (data.records || data.list || []) as Record<string, unknown>[]
    procurementCategory.value = undefined
  } catch {
    procurementRows.value = []
    ElMessage.error('加载待审商品失败')
  } finally {
    loadingProcurement.value = false
  }
}

async function loadServices() {
  loadingService.value = true
  try {
    const list = await getPendingServiceTypes()
    serviceRows.value = Array.isArray(list) ? (list as Record<string, unknown>[]) : []
    serviceCategory.value = undefined
  } catch {
    serviceRows.value = []
    ElMessage.error('加载待审服务失败')
  } finally {
    loadingService.value = false
  }
}

async function loadSecondhand() {
  loadingSecondhand.value = true
  try {
    const res = await getSecondhandList({ page: 1, size: 50, status: '2' })
    const data = res as { records?: unknown[]; list?: unknown[] }
    secondhandRows.value = (data.records || data.list || []) as Record<string, unknown>[]
    secondhandCategory.value = undefined
  } catch {
    secondhandRows.value = []
    ElMessage.error('加载待审闲置失败')
  } finally {
    loadingSecondhand.value = false
  }
}

function onTabChange(name: string | number) {
  if (name === 'property') void loadProperties()
  if (name === 'procurement') void loadProcurement()
  if (name === 'service') void loadServices()
  if (name === 'secondhand') void loadSecondhand()
}

function reloadAll() {
  void loadProperties()
  void loadProcurement()
  void loadServices()
  void loadSecondhand()
}

async function approveProperty(id: number) {
  await runConfirmActionSafe({
    confirmMessage: '确认通过该房源的上架申请？',
    confirmTitle: '通过',
    type: 'success',
    action: async () => {
      await approvePropertyListing(id)
      await loadProperties()
    },
    successMessage: '已通过',
  })
}

async function rejectProperty(id: number) {
  await runConfirmActionSafe({
    confirmMessage: '确定驳回该房源的上架申请？',
    confirmTitle: '驳回',
    type: 'warning',
    action: async () => {
      await rejectPropertyListing(id)
      await loadProperties()
    },
    successMessage: '已驳回',
  })
}

async function approveProcurement(id: number) {
  await runConfirmActionSafe({
    confirmMessage: '确认通过该商品的上架申请？',
    confirmTitle: '通过',
    type: 'success',
    action: async () => {
      await approveProcurementListing(id)
      await loadProcurement()
    },
    successMessage: '已通过',
  })
}

async function rejectProcurement(id: number) {
  await runConfirmActionSafe({
    confirmMessage: '确定驳回该商品？',
    confirmTitle: '驳回',
    type: 'warning',
    action: async () => {
      await rejectProcurementListing(id)
      await loadProcurement()
    },
    successMessage: '已驳回',
  })
}

async function approveService(id: number) {
  await runConfirmActionSafe({
    confirmMessage: '确认通过该服务类型的上架申请？',
    confirmTitle: '通过',
    type: 'success',
    action: async () => {
      await approveServiceTypeListing(id)
      await loadServices()
    },
    successMessage: '已通过',
  })
}

async function rejectService(id: number) {
  await runConfirmActionSafe({
    confirmMessage: '确定驳回该服务类型？',
    confirmTitle: '驳回',
    type: 'warning',
    action: async () => {
      await rejectServiceTypeListing(id)
      await loadServices()
    },
    successMessage: '已驳回',
  })
}

async function approveSecondhand(id: number) {
  await runConfirmActionSafe({
    confirmMessage: '确认通过该闲置商品的上架申请？',
    confirmTitle: '通过',
    type: 'success',
    action: async () => {
      await approveSecondhandListing(id)
      await loadSecondhand()
    },
    successMessage: '已通过',
  })
}

async function rejectSecondhand(id: number) {
  await runConfirmActionSafe({
    confirmMessage: '确定驳回该闲置商品？',
    confirmTitle: '驳回',
    type: 'warning',
    action: async () => {
      await rejectSecondhandListing(id)
      await loadSecondhand()
    },
    successMessage: '已驳回',
  })
}

onMounted(() => {
  void loadProperties()
  void loadProcurement()
  void loadServices()
  void loadSecondhand()
})

watch(enabledTypes, (next) => {
  if (!next.length) {
    selectedType.value = 'property'
    return
  }
  if (!next.includes(activeTab.value)) {
    activeTab.value = next[0]
  }
})
</script>

<style scoped lang="scss">
.content {
  padding: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.type-filter {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.type-filter-label {
  color: #606266;
  font-size: 14px;
}

.filters {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}
</style>
