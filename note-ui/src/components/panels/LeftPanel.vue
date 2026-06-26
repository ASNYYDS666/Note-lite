<template>
  <div class="left-panel" :style="{ width: workspace.leftPanelWidth + 'px' }">
    <NoteTree v-if="workspace.showNoteTree" />
    <SearchPanel v-else-if="workspace.showSearchPanel" />
  </div>
</template>

<script setup>
import { useWorkspaceStore } from '@/store/workspace'
import NoteTree from '@/components/features/note-tree/NoteTree.vue'
import SearchPanel from '@/components/features/search-panel/SearchPanel.vue'

const workspace = useWorkspaceStore()
</script>

<style scoped>
.left-panel {
  position: fixed;
  top: var(--top-bar-height);
  left: var(--icon-rail-width);
  bottom: var(--status-bar-height);
  background: var(--surface-container-low);
  border-right: 1px solid var(--border-subtle);
  overflow: hidden;
  z-index: 80;
}
</style>
