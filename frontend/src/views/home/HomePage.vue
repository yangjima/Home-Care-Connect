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
          <router-link to="/secondhand" class="quick-link">
            <span class="ql-icon">🔄</span>
            <span>二手交易</span>
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
          <div class="stat-item">
            <div class="stat-number">{{ stats.properties }}+</div>
            <div class="stat-label">精选房源</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ stats.services }}+</div>
            <div class="stat-label">社区服务</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ stats.users }}+</div>
            <div class="stat-label">注册用户</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ stats.orders }}+</div>
            <div class="stat-label">完成订单</div>
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

const router = useRouter()
const searchKeyword = ref('')

const stats = ref({
  properties: 1200,
  services: 86,
  users: 5600,
  orders: 9800,
})

const hotProperties = ref([
  { id: 1, title: '城中心精品公寓', address: '长安区中山路188号', price: 3500, area: 65, rooms: 2, livingRooms: 1, bathrooms: 1, type: '普通住宅', decoration: '精装修', image: '' },
  { id: 2, title: '科技园智慧小区', address: '高新区创新路99号', price: 4200, area: 89, rooms: 3, livingRooms: 2, bathrooms: 1, type: '公寓', decoration: '精装修', image: '' },
  { id: 3, title: '河畔花园洋房', address: '滨河区沿河大道188号', price: 5800, area: 120, rooms: 3, livingRooms: 2, bathrooms: 2, type: '洋房', decoration: '豪华装修', image: '' },
  { id: 4, title: '地铁口精装两居', address: '2号线 XX 站', price: 2800, area: 55, rooms: 2, livingRooms: 1, bathrooms: 1, type: '普通住宅', decoration: '精装修', image: '' },
])

const hotServices = ref([
  { id: 1, name: '日常家政保洁', description: '专业家政人员，深度清洁', price: 80, unit: '小时', icon: '🧹' },
  { id: 2, name: '家电维修安装', description: '专业维修，处理各类故障', price: 100, unit: '次', icon: '🔧' },
  { id: 3, name: '老人陪护服务', description: '持证护理人员陪护照料', price: 200, unit: '天', icon: '👴' },
  { id: 4, name: '搬家运输服务', description: '包装搬运一条龙', price: 0, unit: '次', icon: '📦' },
])

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/properties', query: { keyword: searchKeyword.value } })
  } else {
    router.push('/properties')
  }
}
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
