<template>
  <div class="note-list">
    <div class="header">
      <h3>我的笔记</h3>
      <div class="header-right">
        <!-- ✅ 添加搜索框 -->
        <el-input
            v-model="keyword"
            placeholder="搜索笔记..."
            style="width: 200px"
            @keyup.enter="loadNotes"
        />
        <TagFilter
            v-model="selectedTags"
            v-model:match-mode="tagMatch"
            :all-tags="allTags"
            @change="handleTagFilter"
        />
        </div>
      <el-button type="primary" @click="createNote">
        <el-icon><Plus /></el-icon> 新建笔记
      </el-button>
    </div>

    <el-empty v-if="!loading && notes.length === 0" description="暂无笔记，点击右上角创建" />

<!--    <div v-else class="list">-->
<!--      <el-card-->
<!--          v-for="note in notes"-->
<!--          :key="note.id"-->
<!--          class="note-card"-->
<!--          shadow="hover"-->
<!--          @click="editNote(note.id)"-->
<!--      >-->
<!--        <h4 class="title">{{ note.title }}</h4>-->
<!--        <p class="summary">{{ note.summary || '暂无摘要' }}</p>-->
<!--        <div class="meta">-->
<!--          <el-tag v-for="tag in note.tags" :key="tag" size="small" class="tag">-->
<!--            {{ tag }}-->
<!--          </el-tag>-->
<!--          <span class="time">{{ formatTime(note.updatedAt) }}</span>-->
<!--        </div>-->
<!--      </el-card>-->
<!--    </div>-->
    <div v-else class="list">
      <el-card
          v-for="note in notes"
          :key="note.id"
          class="note-card"
          shadow="hover"
      >
        <div @click="editNote(note.id)" class="card-content">
          <h4 class="title">{{ note.title }}</h4>
          <p class="summary">{{ note.summary || '暂无摘要' }}</p>
          <div class="meta">
            <el-tag v-for="tag in note.tags" :key="tag" size="small" class="tag">
              {{ tag }}
            </el-tag>
            <span class="time">{{ formatTime(note.updatedAt) }}</span>
          </div>
        </div>
        <div class="card-actions">
          <el-button
              type="danger"
              size="small"
              :icon="Delete"
              circle
              @click.stop="softDelete(note.id)"
          />
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
import { ref, onMounted, onActivated} from 'vue'
import { useRouter, useRoute, onBeforeRouteUpdate} from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import TagFilter from '@/components/TagFilter.vue'//day05新增
import { Delete } from '@element-plus/icons-vue'//day05新增笔记删除方法
import { ElMessage, ElMessageBox } from 'element-plus'//day05总修改导入

const router = useRouter()
const route = useRoute()
const notes = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const selectedTags = ref([])//day05新增
const tagMatch = ref('ANY')
const allTags = ref([])
const keyword=ref('')

// const loadNotes = async () => {
//   loading.value = true
//   try {
//     const res = await request.get('/note/page', {
//       params: {
//         pageNum: pageNum.value,
//         pageSize: pageSize.value
//       }
//     })
//     notes.value = res.data.records
//     total.value = res.data.total
//   } finally {
//     loading.value = false
//   }
// }
// 修改 loadNotes 方法
const loadNotes = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }

    if (selectedTags.value.length > 0) {
      params.tags = selectedTags.value
      params.tagMatch = tagMatch.value
    }

    if (keyword.value) {
      params.keyword = keyword.value
    }

    const res = await request.get('/note/page', { params })

    notes.value = res.records
    total.value = res.total
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

// 加载所有标签（用于筛选下拉）
const loadAllTags = async () => {
  try {
    const res = await request.get('/note/tags')
    allTags.value = res
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

// 处理标签筛选
const handleTagFilter = () => {
  pageNum.value = 1
  loadNotes()
}

// 方案 A：使用 onActivated（缓存组件时的最佳实践）
// ✅ 当从其他路由返回到这个组件时调用
onActivated(() => {
  // 检查 query 参数是否需要刷新
  if (route.query.refresh) {
    loadNotes()
  }
})

onMounted(()=>{
  loadNotes()
  loadAllTags()
})

//添加笔记删除方法
const softDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定将笔记移入回收站吗？', '提示', {
      type: 'warning'
    })
    await request.delete(`/note/${id}`)  // 默认软删除
    ElMessage.success('已移入回收站')
    loadNotes()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

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
  position: relative;
}

.card-content {
  padding-right: 40px;  /* 为删除按钮留出空间 */
}

.note-card:hover {
  transform: translateY(-2px);
}

.card-actions {
  position: absolute;
  top: 20px;
  right: 20px;
  opacity: 0;
  transition: opacity 0.2s;
}

.note-card:hover .card-actions {
  opacity: 1;
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