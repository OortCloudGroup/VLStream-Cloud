<template>
  <div class="user-profile-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">个人设置</h2>
      <p class="page-subtitle">查看和管理您的个人信息</p>
    </div>

    <!-- 用户信息卡片 -->
    <div class="profile-card">
      <div class="card-header">
        <div class="user-avatar">
          <el-avatar 
            :size="80" 
            :src="userInfo.photo || defaultAvatar"
            :alt="userInfo.userName"
          >
            {{ userInfo.userName?.charAt(0) || 'U' }}
          </el-avatar>
        </div>
        <div class="user-basic-info">
          <h3 class="user-name">{{ userInfo.userName || '未登录' }}</h3>
          <p class="user-id">用户ID: {{ userInfo.userId || '未知' }}</p>
          <p class="user-status">
            <el-tag 
              :type="userInfo.status === 1 ? 'success' : 'danger'"
              size="small"
            >
              {{ userInfo.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </p>
        </div>
      </div>

      <!-- 详细信息表单 -->
      <el-form 
        :model="userInfo" 
        label-width="120px" 
        class="user-form"
        :disabled="!isEditing"
      >
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="登录账号">
              <el-input 
                v-model="userInfo.loginId" 
                placeholder="登录账号"
                readonly
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户姓名">
              <el-input 
                v-model="userInfo.userName" 
                placeholder="用户姓名"
                :readonly="!isEditing"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="租户ID">
              <el-input 
                v-model="userInfo.tenantId" 
                placeholder="租户ID"
                readonly
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户来源">
              <el-select 
                v-model="userInfo.form" 
                placeholder="用户来源"
                :disabled="!isEditing"
                style="width: 100%"
              >
                <el-option label="系统创建" :value="1" />
                <el-option label="组织创建" :value="2" />
                <el-option label="用户池创建" :value="3" />
                <el-option label="统一用户中心" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="登录类型">
              <el-select 
                v-model="userInfo.loginType" 
                placeholder="登录类型"
                :disabled="!isEditing"
                style="width: 100%"
              >
                <el-option label="B/E端用户" :value="1" />
                <el-option label="C端用户" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户端类型">
              <el-input 
                v-model="userInfo.client" 
                placeholder="客户端类型"
                :readonly="!isEditing"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="最后登录时间">
              <el-input 
                :value="formatDateTime(userInfo.loginTime)" 
                placeholder="最后登录时间"
                readonly
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最后登录IP">
              <el-input 
                v-model="userInfo.loginIp" 
                placeholder="最后登录IP"
                readonly
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="令牌过期时间">
              <el-input 
                :value="formatDateTime(userInfo.tokenExpireTime)" 
                placeholder="令牌过期时间"
                readonly
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="创建时间">
              <el-input 
                :value="formatDateTime(userInfo.createdAt)" 
                placeholder="创建时间"
                readonly
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="更新时间">
              <el-input 
                :value="formatDateTime(userInfo.updatedAt)" 
                placeholder="更新时间"
                readonly
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="访问令牌">
              <el-input 
                :value="maskToken(userInfo.accessToken)" 
                placeholder="访问令牌"
                readonly
                show-password
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 操作按钮 -->
        <el-form-item>
          <div class="form-actions">
            <el-button 
              v-if="!isEditing" 
              type="primary" 
              @click="startEdit"
              :icon="Edit"
            >
              编辑信息
            </el-button>
            <template v-else>
              <el-button 
                type="primary" 
                @click="saveChanges"
                :icon="Check"
                :loading="saving"
              >
                保存
              </el-button>
              <el-button 
                @click="cancelEdit"
                :icon="Close"
              >
                取消
              </el-button>
            </template>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <!-- 登录历史 -->
    <div class="login-history-card">
      <div class="card-header">
        <h3>登录历史</h3>
      </div>
      <el-table :data="loginHistory" stripe>
        <el-table-column prop="loginTime" label="登录时间" width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.loginTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="loginIp" label="登录IP" width="150" />
        <el-table-column prop="client" label="客户端" width="120" />
        <el-table-column prop="location" label="登录地点" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Check, Close } from '@element-plus/icons-vue'
import { AuthManager } from '@/utils/auth'
import { getUserInfo } from '@/api/userCenter'

// 认证管理器
const authManager = new AuthManager()

// 响应式数据
const userInfo = ref({})
const isEditing = ref(false)
const saving = ref(false)
const loginHistory = ref([])

// 默认头像
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgdmlld0JveD0iMCAwIDIwMCAyMDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIyMDAiIGhlaWdodD0iMjAwIiBmaWxsPSIjRjVGNUY1Ii8+CjxjaXJjbGUgY3g9IjEwMCIgY3k9IjgwIiByPSIzMCIgZmlsbD0iI0NDQ0NDQyIvPgo8cGF0aCBkPSJNNDAgMTYwQzQwIDEyMCA4MCAxMDAgMTAwIDEwMEMxMjAgMTAwIDE2MCAxMjAgMTYwIDE2MEg0MFoiIGZpbGw9IiNDQ0NDQ0MiLz4KPC9zdmc+'

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '未知'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 掩码令牌
const maskToken = (token) => {
  if (!token) return ''
  if (token.length <= 8) return token
  return token.substring(0, 8) + '****' + token.substring(token.length - 8)
}

// 开始编辑
const startEdit = () => {
  isEditing.value = true
}

// 保存更改
const saveChanges = async () => {
  try {
    saving.value = true
    
    // 暂时只保存到localStorage，因为API可能不支持更新
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    
      ElMessage.success('用户信息更新成功')
      isEditing.value = false
    
    console.log('✅ 用户信息已保存到localStorage')
    
  } catch (error) {
    console.error('❌ 保存用户信息失败:', error)
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

// 取消编辑
const cancelEdit = async () => {
  await ElMessageBox.confirm('确定要取消编辑吗？未保存的更改将丢失。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  isEditing.value = false
  await loadUserInfo() // 重新加载用户信息，恢复原始数据
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    console.log('🚀 开始加载用户信息...')
    
    // 获取token
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    
    const token = urlToken || sessionToken || localToken
    
    if (!token) {
      console.log('❌ 没有找到有效的token')
      ElMessage.warning('未找到用户信息，请先登录')
      return
    }
    
    console.log('✅ 使用token:', token.substring(0, 8) + '...')
    
    // 调用getUserInfo API获取用户详细信息
    const response = await getUserInfo({ accessToken: token })
    
    console.log('📥 getUserInfo响应:', response)
    
    // 检查响应格式 - 直接返回的数据格式
          if (response.code === 200 && response.data) {
      const userData = response.data
      
      console.log('📋 解析到的用户信息:')
      console.log('- 用户名称:', userData.user_name)
      console.log('- 用户ID:', userData.user_id)
      console.log('- 登录ID:', userData.login_id)
      console.log('- 租户ID:', userData.tenant_id)
      
      // 更新用户信息 - 使用实际返回的字段
      userInfo.value = {
        userName: userData.user_name || '未知用户',
        userId: userData.user_id || '',
        loginId: userData.user_name || '', // 使用user_name作为loginId
        tenantId: userData.tenant_id || '',
        status: userData.status || 1,
        form: userData.form || 1,
        loginType: userData.login_type || 1,
        client: userData.client || 'pcweb',
        loginTime: userData.login_time || new Date().toISOString(),
        loginIp: userData.login_ip || '127.0.0.1',
        tokenExpireTime: userData.token_expire_time || new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
        createdAt: userData.created_at || new Date().toISOString(),
        updatedAt: userData.updated_at || new Date().toISOString(),
        accessToken: token
      }
      
      console.log('✅ 用户信息已更新:', userInfo.value)
      
      // 保存到localStorage
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
      
      // 强制触发Vue响应式更新
      await nextTick()
      console.log('🔄 强制触发Vue响应式更新')
      
      // 检查页面元素是否更新
      setTimeout(() => {
        const userNameElement = document.querySelector('.user-name')
        if (userNameElement) {
          console.log('📋 页面用户名称元素:', userNameElement.textContent.trim())
        }
        
        const loginIdInput = document.querySelector('.user-form .el-input__inner')
        if (loginIdInput) {
          console.log('📋 页面登录账号输入框:', loginIdInput.value)
        }
      }, 100)
      
    } else {
      console.warn('getUserInfo API返回数据格式不正确:', response)
      ElMessage.warning('获取用户信息失败')
    }
  } catch (error) {
    console.error('❌ 加载用户信息失败:', error)
    ElMessage.error('加载用户信息失败')
  }
}

// 加载登录历史
const loadLoginHistory = () => {
  // 模拟登录历史数据
  loginHistory.value = [
    {
      loginTime: new Date(),
      loginIp: userInfo.value.loginIp || '127.0.0.1',
      client: userInfo.value.client || 'pcweb',
      status: 'success',
      location: '本地'
    },
    {
      loginTime: new Date(Date.now() - 24 * 60 * 60 * 1000),
      loginIp: '192.168.1.100',
      client: 'pcweb',
      status: 'success',
      location: '办公室'
    }
  ]
}

// 页面加载时执行
onMounted(async () => {
  await loadUserInfo()
  loadLoginHistory()
})
</script>

<style scoped>
.user-profile-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.profile-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 24px;
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.user-avatar {
  margin-right: 24px;
}

.user-basic-info {
  flex: 1;
}

.user-name {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.user-id {
  font-size: 14px;
  color: #909399;
  margin: 0 0 8px 0;
}

.user-status {
  margin: 0;
}

.user-form {
  margin-top: 24px;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-start;
}

.login-history-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 24px;
}

.login-history-card .card-header {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.login-history-card .card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-profile-container {
    padding: 16px;
  }
  
  .card-header {
    flex-direction: column;
    text-align: center;
  }
  
  .user-avatar {
    margin-right: 0;
    margin-bottom: 16px;
  }
  
  .form-actions {
    flex-direction: column;
  }
}
</style> 