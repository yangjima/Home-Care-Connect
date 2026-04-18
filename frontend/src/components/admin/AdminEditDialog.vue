<template>
  <el-dialog :model-value="modelValue" :title="title" :width="width" destroy-on-close @update:model-value="emit('update:modelValue', $event)">
    <slot />
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ cancelText }}</el-button>
      <el-button :type="confirmType" :loading="loading" @click="emit('confirm')">{{ confirmText }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    width?: string
    loading?: boolean
    confirmText?: string
    cancelText?: string
    confirmType?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  }>(),
  {
    width: '620px',
    loading: false,
    confirmText: '保存',
    cancelText: '取消',
    confirmType: 'primary',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
}>()
</script>
