<template>
  <div class="event-group-management tenant_Page draHeaPB">
    <div class="tenant_content">
      <div class="tableTenBox flexRowAC">
        <div
          v-show="!treeCollapsed"
          v-yResize
          class="police_aside_use"
        >
          <div class="treeTitle">{{ pageTitle }}</div>
          <div class="tree_search_content flexRowAC">
            <el-input
              v-model="treeKeyword"
              placeholder="搜索"
              clearable
              prefix-icon="Search"
            />
          </div>
          <div class="tree-content" v-loading="loading">
            <el-tree
              style="background: #fff;"
              class="group-tree"
              node-key="uid"
              :data="filteredTree"
              :props="treeProps"
              :expand-on-click-node="false"
              :default-expand-all="true"
              highlight-current
              @node-click="selectNode"
            >
              <template #default="{ data }">
                <div class="custom-tree-node flexRowAC">
                  <span class="tree-node-label">{{ data.name }}</span>
                  <div class="tree-node-actions flexRowAC" @click.stop>
                    <oort-svg-icon width="16" height="16" name="add" @click="openCreate(data)" />
                    <oort-svg-icon width="16" height="16" name="edit_icon" @click="openEdit(data)" />
                    <oort-svg-icon width="16" height="16" name="delete" color="red" @click="removeGroup(data)" />
                  </div>
                </div>
              </template>
            </el-tree>
            <el-empty v-if="!loading && treeData.length === 0" description="暂无数据" :image-size="80" />
          </div>
        </div>

        <div class="tableTenItU">
          <div class="depNameBox_out flexRowAC">
            <div class="depNameBox flexRowAC">
              <CollapseToggle
                v-if="treeCollapsed"
                class="expand-device-tree-btn"
                :is-expanded="false"
                @toggle="handleTreeToggle"
              />
              <div class="exportBtnBox flexRowAC">
                <button type="button" class="exportBtn newBtn flexRowAC" @click="openCreate()">
                  <el-icon class="BtnImg">
                    <Plus />
                  </el-icon>
                  新建{{ entityName }}
                </button>
                <button-group :button-list="toolbarButtonList" />
              </div>
            </div>
            <div class="searchHeight_out flexRowAC">
              <search-height-box
                keyword="keyword"
                placeholder="搜索"
                :data="searchData"
                @handle="searchResetFn"
              />
              <export-excel-pdf :item="exportItem" @handle="handleExport" />
            </div>
          </div>

          <TableSelf
            class="new_table"
            header-cell-class-name="header_tenant_cell"
            stripe
            v-loading="loading"
            :data="currentPageData"
            row-key="uid"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" :width="clacPXToVW(55)" />
            <el-table-column label="序号" :width="clacPXToVW(65)">
              <template #default="scope">
                {{ scope.$index + (currentPage - 1) * pageSize + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="name" :label="`${entityName}名称`" show-overflow-tooltip />
            <el-table-column label="层级" :width="clacPXToVW(100)">
              <template #default="{ row }">{{ row.level + 1 }} 级</template>
            </el-table-column>
            <el-table-column prop="parentName" label="上级" show-overflow-tooltip />
            <el-table-column prop="remark" label="备注" show-overflow-tooltip />
            <el-table-column prop="created_at" label="创建时间" :width="clacPXToVW(180)" />
            <el-table-column fixed="right" align="right" label="操作" :width="clacPXToVW(160)">
              <template #default="scope">
                <div class="operateAppBox flexRowAC" @click.stop>
                  <div class="new_table_svg_group" @click="openEdit(scope.row)">
                    <oort-svg-icon width="20" height="20" name="edit_icon" class="new_table_svg_group_svg" />
                    <span>编辑</span>
                  </div>
                  <div class="new_table_svg_group" @click="removeGroup(scope.row)">
                    <oort-svg-icon color="red" width="20" height="20" name="delete_icon" class="new_table_svg_group_svg" />
                    <span>删除</span>
                  </div>
                </div>
              </template>
            </el-table-column>
          </TableSelf>

          <div class="paginationBox flexRowAC">
            <el-pagination
              background
              :current-page="currentPage"
              :page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="total"
              layout="total, prev, pager, next, sizes"
              class="justifyAlign"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.uid ? `编辑${entityName}` : `新建${entityName}`"
      width="480px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="82px">
        <el-form-item label="上级" v-if="form.puid">
          <el-input :model-value="form.parentName" disabled />
        </el-form-item>
        <el-form-item :label="`${entityName}名称`" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" maxlength="255" show-word-limit :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" class="common_btn">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save" class="common_btn">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clacPXToVW } from '@/utils/index'
import CollapseToggle from '@/components/CollapseToggle.vue'
import AppConfig from '@/config/AppConfig'
import { event_group_delete_v2, event_group_save_v2, event_group_tree } from '@/api/smartCity/events'

const route = useRoute()
const groupType = computed(() => Number(route.meta.groupType || 3))
const entityName = computed(() => ({ 1: '区域', 2: '分组', 3: '标签' }[groupType.value] || '标签'))
const pageTitle = computed(() => `${entityName.value}管理`)
const treeProps = { label: 'name', children: 'children' }
const treeData = ref([])
const flatRows = ref([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const treeKeyword = ref('')
const treeCollapsed = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const selectedRows = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const form = reactive({ uid: '', puid: '', parentName: '', name: '', remark: '' })
const rules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }] }

const exportItem = ref({ isDisabledExcel: false })
const searchData = ref([
  { label: '关键词', value: 'keyword', type: 'text', default: '' }
])

const filteredRows = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return flatRows.value
  return flatRows.value.filter(item => `${item.name}${item.remark}${item.parentName}`.toLowerCase().includes(value))
})

