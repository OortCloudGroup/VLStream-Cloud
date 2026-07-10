<template>
  <div 
    class="layout-dialog-overlay"
    :style="{ zIndex: 3000 + dialog.id }"
  >
    <div 
      class="layout-dialog draggable-dialog"
      :style="{ 
        left: dialog.position.x + 'px', 
        top: dialog.position.y + 'px',
        position: 'fixed',
        transform: 'none'
      }"
      @mousedown="bringToFront"
    >
      <div 
        class="layout-dialog-header draggable-handle"
        @mousedown="startDrag"
      >
        <span class="layout-dialog-title">{{ getLayoutTitle(dialog.layoutCount) }} - 视频播放</span>
        <div class="dialog-controls">
          <button class="dialog-control-btn minimize-btn" @click="minimize" title="最小化">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 13H5v-2h14v2z"/>
            </svg>
          </button>
          <button class="dialog-control-btn close-btn" @click="close" title="关闭">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
          </button>
        </div>
      </div>
      
      <div class="layout-dialog-content" v-show="!dialog.minimized">
        <div class="dialog-main-content">
          <!-- 视频播放区域 -->
          <div class="video-player-section">
            <div class="video-area">
              <!-- 视频网格布局 -->
              <div class="video-grid" :class="`layout-${getLayoutClass(dialog.layoutCount)}`">
                <div 
                  v-for="index in dialog.layoutCount" 
                  :key="`window-${index}`"
                  class="video-window"
                  :class="{ 
                    'dragging': draggedWindowIndex === index - 1,
                    'drag-over': dragOverWindowIndex === index - 1
                  }"
                  :data-window-index="index - 1"
                  :data-dialog-id="dialog.id"
                  draggable="true"
                  @click="handleVideoWindowClick(index - 1)"
                  @dblclick="handleVideoWindowDoubleClick(index - 1)"
                  @dragstart="handleDragStart($event, index - 1)"
                  @dragover.prevent="handleDragOver($event, index - 1)"
                  @dragenter.prevent="handleDragEnter($event, index - 1)"
                  @dragleave="handleDragLeave($event, index - 1)"
                  @drop="handleDrop($event, index - 1)"
                  @dragend="handleDragEnd"
                >
                  <!-- 拖拽遮罩层 - 确保所有窗口都能被选中和拖拽 -->
                  <div class="drag-overlay" 
                       @click.stop="handleVideoWindowClick(index - 1)"
                       @dblclick.stop="handleVideoWindowDoubleClick(index - 1)"
                       @dragstart.stop="handleDragStart($event, index - 1)"
                       @dragover.stop.prevent="handleDragOver($event, index - 1)"
                       @dragenter.stop.prevent="handleDragEnter($event, index - 1)"
                       @dragleave.stop="handleDragLeave($event, index - 1)"
                       @drop.stop="handleDrop($event, index - 1)"
                       @dragend.stop="handleDragEnd">
                  </div>
                  <!-- 如果有对应的摄像头数据，显示视频内容 -->
                  <template v-if="dialog.cameras[index - 1]">
                    <!-- WebRTC direct playback -->
                    <div v-if="dialog.cameras[index - 1].deviceData && dialog.cameras[index - 1].deviceData.streamUrl && (dialog.cameras[index - 1].deviceData.playMode === 'webrtc' || !dialog.cameras[index - 1].deviceData.playMode) && !isCameraRtcStream(dialog.cameras[index - 1].deviceData.streamUrl)" class="webrtc-direct-container">
                      <video
                        class="webrtc-direct-video"
                        controls
                        autoplay
                        muted
                        playsinline
                        :ref="el => setWebrtcVideoRef(index - 1, el)"
                      ></video>
                      <div class="webrtc-info">
                        <small>WebRTC - {{ dialog.cameras[index - 1].name }}</small>
                      </div>
                    </div>

                    <!-- CameraRTC playback -->
                    
                    <div v-else-if="dialog.cameras[index - 1].deviceData && dialog.cameras[index - 1].deviceData.streamUrl && (dialog.cameras[index - 1].deviceData.playMode === 'cameraRTC' || isCameraRtcStream(dialog.cameras[index - 1].deviceData.streamUrl))" class="camera-rtc-container">
                      <div class="camera-rtc-player" :ref="el => setCameraRtcContainerRef(index - 1, el)"></div>
                    </div>

                    <!-- RTSP playback -->
                    <div v-else-if="dialog.cameras[index - 1].deviceData && dialog.cameras[index - 1].deviceData.streamUrl && (dialog.cameras[index - 1].deviceData.playMode === 'direct' || dialog.cameras[index - 1].deviceData.playMode === 'hls')" class="rtsp-player-container">
                      <RtspPlayer 
                        :rtsp-url="dialog.cameras[index - 1].deviceData.streamUrl || dialog.cameras[index - 1].deviceData.originalRtspUrl"
                        width="100%"
                        height="100%"
                        @connected="handleRtspConnected"
                        @disconnected="handleRtspDisconnected"
                        @error="handleRtspError"
                      />
                    </div>

                    <div v-else class="video-placeholder">
                      <div class="placeholder-content">
                        <div class="placeholder-icon">📹</div>
                        <div class="placeholder-text">暂无视频流</div>
                        <div class="placeholder-details">
                          <div>设备: {{ dialog.cameras[index - 1].name }}</div>
                          <div>状态: 在线</div>
                        </div>
                        
                        <!-- 备用操作选项 -->
                        <div class="placeholder-actions">
                          <button class="action-btn primary" @click="retryWebRTCConnection(dialog.cameras[index - 1])">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
                            </svg>
                            重试连接
                          </button>
                          <button class="action-btn secondary" @click="copyStreamUrl(dialog.cameras[index - 1].deviceData?.streamUrl)">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M16 1H4C2.9 1 2 1.9 2 3v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/>
                            </svg>
                            复制地址
                          </button>
                          <button class="action-btn secondary" @click="openInVlc(dialog.cameras[index - 1].deviceData?.streamUrl)">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z"/>
                            </svg>
                            VLC播放
                          </button>
                        </div>
                      </div>
                    </div>
                  </template>
                  
                  <!-- 没有对应摄像头数据时显示空白占位符 -->
                  <template v-else>
                    <div class="video-placeholder empty-placeholder">
                      <div class="placeholder-content">
                        <div class="placeholder-icon">📺</div>
                        <div class="placeholder-text">空白窗口</div>
                        <div class="placeholder-details">
                          <div>位置: {{ index }}</div>
                          <div>状态: 待分配</div>
                        </div>
                        
                        <!-- 添加设备选项 -->
                        <div class="placeholder-actions">
                          <button class="action-btn primary" @click="addDeviceToWindow(index - 1)">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
                            </svg>
                            添加设备
                          </button>
                        </div>
                      </div>
                    </div>
                  </template>
                </div>
              </div>
              
              <!-- 录像控制区域 -->
              <div class="video-controls">
                <!-- 录像状态显示 -->
                <div v-if="dialog.recording?.isRecording" class="recording-status">
                  <div class="recording-indicator">
                    <span class="recording-dot"></span>
                    录制中 {{ dialog.recording.currentRecordTime }}
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- PTZ控制面板区域 -->
          <div class="ptz-control-section">
            <PTZControl 
              :show-camera-management="true"
              @ptz-control="handlePTZControl"
              @zoom-control="handleZoomControl"
              @control-action="handleControlAction"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import RtspPlayer from './RtspPlayer.vue'
