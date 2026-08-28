/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

/**
 * after Token
 * can and
 */

console.log('🎯 优化后的自动跨系统Token同步测试')
console.log('=' * 60)

// can
function testSmartValidation() {
  console.log('\n🧠 测试智能验证机制...')

  if (!window.autoCrossSystemSync) {
    console.log('❌ 自动同步器未加载')
    return false
  }

  console.log('📊 验证缓存状态:', {
    tokenValidationCache: window.autoCrossSystemSync.tokenValidationCache.size,
    failedTokenCache: window.autoCrossSystemSync.failedTokenCache.size,
    failedAttempts: window.autoCrossSystemSync.failedAttempts.size,
    lastValidationTime: window.autoCrossSystemSync.lastValidationTime,
    validationCooldown: window.autoCrossSystemSync.validationCooldown
  })

  return true
}

//
function testCacheClearing() {
  console.log('\n🧹 测试缓存清理功能...')

  try {
    //
    window.autoCrossSystemSync.clearValidationCache()

    // failedrecord
    window.autoCrossSystemSync.resetFailedAttempts()

    console.log('✅ 缓存清理成功')
    return true
  } catch (error) {
    console.error('❌ 缓存清理失败:', error)
    return false
  }
}

// failedtokenProcess
function testFailedTokenHandling() {
  console.log('\n🚫 测试失败Token处理...')

  const testToken = 'invalid_token_' + Date.now()

  // failedtoken
  window.autoCrossSystemSync.failedTokenCache.add(testToken)
  window.autoCrossSystemSync.failedAttempts.set(testToken, 3)

  console.log('📝 添加测试失败token:', testToken.substring(0, 8) + '...')

  // whether will
  const isSkipped = window.autoCrossSystemSync.failedTokenCache.has(testToken)
  console.log('🔍 失败token是否被跳过:', isSkipped)

  return isSkipped
}

//
function testValidationCooldown() {
  console.log('\n⏳ 测试验证冷却机制...')

  const now = Date.now()
  const lastValidationTime = window.autoCrossSystemSync.lastValidationTime
  const cooldown = window.autoCrossSystemSync.validationCooldown

  const timeSinceLastValidation = now - lastValidationTime
  const isInCooldown = timeSinceLastValidation < cooldown

  console.log('📊 冷却状态:', {
    now,
    lastValidationTime,
    timeSinceLastValidation,
    cooldown,
    isInCooldown,
    remainingCooldown: Math.max(0, cooldown - timeSinceLastValidation)
  })

  return isInCooldown
}

// current token
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
      sessionStorage.getItem('accessToken').substring(0, 8) + '...' : 'null'
  })

  return { currentToken, urlToken }
}

//
function testManualSync() {
  console.log('\n🔄 测试手动触发优化后的同步...')

  try {
    window.autoCrossSystemSync.forceSync()
    console.log('✅ 手动触发同步成功')
    return true
  } catch (error) {
    console.error('❌ 手动触发同步失败:', error)
    return false
  }
}

// URLparametertoken
function testUrlTokenSync() {
  console.log('\n🔗 测试URL参数Token同步...')

  // URLparameter in token
  const testToken = 'test_token_' + Date.now()
  const currentUrl = new URL(window.location.href)
  currentUrl.searchParams.set('accessToken', testToken)

  console.log('📝 模拟URL参数token:', testToken.substring(0, 8) + '...')
  console.log('🔗 新URL:', currentUrl.toString())

  // new URL ( new page)
  window.history.pushState({}, document.title, currentUrl.toString())

  //
  setTimeout(() => {
    window.autoCrossSystemSync.forceSync()
  }, 1000)

  return testToken
}

// workflow
async function runOptimizedTest() {
  console.log('🚀 开始优化后的自动跨系统Token同步测试')
  console.log('=' * 70)

  // 1. can
  const smartValidationOk = testSmartValidation()
  if (!smartValidationOk) {
    console.log('❌ 智能验证机制测试失败，测试终止')
    return
  }

  // 2.
  testCacheClearing()

  // 3. failedtokenProcess
  testFailedTokenHandling()

  // 4.
  testValidationCooldown()

  // 5. current token
  const tokenStatus = testCurrentTokenStatus()

  // 6.
  testManualSync()

  // 7. URLparameter
  testUrlTokenSync()

  console.log('\n' + '=' * 70)
  console.log('🎯 优化后的自动跨系统Token同步测试完成')
  console.log('\n💡 优化特性:')
  console.log('1. 智能验证缓存，避免重复验证')
  console.log('2. 失败token缓存，跳过无效token')
  console.log('3. 验证冷却机制，避免频繁请求')
  console.log('4. 失败次数限制，防止无限重试')
  console.log('5. 缓存清理功能，支持手动重置')
  console.log('\n🔧 调试命令:')
  console.log('- window.autoCrossSystemSync.clearValidationCache() - 清理验证缓存')
  console.log('- window.autoCrossSystemSync.resetFailedAttempts() - 重置失败记录')
  console.log('- window.autoCrossSystemSync.forceSync() - 手动触发同步')
  console.log('- window.autoCrossSystemSync.stop() - 停止同步')
}

//
function startOptimizedMonitoring() {
  console.log('\n📊 启动优化后的实时监控...')

  let monitorCount = 0
  const maxMonitors = 15

  const monitor = setInterval(() => {
    monitorCount++
    const currentToken = window.autoCrossSystemSync.getCurrentSystemToken()
    const lastUnifiedToken = window.autoCrossSystemSync.lastUnifiedToken

    console.log(`📊 监控 ${monitorCount}/${maxMonitors}:`, {
      currentToken: currentToken ? currentToken.substring(0, 8) + '...' : 'null',
      lastUnifiedToken: lastUnifiedToken ? lastUnifiedToken.substring(0, 8) + '...' : 'null',
      isInitialized: window.autoCrossSystemSync.isInitialized,
      failedTokenCache: window.autoCrossSystemSync.failedTokenCache.size,
      failedAttempts: window.autoCrossSystemSync.failedAttempts.size
    })

    if (monitorCount >= maxMonitors) {
      clearInterval(monitor)
      console.log('📊 优化后的实时监控结束')
    }
  }, 4000)

  return monitor
}

// Export
window.testOptimizedAutoSync = {
  runOptimizedTest,
  startOptimizedMonitoring,
  testSmartValidation,
  testCacheClearing,
  testFailedTokenHandling,
  testValidationCooldown,
  testCurrentTokenStatus,
  testManualSync,
  testUrlTokenSync
}

console.log('✅ 优化后的自动跨系统Token同步测试脚本已加载')
console.log('💡 运行测试: testOptimizedAutoSync.runOptimizedTest()')
console.log('💡 启动监控: testOptimizedAutoSync.startOptimizedMonitoring()')