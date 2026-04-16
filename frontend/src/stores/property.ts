/**
 * 房源状态管理
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPropertyList, getPropertyDetail, createViewing } from '@/api/property'
import type { Property } from '@/types'

export const usePropertyStore = defineStore('property', () => {
  const propertyList = ref<Property[]>([])
  const totalCount = ref(0)
  const currentProperty = ref<Property | null>(null)
  const loading = ref(false)

  async function fetchPropertyList(params: { page?: number; size?: number; keyword?: string; type?: string; minPrice?: number; maxPrice?: number }) {
    loading.value = true
    try {
      const result = await getPropertyList({ page: 1, size: 12, ...params })
      propertyList.value = result.list as Property[]
      totalCount.value = result.total
    } finally {
      loading.value = false
    }
  }

  async function fetchPropertyDetail(id: number) {
    loading.value = true
    try {
      const result = await getPropertyDetail(id)
      currentProperty.value = result as Property
    } finally {
      loading.value = false
    }
  }

  async function bookViewing(data: { propertyId: number; viewingTime: string; contactPhone: string; remark?: string }) {
    return await createViewing(data)
  }

  return {
    propertyList,
    totalCount,
    currentProperty,
    loading,
    fetchPropertyList,
    fetchPropertyDetail,
    bookViewing,
  }
})
