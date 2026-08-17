<template>
  <div class="classification-layout">
    <aside class="classification-sidebar">
      <el-tabs v-model="activeType" stretch @tab-change="handleTabChange">
        <el-tab-pane v-for="tab in tabs" :key="tab.value" :label="tab.label" :name="tab.value" />
      </el-tabs>

      <div class="classification-actions">
        <el-button link type="primary" :icon="Plus" @click="openCategoryDialog('add')">新增</el-button>
        <el-button link type="primary" :icon="Edit" :disabled="!selectedCategory" @click="openCategoryDialog('edit')">修改</el-button>
        <el-button link type="danger" :icon="Delete" :disabled="!selectedCategory" @click="removeCategory">删除</el-button>
      </div>

      <el-scrollbar class="classification-tree-scroll">
        <el-tree
          ref="treeRef"
          :data="displayTree"
          node-key="id"
          default-expand-all
          highlight-current
          :expand-on-click-node="false"
          @node-click="handleNodeClick"
        >
          <template #default="{ data }">
            <span class="classification-node">
              <span class="classification-node-name">
                <el-icon><Folder /></el-icon>
                {{ data.categoryName }}
              </span>
              <span class="classification-count">{{ data.deviceCount }}</span>
            </span>
          </template>
        </el-tree>
      </el-scrollbar>

      <el-button class="assign-button" type="primary" plain :disabled="normalizedDeviceKeys.length === 0" @click="openAssignment">
        设置分类<span v-if="normalizedDeviceKeys.length">（{{ normalizedDeviceKeys.length }}）</span>
      </el-button>
      <div class="selection-hint">勾选一台可单独设置，勾选多台可批量设置</div>
    </aside>

    <main class="classification-content">
      <slot />
    </main>

    <el-dialog v-model="categoryDialog.visible" :title="categoryDialog.mode === 'add' ? `新增${activeLabel}` : `修改${activeLabel}`" width="460px" append-to-body>
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
        <el-button @click="categoryDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="categoryDialog.saving" @click="submitCategory">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignmentDialog.visible" title="设置设备分类" width="520px" append-to-body>
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
        <el-button @click="assignmentDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="assignmentDialog.saving" @click="submitAssignment">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Folder, Plus } from '@element-plus/icons-vue'
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
const treeCache = reactive({
  REGION: { tree: [], totalCount: 0, unclassifiedCount: 0 },
  GROUP: { tree: [], totalCount: 0, unclassifiedCount: 0 },
  TAG: { tree: [], totalCount: 0, unclassifiedCount: 0 }
})

const normalizedDeviceKeys = computed(() => props.selectedDeviceKeys.filter(key => key !== null && key !== undefined && key !== '').map(key => String(key)))
const activeLabel = computed(() => tabs.find(tab => tab.value === activeType.value)?.label || '')
const displayTree = computed(() => treeCache[activeType.value].tree)

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

<style scoped>
.classification-layout { display: flex; min-height: calc(100vh - 84px); background: #f5f7fa; }
.classification-sidebar { width: 300px; flex: 0 0 300px; margin: 16px 0 16px 16px; padding: 0 16px 16px; background: #fff; border-radius: 6px; display: flex; flex-direction: column; }
.classification-actions { display: flex; justify-content: center; padding: 0 0 10px; border-bottom: 1px solid #ebeef5; }
.classification-tree-scroll { flex: 1; min-height: 360px; margin: 10px -6px; }
.classification-node { display: flex; align-items: center; justify-content: space-between; width: 100%; padding-right: 8px; }
.classification-node-name { display: inline-flex; align-items: center; gap: 6px; overflow: hidden; text-overflow: ellipsis; }
.classification-count { color: #909399; font-size: 12px; }
.assign-button { width: 100%; margin-top: 8px; }
.selection-hint { color: #909399; font-size: 12px; line-height: 18px; text-align: center; margin-top: 8px; }
.classification-content { flex: 1; min-width: 0; }
.assignment-alert { margin-bottom: 18px; }
:deep(.el-tree-node__content) { height: 34px; }
:deep(.el-tree-node__content > .classification-node) { flex: 1; min-width: 0; }
</style>

