/**
 * 用户管理 API（店长后台）
 */
import { get, patch } from '@/utils/request'
import type { PageParams, PageResult, User } from '@/types'

export function listUsers(params: PageParams & { role?: string; keyword?: string }) {
  const { page, size, ...rest } = params
  return get<PageResult<User>>('/user/users', { page, pageSize: size, ...rest })
}

export function updateUserRole(id: number, role: string) {
  return patch<User>(`/user/users/${id}/role`, { role })
}
