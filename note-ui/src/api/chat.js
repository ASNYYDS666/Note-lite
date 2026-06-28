const BASE_URL = '/api/v1'

function getToken() {
  const raw = localStorage.getItem('token')
  if (raw) return raw
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
    if (userInfo && userInfo.token) return userInfo.token
  } catch { /* ignore */ }
  return ''
}

export async function streamChat({ question, scopeType, scopeIds, style, conversationId, profileId, modelName }) {
  const controller = new AbortController()

  const body = {
    question,
    scopeType: scopeType || 'ALL',
    scopeIds: scopeIds || [],
    style: style || 'detailed'
  }
  if (conversationId) body.conversationId = conversationId
  if (profileId) body.profileId = profileId
  if (modelName) body.modelName = modelName

  const response = await fetch(`${BASE_URL}/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getToken()}`
    },
    body: JSON.stringify(body),
    signal: controller.signal
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `请求失败 (${response.status})`)
  }

  return {
    reader: response.body.getReader(),
    abort: () => controller.abort()
  }
}

// ==================== 对话管理 ====================

export async function fetchConversations() {
  const resp = await fetch(`${BASE_URL}/conversations`, {
    headers: { 'Authorization': `Bearer ${getToken()}` }
  })
  if (!resp.ok) throw new Error('获取对话列表失败')
  const result = await resp.json()
  return result.data || []
}

export async function fetchMessages(conversationId) {
  const resp = await fetch(`${BASE_URL}/conversations/${conversationId}/messages`, {
    headers: { 'Authorization': `Bearer ${getToken()}` }
  })
  if (!resp.ok) throw new Error('获取消息失败')
  const result = await resp.json()
  return result.data || []
}

export async function deleteConversation(conversationId) {
  const resp = await fetch(`${BASE_URL}/conversations/${conversationId}`, {
    method: 'DELETE',
    headers: { 'Authorization': `Bearer ${getToken()}` }
  })
  if (!resp.ok) throw new Error('删除对话失败')
}

// ==================== SSE ====================

export function createSSEParser() {
  let pendingLine = ''

  return function parse(chunk) {
    const results = []
    const combined = pendingLine + chunk
    const lines = combined.split('\n')
    // 最后一行可能不完整，保留到下次拼接
    pendingLine = lines.pop() || ''

    for (const line of lines) {
      if (!line.startsWith('data:')) continue
      // Spring SseEmitter 发送 "data:{...}"(5字符)，标准 SSE 发送 "data: {...}"(6字符)
      const jsonStr = line.slice(5).trim()
      if (!jsonStr) continue
      try {
        results.push(JSON.parse(jsonStr))
      } catch (e) {
        /* ignore malformed JSON */
      }
    }
    return results
  }
}

/**
 * @deprecated 使用 createSSEParser() 代替，避免跨 chunk 行拆分导致 token 丢失
 */
export function parseSSEChunk(chunk) {
  const results = []
  const lines = chunk.split('\n')
  for (const line of lines) {
    if (!line.startsWith('data:')) continue
    const jsonStr = line.slice(5).trim()
    if (!jsonStr) continue
    try {
      results.push(JSON.parse(jsonStr))
    } catch (e) {
      /* ignore malformed JSON */
    }
  }
  return results
}
