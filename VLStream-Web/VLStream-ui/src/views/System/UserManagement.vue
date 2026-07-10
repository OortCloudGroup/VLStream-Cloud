<template>
  <SystemPageShell>
    <!-- 顶部工具栏 -->
    <template #toolbar>
      <div class="toolbar-left">
        <!-- 搜索表单 -->
        <el-form :inline="true" :model="queryParams" class="demo-form-inline" size="default">
          <el-form-item label="账号">
            <el-input v-model="queryParams.account" placeholder="请输入账号" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="queryParams.realName" placeholder="请输入姓名" clearable @keyup.enter="handleSearch" />
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
        <!-- 操作按钮组 -->
        <ActionButtonGroup
          :selected-count="selectedRows.length"
          @add="handleCreate"
          @edit="handleEdit(selectedRows[0])"
          @delete="handleBatchRemove"
        >
          <template #extra-buttons>
            <el-button 
              type="success"
              plain
              :disabled="selectedRows.length !== 1"
              @click="handleOpenGrant(selectedRows[0])"
            >
              分配角色
            </el-button>
          </template>
        </ActionButtonGroup>
      </div>
    </template>

    <!-- 表格区域 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      stripe
      style="width: 100%; height: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="account" label="账号" min-width="120" />
      <el-table-column prop="name" label="昵称" min-width="120" />
      <el-table-column prop="realName" label="姓名" min-width="120" />
      <el-table-column prop="email" label="邮箱" min-width="150" />
      <el-table-column prop="phone" label="手机" min-width="120" />
      <el-table-column prop="sexName" label="性别" width="80" align="center">
        <template #default="scope">
          <span>{{ scope.row.sex === 1 ? '男' : scope.row.sex === 2 ? '女' : '未知' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="roleName" label="角色" min-width="150" show-overflow-tooltip />
      <el-table-column prop="deptName" label="部门" min-width="150" show-overflow-tooltip />
      <el-table-column prop="postName" label="岗位" min-width="150" show-overflow-tooltip />
      
      <!-- 操作列 -->
      <el-table-column label="操作" width="240" fixed="right" align="right">
        <template #default="scope">
          <div class="operation-buttons">
            <el-button class="operation-btn edit-btn" @click="handleEdit(scope.row)">
              编辑
            </el-button>
            <el-button class="operation-btn delete-btn" @click="handleSingleRemove(scope.row)">
              删除
            </el-button>
            
            <!-- “更多”下拉操作，包含重置密码和解锁账号 -->
            <el-dropdown trigger="click" style="margin-left: 8px">
              <el-button class="operation-btn view-btn">
                更多 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleResetPassword(scope.row)">重置密码</el-dropdown-item>
                  <el-dropdown-item @click="handleUnlock(scope.row)">解锁账号</el-dropdown-item>
                  <el-dropdown-item @click="handleOpenGrant(scope.row)">分配角色</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页区域 -->
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

  <!-- 新增/编辑用户对话框 -->
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="680px"
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      style="padding: 10px 20px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="用户账号" prop="account">
            <el-input v-model="form.account" placeholder="请输入账号" :disabled="form.id !== undefined" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入姓名" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="用户昵称" prop="name">
            <el-input v-model="form.name" placeholder="请输入昵称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="用户邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="手机号码" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="用户性别" prop="sex">
            <el-select v-model="form.sex" placeholder="请选择性别" style="width: 100%">
              <el-option :value="1" label="男" />
              <el-option :value="2" label="女" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="出生日期" prop="birthday">
            <el-date-picker
              v-model="form.birthday"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="所属部门" prop="deptId">
            <el-tree-select
              v-model="form.deptId"
              :data="options.depts"
              node-key="id"
              :props="{ label: 'label', children: 'children' }"
              placeholder="请选择部门"
              check-strictly
              style="width: 100%"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="所属岗位" prop="postId">
            <el-select v-model="form.postId" placeholder="请选择岗位" style="width: 100%" clearable>
              <el-option
                v-for="item in options.posts"
                :key="item.id"
                :label="item.postName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="分配角色" prop="roleId">
            <el-tree-select
              v-model="form.roleId"
              :data="options.roles"
              node-key="id"
              :props="{ label: 'label', children: 'children' }"
              placeholder="请选择角色"
              check-strictly
              style="width: 100%"
              clearable
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSaveSubmit">确定</el-button>
    </template>
  </el-dialog>

  <!-- 分配角色单独对话框 -->
  <el-dialog
    v-model="grantVisible"
    title="分配角色"
    width="500px"
    destroy-on-close
  >
    <div style="padding: 10px 20px" v-loading="grantLoading">
      <div style="margin-bottom: 15px">
        <span>当前用户：</span>
        <strong>{{ activeUser?.realName || activeUser?.account }}</strong>
      </div>
      <el-form label-width="80px">
        <el-form-item label="选择角色">
          <el-tree-select
            v-model="grantForm.roleIds"
            :data="options.roles"
            node-key="id"
            :props="{ label: 'label', children: 'children' }"
            placeholder="请选择角色"
            check-strictly
            style="width: 100%"
            multiple
            show-checkbox
          />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="grantVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleGrantSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, ArrowDown } from '@element-plus/icons-vue'
import SystemPageShell from './components/SystemPageShell.vue'
import ActionButtonGroup from '@/components/ActionButtonGroup.vue'
import { 
  getUserList, 
  getUserDetail,
  submitUser, 
  updateUser,
  removeUsers, 
  grantUserRoles, 
  resetUserPassword, 
  unlockUsers 
} from '@/api/system/user'
import { getRoleTree } from '@/api/system/role'
import { getDeptTree } from '@/api/system/dept'
import { getPostSelect } from '@/api/system/post'
import { getPayload, getRecords, getTotal, normalizeTree, joinIds, isSuccess } from './utils/response'

const SINGLE_TENANT_ID = '000000'

// 搜索过滤与加载状态
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const selectedRows = ref([])
const tableData = ref([])

// 分页数据
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 过滤参数
const queryParams = reactive({
  account: '',
  realName: ''
})

// 表单对象与表单校验规则
const formRef = ref()
const form = ref({
  account: '',
  name: '',
  realName: '',
  email: '',
  phone: '',
  sex: null,
  birthday: '',
  tenantId: SINGLE_TENANT_ID,
  deptId: '',
  postId: '',
  roleId: ''
})

const rules = {
  account: [{ required: true, message: '请输入用户账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }]
}

// 选项数据
const options = reactive({
  roles: [],
  depts: [],
  posts: []
})

// 独立分配角色弹窗数据
const grantVisible = ref(false)
const grantLoading = ref(false)
const activeUser = ref(null)
const grantForm = reactive({
  roleIds: []
})

/**
 * 异步加载下拉/树形选择器选项，包括角色树、部门树、岗位列表
 */
async function loadOptions() {
  try {
    const [roleRes, deptRes, postRes] = await Promise.all([
      getRoleTree(),
      getDeptTree(),
      getPostSelect()
    ])
    options.roles = normalizeTree(getPayload(roleRes) || [])
    options.depts = normalizeTree(getPayload(deptRes) || [])
    options.posts = getPayload(postRes) || []
  } catch (error) {
    console.error('加载系统选项参数失败:', error)
  }
}

/**
 * 异步查询分页用户列表，提取记录并绑定到表格数据
 */
async function loadData() {
  loading.value = true
  try {
    const res = await getUserList({
      current: pagination.current,
      size: pagination.size,
      account: queryParams.account,
      realName: queryParams.realName
    })
    tableData.value = getRecords(res)
    pagination.total = getTotal(res)
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户数据失败')
  } finally {
    loading.value = false
  }
}

/**
 * 处理搜索查询，将当前页重置为 1 并加载数据
 */
function handleSearch() {
  pagination.current = 1
  loadData()
}

/**
 * 重置搜索过滤项，将当前页重置为 1 并重新查询列表
 */
function handleReset() {
  queryParams.account = ''
  queryParams.realName = ''
  pagination.current = 1
  loadData()
}

/**
 * 分页大小改变时重新加载数据
 * @param {Number} size 分页大小
 */
function handleSizeChange(size) {
  pagination.size = size
  pagination.current = 1
  loadData()
}

/**
 * 当前页改变时重新加载数据
 * @param {Number} current 当前页码
 */
function handleCurrentChange(current) {
  pagination.current = current
  loadData()
}

/**
 * 处理表格复选框选中状态变化
 * @param {Array} rows 选中的行数据
 */
function handleSelectionChange(rows) {
  selectedRows.value = rows
}

/**
 * 打开新增用户弹窗
 */
function handleCreate() {
  dialogTitle.value = '新增用户'
  form.value = {
    account: '',
    name: '',
    realName: '',
    email: '',
    phone: '',
    sex: null,
    birthday: '',
    tenantId: SINGLE_TENANT_ID,
    deptId: '',
    postId: '',
    roleId: ''
  }
  dialogVisible.value = true
}

/**
 * 打开编辑用户弹窗，先从接口拉取该用户详细信息进行精准回显
 * @param {Object} row 用户行数据
 */
async function handleEdit(row) {
  if (!row?.id) return
  try {
    const res = await getUserDetail({ id: row.id })
    const payload = getPayload(res)
    if (payload) {
      form.value = { ...payload }
    } else {
      // 降级使用行数据
      form.value = { ...row }
    }
    dialogTitle.value = '编辑用户'
    dialogVisible.value = true
  } catch (error) {
    console.warn('获取用户详情失败，已降级回显行数据', error)
    form.value = { ...row }
    dialogTitle.value = '编辑用户'
    dialogVisible.value = true
  }
}

/**
 * 确定并提交新增或编辑用户的表单，校验通过后调用后台 submit 接口
 */
async function handleSaveSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        let res
        if (form.value.id) {
          res = await updateUser(form.value)
        } else {
          res = await submitUser(form.value)
        }
        if (isSuccess(res)) {
          ElMessage.success('保存成功')
          dialogVisible.value = false
          loadData()
        } else {
          ElMessage.error(res?.msg || '保存失败')
        }
      } catch (error) {
        console.error('提交用户信息失败:', error)
        ElMessage.error('网络或服务器异常，提交失败')
      } finally {
        saving.value = false
      }
    }
  })
}

