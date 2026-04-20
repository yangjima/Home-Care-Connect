<template>
  <div class="app-header">
    <div class="container header-inner">
      <div class="logo" @click="$router.push('/home')">
        <span class="logo-icon">🏠</span>
        <span class="logo-text">居服通</span>
      </div>

      <nav class="nav-links">
        <router-link to="/home" class="nav-link" :class="{ active: route.path === '/home' }">
          首页
        </router-link>
        <router-link to="/properties" class="nav-link" :class="{ active: route.path.startsWith('/properties') }">
          找房源
        </router-link>
        <router-link to="/services" class="nav-link" :class="{ active: route.path.startsWith('/services') }">
          找服务
        </router-link>
        <router-link to="/purchase" class="nav-link" :class="{ active: route.path === '/purchase' }">
          本地商城
        </router-link>
        <router-link
          to="/secondhand"
          class="nav-link"
          :class="{ active: route.path === '/secondhand' || route.path.startsWith('/secondhand/') }"
        >
          跳蚤市场
        </router-link>
        <router-link to="/ai" class="nav-link" :class="{ active: route.path === '/ai' }">
          AI 助手
        </router-link>
      </nav>

      <div class="header-actions">
        <template v-if="authStore.isLoggedIn">
          <el-dropdown @command="handleUserCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="authStore.userInfo?.avatar">
                {{ authStore.userInfo?.nickname?.[0] || authStore.userInfo?.username?.[0] || 'U' }}
              </el-avatar>
              <span class="username">{{ displayNickname }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item v-if="canAdmin" command="admin">
                  <el-icon><Shop /></el-icon>系统后台
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" plain size="small" @click="$router.push('/auth/login')">
            登录
          </el-button>
          <el-button type="primary" size="small" @click="$router.push('/auth/register')">
            注册
          </el-button>
        </template>
      </div>

      <el-button class="menu-toggle" :icon="Menu" text @click="mobileMenuOpen = !mobileMenuOpen" />
    </div>

    <!-- 移动端菜单 -->
    <div v-if="mobileMenuOpen" class="mobile-menu">
      <router-link to="/home" class="mobile-link" @click="mobileMenuOpen = false">首页</router-link>
      <router-link to="/properties" class="mobile-link" @click="mobileMenuOpen = false">找房源</router-link>
      <router-link to="/services" class="mobile-link" @click="mobileMenuOpen = false">找服务</router-link>
      <router-link to="/purchase" class="mobile-link" @click="mobileMenuOpen = false">本地商城</router-link>
      <router-link to="/secondhand" class="mobile-link" @click="mobileMenuOpen = false">跳蚤市场</router-link>
      <router-link to="/ai" class="mobile-link" @click="mobileMenuOpen = false">AI 助手</router-link>

      <div class="mobile-auth">
        <template v-if="authStore.isLoggedIn">
          <router-link to="/user/profile" class="mobile-link" @click="mobileMenuOpen = false">个人中心</router-link>
          <router-link v-if="canAdmin" to="/admin/dashboard" class="mobile-link" @click="mobileMenuOpen = false">
            系统后台
          </router-link>
          <button type="button" class="mobile-link mobile-link--button" @click="onMobileLogout">退出登录</button>
        </template>
        <template v-else>
          <router-link to="/auth/login" class="mobile-link" @click="mobileMenuOpen = false">登录</router-link>
          <router-link to="/auth/register" class="mobile-link mobile-link--primary" @click="mobileMenuOpen = false">
            注册
          </router-link>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { User, SwitchButton, Menu, Shop } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { canAccessAdmin } from '@/constants/roles'
import { computed } from 'vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const mobileMenuOpen = ref(false)
const canAdmin = computed(() => canAccessAdmin(authStore.userInfo?.role))
const displayNickname = computed(() => {
  const raw = authStore.userInfo?.nickname || authStore.userInfo?.username || ''
  return raw.slice(0, 12)
})

async function handleUserCommand(command: string) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示')
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/home')
  } else if (command === 'profile') {
    router.push('/user/profile')
  } else if (command === 'admin') {
    router.push('/admin/dashboard')
  }
}

async function onMobileLogout() {
  mobileMenuOpen.value = false
  await handleUserCommand('logout')
}
</script>

<style scoped lang="scss">
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--color-bg-white);
  box-shadow: var(--shadow-light);
  /* Mobile safe-area: avoid sticky header jitter/overlap under status bar */
  padding-top: env(safe-area-inset-top, 0px);
}

.header-inner {
  display: flex;
  align-items: center;
  height: 64px;
  gap: var(--spacing-lg);
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;

  .logo-icon {
    font-size: 28px;
  }

  .logo-text {
    font-size: 22px;
    font-weight: 700;
    color: var(--color-primary);
  }
}

.nav-links {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  flex: 1;
}

.nav-link {
  color: var(--color-text-regular);
  font-size: 15px;
  font-weight: 500;
  padding: 8px 4px;
  position: relative;
  transition: color 0.2s;

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 0;
    height: 2px;
    background: var(--color-primary);
    transition: width 0.2s;
  }

  &:hover,
  &.active {
    color: var(--color-primary);

    &::after {
      width: 100%;
    }
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--border-radius-base);
  transition: background 0.2s;

  &:hover {
    background: var(--color-bg-page);
  }

  .username {
    font-size: 14px;
    color: var(--color-text-primary);
    max-width: 80px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.menu-toggle {
  display: none;
}

.mobile-menu {
  display: none;
  flex-direction: column;
  padding: var(--spacing-md);
  background: var(--color-bg-white);
  border-top: 1px solid var(--border-color-light);
}

.mobile-link {
  display: block;
  width: 100%;
  text-align: left;
  padding: 12px 0;
  color: var(--color-text-primary);
  font-size: 15px;
  border-bottom: 1px solid var(--border-color-light);
  text-decoration: none;
  background: none;
  border-left: none;
  border-right: none;
  border-top: none;
  cursor: pointer;
  font: inherit;

  &:last-child {
    border-bottom: none;
  }
}

.mobile-link--primary {
  color: var(--color-primary);
  font-weight: 600;
}

/* 主菜单最后一项去掉底边，避免与下方账号区块形成「双线」 */
.mobile-menu > a.mobile-link:last-of-type {
  border-bottom: none;
  padding-bottom: 4px;
}

.mobile-auth {
  margin-top: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md) var(--spacing-md);
  background: var(--color-bg-page);
  border-radius: var(--border-radius-base);
}

@media (max-width: 768px) {
  .nav-links,
  .header-actions {
    display: none;
  }

  .menu-toggle {
    display: flex;
    margin-left: auto;
  }

  .mobile-menu {
    display: flex;
  }
}
</style>
