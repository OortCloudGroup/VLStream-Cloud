/**
 * 紧急修复测试脚本
 * 测试修复后的自动同步器
 */

console.log('🚨 紧急修复测试')
console.log('=' * 50)

// 停止所有同步器
function stopAllSyncers() {
  console.log('\n🛑 停止所有同步器...')
  
  // 停止自动同步器
  if (window.autoCrossSystemSync) {
    window.autoCrossSystemSync.stop()
    console.log('✅ 自动同步器已停止')
  }
  
  // 清理所有定时器
  const highestTimeoutId = setTimeout(";");
  for (let i = 0; i < highestTimeoutId; i++) {
    clearTimeout(i);
    clearInterval(i);
  }
  console.log('🧹 所有定时器已清理')
}

// 检查当前状态
function checkCurrentStatus() {
  console.log('\n📊 检查当前状态...')
  
  const currentToken = sessionStorage.getItem('accessToken') || 
                      localStorage.getItem('accessToken') ||
                      sessionStorage.getItem('token') ||
                      localStorage.getItem('token')
  
  const userInfo = sessionStorage.getItem('userInfo') || localStorage.getItem('userInfo')
  
  console.log('📊 当前状态:', {
    currentToken: currentToken ? currentToken.substring(0, 8) + '...' : 'null',
    userInfo: userInfo ? '存在' : 'null',
    autoSyncInitialized: window.autoCrossSystemSync ? window.autoCrossSystemSync.isInitialized : false,
    isRunning: window.autoCrossSystemSync ? window.autoCrossSystemSync.isRunning : false
  })
  
  return { currentToken, userInfo }
}

// 重新初始化同步器
function reinitializeSync() {
  console.log('\n🔄 重新初始化同步器...')
  
  if (window.autoCrossSystemSync) {
    // 清理缓存
    window.autoCrossSystemSync.clearValidationCache()
    window.autoCrossSystemSync.resetFailedAttempts()
    
    // 重新初始化
    window.autoCrossSystemSync.init()
    console.log('✅ 同步器重新初始化完成')
  } else {
    console.log('❌ 自动同步器未找到')
  }
}

// 测试防重复运行机制
function testAntiDuplicate() {
  console.log('\n🔄 测试防重复运行机制...')
  
  if (!window.autoCrossSystemSync) {
    console.log('❌ 自动同步器未找到')
    return
  }
  
  // 连续调用多次
  for (let i = 0; i < 5; i++) {
    console.log(`🔄 第${i + 1}次调用同步检查`)
    window.autoCrossSystemSync.forceSync()
  }
  
  console.log('✅ 防重复运行机制测试完成')
}

// 测试用户信息保留
function testUserInfoPreservation() {
  console.log('\n👤 测试用户信息保留...')
  
  // 模拟用户信息
  const testUserInfo = {
    userName: '测试用户',
    userId: 'test123',
    loginId: 'testuser'
  }
  
  // 保存用户信息
  sessionStorage.setItem('userInfo', JSON.stringify(testUserInfo))
  console.log('💾 保存测试用户信息')
  
  // 模拟token验证失败
  if (window.autoCrossSystemSync) {
    window.autoCrossSystemSync.clearInvalidToken()
    console.log('🧹 清除无效token')
  }
  
  // 检查用户信息是否保留
  const preservedUserInfo = sessionStorage.getItem('userInfo')
  console.log('🔍 用户信息是否保留:', preservedUserInfo ? '是' : '否')
  
  return preservedUserInfo
}

// 完整修复测试
async function runEmergencyFixTest() {
  console.log('🚨 开始紧急修复测试')
  console.log('=' * 60)
  
  // 1. 停止所有同步器
  stopAllSyncers()
  
  // 2. 检查当前状态
  const status = checkCurrentStatus()
  
  // 3. 测试用户信息保留
  const preservedUserInfo = testUserInfoPreservation()
  
  // 4. 重新初始化同步器
  reinitializeSync()
  
  // 5. 测试防重复运行机制
  testAntiDuplicate()
  
  console.log('\n' + '=' * 60)
  console.log('🎯 紧急修复测试完成')
  console.log('\n💡 修复内容:')
  console.log('1. 停止多余的同步器，避免冲突')
  console.log('2. 修复清理逻辑，保留用户信息')
  console.log('3. 添加防重复运行机制')
  console.log('4. 避免过度清理用户数据')
  console.log('\n🔧 调试命令:')
  console.log('- window.autoCrossSystemSync.forceSync() - 手动触发同步')
  console.log('- window.autoCrossSystemSync.stop() - 停止同步')
  console.log('- window.autoCrossSystemSync.clearValidationCache() - 清理缓存')
}

// 导出测试函数
window.testEmergencyFix = {
  runEmergencyFixTest,
  stopAllSyncers,
  checkCurrentStatus,
  reinitializeSync,
  testAntiDuplicate,
  testUserInfoPreservation
}

console.log('✅ 紧急修复测试脚本已加载')
console.log('💡 运行测试: testEmergencyFix.runEmergencyFixTest()') 