<template>
  <div class="listing-review">
    <div class="page-header">
      <div class="page-title">
        <div class="title-main">
          <span class="title-icon">📋</span>
          <span>上架审核</span>
        </div>
        <div class="title-sub">审核各业务线的待上架申请</div>
      </div>
      <div class="page-header__actions">
        <div class="type-filter">
          <span class="type-filter-label">类型</span>
          <el-select v-model="selectedType" placeholder="全部类型" size="default" style="width: 180px">
            <el-option label="全部" value="all" />
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </div>
        <el-button :icon="Refresh" type="primary" plain @click="reloadAll">刷新全部</el-button>
      </div>
    </div>

    <div class="summary-row">
      <div
        v-for="card in summaryCards"
        :key="card.key"
        class="summary-card"
        :class="{ 'summary-card--active': activeTab === card.key }"
        @click="goTab(card.key)"
      >
        <div class="summary-card__icon" :style="{ background: card.iconBg }">{{ card.icon }}</div>
        <div class="summary-card__meta">
          <div class="summary-card__label">{{ card.label }}</div>
          <div class="summary-card__count">
            <span class="count-num">{{ card.count }}</span>
            <span class="count-unit">待审</span>
          </div>
        </div>
      </div>
    </div>

    <div class="panel">
      <el-tabs v-model="activeTab" class="review-tabs" @tab-change="onTabChange">
        <el-tab-pane v-if="enabledTypeSet.has('property')" name="property">
          <template #label>
            <span class="tab-label">
              <span>房源</span>
              <el-badge v-if="propertyRows.length" :value="propertyRows.length" type="danger" class="tab-badge" />
            </span>
          </template>
          <el-table
            v-loading="loadingProperty"
            :data="propertyRows"
            stripe
            class="review-table"
            empty-text=" "
          >
            <template #empty>
              <el-empty description="暂无待审房源" :image-size="90" />
            </template>
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column prop="title" label="标题" min-width="160" />
            <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
            <el-table-column label="租金" width="110">
              <template #default="{ row }">
                <span class="price">¥{{ row.rentPrice }}</span>
                <span class="price-unit">/月</span>
              </template>
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

        <el-tab-pane v-if="enabledTypeSet.has('procurement')" name="procurement">
          <template #label>
            <span class="tab-label">
              <span>本地商城</span>
              <el-badge v-if="procurementRows.length" :value="procurementRows.length" type="danger" class="tab-badge" />
            </span>
          </template>
          <div class="filters">
            <el-select v-model="procurementCategory" clearable placeholder="按分类筛选" size="default" style="width: 220px">
              <el-option v-for="c in procurementCategories" :key="c" :label="c" :value="c" />
            </el-select>
          </div>
          <el-table
            v-loading="loadingProcurement"
            :data="filteredProcurementRows"
            stripe
            class="review-table"
            empty-text=" "
          >
            <template #empty>
              <el-empty description="暂无待审商品" :image-size="90" />
            </template>
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column prop="name" label="名称" min-width="160" />
            <el-table-column prop="category" label="分类" width="110">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.category || '—' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="价格" width="100">
              <template #default="{ row }">
                <span class="price">¥{{ row.price }}</span>
              </template>
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

        <el-tab-pane v-if="enabledTypeSet.has('service')" name="service">
          <template #label>
            <span class="tab-label">
              <span>服务类型</span>
              <el-badge v-if="serviceRows.length" :value="serviceRows.length" type="danger" class="tab-badge" />
            </span>
          </template>
          <div class="filters">
            <el-select v-model="serviceCategory" clearable placeholder="按分类筛选" size="default" style="width: 220px">
              <el-option v-for="c in serviceCategories" :key="c" :label="c" :value="c" />
            </el-select>
          </div>
          <el-table
            v-loading="loadingService"
            :data="filteredServiceRows"
            stripe
            class="review-table"
            empty-text=" "
          >
            <template #empty>
              <el-empty description="暂无待审服务" :image-size="90" />
            </template>
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column prop="name" label="名称" min-width="140" />
            <el-table-column prop="category" label="分类" width="110">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.category || '—' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="基础价" width="100">
              <template #default="{ row }">
                <span class="price">¥{{ row.price }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="approveService(row.id)">通过</el-button>
                <el-button type="danger" size="small" plain @click="rejectService(row.id)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane v-if="enabledTypeSet.has('secondhand')" name="secondhand">
          <template #label>
            <span class="tab-label">
              <span>跳蚤市场</span>
              <el-badge v-if="secondhandRows.length" :value="secondhandRows.length" type="danger" class="tab-badge" />
            </span>
          </template>
          <div class="filters">
            <el-select v-model="secondhandCategory" clearable placeholder="按分类筛选" size="default" style="width: 220px">
              <el-option v-for="c in secondhandCategories" :key="c" :label="c" :value="c" />
            </el-select>
          </div>
          <el-table
            v-loading="loadingSecondhand"
            :data="filteredSecondhandRows"
            stripe
            class="review-table"
            empty-text=" "
          >
            <template #empty>
              <el-empty description="暂无待审闲置" :image-size="90" />
            </template>
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column prop="title" label="标题" min-width="160" />
            <el-table-column prop="category" label="分类" width="110">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.category || '—' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="价格" width="100">
              <template #default="{ row }">
                <span class="price">¥{{ row.price }}</span>
              </template>
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
import { Refresh } from '@element-plus/icons-vue'
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
import { useAdminList } from '@/composables/useAdminList'

type Row = Record<string, unknown>
type TabKey = 'property' | 'procurement' | 'service' | 'secondhand'

const { runConfirmActionSafe } = useAdminActions()

const activeTab = ref<TabKey>('property')
const typeOptions = [
  { label: '房源', value: 'property' },
  { label: '本地商城商品', value: 'procurement' },
  { label: '服务类型', value: 'service' },
  { label: '跳蚤市场', value: 'secondhand' },
] as const
const selectedType = ref<'all' | TabKey>('all')
const enabledTypes = computed<TabKey[]>(() => {
  if (selectedType.value === 'all') {
    return typeOptions.map((item) => item.value) as TabKey[]
  }
  return [selectedType.value]
})
const enabledTypeSet = computed(() => new Set(enabledTypes.value))

function unwrapList(res: unknown): Row[] {
  const data = res as { records?: unknown[]; list?: unknown[] }
  return (data?.records || data?.list || []) as Row[]
}

async function withErrorToast<T>(promise: Promise<T>, errorMsg: string, fallback: T): Promise<T> {
  try {
    return await promise
  } catch {
    ElMessage.error(errorMsg)
    return fallback
  }
}

const {
  loading: loadingProperty,
  rows: propertyRows,
  load: loadProperties,
} = useAdminList<Row>(() =>
  withErrorToast(
    getPropertyList({ page: 1, size: 50, statuses: ['pending'] }).then(unwrapList),
    '加载待审房源失败',
    [] as Row[],
  ),
)

const procurementCategory = ref<string | undefined>()
const {
  loading: loadingProcurement,
  rows: procurementRows,
  load: loadProcurement,
} = useAdminList<Row>(async () => {
  const rows = await withErrorToast(
    getProcurementList({ page: 1, size: 50, status: '2' }).then(unwrapList),
    '加载待审商品失败',
    [] as Row[],
  )
  procurementCategory.value = undefined
  return rows
})

const serviceCategory = ref<string | undefined>()
const {
  loading: loadingService,
  rows: serviceRows,
  load: loadServices,
} = useAdminList<Row>(async () => {
  const list = await withErrorToast(getPendingServiceTypes(), '加载待审服务失败', [] as unknown as object[])
  serviceCategory.value = undefined
  return Array.isArray(list) ? (list as Row[]) : []
})

const secondhandCategory = ref<string | undefined>()
const {
  loading: loadingSecondhand,
  rows: secondhandRows,
  load: loadSecondhand,
} = useAdminList<Row>(async () => {
  const rows = await withErrorToast(
    getSecondhandList({ page: 1, size: 50, status: '2' }).then(unwrapList),
    '加载待审闲置失败',
    [] as Row[],
  )
  secondhandCategory.value = undefined
  return rows
})

function normalizeCategory(value: unknown): string | undefined {
  if (typeof value !== 'string') return undefined
  const s = value.trim()
  return s ? s : undefined
}

function uniqueCategories(rows: Row[]) {
  const set = new Set<string>()
  for (const r of rows) {
    const c = normalizeCategory(r.category)
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

const summaryCards = computed(() =>
  [
    { key: 'property' as TabKey, label: '房源', icon: '🏠', iconBg: 'linear-gradient(135deg,#667eea,#764ba2)', count: propertyRows.value.length },
    { key: 'procurement' as TabKey, label: '本地商城', icon: '🛒', iconBg: 'linear-gradient(135deg,#f6d365,#fda085)', count: procurementRows.value.length },
    { key: 'service' as TabKey, label: '服务类型', icon: '🛠️', iconBg: 'linear-gradient(135deg,#43cea2,#185a9d)', count: serviceRows.value.length },
    { key: 'secondhand' as TabKey, label: '跳蚤市场', icon: '♻️', iconBg: 'linear-gradient(135deg,#ff9a9e,#fad0c4)', count: secondhandRows.value.length },
  ].filter((c) => enabledTypeSet.value.has(c.key)),
)

function goTab(key: TabKey) {
  activeTab.value = key
  onTabChange(key)
}

const loaderByTab: Record<TabKey, () => Promise<void>> = {
  property: loadProperties,
  procurement: loadProcurement,
  service: loadServices,
  secondhand: loadSecondhand,
}

function onTabChange(name: string | number) {
  const loader = loaderByTab[String(name) as TabKey]
  if (loader) void loader()
}

function reloadAll() {
  void loadProperties()
  void loadProcurement()
  void loadServices()
  void loadSecondhand()
}

function makeApproveReject(opts: {
  approveApi: (id: number) => Promise<unknown>
  rejectApi: (id: number) => Promise<unknown>
  reload: () => Promise<void>
  approveMsg: string
  rejectMsg: string
}) {
  return {
    approve: (id: number) =>
      runConfirmActionSafe({
        confirmMessage: opts.approveMsg,
        confirmTitle: '通过',
        type: 'success',
        action: async () => {
          await opts.approveApi(id)
          await opts.reload()
        },
        successMessage: '已通过',
      }),
    reject: (id: number) =>
      runConfirmActionSafe({
        confirmMessage: opts.rejectMsg,
        confirmTitle: '驳回',
        type: 'warning',
        action: async () => {
          await opts.rejectApi(id)
          await opts.reload()
        },
        successMessage: '已驳回',
      }),
  }
}

const { approve: approveProperty, reject: rejectProperty } = makeApproveReject({
  approveApi: approvePropertyListing,
  rejectApi: rejectPropertyListing,
  reload: loadProperties,
  approveMsg: '确认通过该房源的上架申请？',
  rejectMsg: '确定驳回该房源的上架申请？',
})

const { approve: approveProcurement, reject: rejectProcurement } = makeApproveReject({
  approveApi: approveProcurementListing,
  rejectApi: rejectProcurementListing,
  reload: loadProcurement,
  approveMsg: '确认通过该商品的上架申请？',
  rejectMsg: '确定驳回该商品？',
})

const { approve: approveService, reject: rejectService } = makeApproveReject({
  approveApi: approveServiceTypeListing,
  rejectApi: rejectServiceTypeListing,
  reload: loadServices,
  approveMsg: '确认通过该服务类型的上架申请？',
  rejectMsg: '确定驳回该服务类型？',
})

const { approve: approveSecondhand, reject: rejectSecondhand } = makeApproveReject({
  approveApi: approveSecondhandListing,
  rejectApi: rejectSecondhandListing,
  reload: loadSecondhand,
  approveMsg: '确认通过该闲置商品的上架申请？',
  rejectMsg: '确定驳回该闲置商品？',
})

onMounted(() => {
  reloadAll()
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
.listing-review {
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.page-title {
  .title-main {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 22px;
    font-weight: 600;
    color: #1f2d3d;

    .title-icon {
      font-size: 24px;
    }
  }

  .title-sub {
    margin-top: 4px;
    color: #909399;
    font-size: 13px;
  }
}

.page-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.type-filter {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  padding: 4px 10px 4px 14px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.type-filter-label {
  color: #606266;
  font-size: 13px;
}

.type-filter :deep(.el-select .el-input__wrapper) {
  box-shadow: none;
  background: transparent;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 900px) {
  .summary-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  padding: 18px 20px;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  border: 1px solid transparent;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  }

  &--active {
    border-color: #2c7be5;
    box-shadow: 0 6px 22px rgba(44, 123, 229, 0.18);
  }
}

.summary-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
  flex-shrink: 0;
}

.summary-card__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.summary-card__label {
  font-size: 13px;
  color: #909399;
}

.summary-card__count {
  display: flex;
  align-items: baseline;
  gap: 6px;

  .count-num {
    font-size: 26px;
    font-weight: 700;
    color: #1f2d3d;
    line-height: 1;
  }

  .count-unit {
    font-size: 12px;
    color: #c0c4cc;
  }
}

.panel {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  padding: 8px 20px 20px;
}

.review-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.review-tabs :deep(.el-tabs__nav-wrap::after) {
  background: #ebeef5;
  height: 1px;
}

.review-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  height: 46px;
  line-height: 46px;
  padding: 0 20px;
}

.review-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 2px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-badge :deep(.el-badge__content) {
  transform: translateY(-1px);
}

.filters {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  padding: 12px 14px;
  background: #f7f8fb;
  border-radius: 10px;
}

.review-table {
  border-radius: 10px;
  overflow: hidden;
}

.review-table :deep(.el-table__header th) {
  background: #f7f8fb;
  color: #606266;
  font-weight: 600;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}

.price-unit {
  color: #c0c4cc;
  font-size: 12px;
  margin-left: 2px;
}
</style>
