<template>
  <div class="tenant_Page draHeaPB classification-layout">
    <div class="tenant_content">
      <div class="tableTenBox flexRowAC">
        <div v-yResize class="police_aside_use">
          <el-tabs v-model="activeType" class="left-tabs" @tab-change="handleTabChange">
            <el-tab-pane v-for="tab in tabs" :key="tab.value" :label="tab.label" :name="tab.value" />
          </el-tabs>
          <div class="treeTitle">{{ activeLabel }}</div>
          <div class="tree_search_content flexRowAC">
            <el-input
              v-model="treeSearchKeyword"
              placeholder="搜索"
              clearable
              prefix-icon="Search"
            />
          </div>
          <el-tree
            ref="treeRef"
            style="background: #fff;"
            :data="displayTree"
            :props="treeProps"
            node-key="id"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            :filter-node-method="filterNode"
            @node-click="handleNodeClick"
          >
            <template #empty>
              <div class="tree-empty" @click="openRootAdd">暂无数据，点击新增</div>
            </template>
            <template #default="{ node, data }">
              <div
                class="custom-tree-node flexRowAC"
                @mouseenter="hoveredTreeNodeId = data.id"
                @mouseleave="hoveredTreeNodeId = null"
              >
                <div class="tree-node-main flexRowAC">
                  <el-icon class="tree-icon"><Folder /></el-icon>
                  <el-tooltip :open-delay="500" effect="light" :content="node.label" placement="top">
                    <div
                      class="tree-node-label"
                      :class="{ activeDept: selectedCategory && selectedCategory.id === data.id }"
                      @dblclick.stop="handleEditNode(data)"
                    >
                      {{ data.categoryName }} ({{ data.deviceCount || 0 }})
                    </div>
                  </el-tooltip>
                </div>
                <div
                  v-show="hoveredTreeNodeId === data.id || (selectedCategory && selectedCategory.id === data.id)"
                  class="tree-node-actions flexRowAC"
                  @click.stop
                >
                  <oort-svg-icon
                    width="20"
                    height="20"
                    name="delete"
                    color="red"
                    class="tree-action-icon"
                    @click="handleRemoveNode(data)"
                  />
                  <oort-svg-icon
                    width="20"
                    height="20"
                    name="add"
                    class="tree-action-icon"
                    @click="handleAddChild(data)"
                  />
                </div>
              </div>
            </template>
          </el-tree>
          <el-button class="assign-button" type="primary" plain :disabled="normalizedDeviceKeys.length === 0" @click="openAssignment">
            设置分类<span v-if="normalizedDeviceKeys.length">（{{ normalizedDeviceKeys.length }}）</span>
          </el-button>
          <div class="selection-hint">勾选一台可单独设置，勾选多台可批量设置</div>
        </div>

        <div class="tableTenIt">
          <slot />
        </div>
      </div>
    </div>

    <el-dialog v-model="categoryDialog.visible" :title="categoryDialog.mode === 'add' ? `新增${activeLabel}` : `修改${activeLabel}`" width="30%" append-to-body>
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-width="90px">
        <el-form-item label="上级节点" prop="parentId">
          <el-tree-select
            v-model="categoryForm.parentId"
            :data="parentOptions"
            node-key="id"
            check-strictly
            default-expand-all
            :props="{ label: 'categoryName', children: 'children' }"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="categoryForm.categoryName" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="显示顺序" prop="sortNum">
          <el-input-number v-model="categoryForm.sortNum" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialog.visible = false" class="common_btn">取消</el-button>
        <el-button type="primary" :loading="categoryDialog.saving" @click="submitCategory" class="common_btn">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignmentDialog.visible" title="设置设备分类" width="30%" append-to-body>
      <el-alert
        v-if="normalizedDeviceKeys.length > 1"
        title="批量设置会用本次选择覆盖这些设备原有的区域、分组和标签"
        type="warning"
        :closable="false"
        show-icon
        class="assignment-alert"
      />
      <el-form label-width="80px">
        <el-form-item label="区域">
          <el-tree-select v-model="assignmentForm.regionId" :data="treeCache.REGION.tree" node-key="id" check-strictly clearable default-expand-all :props="treeProps" style="width: 100%" />
        </el-form-item>
        <el-form-item label="分组">
          <el-tree-select v-model="assignmentForm.groupId" :data="treeCache.GROUP.tree" node-key="id" check-strictly clearable default-expand-all :props="treeProps" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标签">
          <el-tree-select v-model="assignmentForm.tagIds" :data="treeCache.TAG.tree" node-key="id" multiple show-checkbox check-strictly clearable default-expand-all :props="treeProps" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignmentDialog.visible = false" class="common_btn">取消</el-button>
        <el-button type="primary" :loading="assignmentDialog.saving" @click="submitAssignment" class="common_btn">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { Folder } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addClassificationCategory,
  deleteClassificationCategory,
  getClassificationTree,
  getDeviceClassification,
  saveDeviceClassification,
  updateClassificationCategory
} from '@/api/device/classification'

