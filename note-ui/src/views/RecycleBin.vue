<!--修改前代码-->
<!--<template>-->
<!--  <div class="recycle-bin">-->
<!--    <div class="header">-->
<!--      <h3>回收站</h3>-->
<!--      <el-button type="danger" :loading="clearing" @click="handleClearAll">-->
<!--        清空回收站-->
<!--      </el-button>-->
<!--    </div>-->

<!--    <el-empty v-if="!loading && notes.length === 0" description="回收站是空的" />-->

<!--    <div v-else class="list">-->
<!--      <el-card v-for="note in notes" :key="note.id" class="note-card">-->
<!--        <h4 class="title">{{ note.title }}</h4>-->
<!--        <p class="summary">{{ note.summary || '暂无摘要' }}</p>-->
<!--        <div class="meta">-->
<!--          <el-tag v-for="tag in note.tags" :key="tag" size="small" class="tag">-->
<!--            {{ tag }}-->
<!--          </el-tag>-->
<!--          <span class="time">删除于 {{ formatTime(note.deletedAt) }}</span>-->
<!--        </div>-->
<!--        <div class="actions">-->
<!--          <el-button size="small" type="primary" @click="restoreNote(note.id)">-->
<!--            恢复-->
<!--          </el-button>-->
<!--          <el-button size="small" type="danger" @click="permanentDelete(note.id)">-->
<!--            永久删除-->
<!--          </el-button>-->
<!--        </div>-->
<!--      </el-card>-->
<!--    </div>-->

<!--    <el-pagination-->
<!--        v-if="total > 0"-->
<!--        v-model:current-page="pageNum"-->
<!--        :page-size="pageSize"-->
<!--        :total="total"-->
<!--        layout="prev, pager, next"-->
<!--        @current-change="loadRecycle"-->
<!--    />-->
<!--  </div>-->
<!--</template>-->

<!--<script setup>-->
<!--import { ref, onMounted } from 'vue'-->
<!--import { ElMessage, ElMessageBox } from 'element-plus'-->
<!--import request from '@/utils/request'-->

<!--const notes = ref([])-->
<!--const loading = ref(false)-->
<!--const clearing = ref(false)-->
<!--const pageNum = ref(1)-->
<!--const pageSize = ref(20)-->
<!--const total = ref(0)-->

<!--const loadRecycle = async () => {-->
<!--  loading.value = true-->
<!--  try {-->
<!--    const res = await request.get('/note/recycle/page', {-->
<!--      params: {-->
<!--        pageNum: pageNum.value,-->
<!--        pageSize: pageSize.value-->
<!--      }-->
<!--    })-->
<!--    notes.value = res.records-->
<!--    total.value = res.total-->
<!--  } finally {-->
<!--    loading.value = false-->
<!--  }-->
<!--}-->

<!--const restoreNote = async (id) => {-->
<!--  try {-->
<!--    await request.put(`/note/${id}/restore`)-->
<!--    ElMessage.success('已恢复')-->
<!--    loadRecycle()-->
<!--  } catch (error) {-->
<!--    console.error('恢复失败:', error)-->
<!--  }-->
<!--}-->

<!--const permanentDelete = async (id) => {-->
<!--  try {-->
<!--    await ElMessageBox.confirm('确定永久删除该笔记吗？此操作不可恢复', '警告', {-->
<!--      type: 'warning'-->
<!--    })-->
<!--    await request.delete(`/note/${id}?permanent=true`)-->
<!--    ElMessage.success('已永久删除')-->
<!--    loadRecycle()-->
<!--  } catch (error) {-->
<!--    if (error !== 'cancel') {-->
<!--      console.error('删除失败:', error)-->
<!--    }-->
<!--  }-->
<!--}-->

<!--const handleClearAll = async () => {-->
<!--  try {-->
<!--    await ElMessageBox.confirm('确定清空回收站吗？所有笔记将永久删除', '警告', {-->
<!--      type: 'warning'-->
<!--    })-->
<!--    clearing.value = true-->
<!--    await request.delete('/note/recycle/clear')-->
<!--    ElMessage.success('回收站已清空')-->
<!--    loadRecycle()-->
<!--  } catch (error) {-->
<!--    if (error !== 'cancel') {-->
<!--      console.error('清空失败:', error)-->
<!--    }-->
<!--  } finally {-->
<!--    clearing.value = false-->
<!--  }-->
<!--}-->

<!--const formatTime = (time) => {-->
<!--  if (!time) return ''-->
<!--  return new Date(time).toLocaleString('zh-CN')-->
<!--}-->

<!--onMounted(loadRecycle)-->
<!--</script>-->

