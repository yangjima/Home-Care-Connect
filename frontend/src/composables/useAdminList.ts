import { ref, type Ref } from 'vue'

type FetchFilters = {
  keyword: string
  status: string
}

type FetchRows<T> = (filters: FetchFilters) => Promise<T[]>

type UseAdminListOptions = {
  keywordRef?: Ref<string>
  statusRef?: Ref<string>
}

export function useAdminList<T>(fetchRows: FetchRows<T>, options: UseAdminListOptions = {}) {
  const keyword = options.keywordRef ?? ref('')
  const statusFilter = options.statusRef ?? ref('')
  const loading = ref(false)
  const rows = ref<T[]>([])

  async function load() {
    loading.value = true
    try {
      rows.value = await fetchRows({
        keyword: keyword.value,
        status: statusFilter.value,
      })
    } finally {
      loading.value = false
    }
  }

  return {
    keyword,
    statusFilter,
    loading,
    rows,
    load,
  }
}
