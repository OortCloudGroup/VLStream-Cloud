// 测试全屏布局是否铺满屏幕
console.log('🔧 测试全屏布局是否铺满屏幕...')

// 等待页面加载完成
setTimeout(() => {
  console.log('📋 开始测试全屏布局')
  
  // 检查所有可能的视频播放器元素
  const videoDialogs = document.querySelectorAll('.video-dialog')
  const layoutDialogs = document.querySelectorAll('.layout-dialog')
  const allDialogs = document.querySelectorAll('.video-dialog, .layout-dialog')
  
  console.log('📋 视频播放器元素统计:')
  console.log('- video-dialog 数量:', videoDialogs.length)
  console.log('- layout-dialog 数量:', layoutDialogs.length)
  console.log('- 总计:', allDialogs.length)
  
  if (allDialogs.length > 0) {
    console.log('✅ 找到视频播放器，可以测试全屏布局')
    
    // 检查全屏按钮
    const fullscreenBtn = document.querySelector('.layout-btn[title*="全屏"]')
    if (fullscreenBtn) {
      console.log('✅ 找到全屏按钮')
      
      // 检查当前全屏状态
      const firstDialog = allDialogs[0]
      const isCurrentlyFullscreen = firstDialog.classList.contains('fullscreen-active')
      console.log('📋 当前全屏状态:', isCurrentlyFullscreen ? '全屏' : '非全屏')
      
      if (!isCurrentlyFullscreen) {
        console.log('🧪 测试进入全屏模式...')
        fullscreenBtn.click()
        
        setTimeout(() => {
          const newFullscreenState = firstDialog.classList.contains('fullscreen-active')
          console.log('📋 点击后全屏状态:', newFullscreenState ? '全屏' : '非全屏')
          
          if (newFullscreenState) {
            console.log('✅ 成功进入全屏模式')
            
            // 检查布局元素
            const layoutElements = [
              '.layout-dialog-content',
              '.dialog-main-content', 
              '.video-player-section',
              '.video-area',
              '.video-grid'
            ]
            
            console.log('📋 检查布局元素尺寸:')
            layoutElements.forEach(selector => {
              const element = firstDialog.querySelector(selector)
              if (element) {
                const rect = element.getBoundingClientRect()
                const computedStyle = window.getComputedStyle(element)
                console.log(`- ${selector}:`)
                console.log(`  - 宽度: ${rect.width}px`)
                console.log(`  - 高度: ${rect.height}px`)
                console.log(`  - 显示: ${computedStyle.display}`)
                console.log(`  - Flex: ${computedStyle.flex}`)
                console.log(`  - 背景色: ${computedStyle.backgroundColor}`)
              } else {
                console.log(`- ${selector}: 未找到`)
              }
            })
            
            // 检查是否铺满屏幕
            const dialogRect = firstDialog.getBoundingClientRect()
            const windowWidth = window.innerWidth
            const windowHeight = window.innerHeight
            
            console.log('📋 屏幕尺寸检查:')
            console.log(`- 窗口宽度: ${windowWidth}px`)
            console.log(`- 窗口高度: ${windowHeight}px`)
            console.log(`- 弹窗宽度: ${dialogRect.width}px`)
            console.log(`- 弹窗高度: ${dialogRect.height}px`)
            
            const widthRatio = dialogRect.width / windowWidth
            const heightRatio = dialogRect.height / windowHeight
            
            console.log(`- 宽度比例: ${(widthRatio * 100).toFixed(1)}%`)
            console.log(`- 高度比例: ${(heightRatio * 100).toFixed(1)}%`)
            
            if (widthRatio > 0.95 && heightRatio > 0.95) {
              console.log('✅ 全屏布局已铺满屏幕')
            } else {
              console.log('❌ 全屏布局未完全铺满屏幕')
            }
            
            // 检查PTZ面板是否被隐藏
            const ptzPanels = document.querySelectorAll('.ptz-panel, .ptz-control-section')
            console.log('📋 PTZ面板检查:')
            ptzPanels.forEach((panel, index) => {
              const displayStyle = window.getComputedStyle(panel).display
              console.log(`- PTZ面板 ${index + 1}: ${displayStyle}`)
              
              if (displayStyle === 'none') {
                console.log('✅ PTZ面板已正确隐藏')
              } else {
                console.log('❌ PTZ面板未被隐藏')
              }
            })
            
            // 测试退出全屏
            setTimeout(() => {
              console.log('🧪 测试退出全屏模式...')
              fullscreenBtn.click()
              
              setTimeout(() => {
                const exitFullscreenState = firstDialog.classList.contains('fullscreen-active')
                console.log('📋 退出全屏状态:', exitFullscreenState ? '全屏' : '非全屏')
                
                if (!exitFullscreenState) {
                  console.log('✅ 成功退出全屏模式')
                } else {
                  console.log('❌ 退出全屏失败')
                }
              }, 1000)
            }, 3000)
          } else {
            console.log('❌ 进入全屏失败')
          }
        }, 1000)
      } else {
        console.log('⚠️ 当前已在全屏模式，请先退出全屏再测试')
      }
    } else {
      console.log('❌ 未找到全屏按钮')
    }
  } else {
    console.log('⚠️ 没有找到视频播放器弹窗，请先打开视频播放器')
    console.log('💡 提示：需要先点击分屏按钮打开视频播放器，然后才能测试全屏布局')
  }
  
}, 3000)

console.log('🔧 全屏布局测试脚本已加载，3秒后开始检查...') 