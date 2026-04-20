/**
 * 资产 API (采购 + 二手)
 */
import { get } from '@/utils/request'
import { toOptionalPageQuery } from '@/api/pagination'
import { createResourceApi } from '@/api/resource'
import type { PageParams, PageResult } from '@/types'

// ========== 采购商品 ==========

type ProcurementListParams = PageParams & { category?: string; sort?: string; status?: string }

const procurementApi = createResourceApi<ProcurementListParams>('/asset/procurement-products')

export const getProcurementList = procurementApi.list
export const getProcurementDetail = procurementApi.detail
export const createProcurementProduct = procurementApi.create
export const updateProcurementProduct = procurementApi.update
export const deleteProcurementProduct = procurementApi.remove

/** GET /asset/procurement-products/summary — 已上架商品总数（首页统计用） */
export function getProcurementSummary() {
  return get<{ totalOnShelf: number }>('/asset/procurement-products/summary')
}

export const approveProcurementListing = (id: number) => procurementApi.action(id, 'approve-listing')
export const rejectProcurementListing = (id: number) => procurementApi.action(id, 'reject-listing')

// ========== 二手物品 ==========

type SecondhandListParams = PageParams & { category?: string; condition?: string; status?: string }

const secondhandApi = createResourceApi<SecondhandListParams>('/asset/secondhand-items')

export const getSecondhandList = secondhandApi.list
export const getSecondhandDetail = secondhandApi.detail
export const createSecondhandItem = secondhandApi.create
export const updateSecondhandItem = secondhandApi.update
export const deleteSecondhandItem = secondhandApi.remove

export function getSecondhandSummary() {
  return get<{ totalOnSale: number; newThisWeek: number }>('/asset/secondhand-items/summary')
}

export const approveSecondhandListing = (id: number) => secondhandApi.action(id, 'approve-listing')
export const rejectSecondhandListing = (id: number) => secondhandApi.action(id, 'reject-listing')
export const submitSecondhandListing = (id: number) => secondhandApi.action(id, 'submit-listing')

export function getMySecondhandItems(params?: PageParams) {
  return get<PageResult<object>>('/asset/secondhand-items/my', toOptionalPageQuery(params))
}
