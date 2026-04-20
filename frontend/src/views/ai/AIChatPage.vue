<template>
  <div class="ai-chat-page">
    <AppHeader />
    <div class="chat-container">
      <!-- 侧边栏 -->
      <aside class="chat-sidebar">
        <div class="sidebar-header">
          <h2>AI 助手</h2>
          <el-button text @click="startNewSession">
            <el-icon><Plus /></el-icon>
          </el-button>
        </div>
        <div class="session-list">
          <div
            v-for="session in aiStore.sessions"
            :key="session.id"
            class="session-item"
            :class="{ active: session.id === aiStore.currentSessionId }"
            @click="switchSession(session.id)"
          >
            <div class="session-title">{{ session.title || session.lastMessage?.slice(0, 20) || '新对话' }}</div>
            <div class="session-time">{{ formatTime(session.lastTime) }}</div>
          </div>
          <div v-if="aiStore.sessions.length === 0" class="no-sessions">
            暂无会话记录
          </div>
        </div>
      </aside>

      <!-- 聊天主区域 -->
      <main class="chat-main">
        <div class="messages" ref="messagesRef">
          <div v-if="aiStore.messages.length === 0" class="welcome-area">
            <div class="welcome-icon">🤖</div>
            <h2>您好，我是居服通 AI 助手</h2>
            <p>我可以帮您：</p>
            <ul>
              <li>查找合适的房源</li>
              <li>推荐社区服务</li>
              <li>了解采购商品信息</li>
              <li>解答各类问题</li>
            </ul>
          </div>

          <div
            v-for="(msg, index) in aiStore.messages"
            :key="index"
            class="message-item"
            :class="msg.role"
          >
            <div class="message-avatar">
              <span v-if="msg.role === 'user'">👤</span>
              <span v-else>🤖</span>
            </div>
            <div class="message-content">
              <div v-if="msg.agent && msg.role === 'assistant'" class="agent-tag">{{ msg.agent }}</div>
              <div
                v-if="msg.role === 'assistant'"
                class="message-text is-markdown"
                v-html="renderMarkdown(msg.content)"
              ></div>
              <div v-else class="message-text user-text">{{ msg.content }}</div>
              <div v-if="msg.role === 'assistant' && msg.redirect" class="redirect-row">
                <el-button size="small" type="primary" plain @click="goRedirect(msg.redirect)">
                  前往相关页面 →
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="aiStore.loading" class="message-item assistant">
            <div class="message-avatar">🤖</div>
            <div class="message-content">
              <div class="typing-indicator">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input-area">
          <div class="input-wrapper">
            <el-input
              ref="inputRef"
              v-model="inputText"
              type="textarea"
              :rows="2"
              placeholder="输入您的问题..."
              resize="none"
            />
            <el-button
              type="primary"
              :loading="aiStore.loading"
              :disabled="!inputText.trim()"
              @click="handleSend"
            >
              <el-icon><Promotion /></el-icon>
              发送
            </el-button>
          </div>
          <div class="input-tips">
            按 Enter 发送，Shift + Enter 换行
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import type { InputInstance } from 'element-plus'
import { Plus, Promotion } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import { useAIStore } from '@/stores/ai'
import router from '@/router'
import { renderMarkdown } from '@/utils/markdown'

const aiStore = useAIStore()

const inputText = ref('')
const messagesRef = ref<HTMLElement>()
const inputRef = ref<InputInstance>()

function bindTextareaEnter() {
  const ta = inputRef.value?.textarea
  if (!ta) return
  const onKeydown = (e: KeyboardEvent) => {
    if (e.key !== 'Enter' || e.shiftKey) return
    e.preventDefault()
    void handleSend()
  }
  ta.addEventListener('keydown', onKeydown)
  return () => ta.removeEventListener('keydown', onKeydown)
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

async function goRedirect(url?: string | null) {
  if (!url) return
  await router.push(url)
  scrollToBottom()
}

async function handleSend() {
  if (!inputText.value.trim() || aiStore.loading) return

  const text = inputText.value.trim()
  inputText.value = ''

  await aiStore.sendMessage(text)
  scrollToBottom()
}

function switchSession(sessionId: string) {
  aiStore.setCurrentSession(sessionId)
  scrollToBottom()
}

function startNewSession() {
  const id = `session_${Date.now()}`
  aiStore.setCurrentSession(id)
}

function formatTime(time?: string): string {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return d.toLocaleDateString()
}

let unbindTextarea: (() => void) | undefined

onMounted(async () => {
  // 默认创建一个会话
  if (aiStore.sessions.length === 0) {
    startNewSession()
  }
  scrollToBottom()
  await nextTick()
  unbindTextarea = bindTextareaEnter()
})

onUnmounted(() => {
  unbindTextarea?.()
  aiStore.disconnectWebSocket()
})
</script>

<style scoped lang="scss">
.ai-chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
  overflow: hidden;
}

