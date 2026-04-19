<template>
  <el-dialog
    v-model="visibleInner"
    :title="propertyId ? '编辑房源' : '发布房源'"
    width="640px"
    destroy-on-close
    @closed="onDialogClosed"
  >
    <el-form :model="publishForm" label-width="100px">
      <el-form-item v-if="isSuperAdmin" label="所属门店" required>
        <el-select v-model="publishForm.storeId" placeholder="请选择门店" filterable style="width: 100%">
          <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
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
          <div class="room-cell">
            <el-input-number v-model="publishForm.rooms" :min="0" placeholder="室" controls-position="right" class="room-stepper" />
            <span class="room-suffix">室</span>
          </div>
          <div class="room-cell">
            <el-input-number v-model="publishForm.livingRooms" :min="0" placeholder="厅" controls-position="right" class="room-stepper" />
            <span class="room-suffix">厅</span>
          </div>
          <div class="room-cell">
            <el-input-number v-model="publishForm.bathrooms" :min="0" placeholder="卫" controls-position="right" class="room-stepper" />
            <span class="room-suffix">卫</span>
          </div>
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
      <el-button @click="visibleInner = false">取消</el-button>
      <el-button type="primary" :loading="publishing" @click="handlePublish">
        {{ propertyId ? '保存修改' : '发布' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import {
  createProperty,
  getPropertyDetail,
  updateProperty,
  uploadPropertyMedia,
} from '@/api/property'
import { listStores, type Store } from '@/api/stores'
import { useAuthStore } from '@/stores/auth'
import { isPlatformAdmin, ROLE_ADMIN } from '@/constants/roles'

const MAX_IMAGE_MB = 20
const MAX_VIDEO_MB = 200
const MAX_PROPERTY_IMAGES = 20
const MAX_PROPERTY_VIDEOS = 3

const props = defineProps<{
  visible: boolean
  /** 非空时为编辑模式，打开时拉取详情 */
  propertyId: number | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void
  (e: 'saved'): void
}>()

const authStore = useAuthStore()
const storeOptions = ref<Store[]>([])
const isSuperAdmin = computed(() => authStore.userInfo?.role === ROLE_ADMIN)

const publishing = ref(false)
const imageUploading = ref(false)
const videoUploading = ref(false)

const visibleInner = computed({
  get: () => props.visible,
  set: (v) => emit('update:visible', v),
})

const publishForm = reactive({
  storeId: undefined as number | undefined,
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

function resetForm() {
  Object.assign(publishForm, {
    storeId: undefined,
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
    images: [],
    videos: [],
    coverImage: '',
  })
}

function applyDetail(item: Record<string, unknown>) {
  const layout = parseLayout(String(item.layout || ''))
  const imgs = (item.images as string[] | undefined) || []
  const cover = (item.coverImage as string) || imgs[0] || ''
  Object.assign(publishForm, {
    storeId: item.storeId != null ? Number(item.storeId) : undefined,
    title: String(item.title || ''),
    type: String(item.propertyType || ''),
    price: Number(item.rentPrice || 0),
    area: Number(item.area || 0),
    rooms: layout.rooms,
    livingRooms: layout.livingRooms,
    bathrooms: layout.bathrooms,
    floor: Number(item.floor || 1),
    totalFloors: Number(item.totalFloor || 1),
    decoration: '精装',
    address: String(item.address || ''),
    description: String(item.description || ''),
    images: [...imgs],
    videos: [...((item.videos as string[]) || [])],
    coverImage: cover,
  })
}

watch(
  () => [props.visible, props.propertyId] as const,
  async ([open, id]) => {
    if (!open) return
    if (isSuperAdmin.value) {
      try {
        storeOptions.value = await listStores()
      } catch {
        storeOptions.value = []
      }
    }
    if (!id) {
      resetForm()
      return
    }
    try {
      const raw = await getPropertyDetail(id)
      applyDetail(raw as Record<string, unknown>)
    } catch {
      /* request 已提示 */
    }
  },
  { immediate: true },
)

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

async function handlePublish() {
  if (!publishForm.title || !publishForm.type || publishForm.price <= 0 || !publishForm.address) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (isSuperAdmin.value && !props.propertyId && (publishForm.storeId == null || Number.isNaN(publishForm.storeId))) {
    ElMessage.warning('请选择所属门店')
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
    const payload: Parameters<typeof createProperty>[0] = {
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
    if (isSuperAdmin.value && publishForm.storeId != null) {
      payload.storeId = publishForm.storeId
    }
    if (props.propertyId) {
      await updateProperty(props.propertyId, payload)
      ElMessage.success('房源更新成功')
    } else {
      await createProperty(payload)
      ElMessage.success(
        isPlatformAdmin(authStore.userInfo?.role || '')
          ? '房源已上架'
          : '已保存，请等待店长或超级管理员审核通过后即可在前台展示',
      )
    }
    visibleInner.value = false
    emit('saved')
  } catch {
    ElMessage.error('发布失败，请稍后重试')
  } finally {
    publishing.value = false
  }
}

function onDialogClosed() {
  resetForm()
}
</script>

<style scoped lang="scss">
.room-inputs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px 12px;
  align-items: center;
  width: 100%;
  max-width: 100%;
}

.room-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.room-suffix {
  flex-shrink: 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.room-stepper {
  flex: 1;
  min-width: 0;
  width: 100%;
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
