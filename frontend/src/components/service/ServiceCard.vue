<template>
  <BaseCard class="service-card">
    <div class="service-icon">{{ service.icon || '🛠️' }}</div>
    <h3>{{ service.name }}</h3>
    <p class="service-desc">{{ service.description }}</p>
    <div class="service-footer">
      <span class="price">
        <em>¥</em>{{ service.price }}
        <span class="unit">/{{ service.unit || '次' }}</span>
      </span>
      <el-button type="primary" size="small" @click="$router.push(`/services/${service.id}`)">
        预约
      </el-button>
    </div>
  </BaseCard>
</template>

<script setup lang="ts">
import type { ServiceType } from '@/types'
import BaseCard from '@/components/common/BaseCard.vue'

defineProps<{ service: Partial<ServiceType> }>()
</script>

<style scoped lang="scss">
.service-card {
  text-align: center;

  :deep(.base-card__body) {
    padding: var(--spacing-lg);
  }

  .service-icon {
    font-size: 48px;
    margin-bottom: var(--spacing-md);
  }

  h3 {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 8px;
  }

  .service-desc {
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-bottom: var(--spacing-md);
    min-height: 36px;
  }

  .service-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .price {
    color: #f56c6c;
    font-size: 18px;
    font-weight: 700;

    em {
      font-style: normal;
      font-size: 12px;
    }

    .unit {
      font-size: 12px;
      font-weight: 400;
      color: var(--color-text-secondary);
    }
  }
}
</style>