/**
 * 执行通用删除操作，带二次确认提示
 * @param {String} ids 逗号分隔的用户ID列表
 * @param {String} msg 确认消息提示文案
 */
function executeRemove(ids, msg) {
  ElMessageBox.confirm(msg, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await removeUsers(ids)
      if (isSuccess(res)) {
        ElMessage.success('删除成功')
        loadData()
      } else {
        ElMessage.error(res?.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除用户失败:', error)
      ElMessage.error('删除用户操作失败')
    }
  }).catch(() => {})
}

/**
 * 行内单项删除操作
 * @param {Object} row 待删除的用户行
 */
function handleSingleRemove(row) {
  executeRemove(String(row.id), `确定删除用户 [${row.realName || row.account}] 吗？`)
}

/**
 * 批量删除已勾选的所有用户行
 */
function handleBatchRemove() {
  if (selectedRows.value.length === 0) return
  const ids = joinIds(selectedRows.value)
  executeRemove(ids, `确定删除选中的 ${selectedRows.value.length} 个用户吗？`)
}

/**
 * 打开角色分配弹窗，回显当前已持有的角色ID
 * @param {Object} row 目标用户数据
 */
function handleOpenGrant(row) {
  activeUser.value = row
  grantForm.roleIds = row.roleId ? row.roleId.split(',') : []
  grantVisible.value = true
}

