<template>
  <div class="rtsp-player-container" :style="containerStyle">
    <!-- 播放器容器 -->
    <div class="video-container" :class="{ 'fullscreen': isFullscreen }">
      <!-- YouTube iframe 播放器 -->
      <iframe 
        v-if="isYouTubeStream"
        ref="youtubeElement"
        class="youtube-video"
        :src="youtubeEmbedUrl"
        frameborder="0"
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
        allowfullscreen
        @load="onYouTubeLoad"
      ></iframe>

      <!-- WebRTC 视频元素 -->
      <video 
        v-else
        ref="videoElement"
        class="rtsp-video"
        :class="{ 'connecting': isConnecting, 'error': hasError }"
        autoplay
        muted
        playsinline
        @loadstart="onLoadStart"
        @loadeddata="onLoadedData"
        @error="onVideoError"
        @click="togglePlay"
      ></video>

      <!-- 加载状态 -->
      <div v-if="isConnecting && !isYouTubeStream" class="loading-overlay">
        <div class="loading-spinner"></div>
        <div class="loading-text">正在连接 RTSP 流...</div>
      </div>

      <!-- 错误状态 -->
      <div v-if="hasError && !isYouTubeStream" class="error-overlay">
        <div class="error-icon">⚠</div>
        <div class="error-text">{{ errorMessage }}</div>
        <button class="retry-button" @click="reconnect">重新连接</button>
      </div>

      <!-- 播放控制栏 -->
      <div class="controls-overlay" :class="{ 'visible': showControls }">
        <div class="controls-bar">
          <!-- 播放/暂停按钮 -->
          <button v-if="!isYouTubeStream" class="control-button" @click="togglePlay" :disabled="!isReady">
            <span v-if="isPlaying">⏸</span>
            <span v-else>▶</span>
          </button>

          <!-- 音量控制 -->
          <button v-if="!isYouTubeStream" class="control-button" @click="toggleMute">
            <span v-if="isMuted">🔇</span>
            <span v-else>🔊</span>
          </button>

          <!-- 连接状态 -->
          <div class="status-indicator" :class="statusClass">
            <span class="status-dot"></span>
            <span class="status-text">{{ statusText }}</span>
          </div>

          <!-- 全屏按钮 -->
          <button class="control-button fullscreen-btn" @click="toggleFullscreen">
            <span v-if="isFullscreen">⏷</span>
            <span v-else>⏹</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 调试信息 -->
    <div v-if="showDebugInfo" class="debug-info">
      <div>RTSP URL: {{ rtspUrl }}</div>
      <div v-if="!isYouTubeStream">WebRTC Server: {{ webrtcServerUrlComputed }}</div>
      <div v-if="!isYouTubeStream">WebRTC State: {{ connectionState }}</div>
      <div v-if="!isYouTubeStream">Stream ID: {{ streamId }}</div>
      <div>Resolution: {{ videoResolution }}</div>
      <div v-if="isYouTubeStream">YouTube Stream: {{ isYouTubeStream }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'

// Props
const props = defineProps({
  rtspUrl: {
    type: String,
    required: true
  },
  autoplay: {
    type: Boolean,
    default: true
  },
  showControls: {
    type: Boolean,
    default: true
  },
  showDebugInfo: {
    type: Boolean,
    default: false
  },
  webrtcServer: {
    type: String,
    default: 'http://localhost:8000'
  },
  // 保持向后兼容
  webrtcServerUrl: {
    type: String,
    default: 'http://localhost:8000'
  },
  width: {
    type: Number,
    default: 800
  },
  height: {
    type: Number,
    default: 450
  }
})

// Emits
const emit = defineEmits(['play', 'pause', 'error', 'ready', 'connecting'])

// 响应式数据
const videoElement = ref(null)
const youtubeElement = ref(null)
const isConnecting = ref(false)
const isPlaying = ref(false)
const isReady = ref(false)
const hasError = ref(false)
const errorMessage = ref('')
const isMuted = ref(true)
const isFullscreen = ref(false)
const connectionState = ref('new')
const streamId = ref('')
const videoResolution = ref('')
const showControls = ref(true)

// WebRTC 相关
let webrtcStreamer = null
let pc = null

// 计算属性
const webrtcServerUrlComputed = computed(() => {
  // 优先使用 webrtcServer，然后是 webrtcServerUrl，最后是默认值
  return props.webrtcServer || props.webrtcServerUrl || 'http://localhost:8000'
})

// 检测是否为YouTube流
const isYouTubeStream = computed(() => {
  const lowerUrl = props.rtspUrl.toLowerCase()
  return lowerUrl.includes('youtube.com') || lowerUrl.includes('youtu.be')
})

// 生成YouTube嵌入URL
const youtubeEmbedUrl = computed(() => {
  if (!isYouTubeStream.value) return ''
  
  let url = props.rtspUrl
  
  // 如果是YouTube嵌入URL，直接使用
  if (url.includes('/embed/')) {
    return url
  }
  
  // 如果是普通YouTube URL，转换为嵌入URL
  if (url.includes('youtube.com/watch?v=')) {
    const videoId = url.split('v=')[1]?.split('&')[0]
    if (videoId) {
      return `https://www.youtube.com/embed/${videoId}?autoplay=1&mute=1&controls=0&showinfo=0&rel=0&modestbranding=1`
    }
  }
  
  // 如果是YouTube短链接
  if (url.includes('youtu.be/')) {
    const videoId = url.split('youtu.be/')[1]?.split('?')[0]
    if (videoId) {
      return `https://www.youtube.com/embed/${videoId}?autoplay=1&mute=1&controls=0&showinfo=0&rel=0&modestbranding=1`
    }
  }
  
  return url
})

const statusClass = computed(() => {
  if (isYouTubeStream.value) {
    return 'status-connected'
  }
  
  switch (connectionState.value) {
    case 'connected': return 'status-connected'
    case 'connecting': return 'status-connecting'
    case 'failed': return 'status-error'
    default: return 'status-disconnected'
  }
})

const statusText = computed(() => {
  if (isYouTubeStream.value) {
    return 'YouTube直播'
  }
  
  switch (connectionState.value) {
    case 'connected': return '已连接'
    case 'connecting': return '连接中'
    case 'failed': return '连接失败'
    default: return '未连接'
  }
})

const containerStyle = computed(() => {
  return {
    width: props.width ? `${props.width}px` : '100%',
    height: props.height ? `${props.height}px` : '100%'
  }
})

// 方法
const initWebRTCStreamer = () => {
  try {
    // 创建 WebRTC 配置
    const pcConfig = {
      iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
    }
    
    pc = new RTCPeerConnection(pcConfig)
    
    // 处理远程流
    pc.ontrack = (event) => {
      console.log('Received remote stream:', event.streams[0])
      if (videoElement.value) {
        videoElement.value.srcObject = event.streams[0]
        isReady.value = true
        hasError.value = false
        emit('ready')
      }
    }

    // 处理连接状态变化
    pc.onconnectionstatechange = () => {
      connectionState.value = pc.connectionState
      console.log('WebRTC connection state:', pc.connectionState)
      
      if (pc.connectionState === 'connected') {
        isConnecting.value = false
        isPlaying.value = true
        emit('play')
      } else if (pc.connectionState === 'failed') {
        handleError('WebRTC 连接失败')
      }
    }

    // 处理ICE连接状态变化
    pc.oniceconnectionstatechange = () => {
      console.log('ICE connection state:', pc.iceConnectionState)
      if (pc.iceConnectionState === 'new') {
        // 获取ICE候选 (模仿webrtc-streamer.js的行为)
        getIceCandidates()
      }
    }

    // 处理 ICE 候选
    pc.onicecandidate = (event) => {
      if (event.candidate) {
        console.log('ICE candidate:', event.candidate)
      }
    }

  } catch (error) {
    console.error('Failed to initialize WebRTC:', error)
    handleError('WebRTC 初始化失败: ' + error.message)
  }
}

const connectToWebRTCStreamer = async () => {
  if (!props.rtspUrl) {
    handleError('RTSP URL 不能为空')
    return
  }

  try {
    isConnecting.value = true
    hasError.value = false
    emit('connecting')

    // 生成 peer ID (模仿工作版本)
    const peerid = Math.random().toString()
    streamId.value = peerid
    
    // 创建 offer
    const offer = await pc.createOffer()
    await pc.setLocalDescription(offer)

    // 构建查询参数 (模仿工作版本的调用方式)
    const params = new URLSearchParams({
      peerid: peerid,
      url: props.rtspUrl,
      options: 'rtptransport=tcp&timeout=60'
    })

    // 发送 offer 到 WebRTC-streamer (使用POST + query params + SDP body)
    const response = await fetch(`${webrtcServerUrlComputed.value}/api/call?${params}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/sdp',
      },
      body: offer.sdp
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const data = await response.json()
    console.log('WebRTC-streamer response:', data)
    
    if (data.type === 'answer' && data.sdp) {
      await pc.setRemoteDescription(new RTCSessionDescription(data))
    } else {
      throw new Error('Invalid response from WebRTC streamer: ' + JSON.stringify(data))
    }

  } catch (error) {
    console.error('WebRTC connection error:', error)
    handleError('连接失败: ' + error.message)
  }
}

const getIceCandidates = async () => {
  if (!streamId.value) return

  try {
    const response = await fetch(`${webrtcServerUrlComputed.value}/api/getIceCandidate?peerid=${streamId.value}`)
    if (response.ok) {
      const candidates = await response.json()
      console.log('Received ICE candidates:', candidates)
      
      // 处理ICE候选
      if (Array.isArray(candidates)) {
        for (const candidate of candidates) {
          if (candidate && pc) {
            await pc.addIceCandidate(new RTCIceCandidate(candidate))
          }
        }
      }
    }
  } catch (error) {
    console.warn('Failed to get ICE candidates:', error)
  }
}

const generateStreamId = (rtspUrl) => {
  // 基于 RTSP URL 生成唯一的流 ID
  return btoa(rtspUrl).replace(/[^a-zA-Z0-9]/g, '').substring(0, 16)
}

const handleError = (message) => {
  isConnecting.value = false
  isPlaying.value = false
  hasError.value = true
  errorMessage.value = message
  emit('error', message)
}

const connect = async () => {
  if (isConnecting.value || isPlaying.value) return

  // 如果是YouTube流，直接设置为就绪状态
  if (isYouTubeStream.value) {
    console.log('检测到YouTube流，使用iframe播放')
    isReady.value = true
    isPlaying.value = true
    hasError.value = false
    connectionState.value = 'connected'
    emit('ready')
    emit('play')
    return
  }

  // 初始化 WebRTC
  initWebRTCStreamer()
  
  // 连接到 WebRTC-streamer
  await connectToWebRTCStreamer()
}

const disconnect = async () => {
  // 如果是YouTube流，直接重置状态
  if (isYouTubeStream.value) {
    isConnecting.value = false
    isPlaying.value = false
    isReady.value = false
    connectionState.value = 'closed'
    return
  }

  // 调用 hangup API (模仿工作版本)
  if (streamId.value) {
    try {
      await fetch(`${webrtcServerUrlComputed.value}/api/hangup?peerid=${streamId.value}`, {
        method: 'GET'
      })
      console.log('WebRTC stream hangup called for peerid:', streamId.value)
    } catch (error) {
      console.warn('Failed to call hangup API:', error)
    }
  }

  if (pc) {
    pc.close()
    pc = null
  }
  
  if (videoElement.value) {
    videoElement.value.srcObject = null
  }

  isConnecting.value = false
  isPlaying.value = false
  isReady.value = false
  connectionState.value = 'closed'
  streamId.value = ''
}

const reconnect = async () => {
  disconnect()
  await new Promise(resolve => setTimeout(resolve, 1000)) // 等待 1 秒
  await connect()
}

const togglePlay = () => {
  if (!isReady.value) return

  if (isPlaying.value) {
    videoElement.value?.pause()
    isPlaying.value = false
    emit('pause')
  } else {
    videoElement.value?.play()
    isPlaying.value = true
    emit('play')
  }
}

const toggleMute = () => {
  if (videoElement.value) {
    videoElement.value.muted = !videoElement.value.muted
    isMuted.value = videoElement.value.muted
  }
}

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    // 如果是YouTube流，使用iframe的全屏
    if (isYouTubeStream.value && youtubeElement.value) {
      youtubeElement.value.requestFullscreen()
    } else {
      videoElement.value?.requestFullscreen()
    }
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

// 事件处理
const onLoadStart = () => {
  console.log('Video load started')
}

const onLoadedData = () => {
  console.log('Video data loaded')
  if (videoElement.value) {
    const video = videoElement.value
    videoResolution.value = `${video.videoWidth}x${video.videoHeight}`
  }
}

const onVideoError = (event) => {
  console.error('Video error:', event)
  handleError('视频播放错误')
}

// 控制栏显示逻辑
let controlsTimeout = null
const handleMouseMove = () => {
  showControls.value = true
  clearTimeout(controlsTimeout)
  controlsTimeout = setTimeout(() => {
    if (isPlaying.value) {
      showControls.value = false
    }
  }, 3000)
}

// 生命周期
onMounted(async () => {
  // 添加鼠标移动监听
  const container = videoElement.value?.parentElement || youtubeElement.value?.parentElement
  if (container) {
    container.addEventListener('mousemove', handleMouseMove)
    container.addEventListener('mouseleave', () => {
      if (isPlaying.value) {
        showControls.value = false
      }
    })
  }

  // 全屏状态监听
  document.addEventListener('fullscreenchange', () => {
    isFullscreen.value = !!document.fullscreenElement
  })

  // 自动播放
  if (props.autoplay) {
    await connect()
  }
})

onUnmounted(() => {
  disconnect()
  clearTimeout(controlsTimeout)
})

// 监听 RTSP URL 变化
watch(() => props.rtspUrl, async (newUrl, oldUrl) => {
  if (newUrl !== oldUrl && newUrl) {
    await reconnect()
  }
})

// 暴露方法给父组件
defineExpose({
  connect,
  disconnect,
  reconnect,
  togglePlay,
  toggleFullscreen
})
</script>

<style scoped>
.rtsp-player-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.video-container {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
}

.video-container.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
}

.rtsp-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
  transition: opacity 0.3s ease;
}

.rtsp-video.connecting {
  opacity: 0.7;
}

.rtsp-video.error {
  opacity: 0.3;
}

/* YouTube iframe 样式 */
.youtube-video {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  background: #000;
}

/* 加载状态 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  z-index: 10;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: #1A53FF;
  animation: spin 1s ease-in-out infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

/* 错误状态 */
.error-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.9);
  color: white;
  z-index: 10;
}

.error-icon {
  font-size: 48px;
  color: #ff4757;
  margin-bottom: 16px;
}

.error-text {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 20px;
  text-align: center;
}

.retry-button {
  padding: 8px 16px;
  background: #1A53FF;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.retry-button:hover {
  background: #3d70ff;
}

/* 控制栏 */
.controls-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 20;
}

.controls-overlay.visible {
  opacity: 1;
}

.controls-bar {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  gap: 12px;
}

.control-button {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: background-color 0.2s;
}

.control-button:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.3);
}

.control-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.fullscreen-btn {
  margin-left: auto;
}

/* 状态指示器 */
.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  margin-right: 12px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff4757;
}

.status-indicator.status-connected .status-dot {
  background: #2ed573;
}

.status-indicator.status-connecting .status-dot {
  background: #ffa502;
  animation: pulse 1.5s ease-in-out infinite;
}

.status-indicator.status-error .status-dot {
  background: #ff4757;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.9);
}

/* 调试信息 */
.debug-info {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  font-family: monospace;
  z-index: 30;
}

.debug-info div {
  margin-bottom: 4px;
}

.debug-info div:last-child {
  margin-bottom: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .controls-bar {
    padding: 8px 12px;
    gap: 8px;
  }
  
  .control-button {
    width: 32px;
    height: 32px;
    font-size: 12px;
  }
  
  .status-text {
    display: none;
  }
}
</style> 