<template>
  <div class="secondhand-page">
    <AppHeader />
    <main class="content">
      <div class="tab-bar">
        <div
          v-for="tab in tabs"
          :key="tab.value || 'all'"
          class="tab-item"
          :class="{ active: activeCategory === tab.value }"
          @click="onSelectTab(tab.value)"
        >
          {{ tab.label }}
        </div>
      </div>

      <div class="publish-bar">
        <div class="publish-info">
          当前共有 <span>{{ summary.totalOnSale.toLocaleString('zh-CN') }}</span> 件跳蚤市场物品在售 ·
          <span>本周新增 {{ summary.newThisWeek.toLocaleString('zh-CN') }}</span> 件
        </div>
        <button type="button" class="btn-publish" @click="openPublish">➕ 发布闲置</button>
      </div>

      <div class="search-wrap">
        <input
          v-model.trim="keywordInput"
          type="text"
          class="search-section-input"
          placeholder="🔍 搜索跳蚤市场商品..."
          @keyup.enter="runSearch"
          @input="scheduleSearch"
        />
      </div>

      <div v-loading="loading" class="masonry-wrap">
        <div class="masonry">
          <SecondhandCard v-for="row in itemList" :key="row.id" :item="row" />
        </div>
        <el-empty v-if="!loading && itemList.length === 0" description="暂无闲置商品" />
      </div>

      <div v-if="total > pageSize" class="pagination">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="fetchList"
        />
      </div>
    </main>

    <el-dialog v-model="showPublishDialog" title="发布闲置" width="480px" destroy-on-close>
      <el-form :model="publishForm" label-width="96px">
        <el-form-item label="商品标题" required>
          <el-input v-model="publishForm.title" placeholder="简洁描述您的商品" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="商品分类" required>
          <el-select v-model="publishForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="t in publishCategories" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="新旧程度" required>
          <el-select v-model="publishForm.condition" placeholder="请选择" style="width: 100%">
            <el-option label="几乎全新" value="like_new" />
            <el-option label="良好" value="good" />
            <el-option label="一般" value="fair" />
          </el-select>
        </el-form-item>
        <el-form-item label="现价" required>
          <el-input-number v-model="publishForm.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="publishForm.originalPrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="自提地点">
          <el-input v-model="publishForm.location" placeholder="例如：朝阳区·望京" maxlength="200" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="publishForm.contact" placeholder="选填：手机或微信号" maxlength="100" />
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

    <router-link to="/ai" class="ai-assistant" title="AI助手">
      <span class="ai-icon">💬</span>
    </router-link>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import SecondhandCard from '@/components/asset/SecondhandCard.vue'
import { useSecondhandStore } from '@/stores/secondhand'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'

const secondhandStore = useSecondhandStore()
const authStore = useAuthStore()
const router = useRouter()
const { summary } = storeToRefs(secondhandStore)

const tabs = [
  { label: '🏠 全部', value: '' },
  { label: '🪑 家具家居', value: '家具家居' },
  { label: '📱 数码电器', value: '数码电器' },
  { label: '👗 服饰箱包', value: '服饰箱包' },
  { label: '📚 书籍文具', value: '书籍文具' },
  { label: '🏃 运动户外', value: '运动户外' },
  { label: '🌱 绿植宠物', value: '绿植宠物' },
] as const

const publishCategories = computed(() => tabs.map((t) => t.value).filter(Boolean) as string[])

const activeCategory = ref('')
const keywordInput = ref('')
const keyword = ref('')
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(false)
const itemList = ref<import('@/types').SecondhandItem[]>([])

let searchTimer: ReturnType<typeof setTimeout> | null = null

const showPublishDialog = ref(false)
const publishing = ref(false)
const publishForm = ref({
  title: '',
  category: '',
  condition: '',
  price: 0,
  originalPrice: undefined as number | undefined,
  location: '',
  contact: '',
  description: '',
})

