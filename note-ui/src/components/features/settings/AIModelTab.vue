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
        :testingEmbed="lib.testingEmbed.value"
        :embedTestResult="lib.embedTestResult.value"
        @update="(patch) => { Object.assign(lib.draft.value, patch) }"
        @updateModel="(i, v) => { Object.assign(lib.draft.value.models[i], v) }"
        @removeModel="(i) => { lib.draft.value.models.splice(i, 1) }"
        @refresh="lib.refreshRemoteModels()"
        @apply="lib.applyDraft()"
        @delete="lib.requestDelete(lib.draft.value.id)"
        @dismissError="lib.refreshError.value = null"
        @dismissNotice="lib.dismissNotice()"
        @testEmbed="lib.testEmbed()"
      />
    </template>
    <template v-else>
      <ProfileEmptyState variant="no-selection" />
    </template>

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
</style>
