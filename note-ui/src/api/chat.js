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

export function parseSSEChunk(chunk) {
  const results = []
  const lines = chunk.split('\n')
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
