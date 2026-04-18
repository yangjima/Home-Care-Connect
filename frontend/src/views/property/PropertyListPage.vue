<template>
  <div class="property-list-page">
    <AppHeader />

    <div class="main-content">
      <!-- 左侧筛选栏（对齐原型 02-property-list.html） -->
      <aside class="filter-sidebar">
        <div class="filter-title">
          筛选条件
          <span class="filter-reset" role="button" tabindex="0" @click="resetFilters">重置</span>
        </div>

        <div class="filter-section">
          <div class="filter-label">房型</div>
          <div class="checkbox-group">
            <label
              v-for="opt in roomTypeOptions"
              :key="opt.value"
              class="checkbox-item"
            >
              <input
                v-model="selectedTypes"
                type="checkbox"
                :value="opt.value"
                @change="fetchList"
              >
              {{ opt.label }}
            </label>
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-label">价格区间</div>
          <div class="price-range">
            <div
              v-for="p in pricePresets"
              :key="p.id"
              class="price-option"
              :class="{ active: activePriceId === p.id && !useCustomPrice }"
              role="button"
              tabindex="0"
              @click="selectPricePreset(p.id)"
            >
              {{ p.label }}
            </div>
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-label">房源状态</div>
          <div class="checkbox-group">
            <label v-for="opt in statusOptions" :key="opt.value" class="checkbox-item">
              <input
                v-model="selectedStatuses"
                type="checkbox"
                :value="opt.value"
                @change="fetchList"
              >
              {{ opt.label }}
            </label>
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-label">配套设施</div>
          <div class="checkbox-group">
            <label v-for="f in facilityOptions" :key="f" class="checkbox-item">
              <input
                v-model="selectedFacilities"
                type="checkbox"
                :value="f"
                @change="fetchList"
              >
              {{ f }}
            </label>
          </div>
        </div>
      </aside>

      <div class="content-area">
        <div class="search-bar">
          <input
            v-model="keyword"
            type="text"
            class="search-input"
            placeholder="搜索房源地址、关键词..."
            @keyup.enter="runSearch"
          >
          <button type="button" class="search-btn" @click="runSearch">
            搜索
          </button>
        </div>

        <div class="sort-bar">
          <div class="sort-options">
            <div
              v-for="opt in sortOptions"
              :key="opt.value"
              class="sort-item"
              :class="{ active: sort === opt.value }"
              role="button"
              tabindex="0"
              @click="setSort(opt.value)"
            >
              {{ opt.label }}
            </div>
          </div>
          <div class="result-count">
            共找到 <strong>{{ total }}</strong> 套房源
          </div>
        </div>

        <div v-loading="loading" class="property-list">
          <div
            v-for="item in propertyList"
            :key="item.id"
            class="property-item"
            @click="goDetail(item.id)"
          >
            <div class="property-image-large">
              <img
                v-if="coverSrc(item)"
                :src="coverSrc(item)"
                alt=""
              >
              <span v-else class="img-ph">暂无图片</span>
            </div>
            <div class="property-content" @click.stop>
              <div class="property-header">
                <div>
                  <div class="property-title-large">
                    {{ item.title }}
                  </div>
                  <div class="property-details-large">
                    <div class="detail-item">
                      📐 {{ formatArea(item.area) }}
                    </div>
                    <div class="detail-item">
                      🏢 {{ formatFloor(item) }}
                    </div>
                    <div class="detail-item">
                      🧭 {{ formatOrientation(item.orientation) }}
                    </div>
                  </div>
                </div>
                <div>
                  <div class="property-price-large">
                    ¥{{ formatMoney(item.rentPrice) }}<span class="property-price-unit">/月</span>
                  </div>
                </div>
              </div>
              <div class="property-address">
                📍 {{ item.address }}
              </div>
              <div class="property-tags-large">
                <span v-if="typeLabel(item.propertyType)" class="tag">{{ typeLabel(item.propertyType) }}</span>
                <span v-for="(t, i) in displayTags(item)" :key="`${t}-${i}`" class="tag">{{ t }}</span>
              </div>
              <div class="property-footer">
                <div class="property-meta">
                  <span>👁 {{ item.viewCount ?? 0 }}次浏览</span>
                  <span>🕐 {{ formatPublished(item.createTime) }}</span>
                </div>
                <div class="property-actions">
                  <a
                    class="btn-view"
                    href="javascript:void(0)"
                    @click.prevent="goDetail(item.id)"
                  >查看详情</a>
                  <button
                    type="button"
                    class="btn-booking"
                    @click="bookViewing(item.id)"
                  >
                    预约看房
                  </button>
                </div>
              </div>
            </div>
          </div>

          <el-empty v-if="!loading && propertyList.length === 0" description="暂无符合条件的房源" />
        </div>

        <div v-if="total > 0" class="pagination-wrap">
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            background
            @current-change="fetchList"
          />
        </div>
      </div>
    </div>

    <router-link to="/ai" class="ai-assistant" title="AI助手">
      <span class="ai-icon">💬</span>
    </router-link>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/layout/AppHeader.vue'
