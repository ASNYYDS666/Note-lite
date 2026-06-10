<!--主页面原代码-->
<!--<template>-->
<!--  <div class="note-list">-->
<!--    <div class="header">-->
<!--      <h3>我的笔记</h3>-->
<!--      <div class="header-right">-->
<!--        &lt;!&ndash; ✅ 添加搜索框 &ndash;&gt;-->
<!--        <el-input-->
<!--            v-model="keyword"-->
<!--            placeholder="搜索笔记..."-->
<!--            style="width: 200px"-->
<!--            @keyup.enter="loadNotes"-->
<!--        />-->
<!--        <TagFilter-->
<!--            v-model="selectedTags"-->
<!--            v-model:match-mode="tagMatch"-->
<!--            :all-tags="allTags"-->
<!--            @change="handleTagFilter"-->
<!--        />-->
<!--        </div>-->
<!--      <el-button type="primary" @click="createNote">-->
<!--        <el-icon><Plus /></el-icon> 新建笔记-->
<!--      </el-button>-->
<!--    </div>-->

<!--    <el-empty v-if="!loading && notes.length === 0" description="暂无笔记，点击右上角创建" />-->


<!--    <div v-else class="list">-->
<!--      <el-card-->
<!--          v-for="note in notes"-->
<!--          :key="note.id"-->
<!--          class="note-card"-->
<!--          shadow="hover"-->
<!--      >-->
<!--        <div @click="editNote(note.id)" class="card-content">-->
<!--          <h4 class="title">{{ note.title }}</h4>-->
<!--          <p class="summary">{{ note.summary || '暂无摘要' }}</p>-->
<!--          <div class="meta">-->
<!--            <el-tag v-for="tag in note.tags" :key="tag" size="small" class="tag">-->
<!--              {{ tag }}-->
<!--            </el-tag>-->
<!--            <span class="time">{{ formatTime(note.updatedAt) }}</span>-->
<!--          </div>-->
<!--        </div>-->
<!--        <div class="card-actions">-->
<!--          <el-button-->
<!--              type="danger"-->
<!--              size="small"-->
<!--              :icon="Delete"-->
<!--              circle-->
<!--              @click.stop="softDelete(note.id)"-->
<!--          />-->
<!--        </div>-->
<!--      </el-card>-->
<!--    </div>-->

<!--    <el-pagination-->
<!--        v-if="total > 0"-->
<!--        v-model:current-page="pageNum"-->
<!--        :page-size="pageSize"-->
<!--        :total="total"-->
<!--        layout="prev, pager, next"-->
<!--        @current-change="loadNotes"-->
<!--    />-->
<!--  </div>-->
<!--</template>-->

<!--<script setup>-->
<!--import { ref, onMounted, onActivated} from 'vue'-->
<!--import { useRouter, useRoute, onBeforeRouteUpdate} from 'vue-router'-->
<!--import { Plus } from '@element-plus/icons-vue'-->
<!--import request from '@/utils/request'-->
<!--import TagFilter from '@/components/TagFilter.vue'//day05新增-->
<!--import { Delete } from '@element-plus/icons-vue'//day05新增笔记删除方法-->
<!--import { ElMessage, ElMessageBox } from 'element-plus'//day05总修改导入-->

<!--const router = useRouter()-->
<!--const route = useRoute()-->
<!--const notes = ref([])-->
<!--const loading = ref(false)-->
<!--const pageNum = ref(1)-->
<!--const pageSize = ref(20)-->
<!--const total = ref(0)-->
<!--const selectedTags = ref([])//day05新增-->
<!--const tagMatch = ref('ANY')-->
<!--const allTags = ref([])-->
<!--const keyword=ref('')-->

<!--// 修改 loadNotes 方法-->
<!--const loadNotes = async () => {-->
<!--  loading.value = true-->
<!--  try {-->
<!--    const params = {-->
<!--      pageNum: pageNum.value,-->
<!--      pageSize: pageSize.value-->
<!--    }-->

<!--    if (selectedTags.value.length > 0) {-->
<!--      params.tags = selectedTags.value-->
<!--      params.tagMatch = tagMatch.value-->
<!--    }-->

<!--    if (keyword.value) {-->
<!--      params.keyword = keyword.value-->
<!--    }-->

<!--    const res = await request.get('/note/page', { params })-->

<!--    notes.value = res.records-->
<!--    total.value = res.total-->
<!--  } finally {-->
<!--    loading.value = false-->
<!--  }-->
<!--}-->

<!--const createNote = () => {-->
<!--  router.push('/note/new')-->
<!--}-->

<!--const editNote = (id) => {-->
<!--  router.push(`/note/${id}`)-->
<!--}-->

