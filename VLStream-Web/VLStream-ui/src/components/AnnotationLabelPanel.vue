<template>
  <div class="annotation-label-panel">
    <div class="treeTitle">
      标签栏
      <el-icon class="add-label-icon" @click="handleAddLabel">
        <Plus />
      </el-icon>
    </div>

    <div class="tree_search_content flexRowAC">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索"
        debounce="300"
        prefix-icon="Search"
        clearable
      />
    </div>

    <div class="labels-header">
      <div class="header-cell label-name-col">标签名</div>
      <div class="header-cell label-count-col">标注框数</div>
    </div>

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
            <el-icon class="action-icon" title="编辑" @click.stop="handleEditLabel(label)">
              <Edit />
            </el-icon>
            <el-icon class="action-icon" title="删除" @click.stop="handleDeleteLabel(label.id)">
              <Delete />
            </el-icon>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showLabelDialog"
      :title="editingLabel ? '编辑标签' : '新增标签'"
      width="25%"
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
        <el-button @click="showLabelDialog = false" class="common_btn">取消</el-button>
        <el-button type="primary" @click="handleSaveLabel" class="common_btn">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

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

const emit = defineEmits(['label-selected', 'add-label', 'edit-label', 'delete-label'])

const searchKeyword = ref('')
const showLabelDialog = ref(false)
const editingLabel = ref(null)
const labelFormRef = ref(null)

const labelForm = reactive({
  name: '',
  color: '#409eff'
})

const labelRules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }]
}

const filteredLabels = computed(() => {
  if (!searchKeyword.value) {
    return props.labels
  }
  return props.labels.filter(label =>
    label.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

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
      emit('edit-label', {
        id: editingLabel.value.id,
        name: labelForm.name,
        color: labelForm.color
      })
      ElMessage.success('标签更新成功')
    } else {
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

<style scoped lang="scss">
.annotation-label-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  overflow: hidden;
}

.treeTitle {
  color: var(--el-color-primary);
  padding-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 4px;
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 500;

  &::before {
    content: '';
    width: 3px;
    height: 18px;
    background-color: var(--el-color-primary);
  }
}

.add-label-icon {
  margin-left: auto;
  cursor: pointer;
  color: var(--el-color-primary);
  font-size: 16px;
  padding: 4px;
  border-radius: 4px;

  &:hover {
    background: var(--el-color-primary-hb, #ecf5ff);
  }
}

.tree_search_content {
  padding-bottom: 10px;
  flex-shrink: 0;

  :deep(.el-input__wrapper) {
    background: #fff;
    box-shadow: none;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
  }

  :deep(.el-input__inner) {
    background: #fff;
    border: none;
  }
}

.labels-header {
  display: flex;
  padding: 8px 12px;
  background: #f8f8f9;
  font-size: 12px;
  font-weight: 600;
  color: #666;
  flex-shrink: 0;
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

.labels-container {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.label-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
  border-bottom: 1px solid #f0f0f0;
  min-height: 40px;
  position: relative;
}

.label-item:hover {
  background: #f8f9fa;
}

.label-item.active {
  background: var(--el-color-primary-hb, #e6f7ff);
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: var(--el-color-primary);
  }

  .label-name {
    color: var(--el-color-primary);
  }

  .label-actions {
    opacity: 1;
  }
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
  color: #333;
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
  min-height: 24px;
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
  align-items: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
  background: transparent;
  padding: 0 2px;
}

.label-item:hover .label-actions {
  opacity: 1;
}

.action-icon {
  font-size: 14px;
  color: var(--el-color-primary);
  cursor: pointer;
  padding: 2px;

  &:hover {
    color: var(--el-color-primary);
  }
}
</style>
