<template>
  <div class="recycle-bin">
    <div class="header">
      <h3>回收站</h3>
      <el-button type="danger" :loading="clearing" @click="handleClearAll">
        清空回收站
      </el-button>
    </div>

    <el-empty v-if="!loading && notes.length === 0" description="回收站是空的" />

    <div v-else class="list">
      <el-card v-for="note in notes" :key="note.id" class="note-card">
        <h4 class="title">{{ note.title }}</h4>
        <p class="summary">{{ note.summary || '暂无摘要' }}</p>
        <div class="meta">
          <el-tag v-for="tag in note.tags" :key="tag" size="small" class="tag">
            {{ tag }}
          </el-tag>
          <span class="time">删除于 {{ formatTime(note.deletedAt) }}</span>
        </div>
        <div class="actions">
          <el-button size="small" type="primary" @click="restoreNote(note.id)">
            恢复
          </el-button>
          <el-button size="small" type="danger" @click="permanentDelete(note.id)">
            永久删除
          </el-button>
        </div>
      </el-card>
    </div>

    <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadRecycle"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const notes = ref([])
const loading = ref(false)
const clearing = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const loadRecycle = async () => {
  loading.value = true
  try {
    const res = await request.get('/note/recycle/page', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    notes.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const restoreNote = async (id) => {
  try {
    await request.put(`/note/${id}/restore`)
    ElMessage.success('已恢复')
    loadRecycle()
  } catch (error) {
    console.error('恢复失败:', error)
  }
}

const permanentDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定永久删除该笔记吗？此操作不可恢复', '警告', {
      type: 'warning'
    })
    await request.delete(`/note/${id}?permanent=true`)
    ElMessage.success('已永久删除')
    loadRecycle()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm('确定清空回收站吗？所有笔记将永久删除', '警告', {
      type: 'warning'
    })
    clearing.value = true
    await request.delete('/note/recycle/clear')
    ElMessage.success('回收站已清空')
    loadRecycle()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空失败:', error)
    }
  } finally {
    clearing.value = false
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(loadRecycle)
</script>

<style scoped>
.recycle-bin {
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.note-card {
  margin-bottom: 15px;
  position: relative;
}

.title {
  margin: 0 0 10px 0;
  color: #303133;
}

.summary {
  color: #606266;
  font-size: 14px;
  margin-bottom: 10px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 15px;
}

.tag {
  margin-right: 5px;
}

.time {
  color: #909399;
  font-size: 12px;
  margin-left: auto;
}

.actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  border-top: 1px solid #ebeef5;
  padding-top: 15px;
}
</style>