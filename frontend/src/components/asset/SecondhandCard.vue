<template>
  <div class="secondhand-card" @click="$router.push(`/secondhand/${item.id}`)">
    <div class="item-image">
      <img :src="item.image || '/placeholder-secondhand.jpg'" :alt="item.title" />
      <el-tag class="condition-tag" size="small">{{ conditionLabel }}</el-tag>
    </div>
    <div class="item-info">
      <h3 class="item-title">{{ item.title }}</h3>
      <p class="item-desc">{{ item.description }}</p>
      <div class="item-footer">
        <span class="price"><em>¥</em>{{ item.price }}</span>
        <span class="views">{{ item.viewCount }} 浏览</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SecondhandItem } from '@/types'

const props = defineProps<{ item: Partial<SecondhandItem> }>()

const conditionLabel = computed(() => {
  const map: Record<string, string> = {
    new: '全新',
    like_new: '几乎全新',
    good: '良好',
    fair: '一般',
    poor: '较差',
  }
  return map[props.item.condition || ''] || props.item.condition || ''
})
</script>

<style scoped lang="scss">
.secondhand-card {
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  overflow: hidden;
  box-shadow: var(--shadow-light);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-base);
  }

  .item-image {
    position: relative;
    height: 180px;
    background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .condition-tag {
      position: absolute;
      top: 8px;
      right: 8px;
    }
  }

  .item-info {
    padding: var(--spacing-md);
  }

  .item-title {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 6px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .item-desc {
    font-size: 12px;
    color: var(--color-text-secondary);
    margin-bottom: var(--spacing-sm);
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    min-height: 32px;
  }

  .item-footer {
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
  }

  .views {
    font-size: 12px;
    color: var(--color-text-secondary);
  }
}
</style>
