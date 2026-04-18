<template>
  <div class="secondhand-detail-page">
    <AppHeader />
    <main v-loading="loading" class="main">
      <div class="toolbar container">
        <el-button text type="primary" @click="$router.push('/secondhand')">← 返回跳蚤市场</el-button>
      </div>
      <div v-if="item" class="container card">
        <div class="hero">
          <img v-if="cover" :src="cover" :alt="item.title" class="hero-img" />
          <div v-else class="hero-placeholder">{{ emoji }}</div>
          <span class="badge">{{ conditionLabel }}</span>
        </div>
        <div class="body">
          <h1 class="title">{{ item.title }}</h1>
          <div class="price-row">
            <span class="price">¥{{ formatMoney(item.price) }}</span>
            <span v-if="item.originalPrice && item.originalPrice > item.price" class="orig">
              ¥{{ formatMoney(item.originalPrice) }}
            </span>
          </div>
          <div v-if="item.location" class="loc">📍 {{ item.location }}</div>
          <div class="seller">
            <span class="avatar">{{ sellerInitial }}</span>
            <span class="name">{{ item.userName || '居友' }}</span>
            <span v-if="item.integrityTag" class="tag">诚信</span>
          </div>
          <p v-if="item.description" class="desc">{{ item.description }}</p>
        </div>
      </div>
      <el-empty v-else-if="!loading" description="商品不存在或已下架" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import { useSecondhandStore } from '@/stores/secondhand'
import type { SecondhandItem } from '@/types'

const route = useRoute()
const store = useSecondhandStore()

const loading = ref(true)
const item = ref<SecondhandItem | null>(null)

const CATEGORY_EMOJI: Record<string, string> = {
  家具家居: '🪑',
  数码电器: '📱',
  服饰箱包: '👗',
  书籍文具: '📚',
  运动户外: '🏃',
  绿植宠物: '🌱',
}

const emoji = computed(() => (item.value ? CATEGORY_EMOJI[item.value.category] || '📦' : '📦'))

const imagesArr = computed((): string[] => {
  if (!item.value?.images) return []
  const im = item.value.images as unknown
  if (Array.isArray(im)) return im
  if (typeof im === 'string' && im.startsWith('[')) {
    try {
      return JSON.parse(im) as string[]
    } catch {
      return []
    }
  }
  return []
})

const cover = computed(() => item.value?.image || imagesArr.value[0] || '')

const conditionLabel = computed(() => {
  const c = item.value?.condition || ''
  const map: Record<string, string> = { like_new: '9成新', good: '8成新', fair: '7成新' }
  return map[c] || c
})

const sellerInitial = computed(() => {
  const n = item.value?.userName || ''
  return n.charAt(0) || '居'
})

function formatMoney(v: number) {
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    loading.value = false
    return
  }
  loading.value = true
  await store.fetchItemDetail(id)
  item.value = store.currentItem
  loading.value = false
})
</script>

<style scoped lang="scss">
.secondhand-detail-page {
  min-height: 100vh;
  background: #f0f2f5;
}

.main {
  padding-bottom: 40px;
}

.container {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 20px;
}

.toolbar {
  padding: 12px 0;
}

.card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.hero {
  position: relative;
  height: 280px;
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
}

.hero-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 72px;
}

.badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 4px;
}

.body {
  padding: 20px 24px 28px;
}

.title {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin-bottom: 12px;
  line-height: 1.4;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 8px;
}

.price {
  font-size: 24px;
  font-weight: 700;
  color: #ff4d4f;
}

.orig {
  font-size: 14px;
  color: #bbb;
  text-decoration: line-through;
}

.loc {
  font-size: 13px;
  color: #999;
  margin-bottom: 12px;
}

.seller {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.name {
  font-size: 14px;
  color: #666;
}

.tag {
  font-size: 10px;
  background: #e8f5e9;
  color: #4caf50;
  padding: 2px 8px;
  border-radius: 3px;
}

.desc {
  font-size: 14px;
  color: #555;
  line-height: 1.7;
  white-space: pre-wrap;
}
</style>
