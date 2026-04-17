/**
 * 认证状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, registerByEmail as apiRegisterByEmail, getUserInfo } from '@/api/auth'
import type { User } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('access_token'))
  const refreshToken = ref<string | null>(localStorage.getItem('refresh_token'))
  const userInfo = ref<User | null>(null)

  const isLoggedIn = computed(() => !!accessToken.value)

  function restoreToken() {
    const token = localStorage.getItem('access_token')
    const storedUser = localStorage.getItem('user_info')
    if (token) {
      accessToken.value = token
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser)
      }
    }
  }

  async function login(email: string, password: string) {
    const result = await apiLogin({ username: email, password })
    accessToken.value = result.accessToken
    refreshToken.value = result.refreshToken
    userInfo.value = (result.user as User) || null

    localStorage.setItem('access_token', result.accessToken)
    localStorage.setItem('refresh_token', result.refreshToken)
    if (result.user) {
      localStorage.setItem('user_info', JSON.stringify(result.user))
    }
  }

  async function register(email: string, code: string, password: string, confirmPassword: string) {
    await apiRegisterByEmail({ email, code, password, confirmPassword })
    await login(email, password)
    await fetchUserInfo()
  }

  async function fetchUserInfo() {
    try {
      const info = await getUserInfo()
      userInfo.value = info as User
      localStorage.setItem('user_info', JSON.stringify(info))
    } catch {
      // ignore
    }
  }

  function logout() {
    accessToken.value = null
    refreshToken.value = null
    userInfo.value = null
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    localStorage.removeItem('user_info')
  }

  function setUserInfo(user: User | null) {
    userInfo.value = user
    if (user) {
      localStorage.setItem('user_info', JSON.stringify(user))
    } else {
      localStorage.removeItem('user_info')
    }
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    isLoggedIn,
    restoreToken,
    login,
    register,
    fetchUserInfo,
    setUserInfo,
    logout,
  }
})
