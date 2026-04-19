<template>
  <div class="user-layout">
    <AppHeader />
    <div class="user-container">
      <main class="user-main">
        <router-view v-slot="{ Component }">
          <Transition name="user-fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </router-view>
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppHeader from './AppHeader.vue'
import AppFooter from './AppFooter.vue'
import { useAuthStore } from '@/stores/auth'
import { ROLE_MERCHANT, isPlatformAdmin } from '@/constants/roles'
const authStore = useAuthStore()

const canListProperty = computed(() => {
  const r = authStore.userInfo?.role
  return isPlatformAdmin(r) || r === ROLE_MERCHANT
})
</script>

<style scoped lang="scss">
.user-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.user-container {
  flex: 1;
  max-width: 1440px;
  width: 100%;
  margin: 0 auto;
  padding: var(--spacing-lg);
  padding-left: max(var(--spacing-md), env(safe-area-inset-left));
  padding-right: max(var(--spacing-md), env(safe-area-inset-right));
  padding-bottom: max(var(--spacing-lg), env(safe-area-inset-bottom));
}

@media (max-width: 768px) {
  .user-container {
    padding: var(--spacing-md);
    padding-top: var(--spacing-sm);
    padding-left: max(var(--spacing-md), env(safe-area-inset-left));
    padding-right: max(var(--spacing-md), env(safe-area-inset-right));
  }
}

.user-main {
  flex: 1;
  min-width: 0;
}

.user-fade-enter-active,
.user-fade-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.user-fade-enter-from,
.user-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
</style>
