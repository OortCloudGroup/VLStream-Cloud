<template>
  <div class="test-auth-container">
    <h2>SSO单点登录测试页面</h2>
    
    <div class="test-section">
      <h3>1. 当前Token状态</h3>
      <div class="token-info">
        <p><strong>Session Storage Token:</strong> {{ sessionToken || '无' }}</p>
        <p><strong>Local Storage Token:</strong> {{ localToken || '无' }}</p>
        <p><strong>当前使用Token:</strong> {{ currentToken || '无' }}</p>
      </div>
    </div>

    <div class="test-section">
      <h3>2. 设置新Token</h3>
      <div class="token-input">
        <el-input 
          v-model="newToken" 
          placeholder="请输入新的accessToken"
          style="width: 400px; margin-right: 10px;"
        />
        <el-button type="primary" @click="handleSetToken">设置Token</el-button>
        <el-button type="success" @click="handleAutoGetToken">自动获取外部平台Token</el-button>
        <el-button type="warning" @click="handleClearTokens">清除所有Token</el-button>
        <el-button @click="setTestToken">设置测试Token</el-button>
      </div>
      <div class="token-sync" style="margin-top: 10px;">
        <el-button type="success" @click="syncFromExternalPlatform">从外部平台同步Token</el-button>
        <el-button type="warning" @click="openExternalPlatform">打开外部平台</el-button>
        <el-button type="danger" @click="forceSetNewToken">强制设置新Token</el-button>
        <el-button type="info" @click="realTimeSync">实时同步外部平台</el-button>
        <el-button type="primary" @click="showManualGuide">手动获取Token指南</el-button>
      </div>
    </div>

    <div class="test-section">
      <h3>3. 测试Token验证</h3>
      <div class="test-buttons">
        <el-button type="success" @click="testCurrentToken">测试当前Token</el-button>
        <el-button type="warning" @click="testExternalLogin">测试外部平台登录</el-button>
        <el-button type="info" @click="testUrlToken">测试URL Token</el-button>
      </div>
    </div>

    <div class="test-section">
      <h3>4. 清除Token</h3>
      <div class="clear-buttons">
        <el-button type="danger" @click="clearAllTokens">清除所有Token</el-button>
        <el-button type="warning" @click="forceClearTestData">强制清除测试数据</el-button>
        <el-button @click="logout">退出登录</el-button>
      </div>
    </div>

    <div class="test-section">
      <h3>5. 测试结果</h3>
      <div class="test-result">
        <pre>{{ testResult }}</pre>
      </div>
    </div>

    <div class="test-section">
      <h3>6. 快速跳转测试</h3>
      <div class="jump-buttons">
        <el-button @click="jumpToWorkspace">跳转到工作页面</el-button>
        <el-button @click="jumpWithNewToken">使用新Token跳转</el-button>
      </div>
      <div class="url-test" style="margin-top: 10px;">
        <p><strong>测试URL（带Token参数）:</strong></p>
        <el-input 
          :value="testUrl" 
          readonly 
          style="width: 100%; margin-bottom: 10px;"
        />
        <el-button @click="copyTestUrl">复制测试URL</el-button>
        <el-button @click="openTestUrl">打开测试URL</el-button>
      </div>
    </div>

    <div class="test-section">
      <h3>7. 快速设置Token</h3>
      <div class="quick-setup">
        <el-button type="primary" size="large" @click="quickSetup">一键设置新Token并跳转</el-button>
        <p style="margin-top: 10px; color: #666;">点击此按钮将自动设置新Token并跳转到工作页面</p>
      </div>
    </div>

    <div class="test-section">
      <h3>8. SSO单点登录测试</h3>
      <div class="sso-test">
        <el-button type="success" size="large" @click="testSSO">测试SSO跳转</el-button>
        <el-button type="warning" size="large" @click="simulatePlatformJump">模拟平台跳转</el-button>
        <el-button type="info" size="large" @click="getNewTokenFromPlatform">获取新Token</el-button>
        <el-button type="danger" size="large" @click="debugTokenInfo">调试Token信息</el-button>
        <el-button type="primary" size="large" @click="testCurrentTokenDirectly">直接测试Token</el-button>
        <p style="margin-top: 10px; color: #666;">测试SSO单点登录功能</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authManager } from '@/utils/auth'

const router = useRouter()

const newToken = ref('')
const sessionToken = ref('')
const localToken = ref('')
const currentToken = ref('')
const testResult = ref('')
const testUrl = ref('')

onMounted(() => {
  updateTokenInfo()
  updateTestUrl()
})

