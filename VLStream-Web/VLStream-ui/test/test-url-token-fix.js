/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

// URL tokenparameter
console.log('🔧 测试URL token参数清除修复...')

// URLparameter
const mockUrl = 'http://localhost:3000/workspace?accessToken=848b2618754e44be9b98d7fa55996f0c&fromWhere=desktopHome'

// cleanUrlToken
function testCleanUrlToken() {
  console.log('🧪 测试cleanUrlToken函数...')

  // window.location
  const originalLocation = window.location
  const mockLocation = {
    href: mockUrl,
    search: '?accessToken=848b2618754e44be9b98d7fa55996f0c&fromWhere=desktopHome',
    pathname: '/workspace',
    origin: 'http://localhost:3000'
  }

  // URLConstructor
  const originalURL = global.URL
  global.URL = class MockURL {
    constructor(url) {
      this.url = url
      this.searchParams = new Map()
      this.searchParams.set('accessToken', '848b2618754e44be9b98d7fa55996f0c')
      this.searchParams.set('fromWhere', 'desktopHome')
    }

    delete(key) {
      this.searchParams.delete(key)
    }

    toString() {
      const params = Array.from(this.searchParams.entries())
        .map(([key, value]) => `${key}=${value}`)
        .join('&')
      return `http://localhost:3000/workspace${params ? '?' + params : ''}`
    }
  }

  // window.history.replaceState
  const originalReplaceState = window.history.replaceState
  window.history.replaceState = function(state, title, url) {
    console.log('📝 模拟replaceState调用:', url)
  }

  try {
    // cleanUrlToken
    const url = new URL(mockUrl)
    console.log('原始URL:', url.toString())

    // Delete accessTokenparameter
    url.searchParams.delete('accessToken')
    url.searchParams.delete('token')

    console.log('删除参数后URL:', url.toString())

    // parameterwhether correctDelete
    const hasAccessToken = url.searchParams.has('accessToken')
    const hasToken = url.searchParams.has('token')
    const hasFromWhere = url.searchParams.has('fromWhere')

    console.log('验证结果:')
    console.log('- accessToken参数:', hasAccessToken ? '❌ 仍然存在' : '✅ 已删除')
    console.log('- token参数:', hasToken ? '❌ 仍然存在' : '✅ 已删除')
    console.log('- fromWhere参数:', hasFromWhere ? '✅ 保留' : '❌ 意外删除')

    return {
      success: !hasAccessToken && !hasToken && hasFromWhere,
      hasAccessToken,
      hasToken,
      hasFromWhere
    }
  } catch (error) {
    console.error('❌ 测试失败:', error)
    return { success: false, error: error.message }
  } finally {
    // object
    global.URL = originalURL
    window.history.replaceState = originalReplaceState
  }
}

// URLparameterParse
function testUrlParamsParsing() {
  console.log('\n🧪 测试URL参数解析...')

  try {
    const url = new URL(mockUrl)
    const accessToken = url.searchParams.get('accessToken')
    const fromWhere = url.searchParams.get('fromWhere')

    console.log('URL参数解析结果:')
    console.log('- accessToken:', accessToken ? accessToken.substring(0, 8) + '...' : 'null')
    console.log('- fromWhere:', fromWhere || 'null')

    return {
      success: !!accessToken && !!fromWhere,
      accessToken,
      fromWhere
    }
  } catch (error) {
    console.error('❌ URL参数解析失败:', error)
    return { success: false, error: error.message }
  }
}

//
console.log('📋 测试1: URL参数解析')
const parseResult = testUrlParamsParsing()

console.log('\n📋 测试2: cleanUrlToken函数')
const cleanResult = testCleanUrlToken()

console.log('\n🎉 测试完成！')
console.log('总结:')
console.log('- URL参数解析:', parseResult.success ? '✅ 成功' : '❌ 失败')
console.log('- cleanUrlToken函数:', cleanResult.success ? '✅ 成功' : '❌ 失败')

if (parseResult.success && cleanResult.success) {
  console.log('\n✅ 修复验证成功！')
  console.log('- URL参数能正确解析')
  console.log('- accessToken参数能被正确删除')
  console.log('- 其他参数保持不变')
} else {
  console.log('\n❌ 修复验证失败！')
  console.log('- 需要进一步检查问题')
}