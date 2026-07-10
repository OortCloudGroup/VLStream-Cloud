// 简单的页面状态检查
console.log('🔧 简单页面状态检查...')

function checkPageState() {
  console.log('🧪 检查页面状态...')
  
  // 检查租户信息
  const tenantElements = document.querySelectorAll('.tenant-info')
  console.log('📋 租户信息元素数量:', tenantElements.length)
  
  if (tenantElements.length > 0) {
    const tenantText = tenantElements[0].textContent.trim()
    console.log('- 租户显示文本:', tenantText)
    
    if (tenantText === '获取租户信息失败') {
      console.log('❌ 租户信息显示失败状态')
    } else if (tenantText === '加载中...') {
      console.log('⏳ 租户信息正在加载')
    } else if (tenantText === '陵水运营管理平台') {
      console.log('✅ 租户信息显示正确')
    } else {
      console.log('❓ 租户信息显示未知状态:', tenantText)
    }
  }
  
  // 检查用户信息
  const userElements = document.querySelectorAll('.user-info')
  console.log('📋 用户信息元素数量:', userElements.length)
  
  if (userElements.length > 0) {
    const userText = userElements[0].textContent.trim()
    console.log('- 用户显示文本:', userText)
    
    if (userText === '管理员') {
      console.log('❌ 用户信息显示默认状态')
    } else if (userText === '周亮') {
      console.log('✅ 用户信息显示正确')
    } else {
      console.log('❓ 用户信息显示未知状态:', userText)
    }
  }
  
  // 检查是否有Vue组件实例
  const app = document.querySelector('#app')
  if (app && app.__vue_app__) {
    console.log('✅ Vue应用已挂载')
  } else {
    console.log('❌ Vue应用未挂载')
  }
  
  // 检查是否有错误信息
  const errorElements = document.querySelectorAll('.el-message--error, .el-notification--error')
  if (errorElements.length > 0) {
    console.log('⚠️ 发现错误信息:', errorElements.length, '个')
  }
}

// 延迟执行检查
setTimeout(() => {
  console.log('📋 开始页面状态检查')
  checkPageState()
}, 2000)

console.log('🔧 页面状态检查工具已加载，2秒后开始检查...') 