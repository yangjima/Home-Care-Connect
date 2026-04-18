/**
 * 房源状态管理
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPropertyList, getPropertyDetail, createViewing } from '@/api/property'
import type { PropertyListSort } from '@/api/property'
import type { Property } from '@/types'

export const usePropertyStore = defineStore('property', () => {
  const propertyList = ref<Property[]>([])
  const totalCount = ref(0)
  const currentProperty = ref<Property | null>(null)
  const loading = ref(false)

  async function fetchPropertyList(params: {
    page?: number
    size?: number
    keyword?: string
    type?: string
    types?: string[]
    minPrice?: number
    maxPrice?: number
    statuses?: string[]
    facilities?: string[]
    sort?: PropertyListSort
  }) {
    loading.value = true
    try {
      const page = params.page ?? 1
      const size = params.size ?? 12
      const result = await getPropertyList({ ...params, page, size })
      propertyList.value = (result.list ?? result.records ?? []) as Property[]
      totalCount.value = Number(result.total ?? 0)
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

  /**
   * 预约看房。后端 `ViewingRequest` 仅接收 remark / viewingTime / propertyId，
   * 姓名与电话写入备注以便管家联系（与设计文档看房备注一致）。
   */
  async function bookViewing(data: {
    propertyId: number
    viewingTime: string
    visitorName?: string
    contactPhone?: string
    remark?: string
  }) {
    const parts: string[] = []
    if (data.visitorName?.trim())
      parts.push(`预约人：${data.visitorName.trim()}`)
    if (data.contactPhone?.trim())
      parts.push(`联系电话：${data.contactPhone.trim()}`)
    if (data.remark?.trim())
      parts.push(data.remark.trim())
    const remark = parts.length ? parts.join('\n') : undefined
    return await createViewing({
      propertyId: data.propertyId,
      viewingTime: data.viewingTime,
      remark,
    })
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
