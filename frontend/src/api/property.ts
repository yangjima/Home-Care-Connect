/**
 * 房源 API
 */
import { get, post } from '@/utils/request'
import type { PageParams, PageResult } from '@/types'

// 房源列表
export function getPropertyList(params: PageParams & { type?: string; minPrice?: number; maxPrice?: number }) {
  return get<PageResult<object>>('/properties', params)
}

// 房源详情
export function getPropertyDetail(id: number) {
  return get<object>(`/properties/${id}`)
}

// 创建看房预约
export function createViewing(data: {
  propertyId: number
  viewingTime: string
  contactPhone: string
  remark?: string
}) {
  return post('/viewings', data)
}

// 我的看房预约
export function getMyViewings(params?: PageParams) {
  return get<PageResult<object>>('/viewings/my', params)
}