const total = computed(() => filteredRows.value.length)
const currentPageData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

const filterTree = (nodes) => nodes.reduce((result, node) => {
  const children = filterTree(node.children || [])
  const matched = !treeKeyword.value || node.name.includes(treeKeyword.value)
  if (matched || children.length) result.push({ ...node, children })
  return result
}, [])
const filteredTree = computed(() => filterTree(treeData.value))

const toolbarButtonList = computed(() => [
  { name: '编辑', svg: 'table_edit', clickFn: handleToolbarEdit },
  { name: '删除', svg: 'table_del', clickFn: handleToolbarDelete }
])

const responseList = (response) => response?.data?.list || response?.list || []

const fetchTree = async () => {
  const response = await event_group_tree({
    app_id: AppConfig.events.appID,
    group_type: groupType.value
  })
  if (response?.code && response.code !== 200) {
    throw new Error(response.msg || '加载数据失败')
  }
  return responseList(response)
}

const flattenTree = (nodes, parentName = '', level = 0) => nodes.flatMap(node => [
  { ...node, parentName, level },
  ...flattenTree(node.children || [], node.name, level + 1)
])

const reload = async () => {
  loading.value = true
  try {
    treeData.value = await fetchTree()
    flatRows.value = flattenTree(treeData.value)
    selectedRows.value = []
  } catch (error) {
    ElMessage.error(error?.message || `加载${entityName.value}失败`)
  } finally {
    loading.value = false
  }
}

const handleTreeToggle = () => {
  treeCollapsed.value = !treeCollapsed.value
}

const selectNode = (node) => {
  keyword.value = node.name
  currentPage.value = 1
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handleCurrentChange = (val) => {
  currentPage.value = val
}

const searchResetFn = (val, reset) => {
  if (reset && !(val && val.keyword)) {
    keyword.value = ''
    currentPage.value = 1
    return
  }
  keyword.value = val?.keyword || ''
  currentPage.value = 1
}

const handleExport = () => {
  ElMessage.success('导出数据')
}

const resetForm = () => {
  form.uid = ''
  form.puid = ''
  form.parentName = ''
  form.name = ''
  form.remark = ''
}

const openCreate = (parent = null) => {
  resetForm()
  if (parent) {
    form.puid = parent.uid
    form.parentName = parent.name
  }
  dialogVisible.value = true
}

const openEdit = (node) => {
  form.uid = node.uid
  form.puid = node.puid
  form.parentName = node.parentName || ''
  form.name = node.name
  form.remark = node.remark || ''
  dialogVisible.value = true
}

const handleToolbarEdit = () => {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning('请选择一条记录进行编辑')
    return
  }
  openEdit(selectedRows.value[0])
}

const handleToolbarDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的记录')
    return
  }
  if (selectedRows.value.length > 1) {
    ElMessage.warning('请选择一条记录进行删除')
    return
  }
  await removeGroup(selectedRows.value[0])
}

const save = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const response = await event_group_save_v2({
      app_id: AppConfig.events.appID,
      group_type: groupType.value,
      uid: form.uid || undefined,
      puid: form.puid || undefined,
      name: form.name.trim(),
      remark: form.remark.trim()
    })
    if (response?.code && response.code !== 200) throw new Error(response.msg || '保存失败')
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await reload()
  } catch (error) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const removeGroup = async (node) => {
  try {
    await ElMessageBox.confirm(`确定删除“${node.name}”吗？有子级时无法删除。`, '删除确认', { type: 'warning' })
    const response = await event_group_delete_v2({ app_id: AppConfig.events.appID, uid: node.uid })
    if (response?.code && response.code !== 200) throw new Error(response.msg || '删除失败')
    ElMessage.success('删除成功')
    await reload()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '删除失败')
  }
}

watch(groupType, reload)
onMounted(reload)
</script>

<style scoped lang="scss">
.tenant_Page {
  height: 100%;
  width: 100%;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  background: #f0f2f5;
  display: flex;
  flex-direction: column;

  .tenant_content {
    width: 100%;
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    border-radius: 8px;
  }

  .tableTenBox {
    padding: 20px;
    width: 100%;
    height: 100%;
    flex: 1;
    background: #fff;
    align-items: flex-start;
    min-height: 0;
    border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  }
}

.police_aside_use {
  width: 300px;
  padding-right: 20px;
  flex-shrink: 0;
  height: 100%;
  overflow: hidden;

  .treeTitle {
    color: var(--el-color-primary);
    padding-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 12px;
    padding-top: 4px;

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

  :deep(.el-tree) {
    height: calc(100% - 80px);
    overflow: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }
}

.custom-tree-node {
  flex: 1;
  min-width: 0;
  gap: 4px;

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
}

.tree-content {
  flex: 1;
  overflow-y: auto;
  height: calc(100% - 90px);
}

.tableTenItU {
  flex: 1;
  height: 100%;
  overflow: auto;
  min-width: 0;

  :deep(.header_tenant_cell) {
    background: #F8F8F9;
  }
}

.paginationBox {
  justify-content: center;
  height: 100px;
}

.operateAppBox {
  justify-content: flex-end;
  gap: 2px;
}

.event-group-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  overflow: hidden;
}

:deep(.el-dialog) {
  border-radius: 8px;
}
</style>
