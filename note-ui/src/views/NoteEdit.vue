<!--修改前代码-->
<!--<template>-->
<!--  <div class="note-edit">-->
<!--    <el-input-->
<!--        v-model="title"-->
<!--        placeholder="输入标题..."-->
<!--        class="title-input"-->
<!--        size="large"-->
<!--    />-->

<!--    <MarkdownEditor-->
<!--        v-model="content"-->
<!--        :saving="saving"-->
<!--        @save="handleSave"-->
<!--    />-->
<!--  </div>-->
<!--</template>-->

<!--<script setup>-->
<!--import { ref, onMounted , onUnmounted, watch } from 'vue'-->
<!--import { useRoute, useRouter } from 'vue-router'-->
<!--import { ElMessage, ElMessageBox } from 'element-plus'//day05总修改导入-->
<!--import request from '@/utils/request'-->
<!--import MarkdownEditor from '@/components/MarkdownEditor.vue'-->
<!--import { debounce } from 'lodash-es'-->

<!--const route = useRoute()-->
<!--const router = useRouter()-->

<!--const noteId = ref(route.params.id ? Number(route.params.id) : null)-->
<!--const title = ref('')-->
<!--const content = ref('')-->
<!--const saving = ref(false)-->

<!--// 加载已有笔记-->
<!--onMounted(async () => {-->
<!--  if (noteId.value) {-->
<!--    try {-->
<!--      const res = await request.get(`/note/${noteId.value}`)-->
<!--      title.value = res.title-->
<!--      content.value = res.content-->
<!--    } catch (error) {-->
<!--      ElMessage.error('加载笔记失败')-->
<!--      router.push('/')-->
<!--    }-->
<!--  }-->
<!--  //加载草稿-->
<!--  await loadDraft()-->
<!--})-->

<!--const handleSave = async () => {-->
<!--  if (!title.value.trim()) {-->
<!--    ElMessage.warning('请输入标题')-->
<!--    return-->
<!--  }-->

<!--  saving.value = true-->
<!--  try {-->
<!--    const payload = {-->
<!--      title: title.value,-->
<!--      content: content.value,-->
<!--      tags: []  // 暂不实现标签编辑-->
<!--    }-->

<!--    let res-->
<!--    if (noteId.value) {-->
<!--      await request.put(`/note/${noteId.value}`, payload)-->
<!--      ElMessage.success('保存成功')-->
<!--    } else {-->
<!--      const res = await request.post('/note', payload)-->
<!--      noteId.value = res-->
<!--      ElMessage.success('创建成功')-->
<!--      // 更新 URL，避免重复创建-->
<!--      await router.replace(`/note/${noteId.value}`)-->

<!--      // ✅ 手动清除 new 草稿（clearDraft 此时已用新 noteId，清的不是 :new）-->
<!--      await request.delete('/note/draft', { params: {} })  // 无 noteId 参数 = 清 :new-->
<!--      await clearDraft()  // 再清带 noteId 的（以防万一）-->
<!--    }-->
<!--    //保存成功后清除草稿-->
<!--    await clearDraft()-->
<!--    // ✅ 跳转到列表页，并添加 refresh 参数强制刷新-->
<!--    router.push({ path: '/', query: { refresh: Date.now() } })-->
<!--  } catch (error) {-->
<!--    // 错误已在拦截器处理-->
<!--  } finally {-->
<!--    saving.value = false-->
<!--  }-->
<!--}-->

<!--// 自动保存定时器-->
<!--// let autoSaveTimer = null-->
<!--const autoSave = debounce(async () => {-->
<!--  if (!title.value && !content.value) return-->

<!--  try {-->
<!--    const payload = {-->
<!--      id: noteId.value,-->
<!--      title: title.value,-->
<!--      content: content.value,-->
<!--      tags: []-->
<!--    }-->

<!--    await request.post('/note/draft', payload)-->
<!--    console.log('草稿已自动保存')-->
<!--  } catch (error) {-->
<!--    console.error('自动保存失败:', error)-->
<!--  }-->
<!--}, 3000)  // 3秒防抖-->

<!--// 监听内容变化-->
<!--watch([title, content], () => {-->
<!--  autoSave()-->
<!--}, { deep: true })-->

