<template>
  <div class="embedding-panel">
    <div class="ep-header">
      <div class="ep-header-left">
        <span class="material-symbols-outlined ep-icon">database</span>
        <div>
          <h3 class="ep-title">Embedding 向量化</h3>
          <p class="ep-desc">笔记内容转为向量存入知识库，供 AI 检索使用</p>
        </div>
      </div>
      <div class="ep-status" :class="{ on: enabled }">
        <span class="ep-dot"></span>
        {{ enabled ? '已启用' : '未启用' }}
      </div>
    </div>

    <!-- 启用开关 + 确认 -->
    <div class="ep-toggle-section">
      <label class="ep-switch" @click="toggleEnabled">
        <span class="ep-switch-track" :class="{ on: pendingEnabled }">
          <span class="ep-switch-thumb"></span>
        </span>
        <span class="ep-switch-label">启用本地 Embedding 服务</span>
      </label>

      <button
        v-if="enabled !== pendingEnabled"
        class="ep-confirm-btn"
        @click="confirmToggle"
      >
        {{ pendingEnabled ? '确认启用' : '确认关闭' }}
      </button>
    </div>

    <!-- 启用后的配置 -->
    <div v-if="enabled" class="ep-body">
      <div class="ep-field">
        <label class="ep-label">Embedding 服务商</label>
        <select class="ep-select" v-model="selectedProvider" @change="onProviderChange">
          <option value="">选择服务商...</option>
          <option v-for="p in providers" :key="p.key" :value="p.key">
            {{ p.name }}{{ p.isLocal ? ' (本地)' : '' }}
          </option>
        </select>
      </div>

      <!-- 模型信息 -->
      <div v-if="selectedModel" class="ep-model-card">
        <span class="material-symbols-outlined">check_circle</span>
        <span class="ep-model-name">{{ selectedModel }}</span>
        <span class="ep-dim-badge">512 维</span>
      </div>

      <!-- 未找到本地模型时的提示 -->
      <div v-if="providers.length === 0" class="ep-empty-hint">
        <span class="material-symbols-outlined">info</span>
        未检测到本地 Embedding 模型。请确保已安装并启动 embedding_server.py。
      </div>

      <!-- 测试连接 -->
      <div class="ep-actions">
        <button class="ep-test-btn" :disabled="testing || !selectedProvider" @click="testConnection">
          <span class="material-symbols-outlined" :class="{ spin: testing }">
            {{ testing ? 'sync' : 'network_check' }}
          </span>
          {{ testing ? '测试中...' : '测试连接' }}
        </button>
      </div>

      <div v-if="testResult" class="ep-result" :class="testResult.success ? 'ok' : 'fail'">
        <template v-if="testResult.success">
          连接成功 | 维度: {{ testResult.dimension }} | 延迟: {{ testResult.latencyMs }}ms
        </template>
        <template v-else>
          连接失败: {{ testResult.error }}
        </template>
      </div>

      <button v-if="dirty" class="ep-save-btn" @click="save">
        保存配置
      </button>

      <div v-if="saveFeedback" class="ep-feedback" :class="saveFeedback.type">
        {{ saveFeedback.text }}
      </div>
    </div>

    <!-- 未启用时的提示 -->
    <div v-else class="ep-disabled-hint">
      <span class="material-symbols-outlined">lightbulb</span>
      开启后将使用本地 Embedding 模型处理笔记向量化，Chat 模型不受影响。
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  getEmbeddingConfig, saveEmbeddingConfig,
  getEmbeddingProviders, testEmbeddingConnection
} from '@/api/aiConfig'

const enabled = ref(false)
const pendingEnabled = ref(false)
const selectedProvider = ref('')
const selectedModel = ref('')
const providers = ref([])
const testing = ref(false)
const testResult = ref(null)
const dirty = ref(false)
const loaded = ref(false)
const saveFeedback = ref(null)

const currentConfig = ref({})

