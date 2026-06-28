import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useWorkspaceStore = defineStore('workspace', () => {
  const activeLeftPanel = ref('tree') // 'tree' | 'search'
  const activeMainView = ref('editor') // 'editor' | 'trash'
  const currentNoteId = ref(null)
  const isCreatingNew = ref(false)
  const selectedFolderId = ref(null)
  const agentPanelVisible = ref(false)
  const settingsDialogVisible = ref(false)

  const leftPanelWidth = ref(220)
  const agentPanelWidth = ref(360)

  const MIN_PANEL_WIDTH = 180
  const MAX_PANEL_WIDTH = 600

  const showNoteTree = computed(() => activeLeftPanel.value === 'tree')
  const showSearchPanel = computed(() => activeLeftPanel.value === 'search')
  const showEditor = computed(() => activeMainView.value === 'editor')
  const showTrash = computed(() => activeMainView.value === 'trash')

  function switchLeftPanel(panel) {
    if (activeLeftPanel.value === panel && panel === 'tree') return // keep tree open
    activeLeftPanel.value = panel
  }

  function switchMainView(view) {
    activeMainView.value = view
    if (view === 'trash') {
      currentNoteId.value = null
      isCreatingNew.value = false
    }
  }

  function openNote(id) {
    currentNoteId.value = id
    isCreatingNew.value = (id === null)
    activeMainView.value = 'editor'
  }

  function toggleAgentPanel() {
    agentPanelVisible.value = !agentPanelVisible.value
  }

  function openSettings() {
    settingsDialogVisible.value = true
  }

  function closeSettings() {
    settingsDialogVisible.value = false
  }

  function setLeftPanelWidth(w) {
    leftPanelWidth.value = Math.min(MAX_PANEL_WIDTH, Math.max(MIN_PANEL_WIDTH, w))
  }

  function setAgentPanelWidth(w) {
    agentPanelWidth.value = Math.min(MAX_PANEL_WIDTH, Math.max(MIN_PANEL_WIDTH, w))
  }

  return {
    activeLeftPanel, activeMainView, currentNoteId, isCreatingNew, selectedFolderId,
    agentPanelVisible, settingsDialogVisible,
    leftPanelWidth, agentPanelWidth,
    showNoteTree, showSearchPanel, showEditor, showTrash,
    switchLeftPanel, switchMainView, openNote,
    toggleAgentPanel, openSettings, closeSettings,
    setLeftPanelWidth, setAgentPanelWidth,
    MIN_PANEL_WIDTH, MAX_PANEL_WIDTH
  }
})
