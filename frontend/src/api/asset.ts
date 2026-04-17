/**
 * 资产 API (采购 + 二手)
 */
import { get, post, del, put } from '@/utils/request'
import type { PageParams, PageResult } from '@/types'

// ========== 采购商品 ==========

export function getProcurementList(params: PageParams & { category?: string }) {
  const { page, size, ...rest } = params
  return get<PageResult<object>>('/asset/procurement-products', { page, pageSize: size, ...rest })
}

export function getProcurementDetail(id: number) {
  return get<object>(`/asset/procurement-products/${id}`)
}

// ========== 二手物品 ==========

export function getSecondhandList(params: PageParams & { category?: string; condition?: string }) {
  const { page, size, ...rest } = params
  return get<PageResult<object>>('/asset/secondhand-items', { page, pageSize: size, ...rest })
}

export function getSecondhandDetail(id: number) {
  return get<object>(`/asset/secondhand-items/${id}`)
}

export function createSecondhandItem(data: object) {
  return post<object>('/asset/secondhand-items', data)
}

export function updateSecondhandItem(id: number, data: object) {
  return put<object>(`/asset/secondhand-items/${id}`, data)
}

export function deleteSecondhandItem(id: number) {
  return del(`/asset/secondhand-items/${id}`)
}

export function getMySecondhandItems(params?: PageParams) {
  if (!params) {
    return get<PageResult<object>>('/asset/secondhand-items/my')
  }
  const { page, size, ...rest } = params
  return get<PageResult<object>>('/asset/secondhand-items/my', { page, pageSize: size, ...rest })
}
