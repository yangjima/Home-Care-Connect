<template>
  <div class="my-properties-page">
    <UserSubpageHeader title="我的房源" subtitle="管理与发布房源">
      <template #actions>
        <el-button type="primary" @click="openPublishDialog">
          <el-icon><Plus /></el-icon> 发布房源
        </el-button>
      </template>
    </UserSubpageHeader>

    <div v-loading="loading" class="property-list">
      <div v-for="item in properties" :key="item.id" class="property-card card">
        <div class="property-content">
          <div class="property-image">
            <img :src="item.coverImage || item.images?.[0] || '/placeholder-property.jpg'" :alt="item.title" />
          </div>
          <div class="property-info">
            <h3>{{ item.title }}</h3>
            <div class="property-tags">
              <el-tag size="small">{{ item.type }}</el-tag>
              <el-tag size="small" type="info">{{ item.area }}㎡</el-tag>
              <el-tag size="small" type="info">{{ item.rooms }}室{{ item.livingRooms }}厅{{ item.bathrooms }}卫</el-tag>
            </div>
            <div class="property-meta">
              <span class="price">¥{{ item.price }}<em>/月</em></span>
              <span class="address">{{ item.address }}</span>
            </div>
          </div>
          <div class="property-actions">
            <span class="status-tag" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
            <el-button size="small" @click="viewDetail(item.id)">查看</el-button>
            <el-button
              v-if="['pending', 'rejected', 'reserved'].includes(String(item.status))"
              size="small"
              type="primary"
              plain
              @click="submitPropertyListing(item.id)"
            >
              申请上架
            </el-button>
            <el-button size="small" @click="editProperty(item.id)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="deleteProperty(item.id)">删除</el-button>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && properties.length === 0" description="您还没有发布房源" />
    </div>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="fetchMyProperties"
      />
    </div>

    <!-- 发布房源对话框 -->
    <el-dialog v-model="showPublishDialog" title="发布房源" width="640px" destroy-on-close>
        <el-form :model="publishForm" label-width="100px">
        <el-form-item label="房源标题" required>
          <el-input v-model="publishForm.title" placeholder="如：精装两居室出租" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="房屋类型" required>
          <el-select v-model="publishForm.type" placeholder="请选择" style="width: 100%">
            <el-option label="普通住宅" value="普通住宅" />
            <el-option label="公寓" value="公寓" />
            <el-option label="洋房" value="洋房" />
            <el-option label="别墅" value="别墅" />
          </el-select>
        </el-form-item>
        <el-form-item label="租金(元/月)" required>
          <el-input-number v-model="publishForm.price" :min="0" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="面积(㎡)" required>
          <el-input-number v-model="publishForm.area" :min="0" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="户型" required>
          <div class="room-inputs">
            <el-input-number v-model="publishForm.rooms" :min="0" placeholder="室" /> 室
            <el-input-number v-model="publishForm.livingRooms" :min="0" placeholder="厅" /> 厅
            <el-input-number v-model="publishForm.bathrooms" :min="0" placeholder="卫" /> 卫
          </div>
        </el-form-item>
        <el-form-item label="楼层" required>
          <el-input-number v-model="publishForm.floor" :min="1" :max="publishForm.totalFloors" /> /
          <el-input-number v-model="publishForm.totalFloors" :min="1" />
        </el-form-item>
        <el-form-item label="装修情况">
          <el-select v-model="publishForm.decoration" style="width: 100%">
            <el-option label="毛坯" value="毛坯" />
            <el-option label="简装" value="简装" />
            <el-option label="精装" value="精装" />
            <el-option label="豪华装修" value="豪华装修" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址" required>
          <el-input v-model="publishForm.address" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input v-model="publishForm.description" type="textarea" :rows="3" placeholder="描述房屋的详细信息..." />
        </el-form-item>
        <el-form-item label="房源图片">
          <div class="media-upload-section">
            <div class="media-upload-controls">
              <el-upload
                class="media-uploader"
                :show-file-list="false"
                accept="image/*"
                multiple
                :disabled="publishForm.images.length >= MAX_PROPERTY_IMAGES"
                :http-request="handleImageUploadRequest"
              >
                <el-button type="primary" plain :loading="imageUploading">选择图片上传</el-button>
              </el-upload>
            </div>
            <p class="upload-hint">
              单张不超过 {{ MAX_IMAGE_MB }}MB，最多 {{ MAX_PROPERTY_IMAGES }} 张；上传后可点「封面」指定列表展示图
            </p>
            <div v-if="publishForm.images.length" class="media-preview">
              <div
                v-for="url in publishForm.images"
                :key="url"
                class="preview-item"
                :class="{ 'is-cover': url === publishForm.coverImage }"
              >
                <img :src="url" alt="房源图片" />
                <button type="button" class="remove-btn" @click="removeMedia(url, 'image')">×</button>
                <div class="preview-actions">
                  <el-button
                    v-if="publishForm.coverImage !== url"
                    size="small"
                    type="primary"
                    text
                    @click="publishForm.coverImage = url"
                  >
                    设为封面
                  </el-button>
                  <el-tag v-else size="small" type="success" effect="dark">封面</el-tag>
                </div>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="房源视频">
          <div class="media-upload-section">
            <div class="media-upload-controls">
              <el-upload
                class="media-uploader"
                :show-file-list="false"
                accept="video/mp4,video/webm,video/quicktime"
                multiple
                :disabled="publishForm.videos.length >= MAX_PROPERTY_VIDEOS"
                :http-request="handleVideoUploadRequest"
              >
                <el-button type="primary" plain :loading="videoUploading">选择视频上传</el-button>
              </el-upload>
            </div>
            <p class="upload-hint">
              单文件不超过 {{ MAX_VIDEO_MB }}MB，最多 {{ MAX_PROPERTY_VIDEOS }} 个；服务端会轻度压缩后再存储
            </p>
            <div v-if="publishForm.videos.length" class="media-preview">
              <div v-for="url in publishForm.videos" :key="url" class="preview-item preview-item-video">
                <video :src="url" controls preload="metadata" />
                <button type="button" class="remove-btn" @click="removeMedia(url, 'video')">×</button>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="handlePublish">
          {{ editingId ? '保存修改' : '发布' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import UserSubpageHeader from '@/components/user/UserSubpageHeader.vue'
import type { Property } from '@/types'
import {
  createProperty,
  deletePropertyById,
  getMyProperties,
  publishProperty,
  updateProperty,
  uploadPropertyMedia,
} from '@/api/property'
import { useAuthStore } from '@/stores/auth'
import { isPlatformAdmin } from '@/constants/roles'

/** 与 property-service 上传/保存校验保持一致 */
const MAX_IMAGE_MB = 20
const MAX_VIDEO_MB = 200
const MAX_PROPERTY_IMAGES = 20
const MAX_PROPERTY_VIDEOS = 3

const loading = ref(false)
const publishing = ref(false)
const properties = ref<Property[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showPublishDialog = ref(false)
const editingId = ref<number | null>(null)
const authStore = useAuthStore()
const router = useRouter()
const imageUploading = ref(false)
const videoUploading = ref(false)

const publishForm = reactive({
  title: '',
  type: '',
  price: 0,
  area: 0,
  rooms: 0,
  livingRooms: 0,
  bathrooms: 0,
  floor: 1,
  totalFloors: 1,
  decoration: '精装',
  address: '',
  description: '',
  images: [] as string[],
  videos: [] as string[],
  coverImage: '' as string,
})

const statusMap: Record<string, string> = {
  pending: '待审核',
  rejected: '已驳回',
  vacant: '已上架',
  occupied: '已出租',
  reserved: '已下架',
  published: '已上架',
  rented: '已出租',
  offline: '已下架',
}

function toStatusKey(status: number | string): string {
  if (typeof status === 'number') {
    if (status === 0) return 'pending'
    if (status === 1) return 'published'
    if (status === 2) return 'offline'
  }
  return String(status || '')
}

function statusLabel(status: number | string): string {
  return statusMap[toStatusKey(status)] ?? '未知'
}

function statusClass(status: number | string): string {
  const map: Record<string, string> = {
    pending: 'pending',
    rejected: 'pending',
    vacant: 'active',
    occupied: 'inactive',
    reserved: 'inactive',
    published: 'active',
    rented: 'inactive',
    offline: 'inactive',
  }
  return map[toStatusKey(status)] || ''
}

function parseLayout(layout?: string): { rooms: number; livingRooms: number; bathrooms: number } {
  const matched = layout?.match(/(\d+)\D+(\d+)\D+(\d+)/)
  if (!matched) {
    return { rooms: 0, livingRooms: 0, bathrooms: 0 }
  }
  return {
    rooms: Number(matched[1] || 0),
    livingRooms: Number(matched[2] || 0),
    bathrooms: Number(matched[3] || 0),
  }
}

function normalizeProperty(item: any): Property {
  const layout = parseLayout(item.layout)
  return {
    id: Number(item.id),
    title: item.title || '',
    address: item.address || '',
    price: Number(item.rentPrice || 0),
    area: Number(item.area || 0),
    rooms: layout.rooms,
    livingRooms: layout.livingRooms,
    bathrooms: layout.bathrooms,
    floor: Number(item.floor || 1),
    totalFloors: Number(item.totalFloor || 1),
    type: item.propertyType || '',
    decoration: '精装',
    description: item.description || '',
    images: item.images || (item.coverImage ? [item.coverImage] : []),
    videos: item.videos || [],
    coverImage: item.coverImage || item.images?.[0] || '',
    status: (item.status || 'pending') as any,
  }
}

function validateMediaFile(file: File, mediaType: 'image' | 'video'): boolean {
  const maxMb = mediaType === 'video' ? MAX_VIDEO_MB : MAX_IMAGE_MB
  if (file.size > maxMb * 1024 * 1024) {
    ElMessage.warning(`${mediaType === 'video' ? '视频' : '图片'}大小不能超过 ${maxMb}MB`)
    return false
  }
  return true
}

async function handleImageUploadRequest(options: UploadRequestOptions) {
  const file = options.file as File
  if (publishForm.images.length >= MAX_PROPERTY_IMAGES) {
    ElMessage.warning(`最多上传 ${MAX_PROPERTY_IMAGES} 张图片`)
    options.onError?.(new Error('limit'))
    return
  }
  if (!validateMediaFile(file, 'image')) {
    options.onError?.(new Error('invalid size'))
    return
  }
  imageUploading.value = true
  try {
    const uploaded = await uploadPropertyMedia(file)
    publishForm.images.push(uploaded.url)
    if (!publishForm.coverImage) {
      publishForm.coverImage = uploaded.url
    }
    ElMessage.success('图片上传成功')
    options.onSuccess?.(uploaded)
  } catch (e) {
    ElMessage.error('图片上传失败，请检查登录状态与网络')
    options.onError?.(e as Error)
  } finally {
    imageUploading.value = false
  }
}

async function handleVideoUploadRequest(options: UploadRequestOptions) {
  const file = options.file as File
  if (publishForm.videos.length >= MAX_PROPERTY_VIDEOS) {
    ElMessage.warning(`最多上传 ${MAX_PROPERTY_VIDEOS} 个视频`)
    options.onError?.(new Error('limit'))
    return
  }
  if (!validateMediaFile(file, 'video')) {
    options.onError?.(new Error('invalid size'))
    return
  }
  videoUploading.value = true
  try {
    const uploaded = await uploadPropertyMedia(file)
    publishForm.videos.push(uploaded.url)
    ElMessage.success('视频上传成功')
    options.onSuccess?.(uploaded)
  } catch (e) {
    ElMessage.error('视频上传失败，请检查登录状态与网络')
    options.onError?.(e as Error)
  } finally {
    videoUploading.value = false
  }
}

function removeMedia(url: string, mediaType: 'image' | 'video') {
  const targetList = mediaType === 'video' ? publishForm.videos : publishForm.images
  const index = targetList.indexOf(url)
  if (index >= 0) {
    targetList.splice(index, 1)
  }
  if (mediaType === 'image' && publishForm.coverImage === url) {
    publishForm.coverImage = publishForm.images[0] || ''
  }
}

async function fetchMyProperties() {
  if (!authStore.userInfo?.id) return
  loading.value = true
  try {
    const result = await getMyProperties({
      page: page.value,
      size: pageSize.value,
      ownerId: authStore.userInfo.id,
    })
    const records = (result.records ?? result.list ?? []) as any[]
    properties.value = records.map(normalizeProperty)
    total.value = Number(result.total || 0)
  } finally {
    loading.value = false
  }
}

async function handlePublish() {
  if (!publishForm.title || !publishForm.type || publishForm.price <= 0 || !publishForm.address) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (publishForm.images.length > MAX_PROPERTY_IMAGES) {
    ElMessage.warning(`图片最多 ${MAX_PROPERTY_IMAGES} 张`)
    return
  }
  if (publishForm.videos.length > MAX_PROPERTY_VIDEOS) {
    ElMessage.warning(`视频最多 ${MAX_PROPERTY_VIDEOS} 个`)
    return
  }

  publishing.value = true
  try {
    const cover =
      publishForm.coverImage && publishForm.images.includes(publishForm.coverImage)
        ? publishForm.coverImage
        : publishForm.images[0]
    const payload = {
      title: publishForm.title,
      propertyType: publishForm.type,
      rentPrice: publishForm.price,
      address: publishForm.address,
      area: publishForm.area,
      floor: publishForm.floor,
      totalFloor: publishForm.totalFloors,
      layout: `${publishForm.rooms}室${publishForm.livingRooms}厅${publishForm.bathrooms}卫`,
      description: publishForm.description,
      images: publishForm.images,
      videos: publishForm.videos,
      coverImage: cover,
    }
    if (editingId.value) {
      await updateProperty(editingId.value, payload)
      ElMessage.success('房源更新成功')
    } else {
      await createProperty(payload)
      ElMessage.success(
        isPlatformAdmin(authStore.userInfo?.role || '')
          ? '房源已上架'
          : '已保存，请等待店长或超级管理员审核通过后即可在前台展示',
      )
    }
    showPublishDialog.value = false
    editingId.value = null
    Object.assign(publishForm, {
      title: '', type: '', price: 0, area: 0, rooms: 0,
      livingRooms: 0, bathrooms: 0, floor: 1, totalFloors: 1,
      decoration: '精装', address: '', description: '', images: [], videos: [], coverImage: '',
    })
    fetchMyProperties()
  } catch {
    ElMessage.error('发布失败，请稍后重试')
  } finally {
    publishing.value = false
  }
}

function openPublishDialog() {
  editingId.value = null
  Object.assign(publishForm, {
    title: '', type: '', price: 0, area: 0, rooms: 0,
    livingRooms: 0, bathrooms: 0, floor: 1, totalFloors: 1,
    decoration: '精装', address: '', description: '', images: [], videos: [], coverImage: '',
  })
  showPublishDialog.value = true
}

function viewDetail(id: number) {
  void router.push(`/properties/${id}`)
}

function editProperty(id: number) {
  const target = properties.value.find((item) => item.id === id)
  if (!target) return
  editingId.value = id
  Object.assign(publishForm, {
    title: target.title || '',
    type: target.type || '',
    price: target.price || 0,
    area: target.area || 0,
    rooms: target.rooms || 0,
    livingRooms: target.livingRooms || 0,
    bathrooms: target.bathrooms || 0,
    floor: target.floor || 1,
    totalFloors: target.totalFloors || 1,
    decoration: target.decoration || '精装',
    address: target.address || '',
    description: target.description || '',
    images: [...(target.images || [])],
    videos: [...(target.videos || [])],
    coverImage: target.coverImage || target.images?.[0] || '',
  })
  showPublishDialog.value = true
}

async function submitPropertyListing(id: number) {
  try {
    await publishProperty(id)
    ElMessage.success('已提交上架审核')
    await fetchMyProperties()
  } catch {
    ElMessage.error('提交失败，请稍后重试')
  }
}

async function deleteProperty(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除此房源吗？', '删除房源', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deletePropertyById(id)
    ElMessage.success('删除成功')
    fetchMyProperties()
  } catch {
    // 用户取消
  }
}

onMounted(async () => {
  await authStore.fetchUserInfo()
  fetchMyProperties()
})
</script>

<style scoped lang="scss">
.my-properties-page {
  /* header is unified by UserSubpageHeader */
}

.property-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.property-card {
  padding: var(--spacing-md);
}

.property-content {
  display: flex;
  gap: var(--spacing-md);
}

.property-image {
  width: 140px;
  height: 100px;
  border-radius: var(--border-radius-base);
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.property-info {
  flex: 1;
  min-width: 0;

  h3 {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: var(--spacing-sm);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.property-tags {
  display: flex;
  gap: var(--spacing-xs);
  flex-wrap: wrap;
  margin-bottom: var(--spacing-sm);
}

.property-meta {
  .price {
    font-size: 18px;
    font-weight: 700;
    color: #f56c6c;
    margin-right: var(--spacing-md);

    em {
      font-style: normal;
      font-size: 12px;
      font-weight: 400;
    }
  }

  .address {
    font-size: 13px;
    color: var(--color-text-secondary);
    display: inline-block;
    max-width: 360px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.property-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--spacing-sm);
}

.status-tag {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;

  &.pending { background: #fdf6ec; color: #e6a23c; }
  &.active { background: #f0f9eb; color: #67c23a; }
  &.inactive { background: #f4f4f5; color: #909399; }
}

.room-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-xl);
}

.media-upload-section {
  width: 100%;
}

.media-upload-controls {
  display: flex;
  align-items: center;
}

.media-uploader {
  :deep(.el-upload) {
    display: inline-flex;
    justify-content: flex-start;
    width: auto;
    border: none;
  }
}

.upload-hint {
  display: block;
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}

.media-preview {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  width: 100%;
}

.preview-item {
  position: relative;
  width: 100px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e5e7eb;

  &.is-cover {
    outline: 2px solid var(--el-color-success);
    outline-offset: 1px;
  }
}

.preview-actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 4px 2px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.65));
}

.preview-item img,
.preview-item video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-item-video {
  width: 160px;
}

.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  cursor: pointer;
  line-height: 18px;
  font-size: 12px;
  padding: 0;
}
</style>
