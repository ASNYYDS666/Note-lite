<template>
  <div class="editor-container">
    <FindReplaceBar
      v-if="findMode"
      :editor="editor"
      :mode="findMode"
      @close="findMode = null"
    />

    <div class="editor-toolbar">
      <button
        v-for="l in [1,2,3]"
        :key="'h'+l"
        class="toolbar-btn"
        :class="{ 'is-active': editor?.isActive('heading', { level: l }) }"
        @click="editor?.chain().focus().toggleHeading({ level: l }).run()"
      >H{{ l }}</button>
      <span class="toolbar-divider"></span>
      <button
        v-for="item in toolbarItems"
        :key="item.active"
        class="toolbar-btn"
        :class="{ 'is-active': editor?.isActive(item.active) }"
        @click="editor?.chain().focus()[item.command]().run()"
      >
        <span class="material-symbols-outlined">{{ item.icon }}</span>
      </button>
      <div class="toolbar-spacer"></div>
      <button class="save-btn" :disabled="notes.saving" @click="handleSave">
        {{ notes.saving ? 'Saving...' : 'Save' }}
      </button>
    </div>

    <div class="editor-surface custom-scrollbar" @contextmenu="onContextMenu">
      <div v-if="!currentNoteId && !workspace.isCreatingNew" class="editor-empty">
        <div class="empty-icon">
          <el-icon :size="48"><EditPen /></el-icon>
        </div>
        <p class="empty-title">打开一篇笔记开始编辑</p>
        <p class="empty-sub">点击左侧笔记列表，或点击 <kbd>+</kbd> 新建笔记</p>
      </div>
      <template v-else>
        <input
          class="editor-title"
          v-model="title"
          placeholder="Untitled Note"
          @input="markDirty"
        />
        <EditorContent :editor="editor" class="editor-content" />
      </template>
    </div>

    <EditorStatusBar :content="editorContent" />

    <EditorContextMenu ref="ctxMenuRef" :editor="editor" @openFind="findMode = 'find'" />
    <BubbleToolbar ref="bubbleRef" :editor="editor" />
  </div>
</template>

<script setup>
import { ref, watch, computed, nextTick, onBeforeUnmount, onMounted } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import { EditPen } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useNotesStore } from '@/store/notes'
import { useWorkspaceStore } from '@/store/workspace'
import FindReplaceBar from './FindReplaceBar.vue'
import EditorStatusBar from './EditorStatusBar.vue'
import EditorContextMenu from './EditorContextMenu.vue'
import BubbleToolbar from './BubbleToolbar.vue'

const notes = useNotesStore()
const workspace = useWorkspaceStore()
const currentNoteId = computed(() => workspace.currentNoteId)

const title = ref('')
const editorContent = ref('')
const dirty = ref(false)
const saveTimer = ref(null)
const findMode = ref(null)

const ctxMenuRef = ref(null)
const bubbleRef = ref(null)

const toolbarItems = [
  { command: 'toggleBold',        active: 'bold',        icon: 'format_bold' },
  { command: 'toggleItalic',      active: 'italic',       icon: 'format_italic' },
  { command: 'toggleStrike',      active: 'strike',       icon: 'strikethrough_s' },
  { command: 'toggleCodeBlock',   active: 'codeBlock',    icon: 'code' },
  { command: 'toggleBlockquote',  active: 'blockquote',   icon: 'format_quote' },
  { command: 'toggleBulletList',  active: 'bulletList',   icon: 'format_list_bulleted' },
  { command: 'toggleOrderedList', active: 'orderedList',  icon: 'format_list_numbered' },
]

const editor = useEditor({
  extensions: [
    StarterKit,
    Placeholder.configure({ placeholder: 'Start writing...' }),
  ],
  content: '',
  editable: true,
  onUpdate: ({ editor: ed }) => {
    editorContent.value = ed.getHTML()
    notes.setWordCount(countWords(ed.getText()))
    markDirty()
  },
})

watch(currentNoteId, async (id) => {
  if (id) {
    await notes.loadNote(id)
    const note = notes.currentNote
    if (note && editor.value) {
      title.value = note.title || ''
      editor.value.commands.setContent(note.content || '')
      editorContent.value = note.content || ''
      dirty.value = false
      notes.setWordCount(countWords(note.content || ''))
    }
  } else if (workspace.isCreatingNew) {
    title.value = ''
    editor.value?.commands.setContent('')
    editorContent.value = ''
    dirty.value = false
  }
}, { immediate: true })

function onContextMenu(e) {
  ctxMenuRef.value?.show(e.clientX, e.clientY)
}

function markDirty() {
  dirty.value = true
  if (saveTimer.value) clearTimeout(saveTimer.value)
  saveTimer.value = setTimeout(() => saveDraft(), 3000)
}

async function saveDraft() {
  if (dirty.value) {
    try {
      await notes.saveDraft(currentNoteId.value, editorContent.value)
    } catch { /* silent */ }
  }
}

