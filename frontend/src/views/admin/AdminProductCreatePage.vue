<template>
  <div class="page-shell">
    <div class="page">
      <div class="page-header">
      <div class="page-header__left">
        <div class="page-title">
          <span class="page-title__icon">📦</span>
          <div class="page-title__text">
            <div class="page-title__main">添加商品</div>
            <div class="page-title__sub">完善基础信息与图片，提交后进入管理列表。</div>
          </div>
        </div>
      </div>
      <div class="page-header__right">
        <el-button class="top-action" @click="router.push('/admin/products')">返回列表</el-button>
        <el-button class="top-action" type="primary" :loading="saving" @click="submit">提交</el-button>
      </div>
      </div>

      <div class="grid">
        <div class="panel">
          <div class="panel__header">
            <div class="panel__title">商品信息</div>
            <div class="panel__desc">带 * 为必填项，建议先填名称与价格。</div>
          </div>
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form">
            <el-form-item label="商品类型 *" prop="productType">
              <el-segmented class="segmented" v-model="form.productType" :options="productTypeOptions" />
            </el-form-item>

            <div class="row">
              <el-form-item
                class="row__item"
                :label="(form.productType === 'secondhand' ? '商品标题' : '商品名称') + ' *'"
                prop="name"
              >
                <el-input v-model="form.name" placeholder="例如：折叠助行器 / 电热毯" />
              </el-form-item>
              <el-form-item class="row__item" label="分类 *" prop="category">
                <el-input v-model="form.category" placeholder="例如：居家用品 / 康复辅具" />
              </el-form-item>
            </div>

            <div class="row">
              <el-form-item class="row__item" label="价格（元） *" prop="price">
                <el-input-number
                  v-model="form.price"
                  :min="0"
                  :precision="2"
                  :step="10"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item v-if="form.productType === 'procurement'" class="row__item" label="库存" prop="stock">
                <el-input-number v-model="form.stock" :min="0" :step="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item v-else class="row__item" label="商品成色" prop="condition">
                <el-select v-model="form.condition" style="width: 100%">
                  <el-option label="几乎全新" value="like_new" />
                  <el-option label="良好" value="good" />
                  <el-option label="一般" value="fair" />
                </el-select>
              </el-form-item>
            </div>

            <div class="subsection">
              <div class="subsection__title">展示信息</div>
              <div class="subsection__desc">封面图建议使用 16:9 或 1:1，清晰度更佳。</div>
            </div>

            <el-form-item label="封面图 URL（可选）" prop="image">
              <el-input v-model="form.image" placeholder="粘贴图片链接，例如：https://..." />
            </el-form-item>

            <el-form-item label="描述（可选）">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="5"
                placeholder="写清楚商品亮点、规格、使用说明等…"
              />
            </el-form-item>
          </el-form>
        </div>

        <div class="panel panel--sticky">
          <div class="panel__header panel__header--tight">
            <div class="panel__title">预览</div>
            <div class="panel__desc">展示效果预览（非最终样式）。</div>
          </div>
          <div class="preview-card">
            <div class="preview-card__media">
              <el-image v-if="form.image" class="preview-card__img" :src="form.image" fit="cover" />
              <div v-else class="preview-card__placeholder">
                <div class="preview-card__placeholder-icon">🖼️</div>
                <div class="preview-card__placeholder-text">填写封面图 URL 后可预览</div>
              </div>
              <div class="preview-card__badge">
                {{ form.productType === 'procurement' ? '本地商城' : '跳蚤市场' }}
              </div>
            </div>
            <div class="preview-card__body">
              <div class="preview-card__name">{{ form.name || '未填写名称' }}</div>
              <div class="preview-card__meta">
                <el-tag v-if="form.category" effect="plain" type="info">{{ form.category }}</el-tag>
                <el-tag v-else effect="plain" type="info">未填写分类</el-tag>
                <el-tag effect="plain" :type="form.productType === 'procurement' ? 'success' : 'warning'">
                  {{ form.productType === 'procurement' ? '库存 ' + Number(form.stock || 0) : '成色 ' + conditionText }}
                </el-tag>
              </div>
              <div class="preview-card__price">
                <span class="preview-card__price-symbol">¥</span>
                <span class="preview-card__price-num">{{ Number(form.price || 0).toFixed(2) }}</span>
              </div>
              <div v-if="form.description" class="preview-card__desc">
                {{ form.description }}
              </div>
              <div v-else class="preview-card__desc preview-card__desc--muted">补充描述后会显示在这里。</div>
            </div>
          </div>

          <div class="actions">
            <el-button class="actions__btn" @click="router.push('/admin/products')">取消</el-button>
            <el-button class="actions__btn" type="primary" :loading="saving" @click="submit">提交</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
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

