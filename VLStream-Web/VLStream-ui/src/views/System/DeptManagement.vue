<template>
  <SystemPageShell>
    <!-- 顶部工具栏 -->
    <template #toolbar>
      <div class="toolbar-left">
        <el-form :inline="true" :model="queryParams" size="default">
          <el-form-item label="部门名称">
            <el-input v-model="queryParams.deptName" placeholder="请输入部门名称" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="部门全称">
            <el-input v-model="queryParams.fullName" placeholder="请输入部门全称" clearable @keyup.enter="handleSearch" />
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

    <!-- 树形表格区域 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      row-key="id"
      stripe
      style="width: 100%; height: 100%"
      default-expand-all
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="deptName" label="部门名称" min-width="180" />
      <el-table-column prop="fullName" label="部门全称" min-width="200" />
      <el-table-column prop="sort" label="排序" width="80" align="center" />
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
  </SystemPageShell>

  <!-- 新增/编辑部门对话框 -->
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
      <el-form-item label="上级部门" prop="parentId">
        <el-tree-select
          v-model="form.parentId"
          :data="deptTreeOptions"
          node-key="id"
          :props="{ label: 'label', children: 'children' }"
          placeholder="请选择上级部门"
          check-strictly
          style="width: 100%"
          clearable
        />
      </el-form-item>

      <el-form-item label="部门名称" prop="deptName">
        <el-input v-model="form.deptName" placeholder="请输入部门名称" />
      </el-form-item>

      <el-form-item label="部门全称" prop="fullName">
        <el-input v-model="form.fullName" placeholder="请输入部门全称" />
      </el-form-item>

      <el-form-item label="部门排序" prop="sort">
        <el-input-number v-model="form.sort" :min="1" style="width: 100%" />
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
import { getDeptList, submitDept, removeDepts } from '@/api/system/dept'
import { buildTree, getPayload, normalizeTree, joinIds, isSuccess } from './utils/response'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const selectedRows = ref([])
const tableData = ref([])

// 选项下拉数据
const deptTreeOptions = ref([])

// 查询过滤参数
const queryParams = reactive({
  deptName: '',
  fullName: ''
})

// 表单对象与表单校验规则
const formRef = ref()
const form = ref({
  id: undefined,
  tenantId: '',
  parentId: '',
  deptName: '',
  fullName: '',
  sort: 1,
  remark: ''
})

const rules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  fullName: [{ required: true, message: '请输入部门全称', trigger: 'blur' }]
}

/**
 * 异步查询部门数据列表，若为扁平结构通过 buildTree 转换为树形，最后规范化 label 并渲染至表格
 */
async function loadData() {
  loading.value = true
  try {
    const res = await getDeptList({
      deptName: queryParams.deptName,
      fullName: queryParams.fullName
    })
    const rawList = getPayload(res) || []
    
    // 部门分级树，基于 parentId 重组
    const tree = buildTree(rawList, [0, '0', null, undefined])
    tableData.value = normalizeTree(tree)
    deptTreeOptions.value = normalizeTree(tree)
  } catch (error) {
    console.error('获取部门列表失败:', error)
    ElMessage.error('获取部门数据失败')
  } finally {
    loading.value = false
  }
}

/**
 * 触发搜索
 */
function handleSearch() {
  loadData()
}

/**
 * 重置查询过滤，重新拉取列表
 */
function handleReset() {
  queryParams.deptName = ''
  queryParams.fullName = ''
  loadData()
}

/**
 * 表格行多选变化回调
 * @param {Array} rows 选中的所有行对象
 */
function handleSelectionChange(rows) {
  selectedRows.value = rows
}

/**
 * 打开新增部门弹窗，清空表单
 */
function handleCreate() {
  dialogTitle.value = '新增部门'
  form.value = {
    id: undefined,
    tenantId: '',
    parentId: '',
    deptName: '',
    fullName: '',
    sort: 1,
    remark: ''
  }
  dialogVisible.value = true
}

/**
 * 打开编辑部门弹窗，复制行数据回显
 * @param {Object} row 行数据
 */
function handleEdit(row) {
  if (!row) return
  form.value = { ...row }
  dialogTitle.value = '编辑部门'
  dialogVisible.value = true
}

/**
 * 确定并保存表单配置
 */
async function handleSaveSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        const res = await submitDept(form.value)
        if (isSuccess(res)) {
          ElMessage.success('保存部门成功')
          dialogVisible.value = false
          loadData()
        } else {
          ElMessage.error(res?.msg || '保存失败')
        }
      } catch (error) {
        console.error('提交部门数据失败:', error)
        ElMessage.error('提交部门数据失败')
      } finally {
        saving.value = false
      }
    }
  })
}

/**
 * 发起后台删除调用，带确认层
 * @param {String} ids 逗号拼接的部门ID
 * @param {String} msg 确认弹出消息
 */
function executeRemove(ids, msg) {
  ElMessageBox.confirm(msg, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await removeDepts(ids)
      if (isSuccess(res)) {
        ElMessage.success('删除成功')
        loadData()
      } else {
        ElMessage.error(res?.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除部门操作失败:', error)
      ElMessage.error('删除部门操作失败')
    }
  }).catch(() => {})
}

/**
 * 单行内删除部门
 * @param {Object} row 目标行数据
 */
function handleSingleRemove(row) {
  executeRemove(String(row.id), `确定删除部门 [${row.deptName}] 吗？`)
}

/**
 * 批量删除已勾选的所有部门
 */
function handleBatchRemove() {
  if (selectedRows.value.length === 0) return
  const ids = joinIds(selectedRows.value)
  executeRemove(ids, `确定删除选中的 ${selectedRows.value.length} 个部门吗？`)
}

// 挂载时加载数据
onMounted(() => {
  loadData()
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
