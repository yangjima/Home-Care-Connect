<template>
  <div class="auth-layout">
    <div class="auth-container">
      <div class="auth-brand">
        <h1>居服通</h1>
        <p>智慧社区服务管理平台</p>
      </div>
      <div class="auth-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped lang="scss">
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.auth-container {
  display: flex;
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  overflow: hidden;
  box-shadow: var(--shadow-dark);
  max-width: 900px;
  width: 90%;
}

.auth-brand {
  width: 350px;
  background: linear-gradient(135deg, #409eff 0%, #667eea 100%);
  padding: 60px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: white;

  h1 {
    font-size: 42px;
    font-weight: 700;
    margin-bottom: 16px;
  }

  p {
    font-size: 18px;
    opacity: 0.9;
  }
}

.auth-content {
  flex: 1;
  padding: 60px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 350px;
}

@media (max-width: 768px) {
  .auth-brand {
    display: none;
  }

  .auth-content {
    padding: 40px 24px;
  }
}
</style>
