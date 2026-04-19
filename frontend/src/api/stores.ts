/**
 * 门店管理（超级管理员）
 */
import { get, post, put } from '@/utils/request'

export interface Store {
  id: number
  name: string
  address: string
  phone?: string | null
  managerId?: number | null
}

export function listStores() {
  return get<Store[]>('/user/stores')
}

export function createStore(data: { name: string; address: string; phone?: string }) {
  return post<Store>('/user/stores', data)
}

export function updateStore(id: number, data: { name: string; address: string; phone?: string }) {
  return put<Store>(`/user/stores/${id}`, data)
}