async function handleSave() {
  if (!title.value.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  const content = editorContent.value
  const noteId = currentNoteId.value

  try {
    if (noteId) {
      await notes.updateNote({
        id: noteId,
        title: title.value.trim(),
        content,
        folderId: workspace.selectedFolderId,
        tags: []
      })
    } else {
      const newId = await notes.createNote({
        title: title.value.trim(),
        content,
        folderId: workspace.selectedFolderId,
        tags: []
      })
      workspace.currentNoteId = newId
      workspace.isCreatingNew = false
    }
    dirty.value = false
    notes.clearDraft(noteId).catch(() => {})
    ElMessage.success('已保存')
    if (saveTimer.value) clearTimeout(saveTimer.value)
  } catch {
    ElMessage.error('保存失败')
  }
}

function countWords(text) {
  return text.trim() ? text.trim().split(/\s+/).length : 0
}

// keyboard shortcuts for find/replace
function onKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
    e.preventDefault()
    findMode.value = 'find'
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'h') {
    e.preventDefault()
    findMode.value = 'replace'
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  if (saveTimer.value) clearTimeout(saveTimer.value)
  saveDraft()
})
</script>

<style scoped>
.editor-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}

/* ---- Toolbar ---- */
.editor-toolbar {
  height: 40px;
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  padding: 0 8px;
  gap: 2px;
  overflow-x: auto;
  background: var(--surface-container-lowest);
  flex-shrink: 0;
}

.toolbar-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--radius-default);
  background: none;
  color: var(--on-surface-variant);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  font-family: var(--font-ui);
  transition: background 0.15s, color 0.15s;
}

.toolbar-btn:hover {
  background: var(--surface-container);
  color: var(--on-surface);
}

.toolbar-btn.is-active {
  background: var(--secondary-container);
  color: var(--on-secondary-container);
}

.toolbar-btn .material-symbols-outlined {
  font-size: 16px;
}

.toolbar-divider {
  width: 1px;
  height: 16px;
  background: var(--outline-variant);
  margin: 0 4px;
}

.toolbar-spacer {
  flex: 1;
}

.save-btn {
  background: var(--primary);
  color: var(--on-primary);
  border: none;
  border-radius: var(--radius-default);
  padding: 4px 16px;
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s;
}

.save-btn:hover { opacity: 0.9; }
.save-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ---- Editor surface ---- */
.editor-surface {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
}

.editor-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon { color: var(--outline-variant); margin-bottom: 20px; }

.empty-title {
  font-family: var(--font-editor);
  font-size: 17px;
  color: var(--on-surface-variant);
  margin: 0 0 8px;
}

.empty-sub {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--outline);
  margin: 0;
}

.empty-sub kbd {
  background: var(--surface-container);
  padding: 1px 5px;
  border-radius: 3px;
  font-size: var(--text-ui-sm);
}

/* ---- Editor title ---- */
.editor-title {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  font-family: var(--font-editor);
  font-size: var(--text-headline-lg);
  font-weight: 700;
  color: var(--primary);
  padding: 0;
  margin-bottom: 16px;
  line-height: 1.2;
}

.editor-title::placeholder { color: var(--outline-variant); }

/* ---- TipTap EditorContent ---- */
.editor-content :deep(.tiptap) {
  font-family: var(--font-editor);
  font-size: 16px;
  line-height: 1.6;
  color: var(--on-surface);
  outline: none;
  min-height: 300px;
}

.editor-content :deep(.tiptap p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  color: var(--outline-variant);
  pointer-events: none;
  height: 0;
}

.editor-content :deep(.tiptap h1) {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary);
  margin: 24px 0 8px;
  line-height: 1.3;
}

.editor-content :deep(.tiptap h2) {
  font-size: 22px;
  font-weight: 600;
  color: var(--primary);
  margin: 20px 0 6px;
  line-height: 1.3;
}

.editor-content :deep(.tiptap h3) {
  font-size: 18px;
  font-weight: 600;
  color: var(--primary);
  margin: 16px 0 4px;
}

.editor-content :deep(.tiptap blockquote) {
  border-left: 3px solid var(--accent-sage);
  padding-left: 16px;
  font-style: italic;
  color: var(--on-surface-variant);
  margin: 12px 0;
}

.editor-content :deep(.tiptap pre) {
  background: var(--surface-container);
  color: var(--on-surface);
  padding: 12px 16px;
  border-radius: var(--radius-default);
  margin: 12px 0;
  overflow-x: auto;
  font-family: 'JetBrains Mono', monospace;
  font-size: 14px;
}

.editor-content :deep(.tiptap code) {
  font-family: monospace;
  background: var(--surface-container);
  padding: 2px 6px;
  border-radius: 2px;
  font-size: 0.9em;
}

.editor-content :deep(.tiptap ul),
.editor-content :deep(.tiptap ol) {
  padding-left: 24px;
}

.editor-content :deep(.tiptap li) {
  margin: 4px 0;
}

.editor-content :deep(.tiptap p) {
  margin: 8px 0;
}
</style>
