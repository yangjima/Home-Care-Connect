<template>
  <div class="admin-layout">
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="brand">
        <span class="brand-icon">⚙️</span>
        <span v-if="!isCollapsed" class="brand-text">系统后台</span>
      </div>
      <el-menu
        :default-active="activePath"
        class="sidebar-menu"
        :collapse="isCollapsed"
        router
      >
        <template v-for="item in visibleMenus" :key="item.path">
          <el-sub-menu v-if="item.children && item.children.length" :index="item.path">
            <template #title>
              <span class="menu-icon">{{ item.icon }}</span>
              <span>{{ item.label }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              <template #title>{{ child.label }}</template>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <span class="menu-icon">{{ item.icon }}</span>
            <template #title>{{ item.label }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </aside>

    <section class="main">
      <header class="header">
        <div>
          <h1 class="header-title">{{ currentTitle }}</h1>
          <div class="header-subtitle">系统后台 / {{ currentTitle }}</div>
        </div>
        <div class="header-actions">
          <el-button @click="isCollapsed = !isCollapsed">{{ isCollapsed ? '展开菜单' : '折叠菜单' }}</el-button>
          <el-button type="primary" plain @click="goHome">返回首页</el-button>
        </div>
      </header>

      <main class="page-content">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ROLE_ADMIN, ROLE_MERCHANT, ROLE_STORE_MANAGER } from '@/constants/roles'

type AdminMenuItem = {
  path: string
  label: string
  icon: string
  roles: string[]
  children?: Array<{
    path: string
    label: string
    roles: string[]
  }>
}

const adminMenus: AdminMenuItem[] = [
  { path: '/admin/dashboard', label: '数据看板', icon: '📊', roles: [ROLE_ADMIN, ROLE_STORE_MANAGER, ROLE_MERCHANT] },
  { path: '/admin/properties', label: '房源管理', icon: '🏠', roles: [ROLE_ADMIN, ROLE_STORE_MANAGER, ROLE_MERCHANT] },
  { path: '/admin/services', label: '服务管理', icon: '🛠️', roles: [ROLE_ADMIN, ROLE_STORE_MANAGER] },
  { path: '/admin/services/new', label: '添加服务', icon: '➕', roles: [ROLE_ADMIN, ROLE_STORE_MANAGER] },
  {
    path: '/admin/products',
    label: '商品管理',
    icon: '🛒',
    roles: [ROLE_ADMIN, ROLE_STORE_MANAGER, ROLE_MERCHANT],
    children: [
      { path: '/admin/products/procurement', label: '本地商城商品管理', roles: [ROLE_ADMIN, ROLE_STORE_MANAGER, ROLE_MERCHANT] },
      { path: '/admin/products/secondhand', label: '跳蚤市场商品管理', roles: [ROLE_ADMIN, ROLE_STORE_MANAGER, ROLE_MERCHANT] },
    ],
  },
  { path: '/admin/products/new', label: '添加商品', icon: '📦', roles: [ROLE_ADMIN, ROLE_STORE_MANAGER, ROLE_MERCHANT] },
  { path: '/admin/listing-review', label: '上架审批', icon: '✅', roles: [ROLE_ADMIN, ROLE_STORE_MANAGER] },
  { path: '/admin/orders', label: '订单管理', icon: '📋', roles: [ROLE_ADMIN, ROLE_STORE_MANAGER] },
  { path: '/admin/staff', label: '员工管理', icon: '👥', roles: [ROLE_ADMIN, ROLE_STORE_MANAGER] },
]

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapsed = ref(false)

const userRole = computed(() => authStore.userInfo?.role || '')

const visibleMenus = computed(() =>
  adminMenus
    .filter((item) => item.roles.includes(userRole.value))
    .map((item) => ({
      ...item,
      children: item.children?.filter((c) => c.roles.includes(userRole.value)) || [],
    })),
)

const activePath = computed(() => {
  for (const item of visibleMenus.value) {
    const childHit = item.children?.find((child) => route.path.startsWith(child.path))
    if (childHit) return childHit.path
  }
  const hit = visibleMenus.value.find((item) => route.path.startsWith(item.path))
  return hit?.path || '/admin/dashboard'
})

const currentTitle = computed(() => String(route.meta.title || '系统后台'))

function goHome() {
  router.push('/home')
}
</script>

<style scoped lang="scss">
.admin-layout {
  min-height: 100vh;
  display: flex;
  background: #f5f7fa;
}

.sidebar {
  width: 240px;
  background: #1f2a37;
  color: #fff;
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
}

.sidebar.collapsed {
  width: 64px;
}

.brand {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  font-weight: 600;
}

.brand-icon {
  font-size: 18px;
}

.brand-text {
  white-space: nowrap;
}

.sidebar-menu {
  border-right: none;
  background: transparent;
}

.sidebar-menu :deep(.el-menu) {
  border-right: none;
}

.sidebar-menu :deep(.el-menu-item) {
  color: #d1d5db;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: #374151;
  color: #fff;
}

.menu-icon {
  margin-right: 8px;
}

.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 14px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.header-title {
  margin: 0;
  font-size: 20px;
}

.header-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.page-content {
  padding: 20px;
}
</style>
