<template>
  <aside class="icon-rail">
    <button
      class="rail-btn"
      :class="{ active: workspace.showNoteTree }"
      title="笔记树"
      @click="workspace.switchLeftPanel('tree')"
    >
      <span class="material-symbols-outlined icon-fill">description</span>
    </button>
    <button
      class="rail-btn"
      :class="{ active: workspace.showSearchPanel }"
      title="搜索"
      @click="workspace.switchLeftPanel('search')"
    >
      <span class="material-symbols-outlined">search</span>
    </button>
    <button
      class="rail-btn"
      :class="{ active: workspace.showTrash }"
      title="回收站"
      @click="workspace.switchMainView('trash')"
    >
      <span class="material-symbols-outlined">delete</span>
    </button>

    <div class="rail-divider"></div>

    <button
      class="rail-btn"
      :class="{ active: workspace.agentPanelVisible }"
      title="AI 对话"
      @click="workspace.toggleAgentPanel()"
    >
      <span class="material-symbols-outlined icon-fill">smart_toy</span>
    </button>

    <div class="rail-spacer"></div>

    <button
      class="rail-btn"
      title="新建笔记"
      @click="handleNewNote"
    >
      <span class="material-symbols-outlined">add</span>
    </button>

    <div class="rail-divider"></div>

    <button
      class="rail-btn"
      title="设置"
      @click="workspace.openSettings()"
    >
      <span class="material-symbols-outlined">settings</span>
    </button>
  </aside>
</template>

<script setup>
import { useWorkspaceStore } from '@/store/workspace'

const workspace = useWorkspaceStore()

function handleNewNote() {
  workspace.switchMainView('editor')
  workspace.openNote(null)
}
</script>

<style scoped>
.icon-rail {
  position: fixed;
  top: var(--top-bar-height);
  left: 0;
  bottom: var(--status-bar-height);
  width: var(--icon-rail-width);
  background: var(--surface-container-lowest);
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--gutter-md) 0;
  gap: 2px;
  z-index: 90;
}

.rail-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--radius-lg);
  background: none;
  color: var(--on-surface-variant);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.rail-btn:hover {
  background: var(--surface-container);
  color: var(--on-surface);
}

.rail-btn.active {
  background: var(--secondary-container);
  color: var(--on-secondary-container);
}

.rail-btn .material-symbols-outlined {
  font-size: 18px;
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}

.rail-btn.active .material-symbols-outlined {
  font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}

.icon-fill {
  font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24 !important;
}

.rail-divider {
  width: 24px;
  height: 1px;
  background: var(--border-subtle);
  margin: 4px 0;
}

.rail-spacer {
  flex: 1;
}
</style>
