// 强制更新页面显示
console.log('🔧 强制更新页面显示...')

// 强制调用getUserTenants API并更新页面显示
async function forceUpdateDisplay() {
  console.log('🧪 强制调用getUserTenants API...')
  
  try {
    // 获取token
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    
    const token = urlToken || sessionToken || localToken
    
    console.log('📋 Token信息:')
    console.log('- URL token:', urlToken ? urlToken.substring(0, 8) + '...' : 'null')
    console.log('- Session token:', sessionToken ? sessionToken.substring(0, 8) + '...' : 'null')
    console.log('- Local token:', localToken ? localToken.substring(0, 8) + '...' : 'null')
    console.log('- 最终token:', token ? token.substring(0, 8) + '...' : 'null')
    
    if (!token) {
      console.log('❌ 没有找到有效的token')
      return
    }
    
    // 调用getUserTenants API
    console.log('📡 调用getUserTenants API...')
    
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
      
      // 强制更新页面显示
      if (result.code === 200 && result.data && result.data.list && result.data.list.length > 0) {
        const firstTenant = result.data.list[0]
        
        console.log('📋 用户和租户信息:')
        console.log('- 用户名称:', firstTenant.user_name)
        console.log('- 租户名称:', firstTenant.tenant_name)
        console.log('- 用户ID:', firstTenant.user_id)
        console.log('- 租户ID:', firstTenant.tenant_id)
        
        // 强制更新租户显示
        const tenantElement = document.querySelector('.tenant-info')
        if (tenantElement) {
          // 清空现有内容
          tenantElement.innerHTML = ''
          
          // 添加图标
          const icon = document.createElement('i')
          icon.className = 'el-icon'
          icon.innerHTML = '<svg data-v-1dee96d9="" class="el-icon" width="1em" height="1em" viewBox="0 0 1024 1024" fill="currentColor" aria-hidden="true"><path d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm0 820c-205.4 0-372-166.6-372-372s166.6-372 372-372 372 166.6 372 372-166.6 372-372 372z"></path><path d="M464 688a48 48 0 1 0 96 0 48 48 0 1 0-96 0zm24-112h48c4.4 0 8-3.6 8-8V296c0-4.4-3.6-8-8-8h-48c-4.4 0-8 3.6-8 8v272c0 4.4 3.6 8 8 8z"></path></svg>'
          
          // 添加文本
          const text = document.createTextNode(` ${firstTenant.tenant_name || '未知租户'} `)
          
          // 添加下拉箭头
          const arrow = document.createElement('i')
          arrow.className = 'el-icon'
          arrow.innerHTML = '<svg data-v-1dee96d9="" class="el-icon" width="1em" height="1em" viewBox="0 0 1024 1024" fill="currentColor" aria-hidden="true"><path d="M840.4 300H183.6c-19.7 0-30.7 20.8-18.5 35l328.4 380.8c9.4 10.9 27.5 10.9 37 0L858.9 335c12.2-14.2 1.2-35-18.5-35z"></path></svg>'
          
          // 组装元素
          tenantElement.appendChild(icon)
          tenantElement.appendChild(text)
          tenantElement.appendChild(arrow)
          
          console.log('✅ 强制更新租户显示成功:', firstTenant.tenant_name)
        } else {
          console.log('❌ 没有找到租户信息元素')
        }
        
        // 强制更新用户显示
        const userElement = document.querySelector('.user-info')
        if (userElement) {
          // 清空现有内容
          userElement.innerHTML = ''
          
          // 添加图标
          const icon = document.createElement('i')
          icon.className = 'el-icon'
          icon.innerHTML = '<svg data-v-1dee96d9="" class="el-icon" width="1em" height="1em" viewBox="0 0 1024 1024" fill="currentColor" aria-hidden="true"><path d="M858.5 763.6a374 374 0 0 0-80.6-119.5 375.63 375.63 0 0 0-119.5-80.6c-.4-.2-.8-.3-1.2-.5C719.5 518 760 444.7 760 362c0-137-111-248-248-248S264 225 264 362c0 82.7 40.5 156 102.8 201.1-.4.2-.7.3-1.1.5-44.8 18.9-85 46-119.5 80.6a375.63 375.63 0 0 0-80.6 119.5A371.7 371.7 0 0 0 136 901.8a8 8 0 0 0 8 8.2h60c4.4 0 7.9-3.5 8-7.8 2-77.2 33-149.5 87.8-204.3 56.7-56.7 132-87.9 212.2-87.9s155.5 31.2 212.2 87.9C779 752.7 810 825 812 902.2c.1 4.3 3.6 7.8 8 7.8h60a8 8 0 0 0 8-8.2c-1-47.8-10.9-94.3-29.5-138.2zM512 534c-45.9 0-89.1-17.9-121.6-50.4S340 407.9 340 362c0-45.9 17.9-89.1 50.4-121.6S466.1 190 512 190s89.1 17.9 121.6 50.4S684 316.1 684 362c0 45.9-17.9 89.1-50.4 121.6S557.9 534 512 534z"></path></svg>'
          
          // 添加文本
          const text = document.createTextNode(` ${firstTenant.user_name || '管理员'} `)
          
          // 添加下拉箭头
          const arrow = document.createElement('i')
          arrow.className = 'el-icon'
          arrow.innerHTML = '<svg data-v-1dee96d9="" class="el-icon" width="1em" height="1em" viewBox="0 0 1024 1024" fill="currentColor" aria-hidden="true"><path d="M840.4 300H183.6c-19.7 0-30.7 20.8-18.5 35l328.4 380.8c9.4 10.9 27.5 10.9 37 0L858.9 335c12.2-14.2 1.2-35-18.5-35z"></path></svg>'
          
          // 组装元素
          userElement.appendChild(icon)
          userElement.appendChild(text)
          userElement.appendChild(arrow)
          
          console.log('✅ 强制更新用户显示成功:', firstTenant.user_name)
        } else {
          console.log('❌ 没有找到用户信息元素')
        }
        
        // 保存到localStorage以便后续使用
        localStorage.setItem('userInfo', JSON.stringify({
          userName: firstTenant.user_name,
          userId: firstTenant.user_id,
          tenantName: firstTenant.tenant_name,
          tenantId: firstTenant.tenant_id,
          accessToken: token
        }))
        
        console.log('✅ 用户信息已保存到localStorage')
        
      } else {
        console.log('❌ API返回数据格式不正确:', result)
      }
    } else {
      const errorText = await response.text()
      console.log('❌ API调用失败:', errorText)
    }
  } catch (error) {
    console.error('❌ 强制更新失败:', error)
  }
}

// 延迟执行
setTimeout(() => {
  console.log('📋 开始强制更新显示')
  forceUpdateDisplay()
}, 1000)

console.log('🔧 强制更新工具已加载，1秒后开始执行...') 