onMounted(async () => {
  try {
    const [config, provs] = await Promise.all([
      getEmbeddingConfig(),
      getEmbeddingProviders()
    ])
    currentConfig.value = config || {}
    providers.value = provs || []

    if (config && config.embedProvider) {
      enabled.value = true
      pendingEnabled.value = true
      selectedProvider.value = config.embedProvider
      selectedModel.value = config.embedModel || ''
    } else {
      enabled.value = false
      pendingEnabled.value = false
      // 默认选中 local-embedding
      const local = (provs || []).find(p => p.key === 'local-embedding')
      if (local) {
        selectedProvider.value = 'local-embedding'
        const def = (local.models || []).find(m => m.isDefault)
        selectedModel.value = def ? def.modelName : (local.models[0] || {}).modelName || ''
      }
    }
  } catch { /* ignore */ }
  loaded.value = true
})

function toggleEnabled() {
  pendingEnabled.value = !pendingEnabled.value
}

function confirmToggle() {
  const wasEnabled = enabled.value
  enabled.value = pendingEnabled.value

  if (enabled.value && !wasEnabled) {
    // 启用时，默认选 local-embedding
    const local = providers.value.find(p => p.key === 'local-embedding')
    if (local) {
      selectedProvider.value = 'local-embedding'
      const def = (local.models || []).find(m => m.isDefault)
      selectedModel.value = def ? def.modelName : (local.models[0] || {}).modelName || ''
    }
    testResult.value = null
  } else if (!enabled.value && wasEnabled) {
    selectedProvider.value = ''
    selectedModel.value = ''
    testResult.value = null
  }
  markDirty()
}

function onProviderChange() {
  const p = providers.value.find(p => p.key === selectedProvider.value)
  if (p && p.models && p.models.length > 0) {
    const def = p.models.find(m => m.isDefault)
    selectedModel.value = def ? def.modelName : p.models[0].modelName
  } else {
    selectedModel.value = ''
  }
  testResult.value = null
  markDirty()
}

async function testConnection() {
  if (!selectedProvider.value) return
  testing.value = true
  testResult.value = null
  try {
    const res = await testEmbeddingConnection(selectedProvider.value)
    testResult.value = { success: true, ...res }
  } catch (e) {
    testResult.value = { success: false, error: e.message || '连接失败' }
  } finally {
    testing.value = false
  }
}

async function save() {
  saveFeedback.value = null
  try {
    await saveEmbeddingConfig({
      embedProvider: enabled.value ? selectedProvider.value : '',
      embedModel: enabled.value ? selectedModel.value : '',
      embedUrl: ''
    })
    currentConfig.value = {
      embedProvider: enabled.value ? selectedProvider.value : '',
      embedModel: enabled.value ? selectedModel.value : '',
      embedUrl: ''
    }
    dirty.value = false
    saveFeedback.value = { type: 'ok', text: '配置已保存' }
    setTimeout(() => { saveFeedback.value = null }, 3000)
  } catch {
    saveFeedback.value = { type: 'fail', text: '保存失败，请重试' }
  }
}

function markDirty() {
  if (!loaded.value) return
  const cur = currentConfig.value
  dirty.value =
    (enabled.value ? selectedProvider.value : '') !== (cur.embedProvider || '') ||
    (enabled.value ? selectedModel.value : '') !== (cur.embedModel || '')
}
</script>

<style scoped>
.embedding-panel {
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  background: var(--surface-container-lowest);
  padding: 20px 24px;
}

/* header */
.ep-header {
  display: flex; align-items: flex-start; justify-content: space-between;
  margin-bottom: 18px;
}
.ep-header-left { display: flex; align-items: flex-start; gap: 10px; }
.ep-icon { font-size: 22px; color: var(--accent-primary); margin-top: 2px; }
.ep-title { font-size: 15px; font-weight: 600; color: var(--on-surface); margin: 0 0 2px; }
.ep-desc { font-size: 12px; color: var(--on-surface-variant); margin: 0; }

