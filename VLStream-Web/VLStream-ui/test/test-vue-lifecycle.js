// 检查Vue组件生命周期
console.log('🔧 检查Vue组件生命周期...')

// 检查是否有任何JavaScript错误
function checkForErrors() {
  console.log('🧪 检查JavaScript错误...')
  
  // 检查控制台是否有错误
  console.log('📋 请检查控制台是否有以下错误:')
  console.log('- SyntaxError')
  console.log('- ReferenceError')
  console.log('- TypeError')
  console.log('- 任何红色错误信息')
  
  // 检查页面是否有错误
  const errorElements = document.querySelectorAll('.error, .alert, [class*="error"]')
  if (errorElements.length > 0) {
    console.log('⚠️ 发现页面错误元素:', errorElements.length)
  }
}

// 检查Vue应用状态
function checkVueApp() {
  console.log('🧪 检查Vue应用状态...')
  
  // 检查是否有Vue相关的全局变量
  if (window.__VUE_DEVTOOLS_GLOBAL_HOOK__) {
    console.log('✅ Vue DevTools可用')
  } else {
    console.log('⚠️ Vue DevTools不可用')
  }
  
  // 检查页面是否有Vue相关的属性
  const vueElements = document.querySelectorAll('[data-v-]')
  console.log('📋 Vue组件元素数量:', vueElements.length)
  
  if (vueElements.length > 0) {
    console.log('✅ 发现Vue组件元素')
  } else {
    console.log('❌ 没有发现Vue组件元素')
  }
}

// 检查路由状态
function checkRoute() {
  console.log('🧪 检查路由状态...')
  
  console.log('📋 当前URL:', window.location.href)
  console.log('📋 当前路径:', window.location.pathname)
  console.log('📋 URL参数:', window.location.search)
  
  // 检查是否有路由相关的错误
  const routeError = document.querySelector('.router-error, .route-error')
  if (routeError) {
    console.log('⚠️ 发现路由错误:', routeError.textContent)
  }
}

// 检查网络请求
function checkNetworkRequests() {
  console.log('🧪 检查网络请求...')
  
  // 检查是否有网络请求错误
  console.log('📋 请检查Network标签页是否有:')
  console.log('- 404错误')
  console.log('- 500错误')
  console.log('- 网络连接错误')
  console.log('- getUserTenants API调用')
}

// 主检查函数
function mainCheck() {
  console.log('📋 开始Vue生命周期检查')
  
  checkForErrors()
  checkVueApp()
  checkRoute()
  checkNetworkRequests()
  
  console.log('\n📋 检查结果总结:')
  console.log('1. 如果看到"🎬 组件开始挂载..."，说明onMounted被调用')
  console.log('2. 如果没有看到，说明组件没有正确挂载')
  console.log('3. 检查是否有JavaScript错误阻止了组件挂载')
  console.log('4. 检查Network标签页是否有API调用')
}

// 延迟执行
setTimeout(() => {
  mainCheck()
}, 1000)

console.log('🔧 Vue生命周期检查工具已加载，1秒后开始检查...') 