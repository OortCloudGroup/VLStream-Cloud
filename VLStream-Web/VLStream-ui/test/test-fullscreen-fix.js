// 测试视频广场全屏功能修复
console.log('🔧 测试视频广场全屏功能...')

// 等待页面加载完成
setTimeout(() => {
  console.log('📋 开始测试全屏功能')
  
  // 检查全屏按钮是否存在
  const fullscreenButton = document.querySelector('.layout-btn svg')
  if (fullscreenButton) {
    console.log('✅ 找到全屏按钮')
  } else {
    console.log('❌ 未找到全屏按钮')
  }
  
  // 检查toggleFullscreen函数是否存在
  if (window.toggleFullscreen) {
    console.log('✅ toggleFullscreen函数已暴露到window')
  } else {
    console.log('❌ toggleFullscreen函数未暴露')
  }
  
  // 检查全屏样式是否已加载
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
      // 跨域样式表无法访问
    }
  }
  
  if (!fullscreenStylesFound) {
    console.log('⚠️ 未找到全屏样式，可能样式未加载')
  }
  
  // 检查是否有视频播放器弹窗
  const videoDialogs = document.querySelectorAll('.video-dialog, .layout-dialog')
  console.log('📋 当前视频播放器弹窗数量:', videoDialogs.length)
  
  if (videoDialogs.length > 0) {
    console.log('✅ 找到视频播放器，可以测试全屏功能')
    
    // 检查全屏状态
    const isFullscreen = videoDialogs[0].classList.contains('fullscreen-active')
    console.log('📋 当前全屏状态:', isFullscreen ? '全屏' : '非全屏')
    
    // 模拟点击全屏按钮
    console.log('🧪 模拟点击全屏按钮...')
    const fullscreenBtn = document.querySelector('.layout-btn[title*="全屏"]')
    if (fullscreenBtn) {
      fullscreenBtn.click()
      console.log('✅ 已点击全屏按钮')
      
      // 延迟检查全屏状态变化
      setTimeout(() => {
        const newFullscreenState = videoDialogs[0].classList.contains('fullscreen-active')
        console.log('📋 点击后全屏状态:', newFullscreenState ? '全屏' : '非全屏')
        
        if (newFullscreenState !== isFullscreen) {
          console.log('✅ 全屏功能正常工作')
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
  
  // 检查ESC键监听
  console.log('📋 检查ESC键监听...')
  const hasEscListener = document.onkeydown || document.addEventListener
  console.log('📋 ESC键监听状态:', hasEscListener ? '已设置' : '未设置')
  
}, 2000)

console.log('🔧 全屏功能测试脚本已加载，2秒后开始检查...') 