import PTZControl from './PTZControl.vue'
import { WEBRTC_SERVER_BASE_URL } from '@/api/webrtc'
import { CAMERA_RTC_SOCKET_URL, ensureOPlayer, isCameraRtcStream } from '@/utils/oplayer'

// Props
const props = defineProps({
  dialog: {
    type: Object,
    required: true
  }
})

// Emits
const emit = defineEmits([
  'close',
  'minimize',
  'bring-to-front',
  'start-drag',
  'webrtc-load',
  'webrtc-error',
  'rtsp-connected',
  'rtsp-disconnected',
  'rtsp-error',
  'ptz-control',
  'zoom-control',
  'control-action',
  'retry-webrtc',
  'copy-stream-url',
  'open-in-vlc',
  'add-device',
  'video-window-double-click',
  'video-window-swap'
])

// 获取布局类名
const getLayoutClass = (count) => {
  const classes = {
    1: '1x1',
    4: '2x2',
    6: '3x3',
    8: '4x4',
    9: '5x5',
    16: '6x6',
    22: '6x6', // 二十二画面使用6x6布局
    24: '6x6', // 二十四画面使用6x6布局
    25: '6x6'  // 二十五画面使用6x6布局
  }
  return classes[count] || '1x1'
}

// 获取布局标题
const getLayoutTitle = (count) => {
  const titleMap = {
    1: '单画面',
    4: '四分屏',
    6: '六分屏',
    8: '八分屏',
    9: '九分屏',
    16: '十六分屏',
    22: '二十二画面',
    24: '二十四画面',
    25: '二十五画面'
  }
  return titleMap[count] || '视频播放'
}

