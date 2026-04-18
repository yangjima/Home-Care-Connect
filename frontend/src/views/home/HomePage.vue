<template>
  <div class="home-page">
    <AppHeader />

    <!-- Hero Section -->
    <section class="hero">
      <div class="container">
        <h1 class="hero-title">欢迎来到居服通</h1>
        <p class="hero-subtitle">智慧社区服务管理平台，为您的社区生活提供全方位服务支持</p>

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
    <section v-if="stats.length" class="stats-section">
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
import { usePropertyStore } from '@/stores/property'
import { useServiceStore } from '@/stores/service'
import type { Property, ServiceType } from '@/types'

const router = useRouter()
const propertyStore = usePropertyStore()
const serviceStore = useServiceStore()
const searchKeyword = ref('')

const loadingProperties = ref(false)
const loadingServices = ref(false)
const stats = ref<Array<{ label: string; value: number }>>([])
const hotProperties = ref<Property[]>([])
const hotServices = ref<ServiceType[]>([])

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

function buildStats() {
  const nextStats: Array<{ label: string; value: number }> = []
  if (propertyStore.totalCount > 0) {
    nextStats.push({ label: '精选房源', value: propertyStore.totalCount })
  }
  if (serviceStore.serviceTypes.length > 0) {
    nextStats.push({ label: '社区服务', value: serviceStore.serviceTypes.length })
  }
  stats.value = nextStats
}

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/properties', query: { keyword: searchKeyword.value } })
  } else {
    router.push('/properties')
  }
}

onMounted(async () => {
  await Promise.all([loadHotProperties(), loadHotServices()])
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
  padding: 80px 0;
  text-align: center;

  .hero-title {
    font-size: 48px;
    font-weight: 700;
    margin-bottom: 16px;
  }

  .hero-subtitle {
    font-size: 20px;
    opacity: 0.9;
    margin-bottom: 40px;
  }
}

.search-box {
  max-width: 600px;
  margin: 0 auto 40px;

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
  gap: var(--spacing-xl);
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
    font-size: 40px;
    width: 64px;
    height: 64px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.stats-section {
  background: var(--color-bg-white);
  padding: var(--spacing-xl) 0;
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
    font-size: 36px;
    font-weight: 700;
    color: var(--color-primary);
  }

  .stat-label {
    font-size: 14px;
    color: var(--color-text-secondary);
    margin-top: 4px;
  }
}

.section {
  padding: var(--spacing-xxl) 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);

  h2 {
    font-size: 24px;
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
  gap: var(--spacing-lg);
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-lg);
}

@media (max-width: 1024px) {
  .property-grid,
  .service-grid {
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
  .service-grid {
    grid-template-columns: 1fr;
  }
}
</style>
