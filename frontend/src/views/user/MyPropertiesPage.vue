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
              v-if="['pending', 'rejected', 'reserved'].includes(toPropertyStatusKey(item.status))"
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

    <PropertyFormDialog
      v-model:visible="showPublishDialog"
      :property-id="editingId"
      @saved="onPropertyFormSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserSubpageHeader from '@/components/user/UserSubpageHeader.vue'
import PropertyFormDialog from '@/components/property/PropertyFormDialog.vue'
import type { Property } from '@/types'
import {
  deletePropertyById,
  getMyProperties,
  publishProperty,
} from '@/api/property'
import { useAuthStore } from '@/stores/auth'
import { propertyStatusClass, propertyStatusLabel, toPropertyStatusKey } from '@/utils/status'

const loading = ref(false)
const properties = ref<Property[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showPublishDialog = ref(false)
const editingId = ref<number | null>(null)
const authStore = useAuthStore()
const router = useRouter()

function statusLabel(status: number | string): string {
  return propertyStatusLabel(status)
}

function statusClass(status: number | string): string {
  return propertyStatusClass(status)
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

function onPropertyFormSaved() {
  fetchMyProperties()
}

function openPublishDialog() {
  editingId.value = null
  showPublishDialog.value = true
}

function viewDetail(id: number) {
  void router.push(`/properties/${id}`)
}

function editProperty(id: number) {
  editingId.value = id
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

.pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-xl);
}
</style>
