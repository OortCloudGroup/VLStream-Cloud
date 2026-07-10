<template>
  <div class="video-square">
    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 调试信息面板 -->
      <div v-if="showDebugInfo" class="debug-panel">
        <div class="debug-header">
          <h4>调试信息</h4>
          <el-button size="small" text @click="showDebugInfo = false">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="debug-content">
          <div class="debug-item">
            <strong>API状态:</strong> 
            <span :class="{ 'status-success': !loading, 'status-loading': loading }">
              {{ loading ? '加载中...' : '已完成' }}
            </span>
          </div>
          <div class="debug-item">
            <strong>原始设备数量:</strong> {{ deviceList.length }}
          </div>
          <div class="debug-item">
            <strong>有效设备数量:</strong> {{ realCameraStreams.length }}
          </div>
          <div class="debug-item">
            <strong>当前布局:</strong> {{ layoutMode }}
          </div>
          <div class="debug-item">
            <strong>WebRTC状态:</strong> 
            <span :class="{ 'status-success': webrtcConfig.available, 'status-error': !webrtcConfig.available }">
              {{ webrtcConfig.available ? '可用' : '不可用' }}
            </span>
          </div>
        </div>
      </div>

      <!-- 地图模式 - 与PTZ面板水平布局 -->
      <div class="main-layout">
        <div class="map-area fullscreen-map">
          <!-- 使用封装的布局控制组件 -->
          <VideoLayoutControls
            :layout-mode="layoutMode"
            :show-extended-layout="showExtendedLayout"
            :is-fullscreen="isFullscreen"
            :device-count="deviceList.length"
            :online-count="onlineDeviceCount"
            :offline-count="offlineDeviceCount"
            @layout-select="handleLayoutSelect"
            @extended-layout="showExtendedLayoutDialog"
            @custom-layout="openCustomVideoDialogs"
            @toggle-fullscreen="toggleFullscreen"
          />

          <!-- 地图主体 -->
          <div class="map-container">
            
            <!-- Leaflet地图容器 -->
            <div id="video-square-map" class="leaflet-map-container"></div>
            
            <!-- 地图控制工具栏 -->
            <div class="map-controls">
              <button class="map-control-btn" @click="fitBounds" title="显示全部摄像头">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 5.69l5 4.5V18h-2v-6H9v6H7v-7.81l5-4.5M12 3L2 12h3v8h6v-6h2v6h6v-8h3L12 3z"/>
                </svg>
              </button>
              <button class="map-control-btn" @click="toggleMapView" title="切换地图视图">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
                </svg>
              </button>
              <button class="map-control-btn" @click="refreshCameraLocations" title="刷新摄像头位置">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
                </svg>
              </button>
              <!-- 白天/黑夜模式切换 -->
              <button class="map-control-btn theme-toggle" @click="toggleDarkMode" :title="isDarkMode ? '切换到白天模式' : '切换到黑夜模式'">
                <svg v-if="!isDarkMode" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <!-- 太阳图标 -->
                  <path d="M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58c-.39-.39-1.03-.39-1.41 0-.39.39-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37c-.39-.39-1.03-.39-1.41 0-.39.39-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0 .39-.39.39-1.03 0-1.41l-1.06-1.06zm1.06-10.96c.39-.39.39-1.03 0-1.41-.39-.39-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36c.39-.39.39-1.03 0-1.41-.39-.39-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z"/>
                </svg>
                <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <!-- 月亮图标 -->
                  <path d="M9.37 5.51c-.18.64-.27 1.31-.27 1.99 0 4.08 3.32 7.4 7.4 7.4.68 0 1.35-.09 1.99-.27C17.45 17.19 14.93 19 12 19c-3.86 0-7-3.14-7-7 0-2.93 1.81-5.45 4.37-6.49zM12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9c0-.46-.04-.92-.1-1.36-.98 1.37-2.58 2.26-4.4 2.26-2.98 0-5.4-2.42-5.4-5.4 0-1.81.89-3.42 2.26-4.4-.44-.06-.9-.1-1.36-.1z"/>
                </svg>
              </button>
            </div>

            <!-- 缩放控制按钮 -->
            <div class="zoom-controls">
              <button class="zoom-btn" @click="zoomIn" title="放大">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
                </svg>
              </button>
              <button class="zoom-btn" @click="zoomOut" title="缩小">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19 13H5v-2h14v2z"/>
                </svg>
              </button>
          </div>
        </div>
      </div>
      </div>
      
                <!-- 设备列表面板 - 固定在右侧 -->
          <DeviceListPanel
            :devices="realCameraStreams"
            :tree-data="[]"
            :display-settings="displaySettings"
            @settings="handleSettings"
            @device-click="handleCameraClick"
            @search="handleMapSearch"
          />
    </div>

    <!-- 单个摄像头视频播放弹窗 -->
    <div 
      v-for="dialog in videoDialogs"
      :key="dialog.id"
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
              <!-- WebRTC直连视频 -->
              <video
                v-if="dialog.camera.deviceData && dialog.camera.deviceData.playMode === 'webrtc'"
                class="webrtc-video"
                controls
                autoplay
                muted
                playsinline
                :ref="el => setWebrtcVideoRef(dialog.id, el)"
                @loadeddata="handleWebRTCSuccess"
                @error="handleWebRTCError"
              ></video>

              <div
                v-else-if="dialog.camera.deviceData && (dialog.camera.deviceData.playMode === 'cameraRTC' || isCameraRtcStream(dialog.camera.deviceData.streamUrl))"
                class="camera-rtc-container"
              >
                <div
                  class="camera-rtc-player"
                  :ref="el => setCameraRtcContainerRef(dialog.id, el)"
                ></div>
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
                      复制地址
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 布局视频播放弹窗 -->
    <VideoLayoutDialog
      v-for="dialog in layoutDialogs"
      :key="'layout-' + dialog.id"
      :dialog="dialog"
      @close="closeLayoutDialog"
      @minimize="minimizeLayoutDialog"
      @bring-to-front="bringLayoutToFront"
      @start-drag="startLayoutDrag"
      @webrtc-load="handleWebRTCSuccess"
      @webrtc-error="handleWebRTCError"
      @rtsp-connected="handleDialogRtspConnected"
      @rtsp-disconnected="handleDialogRtspDisconnected"
      @rtsp-error="handleDialogRtspError"
      @ptz-control="handlePTZControl"
      @zoom-control="handleZoomControl"
      @control-action="handleControlAction"
      @retry-webrtc="retryWebRTCConnection"
      @copy-stream-url="copyStreamUrl"
      @open-in-vlc="openInVlc"
      @add-device="addDeviceToWindow"
      @video-window-click="handleVideoWindowClick"
      @video-window-double-click="handleVideoWindowDoubleClick"
      @video-window-swap="handleVideoWindowSwap"
    />

    <!-- 设备显示设置弹窗 -->
    <el-dialog
      v-model="showSettingsDialog" 
      title="显示设置"
      width="400px"
      center
      :close-on-click-modal="true"
      :show-close="true"
      class="settings-dialog"
    >
      <div class="settings-content">
        <div class="setting-group">
          <h4>设备类型</h4>
          <div class="setting-items">
            <el-checkbox-group v-model="displaySettings.deviceTypes">
              <el-checkbox 
                v-for="option in allDeviceTypeOptions" 
                :key="option.value"
                :label="option.value"
              >
                {{ option.label }}
              </el-checkbox>
            </el-checkbox-group>
            </div>
        </div>
        
        <div class="setting-actions">
          <el-button @click="cancelSettings">取消</el-button>
          <el-button type="primary" @click="confirmSettings">确定</el-button>
        </div>
      </div>
    </el-dialog>
  </div>

  <!-- 扩展布局弹窗 -->
  <div v-if="showExtendedLayout" class="extended-layout-overlay" @click="hideExtendedLayoutDialog">
    <div class="extended-layout-dialog" :style="extendedLayoutPosition" @click.stop>
      <div class="extended-layout-content">
        <div class="extended-layout-option" @click="handleExtendedLayoutSelect(22)">
          <div class="layout-option-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
              <rect x="2" y="2" width="4" height="4" rx="1"/>
              <rect x="8" y="2" width="4" height="4" rx="1"/>
              <rect x="14" y="2" width="4" height="4" rx="1"/>
              <rect x="20" y="2" width="2" height="4" rx="1"/>
              <rect x="2" y="8" width="4" height="4" rx="1"/>
              <rect x="8" y="8" width="4" height="4" rx="1"/>
              <rect x="14" y="8" width="4" height="4" rx="1"/>
              <rect x="20" y="8" width="2" height="4" rx="1"/>
              <rect x="2" y="14" width="4" height="4" rx="1"/>
              <rect x="8" y="14" width="4" height="4" rx="1"/>
              <rect x="14" y="14" width="4" height="4" rx="1"/>
              <rect x="20" y="14" width="2" height="4" rx="1"/>
              <rect x="2" y="20" width="4" height="4" rx="1"/>
              <rect x="8" y="20" width="4" height="4" rx="1"/>
              <rect x="14" y="20" width="4" height="4" rx="1"/>
              <rect x="20" y="20" width="2" height="4" rx="1"/>
            </svg>
        </div>
          <span class="layout-option-text">二十二画面</span>
      </div>
        <div class="extended-layout-option" @click="handleExtendedLayoutSelect(24)">
          <div class="layout-option-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
              <rect x="2" y="2" width="6" height="6" rx="1"/>
              <rect x="10" y="2" width="6" height="6" rx="1"/>
              <rect x="18" y="2" width="4" height="6" rx="1"/>
              <rect x="2" y="10" width="6" height="6" rx="1"/>
              <rect x="10" y="10" width="6" height="6" rx="1"/>
              <rect x="18" y="10" width="4" height="6" rx="1"/>
              <rect x="2" y="18" width="6" height="4" rx="1"/>
              <rect x="10" y="18" width="6" height="4" rx="1"/>
              <rect x="18" y="18" width="4" height="4" rx="1"/>
            </svg>
          </div>
          <span class="layout-option-text">二十四画面</span>
        </div>
        <div class="extended-layout-option" @click="handleExtendedLayoutSelect(25)">
          <div class="layout-option-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
              <rect x="2" y="2" width="4" height="4" rx="1"/>
              <rect x="8" y="2" width="4" height="4" rx="1"/>
              <rect x="14" y="2" width="4" height="4" rx="1"/>
              <rect x="20" y="2" width="2" height="4" rx="1"/>
              <rect x="2" y="8" width="4" height="4" rx="1"/>
              <rect x="8" y="8" width="4" height="4" rx="1"/>
              <rect x="14" y="8" width="4" height="4" rx="1"/>
              <rect x="20" y="8" width="2" height="4" rx="1"/>
              <rect x="2" y="14" width="4" height="4" rx="1"/>
              <rect x="8" y="14" width="4" height="4" rx="1"/>
              <rect x="14" y="14" width="4" height="4" rx="1"/>
              <rect x="20" y="14" width="2" height="4" rx="1"/>
              <rect x="2" y="20" width="4" height="4" rx="1"/>
              <rect x="8" y="20" width="4" height="4" rx="1"/>
              <rect x="14" y="20" width="4" height="4" rx="1"/>
              <rect x="20" y="20" width="2" height="4" rx="1"/>
              <rect x="26" y="2" width="2" height="22" rx="1"/>
            </svg>
          </div>
          <span class="layout-option-text">二十五画面</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, nextTick, onMounted, onUnmounted, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {Close} from '@element-plus/icons-vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

// 导入播放器组件
import RtspPlayer from '@/components/RtspPlayer.vue'

// 导入布局控制组件
import VideoLayoutControls from '@/components/VideoLayoutControls.vue'

// 导入视频布局弹窗组件
import VideoLayoutDialog from '@/components/VideoLayoutDialog.vue'

// 导入PTZ控制组件
// 导入设备列表面板组件
import DeviceListPanel from '@/components/DeviceListPanel.vue'
import {getDeviceList} from "@/api/device";
import {WEBRTC_SERVER_BASE_URL} from "@/api/webrtc";
import { CAMERA_RTC_SOCKET_URL, ensureOPlayer, isCameraRtcStream } from '@/utils/oplayer'

// 分页相关
const currentPage = ref(1)
const pageSize = ref(20)

// 基础状态
const loading = ref(false)
const showDebugInfo = ref(false)
const isDarkMode = ref(false)
const isFullscreen = ref(false)
const layoutMode = ref('2x2')
const showExtendedLayout = ref(false)
const extendedLayoutPosition = ref({
  top: '0px',
  left: '0px'
})
const mapViewMode = ref('normal') // 'normal' | 'satellite'

// 设备相关状态
const deviceList = ref([])
const realCameraStreams = ref([])
const mapSearchKeyword = ref('')


// 视频弹窗状态
const videoDialogs = ref([])
const nextDialogId = ref(1)

// 布局弹窗状态
const layoutDialogs = ref([])
const nextLayoutDialogId = ref(1000)
const maxLayoutZIndex = ref(3000)

// WebRTC相关状态
const webrtcConfig = ref({
  serverUrl: WEBRTC_SERVER_BASE_URL,
  available: false,
  enabled: true
})
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

// 地图相关变量
let mapInstance = null
let mapMarkers = []
let mapInitialized = false
let isComponentMounted = false
let currentTileLayer = null

// WebRTC script loader
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

const setWebrtcVideoRef = (dialogId, el) => {
  if (el) {
    webrtcVideoRefs.value.set(dialogId, el)
  } else {
    webrtcVideoRefs.value.delete(dialogId)
  }
}

const setCameraRtcContainerRef = (dialogId, el) => {
  if (el) {
    cameraRtcContainerRefs.value.set(dialogId, el)
  } else {
    cleanupDialogCameraRTC(dialogId)
    cameraRtcContainerRefs.value.delete(dialogId)
  }
}

const cleanupDialogWebRTC = async (dialogId) => {
  const player = webrtcPlayers.value.get(dialogId)
  if (player) {
    try {
      if (typeof player.disconnect === 'function') {
        await player.disconnect()
      } else if (typeof player.stop === 'function') {
        await player.stop()
      }
    } catch (error) {
      console.warn('清理WebRTC直连失败:', error)
    }
    webrtcPlayers.value.delete(dialogId)
  }
  
  const videoEl = webrtcVideoRefs.value.get(dialogId)
  if (videoEl) {
    videoEl.srcObject = null
  }
}

const cleanupDialogCameraRTC = (dialogId) => {
  const player = cameraRtcPlayers.value.get(dialogId)
  if (player?.compInstance?.$destroy) {
    player.compInstance.$destroy()
  }
  cameraRtcPlayers.value.delete(dialogId)

  const container = cameraRtcContainerRefs.value.get(dialogId)
  if (container) {
    container.innerHTML = ''
  }
}

