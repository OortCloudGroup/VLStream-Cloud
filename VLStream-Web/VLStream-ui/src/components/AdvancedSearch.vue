<template>
  <div class="advanced-search">
    <div class="search-wrapper">
      <!-- 搜索框区域 -->
      <div class="search-container">
        <!-- 折叠按钮 -->
        <div class="collapse-btn" @click="toggleExpanded">
          <img 
            :src="collapseIcon" 
            alt="折叠按钮"
            class="collapse-icon"
          />
        </div>
        
        <!-- 搜索框 -->
        <div class="search-box">
          <el-input
            v-model="searchValue"
            placeholder="高级搜索"
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <!-- 搜索按钮 -->
          <el-button 
            type="primary" 
            class="search-btn"
            @click="handleSearch"
          >
            <el-icon><Search /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 导出上传组件 -->
      <div class="export-upload-container">
        <div class="export-upload-wrapper">
          <!-- 导出按钮 -->
          <el-button 
            class="export-btn"
            @click="handleExport"
          >
            <el-icon><Download /></el-icon>
          </el-button>
          
          <!-- 下拉按钮 -->
          <el-dropdown trigger="click" @command="handleDropdownCommand">
            <el-button class="dropdown-btn">
              <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="upload">上传</el-dropdown-item>
                <el-dropdown-item command="template">下载模板</el-dropdown-item>
                <el-dropdown-item command="batch">批量操作</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>

    <!-- 展开的搜索表单 -->
    <div v-if="isExpanded" class="expanded-form">
      <div class="form-content">
        <div class="form-row">
          <div class="form-item">
            <label>设备名称：</label>
            <el-input 
              v-model="searchForm.deviceName" 
              placeholder="请输入设备名称"
              clearable
            />
          </div>
        </div>
        
        <div class="form-row">
          <div class="form-item">
            <label>设备ID：</label>
            <el-input 
              v-model="searchForm.deviceId" 
              placeholder="请输入设备ID"
              clearable
            />
          </div>
        </div>
        
        <div class="form-row">
          <div class="form-item">
            <label>标签名称：</label>
            <TagSelector
              v-model="searchForm.selectedTags"
              placeholder="请选择标签"
              @change="handleTagChange"
            />
          </div>
        </div>
        
        <div class="form-row">
          <div class="form-item">
            <label>日期：</label>
            <DateRangePicker
              v-model="searchForm.dateRange"
              width="408px"
            />
          </div>
        </div>
        
        <div class="form-row buttons-row">
          <div class="buttons-container">
            <SearchResetButtons
              @search="handleAdvancedSearch"
              @reset="handleReset"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search, Refresh, Download, ArrowDown } from '@element-plus/icons-vue'
import TagSelector from '@/components/TagSelector.vue'
import DateRangePicker from '@/components/DateRangePicker.vue'
import SearchResetButtons from '@/components/SearchResetButtons.vue'

// 引入图标
import collapseIconPath from '@/assets/搜索折叠按钮.png'

// 定义事件
const emit = defineEmits(['search', 'reset', 'export', 'upload', 'template', 'batch'])

// 响应式数据
const isExpanded = ref(false)
const searchValue = ref('')
const searchForm = ref({
  deviceName: '',
  deviceId: '',
  tagName: '',
  selectedTags: [],
  dateRange: []
})

// 计算属性
const collapseIcon = computed(() => collapseIconPath)

// 方法
const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value
}

const handleSearch = () => {
  emit('search', {
    keyword: searchValue.value,
    ...searchForm.value,
    // 将selectedTags转换为tagName字符串
    tagName: searchForm.value.selectedTags.length > 0 ? searchForm.value.selectedTags[0] : ''
  })
}

const handleAdvancedSearch = () => {
  emit('search', {
    keyword: searchValue.value,
    ...searchForm.value,
    // 将selectedTags转换为tagName字符串
    tagName: searchForm.value.selectedTags.length > 0 ? searchForm.value.selectedTags[0] : ''
  })
}

const handleTagChange = (selectedTags) => {
  console.log('标签选择变更:', selectedTags)
  // 更新tagName为第一个选中的标签，用于向后兼容
  searchForm.value.tagName = selectedTags.length > 0 ? selectedTags[0] : ''
}

// 导出上传相关处理函数
const handleExport = () => {
  emit('export')
}

const handleDropdownCommand = (command) => {
  switch (command) {
    case 'upload':
      emit('upload')
      break
    case 'template':
      emit('template')
      break
    case 'batch':
      emit('batch')
      break
  }
}

const handleReset = () => {
  searchValue.value = ''
  searchForm.value = {
    deviceName: '',
    deviceId: '',
    tagName: '',
    selectedTags: [],
    dateRange: []
  }
  emit('reset')
}

