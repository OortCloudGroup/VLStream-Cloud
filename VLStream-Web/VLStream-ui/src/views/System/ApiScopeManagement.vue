<template>
  <div class="api-scope-management tenant_Page draHeaPB">
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
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" :width="clacPXToVW(55)" />
            <el-table-column label="序号" :width="clacPXToVW(65)">
              <template #default="scope">
                {{ scope.$index + (pagination.current - 1) * pagination.size + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="scopeName" label="范围名称" min-width="120" />
            <el-table-column prop="resourceCode" label="资源编号" min-width="120" />
            <el-table-column prop="scopePath" label="接口路径" min-width="180" show-overflow-tooltip />
            <el-table-column prop="scopeType" label="规则类型" :width="clacPXToVW(120)" align="center">
              <template #default="scope">
                <span>{{ getScopeTypeName(scope.row.scopeType) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" :width="clacPXToVW(180)" fixed="right" align="right">
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
                </div>
              </template>
            </el-table-column>
          </TableSelf>

          <div class="paginationBox flexRowAC">
            <el-pagination
              background
              :current-page="pagination.current"
              :page-size="pagination.size"
              :page-sizes="[10, 20, 50, 100]"
              :total="pagination.total"
              layout="total, prev, pager, next, sizes"
              class="justifyAlign"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑接口权限对话框 -->
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
        <el-form-item label="所属菜单" prop="menuId">
          <el-tree-select
            v-model="form.menuId"
            :data="menuOptions"
            node-key="id"
            :props="{ label: 'label', children: 'children' }"
            placeholder="请选择关联菜单"
            check-strictly
            style="width: 100%"
            clearable
          />
        </el-form-item>

        <el-form-item label="资源编号" prop="resourceCode">
          <el-input v-model="form.resourceCode" placeholder="请输入资源编号" />
        </el-form-item>

        <el-form-item label="范围名称" prop="scopeName">
          <el-input v-model="form.scopeName" placeholder="请输入范围名称" />
        </el-form-item>

        <el-form-item label="接口路径" prop="scopePath">
          <el-input v-model="form.scopePath" placeholder="请输入 API 请求接口路径 (如 /api/v1/user)" />
        </el-form-item>

        <el-form-item label="规则类型" prop="scopeType">
          <el-select v-model="form.scopeType" placeholder="请选择类型" style="width: 100%">
            <el-option :value="1" label="全部可见" />
            <el-option :value="2" label="本人可见" />
            <el-option :value="3" label="本部门可见" />
            <el-option :value="4" label="本部门及子部门可见" />
            <el-option :value="5" label="自定义可见" />
          </el-select>
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
import { getApiScopeList, getApiScopeDetail, submitApiScope, removeApiScopes } from '@/api/system/apiScope'
import { getMenuOnlyList } from '@/api/system/menu'
import { buildTree, getPayload, getRecords, getTotal, normalizeTree, joinIds, isSuccess } from './utils/response'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增接口权限')
const selectedRows = ref([])
const tableData = ref([])
const exportItem = ref({ isDisabledExcel: false })
const searchData = ref([
  { label: '范围名称', value: 'scopeName', type: 'text', default: '' },
  { label: '资源编号', value: 'resourceCode', type: 'text', default: '' }
])

const toolbarButtonList = computed(() => [
  { name: '编辑', svg: 'table_edit', clickFn: handleToolbarEdit },
  { name: '删除', svg: 'table_del', clickFn: handleBatchRemove }
])

const menuOptions = ref([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const queryParams = reactive({
  scopeName: '',
  resourceCode: ''
})

const formRef = ref()
const form = ref({
  id: undefined,
  menuId: '',
  resourceCode: '',
  scopeName: '',
  scopePath: '',
  scopeType: 1,
  remark: ''
})

const rules = {
  menuId: [{ required: true, message: '请关联菜单', trigger: 'change' }],
  resourceCode: [{ required: true, message: '请输入资源编号', trigger: 'blur' }],
  scopeName: [{ required: true, message: '请输入范围名称', trigger: 'blur' }]
}

function getScopeTypeName(type) {
  const map = {
    1: '全部可见',
    2: '本人可见',
    3: '本部门可见',
    4: '本部门及子部门可见',
    5: '自定义可见'
  }
  return map[type] || '未分配'
}

async function loadMenus() {
  try {
    const res = await getMenuOnlyList()
    const rawList = getPayload(res) || []
    const tree = buildTree(rawList, [0, '0', null, undefined])
    menuOptions.value = normalizeTree(tree)
  } catch (error) {
    console.error('加载关联菜单数据失败:', error)
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getApiScopeList({
      current: pagination.current,
      size: pagination.size,
      scopeName: queryParams.scopeName,
      resourceCode: queryParams.resourceCode
    })
    tableData.value = getRecords(res)
    pagination.total = getTotal(res)
  } catch (error) {
    console.error('加载接口权限数据异常:', error)
    ElMessage.error('加载接口权限数据失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  queryParams.scopeName = ''
  queryParams.resourceCode = ''
  pagination.current = 1
  loadData()
}

const searchResetFn = (val, reset) => {
  if (reset && !(val && (val.keyword || val.scopeName || val.resourceCode))) {
    handleReset()
    return
  }
  queryParams.scopeName = val?.scopeName || val?.keyword || ''
  queryParams.resourceCode = val?.resourceCode || ''
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

function handleSizeChange(size) {
  pagination.size = size
  pagination.current = 1
  loadData()
}

function handleCurrentChange(current) {
  pagination.current = current
  loadData()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

function handleCreate() {
  dialogTitle.value = '新增接口权限'
  form.value = {
    id: undefined,
    menuId: '',
    resourceCode: '',
    scopeName: '',
    scopePath: '',
    scopeType: 1,
    remark: ''
  }
  dialogVisible.value = true
}

async function handleEdit(row) {
  if (!row?.id) return
  try {
    const res = await getApiScopeDetail({ id: row.id })
    const payload = getPayload(res)
    if (payload) {
      form.value = { ...payload }
    } else {
      form.value = { ...row }
    }
    dialogTitle.value = '编辑接口权限'
    dialogVisible.value = true
  } catch (error) {
    console.warn('拉取接口权限详情失败，使用行数据降级展示', error)
    form.value = { ...row }
    dialogTitle.value = '编辑接口权限'
    dialogVisible.value = true
  }
}

async function handleSaveSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        const res = await submitApiScope(form.value)
        if (isSuccess(res)) {
          ElMessage.success('保存成功')
          dialogVisible.value = false
          loadData()
        } else {
          ElMessage.error(res?.msg || '保存失败')
        }
      } catch (error) {
        console.error('提交接口权限规则异常:', error)
        ElMessage.error('保存接口权限发生异常')
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
      const res = await removeApiScopes(ids)
      if (isSuccess(res)) {
        ElMessage.success('删除成功')
        loadData()
      } else {
        ElMessage.error(res?.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除接口权限规则异常:', error)
      ElMessage.error('删除接口权限操作失败')
    }
  }).catch(() => {})
}

function handleSingleRemove(row) {
  executeRemove(String(row.id), `确定删除接口权限规则 [${row.scopeName}] 吗？`)
}

function handleBatchRemove() {
  if (selectedRows.value.length === 0) return
  const ids = joinIds(selectedRows.value)
  executeRemove(ids, `确定删除选中的 ${selectedRows.value.length} 个规则吗？`)
}

onMounted(() => {
  loadData()
  loadMenus()
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

.paginationBox {
  justify-content: center;
  height: 100px;
}

.operateAppBox {
  justify-content: flex-end;
  gap: 2px;
}

.api-scope-management {
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
