<template>
  <div class="purchase-page">
    <AppHeader />

    <main class="main">
      <div class="content">
        <!-- 横幅（原型 01-purchase.html） -->
        <section class="banner">
          <div class="banner-text">
            <h2>📦 社区本地商城</h2>
            <p>办公用品、生活物资大批量采购，邻里共享优惠价</p>
            <button type="button" class="banner-btn" @click="onGuideClick">查看商城指南 →</button>
          </div>
          <div class="banner-img" aria-hidden="true">🛒</div>
          <div class="banner-search-box">
            <div class="banner-search-title">🔍 搜索商品</div>
            <form class="banner-search-form" @submit.prevent="runSearch">
              <span class="banner-search-icon" aria-hidden="true">⌕</span>
              <input
                v-model="searchInput"
                type="search"
                class="banner-search-input"
                placeholder="搜索办公用品、清洁用品..."
                autocomplete="off"
              />
              <button type="submit" class="banner-search-btn">搜索</button>
            </form>
            <div class="banner-hot-search">
              <button type="button" class="hot-tag hot" @click="applyHotSearch('sales')">🔥 销量优先</button>
              <button type="button" class="hot-tag" @click="applyHotKeyword('办公用品')">办公用品</button>
              <button type="button" class="hot-tag" @click="applyHotKeyword('清洁用品')">清洁用品</button>
              <button type="button" class="hot-tag" @click="applyHotKeyword('维修工具')">维修工具</button>
            </div>
          </div>
        </section>

        <!-- 分类 -->
        <div class="category-bar">
          <span class="category-label">分类：</span>
          <button
            v-for="cat in categoryTabs"
            :key="cat.value || 'all'"
            type="button"
            class="cat-btn"
            :class="{ active: selectedCategory === cat.value }"
            @click="selectCategory(cat.value)"
          >
            {{ cat.label }}
          </button>
        </div>

        <!-- 快捷筛选 -->
        <div class="filter-bar">
          <span class="filter-label">热门：</span>
          <button
            type="button"
            class="filter-tag"
            :class="{ hot: sortMode === 'sales' }"
            @click="setSort('sales')"
          >
            🔥 销量优先
          </button>
          <button
            type="button"
            class="filter-tag"
            :class="{ hot: sortMode === 'price_asc' }"
            @click="setSort('price_asc')"
          >
            价格最低
          </button>
          <button
            type="button"
            class="filter-tag"
            :class="{ hot: sortMode === 'newest' }"
            @click="setSort('newest')"
          >
            最新上架
          </button>
          <button
            type="button"
            class="filter-tag"
            :class="{ hot: sortMode === 'in_stock' }"
            @click="setSort('in_stock')"
          >
            有现货
          </button>
        </div>

        <div v-loading="loading" class="product-area">
          <div class="product-grid">
            <article v-for="item in productList" :key="item.id" class="product-card">
              <div class="product-img">
                <img
                  v-if="coverUrl(item)"
                  :src="coverUrl(item)!"
                  :alt="item.name"
                  class="product-img-photo"
                />
                <span v-else class="product-img-emoji" aria-hidden="true">{{ categoryEmoji(item.category) }}</span>
                <span v-if="item.productTag" class="product-tag">{{ item.productTag }}</span>
              </div>
              <div class="product-body">
                <h3 class="product-name">{{ item.name }}</h3>
                <div class="product-meta">
                  <div>
                    <span class="product-price">¥{{ formatPrice(item.price) }}</span>
                    <small class="product-unit">{{ item.unit || '/件' }}</small>
                  </div>
                  <div class="product-sales">已售 {{ item.salesCount ?? 0 }}</div>
                </div>
              </div>
              <div class="product-footer">
                <button type="button" class="btn-add" @click="addToList(item)">➕ 加入清单</button>
                <button type="button" class="btn-cart" title="加入购物车" @click="addToCartQuick(item)">🛒</button>
              </div>
            </article>
          </div>
          <el-empty v-if="!loading && productList.length === 0" description="暂无商品" />
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
      </div>
    </main>

    <button type="button" class="ai-assistant" title="AI 助手" @click="$router.push('/ai')">
      <span aria-hidden="true">💬</span>
    </button>

    <button type="button" class="float-cart" title="购物车" @click="openCartHint">
      <span aria-hidden="true">🛒</span>
      <span v-if="cartCount > 0" class="float-cart-badge">{{ cartCount > 99 ? '99+' : cartCount }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/layout/AppHeader.vue'
import { getProcurementList } from '@/api/asset'
import type { ProcurementProduct } from '@/types'

const CART_STORAGE_KEY = 'homecare_mall_cart'

const loading = ref(false)
const page = ref(1)
const pageSize = ref(8)
const total = ref(0)
const keyword = ref('')
const searchInput = ref('')
const selectedCategory = ref('')
const sortMode = ref<'sales' | 'price_asc' | 'newest' | 'in_stock' | ''>('')

const categoryTabs = [
  { label: '全部商品', value: '' },
  { label: '办公用品', value: '办公用品' },
  { label: '清洁用品', value: '清洁用品' },
  { label: '维修工具', value: '维修工具' },
  { label: '生活物资', value: '生活物资' },
  { label: '安防设备', value: '安防设备' },
  { label: '绿植盆栽', value: '绿植盆栽' },
]

const productList = ref<ProcurementProduct[]>([])
const cartCount = ref(0)

function loadCartCount() {
  try {
    const raw = localStorage.getItem(CART_STORAGE_KEY)
    const arr = raw ? (JSON.parse(raw) as { qty: number }[]) : []
    cartCount.value = arr.reduce((s, i) => s + (i.qty || 0), 0)
  } catch {
    cartCount.value = 0
  }
}

function persistCartItem(item: ProcurementProduct, qty: number) {
  type Line = { id: number; name: string; price: number; unit?: string; qty: number }
  let lines: Line[] = []
  try {
    const raw = localStorage.getItem(CART_STORAGE_KEY)
    lines = raw ? (JSON.parse(raw) as Line[]) : []
  } catch {
    lines = []
  }
  const existing = lines.find((l) => l.id === item.id)
  if (existing) {
    existing.qty += qty
  } else {
    lines.push({
      id: item.id,
      name: item.name,
      price: Number(item.price),
      unit: item.unit,
      qty,
    })
  }
  localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(lines))
  loadCartCount()
}

