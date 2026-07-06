<template>
  <div v-if="workspace.agentPanelVisible" class="agent-panel" :style="{ width: workspace.agentPanelWidth + 'px' }">
    <!-- Header -->
    <div class="panel-header">
      <div class="panel-title" @click="ai.setView('chat')">
        <span class="material-symbols-outlined">smart_toy</span>
        <span>AI 助手</span>
      </div>
      <div class="header-actions">
        <button class="header-btn" :class="{ active: ai.currentView === 'settings' }" @click="toggleView('settings')" title="设置">
          <span class="material-symbols-outlined">settings</span>
        </button>
        <button class="header-btn" :class="{ active: ai.currentView === 'history' }" @click="toggleView('history')" title="历史">
          <span class="material-symbols-outlined">history</span>
        </button>
        <button class="header-btn" @click="handleNewConversation" title="新建对话">
          <span class="material-symbols-outlined">add</span>
        </button>
        <button class="close-btn" @click="workspace.toggleAgentPanel()">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
    </div>

    <!-- ==================== 聊天视图 ==================== -->
    <template v-if="ai.currentView === 'chat'">
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
              <!-- Thinking box -->
              <div v-if="msg.thinkContent" class="think-box">
                <div class="think-header" @click="msg.thinkCollapsed = !msg.thinkCollapsed">
                  <span class="material-symbols-outlined">psychology</span>
                  <span>思考过程</span>
                  <span class="material-symbols-outlined think-arrow" :class="{ 'think-arrow--collapsed': msg.thinkCollapsed }">expand_less</span>
                </div>
                <div v-if="!msg.thinkCollapsed" class="think-body" v-html="renderMarkdown(msg.thinkContent)"></div>
              </div>
              <!-- Answer bubble -->
              <div class="msg-bubble ai-bubble">
                <div v-if="msg.content" v-html="renderMarkdown(msg.content)"></div>
                <div v-else-if="ai.streaming && !msg.thinkContent" class="typing-indicator">
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

      <!-- Context bar above input -->
      <div class="context-bar">
        <span class="scope-tag" :title="ai.scopeLabel">
          <span class="material-symbols-outlined scope-icon">{{ scopeIcon }}</span>
          <span class="scope-text">{{ ai.scopeLabel || '全部笔记' }}</span>
        </span>
        <div class="model-selector" v-if="ai.enabledProfiles.length > 0">
          <select :value="modelKey" @change="onModelChange" class="model-select">
            <optgroup v-for="p in ai.enabledProfiles" :key="p.id" :label="p.profileName">
              <option v-for="m in getModels(p)" :key="m" :value="p.id + '::' + m">{{ m }}</option>
            </optgroup>
          </select>
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
    </template>

    <!-- ==================== 历史视图 ==================== -->
    <template v-if="ai.currentView === 'history'">
      <div class="history-list custom-scrollbar">
        <div v-if="ai.conversations.length === 0" class="message-empty">
          暂无历史对话
        </div>
        <div
          v-for="conv in ai.conversations"
          :key="conv.id"
          class="history-item"
          :class="{ active: conv.id === ai.conversationId }"
          @click="ai.switchConversation(conv.id)"
        >
          <div class="history-info">
            <span class="history-title">{{ conv.title }}</span>
            <span class="history-meta">{{ conv.messageCount }} 条消息 · {{ formatDate(conv.updatedAt) }}</span>
          </div>
          <button class="history-delete" @click.stop="confirmDelete(conv)" title="删除">
            <span class="material-symbols-outlined">delete</span>
          </button>
        </div>
      </div>
    </template>

    <!-- ==================== 设置视图 ==================== -->
    <template v-if="ai.currentView === 'settings'">
      <div class="settings-view custom-scrollbar">
        <!-- 模型启用 -->
        <div class="settings-section">
          <div class="settings-section-title">模型管理</div>
          <div v-if="ai.profiles.length === 0" class="settings-empty">暂无 API 配置，请先在全局设置中添加</div>
          <div v-for="p in ai.profiles" :key="p.id" class="settings-item">
            <div class="settings-item-info">
              <span class="settings-item-name">{{ p.profileName }}</span>
              <span class="settings-item-detail">{{ p.providerKey }}</span>
            </div>
            <label class="toggle-switch">
              <input type="checkbox" :checked="ai.enabledProfileIds.includes(p.id)" @change="ai.toggleProfileEnabled(p.id)" />
              <span class="toggle-slider"></span>
            </label>
          </div>
        </div>

        <!-- Prompt 模块 -->
        <div class="settings-section">
          <div class="settings-section-title">Prompt 模块</div>
          <div class="settings-hint">默认使用简洁风格。可额外启用一种特殊风格（互斥）。</div>
          <div class="settings-item" @click="ai.setExtraPrompt('detailed')">
            <div class="settings-item-info">
              <span class="settings-item-name">详细风格</span>
              <span class="settings-item-detail">详细完整的回答</span>
            </div>
            <label class="toggle-switch">
              <input type="checkbox" :checked="ai.extraPromptKey === 'detailed'" @click.stop @change="ai.setExtraPrompt('detailed')" />
              <span class="toggle-slider"></span>
            </label>
          </div>
          <div class="settings-item" @click="ai.setExtraPrompt('code-review')">
            <div class="settings-item-info">
              <span class="settings-item-name">代码审查</span>
              <span class="settings-item-detail">代码审查视角</span>
            </div>
            <label class="toggle-switch">
              <input type="checkbox" :checked="ai.extraPromptKey === 'code-review'" @click.stop @change="ai.setExtraPrompt('code-review')" />
              <span class="toggle-slider"></span>
            </label>
          </div>
        </div>
      </div>
    </template>

    <!-- 删除确认弹窗 -->
    <div v-if="deleteTarget" class="confirm-overlay" @click="deleteTarget = null">
      <div class="confirm-dialog" @click.stop>
        <p>确认删除对话「{{ deleteTarget.title }}」？</p>
        <div class="confirm-actions">
          <button class="confirm-btn cancel" @click="deleteTarget = null">取消</button>
          <button class="confirm-btn danger" @click="doDelete">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, computed, onMounted, watchEffect } from 'vue'

