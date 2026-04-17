<template>
  <div class="my-properties-page">
    <div class="page-header">
      <h1>我的房源</h1>
      <el-button type="primary" @click="openPublishDialog">
        <el-icon><Plus /></el-icon> 发布房源
      </el-button>
    </div>

    <div v-loading="loading" class="property-list">
      <div v-for="item in properties" :key="item.id" class="property-card card">
        <div class="property-content">
          <div class="property-image">
            <img :src="item.images?.[0] || '/placeholder-property.jpg'" :alt="item.title" />
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
    <el-dialog v-model="showPublishDialog" title="发布房源" width="600px">
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
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Property } from '@/types'
import { createProperty, deletePropertyById, getMyProperties, updateProperty } from '@/api/property'
import { useAuthStore } from '@/stores/auth'

const loading = ref(false)
const publishing = ref(false)
const properties = ref<Property[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showPublishDialog = ref(false)
const editingId = ref<number | null>(null)
const authStore = useAuthStore()

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
})

const statusMap: Record<string, string> = {
  pending: '待审核',
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

async function handlePublish() {
  if (!publishForm.title || !publishForm.type || publishForm.price <= 0 || !publishForm.address) {
    ElMessage.warning('请填写完整信息')
    return
  }

  publishing.value = true
  try {
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
    }
    if (editingId.value) {
      await updateProperty(editingId.value, payload)
      ElMessage.success('房源更新成功')
    } else {
      await createProperty(payload)
      ElMessage.success('发布成功')
    }
    showPublishDialog.value = false
    editingId.value = null
    Object.assign(publishForm, {
      title: '', type: '', price: 0, area: 0, rooms: 0,
      livingRooms: 0, bathrooms: 0, floor: 1, totalFloors: 1,
      decoration: '精装', address: '', description: '',
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
    decoration: '精装', address: '', description: '',
  })
  showPublishDialog.value = true
}

function viewDetail(id: number) {
  window.location.href = `/properties/${id}`
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
  })
  showPublishDialog.value = true
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
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--spacing-lg);

    h1 {
      font-size: 20px;
      font-weight: 700;
    }
  }
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
</style>