const playDialogWebRTC = async (dialog) => {
  try {
    if (!dialog?.camera?.deviceData?.streamUrl) {
      throw new Error('缺少流地址')
    }
    
    exitAnyFullscreen()
    
    await ensureWebRtcStreamerScripts()
    await cleanupDialogWebRTC(dialog.id)
    await nextTick()
    
    const videoEl = webrtcVideoRefs.value.get(dialog.id)
    if (!videoEl) {
      throw new Error('WebRTC视频元素未就绪')
    }
    
    if (!videoEl.id) {
      videoEl.id = `webrtc-dialog-${dialog.id}`
    }
    
    const player = new window.WebRtcStreamer(videoEl.id, WEBRTC_STREAMER_BASE)
    webrtcPlayers.value.set(dialog.id, player)
    
    dialog.camera.deviceData.playMode = 'webrtc'
    player.connect(dialog.camera.deviceData.streamUrl, '', 'rtptransport=tcp&timeout=60')
    
    player.onconnected = () => {
      ElMessage.success('WebRTC直连成功')
    }
  } catch (error) {
    console.error('WebRTC直连播放失败:', error)
    ElMessage.error(`WebRTC播放失败: ${error.message || error}`)
    if (dialog?.camera?.deviceData) {
      dialog.camera.deviceData.playMode = 'direct'
    }
  }
}

// 设备显示设置

const playDialogCameraRTC = async (dialog) => {
  try {
    const deviceData = dialog?.camera?.deviceData
    if (!deviceData?.streamUrl) {
      throw new Error('Missing stream url')
    }

    const deviceId = deviceData.deviceId || deviceData.id
    if (!deviceId) {
      throw new Error('CameraRTC requires deviceId')
    }

    exitAnyFullscreen()

    await ensureOPlayer()
    cleanupDialogCameraRTC(dialog.id)
    await nextTick()

    const container = cameraRtcContainerRefs.value.get(dialog.id)
    if (!container) {
      throw new Error('CameraRTC container not ready')
    }

    const player = new window.OToolBox.OPlayer(container, {
      debuggerMode: false,
      autoSize: true,
      backgroundColor: '#000000',
      showHeader: true,
      webRTCSocketURL: CAMERA_RTC_SOCKET_URL
    })
    cameraRtcPlayers.value.set(dialog.id, player)

    dialog.camera.deviceData.playMode = 'cameraRTC'
    player.play({
      type: 'cameraRTC',
      src: String(deviceId),
      name: dialog?.camera?.name || ''
    })
  } catch (error) {
    console.error('CameraRTC play failed:', error)
    ElMessage.error(`CameraRTC play failed: ${error.message || error}`)
  }
}


const showSettingsDialog = ref(false)
const displaySettings = ref({
  deviceTypes: ['bulletCamera', 'ballCamera'],
  showOnline: true,
  showOffline: true
})

const allDeviceTypeOptions = ref([
  { value: 'bulletCamera', label: '枪机' },
  { value: 'ballCamera', label: '球机' }
])

const parseCoordinateValue = (value) => {
  if (value === null || value === undefined || value === '') return null
  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? parsed : null
}

const getDeviceCoordinates = (device) => {
  if (!device) return null
  const lat = parseCoordinateValue(
    device.latitude ?? device.lat ?? device.location?.lat ?? device.location?.latitude ?? device.position?.lat ?? device.position?.latitude
  )
  const lng = parseCoordinateValue(
    device.longitude ?? device.lng ?? device.location?.lng ?? device.location?.longitude ?? device.position?.lng ?? device.position?.longitude
  )
  if (lat === null || lng === null) return null
  return { lat, lng }
}

const isDeviceOnline = (status) => {
  if (status === null || status === undefined) return false
  if (typeof status === 'number') return status === 1
  const normalized = String(status).trim().toLowerCase()
  if (!normalized) return false
  return normalized === '在线' || normalized === 'online' || normalized === '1' || normalized === 'true'
}

// 计算属性
const onlineDeviceCount = computed(() => {
  return deviceList.value.filter(device => isDeviceOnline(device.status)).length
})

const offlineDeviceCount = computed(() => {
  return deviceList.value.filter(device => !isDeviceOnline(device.status)).length
})

const deviceMapMarkers = computed(() => {
  const markers = []
  const { showOnline, showOffline } = displaySettings.value

  deviceList.value.forEach(device => {
    const coords = getDeviceCoordinates(device)
    if (!coords) return

    const isOnline = isDeviceOnline(device.status)
    if ((isOnline && !showOnline) || (!isOnline && !showOffline)) {
      return
    }

    const hasStream = !!(device.streamUrl || device.originalRtspUrl || device.rtspUrl)

    markers.push({
      id: device.id,
      type: 'camera',
      name: device.deviceName || `设备${device.id}`,
      lat: coords.lat,
      lng: coords.lng,
      hasVideo: hasStream,
      deviceData: device
    })
  })

  return markers
})

const filteredCameraMarkers = computed(() => {
  if (!mapSearchKeyword.value.trim()) {
    return deviceMapMarkers.value
  }
  
  return deviceMapMarkers.value.filter(marker => 
    marker.name.toLowerCase().includes(mapSearchKeyword.value.toLowerCase())
  )
})

// 固定的真实设备数据
const getFixedRealDevices = () => {
  return [
    {
      id: 14,
      deviceName: "测试127",
      deviceId: "127",
      streamUrl: "rtsp://admin:oort301301@192.168.88.127/Streaming/Channels/101?transportmode=unicast&profile=Profile_1",
      originalRtspUrl: "rtsp://admin:oort301301@192.168.88.127/Streaming/Channels/101?transportmode=unicast&profile=Profile_1",
      rtspUrl: "rtsp://admin:oort301301@192.168.88.127/Streaming/Channels/101?transportmode=unicast&profile=Profile_1",
      status: "在线",
      deviceType: "摄像头",
      brand: "HIKVISION",
      ipAddress: "192.168.88.127",
      username: "admin",
      password: "oort301301",
      longitude: "114.114707",
      latitude: "22.558356",
      heightPosition: "地面",
      tag: "摄像头"
    },
    {
      id: 13,
      deviceName: "测试129",
      deviceId: "129",
      streamUrl: "rtsp://admin:oort301301@192.168.88.129/Streaming/Channels/101?transportmode=unicast&profile=Profile_1",
      originalRtspUrl: "rtsp://admin:oort301301@192.168.88.129/Streaming/Channels/101?transportmode=unicast&profile=Profile_1",
      rtspUrl: "rtsp://admin:oort301301@192.168.88.129/Streaming/Channels/101?transportmode=unicast&profile=Profile_1",
      status: "在线",
      deviceType: "球机",
      brand: "HIKVISION",
      ipAddress: "192.168.88.129",
      username: "admin",
      password: "oort301301",
      longitude: "114.114857",
      latitude: "22.558321",
      heightPosition: "地面",
      tag: "摄像头"
    },
    {
      id: 12,
      deviceName: "全球164个摄像头",
      deviceId: "CAM_GLOBAL_164_005",
      streamUrl: "https://www.youtube.com/embed/2BLqhS59Elc?autoplay=1&mute=1&controls=0&showinfo=0&rel=0&modestbranding=1",
      status: "在线",
      deviceType: "云台",
      brand: "YouTube直播",
      heightPosition: "地面",
      tag: "枪机"
    },
    {
      id: 11,
      deviceName: "西武新宿站前广场",
      deviceId: "CAM_SEIBU_SHINJUKU_004",
      streamUrl: "https://www.youtube.com/embed/lA6TaaMGgDo?autoplay=1&mute=1&controls=0&showinfo=0&rel=0&modestbranding=1",
      status: "在线",
      deviceType: "球机",
      brand: "YouTube直播",
      heightPosition: "地面",
      tag: "摄像头"
    },
    {
      id: 10,
      deviceName: "东京新宿歌舞伎町",
      deviceId: "CAM_TOKYO_KABUKICHO_003",
      streamUrl: "https://www.youtube.com/embed/DjdUEyjx8GM?autoplay=1&mute=1&controls=0&showinfo=0&rel=0&modestbranding=1",
      status: "在线",
      deviceType: "枪机",
      brand: "YouTube直播",
      heightPosition: "地面",
      tag: "云台"
    }
  ]
}

// 方法
const initMap = () => {
  // 确保组件仍然挂载且地图容器存在
  if (!isComponentMounted || !document.getElementById('video-square-map')) {
    console.log('组件未挂载或地图容器不存在，跳过地图初始化')
    return
  }
  
  if (mapInstance) {
    try {
      mapInstance.remove()
    } catch (error) {
      console.warn('清理旧地图时出错:', error)
    }
    mapInstance = null
  }
  
  try {
    mapInstance = L.map('video-square-map', {
      zoomControl: false // 禁用默认的缩放控件
    }).setView([35.8617, 104.1954], 6) // 适中的初始缩放级别
    
    // 初始化地图图层
    updateMapLayer()
    
    mapInitialized = true
    console.log('地图初始化成功')
    
    // 地图初始化完成后，如果设备数据已加载，立即添加标记
    if (deviceList.value.length > 0) {
      updateMapMarkers()
      // 默认最大化显示全部摄像头
      nextTick(() => {
        setTimeout(() => {
          fitBounds()
        }, 200) // 稍微延迟确保标记已添加到DOM
      })
    }
  } catch (error) {
    console.error('地图初始化失败:', error)
    mapInitialized = false
  }
}

// 更新地图图层
const updateMapLayer = () => {
  if (!mapInstance) return
  
  // 移除当前图层
  if (currentTileLayer) {
    mapInstance.removeLayer(currentTileLayer)
  }
  
  // 定义瓦片服务器
  const lightTileServers = [
  {
      url: 'https://tile.openstreetmap.de/{z}/{x}/{y}.png',
      name: 'OpenStreetMap DE'
    },
    {
      url: 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
      name: 'CartoDB Voyager (深蓝色)'
    },
    {
      url: 'https://tile.openstreetmap.de/{z}/{x}/{y}.png',
      name: 'OpenStreetMap DE'
    },
    {
      url: 'https://tiles.wmflabs.org/osm/{z}/{x}/{y}.png',
      name: 'OpenStreetMap Wikimedia'
    },
    {
      url: 'https://{s}.basemaps.cartocdn.com/positron/{z}/{x}/{y}{r}.png',
      name: 'CartoDB Positron (蓝色)'
    },
    {
      url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
      name: 'OpenStreetMap (蓝色)'
    }
  ]
  
  const darkTileServers = [
  {
      url: 'https://tile.openstreetmap.de/{z}/{x}/{y}.png',
      name: 'OpenStreetMap DE (蓝色)'
    },
    {
      url: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
      name: 'CartoDB Dark Matter (推荐)'
    },
    {
      url: 'https://{s}.basemaps.cartocdn.com/dark_nolabels/{z}/{x}/{y}{r}.png',
      name: 'CartoDB Dark No Labels'
    },
    {
      url: 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png',
      name: 'CartoDB Light'
    },
    {
      url: 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
      name: 'CartoDB Voyager (深蓝色)'
    },
    {
      url: 'https://tile.openstreetmap.de/{z}/{x}/{y}.png',
      name: 'OpenStreetMap DE (蓝色)'
    }
  ]
  
  const satelliteServers = [
    {
      url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
      name: 'Esri World Imagery'
    },
    {
      url: 'https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}',
      name: 'Google Satellite'
    }
  ]
  
  // 根据模式选择瓦片服务器
  const tileServers = isDarkMode.value ? darkTileServers : lightTileServers
  
  // 添加地图图层
  if (mapViewMode.value === 'satellite') {
    // 卫星图层
    const server = satelliteServers[0]
    currentTileLayer = L.tileLayer(server.url, {
      attribution: '&copy; <a href="https://www.esri.com/">Esri</a> contributors',
      maxZoom: 18
    }).addTo(mapInstance)
  } else {
    // 标准地图图层
    const server = tileServers[0]
    
    // 为CartoDB瓦片服务添加子域名配置
    const tileLayerOptions = {
      maxZoom: 18
    }
    
    // 根据不同的瓦片服务设置不同的attribution
    if (server.url.includes('cartodb') || server.url.includes('cartocdn')) {
      tileLayerOptions.subdomains = 'abcd'
      if (server.url.includes('dark_all')) {
        tileLayerOptions.attribution = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
      } else if (server.url.includes('voyager')) {
        tileLayerOptions.attribution = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
      } else if (server.url.includes('positron')) {
        tileLayerOptions.attribution = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
      } else {
        tileLayerOptions.attribution = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
      }
    } else {
      tileLayerOptions.attribution = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }
    
    currentTileLayer = L.tileLayer(server.url, tileLayerOptions).addTo(mapInstance)
  }
  
  console.log(`地图服务已更新: ${mapViewMode.value === '卫星' ? '卫星' : '标准'}视图，${isDarkMode.value ? '深色' : '浅色'}主题`)
}

const updateMapMarkers = () => {
  if (!mapInstance) return
  
  // 清除现有标记
  mapMarkers.forEach(marker => {
    mapInstance.removeLayer(marker)
  })
  mapMarkers = []
  
  // 创建摄像头设备图标 - 放大图标尺寸
  const deviceIcon = L.icon({
    iconUrl: new URL('@/assets/人员备份 2.png', import.meta.url).href,
    iconSize: [40, 40], // 增大图标尺寸，更清晰可见
    iconAnchor: [20, 20],
    popupAnchor: [0, -20]
  })
  
  // 添加设备标记
  filteredCameraMarkers.value.forEach((markerData, index) => {
    const marker = L.marker([markerData.lat, markerData.lng], { icon: deviceIcon })
      .bindPopup(`
        <div style="text-align: center; min-width: 150px;">
          <strong>📹 ${markerData.name}</strong><br>
          <small style="color: #666;">经度: ${markerData.lng.toFixed(6)}</small><br>
          <small style="color: #666;">纬度: ${markerData.lat.toFixed(6)}</small><br>
          <small style="color: #666;">点击查看视频</small>
        </div>
      `)
      .on('click', () => handleCameraClick(markerData))
      .addTo(mapInstance)
    
    mapMarkers.push(marker)
  })
}

