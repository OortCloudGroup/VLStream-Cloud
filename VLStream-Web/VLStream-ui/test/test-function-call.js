/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

//
console.log('🔧 测试函数调用...')

// etc. pageLoad
setTimeout(() => {
  console.log('📋 开始测试函数调用')

  // control log
  console.log('📋 检查控制台日志...')

  //
  console.log('🔄 手动触发forceLoadUserAndTenantInfo函数...')

  // whether Vuecomponentinstance
  const app = document.querySelector('#app')
  if (app && app.__vue_app__) {
    console.log('✅ Vue应用已挂载')

    // Get componentinstance
    const layoutComponent = document.querySelector('.layout-container')
    if (layoutComponent) {
      console.log('✅ 找到layout组件')

      // whether
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