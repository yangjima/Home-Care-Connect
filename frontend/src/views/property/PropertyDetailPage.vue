<template>
  <div class="property-detail-page">
    <AppHeader />
    <div class="container">
      <div v-loading="loading">
        <div v-if="property">
          <div class="property-gallery">
            <div class="main-image">
              <img :src="property.images?.[0] || '/placeholder-property.jpg'" :alt="property.title" />
            </div>
            <div v-if="property.images?.length > 1" class="thumbnail-list">
              <img v-for="(img, i) in property.images" :key="i" :src="img" />
            </div>
          </div>

          <div class="property-info">
            <div class="info-main">
              <h1 class="property-title">{{ property.title }}</h1>
              <div class="property-tags">
                <el-tag type="primary">{{ property.type }}</el-tag>
                <el-tag>{{ property.decoration }}</el-tag>
                <el-tag type="info">{{ property.floor }}/{{ property.totalFloors }}层</el-tag>
              </div>
              <div class="property-price">
                <span class="price">¥{{ property.price }}</span>
                <span class="unit">元/月</span>
              </div>
              <div class="property-specs">
                <div class="spec-item">
                  <span class="spec-label">面积</span>
                  <span class="spec-value">{{ property.area }}㎡</span>
                </div>
                <div class="spec-item">
                  <span class="spec-label">户型</span>
                  <span class="spec-value">{{ property.rooms }}室{{ property.livingRooms }}厅{{ property.bathrooms }}卫</span>
                </div>
                <div class="spec-item">
                  <span class="spec-label">楼层</span>
                  <span class="spec-value">{{ property.floor }}/{{ property.totalFloors }}</span>
                </div>
                <div class="spec-item">
                  <span class="spec-label">地址</span>
                  <span class="spec-value">{{ property.address }}</span>
                </div>
              </div>
            </div>

            <div class="info-actions">
              <el-button type="primary" size="large" @click="showBookingDialog = true">
                预约看房
              </el-button>
              <el-button size="large" @click="contactAgent">联系经纪人</el-button>
            </div>
          </div>

          <div class="property-desc card">
            <h2>房源描述</h2>
            <p>{{ property.description || '暂无详细描述' }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 预约看房对话框 -->
    <el-dialog v-model="showBookingDialog" title="预约看房" width="500px">
      <el-form :model="bookingForm" label-width="100px">
        <el-form-item label="看房时间">
          <el-date-picker
            v-model="bookingForm.viewingTime"
            type="datetime"
            placeholder="选择看房时间"
            style="width: 100%"
            :disabled-date="(date: Date) => date < new Date()"
          />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="bookingForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="bookingForm.remark" type="textarea" placeholder="有什么特别要求吗？" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBookingDialog = false">取消</el-button>
        <el-button type="primary" :loading="booking" @click="handleBooking">确认预约</el-button>
      </template>
    </el-dialog>

    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { usePropertyStore } from '@/stores/property'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const propertyStore = usePropertyStore()
const authStore = useAuthStore()

const loading = ref(false)
const property = ref<any>(null)
const showBookingDialog = ref(false)
const booking = ref(false)

const bookingForm = ref({
  viewingTime: '',
  contactPhone: '',
  remark: '',
})

onMounted(async () => {
  loading.value = true
  try {
    await propertyStore.fetchPropertyDetail(Number(route.params.id))
    property.value = propertyStore.currentProperty
  } finally {
    loading.value = false
  }
})

function contactAgent() {
  ElMessage.info('请联系：400-888-8888')
}

async function handleBooking() {
  if (!authStore.isLoggedIn) {
    router.push('/auth/login')
    return
  }

  if (!bookingForm.value.viewingTime || !bookingForm.value.contactPhone) {
    ElMessage.warning('请填写完整信息')
    return
  }

  booking.value = true
  try {
    await propertyStore.bookViewing({
      propertyId: property.value.id,
      viewingTime: new Date(bookingForm.value.viewingTime).toISOString(),
      contactPhone: bookingForm.value.contactPhone,
      remark: bookingForm.value.remark,
    })
    ElMessage.success('预约成功！我们会尽快与您联系')
    showBookingDialog.value = false
  } catch {
    ElMessage.error('预约失败，请稍后重试')
  } finally {
    booking.value = false
  }
}
</script>

<style scoped lang="scss">
.property-detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
}

.property-gallery {
  margin-bottom: var(--spacing-lg);

  .main-image {
    height: 400px;
    border-radius: var(--border-radius-large);
    overflow: hidden;
    background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .thumbnail-list {
    display: flex;
    gap: var(--spacing-sm);
    margin-top: var(--spacing-sm);

    img {
      width: 100px;
      height: 70px;
      object-fit: cover;
      border-radius: var(--border-radius-base);
      cursor: pointer;
    }
  }
}

.property-info {
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
  display: flex;
  gap: var(--spacing-xl);
  box-shadow: var(--shadow-light);
}

.info-main {
  flex: 1;
}

.property-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: var(--spacing-md);
}

.property-tags {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
}

.property-price {
  margin-bottom: var(--spacing-lg);

  .price {
    font-size: 32px;
    font-weight: 700;
    color: #f56c6c;
  }

  .unit {
    font-size: 14px;
    color: var(--color-text-secondary);
  }
}

.property-specs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-md);

  .spec-item {
    display: flex;
    gap: var(--spacing-sm);
  }

  .spec-label {
    color: var(--color-text-secondary);
  }

  .spec-value {
    font-weight: 500;
  }
}

.info-actions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  width: 200px;
}

.property-desc {
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-light);

  h2 {
    font-size: 18px;
    margin-bottom: var(--spacing-md);
  }

  p {
    color: var(--color-text-regular);
    line-height: 1.8;
  }
}
</style>