<!--// 加载草稿-->
<!--const loadDraft = async () => {-->
<!--  try {-->
<!--    const params = noteId.value ? { noteId: noteId.value } : {}-->
<!--    const res = await request.get('/note/draft', { params })-->

<!--  //   if (res) {-->
<!--  //     // 如果有草稿，询问是否恢复-->
<!--  //     ElMessageBox.confirm('检测到未保存的草稿，是否恢复？', '提示', {-->
<!--  //       confirmButtonText: '恢复',-->
<!--  //       cancelButtonText: '丢弃',-->
<!--  //       type: 'info'-->
<!--  //     }).then(() => {-->
<!--  //       title.value = res.title || ''-->
<!--  //       content.value = res.content || ''-->
<!--  //     }).catch(() => {-->
<!--  //       // 丢弃草稿-->
<!--  //       clearDraft()-->
<!--  //     })-->
<!--  //   }-->
<!--  // } catch (error) {-->
<!--  //   console.error('加载草稿失败:', error)-->
<!--  // }-->
<!--    if (!res) return  // 没有草稿，直接返回-->

<!--    // 草稿和当前内容完全一样，不打扰用户-->
<!--    if (res.title === title.value && res.content === content.value) {-->
<!--      return-->
<!--    }-->

<!--    ElMessageBox.confirm(-->
<!--        `检测到草稿："${res.title || '无标题'}"，是否恢复？`,-->
<!--        '提示',-->
<!--        {-->
<!--          confirmButtonText: '恢复草稿',-->
<!--          cancelButtonText: '丢弃',-->
<!--          type: 'info'-->
<!--        }-->
<!--    ).then(() => {-->
<!--      title.value = res.title || ''-->
<!--      content.value = res.content || ''-->
<!--    }).catch(() => {-->
<!--      clearDraft()-->
<!--    })-->
<!--  } catch (error) {-->
<!--    console.error('加载草稿失败:', error)-->
<!--  }-->
<!--}-->

<!--// 清除草稿-->
<!--const clearDraft = async () => {-->
<!--  try {-->
<!--    const params = noteId.value ? { noteId: noteId.value } : {}-->
<!--    await request.delete('/note/draft', { params })-->
<!--  } catch (error) {-->
<!--    console.error('清除草稿失败:', error)-->
<!--  }-->
<!--}-->

<!--// 组件卸载时清除定时器-->
<!--onUnmounted(() => {-->
<!--  // if (autoSaveTimer) {-->
<!--  //   clearTimeout(autoSaveTimer)-->
<!--  // }-->
<!--  autoSave.cancel()-->
<!--})-->

<!--</script>-->

<!--<style scoped>-->
<!--.note-edit {-->
<!--  max-width: 1400px;-->
<!--  margin: 0 auto;-->
<!--}-->

<!--.title-input {-->
<!--  margin-bottom: 20px;-->
<!--}-->

<!--.title-input :deep(.el-input__inner) {-->
<!--  font-size: 24px;-->
<!--  font-weight: bold;-->
<!--  border: none;-->
<!--  border-bottom: 2px solid #dcdfe6;-->
<!--  border-radius: 0;-->
<!--  padding: 10px 0;-->
<!--}-->

<!--.title-input :deep(.el-input__inner:focus) {-->
<!--  border-bottom-color: #409eff;-->
<!--}-->
<!--</style>-->

