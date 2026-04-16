/**
 * 二手交易状态管理
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getSecondhandList,
  getSecondhandDetail,
  createSecondhandItem,
  getMySecondhandItems,
} from '@/api/asset'
import type { SecondhandItem } from '@/types'

export const useSecondhandStore = defineStore('secondhand', () => {
  const itemList = ref<SecondhandItem[]>([])
  const myItems = ref<SecondhandItem[]>([])
  const currentItem = ref<SecondhandItem | null>(null)
  const categories = ref<string[]>([])
  const totalCount = ref(0)
  const loading = ref(false)

  async function fetchItemList(params: { page?: number; size?: number; category?: string; keyword?: string }) {
    loading.value = true
    try {
      // keyword 参数透传，由后端处理
      const result = await getSecondhandList({ page: 1, size: 12, ...params })
      itemList.value = result.list as SecondhandItem[]
      totalCount.value = result.total
    } finally {
      loading.value = false
    }
  }

  async function fetchItemDetail(id: number) {
    loading.value = true
    try {
      const result = await getSecondhandDetail(id)
      currentItem.value = result as SecondhandItem
    } finally {
      loading.value = false
    }
  }

  async function publishItem(data: {
    title: string
    description?: string
    category: string
    price: number
    condition: string
    images?: string[]
  }) {
    return await createSecondhandItem(data)
  }

  async function fetchMyItems(params?: { page?: number; size?: number }) {
    loading.value = true
    try {
      const result = await getMySecondhandItems({ page: 1, size: 10, ...params })
      myItems.value = result.list as SecondhandItem[]
      return result
    } finally {
      loading.value = false
    }
  }

  function setCategories(cats: string[]) {
    categories.value = cats
  }

  return {
    itemList,
    myItems,
    currentItem,
    categories,
    totalCount,
    loading,
    fetchItemList,
    fetchItemDetail,
    publishItem,
    fetchMyItems,
    setCategories,
  }
})