<!--&lt;!&ndash;原版登录页面&ndash;&gt;-->
<!--<template>-->
<!--  <div class="login-container">-->
<!--    <el-card class="login-box">-->
<!--      <h2 class="title">Note-lite 登录</h2>-->

<!--      <el-form-->
<!--          ref="formRef"-->
<!--          :model="form"-->
<!--          :rules="rules"-->
<!--          @keyup.enter="handleLogin"-->
<!--      >-->
<!--        <el-form-item prop="username">-->
<!--          <el-input-->
<!--              v-model="form.username"-->
<!--              placeholder="用户名"-->
<!--              :prefix-icon="User"-->
<!--              size="large"-->
<!--          />-->
<!--        </el-form-item>-->

<!--        <el-form-item prop="password">-->
<!--          <el-input-->
<!--              v-model="form.password"-->
<!--              type="password"-->
<!--              placeholder="密码"-->
<!--              :prefix-icon="Lock"-->
<!--              size="large"-->
<!--              show-password-->
<!--          />-->
<!--        </el-form-item>-->

<!--        <el-button-->
<!--            type="primary"-->
<!--            size="large"-->
<!--            :loading="loading"-->
<!--            @click="handleLogin"-->
<!--            style="width: 100%"-->
<!--        >-->
<!--          登 录-->
<!--        </el-button>-->

<!--        <div class="actions">-->
<!--          <router-link to="/register">注册新账号</router-link>-->
<!--        </div>-->
<!--      </el-form>-->
<!--    </el-card>-->
<!--  </div>-->
<!--</template>-->

<!--<script setup>-->
<!--import { ref, reactive } from 'vue'-->
<!--import { useRouter } from 'vue-router'-->
<!--import { User, Lock } from '@element-plus/icons-vue'-->
<!--import { useUserStore } from '@/store/user'-->
<!--import request from '@/utils/request'-->
<!--import { ElMessage } from 'element-plus'-->

<!--const router = useRouter()-->
<!--const userStore = useUserStore()-->
<!--const formRef = ref()-->
<!--const loading = ref(false)-->

<!--const form = reactive({-->
<!--  username: '',-->
<!--  password: ''-->
<!--})-->

<!--const rules = {-->
<!--  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],-->
<!--  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]-->
<!--}-->

<!--const handleLogin = async () => {-->
<!--  try {-->
<!--    await formRef.value.validate()-->
<!--    loading.value = true-->

<!--    const res = await request.post('/ums/login', {-->
<!--      username: form.username,-->
<!--      password: form.password-->
<!--    })-->

<!--    // 假设登录返回：{ token, userId, username }-->
<!--    userStore.setToken(res.token)-->
<!--    userStore.setUserInfo({-->
<!--      userId: res.userId,-->
<!--      username: res.username-->
<!--    })-->

<!--    ElMessage.success('登录成功')-->
<!--    router.push('/')-->
<!--  } catch (error) {-->
<!--    // 错误已在拦截器提示-->
<!--    console.error('登录失败:', error)-->
<!--  } finally {-->
<!--    loading.value = false-->
<!--  }-->
<!--}-->
<!--</script>-->

<!--<style scoped>-->
<!--.login-container {-->
<!--  height: 100vh;-->
<!--  display: flex;-->
<!--  justify-content: center;-->
<!--  align-items: center;-->
<!--  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);-->
<!--}-->

<!--.login-box {-->
<!--  width: 400px;-->
<!--  padding: 20px;-->
<!--}-->

<!--.title {-->
<!--  text-align: center;-->
<!--  margin-bottom: 30px;-->
<!--  color: #333;-->
<!--}-->

<!--.actions {-->
<!--  margin-top: 20px;-->
<!--  text-align: center;-->
<!--}-->
<!--</style>-->