const loadDeviceList = async () => {
  loading.value = true
  try {
    console.log('视频广场：使用固定的真实设备数据...')
    
    // 直接使用固定的真实设备数据
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }

    const response = await getDeviceList(params)
    deviceList.value = response.data.records || []
    
    // 过滤出有视频流且在线的设备
    realCameraStreams.value = deviceList.value.filter(device => {
      return device.streamUrl || device.originalRtspUrl || device.rtspUrl
    })
    
    console.log('视频广场：有效设备数量:', realCameraStreams.value.length)
    
    // 更新地图标记（如果地图已初始化）
    if (mapInstance && mapInitialized) {
      await nextTick(() => {
        updateMapMarkers()
        // 默认最大化显示全部摄像头
        if (filteredCameraMarkers.value.length > 0) {
          setTimeout(() => {
            fitBounds()
          }, 300) // 延迟确保标记已添加并渲染完成
        }
      })
    }
    
    // ElMessage.success(`已加载 ${realCameraStreams.value.length} 个固定设备，地图将自动最大化显示全部摄像头位置`)
    
  } catch (error) {
    console.error('视频广场：加载设备列表失败:', error)
    ElMessage.error('加载设备列表失败')
  } finally {
    loading.value = false
  }
}

const openVideoDialogs = async (count) => {
  console.log(`打开${count}画面视频播放，count:`, count)
  console.log('realCameraStreams.value:', realCameraStreams.value)
  
  // 获取可用设备
  const availableDevices = realCameraStreams.value.slice(0, count)
  
  if (availableDevices.length === 0) {
    ElMessage.warning('暂无可用设备')
    return
  }
  
  // 显示加载提示
  const loading = ElMessage({
    message: `正在准备 ${availableDevices.length} 个视频播放窗口...`,
    type: 'info',
    duration: 0
  })
  
  try {
    // 为每个设备准备WebRTC播放URL
    const preparedDevices = []
    
    for (const device of availableDevices) {
      const streamUrl = device.streamUrl ||
        device.originalRtspUrl ||
        device.rtspUrl ||
        device.url

      const isCameraRtc = isCameraRtcStream(streamUrl)

      const deviceData = {
        ...device,
        streamUrl,
        // Use direct WebRTC playback like handleCameraClick (no iframe HTML)
        playMode: isCameraRtc ? 'cameraRTC' : 'webrtc'
      }

      // Force direct WebRTC player by removing iframe url if present
      if (deviceData.webrtcUrl) {
        delete deviceData.webrtcUrl
      }

      const coords = getDeviceCoordinates(device) || { lat: 39.9042, lng: 116.4074 }

      const cameraData = {
        id: device.id,
        name: device.deviceName,
        lat: coords.lat,
        lng: coords.lng,
        hasVideo: true,
        deviceData
      }
      
      preparedDevices.push(cameraData)
    }
    
    // 创建布局弹窗
    const dialogId = nextLayoutDialogId.value++
    maxLayoutZIndex.value++
    
    // 确保 cameras 数组长度与 layoutCount 一致，用 null 填充空白窗口
    const cameras = [...preparedDevices]
    while (cameras.length < count) {
      cameras.push(null)
    }
    
    const newDialog = {
      id: dialogId,
      cameras: cameras, // 存储所有摄像头，包括空白窗口
      layoutCount: count, // 布局数量
      position: { 
        x: 50, 
        y: 50 
      },
      minimized: false,
      zIndex: maxLayoutZIndex.value,
      recording: {
        isRecording: false,
        recordId: null,
        startTime: null,
        timer: null,
        currentRecordTime: '00:00'
      }
    }
    
    // 清空现有弹窗并添加新弹窗
    layoutDialogs.value = [newDialog]
    
    // 更新布局模式
    layoutMode.value = count === 1 ? '1x1' : count === 4 ? '2x2' : count === 6 ? '3x3' : count === 8 ? '4x4' : count === 9 ? '5x5' : '6x6'
    
    loading.close()
    ElMessage.success(`已打开${count}画面视频播放`)
    
  } catch (error) {
    console.error('创建布局弹窗失败:', error)
    loading.close()
    ElMessage.error('创建视频播放窗口失败')
  }
}

const openCustomVideoDialogs = () => {
  ElMessage.info('自定义视频播放功能')
}

const showExtendedLayoutDialog = () => {
  // 如果弹窗已经显示，则隐藏它
  if (showExtendedLayout.value) {
    showExtendedLayout.value = false
    console.log('隐藏扩展布局弹窗')
    return
  }
  
  // 计算按钮位置
  nextTick(() => {
    const button = document.querySelector('.extended-layout-btn')
    if (button) {
      const rect = button.getBoundingClientRect()
      const dialogWidth = 200 // 弹窗宽度
      
      // 计算弹窗位置：按钮下方，水平居中
      const left = rect.left + (rect.width / 2) - (dialogWidth / 2)
      const top = rect.bottom + 8 // 按钮下方8px间距
      
      extendedLayoutPosition.value = {
        top: `${top}px`,
        left: `${left}px`
      }
    }
  })
  
  showExtendedLayout.value = true
  console.log('显示扩展布局弹窗')
  
  // 确保弹窗在显示后立即应用深色模式样式
  nextTick(() => {
    setTimeout(() => {
      applyExtendedLayoutDarkMode()
    }, 50)
  })
}

const hideExtendedLayoutDialog = () => {
  showExtendedLayout.value = false
  console.log('隐藏扩展布局弹窗')
}

// 处理扩展布局选项选择
const handleExtendedLayoutSelect = (count) => {
  // 先关闭扩展布局弹窗
  showExtendedLayout.value = false
  
  // 然后打开对应的视频弹窗
  openVideoDialogs(count)
  
  console.log('选择扩展布局:', count)
}

// 专门应用扩展布局弹窗深色模式样式的函数
const applyExtendedLayoutDarkMode = () => {
  if (!isDarkMode.value) return
  
  const extendedLayoutDialog = document.querySelector('.extended-layout-dialog')
  if (extendedLayoutDialog) {
    // 强制应用弹窗背景样式
    extendedLayoutDialog.style.setProperty('background', 'rgba(40, 44, 52, 0.95)', 'important')
    extendedLayoutDialog.style.setProperty('border', '1px solid rgba(100, 149, 237, 0.3)', 'important')
    extendedLayoutDialog.style.setProperty('box-shadow', '0 8px 24px rgba(0, 0, 0, 0.5)', 'important')
    extendedLayoutDialog.style.setProperty('backdrop-filter', 'blur(8px)', 'important')
    
    // 强制应用选项样式
    const layoutOptions = extendedLayoutDialog.querySelectorAll('.extended-layout-option')
    layoutOptions.forEach(option => {
      option.style.setProperty('color', '#ffffff', 'important')
    })
    
    // 强制应用选项文字样式
    const layoutOptionTexts = extendedLayoutDialog.querySelectorAll('.layout-option-text')
    layoutOptionTexts.forEach(text => {
      text.style.setProperty('color', '#ffffff', 'important')
    })
    
    // 强制应用图标样式
    const layoutOptionIcons = extendedLayoutDialog.querySelectorAll('.layout-option-icon')
    layoutOptionIcons.forEach(icon => {
      icon.style.setProperty('color', '#7db8ff', 'important')
    })
    
    console.log('强制应用扩展布局弹窗深色模式样式')
  }
}

const bringLayoutToFront = (dialogId) => {
  const dialog = layoutDialogs.value.find(d => d.id === dialogId)
  if (dialog) {
    maxLayoutZIndex.value++
    dialog.zIndex = maxLayoutZIndex.value
    console.log('布局弹窗置顶:', dialogId, 'zIndex:', dialog.zIndex)
  }
}