// 事件处理函数
const close = () => {
  emit('close', props.dialog.id)
}

const minimize = () => {
  emit('minimize', props.dialog.id)
}

const bringToFront = () => {
  emit('bring-to-front', props.dialog.id)
}

const startDrag = (event) => {
  emit('start-drag', event, props.dialog.id)
}

const handleRtspConnected = () => {
  emit('rtsp-connected')
}

const handleRtspDisconnected = () => {
  emit('rtsp-disconnected')
}

const handleRtspError = () => {
  emit('rtsp-error')
}

const handlePTZControl = (data) => {
  emit('ptz-control', data)
}

const handleZoomControl = (data) => {
  emit('zoom-control', data)
}

const handleControlAction = (data) => {
  emit('control-action', data)
}

const retryWebRTCConnection = (camera) => {
  emit('retry-webrtc', camera)
}

const copyStreamUrl = (url) => {
  emit('copy-stream-url', url)
}

const openInVlc = (url) => {
  emit('open-in-vlc', url)
}

const addDeviceToWindow = (windowIndex) => {
  emit('add-device', props.dialog.id, windowIndex)
}


// WebRTC direct playback (similar to DeviceManagement.vue)
const WEBRTC_STREAMER_BASE = WEBRTC_SERVER_BASE_URL
const WEBRTC_SCRIPT_URLS = [
  `${WEBRTC_STREAMER_BASE}/libs/adapter.min.js`,
  `${WEBRTC_STREAMER_BASE}/webrtcstreamer.js`
]
let webrtcScriptLoader = null
const webrtcPlayers = ref(new Map())
const webrtcVideoRefs = ref(new Map())
const cameraRtcPlayers = ref(new Map())
const cameraRtcContainerRefs = ref(new Map())

const loadScriptTag = (src) => {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${src}"]`)) {
      resolve()
      return
    }

    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`Failed to load ${src}`))
    document.head.appendChild(script)
  })
}

const ensureWebRtcStreamerScripts = async () => {
  if (webrtcScriptLoader) {
    return webrtcScriptLoader
  }

  webrtcScriptLoader = Promise.all(WEBRTC_SCRIPT_URLS.map(loadScriptTag)).catch(error => {
    webrtcScriptLoader = null
    throw error
  })

  return webrtcScriptLoader
}

const setWebrtcVideoRef = (windowIndex, el) => {
  if (el) {
    webrtcVideoRefs.value.set(windowIndex, el)
  } else {
    cleanupWebrtcPlayer(windowIndex)
    webrtcVideoRefs.value.delete(windowIndex)
  }
}

const setCameraRtcContainerRef = (windowIndex, el) => {
  if (el) {
    cameraRtcContainerRefs.value.set(windowIndex, el)
  } else {
    cleanupCameraRtcPlayer(windowIndex)
    cameraRtcContainerRefs.value.delete(windowIndex)
  }
}

const cleanupWebrtcPlayer = async (windowIndex) => {
  const player = webrtcPlayers.value.get(windowIndex)
  if (player) {
    try {
      if (typeof player.disconnect === 'function') {
        await player.disconnect()
      } else if (typeof player.stop === 'function') {
        await player.stop()
      }
    } catch (error) {
      console.warn('Failed to stop WebRTC playback:', error)
    }
    webrtcPlayers.value.delete(windowIndex)
  }

  const videoEl = webrtcVideoRefs.value.get(windowIndex)
  if (videoEl) {
    videoEl.srcObject = null
    videoEl.onerror = null
    videoEl.onloadeddata = null
  }
}

const cleanupCameraRtcPlayer = (windowIndex) => {
  const player = cameraRtcPlayers.value.get(windowIndex)
  if (player?.compInstance?.$destroy) {
    player.compInstance.$destroy()
  }
  cameraRtcPlayers.value.delete(windowIndex)

  const container = cameraRtcContainerRefs.value.get(windowIndex)
  if (container) {
    container.innerHTML = ''
  }
}

