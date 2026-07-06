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

// ==================== Embedding 独立配置 ====================

export function getEmbeddingConfig() {
  return request.get('/user/embedding-config')
}

export function saveEmbeddingConfig(data) {
  return request.put('/user/embedding-config', data)
}

export function getEmbeddingProviders() {
  return request.get('/user/embedding-providers')
}

export function testEmbeddingConnection(providerKey) {
  return request.post('/user/embedding-config/test', { providerKey })
}

// ==================== Legacy config (kept for compatibility) ====================

export function getAIConfig() {
  return request.get('/user/ai-config')
}