const startLayoutDrag = (event, dialogId) => {
  console.log('开始拖拽布局弹窗:', dialogId)
  
  const dialog = layoutDialogs.value.find(d => d.id === dialogId)
  if (!dialog) return
  
  // 防止默认行为
  event.preventDefault()
  
  // 获取鼠标相对于弹窗的偏移量
  const rect = event.currentTarget.closest('.layout-dialog').getBoundingClientRect()
  const offsetX = event.clientX - rect.left
  const offsetY = event.clientY - rect.top
  
  // 鼠标移动处理函数
  const handleMouseMove = (e) => {
    const newX = e.clientX - offsetX
    const newY = e.clientY - offsetY
    
    // 限制拖拽范围，确保弹窗不会超出视窗
    const maxX = window.innerWidth - 200 // 至少保留200px可见
    const maxY = window.innerHeight - 100 // 至少保留100px可见
    
    dialog.position.x = Math.max(0, Math.min(newX, maxX))
    dialog.position.y = Math.max(0, Math.min(newY, maxY))
  }
  
  // 鼠标释放处理函数
  const handleMouseUp = () => {
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
    console.log('结束拖拽布局弹窗:', dialogId)
  }
  
  // 添加事件监听器
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

const minimizeLayoutDialog = (dialogId) => {
  const dialog = layoutDialogs.value.find(d => d.id === dialogId)
  if (dialog) {
    dialog.minimized = !dialog.minimized
    console.log('布局弹窗最小化状态:', dialog.minimized)
  }
}

const closeLayoutDialog = (dialogId) => {
  const dialogIndex = layoutDialogs.value.findIndex(dialog => dialog.id === dialogId)
  if (dialogIndex === -1) return
  
  cleanupDialogWebRTC(dialogId)
 
  // 清除计时器
  // if (dialog.recording?.timer) {
  //   clearInterval(dialog.recording.timer)
  // }
  
  layoutDialogs.value.splice(dialogIndex, 1)
  console.log('关闭布局弹窗:', dialogId)
}

// 视频窗口点击处理
const handleVideoWindowClick = (dialogId, windowIndex) => {
  const dialog = layoutDialogs.value.find(d => d.id === dialogId)
  if (!dialog) return
  
  console.log('点击视频窗口:', dialogId, '窗口索引:', windowIndex)
}

// 视频窗口双击处理
const handleVideoWindowDoubleClick = (dialogId, windowIndex) => {
  const dialog = layoutDialogs.value.find(d => d.id === dialogId)
  if (!dialog) return
  
  console.log('双击视频窗口:', dialogId, '窗口索引:', windowIndex)
  
  const clickedCamera = dialog.cameras[windowIndex]
  
  if (clickedCamera) {
    // 有内容的窗口 - 关闭分屏弹窗，打开单画面播放
    console.log(`双击布局弹窗中的摄像头: ${clickedCamera.name}，准备打开单画面播放`)
    
    // 关闭当前分屏弹窗
    closeLayoutDialog(dialogId)
    
    // 将该设备设置为第一个设备，然后打开单画面播放
    const availableDevices = realCameraStreams.value
    if (availableDevices.length > 0) {
      // 找到当前摄像头在设备列表中的索引
      const deviceIndex = availableDevices.findIndex(device => device.id === clickedCamera.id)
      if (deviceIndex !== -1) {
        // 将该设备移到第一个位置
        const device = availableDevices.splice(deviceIndex, 1)[0]
        availableDevices.unshift(device)
        console.log('已将设备移到第一位:', device.deviceName)
      }
    }
    
    // 打开单画面播放
    openVideoDialogs(1)
    
    // 显示操作提示
    ElMessage.success(`${clickedCamera.name} 已切换到单画面播放`)
  } else {
    // 空白窗口
    console.log(`双击空白窗口: ${windowIndex}`)
    ElMessage.info('空白窗口，请先添加设备')
  }
}

// 处理布局选择
const handleLayoutSelect = (count) => {
  console.log('主文件收到布局选择事件，count:', count)
  openVideoDialogs(count)
}

// 视频窗口交换位置
const handleVideoWindowSwap = (sourceDialogId, sourceIndex, targetDialogId, targetIndex) => {
  const sourceDialog = layoutDialogs.value.find(d => d.id === sourceDialogId)
  const targetDialog = layoutDialogs.value.find(d => d.id === targetDialogId)
  
  if (!sourceDialog || !targetDialog) return
  
  // 获取源窗口和目标窗口的摄像头数据
  const sourceCamera = sourceDialog.cameras[sourceIndex]
  const targetCamera = targetDialog.cameras[targetIndex]
  
  // 执行交换
  sourceDialog.cameras[sourceIndex] = targetCamera
  targetDialog.cameras[targetIndex] = sourceCamera
  
  console.log('视频窗口交换完成:', {
    source: { dialogId: sourceDialogId, windowIndex: sourceIndex, camera: sourceCamera?.name },
    target: { dialogId: targetDialogId, windowIndex: targetIndex, camera: targetCamera?.name }
  })
  
  ElMessage.success('视频窗口位置已交换')
}



const toggleFullscreen = () => {
  console.log('切换视频播放器全屏')
  
  // 检查是否有打开的视频播放器（包括单个视频弹窗和布局弹窗）
  const hasOpenDialogs = videoDialogs.value.length > 0 || layoutDialogs.value.length > 0
  
  if (!hasOpenDialogs) {
    ElMessage.warning('请先打开视频播放器')
    return
  }
  
  if (!isFullscreen.value) {
    // 进入全屏模式
    enterVideoPlayerFullscreen()
  } else {
    // 退出全屏模式
    exitVideoPlayerFullscreen()
  }
}

// 进入视频播放器全屏模式
const enterVideoPlayerFullscreen = () => {
  // 查找所有视频播放器弹窗
  const videoDialogs = document.querySelectorAll('.video-dialog, .layout-dialog')
  
  if (videoDialogs.length === 0) {
    ElMessage.error('未找到视频播放器')
    return
  }
  
  // 为每个视频播放器添加全屏样式
  videoDialogs.forEach(dialog => {
    dialog.classList.add('fullscreen-active')
  })
  
  isFullscreen.value = true
  ElMessage.success('视频播放器已进入全屏模式')
  
  // 添加ESC键监听
  document.addEventListener('keydown', handleEscKey)
  
  // 使用浏览器全屏API
  const firstDialog = videoDialogs[0]
  if (firstDialog && firstDialog.requestFullscreen) {
    firstDialog.requestFullscreen().catch(err => {
      console.log('浏览器全屏API失败，使用自定义全屏:', err)
    })
  }
}

// 退出视频播放器全屏模式
const exitVideoPlayerFullscreen = () => {
  // 查找所有视频播放器弹窗
  const videoDialogs = document.querySelectorAll('.video-dialog, .layout-dialog')
  
  // 移除全屏样式class
  videoDialogs.forEach(dialog => {
    dialog.classList.remove('fullscreen-active')
  })
  
  isFullscreen.value = false
  ElMessage.info('已退出全屏模式')
  
  // 移除ESC键监听
  document.removeEventListener('keydown', handleEscKey)
  
  // 退出浏览器全屏API（如果正在使用）
  if (document.exitFullscreen && document.fullscreenElement) {
    document.exitFullscreen().catch(err => {
      console.log('退出浏览器全屏失败:', err)
    })
  }
}

// ESC键处理函数
const handleEscKey = (event) => {
  if (event.key === 'Escape' && isFullscreen.value) {
    exitVideoPlayerFullscreen()
  }
}

// 监听全屏状态变化
const handleFullscreenChange = () => {
  // 检测浏览器全屏状态
  const isCurrentlyFullscreen = !!(
    document.fullscreenElement ||
    document.webkitFullscreenElement ||
    document.mozFullScreenElement ||
    document.msFullscreenElement
  )
  
  if (!isCurrentlyFullscreen && isFullscreen.value) {
    // 用户通过浏览器的ESC键或其他方式退出全屏
    const videoDialogs = document.querySelectorAll('.video-dialog, .layout-dialog')
    videoDialogs.forEach(dialog => {
      dialog.classList.remove('fullscreen-active')
    })
    isFullscreen.value = false
    document.removeEventListener('keydown', handleEscKey)
  }
}

// 添加缺失的函数
const handlePTZControl = (action, camera) => {
  console.log('PTZ控制:', action, camera)
  ElMessage.info(`PTZ控制: ${action}`)
}

const handleZoomControl = (action, camera) => {
  console.log('缩放控制:', action, camera)
  ElMessage.info(`缩放控制: ${action}`)
}

const handleControlAction = (action, camera) => {
  console.log('控制动作:', action, camera)
  ElMessage.info(`控制动作: ${action}`)
}

const handleSettings = () => {
  showSettingsDialog.value = true
}

const handleMapSearch = () => {
  console.log('地图搜索:', mapSearchKeyword.value)
  updateMapMarkers()
  
  if (filteredCameraMarkers.value.length === 0) {
    ElMessage.warning('未找到匹配的设备')
  } else {
    ElMessage.success(`找到 ${filteredCameraMarkers.value.length} 个匹配的设备`)
  }
}



const handleCameraClick = async (deviceData) => {
  // 兼容不同数据来源：地图标记(markerData)和设备列表(device)
  const deviceName = deviceData.name || deviceData.deviceName || '未知设备'
  const cameraData = deviceData.deviceData || deviceData
  
  console.log('点击摄像头:', deviceName, '设备数据:', cameraData)
  
  // 确保设备数据包含流URL信息
  let processedCameraData = { ...cameraData }
  
  // 如果没有streamUrl，尝试从其他字段获取
  if (!processedCameraData.streamUrl) {
    processedCameraData.streamUrl = processedCameraData.rtspUrl || 
                                   processedCameraData.originalRtspUrl || 
                                   processedCameraData.url ||
                                   'rtsp://admin:password@192.168.1.100/stream'  // 默认测试流
  }
  
  // 优先尝试直接使用WebRTC直连播放
  const isCameraRtc = isCameraRtcStream(processedCameraData.streamUrl)
  processedCameraData.playMode = isCameraRtc ? 'cameraRTC' : 'webrtc'

  // 打开单画面视频播放
  const dialog = {
    id: nextDialogId.value++,
    camera: {
      name: deviceName,
      deviceData: processedCameraData
    },
    position: { x: 150 + (videoDialogs.value.length * 50), y: 150 + (videoDialogs.value.length * 50) },
    minimized: false,
    isDraggable: true  // 确保弹窗可拖动
  }
  
  videoDialogs.value.push(dialog)
  await nextTick();
  if (isCameraRtc) {
    await playDialogCameraRTC(dialog)
  } else {
    await playDialogWebRTC(dialog)
  }
  ElMessage.success(`正在播放: ${deviceName}`)
}

const handleTreeNodeClick = (node) => {
  if (node.type === 'device') {
    console.log('选择设备:', node.label)
    
    // 查找对应的设备数据
    if (node.id === 14 || node.id === 13) {
      const deviceData = deviceList.value.find(device => device.id === node.id)
      if (deviceData) {
        const coords = getDeviceCoordinates(deviceData) || { lat: 39.9042, lng: 116.4074 }
        const cameraMarker = {
          id: deviceData.id,
          name: deviceData.deviceName,
          lat: coords.lat,
          lng: coords.lng,
          hasVideo: true,
          deviceData: deviceData
        }
        
        handleCameraClick(cameraMarker)
        ElMessage.success(`正在播放设备: ${node.label}`)
        return
      }
    }
    
    ElMessage.info(`选择演示设备: ${node.label}`)
  } else if (node.type === 'group') {
    node.expanded = !node.expanded
  }
}

const getDeviceStatusClass = (status) => {
  return isDeviceOnline(status) ? 'status-online' : 'status-offline'
}

const getDeviceStatusText = (status) => {
  return isDeviceOnline(status) ? '在线' : '离线'
}

const fitBounds = () => {
  if (mapInstance && mapInitialized && isComponentMounted) {
    try {
      if (filteredCameraMarkers.value.length === 0 || mapMarkers.length === 0) {
        // 没有设备或标记时，显示默认视图
        mapInstance.setView([35.8617, 104.1954], 6) // 适中的中国地图视图
        console.log('没有摄像头标记，显示默认地图视图')
        ElMessage.info('已重置地图视图')
      } else {
        // 有设备标记时，自动调整视图显示所有标记并最大化
        const group = new L.featureGroup(mapMarkers)
        
        // 获取所有标记的边界
        const bounds = group.getBounds()
        
        // 如果只有一个标记，使用高放大级别
        if (mapMarkers.length === 1) {
          const center = bounds.getCenter()
          mapInstance.setView([center.lat, center.lng], 18) // 单个设备时最大放大
        } else {
          // 多个标记时，使用最大化放大显示所有设备
          mapInstance.fitBounds(bounds, { 
            padding: [10, 10], // 最小边距，最大化利用屏幕空间
            maxZoom: 20 // 允许非常高的缩放级别
          })
        }
        // console.log(`自动最大化显示所有摄像头位置，共 ${mapMarkers.length} 个标记`)
        // ElMessage.success(`已自动最大化显示所有 ${filteredCameraMarkers.value.length} 个摄像头位置`)
      }
    } catch (error) {
      console.error('调整地图视图失败:', error)
      ElMessage.error('调整地图视图失败')
      // 失败时回退到默认视图
      try {
        mapInstance.setView([35.8617, 104.1954], 6) // 回退到适中的地图视图
      } catch (fallbackError) {
        console.error('回退到默认视图也失败:', fallbackError)
      }
    }
  } else {
    console.warn('地图未初始化或组件未挂载')
    ElMessage.warning('地图未初始化')
  }
}

const toggleMapView = () => {
  mapViewMode.value = mapViewMode.value === 'normal' ? 'satellite' : 'normal'
  updateMapLayer()
  ElMessage.success(`已切换到${mapViewMode.value === 'normal' ? '标准' : '卫星'}地图`)
}

const refreshCameraLocations = () => {
  updateMapMarkers()
  // ElMessage.success(`设备位置已刷新，共 ${filteredCameraMarkers.value.length} 个设备`)
}

const toggleDarkMode = () => {
  isDarkMode.value = !isDarkMode.value
  
  // 更新地图图层为深色/浅色主题
  updateMapLayer()
  
  // 为整个地图区域添加深色主题类（这将控制所有深色模式样式）
  const mapArea = document.querySelector('.map-area')
  if (mapArea) {
    if (isDarkMode.value) {
      mapArea.classList.add('dark-mode')
      // 给body也添加dark-mode类，用于全局弹窗样式
      document.body.classList.add('dark-mode')
      console.log('添加深色模式类，当前类名:', mapArea.className)
      // 添加深蓝色遮罩层
      addDarkOverlay()
    } else {
      mapArea.classList.remove('dark-mode')
      // 移除body的dark-mode类
      document.body.classList.remove('dark-mode')
      console.log('移除深色模式类，当前类名:', mapArea.className)
      // 移除深蓝色遮罩层
      removeDarkOverlay()
    }
  } else {
    console.error('未找到.map-area元素')
  }
  
  // 强制应用深色模式样式
  setTimeout(() => {
    // 如果扩展布局弹窗正在显示，立即应用深色模式样式
    if (showExtendedLayout.value) {
      applyExtendedLayoutDarkMode()
    }
    const layoutButtonGroup = document.querySelector('.layout-button-group')
    const deviceStats = document.querySelector('.device-stats')
    const deviceListPanel = document.querySelector('.device-list-panel')
    const extendedLayoutBtn = document.querySelector('.extended-layout-btn')
    const extendedLayoutDialog = document.querySelector('.extended-layout-dialog')
    
    console.log('布局按钮组元素:', layoutButtonGroup)
    console.log('设备统计元素:', deviceStats)
    console.log('设备列表面板元素:', deviceListPanel)
    console.log('扩展布局按钮元素:', extendedLayoutBtn)
    console.log('扩展布局弹窗元素:', extendedLayoutDialog)
    
    if (isDarkMode.value) {
      // 强制应用深色模式样式
      if (layoutButtonGroup) {
        layoutButtonGroup.style.background = 'rgba(40, 44, 52, 0.95)'
        layoutButtonGroup.style.border = '1px solid rgba(100, 149, 237, 0.3)'
        layoutButtonGroup.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.3)'
        layoutButtonGroup.style.backdropFilter = 'blur(8px)'
        console.log('强制应用布局按钮组深色样式')
      }
      if (deviceStats) {
        deviceStats.style.background = 'rgba(40, 44, 52, 0.95)'
        deviceStats.style.border = '1px solid rgba(100, 149, 237, 0.3)'
        deviceStats.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.3)'
        deviceStats.style.backdropFilter = 'blur(8px)'
        deviceStats.style.color = '#e8f4fd'
        console.log('强制应用设备统计深色样式')
      }
      
      // 强制应用扩展布局按钮深色样式
      if (extendedLayoutBtn) {
        extendedLayoutBtn.style.background = 'rgba(40, 44, 52, 0.95)'
        extendedLayoutBtn.style.borderColor = 'rgba(100, 149, 237, 0.3)'
        extendedLayoutBtn.style.color = '#ffffff'
        extendedLayoutBtn.style.boxShadow = '0 2px 8px rgba(0, 0, 0, 0.3)'
        console.log('强制应用扩展布局按钮深色样式')
      }
      
      // 强制应用扩展布局弹窗深色样式
      if (extendedLayoutDialog) {
        extendedLayoutDialog.style.background = 'rgba(40, 44, 52, 0.95)'
        extendedLayoutDialog.style.border = '1px solid rgba(100, 149, 237, 0.3)'
        extendedLayoutDialog.style.boxShadow = '0 8px 24px rgba(0, 0, 0, 0.5)'
        extendedLayoutDialog.style.backdropFilter = 'blur(8px)'
        
        // 强制应用弹窗内选项的深色样式
        const layoutOptions = extendedLayoutDialog.querySelectorAll('.extended-layout-option')
        layoutOptions.forEach(option => {
          option.style.color = '#ffffff'
        })
        
        const layoutOptionTexts = extendedLayoutDialog.querySelectorAll('.layout-option-text')
        layoutOptionTexts.forEach(text => {
          text.style.color = '#ffffff'
        })
        
        const layoutOptionIcons = extendedLayoutDialog.querySelectorAll('.layout-option-icon')
        layoutOptionIcons.forEach(icon => {
          icon.style.color = '#7db8ff'
        })
        
        console.log('强制应用扩展布局弹窗深色样式')
      }
      
      if (deviceListPanel) {
        deviceListPanel.style.background = 'rgba(40, 44, 52, 0.95)'
        deviceListPanel.style.border = '1px solid rgba(100, 149, 237, 0.3)'
        deviceListPanel.style.boxShadow = '0 8px 32px rgba(0, 0, 0, 0.3)'
        deviceListPanel.style.backdropFilter = 'blur(8px)'
        
        // 强制应用设备列表内部元素样式
        const deviceListContent = deviceListPanel.querySelector('.device-list-content')
        const deviceListItems = deviceListPanel.querySelector('.device-list-items')
        const deviceListHeader = deviceListPanel.querySelector('.device-list-header')
        const selectedDisplayItems = deviceListPanel.querySelector('.selected-display-items')
        const deviceSearchContainer = deviceListPanel.querySelector('.device-search-container')
        
        if (deviceListContent) {
          deviceListContent.style.background = 'rgba(40, 44, 52, 0.95)'
        }
        if (deviceListItems) {
          deviceListItems.style.background = 'rgba(40, 44, 52, 0.95)'
        }
        if (deviceListHeader) {
          deviceListHeader.style.background = 'rgba(30, 34, 42, 0.95)'
          deviceListHeader.style.borderBottom = '1px solid rgba(100, 149, 237, 0.3)'
        }
        if (selectedDisplayItems) {
          selectedDisplayItems.style.background = 'rgba(30, 34, 42, 0.8)'
          selectedDisplayItems.style.borderBottom = '1px solid rgba(100, 149, 237, 0.3)'
        }
                 if (deviceSearchContainer) {
           deviceSearchContainer.style.background = 'rgba(30, 34, 42, 0.8)'
           deviceSearchContainer.style.borderBottom = '1px solid rgba(100, 149, 237, 0.3)'
         }
         
         // 强制应用文字颜色
         const deviceListTitle = deviceListPanel.querySelector('.device-list-title')
         const deviceNames = deviceListPanel.querySelectorAll('.device-name')
         const deviceDetails = deviceListPanel.querySelectorAll('.device-details')
         const tagLabels = deviceListPanel.querySelectorAll('.tag-label')
         const displayItemsTitle = deviceListPanel.querySelector('.display-items-title')
         const deviceSearchInput = deviceListPanel.querySelector('.device-search-input')
         // 树形结构相关元素
         const treeDeviceNames = deviceListPanel.querySelectorAll('.tree-device-name')
         const treeDeviceDetails = deviceListPanel.querySelectorAll('.tree-device-details')
         const treeGroupLabels = deviceListPanel.querySelectorAll('.tree-group-label')
         
         if (deviceListTitle) {
           deviceListTitle.style.color = '#ffffff'
         }
         if (displayItemsTitle) {
           displayItemsTitle.style.color = '#ffffff'
         }
         if (deviceSearchInput) {
           deviceSearchInput.style.color = '#ffffff'
         }
         
         deviceNames.forEach(name => {
           name.style.setProperty('color', '#ffffff', 'important')
           name.style.setProperty('font-weight', '500', 'important')
         })
         
         deviceDetails.forEach(detail => {
           detail.style.setProperty('color', '#a0a0a0', 'important')
         })
         
         tagLabels.forEach(label => {
           label.style.color = '#ffffff'
         })
         
         // 强制应用树形结构样式
         treeDeviceNames.forEach(name => {
           name.style.setProperty('color', '#ffffff', 'important')
           name.style.setProperty('font-weight', '500', 'important')
         })
         
         treeDeviceDetails.forEach(detail => {
           detail.style.setProperty('color', '#a0a0a0', 'important')
         })
         
         treeGroupLabels.forEach(label => {
           label.style.setProperty('color', '#ffffff', 'important')
           label.style.setProperty('font-weight', '600', 'important')
         })
         
         console.log('强制应用设备列表面板深色样式')
      }
    } else {
      // 恢复浅色模式样式
      if (layoutButtonGroup) {
        layoutButtonGroup.style.background = 'rgba(255, 255, 255, 0.95)'
        layoutButtonGroup.style.border = '1px solid rgba(255, 255, 255, 0.2)'
        layoutButtonGroup.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.15)'
        console.log('恢复布局按钮组浅色样式')
      }
      if (deviceStats) {
        deviceStats.style.background = 'rgba(255, 255, 255, 0.95)'
        deviceStats.style.border = '1px solid rgba(255, 255, 255, 0.2)'
        deviceStats.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.15)'
        deviceStats.style.color = ''
        console.log('恢复设备统计浅色样式')
      }
      
      // 恢复扩展布局按钮浅色样式
      if (extendedLayoutBtn) {
        extendedLayoutBtn.style.background = '#f8f9fa'
        extendedLayoutBtn.style.borderColor = '#ddd'
        extendedLayoutBtn.style.color = '#666'
        extendedLayoutBtn.style.boxShadow = ''
        console.log('恢复扩展布局按钮浅色样式')
      }
      
      // 恢复扩展布局弹窗浅色样式
      if (extendedLayoutDialog) {
        extendedLayoutDialog.style.background = 'white'
        extendedLayoutDialog.style.border = '1px solid #e0e0e0'
        extendedLayoutDialog.style.boxShadow = '0 8px 24px rgba(0, 0, 0, 0.2)'
        extendedLayoutDialog.style.backdropFilter = ''
        
        // 恢复弹窗内选项的浅色样式
        const layoutOptions = extendedLayoutDialog.querySelectorAll('.extended-layout-option')
        layoutOptions.forEach(option => {
          option.style.color = '#333'
        })
        
        const layoutOptionTexts = extendedLayoutDialog.querySelectorAll('.layout-option-text')
        layoutOptionTexts.forEach(text => {
          text.style.color = '#333'
        })
        
        const layoutOptionIcons = extendedLayoutDialog.querySelectorAll('.layout-option-icon')
        layoutOptionIcons.forEach(icon => {
          icon.style.color = '#1A53FF'
        })
        
        console.log('恢复扩展布局弹窗浅色样式')
      }
      
      if (deviceListPanel) {
         deviceListPanel.style.background = 'rgba(255, 255, 255, 0.95)'
         deviceListPanel.style.border = '1px solid rgba(255, 255, 255, 0.2)'
         deviceListPanel.style.boxShadow = '0 8px 32px rgba(0, 0, 0, 0.1)'
         
         // 恢复设备列表内部元素样式
         const deviceListContent = deviceListPanel.querySelector('.device-list-content')
         const deviceListItems = deviceListPanel.querySelector('.device-list-items')
         const deviceListHeader = deviceListPanel.querySelector('.device-list-header')
         const selectedDisplayItems = deviceListPanel.querySelector('.selected-display-items')
         const deviceSearchContainer = deviceListPanel.querySelector('.device-search-container')
         
         if (deviceListContent) {
           deviceListContent.style.background = ''
         }
         if (deviceListItems) {
           deviceListItems.style.background = ''
         }
         if (deviceListHeader) {
           deviceListHeader.style.background = 'rgba(248, 249, 250, 0.8)'
           deviceListHeader.style.borderBottom = '1px solid #e4e7ed'
         }
         if (selectedDisplayItems) {
           selectedDisplayItems.style.background = 'rgba(248, 249, 250, 0.8)'
           selectedDisplayItems.style.borderBottom = '1px solid #e4e7ed'
         }
                   if (deviceSearchContainer) {
            deviceSearchContainer.style.background = 'rgba(248, 249, 250, 0.8)'
            deviceSearchContainer.style.borderBottom = '1px solid #e4e7ed'
          }
          
          // 恢复文字颜色
          const deviceListTitle = deviceListPanel.querySelector('.device-list-title')
          const deviceNames = deviceListPanel.querySelectorAll('.device-name')
          const deviceDetails = deviceListPanel.querySelectorAll('.device-details')
          const tagLabels = deviceListPanel.querySelectorAll('.tag-label')
          const displayItemsTitle = deviceListPanel.querySelector('.display-items-title')
          const deviceSearchInput = deviceListPanel.querySelector('.device-search-input')
          // 树形结构相关元素
          const treeDeviceNames = deviceListPanel.querySelectorAll('.tree-device-name')
          const treeDeviceDetails = deviceListPanel.querySelectorAll('.tree-device-details')
          const treeGroupLabels = deviceListPanel.querySelectorAll('.tree-group-label')
          
          if (deviceListTitle) {
            deviceListTitle.style.color = '#303133'
          }
          if (displayItemsTitle) {
            displayItemsTitle.style.color = '#606266'
          }
          if (deviceSearchInput) {
            deviceSearchInput.style.color = '#606266'
          }
          
                     deviceNames.forEach(name => {
             name.style.setProperty('color', '#303133', 'important')
             name.style.setProperty('font-weight', '500', 'important')
           })
           
           deviceDetails.forEach(detail => {
             detail.style.setProperty('color', '#909399', 'important')
           })
          
          tagLabels.forEach(label => {
            label.style.color = '#606266'
          })
          
                     // 恢复树形结构样式
           treeDeviceNames.forEach(name => {
             name.style.setProperty('color', '#303133', 'important')
             name.style.setProperty('font-weight', '500', 'important')
           })
           
           treeDeviceDetails.forEach(detail => {
             detail.style.setProperty('color', '#909399', 'important')
           })
           
           treeGroupLabels.forEach(label => {
             label.style.setProperty('color', '#606266', 'important')
             label.style.setProperty('font-weight', '600', 'important')
           })
          
          console.log('恢复设备列表面板浅色样式')
       }
    }
    
    // 处理弹窗样式切换 - 查找所有可能的弹窗元素
    const popover = document.querySelector('.device-stats-popover') || 
                    document.querySelector('.el-popper.device-stats-popover') ||
                    document.querySelector('.el-popper[data-popper-placement]') ||
                    document.querySelector('.el-popper') ||
                    document.querySelector('[data-popper-placement]')
    
    if (popover) {
      if (isDarkMode.value) {
        // 应用深色模式弹窗样式
        popover.style.setProperty('background', 'rgba(40, 44, 52, 0.95)', 'important')
        popover.style.setProperty('border', 'none', 'important')
        popover.style.setProperty('box-shadow', '0 4px 12px rgba(0, 0, 0, 0.3)', 'important')
        popover.style.setProperty('color', '#e8f4fd', 'important')
        
        // 强制应用内部元素样式
        const content = popover.querySelector('.device-stats-content')
        if (content) content.style.color = '#e8f4fd'
        
        const typeTitle = popover.querySelector('.device-type-title')
        if (typeTitle) typeTitle.style.color = '#e8f4fd'
        
        const deviceIcon = popover.querySelector('.device-icon')
        if (deviceIcon) deviceIcon.style.color = '#7db8ff'
        
        const totalCount = popover.querySelector('.total-count')
        if (totalCount) {
          totalCount.style.background = 'rgba(30, 34, 42, 0.6)'
          totalCount.style.borderBottom = '1px solid rgba(100, 149, 237, 0.3)'
        }
        
        const countLabel = popover.querySelector('.count-label')
        if (countLabel) countLabel.style.color = '#b0c4de'
        
        const countNumber = popover.querySelector('.count-number')
        if (countNumber) countNumber.style.color = '#7db8ff'
        
        const locationTitles = popover.querySelectorAll('.location-title')
        locationTitles.forEach(title => {
          title.style.color = '#e8f4fd'
        })
        
        const checkboxLabels = popover.querySelectorAll('.el-checkbox__label')
        checkboxLabels.forEach(label => {
          label.style.color = '#e8f4fd'
        })
        
        const checkboxInners = popover.querySelectorAll('.el-checkbox__inner')
        checkboxInners.forEach(inner => {
          inner.style.backgroundColor = 'rgba(30, 34, 42, 0.8)'
          inner.style.borderColor = 'rgba(100, 149, 237, 0.5)'
        })
        
        const checkedInners = popover.querySelectorAll('.el-checkbox__input.is-checked .el-checkbox__inner')
        checkedInners.forEach(inner => {
          inner.style.backgroundColor = '#7db8ff'
          inner.style.borderColor = '#7db8ff'
        })
        
        console.log('强制应用弹窗深色样式')
      } else {
        // 恢复浅色模式弹窗样式
        popover.style.background = ''
        popover.style.border = 'none'
        popover.style.boxShadow = ''
        popover.style.color = ''
        
        const content = popover.querySelector('.device-stats-content')
        if (content) content.style.color = ''
        
        const typeTitle = popover.querySelector('.device-type-title')
        if (typeTitle) typeTitle.style.color = ''
        
        const deviceIcon = popover.querySelector('.device-icon')
        if (deviceIcon) deviceIcon.style.color = ''
        
        const totalCount = popover.querySelector('.total-count')
        if (totalCount) {
          totalCount.style.background = ''
          totalCount.style.borderBottom = ''
        }
        
        const countLabel = popover.querySelector('.count-label')
        if (countLabel) countLabel.style.color = ''
        
        const countNumber = popover.querySelector('.count-number')
        if (countNumber) countNumber.style.color = ''
        
        const locationTitles = popover.querySelectorAll('.location-title')
        locationTitles.forEach(title => {
          title.style.color = ''
        })
        
        const checkboxLabels = popover.querySelectorAll('.el-checkbox__label')
        checkboxLabels.forEach(label => {
          label.style.color = ''
        })
        
        const checkboxInners = popover.querySelectorAll('.el-checkbox__inner')
        checkboxInners.forEach(inner => {
          inner.style.backgroundColor = ''
          inner.style.borderColor = ''
        })
        
        console.log('恢复弹窗浅色样式')
      }
    }
  }, 100)
  
  ElMessage.success(`已切换到${isDarkMode.value ? '夜晚' : '白天'}模式`)
}

