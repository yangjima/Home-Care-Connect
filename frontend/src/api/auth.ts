/**
 * 认证 API
 */
import { get, post, put } from '@/utils/request'

// 登录
export function login(data: { username: string; password: string }) {
  return post<{ accessToken: string; refreshToken: string; expiresIn: number; user?: object }>('/user/auth/login', data)
}

// 邮箱注册（验证码）
export function registerByEmail(data: { email: string; code: string; password: string; confirmPassword: string; realName?: string }) {
  return post<object>('/user/auth/register-email', data)
}

// 发送邮箱验证码
export function sendCaptcha(email: string) {
  return post('/user/auth/send-code', { type: 'email', target: email })
}

// 刷新 Token
export function refreshToken(refreshToken: string) {
  return post<{ accessToken: string }>('/user/auth/refresh', { refreshToken })
}

// 获取用户信息
export function getUserInfo() {
  return get<object>('/user/auth/me')
}

// 更新用户信息
export function updateUser(id: number, data: {
  username?: string
  realName?: string
  phone?: string
  avatar?: string
  email?: string
}) {
  return put<object>(`/user/users/${id}`, data)
}

// 修改密码
export function updatePassword(id: number, data: { oldPassword: string; newPassword: string }) {
  return put(`/user/users/${id}/password`, data)
}

// 上传头像
export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return post<{ avatar: string; user?: object }>('/user/users/avatar', formData as unknown as object, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
