// 测试响应式变量更新
console.log('🔧 测试响应式变量更新...')

// 模拟Vue的ref函数
function ref(initialValue) {
  let value = initialValue
  return {
    get value() {
      return value
    },
    set value(newValue) {
      console.log('🔄 响应式变量更新:', { old: value, new: newValue })
      value = newValue
    }
  }
}

// 模拟layout组件中的响应式变量
const currentUser = ref({
  userName: '管理员',
  userId: '',
  loginId: ''
})

const currentTenant = ref({
  id: '',
  name: '加载中...'
})

console.log('📋 初始状态:')
console.log('- currentUser:', currentUser.value)
console.log('- currentTenant:', currentTenant.value)

// 模拟API响应数据
const mockApiResponse = {
  "code": 200,
  "data": {
    "list": [
      {
        "tenant_id": "0e391fd7-1033-4f09-88c0-187582fee462",
        "tenant_name": "陵水运营管理平台",
        "user_id": "751fc4b0-81b4-4fe2-940b-ac18d7bc3439",
        "user_name": "周亮"
      }
    ]
  }
}

// 模拟loadTenantInfo函数
function simulateLoadTenantInfo() {
  console.log('\n🧪 模拟loadTenantInfo函数...')
  
  const response = mockApiResponse
  
  if (response.code === 200 && response.data) {
    if (response.data.list && response.data.list.length > 0) {
      const firstTenant = response.data.list[0]
      
      // 更新用户信息
      const userInfo = {
        userName: firstTenant.user_name || '管理员',
        userId: firstTenant.user_id || '',
        loginId: firstTenant.user_name || ''
      }
      
      console.log('✅ 提取的用户信息:', userInfo)
      
      // 更新响应式变量
      currentUser.value = userInfo
      console.log('✅ 更新currentUser:', currentUser.value)
      
      // 更新租户信息
      const tenantInfo = {
        id: firstTenant.tenant_id,
        name: firstTenant.tenant_name || '未知租户'
      }
      
      console.log('✅ 提取的租户信息:', tenantInfo)
      
      currentTenant.value = tenantInfo
      console.log('✅ 更新currentTenant:', currentTenant.value)
      
      // 验证更新结果
      console.log('\n🎯 验证更新结果:')
      console.log('- 用户名称:', currentUser.value.userName)
      console.log('- 租户名称:', currentTenant.value.name)
      
      // 检查是否与预期一致
      const expectedUser = '周亮'
      const expectedTenant = '陵水运营管理平台'
      
      if (currentUser.value.userName === expectedUser && currentTenant.value.name === expectedTenant) {
        console.log('✅ 响应式变量更新正确！')
      } else {
        console.log('❌ 响应式变量更新有问题')
        console.log('- 用户名称不匹配:', currentUser.value.userName, 'vs', expectedUser)
        console.log('- 租户名称不匹配:', currentTenant.value.name, 'vs', expectedTenant)
      }
    }
  }
}

// 运行测试
console.log('📋 测试响应式变量更新')
simulateLoadTenantInfo()

console.log('\n🎉 测试完成！')
console.log('总结:')
console.log('- 验证了响应式变量的更新机制')
console.log('- 确认了数据提取和赋值逻辑')
console.log('- 模拟了layout组件的更新流程') 