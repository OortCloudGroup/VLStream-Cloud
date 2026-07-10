// 测试个人设置页面修复效果
console.log('🔧 测试个人设置页面修复效果...')

// 等待页面加载完成
setTimeout(() => {
  console.log('📋 开始测试个人设置页面修复效果')
  
  // 检查是否在个人设置页面
  const isUserProfilePage = window.location.pathname.includes('/user-profile') || 
                           document.querySelector('.user-profile-container')
  
  if (isUserProfilePage) {
    console.log('✅ 当前在个人设置页面')
    
    // 检查控制台日志
    console.log('📋 检查控制台日志...')
    console.log('📋 应该看到以下日志:')
    console.log('- 🚀 开始加载用户信息...')
    console.log('- ✅ 使用token: 848b2618...')
    console.log('- 📥 getUserInfo响应: {status: 200, data: {code: 200, data: {...}}}')
    console.log('- 📋 解析到的用户信息:')
    console.log('  - 用户名称: 周亮')
    console.log('  - 用户ID: 751fc4b0-81b4-4fe2-940b-ac18d7bc3439')
    console.log('  - 登录ID: 周亮')
    console.log('  - 租户ID: 0e391fd7-1033-4f09-88c0-187582fee462')
    console.log('- ✅ 用户信息已更新: {...}')
    console.log('- 🔄 强制触发Vue响应式更新')
    console.log('- 📋 页面用户名称元素: 周亮')
    console.log('- 📋 页面登录账号输入框: 周亮')
    
    // 检查页面显示
    const userNameElement = document.querySelector('.user-name')
    if (userNameElement) {
      const userName = userNameElement.textContent.trim()
      console.log('📋 页面显示的用户名称:', userName)
      
      if (userName === '周亮') {
        console.log('✅ 用户名称显示正确')
      } else if (userName === '未登录') {
        console.log('❌ 用户名称仍显示"未登录"，可能需要刷新页面')
      } else {
        console.log('❓ 用户名称显示未知状态:', userName)
      }
    } else {
      console.log('❌ 未找到用户名称元素')
    }
    
    // 检查表单字段
    const formInputs = document.querySelectorAll('.user-form .el-input__inner')
    console.log('📋 表单字段数量:', formInputs.length)
    
    if (formInputs.length > 0) {
      console.log('✅ 表单字段已加载')
      
      // 检查登录账号字段
      const loginIdInput = Array.from(formInputs).find(input => 
        input.closest('.el-form-item')?.querySelector('label')?.textContent.includes('登录账号')
      )
      
      if (loginIdInput) {
        console.log('📋 登录账号字段值:', loginIdInput.value)
        if (loginIdInput.value === '周亮') {
          console.log('✅ 登录账号字段显示正确')
        } else if (loginIdInput.value === '') {
          console.log('❌ 登录账号字段为空，可能需要刷新页面')
        } else {
          console.log('❓ 登录账号字段显示未知状态:', loginIdInput.value)
        }
      }
      
      // 检查用户姓名字段
      const userNameInput = Array.from(formInputs).find(input => 
        input.closest('.el-form-item')?.querySelector('label')?.textContent.includes('用户姓名')
      )
      
      if (userNameInput) {
        console.log('📋 用户姓名字段值:', userNameInput.value)
        if (userNameInput.value === '周亮') {
          console.log('✅ 用户姓名字段显示正确')
        } else if (userNameInput.value === '') {
          console.log('❌ 用户姓名字段为空，可能需要刷新页面')
        } else {
          console.log('❓ 用户姓名字段显示未知状态:', userNameInput.value)
        }
      }
    }
    
    // 提供刷新建议
    if (document.querySelector('.user-name')?.textContent.trim() === '未登录') {
      console.log('💡 建议: 请刷新页面重新加载用户信息')
    }
    
  } else {
    console.log('❌ 当前不在个人设置页面')
    console.log('💡 请导航到个人设置页面进行测试')
  }
  
}, 3000)

console.log('🔧 测试脚本已加载，3秒后开始检查...') 