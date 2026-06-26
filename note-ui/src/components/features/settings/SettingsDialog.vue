<template>
  <Teleport to="body">
    <div v-if="workspace.settingsDialogVisible" class="settings-overlay" @click.self="workspace.closeSettings()">
      <div class="settings-modal">
        <div class="settings-header">
          <div class="header-left">
            <span class="material-symbols-outlined header-icon">settings</span>
            <h1 class="header-title">Workspace Settings</h1>
          </div>
          <button class="close-btn" @click="workspace.closeSettings()">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="settings-body">
          <nav class="settings-nav">
            <button
              class="nav-item"
              :class="{ active: activeTab === 'general' }"
              @click="activeTab = 'general'"
            >
              <span class="material-symbols-outlined nav-icon">tune</span>
              <span>General</span>
            </button>
            <button
              class="nav-item"
              :class="{ active: activeTab === 'ai' }"
              @click="activeTab = 'ai'"
            >
              <span class="material-symbols-outlined nav-icon">smart_toy</span>
              <span>AI Model</span>
            </button>
            <button
              class="nav-item"
              :class="{ active: activeTab === 'account' }"
              @click="activeTab = 'account'"
            >
              <span class="material-symbols-outlined nav-icon">person_outline</span>
              <span>Account</span>
            </button>
            <button
              class="nav-item"
              :class="{ active: activeTab === 'about' }"
              @click="activeTab = 'about'"
            >
              <span class="material-symbols-outlined nav-icon">info</span>
              <span>About</span>
            </button>
          </nav>
          <div class="settings-content">
            <GeneralTab v-if="activeTab === 'general'" />
            <AIModelTab v-else-if="activeTab === 'ai'" />
            <AccountTab v-else-if="activeTab === 'account'" />
            <AboutTab v-else-if="activeTab === 'about'" />
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
import { useWorkspaceStore } from '@/store/workspace'
import GeneralTab from './GeneralTab.vue'
import AIModelTab from './AIModelTab.vue'
import AccountTab from './AccountTab.vue'
import AboutTab from './AboutTab.vue'

const workspace = useWorkspaceStore()
const activeTab = ref('ai')
</script>

<style scoped>
.settings-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(2px);
}

.settings-modal {
  width: 100%;
  max-width: 920px;
  height: 640px;
  background: var(--bg);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-dialog);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border-subtle);
}

.dark .settings-modal {
  background: var(--surface-container-high);
}

.settings-header {
  height: 56px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-subtle);
  flex-shrink: 0;
}

.dark .settings-header {
  background: rgba(44, 47, 46, 0.8);
  backdrop-filter: blur(8px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 20px;
  color: var(--accent-sage-dark);
}

.header-title {
  font-family: var(--font-editor);
  font-size: 24px;
  font-weight: 600;
  color: var(--on-surface);
  margin: 0;
}

.dark .header-title {
  color: var(--primary-fixed-dim);
}

.close-btn {
  padding: 4px;
  border: none;
  border-radius: var(--radius-default);
  background: none;
  color: var(--on-surface-variant);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s;
}

.close-btn:hover {
  color: var(--on-surface);
}

.settings-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.settings-nav {
  width: 200px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  border-right: 1px solid var(--border-subtle);
  background: var(--surface-container-low);
  flex-shrink: 0;
}

.dark .settings-nav {
  background: rgba(26, 28, 27, 0.5);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 12px;
  border: none;
  border-radius: var(--radius-lg);
  background: none;
  font-family: var(--font-ui);
  font-size: var(--text-ui-base);
  color: var(--on-surface-variant);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  text-align: left;
}

.nav-item:hover {
  background: var(--surface-container-high);
}

.dark .nav-item:hover {
  background: var(--surface-dark);
}

.nav-item.active {
  background: var(--secondary-container);
  color: var(--on-secondary-container);
  font-weight: 600;
}

.dark .nav-item.active {
  background: rgba(60, 74, 60, 0.4);
  color: var(--accent-sage-dark);
}

.nav-icon {
  font-size: 20px;
}

.settings-content {
  flex: 1;
  overflow: hidden;
}
</style>
