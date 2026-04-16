/**
 * AI 聊天 API
 */
import { get, del } from '@/utils/request'

const WS_URL = import.meta.env.VITE_WS_URL || 'ws://127.0.0.1:8000'

// HTTP 对话
export function chat(message: string, sessionId: string = 'default') {
  return get<{ response: string; session_id: string }>('/ai/chat', { message, session_id: sessionId })
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
  const ws = new WebSocket(`${WS_URL}/api/ai/ws/chat?session_id=${sessionId}`)

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
