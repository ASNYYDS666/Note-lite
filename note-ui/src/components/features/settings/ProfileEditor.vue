<template>
  <div class="profile-editor">
    <div class="pe-scroll">
      <h3 class="pe-title">{{ isNew ? '新建 Profile' : '编辑 Profile' }}</h3>

      <!-- Banners -->
      <div v-if="refreshError" class="pe-banner is-error" role="alert">
        <span class="material-symbols-outlined">error</span>
        {{ refreshError }}
        <button class="pe-banner-close" @click="$emit('dismissError')">&times;</button>
      </div>
      <div v-if="notice" class="pe-banner is-notice" role="status">
        <span class="material-symbols-outlined">info</span>
        {{ notice }}
        <button class="pe-banner-close" @click="$emit('dismissNotice')">&times;</button>
      </div>

      <!-- Profile Name -->
      <div class="pe-section">
        <label class="pe-label">Profile Name</label>
        <input class="pe-input" :value="draft.profileName"
          placeholder="我的 DeepSeek" @input="emit('update', { profileName: $event.target.value })" />
      </div>

      <!-- Provider -->
      <div class="pe-section">
        <label class="pe-label">Provider</label>
        <select class="pe-select" :value="draft.providerKey"
          @change="onProviderChange($event.target.value)">
          <option value="" disabled>选择 AI 服务商...</option>
          <option v-for="p in providers" :key="p.key" :value="p.key">{{ p.name }}</option>
        </select>
      </div>

      <!-- Base URL -->
      <div class="pe-section">
        <label class="pe-label">Base URL</label>
        <input class="pe-input" :value="draft.baseUrl"
          :placeholder="selectedProviderBaseUrl || 'https://api.deepseek.com/v1'"
          @input="emit('update', { baseUrl: $event.target.value })" />
      </div>

      <!-- API Key -->
      <div class="pe-section">
        <label class="pe-label">API Key</label>
        <div class="pe-key-row">
          <input class="pe-input" :type="showKey ? 'text' : 'password'"
            :value="draft.apiKey" placeholder="sk-..."
            @input="emit('update', { apiKey: $event.target.value })" />
          <button class="pe-toggle-btn" @click="showKey = !showKey"
            :title="showKey ? '隐藏' : '显示'">
            <span class="material-symbols-outlined">{{ showKey ? 'visibility_off' : 'visibility' }}</span>
          </button>
        </div>
      </div>

      <!-- Models -->
      <div class="pe-section">
        <div class="pe-models-header">
          <label class="pe-label">Chat 模型</label>
          <button class="pe-refresh-btn" :disabled="refreshing" @click="$emit('refresh')">
            <span class="material-symbols-outlined" :class="{ spin: refreshing }">sync</span>
            {{ refreshing ? '刷新中...' : '刷新模型' }}
          </button>
        </div>

        <div v-if="draft.models.length === 0" class="pe-models-empty">
          暂无模型。请填写 Base URL 和 API Key，然后点击「刷新模型」从服务商拉取。
        </div>

        <div v-else class="pe-models-table">
          <div v-for="(m, i) in draft.models" :key="m.id" class="pe-model-row">
            <label class="pe-model-check">
              <input type="checkbox" :checked="m.enabled" @change="emit('updateModel', i, { enabled: $event.target.checked })" />
            </label>
            <span class="pe-model-id">{{ m.id }}</span>
            <button class="pe-model-del" @click="emit('removeModel', i)" title="移除">
              <span class="material-symbols-outlined">delete</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div class="pe-footer">
      <button v-if="!isNew" class="pe-delete-btn" @click="$emit('delete')">
        <span class="material-symbols-outlined">delete</span> 删除
      </button>
      <div class="pe-footer-spacer"></div>
      <span v-if="isDirty" class="pe-dirty-hint">
        <span class="pe-dirty-dot"></span> 未保存更改
      </span>
      <button class="pe-apply-btn" :class="{ 'is-emphasized': isDirty }" :disabled="!canApply" @click="$emit('apply')">
        应用
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  draft: { type: Object, required: true },
  providers: { type: Array, default: () => [] },
  isDirty: { type: Boolean, default: false },
  canApply: { type: Boolean, default: false },
  isNew: { type: Boolean, default: true },
  refreshing: { type: Boolean, default: false },
  refreshError: { type: String, default: null },
  notice: { type: String, default: null },
})

const emit = defineEmits([
  'update', 'updateModel', 'removeModel',
  'refresh', 'apply', 'delete',
  'dismissError', 'dismissNotice'
])

const showKey = ref(false)

const selectedProviderBaseUrl = computed(() => {
  const p = props.providers.find(p => p.key === props.draft.providerKey)
  return p ? p.baseUrl : ''
})

function onProviderChange(key) {
  emit('update', { providerKey: key })
  const p = props.providers.find(p => p.key === key)
  if (p && p.baseUrl && !props.draft.baseUrl) {
    emit('update', { baseUrl: p.baseUrl })
  }
}
</script>

<style scoped>
.profile-editor {
  flex: 1; display: flex; flex-direction: column; overflow: hidden;
}

.pe-scroll {
  flex: 1; overflow-y: auto; padding: 24px 28px;
}

.pe-title {
  font-size: 18px; font-weight: 600; color: var(--on-surface);
  margin: 0 0 16px;
}