import { usePropertyStore } from '@/stores/property'
import { useAuthStore } from '@/stores/auth'
import type { PropertyListSort } from '@/api/property'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

interface PropertyRow {
  id: number
  title?: string
  rentPrice?: number
  area?: number
  floor?: number
  totalFloor?: number
  orientation?: string
  address?: string
  facilities?: string[]
  tags?: string[]
  viewCount?: number
  createTime?: string
  coverImage?: string
  images?: string[]
  propertyType?: string
}

const route = useRoute()
const router = useRouter()
const propertyStore = usePropertyStore()
const authStore = useAuthStore()

const roomTypeOptions = [
  { value: 'apartment', label: '整租公寓' },
  { value: 'studio', label: '单间' },
  { value: 'suite', label: '套间' },
  { value: 'villa', label: '别墅' },
]

const statusOptions = [
  { value: 'vacant', label: '空置' },
  { value: 'occupied', label: '已租' },
  { value: 'reserved', label: '预定中' },
]

const facilityOptions = ['空调', '热水器', '洗衣机', '冰箱', '独立卫浴']

const pricePresets = [
  { id: 'all', min: undefined as number | undefined, max: undefined as number | undefined, label: '不限' },
  { id: 'lt1000', min: undefined, max: 1000, label: '1000元以下' },
  { id: '1000_2000', min: 1000, max: 2000, label: '1000-2000元' },
  { id: '2000_3000', min: 2000, max: 3000, label: '2000-3000元' },
  { id: '3000_5000', min: 3000, max: 5000, label: '3000-5000元' },
  { id: 'gt5000', min: 5000, max: undefined, label: '5000元以上' },
]

const sortOptions: { value: PropertyListSort; label: string }[] = [
  { value: 'comprehensive', label: '综合排序' },
  { value: 'price_asc', label: '价格从低到高' },
  { value: 'price_desc', label: '价格从高到低' },
  { value: 'newest', label: '最新发布' },
  { value: 'views', label: '浏览最多' },
]

const keyword = ref('')
const selectedTypes = ref<string[]>([])
const activePriceId = ref('1000_2000')
const useCustomPrice = ref(false)
const customMinPrice = ref<number | undefined>()
const customMaxPrice = ref<number | undefined>()
const selectedStatuses = ref<string[]>(['vacant'])
const selectedFacilities = ref<string[]>([])
const sort = ref<PropertyListSort>('comprehensive')

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const propertyList = ref<PropertyRow[]>([])

const TYPE_LABELS: Record<string, string> = {
  apartment: '整租公寓',
  studio: '单间',
  suite: '套间',
  villa: '别墅',
}

const ORI_LABELS: Record<string, string> = {
  north: '朝北',
  south: '朝南',
  east: '朝东',
  west: '朝西',
  northeast: '东北',
  northwest: '西北',
  southeast: '东南',
  southwest: '西南',
}

function coverSrc(row: PropertyRow) {
  return row.coverImage || row.images?.[0] || ''
}

function formatMoney(n: number | undefined) {
  if (n == null || Number.isNaN(n))
    return '—'
  return new Intl.NumberFormat('zh-CN').format(n)
}

