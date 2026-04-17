/**
 * AI 聊天状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { chat, createChatWebSocket } from '@/api/ai'
import type { ChatMessage, ChatSession } from '@/types'
import router from '@/router'

export const useAIStore = defineStore('ai', () => {
  const sessions = ref<ChatSession[]>([])
  const currentSessionId = ref<string>('default')
  const messages = ref<ChatMessage[]>([])
  const loading = ref(false)
  const currentAgent = ref<string>('')

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
      const result = await chat(content, currentSessionId.value)
      const assistantReply = result.reply || '抱歉，我暂时无法回答这个问题。'
      addMessage({
        role: 'assistant',
        content: assistantReply,
        agent: result.intent || currentAgent.value,
      })

      // 按设计文档：识别出业务意图时跳转对应页面
      if (result.redirect) {
        await router.push(result.redirect)
      }
    } catch (e) {
      addMessage({
        role: 'assistant',
        content: '抱歉，我遇到了一些问题，请稍后再试。',
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
        const event = data as { type: string; content?: string; agent?: string }
        if (event.type === 'token' && event.content) {
          // 流式消息处理
          const lastMsg = messages.value[messages.value.length - 1]
          if (lastMsg && lastMsg.role === 'assistant' && lastMsg.id === 'streaming') {
            lastMsg.content += event.content
          } else {
            addMessage({ role: 'assistant', content: event.content, id: 'streaming' })
          }
        } else if (event.type === 'end') {
          // 结束流式消息
          const lastMsg = messages.value[messages.value.length - 1]
          if (lastMsg && lastMsg.id === 'streaming') {
            lastMsg.id = undefined
          }
        } else if (event.type === 'agent') {
          currentAgent.value = event.agent || ''
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
    currentSession,
    setCurrentSession,
    sendMessage,
    connectWebSocket,
    sendWebSocketMessage,
    disconnectWebSocket,
  }
})
