// 测试直接访问VLStream-server
async function testVLStreamDirect() {
  const currentToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
  
  console.log('🔍 测试直接访问VLStream-server')
  console.log('当前token:', currentToken)
  console.log('')
  
  const testAPIs = [
    {
      name: 'VLStream设备列表API（直接访问）',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vlstream-server/device/page',
      method: 'GET',
      params: { current: 1, size: 10 }
    },
    {
      name: 'VLStream用户同步API（直接访问）',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vlstream-server/api/user/sync',
      method: 'POST',
      data: {
        userName: '周亮',
        userId: '751fc4b0-81b4-4fe2-940b-ac18d7bc3439',
        tenantId: '0e391fd7-1033-4f09-88c0-187582fee462',
        accessToken: currentToken
      }
    },
    {
      name: 'VLStream租户列表API（直接访问）',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vlstream-server/auth/getTenantList',
      method: 'POST',
      data: { accessToken: currentToken }
    },
    {
      name: 'VLStream健康检查（直接访问）',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vlstream-server/auth/health',
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
      }
      
    } catch (error) {
      console.log(`❌ 请求异常: ${error.message}`)
    }
  }
  
  console.log('\n📝 测试总结:')
  console.log('- 如果直接访问成功，说明网关路由配置有问题')
  console.log('- 如果直接访问也失败，说明VLStream-server服务未启动')
}

// 运行测试
testVLStreamDirect().catch(console.error) 