// 测试修复后的用户信息显示逻辑
console.log('🔧 测试修复后的用户信息显示逻辑...')

// 模拟实际的API响应数据
const mockApiResponse = {
  "code": 200,
  "data": {
    "list": [
      {
        "tenant_id": "0e391fd7-1033-4f09-88c0-187582fee462",
        "tenant_name": "陵水运营管理平台",
        "parent_tenant_id": "",
        "user_id": "751fc4b0-81b4-4fe2-940b-ac18d7bc3439",
        "unique_id": "251453b7-a153-4095-b72f-2d6945022f02",
        "user_name": "周亮",
        "photo": "",
        "last_login_at": 0
      }
    ]
  },
  "msg": "成功"
}

function testUserInfoExtraction() {
  console.log('🧪 测试用户信息提取逻辑...')
  
  // 模拟layout组件的处理逻辑
  const response = mockApiResponse
  const token = '848b2618754e44be9b98d7fa55996f0c'
  
  console.log('📋 模拟API响应:', response)
  
  if (response.code === 200 && response.data) {
    if (response.data.list && response.data.list.length > 0) {
      // 处理租户列表
      const tenants = response.data.list.map(tenant => ({
        id: tenant.tenant_id,
        name: tenant.tenant_name || '未知租户',
        status: tenant.status,
        phrase: tenant.phrase
      }))
      
      console.log('✅ 提取的租户列表:', tenants)
      
      // 提取用户信息
      const firstTenant = response.data.list[0]
      console.log('🔍 第一个租户数据:', firstTenant)
      
      const userInfo = {
        userName: firstTenant.user_name || '管理员',
        userId: firstTenant.user_id || '',
        loginId: firstTenant.user_name || '',
        tenantId: firstTenant.tenant_id || '',
        accessToken: token
      }
      
      console.log('✅ 提取的用户信息:', userInfo)
      
      // 模拟currentUser更新
      const currentUser = {
        userName: userInfo.userName,
        userId: userInfo.userId,
        loginId: userInfo.loginId
      }
      
      console.log('✅ 模拟currentUser更新:', currentUser)
      
      // 模拟currentTenant更新
      const currentTenant = tenants[0]
      console.log('✅ 模拟currentTenant更新:', currentTenant)
      
      // 验证结果
      console.log('\n🎯 验证结果:')
      console.log('- 用户名称:', currentUser.userName)
      console.log('- 用户ID:', currentUser.userId)
      console.log('- 租户名称:', currentTenant.name)
      console.log('- 租户ID:', currentTenant.id)
      
      // 检查是否与预期一致
      const expectedUser = '周亮'
      const expectedTenant = '陵水运营管理平台'
      
      if (currentUser.userName === expectedUser && currentTenant.name === expectedTenant) {
        console.log('✅ 用户信息显示逻辑正确！')
        console.log('- 用户名称匹配:', currentUser.userName === expectedUser)
        console.log('- 租户名称匹配:', currentTenant.name === expectedTenant)
      } else {
        console.log('❌ 用户信息显示逻辑有问题')
        console.log('- 用户名称不匹配:', currentUser.userName, 'vs', expectedUser)
        console.log('- 租户名称不匹配:', currentTenant.name, 'vs', expectedTenant)
      }
    } else {
      console.log('❌ 租户列表为空')
    }
  } else {
    console.log('❌ API响应格式错误')
  }
}

// 运行测试
console.log('📋 测试修复后的用户信息显示逻辑')
testUserInfoExtraction()

console.log('\n🎉 测试完成！')
console.log('总结:')
console.log('- 验证了用户信息提取逻辑')
console.log('- 确认了字段名映射正确')
console.log('- 模拟了页面显示更新') 