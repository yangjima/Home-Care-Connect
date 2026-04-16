/**
 * 认证 API
 */
import { get, post } from '@/utils/request'

// 登录
export function login(data: { email: string; password: string }) {
  return post<{ accessToken: string; refreshToken: string; user: object }>('/auth/login', data)
}

// 注册
export function register(data: { email: string; password: string; confirmPassword: string }) {
  return post<{ accessToken: string; refreshToken: string; user: object }>('/auth/register', data)
}

// 发送验证码
export function sendCaptcha(email: string) {
  return post('/auth/captcha', { email })
}

// 刷新 Token
export function refreshToken(refreshToken: string) {
  return post<{ accessToken: string }>('/auth/refresh', { refreshToken })
}

// 获取用户信息
export function getUserInfo() {
  return get<object>('/users/me')
}
