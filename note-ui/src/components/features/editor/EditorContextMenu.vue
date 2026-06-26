<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="editor-context-menu"
      :style="{ top: pos.y + 'px', left: pos.x + 'px' }"
      @click.stop
    >
      <button @click="exec('cut')" :disabled="!hasSelection">
        <span class="material-symbols-outlined">content_cut</span> Cut
      </button>
      <button @click="exec('copy')" :disabled="!hasSelection">
        <span class="material-symbols-outlined">content_copy</span> Copy
      </button>
      <button @click="exec('paste')">
        <span class="material-symbols-outlined">content_paste</span> Paste
      </button>
      <button @click="selectAll">
        <span class="material-symbols-outlined">select_all</span> Select All
      </button>
      <div class="ctx-divider"></div>
      <button @click="toggleFormat('bold')">
        <span class="material-symbols-outlined">format_bold</span> Bold
      </button>
      <button @click="toggleFormat('italic')">
        <span class="material-symbols-outlined">format_italic</span> Italic
      </button>
      <button @click="toggleFormat('strike')">
        <span class="material-symbols-outlined">strikethrough_s</span> Strikethrough
      </button>
      <div class="ctx-divider"></div>
      <button v-for="l in [1,2,3]" :key="'h'+l" @click="toggleHeading(l)">
        Heading {{ l }}
      </button>
      <div class="ctx-divider"></div>
      <button @click="toggleFormat('bulletList')">
        <span class="material-symbols-outlined">format_list_bulleted</span> Bullet List
      </button>
      <button @click="toggleFormat('orderedList')">
        <span class="material-symbols-outlined">format_list_numbered</span> Ordered List
      </button>
      <button @click="toggleFormat('blockquote')">
        <span class="material-symbols-outlined">format_quote</span> Quote
      </button>
      <div class="ctx-divider"></div>
      <button @click="$emit('openFind')">
        <span class="material-symbols-outlined">find_replace</span> Find & Replace
      </button>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  editor: { type: Object, required: true }
})

const emit = defineEmits(['openFind'])

const visible = ref(false)
const pos = ref({ x: 0, y: 0 })
const hasSelection = ref(false)

function show(x, y) {
  pos.value = { x, y }
  hasSelection.value = !props.editor?.state?.selection?.empty
  visible.value = true
}

function hide() {
  visible.value = false
}

function toggleFormat(name) {
  props.editor?.chain().focus()['toggle' + name.charAt(0).toUpperCase() + name.slice(1)]().run()
  hide()
}

function toggleHeading(level) {
  props.editor?.chain().focus().toggleHeading({ level }).run()
  hide()
}

function exec(cmd) {
  document.execCommand(cmd)
  hide()
}

function selectAll() {
  props.editor?.chain().focus().selectAll().run()
  hide()
}

defineExpose({ show, hide })
</script>

<style scoped>
.editor-context-menu {
  position: fixed;
  z-index: 200;
  background: var(--surface-container-lowest);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-elevated);
  padding: 4px;
  min-width: 180px;
}

.editor-context-menu button {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  text-align: left;
  padding: 6px 10px;
  border: none;
  border-radius: var(--radius-default);
  background: none;
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface);
  cursor: pointer;
}

.editor-context-menu button:hover {
  background: var(--surface-container);
}

.editor-context-menu button:disabled {
  opacity: 0.4;
  cursor: default;
}

.editor-context-menu .material-symbols-outlined {
  font-size: 16px;
}

.ctx-divider {
  height: 1px;
  background: var(--border-subtle);
  margin: 4px;
}
</style>
