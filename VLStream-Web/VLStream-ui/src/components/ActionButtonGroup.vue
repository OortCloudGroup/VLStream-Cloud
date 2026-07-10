<template>
  <div class="action-button-group">
    <!-- 新增按钮 -->
    <el-button 
      v-if="showAdd"
      type="primary" 
      class="add-btn-custom" 
      @click="handleAdd"
      :disabled="addDisabled"
    >
      <el-icon><Plus /></el-icon>
      {{ addText }}
    </el-button>

    <!-- 编辑删除按钮组 -->
    <div v-if="showEditDelete" class="edit-delete-group">
      <el-button 
        class="edit-btn-custom" 
        @click="handleEdit" 
        :disabled="computedEditDisabled"
      >
        <el-icon><Edit /></el-icon>
        {{ editText }}
      </el-button>
      <el-button 
        type="danger" 
        class="delete-btn-custom" 
        @click="handleDelete" 
        :disabled="computedDeleteDisabled"
      >
        <el-icon><Delete /></el-icon>
        {{ computedDeleteText }}
      </el-button>
    </div>

    <!-- 自定义按钮插槽 -->
    <slot name="extra-buttons"></slot>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  // 选中的行数，用于控制按钮禁用状态
  selectedCount: {
    type: Number,
    default: 0
  },
  
  // 控制按钮显示
  showAdd: {
    type: Boolean,
    default: true
  },
  showEditDelete: {
    type: Boolean,
    default: true
  },
  
  // 按钮文本
  addText: {
    type: String,
    default: '新增'
  },
  editText: {
    type: String,
    default: '编辑'
  },
  deleteText: {
    type: String,
    default: '删除'
  },
  batchDeleteText: {
    type: String,
    default: '批量删除'
  },
  
  // 自定义禁用状态
  addDisabled: {
    type: Boolean,
    default: false
  },
  editDisabled: {
    type: Boolean,
    default: undefined
  },
  deleteDisabled: {
    type: Boolean,
    default: undefined
  }
})

const emit = defineEmits(['add', 'edit', 'delete'])

// 计算编辑按钮的禁用状态
const computedEditDisabled = computed(() => {
  if (props.editDisabled !== undefined) {
    return props.editDisabled
  }
  return props.selectedCount !== 1
})

// 计算删除按钮的禁用状态
const computedDeleteDisabled = computed(() => {
  if (props.deleteDisabled !== undefined) {
    return props.deleteDisabled
  }
  return props.selectedCount === 0
})

// 计算删除按钮文本
const computedDeleteText = computed(() => {
  if (props.selectedCount > 1) {
    return props.batchDeleteText
  }
  return props.deleteText
})

// 事件处理
const handleAdd = () => {
  emit('add')
}

const handleEdit = () => {
  emit('edit')
}

const handleDelete = () => {
  emit('delete')
}
</script>

<style scoped>
.action-button-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 新增按钮自定义样式 */
.add-btn-custom {
  width: 82px !important;
  height: 36px !important;
  border-radius: 18px !important;
  background: #1A53FF !important;
  border-color: #1A53FF !important;
  padding: 0 !important;
  font-size: 14px;
  font-weight: 500;
  color: white !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
}

.add-btn-custom:hover,
.add-btn-custom:focus {
  background: #1A53FF !important;
  border-color: #1A53FF !important;
  color: white !important;
  opacity: 0.8;
}

.add-btn-custom:active {
  background: #1A53FF !important;
  border-color: #1A53FF !important;
  color: white !important;
  opacity: 0.9;
}

/* 编辑删除按钮组合 */
.edit-delete-group {
  display: flex;
  align-items: center;
  margin: 0;
  padding: 0;
}

.edit-delete-group .el-button + .el-button {
  margin-left: 0 !important;
}

/* 编辑按钮自定义样式 */
.edit-btn-custom {
  height: 36px !important;
  border-radius: 18px 0 0 18px !important;
  border-right: none !important;
  padding: 0 16px !important;
  font-size: 14px;
  font-weight: 500;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
  margin-right: 0 !important;
  border-color: #d9d9d9 !important;
}

.edit-btn-custom:hover,
.edit-btn-custom:focus {
  border-right: none !important;
  border-color: #409eff !important;
}

.edit-btn-custom:disabled {
  border-right: none !important;
  border-color: #e4e7ed !important;
}

/* 删除按钮自定义样式 */
.delete-btn-custom {
  height: 36px !important;
  border-radius: 0 18px 18px 0 !important;
  border-left: none !important;
  padding: 0 16px !important;
  font-size: 14px;
  font-weight: 500;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
  background: white !important;
  color: #f56c6c !important;
  border-color: #d9d9d9 !important;
  margin-left: 0 !important;
}

.delete-btn-custom:hover,
.delete-btn-custom:focus {
  border-left: none !important;
  background: white !important;
  color: #f56c6c !important;
  border-color: #f56c6c !important;
}

.delete-btn-custom:active {
  background: white !important;
  color: #f56c6c !important;
  border-color: #f56c6c !important;
  border-left: none !important;
}

.delete-btn-custom:disabled {
  border-left: none !important;
  background: #f5f5f5 !important;
  color: #c0c4cc !important;
  border-color: #e4e7ed !important;
}
</style> 