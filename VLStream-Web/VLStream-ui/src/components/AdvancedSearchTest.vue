<template>
  <div class="test-page">
    <h2>高级搜索组件测试</h2>
    
    <div class="test-container">
      <div class="test-section">
        <h3>组件展示</h3>
        <div class="component-wrapper">
          <AdvancedSearch 
            @search="handleSearch"
            @reset="handleReset"
          />
        </div>
      </div>
      
      <div class="test-section">
        <h3>搜索结果</h3>
        <div class="search-results">
          <pre>{{ JSON.stringify(searchResults, null, 2) }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AdvancedSearch from './AdvancedSearch.vue'

const searchResults = ref(null)

const handleSearch = (data) => {
  console.log('搜索数据:', data)
  searchResults.value = data
}

const handleReset = () => {
  console.log('重置搜索')
  searchResults.value = null
}
</script>

<style scoped lang="scss">
.test-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  
  h2 {
    color: #1A53FF;
    margin-bottom: 20px;
  }
  
  .test-container {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  
  .test-section {
    background: white;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    
    h3 {
      margin-top: 0;
      margin-bottom: 16px;
      color: #303133;
    }
    
    .component-wrapper {
      display: flex;
      justify-content: center;
      padding: 20px;
      background: #f5f7fa;
      border-radius: 8px;
    }
    
    .search-results {
      background: #f8f9fa;
      padding: 16px;
      border-radius: 4px;
      border: 1px solid #e9ecef;
      
      pre {
        margin: 0;
        font-family: Monaco, 'Courier New', monospace;
        font-size: 14px;
        color: #495057;
        white-space: pre-wrap;
        word-wrap: break-word;
      }
    }
  }
}
</style> 