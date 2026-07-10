<template>
  <div 
    class="video-dialog-overlay"
    :style="{ zIndex: 2000 + dialog.id }"
  >
    <div 
      class="video-dialog draggable-dialog"
      :style="{ 
        left: dialog.position.x + 'px', 
        top: dialog.position.y + 'px',
        position: 'fixed',
        transform: 'none'
      }"
      @mousedown="bringToFront(dialog.id)"
    >
      <div 
        class="video-dialog-header draggable-handle"
        @mousedown="startDrag($event, dialog.id)"
      >
        <span class="video-dialog-title">{{ dialog.camera.name }} - 视频播放</span>
        <div class="dialog-controls">
          <button class="dialog-control-btn minimize-btn" @click="minimizeDialog(dialog.id)" title="最小化">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 13H5v-2h14v2z"/>
            </svg>
          </button>
          <button class="dialog-control-btn close-btn" @click="closeVideoDialog(dialog.id)" title="关闭">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
          </button>
        </div>
      </div>
      
      <div class="video-dialog-content" v-show="!dialog.minimized">
        <div class="video-player">
          <!-- 实时视频播放区域 -->
          <div class="video-placeholder-player">
            <!-- 优先使用iframe播放WebRTC -->
            <div v-if="dialog.camera.deviceData && dialog.camera.deviceData.webrtcUrl" class="webrtc-iframe-container">
              <iframe 
                :src="dialog.camera.deviceData.webrtcUrl"
                width="840"
                height="520"
                frameborder="0"
                allow="camera; microphone"
                @load="handleWebRTCIframeLoad"
                @error="handleWebRTCIframeError"
              ></iframe>
              <div class="webrtc-info">
                <small>WebRTC播放 - {{ dialog.camera.name }}</small>
              </div>
            </div>
            
            <!-- 备用：使用RtspPlayer组件直接连接 -->
            <div v-else-if="dialog.camera.deviceData && dialog.camera.deviceData.streamUrl && dialog.camera.deviceData.playMode === 'direct'" class="rtsp-player-container">
              <RtspPlayer 
                :rtsp-url="dialog.camera.deviceData.streamUrl || dialog.camera.deviceData.originalRtspUrl"
                :width="840"
                :height="520"
                @connected="handleDialogRtspConnected"
                @disconnected="handleDialogRtspDisconnected"
                @error="handleDialogRtspError"
              />
            </div>
            
            <!-- 无视频流时显示占位符 -->
            <div v-else class="video-placeholder">
              <div class="placeholder-content">
                <div class="placeholder-icon">📹</div>
                <div class="placeholder-text">暂无视频流</div>
                <div class="placeholder-details">
                  <div>设备: {{ dialog.camera.name }}</div>
                  <div>状态: 在线</div>
                </div>
                
                <!-- 备用操作选项 -->
                <div class="placeholder-actions">
                  <button class="action-btn primary" @click="retryWebRTCConnection(dialog.camera)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
                    </svg>
                    重试连接
                  </button>
                  <button class="action-btn secondary" @click="copyStreamUrl(dialog.camera.deviceData?.streamUrl)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M16 1H4C2.9 1 2 1.9 2 3v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/>
                    </svg>
                    复制流地址
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import RtspPlayer from '@/components/RtspPlayer.vue'

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
  'retry-connection',
  'copy-stream-url'
])

// 拖拽相关状态
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })

// 方法
const bringToFront = (id) => {
  emit('bring-to-front', id)
}

const startDrag = (event, id) => {
  emit('start-drag', { event, id })
}

const minimizeDialog = (id) => {
  emit('minimize', id)
}

const closeVideoDialog = (id) => {
  emit('close', id)
}

const handleWebRTCIframeLoad = () => {
  emit('webrtc-load')
}

const handleWebRTCIframeError = () => {
  emit('webrtc-error')
}

const handleDialogRtspConnected = () => {
  emit('rtsp-connected')
}

const handleDialogRtspDisconnected = () => {
  emit('rtsp-disconnected')
}

const handleDialogRtspError = (error) => {
  emit('rtsp-error', error)
}

const retryWebRTCConnection = (camera) => {
  emit('retry-connection', camera)
}

const copyStreamUrl = (url) => {
  if (url) {
    navigator.clipboard.writeText(url).then(() => {
      ElMessage.success('流地址已复制到剪贴板')
    }).catch(() => {
      ElMessage.error('复制失败')
    })
  } else {
    ElMessage.warning('暂无流地址')
  }
}
</script>

<style scoped>
/* 视频弹窗样式 */
.video-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 2000;
}

.video-dialog {
  position: fixed;
  background: white;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  min-width: 400px;
  min-height: 300px;
  pointer-events: all;
  overflow: hidden;
  resize: both;
}

.video-dialog-header {
  background: #f5f5f5;
  padding: 12px 16px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: move;
  user-select: none;
}

.video-dialog-title {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.dialog-controls {
  display: flex;
  gap: 8px;
}

.dialog-control-btn {
  background: none;
  border: none;
  padding: 4px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  transition: all 0.2s;
}

.dialog-control-btn:hover {
  background: #e0e0e0;
  color: #333;
}

.close-btn:hover {
  background: #ff4d4f;
  color: white;
}

.video-dialog-content {
  padding: 16px;
  height: calc(100% - 60px);
  overflow: auto;
}

.video-player {
  width: 100%;
  height: 100%;
}

.video-placeholder-player {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.webrtc-iframe-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.webrtc-iframe-container iframe {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 4px;
}

.webrtc-info {
  position: absolute;
  bottom: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.rtsp-player-container {
  width: 100%;
  height: 100%;
}

.video-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  border-radius: 8px;
  border: 2px dashed #dee2e6;
}

.placeholder-content {
  text-align: center;
  color: #6c757d;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.placeholder-text {
  font-size: 18px;
  font-weight: 500;
  margin-bottom: 8px;
}

.placeholder-details {
  font-size: 14px;
  margin-bottom: 24px;
}

.placeholder-details div {
  margin-bottom: 4px;
}

.placeholder-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.action-btn.primary {
  background: #1A53FF;
  color: white;
}

.action-btn.primary:hover {
  background: #3d70ff;
}

.action-btn.secondary {
  background: #f8f9fa;
  color: #6c757d;
  border: 1px solid #dee2e6;
}

.action-btn.secondary:hover {
  background: #e9ecef;
  color: #495057;
}

/* 拖拽相关样式 */
.draggable-dialog {
  cursor: default;
}

.draggable-handle {
  cursor: move;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .video-dialog {
    min-width: 300px;
    min-height: 200px;
  }
  
  .placeholder-actions {
    flex-direction: column;
    align-items: center;
  }
}
</style> 