const cleanupAllWebrtcPlayers = async () => {
  const keys = Array.from(webrtcPlayers.value.keys())
  for (const key of keys) {
    await cleanupWebrtcPlayer(key)
  }
}

const cleanupAllCameraRtcPlayers = () => {
  const keys = Array.from(cameraRtcPlayers.value.keys())
  for (const key of keys) {
    cleanupCameraRtcPlayer(key)
  }
}

const playWebrtcStream = async (camera, windowIndex) => {
  try {
    const streamUrl = camera?.deviceData?.streamUrl ||
      camera?.deviceData?.originalRtspUrl ||
      camera?.deviceData?.rtspUrl

    if (!streamUrl) {
      throw new Error('Missing stream url')
    }

    await ensureWebRtcStreamerScripts()
    await nextTick()

    const videoEl = webrtcVideoRefs.value.get(windowIndex)
    if (!videoEl) {
      throw new Error('WebRTC video element not ready')
    }

    if (!window.WebRtcStreamer) {
      throw new Error('WebRtcStreamer not loaded')
    }

    if (!videoEl.id) {
      videoEl.id = `layout-webrtc-${props.dialog.id}-${windowIndex}`
    }

    await cleanupWebrtcPlayer(windowIndex)

    const player = new window.WebRtcStreamer(videoEl.id, WEBRTC_STREAMER_BASE)
    webrtcPlayers.value.set(windowIndex, player)

    camera.deviceData.playMode = 'webrtc'

    player.onconnected = () => emit('webrtc-load')
    player.onerror = () => emit('webrtc-error')

    if (typeof player.connect === 'function') {
      player.connect(streamUrl, '', 'rtptransport=tcp&timeout=60')
    } else if (typeof player.play === 'function') {
      player.play(streamUrl)
    } else {
      throw new Error('WebRtcStreamer missing connect/play')
    }
  } catch (error) {
    console.error('WebRTC direct playback failed:', error)
    emit('webrtc-error')
  }
}


const playCameraRtcStream = async (camera, windowIndex) => {
  try {
    const deviceId = camera?.deviceData?.deviceId || camera?.deviceData?.id
    if (!deviceId) {
      throw new Error('CameraRTC requires deviceId')
    }

    await ensureOPlayer()
    await nextTick()

    const container = cameraRtcContainerRefs.value.get(windowIndex)
    if (!container) {
      throw new Error('CameraRTC container not ready')
    }

    cleanupCameraRtcPlayer(windowIndex)

    const player = new window.OToolBox.OPlayer(container, {
      debuggerMode: false,
      autoSize: true,
      backgroundColor: '#000000',
      showHeader: true,
      webRTCSocketURL: CAMERA_RTC_SOCKET_URL
    })
    cameraRtcPlayers.value.set(windowIndex, player)

    camera.deviceData.playMode = 'cameraRTC'
    player.play({
      type: 'cameraRTC',
      src: String(deviceId),
      name: camera?.name || ''
    })
  } catch (error) {
    console.error('CameraRTC playback failed:', error)
  }
}

const syncWebrtcPlayers = async () => {
  await nextTick()
  const cameras = props.dialog?.cameras || []

  cameras.forEach((camera, index) => {
    const streamUrl = camera?.deviceData?.streamUrl ||
      camera?.deviceData?.originalRtspUrl ||
      camera?.deviceData?.rtspUrl

    const isCameraRtc = isCameraRtcStream(streamUrl)

    const shouldPlayCameraRtc = !!(camera && streamUrl && isCameraRtc)
    if (shouldPlayCameraRtc) {
      playCameraRtcStream(camera, index)
    } else {
      cleanupCameraRtcPlayer(index)
    }

    const shouldPlayWebrtc = !!(camera &&
      streamUrl &&
      !isCameraRtc &&
      (camera.deviceData?.playMode === 'webrtc' || !camera.deviceData?.playMode) &&
      !streamUrl.includes('youtube.com'))

    if (shouldPlayWebrtc) {
      playWebrtcStream(camera, index)
    } else {
      cleanupWebrtcPlayer(index)
    }
  })
}


watch(
  () => (props.dialog?.cameras || []).map(cam => cam
    ? `${cam?.id || cam?.deviceData?.id || ''}:${cam?.deviceData?.streamUrl || ''}:${cam?.deviceData?.playMode || ''}`
    : 'empty'),
  () => {
    syncWebrtcPlayers()
  },
  { deep: true, immediate: true }
)

onMounted(() => {
  syncWebrtcPlayers()
})

