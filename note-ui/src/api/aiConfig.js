import request from '@/utils/request'

// ==================== Profile (new) ====================

export function getProfiles() {
  return request.get('/user/profiles')
}

export function saveProfile(data) {
  return request.post('/user/profiles', data)
}

export function deleteProfile(id) {
  return request.delete(`/user/profiles/${id}`)
}

export function refreshModels(baseUrl, apiKey) {
  return request.post('/user/profiles/refresh-models', { baseUrl, apiKey })
}

export function testProfileEmbed(providerKey, apiKey, baseUrl) {
  return request.post('/user/profiles/test-embed', { providerKey, apiKey, baseUrl })
}

// ==================== Legacy config (kept for compatibility) ====================

export function getAIConfig() {
  return request.get('/user/ai-config')
}

export function saveAIConfig(data) {
  return request.post('/user/ai-config', data)
}

export function deleteAIConfig() {
  return request.delete('/user/ai-config')
}

export function testChatConnection() {
  return request.post('/user/ai-config/test-chat')
}

export function testEmbedConnection() {
  return request.post('/user/ai-config/test-embed')
}
