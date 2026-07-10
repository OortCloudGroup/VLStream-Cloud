// 手动触发路由测试
console.log('🔧 手动触发路由测试...')

// 尝试手动触发路由
function manualTriggerRoute() {
  console.log('🧪 尝试手动触发路由...')
  
  // 检查当前路由状态
  const currentPath = window.location.pathname
  const routePath = currentPath.replace('/bus/vls-ui', '')
  
  console.log('📋 当前路由信息:')
  console.log('- 完整路径:', currentPath)
  console.log('- 路由路径:', routePath)
  
  // 尝试手动导航到当前路由
  if (window.$router) {
    console.log('✅ 发现Vue Router实例，尝试手动导航')
    window.$router.push(routePath)
  } else {
    console.log('❌ 没有Vue Router实例')
    
    // 尝试通过window.location重新加载
    console.log('🔄 尝试重新加载页面...')
    window.location.reload()
  }
}

// 检查组件生命周期
function checkComponentLifecycle() {
  console.log('🧪 检查组件生命周期...')
  
  // 检查是否有layout组件
  const layoutContainer = document.querySelector('.layout-container')
  if (layoutContainer) {
    console.log('✅ 发现layout-container组件')
    
    // 检查用户和租户信息元素
    const tenantInfo = document.querySelector('.tenant-info')
    const userInfo = document.querySelector('.user-info')
    
    if (tenantInfo) {
      console.log('✅ 发现租户信息元素:', tenantInfo.textContent?.trim())
    } else {
      console.log('❌ 没有发现租户信息元素')
    }
    
    if (userInfo) {
      console.log('✅ 发现用户信息元素:', userInfo.textContent?.trim())
    } else {
      console.log('❌ 没有发现用户信息元素')
    }
  } else {
    console.log('❌ 没有发现layout-container组件')
  }
}

// 尝试手动触发onMounted
function manualTriggerOnMounted() {
  console.log('🧪 尝试手动触发onMounted...')
  
  // 检查是否有Vue组件实例
  const vueElements = document.querySelectorAll('[data-v-]')
  console.log('📋 Vue组件元素数量:', vueElements.length)
  
  if (vueElements.length > 0) {
    console.log('✅ 发现Vue组件，尝试触发事件')
    
    // 尝试触发一个自定义事件来模拟组件挂载
    const event = new CustomEvent('manualComponentMount', {
      detail: { timestamp: Date.now() }
    })
    window.dispatchEvent(event)
    
    console.log('📡 已发送manualComponentMount事件')
  }
}

// 主检查函数
function mainCheck() {
  console.log('📋 开始手动路由测试')
  
  checkComponentLifecycle()
  manualTriggerRoute()
  manualTriggerOnMounted()
  
  console.log('\n📋 测试结果:')
  console.log('1. 如果组件已渲染但路由未初始化，说明路由守卫有问题')
  console.log('2. 如果手动导航成功，说明路由配置正确')
  console.log('3. 如果事件触发成功，可能触发组件重新挂载')
}

// 延迟执行
setTimeout(() => {
  mainCheck()
}, 1000)

console.log('🔧 手动路由测试工具已加载，1秒后开始测试...') 