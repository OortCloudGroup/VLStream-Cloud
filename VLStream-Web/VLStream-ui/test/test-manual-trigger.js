/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

// loadTenantInfo
console.log('🔧 手动触发loadTenantInfo测试...')

// Get Vuecomponentinstance
function findLayoutComponent() {
  // find can Vuecomponentinstance
  const app = document.querySelector('#app')
  if (app && app.__vue_app__) {
    console.log('✅ 找到Vue应用实例')
    return app.__vue_app__
  }

  // find layoutcomponent
  const layoutElement = document.querySelector('.layout-container')
  if (layoutElement) {
    console.log('✅ 找到layout容器元素')
    return layoutElement
  }

  console.log('❌ 未找到Vue应用或layout组件')
  return null
}

// loadTenantInfo
function manualTriggerLoadTenantInfo() {
  console.log('🧪 尝试手动触发loadTenantInfo...')

  const app = findLayoutComponent()
  if (!app) {
    console.log('❌ 无法找到组件实例')
    return
  }

  // full variable
  if (window.__VUE_DEVTOOLS_GLOBAL_HOOK__) {
    console.log('🔍 Vue DevTools可用，尝试获取组件实例')

    // find layoutcomponent instance
    const instances = window.__VUE_DEVTOOLS_GLOBAL_HOOK__.apps || []
    if (instances.length > 0) {
      console.log('✅ 找到Vue应用实例数量:', instances.length)

      // find layoutcomponent
      for (const instance of instances) {
        if (instance._instance && instance._instance.type && instance._instance.type.name === 'Layout') {
          console.log('✅ 找到Layout组件实例')

          // loadTenantInfo
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

// Execute
setTimeout(() => {
  console.log('📋 开始手动触发测试')
  manualTriggerLoadTenantInfo()
}, 1000)

console.log('🔧 手动触发测试工具已加载，1秒后开始测试...')