<template>
  <div class="note-tree">
    <div class="tree-header">
      <span class="tree-header-label">Explorer</span>
      <button class="tree-header-action" title="新建文件夹" @click="startCreate(null, 'folder')">
        <span class="material-symbols-outlined">create_new_folder</span>
      </button>
    </div>
    <div
      class="tree-body custom-scrollbar"
      @contextmenu="onBodyContextMenu"
    >
      <el-tree
        :data="treeData"
        :props="treeProps"
        node-key="id"
        highlight-current
        :expand-on-click-node="false"
        @node-click="handleNodeClick"
        @node-contextmenu="handleContextMenu"
      >
        <template #default="{ node, data }">
          <template v-if="data.isCreating">
            <span class="tree-node" :class="{ 'is-folder': data.createType === 'folder', 'is-note': data.createType === 'note' }">
              <span v-if="data.createType === 'folder'" class="material-symbols-outlined node-icon folder-icon">folder</span>
              <span v-else class="material-symbols-outlined node-icon note-icon">description</span>
              <InlineInput
                :defaultValue="''"
                style="flex:1"
                @confirm="(v) => handleCreateConfirm(v)"
                @cancel="handleCreateCancel"
              />
            </span>
          </template>
          <template v-else-if="renamingId === data.id">
            <span class="tree-node" :class="{ 'is-folder': data.isFolder, 'is-note': !data.isFolder }">
              <span v-if="data.isFolder" class="material-symbols-outlined node-icon folder-icon">folder</span>
              <span v-else class="material-symbols-outlined node-icon note-icon">description</span>
              <InlineInput
                :defaultValue="data.label"
                style="flex:1"
                @confirm="(v) => handleRenameConfirm(data, v)"
                @cancel="renamingId = null"
              />
            </span>
          </template>
          <template v-else>
            <span class="tree-node" :class="{ 'is-folder': data.isFolder, 'is-note': !data.isFolder }">
              <span v-if="data.isFolder" class="material-symbols-outlined node-icon folder-icon">folder</span>
              <span v-else class="material-symbols-outlined node-icon note-icon">description</span>
              <span class="node-label">{{ data.label }}</span>
              <span v-if="!data.isFolder && isDirtyNote(data.raw?.id)" class="dirty-dot" title="Unsaved">●</span>
            </span>
          </template>
        </template>
      </el-tree>
    </div>

    <!-- Context menu -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
    >
      <template v-if="contextMenu.node === null">
        <!-- Empty area -->
        <button @click="startCreate(null, 'note')">
          <span class="material-symbols-outlined">note_add</span> 新建笔记
        </button>
        <button @click="startCreate(null, 'folder')">
          <span class="material-symbols-outlined">create_new_folder</span> 新建文件夹
        </button>
      </template>
      <template v-else-if="contextMenu.node.isFolder">
        <button @click="startCreate(contextMenu.node.raw.id, 'note')">
          <span class="material-symbols-outlined">note_add</span> 新建笔记
        </button>
        <button @click="startCreate(contextMenu.node.raw.id, 'folder')">
          <span class="material-symbols-outlined">create_new_folder</span> 新建文件夹
        </button>
        <div class="ctx-divider"></div>
        <button @click="startRename(contextMenu.node)">
          <span class="material-symbols-outlined">edit</span> 重命名
        </button>
        <button class="danger" @click="handleDeleteFolder">
          <span class="material-symbols-outlined">delete</span> 删除文件夹
        </button>
      </template>
      <template v-else>
        <button @click="startRename(contextMenu.node)">
          <span class="material-symbols-outlined">edit</span> 重命名
        </button>
        <button @click="handleMoveNote">
          <span class="material-symbols-outlined">drive_file_move</span> 移动到...
        </button>
        <button @click="handleShareNote">
          <span class="material-symbols-outlined">share</span> 生成分享码
        </button>
        <div class="ctx-divider"></div>
        <button class="danger" @click="handleSoftDelete">
          <span class="material-symbols-outlined">delete</span> 移入回收站
        </button>
      </template>
    </div>

    <!-- Move note dialog -->
    <el-dialog v-model="moveDialogVisible" title="移动到..." width="360px">
      <div class="move-folder-list">
        <div
          class="move-folder-item"
          :class="{ 'is-selected': false }"
          @click="doMoveNote(null)"
        >
          <span class="material-symbols-outlined">folder_open</span>
          <span>根目录</span>
        </div>
        <div
          v-for="f in folderOptions"
          :key="f.id"
          class="move-folder-item"
          :style="{ paddingLeft: (12 + f.depth * 20) + 'px' }"
          @click="doMoveNote(f.id)"
        >
          <span class="material-symbols-outlined">folder</span>
          <span>{{ f.name }}</span>
        </div>
      </div>
      <div v-if="folderOptions.length === 0" class="move-empty">暂无文件夹</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useNotesStore } from '@/store/notes'
