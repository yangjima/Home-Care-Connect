<template>
  <div class="home-page">
    <AppHeader />

    <!-- Hero Section -->
    <section class="hero">
      <div class="container">
        <div class="hero-inner">
          <div class="hero-left">
            <div class="hero-kicker">智慧社区 · 一站式生活服务</div>
            <h1 class="hero-title">欢迎来到居服通</h1>
            <p class="hero-subtitle">房源、社区服务、本地商城、跳蚤市场与 AI 助手，一次入口全都搞定。design by 杨金明</p>

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

            <div class="quick-links" aria-label="快捷入口">
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

          <aside class="hero-right" aria-label="平台亮点">
            <div class="hero-panel">
              <div class="panel-title">今日推荐</div>
              <div class="panel-list">
                <router-link to="/properties" class="panel-item">
                  <span class="pi-icon">🏡</span>
                  <div class="pi-text">
                    <div class="pi-main">优质房源</div>
                    <div class="pi-sub">精选 {{ hotProperties.length }} 条，快速预约看房</div>
                  </div>
                </router-link>
                <router-link to="/services" class="panel-item">
                  <span class="pi-icon">🧰</span>
                  <div class="pi-text">
                    <div class="pi-main">热门服务</div>
                    <div class="pi-sub">维修保洁上门，省时更省心</div>
                  </div>
                </router-link>
                <router-link to="/ai" class="panel-item">
                  <span class="pi-icon">✨</span>
                  <div class="pi-text">
                    <div class="pi-main">AI 助手</div>
                    <div class="pi-sub">帮你快速匹配需求与推荐</div>
                  </div>
                </router-link>
              </div>
              <div class="panel-tip">提示：输入关键词回车即可搜索</div>
            </div>
          </aside>
        </div>
      </div>
    </section>

    <!-- Stats -->
    <section class="stats-section">
      <div class="container">
        <div class="stats-card">
          <div class="stats-grid">
            <div v-for="item in stats" :key="item.label" class="stat-item">
              <div class="stat-number">{{ item.value }}</div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Hot Properties -->
    <section class="section section--tight">
      <div class="container">
        <div class="section-header">
          <div class="section-title">
            <h2>热门房源</h2>
            <p>为你优选更受欢迎的社区房源</p>
          </div>
          <router-link to="/properties" class="more-link">查看更多</router-link>
        </div>
        <div class="section-surface">
          <div class="property-grid">
            <PropertyCard v-for="item in hotProperties" :key="item.id" :property="item" />
            <el-empty v-if="!loadingProperties && hotProperties.length === 0" description="暂无热门房源" />
          </div>
        </div>
      </div>
    </section>

    <!-- Hot Services -->
    <section class="section section--alt">
      <div class="container">
        <div class="section-header">
          <div class="section-title">
            <h2>热门服务</h2>
            <p>更快响应的到家服务，随叫随到</p>
          </div>
          <router-link to="/services" class="more-link">查看更多</router-link>
        </div>
        <div class="section-surface">
          <div class="service-grid">
            <ServiceCard v-for="item in hotServices" :key="item.id" :service="item" />
            <el-empty v-if="!loadingServices && hotServices.length === 0" description="暂无热门服务" />
          </div>
        </div>
      </div>
    </section>

    <!-- Local mall -->
    <section class="section">
      <div class="container">
        <div class="section-header">
          <div class="section-title">
            <h2>本地商城</h2>
            <p>精选好物，社区附近更快送达</p>
          </div>
          <router-link to="/purchase" class="more-link">查看更多</router-link>
        </div>
        <div class="section-surface">
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
      </div>
    </section>

    <!-- Flea market -->
    <section class="section section--alt section--end">
      <div class="container">
        <div class="section-header">
          <div class="section-title">
            <h2>跳蚤市场</h2>
            <p>闲置好物循环利用，省钱也环保</p>
          </div>
          <router-link to="/secondhand" class="more-link">查看更多</router-link>
        </div>
        <div class="section-surface">
          <div v-loading="loadingSecondhand" class="feature-grid">
            <SecondhandCard v-for="item in hotSecondhandItems" :key="item.id" :item="item" />
            <el-empty
              v-if="!loadingSecondhand && hotSecondhandItems.length === 0"
              class="grid-empty"
              description="暂无闲置商品"
            />
          </div>
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
  position: relative;
  overflow: hidden;
  background: radial-gradient(1200px 600px at 10% 0%, rgba(255, 255, 255, 0.18) 0%, rgba(255, 255, 255, 0) 60%),
    radial-gradient(900px 520px at 100% 10%, rgba(255, 255, 255, 0.12) 0%, rgba(255, 255, 255, 0) 55%),
    linear-gradient(135deg, #3b82f6 0%, #8b5cf6 50%, #ec4899 100%);
  color: white;
  padding: 72px 0 84px;

  &::before {
    content: '';
    position: absolute;
    inset: -2px;
    background-image:
      radial-gradient(rgba(255, 255, 255, 0.22) 1px, transparent 1px);
    background-size: 24px 24px;
    opacity: 0.18;
    pointer-events: none;
    transform: translate3d(0, 0, 0);
  }

  &::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: -220px;
    width: 980px;
    height: 480px;
    transform: translateX(-50%);
    background: radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.32), rgba(255, 255, 255, 0) 65%);
    filter: blur(6px);
    opacity: 0.9;
    pointer-events: none;
  }
}

