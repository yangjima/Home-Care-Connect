/**
 * 通用资源 API 工厂：封装 list/detail/create/update/delete 等样板调用
 */
import { get, post, put, del } from '@/utils/request'
import { toOptionalPageQuery, toPageQuery } from '@/api/pagination'
import type { PageParams, PageResult } from '@/types'

export type ResourceApi<TList extends PageParams, TDetail = object, TCreate = object, TUpdate = TCreate> = {
  list: (params: TList) => Promise<PageResult<TDetail>>
  optionalList: (params?: TList) => Promise<PageResult<TDetail>>
  detail: (id: number) => Promise<TDetail>
  create: (data: TCreate) => Promise<TDetail>
  update: (id: number, data: TUpdate) => Promise<TDetail>
  remove: (id: number) => Promise<unknown>
  action: <R = object>(id: number, path: string, body?: object) => Promise<R>
}

export function createResourceApi<
  TList extends PageParams = PageParams,
  TDetail = object,
  TCreate = object,
  TUpdate = TCreate,
>(basePath: string): ResourceApi<TList, TDetail, TCreate, TUpdate> {
  const trimmed = basePath.replace(/\/$/, '')
  const itemUrl = (id: number) => `${trimmed}/${id}`

  return {
    list: (params) => get<PageResult<TDetail>>(trimmed, toPageQuery(params)),
    optionalList: (params) => get<PageResult<TDetail>>(trimmed, toOptionalPageQuery(params)),
    detail: (id) => get<TDetail>(itemUrl(id)),
    create: (data) => post<TDetail>(trimmed, data as object),
    update: (id, data) => put<TDetail>(itemUrl(id), data as object),
    remove: (id) => del(itemUrl(id)),
    action: <R = object>(id: number, path: string, body?: object) =>
      post<R>(`${itemUrl(id)}/${path.replace(/^\//, '')}`, body),
  }
}