function onSelectTab(value: string) {
  activeCategory.value = value
  page.value = 1
  fetchList()
}

function scheduleSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => runSearch(), 350)
}

function runSearch() {
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
  keyword.value = keywordInput.value
  page.value = 1
  fetchList()
}

function openPublish() {
  if (!authStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: '/secondhand' } })
    return
  }
  showPublishDialog.value = true
}

async function fetchList() {
  loading.value = true
  try {
    await secondhandStore.fetchItemList({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      category: activeCategory.value || undefined,
    })
    itemList.value = secondhandStore.itemList
    total.value = secondhandStore.totalCount
  } catch {
    itemList.value = []
    total.value = 0
    ElMessage.error('跳蚤市场数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function handlePublish() {
  if (!publishForm.value.title || !publishForm.value.category || !publishForm.value.condition || publishForm.value.price <= 0) {
    ElMessage.warning('请填写完整信息')
    return
  }

  publishing.value = true
  try {
    const payload: Parameters<typeof secondhandStore.publishItem>[0] = {
      title: publishForm.value.title,
      category: publishForm.value.category,
      condition: publishForm.value.condition,
      price: publishForm.value.price,
      description: publishForm.value.description || undefined,
      contact: publishForm.value.contact || undefined,
      location: publishForm.value.location || undefined,
    }
    if (publishForm.value.originalPrice != null && publishForm.value.originalPrice > 0) {
      payload.originalPrice = publishForm.value.originalPrice
    }
    await secondhandStore.publishItem(payload)
    ElMessage.success('已提交上架审核，请等待店长或超级管理员审批')
    showPublishDialog.value = false
    publishForm.value = {
      title: '',
      category: '',
      condition: '',
      price: 0,
      originalPrice: undefined,
      location: '',
      contact: '',
      description: '',
    }
    await secondhandStore.fetchSummary()
    await fetchList()
  } catch {
    ElMessage.error('发布失败，请稍后重试')
  } finally {
    publishing.value = false
  }
}

onMounted(async () => {
  await secondhandStore.fetchSummary()
  await fetchList()
})
</script>

<style scoped lang="scss">
$primary: #2c7be5;
$page-bg: #f0f2f5;

.secondhand-page {
  min-height: 100vh;
  background: $page-bg;
  color: #333;
  padding-bottom: 48px;
}

.content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px 20px 32px;
}

.tab-bar {
  background: #fff;
  border-radius: 12px;
  padding: 0;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  overflow: hidden;
  flex-wrap: wrap;
}

.tab-item {
  flex: 1;
  min-width: 96px;
  padding: 14px 8px;
  text-align: center;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  transition: all 0.3s;
  user-select: none;

  &:hover {
    background: #f8f9fa;
  }

  &.active {
    color: $primary;
    border-bottom-color: $primary;
    font-weight: 600;
  }
}

.publish-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  gap: 12px;
  flex-wrap: wrap;
}

.publish-info {
  font-size: 14px;
  color: #999;

  span {
    color: $primary;
    font-weight: 600;
  }
}

.btn-publish {
  padding: 8px 20px;
  background: $primary;
  color: #fff;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s;
  white-space: nowrap;

  &:hover {
    background: #1a6ad8;
  }
}

.search-wrap {
  margin-bottom: 20px;
  padding: 10px 15px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.search-section-input {
  width: 100%;
  max-width: none;
  padding: 12px 20px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.3s;

  &:focus {
    border-color: $primary;
  }
}

.masonry-wrap {
  min-height: 240px;
}

.masonry {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 24px 0 8px;
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
  transition: transform 0.3s, box-shadow 0.3s;
  z-index: 200;
  text-decoration: none;

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
  }
}

.ai-icon {
  font-size: 28px;
  color: #fff;
  line-height: 1;
}

@media (max-width: 1024px) {
  .masonry {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .masonry {
    grid-template-columns: 1fr;
  }
}
</style>
