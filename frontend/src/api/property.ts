/**
 * 房源 API
 */
import { get, post, put, del } from '@/utils/request'
import type { PageParams, PageResult } from '@/types'

// 房源列表
export function getPropertyList(params: PageParams & { type?: string; minPrice?: number; maxPrice?: number }) {
  const { page, size, type, ...rest } = params
  return get<PageResult<object>>('/property/properties', {
    page,
    pageSize: size,
    propertyType: type,
    ...rest,
  })
}

// 房源详情
export function getPropertyDetail(id: number) {
  return get<object>(`/property/properties/${id}`)
}

// 创建看房预约
export function createViewing(data: {
  propertyId: number
  viewingTime: string
  remark?: string
}) {
  return post('/property/viewings', data)
}

// 我的看房预约
export function getMyViewings(params?: PageParams) {
  if (!params) {
    return get<PageResult<object>>('/property/viewings')
  }
  const { page, size, ...rest } = params
  return get<PageResult<object>>('/property/viewings', { page, pageSize: size, ...rest })
}

// 创建房源
export function createProperty(data: {
  title: string
  description?: string
  propertyType: string
  rentPrice: number
  address: string
  area: number
  layout?: string
  floor?: number
  totalFloor?: number
}) {
  return post<object>('/property/properties', data)
}

// 更新房源
export function updateProperty(id: number, data: {
  title: string
  description?: string
  propertyType: string
  rentPrice: number
  address: string
  area: number
  layout?: string
  floor?: number
  totalFloor?: number
}) {
  return put<object>(`/property/properties/${id}`, data)
}

// 删除房源
export function deletePropertyById(id: number) {
  return del(`/property/properties/${id}`)
}

// 我的房源
export function getMyProperties(params: PageParams & { ownerId: number }) {
  const { page, size, ...rest } = params
  return get<PageResult<object>>('/property/properties', { page, pageSize: size, ...rest })
}
