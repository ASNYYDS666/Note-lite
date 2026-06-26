import { defineStore } from 'pinia'
import { ref } from 'vue'
import { streamChat, parseSSEChunk } from '@/api/chat'

export const useAIStore = defineStore('ai', () => {
  const messages = ref([])
  const streaming = ref(false)
  const scopeType = ref('ALL')
  const chatStyle = ref('detailed')
  const conversationId = ref(null)
  const activeProfileId = ref(null)
  const activeModel = ref(null)
  const profiles = ref([])

  let abortController = null

  function addMessage(role, content, sources = []) {
    messages.value.push({
      id: Date.now().toString(),
      role,
      content,
      sources,
      timestamp: new Date().toISOString()
    })
  }

  function updateLastAssistant(token) {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant') {
      last.content += token
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
      const { reader, abort } = await streamChat({
        question,
        scopeType: scopeType.value,
        scopeIds,
        style: chatStyle.value,
        conversationId: conversationId.value,
        profileId: activeProfileId.value,
        modelName: activeModel.value
      })
      abortController = abort

      const decoder = new TextDecoder()
      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        const chunk = decoder.decode(value, { stream: true })
        const events = parseSSEChunk(chunk)

        for (const event of events) {
          if (event.token) updateLastAssistant(event.token)
          if (event.sources) setLastSources(event.sources)
          if (event.done) {
            if (event.conversationId) conversationId.value = event.conversationId
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

  function clearMessages() {
    messages.value = []
    conversationId.value = null
  }

  async function loadProfiles() {
    try {
      const { getProfiles } = await import('@/api/aiConfig')
      const data = await getProfiles()
      profiles.value = data || []

      // Auto-migrate old config to Profile if no profiles exist
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
    } catch { /* ignore */ }
  }

  function setScopeType(type) {
    scopeType.value = type
  }

  function setChatStyle(style) {
    chatStyle.value = style
  }

  function setActiveProfile(id) {
    activeProfileId.value = id
  }

  function setActiveModel(model) {
    activeModel.value = model
  }

  return {
    messages, streaming, scopeType, chatStyle, conversationId,
    activeProfileId, activeModel, profiles,
    sendMessage, stopStreaming, clearMessages,
    setScopeType, setChatStyle, setActiveProfile, setActiveModel,
    loadProfiles
  }
})