onBeforeUnmount(() => {
  cleanupAllWebrtcPlayers()
  cleanupAllCameraRtcPlayers()
})

// 拖拽状态管理
const draggedWindowIndex = ref(null)
const dragOverWindowIndex = ref(null)

// 视频窗口点击处理
const handleVideoWindowClick = (windowIndex) => {
  emit('video-window-click', props.dialog.id, windowIndex)
}

// 视频窗口双击处理
const handleVideoWindowDoubleClick = (windowIndex) => {
  emit('video-window-double-click', props.dialog.id, windowIndex)
}

// 拖拽开始
const handleDragStart = (event, windowIndex) => {
  const dialog = props.dialog
  if (!dialog) {
    event.preventDefault()
    return
  }
  
  // 设置拖拽状态
  draggedWindowIndex.value = windowIndex
  
  // 设置拖拽数据
  event.dataTransfer.effectAllowed = 'move'
  try {
    event.dataTransfer.setData('text/plain', JSON.stringify({
      dialogId: dialog.id,
      windowIndex,
      cameraData: dialog.cameras[windowIndex] || null
    }))
  } catch (e) {
    console.error('设置拖拽数据失败:', e)
    event.preventDefault()
    return
  }
  
  console.log('开始拖拽窗口:', dialog.id, windowIndex)
}

// 拖拽结束
const handleDragEnd = (event) => {
  console.log('窗口拖拽结束')
  
  // 清理拖拽状态
  draggedWindowIndex.value = null
  dragOverWindowIndex.value = null
}

// 拖拽悬停
const handleDragOver = (event, windowIndex) => {
  event.preventDefault()
  event.dataTransfer.dropEffect = 'move'
}

// 拖拽进入
const handleDragEnter = (event, windowIndex) => {
  event.preventDefault()
  dragOverWindowIndex.value = windowIndex
}

// 拖拽离开
const handleDragLeave = (event, windowIndex) => {
  // 确保鼠标真正离开了元素
  const rect = event.currentTarget.getBoundingClientRect()
  const x = event.clientX
  const y = event.clientY
  
  if (x < rect.left || x > rect.right || y < rect.top || y > rect.bottom) {
    dragOverWindowIndex.value = null
  }
}

// 拖拽放置
const handleDrop = (event, targetWindowIndex) => {
  event.preventDefault()
  event.stopPropagation()
  
  try {
    const data = event.dataTransfer.getData('text/plain')
    if (!data) {
      throw new Error('无拖拽数据')
    }
    const dragData = JSON.parse(data)
    const sourceDialogId = dragData.dialogId
    const sourceWindowIndex = dragData.windowIndex
    
    // 如果是同一个窗口，不处理
    if (sourceDialogId === props.dialog.id && sourceWindowIndex === targetWindowIndex) {
      return
    }
    
    // 执行窗口交换
    emit('video-window-swap', sourceDialogId, sourceWindowIndex, props.dialog.id, targetWindowIndex)
    
    console.log('窗口交换完成:', {
      from: { dialogId: sourceDialogId, windowIndex: sourceWindowIndex },
      to: { dialogId: props.dialog.id, windowIndex: targetWindowIndex }
    })
    
  } catch (e) {
    console.error('处理拖拽放置失败:', e)
  }
  
  // 清理拖拽状态
  draggedWindowIndex.value = null
  dragOverWindowIndex.value = null
}
</script>

<style scoped>
/* 布局弹窗样式 */
.layout-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none;
  z-index: 3000;
}

.layout-dialog {
  position: fixed;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  border: 1px solid #e4e7ed;
  min-width: 1400px;
  min-height: 900px;
  max-height: 95vh;
  pointer-events: auto;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.layout-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
  cursor: move;
  user-select: none;
}

.layout-dialog-title {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.dialog-controls {
  display: flex;
  gap: 8px;
}

.dialog-control-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  transition: all 0.2s ease;
}

.dialog-control-btn:hover {
  background: #e4e7ed;
  color: #303133;
}

.dialog-control-btn.close-btn:hover {
  background: #f56c6c;
  color: white;
}

.layout-dialog-content {
  flex: 1;
  overflow: hidden;
}

.dialog-main-content {
  display: flex;
  gap: 20px;
  padding: 24px;
  height: calc(100% - 60px);
  align-items: flex-start;
  min-width: 1200px;
  overflow: hidden;
}

