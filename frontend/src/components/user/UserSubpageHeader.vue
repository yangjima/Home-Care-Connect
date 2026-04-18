<template>
  <div class="user-subpage-header card">
    <div class="left">
      <el-button class="back-btn" text @click="handleBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <div class="title-wrap">
        <div class="title-row">
          <h1 class="title">{{ title }}</h1>
          <div v-if="$slots.actions" class="actions">
            <slot name="actions" />
          </div>
        </div>
        <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const props = defineProps<{
  title: string
  subtitle?: string
  fallbackTo?: string
}>()

const router = useRouter()

function canGoBack(): boolean {
  // SPA 环境下没有可靠“来源页”时，用 history 长度做保守判断
  return window.history.length > 1
}

function handleBack() {
  if (canGoBack()) {
    router.back()
    return
  }
  router.push(props.fallbackTo || '/user/profile')
}
</script>

<style scoped lang="scss">
.user-subpage-header {
  padding: var(--spacing-md) var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
}

.left {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
}

.back-btn {
  padding-left: 0;
  padding-right: 0;
  height: 28px;
  line-height: 28px;
}

.title-wrap {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-md);
}

.title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.actions {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.subtitle {
  margin: 4px 0 0;
  color: var(--color-text-secondary);
  font-size: 14px;
}
</style>

