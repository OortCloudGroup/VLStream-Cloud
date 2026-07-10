// 测试onMounted修复
console.log('🔧 测试onMounted修复...')

// 检查页面是否显示了正确的用户和租户信息
function checkPageDisplay() {
  console.log('🧪 检查页面显示状态...')
  
  const tenantText = document.querySelector('.tenant-info')?.textContent?.trim()
  const userText = document.querySelector('.user-info')?.textContent?.trim()
  
  console.log('📋 当前页面显示:')
  console.log('- 租户:', tenantText)
  console.log('- 用户:', userText)
  
  if (tenantText === '陵水运营管理平台' && userText === '周亮') {
    console.log('✅ 页面显示正确！修复成功！')
    return true
  } else if (tenantText === '获取租户信息失败' && userText === '管理员') {
    console.log('❌ 页面仍显示默认值，loadTenantInfo可能没有被调用')
    return false
  } else {
    console.log('⚠️ 页面显示异常状态')
    return false
  }
}

// 延迟检查
setTimeout(() => {
  console.log('📋 开始检查页面显示')
  const isCorrect = checkPageDisplay()
  
  if (!isCorrect) {
    console.log('\n🔍 如果页面仍显示默认值，请:')
    console.log('1. 重新加载页面')
    console.log('2. 观察控制台是否有"🎬 组件开始挂载..."日志')
    console.log('3. 观察是否有"🔄 开始调用loadTenantInfo..."日志')
    console.log('4. 如果没有这些日志，说明onMounted没有被调用')
  }
}, 2000)

console.log('🔧 onMounted修复测试工具已加载，2秒后开始检查...') 