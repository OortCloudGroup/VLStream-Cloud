// 测试loadTenantInfo是否被正确调用
console.log('🔧 测试loadTenantInfo调用...')

// 使用更安全的方式监控日志
const originalLog = console.log
const loadTenantInfoLogs = []

// 创建一个安全的日志包装器
const safeLog = function(...args) {
  const message = args.join(' ')
  loadTenantInfoLogs.push(message)
  
  // 直接调用原始console.log，避免递归
  originalLog.apply(console, args)
}

// 替换console.log
console.log = safeLog

// 延迟检查日志
setTimeout(() => {
  console.log('\n📋 loadTenantInfo调用检查结果:')
  
  const hasStartLog = loadTenantInfoLogs.some(log => log.includes('🚀 loadTenantInfo函数开始执行'))
  const hasEndLog = loadTenantInfoLogs.some(log => log.includes('🏁 loadTenantInfo函数执行完成'))
  const hasApiCall = loadTenantInfoLogs.some(log => log.includes('开始调用getUserTenants API'))
  const hasApiResponse = loadTenantInfoLogs.some(log => log.includes('getUserTenants API响应'))
  const hasUserUpdate = loadTenantInfoLogs.some(log => log.includes('更新currentUser'))
  const hasTenantUpdate = loadTenantInfoLogs.some(log => log.includes('设置当前租户'))
  
  console.log('- 函数开始执行:', hasStartLog ? '✅' : '❌')
  console.log('- 函数执行完成:', hasEndLog ? '✅' : '❌')
  console.log('- API调用:', hasApiCall ? '✅' : '❌')
  console.log('- API响应:', hasApiResponse ? '✅' : '❌')
  console.log('- 用户信息更新:', hasUserUpdate ? '✅' : '❌')
  console.log('- 租户信息更新:', hasTenantUpdate ? '✅' : '❌')
  
  if (!hasStartLog) {
    console.log('❌ loadTenantInfo函数没有被调用')
  } else if (!hasEndLog) {
    console.log('❌ loadTenantInfo函数没有正常完成')
  } else if (!hasApiCall) {
    console.log('❌ getUserTenants API没有被调用')
  } else if (!hasApiResponse) {
    console.log('❌ getUserTenants API没有返回响应')
  } else if (!hasUserUpdate || !hasTenantUpdate) {
    console.log('❌ 用户或租户信息没有更新')
  } else {
    console.log('✅ loadTenantInfo函数执行正常')
  }
  
  // 恢复原始console.log
  console.log = originalLog
}, 3000)

console.log('🔧 开始监控loadTenantInfo调用...') 