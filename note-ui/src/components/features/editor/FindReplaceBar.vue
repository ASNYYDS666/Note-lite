<template>
  <div class="find-replace-bar">
    <div class="fr-row">
      <input
        ref="findInput"
        v-model="query"
        class="fr-input"
        placeholder="Find..."
        @input="onQueryChange"
        @keydown.escape="$emit('close')"
        @keydown.enter.prevent="goNext"
        @keydown.shift.enter.prevent="goPrev"
      />
      <span class="fr-count" aria-live="polite">{{ matchCount ? `${activeIndex + 1}/${matchCount}` : '0/0' }}</span>
      <button class="fr-btn" :class="{ 'is-active': caseSensitive }" title="Case sensitive" @click="caseSensitive = !caseSensitive; onQueryChange()">Aa</button>
      <button class="fr-btn" title="Previous" @click="goPrev"><span class="material-symbols-outlined">arrow_upward</span></button>
      <button class="fr-btn" title="Next" @click="goNext"><span class="material-symbols-outlined">arrow_downward</span></button>
      <button class="fr-btn" title="Close" @click="$emit('close')"><span class="material-symbols-outlined">close</span></button>
    </div>
    <div v-if="showReplace" class="fr-row">
      <input
        v-model="replacement"
        class="fr-input"
        placeholder="Replace..."
        @keydown.escape="$emit('close')"
        @keydown.enter.prevent="replaceOne"
      />
      <button class="fr-btn fr-btn-action" @click="replaceOne">Replace</button>
      <button class="fr-btn fr-btn-action" @click="replaceAll">All</button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'

const emit = defineEmits(['close'])

const props = defineProps({
  editor: { type: Object, required: true },
  mode: { type: String, default: 'find' } // 'find' | 'replace'
})

const query = ref('')
const replacement = ref('')
const caseSensitive = ref(false)
const activeIndex = ref(0)
const matches = ref([])
const findInput = ref(null)
const showReplace = ref(props.mode === 'replace')

const matchCount = ref(0)

nextTick(() => {
  findInput.value?.focus()
})

function onQueryChange() {
  if (!query.value) {
    matches.value = []
    matchCount.value = 0
    activeIndex.value = 0
    return
  }
  try {
    const text = props.editor.getText()
    const flags = caseSensitive.value ? 'g' : 'gi'
    const escaped = query.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const re = new RegExp(escaped, flags)
    const result = []
    let m
    while ((m = re.exec(text)) !== null) {
      result.push({ start: m.index, end: m.index + m[0].length })
      if (m.index === re.lastIndex) re.lastIndex++
    }
    matches.value = result
    matchCount.value = result.length
    activeIndex.value = result.length > 0 ? 0 : -1
    if (result.length > 0) highlightMatch(0)
  } catch { /* invalid regex */ }
}

function highlightMatch(idx) {
  if (idx < 0 || idx >= matches.value.length) return
  const m = matches.value[idx]
  props.editor.chain().setTextSelection({ from: m.start, to: m.end }).scrollIntoView().run()
}

function goNext() {
  if (matches.value.length === 0) return
  const next = (activeIndex.value + 1) % matches.value.length
  activeIndex.value = next
  highlightMatch(next)
}

function goPrev() {
  if (matches.value.length === 0) return
  const prev = (activeIndex.value - 1 + matches.value.length) % matches.value.length
  activeIndex.value = prev
  highlightMatch(prev)
}

function replaceOne() {
  if (matches.value.length === 0) return
  const m = matches.value[activeIndex.value]
  props.editor.chain().setTextSelection({ from: m.start, to: m.end }).deleteSelection().insertContent(replacement.value).run()
  onQueryChange()
}

function replaceAll() {
  for (let i = matches.value.length - 1; i >= 0; i--) {
    const m = matches.value[i]
    props.editor.chain().setTextSelection({ from: m.start, to: m.end }).deleteSelection().insertContent(replacement.value).run()
  }
  onQueryChange()
}
</script>

<style scoped>
.find-replace-bar {
  padding: 6px 8px;
  background: var(--surface-container-lowest);
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.fr-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.fr-input {
  flex: 1;
  padding: 4px 8px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-default);
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  background: var(--bg);
  color: var(--on-surface);
  outline: none;
  min-width: 120px;
}

.fr-input:focus {
  border-color: var(--accent-sage);
}

.fr-count {
  font-family: var(--font-ui);
  font-size: 11px;
  color: var(--on-surface-variant);
  min-width: 32px;
  text-align: center;
}

.fr-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--radius-default);
  background: none;
  color: var(--on-surface-variant);
  cursor: pointer;
  font-family: var(--font-ui);
  font-size: 11px;
  font-weight: 600;
}

.fr-btn:hover {
  background: var(--surface-container);
}

.fr-btn.is-active {
  background: var(--secondary-container);
  color: var(--on-secondary-container);
}

.fr-btn .material-symbols-outlined {
  font-size: 14px;
}

.fr-btn-action {
  width: auto;
  padding: 2px 8px;
  font-size: var(--text-ui-sm);
}
</style>