import { useWorkspaceStore } from '@/store/workspace'
import { useAIStore } from '@/store/ai'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const workspace = useWorkspaceStore()
const ai = useAIStore()
const inputText = ref('')
const messageListRef = ref(null)
const deleteTarget = ref(null)

const scopeIcon = computed(() => {
  if (ai.scopeType === 'NOTE') return 'description'
  if (ai.scopeType === 'FOLDER') return 'folder'
  return 'public'
})

const modelKey = computed(() => {
  if (!ai.activeProfileId || !ai.activeModel) return ''
  return ai.activeProfileId + '::' + ai.activeModel
})

watch(
  [() => ai.enabledProfiles, () => ai.activeProfileId, () => ai.activeModel],
  ([profiles, profileId, model]) => {
    if (profileId && model) return
    if (!profiles || profiles.length === 0) return
    const p = profiles[0]
    const models = getModels(p)
    if (models.length === 0) return
    if (!profileId) ai.selectActiveProfile(p.id)
    if (!model) ai.selectActiveModel(models[0])
  },
  { immediate: true }
)

function getModels(profile) {
  if (!profile || !profile.enabledModels) return []
  let arr = profile.enabledModels
  if (typeof arr === 'string') {
    try { arr = JSON.parse(arr) } catch { return [] }
  }
  return arr.map(m => typeof m === 'string' ? m : m.id)
}

function onModelChange(e) {
  const [profileId, model] = e.target.value.split('::')
  ai.selectActiveProfile(Number(profileId))
  ai.selectActiveModel(model)
}

function buildScopeLabel(type) {
  if (type === 'NOTE') return '📄 当前笔记'
  if (type === 'FOLDER') return '📁 当前文件夹'
  return '🌐 全部笔记'
}

// 统一监听：noteId 优先级 > folderId > 全部
function syncScope() {
  if (workspace.currentNoteId) {
    ai.updateScope('NOTE', buildScopeLabel('NOTE'))
  } else if (workspace.selectedFolderId) {
    ai.updateScope('FOLDER', buildScopeLabel('FOLDER'))
  } else {
    ai.updateScope('ALL', buildScopeLabel('ALL'))
  }
}

watchEffect(syncScope)

