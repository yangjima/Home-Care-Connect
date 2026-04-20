<template>
  <el-dialog v-model="visibleInner" title="添加商品" width="780px" destroy-on-close @closed="onClosed">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="商品类型 *" prop="productType">
        <el-segmented v-model="form.productType" :options="productTypeOptions" />
      </el-form-item>

      <div class="grid">
        <div class="col">
          <div class="row">
            <el-form-item
              class="row__item"
              :label="(form.productType === 'secondhand' ? '商品标题' : '商品名称') + ' *'"
              prop="name"
            >
              <el-input v-model="form.name" placeholder="请输入名称" />
            </el-form-item>
            <el-form-item class="row__item" label="分类 *" prop="category">
              <el-select v-model="form.category" placeholder="请选择分类" filterable style="width: 100%">
                <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
              </el-select>
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
              <el-input-number
                v-model="form.stock"
                :min="0"
                :step="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item v-else class="row__item" label="成色（几成新）" prop="conditionPercent">
              <el-input-number
                v-model="form.conditionPercent"
                :min="1"
                :max="10"
                :step="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </div>

          <el-form-item label="封面图 *" prop="image">
            <el-upload
              class="cover-uploader"
              :show-file-list="false"
              accept="image/*"
              :disabled="uploading"
              :http-request="handleCoverUploadRequest"
            >
              <el-button type="primary" plain :loading="uploading">
                {{ form.image ? '重新上传封面图' : '上传封面图' }}
              </el-button>
            </el-upload>
            <div class="upload-hint">建议 1:1 或 16:9，单张不超过 {{ MAX_IMAGE_MB }}MB</div>

            <div v-if="form.image" class="cover-preview">
              <el-image class="cover-preview__img" :src="form.image" fit="cover" />
              <el-button class="cover-preview__remove" size="small" type="danger" plain @click="form.image = ''">
                移除
              </el-button>
            </div>
          </el-form-item>

          <el-form-item label="描述（可选）">
            <el-input v-model="form.description" type="textarea" :rows="4" placeholder="补充商品亮点、规格、使用说明等…" />
          </el-form-item>
        </div>

        <div class="col">
          <div class="preview">
            <div class="preview__title">预览</div>
            <div class="preview-card">
              <div class="preview-card__media">
                <el-image v-if="form.image" class="preview-card__img" :src="form.image" fit="cover" />
                <div v-else class="preview-card__placeholder">
                  <div class="preview-card__placeholder-icon">🖼️</div>
                  <div class="preview-card__placeholder-text">上传封面图后展示预览</div>
                </div>
                <div class="preview-card__badge">
                  {{ form.productType === 'procurement' ? '本地商城' : '跳蚤市场' }}
                </div>
              </div>
              <div class="preview-card__body">
                <div class="preview-card__name">{{ form.name || '未填写名称' }}</div>
                <div class="preview-card__meta">
                  <el-tag effect="plain" type="info">{{ form.category || '未选择分类' }}</el-tag>
                  <el-tag
                    effect="plain"
                    :type="form.productType === 'procurement' ? 'success' : 'warning'"
                  >
                    {{
                      form.productType === 'procurement'
                        ? '库存 ' + Number(form.stock || 0)
                        : '成色 ' + Number(form.conditionPercent || 0) + '成新'
                    }}
                  </el-tag>
                </div>
                <div class="preview-card__price">
                  <span class="preview-card__price-symbol">¥</span>
                  <span class="preview-card__price-num">{{ Number(form.price || 0).toFixed(2) }}</span>
                </div>
                <div v-if="form.description" class="preview-card__desc">{{ form.description }}</div>
                <div v-else class="preview-card__desc preview-card__desc--muted">补充描述后会显示在这里。</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="visibleInner = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
import { createProcurementProduct, createSecondhandItem } from '@/api/asset'
import { uploadPropertyMedia } from '@/api/property'

const MAX_IMAGE_MB = 20

const props = defineProps<{
  visible: boolean
  defaultType?: 'procurement' | 'secondhand'
}>()

