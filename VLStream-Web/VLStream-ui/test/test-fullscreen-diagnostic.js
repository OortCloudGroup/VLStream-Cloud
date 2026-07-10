// 诊断视频广场全屏功能问题
console.log('🔧 诊断视频广场全屏功能问题...')

// 等待页面加载完成
setTimeout(() => {
  console.log('📋 开始诊断全屏功能')
  
  // 检查所有可能的视频播放器元素
  const videoDialogs = document.querySelectorAll('.video-dialog')
  const layoutDialogs = document.querySelectorAll('.layout-dialog')
  const allDialogs = document.querySelectorAll('.video-dialog, .layout-dialog')
  
  console.log('📋 视频播放器元素统计:')
  console.log('- video-dialog 数量:', videoDialogs.length)
  console.log('- layout-dialog 数量:', layoutDialogs.length)
  console.log('- 总计:', allDialogs.length)
  
  // 检查每个弹窗的详细信息
  allDialogs.forEach((dialog, index) => {
    console.log(`📋 弹窗 ${index + 1}:`)
    console.log('- 类名:', dialog.className)
    console.log('- 标签名:', dialog.tagName)
    console.log('- 是否可见:', dialog.offsetWidth > 0 && dialog.offsetHeight > 0)
    console.log('- 当前全屏状态:', dialog.classList.contains('fullscreen-active'))
  })
  
  // 检查全屏按钮
  const fullscreenBtn = document.querySelector('.layout-btn[title*="全屏"]')
  if (fullscreenBtn) {
    console.log('✅ 找到全屏按钮')
    console.log('- 按钮文本:', fullscreenBtn.textContent)
    console.log('- 按钮标题:', fullscreenBtn.title)
  } else {
    console.log('❌ 未找到全屏按钮')
  }
  
  // 检查全屏样式
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
      // 跨域样式表无法访问
    }
  }
  
  console.log(`📋 找到 ${fullscreenStylesFound} 个全屏样式`)
  
  // 检查toggleFullscreen函数
  console.log('📋 检查toggleFullscreen函数...')
  
  // 尝试手动触发全屏功能
  if (allDialogs.length > 0) {
    console.log('🧪 手动测试全屏功能...')
    
    const firstDialog = allDialogs[0]
    const initialFullscreenState = firstDialog.classList.contains('fullscreen-active')
    console.log('- 初始全屏状态:', initialFullscreenState ? '全屏' : '非全屏')
    
    // 手动添加全屏样式
    console.log('- 手动添加全屏样式...')
    firstDialog.classList.add('fullscreen-active')
    
    setTimeout(() => {
      const newFullscreenState = firstDialog.classList.contains('fullscreen-active')
      console.log('- 手动添加后全屏状态:', newFullscreenState ? '全屏' : '非全屏')
      
      if (newFullscreenState) {
        console.log('✅ 全屏样式可以正常添加')
        
        // 手动移除全屏样式
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
  
  // 检查Vue组件状态
  console.log('📋 检查Vue组件状态...')
  console.log('- isFullscreen状态:', window.isFullscreen)
  
  // 检查是否有全局的toggleFullscreen函数
  if (window.toggleFullscreen) {
    console.log('✅ 找到全局toggleFullscreen函数')
  } else {
    console.log('❌ 未找到全局toggleFullscreen函数')
  }
  
}, 3000)

console.log('🔧 全屏功能诊断脚本已加载，3秒后开始检查...') 