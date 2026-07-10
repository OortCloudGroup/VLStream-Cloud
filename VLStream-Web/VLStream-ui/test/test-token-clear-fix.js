/**
 * Token清除修复测试脚本
 * 测试修复后的token清除逻辑
 */

console.log('🔧 Token清除修复测试')
console.log('=' * 50)

// 测试token有效性检查
function testTokenValidity() {
  console.log('\n🔍 测试Token有效性检查...')
  
  const testCases = [
    { token: '3461f0e6...', expected: true, desc: '有效token' },
    { token: 'undefined', expected: false, desc: '字符串undefined' },
    { token: 'null', expected: false, desc: '字符串null' },
    { token: '', expected: false, desc: '空字符串' },
    { token: '   ', expected: false, desc: '空白字符串' },
    { token: null, expected: false, desc: 'null值' },
    { token: undefined, expected: false, desc: 'undefined值' }
  ]
  
  testCases.forEach((testCase, index) => {
    const isValid = isValidToken(testCase.token)
    const result = isValid === testCase.expected ? '✅' : '❌'
    console.log(`${result} ${testCase.desc}: "${testCase.token}" -> ${isValid ? '有效' : '无效'}`)
  })
}

// 检查token是否有效
function isValidToken(token) {
  return token && 
         token !== 'undefined' && 
         token !== 'null' && 
         token.trim() !== ''
}

// 测试当前token状态
function testCurrentTokenState() {
  console.log('\n🔍 测试当前Token状态...')
  
  if (!window.autoCrossSystemSync) {
    console.log('❌ 自动同步器未找到')
    return
  }
  
  const currentToken = window.autoCrossSystemSync.getCurrentSystemToken()
  console.log('📊 当前系统Token:', {
    token: currentToken ? currentToken.substring(0, 8) + '...' : 'null',
    isValid: isValidToken(currentToken),
    length: currentToken ? currentToken.length : 0
  })
  
  return currentToken
}

// 测试存储中的token
function testStoredTokens() {
  console.log('\n💾 测试存储中的Token...')
  
  const storageKeys = ['accessToken', 'token']
  const storageTypes = ['sessionStorage', 'localStorage']
  
  storageTypes.forEach(type => {
    console.log(`\n📂 ${type}:`)
    storageKeys.forEach(key => {
      const value = type === 'sessionStorage' ? sessionStorage.getItem(key) : localStorage.getItem(key)
      const isValid = isValidToken(value)
      console.log(`  ${key}: "${value}" -> ${isValid ? '✅ 有效' : '❌ 无效'}`)
    })
  })
}

// 测试验证缓存状态
function testValidationCache() {
  console.log('\n💾 测试验证缓存状态...')
  
  if (!window.autoCrossSystemSync) {
    console.log('❌ 自动同步器未找到')
    return
  }
  
  const sync = window.autoCrossSystemSync
  console.log('📊 验证缓存状态:', {
    tokenValidationCache: sync.tokenValidationCache.size,
    failedTokenCache: sync.failedTokenCache.size,
    failedAttempts: sync.failedAttempts.size,
    lastValidationTime: sync.lastValidationTime,
    validationCooldown: sync.validationCooldown
  })
  
  // 显示缓存内容
  if (sync.tokenValidationCache.size > 0) {
    console.log('✅ 验证缓存内容:')
    sync.tokenValidationCache.forEach((value, key) => {
      console.log(`  ${key.substring(0, 8)}... -> ${value}`)
    })
  }
  
  if (sync.failedTokenCache.size > 0) {
    console.log('❌ 失败缓存内容:')
    sync.failedTokenCache.forEach(token => {
      console.log(`  ${token.substring(0, 8)}...`)
    })
  }
}

// 清理验证缓存
function clearValidationCache() {
  console.log('\n🧹 清理验证缓存...')
  
  if (!window.autoCrossSystemSync) {
    console.log('❌ 自动同步器未找到')
    return false
  }
  
  try {
    window.autoCrossSystemSync.clearValidationCache()
    console.log('✅ 验证缓存已清理')
    return true
  } catch (error) {
    console.error('❌ 清理验证缓存失败:', error)
    return false
  }
}

