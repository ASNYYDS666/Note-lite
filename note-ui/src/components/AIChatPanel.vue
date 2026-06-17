<template>
  <div class="ai-chat-panel" :class="{ open: visible }">
    <div class="panel-header">
      <div class="header-left">
        <h3>AI 对话</h3>
      </div>
      <div class="header-right">
        <select v-model="scopeType" class="header-select">
          <option value="NOTE" v-if="noteId">当前笔记</option>
          <option value="FOLDER" v-if="folderId">当前文件夹</option>
          <option value="ALL">全部笔记</option>
        </select>
        <select v-model="style" class="header-select">
          <option value="concise">简洁</option>
          <option value="detailed">详细</option>
          <option value="code-review">代码审查</option>
        </select>
        <button class="close-btn" @click="$emit('close')" title="关闭">&#x2715;</button>
      </div>
    </div>

    <div class="messages" ref="msgContainer">
      <div v-if="messages.length === 0" class="empty-chat">
        <div class="empty-icon">
          <svg viewBox="0 0 48 48" fill="none">
            <circle cx="24" cy="24" r="20" stroke="#c8c5e8" stroke-width="1.5"/>
            <path d="M16 20h16M16 26h10" stroke="#c8c5e8" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </div>
        <p>向 AI 提问，基于你的笔记获取智能回答</p>
      </div>

      <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
        <div class="msg-content" v-html="renderMarkdown(msg.content)"></div>
        <div v-if="msg.sources && msg.sources.length" class="msg-sources">
          <span class="sources-label">来源：</span>
          <a v-for="s in msg.sources" :key="s.noteId"
             class="source-link"
             @click="$emit('navigate', s.noteId)">
            {{ s.title || '笔记#' + s.noteId }}
          </a>
        </div>
        <div v-if="msg.error" class="msg-error">{{ msg.error }}</div>
      </div>

      <div v-if="loading" class="msg assistant">
        <div class="typing-dots">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>

    <div class="input-area">
      <textarea
        v-model="input"
        class="chat-input"
        placeholder="输入问题，Enter 发送，Shift+Enter 换行"
        rows="1"
        :disabled="loading"
        @keydown.enter.exact.prevent="send"
        @input="autoResize"
        ref="inputEl"
      ></textarea>
      <button class="send-btn" :disabled="loading || !input.trim()" @click="send">
        <svg viewBox="0 0 20 20" fill="none">
          <path d="M3 10l14-7-7 14-2-5-5-2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { marked } from 'marked'
import { streamChat, parseSSEChunk } from '@/api/chat'

const props = defineProps({
  visible: { type: Boolean, default: false },
  noteId: { type: Number, default: null },
  folderId: { type: Number, default: null }
})

const emit = defineEmits(['close', 'navigate'])

const scopeType = ref('ALL')
const style = ref('detailed')
const input = ref('')
const loading = ref(false)
const messages = ref([])
const msgContainer = ref(null)
const inputEl = ref(null)

watch(() => props.visible, (val) => {
  if (val) {
    nextTick(() => inputEl.value?.focus())
    if (props.noteId) scopeType.value = 'NOTE'
    else if (props.folderId) scopeType.value = 'FOLDER'
  }
})

function renderMarkdown(text) {
  if (!text) return ''
  return marked.parse(text)
}

function autoResize() {
  const el = inputEl.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

function buildScopeIds() {
  if (scopeType.value === 'NOTE' && props.noteId) return [props.noteId]
  if (scopeType.value === 'FOLDER' && props.folderId) return [props.folderId]
  return []
}

async function send() {
  const question = input.value.trim()
  if (!question || loading.value) return

  input.value = ''
  autoResize()
  loading.value = true

  messages.value.push({ role: 'user', content: question })
  const aiMsg = { role: 'assistant', content: '', sources: null, error: null }
  messages.value.push(aiMsg)

  try {
    const { reader } = await streamChat({
      question,
      scopeType: scopeType.value,
      scopeIds: buildScopeIds(),
      style: style.value
    })

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const events = parseSSEChunk(buffer)
      buffer = ''

      for (const evt of events) {
        if (evt.error) {
          aiMsg.error = evt.error
          aiMsg.content = aiMsg.content || '(出错)'
          loading.value = false
          return
        }
        if (evt.done) {
          if (evt.sources) aiMsg.sources = evt.sources
          loading.value = false
          return
        }
        if (evt.token) {
          aiMsg.content += evt.token
        }
      }
      await nextTick()
      scrollToBottom()
    }

    loading.value = false
  } catch (e) {
    aiMsg.error = e.message || '请求失败，请稍后重试'
    loading.value = false
  }
}

function scrollToBottom() {
  const el = msgContainer.value
  if (el) el.scrollTop = el.scrollHeight
}
</script>

<style scoped>
.ai-chat-panel {
  position: fixed;
  top: 0;
  right: -420px;
  width: 400px;
  height: 100vh;
  background: #ffffff;
  border-left: 1px solid #e8e6f0;
  display: flex;
  flex-direction: column;
  z-index: 1000;
  transition: right 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.06);
}

.ai-chat-panel.open {
  right: 0;
}

/* ===== 头部 ===== */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0eef8;
  flex-shrink: 0;
  gap: 8px;
  flex-wrap: wrap;
}

