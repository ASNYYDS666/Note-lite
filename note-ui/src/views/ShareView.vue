<template>
  <div class="share-page">
    <!-- TopBar -->
    <header class="share-topbar glass-nav">
      <div class="topbar-left">
        <span class="topbar-brand">Note-lite</span>
      </div>
      <div class="topbar-right">
        <router-link to="/login" class="topbar-link">Sign in</router-link>
        <router-link to="/register" class="topbar-btn">Sign up</router-link>
      </div>
    </header>

    <!-- Main Content -->
    <main class="share-main">
      <!-- Left Action Rail -->
      <aside class="share-rail">
        <div class="rail-avatar">
          <span class="material-symbols-outlined">description</span>
        </div>
        <div class="rail-divider"></div>
        <button class="rail-action" @click="copyLink" title="Copy Link">
          <span class="material-symbols-outlined">link</span>
        </button>
        <button class="rail-action" title="Download">
          <span class="material-symbols-outlined">download</span>
        </button>
      </aside>

      <!-- Article -->
      <article class="share-article" v-if="!loading && !error">
        <header class="article-header">
          <div class="article-breadcrumb" v-if="note.folderId">
            <span class="material-symbols-outlined">folder_open</span>
            <span>{{ note.folderName || 'Root' }}</span>
          </div>
          <h1 class="article-title">{{ note.title }}</h1>
          <div class="article-meta">
            <div class="meta-author">
              <div class="author-avatar">
                {{ note.authorInitials || 'U' }}
              </div>
              <span>Published by {{ note.authorName || 'Anonymous' }}</span>
            </div>
            <span class="meta-dot"></span>
            <time v-if="note.createdAt">{{ formatDate(note.createdAt) }}</time>
            <span class="meta-dot"></span>
            <span>{{ readTime }} min read</span>
          </div>
        </header>

        <div class="article-body prose-editor" v-html="renderedHtml"></div>

        <!-- Tags Footer -->
        <footer class="article-footer" v-if="note.tags && note.tags.length">
          <div class="article-tags">
            <span
              v-for="tag in note.tags"
              :key="tag"
              class="article-tag"
            >#{{ tag }}</span>
          </div>
        </footer>
      </article>

      <!-- Loading -->
      <div v-else-if="loading" class="share-loading">
        <span class="material-symbols-outlined spin-icon">refresh</span>
        <p>Loading...</p>
      </div>

      <!-- Error -->
      <div v-else class="share-error">
        <span class="material-symbols-outlined error-icon">error</span>
        <p>{{ error }}</p>
      </div>
    </main>

    <!-- Page Footer -->
    <footer class="share-footer">
      <span class="footer-brand">Note-lite</span>
      <p class="footer-desc">
        The digital sanctuary for deep work and literature-focused productivity.
      </p>
      <div class="footer-links">
        <a href="#">Privacy</a>
        <a href="#">Terms</a>
        <a href="#">Twitter</a>
      </div>
      <p class="footer-copy">&copy; 2024 NOTE-LITE. ALL RIGHTS RESERVED.</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref('')
const note = ref({})

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
  if (!note.value.content) return ''
  return DOMPurify.sanitize(marked.parse(note.value.content))
})