const updateTokenInfo = () => {
  sessionToken.value = sessionStorage.getItem('token') || sessionStorage.getItem('accessToken') || '无'
  localToken.value = localStorage.getItem('token') || localStorage.getItem('accessToken') || '无'
  currentToken.value = authManager.getCurrentToken()
}

const setNewToken = () => {
  if (newToken.value) {
    authManager.setNewToken(newToken.value)
    updateTokenInfo()
    updateTestUrl()
    testResult.value = `✅ 新Token已设置: ${newToken.value}`
  }
}

const handleSetToken = () => {
  if (newToken.value.trim()) {
    authManager.setNewToken(newToken.value.trim())
    updateTokenInfo()
    updateTestUrl()
    testResult.value = `✅ 新token已设置到Session Storage: ${newToken.value.trim()}`
    newToken.value = ''
  } else {
    testResult.value = '❌ 请输入token'
  }
}

const handleAutoGetToken = () => {
  // 跳转到外部平台
  const currentUrl = encodeURIComponent(window.location.href)
  const platformUrl = `http://oort.oortcloudsmart.com:21410?redirect=${currentUrl}`
  
  testResult.value = '🔄 正在跳转到外部平台获取真实token...'
  
  // 打开外部平台
  window.open(platformUrl, '_blank')
  
  testResult.value = '✅ 已打开外部平台，请登录后获取token并手动设置'
}

const handleClearTokens = () => {
  // 清除所有token
  sessionStorage.removeItem('accessToken')
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('userCenterToken')
  localStorage.removeItem('accessToken')
  localStorage.removeItem('token')
  localStorage.removeItem('userCenterToken')
  localStorage.removeItem('platformAccessToken')
  localStorage.removeItem('platformToken')
  
  updateTokenInfo()
  testResult.value = '✅ 所有token已清除'
}

const setTestToken = () => {
  const testToken = 'c34f010e8fc042099e9ac812da34f080'
  authManager.setNewToken(testToken)
  // 同时保存到平台token存储
  localStorage.setItem('platformAccessToken', testToken)
  updateTokenInfo()
  testResult.value = `✅ 新Token已设置: ${testToken}`
}

const testCurrentToken = async () => {
  try {
    const token = authManager.getCurrentToken()
    testResult.value = `🔄 正在测试Token: ${token}`
    
    const userInfo = await authManager.verifyToken(token)
    if (userInfo) {
      testResult.value = `✅ Token验证成功！\n用户信息: ${JSON.stringify(userInfo, null, 2)}`
    } else {
      testResult.value = `❌ Token验证失败`
    }
  } catch (error) {
    testResult.value = `❌ 测试失败: ${error.message}`
  }
}

const testExternalLogin = async () => {
  try {
    testResult.value = '🔄 正在测试外部平台登录...'
    const userInfo = await authManager.checkExternalPlatformLogin()
    if (userInfo) {
      testResult.value = `✅ 外部平台登录成功！\n用户信息: ${JSON.stringify(userInfo, null, 2)}`
    } else {
      testResult.value = `❌ 外部平台登录失败`
    }
  } catch (error) {
    testResult.value = `❌ 测试失败: ${error.message}`
  }
}

const testUrlToken = async () => {
  try {
    testResult.value = '🔄 正在测试URL Token...'
    const userInfo = await authManager.checkUrlToken()
    if (userInfo) {
      testResult.value = `✅ URL Token验证成功！\n用户信息: ${JSON.stringify(userInfo, null, 2)}`
    } else {
      testResult.value = `❌ URL中没有有效Token`
    }
  } catch (error) {
    testResult.value = `❌ 测试失败: ${error.message}`
  }
}

const clearAllTokens = () => {
  // 清除所有可能的token相关数据
  sessionStorage.removeItem('accessToken')
  localStorage.removeItem('accessToken')
  sessionStorage.removeItem('userInfo')
  localStorage.removeItem('userInfo')
  sessionStorage.removeItem('token')
  localStorage.removeItem('token')
  sessionStorage.removeItem('userCenterToken')
  localStorage.removeItem('userCenterToken')
  sessionStorage.removeItem('platformAccessToken')
  localStorage.removeItem('platformAccessToken')
  
  // 强制清除页面缓存
  if (window.location.reload) {
    window.location.reload()
  }
  
  updateTokenInfo()
  testResult.value = '✅ 所有Token和用户信息已清除，页面已刷新'
}

