<template>
  <div class="secondhand-list-page">
    <AppHeader />
    <div class="container">
      <div class="page-header">
        <h1>二手交易</h1>
        <p>发现身边的闲置好物</p>
      </div>

      <div class="filters">
        <el-input
          v-model="keyword"
          placeholder="搜索商品..."
          clearable
          style="width: 260px"
          @change="fetchList"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-select v-model="filterCategory" placeholder="全部分类" clearable style="width: 140px" @change="fetchList">
          <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
        </el-select>

        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>

        <div class="filter-right">
          <el-button type="primary" @click="showPublishDialog = true">
            <el-icon><Plus /></el-icon> 发布闲置
          </el-button>
        </div>
      </div>

      <div v-loading="loading" class="item-grid">
        <SecondhandCard v-for="item in itemList" :key="item.id" :item="item" />
        <el-empty v-if="!loading && itemList.length === 0" description="暂无闲置商品" />
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

    <!-- 发布闲置对话框 -->
    <el-dialog v-model="showPublishDialog" title="发布闲置" width="500px">
      <el-form :model="publishForm" label-width="100px">
        <el-form-item label="商品标题" required>
          <el-input v-model="publishForm.title" placeholder="简洁描述您的商品" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="商品分类" required>
          <el-select v-model="publishForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="新旧程度" required>
          <el-select v-model="publishForm.condition" placeholder="请选择" style="width: 100%">
            <el-option label="全新" value="new" />
            <el-option label="几乎全新" value="like_new" />
            <el-option label="良好" value="good" />
            <el-option label="一般" value="fair" />
            <el-option label="较差" value="poor" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" required>
          <el-input-number v-model="publishForm.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="publishForm.description" type="textarea" :rows="3" placeholder="描述商品的详细信息..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="handlePublish">发布</el-button>
      </template>
    </el-dialog>

    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import SecondhandCard from '@/components/asset/SecondhandCard.vue'
import { useSecondhandStore } from '@/stores/secondhand'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const secondhandStore = useSecondhandStore()
const authStore = useAuthStore()
const router = useRouter()

const keyword = ref('')
const filterCategory = ref('')
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(false)
const itemList = ref<any[]>([])
const categories = ref<string[]>(['数码', '家居', '服饰', '图书', '运动', '其他'])

const showPublishDialog = ref(false)
const publishing = ref(false)
const publishForm = ref({
  title: '',
  category: '',
  condition: '',
  price: 0,
  description: '',
})

function resetFilters() {
  keyword.value = ''
  filterCategory.value = ''
  page.value = 1
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    await secondhandStore.fetchItemList({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value,
      category: filterCategory.value,
    })
    itemList.value = secondhandStore.itemList
    total.value = secondhandStore.totalCount
  } finally {
    loading.value = false
  }
}

async function handlePublish() {
  if (!authStore.isLoggedIn) {
    router.push('/auth/login')
    return
  }

  if (!publishForm.value.title || !publishForm.value.category || !publishForm.value.condition || publishForm.value.price <= 0) {
    ElMessage.warning('请填写完整信息')
    return
  }

  publishing.value = true
  try {
    await secondhandStore.publishItem(publishForm.value)
    ElMessage.success('发布成功！')
    showPublishDialog.value = false
    publishForm.value = { title: '', category: '', condition: '', price: 0, description: '' }
    fetchList()
  } catch {
    ElMessage.error('发布失败，请稍后重试')
  } finally {
    publishing.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
.secondhand-list-page {
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

.filters {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md);
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-light);
  flex-wrap: wrap;

  .filter-right {
    margin-left: auto;
  }
}

.item-grid {
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
  .item-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .item-grid { grid-template-columns: 1fr; }
  .filters { flex-wrap: wrap; }
}
</style>