const productTypeOptions = [
  { label: '本地商城商品', value: 'procurement' },
  { label: '跳蚤市场商品', value: 'secondhand' },
]

const conditionText = computed(() => {
  if (form.condition === 'like_new') return '几乎全新'
  if (form.condition === 'fair') return '一般'
  return '良好'
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
.page-shell {
  border-radius: 16px;
  padding: 18px 18px 22px;
  background:
    radial-gradient(900px 360px at 16% 0%, rgba(59, 130, 246, 0.18), transparent 58%),
    radial-gradient(760px 300px at 86% 6%, rgba(16, 185, 129, 0.14), transparent 56%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.78));
  border: 1px solid rgba(148, 163, 184, 0.28);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
}

.page {
  max-width: 1100px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.top-action {
  border-radius: 10px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.page-title__icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2563eb, #60a5fa);
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.22);
}

.page-title__main {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  line-height: 1.25;
}

.page-title__sub {
  margin-top: 2px;
  font-size: 13px;
  color: #6b7280;
}

.grid {
  display: grid;
  grid-template-columns: 1.3fr 0.9fr;
  gap: 16px;
  align-items: start;
}

.panel {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.08);
  padding: 18px 18px 16px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  backdrop-filter: blur(6px);
}

.panel__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel__header--tight {
  margin-bottom: 10px;
}

.panel__title {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 0;
}

.panel__desc {
  font-size: 12px;
  color: #6b7280;
}

.form :deep(.el-form-item__label) {
  color: #374151;
  font-weight: 600;
  padding-bottom: 6px;
}

.form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.form :deep(.el-input__wrapper),
.form :deep(.el-textarea__inner),
.form :deep(.el-input-number .el-input__wrapper),
.form :deep(.el-select__wrapper) {
  border-radius: 12px;
}

.segmented {
  width: 100%;
}

.subsection {
  padding-top: 6px;
  margin: 2px 0 10px;
  border-top: 1px dashed rgba(148, 163, 184, 0.35);
}

.subsection__title {
  margin-top: 12px;
  font-size: 13px;
  font-weight: 700;
  color: #111827;
}

.subsection__desc {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.row__item {
  margin-bottom: 4px;
}

.preview-card {
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: linear-gradient(180deg, rgba(249, 250, 251, 0.9), rgba(255, 255, 255, 1));
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.08);
}

.preview-card__media {
  position: relative;
  background: #f3f4f6;
}

.preview-card__img {
  width: 100%;
  height: 200px;
  display: block;
}

.preview-card__placeholder {
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #6b7280;
  background:
    radial-gradient(600px 220px at 20% 20%, rgba(59, 130, 246, 0.10), transparent 55%),
    radial-gradient(520px 200px at 80% 10%, rgba(16, 185, 129, 0.10), transparent 55%),
    #f3f4f6;
}

.preview-card__placeholder-icon {
  font-size: 28px;
}

.preview-card__placeholder-text {
  font-size: 13px;
}

.preview-card__badge {
  position: absolute;
  left: 12px;
  top: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(148, 163, 184, 0.35);
  backdrop-filter: blur(8px);
}

.preview-card__body {
  padding: 14px 14px 16px;
  display: grid;
  gap: 10px;
}

.preview-card__name {
  font-weight: 800;
  font-size: 16px;
  color: #111827;
  word-break: break-word;
  letter-spacing: 0.2px;
}

.preview-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-card__price {
  display: flex;
  align-items: baseline;
  gap: 4px;
  color: #0f172a;
}

.preview-card__price-symbol {
  font-size: 14px;
  font-weight: 700;
  color: #334155;
}

.preview-card__price-num {
  font-size: 26px;
  font-weight: 900;
  letter-spacing: 0.3px;
  background: linear-gradient(135deg, #2563eb, #22c55e);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.preview-card__desc {
  font-size: 13px;
  color: #374151;
  line-height: 1.6;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.preview-card__desc--muted {
  color: #6b7280;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.actions__btn {
  flex: 1;
}

.panel--sticky {
  position: sticky;
  top: 12px;
}

@media (max-width: 980px) {
  .page-shell {
    padding: 14px 12px 18px;
    border-radius: 14px;
  }

  .grid {
    grid-template-columns: 1fr;
  }

  .panel--sticky {
    position: static;
  }
}

@media (max-width: 520px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-header__right {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }

  .row {
    grid-template-columns: 1fr;
  }
}
</style>
