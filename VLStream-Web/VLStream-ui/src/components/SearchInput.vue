<template>
  <div class="search-input-component">
    <div class="search-row">
      <el-input
        v-model="searchValue"
        :placeholder="placeholder"
        clearable
        :size="size"
        class="search-input"
        @keyup.enter="handleSearch"
      />
      <el-button 
        type="primary" 
        :size="size" 
        class="search-btn" 
        @click="handleSearch"
      >
        {{ buttonText }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

// Props定义
const props = defineProps({
  // 搜索值
  modelValue: {
    type: String,
    default: ''
  },
  // 占位符文本
  placeholder: {
    type: String,
    default: '搜索'
  },
  // 按钮文本
  buttonText: {
    type: String,
    default: '查询'
  },
  // 组件大小
  size: {
    type: String,
    default: 'small',
    validator: (value) => ['large', 'default', 'small'].includes(value)
  },
  // 输入框宽度
  width: {
    type: String,
    default: '200px'
  }
})

// Emits定义
const emit = defineEmits(['update:modelValue', 'search', 'clear'])

// 响应式数据
const searchValue = ref(props.modelValue)

// 方法
const handleSearch = () => {
  emit('search', searchValue.value)
  emit('update:modelValue', searchValue.value)
}

const handleClear = () => {
  searchValue.value = ''
  emit('clear')
  emit('update:modelValue', '')
}

// 监听外部值变化
watch(() => props.modelValue, (newVal) => {
  searchValue.value = newVal
})

// 监听内部值变化
watch(searchValue, (newVal) => {
  emit('update:modelValue', newVal)
  // 如果清空了搜索框，自动触发搜索
  if (newVal === '') {
    emit('search', '')
  }
})
</script>

<style scoped>
.search-input-component {
  display: flex;
  align-items: center;
}

.search-row {
  display: flex;
  gap: 0;
  align-items: center;
  width: 100%;
}

.search-input {
  width: v-bind(width);
  height: 40px;
}

.search-input :deep(.el-input__wrapper) {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.search-btn {
  flex-shrink: 0;
  font-weight: 500;
  height: 40px;
  border-left: none;
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

/* 不同尺寸的高度适配 */
.search-input.el-input--large,
.search-btn.el-button--large {
  height: 48px;
}

.search-input.el-input--default,
.search-btn.el-button--default {
  height: 36px;
}

.search-input.el-input--small,
.search-btn.el-button--small {
  height: 32px;
}
</style> 