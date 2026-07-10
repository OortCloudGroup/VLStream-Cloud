// 直接测试getUserInfo API
console.log('🔧 直接测试getUserInfo API...')

async function testGetUserInfoAPI() {
  console.log('🧪 开始测试getUserInfo API...')
  
  try {
    // 获取token
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    
    const token = urlToken || sessionToken || localToken
    
    if (!token) {
      console.log('❌ 没有找到有效的token')
      return
    }
    
    console.log('✅ 使用token:', token.substring(0, 8) + '...')
    
    // 直接调用getUserInfo API
    console.log('📡 调用getUserInfo API...')
    
    const response = await fetch('http://oort.oortcloudsmart.com:21410/bus/apaas-sso/sso/v1/getUserInfo', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'requesttype': 'app',
        'appid': '6551b0147c4649a894e86bf8de248da4',
        'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
        'accesstoken': token
      },
      body: JSON.stringify({ accessToken: token })
    })
    
    console.log('📥 API响应状态:', response.status)
    
    if (response.ok) {
      const result = await response.json()
      console.log('✅ API调用成功:', result)
      
      if (result.code === 200 && result.data) {
        const userData = result.data
        
        console.log('📋 用户详细信息:')
        console.log('- 用户名称:', userData.user_name)
        console.log('- 用户ID:', userData.user_id)
        console.log('- 登录ID:', userData.login_id)
        console.log('- 租户ID:', userData.tenant_id)
        console.log('- 状态:', userData.status)
        console.log('- 用户来源:', userData.form)
        console.log('- 登录类型:', userData.login_type)
        console.log('- 客户端:', userData.client)
        console.log('- 登录时间:', userData.login_time)
        console.log('- 登录IP:', userData.login_ip)
        console.log('- 令牌过期时间:', userData.token_expire_time)
        console.log('- 创建时间:', userData.created_at)
        console.log('- 更新时间:', userData.updated_at)
        
        // 检查必要字段
        const requiredFields = ['user_name', 'user_id', 'login_id', 'tenant_id']
        const missingFields = requiredFields.filter(field => !userData[field])
        
        if (missingFields.length === 0) {
          console.log('✅ 所有必要字段都存在')
        } else {
          console.log('⚠️ 缺少字段:', missingFields)
        }
        
      } else {
        console.log('❌ API返回数据格式不正确:', result)
      }
      
    } else {
      console.log('❌ API调用失败:', response.status, response.statusText)
    }
    
  } catch (error) {
    console.error('❌ 测试getUserInfo API失败:', error)
  }
}

// 延迟执行测试
setTimeout(() => {
  console.log('📋 开始直接测试getUserInfo API')
  testGetUserInfoAPI()
}, 1000)

console.log('🔧 测试脚本已加载，1秒后开始测试...') 