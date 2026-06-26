<template>
  <input
    ref="inputRef"
    class="inline-input"
    :defaultValue="defaultValue"
    autofocus
    @blur="confirmOrCancel"
    @keydown.enter.prevent="confirmOrCancel"
    @keydown.escape.prevent="$emit('cancel')"
    @click.stop
  />
</template>

<script setup>
import { ref, nextTick } from 'vue'

const props = defineProps({
  defaultValue: { type: String, default: '' }
})

const emit = defineEmits(['confirm', 'cancel'])

const inputRef = ref(null)

function confirmOrCancel() {
  const value = inputRef.value?.value?.trim() || ''
  if (value && value !== props.defaultValue) {
    emit('confirm', value)
  } else {
    emit('cancel')
  }
}

nextTick(() => {
  inputRef.value?.select()
})
</script>

<style scoped>
.inline-input {
  width: 100%;
  padding: 2px 6px;
  border: 1px solid var(--accent-sage);
  border-radius: var(--radius-default);
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface);
  background: var(--surface-container-lowest);
  outline: none;
  box-sizing: border-box;
}
</style>
