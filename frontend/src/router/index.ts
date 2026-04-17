/**
 * 路由配置
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useAuthStore } from '@/stores/auth'

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
    meta: { title: '二手交易' },
  },
  {
    path: '/ai',
    name: 'AIChat',
    component: () => import('@/views/ai/AIChatPage.vue'),
    meta: { title: 'AI 助手' },
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

  // 检查是否需要登录
  if (to.meta.requiresAuth) {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) {
      return next({ name: 'Login', query: { redirect: to.fullPath } })
    }
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
