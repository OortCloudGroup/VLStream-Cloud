/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

// page
console.log('🔧 调试页面显示内容...')

function debugPageDisplay() {
  console.log('🧪 检查页面显示内容...')

  // info
  const tenantElements = document.querySelectorAll('.tenant-info')
  console.log('📋 租户信息元素:', tenantElements)

  tenantElements.forEach((element, index) => {
    console.log(`- 租户元素 ${index}:`, {
      textContent: element.textContent,
      innerHTML: element.innerHTML,
      visible: element.offsetParent !== null
    })
  })

  // userinfo
  const userElements = document.querySelectorAll('.user-info')
  console.log('📋 用户信息元素:', userElements)

  userElements.forEach((element, index) => {
    console.log(`- 用户元素 ${index}:`, {
      textContent: element.textContent,
      innerHTML: element.innerHTML,
      visible: element.offsetParent !== null
    })
  })

  // Vuecomponent (if )
  if (window.__VUE_DEVTOOLS_GLOBAL_HOOK__) {
    console.log('🔍 Vue DevTools可用，可以检查组件状态')
  }

  // whether CSS element
  const headerRight = document.querySelector('.header-right')
  if (headerRight) {
    console.log('📋 右侧头部区域:', {
      display: getComputedStyle(headerRight).display,
      visibility: getComputedStyle(headerRight).visibility,
      opacity: getComputedStyle(headerRight).opacity
    })
  }

  // menuwhether
  const dropdowns = document.querySelectorAll('.el-dropdown')
  dropdowns.forEach((dropdown, index) => {
    console.log(`- 下拉菜单 ${index}:`, {
      display: getComputedStyle(dropdown).display,
      visibility: getComputedStyle(dropdown).visibility,
      opacity: getComputedStyle(dropdown).opacity
    })
  })
}

// Execute , page full Load
setTimeout(() => {
  console.log('📋 开始调试页面显示内容')
  debugPageDisplay()
}, 1000)

console.log('🔧 调试工具已加载，1秒后开始检查页面显示...')