<template>
  <div class="service-list-page">
    <AppHeader />
    <main class="main-content">
      <!-- 页面标题（对齐原型渐变头） -->
      <section class="page-header">
        <div class="page-title">💼 社区服务</div>
        <div class="page-subtitle">专业服务团队 · 标准化价格 · 快速响应</div>
      </section>

      <!-- 分类标签 -->
      <section class="category-tabs">
        <div class="tabs-container">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            class="tab-item"
            :class="{ active: activeCategory === tab.key }"
            @click="activeCategory = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>
      </section>

      <!-- 服务网格 -->
      <div v-loading="loadingTypes" class="service-grid">
        <article
          v-for="item in filteredTypes"
          :key="item.id"
          class="service-card"
          @click="$router.push(`/services/${item.id}`)"
        >
          <div class="service-header">
            <div class="service-icon-large">{{ serviceEmoji(item.name) }}</div>
            <div class="service-title-info">
              <div class="service-name-large">{{ item.name }}</div>
              <div class="service-category">{{ categoryLabel(item.category) }}</div>
            </div>
          </div>
          <p class="service-description">{{ item.description || '专业社区上门服务，欢迎预约。' }}</p>
          <div class="service-features">
            <span v-for="(tag, i) in featureTags(item.category)" :key="i" class="feature-tag">{{ tag }}</span>
          </div>
          <div class="service-footer">
            <div>
              <div class="service-price-large">¥{{ formatPrice(item.price) }}起</div>
              <div class="price-note">{{ priceNote(item.unit) }}</div>
            </div>
            <button
              type="button"
              class="btn-book-service"
              @click.stop="$router.push(`/services/${item.id}`)"
            >
              立即预约
            </button>
          </div>
        </article>
        <el-empty v-if="!loadingTypes && filteredTypes.length === 0" description="暂无服务" />
      </div>

      <!-- 服务人员 -->
      <section v-loading="loadingStaff" class="staff-section">
        <h2 class="section-title">👷 优秀服务人员</h2>
        <div class="staff-grid">
          <div v-for="s in staffList" :key="s.id" class="staff-card">
            <div class="staff-avatar">
              <img v-if="s.avatar" :src="s.avatar" alt="" />
              <span v-else>👨</span>
            </div>
            <div class="staff-name">{{ s.name }}</div>
            <div class="staff-skills">{{ s.skillsLabel }}</div>
            <div class="staff-rating">
              <span class="stars">{{ starText(s.rating) }}</span>
              <span>{{ formatRating(s.rating) }}</span>
            </div>
            <div class="staff-orders">已完成 {{ s.completedOrders }} 单</div>
          </div>
        </div>
        <el-empty v-if="!loadingStaff && staffList.length === 0" description="暂无服务人员" />
      </section>

      <!-- 服务流程 -->
      <section class="process-section">
        <h2 class="section-title">📋 服务流程</h2>
        <div class="process-steps">
          <div class="process-step">
            <div class="step-number">1</div>
            <div class="step-title">选择服务</div>
            <div class="step-description">浏览服务列表，选择您需要的服务类型</div>
          </div>
          <div class="process-step">
            <div class="step-number">2</div>
            <div class="step-title">提交订单</div>
            <div class="step-description">填写服务地址、时间等信息，提交预约</div>
          </div>
          <div class="process-step">
            <div class="step-number">3</div>
            <div class="step-title">师傅接单</div>
            <div class="step-description">系统派单，专业师傅接单并联系您</div>
          </div>
          <div class="process-step">
            <div class="step-number">4</div>
            <div class="step-title">完成服务</div>
            <div class="step-description">师傅上门服务，完成后确认并评价</div>
          </div>
        </div>
      </section>
    </main>

    <button type="button" class="ai-assistant" title="AI助手" @click="$router.push('/ai')">
      <span class="ai-icon">💬</span>
    </button>

    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { getStaffList } from '@/api/service'
import { useServiceStore } from '@/stores/service'
import type { ServiceStaffPublic, ServiceType } from '@/types'

const serviceStore = useServiceStore()
const loadingTypes = ref(false)
const loadingStaff = ref(false)
const serviceTypes = ref<ServiceType[]>([])
const staffList = ref<ServiceStaffPublic[]>([])

const tabs = [
  { key: 'all', label: '全部服务' },
  { key: 'cleaning', label: '保洁服务' },
  { key: 'repair', label: '维修服务' },
  { key: 'other', label: '其他服务' },
] as const

const activeCategory = ref<(typeof tabs)[number]['key']>('all')

const filteredTypes = computed(() => {
  if (activeCategory.value === 'all') {
    return serviceTypes.value
  }
  return serviceTypes.value.filter((t) => t.category === activeCategory.value)
})

const NAME_EMOJI: Record<string, string> = {
  家庭保洁: '🧹',
  日常保洁: '🧹',
  开荒保洁: '🧹',
  家电维修: '🔧',
  水管维修: '🚰',
  开锁换锁: '🔑',
  墙面粉刷: '🎨',
  玻璃清洁: '🪟',
}

function serviceEmoji(name: string) {
  return NAME_EMOJI[name] || '🛠️'
}

