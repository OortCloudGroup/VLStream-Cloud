// 测试VLStream-server API（vls-server服务名）
async function testVLSServerAPIs() {
  const currentToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
  
  console.log('🔍 测试VLStream-server API（vls-server服务名）')
  console.log('当前token:', currentToken)
  console.log('')
  
  const testAPIs = [
    {
      name: 'VLStream设备列表API（直接访问）',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/device/page',
      method: 'GET',
      params: { current: 1, size: 10 }
    },
    {
      name: 'VLStream设备列表API（带api前缀）',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/api/device/page',
      method: 'GET',
      params: { current: 1, size: 10 }
    },
    {
      name: 'VLStream用户同步API',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/api/user/sync',
      method: 'POST',
      data: {
        userName: '周亮',
        userId: '751fc4b0-81b4-4fe2-940b-ac18d7bc3439',
        tenantId: '0e391fd7-1033-4f09-88c0-187582fee462',
        accessToken: currentToken
      }
    },
    {
      name: 'VLStream租户列表API',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/auth/getTenantList',
      method: 'POST',
      data: { accessToken: currentToken }
    },
    {
      name: 'VLStream健康检查',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/auth/health',
      method: 'GET'
    },
    {
      name: 'VLStream根路径测试',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/',
      method: 'GET'
    }
  ]
  
  for (const api of testAPIs) {
    console.log(`\n=== 测试: ${api.name} ===`)
    console.log('URL:', api.url)
    console.log('Method:', api.method)
    
    const headers = {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': '6551b0147c4649a894e86bf8de248da4',
      'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
      'accesstoken': currentToken
    }
    
    console.log('Headers:', headers)
    
    try {
      let response
      
      if (api.method === 'GET') {
        const url = new URL(api.url)
        if (api.params) {
          Object.keys(api.params).forEach(key => {
            url.searchParams.append(key, api.params[key])
          })
        }
        response = await fetch(url.toString(), {
          method: 'GET',
          headers: headers
        })
      } else {
        response = await fetch(api.url, {
          method: api.method,
          headers: headers,
          body: JSON.stringify(api.data || {})
        })
      }
      
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
          console.log('可能原因:')
          console.log('1. VLStream-server服务未启动')
          console.log('2. 网关路由配置问题')
          console.log('3. 服务注册问题')
        } else if (response.status === 400) {
          console.log('🔍 错误分析: 请求参数错误')
          console.log('可能原因:')
          console.log('1. 认证拦截器配置问题')
          console.log('2. appid/secretkey不匹配')
          console.log('3. token验证失败')
        } else if (response.status === 404) {
          console.log('🔍 错误分析: 路径不存在')
          console.log('可能原因:')
          console.log('1. API路径配置错误')
          console.log('2. 控制器映射问题')
        }
      }
      
    } catch (error) {
      console.log(`❌ 请求异常: ${error.message}`)
      if (error.message.includes('Failed to fetch')) {
        console.log('🔍 错误分析: 网络连接失败')
        console.log('可能原因:')
        console.log('1. 服务未启动')
        console.log('2. 端口被占用')
        console.log('3. 防火墙阻止')
      }
    }
  }
  
  console.log('\n📝 测试总结:')
  console.log('- 如果所有API都返回503错误，说明VLStream-server服务未启动或网关路由有问题')
  console.log('- 如果部分API返回4004错误，说明需要检查认证拦截器配置')
  console.log('- 如果路径带/api前缀的API失败，说明路径映射配置有问题')
  console.log('- 如果健康检查成功，说明服务已启动，问题在API路径或认证')
}

// 运行测试
testVLSServerAPIs().catch(console.error) 