/**
 * 服务订单 API
 */
import { get, post } from '@/utils/request'
import { toOptionalPageQuery } from '@/api/pagination'
import { createResourceApi } from '@/api/resource'
import type { PageParams, PageResult, ServiceStaffPublic } from '@/types'

/** 公开：优秀服务人员（服务列表页） */
export function getStaffList() {
  return get<ServiceStaffPublic[]>('/service/staff')
}

// ========== 服务类型 ==========

type ServiceTypeData = {
  name: string
  description?: string
  category?: string
  price: number
  unit?: string
  icon?: string
}

const serviceTypeApi = createResourceApi<PageParams, object, ServiceTypeData>('/service/service-types')

// 服务类型列表（默认仅上架）
export function getServiceTypes(params?: { keyword?: string; activeOnly?: boolean }) {
  return get<object[]>('/service/service-types', { activeOnly: true, ...params })
}

export const getServiceTypeDetail = serviceTypeApi.detail
export const createServiceType = serviceTypeApi.create
export const updateServiceType = (
  id: number,
  data: ServiceTypeData & { category: string },
) => serviceTypeApi.update(id, data)
export const deleteServiceType = serviceTypeApi.remove

/** 平台管理员：待上架审核的服务类型 */
export function getPendingServiceTypes() {
  return get<object[]>('/service/service-types/admin/pending')
}

export const approveServiceTypeListing = (id: number) => serviceTypeApi.action(id, 'approve-listing')
export const rejectServiceTypeListing = (id: number) => serviceTypeApi.action(id, 'reject-listing')
export const submitServiceTypeListing = (id: number) => serviceTypeApi.action(id, 'submit-listing')

// ========== 服务订单 ==========

type OrderListParams = PageParams & { status?: string }

const orderApi = createResourceApi<OrderListParams>('/service/orders')

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

export const getOrderList = orderApi.list
export const getOrderDetail = orderApi.detail
export const deleteOrder = orderApi.remove

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

export const confirmOrder = (id: number) => orderApi.action(id, 'confirm')
export const completeOrder = (id: number) => orderApi.action(id, 'complete')

// 我的订单
export function getMyOrders(params?: OrderListParams) {
  return get<PageResult<object>>('/service/orders/my', toOptionalPageQuery(params))
}

// ========== 评价 ==========

type ReviewListParams = PageParams & { orderId?: number; staffId?: number }

const reviewApi = createResourceApi<ReviewListParams>('/service/reviews')

export const getReviewList = reviewApi.list

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
