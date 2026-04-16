<template>
  <div class="service-list-page">
    <AppHeader />
    <div class="container">
      <div class="page-header">
        <h1>社区服务</h1>
        <p>专业、高效、贴心的社区生活服务</p>
      </div>

      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索服务类型..."
          clearable
          style="width: 300px"
          @change="fetchServices"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <div v-loading="loading" class="service-grid">
        <ServiceCard v-for="item in serviceTypes" :key="item.id" :service="item" />
        <el-empty v-if="!loading && serviceTypes.length === 0" description="暂无服务" />
      </div>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import ServiceCard from '@/components/service/ServiceCard.vue'
import { useServiceStore } from '@/stores/service'

const serviceStore = useServiceStore()
const keyword = ref('')
const loading = ref(false)
const serviceTypes = ref<any[]>([])

async function fetchServices() {
  loading.value = true
  try {
    await serviceStore.fetchServiceTypes(keyword.value)
    serviceTypes.value = serviceStore.serviceTypes
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchServices()
})
</script>

<style scoped lang="scss">
.service-list-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
}

.page-header {
  padding: var(--spacing-xl) 0 var(--spacing-lg);

  h1 {
    font-size: 28px;
    font-weight: 700;
    margin-bottom: 8px;
  }

  p {
    color: var(--color-text-secondary);
  }
}

.search-bar {
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md);
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-light);
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-lg);
  min-height: 300px;
}

@media (max-width: 1024px) {
  .service-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .service-grid { grid-template-columns: 1fr; }
}
</style>
