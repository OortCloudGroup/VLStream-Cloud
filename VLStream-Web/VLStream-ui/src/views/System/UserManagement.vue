<template>
  <div class="user-management tenant_Page draHeaPB">
    <div class="tenant_content">
      <div class="tableTenBox flexRowAC">
        <div class="tableTenItU">
          <div class="depNameBox_out flexRowAC">
            <div class="depNameBox flexRowAC">
              <div class="exportBtnBox flexRowAC">
                <button type="button" class="exportBtn newBtn flexRowAC" @click="handleCreate">
                  <el-icon class="BtnImg">
                    <Plus />
                  </el-icon>
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
            <el-table-column prop="account" label="账号" show-overflow-tooltip />
            <el-table-column prop="name" label="昵称" show-overflow-tooltip />
            <el-table-column prop="realName" label="姓名" show-overflow-tooltip />
            <el-table-column prop="email" label="邮箱" show-overflow-tooltip />
            <el-table-column prop="phone" label="手机" show-overflow-tooltip />
            <el-table-column prop="sexName" label="性别" :width="clacPXToVW(80)" align="center">
              <template #default="scope">
                <span>{{ scope.row.sex === 1 ? '男' : scope.row.sex === 2 ? '女' : '未知' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="roleName" label="角色" show-overflow-tooltip />
            <el-table-column prop="deptName" label="部门" show-overflow-tooltip />
            <el-table-column prop="postName" label="岗位" show-overflow-tooltip />
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
                  <el-dropdown trigger="click">
                    <div class="new_table_svg_group">
                      <oort-svg-icon width="20" height="20" name="more" class="new_table_svg_group_svg" />
                      <span>更多</span>
                    </div>
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

  <!-- 新增/编辑用户对话框 -->
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="40%"
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
      <el-button @click="dialogVisible = false" class="common_btn">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSaveSubmit" class="common_btn">确定</el-button>
    </template>
  </el-dialog>

  <!-- 分配角色单独对话框 -->
  <el-dialog
    v-model="grantVisible"
    title="分配角色"
    width="30%"
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
      <el-button @click="grantVisible = false" class="common_btn">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleGrantSubmit" class="common_btn">确定</el-button>
    </template>
  </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { clacPXToVW } from '@/utils/index'
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
const exportItem = ref({ isDisabledExcel: false })
const searchData = ref([
  { label: '账号', value: 'account', type: 'text', default: '' },
  { label: '姓名', value: 'realName', type: 'text', default: '' }
])

const toolbarButtonList = computed(() => [
  { name: '编辑', svg: 'table_edit', clickFn: handleToolbarEdit },
  { name: '删除', svg: 'table_del', clickFn: handleBatchRemove },
  { name: '分配角色', svg: 'more', clickFn: handleToolbarGrant }
])

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

const searchResetFn = (val, reset) => {
  if (reset && !(val && (val.keyword || val.account || val.realName))) {
    handleReset()
    return
  }
  queryParams.account = val?.account || val?.keyword || ''
  queryParams.realName = val?.realName || ''
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
    ElMessage.warning('请选择一条记录分配角色')
    return
  }
  handleOpenGrant(selectedRows.value[0])
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
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的记录')
    return
  }
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

.user-management {
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