<!--<style scoped>-->
<!--.recycle-bin {-->
<!--  max-width: 1200px;-->
<!--  margin: 0 auto;-->
<!--}-->

<!--.header {-->
<!--  display: flex;-->
<!--  justify-content: space-between;-->
<!--  align-items: center;-->
<!--  margin-bottom: 20px;-->
<!--}-->

<!--.note-card {-->
<!--  margin-bottom: 15px;-->
<!--  position: relative;-->
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
<!--  margin-bottom: 15px;-->
<!--}-->

<!--.tag {-->
<!--  margin-right: 5px;-->
<!--}-->

<!--.time {-->
<!--  color: #909399;-->
<!--  font-size: 12px;-->
<!--  margin-left: auto;-->
<!--}-->

<!--.actions {-->
<!--  display: flex;-->
<!--  gap: 10px;-->
<!--  justify-content: flex-end;-->
<!--  border-top: 1px solid #ebeef5;-->
<!--  padding-top: 15px;-->
<!--}-->
<!--</style>-->


<!--修改后代码-->
<template>
  <div class="recycle-bin">

    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <h2 class="page-title">回收站</h2>
        <span class="note-count" v-if="total > 0">{{ total }} 篇待清理</span>
      </div>
      <button
          v-if="notes.length > 0"
          class="clear-btn"
          :class="{ loading: clearing }"
          :disabled="clearing"
          @click="handleClearAll"
      >
        <svg v-if="!clearing" class="btn-icon" viewBox="0 0 16 16" fill="none">
          <path d="M3 4h10M6 4V3h4v1M5 4l.5 9h5L11 4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <svg v-else class="btn-icon spin" viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-opacity="0.3" stroke-width="2.5"/>
          <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
        </svg>
        {{ clearing ? '清空中...' : '清空回收站' }}
      </button>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && notes.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 80 80" fill="none">
          <circle cx="40" cy="40" r="36" fill="#f5f3ff"/>
          <path d="M25 32h30M30 32V28a2 2 0 012-2h16a2 2 0 012 2v4M35 38v16M45 38v16" stroke="#c8c5e8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <rect x="26" y="32" width="28" height="22" rx="2" stroke="#c8c5e8" stroke-width="2"/>
        </svg>
      </div>
      <p class="empty-title">回收站是空的</p>
      <p class="empty-sub">移入回收站的笔记会在这里等待</p>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="note-list">
      <div v-for="i in 3" :key="i" class="skeleton-card">
        <div class="skeleton-left">
          <div class="skeleton-title"></div>
          <div class="skeleton-line"></div>
          <div class="skeleton-meta"></div>
        </div>
        <div class="skeleton-actions"></div>
      </div>
    </div>

    <!-- 笔记列表 -->
    <div v-if="!loading && notes.length > 0" class="note-list">
      <div
          v-for="note in notes"
          :key="note.id"
          class="note-card"
      >
        <div class="card-main">
          <!-- 删除时间标记 -->
          <div class="delete-badge">
            <svg viewBox="0 0 12 12" fill="none">
              <circle cx="6" cy="6" r="5" stroke="currentColor" stroke-width="1.2"/>
              <path d="M6 3.5V6l1.5 1.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
            </svg>
            {{ formatTime(note.deletedAt) }} 删除
          </div>

          <h3 class="card-title">{{ note.title }}</h3>
          <p class="card-summary">{{ note.summary || '暂无摘要' }}</p>

          <div class="card-tags" v-if="note.tags && note.tags.length > 0">
            <span v-for="tag in note.tags.slice(0, 4)" :key="tag" class="tag-pill">{{ tag }}</span>
            <span v-if="note.tags.length > 4" class="tag-more">+{{ note.tags.length - 4 }}</span>
          </div>
        </div>

        <div class="card-actions">
          <button class="action-btn restore-btn" @click="restoreNote(note.id)">
            <svg viewBox="0 0 16 16" fill="none">
              <path d="M3 8a5 5 0 1 0 1.5-3.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <path d="M3 4.5V8h3.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            恢复
          </button>
          <button class="action-btn delete-btn" @click="permanentDelete(note.id)">
            <svg viewBox="0 0 16 16" fill="none">
              <path d="M3 4h10M6 4V3h4v1M5 4l.5 9h5L11 4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            永久删除
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
          @current-change="loadRecycle"
      />
    </div>

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
      params: { pageNum: pageNum.value, pageSize: pageSize.value }
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
    ElMessage.success('已恢复到笔记列表')
    loadRecycle()
  } catch (error) {
    console.error('恢复失败:', error)
  }
}