import { useWorkspaceStore } from '@/store/workspace'
import request from '@/utils/request'
import InlineInput from './InlineInput.vue'

const notes = useNotesStore()
const workspace = useWorkspaceStore()

const treeProps = { children: 'children', label: 'label' }
const contextMenu = ref({ visible: false, x: 0, y: 0, node: undefined })

// Inline creation state
const creatingIn = ref(null)       // raw folder ID, null = root
const creatingType = ref(null)     // 'note' | 'folder' | null
const renamingId = ref(null)       // tree node key

function closeContextMenu() {
  contextMenu.value.visible = false
}

onMounted(() => {
  notes.loadTree()
  document.addEventListener('click', closeContextMenu)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeContextMenu)
})

function isDirtyNote(noteId) {
  if (!noteId) return false
  return workspace.currentNoteId === noteId
}

// ---- Tree data construction with inline create placeholder ----

function convertFolderNode(fn) {
  const noteChildren = (fn.notes || []).map(n => ({
    id: `note-${n.id}`,
    label: n.title,
    isFolder: false,
    children: [],
    raw: { id: n.id, title: n.title, folderId: n.folderId, updatedAt: n.updatedAt }
  }))
  const folderChildren = (fn.children || []).map(convertFolderNode)
  const children = [...folderChildren, ...noteChildren]

  // Inject creating placeholder if creating inside this folder
  if (creatingIn.value === fn.id && creatingType.value) {
    children.unshift({
      id: `creating-${fn.id}`,
      label: '',
      isFolder: false,
      isCreating: true,
      createType: creatingType.value,
      children: []
    })
  }

  return {
    id: `folder-${fn.id}`,
    label: fn.name,
    isFolder: true,
    children,
    raw: { id: fn.id, name: fn.name, parentId: fn.parentId }
  }
}

function buildTreeData(rawTree) {
  if (!rawTree || typeof rawTree !== 'object') return []
  const nodes = []

  // Inject root-level creating placeholder
  if (creatingIn.value === null && creatingType.value) {
    nodes.push({
      id: 'creating-root',
      label: '',
      isFolder: false,
      isCreating: true,
      createType: creatingType.value,
      children: []
    })
  }

  if (rawTree.folders && Array.isArray(rawTree.folders)) {
    nodes.push(...rawTree.folders.map(convertFolderNode))
  }
  if (rawTree.notes && Array.isArray(rawTree.notes)) {
    nodes.push(...rawTree.notes.map(n => ({
      id: `note-${n.id}`,
      label: n.title,
      isFolder: false,
      children: [],
      raw: { id: n.id, title: n.title, folderId: n.folderId, updatedAt: n.updatedAt }
    })))
  }
  return nodes
}

const treeData = computed(() => buildTreeData(notes.noteTree))

// ---- Inline creation ----

function startCreate(parentFolderId, type) {
  contextMenu.value.visible = false
  creatingIn.value = parentFolderId
  creatingType.value = type
}

async function handleCreateConfirm(name) {
  const type = creatingType.value
  const parentId = creatingIn.value
  creatingIn.value = null
  creatingType.value = null

  try {
    if (type === 'folder') {
      await notes.createFolder(name.trim(), parentId)
      ElMessage.success('文件夹已创建')
    } else {
      await notes.createNote({
        title: name.trim(),
        content: '',
        folderId: parentId,
        tags: []
      })
      ElMessage.success('笔记已创建')
    }
  } catch {
    ElMessage.error('创建失败')
  }
}

function handleCreateCancel() {
  creatingIn.value = null
  creatingType.value = null
}

// ---- Inline rename ----

function startRename(node) {
  contextMenu.value.visible = false
  renamingId.value = node.id
}

async function handleRenameConfirm(data, newName) {
  renamingId.value = null
  try {
    if (data.isFolder) {
      await notes.renameFolder(data.raw.id, newName.trim())
      ElMessage.success('已重命名')
    } else {
      await notes.renameNote(data.raw.id, newName.trim())
      ElMessage.success('已重命名')
    }
  } catch {
    ElMessage.error('重命名失败')
  }
}

// ---- Context menu handlers ----

function onBodyContextMenu(e) {
  // Only trigger if clicking on the empty area, not on a tree node
  if (e.target.closest('.el-tree-node')) return
  e.preventDefault()
  contextMenu.value = { visible: true, x: e.clientX, y: e.clientY, node: null }
}

function handleNodeClick(data) {
  contextMenu.value.visible = false
  renamingId.value = null
  if (data.isCreating) return
  if (data.isFolder) {
    workspace.selectedFolderId = data.raw.id
  } else {
    workspace.openNote(data.raw.id)
  }
}

