/**
 * 服务状态管理
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getServiceTypes, createServiceOrder, getMyOrders, cancelOrder, payOrder } from '@/api/service'
import type { ServiceType, ServiceOrder } from '@/types'

export const useServiceStore = defineStore('service', () => {
  const serviceTypes = ref<ServiceType[]>([])
  const myOrders = ref<ServiceOrder[]>([])
  const loading = ref(false)

  async function fetchServiceTypes(keyword?: string) {
    loading.value = true
    try {
      const result = await getServiceTypes({ keyword })
      serviceTypes.value = result as ServiceType[]
    } finally {
      loading.value = false
    }
  }

  async function createOrder(data: {
    serviceTypeId: number
    serviceTime: string
    serviceAddress: string
    remark?: string
    propertyId?: number
  }) {
    return await createServiceOrder(data)
  }

  async function fetchMyOrders(params?: { page?: number; size?: number; status?: string }) {
    loading.value = true
    try {
      const result = await getMyOrders({ page: 1, size: 10, ...params })
      myOrders.value = (result.list ?? result.records ?? []) as ServiceOrder[]
      return result
    } finally {
      loading.value = false
    }
  }

  async function cancel(id: number, reason?: string) {
    return await cancelOrder(id, reason)
  }

  async function pay(id: number, payMethod: string = 'wechat') {
    return await payOrder(id, payMethod)
  }

  return {
    serviceTypes,
    myOrders,
    loading,
    fetchServiceTypes,
    createOrder,
    fetchMyOrders,
    cancel,
    pay,
  }
})