// 添加深蓝色遮罩层
const addDarkOverlay = () => {
  // 移除已存在的遮罩层
  removeDarkOverlay()
  
  // 创建深蓝色半透明遮罩层
  const overlay = document.createElement('div')
  overlay.id = 'dark-mode-overlay'
  overlay.style.cssText = `
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(135deg, 
      rgba(0, 20, 60, 0.6) 0%, 
      rgba(0, 40, 100, 0.4) 50%, 
      rgba(0, 60, 120, 0.3) 100%);
    pointer-events: none;
    z-index: 1000;
    border-radius: 8px;
    backdrop-filter: brightness(0.7) contrast(1.2);
    transition: all 0.3s ease;
  `
  
  // 找到地图容器并添加遮罩层
  const mapContainer = document.querySelector('.leaflet-container')
  if (mapContainer) {
    mapContainer.style.position = 'relative'
    mapContainer.appendChild(overlay)
    
    // 添加淡入效果
    setTimeout(() => {
      overlay.style.opacity = '1'
    }, 10)
  }
}

// 移除深蓝色遮罩层
const removeDarkOverlay = () => {
  const existingOverlay = document.getElementById('dark-mode-overlay')
  if (existingOverlay) {
    // 添加淡出效果
    existingOverlay.style.opacity = '0'
    existingOverlay.style.transition = 'opacity 0.3s ease'
    
    setTimeout(() => {
      if (existingOverlay.parentNode) {
        existingOverlay.remove()
      }
    }, 300)
  }
}

// 缩放控制方法
const zoomIn = () => {
  if (mapInstance && mapInitialized && isComponentMounted) {
    try {
      mapInstance.zoomIn()
      ElMessage.success('地图已放大')
    } catch (error) {
      console.error('地图放大失败:', error)
      ElMessage.error('地图放大失败')
    }
  } else {
    ElMessage.warning('地图未初始化')
  }
}

const zoomOut = () => {
  if (mapInstance && mapInitialized && isComponentMounted) {
    try {
      mapInstance.zoomOut()
      ElMessage.success('地图已缩小')
    } catch (error) {
      console.error('地图缩小失败:', error)
      ElMessage.error('地图缩小失败')
    }
  } else {
    ElMessage.warning('地图未初始化')
  }
}

const cancelSettings = () => {
  showSettingsDialog.value = false
}

const confirmSettings = () => {
  showSettingsDialog.value = false
  ElMessage.success('设置已保存')
}

// 视频弹窗相关方法
const closeVideoDialog = (id) => {
  const index = videoDialogs.value.findIndex(dialog => dialog.id === id)
  if (index > -1) {
    exitAnyFullscreen()
    cleanupDialogWebRTC(id)
    cleanupDialogCameraRTC(id)
    videoDialogs.value.splice(index, 1)
  }
}

const minimizeDialog = (id) => {
  const dialog = videoDialogs.value.find(d => d.id === id)
  if (dialog) {
    dialog.minimized = !dialog.minimized
  }
}

const bringToFront = (id) => {
  // 实现置顶逻辑
}

