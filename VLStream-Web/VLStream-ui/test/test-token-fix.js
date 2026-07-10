// Token校验流程修复测试脚本
// 用于验证修复后的token校验逻辑

console.log('🔧 Token校验流程修复测试开始...')

// 模拟测试场景
const testScenarios = [
  {
    name: '有效token验证',
    token: '848b2618754e44be9b98d7fa55996f0c',
    expectedResult: 'success'
  },
  {
    name: '无效token验证',
    token: 'invalid_token_123',
    expectedResult: 'failure'
  },
  {
    name: '网络错误场景',
    token: '848b2618754e44be9b98d7fa55996f0c',
    expectedResult: 'network_error'
  }
]

// 测试API配置
const testApiConfig = {
  baseURL: 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso',
  headers: {
    'Content-Type': 'application/json',
    'requesttype': 'app',
    'appid': '6551b0147c4649a894e86bf8de248da4',
    'secretkey': '58f9eeefc65f4b318204ba21f39a8861'
  }
}

// 测试token验证函数
async function testTokenValidation(token) {
  try {
    console.log(`🔍 测试token: ${token.substring(0, 8)}...`)
    
    const response = await fetch(`${testApiConfig.baseURL}/sso/v1/verifyToken`, {
      method: 'POST',
      headers: {
        ...testApiConfig.headers,
        'accesstoken': token
      },
      body: JSON.stringify({
        accessToken: token
      })
    })
    
    if (response.ok) {
      const result = await response.json()
      console.log('✅ API响应成功:', result)
      
      // 使用修复后的响应处理逻辑
      const apiResponse = result.data || result
      
      if (apiResponse && (apiResponse.code === 200 || apiResponse.success === true)) {
        console.log('✅ Token验证成功')
        return { success: true, data: apiResponse.data }
      } else {
        console.log('❌ Token验证失败:', apiResponse)
        return { success: false, error: apiResponse }
      }
    } else {
      console.log(`❌ HTTP错误: ${response.status}`)
      return { success: false, error: { status: response.status } }
    }
  } catch (error) {
    console.error('❌ 请求异常:', error)
    return { success: false, error: error.message }
  }
}

// 测试getUserTenants API
async function testGetUserTenants(token) {
  try {
    console.log(`🔍 测试getUserTenants API: ${token.substring(0, 8)}...`)
    
    const response = await fetch(`${testApiConfig.baseURL}/sso/v1/getUserTenants`, {
      method: 'POST',
      headers: {
        ...testApiConfig.headers,
        'accesstoken': token
      },
      body: JSON.stringify({
        accessToken: token
      })
    })
    
    if (response.ok) {
      const result = await response.json()
      console.log('✅ getUserTenants响应成功:', result)
      
      // 使用修复后的响应处理逻辑
      const apiResponse = result.data || result
      
      if (apiResponse && (apiResponse.code === 200 || apiResponse.success === true)) {
        return { success: true, data: apiResponse.data }
      } else {
        return { success: false, error: apiResponse }
      }
    } else {
      console.log(`❌ getUserTenants HTTP错误: ${response.status}`)
      return { success: false, error: { status: response.status } }
    }
  } catch (error) {
    console.error('❌ getUserTenants请求异常:', error)
    return { success: false, error: error.message }
  }
}

// 运行测试
async function runTests() {
  console.log('🚀 开始运行token校验测试...')
  
  for (const scenario of testScenarios) {
    console.log(`\n📋 测试场景: ${scenario.name}`)
    
    // 测试token验证
    const validationResult = await testTokenValidation(scenario.token)
    
    if (validationResult.success) {
      // 如果token验证成功，测试getUserTenants
      const userResult = await testGetUserTenants(scenario.token)
      console.log('用户信息获取结果:', userResult)
    }
    
    console.log(`✅ 场景 "${scenario.name}" 测试完成`)
  }
  
  console.log('\n🎉 所有测试完成！')
}

// 导出测试函数
if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    testTokenValidation,
    testGetUserTenants,
    runTests
  }
} else {
  // 在浏览器环境中运行测试
  runTests()
} 