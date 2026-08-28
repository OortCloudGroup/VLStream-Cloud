/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

/**
 *
 * after
 */

console.log('🚨 紧急修复测试')
console.log('=' * 50)

// all
function stopAllSyncers() {
  console.log('\n🛑 停止所有同步器...')

  //
  if (window.autoCrossSystemSync) {
    window.autoCrossSystemSync.stop()
    console.log('✅ 自动同步器已停止')
  }

  // all
  const highestTimeoutId = setTimeout(";");
  for (let i = 0; i < highestTimeoutId; i++) {
    clearTimeout(i);
    clearInterval(i);
  }
  console.log('🧹 所有定时器已清理')
}

// current
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

// new Initialize
function reinitializeSync() {
  console.log('\n🔄 重新初始化同步器...')

  if (window.autoCrossSystemSync) {
    //
    window.autoCrossSystemSync.clearValidationCache()
    window.autoCrossSystemSync.resetFailedAttempts()

    // new Initialize
    window.autoCrossSystemSync.init()
    console.log('✅ 同步器重新初始化完成')
  } else {
    console.log('❌ 自动同步器未找到')
  }
}

//
function testAntiDuplicate() {
  console.log('\n🔄 测试防重复运行机制...')

  if (!window.autoCrossSystemSync) {
    console.log('❌ 自动同步器未找到')
    return
  }

  //
  for (let i = 0; i < 5; i++) {
    console.log(`🔄 第${i + 1}次调用同步检查`)
    window.autoCrossSystemSync.forceSync()
  }

  console.log('✅ 防重复运行机制测试完成')
}

// userinfo
function testUserInfoPreservation() {
  console.log('\n👤 测试用户信息保留...')

  // userinfo
  const testUserInfo = {
    userName: '测试用户',
    userId: 'test123',
    loginId: 'testuser'
  }

  // userinfo
  sessionStorage.setItem('userInfo', JSON.stringify(testUserInfo))
  console.log('💾 保存测试用户信息')

  // token failed
  if (window.autoCrossSystemSync) {
    window.autoCrossSystemSync.clearInvalidToken()
    console.log('🧹 清除无效token')
  }

  // userinfowhether
  const preservedUserInfo = sessionStorage.getItem('userInfo')
  console.log('🔍 用户信息是否保留:', preservedUserInfo ? '是' : '否')

  return preservedUserInfo
}

//
async function runEmergencyFixTest() {
  console.log('🚨 开始紧急修复测试')
  console.log('=' * 60)

  // 1. all
  stopAllSyncers()

  // 2. current
  const status = checkCurrentStatus()

  // 3. userinfo
  const preservedUserInfo = testUserInfoPreservation()

  // 4. new Initialize
  reinitializeSync()

  // 5.
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

// Export
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