<template>
  <SystemPageShell>
    <!-- 顶部工具栏 -->
    <template #toolbar>
      <div class="toolbar-left">
        <el-form :inline="true" :model="queryParams" size="default">
          <el-form-item label="接口范围名称">
            <el-input v-model="queryParams.scopeName" placeholder="请输入范围名称" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="资源编号">
            <el-input v-model="queryParams.resourceCode" placeholder="请输入资源编号" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon> 查询
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon> 重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="toolbar-right">
        <!-- 按钮操作组 -->
        <ActionButtonGroup
          :selected-count="selectedRows.length"
          @add="handleCreate"
          @edit="handleEdit(selectedRows[0])"
          @delete="handleBatchRemove"
        />
      </div>
    </template>

    <!-- 接口权限列表表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      stripe
      style="width: 100%; height: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="scopeName" label="范围名称" min-width="120" />
      <el-table-column prop="resourceCode" label="资源编号" min-width="120" />
      <el-table-column prop="scopePath" label="接口路径" min-width="180" show-overflow-tooltip />
      <el-table-column prop="scopeType" label="规则类型" width="120" align="center">
        <template #default="scope">
          <span>{{ getScopeTypeName(scope.row.scopeType) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right" align="right">
        <template #default="scope">
          <div class="operation-buttons">
            <el-button class="operation-btn edit-btn" @click="handleEdit(scope.row)">
              编辑
            </el-button>
            <el-button class="operation-btn delete-btn" @click="handleSingleRemove(scope.row)">
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 底部分页 -->
    <template #pagination>
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </template>
  </SystemPageShell>

  <!-- 新增/编辑接口权限对话框 -->
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="540px"
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
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSaveSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import SystemPageShell from './components/SystemPageShell.vue'
import ActionButtonGroup from '@/components/ActionButtonGroup.vue'
import { getApiScopeList, getApiScopeDetail, submitApiScope, removeApiScopes } from '@/api/system/apiScope'
import { getMenuOnlyList } from '@/api/system/menu'
import { buildTree, getPayload, getRecords, getTotal, normalizeTree, joinIds, isSuccess } from './utils/response'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增接口权限')
const selectedRows = ref([])
const tableData = ref([])

// 选项下拉数据
const menuOptions = ref([])

// 分页数据
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 过滤参数
const queryParams = reactive({
  scopeName: '',
  resourceCode: ''
})

// 表单对象与表单校验规则
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

/**
 * 格式化规则类型展示名称
 * @param {Number} type 规则类型
 * @returns {String} 规则展示文字
 */
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

/**
 * 异步拉取菜单仅含菜单类型的数据
 */
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

/**
 * 异步查询分页接口权限数据
 */
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

/**
 * 搜索过滤
 */
function handleSearch() {
  pagination.current = 1
  loadData()
}

/**
 * 重置查询过滤项
 */
function handleReset() {
  queryParams.scopeName = ''
  queryParams.resourceCode = ''
  pagination.current = 1
  loadData()
}

/**
 * 分页大小改变时重新拉取数据
 * @param {Number} size 分页大小
 */
function handleSizeChange(size) {
  pagination.size = size
  pagination.current = 1
  loadData()
}

/**
 * 页码变化
 * @param {Number} current 页码
 */
function handleCurrentChange(current) {
  pagination.current = current
  loadData()
}

/**
 * 表格行勾选变化回调
 * @param {Array} rows 选中的所有行对象
 */
function handleSelectionChange(rows) {
  selectedRows.value = rows
}

/**
 * 打开新增接口权限对话框
 */
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

/**
 * 打开修改接口权限对话框，获取最新接口详情进行回显
 * @param {Object} row 行数据
 */
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

/**
 * 确定保存，做完整性检验后提交
 */
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

/**
 * 调用后端 API 删除接口权限规则，包含二次确定
 * @param {String} ids 逗号拼接的ID字符串
 * @param {String} msg 确认消息提示
 */
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

/**
 * 行内单个删除接口权限
 * @param {Object} row 待删除行
 */
function handleSingleRemove(row) {
  executeRemove(String(row.id), `确定删除接口权限规则 [${row.scopeName}] 吗？`)
}

/**
 * 批量删除选中的所有接口权限行
 */
function handleBatchRemove() {
  if (selectedRows.value.length === 0) return
  const ids = joinIds(selectedRows.value)
  executeRemove(ids, `确定删除选中的 ${selectedRows.value.length} 个规则吗？`)
}

// 挂载数据
onMounted(() => {
  loadData()
  loadMenus()
})
</script>

<style scoped>
.operation-buttons {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.operation-btn {
  height: 28px !important;
  padding: 0 12px !important;
  font-size: 12px !important;
  border-radius: 14px !important;
  font-weight: 500 !important;
}

.edit-btn {
  background: #ffffff !important;
  color: #1A53FF !important;
  border: 1px solid #1A53FF !important;
}

.edit-btn:hover {
  background: #f0f4ff !important;
}

.delete-btn {
  background: #ffffff !important;
  color: #f56c6c !important;
  border: 1px solid #d9d9d9 !important;
}

.delete-btn:hover {
  border-color: #f56c6c !important;
  color: #f56c6c !important;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.el-form-item {
  margin-bottom: 0;
  margin-right: 16px;
}
</style>
