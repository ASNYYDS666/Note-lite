import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

export const useUserStore = defineStore('user', () => {
  const storedToken = localStorage.getItem('token')
  const fallbackToken = storedToken || 'dev-preview-token'

  // 确保 token 同步到 localStorage（chat.js SSE 请求从 localStorage 读取）
  if (!storedToken) {
    localStorage.setItem('token', fallbackToken)
  }

  const token = ref(fallbackToken)
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null') || {
    username: 'Developer',
    email: 'dev@note-lite.local',
    createdAt: '2026-06-01'
  })

  const isLoggedIn = computed(() => !!token.value)

  async function fetchUserInfo() {
    try {
      const info = await request.get('/user/info')
      if (info) {
        userInfo.value = info
        localStorage.setItem('userInfo', JSON.stringify(info))
      }
    } catch {
      /* 预览模式或无网络时保持默认值 */
    }
  }

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    fetchUserInfo,
    setToken,
    setUserInfo,
    logout
  }
})
