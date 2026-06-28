<template>
  <div class="workspace">
    <TopBar />
    <IconRail />
    <LeftPanel />
    <div
      class="resize-handle resize-handle--left"
      :class="{ 'resize-handle--active': leftDragging }"
      :style="leftHandleStyle"
      @mousedown="onLeftHandleMouseDown"
    />
    <div class="main-content" :style="mainContentStyle">
      <TiptapEditor v-if="workspace.showEditor" />
      <TrashView v-else-if="workspace.showTrash" />
    </div>
    <div
      v-if="workspace.agentPanelVisible"
      class="resize-handle resize-handle--right"
      :class="{ 'resize-handle--active': rightDragging }"
      :style="rightHandleStyle"
      @mousedown="onRightHandleMouseDown"
    />
    <AgentPanel />
    <StatusBar />
    <SettingsDialog />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useWorkspaceStore } from '@/store/workspace'
import { useResizeHandle } from '@/composables/useResizeHandle'
import TopBar from '@/components/layout/TopBar.vue'
import IconRail from '@/components/layout/IconRail.vue'
import LeftPanel from '@/components/panels/LeftPanel.vue'
import AgentPanel from '@/components/panels/AgentPanel.vue'
import StatusBar from '@/components/layout/StatusBar.vue'
import TiptapEditor from '@/components/features/editor/TiptapEditor.vue'
import TrashView from '@/components/features/trash-view/TrashView.vue'
import SettingsDialog from '@/components/features/settings/SettingsDialog.vue'

const workspace = useWorkspaceStore()

const { dragging: leftDragging, onMouseDown: onLeftHandleMouseDown } = useResizeHandle({
  min: workspace.MIN_PANEL_WIDTH,
  max: workspace.MAX_PANEL_WIDTH,
  getWidth: () => workspace.leftPanelWidth,
  onResize: (w) => workspace.setLeftPanelWidth(w)
})

const { dragging: rightDragging, onMouseDown: onRightHandleMouseDown } = useResizeHandle({
  min: workspace.MIN_PANEL_WIDTH,
  max: workspace.MAX_PANEL_WIDTH,
  reverse: true,
  getWidth: () => workspace.agentPanelWidth,
  onResize: (w) => workspace.setAgentPanelWidth(w)
})

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

const leftHandleStyle = computed(() => ({
  left: `calc(var(--icon-rail-width) + ${workspace.leftPanelWidth}px)`
}))

const rightHandleStyle = computed(() => ({
  right: `${workspace.agentPanelWidth}px`
}))
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

.resize-handle {
  position: fixed;
  top: var(--top-bar-height);
  bottom: var(--status-bar-height);
  width: 6px;
  z-index: 90;
  cursor: col-resize;
  transition: background-color 0.15s;
  background: transparent;
}

.resize-handle:hover,
.resize-handle--active {
  background: var(--accent-sage);
  opacity: 0.6;
}
</style>
