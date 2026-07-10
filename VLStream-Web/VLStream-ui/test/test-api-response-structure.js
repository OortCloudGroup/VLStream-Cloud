// 测试getUserTenants API响应的数据结构
console.log('🔧 测试getUserTenants API响应的数据结构...')

async function testApiResponseStructure() {
  console.log('🧪 测试API响应数据结构...')
  
  const testApiConfig = {
    baseURL: 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso',
    headers: {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': '6551b0147c4649a894e86bf8de248da4',
      'secretkey': '58f9eeefc65f4b318204ba21f39a8861'
    }
  }
  
  const token = '848b2618754e44be9b98d7fa55996f0c'
  
  try {
    console.log('\n📡 发送getUserTenants请求...')
    console.log('- URL:', `${testApiConfig.baseURL}/sso/v1/getUserTenants`)
    console.log('- Token:', token.substring(0, 8) + '...')
    console.log('- Body:', { accessToken: token })
    
    const response = await fetch(`${testApiConfig.baseURL}/sso/v1/getUserTenants`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'requesttype': 'app',
        'appid': testApiConfig.headers.appid,
        'secretkey': testApiConfig.headers.secretkey,
        'accesstoken': token
      },
      body: JSON.stringify({ accessToken: token })
    })
    
    console.log('📥 收到响应:')
    console.log('- Status:', response.status)
    console.log('- StatusText:', response.statusText)
    
    if (response.ok) {
      const result = await response.json()
      console.log('✅ getUserTenants响应成功:', result)
      
      // 详细分析响应结构
      console.log('\n🔍 响应结构分析:')
      console.log('- code:', result.code)
      console.log('- data类型:', typeof result.data)
      console.log('- data:', result.data)
      
      if (result.data && result.data.list) {
        console.log('- list长度:', result.data.list.length)
        console.log('- list类型:', typeof result.data.list)
        
        if (result.data.list.length > 0) {
          const firstItem = result.data.list[0]
          console.log('- 第一个项目:', firstItem)
          console.log('- 第一个项目的所有属性:', Object.keys(firstItem))
          
          // 检查可能的用户信息字段
          const possibleUserFields = [
            'user_name', 'userName', 'user_id', 'userId',
            'tenant_name', 'tenantName', 'tenant_id', 'tenantId',
            'company_name', 'companyName'
          ]
          
          console.log('\n🔍 用户信息字段检查:')
          possibleUserFields.forEach(field => {
            if (firstItem.hasOwnProperty(field)) {
              console.log(`- ${field}:`, firstItem[field])
            }
          })
          
          // 模拟layout组件的处理逻辑
          console.log('\n🧪 模拟layout组件处理逻辑:')
          const userInfo = {
            userName: firstItem.user_name || firstItem.userName || '管理员',
            userId: firstItem.user_id || firstItem.userId || '',
            loginId: firstItem.user_name || firstItem.userName || '',
            tenantId: firstItem.tenant_id || firstItem.tenantId || '',
            accessToken: token
          }
          
          console.log('- 提取的用户信息:', userInfo)
          
          const tenantInfo = {
            id: firstItem.tenant_id || firstItem.tenantId,
            name: firstItem.tenant_name || firstItem.tenantName || firstItem.company_name || firstItem.companyName || '未知租户',
            status: firstItem.status,
            phrase: firstItem.phrase
          }
          
          console.log('- 提取的租户信息:', tenantInfo)
        }
      } else {
        console.log('❌ data.list不存在或为空')
      }
    } else {
      const errorText = await response.text()
      console.log(`❌ getUserTenants HTTP错误: ${response.status}`)
      console.log('- Error:', errorText)
    }
  } catch (error) {
    console.error('❌ getUserTenants请求异常:', error)
  }
}

// 运行测试
console.log('📋 测试getUserTenants API响应数据结构')
await testApiResponseStructure()

console.log('\n🎉 测试完成！')
console.log('总结:')
console.log('- 分析了API响应的完整结构')
console.log('- 检查了用户信息和租户信息的字段')
console.log('- 模拟了layout组件的处理逻辑') 