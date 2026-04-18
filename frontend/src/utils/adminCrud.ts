export type AdminStatusValue = string | number | null | undefined

export type AdminStatusOption = {
  label: string
  value: string
}

export const DEFAULT_ADMIN_STATUS_OPTIONS: AdminStatusOption[] = [
  { label: '全部状态', value: '' },
  { label: '已上架', value: '1' },
  { label: '待审核', value: '2' },
  { label: '已下架', value: '0' },
]

export function normalizeAdminStatus(value: AdminStatusValue): string {
  if (value === null || value === undefined) return ''
  return String(value)
}

export function adminStatusText(value: AdminStatusValue): string {
  const normalized = normalizeAdminStatus(value)
  if (normalized === '1') return '已上架'
  if (normalized === '2') return '待审核'
  if (normalized === '0') return '已下架'
  return normalized
}

export function adminStatusTagType(value: AdminStatusValue): 'success' | 'warning' | 'info' {
  const normalized = normalizeAdminStatus(value)
  if (normalized === '1') return 'success'
  if (normalized === '2') return 'warning'
  return 'info'
}