<!--修改后代码-->
<template>
  <div class="note-edit">

    <!-- 顶部操作栏 -->
    <div class="edit-header">
      <button class="back-btn" @click="handleBack">
        <svg viewBox="0 0 16 16" fill="none">
          <path d="M10 4L6 8l4 4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        返回
      </button>

      <div class="header-center">
        <!-- 草稿状态提示 -->
        <transition name="fade">
          <span v-if="draftStatus" class="draft-hint" :class="draftStatus">
            <svg v-if="draftStatus === 'saved'" viewBox="0 0 12 12" fill="none">
              <circle cx="6" cy="6" r="5" stroke="currentColor" stroke-width="1.2"/>
              <path d="M4 6l1.5 1.5L8 4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <svg v-else class="spin-sm" viewBox="0 0 12 12" fill="none">
              <circle cx="6" cy="6" r="4.5" stroke="currentColor" stroke-opacity="0.3" stroke-width="1.5"/>
              <path d="M6 1.5a4.5 4.5 0 0 1 4.5 4.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
            {{ draftStatus === 'saved' ? '草稿已保存' : '保存中...' }}
          </span>
        </transition>
      </div>

      <button
          class="ai-toggle-btn"
          :class="{ active: showAIPanel }"
          @click="showAIPanel = !showAIPanel"
          title="AI 对话"
      >
        <svg viewBox="0 0 20 20" fill="none">
          <circle cx="10" cy="10" r="8" stroke="currentColor" stroke-width="1.5"/>
          <path d="M6 8h.01M10 8h.01M14 8h.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        AI
      </button>

      <button
          class="save-btn"
          :class="{ loading: saving }"
          :disabled="saving"
          @click="handleSave"
      >
        <svg v-if="!saving" viewBox="0 0 16 16" fill="none">
          <path d="M3 8l3.5 3.5L13 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <svg v-else class="spin-sm" viewBox="0 0 16 16" fill="none">
          <circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-opacity="0.3" stroke-width="2"/>
          <path d="M8 1.5a6.5 6.5 0 0 1 6.5 6.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
        {{ saving ? '保存中' : '保存' }}
      </button>
    </div>

    <!-- 标题输入 -->
    <div class="title-area">
      <input
          v-model="title"
          class="title-input"
          placeholder="输入标题..."
          maxlength="200"
      />
      <span class="title-count" :class="{ warn: title.length > 180 }">
        {{ title.length }}/200
      </span>
    </div>

    <!-- Markdown 编辑器 -->
    <MarkdownEditor
        v-model="content"
        :saving="saving"
        @save="handleSave"
    />

    <AIChatPanel
        :visible="showAIPanel"
        :note-id="noteId"
        @close="showAIPanel = false"
        @navigate="handleAINavigate"
    />

  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import AIChatPanel from '@/components/AIChatPanel.vue'
import { debounce } from 'lodash-es'

const route = useRoute()
const router = useRouter()

const noteId = ref(route.params.id ? Number(route.params.id) : null)
const title = ref('')
const content = ref('')
const saving = ref(false)
const showAIPanel = ref(false)
const draftStatus = ref('') // '' | 'saving' | 'saved'

let draftHideTimer = null

// 加载已有笔记
onMounted(async () => {
  if (noteId.value) {
    try {
      const res = await request.get(`/note/${noteId.value}`)
      title.value = res.title
      content.value = res.content
    } catch (error) {
      ElMessage.error('加载笔记失败')
      router.push('/')
      return
    }
  }
  await loadDraft()
})

// 返回按钮
const handleBack = () => {
  router.push('/')
}

// AI 面板跳转到引用的笔记
const handleAINavigate = (targetNoteId) => {
  if (targetNoteId && targetNoteId !== noteId.value) {
    router.push(`/note/${targetNoteId}`)
  }
}

// 保存
const handleSave = async () => {
  if (!title.value.trim()) {
    ElMessage.warning('请输入标题')
    return
  }

  saving.value = true
  try {
    const payload = {
      title: title.value,
      content: content.value,
      tags: []
    }

    if (noteId.value) {
      await request.put(`/note/${noteId.value}`, payload)
      ElMessage.success('保存成功')
    } else {
      const res = await request.post('/note', payload)
      noteId.value = res
      ElMessage.success('创建成功')
      await router.replace(`/note/${noteId.value}`)
      // 清除 :new 草稿
      await request.delete('/note/draft', { params: {} })
      await clearDraft()
    }

    await clearDraft()
    router.push({ path: '/', query: { refresh: Date.now() } })
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    saving.value = false
  }
}

// 自动保存（防抖 3 秒）
const autoSave = debounce(async () => {
  if (!title.value && !content.value) return

  draftStatus.value = 'saving'
  try {
    await request.post('/note/draft', {
      id: noteId.value,
      title: title.value,
      content: content.value,
      tags: []
    })
    draftStatus.value = 'saved'
    // 2 秒后隐藏提示
    clearTimeout(draftHideTimer)
    draftHideTimer = setTimeout(() => {
      draftStatus.value = ''
    }, 2000)
  } catch (error) {
    draftStatus.value = ''
    console.error('自动保存失败:', error)
  }
}, 3000)

watch([title, content], () => {
  autoSave()
}, { deep: true })

// 加载草稿
const loadDraft = async () => {
  try {
    const params = noteId.value ? { noteId: noteId.value } : {}
    const res = await request.get('/note/draft', { params })

    if (!res) return

    if (res.title === title.value && res.content === content.value) return

    ElMessageBox.confirm(
        `检测到草稿："${res.title || '无标题'}"，是否恢复？`,
        '恢复草稿',
        {
          confirmButtonText: '恢复草稿',
          cancelButtonText: '丢弃',
          type: 'info'
        }
    ).then(() => {
      title.value = res.title || ''
      content.value = res.content || ''
    }).catch(() => {
      clearDraft()
    })
  } catch (error) {
    console.error('加载草稿失败:', error)
  }
}

