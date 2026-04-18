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
  getSecondhandSummary,
} from '@/api/asset'
import type { SecondhandItem } from '@/types'

export const useSecondhandStore = defineStore('secondhand', () => {
  const itemList = ref<SecondhandItem[]>([])
  const myItems = ref<SecondhandItem[]>([])
  const currentItem = ref<SecondhandItem | null>(null)
  const categories = ref<string[]>([])
  const totalCount = ref(0)
  const loading = ref(false)
  const summary = ref({ totalOnSale: 0, newThisWeek: 0 })

  async function fetchItemList(params: { page?: number; size?: number; category?: string; keyword?: string; condition?: string }) {
    loading.value = true
    try {
      const page = params.page ?? 1
      const size = params.size ?? 12
      const result = await getSecondhandList({
        page,
        size,
        category: params.category,
        keyword: params.keyword,
        condition: params.condition,
      })
      itemList.value = (result.list ?? result.records ?? []) as SecondhandItem[]
      totalCount.value = Number(result.total ?? 0)
    } finally {
      loading.value = false
    }
  }

  async function fetchSummary() {
    try {
      const data = await getSecondhandSummary()
      summary.value = {
        totalOnSale: Number(data.totalOnSale ?? 0),
        newThisWeek: Number(data.newThisWeek ?? 0),
      }
    } catch {
      summary.value = { totalOnSale: 0, newThisWeek: 0 }
    }
  }

  async function fetchItemDetail(id: number) {
    loading.value = true
    try {
      const result = await getSecondhandDetail(id)
      currentItem.value = result as SecondhandItem
    } catch {
      currentItem.value = null
    } finally {
      loading.value = false
    }
  }

  async function publishItem(data: {
    title: string
    description?: string
    category: string
    price: number
    originalPrice?: number
    condition: string
    contact?: string
    location?: string
    images?: string[]
  }) {
    return await createSecondhandItem(data)
  }

  async function fetchMyItems(params?: { page?: number; size?: number }) {
    loading.value = true
    try {
      const result = await getMySecondhandItems({ page: 1, size: 10, ...params })
      myItems.value = (result.list ?? result.records ?? []) as SecondhandItem[]
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
    summary,
    fetchItemList,
    fetchItemDetail,
    fetchSummary,
    publishItem,
    fetchMyItems,
    setCategories,
  }
})