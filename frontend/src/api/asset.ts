/**
 * 资产 API (采购 + 二手)
 */
import { get, post, del, put } from '@/utils/request'
import type { PageParams, PageResult } from '@/types'

// ========== 采购商品 ==========

export function getProcurementList(params: PageParams & { category?: string; sort?: string; status?: string }) {
  const { page, size, ...rest } = params
  return get<PageResult<object>>('/asset/procurement-products', { page, pageSize: size, ...rest })
}

/** GET /asset/procurement-products/summary — 已上架商品总数（首页统计用） */
export function getProcurementSummary() {
  return get<{ totalOnShelf: number }>('/asset/procurement-products/summary')
}

export function getProcurementDetail(id: number) {
  return get<object>(`/asset/procurement-products/${id}`)
}

export function createProcurementProduct(data: object) {
  return post<object>('/asset/procurement-products', data)
}

export function updateProcurementProduct(id: number, data: object) {
  return put<object>(`/asset/procurement-products/${id}`, data)
}

export function deleteProcurementProduct(id: number) {
  return del(`/asset/procurement-products/${id}`)
}

export function approveProcurementListing(id: number) {
  return post<object>(`/asset/procurement-products/${id}/approve-listing`)
}

export function rejectProcurementListing(id: number) {
  return post<object>(`/asset/procurement-products/${id}/reject-listing`)
}

// ========== 二手物品 ==========

export function getSecondhandList(params: PageParams & { category?: string; condition?: string; status?: string }) {
  const { page, size, ...rest } = params
  return get<PageResult<object>>('/asset/secondhand-items', { page, pageSize: size, ...rest })
}

export function getSecondhandSummary() {
  return get<{ totalOnSale: number; newThisWeek: number }>('/asset/secondhand-items/summary')
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

export function approveSecondhandListing(id: number) {
  return post<object>(`/asset/secondhand-items/${id}/approve-listing`)
}

export function rejectSecondhandListing(id: number) {
  return post<object>(`/asset/secondhand-items/${id}/reject-listing`)
}

export function submitSecondhandListing(id: number) {
  return post<object>(`/asset/secondhand-items/${id}/submit-listing`)
}

export function getMySecondhandItems(params?: PageParams) {
  if (!params) {
    return get<PageResult<object>>('/asset/secondhand-items/my')
  }
  const { page, size, ...rest } = params
  return get<PageResult<object>>('/asset/secondhand-items/my', { page, pageSize: size, ...rest })
}
