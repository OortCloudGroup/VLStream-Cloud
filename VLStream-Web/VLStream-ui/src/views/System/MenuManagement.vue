<template>
  <SystemPageShell>
    <!-- 顶部工具栏 -->
    <template #toolbar>
      <div class="toolbar-left">
        <!-- 扁平页面简单说明或过滤表单 -->
        <el-form :inline="true" :model="queryParams" size="default">
          <el-form-item label="菜单名称">
            <el-input v-model="queryParams.name" placeholder="请输入菜单名称" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="菜单编号">
            <el-input v-model="queryParams.code" placeholder="请输入菜单编号" clearable @keyup.enter="handleSearch" />
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

    <!-- 菜单树形列表表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      row-key="id"
      stripe
      style="width: 100%; height: 100%"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="name" label="菜单名称" min-width="180" />
      <el-table-column prop="code" label="路由编号" min-width="120" />
      <el-table-column prop="alias" label="菜单别名" min-width="120" />
      <el-table-column prop="path" label="路由地址" min-width="150" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="类型" width="100" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.category === 1 ? 'primary' : 'success'">
            {{ scope.row.category === 1 ? '菜单' : scope.row.category === 2 ? '按钮' : '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column prop="action" label="权限标识" min-width="150" show-overflow-tooltip />
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

  <!-- 新增/编辑菜单对话框 -->
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="600px"
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
import { getMenuList, submitMenu, removeMenus } from '@/api/system/menu'
import { buildTree, getPayload, normalizeTree, joinIds, isSuccess } from './utils/response'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const selectedRows = ref([])
const tableData = ref([])

// 上级菜单树选项
const menuTreeOptions = ref([])

// 搜索条件
const queryParams = reactive({
  name: '',
  code: ''
})

// 表单对象与表单校验规则
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

/**
 * 异步查询所有菜单和按钮的扁平列表，使用 buildTree 工具整理成嵌套树状，并为 Element 规范化 label 字段
 */
async function loadData() {
  loading.value = true
  try {
    const res = await getMenuList({
      name: queryParams.name,
      code: queryParams.code
    })
    const rawList = getPayload(res) || []
    
    // 重构成树形结构。SpringBlade 菜单也是 parentId 的分级树
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

/**
 * 触发搜索
 */
function handleSearch() {
  loadData()
}

/**
 * 重置搜索过滤，重新加载
 */
function handleReset() {
  queryParams.name = ''
  queryParams.code = ''
  loadData()
}

/**
 * 复选框改变回调
 * @param {Array} rows 选中的行
 */
function handleSelectionChange(rows) {
  selectedRows.value = rows
}

/**
 * 打开新增菜单弹窗，初始化表单数据
 */
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

/**
 * 打开编辑菜单弹窗，克隆行数据回显
 * @param {Object} row 行数据
 */
function handleEdit(row) {
  if (!row) return
  form.value = { ...row }
  dialogTitle.value = '编辑菜单'
  dialogVisible.value = true
}

/**
 * 确定保存，对表单做必填校验后调用后端 API
 */
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

/**
 * 触发删除逻辑，包含二次确认层
 * @param {String} ids 逗号拼接的菜单ID
 * @param {String} msg 确认消息内容
 */
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

/**
 * 行内单条删除菜单
 * @param {Object} row 待删除行
 */
function handleSingleRemove(row) {
  executeRemove(String(row.id), `确定删除菜单 [${row.name}] 吗？`)
}

/**
 * 批量删除已选中行的菜单项
 */
function handleBatchRemove() {
  if (selectedRows.value.length === 0) return
  const ids = joinIds(selectedRows.value)
  executeRemove(ids, `确定删除选中的 ${selectedRows.value.length} 个菜单吗？`)
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
