<template>
  <div id="app">
    <router-view v-slot="{ Component, route }">
      <transition name="fade" mode="out-in">
        <component :is="Component" :key="route.path" />
      </transition>
    </router-view>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

onMounted(() => {
  // 尝试从 localStorage 恢复登录状态
  authStore.restoreToken()
  authStore.startSessionLifecycle()
})
</script>

<style>
#app {
  min-height: 100vh;
}
</style>
