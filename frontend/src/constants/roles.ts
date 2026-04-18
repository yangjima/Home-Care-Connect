/**
 * 与后端 sys_user.role / JWT 一致：admin=超级管理员，store_manager=店长，supplier=商家，tenant|user=普通用户
 */
export const ROLE_ADMIN = 'admin'
export const ROLE_STORE_MANAGER = 'store_manager'
export const ROLE_MERCHANT = 'supplier'
export const ROLE_TENANT = 'tenant'
export const ROLE_USER = 'user'
export const ROLE_DISTRIBUTOR = 'distributor'
export const ROLE_SERVICE_STAFF = 'service_staff'

export function isPlatformAdmin(role?: string | null): boolean {
  return role === ROLE_ADMIN || role === ROLE_STORE_MANAGER
}

/** 可进入店长后台（含商家工作台） */
export function canAccessAdmin(role?: string | null): boolean {
  return isPlatformAdmin(role) || role === ROLE_MERCHANT
}

export function roleDisplayName(role?: string | null): string {
  switch (role) {
    case ROLE_ADMIN:
      return '超级管理员'
    case ROLE_STORE_MANAGER:
      return '店长'
    case ROLE_MERCHANT:
      return '商家'
    case ROLE_DISTRIBUTOR:
      return '分销员'
    case ROLE_SERVICE_STAFF:
      return '服务人员'
    case ROLE_TENANT:
    case ROLE_USER:
      return '普通用户'
    default:
      return '用户'
  }
}

export function hasAdminRouteRole(userRole: string | undefined, allowed: string[]): boolean {
  if (!userRole) return false
  return allowed.includes(userRole)
}