<!--const formatTime = (time) => {-->
<!--  if (!time) return ''-->
<!--  return new Date(time).toLocaleString('zh-CN', {-->
<!--    month: 'short',-->
<!--    day: 'numeric',-->
<!--    hour: '2-digit',-->
<!--    minute: '2-digit'-->
<!--  })-->
<!--}-->

<!--// 加载所有标签（用于筛选下拉）-->
<!--const loadAllTags = async () => {-->
<!--  try {-->
<!--    const res = await request.get('/note/tags')-->
<!--    allTags.value = res-->
<!--  } catch (error) {-->
<!--    console.error('加载标签失败:', error)-->
<!--  }-->
<!--}-->

<!--// 处理标签筛选-->
<!--const handleTagFilter = () => {-->
<!--  pageNum.value = 1-->
<!--  loadNotes()-->
<!--}-->

<!--// 方案 A：使用 onActivated（缓存组件时的最佳实践）-->
<!--// ✅ 当从其他路由返回到这个组件时调用-->
<!--// onActivated(() => {-->
<!--//   // 检查 query 参数是否需要刷新-->
<!--//   if (route.query.refresh) {-->
<!--//     loadNotes()-->
<!--//   }-->
<!--// })-->

<!--onMounted(()=>{-->
<!--  loadNotes()-->
<!--  loadAllTags()-->
<!--})-->

<!--//添加笔记删除方法-->
<!--const softDelete = async (id) => {-->
<!--  try {-->
<!--    await ElMessageBox.confirm('确定将笔记移入回收站吗？', '提示', {-->
<!--      type: 'warning'-->
<!--    })-->
<!--    await request.delete(`/note/${id}`)  // 默认软删除-->
<!--    ElMessage.success('已移入回收站')-->
<!--    loadNotes()-->
<!--  } catch (error) {-->
<!--    if (error !== 'cancel') {-->
<!--      console.error('删除失败:', error)-->
<!--    }-->
<!--  }-->
<!--}-->

<!--</script>-->

<!--<style scoped>-->
<!--.note-list {-->
<!--  max-width: 1200px;-->
<!--  margin: 0 auto;-->
<!--}-->

<!--.header {-->
<!--  display: flex;-->
<!--  justify-content: space-between;-->
<!--  align-items: center;-->
<!--  margin-bottom: 20px;-->
<!--}-->

<!--.list {-->
<!--  display: grid;-->
<!--  gap: 15px;-->
<!--}-->

<!--.note-card {-->
<!--  cursor: pointer;-->
<!--  transition: transform 0.2s;-->
<!--  position: relative;-->
<!--}-->

<!--.card-content {-->
<!--  padding-right: 40px;  /* 为删除按钮留出空间 */-->
<!--}-->

<!--.note-card:hover {-->
<!--  transform: translateY(-2px);-->
<!--}-->

<!--.card-actions {-->
<!--  position: absolute;-->
<!--  top: 20px;-->
<!--  right: 20px;-->
<!--  opacity: 0;-->
<!--  transition: opacity 0.2s;-->
<!--}-->

<!--.note-card:hover .card-actions {-->
<!--  opacity: 1;-->
<!--}-->

<!--.title {-->
<!--  margin: 0 0 10px 0;-->
<!--  color: #303133;-->
<!--}-->

<!--.summary {-->
<!--  color: #606266;-->
<!--  font-size: 14px;-->
<!--  margin-bottom: 10px;-->
<!--  line-height: 1.5;-->
<!--  display: -webkit-box;-->
<!--  -webkit-line-clamp: 2;-->
<!--  -webkit-box-orient: vertical;-->
<!--  overflow: hidden;-->
<!--}-->

<!--.meta {-->
<!--  display: flex;-->
<!--  align-items: center;-->
<!--  gap: 10px;-->
<!--  flex-wrap: wrap;-->
<!--}-->

<!--.tag {-->
<!--  margin-right: 5px;-->
<!--}-->

<!--.time {-->
<!--  color: #909399;-->
<!--  font-size: 12px;-->
<!--  margin-left: auto;-->
<!--}-->
<!--</style>-->

