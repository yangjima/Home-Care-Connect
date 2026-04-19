<template>
  <div class="service-icon-picker">
    <el-radio-group v-model="mode" class="mode-row" @change="onModeChange">
      <el-radio-button label="preset">预设图标</el-radio-button>
      <el-radio-button label="upload">上传图片</el-radio-button>
    </el-radio-group>

    <div v-if="mode === 'preset'" class="preset-grid">
      <button
        v-for="icon in presets"
        :key="icon"
        type="button"
        class="preset-btn"
        :class="{ active: modelValue === icon && !isUrl(modelValue) }"
        :aria-pressed="modelValue === icon"
        @click="selectPreset(icon)"
      >
        <span class="preset-emoji">{{ icon }}</span>
      </button>
    </div>

    <div v-else class="upload-block">
      <el-upload
        class="upload-inline"
        :show-file-list="false"
        accept="image/*"
        :http-request="handleUpload"
      >
        <el-button type="primary" plain :loading="uploading">选择图片上传</el-button>
      </el-upload>
      <p class="hint">上传后将使用图片地址作为展示图标（与预设二选一）</p>
      <div v-if="modelValue && isUrl(modelValue)" class="preview">
        <img :src="modelValue" alt="图标预览" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { UploadRequestOptions } from 'element-plus'
import { ElMessage } from 'element-plus'
import { uploadPropertyMedia } from '@/api/property'

const PRESETS = ['🧹', '🧽', '🪣', '🧼', '✨', '🧴', '🔧', '🔩', '⚙️', '🛠️', '🔨', '📦', '🏠', '📋', '❓']

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const presets = PRESETS
const uploading = ref(false)

const mode = ref<'preset' | 'upload'>('preset')

function isUrl(s: string) {
  return /^https?:\/\//i.test(s || '')
}

function syncModeFromValue() {
  const v = props.modelValue || ''
  mode.value = isUrl(v) ? 'upload' : 'preset'
}

watch(
  () => props.modelValue,
  () => {
    syncModeFromValue()
  },
  { immediate: true },
)

function selectPreset(icon: string) {
  emit('update:modelValue', icon)
}

function onModeChange() {
  if (mode.value === 'preset' && isUrl(props.modelValue)) {
    emit('update:modelValue', '')
  }
  if (mode.value === 'upload' && props.modelValue && !isUrl(props.modelValue)) {
    emit('update:modelValue', '')
  }
}

async function handleUpload(options: UploadRequestOptions) {
  const file = options.file as File
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请上传图片文件')
    return
  }
  uploading.value = true
  try {
    const uploaded = await uploadPropertyMedia(file)
    emit('update:modelValue', uploaded.url)
    options.onSuccess?.(uploaded)
  } catch {
    options.onError?.(new Error('upload failed'))
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped lang="scss">
.service-icon-picker {
  width: 100%;
}

.mode-row {
  margin-bottom: 12px;
}

.preset-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preset-btn {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  border: 2px solid #e4e7ed;
  background: #fafafa;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.15s, background 0.15s;

  &:hover {
    border-color: #409eff;
    background: #ecf5ff;
  }

  &.active {
    border-color: #409eff;
    background: #ecf5ff;
  }
}

.preset-emoji {
  font-size: 22px;
  line-height: 1;
}

.upload-block {
  .hint {
    margin: 8px 0 0;
    font-size: 12px;
    color: #909399;
  }
}

.preview {
  margin-top: 12px;

  img {
    max-width: 120px;
    max-height: 120px;
    border-radius: 8px;
    object-fit: cover;
    border: 1px solid #ebeef5;
  }
}
</style>
