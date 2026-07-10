// 测试API前缀配置
async function testAPIPrefix() {
  const currentToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
  
  console.log('🔍 测试API前缀配置')
  console.log('当前token:', currentToken)
  console.log('')
  
  const testAPIs = [
    {
      name: '设备列表API（带/api前缀）',
      url: 'http://192.168.60.77:32557/api/device/page',
      method: 'GET',
      params: { current: 1, size: 10 }
    },
    {
      name: '设备列表API（不带/api前缀）',
      url: 'http://192.168.60.77:32557/device/page',
      method: 'GET',
      params: { current: 1, size: 10 }
    },
    {
      name: '用户同步API（带/api前缀）',
      url: 'http://192.168.60.77:32557/api/api/user/sync',
      method: 'POST',
      data: {
        userName: '周亮',
        userId: '751fc4b0-81b4-4fe2-940b-ac18d7bc3439',
        tenantId: '0e391fd7-1033-4f09-88c0-187582fee462',
        accessToken: currentToken
      }
    },
    {
      name: '用户同步API（不带/api前缀）',
      url: 'http://192.168.60.77:32557/api/user/sync',
      method: 'POST',
      data: {
        userName: '周亮',
        userId: '751fc4b0-81b4-4fe2-940b-ac18d7bc3439',
        tenantId: '0e391fd7-1033-4f09-88c0-187582fee462',
        accessToken: currentToken
      }
    },
    {
      name: '租户列表API（带/api前缀）',
      url: 'http://192.168.60.77:32557/api/auth/getTenantList',
      method: 'POST',
      data: { accessToken: currentToken }
    },
    {
      name: '租户列表API（不带/api前缀）',
      url: 'http://192.168.60.77:32557/auth/getTenantList',
      method: 'POST',
      data: { accessToken: currentToken }
    },
    {
      name: '健康检查（带/api前缀）',
      url: 'http://192.168.60.77:32557/api/auth/health',
      method: 'GET'
    },
    {
      name: '健康检查（不带/api前缀）',
      url: 'http://192.168.60.77:32557/auth/health',
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
  console.log('- 如果带/api前缀的API成功，说明全局前缀配置正确')
  console.log('- 如果不带/api前缀的API成功，说明需要修改前端配置')
  console.log('- 如果都不成功，说明服务未启动或认证有问题')
}

// 运行测试
testAPIPrefix().catch(console.error) 