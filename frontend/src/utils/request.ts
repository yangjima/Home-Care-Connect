/**
 * Axios HTTP 客户端封装
 */
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建 axios 实例
const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 添加 Token
    const token = localStorage.getItem('access_token')
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data

    if (code !== 200) {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message || '请求失败'))
    }

    return data
  },
  async (error: AxiosError) => {
    const status = error.response?.status

    if (status === 401) {
      // Token 过期，清除登录状态
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      localStorage.removeItem('user_info')

      ElMessage.error('登录已过期，请重新登录')

      const currentRoute = router.currentRoute.value
      if (currentRoute.name !== 'Login') {
        router.push({ name: 'Login', query: { redirect: currentRoute.fullPath } })
      }
    } else if (status === 403) {
      ElMessage.error('无权限访问')
    } else if (status === 404) {
      ElMessage.error('请求的资源不存在')
    } else if (status === 500) {
      ElMessage.error('服务器错误，请稍后重试')
    } else if (!error.response) {
      ElMessage.error('网络连接失败，请检查网络')
    }

    return Promise.reject(error)
  }
)

// 封装 GET 请求
export function get<T = unknown>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return apiClient.get(url, { params, ...config })
}

// 封装 POST 请求
export function post<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
  return apiClient.post(url, data, config)
}

// 封装 PUT 请求
export function put<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
  return apiClient.put(url, data, config)
}

// 封装 DELETE 请求
export function del<T = unknown>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return apiClient.delete(url, { params, ...config })
}

export default apiClient