onMounted(() => {
  ai.loadProfiles()
  ai.loadConversations()
  syncScope()
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

  // 确保模型已选
  if (!ai.activeProfileId && ai.enabledProfiles.length > 0) {
    const p = ai.enabledProfiles[0]
    ai.selectActiveProfile(p.id)
    const models = getModels(p)
    if (models.length > 0) ai.selectActiveModel(models[0])
  }

  ai.sendMessage(text, scopeIds)
  inputText.value = ''
}

function handleNewConversation() {
  ai.newConversation()
}

function toggleView(view) {
  if (ai.currentView === view) {
    ai.setView('chat')
  } else {
    if (view === 'history') ai.loadConversations()
    if (view === 'settings') ai.loadProfiles()
    ai.setView(view)
  }
}

function confirmDelete(conv) {
  deleteTarget.value = conv
}

function doDelete() {
  if (deleteTarget.value) {
    ai.removeConversation(deleteTarget.value.id)
    deleteTarget.value = null
  }
}

function navigateToNote(noteId) {
  workspace.openNote(noteId)
}

function renderMarkdown(text) {
  if (!text) return ''
  try {
    return DOMPurify.sanitize(marked.parse(text, { breaks: true }))
  } catch {
    return text
  }
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}

function formatDate(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const now = new Date()
  const diff = now - d
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  return d.toLocaleDateString('zh-CN')
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

/* ===== Header ===== */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
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
  cursor: pointer;
  user-select: none;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.header-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: var(--radius-full);
  background: none;
  color: var(--on-surface-variant);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.header-btn:hover { background: var(--surface-container); }
.header-btn.active {
  background: var(--secondary-container);
  color: var(--on-secondary-container);
}

.header-btn .material-symbols-outlined { font-size: 18px; }

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

.close-btn:hover { background: var(--surface-container); }

/* ===== Context bar ===== */
.context-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px 4px;
  gap: 8px;
}

.scope-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  background: var(--surface-container);
  border-radius: var(--radius-default);
  padding: 2px 8px;
  font-size: 11px;
  color: var(--on-surface-variant);
  max-width: 55%;
  overflow: hidden;
}

.scope-icon { font-size: 14px; flex-shrink: 0; }
.scope-text { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.model-selector { flex-shrink: 0; }

.model-select {
  font-size: 11px;
  padding: 3px 6px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  background: var(--surface-container-low);
  color: var(--on-surface);
  outline: none;
  cursor: pointer;
  max-width: 140px;
}

.model-select:focus { border-color: var(--accent-sage); }

/* ===== Message list ===== */
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

.message-item { display: flex; flex-direction: column; gap: 4px; }
.message-item.user { align-items: flex-end; }
.message-item.assistant { align-items: flex-start; }

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

/* Thinking box */
.think-box {
  background: var(--accent-soft);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  margin-bottom: 8px;
  overflow: hidden;
}

.think-header {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 10px; cursor: pointer;
  font-size: 11px; font-weight: 600;
  color: var(--on-surface-variant); user-select: none;
}
.think-header:hover { background: var(--surface-container); }
.think-header .material-symbols-outlined { font-size: 15px; }
.think-arrow { margin-left: auto; transition: transform 0.2s; font-size: 16px; }
.think-arrow--collapsed { transform: rotate(180deg); }
.think-body {
  padding: 6px 10px 10px; font-size: 0.82em;
  color: var(--on-surface-variant);
  border-top: 1px solid var(--border-subtle);
  line-height: 1.55; max-height: 300px; overflow-y: auto;
}

.msg-time {
  font-family: var(--font-ui); font-size: 10px;
  color: var(--on-surface-variant); padding: 0 4px;
}

.msg-avatar {
  width: 24px; height: 24px; border-radius: 50%;
  background: var(--accent-sage); color: white;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; margin-bottom: 4px;
}

.msg-sender {
  font-family: var(--font-ui); font-size: var(--text-ui-sm);
  font-weight: 600; color: var(--on-surface);
  margin-bottom: 4px; display: block;
}

.msg-body { display: flex; flex-direction: column; gap: 4px; }

.msg-sources { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 4px; }

.source-pill {
  display: flex; align-items: center; gap: 4px;
  background: var(--on-tertiary-fixed-variant); color: white;
  border: none; border-radius: var(--radius-default);
  padding: 2px 8px; font-family: var(--font-ui);
  font-size: 10px; cursor: pointer;
}

.source-pill:hover { opacity: 0.8; }

.typing-indicator { display: flex; gap: 4px; padding: 4px 0; }
.typing-indicator span {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--accent-sage);
  animation: typing 1.4s ease-in-out infinite;
}
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; transform: translateY(0); }
  30% { opacity: 1; transform: translateY(-4px); }
}

