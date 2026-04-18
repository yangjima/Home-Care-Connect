<template>
  <div class="admin-crud-toolbar">
    <el-input
      v-model="keywordModel"
      :placeholder="keywordPlaceholder"
      clearable
      :style="{ width: keywordWidth }"
      @keyup.enter="emit('search')"
    />
    <el-select
      v-model="statusModel"
      :placeholder="statusPlaceholder"
      clearable
      :style="{ width: statusWidth }"
      @change="emit('search')"
    >
      <el-option v-for="opt in statusOptions" :key="`${opt.value}-${opt.label}`" :label="opt.label" :value="opt.value" />
    </el-select>
    <el-button @click="emit('search')">刷新</el-button>
    <router-link :to="createPath">
      <el-button :type="createButtonType">{{ createText }}</el-button>
    </router-link>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { DEFAULT_ADMIN_STATUS_OPTIONS } from '@/utils/adminCrud'
import type { AdminStatusOption } from '@/utils/adminCrud'

const keywordModel = defineModel<string>('keyword', { required: true })
const statusModel = defineModel<string>('status', { required: true })

withDefaults(
  defineProps<{
    createPath: string
    createText: string
    createButtonType?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
    keywordPlaceholder?: string
    statusPlaceholder?: string
    keywordWidth?: string
    statusWidth?: string
    statusOptions?: AdminStatusOption[]
  }>(),
  {
    createButtonType: 'primary',
    keywordPlaceholder: '请输入关键词',
    statusPlaceholder: '状态筛选',
    keywordWidth: '260px',
    statusWidth: '140px',
    statusOptions: () => DEFAULT_ADMIN_STATUS_OPTIONS,
  },
)

const emit = defineEmits<{
  search: []
}>()
</script>

<style scoped lang="scss">
.admin-crud-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
</style>
