<template>
  <div class="user-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <p>系统用户账户管理与权限配置</p>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名或邮箱"
          style="width: 300px"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="statusFilter" placeholder="用户状态" style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="正常" value="active" />
          <el-option label="禁用" value="disabled" />
          <el-option label="锁定" value="locked" />
        </el-select>
        <el-select v-model="roleFilter" placeholder="用户角色" style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="管理员" value="admin" />
          <el-option label="操作员" value="operator" />
          <el-option label="观察员" value="viewer" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" :icon="Plus" @click="showAddDialog = true">
          添加用户
        </el-button>
        <el-button :icon="Refresh" @click="refreshUsers">
          刷新
        </el-button>
      </div>
    </div>

    <!-- 用户列表 -->
    <el-table :data="filteredUsers" style="width: 100%" stripe>
      <el-table-column prop="avatar" label="头像" width="80">
        <template #default="{ row }">
          <el-avatar :src="row.avatar" :size="40">
            <el-icon><User /></el-icon>
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="realName" label="真实姓名" width="120" />
      <el-table-column prop="email" label="邮箱" width="200" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="getRoleType(row.role)">
            {{ getRoleText(row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLogin" label="最后登录" width="180">
        <template #default="{ row }">
          {{ formatDate(row.lastLogin) }}
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            size="small"
            @click="editUser(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="row.status === 'active'"
            type="warning"
            size="small"
            @click="disableUser(row)"
          >
            禁用
          </el-button>
          <el-button
            v-if="row.status === 'disabled'"
            type="success"
            size="small"
            @click="enableUser(row)"
          >
            启用
          </el-button>
          <el-button
            type="danger"
            size="small"
            @click="deleteUser(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="totalUsers"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog
      v-model="showAddDialog"
      :title="isEdit ? '编辑用户' : '添加用户'"
      width="600px"
    >
      <el-form
        :model="userForm"
        :rules="userRules"
        ref="userFormRef"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="userForm.username"
            placeholder="请输入用户名"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入电话号码" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="操作员" value="operator" />
            <el-option label="观察员" value="viewer" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input
            v-model="userForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="确认密码" prop="confirmPassword">
          <el-input
            v-model="userForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio label="active">正常</el-radio>
            <el-radio label="disabled">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="userForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置对话框 -->
    <el-dialog v-model="showPermissionDialog" title="权限配置" width="800px">
      <div v-if="selectedUser" class="permission-content">
        <h3>{{ selectedUser.realName }} ({{ selectedUser.username }}) 的权限配置</h3>
        <el-divider />
        
        <div class="permission-section">
          <h4>功能模块权限</h4>
          <el-checkbox-group v-model="permissions.modules">
            <div class="permission-grid">
              <el-checkbox label="video_square">视频广场</el-checkbox>
              <el-checkbox label="device_management">设备管理</el-checkbox>
              <el-checkbox label="event_management">事件管理</el-checkbox>
              <el-checkbox label="algorithm_management">算法管理</el-checkbox>
              <el-checkbox label="user_management">用户管理</el-checkbox>
              <el-checkbox label="system_settings">系统设置</el-checkbox>
            </div>
          </el-checkbox-group>
        </div>
        
        <el-divider />
        
        <div class="permission-section">
          <h4>操作权限</h4>
          <el-checkbox-group v-model="permissions.operations">
            <div class="permission-grid">
              <el-checkbox label="view">查看</el-checkbox>
              <el-checkbox label="add">添加</el-checkbox>
              <el-checkbox label="edit">编辑</el-checkbox>
              <el-checkbox label="delete">删除</el-checkbox>
              <el-checkbox label="export">导出</el-checkbox>
              <el-checkbox label="import">导入</el-checkbox>
            </div>
          </el-checkbox-group>
        </div>
        
        <el-divider />
        
        <div class="permission-section">
          <h4>设备权限</h4>
          <el-checkbox-group v-model="permissions.devices">
            <div class="permission-grid">
              <el-checkbox label="camera_control">摄像头控制</el-checkbox>
              <el-checkbox label="recording">录像功能</el-checkbox>
              <el-checkbox label="snapshot">抓图功能</el-checkbox>
              <el-checkbox label="playback">回放功能</el-checkbox>
            </div>
          </el-checkbox-group>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPermissionDialog = false">取消</el-button>
        <el-button type="primary" @click="savePermissions">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  Search,
  Refresh,
  Plus,
  User
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 响应式数据
const searchKeyword = ref('')
const statusFilter = ref('')
const roleFilter = ref('')
const showAddDialog = ref(false)
const showPermissionDialog = ref(false)
const isEdit = ref(false)
const selectedUser = ref(null)
const users = ref([])
const userFormRef = ref()
const currentPage = ref(1)
const pageSize = ref(20)
const totalUsers = ref(0)

// 表单数据
const userForm = ref({
  username: '',
  realName: '',
  email: '',
  phone: '',
  role: '',
  password: '',
  confirmPassword: '',
  status: 'active',
  remark: ''
})

// 权限数据
const permissions = ref({
  modules: [],
  operations: [],
  devices: []
})

// 表单验证规则
const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入电话号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== userForm.value.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 计算属性
const filteredUsers = computed(() => {
  return users.value.filter(user => {
    const matchesKeyword = !searchKeyword.value || 
      user.username.includes(searchKeyword.value) || 
      user.email.includes(searchKeyword.value) ||
      user.realName.includes(searchKeyword.value)
    const matchesStatus = !statusFilter.value || user.status === statusFilter.value
    const matchesRole = !roleFilter.value || user.role === roleFilter.value
    return matchesKeyword && matchesStatus && matchesRole
  })
})

// 方法
const refreshUsers = () => {
  // 模拟获取用户数据
  users.value = [
    {
      id: 1,
      username: 'admin',
      realName: '系统管理员',
      email: 'admin@vlstream.com',
      phone: '13800138000',
      role: 'admin',
      status: 'active',
      avatar: '',
      lastLogin: '2024-01-15T10:30:00Z',
      createTime: '2024-01-01T00:00:00Z',
      remark: '系统默认管理员账户'
    },
    {
      id: 2,
      username: 'operator1',
      realName: '张三',
      email: 'zhangsan@vlstream.com',
      phone: '13800138001',
      role: 'operator',
      status: 'active',
      avatar: '',
      lastLogin: '2024-01-15T09:15:00Z',
      createTime: '2024-01-02T00:00:00Z',
      remark: '监控操作员'
    },
    {
      id: 3,
      username: 'viewer1',
      realName: '李四',
      email: 'lisi@vlstream.com',
      phone: '13800138002',
      role: 'viewer',
      status: 'disabled',
      avatar: '',
      lastLogin: '2024-01-14T16:45:00Z',
      createTime: '2024-01-03T00:00:00Z',
      remark: '只读用户'
    }
  ]
  totalUsers.value = users.value.length
  ElMessage.success('用户列表已刷新')
}

const editUser = (user) => {
  isEdit.value = true
  userForm.value = {
    ...user,
    password: '',
    confirmPassword: ''
  }
  showAddDialog.value = true
}

const saveUser = async () => {
  if (!userFormRef.value) return
  
  try {
    await userFormRef.value.validate()
    
    if (isEdit.value) {
      // 编辑用户
      const index = users.value.findIndex(u => u.id === userForm.value.id)
      if (index !== -1) {
        users.value[index] = {
          ...users.value[index],
          ...userForm.value,
          updateTime: new Date().toISOString()
        }
        ElMessage.success('用户信息更新成功')
      }
    } else {
      // 添加用户
      const newUser = {
        id: Date.now(),
        ...userForm.value,
        avatar: '',
        lastLogin: null,
        createTime: new Date().toISOString()
      }
      users.value.push(newUser)
      totalUsers.value++
      ElMessage.success('用户添加成功')
    }
    
    showAddDialog.value = false
    resetUserForm()
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

const disableUser = (user) => {
  const index = users.value.findIndex(u => u.id === user.id)
  if (index !== -1) {
    users.value[index].status = 'disabled'
    ElMessage.success(`用户 "${user.username}" 已禁用`)
  }
}

const enableUser = (user) => {
  const index = users.value.findIndex(u => u.id === user.id)
  if (index !== -1) {
    users.value[index].status = 'active'
    ElMessage.success(`用户 "${user.username}" 已启用`)
  }
}

const deleteUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${user.username}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const index = users.value.findIndex(u => u.id === user.id)
    if (index !== -1) {
      users.value.splice(index, 1)
      totalUsers.value--
      ElMessage.success('用户删除成功')
    }
  } catch {
    ElMessage.info('已取消删除')
  }
}

const savePermissions = () => {
  ElMessage.success('权限配置保存成功')
  showPermissionDialog.value = false
}

const resetUserForm = () => {
  userForm.value = {
    username: '',
    realName: '',
    email: '',
    phone: '',
    role: '',
    password: '',
    confirmPassword: '',
    status: 'active',
    remark: ''
  }
  isEdit.value = false
}

const getRoleType = (role) => {
  const typeMap = {
    admin: 'danger',
    operator: 'warning',
    viewer: 'info'
  }
  return typeMap[role] || ''
}

const getRoleText = (role) => {
  const textMap = {
    admin: '管理员',
    operator: '操作员',
    viewer: '观察员'
  }
  return textMap[role] || role
}

const getStatusType = (status) => {
  const typeMap = {
    active: 'success',
    disabled: 'warning',
    locked: 'danger'
  }
  return typeMap[status] || ''
}

const getStatusText = (status) => {
  const textMap = {
    active: '正常',
    disabled: '禁用',
    locked: '锁定'
  }
  return textMap[status] || status
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString()
}

// 生命周期
onMounted(() => {
  refreshUsers()
})
</script>

<style scoped>
.user-management {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.toolbar-left .el-select {
  margin-left: 10px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.permission-content {
  padding: 20px 0;
}

.permission-content h3 {
  margin-bottom: 20px;
  color: #303133;
}

.permission-section {
  margin-bottom: 20px;
}

.permission-section h4 {
  margin-bottom: 15px;
  color: #606266;
  font-size: 16px;
}

.permission-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
}

.permission-grid .el-checkbox {
  margin-right: 0;
}

.el-table {
  margin-top: 20px;
}
</style>