<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

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

// event
const emit = defineEmits(['search', 'reset'])

// Get button
const searchButtonRef = ref(null)

// eventProcess
const handleSearch = () => {
  emit('search')
}

const handleReset = () => {
  emit('reset')
}

// Set button
const forceButtonStyle = () => {
  nextTick(() => {
    const searchButton = searchButtonRef.value?.$el || searchButtonRef.value
    if (searchButton) {
      // Set
      searchButton.style.setProperty('background-color', '#1A53FF', 'important')
      searchButton.style.setProperty('border-color', '#1A53FF', 'important')
      searchButton.style.setProperty('color', 'white', 'important')
      searchButton.style.setProperty('border', '1px solid #1A53FF', 'important')

      // event Process hover
      searchButton.addEventListener('mouseenter', () => {
        searchButton.style.setProperty('background-color', '#4A72FF', 'important')
        searchButton.style.setProperty('border-color', '#4A72FF', 'important')
      })

      searchButton.addEventListener('mouseleave', () => {
        searchButton.style.setProperty('background-color', '#1A53FF', 'important')
        searchButton.style.setProperty('border-color', '#1A53FF', 'important')
      })

      // focus and blurevent
      searchButton.addEventListener('focus', () => {
        searchButton.style.setProperty('background-color', '#1A53FF', 'important')
        searchButton.style.setProperty('border-color', '#1A53FF', 'important')
      })

      console.log('强制设置按钮样式完成')
    }
  })
}

// component after Set
onMounted(() => {
  forceButtonStyle()

  // Set ,
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

/* main Replace - item main #1A53FF */
/* DeviceManagement.vue in full */
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
/* - DeviceManagement.vue in full */
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

/* - */
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

/* can - if */
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