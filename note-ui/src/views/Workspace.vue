<template>
  <div class="workspace">
    <TopBar />
    <IconRail />
    <LeftPanel />
    <div class="main-content" :style="mainContentStyle">
      <TiptapEditor v-if="workspace.showEditor" />
      <TrashView v-else-if="workspace.showTrash" />
    </div>
    <AgentPanel />
    <StatusBar />
    <SettingsDialog />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useWorkspaceStore } from '@/store/workspace'
import TopBar from '@/components/layout/TopBar.vue'
import IconRail from '@/components/layout/IconRail.vue'
import LeftPanel from '@/components/panels/LeftPanel.vue'
import AgentPanel from '@/components/panels/AgentPanel.vue'
import StatusBar from '@/components/layout/StatusBar.vue'
import TiptapEditor from '@/components/features/editor/TiptapEditor.vue'
import TrashView from '@/components/features/trash-view/TrashView.vue'
import SettingsDialog from '@/components/features/settings/SettingsDialog.vue'

const workspace = useWorkspaceStore()

const mainContentStyle = computed(() => {
  const left = `calc(var(--icon-rail-width) + ${workspace.leftPanelWidth}px)`
  const right = workspace.agentPanelVisible ? `${workspace.agentPanelWidth}px` : '0'
  return {
    left,
    right,
    position: 'fixed',
    top: 'var(--top-bar-height)',
    bottom: 'var(--status-bar-height)'
  }
})
</script>

<style scoped>
.workspace {
  height: 100vh;
  overflow: hidden;
  background: var(--bg);
  font-family: var(--font-ui);
  font-size: var(--text-ui-base);
  color: var(--on-surface);
}

.main-content {
  background: var(--bg);
}
</style>
