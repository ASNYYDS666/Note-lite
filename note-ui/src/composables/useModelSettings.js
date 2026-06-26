import { ref, computed } from 'vue'
import { getProfiles, saveProfile, deleteProfile, refreshModels, testProfileEmbed } from '@/api/aiConfig'
import request from '@/utils/request'

export function useModelSettings() {
  // ---- state ----
  const providers = ref([])
  const profiles = ref([])
  const draft = ref(emptyDraft())
  const remoteModels = ref([])
  const refreshing = ref(false)
  const refreshError = ref(null)
  const notice = ref(null)
  const pendingDeleteId = ref(null)
  const editorMode = ref('idle') // 'idle' | 'creating' | 'editing'

  // Embedding test state
  const testingEmbed = ref(false)
  const embedTestResult = ref(null) // { success, embedModel, dimension, latencyMs, error }

  // ---- derived ----
  const selectedProfile = computed(() =>
    draft.value.id ? profiles.value.find(p => p.id === draft.value.id) : null
  )

  const isDirty = computed(() => {
    const existing = selectedProfile.value
    if (!existing) {
      return !!(draft.value.profileName || draft.value.apiKey || draft.value.baseUrl || draft.value.models.length)
    }
    return (
      draft.value.profileName !== existing.profileName ||
      draft.value.apiKey !== '' ||
      draft.value.baseUrl !== (existing.baseUrl || '') ||
      JSON.stringify(draft.value.models) !== JSON.stringify(parseModelList(existing.enabledModels))
    )
  })

  const canApply = computed(() =>
    isDirty.value &&
    draft.value.profileName.trim() &&
    draft.value.providerKey &&
    (draft.value.apiKey || selectedProfile.value) &&
    draft.value.models.length > 0 &&
    draft.value.models.some(m => m.enabled)
  )

  const pendingDeleteProfile = computed(() =>
    pendingDeleteId.value ? profiles.value.find(p => p.id === pendingDeleteId.value) : null
  )

  // ---- helpers ----
  function emptyDraft() {
    return {
      id: null,
      profileName: '',
      providerKey: '',
      apiKey: '',
      baseUrl: '',
      models: []
    }
  }

  function parseModelList(json) {
    if (!json) return []
    try {
      const arr = typeof json === 'string' ? JSON.parse(json) : json
      return arr.map(id => typeof id === 'string' ? { id, enabled: true } : id)
    } catch { return [] }
  }

  function profileToDraft(p) {
    return {
      id: p.id,
      profileName: p.profileName || '',
      providerKey: p.providerKey || '',
      apiKey: '',
      baseUrl: p.baseUrl || '',
      models: parseModelList(p.enabledModels)
    }
  }

  function draftToPayload() {
    const enabledIds = draft.value.models.filter(m => m.enabled).map(m => m.id)
    return {
      id: draft.value.id,
      profileName: draft.value.profileName,
      providerKey: draft.value.providerKey,
      apiKey: draft.value.apiKey || undefined,
      baseUrl: draft.value.baseUrl || undefined,
      enabledModels: enabledIds.length > 0 ? enabledIds : undefined
    }
  }

  // ---- actions ----
  async function loadProviders() {
    try {
      const data = await request.get('/ai/providers', { silent: true })
      providers.value = (data || []).map(p => ({
        key: p.key,
        name: p.name,
        baseUrl: p.baseUrl || '',
        chatModels: p.chatModels || [],
        embedModels: p.embedModels || []
      }))
    } catch { /* ignore */ }
  }

  function selectedProviderEmbedModel() {
    const p = providers.value.find(p => p.key === draft.value.providerKey)
    if (!p || !p.embedModels || p.embedModels.length === 0) return null
    const def = p.embedModels.find(m => m.isDefault)
    return def ? def.modelName : p.embedModels[0].modelName
  }

  async function testEmbed() {
    const providerKey = draft.value.providerKey
    const apiKey = draft.value.apiKey
    let baseUrl = draft.value.baseUrl
    if (!baseUrl) {
      const p = providers.value.find(p => p.key === providerKey)
      baseUrl = p ? p.baseUrl : ''
    }
    if (!providerKey) {
      embedTestResult.value = { success: false, error: '请先选择服务商' }
      return
    }
    if (!apiKey && !draft.value.id) {
      embedTestResult.value = { success: false, error: '请先填写 API Key' }
      return
    }

    testingEmbed.value = true
    embedTestResult.value = null
    try {
      const res = await testProfileEmbed(providerKey, apiKey, baseUrl)
      embedTestResult.value = { success: true, ...res }
    } catch (e) {
      embedTestResult.value = { success: false, error: e.message || '连接失败' }
    } finally {
      testingEmbed.value = false
    }
  }

  async function loadProfiles() {
    try {
      const data = await getProfiles()
      profiles.value = data || []
    } catch { /* ignore */ }

    // Auto-migrate old config to Profile
    if (profiles.value.length === 0) {
      try {
        const { getAIConfig } = await import('@/api/aiConfig')
        const legacy = await getAIConfig()
        if (legacy && legacy.chatProvider) {
          const p = providers.value.find(p => p.key === legacy.chatProvider)
          const migrated = await saveProfile({
            profileName: p ? p.name : legacy.chatProvider,
            providerKey: legacy.chatProvider,
            apiKey: '',  // stored key not returned; user re-enters
            baseUrl: legacy.chatUrl || p?.baseUrl || '',
            enabledModels: legacy.chatModel ? [legacy.chatModel] : []
          })
          if (migrated) {
            profiles.value = [migrated]
          }
        }
      } catch { /* migration not critical */ }
    }
  }

  function startNew() {
    draft.value = emptyDraft()
    remoteModels.value = []
    refreshError.value = null
    notice.value = null
    embedTestResult.value = null
    editorMode.value = 'creating'
  }

  function selectProfile(p) {
    draft.value = profileToDraft(p)
    remoteModels.value = []
    refreshError.value = null
    notice.value = null
    embedTestResult.value = null
    editorMode.value = 'editing'
  }

  async function applyDraft() {
    if (draft.value.models.filter(m => m.enabled).length === 0) {
      notice.value = '请至少启用一个模型'
      return
    }
    try {
      const payload = draftToPayload()
      const saved = await saveProfile(payload)
      await loadProfiles()
      // refresh draft with saved data
      if (saved) {
        draft.value = profileToDraft(saved)
        editorMode.value = 'editing'
      }
      notice.value = '配置已保存'
      setTimeout(() => { if (notice.value === '配置已保存') notice.value = null }, 3000)
    } catch { /* errors handled by interceptor */ }
  }

  function requestDelete(id) {
    pendingDeleteId.value = id
  }

  function cancelDelete() {
    pendingDeleteId.value = null
  }

  async function confirmDelete() {
    if (!pendingDeleteId.value) return
    try {
      await deleteProfile(pendingDeleteId.value)
      await loadProfiles()
      if (draft.value.id === pendingDeleteId.value) {
        draft.value = emptyDraft()
        editorMode.value = 'idle'
      }
    } catch { /* ignore */ }
    pendingDeleteId.value = null
  }

  async function refreshRemoteModels() {
    let apiKey = draft.value.apiKey
    let baseUrl = draft.value.baseUrl

    if (!baseUrl) {
      refreshError.value = '请先填写 Base URL'
      return
    }
    // If editing existing profile and no new key entered, the backend uses the stored encrypted key
    if (!apiKey && draft.value.id) {
      // Pass empty string — backend needs at least one of apiKey or profileId
      // The refresh endpoint takes apiKey directly for this call
    }
    if (!apiKey && !draft.value.id) {
      refreshError.value = '请先填写 API Key 和 Base URL'
      return
    }

    refreshing.value = true
    refreshError.value = null
    try {
      const models = await refreshModels(baseUrl, apiKey)
      remoteModels.value = models || []

      // Merge into draft: add new models, preserve existing enabled state
      const existingIds = new Set(draft.value.models.map(m => m.id))
      for (const m of (models || [])) {
        if (!existingIds.has(m.id)) {
          draft.value.models.push({ id: m.id, enabled: true })
        }
      }
      notice.value = `发现 ${models.length} 个模型`
    } catch (e) {
      refreshError.value = e?.message || '刷新失败'
    } finally {
      refreshing.value = false
    }
  }

  function dismissNotice() {
    notice.value = null
  }

  function discardDraft() {
    draft.value = emptyDraft()
    editorMode.value = 'idle'
    notice.value = null
  }

  return {
    // state
    providers, profiles, draft, remoteModels, refreshing, refreshError, notice,
    pendingDeleteId, editorMode,
    testingEmbed, embedTestResult,
    // derived
    selectedProfile, isDirty, canApply, pendingDeleteProfile,
    // actions
    loadProviders, loadProfiles, startNew, selectProfile, applyDraft,
    requestDelete, cancelDelete, confirmDelete,
    refreshRemoteModels, dismissNotice, discardDraft,
    selectedProviderEmbedModel, testEmbed
  }
}
