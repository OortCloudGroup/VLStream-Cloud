<template>
  <div class="login-container">
    <div class="login-card">
      <!-- 登录头部 -->
      <div class="login-header">
        <div class="logo-section">
          <img :src="loginLogo" alt="VLStream" class="login-logo">
        </div>
      </div>

      <!-- 登录表单 -->
      <div v-if="tenantMode === 'single'" class="login-form">
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          @submit.prevent="handleLogin"
        >
          <!-- 用户名 -->
          <el-form-item prop="loginId">
            <el-input
              v-model="loginForm.loginId"
              placeholder="请输入用户名"
              prefix-icon="el-icon-user"
              size="large"
            />
          </el-form-item>

          <!-- 密码 -->
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="el-icon-lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <!-- 登录按钮 -->
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loginLoading"
              class="login-button"
              @click="handleLogin"
            >
              {{ loginLoading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      <div v-else-if="tenantMode === 'multi'" class="platform-login-tip">
        <h3>统一平台登录</h3>
        <p>{{ platformRedirecting ? '正在前往统一平台登录...' : '当前服务已启用多租户模式。' }}</p>
        <p v-if="platformLoginError" class="platform-login-error">{{ platformLoginError }}</p>
        <el-button type="primary" :loading="platformRedirecting" class="login-button" @click="redirectToPlatformLogin">
          {{ platformRedirecting ? '正在跳转...' : '前往统一平台登录' }}
        </el-button>
      </div>
      <div v-else class="platform-login-tip">正在读取登录模式...</div>

    </div>

    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import smCrypto from 'sm-crypto'
import { AuthManager } from '@/utils/auth'
import { loginUser } from '@/api/auth'
import loginLogo from '@/assets/img/img.png'

const router = useRouter()
const route = useRoute()
const authManager = new AuthManager()
const { sm2 } = smCrypto
const BLADE_AUTH_PUBLIC_KEY = import.meta.env.VITE_BLADE_AUTH_PUBLIC_KEY || '049787e408dea94acb3655acc5a7c7c7010bb9f140c84926c667ea616366082a118141c8dcb3e78a9d85d64fb765a250ff73448b18938f2219b94f782e28e1df64'
const SINGLE_TENANT_ID = '000000'
const PLATFORM_LOGIN_URL = import.meta.env.VITE_PLATFORM_LOGIN_URL || 'https://workup-dev.myoumuamua.com:6433/bus/apaas-web/loginPage/index.html'
const PLATFORM_APP_NAME = import.meta.env.VITE_PLATFORM_APP_NAME || 'VLStream'
const PLATFORM_REDIRECT_KEY = 'platformLoginRedirect'

// 表单引用
const loginFormRef = ref()

// 登录表单数据
const loginForm = reactive({
  loginId: '',
  password: '',
  tenantId: SINGLE_TENANT_ID
})

// 登录状态
const loginLoading = ref(false)
const tenantMode = ref('loading')
const platformLoginError = ref('')
const platformRedirecting = ref(false)

// 表单验证规则
const loginRules = {
  loginId: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

// 使用后端配置的 SM2 公钥加密登录密码。
const encryptPassword = (password) => {
  return sm2.doEncrypt(password, BLADE_AUTH_PUBLIC_KEY, 0)
}

const platformCallbackUrl = () => {
  return new URL(`${import.meta.env.BASE_URL}login`, window.location.origin).toString()
}

const redirectToPlatformLogin = () => {
  platformRedirecting.value = true
  platformLoginError.value = ''
  sessionStorage.setItem(PLATFORM_REDIRECT_KEY, String(route.query.redirect || '/'))
  const loginUrl = new URL(PLATFORM_LOGIN_URL)
  loginUrl.searchParams.set('appname', PLATFORM_APP_NAME)
  loginUrl.searchParams.set('redirect_uri', platformCallbackUrl())
  if (!loginUrl.hash) loginUrl.hash = '/'
  window.location.replace(loginUrl.toString())
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    const valid = await loginFormRef.value.validate()
    if (!valid) return

    loginLoading.value = true

    // 构建 SpringBlade 登录参数
    const loginData = {
      grantType: 'password',
      tenantId: SINGLE_TENANT_ID,
      account: loginForm.loginId,
      password: encryptPassword(loginForm.password)
    }

    console.log('登录数据:', loginData)

    // 调用登录接口
    const response = await loginUser(loginData)

    if (response.code === 200) {
      const authInfo = response.data
      const userData = {
        ...authInfo,
        loginId: authInfo.account,
        userName: authInfo.userName || authInfo.account
      }

      // 保存用户信息和token
      localStorage.setItem('userInfo', JSON.stringify(userData))
      localStorage.setItem('accessToken', userData.accessToken)
      sessionStorage.setItem('userInfo', JSON.stringify(userData))
      sessionStorage.setItem('accessToken', userData.accessToken)

      // 同步用户信息到本地数据库
      await authManager.saveUserToLocal(userData)

      ElMessage.success('登录成功')

      // 登录成功后回到原目标页，没有 redirect 时进入首页。
      await router.replace(route.query.redirect || '/')

    } else {
      ElMessage.error(response.msg || '登录失败')
    }

  } catch (error) {
    console.error('登录失败:', error)
    // 认证拦截器会把业务失败响应封装在 error.data 中；优先读取该消息，避免将账号密码错误误显示为网络错误。
    const errorData = error.response?.data || error.data
    const errorMsg = errorData?.msg || errorData?.message || (typeof errorData === 'string' && errorData) || error.message || '网络错误'
    ElMessage.error('登录失败: ' + errorMsg)
  } finally {
    loginLoading.value = false
  }
}

// 页面加载时检查是否已登录
onMounted(async () => {
  tenantMode.value = await authManager.getTenantMode()
  // 检查URL中的token（外部系统跳转）
  const urlParams = new URLSearchParams(window.location.search)
  const token = urlParams.get('accessToken') || urlParams.get('access_token') || urlParams.get('token')

  if (token) {
    try {
      const userInfo = await authManager.checkUrlToken()
      if (userInfo) {
        ElMessage.success('自动登录成功')
        const redirect = sessionStorage.getItem(PLATFORM_REDIRECT_KEY) || route.query.redirect || '/'
        sessionStorage.removeItem(PLATFORM_REDIRECT_KEY)
        await router.replace(redirect)
        return
      }
      if (tenantMode.value === 'multi') {
        platformLoginError.value = '平台登录凭证校验失败，请返回统一平台重新进入。'
      }
    } catch (error) {
      console.error('URL token验证失败:', error)
      platformLoginError.value = error?.response?.data?.msg || error?.message || '平台登录失败'
    }
  }

  if (tenantMode.value === 'multi') {
    if (!token) redirectToPlatformLogin()
    return
  }

  // 检查本地token
  const localToken = localStorage.getItem('accessToken')
  if (localToken) {
    try {
      const userInfo = await authManager.checkLocalToken()
      if (userInfo) {
        ElMessage.success('自动登录成功')
        router.push('/')
        return
      }
    } catch (error) {
      console.error('本地token验证失败:', error)
    }
  }
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.login-card {
  width: 400px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 10;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo-section {
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-logo {
  width: 282px;
  max-width: 100%;
  height: auto;
}

.login-form {
  margin-bottom: 30px;
}

.platform-login-tip {
  margin-bottom: 30px;
  text-align: center;
  color: #606266;
  line-height: 1.8;
}

.platform-login-error {
  color: #f56c6c;
}

.login-form .el-form-item {
  margin-bottom: 20px;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.login-button:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
}

.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 6s ease-in-out infinite;
}

.circle-1 {
  width: 120px;
  height: 120px;
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.circle-2 {
  width: 80px;
  height: 80px;
  top: 70%;
  right: 10%;
  animation-delay: 2s;
}

.circle-3 {
  width: 60px;
  height: 60px;
  top: 40%;
  left: 80%;
  animation-delay: 4s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-20px);
  }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-card {
    width: 90%;
    padding: 30px 20px;
  }

}
</style>
