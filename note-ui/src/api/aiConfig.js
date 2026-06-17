import request from '@/utils/request'

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
