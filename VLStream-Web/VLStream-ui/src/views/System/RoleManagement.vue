<template>
  <SystemPageShell>
    <!-- 顶部工具栏 -->
    <template #toolbar>
      <div class="toolbar-left">
        <el-form :inline="true" :model="queryParams" size="default">
          <el-form-item label="角色名称">
            <el-input v-model="queryParams.roleName" placeholder="请输入角色名称" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="角色别名">
            <el-input v-model="queryParams.roleAlias" placeholder="请输入角色别名" clearable @keyup.enter="handleSearch" />
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
        <!-- 基础操作按钮组 -->
        <ActionButtonGroup
          :selected-count="selectedRows.length"
          @add="handleCreate"
          @edit="handleEdit(selectedRows[0])"
          @delete="handleBatchRemove"
        >
          <template #extra-buttons>
            <el-button
              type="warning"
              plain
              :disabled="selectedRows.length !== 1"
              @click="handleOpenGrant(selectedRows[0])"
            >
              权限配置
            </el-button>
          </template>
        </ActionButtonGroup>
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
      <el-table-column prop="roleName" label="角色名称" min-width="180" />
      <el-table-column prop="roleAlias" label="角色别名" min-width="150" />
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column label="操作" width="220" fixed="right" align="right">
        <template #default="scope">
          <div class="operation-buttons">
            <el-button class="operation-btn edit-btn" @click="handleEdit(scope.row)">
              编辑
            </el-button>
            <el-button class="operation-btn delete-btn" @click="handleSingleRemove(scope.row)">
              删除
            </el-button>
            <el-button class="operation-btn view-btn" @click="handleOpenGrant(scope.row)">
              授权
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </SystemPageShell>

  <!-- 新增/编辑角色对话框 -->
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
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSaveSubmit">确定</el-button>
    </template>
  </el-dialog>

  <!-- 复合权限授权弹窗 -->
  <PermissionGrantDialog
    v-model="grantVisible"
    :role="activeRole"
    @success="loadData"
  />
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import SystemPageShell from './components/SystemPageShell.vue'
import PermissionGrantDialog from './components/PermissionGrantDialog.vue'
import ActionButtonGroup from '@/components/ActionButtonGroup.vue'
import { getRoleList, submitRole, removeRoles } from '@/api/system/role'
import { buildTree, getPayload, normalizeTree, joinIds, isSuccess } from './utils/response'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const selectedRows = ref([])
const tableData = ref([])

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
    
    // SpringBlade 角色数据是分级的。如果数据带有 parentId，我们用 buildTree 转换为树形结构
    const tree = buildTree(rawList, [0, '0', null, undefined])
    tableData.value = normalizeTree(tree)
    roleTreeOptions.value = normalizeTree(tree)
  } catch (error) {
    console.error('获取角色列表失败:', error)
    ElMessage.ElMessage?.error('获取角色列表数据失败')
  } finally {
    loading.value = false
  }
}

/**
 * 执行查询操作
 */
function handleSearch() {
  loadData()
}

/**
 * 重置查询过滤项，重新拉取列表
 */
function handleReset() {
  queryParams.roleName = ''
  queryParams.roleAlias = ''
  loadData()
}

/**
 * 表格行选中变化回调
 * @param {Array} rows 选中的所有行对象
 */
function handleSelectionChange(rows) {
  selectedRows.value = rows
}

/**
 * 打开新增角色弹窗，重置表单为默认值
 */
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

/**
 * 打开编辑角色弹窗，克隆行数据至表单
 * @param {Object} row 待编辑的角色行数据
 */
function handleEdit(row) {
  if (!row) return
  form.value = { ...row }
  dialogTitle.value = '编辑角色'
  dialogVisible.value = true
}

/**
 * 提交保存角色新增或更新结果
 */
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

/**
 * 调用后端 remove 接口物理或逻辑删除选中的角色记录
 * @param {String} ids 逗号拼接的角色 ID 列表
 * @param {String} msg 确认弹出层中的文本内容
 */
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

/**
 * 行内单个删除角色操作
 * @param {Object} row 待删除行数据
 */
function handleSingleRemove(row) {
  executeRemove(String(row.id), `确定删除角色 [${row.roleName}] 吗？`)
}

/**
 * 批量删除选中的所有角色行
 */
function handleBatchRemove() {
  if (selectedRows.value.length === 0) return
  const ids = joinIds(selectedRows.value)
  executeRemove(ids, `确定删除选中的 ${selectedRows.value.length} 个角色吗？`)
}

/**
 * 打开给角色授权权限的弹窗
 * @param {Object} row 目标角色数据
 */
function handleOpenGrant(row) {
  activeRole.value = row
  grantVisible.value = true
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

.view-btn {
  background: #ffffff !important;
  color: #e6a23c !important;
  border: 1px solid #e6a23c !important;
}

.view-btn:hover {
  background: #fdf6ec !important;
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
