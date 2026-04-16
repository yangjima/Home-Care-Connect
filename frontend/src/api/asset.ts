/**
 * 资产 API (采购 + 二手)
 */
import { get, post, del, put } from '@/utils/request'
import type { PageParams, PageResult } from '@/types'

// ========== 采购商品 ==========

export function getProcurementList(params: PageParams & { category?: string }) {
  return get<PageResult<object>>('/procurement-products', params)
}

export function getProcurementDetail(id: number) {
  return get<object>(`/procurement-products/${id}`)
}

// ========== 二手物品 ==========

export function getSecondhandList(params: PageParams & { category?: string; condition?: string }) {
  return get<PageResult<object>>('/secondhand-items', params)
}

export function getSecondhandDetail(id: number) {
  return get<object>(`/secondhand-items/${id}`)
}

export function createSecondhandItem(data: object) {
  return post<object>('/secondhand-items', data)
}

export function updateSecondhandItem(id: number, data: object) {
  return put<object>(`/secondhand-items/${id}`, data)
}

export async function deleteSecondhandItem(id: number) {
  return await (await import('@/utils/request')).default.delete(`/secondhand-items/${id}`)
}

export function getMySecondhandItems(params?: PageParams) {
  return get<PageResult<object>>('/secondhand-items/my', params)
}

export function getSecondhandCategories() {
  return get<string[]>('/secondhand/categories')
}
