import type { User } from '@/types'

const ROLE_LABELS: Record<string, string> = {
  admin: '管理员',
  store_manager: '店长',
  supplier: '商家',
  service_staff: '服务人员',
  distributor: '分销员',
  tenant: '用户',
  user: '用户',
}

export function roleText(r: string | null | undefined): string {
  const key = String(r ?? '')
  return ROLE_LABELS[key] || key
}

export function roleClass(r: string | null | undefined): string {
  if (r === 'admin' || r === 'store_manager') return 'role-admin'
  if (r === 'distributor') return 'role-distributor'
  return 'role-staff'
}

export function accountStatusText(s?: string | null): string {
  if (s === 'active') return '正常'
  if (s === 'inactive') return '已停用'
  if (s === 'banned') return '已封禁'
  return s || '—'
}

export function accountStatusClass(s?: string | null): string {
  if (s === 'active') return 'status-online'
  if (s === 'inactive') return 'status-offline'
  if (s === 'banned') return 'status-banned'
  return 'status-offline'
}

export function genderText(g?: string | null): string {
  if (g === 'male') return '男'
  if (g === 'female') return '女'
  return '—'
}

export function displayName(row: Partial<User>): string {
  return row.nickname || row.realName || row.username || `用户${row.id ?? ''}`
}

export function formatDateTime(s?: string | (number | string)[] | null): string {
  if (s == null || s === '') return '—'
  if (Array.isArray(s) && s.length >= 3) {
    const y = s[0]
    const mo = String(s[1]).padStart(2, '0')
    const d = String(s[2]).padStart(2, '0')
    const h = s.length > 3 ? String(s[3] ?? 0).padStart(2, '0') : '00'
    const mi = s.length > 4 ? String(s[4] ?? 0).padStart(2, '0') : '00'
    const se = s.length > 5 ? String(s[5] ?? 0).padStart(2, '0') : '00'
    return `${y}-${mo}-${d} ${h}:${mi}:${se}`
  }
  if (typeof s === 'string') {
    return s.replace('T', ' ').slice(0, 19)
  }
  return '—'
}