function handleContextMenu(event, data) {
  event.preventDefault()
  if (data.isCreating) return
  contextMenu.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY,
    node: data
  }
}

async function handleDeleteFolder() {
  const node = contextMenu.value.node
  contextMenu.value.visible = false
  try {
    await ElMessageBox.confirm('删除文件夹后，内部笔记将自动移入回收站', '确认删除', { type: 'warning' })
    await notes.deleteFolder(node.raw.id)
    ElMessage.success('文件夹已删除')
  } catch { /* cancelled */ }
}

const moveDialogVisible = ref(false)
const movingNote = ref(null)

const folderOptions = computed(() => {
  const raw = notes.noteTree
  if (!raw || !raw.folders) return []
  const result = []
  function walk(folders, depth) {
    for (const f of folders) {
      result.push({ id: f.id, name: f.name, depth })
      if (f.children) walk(f.children, depth + 1)
    }
  }
  walk(raw.folders, 0)
  return result
})

function handleMoveNote() {
  const node = contextMenu.value.node
  contextMenu.value.visible = false
  movingNote.value = node.raw
  moveDialogVisible.value = true
}

async function doMoveNote(targetFolderId) {
  try {
    await notes.moveNote(movingNote.value.id, targetFolderId)
    ElMessage.success(`已移动到 ${targetFolderId ? '指定文件夹' : '根目录'}`)
    moveDialogVisible.value = false
  } catch {
    ElMessage.error('移动失败')
  }
}

async function handleShareNote() {
  const node = contextMenu.value.node
  contextMenu.value.visible = false
  try {
    const res = await request.post('/share', { noteId: node.raw.id, permission: 'READ' })
    await navigator.clipboard.writeText(res.shareCode)
    ElMessage.success(`分享码已复制：${res.shareCode}`)
  } catch {
    ElMessage.error('生成分享码失败')
  }
}

async function handleSoftDelete() {
  const node = contextMenu.value.node
  contextMenu.value.visible = false
  try {
    await ElMessageBox.confirm('确定将笔记移入回收站吗？', '提示', { type: 'warning' })
    await notes.softDeleteNote(node.raw.id)
    ElMessage.success('已移入回收站')
  } catch { /* cancelled */ }
}
</script>

<style scoped>
.note-tree {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
}

.tree-header-label {
  font-family: var(--font-ui);
  font-size: var(--text-ui-label);
  font-weight: 600;
  color: var(--on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.tree-header-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: var(--radius-default);
  background: none;
  color: var(--on-surface-variant);
  cursor: pointer;
  font-size: 16px;
  transition: background 0.15s;
}

.tree-header-action:hover {
  background: var(--surface-container);
}

.tree-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 4px;
}

.tree-body :deep(.el-tree) {
  background: transparent;
  --el-tree-node-hover-bg-color: var(--surface-container);
}

.tree-body :deep(.el-tree-node__content) {
  height: 28px;
  padding: 0 8px;
  border-radius: var(--radius-default);
}

.tree-body :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: var(--accent-soft);
  border-right: 2px solid var(--accent-sage);
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface);
  width: 100%;
}

.node-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.folder-icon { color: var(--accent-sage); }
.note-icon { color: var(--on-surface-variant); }

.node-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dirty-dot {
  color: var(--accent-sage);
  font-size: 18px;
  line-height: 1;
  flex-shrink: 0;
  margin-left: auto;
}

/* Context menu */
.context-menu {
  position: fixed;
  z-index: 200;
  background: var(--surface-container-lowest);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-elevated);
  padding: 4px;
  min-width: 160px;
}

.context-menu button {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  text-align: left;
  padding: 6px 12px;
  border: none;
  border-radius: var(--radius-default);
  background: none;
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface);
  cursor: pointer;
}

.context-menu button:hover {
  background: var(--surface-container);
}

.context-menu .material-symbols-outlined {
  font-size: 16px;
}

.context-menu button.danger {
  color: var(--error-red);
}

.context-menu button.danger:hover {
  background: var(--error-container);
}

.ctx-divider {
  height: 1px;
  background: var(--border-subtle);
  margin: 4px;
}

/* ---- Move dialog ---- */
.move-folder-list {
  max-height: 320px;
  overflow-y: auto;
}

.move-folder-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: var(--radius-default);
  cursor: pointer;
  transition: background 0.15s;
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface);
}

.move-folder-item:hover {
  background: var(--surface-container);
}

.move-folder-item .material-symbols-outlined {
  font-size: 18px;
  color: var(--secondary);
}

.move-empty {
  text-align: center;
  color: var(--outline);
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  padding: 20px;
}
</style>
