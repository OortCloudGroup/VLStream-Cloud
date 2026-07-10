// 测试网关路由配置
async function testGatewayRouting() {
  const currentToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
  
  console.log('🔍 测试网关路由配置')
  console.log('当前token:', currentToken)
  console.log('')
  
  const testRoutes = [
    {
      name: '直接访问VLStream-server（本地端口）',
      url: 'http://localhost:18080/auth/health',
      method: 'GET',
      skipAuth: true
    },
    {
      name: '网关路由 - vls-server',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/auth/health',
      method: 'GET'
    },
    {
      name: '网关路由 - vlstream-server',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vlstream-server/auth/health',
      method: 'GET'
    },
    {
      name: '网关路由 - 不带服务名',
      url: 'http://oort.oortcloudsmart.com:21410/bus/auth/health',
      method: 'GET'
    },
    {
      name: '网关根路径测试',
      url: 'http://oort.oortcloudsmart.com:21410/bus/',
      method: 'GET',
      skipAuth: true
    }
  ]
  
  for (const route of testRoutes) {
    console.log(`\n=== 测试: ${route.name} ===`)
    console.log('URL:', route.url)
    console.log('Method:', route.method)
    
    const headers = route.skipAuth ? {
      'Content-Type': 'application/json'
    } : {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': '6551b0147c4649a894e86bf8de248da4',
      'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
      'accesstoken': currentToken
    }
    
    console.log('Headers:', headers)
    
    try {
      const response = await fetch(route.url, {
        method: route.method,
        headers: headers
      })
      
      console.log(`响应状态: ${response.status}`)
      
      if (response.ok) {
        const data = await response.json()
        console.log('✅ 请求成功！')
        console.log('响应数据:', data)
      } else {
        const errorData = await response.json()
        console.log('❌ 请求失败！')
        console.log('错误信息:', errorData)
        
        // 错误分析
        if (response.status === 503) {
          console.log('🔍 错误分析: 服务不可用')
          console.log('可能原因: 服务未注册到网关')
        } else if (response.status === 400) {
          console.log('🔍 错误分析: 请求参数错误')
          console.log('可能原因: 网关路由配置问题')
        } else if (errorData.code === 40001) {
          console.log('🔍 错误分析: 网关路由规则问题')
          console.log('可能原因: 服务名不匹配或路由配置错误')
        }
      }
      
    } catch (error) {
      console.log(`❌ 请求异常: ${error.message}`)
      if (error.message.includes('Failed to fetch')) {
        console.log('🔍 错误分析: 网络连接失败')
        console.log('可能原因: 服务未启动或端口被占用')
      }
    }
  }
  
  console.log('\n📝 测试总结:')
  console.log('- 如果直接访问成功但网关访问失败，说明网关路由配置有问题')
  console.log('- 如果所有网关路由都返回40001，说明服务注册有问题')
  console.log('- 如果部分路由成功，说明路由规则配置不一致')
}

// 运行测试
testGatewayRouting().catch(console.error) 