// 重置失败记录
function resetFailedAttempts() {
  console.log('\n🔄 重置失败记录...')
  
  if (!window.autoCrossSystemSync) {
    console.log('❌ 自动同步器未找到')
    return false
  }
  
  try {
    window.autoCrossSystemSync.resetFailedAttempts()
    console.log('✅ 失败记录已重置')
    return true
  } catch (error) {
    console.error('❌ 重置失败记录失败:', error)
    return false
  }
}

// 测试手动同步
function testManualSync() {
  console.log('\n🔄 测试手动同步...')
  
  if (!window.autoCrossSystemSync) {
    console.log('❌ 自动同步器未找到')
    return false
  }
  
  try {
    window.autoCrossSystemSync.forceSync()
    console.log('✅ 手动触发同步成功')
    return true
  } catch (error) {
    console.error('❌ 手动触发同步失败:', error)
    return false
  }
}

// 模拟token验证失败场景
function simulateTokenValidationFailure() {
  console.log('\n🧪 模拟Token验证失败场景...')
  
  if (!window.autoCrossSystemSync) {
    console.log('❌ 自动同步器未找到')
    return
  }
  
  const sync = window.autoCrossSystemSync
  const currentToken = sync.getCurrentSystemToken()
  
  if (!currentToken) {
    console.log('⚠️ 当前没有token，无法模拟')
    return
  }
  
  console.log('📊 模拟前状态:')
  console.log(`  当前token: ${currentToken.substring(0, 8)}...`)
  console.log(`  验证缓存大小: ${sync.tokenValidationCache.size}`)
  console.log(`  失败缓存大小: ${sync.failedTokenCache.size}`)
  
  // 模拟验证失败
  sync.failedTokenCache.add(currentToken)
  sync.failedAttempts.set(currentToken, 3)
  
  console.log('📊 模拟后状态:')
  console.log(`  失败缓存大小: ${sync.failedTokenCache.size}`)
  console.log(`  失败次数: ${sync.failedAttempts.get(currentToken)}`)
  
  console.log('💡 现在token应该被标记为失败，不会再次验证')
}

// 完整修复测试
async function runTokenClearFixTest() {
  console.log('🔧 开始Token清除修复测试')
  console.log('=' * 60)
  
  // 1. 测试token有效性检查
  testTokenValidity()
  
  // 2. 测试当前token状态
  const currentToken = testCurrentTokenState()
  
  // 3. 测试存储中的token
  testStoredTokens()
  
  // 4. 测试验证缓存状态
  testValidationCache()
  
  // 5. 清理验证缓存
  clearValidationCache()
  
  // 6. 重置失败记录
  resetFailedAttempts()
  
  // 7. 测试手动同步
  testManualSync()
  
  // 8. 模拟token验证失败场景
  simulateTokenValidationFailure()
  
  console.log('\n' + '=' * 60)
  console.log('🎯 Token清除修复测试完成')
  console.log('\n💡 修复内容:')
  console.log('1. 修复验证冷却逻辑，避免错误清除有效token')
  console.log('2. 改进token有效性判断，只清除真正无效的token')
  console.log('3. 避免过度清理，保留用户信息')
  console.log('4. 添加网络问题检测，避免临时验证失败导致清除')
  console.log('\n🔧 调试命令:')
  console.log('- testCurrentTokenState() - 查看当前token状态')
  console.log('- testStoredTokens() - 查看存储中的token')
  console.log('- testValidationCache() - 查看验证缓存')
  console.log('- clearValidationCache() - 清理验证缓存')
  console.log('- resetFailedAttempts() - 重置失败记录')
  console.log('- testManualSync() - 测试手动同步')
}

// 导出测试函数
window.testTokenClearFix = {
  runTokenClearFixTest,
  testTokenValidity,
  testCurrentTokenState,
  testStoredTokens,
  testValidationCache,
  clearValidationCache,
  resetFailedAttempts,
  testManualSync,
  simulateTokenValidationFailure,
  isValidToken
}

console.log('✅ Token清除修复测试脚本已加载')
console.log('💡 运行测试: testTokenClearFix.runTokenClearFixTest()') 