/* ===== Input ===== */
.panel-input {
  padding: 12px 16px; border-top: 1px solid var(--border-subtle); position: relative;
}

.input-textarea {
  width: 100%; background: var(--surface-container-low);
  border: 1px solid var(--border-subtle); border-radius: var(--radius-lg);
  padding: 8px 36px 8px 12px; font-family: var(--font-ui);
  font-size: var(--text-ui-sm); color: var(--on-surface);
  resize: none; outline: none;
}
.input-textarea:focus { border-color: var(--accent-sage); }
.input-textarea::placeholder { color: var(--outline-variant); }
.input-textarea:disabled { opacity: 0.5; }

.send-btn {
  position: absolute; right: 24px; bottom: 20px;
  display: flex; align-items: center; justify-content: center;
  width: 28px; height: 28px; border: none;
  border-radius: var(--radius-default); background: none;
  color: var(--accent-sage); cursor: pointer;
}
.send-btn:hover:not(:disabled) { background: var(--surface-container); }
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* ===== History view ===== */
.history-list {
  flex: 1; overflow-y: auto; padding: 8px;
}

.history-item {
  display: flex; align-items: center;
  padding: 10px 12px; border-radius: var(--radius-md);
  cursor: pointer; transition: background 0.15s;
}

.history-item:hover { background: var(--surface-container); }
.history-item.active { background: var(--secondary-container); }

.history-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }

.history-title {
  font-size: var(--text-ui-sm); font-weight: 500;
  color: var(--on-surface); white-space: nowrap;
  overflow: hidden; text-overflow: ellipsis;
}

.history-meta { font-size: 10px; color: var(--on-surface-variant); }

.history-delete {
  display: flex; align-items: center; justify-content: center;
  width: 28px; height: 28px; border: none; border-radius: var(--radius-full);
  background: none; color: var(--on-surface-variant);
  cursor: pointer; opacity: 0; transition: opacity 0.15s, color 0.15s;
}

.history-item:hover .history-delete { opacity: 1; }
.history-delete:hover { color: var(--error); background: var(--error-container); }

.history-delete .material-symbols-outlined { font-size: 16px; }

/* ===== Settings view ===== */
.settings-view {
  flex: 1; overflow-y: auto; padding: 12px;
}

.settings-section {
  margin-bottom: 20px;
}

.settings-section-title {
  font-size: var(--text-ui-sm); font-weight: 600;
  color: var(--on-surface); margin-bottom: 6px;
  padding-bottom: 4px; border-bottom: 1px solid var(--border-subtle);
}

.settings-hint {
  font-size: 11px; color: var(--on-surface-variant); margin-bottom: 8px;
}

.settings-empty {
  font-size: 12px; color: var(--on-surface-variant); padding: 12px 0; text-align: center;
}

.settings-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 10px; border-radius: var(--radius-md);
  cursor: pointer; transition: background 0.15s;
}

.settings-item:hover { background: var(--surface-container); }

.settings-item-info { display: flex; flex-direction: column; gap: 1px; }

.settings-item-name { font-size: var(--text-ui-sm); color: var(--on-surface); }
.settings-item-detail { font-size: 10px; color: var(--on-surface-variant); }

/* Toggle switch */
.toggle-switch {
  position: relative; display: inline-block;
  width: 36px; height: 20px; flex-shrink: 0;
}

.toggle-switch input { opacity: 0; width: 0; height: 0; }

.toggle-slider {
  position: absolute; cursor: pointer;
  top: 0; left: 0; right: 0; bottom: 0;
  background: var(--surface-container-high);
  border-radius: 20px; transition: background 0.2s;
  border: 1px solid var(--outline-variant);
}

.toggle-slider::before {
  content: ''; position: absolute;
  height: 14px; width: 14px; left: 2px; bottom: 2px;
  background: var(--on-surface-variant);
  border-radius: 50%; transition: transform 0.2s, background 0.2s;
}