/* status badge */
.ep-status {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; font-weight: 600; color: var(--on-surface-variant);
  padding: 4px 12px; border-radius: 20px; background: var(--surface-container);
  white-space: nowrap; flex-shrink: 0;
}
.ep-status.on { background: #e8f5e9; color: #2e7d32; }
.ep-dot {
  width: 8px; height: 8px; border-radius: 50%; background: #9e9e9e;
}
.ep-status.on .ep-dot { background: #2e7d32; }

/* toggle section */
.ep-toggle-section {
  display: flex; align-items: center; gap: 12px; margin-bottom: 16px;
  padding: 12px 16px; border-radius: 8px; background: var(--surface-container);
}

.ep-switch {
  display: flex; align-items: center; gap: 10px; cursor: pointer; user-select: none;
  flex: 1;
}
.ep-switch-track {
  width: 44px; height: 24px; border-radius: 12px;
  background: #bdbdbd; position: relative; transition: background 0.2s;
  flex-shrink: 0;
}
.ep-switch-track.on { background: var(--accent-primary); }
.ep-switch-thumb {
  width: 20px; height: 20px; border-radius: 50%; background: #fff;
  position: absolute; top: 2px; left: 2px;
  transition: transform 0.2s; box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}
.ep-switch-track.on .ep-switch-thumb { transform: translateX(20px); }
.ep-switch-label { font-size: 13px; font-weight: 500; color: var(--on-surface); }

.ep-confirm-btn {
  padding: 6px 16px; border: none; border-radius: 6px;
  font-family: var(--font-ui); font-size: 12px; font-weight: 600;
  cursor: pointer; white-space: nowrap;
  background: var(--accent-primary); color: #fff;
  animation: pulse 0.6s ease-in-out;
}
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(107, 122, 106, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(107, 122, 106, 0); }
}

/* body */
.ep-body { margin-top: 4px; }

.ep-field { margin-bottom: 10px; }
.ep-label {
  display: block; font-size: 11px; font-weight: 600;
  text-transform: uppercase; letter-spacing: 0.04em;
  color: var(--on-surface-variant); margin-bottom: 4px;
}

.ep-select {
  width: 100%; padding: 8px 10px;
  border: 1px solid var(--border-default); border-radius: 8px;
  background: var(--bg); color: var(--on-surface);
  font-family: var(--font-ui); font-size: var(--text-ui-sm);
  outline: none; box-sizing: border-box; cursor: pointer;
}
.ep-select:focus { border-color: var(--accent-primary); }

.ep-model-card {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 12px; margin-bottom: 10px;
  border-radius: 8px; background: var(--surface-container);
  font-size: var(--text-ui-sm);
}
.ep-model-card .material-symbols-outlined { font-size: 18px; color: #2e7d32; }
.ep-model-name { font-family: monospace; font-weight: 600; color: var(--on-surface); }
.ep-dim-badge {
  font-size: 10px; padding: 1px 6px; border-radius: 10px;
  background: var(--bg); color: var(--on-surface-variant);
  margin-left: auto;
}

.ep-actions { margin-bottom: 8px; }

.ep-test-btn {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 14px; border: 1px solid var(--border-default);
  border-radius: 8px; background: var(--bg);
  font-family: var(--font-ui); font-size: var(--text-ui-sm);
  color: var(--accent-primary); cursor: pointer;
}
.ep-test-btn:hover:not(:disabled) { background: var(--surface-container); }
.ep-test-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ep-test-btn .material-symbols-outlined { font-size: 16px; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.ep-result { margin-top: 8px; padding: 8px 12px; border-radius: 8px; font-size: 12px; }
.ep-result.ok { background: #e8f5e9; color: #2e7d32; }
.ep-result.fail { background: #ffebee; color: #c62828; }

.ep-save-btn {
  margin-top: 12px; padding: 8px 24px; border: none; border-radius: 8px;
  background: var(--accent-primary); color: white;
  font-family: var(--font-ui); font-size: var(--text-ui-sm); font-weight: 600;
  cursor: pointer; animation: pulse 0.6s ease-in-out;
}
.ep-save-btn:hover { filter: brightness(1.1); }

.ep-feedback { margin-top: 8px; padding: 8px 12px; border-radius: 8px; font-size: 12px; }
.ep-feedback.ok { background: #e8f5e9; color: #2e7d32; }
.ep-feedback.fail { background: #ffebee; color: #c62828; }

/* hints */
.ep-empty-hint {
  display: flex; align-items: center; gap: 6px; margin-top: 8px;
  padding: 10px 12px; border-radius: 8px; background: #fff3e0;
  font-size: 12px; color: #e65100;
}
.ep-empty-hint .material-symbols-outlined { font-size: 16px; }

.ep-disabled-hint {
  display: flex; align-items: center; gap: 8px; margin-top: 4px;
  padding: 12px 16px; border-radius: 8px;
  border: 1px dashed var(--border-default);
  font-size: 13px; color: var(--on-surface-variant);
}
.ep-disabled-hint .material-symbols-outlined { font-size: 18px; color: var(--accent-primary); }
</style>
