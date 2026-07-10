// 测试最终修复效果
console.log('🔧 测试最终修复效果...')

// 等待页面加载完成
setTimeout(() => {
  console.log('📋 开始测试最终修复效果')
  
  // 检查Vue组件是否挂载
  const app = document.querySelector('#app')
  if (!app) {
    console.log('❌ Vue应用未找到')
    return
  }
  
  console.log('✅ Vue应用已找到')
  
  // 检查用户信息显示
  const userElement = document.querySelector('.user-info')
  if (userElement) {
    const userText = userElement.textContent.trim()
    console.log('📋 用户信息显示:', userText)
    
    if (userText === '管理员') {
      console.log('❌ 用户信息仍显示默认状态')
    } else {
      console.log('✅ 用户信息已更新:', userText)
    }
  } else {
    console.log('❌ 未找到用户信息元素')
  }
  
  // 检查租户信息显示
  const tenantElement = document.querySelector('.tenant-info')
  if (tenantElement) {
    const tenantText = tenantElement.textContent.trim()
    console.log('📋 租户信息显示:', tenantText)
    
    if (tenantText === '加载中...') {
      console.log('❌ 租户信息仍显示加载状态')
    } else {
      console.log('✅ 租户信息已更新:', tenantText)
    }
  } else {
    console.log('❌ 未找到租户信息元素')
  }
  
  // 检查控制台日志
  console.log('📋 请检查控制台是否有以下日志:')
  console.log('- 🎬 组件开始挂载...')
  console.log('- 🚀 forceLoadUserAndTenantInfo函数开始执行...')
  console.log('- ✅ 更新currentUser: {userName: "周亮", ...}')
  console.log('- ✅ 设置当前租户: {name: "陵水运营管理平台", ...}')
  console.log('- ⏰ 延迟检查用户和租户信息...')
  
}, 3000)

console.log('🔧 测试脚本已加载，3秒后开始检查...') 