const permanentDelete = async (id) => {
  try {
    await ElMessageBox.confirm('永久删除后无法恢复，确定继续吗？', '永久删除', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    })
    await request.delete(`/note/${id}?permanent=true`)
    ElMessage.success('已永久删除')
    loadRecycle()
  } catch (error) {
    if (error !== 'cancel') console.error('删除失败:', error)
  }
}

const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm(
        `将永久删除回收站中全部 ${total.value} 篇笔记，此操作不可撤销。`,
        '清空回收站',
        {
          type: 'warning',
          confirmButtonText: '全部删除',
          cancelButtonText: '取消',
          confirmButtonClass: 'el-button--danger'
        }
    )
    clearing.value = true
    await request.delete('/note/recycle/clear')
    ElMessage.success('回收站已清空')
    loadRecycle()
  } catch (error) {
    if (error !== 'cancel') console.error('清空失败:', error)
  } finally {
    clearing.value = false
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / 86400000)} 天前`
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

onMounted(loadRecycle)
</script>

<style scoped>
.recycle-bin {
  max-width: 900px;
  margin: 0 auto;
  padding: 4px 0;
}

/* ===== 顶部工具栏 ===== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
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
  color: #d47a5a;
  font-weight: 400;
}

.clear-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff5f3;
  color: #c0522e;
  border: 1px solid #f5cfc4;
  border-radius: 9px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, opacity 0.15s;
}

.clear-btn:hover:not(:disabled) {
  background: #fee5de;
  border-color: #f0b8a8;
}

.clear-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.spin {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin-bottom: 20px;
}

.empty-icon svg { width: 100%; height: 100%; }

.empty-title {
  font-size: 16px;
  font-weight: 500;
  color: #4a4a6a;
  margin: 0 0 8px 0;
}

.empty-sub {
  font-size: 14px;
  color: #9896b0;
  margin: 0;
}

/* ===== 骨架屏 ===== */
.skeleton-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: white;
  border: 1px solid #f0eef8;
  border-radius: 14px;
  padding: 20px 22px;
  margin-bottom: 10px;
}

.skeleton-left { flex: 1; }

.skeleton-title,
.skeleton-line,
.skeleton-meta,
.skeleton-actions {
  background: #f0eef8;
  border-radius: 4px;
  animation: shimmer 1.4s ease-in-out infinite;
}

.skeleton-title { width: 55%; height: 18px; margin-bottom: 10px; }
.skeleton-line  { width: 85%; height: 13px; margin-bottom: 7px; }
.skeleton-meta  { width: 30%; height: 13px; }
.skeleton-actions { width: 140px; height: 32px; border-radius: 8px; flex-shrink: 0; margin-left: 20px; }

@keyframes shimmer {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ===== 笔记列表 ===== */
.note-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 24px;
}

/* ===== 笔记卡片 ===== */
.note-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: white;
  border: 1px solid #f0eef8;
  border-radius: 14px;
  padding: 20px 22px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.note-card:hover {
  border-color: #e8e4f8;
  box-shadow: 0 4px 16px rgba(107, 92, 231, 0.06);
}

.card-main {
  flex: 1;
  min-width: 0;
}

.delete-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #c09080;
  background: #fff5f0;
  border-radius: 20px;
  padding: 3px 9px;
  margin-bottom: 10px;
}

.delete-badge svg {
  width: 11px;
  height: 11px;
  flex-shrink: 0;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #2a2a3e;
  margin: 0 0 6px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: -0.2px;
}

.card-summary {
  font-size: 13px;
  color: #7c7a96;
  line-height: 1.6;
  margin: 0 0 10px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.tag-pill {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 20px;
  background: #f0eef8;
  color: #6b5ce7;
  font-weight: 500;
}

.tag-more {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 20px;
  background: #f5f5f5;
  color: #9896b0;
}

/* ===== 操作按钮组 ===== */
.card-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100px;
  padding: 7px 0;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  border: 1px solid transparent;
}

.action-btn svg {
  width: 13px;
  height: 13px;
  flex-shrink: 0;
}

.restore-btn {
  background: #f0eeff;
  color: #5a50d8;
  border-color: #dddaf8;
}

.restore-btn:hover {
  background: #e6e2ff;
  border-color: #c8c2f4;
}

.delete-btn {
  background: #fff5f3;
  color: #c0522e;
  border-color: #f5cfc4;
}

.delete-btn:hover {
  background: #fee5de;
  border-color: #f0b8a8;
}

/* ===== 分页 ===== */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 8px 0 4px;
}

.pagination-wrap :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: #6b5ce7;
}

.pagination-wrap :deep(.el-pagination.is-background .el-pager li:hover) {
  color: #6b5ce7;
}
</style>