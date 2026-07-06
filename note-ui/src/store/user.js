import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { useAIStore } from '@/store/ai'

export const useUserStore = defineStore('user', () => {
  const storedToken = localStorage.getItem('token')
  const token = ref(storedToken || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

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
    // 清理 AI 对话状态，防止切换账号后残留
    useAIStore().newConversation()
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
