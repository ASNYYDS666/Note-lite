<template>
  <div class="tab-content">
    <div class="setting-row">
      <div class="setting-info">
        <span class="setting-label">Language</span>
        <span class="setting-desc">界面语言切换</span>
      </div>
      <el-select v-model="lang" size="small" style="width: 160px" disabled>
        <el-option label="简体中文" value="zh-CN" />
        <el-option label="English" value="en-US" />
      </el-select>
    </div>
    <div class="setting-row">
      <div class="setting-info">
        <span class="setting-label">Theme</span>
        <span class="setting-desc">亮色 / 暗色主题切换</span>
      </div>
      <el-select v-model="theme" size="small" style="width: 160px" @change="onThemeChange">
        <el-option label="Light" value="light" />
        <el-option label="Dark" value="dark" />
      </el-select>
    </div>
    <p class="placeholder-note">语言切换暂未开放</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const THEME_KEY = 'note-lite-theme'

const lang = ref('zh-CN')
const theme = ref('light')

onMounted(() => {
  const saved = localStorage.getItem(THEME_KEY)
  if (saved === 'dark' || saved === 'light') {
    theme.value = saved
  }
})

function onThemeChange(val) {
  localStorage.setItem(THEME_KEY, val)
  if (val === 'dark') {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
}
</script>

<style scoped>
.tab-content {
  padding: 20px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-subtle);
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.setting-label {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  font-weight: 500;
  color: var(--on-surface);
}

.setting-desc {
  font-family: var(--font-ui);
  font-size: 11px;
  color: var(--on-surface-variant);
}

.placeholder-note {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--outline);
  margin-top: 16px;
}
</style>
