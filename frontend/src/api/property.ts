/**
 * 房源 API
 */
import { get, post, put } from '@/utils/request'
import { toOptionalPageQuery, toPageQuery } from '@/api/pagination'
import { createResourceApi } from '@/api/resource'
import type { PageParams, PageResult } from '@/types'

export type PropertyListSort =
  | 'comprehensive'
  | 'price_asc'
  | 'price_desc'
  | 'newest'
  | 'views'

const propertyApi = createResourceApi<PageParams>('/property/properties')

/** 与设计文档 GET /properties 查询参数一致 */
export function getPropertyList(
  params: PageParams & {
    type?: string
    types?: string[]
    minPrice?: number
    maxPrice?: number
    keyword?: string
    statuses?: string[]
    facilities?: string[]
    sort?: PropertyListSort
    ownerId?: number
  },
) {
  const { page, size, type, types, statuses, facilities, sort, ...rest } = params
  return get<PageResult<Record<string, unknown>>>('/property/properties', {
    page,
    pageSize: size,
    propertyType: type,
    types: types?.length ? types.join(',') : undefined,
    statuses: statuses?.length ? statuses.join(',') : undefined,
    facilities: facilities?.length ? facilities.join(',') : undefined,
    sort: sort ?? 'comprehensive',
    ...rest,
  })
}

export const getPropertyDetail = propertyApi.detail
export const deletePropertyById = propertyApi.remove

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
  return get<PageResult<object>>('/property/viewings', toOptionalPageQuery(params))
}

type PropertyData = {
  title: string
  description?: string
  propertyType: string
  rentPrice: number
  address: string
  area: number
  layout?: string
  floor?: number
  totalFloor?: number
  images?: string[]
  videos?: string[]
  /** 必须在 images 列表中，用于列表/卡片展示 */
  coverImage?: string
  /** 超级管理员指定房源所属门店 */
  storeId?: number
}

export function createProperty(data: PropertyData) {
  return post<object>('/property/properties', data)
}

export function updateProperty(id: number, data: PropertyData) {
  return put<object>(`/property/properties/${id}`, data)
}

// 上传房源媒体（图片/视频）
// 注意：不要手动设置 multipart Content-Type，否则缺少 boundary，网关/服务无法解析文件体
export function uploadPropertyMedia(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return post<{ url: string; mediaType: 'image' | 'video' }>(
    '/property/properties/media/upload',
    formData as unknown as object,
  )
}

export const publishProperty = (id: number) => propertyApi.action(id, 'publish')
export const approvePropertyListing = (id: number) => propertyApi.action(id, 'approve-listing')
export const rejectPropertyListing = (id: number) => propertyApi.action(id, 'reject-listing')
export const offlineProperty = (id: number) => propertyApi.action(id, 'offline')

export function recommendProperty(id: number, recommended: boolean) {
  return post<object>(`/property/properties/${id}/recommend`, {}, { params: { recommended } })
}

// 我的房源
export function getMyProperties(params: PageParams & { ownerId: number }) {
  return get<PageResult<object>>('/property/properties', toPageQuery(params))
}