<!--主页面新代码-->
<template>
  <div class="note-list">

    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <h2 class="page-title">我的笔记</h2>
        <span class="note-count">{{ total }} 篇</span>
      </div>
      <div class="toolbar-right">
        <div class="search-wrapper">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
              v-model="keyword"
              class="search-input"
              placeholder="搜索笔记..."
              @keyup.enter="loadNotes"
          />
        </div>
        <TagFilter
            v-model="selectedTags"
            v-model:match-mode="tagMatch"
            :all-tags="allTags"
            @change="handleTagFilter"
        />
        <button class="new-btn" @click="createNote">
          <el-icon><Plus /></el-icon>
          新建笔记
        </button>
      </div>
    </div>

    <!-- 统计行 -->
    <div class="stats-row" v-if="!loading && notes.length > 0">
      <div class="stat-item">
        <span class="stat-num">{{ total }}</span>
        <span class="stat-label">全部笔记</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-num">{{ allTags.length }}</span>
        <span class="stat-label">标签总数</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-num">{{ selectedTags.length > 0 ? notes.length : '-' }}</span>
        <span class="stat-label">当前筛选</span>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && notes.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 64 64" fill="none">
          <rect x="8" y="12" width="48" height="40" rx="6" stroke="#c8c5e8" stroke-width="2"/>
          <line x1="20" y1="24" x2="44" y2="24" stroke="#c8c5e8" stroke-width="2" stroke-linecap="round"/>
          <line x1="20" y1="32" x2="38" y2="32" stroke="#c8c5e8" stroke-width="2" stroke-linecap="round"/>
          <line x1="20" y1="40" x2="30" y2="40" stroke="#c8c5e8" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </div>
      <p class="empty-title">还没有笔记</p>
      <p class="empty-sub">点击「新建笔记」开始记录</p>
      <button class="empty-btn" @click="createNote">
        <el-icon><Plus /></el-icon>
        新建笔记
      </button>
    </div>

    <!-- 加载骨架屏 -->
    <div v-if="loading" class="note-grid">
      <div v-for="i in 6" :key="i" class="skeleton-card">
        <div class="skeleton-tag"></div>
        <div class="skeleton-title"></div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line short"></div>
      </div>
    </div>

    <!-- 笔记网格 -->
    <div v-if="!loading && notes.length > 0" class="note-grid">
      <div
          v-for="note in notes"
          :key="note.id"
          class="note-card"
          :class="getCardAccent(note)"
          @click="editNote(note.id)"
      >
        <!-- 标签行 -->
        <div class="card-tags" v-if="note.tags && note.tags.length > 0">
          <span
              v-for="tag in note.tags.slice(0, 3)"
              :key="tag"
              class="tag-pill"
          >{{ tag }}</span>
          <span v-if="note.tags.length > 3" class="tag-more">+{{ note.tags.length - 3 }}</span>
        </div>

        <!-- 标题 -->
        <h3 class="card-title">{{ note.title }}</h3>

        <!-- 摘要 -->
        <p class="card-summary">{{ note.summary || '暂无摘要' }}</p>

        <!-- 底部 -->
        <div class="card-footer">
          <span class="card-time">{{ formatTime(note.updatedAt) }}</span>
          <button
              class="delete-btn"
              @click.stop="softDelete(note.id)"
              title="移入回收站"
          >
            <el-icon><Delete /></el-icon>
          </button>
          <button
              class="share-btn"
              @click.stop="generateShare(note.id)"
              title="生成分享码"
          >
            <el-icon><Share /></el-icon>
          </button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="total > pageSize">
      <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadNotes"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Plus, Delete, Search, Share} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import TagFilter from '@/components/TagFilter.vue'

const router = useRouter()
const route = useRoute()

const notes = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const selectedTags = ref([])
const tagMatch = ref('ANY')
const allTags = ref([])
const keyword = ref('')

// 根据标签给卡片赋予不同的左边框色调，循环用
const accentColors = ['accent-purple', 'accent-teal', 'accent-amber', 'accent-coral', 'accent-blue']
const getCardAccent = (note) => {
  if (!note.tags || note.tags.length === 0) return ''
  // 用 note.id 取模，保证同一张卡片颜色稳定
  return accentColors[note.id % accentColors.length]
}

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

