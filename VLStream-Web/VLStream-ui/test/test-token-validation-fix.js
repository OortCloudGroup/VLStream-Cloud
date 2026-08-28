/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

/**
 * Token
 * after token
 */

console.log('🔧 Token验证修复测试')
console.log('=' * 50)

// token
function testTokenValidityCheck() {
  console.log('\n🔍 测试Token有效性检查...')

  const testTokens = [
    'valid_token_123',
    'undefined',
    'null',
    '',
    '   ',
    null,
    undefined
  ]

  testTokens.forEach((token, index) => {
    const isValid = isValidToken(token)
    console.log(`Token ${index + 1}: "${token}" -> ${isValid ? '✅ 有效' : '❌ 无效'}`)
  })
}

// tokenwhether
function isValidToken(token) {
  return token &&
         token !== 'undefined' &&
         token !== 'null' &&
         token.trim() !== ''
}

// current tokenGet
function testCurrentSystemToken() {
  console.log('\n🔍 测试当前系统Token获取...')

  if (!window.autoCrossSystemSync) {
    console.log('❌ 自动同步器未找到')
    return
  }

  const currentToken = window.autoCrossSystemSync.getCurrentSystemToken()
  console.log('📊 当前系统Token:', {
    token: currentToken ? currentToken.substring(0, 8) + '...' : 'null',
    isValid: isValidToken(currentToken)
  })

  return currentToken
}

// in token
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

// token
function cleanInvalidTokens() {
  console.log('\n🧹 清理无效Token...')

  const storageKeys = ['accessToken', 'token']
  const storageTypes = ['sessionStorage', 'localStorage']

  let cleanedCount = 0

  storageTypes.forEach(type => {
    storageKeys.forEach(key => {
      const value = type === 'sessionStorage' ? sessionStorage.getItem(key) : localStorage.getItem(key)
      if (!isValidToken(value)) {
        if (type === 'sessionStorage') {
          sessionStorage.removeItem(key)
        } else {
          localStorage.removeItem(key)
        }
        console.log(`🗑️ 清理${type}.${key}: "${value}"`)
        cleanedCount++
      }
    })
  })

  console.log(`✅ 清理完成，共清理 ${cleanedCount} 个无效token`)
  return cleanedCount
}

//
function testManualSync() {
  console.log('\n🔄 测试手动触发同步...')

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

//
function testValidationCache() {
  console.log('\n💾 测试验证缓存...')

  if (!window.autoCrossSystemSync) {
    console.log('❌ 自动同步器未找到')
    return
  }

  console.log('📊 验证缓存状态:', {
    tokenValidationCache: window.autoCrossSystemSync.tokenValidationCache.size,
    failedTokenCache: window.autoCrossSystemSync.failedTokenCache.size,
    failedAttempts: window.autoCrossSystemSync.failedAttempts.size
  })

  //
  window.autoCrossSystemSync.clearValidationCache()
  console.log('🧹 验证缓存已清理')
}

//
async function runTokenValidationFixTest() {
  console.log('🔧 开始Token验证修复测试')
  console.log('=' * 60)

  // 1. token
  testTokenValidityCheck()

  // 2. in token
  testStoredTokens()

  // 3. token
  const cleanedCount = cleanInvalidTokens()

  // 4. current tokenGet
  const currentToken = testCurrentSystemToken()

  // 5.
  testValidationCache()

  // 6.
  testManualSync()

  console.log('\n' + '=' * 60)
  console.log('🎯 Token验证修复测试完成')
  console.log('\n💡 修复内容:')
  console.log('1. 过滤掉undefined、null、空字符串等无效token')
  console.log('2. 避免验证无效token')
  console.log('3. 只清除真正无效的token')
  console.log('4. 保留有效的token和用户信息')
  console.log(`5. 清理了 ${cleanedCount} 个无效token`)
  console.log('\n🔧 调试命令:')
  console.log('- testStoredTokens() - 查看存储中的token')
  console.log('- cleanInvalidTokens() - 清理无效token')
  console.log('- testCurrentSystemToken() - 测试当前token获取')
  console.log('- testValidationCache() - 测试验证缓存')
}

// Export
window.testTokenValidationFix = {
  runTokenValidationFixTest,
  testTokenValidityCheck,
  testCurrentSystemToken,
  testStoredTokens,
  cleanInvalidTokens,
  testManualSync,
  testValidationCache,
  isValidToken
}

console.log('✅ Token验证修复测试脚本已加载')
console.log('💡 运行测试: testTokenValidationFix.runTokenValidationFixTest()')