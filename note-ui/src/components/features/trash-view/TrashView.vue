<template>
  <div class="trash-view">
    <div class="trash-header">
      <div class="header-left">
        <h1 class="trash-title">Recycle Bin</h1>
        <span class="trash-badge">{{ notes.trashTotal }} deleted items</span>
      </div>
      <button class="clear-btn" @click="handleClearAll" :disabled="notes.trashTotal === 0">
        <span class="material-symbols-outlined">delete_sweep</span>
        <span>Clear All</span>
      </button>
    </div>

    <div class="trash-body custom-scrollbar">
      <!-- Empty state -->
      <div v-if="notes.trashNotes.length === 0" class="trash-empty">
        <span class="material-symbols-outlined empty-icon">auto_delete</span>
        <p class="empty-title">回收站为空</p>
        <p class="empty-desc">删除的笔记会出现在这里，30天内可恢复</p>
      </div>

      <!-- Table header -->
      <div v-else class="trash-table">
        <div class="trash-table-header">
          <div class="col-name">Name</div>
          <div class="col-folder">Original Path</div>
          <div class="col-actions">Actions</div>
        </div>

        <div
          v-for="note in notes.trashNotes"
          :key="note.id"
          class="trash-row group"
          @contextmenu.prevent="showMenu($event, note)"
        >
          <div class="col-name">
            <span class="material-symbols-outlined file-icon">description</span>
            <span class="file-name">{{ note.title || '无标题' }}</span>
          </div>
          <div class="col-folder">
            <span class="folder-path">{{ getFolderPath(note.folderId) }}</span>
          </div>
          <div class="col-actions">
            <button class="icon-btn restore" title="恢复" @click="handleRestore(note.id)">
              <span class="material-symbols-outlined">restore_from_trash</span>
            </button>
            <button class="icon-btn delete" title="永久删除" @click="handlePermanentDelete(note.id)">
              <span class="material-symbols-outlined">delete_forever</span>
            </button>
          </div>
        </div>

        <!-- Decorative footer quote -->
        <div class="trash-quote">
          <span class="material-symbols-outlined quote-icon">auto_delete</span>
          <p class="quote-text">Cleanliness is next to digital efficiency.</p>
        </div>
      </div>
    </div>

    <!-- Context Menu -->
    <Teleport to="body">
      <div
        v-if="contextMenu.visible"
        class="context-menu"
        :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
        @click.stop
      >
        <button class="ctx-item" @click="handleRestore(contextMenu.noteId); contextMenu.visible = false">
          <span class="material-symbols-outlined">restore_from_trash</span>
          <span>Restore</span>
        </button>
        <div class="ctx-divider"></div>
        <button class="ctx-item danger" @click="handlePermanentDelete(contextMenu.noteId); contextMenu.visible = false">
          <span class="material-symbols-outlined">delete_forever</span>
          <span>Permanently Delete</span>
        </button>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useNotesStore } from '@/store/notes'

const notes = useNotesStore()

const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  noteId: null
})

function showMenu(e, note) {
  contextMenu.x = e.clientX
  contextMenu.y = e.clientY
  contextMenu.noteId = note.id
  contextMenu.visible = true
}

function hideMenu() {
  contextMenu.visible = false
}

function getFolderPath(folderId) {
  // 简化：从 noteTree 查找文件夹路径
  if (!folderId) return '/'
  const tree = notes.noteTree
  if (!tree) return '/'
  const folder = findFolderById(tree.folders, folderId)
  return folder ? '/' + folder : '/'
}

function findFolderById(folders, id) {
  for (const f of folders) {
    if (f.id === id) return f.name
    if (f.children) {
      const found = findFolderById(f.children, id)
      if (found) return f.name + '/' + found
    }
  }
  return null
}

async function handleRestore(id) {
  try {
    await notes.restoreNote(id)
    ElMessage.success('已恢复到原文件夹')
  } catch {
    ElMessage.error('恢复失败')
  }
}

async function handlePermanentDelete(id) {
  try {
    await ElMessageBox.confirm('将永久删除该笔记，不可恢复', '确认删除', { type: 'warning' })
    await notes.permanentDeleteNote(id)
    ElMessage.success('已永久删除')
  } catch { /* cancelled */ }
}

async function handleClearAll() {
  try {
    await ElMessageBox.confirm('将清空回收站中的所有笔记，不可恢复', '确认清空', { type: 'warning' })
    await notes.clearTrash()
    ElMessage.success('回收站已清空')
  } catch { /* cancelled */ }
}

