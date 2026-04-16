<template>
  <div class="service-order-page">
    <AppHeader />
    <div class="container">
      <div class="page-header">
        <h1>预约服务</h1>
        <el-button text @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
      </div>

      <div class="order-form card">
        <h2>{{ serviceType?.name }}</h2>
        <p class="service-price">
          <em>¥</em>{{ serviceType?.price }}<span>/{{ serviceType?.unit }}</span>
        </p>
        <p class="service-desc">{{ serviceType?.description }}</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="mt-xl">
          <el-form-item label="服务时间" prop="serviceTime">
            <el-date-picker
              v-model="form.serviceTime"
              type="datetime"
              placeholder="选择服务时间"
              style="width: 100%"
              :disabled-date="(date: Date) => date < new Date()"
            />
          </el-form-item>
          <el-form-item label="服务地址" prop="serviceAddress">
            <el-input v-model="form.serviceAddress" placeholder="请输入详细服务地址" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" placeholder="有什么特别要求吗？" />
          </el-form-item>
        </el-form>

        <div class="form-actions">
          <el-button size="large" @click="$router.back()">取消</el-button>
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
            提交订单
          </el-button>
        </div>
      </div>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { useServiceStore } from '@/stores/service'
import { useAuthStore } from '@/stores/auth'
import { getServiceTypeDetail } from '@/api/service'

const route = useRoute()
const router = useRouter()
const serviceStore = useServiceStore()
const authStore = useAuthStore()

const serviceType = ref<any>(null)
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  serviceTime: '',
  serviceAddress: '',
  remark: '',
})

const rules = {
  serviceTime: [{ required: true, message: '请选择服务时间', trigger: 'change' }],
  serviceAddress: [{ required: true, message: '请输入服务地址', trigger: 'blur' }],
}

onMounted(async () => {
  const id = Number(route.params.id)
  const detail = await getServiceTypeDetail(id)
  serviceType.value = detail
})

async function handleSubmit() {
  if (!authStore.isLoggedIn) {
    router.push('/auth/login')
    return
  }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await serviceStore.createOrder({
      serviceTypeId: serviceType.value.id,
      serviceTime: new Date(form.serviceTime).toISOString(),
      serviceAddress: form.serviceAddress,
      remark: form.remark,
    })
    ElMessage.success('订单创建成功！')
    router.push('/user/orders')
  } catch {
    ElMessage.error('订单创建失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.service-order-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
}

.page-header {
  padding: var(--spacing-lg) 0;
  display: flex;
  align-items: center;
  justify-content: space-between;

  h1 {
    font-size: 24px;
    font-weight: 700;
  }
}

.order-form {
  max-width: 600px;
  margin: 0 auto;
  padding: var(--spacing-xl);

  h2 {
    font-size: 20px;
    margin-bottom: var(--spacing-sm);
  }

  .service-price {
    color: #f56c6c;
    font-size: 24px;
    font-weight: 700;
    margin-bottom: var(--spacing-sm);

    em {
      font-style: normal;
      font-size: 14px;
    }

    span {
      font-size: 14px;
      font-weight: 400;
      color: var(--color-text-secondary);
    }
  }

  .service-desc {
    color: var(--color-text-secondary);
    margin-bottom: var(--spacing-md);
  }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-md);
  margin-top: var(--spacing-xl);
}
</style>
