<template>
  <div class="menu-management tenant_Page draHeaPB">
    <div class="tenant_content">
      <div class="tableTenBox flexRowAC">
        <div class="tableTenItU">
          <div class="depNameBox_out flexRowAC">
            <div class="depNameBox flexRowAC">
              <div class="exportBtnBox flexRowAC">
                <button type="button" class="exportBtn newBtn flexRowAC" @click="handleCreate">
                  <el-icon class="BtnImg"><Plus /></el-icon>
                  新建
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
            :data="tableData"
            row-key="id"
            :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" :width="clacPXToVW(55)" />
            <el-table-column prop="name" label="菜单名称" min-width="180" />
            <el-table-column prop="code" label="路由编号" min-width="120" />
            <el-table-column prop="alias" label="菜单别名" min-width="120" />
            <el-table-column prop="path" label="路由地址" min-width="150" show-overflow-tooltip />
            <el-table-column prop="categoryName" label="类型" :width="clacPXToVW(100)" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.category === 1 ? 'primary' : 'success'">
                  {{ scope.row.category === 1 ? '菜单' : scope.row.category === 2 ? '按钮' : '未知' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sort" label="排序" :width="clacPXToVW(80)" align="center" />
            <el-table-column prop="action" label="权限标识" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" :width="clacPXToVW(180)" fixed="right" align="right">
              <template #default="scope">
                <div class="operateAppBox flexRowAC" @click.stop>
                  <div class="new_table_svg_group" @click="handleEdit(scope.row)">
                    <oort-svg-icon width="14" height="14" name="edit_icon" class="new_table_svg_group_svg" />
                    <span>编辑</span>
                  </div>
                  <div class="new_table_svg_group" @click="handleSingleRemove(scope.row)">
                    <oort-svg-icon color="red" width="14" height="14" name="delete_icon" class="new_table_svg_group_svg" />
                    <span>删除</span>
                  </div>
                </div>
              </template>
            </el-table-column>
          </TableSelf>
        </div>
      </div>
    </div>

    <!-- 新增/编辑菜单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="35%"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        style="padding: 10px 20px"
      >
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="menuTreeOptions"
            node-key="id"
            :props="{ label: 'label', children: 'children' }"
            placeholder="请选择上级菜单 (不选则为根节点)"
            check-strictly
            style="width: 100%"
            clearable
          />
        </el-form-item>

        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入菜单名称" />
        </el-form-item>

        <el-form-item label="路由编号" prop="code">
          <el-input v-model="form.code" placeholder="请输入路由编号" />
        </el-form-item>

        <el-form-item label="菜单别名" prop="alias">
          <el-input v-model="form.alias" placeholder="请输入菜单别名" />
        </el-form-item>

        <el-form-item label="菜单类型" prop="category">
          <el-radio-group v-model="form.category">
            <el-radio :label="1">菜单</el-radio>
            <el-radio :label="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.category === 1" label="路由地址" prop="path">
          <el-input v-model="form.path" placeholder="请输入路由 path (如 /system/users)" />
        </el-form-item>

        <el-form-item label="权限标识" prop="action">
          <el-input v-model="form.action" placeholder="请输入权限标识 (如 system_user_list)" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="菜单排序" prop="sort">
              <el-input-number v-model="form.sort" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否新窗口" prop="isOpen">
              <el-switch
                v-model="form.isOpen"
                :active-value="1"
                :inactive-value="2"
                active-text="是"
                inactive-text="否"
                inline-prompt
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="菜单图标" prop="source">
          <el-input v-model="form.source" placeholder="请输入图标名称" />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" class="common_btn">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveSubmit" class="common_btn">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { clacPXToVW } from '@/utils/index'
import { getMenuList, submitMenu, removeMenus } from '@/api/system/menu'
import { buildTree, getPayload, normalizeTree, joinIds, isSuccess } from './utils/response'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const selectedRows = ref([])
const tableData = ref([])
const exportItem = ref({ isDisabledExcel: false })
const searchData = ref([
  { label: '菜单名称', value: 'name', type: 'text', default: '' },
  { label: '菜单编号', value: 'code', type: 'text', default: '' }
])

const toolbarButtonList = computed(() => [
  { name: '编辑', svg: 'table_edit', clickFn: handleToolbarEdit },
  { name: '删除', svg: 'table_del', clickFn: handleBatchRemove }
])

const menuTreeOptions = ref([])

const queryParams = reactive({
  name: '',
  code: ''
})

const formRef = ref()
const form = ref({
  id: undefined,
  parentId: '',
  code: '',
  name: '',
  alias: '',
  path: '',
  source: '',
  category: 1,
  action: '',
  isOpen: 2,
  sort: 1,
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入路由编号', trigger: 'blur' }],
  category: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMenuList({
      name: queryParams.name,
      code: queryParams.code
    })
    const rawList = getPayload(res) || []

    const tree = buildTree(rawList, [0, '0', null, undefined])
    tableData.value = normalizeTree(tree)
    menuTreeOptions.value = normalizeTree(tree)
  } catch (error) {
    console.error('加载菜单列表失败:', error)
    ElMessage.error('加载菜单列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function handleReset() {
  queryParams.name = ''
  queryParams.code = ''
  loadData()
}

const searchResetFn = (val, reset) => {
  if (reset && !(val && (val.keyword || val.name || val.code))) {
    handleReset()
    return
  }
  queryParams.name = val?.name || val?.keyword || ''
  queryParams.code = val?.code || ''
  handleSearch()
}

const handleExport = () => {
  ElMessage.success('导出数据')
}

function handleToolbarEdit() {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning('请选择一条记录进行编辑')
    return
  }
  handleEdit(selectedRows.value[0])
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

function handleCreate() {
  dialogTitle.value = '新增菜单'
  form.value = {
    id: undefined,
    parentId: '',
    code: '',
    name: '',
    alias: '',
    path: '',
    source: '',
    category: 1,
    action: '',
    isOpen: 2,
    sort: 1,
    remark: ''
  }
  dialogVisible.value = true
}

function handleEdit(row) {
  if (!row) return
  form.value = { ...row }
  dialogTitle.value = '编辑菜单'
  dialogVisible.value = true
}

async function handleSaveSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        const res = await submitMenu(form.value)
        if (isSuccess(res)) {
          ElMessage.success('保存菜单成功')
          dialogVisible.value = false
          loadData()
        } else {
          ElMessage.error(res?.msg || '保存失败')
        }
      } catch (error) {
        console.error('提交菜单表单异常:', error)
        ElMessage.error('提交菜单表单异常')
      } finally {
        saving.value = false
      }
    }
  })
}

function executeRemove(ids, msg) {
  ElMessageBox.confirm(msg, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await removeMenus(ids)
      if (isSuccess(res)) {
        ElMessage.success('删除菜单成功')
        loadData()
      } else {
        ElMessage.error(res?.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除菜单异常:', error)
      ElMessage.error('删除菜单异常')
    }
  }).catch(() => {})
}

function handleSingleRemove(row) {
  executeRemove(String(row.id), `确定删除菜单 [${row.name}] 吗？`)
}

function handleBatchRemove() {
  if (selectedRows.value.length === 0) return
  const ids = joinIds(selectedRows.value)
  executeRemove(ids, `确定删除选中的 ${selectedRows.value.length} 个菜单吗？`)
}

onMounted(() => {
  loadData()
})
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

.tableTenItU {
  flex: 1;
  height: 100%;
  overflow: auto;
  min-width: 0;

  :deep(.header_tenant_cell) {
    background: #F8F8F9;
  }
}

.operateAppBox {
  justify-content: flex-end;
  gap: 2px;
}

.menu-management {
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
