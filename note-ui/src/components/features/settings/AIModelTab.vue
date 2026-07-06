<template>
  <div class="ai-model-layout">
    <!-- Left: Profile List -->
    <ProfileList
      :profiles="lib.profiles.value"
      :activeId="lib.draft.value.id"
      @add="lib.startNew()"
      @select="lib.selectProfile($event)"
    />

    <!-- Right -->
    <div class="ai-model-right">
      <!-- Chat Profile 区域 -->
      <div class="chat-section">
        <template v-if="lib.profiles.value.length === 0 && lib.editorMode.value === 'idle'">
          <ProfileEmptyState variant="no-profiles" @add="lib.startNew()" />
        </template>
        <template v-else-if="lib.editorMode.value === 'creating' || lib.editorMode.value === 'editing'">
          <ProfileEditor
            :draft="lib.draft.value"
            :providers="lib.providers.value"
            :isDirty="lib.isDirty.value"
            :canApply="lib.canApply.value"
            :isNew="lib.editorMode.value === 'creating'"
            :refreshing="lib.refreshing.value"
            :refreshError="lib.refreshError.value"
            :notice="lib.notice.value"
            @update="(patch) => { Object.assign(lib.draft.value, patch) }"
            @updateModel="(i, v) => { Object.assign(lib.draft.value.models[i], v) }"
            @removeModel="(i) => { lib.draft.value.models.splice(i, 1) }"
            @refresh="lib.refreshRemoteModels()"
            @apply="lib.applyDraft()"
            @delete="lib.requestDelete(lib.draft.value.id)"
            @dismissError="lib.refreshError.value = null"
            @dismissNotice="lib.dismissNotice()"
          />
        </template>
        <template v-else>
          <ProfileEmptyState variant="no-selection" />
        </template>
      </div>

      <!-- Embedding 独立配置区域 -->
      <div class="embedding-section">
        <EmbeddingConfig />
      </div>
    </div>

    <!-- Delete confirmation -->
    <DeleteConfirmDialog
      :open="lib.pendingDeleteId.value !== null"
      title="确认删除 Profile"
      :description="lib.pendingDeleteProfile.value
        ? `确定要删除「${lib.pendingDeleteProfile.value.profileName}」吗？删除后无法恢复。`
        : ''"
      confirmLabel="删除"
      cancelLabel="取消"
      @confirm="lib.confirmDelete()"
      @cancel="lib.cancelDelete()"
    />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useModelSettings } from '@/composables/useModelSettings'
import ProfileList from './ProfileList.vue'
import ProfileEditor from './ProfileEditor.vue'
import ProfileEmptyState from './ProfileEmptyState.vue'
import DeleteConfirmDialog from './DeleteConfirmDialog.vue'
import EmbeddingConfig from './EmbeddingConfig.vue'

const lib = useModelSettings()

onMounted(() => {
  lib.loadProviders()
  lib.loadProfiles()
})
</script>

<style scoped>
.ai-model-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.ai-model-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  gap: 0;
}

.chat-section {
  flex-shrink: 0;
}

.embedding-section {
  padding: 20px 28px 28px;
  border-top: 1px solid var(--border-subtle);
  margin-top: 4px;
}
</style>
