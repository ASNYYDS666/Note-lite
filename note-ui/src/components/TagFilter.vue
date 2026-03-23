//day05新建标签筛选组件
<template>
  <div class="tag-filter">
    <el-select
        v-model="selectedTags"
        multiple
        collapse-tags
        collapse-tags-tooltip
        placeholder="按标签筛选"
        clearable
        @change="handleChange"
    >
      <el-option
          v-for="tag in allTags"
          :key="tag"
          :label="tag"
          :value="tag"
      />
    </el-select>

    <el-radio-group v-model="matchMode" size="small" @change="handleChange">
      <el-radio-button label="ANY">任意标签</el-radio-button>
      <el-radio-button label="ALL">全部标签</el-radio-button>
    </el-radio-group>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  matchMode: {
    type: String,
    default: 'ANY'
  },
  allTags: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'update:matchMode', 'change'])

const selectedTags = ref(props.modelValue)
const matchMode = ref(props.matchMode)

watch(() => props.modelValue, (val) => {
  selectedTags.value = val
})

watch(() => props.matchMode, (val) => {
  matchMode.value = val
})

const handleChange = () => {
  emit('update:modelValue', selectedTags.value)
  emit('update:matchMode', matchMode.value)
  emit('change', {
    tags: selectedTags.value,
    matchMode: matchMode.value
  })
}
</script>

<style scoped>
.tag-filter {
  display: flex;
  gap: 10px;
  align-items: center;
}

.tag-filter .el-select {
  width: 200px;
}
</style>