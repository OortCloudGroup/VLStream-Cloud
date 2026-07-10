/**
 * 测试getUserTenants API修复效果
 * 验证API从本地服务器改为统一用户平台
 */

// 测试getUserTenants API调用修复
console.log('🔧 测试getUserTenants API调用修复...')

// 模拟API配置
const testApiConfig = {
  baseURL: 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso',
  headers: {
    'Content-Type': 'application/json',
    'requesttype': 'app',
    'appid': '6551b0147c4649a894e86bf8de248da4',
    'secretkey': '58f9eeefc65f4b318204ba21f39a8861'
  }
}

// 测试getUserTenants API调用
async function testGetUserTenantsFix() {
  console.log('🧪 测试getUserTenants API调用修复...')
  
  const testToken = '848b2618754e44be9b98d7fa55996f0c'
  
  try {
    console.log('📡 发送getUserTenants请求...')
    console.log('- URL:', `${testApiConfig.baseURL}/sso/v1/getUserTenants`)
    console.log('- Token:', testToken.substring(0, 8) + '...')
    console.log('- Headers:', {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': testApiConfig.headers.appid,
      'secretkey': testApiConfig.headers.secretkey,
      'accesstoken': testToken
    })
    console.log('- Body:', '{}') // 空的请求体
    
    const response = await fetch(`${testApiConfig.baseURL}/sso/v1/getUserTenants`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'requesttype': 'app',
        'appid': testApiConfig.headers.appid,
        'secretkey': testApiConfig.headers.secretkey,
        'accesstoken': testToken
      },
      body: JSON.stringify({}) // 空的请求体
    })
    
    console.log('📥 收到响应:')
    console.log('- Status:', response.status)
    console.log('- StatusText:', response.statusText)
    
    if (response.ok) {
      const result = await response.json()
      console.log('✅ getUserTenants响应成功:', result)
      return { success: true, data: result }
    } else {
      const errorText = await response.text()
      console.log(`❌ getUserTenants HTTP错误: ${response.status}`)
      console.log('- Error:', errorText)
      return { success: false, error: errorText, status: response.status }
    }
  } catch (error) {
    console.error('❌ getUserTenants请求异常:', error)
    return { success: false, error: error.message }
  }
}

// 测试不同的请求体
async function testGetUserTenantsWithBody() {
  console.log('\n🧪 测试getUserTenants API调用（带请求体）...')
  
  const testToken = '848b2618754e44be9b98d7fa55996f0c'
  
  try {
    console.log('📡 发送getUserTenants请求（带请求体）...')
    console.log('- URL:', `${testApiConfig.baseURL}/sso/v1/getUserTenants`)
    console.log('- Token:', testToken.substring(0, 8) + '...')
    console.log('- Body:', { accessToken: testToken })
    
    const response = await fetch(`${testApiConfig.baseURL}/sso/v1/getUserTenants`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'requesttype': 'app',
        'appid': testApiConfig.headers.appid,
        'secretkey': testApiConfig.headers.secretkey,
        'accesstoken': testToken
      },
      body: JSON.stringify({ accessToken: testToken }) // 带accessToken的请求体
    })
    
    console.log('📥 收到响应:')
    console.log('- Status:', response.status)
    console.log('- StatusText:', response.statusText)
    
    if (response.ok) {
      const result = await response.json()
      console.log('✅ getUserTenants响应成功:', result)
      return { success: true, data: result }
    } else {
      const errorText = await response.text()
      console.log(`❌ getUserTenants HTTP错误: ${response.status}`)
      console.log('- Error:', errorText)
      return { success: false, error: errorText, status: response.status }
    }
  } catch (error) {
    console.error('❌ getUserTenants请求异常:', error)
    return { success: false, error: error.message }
  }
}

// 运行测试
console.log('📋 测试1: getUserTenants API调用（空请求体）')
const result1 = await testGetUserTenantsFix()

console.log('\n📋 测试2: getUserTenants API调用（带请求体）')
const result2 = await testGetUserTenantsWithBody()

console.log('\n🎉 测试完成！')
console.log('总结:')
console.log('- 空请求体测试:', result1.success ? '✅ 成功' : '❌ 失败')
console.log('- 带请求体测试:', result2.success ? '✅ 成功' : '❌ 失败')

if (result1.success) {
  console.log('\n✅ 修复验证成功！')
  console.log('- getUserTenants API能正确处理空请求体')
  console.log('- token通过请求头正确传递')
} else {
  console.log('\n❌ 修复验证失败！')
  console.log('- 需要进一步检查API调用问题')
  console.log('- 错误信息:', result1.error)
} 