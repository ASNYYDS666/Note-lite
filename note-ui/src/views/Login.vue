<template>
  <div class="login-page">
    <!-- Background atmospheric effect -->
    <div class="atmosphere">
      <div class="blur-circle blur-1"></div>
      <div class="blur-circle blur-2"></div>
    </div>

    <main class="login-main">
      <!-- Left: Brand Section -->
      <section class="brand-section">
        <div class="geometric-motif">
          <svg width="100%" height="100%" preserveAspectRatio="none" viewBox="0 0 100 100">
            <path d="M0 0 L100 100 M100 0 L0 100" stroke="currentColor" stroke-width="0.1" />
            <circle cx="50" cy="50" r="40" fill="none" stroke="currentColor" stroke-width="0.1" />
          </svg>
        </div>

        <div class="brand-content">
          <div class="brand-logo">
            <div class="logo-icon">
              <span class="material-symbols-outlined">description</span>
            </div>
            <span class="brand-name">Note-lite</span>
          </div>

          <h1 class="brand-headline">为深度工作而生的<br>数字圣殿。</h1>
          <p class="brand-desc">
            精雕细琢的高易读性写作环境，专为清晰与专注而设计。原生支持 Markdown，AI 知识库问答，美学自律。
          </p>

          <div class="brand-info">
            <div class="info-item">
              <span class="info-label">工作区</span>
              <span class="info-value">v2.4.0 Studio</span>
            </div>
            <div class="info-item">
              <span class="info-label">加密方式</span>
              <span class="info-value">AES-256 Bit</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Right: Form Section -->
      <section class="form-section">
        <div class="form-wrapper">
          <div class="form-header">
            <h2 class="form-title">欢迎回来</h2>
            <p class="form-subtitle">请输入您的凭据以访问您的数字圣殿。</p>
          </div>

          <form class="login-form" @submit.prevent="handleLogin">
            <div class="input-group">
              <label class="input-label" for="username">用户名 / 邮箱</label>
              <div class="input-row">
                <span class="material-symbols-outlined input-icon">person</span>
                <input
                  id="username"
                  v-model="form.username"
                  class="auth-input"
                  type="text"
                  placeholder="请输入用户名"
                  autocomplete="username"
                />
              </div>
            </div>

            <div class="input-group">
              <div class="label-row">
                <label class="input-label" for="password">密码</label>
                <a class="forgot-link" href="#">忘记密码？</a>
              </div>
              <div class="input-row">
                <span class="material-symbols-outlined input-icon">lock</span>
                <input
                  id="password"
                  v-model="form.password"
                  class="auth-input"
                  type="password"
                  placeholder="••••••••"
                  autocomplete="current-password"
                />
              </div>
            </div>

            <button
              class="submit-btn"
              type="submit"
              :disabled="loading"
              :class="{ loading: loading }"
            >
              <span v-if="!loading">登录</span>
              <span v-else class="loading-text">
                <span class="material-symbols-outlined spin-icon">progress_activity</span>
              </span>
            </button>
          </form>

          <div class="form-footer-link">
            <span>还没有账号？</span>
            <router-link to="/register">点击注册</router-link>
          </div>
        </div>

        <footer class="form-footer">
          <div class="footer-left">
            <span class="footer-text">v2.4.0-stable</span>
            <span class="footer-text status-text">
              <span class="status-dot"></span>
              系统运行正常
            </span>
          </div>
          <div class="footer-right">
            <a class="footer-text" href="#">安全</a>
            <a class="footer-text" href="#">隐私</a>
          </div>
        </footer>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

function validate() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入用户名')
    return false
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return false
  }
  return true
}

