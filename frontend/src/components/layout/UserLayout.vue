<template>
  <div class="user-layout">
    <AppHeader />
    <div class="user-container">
      <aside class="user-sidebar">
        <div class="sidebar-menu">
          <router-link to="/user/profile" class="menu-item" :class="{ active: route.path === '/user/profile' }">
            <el-icon><User /></el-icon>
            个人资料
          </router-link>
          <router-link to="/user/orders" class="menu-item" :class="{ active: route.path === '/user/orders' }">
            <el-icon><List /></el-icon>
            我的订单
          </router-link>
          <router-link to="/user/properties" class="menu-item" :class="{ active: route.path === '/user/properties' }">
            <el-icon><House /></el-icon>
            我的房源
          </router-link>
        </div>
      </aside>
      <main class="user-main">
        <router-view />
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { User, List, House } from '@element-plus/icons-vue'
import AppHeader from './AppHeader.vue'
import AppFooter from './AppFooter.vue'

const route = useRoute()
</script>

<style scoped lang="scss">
.user-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.user-container {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: var(--spacing-lg);
  display: flex;
  gap: var(--spacing-lg);
}

.user-sidebar {
  width: 220px;
  flex-shrink: 0;
}

.sidebar-menu {
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-light);
  padding: var(--spacing-md);
  position: sticky;
  top: 88px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 12px var(--spacing-md);
  border-radius: var(--border-radius-base);
  color: var(--color-text-regular);
  font-size: 14px;
  transition: all 0.2s;
  margin-bottom: 4px;

  &:hover {
    background: var(--color-bg-page);
    color: var(--color-primary);
  }

  &.active {
    background: rgba(64, 158, 255, 0.1);
    color: var(--color-primary);
    font-weight: 500;
  }
}

.user-main {
  flex: 1;
  min-width: 0;
}

@media (max-width: 768px) {
  .user-sidebar {
    display: none;
  }
}
</style>