/**
 * 提交角色分配修改，调用后端 /system/user/authRole 接口
 */
async function handleGrantSubmit() {
  if (!activeUser.value?.id) return
  saving.value = true
  try {
    const userIds = String(activeUser.value.id)
    const roleIds = grantForm.roleIds.join(',')
    const res = await grantUserRoles(userIds, roleIds)
    if (isSuccess(res)) {
      ElMessage.success('分配角色成功')
      grantVisible.value = false
      loadData()
    } else {
      ElMessage.error(res?.msg || '分配角色失败')
    }
  } catch (error) {
    console.error('分配角色失败:', error)
    ElMessage.error('分配角色失败')
  } finally {
    saving.value = false
  }
}

/**
 * 提示确认并重置指定用户的密码
 * @param {Object} row 目标用户数据
 */
function handleResetPassword(row) {
  ElMessageBox.confirm(`确定重置用户 [${row.realName || row.account}] 的密码为默认密码吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await resetUserPassword(String(row.id))
      if (isSuccess(res)) {
        ElMessage.success('重置密码成功')
      } else {
        ElMessage.error(res?.msg || '重置密码失败')
      }
    } catch (error) {
      console.error('重置密码操作异常:', error)
      ElMessage.error('重置密码操作异常')
    }
  }).catch(() => {})
}

/**
 * 提示确认并解锁指定的被锁账号
 * @param {Object} row 目标用户数据
 */
function handleUnlock(row) {
  ElMessageBox.confirm(`确定解锁用户 [${row.realName || row.account}] 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await unlockUsers(String(row.id))
      if (isSuccess(res)) {
        ElMessage.success('解锁成功')
      } else {
        ElMessage.error(res?.msg || '解锁失败')
      }
    } catch (error) {
      console.error('解锁账号操作异常:', error)
      ElMessage.error('解锁账号操作异常')
    }
  }).catch(() => {})
}

// 挂载时加载字典/配置项及用户列表数据
onMounted(() => {
  loadOptions()
  loadData()
})
</script>

<style scoped>
.operation-buttons {
  display: flex;
  justify-content: flex-end;
  align-items: center;
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
  color: #606266 !important;
  border: 1px solid #d9d9d9 !important;
}

.view-btn:hover {
  border-color: #1A53FF !important;
  color: #1A53FF !important;
}

.toolbar-left {
  display: flex;
  align-items: center;
  flex: 1 1 520px;
  min-width: 360px;
}

.demo-form-inline .el-form-item {
  margin-bottom: 0;
  margin-right: 16px;
}

.demo-form-inline {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  row-gap: 8px;
}

.demo-form-inline :deep(.el-input) {
  width: 180px;
}

.toolbar-right {
  flex: 0 0 auto;
}
</style>
