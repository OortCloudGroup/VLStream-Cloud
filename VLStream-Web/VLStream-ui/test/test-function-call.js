// 测试函数调用
console.log('🔧 测试函数调用...')

// 等待页面加载完成
setTimeout(() => {
  console.log('📋 开始测试函数调用')
  
  // 检查控制台日志
  console.log('📋 检查控制台日志...')
  
  // 手动触发函数调用
  console.log('🔄 手动触发forceLoadUserAndTenantInfo函数...')
  
  // 检查是否有Vue组件实例
  const app = document.querySelector('#app')
  if (app && app.__vue_app__) {
    console.log('✅ Vue应用已挂载')
    
    // 尝试获取组件实例
    const layoutComponent = document.querySelector('.layout-container')
    if (layoutComponent) {
      console.log('✅ 找到layout组件')
      
      // 检查是否有我们的函数
      if (window.forceLoadUserAndTenantInfo) {
        console.log('✅ 找到forceLoadUserAndTenantInfo函数')
        window.forceLoadUserAndTenantInfo()
      } else {
        console.log('❌ 未找到forceLoadUserAndTenantInfo函数')
      }
    } else {
      console.log('❌ 未找到layout组件')
    }
  } else {
    console.log('❌ Vue应用未挂载')
  }
  
}, 2000)

console.log('🔧 测试脚本已加载，2秒后开始检查...') 