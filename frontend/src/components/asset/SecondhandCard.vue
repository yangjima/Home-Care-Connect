<template>
  <div class="item-card" @click="$router.push(`/secondhand/${item.id}`)">
    <div class="item-img">
      <img v-if="coverUrl" :src="coverUrl" :alt="item.title" class="cover" />
      <span v-else class="emoji-fallback">{{ categoryEmoji }}</span>
      <span class="item-condition">{{ conditionLabel }}</span>
      <button type="button" class="item-like" :aria-pressed="liked" @click.stop="toggleLike">
        {{ liked ? '❤️' : '🤍' }}
      </button>
    </div>
    <div class="item-body">
      <div class="item-name">{{ item.title }}</div>
      <div class="item-price-row">
        <span class="item-price">
          ¥{{ formatMoney(item.price) }}
          <small v-if="item.originalPrice && item.originalPrice > item.price">¥{{ formatMoney(item.originalPrice) }}</small>
        </span>
      </div>
      <div class="item-seller">
        <div class="seller-avatar">{{ sellerInitial }}</div>
        <span class="seller-name">{{ item.userName || '居友' }}</span>
        <span v-if="item.integrityTag" class="seller-tag">诚信</span>
      </div>
      <div v-if="item.location" class="item-location">📍 {{ item.location }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { SecondhandItem } from '@/types'

const props = defineProps<{ item: SecondhandItem }>()

const liked = ref(false)

const storageKey = computed(() => `secondhand-like-${props.item.id}`)

onMounted(() => {
  liked.value = localStorage.getItem(storageKey.value) === '1'
})

function toggleLike() {
  liked.value = !liked.value
  localStorage.setItem(storageKey.value, liked.value ? '1' : '0')
}

const CATEGORY_EMOJI: Record<string, string> = {
  家具家居: '🪑',
  数码电器: '📱',
  服饰箱包: '👗',
  书籍文具: '📚',
  运动户外: '🏃',
  绿植宠物: '🌱',
  家具: '🪑',
  家电: '📺',
}

const categoryEmoji = computed(() => CATEGORY_EMOJI[props.item.category] || '📦')

const imagesArr = computed((): string[] => {
  const im = props.item.images as unknown
  if (Array.isArray(im)) return im
  if (typeof im === 'string' && im.startsWith('[')) {
    try {
      return JSON.parse(im) as string[]
    } catch {
      return []
    }
  }
  return []
})

const coverUrl = computed(() => {
  if (props.item.image) return props.item.image
  if (imagesArr.value[0]) return imagesArr.value[0]
  return ''
})

const conditionLabel = computed(() => {
  const map: Record<string, string> = {
    like_new: '9成新',
    good: '8成新',
    fair: '7成新',
  }
  return map[props.item.condition] || props.item.condition || ''
})

const sellerInitial = computed(() => {
  const n = props.item.userName || ''
  const c = n.charAt(0)
  return c || '居'
})

function formatMoney(v: number) {
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}
</script>

<style scoped lang="scss">
$item-radius: 12px;
$primary: #2c7be5;
$price: #ff4d4f;

.item-card {
  background: #fff;
  border-radius: $item-radius;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s, box-shadow 0.3s;
  cursor: pointer;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  }
}

.item-img {
  width: 100%;
  height: 180px;
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56px;
  position: relative;

  .cover {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .emoji-fallback {
    user-select: none;
  }
}

.item-condition {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}

.item-like {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #fff;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  border: none;
  padding: 0;
  line-height: 1;
}

.item-body {
  padding: 14px;
}

.item-name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: #333;
  min-height: 40px;
}

.item-price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.item-price {
  font-size: 18px;
  font-weight: bold;
  color: $price;

  small {
    font-size: 12px;
    font-weight: normal;
    text-decoration: line-through;
    color: #bbb;
    margin-left: 5px;
  }
}

.item-seller {
  display: flex;
  align-items: center;
  gap: 8px;
}

.seller-avatar {
  width: 24px;
  height: 24px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  flex-shrink: 0;
}

.seller-name {
  font-size: 12px;
  color: #666;
}

.seller-tag {
  font-size: 10px;
  background: #e8f5e9;
  color: #4caf50;
  padding: 1px 6px;
  border-radius: 3px;
}

.item-location {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
}
</style>
