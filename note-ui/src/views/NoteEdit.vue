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
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import MarkdownEditor from '@/components/MarkdownEditor.vue'

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
      title.value = res.data.title
      content.value = res.data.content
    } catch (error) {
      ElMessage.error('加载笔记失败')
      router.push('/')
    }
  }
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

    if (noteId.value) {
      await request.put(`/note/${noteId.value}`, payload)
      ElMessage.success('保存成功')
    } else {
      const res = await request.post('/note', payload)
      noteId.value = res.data
      ElMessage.success('创建成功')
      // 更新 URL，避免重复创建
      router.replace(`/note/${noteId.value}`)
    }
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    saving.value = false
  }
}
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
