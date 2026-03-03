<template>
  <div class="note-list">
    <div class="header">
      <h3>我的笔记</h3>
      <el-button type="primary" @click="createNote">
        <el-icon><Plus /></el-icon> 新建笔记
      </el-button>
    </div>

    <el-empty v-if="!loading && notes.length === 0" description="暂无笔记，点击右上角创建" />

    <div v-else class="list">
      <el-card
          v-for="note in notes"
          :key="note.id"
          class="note-card"
          shadow="hover"
          @click="editNote(note.id)"
      >
        <h4 class="title">{{ note.title }}</h4>
        <p class="summary">{{ note.summary || '暂无摘要' }}</p>
        <div class="meta">
          <el-tag v-for="tag in note.tags" :key="tag" size="small" class="tag">
            {{ tag }}
          </el-tag>
          <span class="time">{{ formatTime(note.updatedAt) }}</span>
        </div>
      </el-card>
    </div>

    <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadNotes"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const notes = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const loadNotes = async () => {
  loading.value = true
  try {
    const res = await request.get('/note/page', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    notes.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const createNote = () => {
  router.push('/note/new')
}

const editNote = (id) => {
  router.push(`/note/${id}`)
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(loadNotes)
</script>

<style scoped>
.note-list {
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.list {
  display: grid;
  gap: 15px;
}

.note-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.note-card:hover {
  transform: translateY(-2px);
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
}

.tag {
  margin-right: 5px;
}

.time {
  color: #909399;
  font-size: 12px;
  margin-left: auto;
}
</style>