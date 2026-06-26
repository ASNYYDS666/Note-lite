<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="bubble-toolbar"
      :style="{ top: pos.y + 'px', left: pos.x + 'px' }"
    >
      <button
        v-for="item in items"
        :key="item.active"
        class="bubble-btn"
        :class="{ 'is-active': isActive(item.active) }"
        @mousedown.prevent="toggleFormat(item.command)"
      >
        <span class="material-symbols-outlined">{{ item.icon }}</span>
      </button>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  editor: { type: Object, required: true }
})

const visible = ref(false)
const pos = ref({ x: 0, y: 0 })

const items = [
  { command: 'toggleBold', icon: 'format_bold', active: 'bold' },
  { command: 'toggleItalic', icon: 'format_italic', active: 'italic' },
  { command: 'toggleStrike', icon: 'strikethrough_s', active: 'strike' },
  { command: 'toggleCode', icon: 'code', active: 'code' }
]

function isActive(name) {
  return props.editor?.isActive(name) || false
}

function toggleFormat(command) {
  if (!props.editor) return
  props.editor.chain().focus()[command]().run()
}

function show(x, y) {
  pos.value = { x, y }
  visible.value = true
}

function hide() {
  visible.value = false
}

defineExpose({ show, hide })
</script>

<style scoped>
.bubble-toolbar {
  position: fixed;
  z-index: 200;
  display: flex;
  gap: 2px;
  padding: 4px;
  background: var(--surface-container-lowest);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-elevated);
  transform: translate(-50%, -120%);
}

.bubble-btn {
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
  transition: background 0.15s, color 0.15s;
}

.bubble-btn:hover {
  background: var(--surface-container);
  color: var(--on-surface);
}

.bubble-btn.is-active {
  background: var(--secondary-container);
  color: var(--on-secondary-container);
}

.bubble-btn .material-symbols-outlined {
  font-size: 16px;
}
</style>
