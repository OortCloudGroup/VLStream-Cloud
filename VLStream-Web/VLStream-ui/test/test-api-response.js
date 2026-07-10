// 测试API响应，检查后端视频文件处理
console.log('🔧 测试API响应，检查后端视频文件处理...')

// 等待页面加载完成
setTimeout(() => {
  console.log('📋 开始测试API响应')
  
  // 测试视频文件API
  const testVideoUrl = 'http://localhost:18080/api/recordings/2025%2F07%2F28%2F%E6%B5%8B%E8%AF%95127_14_20250728_173835.mp4'
  const testThumbnailUrl = 'http://localhost:18080/api/recordings/2025%2F07%2F28%2F%E6%B5%8B%E8%AF%95127_14_20250728_173835_thumbnail.jpg'
  
  console.log('📋 测试视频文件API:')
  console.log('- URL:', testVideoUrl)
  
  // 测试视频文件请求
  fetch(testVideoUrl, { method: 'HEAD' })
    .then(response => {
      console.log('📋 视频文件API响应:')
      console.log('- status:', response.status)
      console.log('- statusText:', response.statusText)
      console.log('- ok:', response.ok)
      console.log('- headers:', Object.fromEntries(response.headers.entries()))
      
      if (response.ok) {
        console.log('✅ 视频文件API请求成功')
        
        const contentType = response.headers.get('content-type')
        const contentLength = response.headers.get('content-length')
        
        console.log('- Content-Type:', contentType)
        console.log('- Content-Length:', contentLength)
        
        if (contentType && contentType.includes('video/')) {
          console.log('✅ Content-Type正确')
        } else {
          console.log('⚠️ Content-Type可能不正确:', contentType)
        }
        
        if (contentLength && parseInt(contentLength) > 0) {
          console.log('✅ 文件大小正常')
        } else {
          console.log('⚠️ 文件大小可能有问题')
        }
      } else {
        console.log('❌ 视频文件API请求失败')
        
        if (response.status === 404) {
          console.log('💡 文件不存在，检查路径是否正确')
        } else if (response.status === 400) {
          console.log('💡 请求参数错误，检查URL编码')
        } else if (response.status === 500) {
          console.log('💡 服务器内部错误，检查后端日志')
        }
      }
    })
    .catch(error => {
      console.log('❌ 视频文件API请求异常:', error)
    })
  
  // 测试缩略图API
  console.log('📋 测试缩略图API:')
  console.log('- URL:', testThumbnailUrl)
  
  fetch(testThumbnailUrl, { method: 'HEAD' })
    .then(response => {
      console.log('📋 缩略图API响应:')
      console.log('- status:', response.status)
      console.log('- statusText:', response.statusText)
      console.log('- ok:', response.ok)
      console.log('- headers:', Object.fromEntries(response.headers.entries()))
      
      if (response.ok) {
        console.log('✅ 缩略图API请求成功')
        
        const contentType = response.headers.get('content-type')
        const contentLength = response.headers.get('content-length')
        
        console.log('- Content-Type:', contentType)
        console.log('- Content-Length:', contentLength)
        
        if (contentType && (contentType.includes('image/') || contentType.includes('video/'))) {
          console.log('✅ Content-Type正确')
        } else {
          console.log('⚠️ Content-Type可能不正确:', contentType)
        }
      } else {
        console.log('❌ 缩略图API请求失败')
        
        if (response.status === 404) {
          console.log('💡 缩略图文件不存在，可能需要生成缩略图')
        }
      }
    })
    .catch(error => {
      console.log('❌ 缩略图API请求异常:', error)
    })
  
  // 测试不同的URL格式
  console.log('📋 测试不同的URL格式...')
  
  const testUrls = [
    // 原始格式
    'http://localhost:18080/api/recordings/2025/07/28/测试127_14_20250728_173835.mp4',
    // URL编码格式
    'http://localhost:18080/api/recordings/2025%2F07%2F28%2F%E6%B5%8B%E8%AF%95127_14_20250728_173835.mp4',
    // 部分编码格式
    'http://localhost:18080/api/recordings/2025/07/28/%E6%B5%8B%E8%AF%95127_14_20250728_173835.mp4'
  ]
  
  testUrls.forEach((url, index) => {
    console.log(`🧪 测试URL格式 ${index + 1}:`, url)
    
    fetch(url, { method: 'HEAD' })
      .then(response => {
        console.log(`- 格式 ${index + 1} 状态:`, response.status)
        if (response.ok) {
          console.log(`✅ 格式 ${index + 1} 成功`)
        }
      })
      .catch(error => {
        console.log(`❌ 格式 ${index + 1} 失败:`, error.message)
      })
  })
  
  // 检查后端API配置建议
  console.log('📋 后端API配置建议:')
  console.log('1. 确保后端有 /api/recordings/* 的路由配置')
  console.log('2. 检查文件路径解析是否正确')
  console.log('3. 确认视频文件实际存在')
  console.log('4. 检查CORS配置')
  console.log('5. 确认Content-Type头设置正确')
  
  // 检查可能的文件路径
  console.log('📋 可能的文件路径:')
  console.log('- 绝对路径: F:\\work\\vls-tr\\VLStream-server\\recordings\\2025\\07\\28\\测试127_14_20250728_173835.mp4')
  console.log('- 相对路径: recordings/2025/07/28/测试127_14_20250728_173835.mp4')
  console.log('- URL路径: /api/recordings/2025/07/28/测试127_14_20250728_173835.mp4')
  
}, 3000)

console.log('🔧 API响应测试脚本已加载，3秒后开始检查...') 