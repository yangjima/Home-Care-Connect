<template>
  <el-dialog :model-value="modelValue" title="用户详情" width="560px" destroy-on-close
    @update:model-value="emit('update:modelValue', $event as boolean)">
    <div v-loading="loading" class="dialog-body">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户名">{{ detail.username || '—' }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ detail.realName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ genderText(detail.gender) }}</el-descriptions-item>
          <el-descriptions-item label="手机">{{ detail.phone || '—' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ roleText(String(detail.role || '')) }}</el-descriptions-item>
          <el-descriptions-item label="门店">
            {{ detail.storeName || (detail.storeId != null ? `#${detail.storeId}` : '—') }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">{{ accountStatusText(detail.status) }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="最近登录">{{ formatDateTime(detail.lastLoginAt) }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import type { User } from '@/types'
import { accountStatusText, formatDateTime, genderText, roleText } from '@/utils/userFormat'

defineProps<{
  modelValue: boolean
  loading: boolean
  detail: User | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
}>()
</script>

<style scoped lang="scss">
.dialog-body {
  min-height: 80px;
}
</style>