.hero-inner {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 36px;
  align-items: center;
}

.hero-left {
  text-align: left;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(10px);
  font-size: 13px;
  letter-spacing: 0.06em;
  margin-bottom: 14px;
}

.hero-title {
  font-size: 52px;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin-bottom: 12px;
  line-height: 1.12;
}

.hero-subtitle {
  font-size: 18px;
  opacity: 0.92;
  margin-bottom: 22px;
  max-width: 46ch;
}

.search-box {
  max-width: 640px;
  margin: 0 0 18px;

  :deep(.el-input__wrapper) {
    border-radius: 999px 0 0 999px;
    padding-left: 20px;
    background: rgba(255, 255, 255, 0.95);
    border: 1px solid rgba(255, 255, 255, 0.55);
    box-shadow: 0 14px 36px rgba(0, 0, 0, 0.18);
  }

  :deep(.el-input-group__append) {
    border-radius: 0 999px 999px 0;
    background: rgba(17, 24, 39, 0.92);
    color: white;
    border: none;

    .el-button {
      color: white;
    }
  }
}

.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.quick-link {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  color: white;
  opacity: 0.95;
  transition: opacity 0.2s, transform 0.2s, background 0.2s, border-color 0.2s;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(10px);

  &:hover {
    opacity: 1;
    transform: translateY(-2px);
    background: rgba(255, 255, 255, 0.18);
    border-color: rgba(255, 255, 255, 0.28);
  }

  .ql-icon {
    font-size: 20px;
    width: 34px;
    height: 34px;
    background: rgba(255, 255, 255, 0.16);
    border-radius: 10px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
}

.hero-right {
  display: flex;
  justify-content: flex-end;
}

.hero-panel {
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 18px;
  padding: 18px;
  backdrop-filter: blur(14px);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.24);
}

.panel-title {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.08em;
  opacity: 0.92;
  margin-bottom: 12px;
}

.panel-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.panel-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 12px;
  border-radius: 14px;
  color: #fff;
  background: rgba(17, 24, 39, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.16);
  transition: transform 0.2s, background 0.2s, border-color 0.2s;

  &:hover {
    transform: translateY(-2px);
    background: rgba(17, 24, 39, 0.30);
    border-color: rgba(255, 255, 255, 0.22);
  }
}

.pi-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: rgba(255, 255, 255, 0.14);
}

.pi-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.pi-main {
  font-weight: 700;
  font-size: 14px;
}

.pi-sub {
  font-size: 12px;
  opacity: 0.9;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.panel-tip {
  margin-top: 12px;
  font-size: 12px;
  opacity: 0.88;
}

.stats-section {
  position: relative;
  padding: 0 0 var(--spacing-xl);
  margin-top: -36px;
}

.stats-card {
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 18px;
  backdrop-filter: blur(10px);
  box-shadow: 0 18px 60px rgba(15, 23, 42, 0.12);
  padding: 18px 18px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-lg);
}

.stat-item {
  text-align: center;
  padding: 8px 0;

  .stat-number {
    font-size: 32px;
    font-weight: 800;
    color: #111827;
    letter-spacing: -0.02em;
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

.section-surface {
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 18px;
  padding: 12px;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(8px);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);

  h2 {
    font-size: 24px;
    font-weight: 800;
    letter-spacing: -0.01em;
    margin-bottom: 2px;
  }
}

.section-title {
  display: flex;
  flex-direction: column;

  p {
    font-size: 13px;
    color: var(--color-text-secondary);
  }
}

.section--alt {
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.02) 0%, rgba(15, 23, 42, 0.00) 100%);
}

.section--tight {
  padding-top: var(--spacing-md);
}

.section--end {
  padding-bottom: calc(var(--spacing-lg) + 6px);
}

.more-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  padding: 10px 12px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.10);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;

  &::after {
    content: '→';
    opacity: 0.9;
  }

  &:hover {
    transform: translateY(-1px);
    border-color: rgba(59, 130, 246, 0.35);
    box-shadow: 0 14px 34px rgba(15, 23, 42, 0.12);
    color: #0f172a;
  }
}

.property-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-sm);
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-sm);
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-sm);
  min-height: 80px;
}

.property-grid :deep(.base-card),
.service-grid :deep(.base-card),
.feature-grid :deep(.base-card) {
  transition: transform 0.2s, box-shadow 0.2s;
}

.property-grid :deep(.base-card:hover),
.service-grid :deep(.base-card:hover),
.feature-grid :deep(.base-card:hover) {
  transform: translateY(-2px);
  box-shadow: 0 18px 46px rgba(15, 23, 42, 0.12);
}

.mall-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.06);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
  text-decoration: none;
  color: inherit;
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 22px 50px rgba(15, 23, 42, 0.12);
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
  .hero-inner {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .hero-right {
    justify-content: flex-start;
  }

  .property-grid,
  .service-grid,
  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero {
    padding: 52px 0 72px;
  }

  .hero-left {
    text-align: left;
  }

  .hero-title {
    font-size: 38px;
  }

  .hero-subtitle {
    font-size: 15px;
  }

  .search-box {
    max-width: 100%;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .property-grid,
  .service-grid,
  .feature-grid {
    grid-template-columns: 1fr;
  }

  .section-surface {
    padding: 12px;
  }
}
</style>