function formatArea(area: number | undefined) {
  if (area == null)
    return '—㎡'
  return `${area}㎡`
}

function formatFloor(row: PropertyRow) {
  const f = row.floor
  const tf = row.totalFloor
  if (f != null && tf != null)
    return `${f}楼/共${tf}楼`
  if (f != null)
    return `${f}楼`
  return '—'
}

function formatOrientation(o: string | undefined) {
  if (!o)
    return '—'
  const key = o.toLowerCase()
  return ORI_LABELS[o] ?? ORI_LABELS[key] ?? o
}

function typeLabel(t: string | undefined) {
  if (!t)
    return ''
  return TYPE_LABELS[t] ?? ''
}

function displayTags(row: PropertyRow) {
  const fac = row.facilities ?? []
  const tag = row.tags ?? []
  const merged = [...fac, ...tag]
  return merged.slice(0, 8)
}

function formatPublished(t: string | undefined) {
  if (!t)
    return '—'
  return dayjs(t).fromNow()
}

function selectPricePreset(id: string) {
  useCustomPrice.value = false
  customMinPrice.value = undefined
  customMaxPrice.value = undefined
  activePriceId.value = id
  page.value = 1
  fetchList()
}

function setSort(s: PropertyListSort) {
  sort.value = s
  page.value = 1
  fetchList()
}

function runSearch() {
  page.value = 1
  fetchList()
}

function resetFilters() {
  keyword.value = ''
  selectedTypes.value = []
  activePriceId.value = '1000_2000'
  useCustomPrice.value = false
  customMinPrice.value = undefined
  customMaxPrice.value = undefined
  selectedStatuses.value = ['vacant']
  selectedFacilities.value = []
  sort.value = 'comprehensive'
  page.value = 1
  fetchList()
}

function priceRangeForRequest(): { minPrice?: number; maxPrice?: number } {
  if (useCustomPrice.value) {
    return {
      minPrice: customMinPrice.value,
      maxPrice: customMaxPrice.value,
    }
  }
  const preset = pricePresets.find(p => p.id === activePriceId.value)
  return {
    minPrice: preset?.min,
    maxPrice: preset?.max,
  }
}

async function fetchList() {
  loading.value = true
  try {
    const pr = priceRangeForRequest()
    await propertyStore.fetchPropertyList({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      types: selectedTypes.value.length ? selectedTypes.value : undefined,
      minPrice: pr.minPrice,
      maxPrice: pr.maxPrice,
      statuses: selectedStatuses.value.length ? selectedStatuses.value : ['vacant'],
      facilities: selectedFacilities.value.length ? selectedFacilities.value : undefined,
      sort: sort.value,
    })
    propertyList.value = propertyStore.propertyList as PropertyRow[]
    total.value = propertyStore.totalCount
  }
  catch {
    propertyList.value = []
    total.value = 0
    ElMessage.error('房源加载失败，请稍后重试')
  }
  finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  router.push(`/properties/${id}`)
}

function bookViewing(id: number) {
  if (!authStore.isLoggedIn) {
    ElMessage.info('请先登录后再预约看房')
    router.push({ name: 'Login', query: { redirect: `/properties/${id}?book=1` } })
    return
  }
  router.push({ path: `/properties/${id}`, query: { book: '1' } })
}

onMounted(() => {
  const q = route.query
  if (q.keyword)
    keyword.value = String(q.keyword)
  if (q.sort && typeof q.sort === 'string')
    sort.value = q.sort as PropertyListSort

  if (q.minPrice != null || q.maxPrice != null) {
    useCustomPrice.value = true
    activePriceId.value = 'all'
    if (q.minPrice != null)
      customMinPrice.value = Number(q.minPrice)
    if (q.maxPrice != null)
      customMaxPrice.value = Number(q.maxPrice)
  }

  fetchList()
})
</script>

<style scoped lang="scss">
$primary: #2c7be5;
$accent: #ffa500;
$page-bg: #f5f7fa;

.property-list-page {
  min-height: 100vh;
  background: $page-bg;
  color: #333;
  padding-top: 0;
}

