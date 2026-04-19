/**
 * 认证状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { login as apiLogin, registerByEmail as apiRegisterByEmail, getUserInfo, refreshToken as apiRefreshToken } from '@/api/auth'
import type { User } from '@/types'
import { encryptLoginPassword } from '@/utils/passwordCrypto'

const ONE_DAY_MS = 24 * 60 * 60 * 1000
const ACTIVITY_STORAGE_KEY = 'last_activity_at'
const ACTIVITY_TOUCH_INTERVAL_MS = 30 * 1000
const SESSION_CHECK_INTERVAL_MS = 60 * 1000
const TOKEN_REFRESH_INTERVAL_MS = 10 * 60 * 1000

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('access_token'))
  const refreshToken = ref<string | null>(localStorage.getItem('refresh_token'))
  const userInfo = ref<User | null>(null)
  const sessionInitialized = ref(false)
  const lastActivityTouchAt = ref(0)
  const isRefreshingToken = ref(false)

  const isLoggedIn = computed(() => !!accessToken.value)

  function getLastActivityAt() {
    const value = Number(localStorage.getItem(ACTIVITY_STORAGE_KEY) || '0')
    return Number.isFinite(value) ? value : 0
  }

  function updateLastActivity(now = Date.now()) {
    localStorage.setItem(ACTIVITY_STORAGE_KEY, String(now))
    lastActivityTouchAt.value = now
  }

  function clearActivity() {
    localStorage.removeItem(ACTIVITY_STORAGE_KEY)
    lastActivityTouchAt.value = 0
  }

  function redirectToLogin() {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.href = `/auth/login?redirect=${redirect}`
  }

  function handleSessionExpired(showMessage = true) {
    logout()
    if (showMessage) {
      ElMessage.error('登录已过期，请重新登录')
    }
    redirectToLogin()
  }

  function handleUserActivity() {
    if (!isLoggedIn.value) {
      return
    }

    const now = Date.now()
    if (now - lastActivityTouchAt.value >= ACTIVITY_TOUCH_INTERVAL_MS) {
      updateLastActivity(now)
    }
  }

  async function refreshAccessTokenIfNeeded() {
    if (!isLoggedIn.value || isRefreshingToken.value || !refreshToken.value) {
      return
    }

    const idleDuration = Date.now() - getLastActivityAt()
    if (idleDuration >= ONE_DAY_MS) {
      return
    }

    isRefreshingToken.value = true
    try {
      const result = await apiRefreshToken(refreshToken.value)
      if (!result?.accessToken) {
        handleSessionExpired(false)
        return
      }

      accessToken.value = result.accessToken
      localStorage.setItem('access_token', result.accessToken)

      if ((result as { refreshToken?: string }).refreshToken) {
        refreshToken.value = (result as { refreshToken?: string }).refreshToken || null
        if (refreshToken.value) {
          localStorage.setItem('refresh_token', refreshToken.value)
        }
      }
    } catch {
      handleSessionExpired(false)
    } finally {
      isRefreshingToken.value = false
    }
  }

  function startSessionLifecycle() {
    if (sessionInitialized.value) {
      return
    }

    sessionInitialized.value = true
    updateLastActivity(getLastActivityAt() || Date.now())

    const activityEvents: (keyof WindowEventMap)[] = ['click', 'keydown', 'mousemove', 'scroll', 'touchstart']
    activityEvents.forEach((eventName) => {
      window.addEventListener(eventName, handleUserActivity, { passive: true })
    })

    window.setInterval(() => {
      if (!isLoggedIn.value) {
        return
      }

      const idleDuration = Date.now() - getLastActivityAt()
      if (idleDuration >= ONE_DAY_MS) {
        handleSessionExpired()
      }
    }, SESSION_CHECK_INTERVAL_MS)

    window.setInterval(() => {
      void refreshAccessTokenIfNeeded()
    }, TOKEN_REFRESH_INTERVAL_MS)
  }

  function restoreToken() {
    const token = localStorage.getItem('access_token')
    const storedUser = localStorage.getItem('user_info')
    if (token) {
      accessToken.value = token
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser)
      }
      updateLastActivity(getLastActivityAt() || Date.now())
    }
  }

  async function login(email: string, password: string) {
    let encryptedPassword: string
    try {
      encryptedPassword = await encryptLoginPassword(password)
    } catch (e) {
      const msg = e instanceof Error ? e.message : '密码加密失败'
      ElMessage.error(msg)
      throw e
    }
    const result = await apiLogin({ username: email, password: encryptedPassword })
    accessToken.value = result.accessToken
    refreshToken.value = result.refreshToken
    userInfo.value = (result.user as User) || null

    localStorage.setItem('access_token', result.accessToken)
    localStorage.setItem('refresh_token', result.refreshToken)
    if (result.user) {
      localStorage.setItem('user_info', JSON.stringify(result.user))
    }
    updateLastActivity()
  }

  async function register(email: string, code: string, password: string, confirmPassword: string) {
    let encryptedPassword: string
    let encryptedConfirmPassword: string
    try {
      encryptedPassword = await encryptLoginPassword(password)
      encryptedConfirmPassword = await encryptLoginPassword(confirmPassword)
    } catch (e) {
      const msg = e instanceof Error ? e.message : '密码加密失败'
      ElMessage.error(msg)
      throw e
    }
    await apiRegisterByEmail({ email, code, password: encryptedPassword, confirmPassword: encryptedConfirmPassword })
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
    clearActivity()
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
    startSessionLifecycle,
    login,
    register,
    fetchUserInfo,
    setUserInfo,
    logout,
  }
})
