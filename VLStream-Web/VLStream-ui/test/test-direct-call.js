// 直接调用loadTenantInfo测试
console.log('🔧 直接调用loadTenantInfo测试...')

// 尝试直接调用loadTenantInfo函数
async function directCallLoadTenantInfo() {
  console.log('🧪 尝试直接调用loadTenantInfo...')
  
  try {
    // 检查是否有token
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    
    const token = urlToken || sessionToken || localToken
    
    console.log('📋 Token检查:')
    console.log('- URL token:', urlToken ? urlToken.substring(0, 8) + '...' : 'null')
    console.log('- Session token:', sessionToken ? sessionToken.substring(0, 8) + '...' : 'null')
    console.log('- Local token:', localToken ? localToken.substring(0, 8) + '...' : 'null')
    console.log('- 最终token:', token ? token.substring(0, 8) + '...' : 'null')
    
    if (!token) {
      console.log('❌ 没有找到有效的token')
      return
    }
    
    // 直接调用getUserTenants API
    console.log('📡 直接调用getUserTenants API...')
    
    const response = await fetch('http://oort.oortcloudsmart.com:21410/bus/apaas-sso/sso/v1/getUserTenants', {
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
      
      // 手动更新页面显示
      if (result.code === 200 && result.data && result.data.list && result.data.list.length > 0) {
        const firstTenant = result.data.list[0]
        
        // 更新租户显示
        const tenantElement = document.querySelector('.tenant-info')
        if (tenantElement) {
          tenantElement.textContent = firstTenant.tenant_name || '未知租户'
          console.log('✅ 手动更新租户显示:', firstTenant.tenant_name)
        }
        
        // 更新用户显示
        const userElement = document.querySelector('.user-info')
        if (userElement) {
          userElement.textContent = firstTenant.user_name || '管理员'
          console.log('✅ 手动更新用户显示:', firstTenant.user_name)
        }
      }
    } else {
      const errorText = await response.text()
      console.log('❌ API调用失败:', errorText)
    }
  } catch (error) {
    console.error('❌ 直接调用失败:', error)
  }
}

// 延迟执行
setTimeout(() => {
  console.log('📋 开始直接调用测试')
  directCallLoadTenantInfo()
}, 1000)

console.log('🔧 直接调用测试工具已加载，1秒后开始测试...') 