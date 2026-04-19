/**
 * 用户管理 API（店长后台）
 */
import { del, get, patch, post, put } from '@/utils/request'
import { toPageQuery } from '@/api/pagination'
import type { PageParams, PageResult, User, UserStats } from '@/types'

export function listUsers(params: PageParams & { role?: string; keyword?: string }) {
  return get<PageResult<User>>('/user/users', toPageQuery(params))
}

export function getUserStats() {
  return get<UserStats>('/user/users/stats')
}

export function createUserByAdmin(payload: {
  username: string
  password: string
  role: string
  realName?: string
  phone?: string
  email?: string
  /** 店长、服务人员等必填（店长创建时由后端强制为本店） */
  storeId?: number
}) {
  return post<User>('/user/users', payload)
}

export function updateUserStore(id: number, storeId: number | null) {
  return patch<User>(`/user/users/${id}/store`, { storeId })
}

export function getUserById(id: number) {
  return get<User>(`/user/users/${id}`)
}

/** 管理端更新资料（姓名、性别、手机等），用户名/邮箱不可改 */
export function updateUserProfile(
  id: number,
  payload: { realName?: string | null; gender?: string | null; phone?: string | null },
) {
  return put<User>(`/user/users/${id}`, payload)
}

export function updateUserStatus(id: number, status: string) {
  return patch<void>(`/user/users/${id}/status`, { status })
}

export function updateUserRole(id: number, role: string) {
  return patch<User>(`/user/users/${id}/role`, { role })
}

export function deleteUser(id: number) {
  return del<void>(`/user/users/${id}`)
}
