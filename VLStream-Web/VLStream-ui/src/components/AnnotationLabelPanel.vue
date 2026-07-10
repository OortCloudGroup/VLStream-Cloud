<template>
  <div class="annotation-label-panel">
    <!-- 标签面板头部 -->
    <div class="panel-header">
      <h3>标签栏</h3>
      <el-button
        type="primary"
        size="small"
        @click="handleAddLabel"
        class="add-label-btn"
      >
        <el-icon><Plus /></el-icon>
      </el-button>
    </div>

    <!-- 搜索查询区域 -->
    <div class="search-section">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索标签"
        clearable
        size="small"
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 标签栏表头 -->
    <div class="labels-header">
      <div class="header-cell label-name-col">标签名</div>
      <div class="header-cell label-count-col">标注框数</div>
    </div>

    <!-- 标签列表容器 -->
    <div class="labels-container">
      <div
        v-for="label in filteredLabels"
        :key="label.id"
        class="label-item"
        :class="{ active: selectedLabelId === label.id }"
        @click="selectLabel(label.id)"
      >
        <div class="label-main-info">
          <div class="label-color" :style="{ backgroundColor: label.color }"></div>
          <span class="label-name">{{ label.name }}</span>
        </div>
        <div class="label-stats">
          <span class="label-count">{{ label.usageCount || 0 }}</span>
          <div class="label-actions">
            <el-button
              type="text"
              size="small"
              @click.stop="handleEditLabel(label)"
              title="编辑"
            >
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button
              type="text"
              size="small"
              @click.stop="handleDeleteLabel(label.id)"
              title="删除"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 标签编辑弹窗 -->
    <el-dialog
      v-model="showLabelDialog"
      :title="editingLabel ? '编辑标签' : '新增标签'"
      width="400px"
    >
      <el-form :model="labelForm" :rules="labelRules" ref="labelFormRef" label-width="80px">
        <el-form-item label="标签名" prop="name">
          <el-input v-model="labelForm.name" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-color-picker v-model="labelForm.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLabelDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveLabel">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'

// Props
const props = defineProps({
  labels: {
    type: Array,
    default: () => []
  },
  selectedLabelId: {
    type: [Number, String],
    default: null
  }
})

// Emits
const emit = defineEmits(['label-selected', 'add-label', 'edit-label', 'delete-label'])

// 响应式数据
const searchKeyword = ref('')
const showLabelDialog = ref(false)
const editingLabel = ref(null)
const labelFormRef = ref(null)

// 表单数据
const labelForm = reactive({
  name: '',
  color: '#409eff'
})

// 表单验证规则
const labelRules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }]
}

// 计算属性 - 过滤后的标签
const filteredLabels = computed(() => {
  if (!searchKeyword.value) {
    return props.labels
  }
  return props.labels.filter(label => 
    label.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// 方法
const handleSearch = () => {
  // 搜索逻辑已通过计算属性实现
}

const selectLabel = (labelId) => {
  emit('label-selected', labelId)
}

const handleAddLabel = () => {
  editingLabel.value = null
  labelForm.name = ''
  labelForm.color = '#409eff'
  showLabelDialog.value = true
}

const handleEditLabel = (label) => {
  editingLabel.value = label
  labelForm.name = label.name
  labelForm.color = label.color
  showLabelDialog.value = true
}

const handleDeleteLabel = (labelId) => {
  ElMessageBox.confirm('确定要删除这个标签吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    emit('delete-label', labelId)
    ElMessage.success('标签删除成功')
  })
}

const handleSaveLabel = async () => {
  if (!labelFormRef.value) return
  
  try {
    await labelFormRef.value.validate()
    
    if (editingLabel.value) {
      // 编辑标签
      emit('edit-label', {
        id: editingLabel.value.id,
        name: labelForm.name,
        color: labelForm.color
      })
      ElMessage.success('标签更新成功')
    } else {
      // 新增标签
      emit('add-label', {
        name: labelForm.name,
        color: labelForm.color,
        usageCount: 0
      })
      ElMessage.success('标签添加成功')
    }
    
    showLabelDialog.value = false
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}
</script>

<style scoped>
.annotation-label-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: white;
}

/* 面板头部 */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.add-label-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 6px;
}

/* 搜索区域 */
.search-section {
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
  background: #fafafa;
}

.search-section .el-input {
  border-radius: 6px;
}

.search-section .el-input__wrapper {
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

/* 标签栏表头 */
.labels-header {
  display: flex;
  padding: 8px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e8e8e8;
  font-size: 12px;
  font-weight: 600;
  color: #666;
}

.header-cell {
  flex: 1;
}

.label-name-col {
  flex: 2;
}

.label-count-col {
  flex: 1;
  text-align: center;
}

/* 标签列表容器 */
.labels-container {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.label-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
  border-bottom: 1px solid #f0f0f0;
  min-height: 40px;
}

.label-item:hover {
  background: #f8f9fa;
}

.label-item.active {
  background: #e6f7ff;
  border-color: #1890ff;
}

.label-main-info {
  display: flex;
  align-items: center;
  flex: 2;
  min-width: 0;
}

.label-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  margin-right: 8px;
  border: 1px solid #d9d9d9;
  flex-shrink: 0;
}

.label-name {
  font-size: 14px;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.label-stats {
  display: flex;
  align-items: center;
  flex: 1;
  justify-content: center;
  position: relative;
}

.label-count {
  font-size: 14px;
  color: #666;
  text-align: center;
  min-width: 40px;
  flex-shrink: 0;
}

.label-actions {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
  background: white;
  padding: 2px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.label-item:hover .label-actions {
  opacity: 1;
}

.label-actions .el-button {
  padding: 2px;
  width: 20px;
  height: 20px;
  font-size: 12px;
}
</style>
