/**
 * 服务订单 API
 */
import { get, post, del, put } from '@/utils/request'
import type { PageParams, PageResult, ServiceStaffPublic } from '@/types'

/** 公开：优秀服务人员（服务列表页） */
export function getStaffList() {
  return get<ServiceStaffPublic[]>('/service/staff')
}

// 服务类型列表（默认仅上架）
export function getServiceTypes(params?: { keyword?: string; activeOnly?: boolean }) {
  return get<object[]>('/service/service-types', { activeOnly: true, ...params })
}

// 服务类型详情
export function getServiceTypeDetail(id: number) {
  return get<object>(`/service/service-types/${id}`)
}

export function createServiceType(data: {
  name: string
  description?: string
  category?: string
  price: number
  unit?: string
  icon?: string
}) {
  return post<object>('/service/service-types', data)
}

export function updateServiceType(
  id: number,
  data: { name: string; description?: string; category: string; price: number; unit?: string; icon?: string },
) {
  return put<object>(`/service/service-types/${id}`, data)
}

export function deleteServiceType(id: number) {
  return del(`/service/service-types/${id}`)
}

/** 平台管理员：待上架审核的服务类型 */
export function getPendingServiceTypes() {
  return get<object[]>('/service/service-types/admin/pending')
}

export function approveServiceTypeListing(id: number) {
  return post<object>(`/service/service-types/${id}/approve-listing`)
}

export function rejectServiceTypeListing(id: number) {
  return post<object>(`/service/service-types/${id}/reject-listing`)
}

export function submitServiceTypeListing(id: number) {
  return post<object>(`/service/service-types/${id}/submit-listing`)
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
export function getOrderList(params: PageParams & { status?: string }) {
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
