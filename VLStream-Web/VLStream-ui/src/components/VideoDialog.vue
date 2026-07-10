<template>
  <div 
    class="video-dialog-overlay"
    :style="{ zIndex: zIndex }"
  >
    <div 
      class="video-dialog draggable-dialog"
      :class="{ 'fullscreen-active': isFullscreen }"
      :style="{ 
        left: position.x + 'px', 
        top: position.y + 'px',
        position: 'fixed',
        transform: 'none'
      }"
      @mousedown="$emit('bring-to-front')"
    >
      <!-- 弹窗头部 -->
      <div 
        class="video-dialog-header draggable-handle"
        @mousedown="$emit('start-drag', $event)"
      >
        <span class="video-dialog-title">{{ title }}</span>
        <div class="dialog-controls">
          <button class="dialog-control-btn minimize-btn" @click="$emit('minimize')" title="最小化">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 13H5v-2h14v2z"/>
            </svg>
          </button>
          <button class="dialog-control-btn fullscreen-btn" @click="$emit('toggle-fullscreen')" :title="isFullscreen ? '退出全屏' : '全屏'">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
              <path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/>
            </svg>
          </button>
          <button class="dialog-control-btn close-btn" @click="$emit('close')" title="关闭">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
          </button>
        </div>
      </div>
      
      <!-- 弹窗内容 -->
      <div class="video-dialog-content" v-show="!isMinimized">
        <div class="dialog-main-content">
          <!-- 视频播放区域 -->
          <div class="video-player-section">
            <!-- 拖拽提示 -->
            <div class="drag-tip" v-if="layoutCount > 1">
              <el-icon><InfoFilled /></el-icon>
              <span>直接拖拽视频窗口可交换位置</span>
            </div>
            
            <div class="video-area">
              <!-- 视频网格布局 -->
              <div class="video-grid" :class="`layout-${getLayoutClass(layoutCount)}`">
                <div 
                  v-for="index in layoutCount" 
                  :key="`window-${index}`"
                  class="video-window"
                  :class="{ 
                    active: selectedVideoIndex === index - 1,
                    'dragging': draggingWindow === index - 1,
                    'drag-over': dragOverWindow === index - 1
                  }"
                  :data-window-index="index - 1"
                  draggable="true"
                  @click="$emit('select-video-window', index - 1)"
                  @dblclick="$emit('video-double-click', index - 1)"
                  @dragstart="$emit('window-drag-start', $event, index - 1)"
                  @dragover.prevent="$emit('window-drag-over', $event, index - 1)"
                  @dragenter.prevent="$emit('window-drag-enter', $event, index - 1)"
                  @dragleave="$emit('window-drag-leave', $event, index - 1)"
                  @drop="$emit('window-drop', $event, index - 1)"
                  @dragend="$emit('window-drag-end')"
                >
                  <!-- 拖拽遮罩层 -->
                  <div class="drag-overlay" 
                       @click.stop="$emit('select-video-window', index - 1)"
                       @dblclick.stop="$emit('video-double-click', index - 1)"
                       @dragstart.stop="$emit('window-drag-start', $event, index - 1)"
                       @dragover.stop.prevent="$emit('window-drag-over', $event, index - 1)"
                       @dragenter.stop.prevent="$emit('window-drag-enter', $event, index - 1)"
                       @dragleave.stop="$emit('window-drag-leave', $event, index - 1)"
                       @drop.stop="$emit('window-drop', $event, index - 1)"
                       @dragend.stop="$emit('window-drag-end')">
                  </div>
                  
                  <!-- 如果有对应的摄像头数据，显示视频内容 -->
                  <template v-if="cameras[index - 1]">
                    <!-- 优先使用iframe播放WebRTC -->
                    <div v-if="cameras[index - 1].deviceData && cameras[index - 1].deviceData.webrtcUrl" class="webrtc-iframe-container">
                      <iframe 
                        :src="cameras[index - 1].deviceData.webrtcUrl"
                        width="100%"
                        height="100%"
                        frameborder="0"
                        allow="camera; microphone"
                        @load="$emit('webrtc-iframe-load')"
                        @error="$emit('webrtc-iframe-error')"
                      ></iframe>
                      <div class="webrtc-info">
                        <small>WebRTC播放 - {{ cameras[index - 1].name }}</small>
                      </div>
                    </div>
                    
                    <!-- 备用：使用RtspPlayer组件直接连接 -->
                    <div v-else-if="cameras[index - 1].deviceData && cameras[index - 1].deviceData.streamUrl && cameras[index - 1].deviceData.playMode === 'direct'" class="rtsp-player-container">
                      <RtspPlayer 
                        :rtsp-url="cameras[index - 1].deviceData.streamUrl || cameras[index - 1].deviceData.originalRtspUrl"
                        width="100%"
                        height="100%"
                        @connected="$emit('rtsp-connected')"
                        @disconnected="$emit('rtsp-disconnected')"
                        @error="$emit('rtsp-error')"
                      />
                    </div>
                    
                    <!-- 有设备但无视频流时显示占位符 -->
                    <div v-else class="video-placeholder">
                      <div class="placeholder-content">
                        <div class="placeholder-icon">📹</div>
                        <div class="placeholder-text">暂无视频流</div>
                        <div class="placeholder-details">
                          <div>设备: {{ cameras[index - 1].name }}</div>
                          <div>状态: 在线</div>
                        </div>
                        
                        <!-- 备用操作选项 -->
                        <div class="placeholder-actions">
                          <button class="action-btn primary" @click="$emit('retry-webrtc-connection', cameras[index - 1])">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
                            </svg>
                            重试连接
                          </button>
                          <button class="action-btn secondary" @click="$emit('copy-stream-url', cameras[index - 1].deviceData?.streamUrl)">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M16 1H4C2.9 1 2 1.9 2 3v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/>
                            </svg>
                            复制地址
                          </button>
                          <button class="action-btn secondary" @click="$emit('open-in-vlc', cameras[index - 1].deviceData?.streamUrl)">
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
                          <button class="action-btn primary" @click="$emit('add-device-to-window', index - 1)">
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
            </div>
            
            <!-- 录像控制区域 -->
            <div class="video-controls">
              <button 
                class="video-record-btn" 
                @click="$emit('toggle-recording')"
                :class="{ 'recording': isRecording }"
              >
                <span v-if="!isRecording">开始录像</span>
                <span v-else>停止录像</span>
              </button>
              
              <!-- 录像状态显示 -->
              <div v-if="isRecording" class="recording-status">
                <div class="recording-indicator">
                  <span class="recording-dot"></span>
                  录制中 {{ recordingTime }}
                </div>
              </div>
            </div>
          </div>
          
          <!-- PTZ控制面板区域 -->
          <div class="ptz-control-section">
            <PTZPanel 
              :show-camera-management="true"
              @ptz-control="$emit('ptz-control', $event)"
              @zoom-control="$emit('zoom-control', $event)"
              @control-action="$emit('control-action', $event)"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import RtspPlayer from '@/components/RtspPlayer.vue'
