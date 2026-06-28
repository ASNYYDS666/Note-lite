<template>
  <div class="profile-list">
    <div class="pl-header">
      <span class="pl-title">Profiles</span>
      <button class="pl-add-btn" @click="$emit('add')" title="新建 Profile">
        <span class="material-symbols-outlined">add</span>
      </button>
    </div>
    <div class="pl-items">
      <button
        v-for="p in profiles"
        :key="p.id"
        class="pl-row"
        :class="{ 'is-active': p.id === activeId }"
        @click="$emit('select', p)"
      >
        <span class="material-symbols-outlined pl-icon">bolt</span>
        <div class="pl-info">
          <span class="pl-name">{{ p.profileName }}</span>
          <span class="pl-meta">{{ modelSummary(p) }}</span>
        </div>
        <span class="pl-badge">{{ enabledCount(p) }}</span>
        <label class="pl-enable-toggle" @click.stop>
          <input type="checkbox" :checked="enabled(p.id)" @change="toggleEnable(p.id)" />
          <span class="pl-toggle-slider"></span>
        </label>
      </button>
      <div v-if="profiles.length === 0" class="pl-empty">
        暂无 Profile，点击 + 新建
      </div>
    </div>
  </div>
</template>

<script setup>
import { useAIStore } from '@/store/ai'

defineProps({
  profiles: { type: Array, default: () => [] },
  activeId: { type: [String, Number], default: null }
})

defineEmits(['add', 'select'])

const ai = useAIStore()

function enabled(profileId) {
  return ai.enabledProfileIds.includes(profileId)
}

function toggleEnable(profileId) {
  ai.toggleProfileEnabled(profileId)
}

function parseModels(p) {
  if (!p.enabledModels) return []
  try { return typeof p.enabledModels === 'string' ? JSON.parse(p.enabledModels) : p.enabledModels }
  catch { return [] }
}

function enabledCount(p) {
  return parseModels(p).length
}

function modelSummary(p) {
  const models = parseModels(p)
  if (!models.length) return '无模型'
  return models.slice(0, 3).map(m => typeof m === 'string' ? m : m.id).join(', ')
}
</script>

<style scoped>
.profile-list {
  width: 220px;
  padding: 16px;
  border-right: 1px solid var(--border-subtle);
  overflow-y: auto;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pl-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pl-title {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--on-surface-variant);
}

.pl-add-btn {
  width: 24px; height: 24px;
  display: flex; align-items: center; justify-content: center;
  border: none; border-radius: var(--radius-default);
  background: none; color: var(--on-surface-variant);
  cursor: pointer;
}
.pl-add-btn:hover { background: var(--surface-container); }
.pl-add-btn .material-symbols-outlined { font-size: 16px; }

.pl-items { display: flex; flex-direction: column; gap: 4px; }

.pl-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-radius: var(--radius-default);
  border: 1px solid transparent; background: none;
  cursor: pointer; text-align: left; width: 100%;
  transition: background 0.15s, border-color 0.15s;
}
.pl-row:hover { background: var(--surface-container); }
.pl-row.is-active {
  background: var(--surface-container);
  border-color: var(--accent-sage);
}

.pl-icon { font-size: 18px; color: var(--accent-sage); flex-shrink: 0; }

.pl-info { display: flex; flex-direction: column; min-width: 0; flex: 1; }
.pl-name { font-size: var(--text-ui-sm); font-weight: 600; color: var(--on-surface); }
.pl-meta { font-size: 10px; color: var(--on-surface-variant); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.pl-badge {
  font-size: 10px; font-weight: 600; min-width: 18px; height: 18px;
  display: flex; align-items: center; justify-content: center;
  background: var(--secondary-container); color: var(--on-secondary-container);
  border-radius: 9px; padding: 0 5px;
}

.pl-empty {
  font-size: 11px; color: var(--outline); text-align: center; padding: 20px 0;
}

.pl-enable-toggle {
  position: relative; display: inline-block;
  width: 36px; height: 20px; flex-shrink: 0; margin-left: 6px;
}

.pl-enable-toggle input { opacity: 0; width: 0; height: 0; }

.pl-toggle-slider {
  position: absolute; cursor: pointer;
  top: 0; left: 0; right: 0; bottom: 0;
  background: var(--surface-container-high);
  border-radius: 20px; transition: background 0.2s;
  border: 1px solid var(--outline-variant);
}

.pl-toggle-slider::before {
  content: ''; position: absolute;
  height: 14px; width: 14px; left: 2px; bottom: 2px;
  background: var(--on-surface-variant);
  border-radius: 50%; transition: transform 0.2s, background 0.2s;
}

.pl-enable-toggle input:checked + .pl-toggle-slider {
  background: var(--secondary-container);
  border-color: var(--secondary);
}

.pl-enable-toggle input:checked + .pl-toggle-slider::before {
  transform: translateX(16px);
  background: var(--secondary);
}
</style>
