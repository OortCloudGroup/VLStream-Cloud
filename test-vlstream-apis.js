// 测试VLStream-server的API
async function testVLStreamAPIs() {
  const currentToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
  
  console.log('🔍 测试VLStream-server API')
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
        
        // 分析错误类型
        if (errorData.code === 4004) {
          console.log('🔍 错误分析: accessToken无效.校验不通过')
          console.log('可能原因:')
          console.log('1. VLStream-server没有全局认证拦截器')
          console.log('2. 需要手动在每个API中验证token')
          console.log('3. 路径映射问题')
        } else if (errorData.code === 503) {
          console.log('🔍 错误分析: 服务不可用')
          console.log('可能原因:')
          console.log('1. VLStream-server服务未启动')
          console.log('2. 网关路由配置问题')
          console.log('3. 服务注册问题')
        }
      }
      
    } catch (error) {
      console.log(`❌ 请求异常: ${error.message}`)
    }
  }
  
  console.log('\n📝 测试总结:')
  console.log('- 如果所有API都返回503错误，说明VLStream-server服务未启动或网关路由有问题')
  console.log('- 如果部分API返回4004错误，说明需要添加全局认证拦截器')
  console.log('- 如果路径带/api前缀的API失败，说明路径映射配置有问题')
}

// 运行测试
testVLStreamAPIs().catch(console.error) 