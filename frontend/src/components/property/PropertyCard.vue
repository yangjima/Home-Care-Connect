<template>
  <div class="property-card" @click="$router.push(`/properties/${property.id}`)">
    <div class="card-image">
      <img :src="(property.images && property.images[0]) || '/placeholder-property.jpg'" :alt="property.title" />
      <span class="card-tag">{{ property.type || '普通住宅' }}</span>
    </div>
    <div class="card-body">
      <h3 class="card-title">{{ property.title }}</h3>
      <p class="card-address">
        <el-icon><Location /></el-icon>
        {{ property.address }}
      </p>
      <div class="card-tags">
        <span>{{ property.area }}㎡</span>
        <span>{{ property.rooms }}室{{ property.livingRooms || 1 }}厅{{ property.bathrooms || 1 }}卫</span>
        <span>{{ property.decoration || '普通装修' }}</span>
      </div>
      <div class="card-footer">
        <span class="price">
          <em>¥</em>{{ property.price }}
          <span class="unit">/月</span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Location } from '@element-plus/icons-vue'
import type { Property } from '@/types'

defineProps<{ property: Partial<Property> }>()
</script>

<style scoped lang="scss">
.property-card {
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  overflow: hidden;
  box-shadow: var(--shadow-light);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-base);
  }
}

.card-image {
  position: relative;
  height: 180px;
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .card-tag {
    position: absolute;
    top: 12px;
    left: 12px;
    background: var(--color-primary);
    color: white;
    padding: 4px 10px;
    border-radius: 12px;
    font-size: 12px;
  }
}

.card-body {
  padding: var(--spacing-md);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-address {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 12px;

  span {
    font-size: 12px;
    color: var(--color-text-secondary);
    background: var(--color-bg-page);
    padding: 2px 8px;
    border-radius: 10px;
  }
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .price {
    color: #f56c6c;
    font-size: 20px;
    font-weight: 700;

    em {
      font-style: normal;
      font-size: 14px;
    }

    .unit {
      font-size: 12px;
      font-weight: 400;
      color: var(--color-text-secondary);
    }
  }
}
</style>
