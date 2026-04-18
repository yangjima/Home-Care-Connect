<template>
  <div class="property-detail-page">
    <AppHeader />

    <div class="main-content">
      <nav class="breadcrumb" aria-label="面包屑">
        <router-link to="/home">
          首页
        </router-link>
        <span class="sep">/</span>
        <router-link to="/properties">
          房源列表
        </router-link>
        <span class="sep">/</span>
        <span class="current">房源详情</span>
      </nav>

      <div v-loading="loading" class="detail-wrap">
        <template v-if="detail">
          <div class="detail-container">
            <div class="detail-left">
              <!-- 图片展示区 -->
              <div class="image-gallery card-block">
                <div class="main-image">
                  <img
                    v-if="activeImage"
                    :src="activeImage"
                    :alt="detail.title"
                  >
                  <div v-else class="image-ph">
                    暂无图片
                  </div>
                </div>
                <div v-if="galleryImages.length" class="thumbnail-list">
                  <button
                    v-for="(img, i) in galleryImages"
                    :key="`${img}-${i}`"
                    type="button"
                    class="thumbnail"
                    :class="{ active: activeImage === img }"
                    @click="activeImage = img"
                  >
                    <img :src="img" alt="">
                  </button>
                </div>
              </div>

              <!-- 房源基本信息 -->
              <div class="property-info-card card-block">
                <h1 class="property-title-detail">
                  {{ detail.title }}
                </h1>
                <div class="property-price-detail">
                  ¥{{ formatMoney(detail.rentPrice) }}<span class="price-unit">/月</span>
                </div>

                <div class="info-grid">
                  <div class="info-item">
                    <div class="info-label">
                      面积
                    </div>
                    <div class="info-value">
                      {{ formatArea(detail.area) }}
                    </div>
                  </div>
                  <div class="info-item">
                    <div class="info-label">
                      楼层
                    </div>
                    <div class="info-value">
                      {{ formatFloor(detail) }}
                    </div>
                  </div>
                  <div class="info-item">
                    <div class="info-label">
                      朝向
                    </div>
                    <div class="info-value">
                      {{ formatOrientation(detail.orientation) }}
                    </div>
                  </div>
                  <div class="info-item">
                    <div class="info-label">
                      房型
                    </div>
                    <div class="info-value">
                      {{ typeLabel(detail.propertyType) }}
                    </div>
                  </div>
                  <div class="info-item">
                    <div class="info-label">
                      装修
                    </div>
                    <div class="info-value">
                      {{ decorationLabel }}
                    </div>
                  </div>
                  <div class="info-item">
                    <div class="info-label">
                      状态
                    </div>
                    <div class="info-value" :class="statusClass(detail.status)">
                      {{ statusLabel(detail.status) }}
                    </div>
                  </div>
                </div>

                <div class="property-tags-detail">
                  <span
                    v-for="(t, i) in displayTagList"
                    :key="`tag-${i}-${t}`"
                    class="tag"
                  >{{ facilityTagEmoji(t) }}{{ stripLeadingEmoji(t) }}</span>
                </div>
              </div>

              <!-- 房源描述 -->
              <div class="description-card card-block">
                <div class="card-title">
                  📝 房源描述
                </div>
                <div class="description-text" v-html="descriptionHtml" />
              </div>

              <!-- 配套设施 -->
              <div v-if="facilityList.length" class="description-card card-block">
                <div class="card-title">
                  🏡 配套设施
                </div>
                <div class="facilities-grid">
                  <div
                    v-for="(name, i) in facilityList"
                    :key="`fac-${i}-${name}`"
                    class="facility-item"
                  >
                    <div class="facility-icon">
                      {{ facilityIcon(name) }}
                    </div>
                    <div class="facility-name">
                      {{ name }}
                    </div>
                  </div>
                </div>
              </div>

              <!-- 位置信息 -->
              <div class="description-card card-block">
                <div class="card-title">
                  📍 位置信息
                </div>
                <p class="location-line">
                  {{ detail.address }}<span v-if="detail.district"> · {{ detail.district }}</span>
                </p>
                <div class="location-map">
                  地图展示区域（可后续接入地图 API）
                </div>
              </div>

              <!-- 房源视频（设计文档支持；原型无单独区块） -->
              <div v-if="detail.videos?.length" class="description-card card-block">
                <div class="card-title">
                  🎬 房源视频
                </div>
                <div class="video-list">
                  <div
                    v-for="(video, i) in detail.videos"
                    :key="`${video}-${i}`"
                    class="video-thumb"
                    @click="openVideoDialog(video)"
                  >
                    <video :src="video" preload="metadata" muted />
                    <div class="video-play-mask">
                      <span class="play-icon">▶</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 右侧预约 -->
            <aside class="booking-sidebar">
              <div class="booking-card card-block">
                <div class="manager-info">
                  <div class="manager-avatar" aria-hidden="true">
                    👤
                  </div>
                  <div class="manager-details">
                    <div class="manager-name">
                      {{ managerDisplayName }}
                    </div>
                    <div class="manager-role">
                      房源管家 · 带看与咨询
                    </div>
                  </div>
                </div>

                <form class="booking-form" @submit.prevent="handleBookingSubmit">
                  <div class="form-group">
                    <label class="form-label" for="pv-name">您的姓名</label>
                    <input
                      id="pv-name"
                      v-model="bookingForm.visitorName"
                      type="text"
                      class="form-input"
                      placeholder="请输入您的姓名"
                      autocomplete="name"
                    >
                  </div>
                  <div class="form-group">
                    <label class="form-label" for="pv-phone">联系电话</label>
                    <input
                      id="pv-phone"
                      v-model="bookingForm.contactPhone"
                      type="tel"
                      class="form-input"
                      placeholder="请输入您的手机号"
                      autocomplete="tel"
                    >
                  </div>
                  <div class="form-group">
                    <label class="form-label" for="pv-time">预约时间</label>
                    <input
                      id="pv-time"
                      v-model="bookingForm.viewingTimeLocal"
                      type="datetime-local"
                      class="form-input"
                    >
                  </div>
                  <div class="form-group">
                    <label class="form-label" for="pv-remark">备注信息</label>
                    <textarea
                      id="pv-remark"
                      v-model="bookingForm.remark"
                      class="form-input"
                      rows="3"
                      placeholder="请输入您的需求或问题"
                    />
                  </div>
                  <button type="submit" class="btn-booking-large" :disabled="booking">
                    {{ booking ? '提交中…' : '立即预约看房' }}
                  </button>
                  <button type="button" class="btn-contact" @click="contactManager">
                    联系房源管家
                  </button>
                </form>
              </div>

              <div v-if="relatedList.length" class="related-card card-block">
                <div class="related-title">
                  🔥 相关推荐
                </div>
                <div
                  v-for="item in relatedList"
                  :key="item.id"
                  class="related-item"
                  role="button"
                  tabindex="0"
                  @click="goRelated(item.id)"
                  @keyup.enter="goRelated(item.id)"
                >
                  <div class="related-image">
                    <img v-if="relatedCover(item)" :src="relatedCover(item)!" alt="">
                  </div>
                  <div class="related-info">
                    <div class="related-name">
                      {{ item.title }}
                    </div>
                    <div class="related-price">
                      ¥{{ formatMoney(item.rentPrice) }}/月
                    </div>
                  </div>
                </div>
              </div>
            </aside>
          </div>
        </template>
        <el-empty v-else-if="!loading" description="房源不存在或已下架" />
      </div>
    </div>

    <el-dialog
      v-model="showVideoDialog"
      title="视频预览"
      width="800px"
      destroy-on-close
    >
      <div class="video-dialog-content">
        <video v-if="activeVideo" :src="activeVideo" controls autoplay />
      </div>
    </el-dialog>

    <router-link to="/ai" class="ai-assistant" title="AI助手">
      <span class="ai-icon">💬</span>
    </router-link>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/layout/AppHeader.vue'
