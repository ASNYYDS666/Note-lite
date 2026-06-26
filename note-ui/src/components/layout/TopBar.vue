<template>
  <header class="topbar">
    <div class="topbar-left">
      <h1 class="topbar-brand">Note-lite</h1>
      <nav class="topbar-nav">
        <button class="nav-btn">File</button>
        <button class="nav-btn">Edit</button>
        <button class="nav-btn">View</button>
        <button class="nav-btn">Help</button>
      </nav>
    </div>
    <div class="topbar-right">
      <span class="user-name" v-if="userStore.userInfo?.username">
        {{ userStore.userInfo.username }}
      </span>
      <el-button text class="logout-btn" @click="handleLogout">
        退出
      </el-button>
    </div>
  </header>
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
</script>

<style scoped>
.topbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: var(--top-bar-height);
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--container-margin);
  background: var(--surface);
  border-bottom: 1px solid var(--border-default);
  backdrop-filter: blur(8px);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.topbar-brand {
  font-family: var(--font-editor);
  font-size: 18px;
  font-weight: 700;
  color: var(--primary);
  margin: 0;
  letter-spacing: -0.3px;
}

.topbar-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-btn {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
  background: none;
  border: none;
  padding: 2px 8px;
  border-radius: var(--radius-default);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.nav-btn:hover {
  background: var(--surface-container-high);
  color: var(--on-surface);
}

.nav-btn:active {
  opacity: 0.7;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
}

.logout-btn {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant) !important;
}

.logout-btn:hover {
  color: var(--error) !important;
}
</style>
