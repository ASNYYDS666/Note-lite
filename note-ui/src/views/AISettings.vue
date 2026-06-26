<template>
  <div class="ai-settings">
    <h2>AI 设置</h2>

    <!-- Chat 配置 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>Chat 对话配置</span>
          <div class="card-actions">
            <el-button size="small" type="success" @click="testChat" :loading="testingChat">
              测试连接
            </el-button>
          </div>
        </div>
      </template>
      <el-form :model="form" label-width="130px" style="max-width: 560px">
        <el-form-item label="服务商">
          <el-select v-model="form.chatProvider">
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="OpenAI" value="openai" />
          </el-select>
        </el-form-item>

        <el-form-item label="API Key">
          <el-input v-model="form.chatApiKey" type="password" show-password placeholder="sk-xxxxxxxx" />
        </el-form-item>

        <el-form-item label="模型">
          <el-input v-model="form.chatModel" placeholder="deepseek-chat" />
        </el-form-item>

        <el-form-item label="自定义 API 地址">
          <el-input v-model="form.chatUrl" placeholder="留空则用官方地址，中转站填此" />
        </el-form-item>
      </el-form>

      <div v-if="chatResult" class="test-result" :class="chatResult.success ? 'ok' : 'fail'">
        <span v-if="chatResult.success">✅ Chat 连接成功 | 模型: {{ chatResult.model }} | 延迟: {{ chatResult.latencyMs }}ms</span>
        <span v-else>❌ {{ chatResult.error }}</span>
      </div>
    </el-card>

    <!-- Embedding 配置 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>Embedding 向量化配置</span>
          <div class="card-actions">
            <el-button size="small" type="success" @click="testEmbed" :loading="testingEmbed">
              测试连接
            </el-button>
          </div>
        </div>
      </template>
      <el-form :model="form" label-width="130px" style="max-width: 560px">
        <el-form-item label="服务商">
          <el-select v-model="form.embedProvider">
            <el-option label="OpenAI" value="openai" />
            <el-option label="DeepSeek" value="deepseek" />
          </el-select>
        </el-form-item>

        <el-form-item label="API Key">
          <el-input v-model="form.embedApiKey" type="password" show-password placeholder="sk-xxxxxxxx" />
        </el-form-item>

        <el-form-item label="模型">
          <el-input v-model="form.embedModel" placeholder="openai 用 text-embedding-3-small；deepseek 同 key 即可" />
        </el-form-item>

        <el-form-item label="自定义 API 地址">
          <el-input v-model="form.embedUrl" placeholder="留空则用官方地址，中转站填此" />
        </el-form-item>
      </el-form>

      <div v-if="embedResult" class="test-result" :class="embedResult.success ? 'ok' : 'fail'">
        <span v-if="embedResult.success">✅ Embedding 连接成功 | 模型: {{ embedResult.model }} | 维度: {{ embedResult.dimension }} | 延迟: {{ embedResult.latencyMs }}ms</span>
        <span v-else>❌ {{ embedResult.error }}</span>
      </div>
    </el-card>

    <!-- 操作按钮 -->
    <div class="form-actions">
      <el-button type="primary" size="large" @click="save" :loading="saving">保存配置</el-button>
      <el-button type="danger" size="large" @click="remove" :loading="deleting" plain>删除配置</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAIConfig, saveAIConfig, deleteAIConfig, testChatConnection, testEmbedConnection } from '@/api/aiConfig'

const saving = ref(false)
const deleting = ref(false)
const testingChat = ref(false)
const testingEmbed = ref(false)
const chatResult = ref(null)
const embedResult = ref(null)

const form = reactive({
  chatProvider: 'deepseek',
  chatApiKey: '',
  chatModel: 'deepseek-chat',
  chatUrl: '',
  embedProvider: 'openai',
  embedApiKey: '',
  embedModel: 'text-embedding-3-small',
  embedUrl: '',
})