async function handleLogin() {
  if (!validate()) return

  try {
    loading.value = true
    const res = await request.post('/user/login', {
      username: form.username,
      password: form.password
    })
    userStore.setToken(res.token)
    userStore.setUserInfo({
      userId: res.userId,
      username: res.username
    })
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  overflow: hidden;
  position: relative;
  background: var(--bg);
  font-family: var(--font-ui);
}

/* ===== Atmosphere ===== */
.atmosphere {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.blur-circle {
  position: absolute;
  border-radius: 50%;
}

.blur-1 {
  width: 40vw;
  height: 40vw;
  top: -10%;
  left: -10%;
  background: var(--secondary-container);
  filter: blur(120px);
  opacity: 0.4;
}

.blur-2 {
  width: 50vw;
  height: 50vw;
  bottom: -10%;
  right: -10%;
  background: var(--accent-sage);
  filter: blur(150px);
  opacity: 0.1;
}

/* ===== Main Layout ===== */
.login-main {
  position: relative;
  z-index: 10;
  display: flex;
  height: 100%;
  background: var(--bg);
}

/* ===== Left Brand Section ===== */
.brand-section {
  display: none;
  width: 50%;
  height: 100%;
  flex-direction: column;
  justify-content: center;
  padding: 0 96px;
  background: var(--surface-container-lowest);
  border-right: 1px solid var(--border-subtle);
  position: relative;
  overflow: hidden;
}

@media (min-width: 768px) {
  .brand-section {
    display: flex;
  }
}

.geometric-motif {
  position: absolute;
  top: 0;
  right: 0;
  width: 100%;
  height: 100%;
  opacity: 0.03;
  pointer-events: none;
  color: var(--primary);
}

.brand-content {
  position: relative;
  z-index: 1;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 48px;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: var(--primary);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-icon .material-symbols-outlined {
  font-size: 20px;
  color: var(--on-primary);
}

.brand-name {
  font-family: var(--font-editor);
  font-size: var(--text-headline-md);
  font-weight: 600;
  color: var(--on-surface);
  letter-spacing: -0.3px;
}

.brand-headline {
  font-family: var(--font-editor);
  font-size: var(--text-headline-lg);
  font-weight: 700;
  font-style: italic;
  color: var(--on-surface);
  line-height: 1.2;
  margin: 0 0 24px;
  max-width: 420px;
}

.brand-desc {
  font-family: var(--font-ui);
  font-size: var(--text-editor-body);
  color: var(--on-surface-variant);
  line-height: 1.6;
  margin: 0 0 48px;
  max-width: 360px;
}

.brand-info {
  display: flex;
  gap: 48px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-label {
  font-family: var(--font-ui);
  font-size: var(--text-ui-label);
  font-weight: 600;
  color: var(--on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.info-value {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--accent-sage);
}

/* ===== Right Form Section ===== */
.form-section {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 80px 32px 0 32px;
  background: var(--bg);
}

@media (min-width: 768px) {
  .form-section {
    padding: 80px 96px 0 96px;
  }
}

.form-wrapper {
  width: 100%;
  max-width: 360px;
  margin: auto auto;
}

.form-header {
  margin-bottom: 40px;
}

.form-title {
  font-family: var(--font-editor);
  font-size: var(--text-headline-md);
  font-weight: 600;
  color: var(--on-surface);
  margin: 0 0 8px;
}

.form-subtitle {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
  margin: 0;
}

/* ===== Input Fields ===== */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.input-group {
  display: flex;
  flex-direction: column;
}

.input-label {
  font-family: var(--font-ui);
  font-size: var(--text-ui-label);
  font-weight: 600;
  color: var(--on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 8px;
  transition: color 0.2s;
}

.input-group:focus-within .input-label {
  color: var(--accent-sage);
}

.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.forgot-link {
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--accent-sage);
  text-decoration: none;
}

.forgot-link:hover {
  text-decoration: underline;
}

.input-row {
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--border-default);
  transition: border-color 0.2s;
}

.input-group:focus-within .input-row {
  border-bottom-color: var(--accent-sage);
}

.input-icon {
  font-size: 18px;
  color: var(--on-surface-variant);
  flex-shrink: 0;
  transition: color 0.2s;
}

.input-group:focus-within .input-icon {
  color: var(--accent-sage);
}

.auth-input {
  flex: 1;
  border: none;
  background: none;
  padding: 10px 0;
  font-family: var(--font-ui);
  font-size: var(--text-ui-base);
  color: var(--on-surface);
  outline: none;
}

.auth-input::placeholder {
  color: var(--outline-variant);
}

/* ===== Submit Button ===== */
.submit-btn {
  width: 100%;
  padding: 12px;
  background: var(--accent-sage);
  color: var(--on-secondary);
  border: none;
  border-radius: var(--radius-sm);
  font-family: var(--font-ui);
  font-size: var(--text-ui-label);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: background 0.2s, transform 0.15s;
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
}

.submit-btn:hover:not(:disabled) {
  background: var(--accent-sage-dark);
}

.submit-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.loading-text {
  display: flex;
  align-items: center;
  justify-content: center;
}

.spin-icon {
  animation: spin 1s linear infinite;
  font-size: 20px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ===== Footer Link ===== */
.form-footer-link {
  text-align: center;
  margin-top: 48px;
  font-family: var(--font-ui);
  font-size: var(--text-ui-sm);
  color: var(--on-surface-variant);
}

.form-footer-link a {
  color: var(--accent-sage);
  font-weight: 500;
  text-decoration: none;
  margin-left: 4px;
}

.form-footer-link a:hover {
  text-decoration: underline;
}

/* ===== Bottom Footer ===== */
.form-footer {
  margin-top: auto;
  padding-top: 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 360px;
  margin-left: auto;
  margin-right: auto;
  width: 100%;
  opacity: 0.4;
  transition: opacity 0.3s;
}

.form-footer:hover {
  opacity: 1;
}

.footer-left,
.footer-right {
  display: flex;
  gap: 16px;
}

.footer-text {
  font-family: var(--font-ui);
  font-size: var(--text-status-bar);
  color: var(--on-surface-variant);
  text-decoration: none;
}

a.footer-text:hover {
  color: var(--accent-sage);
}

.status-text {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--success-green);
}
</style>
