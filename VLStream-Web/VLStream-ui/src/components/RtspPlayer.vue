<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="rtsp-player-container" :style="containerStyle">
    <!--  -->
    <div class="video-container" :class="{ 'fullscreen': isFullscreen }">
      <!-- YouTube iframe -->
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

      <!-- WebRTC element -->
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
        @playing="onPlaying"
        @error="onVideoError"
        @click="togglePlay"
      ></video>

      <!-- Load -->
      <div v-if="isConnecting && !isYouTubeStream" class="loading-overlay">
        <div class="loading-spinner"></div>
        <div class="loading-text">{{ $tp('正在连接 RTSP 流...') }}</div>
      </div>

      <!--  -->
      <div v-if="hasError && !isYouTubeStream" class="error-overlay">
        <div class="error-icon">⚠</div>
        <div class="error-text">{{ errorMessage }}</div>
        <button class="retry-button" @click="reconnect">{{ $tp('重新连接') }}</button>
      </div>

      <!-- control -->
      <div class="controls-overlay" :class="{ 'visible': controlsVisible }">
        <div class="controls-bar">
          <!-- / button -->
          <button v-if="!isYouTubeStream" class="control-button" @click="togglePlay" :disabled="!isReady">
            <span v-if="isPlaying">⏸</span>
            <span v-else>▶</span>
          </button>

          <!-- control -->
          <button v-if="!isYouTubeStream" class="control-button" @click="toggleMute">
            <span v-if="isMuted">🔇</span>
            <span v-else>🔊</span>
          </button>

          <!--  -->
          <div class="status-indicator" :class="statusClass">
            <span class="status-dot"></span>
            <span class="status-text">{{ statusText }}</span>
          </div>

          <!-- full button -->
          <button class="control-button fullscreen-btn" @click="toggleFullscreen">
            <span v-if="isFullscreen">⏷</span>
            <span v-else>⏹</span>
          </button>
        </div>
      </div>
    </div>

    <!-- info -->
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
import { translatePhrase as uiText } from '@/i18n'
import { ensureWebRTCBackendConfig, WEBRTC_SERVER_BASE_URL } from '@/api/webrtc'
import { WebrtcPreviewSession } from '@/utils/webrtcPreviewSession'

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
    default: ''
  },
  // after
  webrtcServerUrl: {
    type: String,
    default: ''
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

// data
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
const controlsVisible = ref(true)

// WebRTC related
let webrtcStreamer = null
let generation = 0

// property
const webrtcServerUrlComputed = computed(() => {
  // webrtcServer, after is webrtcServerUrl, after is value
  return props.webrtcServer || props.webrtcServerUrl || WEBRTC_SERVER_BASE_URL
})

// whether to YouTube
const isYouTubeStream = computed(() => {
  const lowerUrl = props.rtspUrl.toLowerCase()
  return lowerUrl.includes('youtube.com') || lowerUrl.includes('youtu.be')
})

// Generate YouTube URL
const youtubeEmbedUrl = computed(() => {
  if (!isYouTubeStream.value) return ''

  let url = props.rtspUrl

  // if is YouTube URL,
  if (url.includes('/embed/')) {
    return url
  }

  // if is YouTube URL, Convert to URL
  if (url.includes('youtube.com/watch?v=')) {
    const videoId = url.split('v=')[1]?.split('&')[0]
    if (videoId) {
      return `https://www.youtube.com/embed/${videoId}?autoplay=1&mute=1&controls=0&showinfo=0&rel=0&modestbranding=1`
    }
  }

  // if is YouTube
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
    return uiText('YouTube直播')
  }

  switch (connectionState.value) {
    case 'connected': return uiText('已连接')
    case 'connecting': return uiText('连接中')
    case 'failed': return uiText('连接失败')
    default: return uiText('未连接')
  }
})

const containerStyle = computed(() => {
  return {
    width: props.width ? `${props.width}px` : '100%',
    height: props.height ? `${props.height}px` : '100%'
  }
})

// method
const connectToWebRTCStreamer = async () => {
  if (!props.rtspUrl) {
    handleError('RTSP URL 不能为空')
    return
  }
  const current = ++generation
  isConnecting.value = true
  hasError.value = false
  connectionState.value = 'connecting'
  emit('connecting')
  try {
    if (!props.webrtcServer && !props.webrtcServerUrl) await ensureWebRTCBackendConfig()
    if (current !== generation || !videoElement.value) return
    const session = new WebrtcPreviewSession({
      video: videoElement.value,
      serverUrl: webrtcServerUrlComputed.value,
      onState: state => {
        if (current !== generation) return
        connectionState.value = state
        if (state === 'connected') isConnecting.value = false
      },
      onReady: () => {
        if (current !== generation) return
        isReady.value = true
        hasError.value = false
        emit('ready')
      },
      onError: message => {
        if (current === generation) handleError('连接失败: ' + message)
      }
    })
    webrtcStreamer = session
    await session.start(props.rtspUrl)
    if (current === generation) streamId.value = session.peerId
  } catch (error) {
    if (current === generation) handleError('连接失败: ' + error.message)
  }
}

const generateStreamId = (rtspUrl) => {
  // RTSP URL Generate ID
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

  // if is YouTube , Set to then
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

  // WebRTC-streamer
  await connectToWebRTCStreamer()
}

const disconnect = async () => {
  generation += 1
  const previous = webrtcStreamer
  webrtcStreamer = null
  const released = previous?.disconnect()
  if (videoElement.value) videoElement.value.srcObject = null
  isConnecting.value = false
  isPlaying.value = false
  isReady.value = false
  connectionState.value = 'closed'
  streamId.value = ''
  await released
}

const reconnect = async () => {
  await disconnect()
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
    // if is YouTube , iframe full
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

// eventProcess
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

const onPlaying = () => {
  isConnecting.value = false
  isPlaying.value = true
  emit('play')
}

const onVideoError = (event) => {
  console.error('Video error:', event)
  handleError('视频播放错误')
}

// control
let controlsTimeout = null
const handleMouseMove = () => {
  controlsVisible.value = true
  clearTimeout(controlsTimeout)
  controlsTimeout = setTimeout(() => {
    if (isPlaying.value) {
      controlsVisible.value = false
    }
  }, 3000)
}

//
onMounted(async () => {
  //
  const container = videoElement.value?.parentElement || youtubeElement.value?.parentElement
  if (container) {
    container.addEventListener('mousemove', handleMouseMove)
    container.addEventListener('mouseleave', () => {
      if (isPlaying.value) {
        controlsVisible.value = false
      }
    })
  }

  // full
  document.addEventListener('fullscreenchange', () => {
    isFullscreen.value = !!document.fullscreenElement
  })

  //
  if (props.autoplay) {
    await connect()
  }
})

onUnmounted(() => {
  disconnect()
  clearTimeout(controlsTimeout)
})

// RTSP URL
watch(() => props.rtspUrl, async (newUrl, oldUrl) => {
  if (newUrl !== oldUrl && newUrl) {
    await reconnect()
  }
})

// method component
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

/* YouTube iframe */
.youtube-video {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  background: #000;
}

/* Load */
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

/*  */
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

/* control */
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

/*  */
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

/* info */
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

/*  */
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
