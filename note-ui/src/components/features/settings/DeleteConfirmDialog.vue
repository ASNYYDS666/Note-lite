<template>
  <Teleport to="body">
    <div v-if="open" class="delete-overlay" @click="$emit('cancel')">
      <div class="delete-dialog" role="alertdialog" aria-modal="true" @click.stop>
        <h4 class="dc-title">{{ title }}</h4>
        <p class="dc-desc">{{ description }}</p>
        <div class="dc-actions">
          <button class="dc-btn dc-btn-cancel" @click="$emit('cancel')">{{ cancelLabel }}</button>
          <button ref="confirmRef" class="dc-btn dc-btn-confirm" @click="$emit('confirm')">{{ confirmLabel }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '确认删除' },
  description: { type: String, default: '' },
  confirmLabel: { type: String, default: '删除' },
  cancelLabel: { type: String, default: '取消' }
})

defineEmits(['confirm', 'cancel'])

const confirmRef = ref(null)

watch(() => props.open, (val) => {
  if (val) {
    requestAnimationFrame(() => confirmRef.value?.focus())
  }
})
</script>

<style scoped>
.delete-overlay {
  position: fixed; inset: 0; z-index: 300;
  background: rgba(0,0,0,0.3);
  display: flex; align-items: center; justify-content: center;
}

.delete-dialog {
  background: var(--surface-container-lowest);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-xl);
  padding: 24px; max-width: 400px; width: 90%;
  box-shadow: var(--shadow-elevated);
}

.dc-title { margin: 0 0 8px; font-size: 16px; font-weight: 600; color: var(--on-surface); }
.dc-desc { margin: 0 0 20px; font-size: 13px; color: var(--on-surface-variant); line-height: 1.5; }

.dc-actions { display: flex; justify-content: flex-end; gap: 8px; }

.dc-btn {
  padding: 6px 16px; border: none; border-radius: var(--radius-default);
  font-family: var(--font-ui); font-size: var(--text-ui-sm); font-weight: 500;
  cursor: pointer;
}
.dc-btn-cancel { background: var(--surface-container); color: var(--on-surface); }
.dc-btn-cancel:hover { background: var(--surface-container-high); }
.dc-btn-confirm { background: var(--error-red); color: #fff; }
.dc-btn-confirm:hover { opacity: 0.9; }
</style>