.toggle-switch input:checked + .toggle-slider {
  background: var(--secondary-container);
  border-color: var(--secondary);
}

.toggle-switch input:checked + .toggle-slider::before {
  transform: translateX(16px);
  background: var(--secondary);
}

/* ===== Confirm dialog ===== */
.confirm-overlay {
  position: absolute; inset: 0;
  background: rgba(0,0,0,0.3);
  display: flex; align-items: center; justify-content: center;
  z-index: 100;
}

.confirm-dialog {
  background: var(--surface-container-lowest);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  max-width: 260px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
}

.confirm-dialog p {
  margin: 0 0 16px; font-size: var(--text-ui-sm); color: var(--on-surface);
}

.confirm-actions { display: flex; gap: 8px; justify-content: flex-end; }

.confirm-btn {
  padding: 6px 16px; border: none; border-radius: var(--radius-default);
  font-size: var(--text-ui-sm); cursor: pointer;
}

.confirm-btn.cancel { background: var(--surface-container); color: var(--on-surface); }
.confirm-btn.danger { background: var(--error); color: var(--on-error); }

/* Markdown styles */
.think-body :deep(p) { margin: 0 0 4px; }
.think-body :deep(p:last-child) { margin-bottom: 0; }
.think-body :deep(code) {
  background: var(--surface-container);
  font-family: var(--font-code); font-size: 0.9em;
  padding: 1px 4px; border-radius: var(--radius-sm);
}

.ai-bubble :deep(p) { margin: 0 0 8px; }
.ai-bubble :deep(p:last-child) { margin-bottom: 0; }
.ai-bubble :deep(ul), .ai-bubble :deep(ol) { padding-left: 16px; margin: 4px 0; }
.ai-bubble :deep(li) { margin-bottom: 2px; }
.ai-bubble :deep(h1) { font-size: 1.25rem; font-weight: 700; margin: 16px 0 8px; color: var(--on-surface); }
.ai-bubble :deep(h2) { font-size: 1.1rem; font-weight: 700; margin: 14px 0 6px; color: var(--on-surface); }
.ai-bubble :deep(h3) { font-size: 1rem; font-weight: 600; margin: 12px 0 4px; color: var(--on-surface); }
.ai-bubble :deep(h4), .ai-bubble :deep(h5), .ai-bubble :deep(h6) { font-size: 0.9rem; font-weight: 600; margin: 10px 0 4px; color: var(--on-surface-variant); }
.ai-bubble :deep(code) {
  background: var(--surface-container); color: var(--on-secondary-container);
  font-family: var(--font-code); font-size: 0.85em;
  padding: 1px 5px; border-radius: var(--radius-sm);
}
.ai-bubble :deep(pre) {
  background: var(--surface-dark); color: var(--inverse-on-surface);
  font-family: var(--font-code); font-size: 0.82em;
  padding: 12px 14px; border-radius: var(--radius-md);
  overflow-x: auto; margin: 8px 0; line-height: 1.45;
}
.ai-bubble :deep(pre code) { background: none; color: inherit; padding: 0; border-radius: 0; font-size: inherit; }
.ai-bubble :deep(blockquote) {
  border-left: 3px solid var(--accent-sage); padding: 4px 12px;
  margin: 8px 0; color: var(--on-surface-variant);
  background: var(--accent-soft); border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}
.ai-bubble :deep(table) { border-collapse: collapse; width: 100%; margin: 8px 0; font-size: 0.85em; }
.ai-bubble :deep(th) { background: var(--surface-container); font-weight: 600; padding: 6px 10px; border: 1px solid var(--border-subtle); text-align: left; }
.ai-bubble :deep(td) { padding: 5px 10px; border: 1px solid var(--border-subtle); }
.ai-bubble :deep(tr:nth-child(even)) { background: var(--surface-container-low); }
.ai-bubble :deep(a) { color: var(--accent-sage); text-decoration: underline; }
.ai-bubble :deep(hr) { border: none; border-top: 1px solid var(--border-subtle); margin: 12px 0; }
.ai-bubble :deep(img) { max-width: 100%; border-radius: var(--radius-sm); }
.ai-bubble :deep(strong) { font-weight: 700; color: var(--on-surface); }
</style>
