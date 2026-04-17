<template>
  <div class="property-list-page">
    <AppHeader />
    <div class="container">
      <div class="page-header">
        <div>
          <h1>房源列表</h1>
          <p>找到您理想中的家</p>
        </div>
        <el-button type="primary" @click="goPublishProperty">
          发布房源
        </el-button>
      </div>

      <div class="filters">
        <el-input
          v-model="keyword"
          placeholder="搜索关键词..."
          clearable
          style="width: 240px"
          @change="fetchList"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-select v-model="filterType" placeholder="房屋类型" clearable style="width: 140px" @change="fetchList">
          <el-option label="普通住宅" value="普通住宅" />
          <el-option label="公寓" value="公寓" />
          <el-option label="洋房" value="洋房" />
          <el-option label="别墅" value="别墅" />
        </el-select>

        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
      </div>

      <div v-loading="loading" class="property-grid">
        <PropertyCard v-for="item in propertyList" :key="item.id" :property="item" />
        <el-empty v-if="!loading && propertyList.length === 0" description="暂无符合条件的房源" />
      </div>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="fetchList"
        />
      </div>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Refresh } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import PropertyCard from '@/components/property/PropertyCard.vue'
import { usePropertyStore } from '@/stores/property'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const propertyStore = usePropertyStore()
const authStore = useAuthStore()

const keyword = ref('')
const filterType = ref('')
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(false)
const propertyList = ref<any[]>([])

function resetFilters() {
  keyword.value = ''
  filterType.value = ''
  page.value = 1
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    await propertyStore.fetchPropertyList({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value,
      type: filterType.value,
    })
    propertyList.value = propertyStore.propertyList
    total.value = propertyStore.totalCount
  } catch {
    propertyList.value = []
    total.value = 0
    ElMessage.error('房源加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function goPublishProperty() {
  if (!authStore.isLoggedIn) {
    router.push('/auth/login')
    return
  }
  router.push('/user/properties')
}

onMounted(() => {
  if (route.query.keyword) {
    keyword.value = route.query.keyword as string
  }
  fetchList()
})
</script>

<style scoped lang="scss">
.property-list-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
}

.page-header {
  padding: var(--spacing-xl) 0 var(--spacing-lg);
  display: flex;
  justify-content: space-between;
  align-items: center;

  h1 {
    font-size: 28px;
    font-weight: 700;
    margin-bottom: 8px;
  }

  p {
    color: var(--color-text-secondary);
  }
}

.filters {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md);
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-light);
}

.property-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-lg);
  min-height: 300px;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: var(--spacing-xl) 0;
}

@media (max-width: 1024px) {
  .property-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .property-grid {
    grid-template-columns: 1fr;
  }

  .filters {
    flex-wrap: wrap;
  }
}
</style>
