/**
 * 服务订单 API
 */
import { get, post } from '@/utils/request'
import type { PageParams, PageResult } from '@/types'

// 服务类型列表
export function getServiceTypes(params?: { keyword?: string }) {
  return get<object[]>('/service-types', params)
}

// 服务类型详情
export function getServiceTypeDetail(id: number) {
  return get<object>(`/service-types/${id}`)
}

// 创建服务订单
export function createServiceOrder(data: {
  serviceTypeId: number
  serviceTime: string
  serviceAddress: string
  remark?: string
  propertyId?: number
}) {
  return post<{ orderNo: string; id: number }>('/orders', data)
}

// 订单列表
export function getOrderList(params: PageParams & { status?: number }) {
  return get<PageResult<object>>('/orders', params)
}

// 订单详情
export function getOrderDetail(id: number) {
  return get<object>(`/orders/${id}`)
}

// 取消订单
export function cancelOrder(id: number, reason?: string) {
  return post(`/orders/${id}/cancel`, { reason })
}

// 支付订单
export function payOrder(id: number, payMethod: string = 'wechat') {
  return post(`/orders/${id}/pay`, { payMethod })
}

// 确认订单
export function confirmOrder(id: number) {
  return post(`/orders/${id}/confirm`)
}

// 完成订单
export function completeOrder(id: number) {
  return post(`/orders/${id}/complete`)
}

// 删除订单
export async function deleteOrder(id: number) {
  return await (await import('@/utils/request')).default.delete(`/orders/${id}`)
}

// 我的订单
export function getMyOrders(params?: PageParams) {
  return get<PageResult<object>>('/orders/my', params)
}

// 评价列表
export function getReviewList(params: PageParams & { orderId?: number; staffId?: number }) {
  return get<PageResult<object>>('/reviews', params)
}

// 创建评价
export function createReview(data: {
  orderId: number
  rating: number
  content?: string
  images?: string[]
  isAnonymous?: boolean
}) {
  return post('/reviews', data)
}
