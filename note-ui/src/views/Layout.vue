<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="logo">Note-lite</div>
      <div class="user-info">
        <span>{{ userStore.userInfo?.username }}</span>
        <el-button type="text" @click="logout">退出</el-button>
      </div>
    </el-header>

    <el-container>
      <el-aside width="200px" class="aside">
        <el-menu
            :router="true"
            :default-active="$route.path"
            class="menu"
        >
          <el-menu-item index="/">
            <el-icon><Document /></el-icon>
            <span>我的笔记</span>
          </el-menu-item>
          <el-menu-item index="/recycle">  <!-- day05新增 -->
            <el-icon><Delete /></el-icon>
            <span>回收站</span>
          </el-menu-item>
          <el-menu-item index="/note/new">
            <el-icon><Plus /></el-icon>
            <span>新建笔记</span>
          </el-menu-item>
          <el-menu-item index="/ai-settings">
            <el-icon><Setting /></el-icon>
            <span>AI 设置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import { Document, Plus, Delete, Setting } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'


const userStore = useUserStore()
const router = useRouter()

const logout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.aside {
  background: #fff;
  border-right: 1px solid #dcdfe6;
}

.menu {
  border-right: none;
}

.main {
  background: #f5f7fa;
  padding: 20px;
}
</style>