const forceClearTestData = () => {
  // 强制清除所有测试数据
  const testTokens = ['c0c81bef2c934f829df667a202c99d1e', 'c34f010e8fc042099e9ac812da34f080']
  
  // 清除所有存储
  sessionStorage.clear()
  localStorage.clear()
  
  // 清除所有可能的测试token
  testTokens.forEach(token => {
    sessionStorage.removeItem('accessToken')
    localStorage.removeItem('accessToken')
    sessionStorage.removeItem('platformAccessToken')
    localStorage.removeItem('platformAccessToken')
  })
  
  updateTokenInfo()
  testResult.value = '✅ 强制清除所有测试数据完成，现在可以同步真实平台数据'
}

const logout = () => {
  authManager.logout()
}

const jumpToWorkspace = () => {
  router.push('/workspace')
}

const jumpWithNewToken = () => {
  if (newToken.value) {
    authManager.setNewToken(newToken.value)
    router.push('/workspace')
  }
}

const syncFromExternalPlatform = async () => {
  try {
    testResult.value = '🔄 正在从外部平台同步Token...'
    const token = await authManager.getTokenFromExternalPlatform()
    if (token) {
      authManager.setNewToken(token)
      updateTokenInfo()
      testResult.value = `✅ 从外部平台同步成功！Token: ${token}`
    } else {
      testResult.value = '❌ 无法从外部平台同步Token，请手动设置'
    }
  } catch (error) {
    testResult.value = `❌ 同步失败: ${error.message}`
  }
}

const openExternalPlatform = () => {
  window.open('http://oort.oortcloudsmart.com:21410', '_blank')
  testResult.value = '✅ 已打开外部平台，请复制Token后手动设置'
}

const updateTestUrl = () => {
  const baseUrl = window.location.origin + '/bus/vls-ui/workspace'
  const token = newToken.value || 'c34f010e8fc042099e9ac812da34f080'
  testUrl.value = `${baseUrl}?accessToken=${token}`
}

const copyTestUrl = () => {
  navigator.clipboard.writeText(testUrl.value).then(() => {
    testResult.value = '✅ 测试URL已复制到剪贴板'
  }).catch(() => {
    testResult.value = '❌ 复制失败，请手动复制'
  })
}

const openTestUrl = () => {
  window.open(testUrl.value, '_blank')
  testResult.value = '✅ 已打开测试URL'
}

const forceSetNewToken = () => {
  const newTokenValue = 'c34f010e8fc042099e9ac812da34f080'
  
  // 强制清除所有旧数据
  sessionStorage.clear()
  localStorage.clear()
  
  // 设置新token
  sessionStorage.setItem('accessToken', newTokenValue)
  
  // 更新显示
  updateTokenInfo()
  updateTestUrl()
  
  testResult.value = `✅ 强制设置新Token完成: ${newTokenValue}`
  
  // 3秒后跳转到工作页面
  setTimeout(() => {
    router.push('/workspace')
  }, 3000)
}

const quickSetup = () => {
  const newTokenValue = 'c34f010e8fc042099e9ac812da34f080'
  authManager.setNewToken(newTokenValue)
  updateTokenInfo()
  updateTestUrl()
  testResult.value = `✅ 一键设置新Token完成: ${newTokenValue}`
  setTimeout(() => {
    router.push('/workspace')
  }, 3000)
}

const realTimeSync = async () => {
  testResult.value = '🔄 开始实时同步外部平台Session Storage...'
  
  try {
    // 先强制清除测试数据
    const testTokens = ['c0c81bef2c934f829df667a202c99d1e', 'c34f010e8fc042099e9ac812da34f080']
    testTokens.forEach(token => {
      if (sessionStorage.getItem('accessToken') === token) {
        sessionStorage.removeItem('accessToken')
      }
      if (localStorage.getItem('accessToken') === token) {
        localStorage.removeItem('accessToken')
      }
    })
    
    // 打开外部平台
    const platformWindow = window.open('http://oort.oortcloudsmart.com:21410', '_blank')
    
    // 等待平台加载
    testResult.value = '🔄 等待外部平台加载完成...'
    await new Promise(resolve => setTimeout(resolve, 8000)) // 增加等待时间
    
    // 尝试同步真实token
    const token = await authManager.getTokenFromExternalPlatform()
    if (token && !testTokens.includes(token)) {
      authManager.setNewToken(token)
      updateTokenInfo()
      testResult.value = `✅ 实时同步成功！真实Token: ${token}`
      
      // 关闭平台窗口
      if (platformWindow) {
        platformWindow.close()
      }
      
      // 跳转到工作页面
      setTimeout(() => {
        router.push('/workspace')
      }, 2000)
    } else {
      testResult.value = '❌ 无法自动同步外部平台token，请使用手动获取指南'
      showManualGuide()
    }
  } catch (error) {
    testResult.value = `❌ 实时同步失败: ${error.message}`
  }
}

