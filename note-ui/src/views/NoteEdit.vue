<template>
  <div class="note-edit">
    <el-input
        v-model="title"
        placeholder="输入标题..."
        class="title-input"
        size="large"
    />

    <MarkdownEditor
        v-model="content"
        :saving="saving"
        @save="handleSave"
    />
  </div>
</template>

<script setup>
import { ref, onMounted , onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'//day05总修改导入
import request from '@/utils/request'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { debounce } from 'lodash-es'

const route = useRoute()
const router = useRouter()

const noteId = ref(route.params.id ? Number(route.params.id) : null)
const title = ref('')
const content = ref('')
const saving = ref(false)

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
    }
  }
  //加载草稿
  await loadDraft()
})

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
      tags: []  // 暂不实现标签编辑
    }

    let res
    if (noteId.value) {
      await request.put(`/note/${noteId.value}`, payload)
      ElMessage.success('保存成功')
    } else {
      const res = await request.post('/note', payload)
      noteId.value = res
      ElMessage.success('创建成功')
      // 更新 URL，避免重复创建
      await router.replace(`/note/${noteId.value}`)

      // ✅ 手动清除 new 草稿（clearDraft 此时已用新 noteId，清的不是 :new）
      await request.delete('/note/draft', { params: {} })  // 无 noteId 参数 = 清 :new
      await clearDraft()  // 再清带 noteId 的（以防万一）
    }
    //保存成功后清除草稿
    await clearDraft()
    // ✅ 跳转到列表页，并添加 refresh 参数强制刷新
    router.push({ path: '/', query: { refresh: Date.now() } })
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    saving.value = false
  }
}

// 自动保存定时器
// let autoSaveTimer = null
const autoSave = debounce(async () => {
  if (!title.value && !content.value) return

  try {
    const payload = {
      id: noteId.value,
      title: title.value,
      content: content.value,
      tags: []
    }

    await request.post('/note/draft', payload)
    console.log('草稿已自动保存')
  } catch (error) {
    console.error('自动保存失败:', error)
  }
}, 3000)  // 3秒防抖

// 监听内容变化
watch([title, content], () => {
  autoSave()
}, { deep: true })

// 加载草稿
const loadDraft = async () => {
  try {
    const params = noteId.value ? { noteId: noteId.value } : {}
    const res = await request.get('/note/draft', { params })

  //   if (res) {
  //     // 如果有草稿，询问是否恢复
  //     ElMessageBox.confirm('检测到未保存的草稿，是否恢复？', '提示', {
  //       confirmButtonText: '恢复',
  //       cancelButtonText: '丢弃',
  //       type: 'info'
  //     }).then(() => {
  //       title.value = res.title || ''
  //       content.value = res.content || ''
  //     }).catch(() => {
  //       // 丢弃草稿
  //       clearDraft()
  //     })
  //   }
  // } catch (error) {
  //   console.error('加载草稿失败:', error)
  // }
    if (!res) return  // 没有草稿，直接返回

    // 草稿和当前内容完全一样，不打扰用户
    if (res.title === title.value && res.content === content.value) {
      return
    }

    ElMessageBox.confirm(
        `检测到草稿："${res.title || '无标题'}"，是否恢复？`,
        '提示',
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

// 组件卸载时清除定时器
onUnmounted(() => {
  // if (autoSaveTimer) {
  //   clearTimeout(autoSaveTimer)
  // }
  autoSave.cancel()
})

</script>

<style scoped>
.note-edit {
  max-width: 1400px;
  margin: 0 auto;
}

.title-input {
  margin-bottom: 20px;
}

.title-input :deep(.el-input__inner) {
  font-size: 24px;
  font-weight: bold;
  border: none;
  border-bottom: 2px solid #dcdfe6;
  border-radius: 0;
  padding: 10px 0;
}

.title-input :deep(.el-input__inner:focus) {
  border-bottom-color: #409eff;
}
</style>
