import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { streamChat, createSSEParser, fetchConversations, fetchMessages, deleteConversation } from '@/api/chat'

export const useAIStore = defineStore('ai', () => {
  // ==================== 视图 ====================
  const currentView = ref('chat') // 'chat' | 'history' | 'settings'

  // ==================== 对话管理 ====================
  const conversations = ref([])
  const messages = ref([])
  const conversationId = ref(null)
  const streaming = ref(false)

  // ==================== 范围 ====================
  const scopeType = ref('ALL')   // 'ALL' | 'NOTE' | 'FOLDER'
  const scopeLabel = ref('')     // 上下文标签显示文本

  // ==================== 模型 ====================
  const activeProfileId = ref(null)
  const activeModel = ref(null)
  const profiles = ref([])
  // 已启用的 profile id 集合（持久化到 localStorage）
  const enabledProfileIds = ref(loadEnabledProfileIds())

  // ==================== Prompt 模块 ====================
  const chatStyle = ref('concise') // 始终默认简洁
  // Prompt 模块中额外启用的风格（互斥，只能选一个或都不选）
  const extraPromptKey = ref(null) // null | 'detailed' | 'code-review'

  let abortController = null

  // ==================== 计算属性 ====================

  /** 启用的 profiles */
  const enabledProfiles = computed(() =>
    profiles.value.filter(p => enabledProfileIds.value.includes(p.id))
  )

  /** 当前选中 profile */
  const currentProfile = computed(() =>
    profiles.value.find(p => p.id === activeProfileId.value) || null
  )

  /** 当前 profile 可用的模型列表 */
  const activeModels = computed(() => {
    const p = currentProfile.value
    if (!p) return []
    return parseModels(p.enabledModels)
  })

  // ==================== 视图切换 ====================

  function setView(view) {
    currentView.value = view
  }

  // ==================== 对话 CRUD ====================

  async function loadConversations() {
    try {
      conversations.value = await fetchConversations()
    } catch { /* ignore */ }
  }

  async function switchConversation(id) {
    conversationId.value = id
    messages.value = []
    try {
      const msgs = await fetchMessages(id)
      messages.value = msgs.map(m => ({
        id: m.id.toString(),
        role: m.role,
        content: m.content || '',
        sources: m.sources || [],
        timestamp: m.createdAt,
        thinkContent: '',
        thinkCollapsed: true
      }))
    } catch { /* ignore */ }
    setView('chat')
  }

  async function removeConversation(id) {
    try {
      await deleteConversation(id)
      conversations.value = conversations.value.filter(c => c.id !== id)
      if (conversationId.value === id) {
        newConversation()
      }
    } catch { /* ignore */ }
  }

  function newConversation() {
    messages.value = []
    conversationId.value = null
    setView('chat')
  }

  // ==================== 消息 ====================

  function addMessage(role, content, sources = []) {
    const msg = {
      id: Date.now().toString(),
      role,
      content: role === 'assistant' ? '' : content,
      sources,
      timestamp: new Date().toISOString()
    }
    if (role === 'assistant') {
      msg.thinkContent = ''
    }
    messages.value.push(msg)
  }

  function updateLastAssistant(token, thinking) {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant') {
      if (thinking) {
        last.thinkContent = (last.thinkContent || '') + token
      } else {
        last.content += token
      }
    }
  }

  function setLastSources(sources) {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant') {
      last.sources = sources
    }
  }

  async function sendMessage(question, scopeIds = []) {
    addMessage('user', question)
    addMessage('assistant', '')

    streaming.value = true
    try {
      // 确定实际使用的 style：extraPromptKey 不为空则用它，否则用默认 concise
      const effectiveStyle = extraPromptKey.value || 'concise'

      const { reader, abort } = await streamChat({
        question,
        scopeType: scopeType.value,
        scopeIds,
        style: effectiveStyle,
        conversationId: conversationId.value,
        profileId: activeProfileId.value,
        modelName: activeModel.value
      })
      abortController = abort

      const decoder = new TextDecoder()
      const parseSSE = createSSEParser()
      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        const chunk = decoder.decode(value, { stream: true })
        const events = parseSSE(chunk)

        for (const event of events) {
          if (event.token) updateLastAssistant(event.token, event.thinking)
          if (event.sources) setLastSources(event.sources)
          if (event.done) {
            if (event.conversationId) {
              conversationId.value = event.conversationId
              // 新对话创建后刷新列表
              loadConversations()
            }
          }
          if (event.error) {
            updateLastAssistant('\n\n[Error: ' + event.error + ']')
          }
        }
      }
    } catch (e) {
      if (e.name !== 'AbortError') {
        updateLastAssistant('\n\n[请求失败: ' + e.message + ']')
      }
    } finally {
      streaming.value = false
      abortController = null
    }
  }

  function stopStreaming() {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
  }

  // ==================== 模型管理 ====================

  async function loadProfiles() {
    try {
      const { getProfiles } = await import('@/api/aiConfig')
      const data = await getProfiles()
      profiles.value = data || []

      if (profiles.value.length === 0) {
        const { getAIConfig, saveProfile } = await import('@/api/aiConfig')
        const legacy = await getAIConfig()
        if (legacy && legacy.chatProvider) {
          const migrated = await saveProfile({
            profileName: legacy.chatProvider,
            providerKey: legacy.chatProvider,
            apiKey: '',
            baseUrl: legacy.chatUrl || '',
            enabledModels: legacy.chatModel ? [legacy.chatModel] : []
          })
          if (migrated) profiles.value = [migrated]
        }
      }

      // 如果没有任何启用的 profile，自动启用第一个
      if (enabledProfileIds.value.length === 0 && profiles.value.length > 0) {
        enabledProfileIds.value = [profiles.value[0].id]
        saveEnabledProfileIds()
      }
    } catch { /* ignore */ }
  }

  function toggleProfileEnabled(profileId) {
    const idx = enabledProfileIds.value.indexOf(profileId)
    if (idx >= 0) {
      enabledProfileIds.value.splice(idx, 1)
    } else {
      enabledProfileIds.value.push(profileId)
    }
    saveEnabledProfileIds()
    // 如果当前选中的 profile 被禁用，清除选择
    if (activeProfileId.value === profileId && idx < 0) {
      // 它被移除了，但我们刚才是 push，所以 idx < 0 意味着之前没启用
    }
    if (activeProfileId.value && !enabledProfileIds.value.includes(activeProfileId.value)) {
      activeProfileId.value = null
      activeModel.value = null
    }
  }

  function selectActiveProfile(profileId) {
    activeProfileId.value = profileId
    activeModel.value = null
  }

  function selectActiveModel(model) {
    activeModel.value = model
  }

  // ==================== Prompt 模块 ====================

  function setExtraPrompt(key) {
    // 互斥：再次点击同一个则取消
    extraPromptKey.value = extraPromptKey.value === key ? null : key
  }

  // ==================== Scope ====================

  function updateScope(type, label) {
    scopeType.value = type
    scopeLabel.value = label
  }

  // ==================== 持久化 ====================

  function loadEnabledProfileIds() {
    try {
      const raw = localStorage.getItem('ai_enabled_profiles')
      return raw ? JSON.parse(raw) : []
    } catch { return [] }
  }

  function saveEnabledProfileIds() {
    localStorage.setItem('ai_enabled_profiles', JSON.stringify(enabledProfileIds.value))
  }

  // ==================== 工具 ====================

  function parseModels(json) {
    if (!json) return []
    try { return typeof json === 'string' ? JSON.parse(json) : json }
    catch { return [] }
  }

  return {
    // 视图
    currentView,
    // 对话
    conversations, messages, conversationId, streaming,
    // 范围
    scopeType, scopeLabel,
    // 模型
    activeProfileId, activeModel, profiles, enabledProfileIds, enabledProfiles, currentProfile, activeModels,
    // Prompt
    chatStyle, extraPromptKey,
    // 方法
    setView,
    loadConversations, switchConversation, removeConversation, newConversation,
    sendMessage, stopStreaming,
    loadProfiles, toggleProfileEnabled, selectActiveProfile, selectActiveModel,
    setExtraPrompt, updateScope
  }
})
