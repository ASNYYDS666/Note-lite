<template>
  <footer class="status-bar">
    <div class="status-left">
      <span class="status-dot"></span>
      <span>{{ currentViewLabel }}</span>
    </div>
    <div class="status-right">
      <span v-if="notes.wordCount > 0">{{ notes.wordCount.toLocaleString() }} 字</span>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'
import { useWorkspaceStore } from '@/store/workspace'
import { useNotesStore } from '@/store/notes'

const workspace = useWorkspaceStore()
const notes = useNotesStore()

const currentViewLabel = computed(() => {
  if (workspace.showTrash) return '回收站'
  if (workspace.currentNoteId) return '编辑器'
  return 'Note-lite'
})
</script>

<style scoped>
.status-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: var(--status-bar-height);
  background: var(--surface-dark);
  color: var(--on-tertiary-container);
  border-top: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  font-family: var(--font-ui);
  font-size: var(--text-status-bar);
  font-weight: 500;
  line-height: 1.2;
  z-index: 100;
}

.status-left,
.status-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--success-green);
}
</style>
