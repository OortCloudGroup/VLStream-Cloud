<template>
  <div class="role-management tenant_Page draHeaPB">
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
            default-expand-all
            :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" :width="clacPXToVW(55)" />
            <el-table-column prop="roleName" label="角色名称" min-width="180" />
            <el-table-column prop="roleAlias" label="角色别名" min-width="150" />
            <el-table-column prop="sort" label="排序" :width="clacPXToVW(80)" align="center" />
            <el-table-column label="操作" :width="clacPXToVW(220)" fixed="right" align="right">
              <template #default="scope">
                <div class="operateAppBox flexRowAC" @click.stop>
                  <div class="new_table_svg_group" @click="handleEdit(scope.row)">
                    <oort-svg-icon width="20" height="20" name="edit_icon" class="new_table_svg_group_svg" />
                    <span>编辑</span>
                  </div>
                  <div class="new_table_svg_group" @click="handleSingleRemove(scope.row)">
                    <oort-svg-icon color="red" width="20" height="20" name="delete_icon" class="new_table_svg_group_svg" />
                    <span>删除</span>
                  </div>
                  <div class="new_table_svg_group" @click="handleOpenGrant(scope.row)">
                    <oort-svg-icon width="20" height="20" name="more" class="new_table_svg_group_svg" />
                    <span>授权</span>
                  </div>
                </div>
              </template>
            </el-table-column>
          </TableSelf>
        </div>
      </div>
    </div>

    <!-- 新增/编辑角色对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="30%"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        style="padding: 10px 20px"
      >
        <el-form-item label="上级角色" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="roleTreeOptions"
            node-key="id"
            :props="{ label: 'label', children: 'children' }"
            placeholder="请选择上级角色"
            check-strictly
            style="width: 100%"
            clearable
          />
        </el-form-item>

        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>

        <el-form-item label="角色别名" prop="roleAlias">
          <el-input v-model="form.roleAlias" placeholder="请输入角色别名" />
        </el-form-item>

        <el-form-item label="角色排序" prop="sort">
          <el-input-number v-model="form.sort" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" class="common_btn">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveSubmit" class="common_btn">确定</el-button>
      </template>
    </el-dialog>

    <!-- 复合权限授权弹窗 -->
    <PermissionGrantDialog
      v-model="grantVisible"
      :role="activeRole"
      @success="loadData"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { clacPXToVW } from '@/utils/index'
import PermissionGrantDialog from './components/PermissionGrantDialog.vue'
import { getRoleList, submitRole, removeRoles } from '@/api/system/role'
import { buildTree, getPayload, normalizeTree, joinIds, isSuccess } from './utils/response'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const selectedRows = ref([])
const tableData = ref([])
const exportItem = ref({ isDisabledExcel: false })
const searchData = ref([
  { label: '角色名称', value: 'roleName', type: 'text', default: '' },
  { label: '角色别名', value: 'roleAlias', type: 'text', default: '' }
])

const toolbarButtonList = computed(() => [
  { name: '编辑', svg: 'table_edit', clickFn: handleToolbarEdit },
  { name: '删除', svg: 'table_del', clickFn: handleBatchRemove },
  { name: '权限配置', svg: 'more', clickFn: handleToolbarGrant }
])

// 选项下拉列表数据
const roleTreeOptions = ref([])

// 查询参数
const queryParams = reactive({
  roleName: '',
  roleAlias: ''
})

// 表单对象与表单校验规则
const formRef = ref()
const form = ref({
  id: undefined,
  tenantId: '',
  parentId: '',
  roleName: '',
  roleAlias: '',
  sort: 1
})

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleAlias: [{ required: true, message: '请输入角色别名', trigger: 'blur' }]
}

// 授权弹窗状态
const grantVisible = ref(false)
const activeRole = ref(null)

/**
 * 异步查询角色列表数据，若后端返回扁平格式则通过 buildTree 重构为树，最后规范化属性名
 */
async function loadData() {
  loading.value = true
  try {
    const res = await getRoleList({
      roleName: queryParams.roleName,
      roleAlias: queryParams.roleAlias
    })
    const rawList = getPayload(res) || []

    const tree = buildTree(rawList, [0, '0', null, undefined])
    tableData.value = normalizeTree(tree)
    roleTreeOptions.value = normalizeTree(tree)
  } catch (error) {
    console.error('获取角色列表失败:', error)
    ElMessage.error('获取角色列表数据失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function handleReset() {
  queryParams.roleName = ''
  queryParams.roleAlias = ''
  loadData()
}

const searchResetFn = (val, reset) => {
  if (reset && !(val && (val.keyword || val.roleName || val.roleAlias))) {
    handleReset()
    return
  }
  queryParams.roleName = val?.roleName || val?.keyword || ''
  queryParams.roleAlias = val?.roleAlias || ''
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

function handleToolbarGrant() {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning('请选择一条记录进行权限配置')
    return
  }
  handleOpenGrant(selectedRows.value[0])
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

function handleCreate() {
  dialogTitle.value = '新增角色'
  form.value = {
    id: undefined,
    tenantId: '',
    parentId: '',
    roleName: '',
    roleAlias: '',
    sort: 1
  }
  dialogVisible.value = true
}

function handleEdit(row) {
  if (!row) return
  form.value = { ...row }
  dialogTitle.value = '编辑角色'
  dialogVisible.value = true
}

async function handleSaveSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        const res = await submitRole(form.value)
        if (isSuccess(res)) {
          ElMessage.success('保存角色成功')
          dialogVisible.value = false
          loadData()
        } else {
          ElMessage.error(res?.msg || '保存失败')
        }
      } catch (error) {
        console.error('提交角色信息异常:', error)
        ElMessage.error('提交角色信息异常')
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
      const res = await removeRoles(ids)
      if (isSuccess(res)) {
        ElMessage.success('删除成功')
        loadData()
      } else {
        ElMessage.error(res?.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除角色异常:', error)
      ElMessage.error('删除角色异常')
    }
  }).catch(() => {})
}

function handleSingleRemove(row) {
  executeRemove(String(row.id), `确定删除角色 [${row.roleName}] 吗？`)
}

function handleBatchRemove() {
  if (selectedRows.value.length === 0) return
  const ids = joinIds(selectedRows.value)
  executeRemove(ids, `确定删除选中的 ${selectedRows.value.length} 个角色吗？`)
}

function handleOpenGrant(row) {
  activeRole.value = row
  grantVisible.value = true
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

.role-management {
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
