import type { PageParams } from '@/types'

type RestPageParams<T extends PageParams> = Omit<T, 'page' | 'size'>

export type PageQuery<T extends PageParams> = RestPageParams<T> & {
  page: number
  pageSize: number
}

export function toPageQuery<T extends PageParams>(params: T): PageQuery<T> {
  const { page, size, ...rest } = params
  return { page, pageSize: size, ...rest }
}

export function toOptionalPageQuery<T extends PageParams>(params?: T): PageQuery<T> | undefined {
  return params ? toPageQuery(params) : undefined
}