const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void
  (e: 'saved'): void
}>()

const visibleInner = computed({
  get: () => props.visible,
  set: (v) => emit('update:visible', v),
})

const productTypeOptions = [
  { label: '本地商城商品', value: 'procurement' },
  { label: '跳蚤市场商品', value: 'secondhand' },
]

const procurementCategories = ['居家用品', '康复辅具', '健康护理', '家电数码', '生活服务', '食品保健', '其他']
const secondhandCategories = ['闲置物品', '家电数码', '居家用品', '母婴用品', '图书文具', '衣物饰品', '其他']

const categoryOptions = computed(() =>
  (form.productType === 'procurement' ? procurementCategories : secondhandCategories).slice(),
)

const formRef = ref<FormInstance>()
const saving = ref(false)
const uploading = ref(false)

const form = reactive({
  productType: 'procurement' as 'procurement' | 'secondhand',
  name: '',
  category: '',
  price: 0,
  stock: 1,
  conditionPercent: 8,
  image: '',
  description: '',
})

watch(
  () => props.visible,
  (open) => {
    if (!open) return
    resetForm()
    if (props.defaultType) {
      form.productType = props.defaultType
    }
  },
)

watch(
  () => form.productType,
  () => {
    form.category = ''
  },
)

function resetForm() {
  Object.assign(form, {
    productType: 'procurement',
    name: '',
    category: '',
    price: 0,
    stock: 1,
    conditionPercent: 8,
    image: '',
    description: '',
  })
}

function validateImageFile(file: File) {
  if (file.size > MAX_IMAGE_MB * 1024 * 1024) {
    ElMessage.warning(`图片大小不能超过 ${MAX_IMAGE_MB}MB`)
    return false
  }
  return true
}

async function handleCoverUploadRequest(options: UploadRequestOptions) {
  const file = options.file as File
  if (!validateImageFile(file)) {
    options.onError?.(new Error('invalid size'))
    return
  }
  uploading.value = true
  try {
    const uploaded = await uploadPropertyMedia(file)
    form.image = uploaded.url
    ElMessage.success('封面图上传成功')
    options.onSuccess?.(uploaded)
  } catch (e) {
    ElMessage.error('封面图上传失败，请检查登录状态与网络')
    options.onError?.(e as Error)
  } finally {
    uploading.value = false
  }
}

function mapConditionPercentToEnum(percent: number): 'like_new' | 'good' | 'fair' {
  if (percent >= 9) return 'like_new'
  if (percent >= 7) return 'good'
  return 'fair'
}

const rules: FormRules = {
  productType: [{ required: true, message: '请选择商品类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'change' }],
  image: [{ required: true, message: '请上传封面图', trigger: 'change' }],
  conditionPercent: [
    {
      validator: (_rule, value, cb) => {
        if (form.productType !== 'secondhand') return cb()
        const n = Number(value)
        if (!Number.isFinite(n) || n < 1 || n > 10) {
          cb(new Error('成色请填写 1~10（几成新）'))
          return
        }
        cb()
      },
      trigger: 'change',
    },
  ],
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
        condition: mapConditionPercentToEnum(Number(form.conditionPercent || 0)),
        description: form.description || undefined,
        image: form.image || undefined,
      })
    }

    ElMessage.success('商品已提交')
    visibleInner.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

function onClosed() {
  resetForm()
}
</script>

<style scoped lang="scss">
.grid {
  display: grid;
  grid-template-columns: 1.35fr 0.85fr;
  gap: 14px;
  align-items: start;
}

.row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.row__item {
  margin-bottom: 4px;
}

.upload-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.cover-preview {
  margin-top: 10px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: #f8fafc;
  display: grid;
  gap: 10px;
  padding: 10px;
}

.cover-preview__img {
  width: 100%;
  height: 170px;
  border-radius: 12px;
  overflow: hidden;
}

.cover-preview__remove {
  justify-self: start;
}

.preview__title {
  font-size: 13px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 10px;
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

@media (max-width: 820px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .row {
    grid-template-columns: 1fr;
  }
}
</style>

