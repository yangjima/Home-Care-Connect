/**
 * 路由配置
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useAuthStore } from '@/stores/auth'
import {
  ROLE_ADMIN,
  ROLE_MERCHANT,
  ROLE_STORE_MANAGER,
  canAccessAdmin,
  hasAdminRouteRole,
} from '@/constants/roles'

NProgress.configure({ showSpinner: false })

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/home',
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/home/HomePage.vue'),
    meta: { title: '首页' },
  },
  {
    path: '/auth',
    component: () => import('@/views/auth/AuthLayout.vue'),
    children: [
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/auth/LoginPage.vue'),
        meta: { title: '登录' },
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/views/auth/RegisterPage.vue'),
        meta: { title: '注册' },
      },
    ],
  },
  {
    path: '/properties',
    name: 'PropertyList',
    component: () => import('@/views/property/PropertyListPage.vue'),
    meta: { title: '房源列表' },
  },
  {
    path: '/properties/:id',
    name: 'PropertyDetail',
    component: () => import('@/views/property/PropertyDetailPage.vue'),
    meta: { title: '房源详情' },
  },
  {
    path: '/services',
    name: 'ServiceList',
    component: () => import('@/views/service/ServiceListPage.vue'),
    meta: { title: '服务列表' },
  },
  {
    path: '/services/:id',
    name: 'ServiceOrder',
    component: () => import('@/views/service/ServiceOrderPage.vue'),
    meta: { title: '预约服务' },
  },
  {
    path: '/purchase',
    name: 'Purchase',
    component: () => import('@/views/asset/PurchasePage.vue'),
    meta: { title: '本地商城' },
  },
  {
    path: '/secondhand',
    name: 'SecondhandList',
    component: () => import('@/views/asset/SecondhandListPage.vue'),
    meta: { title: '跳蚤市场' },
  },
  {
    path: '/secondhand/:id(\\d+)',
    name: 'SecondhandDetail',
    component: () => import('@/views/asset/SecondhandDetailPage.vue'),
    meta: { title: '闲置详情' },
  },
  {
    path: '/ai',
    name: 'AIChat',
    component: () => import('@/views/ai/AIChatPage.vue'),
    meta: { title: 'AI 助手' },
  },
  {
    path: '/admin',
    component: () => import('@/components/layout/AdminLayout.vue'),
    meta: { requiresAuth: true, admin: true },
    children: [
      {
        path: '',
        redirect: { name: 'AdminDashboard' },
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/AdminDashboardPage.vue'),
        meta: {
          title: '数据看板',
          adminRoles: [ROLE_ADMIN, ROLE_STORE_MANAGER, ROLE_MERCHANT],
        },
      },
      {
        path: 'properties',
        name: 'AdminProperties',
        component: () => import('@/views/admin/AdminPropertyMgmtPage.vue'),
        meta: {
          title: '房源管理',
          adminRoles: [ROLE_ADMIN, ROLE_STORE_MANAGER, ROLE_MERCHANT],
        },
      },
      {
        path: 'listing-review',
        name: 'AdminListingReview',
        component: () => import('@/views/admin/AdminListingReviewPage.vue'),
        meta: {
          title: '上架审批',
          adminRoles: [ROLE_ADMIN, ROLE_STORE_MANAGER],
        },
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/AdminOrderMgmtPage.vue'),
        meta: {
          title: '订单管理',
          adminRoles: [ROLE_ADMIN, ROLE_STORE_MANAGER],
        },
      },
      {
        path: 'services',
        name: 'AdminServices',
        component: () => import('@/views/admin/AdminServiceMgmtPage.vue'),
        meta: {
          title: '服务管理',
          adminRoles: [ROLE_ADMIN, ROLE_STORE_MANAGER],
        },
      },
      {
        path: 'services/new',
        name: 'AdminServiceCreate',
        component: () => import('@/views/admin/AdminServiceCreatePage.vue'),
        meta: {
          title: '添加服务',
          adminRoles: [ROLE_ADMIN, ROLE_STORE_MANAGER],
        },
      },
      {
        path: 'products',
        name: 'AdminProducts',
        component: () => import('@/views/admin/AdminProductMgmtPage.vue'),
        meta: {
          title: '商品管理',
          adminRoles: [ROLE_ADMIN, ROLE_STORE_MANAGER, ROLE_MERCHANT],
        },
      },
      {
        path: 'products/procurement',
        name: 'AdminProductsProcurement',
        component: () => import('@/views/admin/AdminProductMgmtPage.vue'),
        meta: {
          title: '本地商城商品管理',
          adminRoles: [ROLE_ADMIN, ROLE_STORE_MANAGER, ROLE_MERCHANT],
        },
      },
      {
        path: 'products/secondhand',
        name: 'AdminProductsSecondhand',
        component: () => import('@/views/admin/AdminProductMgmtPage.vue'),
        meta: {
          title: '跳蚤市场商品管理',
          adminRoles: [ROLE_ADMIN, ROLE_STORE_MANAGER, ROLE_MERCHANT],
        },
      },
      {
        path: 'products/new',
        name: 'AdminProductCreate',
        component: () => import('@/views/admin/AdminProductCreatePage.vue'),
        meta: {
          title: '添加商品',
          adminRoles: [ROLE_ADMIN, ROLE_STORE_MANAGER, ROLE_MERCHANT],
        },
      },
      {
        path: 'staff',
        name: 'AdminStaff',
        component: () => import('@/views/admin/AdminStaffMgmtPage.vue'),
        meta: {
          title: '员工管理',
          adminRoles: [ROLE_ADMIN, ROLE_STORE_MANAGER],
        },
      },
    ],
  },
  {
    path: '/user',
    component: () => import('@/components/layout/UserLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/ProfilePage.vue'),
        meta: { title: '个人中心', requiresAuth: true },
      },
      {
        path: 'orders',
        name: 'MyOrders',
        component: () => import('@/views/user/OrdersPage.vue'),
        meta: { title: '我的订单', requiresAuth: true },
      },
      {
        path: 'properties',
        name: 'MyProperties',
        component: () => import('@/views/user/MyPropertiesPage.vue'),
        meta: { title: '我的房源', requiresAuth: true },
      },
      {
        path: 'viewings',
        name: 'MyViewings',
        component: () => import('@/views/user/MyViewingsPage.vue'),
        meta: { title: '我的看房', requiresAuth: true },
      },
      {
        path: 'secondhand',
        name: 'MySecondhand',
        component: () => import('@/views/user/MySecondhandPage.vue'),
        meta: { title: '跳蚤市场发布', requiresAuth: true },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundPage.vue'),
    meta: { title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  NProgress.start()

  // 设置页面标题
  document.title = `${to.meta.title || '居服通'} - 居服通`

  const authStore = useAuthStore()

  // 检查是否需要登录
  if (to.meta.requiresAuth) {
    if (!authStore.isLoggedIn) {
      return next({ name: 'Login', query: { redirect: to.fullPath } })
    }
  }

  if (to.matched.some((r) => r.meta.admin)) {
    const r = authStore.userInfo?.role
    if (!canAccessAdmin(r)) {
      return next({ path: '/home' })
    }
    const withRoles = [...to.matched].reverse().find((x) => x.meta?.adminRoles)
    if (withRoles?.meta?.adminRoles) {
      const allowed = withRoles.meta.adminRoles as string[]
      if (!hasAdminRouteRole(r, allowed)) {
        return next({ name: 'AdminDashboard' })
      }
    }
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