.video-player-section {
  display: flex;
  flex-direction: column;
  background: transparent;
  border-radius: 8px;
  overflow: hidden;
  width: 1140px;
  flex: 1;
  min-height: 0;
}

.video-area {
  width: 100%;
  height: 100%;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  flex: 1;
  min-height: 0;
  max-height: none;
}

.video-grid {
  display: grid;
  gap: 8px;
  width: 100%;
  flex: 1;
  min-height: 762px;
  height: calc(100% - 48px);
}

/* 布局类样式 */
.video-grid.layout-1x1 {
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
}

.video-grid.layout-2x2 {
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
}

/* 6分屏L形布局：主屏占据4个位置，5个小屏围绕在右侧和底部 */
.video-grid.layout-3x3 {
  grid-template-columns: 1fr 1fr 1fr;
  grid-template-rows: 1fr 1fr 1fr;
}

.video-grid.layout-3x3 .video-window:nth-child(1) {
  grid-column: 1 / 3;
  grid-row: 1 / 3;
}

.video-grid.layout-3x3 .video-window:nth-child(2) {
  grid-column: 3 / 4;
  grid-row: 1 / 2;
}

.video-grid.layout-3x3 .video-window:nth-child(3) {
  grid-column: 3 / 4;
  grid-row: 2 / 3;
}

.video-grid.layout-3x3 .video-window:nth-child(4) {
  grid-column: 3 / 4;
  grid-row: 3 / 4;
}

.video-grid.layout-3x3 .video-window:nth-child(5) {
  grid-column: 1 / 2;
  grid-row: 3 / 4;
}

.video-grid.layout-3x3 .video-window:nth-child(6) {
  grid-column: 2 / 3;
  grid-row: 3 / 4;
}

/* 8分屏L形布局：3×3主屏+右侧列4个+底部行4个，第5个屏右下角共享 */
.video-grid.layout-4x4 {
  grid-template-columns: 1fr 1fr 1fr 1fr;
  grid-template-rows: 1fr 1fr 1fr 1fr;
}

.video-grid.layout-4x4 .video-window:nth-child(1) {
  grid-column: 1 / 4;
  grid-row: 1 / 4;
}

.video-grid.layout-4x4 .video-window:nth-child(2) {
  grid-column: 4 / 5;
  grid-row: 1 / 2;
}

.video-grid.layout-4x4 .video-window:nth-child(3) {
  grid-column: 4 / 5;
  grid-row: 2 / 3;
}

.video-grid.layout-4x4 .video-window:nth-child(4) {
  grid-column: 4 / 5;
  grid-row: 3 / 4;
}

.video-grid.layout-4x4 .video-window:nth-child(5) {
  grid-column: 4 / 5;
  grid-row: 4 / 5;
}

.video-grid.layout-4x4 .video-window:nth-child(6) {
  grid-column: 1 / 2;
  grid-row: 4 / 5;
}

.video-grid.layout-4x4 .video-window:nth-child(7) {
  grid-column: 2 / 3;
  grid-row: 4 / 5;
}

.video-grid.layout-4x4 .video-window:nth-child(8) {
  grid-column: 3 / 4;
  grid-row: 4 / 5;
}

.video-grid.layout-5x5 {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, 1fr);
}

.video-grid.layout-6x6 {
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(4, 1fr);
}

.video-window {
  background: transparent;
  border: 1px solid #ddd;
  border-radius: 0;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  width: 100%;
  height: 100%;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

.drag-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 10;
  pointer-events: auto;
  background: transparent;
}

/* 四分屏布局 - 确保每个子区域都占满网格单元格 */
.video-grid.layout-2x2 .video-window {
  height: 100%;
  width: 100%;
}

/* 小屏等高布局 - 移除强制正方形，让小屏等高分布 */
.video-grid.layout-4x4 .video-window:not(:first-child),
.video-grid.layout-3x3 .video-window:not(:first-child) {
  height: 100%;
  width: 100%;
}

.video-window.active {
  border: 2px solid #409eff;
}

/* 视频窗口过渡动画 */
.video-window {
  transition: all 0.3s ease-in-out;
}

.video-window iframe,
.video-window .camera-rtc-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
  overflow: hidden;
}

.camera-rtc-player {
  width: 100%;
  height: 100%;
}

.rtsp-player-container {
  width: 100%;
  height: 100%;
  transition: width 0.3s ease-in-out, height 0.3s ease-in-out;
}

