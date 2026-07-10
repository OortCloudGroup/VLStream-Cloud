// 调试页面显示内容
console.log('🔧 调试页面显示内容...')

function debugPageDisplay() {
  console.log('🧪 检查页面显示内容...')
  
  // 检查租户信息显示
  const tenantElements = document.querySelectorAll('.tenant-info')
  console.log('📋 租户信息元素:', tenantElements)
  
  tenantElements.forEach((element, index) => {
    console.log(`- 租户元素 ${index}:`, {
      textContent: element.textContent,
      innerHTML: element.innerHTML,
      visible: element.offsetParent !== null
    })
  })
  
  // 检查用户信息显示
  const userElements = document.querySelectorAll('.user-info')
  console.log('📋 用户信息元素:', userElements)
  
  userElements.forEach((element, index) => {
    console.log(`- 用户元素 ${index}:`, {
      textContent: element.textContent,
      innerHTML: element.innerHTML,
      visible: element.offsetParent !== null
    })
  })
  
  // 检查Vue组件状态（如果可用）
  if (window.__VUE_DEVTOOLS_GLOBAL_HOOK__) {
    console.log('🔍 Vue DevTools可用，可以检查组件状态')
  }
  
  // 检查是否有CSS隐藏了元素
  const headerRight = document.querySelector('.header-right')
  if (headerRight) {
    console.log('📋 右侧头部区域:', {
      display: getComputedStyle(headerRight).display,
      visibility: getComputedStyle(headerRight).visibility,
      opacity: getComputedStyle(headerRight).opacity
    })
  }
  
  // 检查下拉菜单是否被隐藏
  const dropdowns = document.querySelectorAll('.el-dropdown')
  dropdowns.forEach((dropdown, index) => {
    console.log(`- 下拉菜单 ${index}:`, {
      display: getComputedStyle(dropdown).display,
      visibility: getComputedStyle(dropdown).visibility,
      opacity: getComputedStyle(dropdown).opacity
    })
  })
}

// 延迟执行，确保页面完全加载
setTimeout(() => {
  console.log('📋 开始调试页面显示内容')
  debugPageDisplay()
}, 1000)

console.log('🔧 调试工具已加载，1秒后开始检查页面显示...') 