// 测试后端token验证逻辑
async function testBackendTokenValidation() {
  const currentToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
  
  console.log('🔍 测试后端token验证逻辑')
  console.log('当前token:', currentToken)
  console.log('')
  
  const testAPIs = [
    {
      name: '设备列表API',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/api/device/page',
      method: 'GET',
      params: { current: 1, size: 10 }
    },
    {
      name: '用户同步API',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/api/user/sync',
      method: 'POST',
      data: {
        userName: '测试用户',
        userId: 'test-user-id',
        tenantId: 'test-tenant-id'
      }
    },
    {
      name: '租户列表API',
      url: 'http://oort.oortcloudsmart.com:21410/bus/vls-server/auth/getTenantList',
      method: 'GET'
    },
    {
      name: '统一用户中心verifyToken',
      url: 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso/sso/v1/verifyToken',
      method: 'POST',
      data: { accessToken: currentToken }
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
          console.log('1. 后端appid/secretkey配置与前端不一致')
          console.log('2. 后端验证算法有问题')
          console.log('3. token格式或签名验证失败')
        }
      }
      
    } catch (error) {
      console.log(`❌ 请求异常: ${error.message}`)
    }
  }
  
  console.log('\n📝 测试总结:')
  console.log('- 如果所有API都返回4004错误，说明后端配置有问题')
  console.log('- 如果只有部分API返回4004错误，说明特定API的验证逻辑有问题')
  console.log('- 如果统一用户中心API正常，说明token本身是有效的')
}

// 运行测试
testBackendTokenValidation().catch(console.error) 