import { usePropertyStore } from '@/stores/property'
import { useAuthStore } from '@/stores/auth'
import { getPropertyList } from '@/api/property'

const route = useRoute()
const router = useRouter()
const propertyStore = usePropertyStore()
const authStore = useAuthStore()

/** 与 Property Service `PropertyResponse` 对齐 */
interface PropertyDetailRow {
  id: number
  title?: string
  description?: string
  propertyType?: string
  rentPrice?: number
  address?: string
  district?: string
  area?: number
  layout?: string
  floor?: number
  totalFloor?: number
  orientation?: string
  facilities?: string[]
  tags?: string[]
  status?: string
  images?: string[]
  videos?: string[]
  coverImage?: string
  viewCount?: number
  ownerName?: string
  createTime?: string
}

const TYPE_LABELS: Record<string, string> = {
  apartment: '整租公寓',
  studio: '单间',
  suite: '套间',
  villa: '别墅',
}

const ORI_LABELS: Record<string, string> = {
  north: '朝北',
  south: '朝南',
  east: '朝东',
  west: '朝西',
  northeast: '东北',
  northwest: '西北',
  southeast: '东南',
  southwest: '西南',
}

const STATUS_LABELS: Record<string, string> = {
  vacant: '空置',
  occupied: '已租',
  reserved: '预定中',
}

