// 测试实际应用场景
console.log('🔧 测试实际应用场景...')

// 模拟实际应用中的情况
async function testActualAppScenario() {
  console.log('🧪 模拟实际应用场景...')
  
  const testToken = '848b2618754e44be9b98d7fa55996f0c'
  
  // 模拟URL中的token
  const originalSearch = window.location.search
  const mockUrl = `http://localhost:3000/workspace?accessToken=${testToken}&fromWhere=desktopHome`
  
  console.log('📋 场景1: URL中有token')
  console.log('- 模拟URL:', mockUrl)
  console.log('- URL参数:', new URLSearchParams(mockUrl.split('?')[1]))
  
  // 模拟getUserTenants的token获取逻辑
  const urlParams = new URLSearchParams(mockUrl.split('?')[1])
  const urlToken = urlParams.get('accessToken') || urlParams.get('token')
  console.log('- 获取到的URL token:', urlToken ? urlToken.substring(0, 8) + '...' : 'null')
  
  // 模拟API调用
  const testApiConfig = {
    baseURL: 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso',
    headers: {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': '6551b0147c4649a894e86bf8de248da4',
      'secretkey': '58f9eeefc65f4b318204ba21f39a8861'
    }
  }
  
  try {
    console.log('\n📡 发送getUserTenants请求（模拟实际应用）...')
    console.log('- URL:', `${testApiConfig.baseURL}/sso/v1/getUserTenants`)
    console.log('- Token:', urlToken ? urlToken.substring(0, 8) + '...' : 'null')
    console.log('- Headers:', {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': testApiConfig.headers.appid,
      'secretkey': testApiConfig.headers.secretkey,
      'accesstoken': urlToken
    })
    console.log('- Body:', { accessToken: urlToken })
    
    const response = await fetch(`${testApiConfig.baseURL}/sso/v1/getUserTenants`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'requesttype': 'app',
        'appid': testApiConfig.headers.appid,
        'secretkey': testApiConfig.headers.secretkey,
        'accesstoken': urlToken
      },
      body: JSON.stringify({ accessToken: urlToken })
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

// 测试URL token被清除的情况
async function testUrlTokenCleared() {
  console.log('\n📋 场景2: URL中的token被清除')
  console.log('- 模拟URL:', 'http://localhost:3000/workspace?fromWhere=desktopHome')
  console.log('- URL参数:', new URLSearchParams('fromWhere=desktopHome'))
  
  // 模拟URL中的token被清除
  const urlParams = new URLSearchParams('fromWhere=desktopHome')
  const urlToken = urlParams.get('accessToken') || urlParams.get('token')
  console.log('- 获取到的URL token:', urlToken ? urlToken.substring(0, 8) + '...' : 'null')
  
  // 模拟从sessionStorage获取token
  const testToken = '848b2618754e44be9b98d7fa55996f0c'
  console.log('- 模拟sessionStorage token:', testToken.substring(0, 8) + '...')
  
  const testApiConfig = {
    baseURL: 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso',
    headers: {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': '6551b0147c4649a894e86bf8de248da4',
      'secretkey': '58f9eeefc65f4b318204ba21f39a8861'
    }
  }
  
  try {
    console.log('\n📡 发送getUserTenants请求（URL token被清除）...')
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
console.log('📋 测试1: URL中有token的场景')
const result1 = await testActualAppScenario()

console.log('\n📋 测试2: URL token被清除的场景')
const result2 = await testUrlTokenCleared()

console.log('\n🎉 测试完成！')
console.log('总结:')
console.log('- URL中有token:', result1.success ? '✅ 成功' : '❌ 失败')
console.log('- URL token被清除:', result2.success ? '✅ 成功' : '❌ 失败')

if (result1.success && result2.success) {
  console.log('\n✅ 实际应用场景测试成功！')
  console.log('- 两种场景都能正常工作')
  console.log('- token获取逻辑正确')
} else {
  console.log('\n❌ 实际应用场景测试失败！')
  console.log('- 需要进一步检查问题')
  console.log('- 场景1错误:', result1.error)
  console.log('- 场景2错误:', result2.error)
} 