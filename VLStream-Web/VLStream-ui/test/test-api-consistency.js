// 测试API调用一致性
console.log('🔧 测试API调用一致性...')

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

// 测试verifyToken API
async function testVerifyToken() {
  console.log('🧪 测试verifyToken API...')
  
  const testToken = '848b2618754e44be9b98d7fa55996f0c'
  
  try {
    console.log('📡 发送verifyToken请求...')
    console.log('- URL:', `${testApiConfig.baseURL}/sso/v1/verifyToken`)
    console.log('- Token:', testToken.substring(0, 8) + '...')
    console.log('- Headers:', {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': testApiConfig.headers.appid,
      'secretkey': testApiConfig.headers.secretkey,
      'accesstoken': testToken
    })
    console.log('- Body:', { accessToken: testToken })
    
    const response = await fetch(`${testApiConfig.baseURL}/sso/v1/verifyToken`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'requesttype': 'app',
        'appid': testApiConfig.headers.appid,
        'secretkey': testApiConfig.headers.secretkey,
        'accesstoken': testToken
      },
      body: JSON.stringify({ accessToken: testToken })
    })
    
    console.log('📥 收到响应:')
    console.log('- Status:', response.status)
    console.log('- StatusText:', response.statusText)
    
    if (response.ok) {
      const result = await response.json()
      console.log('✅ verifyToken响应成功:', result)
      return { success: true, data: result }
    } else {
      const errorText = await response.text()
      console.log(`❌ verifyToken HTTP错误: ${response.status}`)
      console.log('- Error:', errorText)
      return { success: false, error: errorText, status: response.status }
    }
  } catch (error) {
    console.error('❌ verifyToken请求异常:', error)
    return { success: false, error: error.message }
  }
}

// 测试getUserTenants API（修复前）
async function testGetUserTenantsBefore() {
  console.log('\n🧪 测试getUserTenants API（修复前 - 空请求体）...')
  
  const testToken = '848b2618754e44be9b98d7fa55996f0c'
  
  try {
    console.log('📡 发送getUserTenants请求（修复前）...')
    console.log('- URL:', `${testApiConfig.baseURL}/sso/v1/getUserTenants`)
    console.log('- Token:', testToken.substring(0, 8) + '...')
    console.log('- Headers:', {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': testApiConfig.headers.appid,
      'secretkey': testApiConfig.headers.secretkey,
      'accesstoken': testToken
    })
    console.log('- Body:', '{}')
    
    const response = await fetch(`${testApiConfig.baseURL}/sso/v1/getUserTenants`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'requesttype': 'app',
        'appid': testApiConfig.headers.appid,
        'secretkey': testApiConfig.headers.secretkey,
        'accesstoken': testToken
      },
      body: JSON.stringify({})
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

// 测试getUserTenants API（修复后）
async function testGetUserTenantsAfter() {
  console.log('\n🧪 测试getUserTenants API（修复后 - 带请求体）...')
  
  const testToken = '848b2618754e44be9b98d7fa55996f0c'
  
  try {
    console.log('📡 发送getUserTenants请求（修复后）...')
    console.log('- URL:', `${testApiConfig.baseURL}/sso/v1/getUserTenants`)
    console.log('- Token:', testToken.substring(0, 8) + '...')
    console.log('- Headers:', {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': testApiConfig.headers.appid,
      'secretkey': testApiConfig.headers.secretkey,
      'accesstoken': testToken
    })
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
      body: JSON.stringify({ accessToken: testToken })
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
console.log('📋 测试1: verifyToken API')
const verifyResult = await testVerifyToken()

console.log('\n📋 测试2: getUserTenants API（修复前）')
const getUserTenantsBeforeResult = await testGetUserTenantsBefore()

console.log('\n📋 测试3: getUserTenants API（修复后）')
const getUserTenantsAfterResult = await testGetUserTenantsAfter()

console.log('\n🎉 测试完成！')
console.log('总结:')
console.log('- verifyToken API:', verifyResult.success ? '✅ 成功' : '❌ 失败')
console.log('- getUserTenants API（修复前）:', getUserTenantsBeforeResult.success ? '✅ 成功' : '❌ 失败')
console.log('- getUserTenants API（修复后）:', getUserTenantsAfterResult.success ? '✅ 成功' : '❌ 失败')

if (verifyResult.success && getUserTenantsAfterResult.success) {
  console.log('\n✅ 修复验证成功！')
  console.log('- verifyToken和getUserTenants API调用方式一致')
  console.log('- 两个API都传递相同的accessToken参数')
  console.log('- API调用一致性得到保证')
} else {
  console.log('\n❌ 修复验证失败！')
  console.log('- 需要进一步检查API调用问题')
  console.log('- verifyToken错误:', verifyResult.error)
  console.log('- getUserTenants错误:', getUserTenantsAfterResult.error)
} 