/* 拖拽时的视觉反馈 */
.video-window.dragging {
  opacity: 0.8;
  transform: scale(0.95);
  z-index: 1000;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.3);
  border: 2px dashed #1A53FF;
  transition: all 0.2s ease;
  position: relative;
}

.video-window.drag-over {
  border: 2px dashed #67c23a;
  background-color: rgba(103, 194, 58, 0.1);
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
  transition: all 0.2s ease;
  z-index: 1001;
  position: relative;
}

.video-window.drop-target {
  border: 2px dashed #409eff;
  background-color: rgba(64, 158, 255, 0.1);
  transition: all 0.2s ease;
}

.video-window[draggable="true"] {
  cursor: grab;
}

.video-window[draggable="true"]:active {
  cursor: grabbing;
}

/* 拖拽时的提示样式 */
.video-window.dragging::before {
  content: "拖拽到目标位置";
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(26, 83, 255, 0.9);
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  z-index: 1001;
  pointer-events: none;
}

.video-window.drop-target::before {
  content: "释放以交换位置";
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(103, 194, 58, 0.9);
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  z-index: 1001;
  pointer-events: none;
}

/* 不同布局下的WebRTC播放器优化 */
.video-grid.layout-1x1 .webrtc-iframe-player {
  min-height: 650px;
  width: 100% !important;
  height: 100% !important;
}

/* 确保视频播放器能够正确扩展 */
.video-window iframe,
.video-window .webrtc-iframe-player,
.video-window .rtsp-player-container {
  width: 100%;
  height: 100%;
}

.video-grid.layout-2x2 .webrtc-iframe-player {
  min-height: 300px;
}

.video-grid.layout-3x3 .webrtc-iframe-player {
  min-height: 200px;
}

.video-grid.layout-4x4 .webrtc-iframe-player,
.video-grid.layout-5x5 .webrtc-iframe-player,
.video-grid.layout-6x6 .webrtc-iframe-player {
  min-height: 150px;
}

/* WebRTC iframe基础样式（简化版） */
.webrtc-iframe {
  border: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
  box-sizing: border-box !important;
  width: 100% !important;
  height: 100% !important;
}

.youtube-iframe-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.youtube-iframe-container iframe {
  width: 100% !important;
  height: 100% !important;
  border: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
  box-sizing: border-box !important;
}

.webrtc-iframe-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.webrtc-iframe-container iframe {
  width: 100% !important;
  height: 100% !important;
  border: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
  box-sizing: border-box !important;
}

/* 优化小窗口模式下的播放器信息显示 */
.video-grid.layout-4x4 .player-info,
.video-grid.layout-5x5 .player-info,
.video-grid.layout-6x6 .player-info {
  font-size: 10px;
}

.video-grid.layout-6x6 .player-overlay {
  padding: 4px;
}

/* WebRTC播放器容器的额外优化 */
.webrtc-iframe-player {
  background: #000;
  border-radius: 0;
  contain: layout style paint;
}

/* 视频窗口内的WebRTC iframe特殊处理 */
.video-window .webrtc-iframe-player {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

/* WebRTC iframe容器 */
.webrtc-iframe-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.webrtc-iframe-container iframe {
  width: 100%;
  height: 100%;
  border: none;
}

/* YouTube iframe容器 */
.youtube-iframe-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.youtube-iframe-container iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.video-window .video-player-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

/* 视频弹窗新增样式 */
.video-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  position: absolute;
  top: 0;
  left: 0;
}

/* 空白占位符样式 */
.video-placeholder.empty-placeholder {
  background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 100%);
  border: 2px dashed #404040;
}

.video-placeholder.empty-placeholder .placeholder-icon {
  color: #666666;
  font-size: 36px;
}

.video-placeholder.empty-placeholder .placeholder-text {
  color: #999999;
  font-size: 16px;
}

.video-placeholder.empty-placeholder .placeholder-details {
  color: #666666;
  font-size: 12px;
}

/* WebRTC direct player */
.webrtc-direct-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
  overflow: hidden;
}

.webrtc-direct-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

/* RTSP播放器容器样式 */
.rtsp-player-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
  overflow: hidden;
}

/* WebRTC iframe播放器样式 */
.webrtc-iframe-player {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  background: #000;
}

.webrtc-iframe {
  border: none;
  background: #000;
  position: absolute;
  top: 0;
  left: 0;
}

