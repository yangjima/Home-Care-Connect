<template>
  <div class="home-page">
    <AppHeader />

    <!-- Hero Section -->
    <section class="hero">
      <div class="container">
        <h1 class="hero-title">欢迎来到居服通</h1>
        <p class="hero-subtitle">智慧社区服务管理平台，为您的社区生活提供全方位服务支持(design by 杨金明)</p>

        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索房源、服务或二手物品..."
            size="large"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button :icon="Search" @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </div>

        <div class="quick-links">
          <router-link to="/properties" class="quick-link">
            <span class="ql-icon">🏠</span>
            <span>找房源</span>
          </router-link>
          <router-link to="/services" class="quick-link">
            <span class="ql-icon">🛠️</span>
            <span>找服务</span>
          </router-link>
          <router-link to="/purchase" class="quick-link">
            <span class="ql-icon">🛒</span>
            <span>本地商城</span>
          </router-link>
          <router-link to="/secondhand" class="quick-link">
            <span class="ql-icon">🔄</span>
            <span>跳蚤市场</span>
          </router-link>
          <router-link to="/ai" class="quick-link">
            <span class="ql-icon">🤖</span>
            <span>AI 助手</span>
          </router-link>
        </div>
      </div>
    </section>

    <!-- Stats -->
    <section class="stats-section">
      <div class="container">
        <div class="stats-grid">
          <div v-for="item in stats" :key="item.label" class="stat-item">
            <div class="stat-number">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Hot Properties -->
    <section class="section">
      <div class="container">
        <div class="section-header">
          <h2>热门房源</h2>
          <router-link to="/properties" class="more-link">查看更多 →</router-link>
        </div>
        <div class="property-grid">
          <PropertyCard v-for="item in hotProperties" :key="item.id" :property="item" />
          <el-empty v-if="!loadingProperties && hotProperties.length === 0" description="暂无热门房源" />
        </div>
      </div>
    </section>

    <!-- Hot Services -->
    <section class="section">
      <div class="container">
        <div class="section-header">
          <h2>热门服务</h2>
          <router-link to="/services" class="more-link">查看更多 →</router-link>
        </div>
        <div class="service-grid">
          <ServiceCard v-for="item in hotServices" :key="item.id" :service="item" />
          <el-empty v-if="!loadingServices && hotServices.length === 0" description="暂无热门服务" />
        </div>
      </div>
    </section>

    <!-- Local mall -->
    <section class="section">
      <div class="container">
        <div class="section-header">
          <h2>本地商城</h2>
          <router-link to="/purchase" class="more-link">查看更多 →</router-link>
        </div>
        <div v-loading="loadingMall" class="feature-grid">
          <router-link
            v-for="item in hotMallProducts"
            :key="item.id"
            :to="'/purchase'"
            class="mall-card"
          >
            <div class="mall-card-img">
              <img v-if="mallCoverUrl(item)" :src="mallCoverUrl(item)!" :alt="item.name" class="mall-card-photo" />
              <span v-else class="mall-card-emoji" aria-hidden="true">{{ mallCategoryEmoji(item.category) }}</span>
              <span v-if="item.productTag" class="mall-card-tag">{{ item.productTag }}</span>
            </div>
            <div class="mall-card-body">
              <h3 class="mall-card-name">{{ item.name }}</h3>
              <div class="mall-card-meta">
                <span class="mall-card-price">¥{{ formatMallPrice(item.price) }}</span>
                <span class="mall-card-sales">已售 {{ item.salesCount ?? 0 }}</span>
              </div>
            </div>
          </router-link>
          <el-empty
            v-if="!loadingMall && hotMallProducts.length === 0"
            class="grid-empty"
            description="暂无商城商品"
          />
        </div>
      </div>
    </section>

    <!-- Flea market -->
    <section class="section">
      <div class="container">
        <div class="section-header">
          <h2>跳蚤市场</h2>
          <router-link to="/secondhand" class="more-link">查看更多 →</router-link>
        </div>
        <div v-loading="loadingSecondhand" class="feature-grid">
          <SecondhandCard v-for="item in hotSecondhandItems" :key="item.id" :item="item" />
          <el-empty
            v-if="!loadingSecondhand && hotSecondhandItems.length === 0"
            class="grid-empty"
            description="暂无闲置商品"
          />
        </div>
      </div>
    </section>

    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import PropertyCard from '@/components/property/PropertyCard.vue'
