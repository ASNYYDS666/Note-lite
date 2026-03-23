<template>
  <div class="login-container">
    <el-card class="login-box">
      <h2 class="title">Note-lite 登录</h2>

      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
              v-model="form.username"
              placeholder="用户名"
              :prefix-icon="User"
              size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              size="large"
              show-password
          />
        </el-form-item>

        <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            style="width: 100%"
        >
          登 录
        </el-button>

        <div class="actions">
          <router-link to="/register">注册新账号</router-link>
        </div>
      </el-form>
    </el-card>
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

    const res = await request.post('/ums/login', {
      username: form.username,
      password: form.password
    })

    // 假设登录返回：{ token, userId, username }
    userStore.setToken(res.token)
    userStore.setUserInfo({
      userId: res.userId,
      username: res.username
    })

    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    // 错误已在拦截器提示
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 20px;
}

.title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.actions {
  margin-top: 20px;
  text-align: center;
}
</style>