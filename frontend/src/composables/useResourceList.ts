import { ref } from 'vue'
import type { PageParams } from '@/types'

type AnyObject = Record<string, unknown>

type PagedResponse = {
  records?: unknown[]
  list?: unknown[]
  total?: number
}

type Fetcher<TParams extends PageParams> = (params: TParams) => Promise<unknown>

type UseResourceListOptions<TParams extends PageParams = PageParams> = {
  pageSize?: number
  extraParams?: () => Partial<TParams>
}

/**
 * 通用分页列表 composable — 适用于"我的 XX"类用户端页面。
 * 默认解析响应为 `records | list` + `total`。
 */
export function useResourceList<TRow extends AnyObject = AnyObject, TParams extends PageParams = PageParams>(
  fetcher: Fetcher<TParams>,
  options: UseResourceListOptions<TParams> = {},
) {
  const loading = ref(false)
  const rows = ref<TRow[]>([])
  const total = ref(0)
  const page = ref(1)
  const size = ref(options.pageSize ?? 10)

  async function load() {
    loading.value = true
    try {
      const params = { page: page.value, size: size.value, ...(options.extraParams?.() || {}) } as unknown as TParams
      const res = (await fetcher(params)) as PagedResponse
      rows.value = ((res?.records || res?.list || []) as TRow[]) ?? []
      total.value = res?.total ?? 0
    } finally {
      loading.value = false
    }
  }

  return { loading, rows, total, page, size, load }
}