.chat-container {
  flex: 1;
  display: flex;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  padding: var(--spacing-lg);
  gap: var(--spacing-lg);
  min-height: 0;
  overflow: hidden;
}

.chat-sidebar {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-light);
  overflow: hidden;
}

.sidebar-header {
  padding: var(--spacing-md);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--color-border-light);

  h2 {
    font-size: 16px;
    font-weight: 700;
  }
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-sm);
}

.session-item {
  padding: 10px var(--spacing-sm);
  border-radius: var(--border-radius-base);
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;

  &:hover {
    background: var(--color-bg-page);
  }

  &.active {
    background: rgba(64, 158, 255, 0.1);
  }

  .session-title {
    font-size: 13px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-bottom: 4px;
  }

  .session-time {
    font-size: 11px;
    color: var(--color-text-secondary);
  }
}

.no-sessions {
  text-align: center;
  padding: var(--spacing-lg);
  color: var(--color-text-secondary);
  font-size: 13px;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-light);
  overflow: hidden;
  min-height: 0;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-lg);
}

.welcome-area {
  text-align: center;
  padding: calc(var(--spacing-xl) * 2);

  .welcome-icon {
    font-size: 64px;
    margin-bottom: var(--spacing-md);
  }

  h2 {
    font-size: 20px;
    margin-bottom: var(--spacing-sm);
  }

  p {
    color: var(--color-text-secondary);
    margin-bottom: var(--spacing-sm);
  }

  ul {
    list-style: none;
    padding: 0;
    color: var(--color-text-regular);

    li {
      padding: 4px 0;
    }
  }
}

.message-item {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);

  &.user {
    flex-direction: row-reverse;

    .message-text {
      background: var(--color-primary);
      color: #fff;
    }
  }

  &.assistant {
    .message-text {
      background: var(--color-bg-page);
      color: var(--color-text-regular);
    }
  }

  .message-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: var(--color-bg-page);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    flex-shrink: 0;
  }

  .message-content {
    max-width: 70%;
  }

  .agent-tag {
    font-size: 11px;
    color: var(--color-primary);
    margin-bottom: 4px;
  }

  .message-text {
    padding: var(--spacing-sm) var(--spacing-md);
    border-radius: var(--border-radius-base);
    line-height: 1.6;
    font-size: 14px;
    word-break: break-word;
  }

  .message-text.user-text {
    white-space: pre-wrap;
  }

  /* Markdown content styling (scoped + v-html) */
  .message-text.is-markdown {
    :deep(p) {
      margin: 0 0 10px;
    }

    :deep(p:last-child) {
      margin-bottom: 0;
    }

    :deep(ul),
    :deep(ol) {
      margin: 6px 0 10px;
      padding-left: 18px;
    }

    :deep(li) {
      margin: 4px 0;
    }

    :deep(code) {
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
      font-size: 12px;
      background: rgba(0, 0, 0, 0.06);
      padding: 2px 5px;
      border-radius: 6px;
    }

    :deep(pre) {
      margin: 8px 0 10px;
      background: #0b1020;
      color: #e7eaf3;
      padding: 10px 12px;
      border-radius: 10px;
      overflow: auto;
    }

    :deep(pre code) {
      background: transparent;
      padding: 0;
      color: inherit;
      font-size: 12px;
      white-space: pre;
    }

    :deep(blockquote) {
      margin: 8px 0 10px;
      padding: 6px 10px;
      border-left: 3px solid var(--color-primary);
      background: rgba(64, 158, 255, 0.08);
      color: var(--color-text-regular);
    }

    :deep(a) {
      color: var(--color-primary);
      text-decoration: underline;
      word-break: break-all;
    }
  }

  .redirect-row {
    margin-top: 8px;
  }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: var(--spacing-sm) var(--spacing-md);

  span {
    width: 8px;
    height: 8px;
    background: var(--color-text-secondary);
    border-radius: 50%;
    animation: bounce 1.4s infinite ease-in-out;

    &:nth-child(1) { animation-delay: 0s; }
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.8); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.chat-input-area {
  padding: var(--spacing-md);
  border-top: 1px solid var(--color-border-light);
}

.input-wrapper {
  display: flex;
  gap: var(--spacing-sm);
  align-items: flex-end;
}

.input-tips {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 6px;
}

@media (max-width: 768px) {
  .chat-sidebar {
    display: none;
  }
}
</style>