const readTime = computed(() => {
  if (!note.value.content) return 1
  const words = note.value.content.replace(/[#*`\[\]()]/g, '').split(/\s+/).length
  return Math.max(1, Math.ceil(words / 200))
})

function formatDate(dateStr) {
  const d = new Date(dateStr)
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

function copyLink() {
  navigator.clipboard.writeText(window.location.href).then(() => {
    ElMessage.success('Link copied')
  }).catch(() => {
    ElMessage.error('Failed to copy')
  })
}

onMounted(async () => {
  const code = route.params.code
  if (!code) {
    error.value = '无效的分享码'
    loading.value = false
    return
  }
  try {
    const res = await request.get(`/share/${code}`)
    const n = res.note || {}
    note.value = {
      ...n,
      authorInitials: n.authorName ? n.authorName.charAt(0).toUpperCase() : 'U',
      authorName: n.authorName || 'Anonymous',
      folderName: n.folderName || ''
    }
  } catch (err) {
    if (err.code === 30001) {
      error.value = '分享的笔记已被作者删除'
    } else if (err.code === 30002) {
      error.value = '该分享链接已过期'
    } else {
      error.value = err.message || '分享内容不存在或已过期'
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.share-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  font-family: var(--font-ui);
  color: var(--on-surface);
}

/* ===== TopBar ===== */
.share-topbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: var(--top-bar-height);
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--container-margin);
  background: var(--bg);
  border-bottom: 1px solid var(--border-subtle);
}

.glass-nav {
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.topbar-brand {
  font-family: var(--font-editor);
  font-size: 24px;
  font-weight: 700;
  color: var(--primary);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.topbar-link {
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
  text-decoration: none;
  padding: 4px 12px;
  transition: color 0.15s;
}

.topbar-link:hover {
  color: var(--on-surface);
}

.topbar-btn {
  font-size: var(--text-ui-sm);
  padding: 4px 12px;
  background: var(--primary);
  color: var(--on-primary);
  border-radius: var(--radius-lg);
  text-decoration: none;
  transition: opacity 0.15s;
}

.topbar-btn:hover {
  opacity: 0.9;
}

/* ===== Main ===== */
.share-main {
  flex: 1;
  padding: calc(var(--top-bar-height) + 32px) var(--container-margin) 96px;
  display: flex;
  justify-content: center;
  position: relative;
}

/* ===== Action Rail ===== */
.share-rail {
  display: none;
  position: fixed;
  left: 48px;
  top: 96px;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  width: 48px;
}

@media (min-width: 1024px) {
  .share-rail {
    display: flex;
  }
}

.rail-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--secondary-container);
  color: var(--on-secondary-container);
  display: flex;
  align-items: center;
  justify-content: center;
}

.rail-avatar .material-symbols-outlined {
  font-size: 20px;
}

.rail-divider {
  width: 1px;
  height: 48px;
  background: var(--outline-variant);
}

.rail-action {
  padding: 8px;
  border: none;
  border-radius: var(--radius-default);
  background: none;
  color: var(--on-surface-variant);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s;
}

.rail-action:hover {
  color: var(--on-surface);
}

/* ===== Article ===== */
.share-article {
  width: 100%;
  max-width: 800px;
  padding: 48px 32px;
  background: var(--surface-container-lowest);
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-subtle);
  box-shadow: var(--shadow-elevated);
}

@media (min-width: 640px) {
  .share-article {
    padding: 64px;
  }
}

.article-header {
  margin-bottom: 48px;
  padding-bottom: 32px;
  border-bottom: 1px solid var(--outline-variant);
}

.article-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--text-ui-sm);
  color: var(--accent-sage);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 600;
  margin-bottom: 16px;
}

.article-breadcrumb .material-symbols-outlined {
  font-size: 14px;
}

.article-title {
  font-family: var(--font-editor);
  font-size: var(--text-headline-lg);
  font-weight: 700;
  color: var(--on-surface);
  margin: 0 0 16px;
  line-height: 1.2;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
}

.meta-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--primary);
  color: var(--on-primary);
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.meta-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--outline-variant);
}

/* ===== Article Body ===== */
.article-body {
  font-family: var(--font-editor);
  font-size: var(--text-editor-body);
  line-height: 1.6;
  color: var(--on-surface);
}

.article-body :deep(h2) {
  font-family: var(--font-editor);
  font-weight: 600;
  font-size: 28px;
  color: var(--on-surface);
  margin: 32px 0 16px;
}

.article-body :deep(p) {
  margin-bottom: 20px;
}

.article-body :deep(blockquote) {
  border-left: 3px solid var(--accent-sage);
  padding-left: 24px;
  font-style: italic;
  color: var(--on-surface-variant);
  margin: 32px 0;
}

.article-body :deep(pre) {
  background: var(--primary-container);
  color: var(--primary-fixed);
  padding: 16px;
  border-radius: var(--radius-default);
  overflow-x: auto;
  font-family: monospace;
  font-size: 14px;
  margin: 24px 0;
}

.dark .article-body :deep(pre) {
  background: #1a1c1b;
  color: var(--primary-fixed-dim);
}

.article-body :deep(code) {
  font-family: monospace;
  font-size: 90%;
  background: var(--surface-container);
  padding: 2px 6px;
  border-radius: 2px;
  color: var(--accent-sage);
}

.article-body :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

.article-body :deep(img) {
  max-width: 100%;
  border-radius: var(--radius-lg);
  margin: 32px 0;
}

/* ===== Article Footer ===== */
.article-footer {
  margin-top: 64px;
  padding-top: 32px;
  border-top: 1px solid var(--outline-variant);
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.article-tag {
  padding: 4px 12px;
  background: var(--surface-container);
  border-radius: var(--radius-full);
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
}

/* ===== Loading / Error ===== */
.share-loading,
.share-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 100px 20px;
  color: var(--on-surface-variant);
}

.spin-icon {
  font-size: 32px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.error-icon {
  font-size: 48px;
  color: var(--error-red);
}

/* ===== Page Footer ===== */
.share-footer {
  padding: 48px var(--container-margin);
  border-top: 1px solid var(--border-subtle);
  background: var(--surface-container-low);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

.footer-brand {
  font-family: var(--font-editor);
  font-size: var(--text-headline-md);
  font-weight: 700;
  color: var(--on-surface);
  opacity: 0.5;
}

.footer-desc {
  max-width: 400px;
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
  margin: 0;
}

.footer-links {
  display: flex;
  gap: 32px;
  margin-top: 8px;
}

.footer-links a {
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
  text-decoration: none;
  transition: color 0.15s;
}

.footer-links a:hover {
  color: var(--on-surface);
}

.footer-copy {
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--outline-variant);
  margin-top: 16px;
}
</style>
