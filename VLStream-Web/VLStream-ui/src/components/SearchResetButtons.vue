<template>
  <div class="search-buttons">
    <el-button 
      ref="searchButtonRef"
      type="primary" 
      @click="handleSearch"
      class="force-primary-style"
      :style="{ 
        backgroundColor: '#1A53FF',
        borderColor: '#1A53FF',
        color: 'white'
      }"
    >
      <el-icon><Search /></el-icon>
      搜索
    </el-button>
    <el-button @click="handleReset">
      <el-icon><Refresh /></el-icon>
      重置
    </el-button>
  </div>
</template>

<script setup>
import { Search, Refresh } from '@element-plus/icons-vue'
import { onMounted, nextTick, ref } from 'vue'

// 定义事件
const emit = defineEmits(['search', 'reset'])

// 获取按钮引用
const searchButtonRef = ref(null)

// 事件处理
const handleSearch = () => {
  emit('search')
}

const handleReset = () => {
  emit('reset')
}

// 强制设置按钮样式
const forceButtonStyle = () => {
  nextTick(() => {
    const searchButton = searchButtonRef.value?.$el || searchButtonRef.value
    if (searchButton) {
      // 强制设置样式
      searchButton.style.setProperty('background-color', '#1A53FF', 'important')
      searchButton.style.setProperty('border-color', '#1A53FF', 'important')
      searchButton.style.setProperty('color', 'white', 'important')
      searchButton.style.setProperty('border', '1px solid #1A53FF', 'important')
      
      // 监听鼠标事件来处理hover状态
      searchButton.addEventListener('mouseenter', () => {
        searchButton.style.setProperty('background-color', '#4A72FF', 'important')
        searchButton.style.setProperty('border-color', '#4A72FF', 'important')
      })
      
      searchButton.addEventListener('mouseleave', () => {
        searchButton.style.setProperty('background-color', '#1A53FF', 'important')
        searchButton.style.setProperty('border-color', '#1A53FF', 'important')
      })
      
      // 监听focus和blur事件
      searchButton.addEventListener('focus', () => {
        searchButton.style.setProperty('background-color', '#1A53FF', 'important')
        searchButton.style.setProperty('border-color', '#1A53FF', 'important')
      })
      
      console.log('强制设置按钮样式完成')
    }
  })
}

// 组件挂载后强制设置样式
onMounted(() => {
  forceButtonStyle()
  
  // 延迟再次强制设置，确保样式被应用
  setTimeout(() => {
    forceButtonStyle()
  }, 100)
})
</script>

<style scoped lang="scss">
.search-buttons {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

/* 主色调替换 - 使用项目标准主题色 #1A53FF */
/* 使用更强的选择器覆盖DeviceManagement.vue中的全局样式 */
:deep(.el-button--primary.el-button) {
  background-color: #1A53FF !important;
  border-color: #1A53FF !important;
  color: white !important;
}

:deep(.el-button--primary.el-button:hover) {
  background-color: #4A72FF !important;
  border-color: #4A72FF !important;
  color: white !important;
}

:deep(.el-button--primary.el-button:focus) {
  background-color: #1A53FF !important;
  border-color: #1A53FF !important;
  color: white !important;
}

:deep(.el-button--primary.el-button:active) {
  background-color: #1A53FF !important;
  border-color: #1A53FF !important;
  color: white !important;
}
</style>

<style lang="scss">
/* 超强选择器 - 强制覆盖DeviceManagement.vue中的全局透明背景样式 */
.search-buttons .el-button.el-button--primary.force-primary-style {
  background-color: #1A53FF !important;
  border: 1px solid #1A53FF !important;
  color: white !important;
}

.search-buttons .el-button.el-button--primary.force-primary-style:hover {
  background-color: #4A72FF !important;
  border: 1px solid #4A72FF !important;
  color: white !important;
}

.search-buttons .el-button.el-button--primary.force-primary-style:focus {
  background-color: #1A53FF !important;
  border: 1px solid #1A53FF !important;
  color: white !important;
}

.search-buttons .el-button.el-button--primary.force-primary-style:active {
  background-color: #1A53FF !important;
  border: 1px solid #1A53FF !important;
  color: white !important;
}

/* 多重选择器 - 确保最高优先级 */
.search-buttons .el-button.el-button--primary.el-button.force-primary-style {
  background-color: #1A53FF !important;
  border: 1px solid #1A53FF !important;
  color: white !important;
}

.search-buttons .el-button.el-button--primary.el-button.force-primary-style:hover {
  background-color: #4A72FF !important;
  border: 1px solid #4A72FF !important;
  color: white !important;
}

.search-buttons .el-button.el-button--primary.el-button.force-primary-style:focus {
  background-color: #1A53FF !important;
  border: 1px solid #1A53FF !important;
  color: white !important;
}

.search-buttons .el-button.el-button--primary.el-button.force-primary-style:active {
  background-color: #1A53FF !important;
  border: 1px solid #1A53FF !important;
  color: white !important;
}

/* 万能选择器 - 如果上面的还不够用 */
div.search-buttons .el-button.el-button--primary.el-button.force-primary-style[type="button"] {
  background-color: #1A53FF !important;
  border: 1px solid #1A53FF !important;
  color: white !important;
}

div.search-buttons .el-button.el-button--primary.el-button.force-primary-style[type="button"]:hover {
  background-color: #4A72FF !important;
  border: 1px solid #4A72FF !important;
  color: white !important;
}

div.search-buttons .el-button.el-button--primary.el-button.force-primary-style[type="button"]:focus {
  background-color: #1A53FF !important;
  border: 1px solid #1A53FF !important;
  color: white !important;
}

div.search-buttons .el-button.el-button--primary.el-button.force-primary-style[type="button"]:active {
  background-color: #1A53FF !important;
  border: 1px solid #1A53FF !important;
  color: white !important;
}
</style> 