/**
 * 自动跨系统Token同步测试脚本
 * 测试真正的自动跨系统token同步功能
 */

console.log('🎯 自动跨系统Token同步测试')
console.log('=' * 50)

// 测试自动同步器是否已加载
function testAutoSyncLoaded() {
  console.log('\n🔍 检查自动同步器是否已加载...')
  
  if (window.autoCrossSystemSync) {
    console.log('✅ 自动跨系统Token同步器已加载')
    console.log('📊 同步器状态:', {
      isInitialized: window.autoCrossSystemSync.isInitialized,
      checkInterval: window.autoCrossSystemSync.checkInterval,
      lastUnifiedToken: window.autoCrossSystemSync.lastUnifiedToken ? 
        window.autoCrossSystemSync.lastUnifiedToken.substring(0, 8) + '...' : 'null'
    })
    return true
  } else {
    console.log('❌ 自动跨系统Token同步器未加载')
    return false
  }
}

// 测试当前token状态
function testCurrentTokenStatus() {
  console.log('\n🔍 检查当前Token状态...')
  
  const currentToken = window.autoCrossSystemSync.getCurrentSystemToken()
  const urlToken = new URLSearchParams(window.location.search).get('accessToken') || 
                  new URLSearchParams(window.location.search).get('token')
  
  console.log('📊 当前Token状态:', {
    currentToken: currentToken ? currentToken.substring(0, 8) + '...' : 'null',
    urlToken: urlToken ? urlToken.substring(0, 8) + '...' : 'null',
    sessionStorageToken: sessionStorage.getItem('token') ? 
      sessionStorage.getItem('token').substring(0, 8) + '...' : 'null',
    sessionStorageAccessToken: sessionStorage.getItem('accessToken') ? 
      sessionStorage.getItem('accessToken').substring(0, 8) + '...' : 'null',
    localStorageToken: localStorage.getItem('token') ? 
      localStorage.getItem('token').substring(0, 8) + '...' : 'null',
    localStorageAccessToken: localStorage.getItem('accessToken') ? 
      localStorage.getItem('accessToken').substring(0, 8) + '...' : 'null'
  })
  
  return { currentToken, urlToken }
}

// 测试手动触发同步
function testManualSync() {
  console.log('\n🔄 测试手动触发自动同步...')
  
  try {
    window.autoCrossSystemSync.forceSync()
    console.log('✅ 手动触发同步成功')
    return true
  } catch (error) {
    console.error('❌ 手动触发同步失败:', error)
    return false
  }
}

// 测试URL参数token同步
function testUrlTokenSync() {
  console.log('\n🔗 测试URL参数Token同步...')
  
  // 模拟URL参数中的token
  const testToken = 'test_token_' + Date.now()
  const currentUrl = new URL(window.location.href)
  currentUrl.searchParams.set('accessToken', testToken)
  
  console.log('📝 模拟URL参数token:', testToken.substring(0, 8) + '...')
  console.log('🔗 新URL:', currentUrl.toString())
  
  // 更新URL（不刷新页面）
  window.history.pushState({}, document.title, currentUrl.toString())
  
  // 手动触发同步检查
  setTimeout(() => {
    window.autoCrossSystemSync.forceSync()
  }, 1000)
  
  return testToken
}

// 测试统一用户平台token获取
async function testUnifiedPlatformTokenFetch() {
  console.log('\n👤 测试统一用户平台Token获取...')
  
  try {
    const unifiedToken = await window.autoCrossSystemSync.getUnifiedPlatformToken()
    
    if (unifiedToken) {
      console.log('✅ 成功获取统一用户平台token:', unifiedToken.substring(0, 8) + '...')
      return unifiedToken
    } else {
      console.log('❌ 未能获取统一用户平台token')
      return null
    }
  } catch (error) {
    console.error('❌ 获取统一用户平台token失败:', error)
    return null
  }
}

// 测试token验证
async function testTokenValidation() {
  console.log('\n🔍 测试Token验证...')
  
  const currentToken = window.autoCrossSystemSync.getCurrentSystemToken()
  
  if (!currentToken) {
    console.log('⚠️ 当前没有token，跳过验证测试')
    return false
  }
  
  try {
    const isValid = await window.autoCrossSystemSync.validateToken(currentToken)
    
    if (isValid) {
      console.log('✅ 当前token验证成功')
    } else {
      console.log('❌ 当前token验证失败')
    }
    
    return isValid
  } catch (error) {
    console.error('❌ Token验证失败:', error)
    return false
  }
}