/* banners */
.pe-banner {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 12px; border-radius: var(--radius-default);
  font-size: 12px; margin-bottom: 12px;
}
.pe-banner.is-error { background: var(--error-container); color: var(--on-error-container); }
.pe-banner.is-notice { background: var(--secondary-container); color: var(--on-secondary-container); }
.pe-banner .material-symbols-outlined { font-size: 16px; }
.pe-banner-close {
  margin-left: auto; border: none; background: none; cursor: pointer;
  font-size: 16px; color: inherit; padding: 0 4px;
}

/* form sections */
.pe-section { margin-bottom: 20px; }
.pe-label {
  display: block; font-size: 11px; font-weight: 600;
  text-transform: uppercase; letter-spacing: 0.06em;
  color: var(--on-surface-variant); margin-bottom: 6px;
}

.pe-input {
  width: 100%; padding: 8px 10px;
  border: 1px solid var(--border-default); border-radius: var(--radius-default);
  background: var(--bg); color: var(--on-surface);
  font-family: var(--font-ui); font-size: var(--text-ui-sm);
  outline: none; box-sizing: border-box;
}
.pe-input:focus { border-color: var(--accent-sage); }

.pe-select {
  width: 100%; padding: 8px 10px;
  border: 1px solid var(--border-default); border-radius: var(--radius-default);
  background: var(--bg); color: var(--on-surface);
  font-family: var(--font-ui); font-size: var(--text-ui-sm);
  outline: none; box-sizing: border-box; cursor: pointer;
}
.pe-select:focus { border-color: var(--accent-sage); }

.pe-key-row { display: flex; gap: 4px; }
.pe-key-row .pe-input { flex: 1; }

.pe-toggle-btn {
  width: 36px; height: 36px; display: flex; align-items: center; justify-content: center;
  border: 1px solid var(--border-default); border-radius: var(--radius-default);
  background: var(--bg); color: var(--on-surface-variant); cursor: pointer;
  flex-shrink: 0;
}
.pe-toggle-btn:hover { background: var(--surface-container); }
.pe-toggle-btn .material-symbols-outlined { font-size: 18px; }

/* models */
.pe-models-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;
}
.pe-models-header .pe-label { margin-bottom: 0; }

.pe-refresh-btn {
  display: flex; align-items: center; gap: 4px;
  padding: 4px 10px; border: 1px solid var(--border-default);
  border-radius: var(--radius-default); background: var(--bg);
  font-family: var(--font-ui); font-size: 11px; color: var(--accent-sage);
  cursor: pointer;
}
.pe-refresh-btn:hover:not(:disabled) { background: var(--surface-container); }
.pe-refresh-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.pe-refresh-btn .material-symbols-outlined { font-size: 14px; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.pe-models-empty {
  font-size: 12px; color: var(--on-surface-variant);
  padding: 16px; text-align: center; border: 1px dashed var(--border-default);
  border-radius: var(--radius-default);
}

.pe-models-table { display: flex; flex-direction: column; gap: 2px; }

.pe-model-row {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px; border-radius: var(--radius-default);
}
.pe-model-row:hover { background: var(--surface-container); }

.pe-model-check { display: flex; align-items: center; }
.pe-model-check input { accent-color: var(--accent-sage); cursor: pointer; }

.pe-model-id {
  flex: 1; font-size: var(--text-ui-sm); font-family: monospace;
  color: var(--on-surface);
}

.pe-model-del {
  width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;
  border: none; border-radius: var(--radius-default); background: none;
  color: var(--on-surface-variant); cursor: pointer;
  opacity: 0; transition: opacity 0.15s;
}
.pe-model-row:hover .pe-model-del { opacity: 1; }
.pe-model-del:hover { background: var(--error-container); color: var(--error-red); }
.pe-model-del .material-symbols-outlined { font-size: 14px; }

/* footer */
.pe-footer {
  display: flex; align-items: center; padding: 12px 28px;
  border-top: 1px solid var(--border-subtle); gap: 8px; flex-shrink: 0;
}

.pe-delete-btn {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 12px; border: none; border-radius: var(--radius-default);
  background: none; color: var(--error-red);
  font-family: var(--font-ui); font-size: var(--text-ui-sm); font-weight: 500;
  cursor: pointer;
}
.pe-delete-btn:hover { background: var(--error-container); }
.pe-delete-btn .material-symbols-outlined { font-size: 16px; }

.pe-footer-spacer { flex: 1; }

.pe-dirty-hint {
  display: flex; align-items: center; gap: 4px;
  font-size: 11px; color: var(--accent-sage);
}
.pe-dirty-dot {
  width: 6px; height: 6px; border-radius: 50%; background: var(--accent-sage);
}

.pe-apply-btn {
  padding: 6px 20px; border: none; border-radius: var(--radius-lg);
  background: var(--surface-container); color: var(--on-surface);
  font-family: var(--font-ui); font-size: var(--text-ui-sm); font-weight: 600;
  cursor: pointer; transition: background 0.15s, opacity 0.15s;
}
.pe-apply-btn.is-emphasized {
  background: var(--accent-sage); color: var(--on-secondary);
  box-shadow: 0 2px 8px rgba(107, 122, 106, 0.15);
}
.pe-apply-btn:hover:not(:disabled) { filter: brightness(1.1); }
.pe-apply-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