const props = defineProps({
  protocolType: { type: String, required: true },
  selectedDeviceKeys: { type: Array, default: () => [] }
})
const emit = defineEmits(['filter-change', 'assigned'])

const tabs = [
  { label: '区域', value: 'REGION' },
  { label: '分组', value: 'GROUP' },
  { label: '标签', value: 'TAG' }
]
const treeProps = { label: 'categoryName', children: 'children' }
const activeType = ref('REGION')
const treeRef = ref()
const selectedCategory = ref(null)
const hoveredTreeNodeId = ref(null)
const treeSearchKeyword = ref('')
const treeCache = reactive({
  REGION: { tree: [], totalCount: 0, unclassifiedCount: 0 },
  GROUP: { tree: [], totalCount: 0, unclassifiedCount: 0 },
  TAG: { tree: [], totalCount: 0, unclassifiedCount: 0 }
})

const normalizedDeviceKeys = computed(() => props.selectedDeviceKeys.filter(key => key !== null && key !== undefined && key !== '').map(key => String(key)))
const activeLabel = computed(() => tabs.find(tab => tab.value === activeType.value)?.label || '')
const displayTree = computed(() => treeCache[activeType.value].tree)

watch(treeSearchKeyword, val => {
  treeRef.value?.filter(val)
})

function filterNode(value, data) {
  if (!value) return true
  return (data.categoryName || '').indexOf(value) !== -1
}

async function loadTree(type = activeType.value) {
  const response = await getClassificationTree(type, props.protocolType)
  const data = response.data || {}
  treeCache[type].tree = data.tree || []
  treeCache[type].totalCount = data.totalCount || 0
  treeCache[type].unclassifiedCount = data.unclassifiedCount || 0
}

async function loadAllTrees() {
  await Promise.all(tabs.map(tab => loadTree(tab.value)))
}

async function handleTabChange(type) {
  selectedCategory.value = null
  treeSearchKeyword.value = ''
  hoveredTreeNodeId.value = null
  await loadTree(type)
  emit('filter-change', { categoryType: undefined, categoryId: undefined, unclassified: undefined })
  nextTick(() => treeRef.value?.setCurrentKey(null))
}

function handleNodeClick(node) {
  selectedCategory.value = node
  emit('filter-change', { categoryType: activeType.value, categoryId: String(node.id), unclassified: false })
}

const categoryDialog = reactive({ visible: false, mode: 'add', saving: false })
const categoryFormRef = ref()
const categoryForm = reactive({ id: undefined, categoryType: 'REGION', parentId: '0', categoryName: '', sortNum: 0 })
const categoryRules = { categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }

function cloneWithoutNode(nodes, excludedId) {
  return nodes.filter(node => String(node.id) !== String(excludedId)).map(node => ({
    ...node,
    children: cloneWithoutNode(node.children || [], excludedId)
  }))
}

const parentOptions = computed(() => [
  { id: '0', categoryName: '顶级节点', children: cloneWithoutNode(treeCache[activeType.value].tree, categoryDialog.mode === 'edit' ? categoryForm.id : null) }
])

function openCategoryDialog(mode) {
  categoryDialog.mode = mode
  categoryForm.id = mode === 'edit' ? String(selectedCategory.value.id) : undefined
  categoryForm.categoryType = activeType.value
  categoryForm.parentId = mode === 'edit' ? String(selectedCategory.value.parentId || 0) : (selectedCategory.value ? String(selectedCategory.value.id) : '0')
  categoryForm.categoryName = mode === 'edit' ? selectedCategory.value.categoryName : ''
  categoryForm.sortNum = mode === 'edit' ? (selectedCategory.value.sortNum || 0) : 0
  categoryDialog.visible = true
  nextTick(() => categoryFormRef.value?.clearValidate())
}

function handleAddChild(data) {
  selectedCategory.value = data
  openCategoryDialog('add')
}

function openRootAdd() {
  selectedCategory.value = null
  openCategoryDialog('add')
}

function handleRemoveNode(data) {
  selectedCategory.value = data
  removeCategory()
}

function handleEditNode(data) {
  selectedCategory.value = data
  openCategoryDialog('edit')
}

async function submitCategory() {
  await categoryFormRef.value.validate()
  categoryDialog.saving = true
  try {
    const payload = { ...categoryForm }
    if (categoryDialog.mode === 'add') await addClassificationCategory(payload)
    else await updateClassificationCategory(payload)
    ElMessage.success('保存成功')
    categoryDialog.visible = false
    selectedCategory.value = null
    await loadTree(activeType.value)
  } finally {
    categoryDialog.saving = false
  }
}

async function removeCategory() {
  await ElMessageBox.confirm(`确认删除“${selectedCategory.value.categoryName}”吗？`, '提示', { type: 'warning' })
  await deleteClassificationCategory(String(selectedCategory.value.id))
  ElMessage.success('删除成功')
  selectedCategory.value = null
  await loadTree(activeType.value)
}