function categoryLabel(cat?: string) {
  const m: Record<string, string> = {
    cleaning: '保洁服务',
    repair: '维修服务',
    other: '其他服务',
  }
  return cat ? m[cat] || '社区服务' : '社区服务'
}

function featureTags(cat?: string): string[] {
  const m: Record<string, string[]> = {
    cleaning: ['专业团队', '环保清洁', '快速响应'],
    repair: ['持证上岗', '收费透明', '质保服务'],
    other: ['持证上岗', '快速上门', '安全可靠'],
  }
  return m[cat || 'other'] ?? m.other
}

function formatPrice(p: number) {
  const n = Number(p)
  if (Number.isNaN(n)) {
    return '0'
  }
  return n % 1 === 0 ? String(Math.round(n)) : n.toFixed(2)
}

function priceNote(unit: string) {
  if (!unit) {
    return '按次计费'
  }
  if (unit === '次') {
    return '按次计费'
  }
  return `按${unit}计费`
}

function formatRating(r: number) {
  return Number(r).toFixed(1)
}

function starText(rating: number) {
  const n = Math.min(5, Math.max(0, Math.round(Number(rating))))
  return '⭐'.repeat(n || 5)
}

async function loadTypes() {
  loadingTypes.value = true
  try {
    await serviceStore.fetchServiceTypes()
    serviceTypes.value = serviceStore.serviceTypes as ServiceType[]
  } finally {
    loadingTypes.value = false
  }
}

async function loadStaff() {
  loadingStaff.value = true
  try {
    staffList.value = await getStaffList()
  } catch {
    staffList.value = []
  } finally {
    loadingStaff.value = false
  }
}

onMounted(() => {
  loadTypes()
  loadStaff()
})
</script>

<style scoped lang="scss">
.service-list-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  color: #333;
}

.main-content {
  flex: 1;
  padding: 20px 40px 40px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 40px;
  margin-bottom: 30px;
  color: #fff;
  text-align: center;
}

.page-title {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 10px;
}

.page-subtitle {
  font-size: 16px;
  opacity: 0.9;
}

.category-tabs {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.tabs-container {
  display: flex;
  gap: 15px;
  justify-content: center;
  flex-wrap: wrap;
}

.tab-item {
  padding: 12px 30px;
  border: 1px solid #e0e0e0;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 15px;
  color: #666;
  background: #fff;

  &:hover {
    border-color: #2c7be5;
    color: #2c7be5;
  }

  &.active {
    background-color: #2c7be5;
    color: #fff;
    border-color: #2c7be5;
  }
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 25px;
  margin-bottom: 40px;
  min-height: 120px;
}

.service-card {
  background: #fff;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
  cursor: pointer;
  display: flex;
  flex-direction: column;

  &:hover {
    box-shadow: 0 6px 24px rgba(0, 0, 0, 0.15);
    transform: translateY(-5px);
  }
}

.service-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.service-icon-large {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  flex-shrink: 0;
}

.service-title-info {
  flex: 1;
  min-width: 0;
}

.service-name-large {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.service-category {
  font-size: 13px;
  color: #999;
}

.service-description {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 20px;
  flex: 1;
}

.service-features {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.feature-tag {
  background-color: #f0f7ff;
  color: #2c7be5;
  padding: 5px 12px;
  border-radius: 4px;
  font-size: 12px;
}

.service-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.service-price-large {
  font-size: 24px;
  color: #ffa500;
  font-weight: bold;
}

.price-note {
  font-size: 12px;
  color: #999;
}

.btn-book-service {
  padding: 10px 24px;
  background-color: #2c7be5;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;

  &:hover {
    background-color: #1a5bb8;
  }
}

.staff-section {
  background: #fff;
  border-radius: 12px;
  padding: 30px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 25px;
  color: #333;
}

.staff-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.staff-card {
  text-align: center;
  padding: 20px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  transition: all 0.3s;

  &:hover {
    border-color: #2c7be5;
    box-shadow: 0 4px 16px rgba(44, 123, 229, 0.15);
  }
}

.staff-avatar {
  width: 80px;
  height: 80px;
  background-color: #e0e0e0;
  border-radius: 50%;
  margin: 0 auto 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.staff-name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
  color: #333;
}

.staff-skills {
  font-size: 13px;
  color: #666;
  margin-bottom: 10px;
}

.staff-rating {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 14px;
  color: #ffa500;
  flex-wrap: wrap;

  .stars {
    letter-spacing: -2px;
  }
}

.staff-orders {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.process-section {
  background: #fff;
  border-radius: 12px;
  padding: 30px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.process-steps {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-top: 25px;
}

.process-step {
  text-align: center;
}

.step-number {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  margin: 0 auto 15px;
}

.step-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
  color: #333;
}

.step-description {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
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
  transition: all 0.3s;
  z-index: 999;
  border: none;
  padding: 0;

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
  }
}

.ai-icon {
  font-size: 28px;
  color: #fff;
}

@media (max-width: 1200px) {
  .service-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .staff-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .process-steps {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 16px;
  }

  .page-title {
    font-size: 24px;
  }

  .service-grid {
    grid-template-columns: 1fr;
  }

  .staff-grid {
    grid-template-columns: 1fr;
  }

  .process-steps {
    grid-template-columns: 1fr;
  }
}
</style>