import PTZPanel from '@/components/PTZPanel.vue'

// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '视频播放'
  },
  position: {
    type: Object,
    default: () => ({ x: 100, y: 100 })
  },
  zIndex: {
    type: Number,
    default: 2000
  },
  layoutCount: {
    type: Number,
    default: 4
  },
  cameras: {
    type: Array,
    default: () => []
  },
  selectedVideoIndex: {
    type: Number,
    default: 0
  },
  draggingWindow: {
    type: Number,
    default: null
  },
  dragOverWindow: {
    type: Number,
    default: null
  },
  isMinimized: {
    type: Boolean,
    default: false
  },
  isFullscreen: {
    type: Boolean,
    default: false
  },
  isRecording: {
    type: Boolean,
    default: false
  },
  recordingTime: {
    type: String,
    default: '00:00:00'
  }
})

// Emits
const emit = defineEmits([
  'close',
  'minimize',
  'toggle-fullscreen',
  'bring-to-front',
  'start-drag',
  'select-video-window',
  'video-double-click',
  'window-drag-start',
  'window-drag-over',
  'window-drag-enter',
  'window-drag-leave',
  'window-drop',
  'window-drag-end',
  'webrtc-iframe-load',
  'webrtc-iframe-error',
  'rtsp-connected',
  'rtsp-disconnected',
  'rtsp-error',
  'retry-webrtc-connection',
  'copy-stream-url',
  'open-in-vlc',
  'add-device-to-window',
  'toggle-recording',
  'ptz-control',
  'zoom-control',
  'control-action'
])

// 方法
const getLayoutClass = (count) => {
  const layoutMap = {
    1: '1x1',
    4: '2x2',
    9: '3x3',
    16: '4x4',
    25: '5x5',
    36: '6x6'
  }
  return layoutMap[count] || '2x2'
}
</script>

<style scoped>
.video-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: transparent;
  pointer-events: none;
  z-index: 2000;
}

.video-dialog {
  position: fixed;
  width: 1200px;
  height: 800px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  pointer-events: auto;
}

.video-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #f5f5f5;
  border-bottom: 1px solid #e4e7ed;
  cursor: move;
  user-select: none;
}

.video-dialog-title {
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
  background: #f0f0f0;
  color: #303133;
}

.dialog-control-btn.close-btn:hover {
  background: #f56c6c;
  color: white;
}

.video-dialog-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.dialog-main-content {
  display: flex;
  width: 100%;
  height: 100%;
}

.video-player-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  background: #000;
}