// 清除草稿
const clearDraft = async () => {
  try {
    const params = noteId.value ? { noteId: noteId.value } : {}
    await request.delete('/note/draft', { params })
  } catch (error) {
    console.error('清除草稿失败:', error)
  }
}

onUnmounted(() => {
  autoSave.cancel()
  clearTimeout(draftHideTimer)
})
</script>

<style scoped>
.note-edit {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px); /* 减去 Layout header 高度 */
  max-width: 100%;
  overflow: hidden;
}

/* ===== 顶部操作栏 ===== */
.edit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 16px 0;
  flex-shrink: 0;
  gap: 12px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  background: white;
  border: 1px solid #e8e6f0;
  border-radius: 9px;
  padding: 7px 14px;
  font-size: 13px;
  color: #4a4a6a;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  flex-shrink: 0;
}

.back-btn svg {
  width: 14px;
  height: 14px;
}

.back-btn:hover {
  background: #f5f3ff;
  border-color: #c8c5e8;
  color: #5a50d8;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

/* 草稿状态提示 */
.draft-hint {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  padding: 5px 10px;
  border-radius: 20px;
}

.draft-hint svg {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
}

.draft-hint.saved {
  color: #2e7d5a;
  background: #edf7f2;
}

.draft-hint.saving {
  color: #7a6a30;
  background: #fdf8e6;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 保存按钮 */
.save-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #5a50d8 0%, #7b6ef0 100%);
  color: white;
  border: none;
  border-radius: 9px;
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s, transform 0.15s;
  flex-shrink: 0;
}

.ai-toggle-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  background: white;
  border: 1px solid #e8e6f0;
  border-radius: 9px;
  padding: 8px 14px;
  font-size: 13px;
  color: #6b5ce7;
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}

.ai-toggle-btn svg {
  width: 14px;
  height: 14px;
}

.ai-toggle-btn:hover {
  background: #f5f3ff;
  border-color: #c8c5e8;
}

.ai-toggle-btn.active {
  background: #6b5ce7;
  border-color: #6b5ce7;
  color: white;
}

.save-btn svg {
  width: 14px;
  height: 14px;
}

.save-btn:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
}

.save-btn:active:not(:disabled) {
  transform: translateY(0);
}

.save-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spin-sm {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== 标题区域 ===== */
.title-area {
  position: relative;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.title-input {
  width: 100%;
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  border: none;
  border-bottom: 2px solid #e8e6f0;
  border-radius: 0;
  padding: 8px 60px 12px 2px;
  outline: none;
  background: transparent;
  font-family: 'PingFang SC', 'Hiragino Sans GB', sans-serif;
  letter-spacing: -0.5px;
  transition: border-color 0.2s;
  line-height: 1.3;
}

.title-input::placeholder {
  color: #c0bdd4;
  font-weight: 400;
}

.title-input:focus {
  border-bottom-color: #6b5ce7;
}

.title-count {
  position: absolute;
  right: 4px;
  bottom: 14px;
  font-size: 11px;
  color: #c0bdd4;
  transition: color 0.2s;
  pointer-events: none;
}

.title-count.warn {
  color: #d47a5a;
}

/* ===== Markdown 编辑器区域 ===== */
/* 让 MarkdownEditor 组件撑满剩余高度 */
.note-edit :deep(.md-editor) {
  flex: 1;
  min-height: 0;
  height: auto;
  border-radius: 12px;
  border: 1px solid #f0eef8;
  overflow: hidden;
}

.note-edit :deep(.toolbar) {
  background: white;
  border-bottom: 1px solid #f0eef8;
  padding: 10px 16px;
}

.note-edit :deep(.edit-area) {
  background: #faf9fe;
  font-size: 14px;
  line-height: 1.75;
  color: #2a2a3e;
}

.note-edit :deep(.preview-area) {
  font-size: 14px;
  line-height: 1.75;
  color: #2a2a3e;
}

.note-edit :deep(.preview-area h1),
.note-edit :deep(.preview-area h2) {
  border-bottom-color: #f0eef8;
}
</style>
