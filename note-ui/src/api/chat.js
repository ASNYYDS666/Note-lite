const BASE_URL = '/api/v1'

function getToken() {
  return localStorage.getItem('token') || ''
}

/**
 * SSE 流式对话
 * @param {{ question, scopeType, scopeIds, style }} params
 * @returns {Promise<{ reader: ReadableStreamDefaultReader, abort: Function }>}
 */
export async function streamChat({ question, scopeType, scopeIds, style }) {
  const controller = new AbortController()

  const response = await fetch(`${BASE_URL}/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getToken()}`
    },
    body: JSON.stringify({
      question,
      scopeType: scopeType || 'ALL',
      scopeIds: scopeIds || [],
      style: style || 'detailed'
    }),
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

/**
 * 解析 SSE 数据块
 * @param {string} chunk - SSE 文本块
 * @returns {{ token?: string, done?: boolean, error?: string, sources?: Array }[]}
 */
export function parseSSEChunk(chunk) {
  const results = []
  const lines = chunk.split('\n')
  for (const line of lines) {
    if (!line.startsWith('data: ')) continue
    const jsonStr = line.slice(6).trim()
    if (!jsonStr) continue
    try {
      results.push(JSON.parse(jsonStr))
    } catch (e) {
      /* ignore malformed JSON */
    }
  }
  return results
}
