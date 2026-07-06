<template>
  <div class="search-panel">
    <div class="search-header">
      <span class="search-header-label">Search</span>
    </div>
    <div class="search-input-wrap">
      <span class="material-symbols-outlined search-icon">search</span>
      <input
        v-model="keyword"
        class="search-input"
        placeholder="搜索笔记标题..."
        @input="handleSearch"
      />
    </div>
    <div class="search-body custom-scrollbar">
      <div v-if="!keyword" class="search-empty">输入关键词开始搜索</div>
      <div v-else-if="filteredList.length === 0" class="search-empty">无匹配结果</div>
      <div
        v-for="item in filteredList"
        :key="item.id"
        class="search-result-item"
        @click="workspace.openNote(item.id)"
      >
        <span class="material-symbols-outlined">description</span>
        <span>{{ item.title }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

import { useNotesStore } from '@/store/notes'
import { useWorkspaceStore } from '@/store/workspace'

const notes = useNotesStore()
const workspace = useWorkspaceStore()
const keyword = ref('')

function flattenTree(tree, result = []) {
  if (!tree || typeof tree !== 'object') return result

  // 收集当前层级笔记 (根层 NoteTreeVO.notes / 文件夹层 FolderNode.notes)
  if (Array.isArray(tree.notes)) {
    for (const note of tree.notes) {
      result.push({ id: note.id, title: note.title })
    }
  }

  // 递归子节点 (根层 NoteTreeVO.folders / 文件夹层 FolderNode.children)
  const children = tree.folders || tree.children
  if (Array.isArray(children)) {
    for (const child of children) {
      flattenTree(child, result)
    }
  }

  return result
}

const allNotes = computed(() => flattenTree(notes.noteTree))

const filteredList = computed(() => {
  if (!keyword.value.trim()) return []
  const kw = keyword.value.trim().toLowerCase()
  return allNotes.value.filter(n => n.title && n.title.toLowerCase().includes(kw)).slice(0, 20)
})

function handleSearch() {
  // local filtering, no API call
}
</script>

<style scoped>
.search-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.search-header {
  padding: 8px 12px;
}

.search-header-label {
  font-family: var(--font-ui);
  font-size: var(--text-ui-label);
  font-weight: 600;
  color: var(--on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.search-input-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 8px 8px;
  padding: 4px 8px;
  background: var(--surface-container-lowest);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-default);
}

.search-input-wrap:focus-within {
  border-color: var(--accent-sage);
}

.search-icon {
  font-size: 14px;
  color: var(--on-surface-variant);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface);
}

.search-input::placeholder {
  color: var(--outline-variant);
}

.search-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.search-empty {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
  text-align: center;
  padding: 24px 0;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: var(--radius-default);
  cursor: pointer;
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface);
  transition: background 0.15s;
}

.search-result-item:hover {
  background: var(--surface-container);
}

.search-result-item .el-icon {
  font-size: 14px;
  color: var(--on-surface-variant);
  flex-shrink: 0;
}
</style>
