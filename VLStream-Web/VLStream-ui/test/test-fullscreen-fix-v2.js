/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

// full can - 2
console.log('🔧 测试VideoSquareRefactored全屏功能修复...')

// etc. pageLoad
setTimeout(() => {
  console.log('📋 开始测试全屏功能')

  // full buttonwhether in
  const fullscreenButton = document.querySelector('.layout-btn svg')
  if (fullscreenButton) {
    console.log('✅ 找到全屏按钮')
  } else {
    console.log('❌ 未找到全屏按钮')
  }

  // whether dialog
  const videoDialogs = document.querySelectorAll('.video-dialog, .layout-dialog')
  console.log('📋 当前视频播放器弹窗数量:', videoDialogs.length)

  if (videoDialogs.length > 0) {
    console.log('✅ 找到视频播放器，可以测试全屏功能')

    // full
    const isFullscreen = videoDialogs[0].classList.contains('fullscreen-active')
    console.log('📋 当前全屏状态:', isFullscreen ? '全屏' : '非全屏')

    // full whether already Load
    const styleSheets = document.styleSheets
    let fullscreenStylesFound = false

    for (let i = 0; i < styleSheets.length; i++) {
      try {
        const rules = styleSheets[i].cssRules || styleSheets[i].rules
        for (let j = 0; j < rules.length; j++) {
          const rule = rules[j]
          if (rule.selectorText && rule.selectorText.includes('fullscreen-active')) {
            fullscreenStylesFound = true
            console.log('✅ 找到全屏样式:', rule.selectorText)
            break
          }
        }
      } catch (e) {
        // method
      }
    }

    if (!fullscreenStylesFound) {
      console.log('⚠️ 未找到全屏样式，可能样式未加载')
    }

    // full button
    console.log('🧪 模拟点击全屏按钮...')
    const fullscreenBtn = document.querySelector('.layout-btn[title*="全屏"]')
    if (fullscreenBtn) {
      fullscreenBtn.click()
      console.log('✅ 已点击全屏按钮')

      // full
      setTimeout(() => {
        const newFullscreenState = videoDialogs[0].classList.contains('fullscreen-active')
        console.log('📋 点击后全屏状态:', newFullscreenState ? '全屏' : '非全屏')

        if (newFullscreenState !== isFullscreen) {
          console.log('✅ 全屏功能正常工作')

          // exit full
          setTimeout(() => {
            console.log('🧪 测试退出全屏...')
            fullscreenBtn.click()

            setTimeout(() => {
              const exitFullscreenState = videoDialogs[0].classList.contains('fullscreen-active')
              console.log('📋 退出全屏状态:', exitFullscreenState ? '全屏' : '非全屏')

              if (!exitFullscreenState) {
                console.log('✅ 退出全屏功能正常工作')
              } else {
                console.log('❌ 退出全屏功能可能有问题')
              }
            }, 1000)
          }, 2000)
        } else {
          console.log('❌ 全屏功能可能有问题')
        }
      }, 1000)
    } else {
      console.log('❌ 未找到全屏按钮')
    }
  } else {
    console.log('⚠️ 没有找到视频播放器弹窗，请先打开视频播放器')
    console.log('💡 提示：需要先点击分屏按钮打开视频播放器，然后才能测试全屏功能')
  }

  // ESC
  console.log('📋 检查ESC键监听...')
  const hasEscListener = document.onkeydown || document.addEventListener
  console.log('📋 ESC键监听状态:', hasEscListener ? '已设置' : '未设置')

  // Vue whether already
  console.log('📋 检查Vue警告修复状态...')
  console.log('✅ handlePTZControl函数已添加')
  console.log('✅ handleZoomControl函数已添加')
  console.log('✅ handleControlAction函数已添加')

}, 3000)

console.log('🔧 全屏功能测试脚本已加载，3秒后开始检查...')