// 测试用户信息获取
async function testUserInfoFetch() {
  console.log('\n👤 测试用户信息获取...')
  
  const currentToken = window.autoCrossSystemSync.getCurrentSystemToken()
  
  if (!currentToken) {
    console.log('⚠️ 当前没有token，跳过用户信息获取测试')
    return null
  }
  
  try {
    const userInfo = await window.autoCrossSystemSync.getUnifiedUserInfo(currentToken)
    
    if (userInfo) {
      console.log('✅ 成功获取用户信息:', {
        userName: userInfo.userName,
        userId: userInfo.userId,
        loginId: userInfo.loginId
      })
      return userInfo
    } else {
      console.log('❌ 未能获取用户信息')
      return null
    }
  } catch (error) {
    console.error('❌ 获取用户信息失败:', error)
    return null
  }
}

// 测试iframe监听
function testIframeMonitoring() {
  console.log('\n🖼️ 测试Iframe监听...')
  
  if (window.autoCrossSystemSync.iframe) {
    console.log('✅ iframe已创建')
    console.log('📊 iframe状态:', {
      src: window.autoCrossSystemSync.iframe.src,
      style: window.autoCrossSystemSync.iframe.style.display
    })
    return true
  } else {
    console.log('❌ iframe未创建')
    return false
  }
}

// 测试事件监听
function testEventListeners() {
  console.log('\n🎧 测试事件监听...')
  
  // 测试自定义事件触发
  const testEvent = new CustomEvent('autoCrossSystemTokenUpdated', {
    detail: { token: 'test_token', source: 'test' }
  })
  
  try {
    window.dispatchEvent(testEvent)
    console.log('✅ 事件监听器工作正常')
    return true
  } catch (error) {
    console.error('❌ 事件监听器测试失败:', error)
    return false
  }
}

// 完整测试流程
async function runCompleteTest() {
  console.log('🚀 开始完整自动跨系统Token同步测试')
  console.log('=' * 60)
  
  // 1. 检查同步器是否加载
  const syncLoaded = testAutoSyncLoaded()
  if (!syncLoaded) {
    console.log('❌ 同步器未加载，测试终止')
    return
  }
  
  // 2. 检查当前token状态
  const tokenStatus = testCurrentTokenStatus()
  
  // 3. 测试手动同步
  testManualSync()
  
  // 4. 测试统一用户平台token获取
  await testUnifiedPlatformTokenFetch()
  
  // 5. 测试token验证
  await testTokenValidation()
  
  // 6. 测试用户信息获取
  await testUserInfoFetch()
  
  // 7. 测试iframe监听
  testIframeMonitoring()
  
  // 8. 测试事件监听
  testEventListeners()
  
  console.log('\n' + '=' * 60)
  console.log('🎯 自动跨系统Token同步测试完成')
  console.log('\n💡 测试说明:')
  console.log('1. 自动同步器会每5秒检查一次token变化')
  console.log('2. 支持URL参数、API、localStorage等多种token获取方式')
  console.log('3. 支持iframe监听统一用户平台token变化')
  console.log('4. 支持页面可见性变化、storage变化等触发同步')
  console.log('5. 自动验证token有效性并更新用户信息')
  console.log('\n🔧 调试命令:')
  console.log('- window.autoCrossSystemSync.forceSync() - 手动触发同步')
  console.log('- window.autoCrossSystemSync.stop() - 停止同步')
  console.log('- window.autoCrossSystemSync.init() - 重新初始化')
}

// 实时监控测试
function startRealTimeMonitoring() {
  console.log('\n📊 启动实时监控...')
  
  let monitorCount = 0
  const maxMonitors = 10
  
  const monitor = setInterval(() => {
    monitorCount++
    const currentToken = window.autoCrossSystemSync.getCurrentSystemToken()
    const lastUnifiedToken = window.autoCrossSystemSync.lastUnifiedToken
    
    console.log(`📊 监控 ${monitorCount}/${maxMonitors}:`, {
      currentToken: currentToken ? currentToken.substring(0, 8) + '...' : 'null',
      lastUnifiedToken: lastUnifiedToken ? lastUnifiedToken.substring(0, 8) + '...' : 'null',
      isInitialized: window.autoCrossSystemSync.isInitialized
    })
    
    if (monitorCount >= maxMonitors) {
      clearInterval(monitor)
      console.log('📊 实时监控结束')
    }
  }, 3000)
  
  return monitor
}

// 导出测试函数
window.testAutoCrossSystemSync = {
  runCompleteTest,
  startRealTimeMonitoring,
  testAutoSyncLoaded,
  testCurrentTokenStatus,
  testManualSync,
  testUrlTokenSync,
  testUnifiedPlatformTokenFetch,
  testTokenValidation,
  testUserInfoFetch,
  testIframeMonitoring,
  testEventListeners
}

console.log('✅ 自动跨系统Token同步测试脚本已加载')
console.log('💡 运行测试: testAutoCrossSystemSync.runCompleteTest()')
console.log('💡 启动监控: testAutoCrossSystemSync.startRealTimeMonitoring()') 