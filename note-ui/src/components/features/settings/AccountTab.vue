<template>
  <div class="tab-content">
    <div class="info-row">
      <span class="info-label">Username</span>
      <span class="info-value">{{ userStore.userInfo?.username || '-' }}</span>
    </div>
    <div class="info-row">
      <span class="info-label">Email</span>
      <span class="info-value">{{ userStore.userInfo?.email || '-' }}</span>
    </div>
    <div class="info-row">
      <span class="info-label">Registered</span>
      <span class="info-value">{{ formatDate(userStore.userInfo?.createdAt) }}</span>
    </div>
    <div class="logout-section">
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()

onMounted(() => {
  userStore.fetchUserInfo()
})

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.tab-content {
  padding: 20px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-subtle);
}

.info-label {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
}

.info-value {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface);
  font-weight: 500;
}

.logout-section {
  margin-top: 24px;
}

.logout-btn {
  padding: 6px 20px;
  border: 1px solid var(--error-red);
  border-radius: var(--radius-default);
  background: none;
  color: var(--error-red);
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  cursor: pointer;
}

.logout-btn:hover {
  background: var(--error-container);
}
</style>
