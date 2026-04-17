/**
 * 服务订单 API
 */
import { get, post, del } from '@/utils/request'
import type { PageParams, PageResult } from '@/types'

// 服务类型列表
export function getServiceTypes(params?: { keyword?: string }) {
  return get<object[]>('/service/service-types', params)
}

// 服务类型详情
export function getServiceTypeDetail(id: number) {
  return get<object>(`/service/service-types/${id}`)
}

// 创建服务订单
export function createServiceOrder(data: {
  serviceTypeId: number
  serviceTime: string
  serviceAddress: string
  remark?: string
  propertyId?: number
}) {
  return post<{ orderNo: string; id: number }>('/service/orders', data)
}

// 订单列表
export function getOrderList(params: PageParams & { status?: number }) {
  const { page, size, ...rest } = params
  return get<PageResult<object>>('/service/orders', { page, pageSize: size, ...rest })
}

// 订单详情
export function getOrderDetail(id: number) {
  return get<object>(`/service/orders/${id}`)
}

// 取消订单
export function cancelOrder(id: number, reason?: string) {
  return post(`/service/orders/${id}/cancel`, undefined, {
    params: { reason },
  })
}

// 支付订单
export function payOrder(id: number, payMethod: string = 'wechat') {
  return post(`/service/orders/${id}/pay`, undefined, {
    params: { payMethod },
  })
}

// 确认订单
export function confirmOrder(id: number) {
  return post(`/service/orders/${id}/confirm`)
}

// 完成订单
export function completeOrder(id: number) {
  return post(`/service/orders/${id}/complete`)
}

// 删除订单
export function deleteOrder(id: number) {
  return del(`/service/orders/${id}`)
}

// 我的订单
export function getMyOrders(params?: (PageParams & { status?: string })) {
  if (!params) {
    return get<PageResult<object>>('/service/orders/my')
  }
  const { page, size, ...rest } = params
  return get<PageResult<object>>('/service/orders/my', { page, pageSize: size, ...rest })
}

// 评价列表
export function getReviewList(params: PageParams & { orderId?: number; staffId?: number }) {
  const { page, size, ...rest } = params
  return get<PageResult<object>>('/service/reviews', { page, pageSize: size, ...rest })
}

// 创建评价
export function createReview(data: {
  orderId: number
  rating: number
  content?: string
  images?: string[]
  isAnonymous?: boolean
}) {
  return post('/service/reviews', data)
}
