// 直接检查函数调用
console.log('🔧 直接检查函数调用...')

// 检查是否有任何loadTenantInfo相关的日志
function checkForLoadTenantInfoLogs() {
  console.log('🧪 检查是否有loadTenantInfo相关日志...')
  
  // 获取所有控制台日志（如果可能）
  const logs = []
  
  // 检查是否有任何包含loadTenantInfo的日志
  console.log('📋 请检查控制台是否有以下日志:')
  console.log('- 🎬 组件开始挂载...')
  console.log('- 🚀 loadTenantInfo函数开始执行...')
  console.log('- 🏁 loadTenantInfo函数执行完成')
  console.log('- ✅ loadTenantInfo调用完成')
  
  console.log('\n📋 如果没有看到这些日志，说明:')
  console.log('1. loadTenantInfo函数没有被调用')
  console.log('2. 或者函数调用有错误')
  
  // 检查当前页面状态
  const tenantText = document.querySelector('.tenant-info')?.textContent?.trim()
  const userText = document.querySelector('.user-info')?.textContent?.trim()
  
  console.log('\n📋 当前页面状态:')
  console.log('- 租户显示:', tenantText)
  console.log('- 用户显示:', userText)
  
  if (tenantText === '获取租户信息失败' && userText === '管理员') {
    console.log('\n❌ 确认问题: loadTenantInfo函数没有被正确调用')
    console.log('建议: 重新加载页面，观察控制台日志')
  }
}

// 延迟执行检查
setTimeout(() => {
  console.log('📋 开始直接检查')
  checkForLoadTenantInfoLogs()
}, 1000)

console.log('🔧 直接检查工具已加载，1秒后开始检查...') 