.header-left h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  white-space: nowrap;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-select {
  font-size: 11px;
  padding: 4px 6px;
  border: 1px solid #e8e6f0;
  border-radius: 6px;
  background: #faf9fe;
  color: #4a4a6a;
  outline: none;
  cursor: pointer;
}

.header-select:focus {
  border-color: #6b5ce7;
}

.close-btn {
  background: none;
  border: none;
  font-size: 16px;
  color: #9896b0;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 6px;
  transition: background 0.15s;
}

.close-btn:hover {
  background: #f0eef8;
  color: #4a4a6a;
}

/* ===== 消息区 ===== */
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: 40px 20px;
}

.empty-icon svg {
  width: 56px;
  height: 56px;
  margin-bottom: 14px;
}

.empty-chat p {
  font-size: 13px;
  color: #9896b0;
  margin: 0;
  line-height: 1.6;
}

/* ===== 消息气泡 ===== */
.msg {
  max-width: 90%;
  animation: fadeIn 0.2s ease;
}

.msg.user {
  align-self: flex-end;
}

.msg.user .msg-content {
  background: linear-gradient(135deg, #5a50d8 0%, #7b6ef0 100%);
  color: #fff;
  border-radius: 14px 14px 4px 14px;
  padding: 10px 14px;
  font-size: 13px;
  line-height: 1.6;
}

.msg.assistant {
  align-self: flex-start;
}

.msg.assistant .msg-content {
  background: #f5f4fb;
  color: #2a2a3e;
  border-radius: 14px 14px 14px 4px;
  padding: 10px 14px;
  font-size: 13px;
  line-height: 1.7;
}

.msg-content :deep(p) { margin: 0 0 6px 0; }
.msg-content :deep(p:last-child) { margin-bottom: 0; }
.msg-content :deep(ul), .msg-content :deep(ol) { margin: 4px 0; padding-left: 18px; }
.msg-content :deep(li) { margin-bottom: 2px; }
.msg-content :deep(pre) {
  background: #1e1e2e;
  border-radius: 8px;
  padding: 12px;
  margin: 8px 0;
  overflow-x: auto;
}
.msg-content :deep(code) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
}
.msg-content :deep(p code) {
  background: #e8e6f0;
  padding: 1px 5px;
  border-radius: 4px;
  color: #5a50d8;
}
.msg-content :deep(pre code) {
  background: none;
  padding: 0;
  color: #cdd6f4;
}
.msg-content :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  font-size: 12px;
}
.msg-content :deep(th), .msg-content :deep(td) {
  border: 1px solid #dddaf4;
  padding: 5px 8px;
  text-align: left;
}
.msg-content :deep(th) {
  background: #f0eef8;
  font-weight: 500;
}
.msg-content :deep(blockquote) {
  border-left: 3px solid #6b5ce7;
  margin: 6px 0;
  padding: 4px 12px;
  color: #6a6a8a;
  background: #faf9fe;
  border-radius: 0 6px 6px 0;
}
.msg-content :deep(strong) { font-weight: 600; }
.msg-content :deep(h1), .msg-content :deep(h2), .msg-content :deep(h3) {
  margin: 8px 0 4px 0;
  font-size: 14px;
}
.msg-content :deep(a) { color: #6b5ce7; }

/* 来源 */
.msg-sources {
  margin-top: 8px;
  font-size: 11px;
  color: #9896b0;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.sources-label { flex-shrink: 0; }

.source-link {
  color: #6b5ce7;
  cursor: pointer;
  text-decoration: none;
  padding: 1px 6px;
  background: #f5f3ff;
  border-radius: 4px;
  transition: background 0.15s;
}

.source-link:hover {
  background: #e8e4ff;
}

/* 错误 */
.msg-error {
  margin-top: 6px;
  font-size: 12px;
  color: #d85a5a;
  background: #fff0f0;
  padding: 6px 10px;
  border-radius: 6px;
}

/* 加载动画 */
.typing-dots {
  display: flex;
  gap: 4px;
  padding: 10px 14px;
}

.typing-dots span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #c8c5e8;
  animation: dotPulse 1.2s ease-in-out infinite;
}

.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes dotPulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1); }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== 输入区 ===== */
.input-area {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid #f0eef8;
  flex-shrink: 0;
}

.chat-input {
  flex: 1;
  border: 1px solid #e8e6f0;
  border-radius: 10px;
  padding: 8px 12px;
  font-size: 13px;
  resize: none;
  outline: none;
  line-height: 1.5;
  font-family: inherit;
  transition: border-color 0.2s;
  max-height: 120px;
}

.chat-input:focus {
  border-color: #6b5ce7;
  box-shadow: 0 0 0 3px rgba(107, 92, 231, 0.08);
}

.chat-input:disabled {
  background: #f9f8fc;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #5a50d8 0%, #7b6ef0 100%);
  border: none;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: opacity 0.15s;
}

.send-btn svg {
  width: 16px;
  height: 16px;
}

.send-btn:hover:not(:disabled) { opacity: 0.9; }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* 响应式 */
@media (max-width: 640px) {
  .ai-chat-panel {
    width: 100vw;
    right: -100vw;
  }
}
</style>
