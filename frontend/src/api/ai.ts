/**
 * AI 聊天 API
 */
import { get, post, del } from '@/utils/request'

export interface AIChatResponse {
  intent: 'property' | 'service' | 'procurement' | 'general'
  reply: string
  data?: Record<string, unknown>
  redirect?: string | null
  session_id: string
}

function getWsBaseUrl() {
  const envUrl = import.meta.env.VITE_WS_URL as string | undefined
  if (envUrl) return envUrl

  if (typeof window === 'undefined') return 'ws://127.0.0.1:8080'
  const isHttps = window.location.protocol === 'https:'
  const wsProtocol = isHttps ? 'wss:' : 'ws:'
  return `${wsProtocol}//${window.location.host}`
}

// HTTP 对话
export function chat(message: string, sessionId: string = 'default') {
  return post<AIChatResponse>('/ai/chat', { message, session_id: sessionId })
}

// 获取会话历史
export function getChatHistory(sessionId: string) {
  return get<object[]>(`/ai/sessions/${sessionId}/history`)
}

// 清除会话
export function clearSession(sessionId: string) {
  return del(`/ai/sessions/${sessionId}`)
}

// 创建 WebSocket 连接
export function createChatWebSocket(sessionId: string, onMessage: (data: object) => void, onError?: (e: Event) => void) {
  const wsBase = getWsBaseUrl()
  const ws = new WebSocket(`${wsBase}/api/ai/ws/chat?session_id=${encodeURIComponent(sessionId)}`)

  ws.onopen = () => {
    console.log('WebSocket 连接已建立')
  }

  ws.onmessage = (event) => {
    const data = JSON.parse(event.data)
    onMessage(data)
  }

  ws.onerror = (error) => {
    console.error('WebSocket 错误:', error)
    onError?.(error)
  }

  ws.onclose = () => {
    console.log('WebSocket 连接已关闭')
  }

  return ws
}