const startDrag = (event, id) => {
  event.preventDefault()
  const dialog = videoDialogs.value.find(d => d.id === id)
  if (!dialog) return
  
  const startX = event.clientX - dialog.position.x
  const startY = event.clientY - dialog.position.y
  
  const handleMouseMove = (e) => {
    dialog.position.x = e.clientX - startX
    dialog.position.y = e.clientY - startY
    
    // 限制拖动范围在视窗内
    const maxX = window.innerWidth - 400  // 弹窗宽度大约400px
    const maxY = window.innerHeight - 300  // 弹窗高度大约300px
    
    dialog.position.x = Math.max(0, Math.min(dialog.position.x, maxX))
    dialog.position.y = Math.max(0, Math.min(dialog.position.y, maxY))
  }
  
  const handleMouseUp = () => {
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }
  
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

const exitAnyFullscreen = () => {
  try {
    const dialogs = document.querySelectorAll('.video-dialog, .layout-dialog')
    dialogs.forEach(d => d.classList.remove('fullscreen-active'))
    if (document.fullscreenElement && document.exitFullscreen) {
      document.exitFullscreen().catch(() => {})
    }
  } catch (e) {
    console.warn('退出全屏时出错:', e)
  }
}

const handleWebRTCSuccess = () => {
  exitAnyFullscreen()
  ElMessage.success('WebRTC直连成功')
}

const handleWebRTCError = () => {
  ElMessage.error('WebRTC直连失败')
}

const handleDialogRtspConnected = () => {
  ElMessage.success('RTSP连接成功')
}

const handleDialogRtspDisconnected = () => {
  ElMessage.warning('RTSP连接断开')
}

const handleDialogRtspError = (error) => {
  ElMessage.error(`RTSP连接错误: ${error}`)
}

const retryWebRTCConnection = async (camera) => {
  if (!camera?.deviceData?.streamUrl) {
    ElMessage.warning('缺少流地址，无法重试WebRTC')
    return
  }
  
  const dialog = videoDialogs.value.find(d => d.camera === camera)
  if (dialog) {
    if (isCameraRtcStream(camera.deviceData.streamUrl)) {
      await playDialogCameraRTC(dialog)
    } else {
      await playDialogWebRTC(dialog)
    }
  } else {
    ElMessage.info(`重试连接摄像头: ${camera.name}`)
  }
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

const openInVlc = (url) => {
  if (url) {
    window.open(`vlc://${url}`)
    ElMessage.info('尝试在VLC中打开')
  }
}

const addDeviceToWindow = (dialogId, windowIndex) => {
  console.log('添加设备到窗口:', dialogId, windowIndex)
  ElMessage.info('添加设备功能')
}

// 生命周期
onMounted(async () => {
  try {
    isComponentMounted = true
    loading.value = true
    
    // 等待DOM完全渲染
    await nextTick()
    
    // 检查组件是否仍然挂载
    if (!isComponentMounted) {
      console.log('组件在初始化过程中被卸载')
      return
    }
    
    // 先加载设备列表
    await loadDeviceList()
    
    // 然后初始化地图
    initMap()
    
    // 添加全屏状态监听
    document.addEventListener('fullscreenchange', handleFullscreenChange)
    document.addEventListener('webkitfullscreenchange', handleFullscreenChange)
    document.addEventListener('mozfullscreenchange', handleFullscreenChange)
    document.addEventListener('MSFullscreenChange', handleFullscreenChange)
    
    // 确保地图默认最大化显示全部摄像头
    nextTick(() => {
      setTimeout(() => {
        if (mapInstance && mapInitialized && filteredCameraMarkers.value.length > 0) {
          // console.log('执行默认最大化显示全部摄像头:', filteredCameraMarkers.value.length)
          fitBounds()
        }
      }, 600) // 给足够的时间让地图和标记完全加载
    })

    
    // ElMessage.success('VideoSquare加载完成')
  } catch (error) {
    console.error('初始化失败:', error)
    if (isComponentMounted) {
    ElMessage.error('初始化失败')
    }
  } finally {
    if (isComponentMounted) {
    loading.value = false
    }
  }
})

onUnmounted(() => {
  console.log('VideoSquareRefactored组件开始卸载')
  isComponentMounted = false
  mapInitialized = false
  
  // 清理全屏状态监听
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.removeEventListener('mozfullscreenchange', handleFullscreenChange)
  document.removeEventListener('MSFullscreenChange', handleFullscreenChange)
  document.removeEventListener('keydown', handleEscKey)
  
  // 清理地图
  if (mapInstance) {
    try {
      mapInstance.remove()
      console.log('地图清理成功')
    } catch (error) {
      console.warn('地图清理时出错:', error)
    } finally {
      mapInstance = null
    }
  }
  
  // 清理所有WebRTC直连
  Array.from(webrtcPlayers.value.keys()).forEach(id => {
    cleanupDialogWebRTC(id)
  })

  Array.from(cameraRtcPlayers.value.keys()).forEach(id => {
    cleanupDialogCameraRTC(id)
  })
  
  console.log('VideoSquareRefactored组件卸载完成')
})
</script>

<style>
/* 全局隐藏滚动条 */
html, body {
  overflow-x: hidden !important;
  overflow-y: auto !important;
  scrollbar-width: none !important; /* Firefox */
  -ms-overflow-style: none !important; /* Internet Explorer 10+ */
}

html::-webkit-scrollbar, 
body::-webkit-scrollbar {
  display: none !important; /* Chrome, Safari, Opera */
}

/* 隐藏容器的滚动条，但保留地图标识 */
*:not(.leaflet-control-attribution):not(.leaflet-control-attribution *) {
  scrollbar-width: none !important;
  -ms-overflow-style: none !important;
}

*:not(.leaflet-control-attribution):not(.leaflet-control-attribution *)::-webkit-scrollbar {
  display: none !important;
}

/* 强制显示地图标识文字 */
.leaflet-control-attribution {
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
  position: fixed !important;
  bottom: 10px !important;
  right: 10px !important;
  z-index: 9999 !important;
  background: rgba(255, 255, 255, 0.9) !important;
  padding: 3px 6px !important;
  border-radius: 3px !important;
  font-size: 11px !important;
  color: #333 !important;
  box-shadow: 0 1px 3px rgba(0,0,0,0.3) !important;
}

.leaflet-control-attribution a {
  display: inline !important;
  visibility: visible !important;
  color: #0078A8 !important;
  text-decoration: none !important;
}

.leaflet-control-attribution a:hover {
  text-decoration: underline !important;
}
</style>

<style scoped>

/* 主容器样式 */
.video-square {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  overflow: hidden;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: visible;
}

.main-layout {
  flex: 1;
  display: flex;
  overflow: visible;
}

.map-area {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: visible;
}

/* 调试面板样式 */
.debug-panel {
  position: absolute;
  top: 20px;
  left: 20px;
  width: 300px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  border: 1px solid #e0e0e0;
}

.debug-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  border-radius: 8px 8px 0 0;
}

.debug-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.debug-content {
  padding: 16px;
  max-height: 300px;
  overflow-y: auto;
}

.debug-item {
  margin-bottom: 12px;
  font-size: 13px;
  line-height: 1.4;
}

.debug-item:last-child {
  margin-bottom: 0;
}

.status-success {
  color: #52c41a;
}

.status-loading {
  color: #1890ff;
}

.status-error {
  color: #ff4d4f;
}

/* 视频布局控制栏 */
.video-layout-controls {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
  z-index: 500;
  background: white;
  padding: 16px 24px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  border: 1px solid #e0e0e0;
  min-width: 600px;
}

.layout-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
}

