// 手动触发loadTenantInfo测试
console.log('🔧 手动触发loadTenantInfo测试...')

// 尝试获取Vue组件实例
function findLayoutComponent() {
  // 查找可能的Vue组件实例
  const app = document.querySelector('#app')
  if (app && app.__vue_app__) {
    console.log('✅ 找到Vue应用实例')
    return app.__vue_app__
  }
  
  // 查找layout组件
  const layoutElement = document.querySelector('.layout-container')
  if (layoutElement) {
    console.log('✅ 找到layout容器元素')
    return layoutElement
  }
  
  console.log('❌ 未找到Vue应用或layout组件')
  return null
}

// 手动触发loadTenantInfo
function manualTriggerLoadTenantInfo() {
  console.log('🧪 尝试手动触发loadTenantInfo...')
  
  const app = findLayoutComponent()
  if (!app) {
    console.log('❌ 无法找到组件实例')
    return
  }
  
  // 尝试通过全局变量访问
  if (window.__VUE_DEVTOOLS_GLOBAL_HOOK__) {
    console.log('🔍 Vue DevTools可用，尝试获取组件实例')
    
    // 查找layout组件的实例
    const instances = window.__VUE_DEVTOOLS_GLOBAL_HOOK__.apps || []
    if (instances.length > 0) {
      console.log('✅ 找到Vue应用实例数量:', instances.length)
      
      // 尝试查找layout组件
      for (const instance of instances) {
        if (instance._instance && instance._instance.type && instance._instance.type.name === 'Layout') {
          console.log('✅ 找到Layout组件实例')
          
          // 尝试调用loadTenantInfo
          if (instance._instance.exposed && instance._instance.exposed.loadTenantInfo) {
            console.log('✅ 找到loadTenantInfo方法，尝试调用...')
            instance._instance.exposed.loadTenantInfo()
            return
          }
        }
      }
    }
  }
  
  console.log('❌ 无法找到loadTenantInfo方法')
}

// 延迟执行
setTimeout(() => {
  console.log('📋 开始手动触发测试')
  manualTriggerLoadTenantInfo()
}, 1000)

console.log('🔧 手动触发测试工具已加载，1秒后开始测试...') 