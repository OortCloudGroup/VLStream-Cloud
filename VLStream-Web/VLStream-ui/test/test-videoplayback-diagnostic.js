// 详细诊断视频回放页面播放问题
console.log('🔧 详细诊断视频回放页面播放问题...')

// 等待页面加载完成
setTimeout(() => {
  console.log('📋 开始详细诊断')
  
  // 检查是否在视频回放页面
  const isVideoPlaybackPage = window.location.pathname.includes('VideoPlayback') || 
                             document.querySelector('.video-playback')
  
  if (!isVideoPlaybackPage) {
    console.log('⚠️ 当前不在视频回放页面')
    return
  }
  
  console.log('✅ 当前在视频回放页面')
  
  // 检查视频播放器
  const videoPlayer = document.querySelector('.recorded-video-player')
  if (!videoPlayer) {
    console.log('❌ 未找到视频播放器')
    return
  }
  
  console.log('📋 视频播放器详细信息:')
  console.log('- src:', videoPlayer.src)
  console.log('- currentSrc:', videoPlayer.currentSrc)
  console.log('- readyState:', videoPlayer.readyState)
  console.log('- networkState:', videoPlayer.networkState)
  console.log('- error:', videoPlayer.error)
  
  if (videoPlayer.error) {
    console.log('❌ 视频播放错误详情:')
    console.log('- error.code:', videoPlayer.error.code)
    console.log('- error.message:', videoPlayer.error.message)
    
    // 错误代码说明
    const errorMessages = {
      1: 'MEDIA_ERR_ABORTED - 用户中止了视频加载',
      2: 'MEDIA_ERR_NETWORK - 网络错误',
      3: 'MEDIA_ERR_DECODE - 视频解码错误',
      4: 'MEDIA_ERR_SRC_NOT_SUPPORTED - 视频格式不支持或文件损坏'
    }
    
    const errorMessage = errorMessages[videoPlayer.error.code] || '未知错误'
    console.log('- 错误说明:', errorMessage)
  }
  
  // 测试网络请求
  if (videoPlayer.src) {
    console.log('🧪 测试网络请求...')
    
    // 创建一个测试请求
    fetch(videoPlayer.src, { method: 'HEAD' })
      .then(response => {
        console.log('📋 网络请求结果:')
        console.log('- status:', response.status)
        console.log('- statusText:', response.statusText)
        console.log('- ok:', response.ok)
        console.log('- headers:', Object.fromEntries(response.headers.entries()))
        
        if (response.ok) {
          console.log('✅ 网络请求成功')
          
          // 检查Content-Type
          const contentType = response.headers.get('content-type')
          console.log('- Content-Type:', contentType)
          
          if (contentType && contentType.includes('video/')) {
            console.log('✅ Content-Type正确')
          } else {
            console.log('⚠️ Content-Type可能不正确:', contentType)
          }
          
          // 检查Content-Length
          const contentLength = response.headers.get('content-length')
          console.log('- Content-Length:', contentLength)
          
          if (contentLength && parseInt(contentLength) > 0) {
            console.log('✅ 文件大小正常')
          } else {
            console.log('⚠️ 文件大小可能有问题')
          }
        } else {
          console.log('❌ 网络请求失败')
        }
      })
      .catch(error => {
        console.log('❌ 网络请求异常:', error)
      })
  }
  
  // 检查CORS设置
  console.log('📋 CORS检查:')
  console.log('💡 如果网络请求失败，可能是CORS问题')
  console.log('💡 后端需要设置正确的CORS头:')
  console.log('   - Access-Control-Allow-Origin: *')
  console.log('   - Access-Control-Allow-Methods: GET, HEAD')
  console.log('   - Access-Control-Allow-Headers: Range')
  
  // 检查视频格式支持
  console.log('📋 浏览器视频格式支持检查:')
  const video = document.createElement('video')
  const formats = ['video/mp4', 'video/webm', 'video/ogg']
  
  formats.forEach(format => {
    const canPlay = video.canPlayType(format)
    console.log(`- ${format}: ${canPlay}`)
  })
  
  // 建议解决方案
  console.log('📋 建议解决方案:')
  console.log('1. 检查后端API是否正确配置')
  console.log('2. 确认视频文件路径是否正确')
  console.log('3. 检查CORS设置')
  console.log('4. 确认Content-Type头设置正确')
  console.log('5. 检查视频文件是否损坏')
  
  // 测试直接访问URL
  if (videoPlayer.src) {
    console.log('🧪 测试直接访问视频URL...')
    console.log('💡 请在浏览器新标签页中打开以下URL测试:')
    console.log(videoPlayer.src)
  }
  
}, 3000)

console.log('🔧 详细诊断脚本已加载，3秒后开始检查...') 