function formatPrice(p: number | string) {
  const n = Number(p)
  if (Number.isNaN(n)) return String(p)
  const rounded = Math.round(n * 100) / 100
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(2)
}

function categoryEmoji(cat: string) {
  const map: Record<string, string> = {
    办公用品: '📎',
    清洁用品: '🧹',
    维修工具: '🔧',
    绿植盆栽: '🌿',
    安防设备: '📹',
    生活物资: '🪑',
    家具: '🪑',
    家电: '🔌',
    日用品: '🧴',
  }
  return map[cat] || '📦'
}

function parseImages(raw: ProcurementProduct['images']): string[] {
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

function coverUrl(item: ProcurementProduct) {
  if (item.image) return item.image
  const arr = parseImages(item.images)
  return arr[0] || ''
}

async function fetchList() {
  loading.value = true
  try {
    const result = await getProcurementList({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      category: selectedCategory.value || undefined,
      sort: sortMode.value || undefined,
    })
    const rows = (result.list ?? result.records ?? []) as ProcurementProduct[]
    productList.value = rows
    total.value = Number(result.total ?? 0)
  } catch {
    productList.value = []
    total.value = 0
    ElMessage.error('商城数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function runSearch() {
  keyword.value = searchInput.value.trim()
  page.value = 1
  fetchList()
}

function applyHotSearch(sort: 'sales') {
  sortMode.value = sort
  page.value = 1
  fetchList()
}

function applyHotKeyword(k: string) {
  keyword.value = k
  searchInput.value = k
  page.value = 1
  fetchList()
}

function selectCategory(value: string) {
  selectedCategory.value = value
  page.value = 1
  fetchList()
}

function setSort(mode: typeof sortMode.value) {
  if (sortMode.value === mode) {
    sortMode.value = ''
  } else {
    sortMode.value = mode
  }
  page.value = 1
  fetchList()
}

function addToList(item: ProcurementProduct) {
  persistCartItem(item, 1)
  ElMessage.success(`「${item.name}」已加入清单`)
}

function addToCartQuick(item: ProcurementProduct) {
  persistCartItem(item, 1)
  ElMessage.success('已加入购物车')
}

function openCartHint() {
  if (cartCount.value === 0) {
    ElMessage.info('购物车还是空的，先点「加入清单」或 🛒 添加商品吧')
    return
  }
  ElMessage.success(`购物车内共 ${cartCount.value} 件商品（本地暂存，结算功能敬请期待）`)
}

function onGuideClick() {
  ElMessage.info('商城指南：批量采购请联系门店店长；更多说明即将上线。')
}

onMounted(() => {
  document.title = '本地商城 - 居服通'
  searchInput.value = keyword.value
  loadCartCount()
  fetchList()
})
</script>

<style scoped lang="scss">
/* 色板与原型 01-purchase.html 对齐 */
$primary: #2c7be5;
$banner-from: #667eea;
$banner-to: #764ba2;
$danger: #ff4d4f;
$page-bg: #f0f2f5;

.purchase-page {
  min-height: 100vh;
  background: $page-bg;
  color: #333;
  padding-bottom: 100px;
}

.main {
  padding-top: 8px;
}

.content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 25px 20px;
}

.banner {
  background: linear-gradient(135deg, $banner-from 0%, $banner-to 100%);
  border-radius: 12px;
  padding: 30px 35px;
  margin-bottom: 25px;
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.banner-text {
  flex: 1 1 200px;
  min-width: 200px;

  h2 {
    font-size: 22px;
    color: #fff;
    margin: 0 0 6px;
    font-weight: 700;
  }

  p {
    color: rgba(255, 255, 255, 0.85);
    font-size: 14px;
    margin: 0;
  }
}

.banner-btn {
  margin-top: 12px;
  padding: 10px 24px;
  background: #fff;
  color: $banner-to;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  }
}

.banner-img {
  font-size: 80px;
  line-height: 1;
  flex-shrink: 0;
}

.banner-search-box {
  flex: 1 1 280px;
  max-width: 360px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.banner-search-title {
  color: rgba(255, 255, 255, 0.95);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  gap: 6px;

  &::before {
    content: '';
    display: inline-block;
    width: 3px;
    height: 14px;
    background: #fff;
    border-radius: 2px;
  }
}

.banner-search-form {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 50px;
  padding: 5px 5px 5px 18px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.banner-search-form:focus-within {
  box-shadow: 0 6px 30px rgba(44, 123, 229, 0.3), 0 0 0 2px rgba(44, 123, 229, 0.35);
}

.banner-search-icon {
  color: $banner-to;
  font-size: 18px;
  margin-right: 8px;
  opacity: 0.75;
}

.banner-search-input {
  flex: 1;
  min-width: 0;
  padding: 8px 0;
  border: none;
  outline: none;
  font-size: 14px;
  color: #333;
  background: transparent;
}

.banner-search-input::placeholder {
  color: #aaa;
}

.banner-search-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, $banner-from, $banner-to);
  color: #fff;
  border: none;
  border-radius: 50px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.95;
  }
}

.banner-hot-search {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 0 4px;
}

.hot-tag {
  padding: 3px 10px;
  background: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.88);
  border-radius: 50px;
  font-size: 12px;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.22);
  transition: background 0.2s, color 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.28);
    color: #fff;
  }

  &.hot {
    background: rgba(255, 77, 79, 0.35);
    border-color: rgba(255, 77, 79, 0.55);
    color: #fff;
  }
}

