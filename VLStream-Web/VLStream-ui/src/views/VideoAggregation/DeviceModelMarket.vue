<template>
  <div class="device-model-market">
    <div class="model-market-container">
      <!-- 面包屑导航 -->
      <div class="breadcrumb-nav">
<!--        <span class="breadcrumb-item">配置AI事件</span>-->
        <span class="breadcrumb-separator">></span>
        <span class="breadcrumb-item active">算法超市</span>
        </div>
        
      <!-- 算法分类标签 -->
      <div class="category-tabs">
        <div 
          class="category-tab" 
          :class="{ active: activeCategory === 'all' }"
          @click="setActiveCategory('all')"
        >
          全部
        </div>
        <div 
          class="category-tab" 
          :class="{ active: activeCategory === 'person' }"
          @click="setActiveCategory('person')"
        >
          人员检测类
        </div>
        <div 
          class="category-tab" 
          :class="{ active: activeCategory === 'video' }"
          @click="setActiveCategory('video')"
        >
          视频分析类
        </div>
        <div 
          class="category-tab" 
          :class="{ active: activeCategory === 'face' }"
          @click="setActiveCategory('face')"
        >
          人脸识别类
        </div>
      </div>

      <!-- 主要内容区域 -->
      <div class="main-content">
        <!-- 左侧算法网格 -->
        <div class="algorithms-section">
          <div class="algorithm-grid">
            <div 
              v-for="model in filteredModelList" 
            :key="model.id" 
              class="algorithm-card"
            :class="{ selected: model.selected }"
            @click="toggleModelSelection(model)"
          >
              <div class="algorithm-image">
              <img :src="model.image" :alt="model.name" />
                <div v-if="model.selected" class="selected-overlay">
                  <el-icon class="check-icon"><Check /></el-icon>
                </div>
              </div>
              <div class="algorithm-info">
                <h4 class="algorithm-name">{{ model.name }}</h4>
                <p class="algorithm-type">{{ model.type }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧已选择模型列表 -->
        <div class="selected-section">
          <div class="selected-header">
            <h3>已选取模型</h3>
            <div class="selected-count">{{ selectedModels.length }}</div>
          </div>
          
          <div class="selected-list">
          <div 
            v-for="model in selectedModels" 
            :key="model.id" 
              class="selected-item"
            >
              <div class="selected-item-content">
                <span class="selected-item-name">{{ model.name }}</span>
                <el-icon 
                  class="remove-icon" 
                  @click="removeModelSelection(model)"
                >
                  <Close />
                </el-icon>
          </div>
        </div>

            <div v-if="selectedModels.length === 0" class="empty-selected">
              <p>暂无选择模型</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部操作按钮 -->
        <div class="footer-actions">
          <el-button @click="handleCancel">取消</el-button>
          <el-button 
            type="primary" 
          @click="handleConfirm"
            :disabled="selectedModels.length === 0"
          >
          确定
          </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import { modelListData } from './constants.js'

const props = defineProps({
  deviceInfo: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['save', 'cancel'])

// 响应式数据
const activeCategory = ref('all')

// 模型列表
const modelList = ref([...modelListData])

// 计算属性
const selectedModels = computed(() => {
  return modelList.value.filter(model => model.selected)
})

const filteredModelList = computed(() => {
  let filtered = modelList.value

  // 分类过滤
  if (activeCategory.value !== 'all') {
    filtered = filtered.filter(model => model.category === activeCategory.value)
  }

  return filtered
})

// 方法
const setActiveCategory = (category) => {
  activeCategory.value = category
}

const toggleModelSelection = (model) => {
  model.selected = !model.selected
  
  if (model.selected) {
    ElMessage.success(`已选择 "${model.name}"`)
  } else {
    ElMessage.info(`已取消选择 "${model.name}"`)
  }
}

const removeModelSelection = (model) => {
  model.selected = false
  ElMessage.info(`已移除 "${model.name}"`)
}

const handleConfirm = () => {
  if (selectedModels.value.length === 0) {
    ElMessage.warning('请至少选择一个模型')
    return
  }
  
  const modelData = selectedModels.value.map(model => ({
    id: model.id,
    name: model.name,
    type: model.type,
    category: model.category
  }))
  
  emit('save', modelData)
  ElMessage.success(`已选择 ${selectedModels.value.length} 个模型`)
}

const handleCancel = () => {
  emit('cancel')
}
</script>

<style scoped lang="scss">
.device-model-market {
  padding: 20px;
  background: #f5f7fa;
  height: 100vh;
  overflow: hidden;
  
  .model-market-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    padding: 20px;
  }
  
  .breadcrumb-nav {
    display: flex;
    align-items: center;
    margin-bottom: 20px;
    padding: 10px 0;
    border-bottom: 1px solid #ebeef5;
    
    .breadcrumb-item {
      color: #606266;
      font-size: 14px;
      
      &.active {
        color: #303133;
        font-weight: 500;
      }
    }
    
    .breadcrumb-separator {
      margin: 0 12px;
      color: #c0c4cc;
    }
  }
  
  .category-tabs {
    display: flex;
    margin-bottom: 20px;
    border-bottom: 1px solid #ebeef5;
    
    .category-tab {
      padding: 12px 24px;
      cursor: pointer;
      border-bottom: 2px solid transparent;
      transition: all 0.3s;
      color: #606266;
      font-size: 14px;
      
      &:hover {
        color: #1A53FF;
        background: #f0f2f5;
      }
      
      &.active {
        color: #1A53FF;
        border-bottom-color: #1A53FF;
        font-weight: 500;
      }
    }
  }
  
  .main-content {
    flex: 1;
    display: flex;
    gap: 20px;
    overflow: hidden;
    
    .algorithms-section {
      flex: 1;
      overflow-y: auto;
      
      .algorithm-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 16px;
        
        .algorithm-card {
      border: 1px solid #ebeef5;
      border-radius: 8px;
      overflow: hidden;
      transition: all 0.3s;
      cursor: pointer;
      background: white;
      
      &:hover {
            border-color: #1A53FF;
            box-shadow: 0 2px 12px rgba(26, 83, 255, 0.15);
      }
      
      &.selected {
            border-color: #1A53FF;
            box-shadow: 0 2px 12px rgba(26, 83, 255, 0.2);
          }
          
          .algorithm-image {
        position: relative;
            height: 120px;
        overflow: hidden;
        
        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
        
            .selected-overlay {
          position: absolute;
              top: 0;
              left: 0;
              right: 0;
              bottom: 0;
              background: rgba(26, 83, 255, 0.7);
          display: flex;
          align-items: center;
          justify-content: center;
          
          .check-icon {
            color: white;
                font-size: 24px;
              }
            }
          }
          
          .algorithm-info {
            padding: 12px;
            
            .algorithm-name {
              margin: 0 0 4px 0;
              font-size: 14px;
              font-weight: 500;
          color: #333;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }
            
            .algorithm-type {
              margin: 0;
              font-size: 12px;
          color: #666;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }
          }
        }
      }
    }
    
    .selected-section {
      width: 300px;
      border-left: 1px solid #ebeef5;
      padding-left: 20px;
      display: flex;
      flex-direction: column;
      
      .selected-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        
        h3 {
          margin: 0;
          font-size: 16px;
          color: #333;
        }
        
        .selected-count {
          background: #1A53FF;
          color: white;
          border-radius: 12px;
          padding: 4px 8px;
          font-size: 12px;
          min-width: 20px;
          text-align: center;
        }
      }
      
      .selected-list {
        flex: 1;
        overflow-y: auto;
        
        .selected-item {
          margin-bottom: 8px;
          
          .selected-item-content {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 8px 12px;
            background: #f8f9fa;
            border-radius: 4px;
            border: 1px solid #e9ecef;
            
            .selected-item-name {
              flex: 1;
              font-size: 14px;
              color: #333;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }
            
            .remove-icon {
              color: #f56c6c;
              cursor: pointer;
              font-size: 14px;
              margin-left: 8px;
              
              &:hover {
                color: #f78989;
              }
            }
          }
        }
        
        .empty-selected {
          text-align: center;
          padding: 40px 20px;
          color: #909399;
          
          p {
            margin: 0;
            font-size: 14px;
          }
        }
      }
    }
  }
  
  .footer-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding-top: 20px;
    border-top: 1px solid #ebeef5;
    margin-top: 20px;
  }
}

// 响应式设计
@media (max-width: 1200px) {
  .device-model-market {
    .algorithms-section {
      .algorithm-grid {
        grid-template-columns: repeat(3, 1fr);
      }
    }
  }
}

@media (max-width: 768px) {
  .device-model-market {
    .main-content {
      flex-direction: column;
      
      .selected-section {
        width: 100%;
        border-left: none;
        border-top: 1px solid #ebeef5;
        padding-left: 0;
        padding-top: 20px;
      }
    }
    
    .algorithms-section {
      .algorithm-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
  }
}
</style> 