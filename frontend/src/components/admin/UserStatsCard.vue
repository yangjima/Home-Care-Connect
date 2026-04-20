<template>
  <div class="stats-row">
    <div class="stat-card">
      <div class="stat-icon">👥</div>
      <div class="stat-value">{{ stats.total }}</div>
      <div class="stat-label">用户总数</div>
    </div>
    <div class="stat-card">
      <div class="stat-icon">🔧</div>
      <div class="stat-value">{{ stats.serviceStaff }}</div>
      <div class="stat-label">服务人员</div>
    </div>
    <div class="stat-card">
      <div class="stat-icon">📢</div>
      <div class="stat-value">{{ stats.distributor }}</div>
      <div class="stat-label">分销员</div>
    </div>
    <div class="stat-card">
      <div class="stat-icon">✅</div>
      <div class="stat-value">{{ stats.online }}</div>
      <div class="stat-label">在线</div>
      <div class="stat-hint">最近 {{ onlineHintMinutes }} 分钟内有登录</div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  stats: { total: number; serviceStaff: number; distributor: number; online: number }
  onlineHintMinutes: number
}>()
</script>

<style scoped lang="scss">
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}
@media (max-width: 900px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  text-align: center;
}
.stat-icon {
  font-size: 36px;
  margin-bottom: 10px;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
}
.stat-label {
  font-size: 14px;
  color: #999;
}
.stat-hint {
  font-size: 12px;
  color: #bbb;
  margin-top: 6px;
  line-height: 1.3;
}
</style>