.drag-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(26, 83, 255, 0.1);
  border: 1px solid rgba(26, 83, 255, 0.3);
  border-radius: 6px;
  color: #1A53FF;
  font-size: 12px;
  margin-bottom: 12px;
}

.video-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-grid {
  display: grid;
  gap: 2px;
  width: 100%;
  height: 100%;
  background: #000;
}

.video-grid.layout-1x1 {
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
}

.video-grid.layout-2x2 {
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
}

.video-grid.layout-3x3 {
  grid-template-columns: 1fr 1fr 1fr;
  grid-template-rows: 1fr 1fr 1fr;
}

.video-grid.layout-4x4 {
  grid-template-columns: 1fr 1fr 1fr 1fr;
  grid-template-rows: 1fr 1fr 1fr 1fr;
}

.video-grid.layout-5x5 {
  grid-template-columns: 1fr 1fr 1fr 1fr 1fr;
  grid-template-rows: 1fr 1fr 1fr 1fr 1fr;
}

.video-grid.layout-6x6 {
  grid-template-columns: 1fr 1fr 1fr 1fr 1fr 1fr;
  grid-template-rows: 1fr 1fr 1fr 1fr 1fr 1fr;
}

.video-window {
  position: relative;
  background: #1a1a1a;
  border: 1px solid #333;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
}

.video-window:hover {
  border-color: #1A53FF;
}

.video-window.active {
  border-color: #1A53FF;
  box-shadow: 0 0 0 2px rgba(26, 83, 255, 0.3);
}

.video-window.dragging {
  opacity: 0.7;
  transform: scale(0.95);
}

.video-window.drag-over {
  border-color: #f56c6c;
  box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.3);
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
  border: 2px solid transparent;
  transition: all 0.2s ease;
}

.video-window.active .drag-overlay {
  background: rgba(26, 83, 255, 0.1);
  border: 2px solid #1A53FF;
}

.video-window.dragging .drag-overlay {
  background: rgba(26, 83, 255, 0.2);
  border: 2px dashed #1A53FF;
}

.video-window.drag-over .drag-overlay {
  background: rgba(245, 108, 108, 0.2);
  border: 2px solid #f56c6c;
}

.webrtc-iframe-container,
.rtsp-player-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.webrtc-iframe-container iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #000;
}

.webrtc-info {
  position: absolute;
  bottom: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 10px;
}

.video-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1a1a1a;
  color: #666;
}

.placeholder-content {
  text-align: center;
  padding: 20px;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.placeholder-text {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
  color: #999;
}

.placeholder-details {
  font-size: 12px;
  color: #666;
  margin-bottom: 16px;
}

.placeholder-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid #333;
  border-radius: 4px;
  background: transparent;
  color: #ccc;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s ease;
}

.action-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
}

.action-btn.primary {
  background: #1A53FF;
  border-color: #1A53FF;
  color: white;
}

.action-btn.primary:hover {
  background: #3d70ff;
  border-color: #3d70ff;
}

.video-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 16px 0;
  border-top: 1px solid #333;
  margin-top: 16px;
}

.video-record-btn {
  padding: 8px 16px;
  border: 1px solid #333;
  border-radius: 4px;
  background: transparent;
  color: #ccc;
  cursor: pointer;
  transition: all 0.2s ease;
}

.video-record-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
}

.video-record-btn.recording {
  background: #f56c6c;
  border-color: #f56c6c;
  color: white;
}

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
}

.recording-dot {
  width: 8px;
  height: 8px;
  background: #f56c6c;
  border-radius: 50%;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.ptz-control-section {
  width: 320px;
  background: #f5f5f5;
  border-left: 1px solid #e4e7ed;
  overflow-y: auto;
}

/* 全屏样式 */
.video-dialog.fullscreen-active {
  width: 100vw !important;
  height: 100vh !important;
  max-width: 100vw !important;
  max-height: 100vh !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  margin: 0 !important;
  border-radius: 0 !important;
  background: #000 !important;
  z-index: 9999 !important;
  position: fixed !important;
}

.video-dialog.fullscreen-active .video-dialog-header {
  position: absolute !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  z-index: 10 !important;
  background: rgba(0, 0, 0, 0.8) !important;
  backdrop-filter: blur(10px) !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1) !important;
}

.video-dialog.fullscreen-active .video-dialog-content {
  width: 100% !important;
  height: 100% !important;
  padding: 0 !important;
  background: #000 !important;
}

.video-dialog.fullscreen-active .ptz-control-section {
  position: absolute !important;
  right: 0 !important;
  top: 0 !important;
  bottom: 0 !important;
  width: 320px !important;
  height: 100% !important;
  background: rgba(0, 0, 0, 0.9) !important;
  backdrop-filter: blur(10px) !important;
  border-left: 1px solid rgba(255, 255, 255, 0.1) !important;
  z-index: 5 !important;
}
</style> 