.category-bar {
  background: #fff;
  border-radius: 12px;
  padding: 15px 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  gap: 10px;
  align-items: center;
  overflow-x: auto;
}

.category-label {
  font-size: 13px;
  color: #999;
  margin-right: 4px;
  flex-shrink: 0;
}

.cat-btn {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid #e0e0e0;
  background: #fff;
  color: #666;
  white-space: nowrap;
  transition: all 0.2s;

  &:hover {
    border-color: $primary;
    color: $primary;
  }

  &.active {
    background: $primary;
    color: #fff;
    border-color: $primary;
  }
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  align-items: center;
}

.filter-label {
  font-size: 13px;
  color: #999;
}

.filter-tag {
  padding: 5px 14px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: $primary;
    color: $primary;
  }

  &.hot {
    border-color: $danger;
    color: $danger;
  }
}

.product-area {
  min-height: 200px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

@media (max-width: 1100px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .banner {
    padding: 22px 18px;
  }
}

@media (max-width: 480px) {
  .product-grid {
    grid-template-columns: 1fr;
  }
}

.product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: transform 0.25s, box-shadow 0.25s;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  }
}

.product-img {
  width: 100%;
  height: 160px;
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.product-img-photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-img-emoji {
  font-size: 56px;
  line-height: 1;
}

.product-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: $danger;
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}

.product-body {
  padding: 14px;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.product-price {
  font-size: 18px;
  font-weight: bold;
  color: $danger;
}

.product-unit {
  font-size: 12px;
  font-weight: normal;
  color: #999;
  margin-left: 4px;
}

.product-sales {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
}

.product-footer {
  padding: 10px 14px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 8px;
}

.btn-add {
  flex: 1;
  padding: 7px;
  background: #fff3e0;
  color: #ff9800;
  border-radius: 6px;
  font-size: 13px;
  border: none;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: #ffe0b2;
  }
}

.btn-cart {
  padding: 7px 12px;
  background: #fff;
  color: $primary;
  border-radius: 6px;
  font-size: 13px;
  border: 1px solid $primary;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: #f0f7ff;
  }
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 28px 0 8px;
}

.ai-assistant {
  position: fixed;
  bottom: 100px;
  right: 30px;
  width: 56px;
  height: 56px;
  border: none;
  background: linear-gradient(135deg, $banner-from, $banner-to);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  transition: transform 0.2s;
  z-index: 200;

  &:hover {
    transform: scale(1.08);
  }
}

.float-cart {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 56px;
  height: 56px;
  border: none;
  background: linear-gradient(135deg, $banner-from, $banner-to);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  transition: transform 0.2s;
  z-index: 199;

  &:hover {
    transform: scale(1.08);
  }
}

.float-cart-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  background: $danger;
  color: #fff;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 50%;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #fff;
  box-sizing: border-box;
}
</style>
