<template>
  <div class="not-found-page">
    <AppHeader />
    <div class="container">
      <div class="not-found-content">
        <div class="error-code">404</div>
        <h1>页面不存在</h1>
        <p>抱歉，您访问的页面不存在或已被删除</p>
        <div class="actions">
          <el-button type="primary" size="large" @click="$router.push('/home')">
            返回首页
          </el-button>
          <el-button size="large" @click="$router.back()">
            返回上一页
          </el-button>
        </div>
      </div>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
</script>

<style scoped lang="scss">
.not-found-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
}

.not-found-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: var(--spacing-xl) 0;
}

.error-code {
  font-size: 120px;
  font-weight: 900;
  color: var(--color-primary);
  opacity: 0.15;
  line-height: 1;
  margin-bottom: var(--spacing-lg);
  letter-spacing: -8px;
}

h1 {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: var(--spacing-sm);
}

p {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-xl);
}

.actions {
  display: flex;
  gap: var(--spacing-md);
}
</style>
