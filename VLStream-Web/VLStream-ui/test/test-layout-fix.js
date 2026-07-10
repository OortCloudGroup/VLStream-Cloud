// 测试layout组件修复后的getUserTenants调用
console.log('🔧 测试layout组件修复后的getUserTenants调用...')

// 模拟修复后的loadTenantInfo函数
async function testLayoutLoadTenantInfoFixed() {
  console.log('🧪 模拟修复后的loadTenantInfo函数...')
  
  // 模拟不同的URL状态
  const scenarios = [
    {
      name: 'URL中有token',
      url: 'http://localhost:3000/workspace?accessToken=848b2618754e44be9b98d7fa55996f0c&fromWhere=desktopHome',
      sessionToken: null,
      localToken: null
    },
    {
      name: 'URL中无token，sessionStorage有token',
      url: 'http://localhost:3000/workspace?fromWhere=desktopHome',
      sessionToken: '848b2618754e44be9b98d7fa55996f0c',
      localToken: null
    },
    {
      name: 'URL中无token，localStorage有token',
      url: 'http://localhost:3000/workspace?fromWhere=desktopHome',
      sessionToken: null,
      localToken: '848b2618754e44be9b98d7fa55996f0c'
    }
  ]
  
  const testApiConfig = {
    baseURL: 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso',
    headers: {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': '6551b0147c4649a894e86bf8de248da4',
      'secretkey': '58f9eeefc65f4b318204ba21f39a8861'
    }
  }
  
  for (const scenario of scenarios) {
    console.log(`\n📋 测试场景: ${scenario.name}`)
    console.log('- 模拟URL:', scenario.url)
    console.log('- sessionStorage token:', scenario.sessionToken ? scenario.sessionToken.substring(0, 8) + '...' : 'null')
    console.log('- localStorage token:', scenario.localToken ? scenario.localToken.substring(0, 8) + '...' : 'null')
    
    // 模拟修复后的token获取逻辑
    const urlParams = new URLSearchParams(scenario.url.split('?')[1])
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    const sessionToken = scenario.sessionToken
    const localToken = scenario.localToken
    
    console.log('- 获取到的URL token:', urlToken ? urlToken.substring(0, 8) + '...' : 'null')
    
    // 确定使用的token
    let token = urlToken || sessionToken || localToken
    console.log('- 最终使用的token:', token ? token.substring(0, 8) + '...' : 'null')
    
    if (!token) {
      console.log('❌ 没有找到任何token，跳过API调用')
      continue
    }
    
    try {
      console.log('\n📡 发送getUserTenants请求（修复后）...')
      console.log('- URL:', `${testApiConfig.baseURL}/sso/v1/getUserTenants`)
      console.log('- Token:', token.substring(0, 8) + '...')
      console.log('- Headers:', {
        'Content-Type': 'application/json',
        'requesttype': 'app',
        'appid': testApiConfig.headers.appid,
        'secretkey': testApiConfig.headers.secretkey,
        'accesstoken': token
      })
      console.log('- Body:', { accessToken: token }) // 修复：包含accessToken
      
      const response = await fetch(`${testApiConfig.baseURL}/sso/v1/getUserTenants`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'requesttype': 'app',
          'appid': testApiConfig.headers.appid,
          'secretkey': testApiConfig.headers.secretkey,
          'accesstoken': token
        },
        body: JSON.stringify({ accessToken: token }) // 修复：包含accessToken
      })
      
      console.log('📥 收到响应:')
      console.log('- Status:', response.status)
      console.log('- StatusText:', response.statusText)
      
      if (response.ok) {
        const result = await response.json()
        console.log('✅ getUserTenants响应成功:', result)
      } else {
        const errorText = await response.text()
        console.log(`❌ getUserTenants HTTP错误: ${response.status}`)
        console.log('- Error:', errorText)
      }
    } catch (error) {
      console.error('❌ getUserTenants请求异常:', error)
    }
  }
}

// 运行测试
console.log('📋 测试layout组件修复后的getUserTenants调用')
await testLayoutLoadTenantInfoFixed()

console.log('\n🎉 测试完成！')
console.log('总结:')
console.log('- 修复了layout组件中的getUserTenants调用')
console.log('- 现在传递正确的accessToken参数')
console.log('- 应该能解决400错误问题') 