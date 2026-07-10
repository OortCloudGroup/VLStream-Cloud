// 测试视频回放页面URL构建和播放功能
console.log('🔧 测试视频回放页面URL构建和播放功能...')

// 等待页面加载完成
setTimeout(() => {
  console.log('📋 开始测试视频回放页面URL构建')
  
  // 检查是否在视频回放页面
  const isVideoPlaybackPage = window.location.pathname.includes('VideoPlayback') || 
                             document.querySelector('.video-playback')
  
  if (!isVideoPlaybackPage) {
    console.log('⚠️ 当前不在视频回放页面，请先导航到视频回放页面')
    console.log('💡 提示：需要访问视频回放页面才能测试视频播放功能')
    return
  }
  
  console.log('✅ 当前在视频回放页面')
  
  // 检查视频播放器元素
  const videoPlayer = document.querySelector('.recorded-video-player')
  const videoContainer = document.querySelector('.recorded-video-container')
  const videoPlaceholder = document.querySelector('.video-placeholder')
  
  console.log('📋 视频播放器元素检查:')
  console.log('- recorded-video-player:', videoPlayer ? '找到' : '未找到')
  console.log('- recorded-video-container:', videoContainer ? '找到' : '未找到')
  console.log('- video-placeholder:', videoPlaceholder ? '找到' : '未找到')
  
  // 检查当前视频URL
  if (videoPlayer) {
    const currentSrc = videoPlayer.src
    console.log('📋 当前视频URL:')
    console.log('- src:', currentSrc)
    
    if (currentSrc) {
      console.log('✅ 视频URL已设置')
      
      // 测试URL格式
      if (currentSrc.includes('/api/video-record/file/')) {
        console.log('✅ URL格式正确，使用新的API路径')
      } else if (currentSrc.includes('/api/recordings/')) {
        console.log('⚠️ URL格式为旧版本，可能需要更新')
      } else {
        console.log('❌ URL格式未知')
      }
      
      // 检查URL编码
      const decodedUrl = decodeURIComponent(currentSrc)
      console.log('📋 URL解码检查:')
      console.log('- 原始URL:', currentSrc)
      console.log('- 解码后URL:', decodedUrl)
      
      // 检查中文文件名
      if (decodedUrl.includes('测试') || decodedUrl.includes('中文')) {
        console.log('✅ 中文文件名已正确编码')
      } else {
        console.log('⚠️ 未检测到中文文件名')
      }
    } else {
      console.log('⚠️ 视频URL未设置，请先选择视频')
    }
  }
  
  // 检查缩略图列表
  const thumbnailItems = document.querySelectorAll('.video-thumbnail-item')
  console.log('📋 缩略图列表检查:')
  console.log('- 缩略图数量:', thumbnailItems.length)
  
  if (thumbnailItems.length > 0) {
    console.log('✅ 找到缩略图列表')
    
    // 检查第一个缩略图的URL
    const firstThumbnail = thumbnailItems[0].querySelector('.thumbnail-img')
    if (firstThumbnail && firstThumbnail.src) {
      console.log('📋 第一个缩略图URL:')
      console.log('- src:', firstThumbnail.src)
      
      if (firstThumbnail.src.includes('/api/video-record/thumbnail/')) {
        console.log('✅ 缩略图URL格式正确')
      } else {
        console.log('⚠️ 缩略图URL格式可能不正确')
      }
    }
  } else {
    console.log('⚠️ 没有找到缩略图，请先加载视频记录')
  }
  
  // 检查视频记录数据
  const videoRecords = window.videoRecords || []
  console.log('📋 视频记录数据检查:')
  console.log('- 记录数量:', videoRecords.length)
  
  if (videoRecords.length > 0) {
    console.log('✅ 找到视频记录数据')
    
    const firstRecord = videoRecords[0]
    console.log('📋 第一条记录信息:')
    console.log('- 设备名称:', firstRecord.deviceName)
    console.log('- 文件名:', firstRecord.fileName)
    console.log('- 文件路径:', firstRecord.filePath)
    console.log('- 缩略图路径:', firstRecord.thumbnailPath)
    
    // 测试URL构建函数
    if (window.getVideoFileUrl) {
      const testUrl = window.getVideoFileUrl(firstRecord.filePath)
      console.log('📋 测试URL构建:')
      console.log('- 输入路径:', firstRecord.filePath)
      console.log('- 构建URL:', testUrl)
      
      if (testUrl && testUrl.includes('/api/video-record/file/')) {
        console.log('✅ URL构建函数工作正常')
      } else {
        console.log('❌ URL构建函数可能有问题')
      }
    } else {
      console.log('⚠️ getVideoFileUrl函数未暴露到全局')
    }
  } else {
    console.log('⚠️ 没有视频记录数据，请先加载数据')
  }
  
  // 检查视频播放状态
  if (videoPlayer) {
    console.log('📋 视频播放状态检查:')
    console.log('- readyState:', videoPlayer.readyState)
    console.log('- networkState:', videoPlayer.networkState)
    console.log('- paused:', videoPlayer.paused)
    console.log('- ended:', videoPlayer.ended)
    console.log('- error:', videoPlayer.error)
    
    if (videoPlayer.error) {
      console.log('❌ 视频播放错误:', videoPlayer.error)
    } else if (videoPlayer.readyState >= 2) {
      console.log('✅ 视频已加载')
    } else {
      console.log('⚠️ 视频正在加载中')
    }
  }
  
  // 检查网络请求
  console.log('📋 网络请求检查:')
  console.log('💡 请检查浏览器开发者工具的Network标签页，查看视频文件请求是否成功')
  console.log('💡 如果请求失败，可能需要检查后端API是否正确配置')
  
  console.log('✅ 视频回放页面URL构建测试完成')
  
}, 3000)

console.log('🔧 视频回放页面URL构建测试脚本已加载，3秒后开始检查...') 