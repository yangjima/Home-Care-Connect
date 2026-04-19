<template>
  <div
    class="admin-layout"
    :class="{ 'is-mobile': isMobile, 'nav-open': mobileNavOpen }"
  >
    <div
      v-if="isMobile && mobileNavOpen"
      class="sidebar-backdrop"
      aria-hidden="true"
      @click="mobileNavOpen = false"
    />
    <aside class="sidebar" :class="{ collapsed: isCollapsed && !isMobile }">
      <div class="brand">
        <span class="brand-icon">⚙️</span>
        <span v-if="!isCollapsed || isMobile" class="brand-text">系统后台</span>
        <button
          v-if="isMobile"
          type="button"
          class="sidebar-close-btn"
          aria-label="关闭菜单"
          @click="mobileNavOpen = false"
        >
          <el-icon :size="18">
            <Close />
          </el-icon>
        </button>
        <button
          v-else
          type="button"
          class="sidebar-collapse-btn"
          :title="isCollapsed ? '展开菜单' : '折叠菜单'"
          :aria-expanded="!isCollapsed"
          aria-label="折叠或展开侧栏"
          @click="isCollapsed = !isCollapsed"
        >
          <el-icon :size="16">
            <DArrowLeft v-if="!isCollapsed" />
            <DArrowRight v-else />
          </el-icon>
        </button>
      </div>
      <el-menu
        :default-active="activePath"
        class="sidebar-menu"
        :collapse="isCollapsed && !isMobile"
        router
        @select="onMenuSelect"
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
        <div class="header-leading">
          <button
            v-if="isMobile"
            type="button"
            class="header-menu-btn"
            aria-label="打开菜单"
            @click="mobileNavOpen = true"
          >
            <el-icon :size="22">
              <Menu />
            </el-icon>
          </button>
          <div class="header-titles">
            <h1 class="header-title">{{ currentTitle }}</h1>
            <div class="header-subtitle">系统后台 / {{ currentTitle }}</div>
          </div>
        </div>
        <div class="header-actions">
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
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useMediaQuery } from '@vueuse/core'
import { Close, DArrowLeft, DArrowRight, Menu } from '@element-plus/icons-vue'
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
  { path: '/admin/stores', label: '门店管理', icon: '🏪', roles: [ROLE_ADMIN] },
  { path: '/admin/properties', label: '房源管理', icon: '🏠', roles: [ROLE_ADMIN, ROLE_STORE_MANAGER, ROLE_MERCHANT] },
  { path: '/admin/services', label: '服务管理', icon: '🛠️', roles: [ROLE_ADMIN, ROLE_STORE_MANAGER] },
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
  { path: '/admin/staff', label: '用户管理', icon: '👥', roles: [ROLE_ADMIN, ROLE_STORE_MANAGER] },
]

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapsed = ref(false)
const isMobile = useMediaQuery('(max-width: 768px)')
const mobileNavOpen = ref(false)

watch(isMobile, (m) => {
  if (m) {
    mobileNavOpen.value = false
    isCollapsed.value = false
  }
})

watch(
  () => route.fullPath,
  () => {
    mobileNavOpen.value = false
  },
)

watch(
  [mobileNavOpen, isMobile],
  ([open, mobile]) => {
    if (typeof document === 'undefined') return
    document.body.style.overflow = mobile && open ? 'hidden' : ''
  },
  { immediate: true },
)

function onMenuSelect() {
  if (isMobile.value) mobileNavOpen.value = false
}

onBeforeUnmount(() => {
  if (typeof document !== 'undefined') document.body.style.overflow = ''
})

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
  padding: 0 12px 0 16px;
  font-weight: 600;
  flex-shrink: 0;
}

.brand-icon {
  font-size: 18px;
}

.brand-text {
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.sidebar-collapse-btn {
  margin-left: auto;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: #e5e7eb;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.14);
    color: #fff;
  }
}

.sidebar.collapsed .brand {
  padding: 0 8px;
  justify-content: center;
}

.sidebar.collapsed .sidebar-collapse-btn {
  margin-left: 0;
}

.sidebar-menu {
  border-right: none;
  background: transparent;
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.sidebar-menu :deep(.el-menu) {
  border-right: none;
}

.sidebar-menu :deep(.el-menu-item) {
  color: #d1d5db;
}

.sidebar-menu :deep(.el-sub-menu__title) {
  color: #d1d5db;
}

.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06) !important;
}

.sidebar-menu :deep(.el-menu--inline) {
  background: #18212f !important;
}

.sidebar-menu :deep(.el-menu--inline .el-menu-item) {
  background: transparent !important;
  color: #c4cdd5;
}

.sidebar-menu :deep(.el-menu--inline .el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06) !important;
  color: #f3f4f6;
}

.sidebar-menu :deep(.el-menu--inline .el-menu-item.is-active) {
  background: #374151 !important;
  color: #fff;
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
  flex: 1;
  min-width: 0;
  min-height: 0;
}

.sidebar-backdrop {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(15, 23, 42, 0.45);
  -webkit-tap-highlight-color: transparent;
}

.header-leading {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.header-titles {
  min-width: 0;
}

.header-menu-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 0;
  border: none;
  border-radius: 10px;
  background: #f3f4f6;
  color: #1f2937;
  cursor: pointer;
  transition: background 0.15s ease;

  &:active {
    background: #e5e7eb;
  }
}

.sidebar-close-btn {
  margin-left: auto;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  color: #e5e7eb;
  cursor: pointer;

  &:active {
    background: rgba(255, 255, 255, 0.16);
  }
}

@media (max-width: 768px) {
  .admin-layout {
    display: block;
    min-height: 100dvh;
  }

  .admin-layout.is-mobile .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 2001;
    width: min(88vw, 300px);
    max-width: 300px;
    height: 100vh;
    height: 100dvh;
    transform: translateX(-100%);
    transition: transform 0.22s ease;
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.18);
  }

  .admin-layout.is-mobile.nav-open .sidebar {
    transform: translateX(0);
  }

  .admin-layout.is-mobile .main {
    width: 100%;
    min-height: 100dvh;
  }

  .header {
    flex-wrap: wrap;
    align-items: flex-start;
    padding: 12px 14px;
    padding-top: max(12px, env(safe-area-inset-top));
    gap: 12px;
  }

  .header-title {
    font-size: 17px;
    line-height: 1.35;
  }

  .header-subtitle {
    font-size: 12px;
    margin-top: 2px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: min(70vw, 100%);
  }

  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .header-actions :deep(.el-button) {
    padding: 8px 12px;
  }

  .page-content {
    padding: 12px;
    padding-bottom: max(12px, env(safe-area-inset-bottom));
  }

  /* 管理页宽表格：横向滚动，避免挤压变形 */
  .page-content :deep(.table-card) {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .page-content :deep(.el-table) {
    min-width: 640px;
  }

  .page-content :deep(.toolbar) {
    padding: 14px 16px;
  }

  .page-content :deep(.pagination) {
    flex-direction: column;
    align-items: stretch;
    text-align: center;
  }
}
</style>
