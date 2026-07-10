// 测试token保存和获取流程
console.log('🔧 测试token保存和获取流程...')

// 模拟用户信息
const mockUserInfo = {
  userId: '751fc4b0-81b4-4fe2-940b-ac18d7bc3439',
  tenantId: '0e391fd7-1033-4f09-88c0-187582fee462',
  userName: '周亮',
  accessToken: '848b2618754e44be9b98d7fa55996f0c'
}

// 测试保存用户信息
function testSaveUserInfo(userInfo) {
  console.log('💾 开始保存用户信息:', {
    userName: userInfo.userName,
    tenantId: userInfo.tenantId,
    accessToken: userInfo.accessToken.substring(0, 8) + '...'
  })
  
  try {
    // 保存到localStorage和sessionStorage
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
    localStorage.setItem('accessToken', userInfo.accessToken)
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo))
    sessionStorage.setItem('accessToken', userInfo.accessToken)
    
    // 验证保存是否成功
    const savedSessionToken = sessionStorage.getItem('accessToken')
    const savedLocalToken = localStorage.getItem('accessToken')
    const savedSessionUserInfo = sessionStorage.getItem('userInfo')
    const savedLocalUserInfo = localStorage.getItem('userInfo')
    
    console.log('✅ 保存完成，验证结果:')
    console.log('- sessionToken:', savedSessionToken ? savedSessionToken.substring(0, 8) + '...' : 'null')
    console.log('- localToken:', savedLocalToken ? savedLocalToken.substring(0, 8) + '...' : 'null')
    console.log('- sessionUserInfo:', savedSessionUserInfo ? '已保存' : 'null')
    console.log('- localUserInfo:', savedLocalUserInfo ? '已保存' : 'null')
    
    return {
      success: true,
      sessionToken: savedSessionToken,
      localToken: savedLocalToken
    }
  } catch (error) {
    console.error('❌ 保存失败:', error)
    return { success: false, error: error.message }
  }
}

// 测试获取用户信息
function testGetUserInfo() {
  console.log('🔍 开始获取用户信息...')
  
  try {
    const sessionToken = sessionStorage.getItem('accessToken')
    const localToken = localStorage.getItem('accessToken')
    const sessionUserInfo = sessionStorage.getItem('userInfo')
    const localUserInfo = localStorage.getItem('userInfo')
    
    console.log('📥 获取结果:')
    console.log('- sessionToken:', sessionToken ? sessionToken.substring(0, 8) + '...' : 'null')
    console.log('- localToken:', localToken ? localToken.substring(0, 8) + '...' : 'null')
    console.log('- sessionUserInfo:', sessionUserInfo ? '已获取' : 'null')
    console.log('- localUserInfo:', localUserInfo ? '已获取' : 'null')
    
    if (sessionUserInfo) {
      const userInfo = JSON.parse(sessionUserInfo)
      console.log('- 用户信息:', {
        userName: userInfo.userName,
        tenantId: userInfo.tenantId,
        accessToken: userInfo.accessToken.substring(0, 8) + '...'
      })
    }
    
    return {
      success: true,
      sessionToken,
      localToken,
      sessionUserInfo: sessionUserInfo ? JSON.parse(sessionUserInfo) : null,
      localUserInfo: localUserInfo ? JSON.parse(localUserInfo) : null
    }
  } catch (error) {
    console.error('❌ 获取失败:', error)
    return { success: false, error: error.message }
  }
}

// 测试清除token
function testClearTokens() {
  console.log('🧹 开始清除token...')
  
  try {
    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
    localStorage.removeItem('accessToken')
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    
    console.log('✅ token已清除')
    
    // 验证清除结果
    const sessionToken = sessionStorage.getItem('accessToken')
    const localToken = localStorage.getItem('accessToken')
    
    console.log('🔍 清除后验证:')
    console.log('- sessionToken:', sessionToken ? sessionToken.substring(0, 8) + '...' : 'null')
    console.log('- localToken:', localToken ? localToken.substring(0, 8) + '...' : 'null')
    
    return {
      success: true,
      sessionToken: null,
      localToken: null
    }
  } catch (error) {
    console.error('❌ 清除失败:', error)
    return { success: false, error: error.message }
  }
}

// 运行测试
console.log('\n📋 测试1: 保存用户信息')
const saveResult = testSaveUserInfo(mockUserInfo)

console.log('\n📋 测试2: 获取用户信息')
const getResult = testGetUserInfo()

console.log('\n📋 测试3: 清除token')
const clearResult = testClearTokens()

console.log('\n📋 测试4: 再次获取用户信息（应该为空）')
const getResultAfterClear = testGetUserInfo()

console.log('\n🎉 测试完成！')
console.log('总结:')
console.log('- 保存测试:', saveResult.success ? '✅ 成功' : '❌ 失败')
console.log('- 获取测试:', getResult.success ? '✅ 成功' : '❌ 失败')
console.log('- 清除测试:', clearResult.success ? '✅ 成功' : '❌ 失败')
console.log('- 清除后获取:', getResultAfterClear.success ? '✅ 成功' : '❌ 失败') 