/**
 * AI 聊天状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { chat, createChatWebSocket, type AIChatResponse } from '@/api/ai'
import type { ChatMessage, ChatSession } from '@/types'
import router from '@/router'

export const useAIStore = defineStore('ai', () => {
  const sessions = ref<ChatSession[]>([])
  const currentSessionId = ref<string>('default')
  const messages = ref<ChatMessage[]>([])
  const loading = ref(false)
  const currentAgent = ref<string>('')
  const autoRedirect = ref(false)

  let ws: WebSocket | null = null

  const currentSession = computed(() =>
    sessions.value.find((s) => s.id === currentSessionId.value)
  )

  function getOrCreateSession(sessionId: string): ChatSession {
    let session = sessions.value.find((s) => s.id === sessionId)
    if (!session) {
      session = { id: sessionId, messages: [] }
      sessions.value.push(session)
    }
    return session
  }

  function setCurrentSession(sessionId: string) {
    currentSessionId.value = sessionId
    const session = getOrCreateSession(sessionId)
    messages.value = session.messages
  }

  function addMessage(msg: ChatMessage) {
    messages.value.push({
      ...msg,
      timestamp: msg.timestamp || Date.now(),
    })
    // 同步到 session
    const session = getOrCreateSession(currentSessionId.value)
    session.messages = [...messages.value]
    session.lastMessage = msg.content
    session.lastTime = new Date().toISOString()
  }

  async function sendMessage(content: string) {
    // 添加用户消息
    addMessage({ role: 'user', content })

    loading.value = true
    try {
      const result = (await chat(content, currentSessionId.value)) as AIChatResponse | null
      const payload = result && typeof result === 'object' ? result : ({} as AIChatResponse)
      const rawReply = payload.reply
      const assistantReply =
        typeof rawReply === 'string' && rawReply.trim()
          ? rawReply.trim()
          : '抱歉，我暂时无法回答这个问题。'
      const msg: ChatMessage = {
        role: 'assistant',
        content: assistantReply,
        agent: payload.intent || currentAgent.value,
        redirect: payload.redirect ?? null,
        filters: payload.filters,
      }
      addMessage(msg)

      // 按设计文档：跳转应可取消；默认不自动跳转
      const confidence = typeof payload.confidence === 'number' ? payload.confidence : 0
      const subAction = payload.sub_action
      if (autoRedirect.value && payload.redirect && confidence > 0.8 && subAction === 'list') {
        await router.push(payload.redirect)
      }
    } catch (e) {
      const detail = e instanceof Error ? e.message : String(e)
      addMessage({
        role: 'assistant',
        content: `抱歉，请求失败（${detail}）。请确认本机网关、AI 服务与前端反代正常；使用真实大模型时请在环境变量中配置 DASHSCOPE_API_KEY。`,
      })
    } finally {
      loading.value = false
    }
  }

  function connectWebSocket(sessionId: string) {
    if (ws) {
      ws.close()
    }

    ws = createChatWebSocket(
      sessionId,
      (data: object) => {
        const event = data as any
        if (event.type === 'token' && event.content) {
          // 流式消息处理
          const lastMsg = messages.value[messages.value.length - 1]
          if (lastMsg && lastMsg.role === 'assistant' && lastMsg.id === 'streaming') {
            lastMsg.content += event.content
          } else {
            addMessage({ role: 'assistant', content: event.content, id: 'streaming' })
          }
        } else if (event.type === 'intent' && event.data) {
          currentAgent.value = event.data.intent || ''
          const lastMsg = messages.value[messages.value.length - 1]
          if (lastMsg && lastMsg.role === 'assistant' && lastMsg.id === 'streaming') {
            lastMsg.agent = currentAgent.value
          }
        } else if (event.type === 'result' && event.data) {
          const lastMsg = messages.value[messages.value.length - 1]
          if (lastMsg && lastMsg.role === 'assistant' && lastMsg.id === 'streaming') {
            lastMsg.redirect = event.data.redirect ?? null
            lastMsg.filters = event.data.filters
          }
        } else if (event.type === 'end') {
          // 结束流式消息
          const lastMsg = messages.value[messages.value.length - 1]
          if (lastMsg && lastMsg.id === 'streaming') {
            lastMsg.id = undefined
          }
        }
      },
      (error) => {
        console.error('WebSocket error:', error)
      }
    )

    setCurrentSession(sessionId)
  }

  function sendWebSocketMessage(content: string) {
    if (ws && ws.readyState === WebSocket.OPEN) {
      addMessage({ role: 'user', content })
      ws.send(JSON.stringify({ message: content, session_id: currentSessionId.value }))
    }
  }

  function disconnectWebSocket() {
    if (ws) {
      ws.close()
      ws = null
    }
  }

  return {
    sessions,
    currentSessionId,
    messages,
    loading,
    currentAgent,
    autoRedirect,
    currentSession,
    setCurrentSession,
    sendMessage,
    connectWebSocket,
    sendWebSocketMessage,
    disconnectWebSocket,
  }
})
