<template>
  <div v-if="workspace.agentPanelVisible" class="agent-panel" :style="{ width: workspace.agentPanelWidth + 'px' }">
    <!-- Header -->
    <div class="panel-header">
      <div class="panel-title">
        <span class="material-symbols-outlined">smart_toy</span>
        <span>AI 助手</span>
      </div>
      <button class="close-btn" @click="workspace.toggleAgentPanel()">
        <span class="material-symbols-outlined">close</span>
      </button>
    </div>

    <!-- Controls -->
    <div class="panel-controls">
      <div class="control-group">
        <label class="control-label">范围</label>
        <el-select :model-value="ai.scopeType" @update:model-value="(v) => ai.scopeType = v" size="small" class="control-select">
          <el-option label="全部笔记" value="ALL" />
          <el-option label="当前笔记" value="NOTE" />
          <el-option label="当前文件夹" value="FOLDER" />
        </el-select>
        <span v-if="ai.scopeType === 'NOTE' && !workspace.currentNoteId" class="scope-hint">请先在左侧选中一篇笔记</span>
        <span v-else-if="ai.scopeType === 'FOLDER' && !workspace.selectedFolderId" class="scope-hint">请先在左侧选中一个文件夹</span>
      </div>
      <div class="control-group">
        <label class="control-label">风格</label>
        <el-select :model-value="ai.chatStyle" @update:model-value="(v) => ai.chatStyle = v" size="small" class="control-select">
          <el-option label="简洁" value="concise" />
          <el-option label="详细" value="detailed" />
          <el-option label="代码审查" value="code-review" />
        </el-select>
      </div>
      <div class="control-group">
        <label class="control-label">API 配置</label>
        <el-select :model-value="ai.activeProfileId" @update:model-value="selectProfile" size="small" class="control-select">
          <el-option label="旧配置" :value="0" />
          <el-option v-for="p in ai.profiles" :key="p.id" :label="p.profileName" :value="p.id" />
        </el-select>
      </div>
      <div class="control-group" v-if="ai.activeProfileId">
        <template v-if="activeModels.length > 0">
          <label class="control-label">模型</label>
          <el-select :model-value="ai.activeModel" @update:model-value="(v) => ai.activeModel = v" size="small" class="control-select">
            <el-option v-for="m in activeModels" :key="m" :label="m" :value="m" />
          </el-select>
        </template>
        <span v-else class="scope-hint">请在设置中刷新模型</span>
      </div>
    </div>

    <!-- Messages -->
    <div class="message-list custom-scrollbar" ref="messageListRef">
      <div v-if="ai.messages.length === 0" class="message-empty">
        向 AI 提问，基于你的笔记获取智能回答
      </div>
      <div
        v-for="msg in ai.messages"
        :key="msg.id"
        class="message-item"
        :class="msg.role"
      >
        <!-- User message -->
        <template v-if="msg.role === 'user'">
          <div class="msg-bubble user-bubble">
            {{ msg.content }}
          </div>
          <span class="msg-time">{{ formatTime(msg.timestamp) }}</span>
        </template>

        <!-- AI message -->
        <template v-else>
          <div class="msg-avatar">
            <span class="material-symbols-outlined">smart_toy</span>
          </div>
          <div class="msg-body">
            <span class="msg-sender">Assistant</span>
            <div class="msg-bubble ai-bubble">
              <div v-if="msg.content" v-html="renderMarkdown(msg.content)"></div>
              <div v-else-if="ai.streaming" class="typing-indicator">
                <span></span><span></span><span></span>
              </div>
            </div>
            <!-- Sources -->
            <div v-if="msg.sources?.length" class="msg-sources">
              <button
                v-for="src in msg.sources"
                :key="src.noteId"
                class="source-pill"
                @click="navigateToNote(src.noteId)"
              >
                <span class="material-symbols-outlined">link</span>
                {{ src.title }}
              </button>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- Input -->
    <div class="panel-input">
      <textarea
        v-model="inputText"
        class="input-textarea"
        placeholder="输入问题..."
        rows="2"
        :disabled="ai.streaming"
        @keydown.enter.exact.prevent="handleSend"
      ></textarea>
      <button
        class="send-btn"
        :disabled="!inputText.trim() || ai.streaming"
        @click="handleSend"
      >
        <span class="material-symbols-outlined">send</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, computed, onMounted } from 'vue'

import { useWorkspaceStore } from '@/store/workspace'
import { useAIStore } from '@/store/ai'
import { marked } from 'marked'

const workspace = useWorkspaceStore()
const ai = useAIStore()
const inputText = ref('')
const messageListRef = ref(null)

function selectProfile(id) {
  // 0 是"旧配置"的哨兵值，转换为 null 使后端走 legacy 模式
  ai.activeProfileId = id || null
  ai.activeModel = null
  if (id && ai.profiles.length === 0) {
    ai.loadProfiles()
  }
}

