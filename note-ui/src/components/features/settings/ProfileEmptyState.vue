<template>
  <div class="profile-empty" :class="variant">
    <span class="material-symbols-outlined empty-icon">
      {{ variant === 'no-profiles' ? 'smart_toy' : 'arrow_back' }}
    </span>
    <p class="empty-title" v-if="variant === 'no-profiles'">还没有配置 API Profile</p>
    <p class="empty-hint">
      {{ variant === 'no-profiles'
        ? '创建一个 Profile 来连接 AI 服务商，然后即可使用 AI 对话功能。'
        : '从左侧列表选择一个 Profile 开始编辑，或点击 + 新建。' }}
    </p>
    <button v-if="variant === 'no-profiles'" class="empty-action" @click="$emit('add')">
      <span class="material-symbols-outlined">add</span> 新建 Profile
    </button>
  </div>
</template>

<script setup>
defineProps({
  variant: { type: String, default: 'no-profiles' } // 'no-profiles' | 'no-selection'
})
defineEmits(['add'])
</script>

<style scoped>
.profile-empty {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 40px 24px; text-align: center; gap: 8px;
}
.profile-empty.no-selection { opacity: 0.6; }

.empty-icon { font-size: 40px; color: var(--outline-variant); margin-bottom: 8px; }
.empty-title { font-size: 15px; font-weight: 600; color: var(--on-surface); margin: 0; }
.empty-hint { font-size: 12px; color: var(--on-surface-variant); margin: 0; max-width: 260px; line-height: 1.5; }

.empty-action {
  display: flex; align-items: center; gap: 6px; margin-top: 12px;
  padding: 8px 20px; border: none; border-radius: var(--radius-lg);
  background: var(--accent-sage); color: var(--on-secondary);
  font-family: var(--font-ui); font-size: var(--text-ui-sm); font-weight: 600;
  cursor: pointer;
}
.empty-action:hover { filter: brightness(1.1); }
.empty-action .material-symbols-outlined { font-size: 16px; }
</style>
