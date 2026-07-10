// 测试PTZ面板全屏隐藏功能
console.log('🔧 测试PTZ面板全屏隐藏功能...')

// 等待页面加载完成
setTimeout(() => {
  console.log('📋 开始测试PTZ面板隐藏功能')
  
  // 检查所有可能的视频播放器元素
  const videoDialogs = document.querySelectorAll('.video-dialog')
  const layoutDialogs = document.querySelectorAll('.layout-dialog')
  const allDialogs = document.querySelectorAll('.video-dialog, .layout-dialog')
  
  console.log('📋 视频播放器元素统计:')
  console.log('- video-dialog 数量:', videoDialogs.length)
  console.log('- layout-dialog 数量:', layoutDialogs.length)
  console.log('- 总计:', allDialogs.length)
  
  if (allDialogs.length > 0) {
    console.log('✅ 找到视频播放器，可以测试PTZ隐藏功能')
    
    // 检查PTZ面板元素
    const ptzPanels = document.querySelectorAll('.ptz-panel, .ptz-control-section')
    console.log('📋 PTZ面板元素数量:', ptzPanels.length)
    
    ptzPanels.forEach((panel, index) => {
      console.log(`📋 PTZ面板 ${index + 1}:`)
      console.log('- 类名:', panel.className)
      console.log('- 是否可见:', panel.offsetWidth > 0 && panel.offsetHeight > 0)
      console.log('- 当前显示状态:', window.getComputedStyle(panel).display)
    })
    
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
            
            // 检查PTZ面板是否被隐藏
            const ptzPanelsAfterFullscreen = document.querySelectorAll('.ptz-panel, .ptz-control-section')
            console.log('📋 全屏后PTZ面板数量:', ptzPanelsAfterFullscreen.length)
            
            ptzPanelsAfterFullscreen.forEach((panel, index) => {
              const displayStyle = window.getComputedStyle(panel).display
              console.log(`📋 PTZ面板 ${index + 1} 显示状态:`, displayStyle)
              
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
                  
                  // 检查PTZ面板是否重新显示
                  const ptzPanelsAfterExit = document.querySelectorAll('.ptz-panel, .ptz-control-section')
                  ptzPanelsAfterExit.forEach((panel, index) => {
                    const displayStyle = window.getComputedStyle(panel).display
                    console.log(`📋 退出全屏后PTZ面板 ${index + 1} 显示状态:`, displayStyle)
                    
                    if (displayStyle !== 'none') {
                      console.log('✅ PTZ面板已重新显示')
                    } else {
                      console.log('❌ PTZ面板仍然隐藏')
                    }
                  })
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
    console.log('💡 提示：需要先点击分屏按钮打开视频播放器，然后才能测试PTZ隐藏功能')
  }
  
}, 3000)

console.log('🔧 PTZ面板隐藏测试脚本已加载，3秒后开始检查...') 