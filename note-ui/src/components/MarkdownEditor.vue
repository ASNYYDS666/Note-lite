<template>
  <div class="md-editor">
    <div class="toolbar">
      <el-button-group>
        <el-button size="small" @click="insert('**', '**')">B</el-button>
        <el-button size="small" @click="insert('*', '*')">I</el-button>
        <el-button size="small" @click="insert('## ', '')">H</el-button>
        <el-button size="small" @click="insert('- ', '')">List</el-button>
        <el-button size="small" @click="insert('```\n', '\n```')">Code</el-button>
      </el-button-group>

      <el-button type="primary" size="small" :loading="saving" @click="save">
        保存
      </el-button>
    </div>

    <div class="editor-container">
      <textarea
          ref="textarea"
          v-model="content"
          class="edit-area"
          placeholder="输入 Markdown..."
          @input="onInput"
      />
      <div class="preview-area" v-html="renderedHtml" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const props = defineProps({
  modelValue: { type: String, default: '' },
  saving: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'save'])

const content = ref(props.modelValue)
const textarea = ref(null)

// 配置 marked
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

const renderedHtml = computed(() => marked.parse(content.value))

watch(() => props.modelValue, (val) => {
  if (val !== content.value) content.value = val
})

const onInput = () => {
  emit('update:modelValue', content.value)
}

const insert = (before, after) => {
  const el = textarea.value
  const start = el.selectionStart
  const end = el.selectionEnd
  const selected = content.value.substring(start, end)

  content.value = (
      content.value.substring(0, start) +
      before + selected + after +
      content.value.substring(end)
  )

  emit('update:modelValue', content.value)

  // 恢复光标
  setTimeout(() => {
    el.focus()
    const newPos = start + before.length + selected.length
    el.setSelectionRange(newPos, newPos)
  }, 0)
}

const save = () => {
  emit('save')
}
</script>

<style scoped>
.md-editor {
  height: calc(100vh - 200px);
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
}

.toolbar {
  padding: 10px;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  justify-content: space-between;
}

.editor-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.edit-area, .preview-area {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.6;
}

.edit-area {
  border: none;
  border-right: 1px solid #dcdfe6;
  resize: none;
  outline: none;
  background: #fafafa;
}

.preview-area :deep(h1) { font-size: 2em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
.preview-area :deep(h2) { font-size: 1.5em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
.preview-area :deep(pre) { background: #f6f8fa; padding: 16px; border-radius: 6px; overflow-x: auto; }
.preview-area :deep(code) { font-family: 'SFMono-Regular', Consolas, monospace; font-size: 85%; }
.preview-area :deep(blockquote) { border-left: 4px solid #dfe2e5; padding-left: 16px; color: #6a737d; margin: 0; }
</style>