onMounted(async () => {
  try {
    const data = await getAIConfig()
    if (data) {
      form.chatProvider = data.chatProvider || 'deepseek'
      form.chatModel = data.chatModel || 'deepseek-chat'
      form.chatUrl = data.chatUrl || ''
      form.embedProvider = data.embedProvider || 'openai'
      form.embedModel = data.embedModel || 'text-embedding-3-small'
      form.embedUrl = data.embedUrl || ''
    }
  } catch {
    // 未配置过
  }
})

async function save() {
  if (!form.chatApiKey && !form.embedApiKey) {
    ElMessage.warning('请至少填入一个 API Key')
    return
  }
  saving.value = true
  chatResult.value = null
  embedResult.value = null
  try {
    await saveAIConfig({
      chatProvider: form.chatProvider,
      chatApiKey: form.chatApiKey || '',
      chatModel: form.chatModel,
      chatUrl: form.chatUrl || null,
      embedProvider: form.embedProvider,
      embedApiKey: form.embedApiKey || '',
      embedModel: form.embedModel,
      embedUrl: form.embedUrl || null,
    })
    ElMessage.success('AI 配置保存成功')
    form.chatApiKey = ''
    form.embedApiKey = ''
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function testChat() {
  if (!form.chatApiKey) {
    ElMessage.warning('请先填入 Chat API Key')
    return
  }
  testingChat.value = true
  chatResult.value = null
  try {
    // 先保存再测试
    await saveAIConfig({
      chatProvider: form.chatProvider,
      chatApiKey: form.chatApiKey,
      chatModel: form.chatModel,
      chatUrl: form.chatUrl || null,
      embedProvider: form.embedProvider,
      embedApiKey: form.embedApiKey || '',
      embedModel: form.embedModel,
      embedUrl: form.embedUrl || null,
    })
    const res = await testChatConnection()
    chatResult.value = { success: true, model: res.model, latencyMs: res.latencyMs }
    ElMessage.success('Chat 连接成功')
  } catch (e) {
    chatResult.value = { success: false, error: e.message || '连接失败' }
    ElMessage.error('Chat 连接失败')
  } finally {
    testingChat.value = false
  }
}

async function testEmbed() {
  if (!form.embedApiKey) {
    ElMessage.warning('请先填入 Embedding API Key')
    return
  }
  testingEmbed.value = true
  embedResult.value = null
  try {
    // 先保存再测试
    await saveAIConfig({
      chatProvider: form.chatProvider,
      chatApiKey: form.chatApiKey || '',
      chatModel: form.chatModel,
      chatUrl: form.chatUrl || null,
      embedProvider: form.embedProvider,
      embedApiKey: form.embedApiKey,
      embedModel: form.embedModel,
      embedUrl: form.embedUrl || null,
    })
    const res = await testEmbedConnection()
    embedResult.value = { success: true, model: res.model, dimension: res.dimension, latencyMs: res.latencyMs }
    ElMessage.success(`Embedding 连接成功，向量维度 ${res.dimension}`)
  } catch (e) {
    embedResult.value = { success: false, error: e.message || '连接失败' }
    ElMessage.error('Embedding 连接失败')
  } finally {
    testingEmbed.value = false
  }
}

async function remove() {
  try {
    await ElMessageBox.confirm('确定要删除 AI 配置吗？删除后无法使用 AI 对话功能。', '确认删除', {
      type: 'warning',
    })
  } catch {
    return
  }
  deleting.value = true
  try {
    await deleteAIConfig()
    form.chatApiKey = ''
    form.embedApiKey = ''
    chatResult.value = null
    embedResult.value = null
    ElMessage.success('AI 配置已删除')
  } catch {
    ElMessage.error('删除失败')
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.ai-settings {
  max-width: 640px;
  margin: 0 auto;
}

.ai-settings h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 20px;
}

.config-card {
  margin-bottom: 18px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header span {
  font-size: 15px;
  font-weight: 600;
}

.card-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.test-result {
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
}

.test-result.ok {
  background: #edf7f2;
  color: #2e7d5a;
  border: 1px solid #c6e5d4;
}

.test-result.fail {
  background: #fff0f0;
  color: #c0392b;
  border: 1px solid #f5c6cb;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}
</style>
