<!--<template>-->
<!--  <div class="share-view">-->
<!--    <div v-if="loading" class="loading">加载中...</div>-->
<!--    <div v-else-if="note">-->
<!--      <h1>{{ note.title }}</h1>-->
<!--      <div class="markdown-body" v-html="renderedHtml"></div>-->
<!--    </div>-->
<!--    <div v-else>分享码无效或已过期</div>-->
<!--  </div>-->
<!--</template>-->

<!--<script setup>-->
<!--import { ref, onMounted, computed } from 'vue'-->
<!--import { useRoute } from 'vue-router'-->
<!--import { marked } from 'marked'-->
<!--import request from '@/utils/request'-->

<!--const route = useRoute()-->
<!--const loading = ref(true)-->
<!--const note = ref(null)-->
<!--const permission = ref('READ')-->

<!--const renderedHtml = computed(() => {-->
<!--  if (!note.value) return ''-->
<!--  return marked.parse(note.value.content || '')-->
<!--})-->

<!--onMounted(async () => {-->
<!--  const code = route.params.code-->
<!--  try {-->
<!--    const res = await request.get(`/share/${code}`)-->
<!--    note.value = res.note-->
<!--    permission.value = res.permission-->
<!--  } catch (error) {-->
<!--    console.error('访问分享失败', error)-->
<!--  } finally {-->
<!--    loading.value = false-->
<!--  }-->
<!--})-->
<!--</script>-->

<!--<style scoped>-->
<!--.share-view {-->
<!--  max-width: 800px;-->
<!--  margin: 0 auto;-->
<!--  padding: 20px;-->
<!--}-->
<!--</style>-->

<template>
  <div class="share-view">
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
    <div v-else-if="error" class="error-container">
      <el-icon><WarningFilled /></el-icon>
      <p>{{ error }}</p>
    </div>
    <div v-else class="content-container">
      <h1 class="share-title">{{ note.title }}</h1>
      <div class="share-meta" v-if="note.tags && note.tags.length">
        <el-tag v-for="tag in note.tags" :key="tag" size="small">{{ tag }}</el-tag>
      </div>
      <div class="markdown-body" v-html="renderedHtml"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import { ElMessage } from 'element-plus'
import { Loading, WarningFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const loading = ref(true)
const error = ref('')
const note = ref(null)

// 配置 marked（与编辑器保持一致）
marked.setOptions({
  highlight: (code, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

const renderedHtml = computed(() => {
  if (!note.value) return ''
  return marked.parse(note.value.content || '')
})

onMounted(async () => {
  const code = route.params.code
  if (!code) {
    error.value = '无效的分享码'
    loading.value = false
    return
  }
  try {
    const res = await request.get(`/share/${code}`)
    note.value = res.note
  } catch (err) {
    // 请求拦截器已经处理了错误弹窗，这里只设置本地错误信息用于显示
    error.value = err.message || '分享内容不存在或已过期'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.share-view {
  min-height: 100vh;
  background: #f8f7f4;
  padding: 40px 20px;
}

.loading-container,
.error-container {
  text-align: center;
  margin-top: 100px;
  color: #7c7a96;
}

.loading-container .el-icon {
  font-size: 32px;
  margin-bottom: 12px;
}

.error-container .el-icon {
  font-size: 48px;
  color: #f56c6c;
  margin-bottom: 16px;
}

.content-container {
  max-width: 900px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  padding: 32px 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}

.share-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 12px 0;
  letter-spacing: -0.3px;
}

.share-meta {
  margin-bottom: 24px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  border-bottom: 1px solid #f0eef8;
  padding-bottom: 16px;
}

/* Markdown 样式与编辑器预览保持一致 */
.markdown-body :deep(h1) {
  font-size: 2em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}
.markdown-body :deep(h2) {
  font-size: 1.5em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}
.markdown-body :deep(pre) {
  background: #f6f8fa;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
}
.markdown-body :deep(code) {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 85%;
}
.markdown-body :deep(blockquote) {
  border-left: 4px solid #dfe2e5;
  padding-left: 16px;
  color: #6a737d;
  margin: 0;
}
</style>