const showManualGuide = () => {
  const guide = `
=== 手动获取外部平台Token步骤 ===

1. 打开外部平台: http://oort.oortcloudsmart.com:21410
2. 登录平台
3. 按F12打开浏览器开发者工具
4. 切换到Console标签页
5. 在Console中执行以下命令：
   sessionStorage.getItem("token")
6. 复制返回的token值
7. 在测试页面的"设置新Token"输入框中粘贴
8. 点击"设置Token"按钮

注意：由于跨域限制，无法自动获取外部平台的Session Storage
`
  testResult.value = guide
  alert(guide)
}

const testSSO = () => {
  // 使用你提供的真实token
  const realToken = 'd53163ba908e4a0ebe5bc260499d7b07'
  const ssoUrl = `http://localhost:3000/bus/vls-ui/workspace?accessToken=${realToken}&fromWhere=desktopHome`
  
  testResult.value = `🔄 正在测试SSO跳转...\nURL: ${ssoUrl}`
  
  // 在新窗口打开SSO测试URL
  window.open(ssoUrl, '_blank')
}

const getNewTokenFromPlatform = () => {
  testResult.value = `🔄 正在打开外部平台获取新token...\n\n请按以下步骤操作：\n1. 登录外部平台\n2. 按F12打开开发者工具\n3. 在Console中执行以下命令：\n   sessionStorage.getItem("token")\n   sessionStorage.getItem("accessToken")\n   sessionStorage.getItem("userToken")\n   localStorage.getItem("token")\n   localStorage.getItem("accessToken")\n4. 复制返回的token值\n5. 在测试页面设置新token\n\n注意：请确保登录状态是有效的`
  
  // 打开外部平台
  window.open('http://oort.oortcloudsmart.com:21410', '_blank')
}

const debugTokenInfo = () => {
  const debugInfo = `
=== Token调试信息 ===

当前Session Storage:
- token: ${sessionStorage.getItem('token') || '无'}
- accessToken: ${sessionStorage.getItem('accessToken') || '无'}
- userToken: ${sessionStorage.getItem('userToken') || '无'}

当前LocalStorage:
- token: ${localStorage.getItem('token') || '无'}
- accessToken: ${localStorage.getItem('accessToken') || '无'}
- userToken: ${localStorage.getItem('userToken') || '无'}

URL参数:
- accessToken: ${new URLSearchParams(window.location.search).get('accessToken') || '无'}
- token: ${new URLSearchParams(window.location.search).get('token') || '无'}

建议：
1. 确保外部平台登录状态有效
2. 尝试重新登录外部平台
3. 检查token是否过期
`
  testResult.value = debugInfo
}

const testCurrentTokenDirectly = async () => {
  try {
    const token = sessionStorage.getItem('token')
    if (!token) {
      testResult.value = '❌ 当前没有token'
      return
    }
    
    testResult.value = `🔄 正在测试当前token: ${token}`
    
    const response = await fetch('http://localhost:18080/auth/verifyToken', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ accessToken: token })
    })
    
    const result = await response.json()
    testResult.value = `✅ 测试结果:\n${JSON.stringify(result, null, 2)}`
    
  } catch (error) {
    testResult.value = `❌ 测试失败: ${error.message}`
  }
}

const simulatePlatformJump = () => {
  // 模拟外部平台的跳转URL格式
  const realToken = 'd53163ba908e4a0ebe5bc260499d7b07'
  const platformJumpUrl = `http://oort.oortcloudsmart.com:21410/bus/vls-ui/workspace?accessToken=${realToken}&fromWhere=desktopHome`
  
  testResult.value = `🔄 模拟外部平台跳转...\nURL: ${platformJumpUrl}\n\n注意：这是外部平台的跳转格式，实际使用时需要从外部平台跳转过来`
  
  // 显示跳转URL供复制
  navigator.clipboard.writeText(platformJumpUrl).then(() => {
    testResult.value += '\n\n✅ 跳转URL已复制到剪贴板'
  })
}
</script>

<style scoped>
.test-auth-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.test-section {
  margin-bottom: 30px;
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
}

.test-section h3 {
  margin-top: 0;
  color: #303133;
  border-bottom: 2px solid #409eff;
  padding-bottom: 10px;
}

.token-info p {
  margin: 8px 0;
  font-family: monospace;
  background: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
}

.token-input {
  display: flex;
  align-items: center;
  gap: 10px;
}

.test-buttons, .clear-buttons, .jump-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.test-result {
  background: #f5f5f5;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 15px;
  max-height: 300px;
  overflow-y: auto;
}

.test-result pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: monospace;
  font-size: 12px;
}
</style> 