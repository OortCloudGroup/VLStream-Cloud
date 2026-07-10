// 测试axios响应结构处理
console.log('🔧 测试axios响应结构处理...')

// 模拟axios响应结构
const mockAxiosResponse = {
  data: {
    code: 200,
    data: {
      userId: '751fc4b0-81b4-4fe2-940b-ac18d7bc3439',
      tenantId: '0e391fd7-1033-4f09-88c0-187582fee462',
      userName: '周亮',
      accessToken: '848b2618754e44be9b98d7fa55996f0c'
    },
    msg: '成功'
  },
  status: 200,
  statusText: 'OK',
  headers: {},
  config: {}
}

// 模拟直接API响应结构
const mockDirectResponse = {
  code: 200,
  data: {
    userId: '751fc4b0-81b4-4fe2-940b-ac18d7bc3439',
    tenantId: '0e391fd7-1033-4f09-88c0-187582fee462',
    userName: '周亮',
    accessToken: '848b2618754e44be9b98d7fa55996f0c'
  },
  msg: '成功'
}

// 测试响应处理函数
function testResponseProcessing(response) {
  console.log('📥 原始响应:', response)
  
  // 修复后的响应处理逻辑
  const apiResponse = response.data || response
  console.log('🔧 处理后的API响应:', apiResponse)
  
  if (apiResponse && (apiResponse.code === 200 || apiResponse.success === true)) {
    console.log('✅ Token验证成功，用户信息:', apiResponse.data)
    return {
      success: true,
      data: apiResponse.data
    }
  } else {
    console.log('❌ Token验证失败')
    return {
      success: false,
      error: apiResponse
    }
  }
}

// 测试axios响应结构
console.log('\n📋 测试1: axios响应结构')
const result1 = testResponseProcessing(mockAxiosResponse)
console.log('结果:', result1)

// 测试直接API响应结构
console.log('\n📋 测试2: 直接API响应结构')
const result2 = testResponseProcessing(mockDirectResponse)
console.log('结果:', result2)

// 测试失败响应
console.log('\n📋 测试3: 失败响应')
const mockFailureResponse = {
  data: {
    code: 400,
    msg: 'token无效'
  },
  status: 200,
  statusText: 'OK'
}
const result3 = testResponseProcessing(mockFailureResponse)
console.log('结果:', result3)

console.log('\n🎉 测试完成！') 