function handleClickOutside(e) {
  if (contextMenu.visible) hideMenu()
}

onMounted(() => {
  notes.loadTrash()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.trash-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}

/* ===== Header ===== */
.trash-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  min-height: 64px;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--bg);
}

.dark .trash-header {
  background: rgba(30, 33, 32, 0.4);
  backdrop-filter: blur(12px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.trash-title {
  font-family: var(--font-editor);
  font-size: var(--text-headline-md);
  font-weight: 600;
  color: var(--on-surface);
  margin: 0;
}

.dark .trash-title {
  color: var(--primary-fixed);
}

.trash-badge {
  padding: 2px 8px;
  border-radius: var(--radius-default);
  background: rgba(60, 74, 60, 0.3);
  color: var(--secondary-fixed);
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
}

.clear-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border: 1px solid var(--error-red);
  border-radius: var(--radius-lg);
  background: none;
  color: var(--error-red);
  font-family: var(--font-ui);
  font-size: var(--text-ui-base);
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
}

.clear-btn:hover:not(:disabled) {
  background: rgba(186, 26, 26, 0.1);
}

.clear-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.clear-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.clear-btn .material-symbols-outlined {
  font-size: 18px;
}

/* ===== Body ===== */
.trash-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.trash-table {
  max-width: 1024px;
  margin: 0 auto;
}

.trash-table-header {
  display: grid;
  grid-template-columns: 7fr 3fr 2fr;
  padding: 8px 16px;
  border-bottom: 1px solid var(--border-subtle);
  font-family: var(--font-ui);
  font-size: var(--text-ui-label);
  font-weight: 600;
  color: var(--on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 8px;
}

.trash-row {
  display: grid;
  grid-template-columns: 7fr 3fr 2fr;
  align-items: center;
  padding: 10px 16px;
  border-radius: var(--radius-default);
  transition: background 0.15s;
  cursor: default;
}

.trash-row:hover {
  background: var(--surface-container);
}

.dark .trash-row:hover {
  background: rgba(60, 74, 60, 0.1);
}

.col-name {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.file-icon {
  font-size: 18px;
  color: var(--on-surface-variant);
  flex-shrink: 0;
}

.file-name {
  font-family: var(--font-ui);
  font-size: var(--text-ui-base);
  color: var(--on-surface);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark .file-name {
  color: var(--primary-fixed);
}

.col-folder {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.folder-path {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
}

.col-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}

.trash-row:hover .col-actions {
  opacity: 1;
}

.icon-btn {
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

.icon-btn .material-symbols-outlined {
  font-size: 18px;
}

.icon-btn.restore:hover {
  color: var(--accent-sage-dark);
}

.icon-btn.delete:hover {
  color: var(--error-red);
}

/* ===== Empty State ===== */
.trash-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 56px;
  color: var(--on-surface-variant);
  opacity: 0.3;
  margin-bottom: 16px;
}

.empty-title {
  font-family: var(--font-editor);
  font-size: var(--text-headline-md);
  color: var(--on-surface-variant);
  margin: 0 0 8px;
}

.empty-desc {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
  opacity: 0.6;
}

/* ===== Decorative Quote ===== */
.trash-quote {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  margin-top: 32px;
  opacity: 0.3;
  user-select: none;
}

.quote-icon {
  font-size: 48px;
  color: var(--on-surface-variant);
  margin-bottom: 16px;
}

.quote-text {
  font-family: var(--font-editor);
  font-size: var(--text-editor-body);
  font-style: italic;
  color: var(--on-surface-variant);
  margin: 0;
}

/* ===== Context Menu ===== */
.context-menu {
  position: fixed;
  z-index: 300;
  min-width: 180px;
  padding: 4px;
  background: var(--surface-container-lowest);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-dialog);
  backdrop-filter: blur(12px);
}

.dark .context-menu {
  background: rgba(30, 33, 32, 0.95);
}

.ctx-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px 12px;
  border: none;
  border-radius: var(--radius-default);
  background: none;
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface);
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;
}

.ctx-item:hover {
  background: var(--surface-container);
}

.dark .ctx-item {
  color: var(--primary-fixed);
}

.dark .ctx-item:hover {
  background: rgba(60, 74, 60, 0.2);
}

.ctx-item.danger {
  color: var(--error-red);
}

.ctx-item .material-symbols-outlined {
  font-size: 18px;
}

.ctx-divider {
  height: 1px;
  background: var(--border-subtle);
  margin: 4px 8px;
}
</style>
