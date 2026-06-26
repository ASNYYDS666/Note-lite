import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useNotesStore = defineStore('notes', () => {
  const noteTree = ref(null)
  const currentNote = ref(null)
  const loading = ref(false)
  const saving = ref(false)
  const wordCount = ref(0)

  const trashNotes = ref([])
  const trashTotal = ref(0)
  const trashPage = ref(1)

  // Folder CRUD
  async function loadTree() {
    try {
      const data = await request.get('/note/tree')
      noteTree.value = data
    } catch (e) {
      console.error('Failed to load note tree:', e)
    }
  }

  async function createFolder(name, parentId = null) {
    await request.post('/note/folder', null, { params: { name, parentId } })
    await loadTree()
  }

  async function renameFolder(id, name) {
    await request.put(`/note/folder/${id}`, null, { params: { name } })
    await loadTree()
  }

  async function deleteFolder(id) {
    await request.delete(`/note/folder/${id}`)
    await loadTree()
  }

  // Note CRUD
  async function loadNote(id) {
    loading.value = true
    try {
      currentNote.value = await request.get(`/note/${id}`)
      return currentNote.value
    } finally {
      loading.value = false
    }
  }

  async function createNote({ title, content, folderId, tags }) {
    saving.value = true
    try {
      const note = await request.post('/note', { title, content, folderId, tags })
      await loadTree()
      return note
    } finally {
      saving.value = false
    }
  }

  async function updateNote({ id, title, content, folderId, tags }) {
    saving.value = true
    try {
      await request.put(`/note/${id}`, { id, title, content, folderId, tags })
    } finally {
      saving.value = false
    }
  }

  async function moveNote(id, folderId) {
    await request.put(`/note/${id}/move`, null, { params: { folderId } })
    await loadTree()
  }

  async function renameNote(id, title) {
    await request.put(`/note/${id}/rename`, null, { params: { title } })
    await loadTree()
  }

  async function softDeleteNote(id) {
    await request.delete(`/note/${id}`)
    await loadTree()
  }

  // Draft
  async function saveDraft(noteId, content) {
    const title = currentNote.value?.title || ''
    await request.post('/note/draft', { id: noteId, title, content })
  }

  async function getDraft(noteId) {
    try {
      return await request.get(`/note/draft?noteId=${noteId || ''}`)
    } catch {
      return null
    }
  }

  async function clearDraft(noteId) {
    await request.delete(`/note/draft?noteId=${noteId || ''}`)
  }

  // Trash
  async function loadTrash(page = 1) {
    trashPage.value = page
    try {
      const res = await request.get('/note/recycle/page', { params: { pageNum: page, pageSize: 50 } })
      trashNotes.value = res.records || []
      trashTotal.value = res.total || 0
    } catch (e) {
      console.error('Failed to load trash:', e)
    }
  }

  async function restoreNote(id) {
    await request.put(`/note/${id}/restore`)
    await loadTrash(trashPage.value)
    await loadTree()
  }

  async function permanentDeleteNote(id) {
    await request.delete(`/note/${id}?permanent=true`)
    await loadTrash(trashPage.value)
  }

  async function clearTrash() {
    await request.delete('/note/recycle/clear')
    await loadTrash(1)
  }

  function setWordCount(count) {
    wordCount.value = count
  }

  return {
    noteTree, currentNote, loading, saving, wordCount,
    trashNotes, trashTotal, trashPage,
    loadTree, createFolder, renameFolder, deleteFolder,
    loadNote, createNote, updateNote, moveNote, renameNote, softDeleteNote,
    saveDraft, getDraft, clearDraft,
    loadTrash, restoreNote, permanentDeleteNote, clearTrash,
    setWordCount
  }
})
