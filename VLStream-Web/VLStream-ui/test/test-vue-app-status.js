// 检查Vue应用状态
console.log('🔧 检查Vue应用状态...')

// 检查Vue应用是否正常运行
function checkVueApp() {
  console.log('🧪 检查Vue应用...')
  
  // 检查Vue DevTools
  if (window.__VUE_DEVTOOLS_GLOBAL_HOOK__) {
    console.log('✅ Vue DevTools可用')
  } else {
    console.log('❌ Vue DevTools不可用')
  }
  
  // 检查是否有Vue相关的全局变量
  if (window.Vue) {
    console.log('✅ 发现Vue全局变量')
  } else {
    console.log('❌ 没有发现Vue全局变量')
  }
  
  // 检查页面上的Vue组件
  const vueElements = document.querySelectorAll('[data-v-]')
  console.log('📋 Vue组件元素数量:', vueElements.length)
  
  if (vueElements.length > 0) {
    console.log('✅ 发现Vue组件元素')
    vueElements.forEach((el, index) => {
      console.log(`- 组件 ${index + 1}:`, el.tagName, el.className)
    })
  } else {
    console.log('❌ 没有发现Vue组件元素')
  }
}

// 检查路由状态
function checkRouter() {
  console.log('🧪 检查路由状态...')
  
  console.log('📋 当前URL:', window.location.href)
  console.log('📋 当前路径:', window.location.pathname)
  
  // 检查是否有router-view元素
  const routerView = document.querySelector('router-view, [data-v-router-view]')
  if (routerView) {
    console.log('✅ 发现router-view元素')
  } else {
    console.log('❌ 没有发现router-view元素')
  }
}

// 检查JavaScript错误
function checkErrors() {
  console.log('🧪 检查JavaScript错误...')
  
  // 监听错误事件
  window.addEventListener('error', (event) => {
    console.error('❌ JavaScript错误:', event.error)
    console.error('❌ 错误文件:', event.filename)
    console.error('❌ 错误行号:', event.lineno)
  })
  
  // 监听未处理的Promise拒绝
  window.addEventListener('unhandledrejection', (event) => {
    console.error('❌ 未处理的Promise拒绝:', event.reason)
  })
  
  console.log('📋 已设置错误监听器')
}

// 检查网络请求
function checkNetwork() {
  console.log('🧪 检查网络请求...')
  
  // 检查是否有网络请求错误
  console.log('📋 请检查Network标签页:')
  console.log('- 是否有404错误')
  console.log('- 是否有500错误')
  console.log('- 是否有JavaScript文件加载失败')
  console.log('- 是否有CSS文件加载失败')
}

// 主检查函数
function mainCheck() {
  console.log('📋 开始Vue应用状态检查')
  
  checkVueApp()
  checkRouter()
  checkErrors()
  checkNetwork()
  
  console.log('\n📋 检查结果:')
  console.log('1. 如果Vue DevTools不可用，说明Vue应用没有正确初始化')
  console.log('2. 如果没有Vue组件元素，说明组件没有渲染')
  console.log('3. 如果有JavaScript错误，会显示在控制台')
  console.log('4. 如果有网络错误，请检查Network标签页')
}

// 延迟执行
setTimeout(() => {
  mainCheck()
}, 1000)

console.log('🔧 Vue应用状态检查工具已加载，1秒后开始检查...') 