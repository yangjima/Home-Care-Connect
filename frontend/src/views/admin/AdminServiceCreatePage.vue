<template>
  <div class="page">
    <div class="card">
      <div class="card-title">新增服务</div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="服务名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：深度保洁" />
        </el-form-item>
        <el-form-item label="服务分类" prop="category">
          <el-input v-model="form.category" placeholder="例如：cleaning / repair" />
        </el-form-item>
        <el-form-item label="基础价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="计价单位" prop="unit">
          <el-input v-model="form.unit" placeholder="次" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="可选：emoji 或图标名" />
        </el-form-item>
        <el-form-item label="服务描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item>
          <el-button @click="router.push('/admin/services')">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createServiceType } from '@/api/service'

const router = useRouter()
const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive({
  name: '',
  category: '',
  price: 0,
  unit: '次',
  icon: '',
  description: '',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入服务名称', trigger: 'blur' }],
  category: [{ required: true, message: '请输入服务分类', trigger: 'blur' }],
  price: [{ required: true, message: '请输入基础价格', trigger: 'change' }],
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await createServiceType({
      name: form.name,
      category: form.category,
      price: Number(form.price),
      unit: form.unit || '次',
      icon: form.icon || undefined,
      description: form.description || undefined,
    })
    ElMessage.success('服务创建成功')
    router.push('/admin/services')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.page {
  max-width: 860px;
}

.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 24px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
}
</style>
