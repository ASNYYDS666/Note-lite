<template>
  <div class="editor-status-bar">
    <span class="status-file-type">Markdown</span>
    <button class="status-stats" @click="expanded = !expanded">
      <span class="material-symbols-outlined" style="font-size:14px">text_fields</span>
      <span>{{ stats.words }} words</span>
      <span v-if="expanded" class="stats-detail">
        &nbsp;· {{ stats.chars }} chars · {{ stats.lines }} lines · ~{{ stats.readTime }} min read
      </span>
    </button>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  content: { type: String, default: '' }
})

const expanded = ref(false)

const stats = computed(() => {
  const text = (props.content || '').replace(/<[^>]*>/g, '')
  const chars = text.length
  const words = text.trim() ? text.trim().split(/\s+/).length : 0
  const lines = text.split(/\n/).length
  const readTime = Math.max(1, Math.ceil(words / 200))
  return { chars, words, lines, readTime }
})
</script>

<style scoped>
.editor-status-bar {
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  border-top: 1px solid var(--border-subtle);
  background: var(--surface-container-lowest);
  font-family: var(--font-ui);
  font-size: 11px;
  color: var(--on-surface-variant);
  flex-shrink: 0;
}

.status-stats {
  display: flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  color: inherit;
  font-family: inherit;
  font-size: inherit;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: var(--radius-default);
}

.status-stats:hover {
  background: var(--surface-container);
}

.stats-detail {
  opacity: 0.7;
}
</style>