.main-content {
  margin-top: 0;
  padding: 20px 40px 48px;
  display: flex;
  gap: 20px;
  max-width: 1400px;
  margin-left: auto;
  margin-right: auto;
}

.filter-sidebar {
  width: 260px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  height: fit-content;
  position: sticky;
  top: 80px;
}

.filter-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #333;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-reset {
  font-size: 14px;
  color: $primary;
  cursor: pointer;
  font-weight: normal;
}

.filter-section {
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
    margin-bottom: 0;
    padding-bottom: 0;
  }
}

.filter-label {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 12px;
  color: #333;
}

.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.checkbox-item {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-size: 14px;

  input {
    margin-right: 8px;
    cursor: pointer;
  }
}

.price-range {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.price-option {
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  cursor: pointer;
  text-align: center;
  transition: all 0.3s;
  font-size: 14px;

  &:hover {
    border-color: $primary;
    color: $primary;
  }

  &.active {
    background-color: $primary;
    color: #fff;
    border-color: $primary;
  }
}

.content-area {
  flex: 1;
  min-width: 0;
}

.search-bar {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  gap: 10px;
}

.search-input {
  flex: 1;
  padding: 12px 20px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 15px;
  outline: none;

  &:focus {
    border-color: $primary;
  }
}

.search-btn {
  padding: 12px 40px;
  background-color: $primary;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background-color: #1a5bb8;
  }
}

.sort-bar {
  background: #fff;
  border-radius: 12px;
  padding: 15px 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.sort-options {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.sort-item {
  padding: 6px 16px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s;
  color: #666;
  font-size: 14px;

  &:hover {
    color: $primary;
  }

  &.active {
    background-color: #f0f7ff;
    color: $primary;
    font-weight: 500;
  }
}

.result-count {
  color: #666;
  font-size: 14px;
}

.property-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 200px;
}

.property-item {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  gap: 20px;
  transition: all 0.3s;
  cursor: pointer;

  &:hover {
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
  }
}

.property-image-large {
  width: 280px;
  height: 200px;
  background-color: #e0e0e0;
  border-radius: 8px;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .img-ph {
    color: #999;
    font-size: 14px;
  }
}

.property-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.property-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  gap: 12px;
}

.property-title-large {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  word-break: break-word;
}

.property-price-large {
  font-size: 28px;
  color: $accent;
  font-weight: bold;
  white-space: nowrap;
}

.property-price-unit {
  font-size: 14px;
  color: #999;
  font-weight: normal;
}

.property-details-large {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 20px;
  margin-bottom: 8px;
  color: #666;
  font-size: 15px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.property-address {
  color: #666;
  font-size: 14px;
  margin-bottom: 12px;
  word-break: break-word;
}

.property-tags-large {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.tag {
  background-color: #f0f7ff;
  color: $primary;
  padding: 5px 12px;
  border-radius: 4px;
  font-size: 13px;
}

.property-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  flex-wrap: wrap;
  gap: 12px;
}

.property-meta {
  display: flex;
  gap: 15px;
  color: #999;
  font-size: 13px;
}

.property-actions {
  display: flex;
  gap: 10px;
}

.btn-view {
  padding: 8px 24px;
  background-color: #fff;
  color: $primary;
  border: 1px solid $primary;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  text-decoration: none;
  font-size: 14px;
  line-height: 1.4;
  display: inline-flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background-color: #f0f7ff;
  }
}

.btn-booking {
  padding: 8px 24px;
  background-color: $accent;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;

  &:hover {
    background-color: #ff8c00;
  }
}

.pagination-wrap {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-top: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: center;
}

.ai-assistant {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  transition: all 0.3s;
  z-index: 999;
  text-decoration: none;

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
  }
}

.ai-icon {
  font-size: 28px;
  line-height: 1;
}

@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
    padding: 16px;
  }

  .filter-sidebar {
    width: 100%;
    position: static;
  }

  .property-item {
    flex-direction: column;
  }

  .property-image-large {
    width: 100%;
    height: 200px;
  }
}
</style>
