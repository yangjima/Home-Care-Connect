<template>
  <div class="page">
    <div class="card">
      <div class="card-title">添加商品</div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
        <el-form-item label="商品类型" prop="productType">
          <el-radio-group v-model="form.productType">
            <el-radio-button label="procurement">本地商城商品</el-radio-button>
            <el-radio-button label="secondhand">跳蚤市场商品</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="form.productType === 'secondhand' ? '商品标题' : '商品名称'" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model="form.category" placeholder="例如：居家用品" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item v-if="form.productType === 'procurement'" label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :step="1" />
        </el-form-item>
        <el-form-item v-if="form.productType === 'secondhand'" label="商品成色" prop="condition">
          <el-select v-model="form.condition" style="width: 220px">
            <el-option label="几乎全新" value="like_new" />
            <el-option label="良好" value="good" />
            <el-option label="一般" value="fair" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图" prop="image">
          <el-input v-model="form.image" placeholder="图片 URL，可选" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item>
          <el-button @click="router.push('/admin/products')">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { createProcurementProduct, createSecondhandItem } from '@/api/asset'

const router = useRouter()
const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive({
  productType: 'procurement',
  name: '',
  category: '',
  price: 0,
  stock: 1,
  condition: 'good',
  image: '',
  description: '',
})

const rules: FormRules = {
  productType: [{ required: true, message: '请选择商品类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'change' }],
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.productType === 'procurement') {
      await createProcurementProduct({
        name: form.name,
        category: form.category,
        price: Number(form.price),
        stock: Number(form.stock),
        description: form.description || undefined,
        image: form.image || undefined,
      })
    } else {
      await createSecondhandItem({
        title: form.name,
        category: form.category,
        price: Number(form.price),
        condition: form.condition,
        description: form.description || undefined,
        image: form.image || undefined,
      })
    }
    ElMessage.success('商品已提交')
    router.push('/admin/products')
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