const createNote = () => router.push('/note/new')
const editNote = (id) => router.push(`/note/${id}`)

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / 3600000)} 小时前`
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const loadAllTags = async () => {
  try {
    const res = await request.get('/note/tags')
    allTags.value = res
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

const handleTagFilter = () => {
  pageNum.value = 1
  loadNotes()
}

const softDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定将笔记移入回收站吗？', '提示', { type: 'warning' })
    await request.delete(`/note/${id}`)
    ElMessage.success('已移入回收站')
    loadNotes()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const generateShare = async (noteId) => {
  try {
    const res = await request.post('/share', { noteId, permission: 'READ' })
    const shareCode = res.shareCode
    // 复制到剪贴板
    await navigator.clipboard.writeText(shareCode)
    ElMessage.success(`分享码已生成并复制：${shareCode}`)
  } catch (error) {
    ElMessage.error('生成分享码失败')
  }
}

onMounted(() => {
  loadNotes()
  loadAllTags()
})
</script>

<style scoped>
.note-list {
  max-width: 1200px;
  margin: 0 auto;
  padding: 4px 0;
}

/* ===== 顶部工具栏 ===== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
  letter-spacing: -0.3px;
}

.note-count {
  font-size: 13px;
  color: #9896b0;
  font-weight: 400;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* 搜索框 */
.search-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  background: white;
  border: 1px solid #e8e6f0;
  border-radius: 9px;
  padding: 6px 12px;
  transition: border-color 0.2s;
}

.search-wrapper:focus-within {
  border-color: #6b5ce7;
  box-shadow: 0 0 0 3px rgba(107, 92, 231, 0.08);
}

.search-icon {
  font-size: 14px;
  color: #b0aec8;
  flex-shrink: 0;
}

.search-input {
  border: none;
  outline: none;
  font-size: 13px;
  color: #1a1a2e;
  background: transparent;
  width: 180px;
}

.search-input::placeholder {
  color: #c0bdd4;
}

/* 新建按钮 */
.new-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #5a50d8 0%, #7b6ef0 100%);
  color: white;
  border: none;
  border-radius: 9px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s, transform 0.15s;
  white-space: nowrap;
}

.new-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.new-btn:active {
  transform: translateY(0);
}

/* ===== 统计行 ===== */
.stats-row {
  display: flex;
  align-items: center;
  gap: 20px;
  background: white;
  border: 1px solid #f0eef8;
  border-radius: 12px;
  padding: 14px 20px;
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-num {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: #9896b0;
}

.stat-divider {
  width: 1px;
  height: 28px;
  background: #f0eef8;
}

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin-bottom: 20px;
}

.empty-icon svg {
  width: 100%;
  height: 100%;
}

.empty-title {
  font-size: 17px;
  font-weight: 500;
  color: #4a4a6a;
  margin: 0 0 8px 0;
}

.empty-sub {
  font-size: 14px;
  color: #9896b0;
  margin: 0 0 24px 0;
}

.empty-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #5a50d8 0%, #7b6ef0 100%);
  color: white;
  border: none;
  border-radius: 9px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s;
}

.empty-btn:hover { opacity: 0.9; }

/* ===== 骨架屏 ===== */
.skeleton-card {
  background: white;
  border-radius: 14px;
  border: 1px solid #f0eef8;
  padding: 18px;
}

.skeleton-tag,
.skeleton-title,
.skeleton-line {
  background: #f0eef8;
  border-radius: 4px;
  animation: shimmer 1.4s ease-in-out infinite;
}

.skeleton-tag { width: 60px; height: 20px; border-radius: 20px; margin-bottom: 12px; }
.skeleton-title { width: 75%; height: 18px; margin-bottom: 10px; }
.skeleton-line { width: 100%; height: 13px; margin-bottom: 7px; }
.skeleton-line.short { width: 60%; }

@keyframes shimmer {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ===== 笔记网格 ===== */
.note-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 24px;
}

@media (max-width: 960px) {
  .note-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 640px) {
  .note-grid { grid-template-columns: 1fr; }
}

/* ===== 笔记卡片 ===== */
.note-card {
  background: white;
  border: 1px solid #f0eef8;
  border-radius: 14px;
  padding: 18px;
  cursor: pointer;
  transition: border-color 0.2s, transform 0.2s, box-shadow 0.2s;
  position: relative;
  display: flex;
  flex-direction: column;
  border-left: 3px solid #e8e6f0;
}

.note-card:hover {
  border-color: #dddaf4;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(107, 92, 231, 0.08);
}

.note-card:hover .delete-btn {
  opacity: 1;
}

/* 左边框色调 */
.accent-purple { border-left-color: #6b5ce7; }
.accent-teal   { border-left-color: #1D9E75; }
.accent-amber  { border-left-color: #BA7517; }
.accent-coral  { border-left-color: #D85A30; }
.accent-blue   { border-left-color: #378ADD; }

/* 标签 */
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-bottom: 10px;
}

.tag-pill {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 20px;
  background: #f0eef8;
  color: #6b5ce7;
  font-weight: 500;
  letter-spacing: 0.1px;
}

.tag-more {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 20px;
  background: #f5f5f5;
  color: #9896b0;
}

/* 标题 */
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px 0;
  line-height: 1.4;
  letter-spacing: -0.2px;
  /* 超出2行截断 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 摘要 */
.card-summary {
  font-size: 13px;
  color: #7c7a96;
  line-height: 1.65;
  margin: 0;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 卡片底部 */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #f5f4fb;
}

.card-time {
  font-size: 11px;
  color: #b0aec8;
}

.delete-btn {
  opacity: 0;
  width: 26px;
  height: 26px;
  border-radius: 7px;
  background: #fff0f0;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #d85a5a;
  transition: opacity 0.2s, background 0.15s;
  font-size: 13px;
}

.delete-btn:hover {
  background: #ffe4e4;
}

/* ===== 分页 ===== */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 8px 0 4px;
}

/* 覆盖 Element Plus 分页样式 */
.pagination-wrap :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: #6b5ce7;
}

.pagination-wrap :deep(.el-pagination.is-background .el-pager li:hover) {
  color: #6b5ce7;
}
</style>