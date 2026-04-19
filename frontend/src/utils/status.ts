type TagType = 'warning' | 'success' | 'info' | 'primary' | 'danger'

export function toPropertyStatusKey(status: number | string): string {
  if (typeof status === 'number') {
    if (status === 0) return 'pending'
    if (status === 1) return 'published'
    if (status === 2) return 'offline'
  }
  return String(status || '')
}

export function propertyStatusLabel(status: number | string): string {
  const map: Record<string, string> = {
    pending: '待审核',
    rejected: '已驳回',
    vacant: '已上架',
    occupied: '已出租',
    reserved: '已下架',
    published: '已上架',
    rented: '已出租',
    offline: '已下架',
  }
  return map[toPropertyStatusKey(status)] ?? '未知'
}

export function propertyStatusClass(status: number | string): string {
  const map: Record<string, string> = {
    pending: 'pending',
    rejected: 'pending',
    vacant: 'active',
    occupied: 'inactive',
    reserved: 'inactive',
    published: 'active',
    rented: 'inactive',
    offline: 'inactive',
  }
  return map[toPropertyStatusKey(status)] || ''
}

export function toOrderStatusKey(status: number | string): string {
  if (typeof status === 'number') {
    if (status === 0) return 'pending'
    if (status === 1) return 'assigned'
    if (status === 2) return 'in_progress'
    if (status === 3) return 'completed'
    if (status === 4) return 'cancelled'
  }
  return String(status || '')
}

export function orderStatusLabel(status: number | string): string {
  const map: Record<string, string> = {
    pending: '待处理',
    assigned: '已确认',
    accepted: '已接单',
    in_progress: '服务中',
    completed: '已完成',
    cancelled: '已取消',
  }
  return map[toOrderStatusKey(status)] || '未知'
}

export function orderStatusType(status: number | string): TagType {
  const map: Record<string, TagType> = {
    pending: 'warning',
    assigned: 'primary',
    accepted: 'primary',
    in_progress: 'primary',
    completed: 'success',
    cancelled: 'info',
  }
  return map[toOrderStatusKey(status)] || 'info'
}

export function orderStatusClass(status: number | string): string {
  const map: Record<string, string> = {
    pending: 'pending',
    assigned: 'confirmed',
    accepted: 'confirmed',
    in_progress: 'processing',
    completed: 'completed',
    cancelled: 'cancelled',
  }
  return map[toOrderStatusKey(status)] || ''
}

export function orderStatusLabelForAdmin(status: number | string): string {
  const map: Record<string, string> = {
    pending: '待付款',
    assigned: '待服务',
    accepted: '已支付',
    in_progress: '进行中',
    completed: '已完成',
    cancelled: '已取消',
  }
  return map[toOrderStatusKey(status)] || String(status || '')
}

export function viewingStatusLabel(status: string): string {
  const map: Record<string, string> = {
    pending: '待确认',
    confirmed: '已确认',
    cancelled: '已取消',
    completed: '已完成',
  }
  return map[status] || status
}

export function viewingStatusType(status: string): TagType {
  const map: Record<string, TagType> = {
    pending: 'warning',
    confirmed: 'success',
    completed: 'success',
    cancelled: 'info',
  }
  return map[status] || 'info'
}