.player-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: 8px;
  background: linear-gradient(180deg, rgba(0,0,0,0.8) 0%, transparent 50%);
  color: white;
  pointer-events: none;
  z-index: 2;
}

.player-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.player-title {
  font-weight: 500;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.8);
}

.player-status {
  color: #67c23a;
  font-size: 11px;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.8);
}

/* 直接WebRTC播放器样式 */
.rtsp-webrtc-player {
  width: 100%;
  height: 100%;
}

/* RTSP备用显示样式 */
.rtsp-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 100%);
  color: white;
}

.rtsp-fallback .rtsp-info-content {
  text-align: center;
  padding: 20px;
  max-width: 250px;
}

.rtsp-fallback .rtsp-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 8px;
  color: white;
}

.rtsp-fallback .rtsp-note {
  font-size: 12px;
  color: #f56c6c;
  margin-bottom: 8px;
}

/* 小窗口模式优化 */
.video-grid.layout-4x4 .rtsp-info-content,
.video-grid.layout-5x5 .rtsp-info-content,
.video-grid.layout-6x6 .rtsp-info-content {
  padding: 8px;
  max-width: 120px;
}

.video-grid.layout-4x4 .rtsp-title,
.video-grid.layout-5x5 .rtsp-title,
.video-grid.layout-6x6 .rtsp-title {
  font-size: 10px;
  margin-bottom: 4px;
}

.video-grid.layout-4x4 .rtsp-note,
.video-grid.layout-5x5 .rtsp-note,
.video-grid.layout-6x6 .rtsp-note {
  font-size: 8px;
  margin-bottom: 8px;
}

.video-grid.layout-4x4 .rtsp-btn,
.video-grid.layout-5x5 .rtsp-btn,
.video-grid.layout-6x6 .rtsp-btn {
  padding: 2px 6px;
  font-size: 8px;
}

.video-grid.layout-6x6 .rtsp-actions {
  gap: 4px;
}

/* 小窗口模式下的加载状态优化 */
.video-grid.layout-4x4 .loading-content,
.video-grid.layout-5x5 .loading-content,
.video-grid.layout-6x6 .loading-content {
  padding: 8px;
}

.video-grid.layout-4x4 .loading-title,
.video-grid.layout-5x5 .loading-title,
.video-grid.layout-6x6 .loading-title {
  font-size: 10px;
  margin-bottom: 4px;
}

.video-grid.layout-4x4 .loading-note,
.video-grid.layout-5x5 .loading-note,
.video-grid.layout-6x6 .loading-note {
  font-size: 8px;
}

/* 视频区域全屏样式 */
.video-area.fullscreen-active {
  width: 100vw !important;
  height: 100vh !important;
  background: #000 !important;
  padding: 0 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  z-index: 9999 !important;
}

/* 全屏状态下的视频网格优化 */
.video-area.fullscreen-active .video-grid {
  width: 100vw !important;
  height: 100vh !important;
  gap: 2px !important;
  max-width: 100vw !important;
  max-height: 100vh !important;
}

/* 全屏状态下的视频窗口优化 */
.video-area.fullscreen-active .video-window {
  border: 1px solid #333 !important;
  min-height: auto !important;
  min-width: auto !important;
}

/* 视频控制区域 */
.video-controls {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 6px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  margin-bottom: 4px;
  flex-shrink: 0;
  height: 40px;
}

/* PTZ控制面板区域 */
.ptz-control-section {
  flex: 0 0 320px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  padding: 0;
  overflow: hidden;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 占位符内容样式 */
.placeholder-content {
  text-align: center;
  padding: 20px;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.placeholder-text {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
  color: #303133;
}

.placeholder-details {
  font-size: 12px;
  color: #606266;
  margin-bottom: 16px;
}

.placeholder-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s ease;
}

.action-btn.primary {
  background: #409eff;
  color: white;
}

.action-btn.primary:hover {
  background: #337ecc;
}

.action-btn.secondary {
  background: #f4f4f5;
  color: #606266;
}

.action-btn.secondary:hover {
  background: #e9e9eb;
}

/* 录像状态样式 */
.recording-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.recording-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #f56c6c;
  font-size: 12px;
  font-weight: 500;
}

.recording-dot {
  width: 8px;
  height: 8px;
  background: #f56c6c;
  border-radius: 50%;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

/* WebRTC和YouTube信息样式 */
.webrtc-info,
.youtube-info {
  position: absolute;
  bottom: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 10px;
}
</style> 
