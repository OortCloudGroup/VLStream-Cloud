/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

// full can
console.log('🔧 诊断视频广场全屏功能问题...')

// etc. pageLoad
setTimeout(() => {
  console.log('📋 开始诊断全屏功能')

  // all can element
  const videoDialogs = document.querySelectorAll('.video-dialog')
  const layoutDialogs = document.querySelectorAll('.layout-dialog')
  const allDialogs = document.querySelectorAll('.video-dialog, .layout-dialog')

  console.log('📋 视频播放器元素统计:')
  console.log('- video-dialog 数量:', videoDialogs.length)
  console.log('- layout-dialog 数量:', layoutDialogs.length)
  console.log('- 总计:', allDialogs.length)

  // each dialog info
  allDialogs.forEach((dialog, index) => {
    console.log(`📋 弹窗 ${index + 1}:`)
    console.log('- 类名:', dialog.className)
    console.log('- 标签名:', dialog.tagName)
    console.log('- 是否可见:', dialog.offsetWidth > 0 && dialog.offsetHeight > 0)
    console.log('- 当前全屏状态:', dialog.classList.contains('fullscreen-active'))
  })

  // full button
  const fullscreenBtn = document.querySelector('.layout-btn[title*="全屏"]')
  if (fullscreenBtn) {
    console.log('✅ 找到全屏按钮')
    console.log('- 按钮文本:', fullscreenBtn.textContent)
    console.log('- 按钮标题:', fullscreenBtn.title)
  } else {
    console.log('❌ 未找到全屏按钮')
  }

  // full
  const styleSheets = document.styleSheets
  let fullscreenStylesFound = 0

  for (let i = 0; i < styleSheets.length; i++) {
    try {
      const rules = styleSheets[i].cssRules || styleSheets[i].rules
      for (let j = 0; j < rules.length; j++) {
        const rule = rules[j]
        if (rule.selectorText && rule.selectorText.includes('fullscreen-active')) {
          fullscreenStylesFound++
          console.log(`✅ 找到全屏样式 ${fullscreenStylesFound}:`, rule.selectorText)
        }
      }
    } catch (e) {
      // method
    }
  }

  console.log(`📋 找到 ${fullscreenStylesFound} 个全屏样式`)

  // toggleFullscreen
  console.log('📋 检查toggleFullscreen函数...')

  // full can
  if (allDialogs.length > 0) {
    console.log('🧪 手动测试全屏功能...')

    const firstDialog = allDialogs[0]
    const initialFullscreenState = firstDialog.classList.contains('fullscreen-active')
    console.log('- 初始全屏状态:', initialFullscreenState ? '全屏' : '非全屏')

    // full
    console.log('- 手动添加全屏样式...')
    firstDialog.classList.add('fullscreen-active')

    setTimeout(() => {
      const newFullscreenState = firstDialog.classList.contains('fullscreen-active')
      console.log('- 手动添加后全屏状态:', newFullscreenState ? '全屏' : '非全屏')

      if (newFullscreenState) {
        console.log('✅ 全屏样式可以正常添加')

        // full
        console.log('- 手动移除全屏样式...')
        firstDialog.classList.remove('fullscreen-active')

        setTimeout(() => {
          const finalFullscreenState = firstDialog.classList.contains('fullscreen-active')
          console.log('- 手动移除后全屏状态:', finalFullscreenState ? '全屏' : '非全屏')

          if (!finalFullscreenState) {
            console.log('✅ 全屏样式可以正常移除')
          } else {
            console.log('❌ 全屏样式移除失败')
          }
        }, 500)
      } else {
        console.log('❌ 全屏样式添加失败')
      }
    }, 500)
  } else {
    console.log('⚠️ 没有找到视频播放器弹窗')
  }

  // Vuecomponent
  console.log('📋 检查Vue组件状态...')
  console.log('- isFullscreen状态:', window.isFullscreen)

  // whether full toggleFullscreen
  if (window.toggleFullscreen) {
    console.log('✅ 找到全局toggleFullscreen函数')
  } else {
    console.log('❌ 未找到全局toggleFullscreen函数')
  }

}, 3000)

console.log('🔧 全屏功能诊断脚本已加载，3秒后开始检查...')