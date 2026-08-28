/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

//
console.log('🔧 测试路由匹配...')

// current and
function checkRouteMatching() {
  console.log('🧪 检查路由匹配...')

  const currentPath = window.location.pathname
  const currentSearch = window.location.search
  const fullUrl = window.location.href

  console.log('📋 当前URL信息:')
  console.log('- 完整URL:', fullUrl)
  console.log('- 路径:', currentPath)
  console.log('- 查询参数:', currentSearch)

  //
  if (currentPath.startsWith('/bus/vls-ui/')) {
    console.log('✅ 路径包含正确的base URL')
    const routePath = currentPath.replace('/bus/vls-ui', '')
    console.log('- 路由路径:', routePath)

    if (routePath === '/workspace') {
      console.log('✅ 路由路径匹配 /workspace')
    } else {
      console.log('❌ 路由路径不匹配 /workspace')
    }
  } else {
    console.log('❌ 路径不包含正确的base URL')
  }

  // whether routerinstance
  if (window.$router) {
    console.log('✅ 发现Vue Router实例')
    console.log('- 当前路由:', window.$router.currentRoute.value)
  } else {
    console.log('❌ 没有发现Vue Router实例')
  }

  // page
  const appElement = document.querySelector('#app')
  if (appElement) {
    console.log('✅ 发现#app元素')
    console.log('- 内容:', appElement.innerHTML.substring(0, 200) + '...')
  } else {
    console.log('❌ 没有发现#app元素')
  }
}

// Vue
function checkVueMount() {
  console.log('🧪 检查Vue应用挂载...')

  // whether Vue
  const mountPoints = document.querySelectorAll('#app, [data-v-app]')
  console.log('📋 Vue挂载点数量:', mountPoints.length)

  mountPoints.forEach((el, index) => {
    console.log(`- 挂载点 ${index + 1}:`, el.tagName, el.id, el.className)
  })

  // pagewhether
  const bodyContent = document.body.innerHTML
  console.log('📋 页面内容长度:', bodyContent.length)

  if (bodyContent.length < 100) {
    console.log('⚠️ 页面内容很少，可能没有正确加载')
  } else {
    console.log('✅ 页面有足够的内容')
  }
}

// main
function mainCheck() {
  console.log('📋 开始路由匹配检查')

  checkRouteMatching()
  checkVueMount()

  console.log('\n📋 检查结果:')
  console.log('1. 如果路径不匹配，说明路由配置有问题')
  console.log('2. 如果没有#app元素，说明Vue应用没有挂载')
  console.log('3. 如果页面内容很少，说明组件没有渲染')
}

// Execute
setTimeout(() => {
  mainCheck()
}, 1000)

console.log('🔧 路由匹配检查工具已加载，1秒后开始检查...')