const FACILITY_ICONS: Record<string, string> = {
  空调: '🌡️',
  热水器: '🚿',
  洗衣机: '👕',
  冰箱: '❄️',
  电视: '📺',
  床: '🛏️',
  桌椅: '🪑',
  宽带: '🌐',
  独立卫浴: '🛁',
  家具齐全: '🪑',
  近地铁: '🚇',
}

const loading = ref(false)
const detail = ref<PropertyDetailRow | null>(null)
const activeImage = ref('')
const activeVideo = ref('')
const showVideoDialog = ref(false)
const booking = ref(false)
const relatedList = ref<PropertyDetailRow[]>([])

const bookingForm = ref({
  visitorName: '',
  contactPhone: '',
  viewingTimeLocal: '',
  remark: '',
})

const galleryImages = computed(() => {
  const d = detail.value
  if (!d?.images?.length)
    return []
  return d.images
})

const facilityList = computed(() => detail.value?.facilities ?? [])

const displayTagList = computed(() => {
  const d = detail.value
  const tags = [...(d?.facilities ?? []), ...(d?.tags ?? [])]
  const seen = new Set<string>()
  const out: string[] = []
  for (const t of tags) {
    const s = (t ?? '').trim()
    if (!s || seen.has(s))
      continue
    seen.add(s)
    out.push(s)
  }
  return out
})

const decorationLabel = computed(() => {
  const tags = detail.value?.tags ?? []
  const hit = tags.find(t => /精装|简装|毛坯|装修/.test(t))
  return hit ?? '—'
})

const descriptionHtml = computed(() => {
  const raw = (detail.value?.description ?? '').trim()
  if (!raw)
    return '<p>暂无详细描述</p>'
  const paras = raw.split(/\n+/).map(p => p.trim()).filter(Boolean)
  return paras.map(p => `<p>${escapeHtml(p)}</p>`).join('')
})

const managerDisplayName = computed(() => {
  const n = detail.value?.ownerName?.trim()
  return n && n.length > 0 ? n : '房源管家'
})

function escapeHtml(s: string) {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function formatMoney(n: number | undefined) {
  if (n == null || Number.isNaN(Number(n)))
    return '—'
  return Number(n).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

function formatArea(a: number | undefined) {
  if (a == null || Number.isNaN(Number(a)))
    return '—'
  return `${Number(a)}㎡`
}

function formatFloor(row: PropertyDetailRow) {
  const f = row.floor
  const tf = row.totalFloor
  if (f != null && tf != null)
    return `${f}/${tf}层`
  if (f != null)
    return `${f}层`
  return '—'
}

function formatOrientation(code: string | undefined) {
  if (!code)
    return '—'
  return ORI_LABELS[code] ?? code
}

function typeLabel(t: string | undefined) {
  if (!t)
    return '—'
  return TYPE_LABELS[t] ?? t
}

function statusLabel(s: string | undefined) {
  if (!s)
    return '—'
  return STATUS_LABELS[s] ?? s
}

function statusClass(s: string | undefined) {
  if (s === 'vacant')
    return 'status-vacant'
  if (s === 'occupied')
    return 'status-occupied'
  return ''
}

function facilityIcon(name: string) {
  return FACILITY_ICONS[name] ?? '✓'
}

function facilityTagEmoji(name: string) {
  const icon = FACILITY_ICONS[name]
  return icon ? `${icon} ` : ''
}

function stripLeadingEmoji(s: string) {
  return s.replace(/^[\p{Emoji}\uFE0F\s]+/u, '').trim() || s
}

function relatedCover(row: PropertyDetailRow) {
  return row.coverImage || row.images?.[0] || ''
}

function openVideoDialog(video: string) {
  activeVideo.value = video
  showVideoDialog.value = true
}

function contactManager() {
  ElMessage.info('请填写预约表单留下联系方式，或在工作时间通过平台消息联系管家。')
}

function goRelated(id: number) {
  if (id === detail.value?.id)
    return
  router.push(`/properties/${id}`)
}

async function loadRelated(currentId: number) {
  try {
    const res = await getPropertyList({
      page: 1,
      size: 8,
      statuses: ['vacant'],
    })
    const rows = (res.list ?? res.records ?? []) as PropertyDetailRow[]
    relatedList.value = rows.filter(r => r.id !== currentId).slice(0, 3)
  }
  catch {
    relatedList.value = []
  }
}

async function loadDetail() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    detail.value = null
    return
  }
  loading.value = true
  try {
    await propertyStore.fetchPropertyDetail(id)
    const row = propertyStore.currentProperty as PropertyDetailRow | null
    detail.value = row
    activeImage.value = row?.coverImage || row?.images?.[0] || ''
    await loadRelated(id)
  }
  catch {
    detail.value = null
    relatedList.value = []
  }
  finally {
    loading.value = false
  }
}