function parseModels(json) {
  if (!json) return []
  try { return typeof json === 'string' ? JSON.parse(json) : json }
  catch { return [] }
}

const activeModels = computed(() => {
  const p = ai.profiles.find(p => p.id === ai.activeProfileId)
  if (!p) return []
  return parseModels(p.enabledModels).map(m => typeof m === 'string' ? m : m.id)
})

onMounted(() => {
  ai.loadProfiles()
})

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

watch(() => ai.messages.length, scrollToBottom)

function handleSend() {
  const text = inputText.value.trim()
  if (!text || ai.streaming) return

  const scopeIds = []
  if (ai.scopeType === 'NOTE' && workspace.currentNoteId) {
    scopeIds.push(workspace.currentNoteId)
  } else if (ai.scopeType === 'FOLDER' && workspace.selectedFolderId) {
    scopeIds.push(workspace.selectedFolderId)
  }

  ai.sendMessage(text, scopeIds)
  inputText.value = ''
}

function navigateToNote(noteId) {
  workspace.openNote(noteId)
}

function renderMarkdown(text) {
  if (!text) return ''
  try {
    return marked.parse(text, { breaks: true })
  } catch {
    return text
  }
}

function formatTime(ts) {
  const d = new Date(ts)
  return d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.agent-panel {
  position: fixed;
  top: var(--top-bar-height);
  right: 0;
  bottom: var(--status-bar-height);
  background: var(--surface-container-lowest);
  border-left: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  z-index: 80;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-ui);
  font-size: var(--text-ui-base);
  font-weight: 600;
  color: var(--accent-sage);
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: var(--radius-full);
  background: none;
  color: var(--on-surface-variant);
  cursor: pointer;
}

.close-btn:hover {
  background: var(--surface-container);
}

.panel-controls {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.control-label {
  font-family: var(--font-ui);
  font-size: 10px;
  font-weight: 600;
  color: var(--on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.control-select {
  font-size: var(--text-ui-sm);
}

.scope-hint {
  font-size: 10px;
  color: var(--error-red);
  margin-top: 2px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-empty {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
  text-align: center;
  padding: 32px 16px;
}

.message-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-item.user {
  align-items: flex-end;
}

.message-item.assistant {
  align-items: flex-start;
}

.msg-bubble {
  max-width: 90%;
  padding: 10px 14px;
  border-radius: var(--radius-lg);
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  line-height: 1.5;
}

.user-bubble {
  background: var(--secondary-container);
  color: var(--on-secondary-container);
  border-bottom-right-radius: 0;
}

.ai-bubble {
  background: var(--surface-container-high);
  color: var(--on-surface);
  border-top-left-radius: 0;
}

.ai-bubble :deep(p) { margin: 0 0 8px; }
.ai-bubble :deep(p:last-child) { margin-bottom: 0; }
.ai-bubble :deep(ul), .ai-bubble :deep(ol) { padding-left: 16px; margin: 4px 0; }
.ai-bubble :deep(li) { margin-bottom: 2px; }

.msg-time {
  font-family: var(--font-ui);
  font-size: 10px;
  color: var(--on-surface-variant);
  padding: 0 4px;
}

.msg-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--accent-sage);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  margin-bottom: 4px;
}

.msg-sender {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  font-weight: 600;
  color: var(--on-surface);
  margin-bottom: 4px;
  display: block;
}

.msg-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.msg-sources {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.source-pill {
  display: flex;
  align-items: center;
  gap: 4px;
  background: var(--on-tertiary-fixed-variant);
  color: white;
  border: none;
  border-radius: var(--radius-default);
  padding: 2px 8px;
  font-family: var(--font-ui);
  font-size: 10px;
  cursor: pointer;
  transition: opacity 0.15s;
}

.source-pill:hover {
  opacity: 0.8;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-sage);
  animation: typing 1.4s ease-in-out infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; transform: translateY(0); }
  30% { opacity: 1; transform: translateY(-4px); }
}

.panel-input {
  padding: 12px 16px;
  border-top: 1px solid var(--border-subtle);
  position: relative;
}

.input-textarea {
  width: 100%;
  background: var(--surface-container-low);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 8px 36px 8px 12px;
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface);
  resize: none;
  outline: none;
}

.input-textarea:focus {
  border-color: var(--accent-sage);
}

.input-textarea::placeholder {
  color: var(--outline-variant);
}

.input-textarea:disabled {
  opacity: 0.5;
}

.send-btn {
  position: absolute;
  right: 24px;
  bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: var(--radius-default);
  background: none;
  color: var(--accent-sage);
  cursor: pointer;
  transition: background 0.15s;
}

.send-btn:hover:not(:disabled) {
  background: var(--surface-container);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: var(--secondary-fixed);
  border-radius: 10px;
}
</style>