import ServiceCard from '@/components/service/ServiceCard.vue'
import SecondhandCard from '@/components/asset/SecondhandCard.vue'
import { getProcurementList, getProcurementSummary, getSecondhandList, getSecondhandSummary } from '@/api/asset'
import { usePropertyStore } from '@/stores/property'
import { useServiceStore } from '@/stores/service'
import type { Property, ProcurementProduct, SecondhandItem, ServiceType } from '@/types'

const router = useRouter()
const propertyStore = usePropertyStore()
const serviceStore = useServiceStore()
const searchKeyword = ref('')

const loadingProperties = ref(false)
const loadingServices = ref(false)
const loadingMall = ref(false)
const loadingSecondhand = ref(false)
const stats = ref<Array<{ label: string; value: number }>>([])
const hotProperties = ref<Property[]>([])
const hotServices = ref<ServiceType[]>([])
const hotMallProducts = ref<ProcurementProduct[]>([])
const hotSecondhandItems = ref<SecondhandItem[]>([])
const mallTotal = ref(0)
const fleaMarketTotal = ref(0)

async function loadHotProperties() {
  loadingProperties.value = true
  try {
    await propertyStore.fetchPropertyList({ page: 1, size: 4 })
    hotProperties.value = propertyStore.propertyList.slice(0, 4)
  } catch {
    hotProperties.value = []
  } finally {
    loadingProperties.value = false
  }
}

async function loadHotServices() {
  loadingServices.value = true
  try {
    await serviceStore.fetchServiceTypes()
    hotServices.value = serviceStore.serviceTypes.slice(0, 4)
  } catch {
    hotServices.value = []
  } finally {
    loadingServices.value = false
  }
}

function parseMallImages(raw: ProcurementProduct['images']): string[] {
  if (!raw) return []
  if (Array.isArray(raw)) return raw.filter(Boolean) as string[]
  if (typeof raw === 'string') {
    try {
      const v = JSON.parse(raw) as unknown
      if (Array.isArray(v)) return v.filter((x) => typeof x === 'string') as string[]
    } catch {
      if (raw.startsWith('http')) return [raw]
    }
  }
  return []
}

function mallCoverUrl(item: ProcurementProduct) {
  if (item.image) return item.image
  const arr = parseMallImages(item.images)
  return arr[0] || ''
}

function mallCategoryEmoji(cat: string) {
  const map: Record<string, string> = {
    办公用品: '📎',
    清洁用品: '🧹',
    维修工具: '🔧',
    绿植盆栽: '🌿',
    安防设备: '📹',
    生活物资: '🪑',
  }
  return map[cat] || '📦'
}

function formatMallPrice(p: number | string) {
  const n = Number(p)
  if (Number.isNaN(n)) return String(p)
  const rounded = Math.round(n * 100) / 100
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(2)
}

async function loadHotMall() {
  loadingMall.value = true
  try {
    const [sumRes, listRes] = await Promise.allSettled([
      getProcurementSummary(),
      getProcurementList({ page: 1, size: 4, sort: 'sales' }),
    ])
    if (listRes.status !== 'fulfilled') {
      throw listRes.reason
    }
    const result = listRes.value
    const rows = (result.list ?? result.records ?? []) as ProcurementProduct[]
    hotMallProducts.value = rows
    const fromPage = Number(result.total ?? 0)
    if (sumRes.status === 'fulfilled') {
      mallTotal.value = Number(sumRes.value.totalOnShelf ?? 0)
    } else {
      mallTotal.value = Math.max(fromPage, rows.length)
    }
  } catch {
    hotMallProducts.value = []
    mallTotal.value = 0
  } finally {
    loadingMall.value = false
  }
}

