<template>
  <div class="purchase-page">
    <AppHeader />
    <div class="container">
      <div class="page-header">
        <h1>本地商城</h1>
        <p>社区采购商品，真实库存，快速下单</p>
      </div>

      <div class="filters">
        <el-input
          v-model="keyword"
          placeholder="搜索商品..."
          clearable
          style="width: 260px"
          @change="fetchList"
        />
        <el-select v-model="category" placeholder="全部分类" clearable style="width: 160px" @change="fetchList">
          <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </div>

      <div v-loading="loading" class="product-grid">
        <div v-for="item in productList" :key="item.id" class="product-card card">
          <img :src="item.image || item.images?.[0] || '/placeholder-secondhand.jpg'" :alt="item.name" class="cover" />
          <div class="title">{{ item.name }}</div>
          <div class="desc">{{ item.description || '暂无描述' }}</div>
          <div class="meta">
            <span class="price">¥{{ item.price }}</span>
            <span class="stock">库存 {{ item.stock ?? 0 }}</span>
          </div>
        </div>
        <el-empty v-if="!loading && productList.length === 0" description="暂无商品" />
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
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { getProcurementList } from '@/api/asset'
import type { ProcurementProduct } from '@/types'

const loading = ref(false)
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const keyword = ref('')
const category = ref('')
const productList = ref<ProcurementProduct[]>([])
const categoryOptions = ref(['家具', '家电', '清洁用品', '办公用品', '其他'])

async function fetchList() {
  loading.value = true
  try {
    const result = await getProcurementList({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      category: category.value || undefined,
    })
    productList.value = (result.list ?? result.records ?? []) as ProcurementProduct[]
    total.value = Number(result.total ?? 0)
  } catch {
    productList.value = []
    total.value = 0
    ElMessage.error('商城数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped lang="scss">
.purchase-page {
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

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-lg);
  min-height: 280px;
}

.product-card {
  padding: var(--spacing-sm);

  .cover {
    width: 100%;
    height: 160px;
    object-fit: cover;
    border-radius: var(--border-radius-base);
    background: #f5f7fa;
  }

  .title {
    margin-top: 10px;
    font-weight: 600;
  }

  .desc {
    margin-top: 6px;
    font-size: 12px;
    color: var(--color-text-secondary);
    min-height: 32px;
  }

  .meta {
    margin-top: 8px;
    display: flex;
    justify-content: space-between;

    .price {
      color: #f56c6c;
      font-weight: 700;
    }

    .stock {
      color: var(--color-text-secondary);
      font-size: 12px;
    }
  }
}

.pagination {
  display: flex;
  justify-content: center;
  padding: var(--spacing-xl) 0;
}
</style>
