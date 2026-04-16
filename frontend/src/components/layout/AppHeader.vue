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
        <router-link to="/secondhand" class="nav-link" :class="{ active: route.path === '/secondhand' }">
          二手交易
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
              <span class="username">{{ authStore.userInfo?.nickname || authStore.userInfo?.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="orders">
                  <el-icon><List /></el-icon>我的订单
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
      <router-link to="/secondhand" class="mobile-link" @click="mobileMenuOpen = false">二手交易</router-link>
      <router-link to="/ai" class="mobile-link" @click="mobileMenuOpen = false">AI 助手</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { User, List, SwitchButton, Menu } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const mobileMenuOpen = ref(false)

async function handleUserCommand(command: string) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示')
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/home')
  } else if (command === 'profile') {
    router.push('/user/profile')
  } else if (command === 'orders') {
    router.push('/user/orders')
  }
}
</script>

<style scoped lang="scss">
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--color-bg-white);
  box-shadow: var(--shadow-light);
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
  padding: 12px 0;
  color: var(--color-text-primary);
  font-size: 15px;
  border-bottom: 1px solid var(--border-color-light);

  &:last-child {
    border-bottom: none;
  }
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