const assignmentDialog = reactive({ visible: false, saving: false })
const assignmentForm = reactive({ regionId: undefined, groupId: undefined, tagIds: [] })

async function openAssignment() {
  if (!normalizedDeviceKeys.value.length) return
  await loadAllTrees()
  assignmentForm.regionId = undefined
  assignmentForm.groupId = undefined
  assignmentForm.tagIds = []
  if (normalizedDeviceKeys.value.length === 1) {
    const response = await getDeviceClassification(props.protocolType, normalizedDeviceKeys.value[0])
    const data = response.data || {}
    assignmentForm.regionId = data.regionId ? String(data.regionId) : undefined
    assignmentForm.groupId = data.groupId ? String(data.groupId) : undefined
    assignmentForm.tagIds = (data.tagIds || []).map(id => String(id))
  }
  assignmentDialog.visible = true
}

async function submitAssignment() {
  assignmentDialog.saving = true
  try {
    await saveDeviceClassification({
      protocolType: props.protocolType,
      deviceKeys: normalizedDeviceKeys.value,
      regionId: assignmentForm.regionId,
      groupId: assignmentForm.groupId,
      tagIds: assignmentForm.tagIds
    })
    ElMessage.success('分类设置成功')
    assignmentDialog.visible = false
    await loadAllTrees()
    emit('assigned')
  } finally {
    assignmentDialog.saving = false
  }
}

onMounted(async () => {
  await loadTree('REGION')
})
</script>

<style lang="scss" scoped>
.classification-layout {
  height: 100%;
  background: #f0f2f5;
}

.police_aside_use {
  width: 300px;
  padding-right: 20px;
  flex-shrink: 0;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .treeTitle {
    color: var(--el-color-primary);
    padding-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 12px;
    padding-top: 4px;
    flex-shrink: 0;

    &::before {
      content: '';
      width: 3px;
      height: 18px;
      background-color: var(--el-color-primary);
    }
  }

  .tree_search_content {
    justify-content: center;
    padding-bottom: 10px;
    flex-shrink: 0;

    :deep(.el-input__wrapper) {
      background: #fff;
      box-shadow: none;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
    }
  }

  :deep(.el-tree-node__content) {
    --el-tree-node-hover-bg-color: var(--el-menu-hover-bg-color);
    height: 38px;
    font-size: 14px;
    color: #333;

    .custom-tree-node {
      width: 100%;
      justify-content: space-between;
      padding-right: 4px;
    }
  }

  :deep(.el-tree-node) {
    .el-tree-node.is-current.is-focusable > .el-tree-node__content {
      background-color: var(--el-color-primary-hb);
      color: var(--el-color-primary);
    }
  }

  :deep(.el-tree) {
    flex: 1;
    min-height: 0;
    overflow: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }
}

.left-tabs {
  margin-bottom: 12px;
  flex-shrink: 0;

  :deep(.el-tabs__header) {
    margin: 0;
    border-bottom: 1px solid #e4e7ed;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__nav) {
    display: flex;
    width: 100%;
  }

  :deep(.el-tabs__item) {
    flex: 1;
    text-align: center;
    padding: 0;
    height: 40px;
    line-height: 40px;
  }
}

.custom-tree-node {
  flex: 1;
  min-width: 0;
  gap: 4px;

  .tree-node-main {
    flex: 1;
    min-width: 0;
    gap: 4px;
    overflow: hidden;
  }

  .tree-node-label {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .tree-node-actions {
    flex-shrink: 0;
    gap: 8px;
    margin-left: 8px;
  }

  .tree-action-icon {
    cursor: pointer;
  }

  .tree-icon {
    flex-shrink: 0;
    font-size: 14px;
    color: var(--el-color-primary);
  }

  .activeDept {
    color: var(--el-color-primary);
  }
}

.assign-button {
  width: 100%;
  margin-top: 8px;
  flex-shrink: 0;
}

.selection-hint {
  color: #909399;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  margin-top: 8px;
  flex-shrink: 0;
}

.tree-empty {
  padding: 24px 0;
  color: #909399;
  font-size: 14px;
  text-align: center;
  cursor: pointer;
}

.tableTenIt {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: auto;
  display: flex;
  flex-direction: column;
}

.assignment-alert {
  margin-bottom: 18px;
}

:slotted(.device-table-panel) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:slotted(.paginationBox) {
  justify-content: flex-end;
  height: 80px;
  flex-shrink: 0;
}

:slotted(.password-container) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

:slotted(.eye-icon) {
  cursor: pointer;
}

:slotted(.header_tenant_cell) {
  background: #F8F8F9;
}

:slotted(.operateAppBox) {
  justify-content: flex-end;
  gap: 2px;
}

:slotted(.new_table) {
  flex: 1;
}
</style>