function scrollToBooking() {
  nextTick(() => {
    document.querySelector('.booking-card')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    document.getElementById('pv-name')?.focus()
  })
}

async function handleBookingSubmit() {
  if (!detail.value)
    return

  if (!authStore.isLoggedIn) {
    ElMessage.info('请先登录后再预约看房')
    router.push({ name: 'Login', query: { redirect: `/properties/${detail.value.id}?book=1` } })
    return
  }

  const name = bookingForm.value.visitorName.trim()
  const phone = bookingForm.value.contactPhone.trim()
  const local = bookingForm.value.viewingTimeLocal

  if (!name) {
    ElMessage.warning('请填写您的姓名')
    return
  }
  if (!/^1\d{10}$/.test(phone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  if (!local) {
    ElMessage.warning('请选择预约时间')
    return
  }

  const viewingIso = new Date(local).toISOString()

  booking.value = true
  try {
    await propertyStore.bookViewing({
      propertyId: detail.value.id,
      viewingTime: viewingIso,
      visitorName: name,
      contactPhone: phone,
      remark: bookingForm.value.remark.trim() || undefined,
    })
    ElMessage.success('预约成功！我们会尽快与您联系')
    bookingForm.value = {
      visitorName: '',
      contactPhone: '',
      viewingTimeLocal: '',
      remark: '',
    }
  }
  catch {
    ElMessage.error('预约失败，请稍后重试')
  }
  finally {
    booking.value = false
  }
}

onMounted(async () => {
  await loadDetail()
  if (route.query.book === '1')
    scrollToBooking()
})

watch(
  () => route.params.id,
  async () => {
    await loadDetail()
    if (route.query.book === '1')
      scrollToBooking()
  },
)

watch(
  () => route.query.book,
  (v) => {
    if (v === '1')
      scrollToBooking()
  },
)
</script>

<style scoped lang="scss">
$primary: #2c7be5;
$accent: #ffa500;
$page-bg: #f5f7fa;

.property-detail-page {
  min-height: 100vh;
  background: $page-bg;
  color: #333;
}

.main-content {
  padding: 20px 40px 48px;
  max-width: 1400px;
  margin-left: auto;
  margin-right: auto;
}

.breadcrumb {
  padding: 15px 0;
  color: #666;
  font-size: 14px;

  a {
    color: $primary;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  .sep {
    margin: 0 6px;
    color: #ccc;
  }

  .current {
    color: #333;
  }
}

.detail-wrap {
  min-height: 200px;
}

.detail-container {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.detail-left {
  flex: 1;
  min-width: 0;
}

.card-block {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.image-gallery {
  padding: 20px;
  margin-bottom: 20px;
}

.main-image {
  width: 100%;
  height: 500px;
  border-radius: 8px;
  overflow: hidden;
  background: #e0e0e0;
  margin-bottom: 15px;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.image-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 16px;
}

.thumbnail-list {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}

.thumbnail {
  height: 100px;
  border-radius: 6px;
  overflow: hidden;
  border: 2px solid transparent;
  padding: 0;
  cursor: pointer;
  background: #e8e8e8;
  transition: border-color 0.2s;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  &:hover,
  &.active {
    border-color: $primary;
  }
}

.property-info-card {
  padding: 30px;
  margin-bottom: 20px;
}

.property-title-detail {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0 0 15px;
  line-height: 1.3;
  word-break: break-word;
}

.property-price-detail {
  font-size: 36px;
  color: $accent;
  font-weight: bold;
  margin-bottom: 20px;
}

.price-unit {
  font-size: 18px;
  color: #999;
  font-weight: normal;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 20px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
}

.info-item {
  text-align: center;
}

.info-label {
  color: #999;
  font-size: 14px;
  margin-bottom: 8px;
}

.info-value {
  color: #333;
  font-size: 18px;
  font-weight: 500;

  &.status-vacant {
    color: #52c41a;
  }

  &.status-occupied {
    color: #999;
  }
}

.property-tags-detail {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.tag {
  background-color: #f0f7ff;
  color: $primary;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
}

.description-card {
  padding: 30px;
  margin-bottom: 20px;
}

.card-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #333;
}

.description-text {
  line-height: 1.8;
  color: #666;
  font-size: 15px;

  :deep(p) {
    margin: 0 0 15px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.facilities-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
}

@media (max-width: 900px) {
  .facilities-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.facility-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 6px;
}

.facility-icon {
  font-size: 24px;
  line-height: 1;
}

.facility-name {
  font-size: 14px;
  color: #333;
}

.location-line {
  margin: 0 0 15px;
  color: #666;
}

.location-map {
  width: 100%;
  height: 300px;
  background-color: #e8e8e8;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 15px;
}

.video-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
}

.video-thumb {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  background: #000;
  height: 140px;

  video {
    width: 100%;
    height: 140px;
    object-fit: cover;
  }

  &:hover .video-play-mask {
    background: rgba(0, 0, 0, 0.45);
  }
}

.video-play-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
  transition: background 0.2s ease;
}

.play-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  color: #111827;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  padding-left: 3px;
}

.video-dialog-content video {
  width: 100%;
  max-height: 70vh;
  background: #000;
  border-radius: 8px;
}

.booking-sidebar {
  width: 360px;
  flex-shrink: 0;
  position: sticky;
  top: 80px;
}

@media (max-width: 1024px) {
  .detail-container {
    flex-direction: column;
  }

  .booking-sidebar {
    width: 100%;
    position: static;
  }

  .thumbnail-list {
    grid-template-columns: repeat(3, 1fr);
  }

  .main-image {
    height: 320px;
  }
}

.booking-card {
  padding: 30px;
  margin-bottom: 20px;
}

.manager-info {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.manager-avatar {
  width: 60px;
  height: 60px;
  background-color: #e8e8e8;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.manager-details {
  flex: 1;
  min-width: 0;
}

.manager-name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 5px;
}

.manager-role {
  font-size: 13px;
  color: #999;
}

.booking-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  color: #666;
}

.form-input {
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  font-family: inherit;
  resize: vertical;

  &:focus {
    border-color: $primary;
  }
}

.btn-booking-large {
  padding: 15px;
  background-color: $accent;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;

  &:hover:not(:disabled) {
    background-color: #ff8c00;
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }
}

.btn-contact {
  padding: 15px;
  background-color: #fff;
  color: $primary;
  border: 1px solid $primary;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background-color: #f0f7ff;
  }
}

.related-card {
  padding: 20px;
}

.related-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 15px;
}

.related-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  align-items: center;

  &:last-child {
    border-bottom: none;
  }

  &:hover .related-name {
    color: $primary;
  }
}

.related-image {
  width: 80px;
  height: 60px;
  background-color: #e8e8e8;
  border-radius: 6px;
  flex-shrink: 0;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.related-info {
  flex: 1;
  min-width: 0;
}

.related-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 5px;
  word-break: break-word;
}

.related-price {
  font-size: 16px;
  color: $accent;
  font-weight: 500;
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
  transition: transform 0.2s, box-shadow 0.2s;
  z-index: 900;
  text-decoration: none;

  &:hover {
    transform: scale(1.06);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.28);
  }
}

.ai-icon {
  font-size: 28px;
  color: #fff;
}
</style>