.layout-btn {
  width: 40px;
  height: 40px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.layout-btn:hover {
  border-color: #1A53FF;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(26, 83, 255, 0.2);
}

.layout-btn.active {
  border-color: #1A53FF;
  background: #1A53FF;
  color: white;
}

.layout-btn img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.extended-layout-btn {
  width: 40px;
  height: 40px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.extended-layout-btn:hover {
  border-color: #1A53FF;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(26, 83, 255, 0.2);
}

.dots {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: 16px;
  height: 16px;
}

.dot {
  width: 3px;
  height: 3px;
  background-color: currentColor;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.extended-layout-btn:hover .dot {
  background-color: #000000;
}

.extended-layout-btn.active .dot {
  background-color: #ffffff;
}









.device-list-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.device-list-actions {
  display: flex;
  gap: 8px;
}

.device-action-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(0, 0, 0, 0.7);
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  transition: all 0.2s ease;
  padding: 2px;
}

.device-action-btn:hover {
  background: rgba(0, 0, 0, 0.9);
  color: #ffffff;
}

.device-action-btn.active {
  background: rgba(24, 144, 255, 0.8);
  color: #ffffff;
}

.device-action-btn img {
  opacity: 0.7;
  transition: opacity 0.2s ease;
}

.device-action-btn:hover img,
.device-action-btn.active img {
  opacity: 1;
}

/* 深色模式下的操作按钮 */
.map-area.dark-mode .device-action-btn {
  background: rgba(26, 35, 50, 0.9);
  color: #ffffff;
}

.map-area.dark-mode .device-action-btn:hover {
  background: rgba(26, 35, 50, 1);
  color: #ffffff;
}

  .map-area.dark-mode .device-action-btn.active {
    background: rgba(65, 105, 225, 0.8);
    color: #ffffff;
  }

  /* 选中的显示项目区域样式 */
  .selected-display-items {
    padding: 12px;
    border-bottom: 1px solid #e4e7ed;
    background: rgba(248, 249, 250, 0.8);
    margin: 0;
  }

  /* 深色模式下的选中显示项目 */
  .map-area.dark-mode .selected-display-items {
    background: rgba(15, 20, 25, 0.8);
    border-bottom: 1px solid rgba(100, 149, 237, 0.3);
  }

  .display-items-list {
    display: flex !important;
    flex-direction: row !important;
    flex-wrap: nowrap !important;
    gap: 16px;
    width: 100%;
    align-items: center;
    overflow-x: auto;
  }

  .display-item-tag {
    display: flex;
    align-items: center;
    gap: 8px;
    background: rgba(255, 255, 255, 0.9);
    border: 1px solid #e4e7ed;
    border-radius: 6px;
    padding: 6px 12px;
  font-size: 12px;
    color: #606266;
    white-space: nowrap;
    flex-shrink: 0;
    transition: all 0.2s ease;
  }

  .display-item-tag:hover {
    border-color: #1A53FF;
    background: rgba(26, 83, 255, 0.05);
  }

  /* 深色模式下的标签 */
  .map-area.dark-mode .display-item-tag {
    background: rgba(26, 35, 50, 0.9);
    border-color: rgba(100, 149, 237, 0.3);
    color: #a8c8ec;
  }

  .map-area.dark-mode .display-item-tag:hover {
    border-color: #87ceeb;
    background: rgba(135, 206, 235, 0.1);
  }

  .tag-content {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .tag-label {
  font-weight: 500;
  }

  .tag-stats-btn {
    background: none;
    border: none;
    cursor: pointer;
    padding: 2px;
    border-radius: 3px;
    color: #909399;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .tag-stats-btn:hover {
    background: rgba(144, 147, 153, 0.1);
    color: #606266;
  }

  .tag-stats-btn svg.rotated {
    transform: rotate(180deg);
  }

  /* 设备统计弹窗内容 */
  .device-stats-content {
    padding: 0;
  }

  .device-stats-expanded {
    background: white;
    border-radius: 6px;
    overflow: hidden;
  }

  .stats-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: #f8f9fa;
    border-bottom: 1px solid #e4e7ed;
  }

  .device-icon {
    color: #1A53FF;
  }

  .device-type-title {
    font-weight: 600;
    color: #303133;
    font-size: 14px;
  }

  .total-count {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;
  }

  .count-label {
    font-size: 13px;
    color: #606266;
  }

  .count-number {
    font-size: 16px;
    font-weight: 600;
    color: #1A53FF;
  }

  .location-stats {
    padding: 12px 16px;
  }

  .location-group {
    margin-bottom: 12px;
  }

  .location-group:last-child {
    margin-bottom: 0;
  }

  .location-title {
    font-size: 12px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 8px;
  }

  .status-options {
    display: flex;
    gap: 16px;
  }

  .status-item {
    display: flex;
    align-items: center;
  }

  .status-checkbox {
    font-size: 12px;
  }

.device-list-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 搜索框 */
.device-search-container {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.device-search-wrapper {
  position: relative;
  display: flex;
}

.device-search-input {
  flex: 1;
  height: 36px;
  padding: 0 40px 0 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.device-search-input:focus {
  border-color: #1A53FF;
}

.device-search-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
  transition: color 0.2s;
}

.device-search-btn:hover {
  color: #1A53FF;
}

/* 设备列表 */
.device-list-items {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.device-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.device-item:hover {
  background: #f8f9fa;
}

.device-icon {
  width: 32px;
  height: 32px;
  background: #e8f4ff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1A53FF;
}

.device-info {
  flex: 1;
  min-width: 0;
}

.device-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.device-details {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.device-type-tag {
  padding: 2px 6px;
  background: #e8f4ff;
  color: #1A53FF;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.device-status {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.device-status.status-online {
  background: #e8f5e8;
  color: #52c41a;
}

.device-status.status-offline {
  background: #fff2f0;
  color: #ff4d4f;
}

.device-ip {
  font-size: 11px;
  color: #999;
}

/* 树形视图 */
.tree-view {
  padding: 8px;
}

.tree-group {
  margin-bottom: 8px;
}

.tree-group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.tree-group-header:hover {
  background: #e9ecef;
}

.tree-expand-icon {
  transition: transform 0.2s;
}

.tree-expand-icon.expanded {
  transform: rotate(90deg);
}

.tree-group-icon {
  color: #1A53FF;
}

.tree-group-label {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.tree-group-count {
  font-size: 12px;
  color: #666;
}

.tree-device {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-left: 16px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.tree-device:hover {
  background: #f8f9fa;
}

.tree-device-indent {
  width: 16px;
}

.tree-device-icon {
  color: #1A53FF;
}

.tree-device-info {
  flex: 1;
  min-width: 0;
}

.tree-device-name {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 2px;
}

.tree-device-details {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* 地图容器 */
.map-container {
  flex: 1;
  position: relative;
  overflow: visible;
}

/* 确保地图标识文字可见 */
.map-container .leaflet-control-attribution {
  position: absolute !important;
  bottom: 0 !important;
  right: 0 !important;
  z-index: 1000 !important;
  background: rgba(255, 255, 255, 0.8) !important;
  padding: 2px 5px !important;
  font-size: 11px !important;
  line-height: 1.4 !important;
  color: #333 !important;
  border-radius: 3px 0 0 0 !important;
}

.leaflet-map-container {
  width: 100%;
  height: 100%;
}

/* 地图控制工具栏 */
.map-controls {
  position: absolute;
  top: 200px;
  left: 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  z-index: 500;
}

/* 缩放控制按钮 */
.zoom-controls {
  position: absolute;
  top: 120px;
  left: 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  z-index: 500;
}

.zoom-btn {
  width: 32px;
  height: 32px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.zoom-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
  box-shadow: 0 2px 8px rgba(26, 83, 255, 0.2);
}

.map-control-btn {
  width: 40px;
  height: 40px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.map-control-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
  transform: translateY(-1px);
}

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
}

.video-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
  cursor: move;
}

.video-dialog-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.dialog-controls {
  display: flex;
  gap: 8px;
}

.dialog-control-btn {
  width: 24px;
  height: 24px;
  background: none;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  transition: all 0.2s;
}

.dialog-control-btn:hover {
  background: #e9ecef;
  color: #333;
}

.close-btn:hover {
  background: #ff4d4f;
  color: white;
}

.video-dialog-content {
  width: 860px;
  height: 540px;
}

.video-player {
  width: 100%;
  height: 100%;
}

.video-placeholder-player {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.webrtc-iframe-container,
.rtsp-player-container,
.camera-rtc-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.webrtc-video {
  width: 100%;
  height: 100%;
  background: #000;
  object-fit: contain;
  border-radius: 6px;
}

.camera-rtc-player {
  width: 100%;
  height: 100%;
}

.webrtc-iframe-container iframe {
  width: 100%;
  height: 100%;
  border: none;
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

.video-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1a1a1a;
  color: white;
}

.placeholder-content {
  text-align: center;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.placeholder-text {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
}

.placeholder-details {
  margin-bottom: 24px;
  color: #ccc;
}

.placeholder-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
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
  background: #6c757d;
  color: white;
}

.action-btn.secondary:hover {
  background: #5a6268;
}

/* 设置弹窗样式 */
.settings-content {
  padding: 16px;
}

.setting-group {
  margin-bottom: 24px;
}

.setting-group h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.setting-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setting-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

/* 深色模式样式 */
.map-area.dark-mode .video-layout-controls {
  background: rgba(40, 44, 52, 0.95);
  border-bottom: 1px solid rgba(100, 149, 237, 0.3);
  backdrop-filter: blur(8px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.map-area.dark-mode .layout-btn {
  background: rgba(40, 44, 52, 0.95);
  border-color: rgba(100, 149, 237, 0.3);
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.map-area.dark-mode .layout-btn:hover {
  border-color: #1A53FF;
  background: rgba(26, 83, 255, 0.2);
  box-shadow: 0 2px 12px rgba(26, 83, 255, 0.4);
}

.map-area.dark-mode .layout-btn.active {
  border-color: #1A53FF;
  background: #1A53FF;
  box-shadow: 0 2px 12px rgba(26, 83, 255, 0.6);
}

.map-area.dark-mode .extended-layout-btn {
  background: rgba(40, 44, 52, 0.95) !important;
  border-color: rgba(100, 149, 237, 0.3) !important;
  color: #ffffff !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3) !important;
}

.map-area.dark-mode .extended-layout-btn:hover {
  border-color: #1A53FF !important;
  background: rgba(26, 83, 255, 0.2) !important;
  box-shadow: 0 2px 12px rgba(26, 83, 255, 0.4) !important;
}

.map-area.dark-mode .extended-layout-btn.active {
  background: #1A53FF !important;
  border-color: #1A53FF !important;
}



















.map-area.dark-mode .stats-label {
  color: #ffffff;
  font-weight: 500;
}

.map-area.dark-mode .stats-value.online {
  background: rgba(40, 44, 52, 0.8);
  border: 1px solid rgba(26, 83, 255, 0.4);
  color: #7db8ff;
  box-shadow: 0 2px 8px rgba(26, 83, 255, 0.2);
}

.map-area.dark-mode .stats-value.offline {
  background: rgba(40, 44, 52, 0.8);
  border: 1px solid rgba(245, 108, 108, 0.4);
  color: #f78989;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.2);
}

.map-area.dark-mode .stats-value.total {
  background: rgba(40, 44, 52, 0.8);
  border: 1px solid rgba(100, 149, 237, 0.3);
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.map-area.dark-mode .map-control-btn,
.map-area.dark-mode .zoom-btn {
  background: rgba(40, 44, 52, 0.95);
  border-color: rgba(100, 149, 237, 0.3);
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.map-area.dark-mode .map-control-btn:hover,
.map-area.dark-mode .zoom-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
  background: rgba(26, 83, 255, 0.2);
  box-shadow: 0 2px 12px rgba(26, 83, 255, 0.4);
}

/* 主题切换按钮特殊样式 */
.map-area.dark-mode .theme-toggle {
  background: rgba(40, 44, 52, 0.95);
  border-color: rgba(100, 149, 237, 0.3);
  color: #ffd700;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.map-area.dark-mode .theme-toggle:hover {
  border-color: #ffd700;
  color: #ffd700;
  background: rgba(255, 215, 0, 0.1);
  box-shadow: 0 2px 12px rgba(255, 215, 0, 0.3);
}

/* 地图控制面板黑夜模式 */
.map-area.dark-mode .map-controls {
  background: rgba(40, 44, 52, 0.1);
  border-radius: 8px;
  padding: 4px;
  backdrop-filter: blur(4px);
}

.map-area.dark-mode .zoom-controls {
  background: rgba(40, 44, 52, 0.1);
  border-radius: 8px;
  padding: 4px;
  backdrop-filter: blur(4px);
}

.map-area.dark-mode .device-stats {
  background: rgba(40, 44, 52, 0.95);
  border-color: rgba(100, 149, 237, 0.3);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(8px);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .video-layout-controls {
    position: static;
    transform: none;
    margin: 20px;
    flex-wrap: wrap;
    min-width: auto;
  }
  
  .device-list-panel {
    position: static;
    width: 100%;
    height: 300px;
    margin: 20px;
  }
  
  .layout-buttons {
    flex-wrap: wrap;
  }
  
  .device-stats {
    flex-wrap: wrap;
  }
}

/* 扩展布局弹窗样式 */
.extended-layout-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: transparent;
  pointer-events: none;
  z-index: 4000;
}

.extended-layout-dialog {
  position: fixed;
  background: white;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  pointer-events: auto;
  border: 1px solid #e0e0e0;
  min-width: 200px;
  z-index: 4001;
}

.extended-layout-content {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.extended-layout-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #333;
}

.extended-layout-option:hover {
  background: rgba(26, 83, 255, 0.1);
  transform: translateY(-1px);
}

.layout-option-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1A53FF;
}

.layout-option-text {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

/* 深色模式下的扩展布局弹窗 */
.map-area.dark-mode .extended-layout-dialog {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5) !important;
  backdrop-filter: blur(8px) !important;
}

.map-area.dark-mode .extended-layout-option {
  color: #ffffff !important;
}

.map-area.dark-mode .extended-layout-option:hover {
  background: rgba(100, 149, 237, 0.2) !important;
}

.map-area.dark-mode .layout-option-icon {
  color: #7db8ff !important;
}

.map-area.dark-mode .layout-option-text {
  color: #ffffff !important;
}

/* 全局深色模式样式覆盖 - 确保所有组件都能正确应用深色主题 */
.map-area.dark-mode .layout-button-group {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
  backdrop-filter: blur(8px) !important;
}

.map-area.dark-mode .device-stats {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  color: #e8f4fd !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
  backdrop-filter: blur(8px) !important;
}

.map-area.dark-mode .device-list-panel {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3) !important;
  backdrop-filter: blur(8px) !important;
}

/* 设备列表面板黑夜模式 */
.map-area.dark-mode .device-list-panel {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
  backdrop-filter: blur(8px) !important;
}

.map-area.dark-mode .device-list-panel .device-list-header {
  background: rgba(30, 34, 42, 0.95) !important;
  border-bottom: 1px solid rgba(100, 149, 237, 0.3) !important;
}

.map-area.dark-mode .device-list-panel .device-list-title {
  color: #ffffff !important;
}

.map-area.dark-mode .device-list-panel .device-action-btn {
  background: rgba(40, 44, 52, 0.8) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  color: #ffffff !important;
}

.map-area.dark-mode .device-list-panel .device-action-btn:hover {
  background: rgba(26, 83, 255, 0.2) !important;
  border-color: #1A53FF !important;
}

.map-area.dark-mode .device-list-panel .device-action-btn.active {
  background: #1A53FF !important;
  border-color: #1A53FF !important;
}

.map-area.dark-mode .device-list-panel .selected-display-items {
  background: rgba(30, 34, 42, 0.8) !important;
  border: 1px solid rgba(100, 149, 237, 0.2) !important;
}

.map-area.dark-mode .device-list-panel .display-item-tag {
  background: rgba(40, 44, 52, 0.8) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  color: #ffffff !important;
}

.map-area.dark-mode .device-list-panel .device-search-input {
  background: rgba(40, 44, 52, 0.8) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  color: #ffffff !important;
}

.map-area.dark-mode .device-list-panel .device-search-input::placeholder {
  color: #a0a0a0 !important;
}

.map-area.dark-mode .device-list-panel .device-item {
  background: rgba(40, 44, 52, 0.5) !important;
  border: 1px solid rgba(100, 149, 237, 0.2) !important;
  color: #ffffff !important;
}

.map-area.dark-mode .device-list-panel .device-item:hover {
  background: rgba(26, 83, 255, 0.1) !important;
  border-color: rgba(100, 149, 237, 0.4) !important;
}

/* 设备列表面板的标签和状态样式 */
.map-area.dark-mode .device-list-panel .device-type-tag {
  background: rgba(26, 83, 255, 0.2) !important;
  color: #7db8ff !important;
  border: 1px solid rgba(26, 83, 255, 0.4) !important;
}

.map-area.dark-mode .device-list-panel .device-status.status-online {
  color: #7db8ff !important;
}

.map-area.dark-mode .device-list-panel .device-status.status-offline {
  color: #f78989 !important;
}

.map-area.dark-mode .device-list-panel .total-count {
  background: rgba(30, 34, 42, 0.8) !important;
  border-bottom: 1px solid rgba(100, 149, 237, 0.3) !important;
}

.map-area.dark-mode .device-list-panel .count-label {
  color: #a0a0a0 !important;
}

.map-area.dark-mode .device-list-panel .count-number {
  color: #7db8ff !important;
}

.map-area.dark-mode .device-list-panel .location-title {
  color: #ffffff !important;
}

.map-area.dark-mode .device-list-panel .device-name {
  color: #ffffff !important;
}

.map-area.dark-mode .device-list-panel .device-ip {
  color: #a0a0a0 !important;
}

.map-area.dark-mode .device-list-panel .device-location {
  color: #a0a0a0 !important;
}

/* 设备列表面板的复选框样式 */
.map-area.dark-mode .device-list-panel .el-checkbox__input.is-checked .el-checkbox__inner {
  background-color: #1A53FF !important;
  border-color: #1A53FF !important;
}

.map-area.dark-mode .device-list-panel .el-checkbox__inner {
  background-color: rgba(40, 44, 52, 0.8) !important;
  border-color: rgba(100, 149, 237, 0.3) !important;
}

.map-area.dark-mode .device-list-panel .el-checkbox__label {
  color: #ffffff !important;
}
</style>

<!-- 全局深色模式样式 - 最高优先级确保生效 -->
<style>
.map-area.dark-mode .layout-button-group {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
  backdrop-filter: blur(8px) !important;
}

.map-area.dark-mode .device-stats {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  color: #e8f4fd !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
  backdrop-filter: blur(8px) !important;
}

.map-area.dark-mode .device-list-panel {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3) !important;
  backdrop-filter: blur(8px) !important;
}

.map-area.dark-mode .device-list-header {
  background: rgba(30, 34, 42, 0.95) !important;
  border-bottom: 1px solid rgba(100, 149, 237, 0.3) !important;
}

.map-area.dark-mode .device-list-title {
  color: #ffffff !important;
}

.map-area.dark-mode .device-list-content {
  background: rgba(40, 44, 52, 0.95) !important;
}

.map-area.dark-mode .device-list-items {
  background: rgba(40, 44, 52, 0.95) !important;
}

.map-area.dark-mode .selected-display-items {
  background: rgba(30, 34, 42, 0.8) !important;
  border-bottom: 1px solid rgba(100, 149, 237, 0.3) !important;
}

.map-area.dark-mode .display-item-tag {
  background: rgba(40, 44, 52, 0.8) !important;
  border: none !important;
  color: #ffffff !important;
}

.map-area.dark-mode .device-search-container {
  background: rgba(30, 34, 42, 0.8) !important;
  border-bottom: 1px solid rgba(100, 149, 237, 0.3) !important;
}

.map-area.dark-mode .device-search-input {
  background: rgba(40, 44, 52, 0.8) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  color: #ffffff !important;
}

.map-area.dark-mode .device-search-input::placeholder {
  color: #a0a0a0 !important;
}

.map-area.dark-mode .device-item {
  background: rgba(50, 54, 62, 0.6) !important;
  border-bottom: 1px solid rgba(100, 149, 237, 0.2) !important;
  color: #ffffff !important;
}

.map-area.dark-mode .device-item:hover {
  background: rgba(26, 83, 255, 0.1) !important;
}

.map-area.dark-mode .device-name {
  color: #ffffff !important;
}

/* 最高优先级强制覆盖列表结构样式 */
body.dark-mode .device-name,
.dark-mode .device-name,
.map-area.dark-mode .device-item .device-name,
.map-area.dark-mode .device-list-panel .device-item .device-name {
  color: #ffffff !important;
  font-weight: 500 !important;
}

body.dark-mode .device-details,
.dark-mode .device-details,
.map-area.dark-mode .device-item .device-details,
.map-area.dark-mode .device-list-panel .device-item .device-details {
  color: #a0a0a0 !important;
}

body.dark-mode .device-ip,
.dark-mode .device-ip,
.map-area.dark-mode .device-item .device-ip,
.map-area.dark-mode .device-list-panel .device-item .device-ip {
  color: #a0a0a0 !important;
}

.map-area.dark-mode .device-action-btn {
  background: rgba(40, 44, 52, 0.8) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  color: #ffffff !important;
}

.map-area.dark-mode .device-action-btn:hover {
  background: rgba(26, 83, 255, 0.2) !important;
  border-color: #1A53FF !important;
}

.map-area.dark-mode .device-action-btn.active {
  background: #1A53FF !important;
  border-color: #1A53FF !important;
}

.map-area.dark-mode .device-type-tag {
  background: rgba(26, 83, 255, 0.2) !important;
  color: #7db8ff !important;
  border: 1px solid rgba(26, 83, 255, 0.4) !important;
}

/* 搜索框上面的显示数据标签 - 取消边框 */
.selected-display-items .display-item-tag {
  background: transparent !important;
  border: none !important;
  padding: 0 !important;
}

.map-area.dark-mode .selected-display-items .display-item-tag {
  background: transparent !important;
  border: none !important;
  color: #ffffff !important;
  padding: 0 !important;
}

.map-area.dark-mode .device-status.online {
  color: #7db8ff !important;
}

.map-area.dark-mode .device-status.offline {
  color: #f78989 !important;
}

.map-area.dark-mode .device-ip {
  color: #a0a0a0 !important;
}

.map-area.dark-mode .device-location {
  color: #a0a0a0 !important;
}

.map-area.dark-mode .device-details {
  color: #a0a0a0 !important;
}

.map-area.dark-mode .tag-label {
  color: #ffffff !important;
}

.map-area.dark-mode .tag-stats-btn {
  color: #a0a0a0 !important;
}

.map-area.dark-mode .tag-stats-btn:hover {
  background: rgba(100, 149, 237, 0.1) !important;
  color: #7db8ff !important;
}

/* 深色模式下的弹窗样式 - 全局强制应用 */
.map-area.dark-mode :deep(.device-stats-popover) {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
  color: #e8f4fd !important;
}

.map-area.dark-mode :deep(.device-stats-popover .device-stats-content) {
  color: #e8f4fd !important;
}

.map-area.dark-mode :deep(.device-stats-popover .device-stats-expanded) {
  background: transparent !important;
  color: #e8f4fd !important;
}

.map-area.dark-mode :deep(.device-stats-popover .stats-header) {
  color: #e8f4fd !important;
}

.map-area.dark-mode :deep(.device-stats-popover .device-icon) {
  color: #7db8ff !important;
}

.map-area.dark-mode :deep(.device-stats-popover .device-type-title) {
  color: #e8f4fd !important;
}

.map-area.dark-mode :deep(.device-stats-popover .total-count) {
  background: rgba(30, 34, 42, 0.6) !important;
  border-bottom: 1px solid rgba(100, 149, 237, 0.3) !important;
}

.map-area.dark-mode :deep(.device-stats-popover .count-label) {
  color: #b0c4de !important;
}

.map-area.dark-mode :deep(.device-stats-popover .count-number) {
  color: #7db8ff !important;
}

.map-area.dark-mode :deep(.device-stats-popover .location-title) {
  color: #e8f4fd !important;
}

.map-area.dark-mode :deep(.device-stats-popover .status-checkbox) {
  color: #e8f4fd !important;
}

.map-area.dark-mode :deep(.device-stats-popover .el-checkbox__label) {
  color: #e8f4fd !important;
}

.map-area.dark-mode :deep(.device-stats-popover .el-checkbox__inner) {
  background-color: rgba(30, 34, 42, 0.8) !important;
  border-color: rgba(100, 149, 237, 0.5) !important;
}

.map-area.dark-mode :deep(.device-stats-popover .el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #7db8ff !important;
  border-color: #7db8ff !important;
}

.map-area.dark-mode :deep(.device-stats-popover .el-checkbox__input.is-checked .el-checkbox__inner::after) {
  border-color: rgba(30, 34, 42, 0.95) !important;
}

/* 全局深色模式弹窗样式 - 覆盖Element Plus */
body :deep(.el-popper.device-stats-popover) {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
  color: #e8f4fd !important;
}

/* 当页面处于深色模式时强制应用弹窗深色样式 */
.dark-mode ~ * .el-popper.device-stats-popover,
html.dark-mode .el-popper.device-stats-popover,
body.dark-mode .el-popper.device-stats-popover,
body.dark-mode .el-popper[data-popper-placement],
body.dark-mode .device-stats-popover {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
  color: #e8f4fd !important;
}

/* 深色模式下弹窗内容样式 */
body.dark-mode .device-stats-popover * {
  color: #e8f4fd !important;
}

body.dark-mode .device-stats-popover .device-type-title {
  color: #e8f4fd !important;
}

body.dark-mode .device-stats-popover .device-icon {
  color: #7db8ff !important;
}

body.dark-mode .device-stats-popover .total-count {
  background: rgba(30, 34, 42, 0.6) !important;
  border-bottom: 1px solid rgba(100, 149, 237, 0.3) !important;
}

body.dark-mode .device-stats-popover .count-label {
  color: #b0c4de !important;
}

body.dark-mode .device-stats-popover .count-number {
  color: #7db8ff !important;
}

body.dark-mode .device-stats-popover .location-title {
  color: #e8f4fd !important;
}

body.dark-mode .device-stats-popover .el-checkbox__label {
  color: #e8f4fd !important;
}

body.dark-mode .device-stats-popover .el-checkbox__inner {
  background-color: rgba(30, 34, 42, 0.8) !important;
  border-color: rgba(100, 149, 237, 0.5) !important;
}

body.dark-mode .device-stats-popover .el-checkbox__input.is-checked .el-checkbox__inner {
  background-color: #7db8ff !important;
  border-color: #7db8ff !important;
}

/* 强制覆盖所有白色背景 - 限制在弹窗内部 */
body.dark-mode .device-stats-popover div:not(.leaflet-container):not(.leaflet-map-pane):not([class*="leaflet"]) {
  background-color: rgba(40, 44, 52, 0.95) !important;
}

body.dark-mode .device-stats-popover .total-count {
  background-color: rgba(30, 34, 42, 0.6) !important;
}

body.dark-mode .device-stats-popover .el-checkbox__inner {
  background-color: rgba(30, 34, 42, 0.8) !important;
}

body.dark-mode .device-stats-popover .el-checkbox__input.is-checked .el-checkbox__inner {
  background-color: #7db8ff !important;
}

/* 最高优先级强制覆盖 Element Plus 弹窗样式 - 限制范围避免影响地图 */
body.dark-mode .el-popper.device-stats-popover,
body.dark-mode .el-popover.device-stats-popover {
  background: rgba(40, 44, 52, 0.95) !important;
  background-color: rgba(40, 44, 52, 0.95) !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
  color: #e8f4fd !important;
}

body.dark-mode .el-popper.device-stats-popover *,
body.dark-mode .el-popover.device-stats-popover * {
  background: rgba(40, 44, 52, 0.95) !important;
  background-color: rgba(40, 44, 52, 0.95) !important;
  color: #e8f4fd !important;
}

body.dark-mode .el-popper .el-popper__arrow::before {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
}

/* 弹窗内容区域边框 */
body.dark-mode .device-stats-popover .device-stats-content,
body.dark-mode .el-popper .device-stats-content,
body.dark-mode .el-popover .device-stats-content {
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  border-radius: 8px !important;
  padding: 12px !important;
}

/* 特殊元素的特定背景 */
body.dark-mode .el-popper.device-stats-popover .total-count,
body.dark-mode .el-popover.device-stats-popover .total-count {
  background: rgba(30, 34, 42, 0.6) !important;
  background-color: rgba(30, 34, 42, 0.6) !important;
  border: 1px solid rgba(100, 149, 237, 0.2) !important;
  border-radius: 6px !important;
}

body.dark-mode .el-popper.device-stats-popover .el-checkbox__inner,
body.dark-mode .el-popover.device-stats-popover .el-checkbox__inner {
  background: rgba(30, 34, 42, 0.8) !important;
  background-color: rgba(30, 34, 42, 0.8) !important;
  border-color: rgba(100, 149, 237, 0.5) !important;
}

body.dark-mode .el-popper.device-stats-popover .el-checkbox__input.is-checked .el-checkbox__inner,
body.dark-mode .el-popover.device-stats-popover .el-checkbox__input.is-checked .el-checkbox__inner {
  background: #7db8ff !important;
  background-color: #7db8ff !important;
  border-color: #7db8ff !important;
}

/* 浅色模式弹窗样式 - 移除外边框，限制范围避免影响地图 */
.el-popper.device-stats-popover,
.el-popover.device-stats-popover {
  border: none !important;
}

/* 浅色模式弹窗内容区域 */
.device-stats-popover .device-stats-content,
.el-popper.device-stats-popover .device-stats-content,
.el-popover.device-stats-popover .device-stats-content {
  border: 1px solid #e4e7ed !important;
  border-radius: 8px !important;
  padding: 12px !important;
}

.device-stats-popover .total-count,
.el-popper.device-stats-popover .total-count,
.el-popover.device-stats-popover .total-count {
  border: 1px solid #f0f0f0 !important;
  border-radius: 6px !important;
}

.map-area.dark-mode .display-items-title {
  color: #ffffff !important;
}

.map-area.dark-mode .count-label {
  color: #a0a0a0 !important;
}

.map-area.dark-mode .count-number {
  color: #7db8ff !important;
}

.map-area.dark-mode .location-title {
  color: #ffffff !important;
}

.map-area.dark-mode .tree-device-name {
  color: #ffffff !important;
}

.map-area.dark-mode .tree-device-details {
  color: #a0a0a0 !important;
}

.map-area.dark-mode .tree-group-label {
  color: #ffffff !important;
}

/* 提高优先级的树形结构深色模式样式 */
.map-area.dark-mode .device-list-panel .tree-device-name {
  color: #ffffff !important;
}

/* 全局最高优先级的扩展布局弹窗深色模式样式 */
.map-area.dark-mode .extended-layout-dialog {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5) !important;
  backdrop-filter: blur(8px) !important;
}

.map-area.dark-mode .extended-layout-option {
  color: #ffffff !important;
}

.map-area.dark-mode .extended-layout-option:hover {
  background: rgba(100, 149, 237, 0.2) !important;
}

.map-area.dark-mode .layout-option-icon {
  color: #7db8ff !important;
}

.map-area.dark-mode .layout-option-text {
  color: #ffffff !important;
}

.map-area.dark-mode .device-list-panel .tree-device-details {
  color: #a0a0a0 !important;
}

.map-area.dark-mode .device-list-panel .tree-group-label {
  color: #ffffff !important;
}

/* 最高优先级强制覆盖树形结构样式 */
body.dark-mode .tree-device-name,
.dark-mode .tree-device-name,
.map-area.dark-mode .tree-device-item .tree-device-name,
.map-area.dark-mode .device-list-panel .tree-device-item .tree-device-name {
  color: #ffffff !important;
  font-weight: 500 !important;
}

body.dark-mode .tree-device-details,
.dark-mode .tree-device-details,
.map-area.dark-mode .tree-device-item .tree-device-details,
.map-area.dark-mode .device-list-panel .tree-device-item .tree-device-details {
  color: #a0a0a0 !important;
}

body.dark-mode .tree-group-label,
.dark-mode .tree-group-label,
.map-area.dark-mode .tree-group .tree-group-label,
.map-area.dark-mode .device-list-panel .tree-group .tree-group-label {
  color: #ffffff !important;
  font-weight: 600 !important;
}

.map-area.dark-mode .tree-group-count {
  color: #a0a0a0 !important;
}

.map-area.dark-mode .stat-label {
  color: #a0a0a0 !important;
}

.map-area.dark-mode .stat-value {
  color: #ffffff !important;
}

/* 视频播放器弹窗全屏样式 */
.video-dialog.fullscreen-active,
.layout-dialog.fullscreen-active {
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

/* 全屏状态下的视频播放器内容 */
.video-dialog.fullscreen-active .video-dialog-content,
.layout-dialog.fullscreen-active .layout-dialog-content {
  width: 100% !important;
  height: 100% !important;
  padding: 0 !important;
  background: #000 !important;
}

/* 全屏状态下的视频播放器头部 */
.video-dialog.fullscreen-active .video-dialog-header,
.layout-dialog.fullscreen-active .layout-dialog-header {
  position: absolute !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  z-index: 10 !important;
  background: rgba(0, 0, 0, 0.8) !important;
  backdrop-filter: blur(10px) !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1) !important;
}

/* 全屏状态下的视频播放器主体 */
.video-dialog.fullscreen-active .video-dialog-body,
.layout-dialog.fullscreen-active .layout-dialog-body {
  width: 100% !important;
  height: 100% !important;
  padding: 0 !important;
  background: #000 !important;
  display: flex !important;
  flex-direction: column !important;
}

/* 全屏状态下的对话框内容 */
.video-dialog.fullscreen-active .video-dialog-content,
.layout-dialog.fullscreen-active .layout-dialog-content {
  width: 100% !important;
  height: 100% !important;
  padding: 0 !important;
  background: #000 !important;
  display: flex !important;
  flex-direction: column !important;
}

/* 全屏状态下的主要内容区域 */
.video-dialog.fullscreen-active .dialog-main-content,
.layout-dialog.fullscreen-active .dialog-main-content {
  width: 100% !important;
  height: 100% !important;
  display: flex !important;
  flex-direction: row !important;
  background: #000 !important;
}

/* 全屏状态下的视频播放区域 */
.video-dialog.fullscreen-active .video-player-section,
.layout-dialog.fullscreen-active .video-player-section {
  flex: 1 !important;
  width: 100% !important;
  height: 100% !important;
  display: flex !important;
  flex-direction: column !important;
  background: #000 !important;
}

/* 全屏状态下的视频区域 */
.video-dialog.fullscreen-active .video-area,
.layout-dialog.fullscreen-active .video-area {
  flex: 1 !important;
  width: 100% !important;
  height: 100% !important;
  display: flex !important;
  flex-direction: column !important;
  background: #000 !important;
}

/* 全屏状态下的视频网格 */
.video-dialog.fullscreen-active .video-grid,
.layout-dialog.fullscreen-active .video-grid {
  width: 100% !important;
  height: 100% !important;
  gap: 2px !important;
  padding: 0 !important;
  flex: 1 !important;
}

/* 全屏状态下的视频窗口 */
.video-dialog.fullscreen-active .video-window,
.layout-dialog.fullscreen-active .video-window {
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  background: #000 !important;
}

/* 全屏状态下的视频播放器 */
.video-dialog.fullscreen-active .video-player-container,
.layout-dialog.fullscreen-active .video-player-container {
  width: 100% !important;
  height: 100% !important;
}

/* 全屏状态下的视频内容 */
.video-dialog.fullscreen-active .video-content,
.layout-dialog.fullscreen-active .video-content {
  width: 100% !important;
  height: 100% !important;
}

/* 全屏状态下的iframe播放器 */
.video-dialog.fullscreen-active .stream-iframe,
.layout-dialog.fullscreen-active .stream-iframe {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
}

/* 全屏状态下的HLS视频播放器 */
.video-dialog.fullscreen-active .hls-video,
.layout-dialog.fullscreen-active .hls-video {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
}

/* 全屏状态下的HTTP和YouTube播放器 */
.video-dialog.fullscreen-active .http-stream-player,
.video-dialog.fullscreen-active .youtube-stream-player,
.video-dialog.fullscreen-active .hls-stream-player,
.layout-dialog.fullscreen-active .http-stream-player,
.layout-dialog.fullscreen-active .youtube-stream-player,
.layout-dialog.fullscreen-active .hls-stream-player {
  width: 100% !important;
  height: 100% !important;
}

/* 全屏状态下的RTSP流信息 */
.video-dialog.fullscreen-active .rtsp-stream-info,
.layout-dialog.fullscreen-active .rtsp-stream-info {
  width: 100% !important;
  height: 100% !important;
}

/* 全屏状态下的PTZ面板 - 隐藏 */
.video-dialog.fullscreen-active .ptz-panel,
.layout-dialog.fullscreen-active .ptz-panel,
.video-dialog.fullscreen-active .ptz-control-section,
.layout-dialog.fullscreen-active .ptz-control-section {
  display: none !important;
}

/* 全屏状态下的空白占位符 */
.video-dialog.fullscreen-active .video-placeholder,
.layout-dialog.fullscreen-active .video-placeholder {
  background: #1a1a1a !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  color: rgba(255, 255, 255, 0.6) !important;
}
</style> 
