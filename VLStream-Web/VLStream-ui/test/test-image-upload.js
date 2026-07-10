// 测试图片上传API
async function testImageUpload() {
  const currentToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
  
  console.log('🔍 测试图片上传API')
  console.log('当前token:', currentToken)
  console.log('')
  
  const testAPIs = [
    {
      name: '图片上传API（直接路径）',
      url: 'http://192.168.60.77:32557/image/upload',
      method: 'POST'
    },
    {
      name: '图片上传API（带/api前缀）',
      url: 'http://192.168.60.77:32557/api/image/upload',
      method: 'POST'
    },
    {
      name: '图片存在检查API',
      url: 'http://192.168.60.77:32557/image/exists',
      method: 'GET',
      params: { fileName: 'test.jpg' }
    },
    {
      name: '健康检查API',
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
        // 对于POST请求，只测试连接，不实际上传文件
        response = await fetch(api.url, {
          method: 'OPTIONS',
          headers: headers
        })
      }
      
      console.log(`响应状态: ${response.status}`)
      
      if (response.ok) {
        console.log('✅ 请求成功！')
        if (api.method === 'GET') {
          const data = await response.json()
          console.log('响应数据:', data)
        }
      } else {
        console.log('❌ 请求失败！')
        if (response.status === 404) {
          console.log('🔍 错误分析: 路径不存在')
          console.log('可能原因: API前缀配置问题')
        } else if (response.status === 405) {
          console.log('🔍 错误分析: 方法不允许')
          console.log('可能原因: 服务正常，但请求方法不对')
        }
      }
      
    } catch (error) {
      console.log(`❌ 请求异常: ${error.message}`)
    }
  }
  
  console.log('\n📝 测试总结:')
  console.log('- 如果直接路径成功，说明图片上传控制器配置正确')
  console.log('- 如果带/api前缀失败，说明前缀排除配置正确')
  console.log('- 如果都不成功，说明服务未启动或配置有问题')
}

// 运行测试
testImageUpload().catch(console.error) 