async function loadHotSecondhand() {
  loadingSecondhand.value = true
  try {
    const [summary, listResult] = await Promise.all([
      getSecondhandSummary(),
      getSecondhandList({ page: 1, size: 4 }),
    ])
    fleaMarketTotal.value = Number(summary.totalOnSale ?? 0)
    const rows = (listResult.list ?? listResult.records ?? []) as SecondhandItem[]
    hotSecondhandItems.value = rows
  } catch {
    hotSecondhandItems.value = []
    fleaMarketTotal.value = 0
  } finally {
    loadingSecondhand.value = false
  }
}

function buildStats() {
  stats.value = [
    { label: '精选房源', value: propertyStore.totalCount },
    { label: '社区服务', value: serviceStore.serviceTypes.length },
    { label: '本地商城', value: mallTotal.value },
    { label: '跳蚤市场', value: fleaMarketTotal.value },
  ]
}

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/properties', query: { keyword: searchKeyword.value } })
  } else {
    router.push('/properties')
  }
}

onMounted(async () => {
  await Promise.all([loadHotProperties(), loadHotServices(), loadHotMall(), loadHotSecondhand()])
  buildStats()
})
</script>

<style scoped lang="scss">
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.hero {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 44px 0 36px;
  text-align: center;

  .hero-title {
    font-size: 40px;
    font-weight: 700;
    margin-bottom: 10px;
  }

  .hero-subtitle {
    font-size: 18px;
    opacity: 0.9;
    margin-bottom: 22px;
  }
}

.search-box {
  max-width: 600px;
  margin: 0 auto 22px;

  :deep(.el-input__wrapper) {
    border-radius: 24px 0 0 24px;
    padding-left: 20px;
  }

  :deep(.el-input-group__append) {
    border-radius: 0 24px 24px 0;
    background: var(--color-primary);
    color: white;
    border: none;

    .el-button {
      color: white;
    }
  }
}

.quick-links {
  display: flex;
  justify-content: center;
  gap: var(--spacing-lg);
}

.quick-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: white;
  opacity: 0.9;
  transition: opacity 0.2s, transform 0.2s;

  &:hover {
    opacity: 1;
    transform: translateY(-4px);
  }

  .ql-icon {
    font-size: 36px;
    width: 56px;
    height: 56px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.stats-section {
  background: var(--color-bg-white);
  padding: var(--spacing-md) 0 var(--spacing-lg);
  box-shadow: var(--shadow-light);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-lg);
}

.stat-item {
  text-align: center;

  .stat-number {
    font-size: 30px;
    font-weight: 700;
    color: var(--color-primary);
  }

  .stat-label {
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-top: 2px;
  }
}

.section {
  padding: var(--spacing-lg) 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);

  h2 {
    font-size: 20px;
    font-weight: 600;
  }
}

.more-link {
  color: var(--color-primary);
  font-size: 14px;
}

.property-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-md);
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-md);
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-md);
  min-height: 80px;
}

.mall-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: var(--shadow-light);
  text-decoration: none;
  color: inherit;
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  }
}

.mall-card-img {
  width: 100%;
  height: 140px;
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.mall-card-photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mall-card-emoji {
  font-size: 48px;
  line-height: 1;
}

.mall-card-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: var(--el-color-danger, #ff4d4f);
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}

.mall-card-body {
  padding: 12px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.mall-card-name {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: var(--color-text-primary, #333);
}

.mall-card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-top: auto;
}

.mall-card-price {
  font-size: 17px;
  font-weight: 700;
  color: #ff4d4f;
}

.mall-card-sales {
  font-size: 12px;
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.grid-empty {
  grid-column: 1 / -1;
}

@media (max-width: 1024px) {
  .property-grid,
  .service-grid,
  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 32px;
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .quick-links {
    gap: var(--spacing-lg);
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .property-grid,
  .service-grid,
  .feature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