<template>
  <div class="login-page">
    <!-- 左侧装饰区 -->
    <div class="left-panel">
      <div class="brand">
        <div class="logo-mark">
          <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect x="4" y="4" width="24" height="24" rx="6" fill="white" fill-opacity="0.15"/>
            <rect x="4" y="4" width="24" height="24" rx="6" stroke="white" stroke-opacity="0.4" stroke-width="1"/>
            <line x1="10" y1="12" x2="22" y2="12" stroke="white" stroke-width="1.8" stroke-linecap="round"/>
            <line x1="10" y1="17" x2="19" y2="17" stroke="white" stroke-width="1.8" stroke-linecap="round"/>
            <line x1="10" y1="22" x2="15" y2="22" stroke="white" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </div>
        <span class="brand-name">Note-lite</span>
      </div>

      <div class="left-content">
        <h1 class="left-title">记录每一个<br>灵感瞬间</h1>
        <p class="left-desc">支持 Markdown 编辑、标签分类、草稿自动保存，让思考有处安放。</p>

        <div class="feature-list">
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>Markdown 实时预览</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>标签筛选与管理</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>草稿自动保存</span>
          </div>
        </div>
      </div>

      <!-- 背景装饰圆 -->
      <div class="deco-circle deco-1"></div>
      <div class="deco-circle deco-2"></div>
    </div>

    <!-- 右侧表单区 -->
    <div class="right-panel">
      <div class="form-container">
        <div class="form-header">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">登录以继续使用</p>
        </div>

        <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            class="login-form"
            @keyup.enter="handleLogin"
        >
          <div class="field-group">
            <label class="field-label">用户名</label>
            <el-form-item prop="username">
              <el-input
                  v-model="form.username"
                  placeholder="请输入用户名"
                  size="large"
                  class="custom-input"
              >
                <template #prefix>
                  <el-icon class="input-icon"><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </div>

          <div class="field-group">
            <div class="label-row">
              <label class="field-label">密码</label>
            </div>
            <el-form-item prop="password">
              <el-input
                  v-model="form.password"
                  type="password"
                  placeholder="请输入密码"
                  size="large"
                  show-password
                  class="custom-input"
              >
                <template #prefix>
                  <el-icon class="input-icon"><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </div>

          <button
              class="login-btn"
              :class="{ loading: loading }"
              :disabled="loading"
              @click.prevent="handleLogin"
          >
            <span v-if="!loading">登录</span>
            <span v-else class="loading-content">
              <svg class="spin" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-opacity="0.3" stroke-width="2.5"/>
                <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
              </svg>
              登录中...
            </span>
          </button>
        </el-form>

        <div class="form-footer">
          <span class="footer-text">还没有账号？</span>
          <router-link to="/register" class="register-link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  try {
    await formRef.value.validate()
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
  display: flex;
  height: 100vh;
  background: #f8f7f4;
  font-family: 'PingFang SC', 'Hiragino Sans GB', sans-serif;
}

/* ===== 左侧装饰区 ===== */
.left-panel {
  width: 45%;
  background: linear-gradient(150deg, #4a42b0 0%, #6b5ce7 50%, #8b7cf8 100%);
  display: flex;
  flex-direction: column;
  padding: 40px 48px;
  position: relative;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
  z-index: 1;
}

.logo-mark {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
}

.logo-mark svg {
  width: 100%;
  height: 100%;
}

.brand-name {
  font-size: 20px;
  font-weight: 600;
  color: white;
  letter-spacing: 0.5px;
}

.left-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  z-index: 1;
  padding-bottom: 40px;
}

.left-title {
  font-size: 38px;
  font-weight: 700;
  color: white;
  line-height: 1.25;
  margin: 0 0 20px 0;
  letter-spacing: -0.5px;
}

.left-desc {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.75);
  line-height: 1.7;
  margin: 0 0 36px 0;
  max-width: 320px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.7);
  flex-shrink: 0;
}

/* 装饰圆 */
.deco-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  pointer-events: none;
}

.deco-1 {
  width: 360px;
  height: 360px;
  bottom: -120px;
  right: -100px;
}

.deco-2 {
  width: 200px;
  height: 200px;
  top: 80px;
  right: 40px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* ===== 右侧表单区 ===== */
.right-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #f8f7f4;
}

.form-container {
  width: 100%;
  max-width: 380px;
}

.form-header {
  margin-bottom: 36px;
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.form-subtitle {
  font-size: 15px;
  color: #8b8fa8;
  margin: 0;
}

/* 表单字段 */
.login-form {
  margin-bottom: 24px;
}

.field-group {
  margin-bottom: 20px;
}

.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.field-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #4a4a6a;
  margin-bottom: 8px;
}

/* 覆盖 Element Plus input 样式 */
.custom-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  border: 1.5px solid #e8e6f0;
  background: white;
  box-shadow: none !important;
  padding: 4px 14px;
  transition: border-color 0.2s;
}

.custom-input :deep(.el-input__wrapper:hover) {
  border-color: #c5bff0;
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  border-color: #6b5ce7;
  box-shadow: 0 0 0 3px rgba(107, 92, 231, 0.1) !important;
}

.custom-input :deep(.el-input__inner) {
  font-size: 14px;
  color: #1a1a2e;
  height: 40px;
}

.custom-input :deep(.el-input__inner::placeholder) {
  color: #c0bdd4;
}

.input-icon {
  color: #b0aec8;
  font-size: 15px;
}

/* 取消 el-form-item 默认 margin */
.field-group :deep(.el-form-item) {
  margin-bottom: 0;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(135deg, #5a50d8 0%, #7b6ef0 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  transition: opacity 0.2s, transform 0.15s;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-btn:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.loading-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spin {
  width: 18px;
  height: 18px;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 底部链接 */
.form-footer {
  text-align: center;
}

.footer-text {
  font-size: 14px;
  color: #8b8fa8;
}

.register-link {
  font-size: 14px;
  color: #6b5ce7;
  font-weight: 500;
  text-decoration: none;
  margin-left: 4px;
  transition: opacity 0.15s;
}

.register-link:hover {
  opacity: 0.75;
}

/* 响应式：小屏隐藏左侧 */
@media (max-width: 768px) {
  .left-panel {
    display: none;
  }
  .right-panel {
    padding: 24px;
  }
}
</style>