// 暴露方法给父组件
defineExpose({
  reset: handleReset
})
</script>

<style scoped lang="scss">
.advanced-search {
  position: relative;
  
  .search-wrapper {
    display: flex;
    align-items: center;
    gap: 20px;
  }
  
  .search-container {
    display: flex;
    align-items: center;
    background: #FFFFFF;
    border: 1px solid #1A53FF;
    border-radius: 170px;
    height: 36px;
    width: 536px;
    padding: 0;
    position: relative;
    overflow: hidden;
    
    .collapse-btn {
      position: absolute;
      right: 65px; // 搜索按钮宽度55px + 2px间距
      top: 50%;
      transform: translateY(-50%);
      cursor: pointer;
      z-index: 10;
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      transition: all 0.3s;
      
      &:hover {
        background: rgba(26, 83, 255, 0.1);
      }
      
      .collapse-icon {
        width: 16px;
        height: 16px;
        display: block;
      }
    }
    
    .search-box {
      flex: 1;
      display: flex;
      align-items: center;
      margin-left: 12px;
      margin-right: 0;
      height: 100%;
      
      .search-input {
        flex: 1;
        height: 100%;
        margin-right: 81px; // 为折叠按钮和搜索按钮留出空间 (20px + 2px + 55px + 4px)
        
        :deep(.el-input__wrapper) {
          background: transparent;
          border: none;
          box-shadow: none;
          padding: 0 12px;
          border-radius: 0;
          height: 100%;
          align-items: center;
        }
        
        :deep(.el-input__inner) {
          background: transparent;
          border: none;
          font-size: 14px;
          color: #303133;
          height: 100%;
          line-height: 34px;
          
          &::placeholder {
            color: #a8abb2;
          }
        }
      }
      
      .search-btn {
        position: absolute;
        right: 1px;
        top: 50%;
        transform: translateY(-50%);
        width: 55px;
        height: 34px;
        background: #999999;
        border: 1px solid #999999;
        border-radius: 0 170px 170px 0;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0;
        
        &:hover {
          background: #aaaaaa;
          border-color: #aaaaaa;
        }
        
        .el-icon {
          color: #000000; // 搜索图标颜色改为黑色
          font-size: 16px;
        }
      }
    }
  }
  
  .expanded-form {
    position: absolute;
    top: 100%;
    left: 0;
    width: 536px;
    background: white;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    z-index: 1000;
    margin-top: 8px;
    
    .form-content {
      padding: 20px;
      
      .form-row {
        display: flex;
        gap: 16px;
        margin-bottom: 16px;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .form-item {
          flex: 1;
          display: flex;
          align-items: center;
          
          label {
            font-size: 14px;
            color: #606266;
            margin-right: 8px;
            white-space: nowrap;
            min-width: 80px;
          }
          
          .el-input,
          .tag-selector,
          .date-range-picker-wrapper {
            flex: 1;
            width: 408px !important;
            max-width: 408px;
          }
          
          // 确保TagSelector内部的el-select也是408px宽度
          :deep(.tag-selector) {
            width: 408px !important;
            
            .el-select {
              width: 100% !important;
            }
          }
          
          // 确保日期选择器也是408px宽度
          :deep(.date-range-picker-wrapper) {
            width: 408px !important;
            
            .el-date-editor {
              width: 100% !important;
            }
          }
          
          // 确保输入框包装器也是408px宽度
          :deep(.el-input) {
            width: 408px !important;
          }
        }
      }
      
      .buttons-row {
        display: flex;
        justify-content: flex-end;
        margin-bottom: 0;
        
        .buttons-container {
          display: flex;
          align-items: center;
          margin-right: 88px; /* 对齐到日期框右边界 (标签宽度88px，让按钮右边界与日期框右边界对齐) */
        }
      }
    }
  }
  
  // 导出上传组件样式
  .export-upload-container {
    flex-shrink: 0;
    
    .export-upload-wrapper {
      display: flex;
      background: #FAFAFA;
      border-radius: 4px;
      width: 80px;
      height: 36px;
      overflow: hidden;
      
      .export-btn {
        width: 40px;
        height: 36px;
        background: #FAFAFA;
        border: none;
        border-radius: 4px 0 0 4px;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.3s;
        
        &:hover {
          background: #F0F0F0;
        }
        
        .el-icon {
          font-size: 16px;
          color: #606266;
          transform: rotate(180deg);
        }
      }
      
      .dropdown-btn {
        width: 40px;
        height: 36px;
        background: #FAFAFA;
        border: none;
        border-radius: 0 4px 4px 0;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.3s;
        border-left: 1px solid #E4E7ED;
        
        &:hover {
          background: #F0F0F0;
        }
        
        .el-icon {
          font-size: 14px;
          color: #606266;
        }
      }
    }
  }
}
</style> 