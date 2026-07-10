<template>
  <div class="video-square">
    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 调试信息面板 -->
      <DebugPanel
        v-if="showDebugInfo"
        :show-debug-info="showDebugInfo"
        :loading="loading"
        :device-list="deviceList"
        :real-camera-streams="realCameraStreams"
        :layout-mode="layoutMode"
        :webrtc-config="webrtcConfig"
        @close="showDebugInfo = false"
      />

      <!-- 地图模式 - 与PTZ面板水平布局 -->
      <div class="main-layout">
        <div class="map-area fullscreen-map">
          <!-- 地图顶部控制栏 - 布局按钮居中，设备统计右侧 -->
          <VideoLayoutControls
            :layout-mode="layoutMode"
            :show-extended-layout="showExtendedLayout"
            :is-fullscreen="isFullscreen"
            :device-list="deviceList"
            :online-device-count="onlineDeviceCount"
            :offline-device-count="offlineDeviceCount"
            :mode1-icon="mode1Icon"
            :mode2-icon="mode2Icon"
            :mode3-icon="mode3Icon"
            :mode4-icon="mode4Icon"
            :mode5-icon="mode5Icon"
            :mode6-icon="mode6Icon"
            @select-layout="selectLayout"
                         @show-extended-layout="showExtendedLayoutDialogHandler"
            @open-video-dialogs="openVideoDialogs"
            @open-custom-video-dialogs="openCustomVideoDialogs"
            @toggle-fullscreen="toggleFullscreen"
          />
          
          <!-- 设备列表面板 - 移到右侧 -->
          <DeviceListPanel
            :is-tree-view="isTreeView"
            :selected-display-items="selectedDisplayItems"
            :current-stats-item="currentStatsItem"
            :show-device-stats-dialog="showDeviceStatsDialog"
            :map-search-keyword="mapSearchKeyword"
            :real-camera-streams="realCameraStreams"
            :tree-data="treeData"
            :location-categories="locationCategories"
            :status-options="statusOptions"
            @settings="handleSettings"
            @toggle-tree-view="toggleTreeView"
            @toggle-list-view="toggleListView"
            @show-device-stats-modal="showDeviceStatsModal"
            @hide-device-stats-modal="hideDeviceStatsModal"
            @map-search="handleMapSearch"
            @camera-click="handleCameraClick"
            @tree-node-click="handleTreeNodeClick"
          />

          <!-- 地图主体 -->
          <div class="map-container">
            <!-- Leaflet地图容器 -->
            <div id="video-square-map" class="leaflet-map-container"></div>
            
            <!-- 地图控制工具栏 -->
            <MapControls
              :is-dark-mode="isDarkMode"
              @fit-bounds="fitBounds"
              @toggle-map-view="toggleMapView"
              @refresh-camera-locations="refreshCameraLocations"
              @toggle-dark-mode="toggleDarkMode"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 单个摄像头视频播放弹窗 -->
    <VideoDialog
      v-for="dialog in videoDialogs"
      :key="dialog.id"
      :dialog="dialog"
      @close="closeVideoDialog"
      @minimize="minimizeDialog"
      @bring-to-front="bringToFront"
      @start-drag="startDrag"
      @webrtc-load="handleWebRTCIframeLoad"
      @webrtc-error="handleWebRTCIframeError"
      @rtsp-connected="handleDialogRtspConnected"
      @rtsp-disconnected="handleDialogRtspDisconnected"
      @rtsp-error="handleDialogRtspError"
      @retry-connection="retryWebRTCConnection"
      @copy-stream-url="copyStreamUrl"
    />

    <!-- 其他弹窗和设置界面 -->
    <el-dialog
      v-model="showExtendedLayoutDialog"
      title="扩展布局选项"
      width="600px"
    >
      <div class="extended-layout-options">
        <h4>选择布局模式</h4>
        <div class="layout-grid">
          <button
            v-for="layout in extendedLayouts"
            :key="layout.mode"
            class="layout-option"
            :class="{ active: layoutMode === layout.mode }"
            @click="selectLayout(layout.mode)"
          >
            <div class="layout-preview" :style="layout.gridStyle">
              <div
                v-for="n in layout.count"
                :key="n"
                class="layout-cell"
              ></div>
            </div>
            <span class="layout-label">{{ layout.label }}</span>
          </button>
        </div>
      </div>
    </el-dialog>

    <!-- 设置面板 -->
    <el-dialog v-model="showSettings" title="设置" width="500px">
      <div class="settings-content">
        <h4>显示设置</h4>
        <div class="setting-group">
          <el-checkbox v-model="showOfflineDevices">
            显示离线设备
          </el-checkbox>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, nextTick, onMounted, onUnmounted, ref} from 'vue'
import {ElMessage} from 'element-plus'
import L from 'leaflet'

// API imports
import {getDeviceList} from '@/api/device'

// Component imports

// 导入拆分后的组件
import VideoLayoutControls from './components/VideoLayoutControls.vue'
import DebugPanel from './components/DebugPanel.vue'
import DeviceListPanel from './components/DeviceListPanel.vue'
import VideoDialog from './components/VideoDialog.vue'
import MapControls from './components/MapControls.vue'

// Asset imports
import mode1Icon from '@/assets/mode1_default.png'
import mode2Icon from '@/assets/mode1_default.png'
import mode3Icon from '@/assets/mode3_default.png'
import mode4Icon from '@/assets/mode4_default.png'
import mode5Icon from '@/assets/mode5_default.png'
import mode6Icon from '@/assets/mode6_default.png'

// 基础状态
const loading = ref(false)
const showDebugInfo = ref(false)
const isDarkMode = ref(false)
const isFullscreen = ref(false)
const layoutMode = ref(1)
const showExtendedLayout = ref(true)
const showExtendedLayoutDialog = ref(false)
const showSettings = ref(false)
const showOfflineDevices = ref(true)

// 设备相关状态
const deviceList = ref([])
const realCameraStreams = ref({})
const selectedDisplayItems = ref([])
const mapSearchKeyword = ref('')
const isTreeView = ref(false)
const treeData = ref([])
const locationCategories = ref([])
const statusOptions = ref([
  { label: '全部', value: '' },
  { label: '在线', value: 'online' },
  { label: '离线', value: 'offline' }
])

// 视频弹窗状态
const videoDialogs = ref([])
const currentStatsItem = ref(null)
const showDeviceStatsDialog = ref(false)
const webrtcConfig = ref({})

// 地图相关
let map = null
let mapInitialized = false
let isComponentMounted = false

// 计算属性
const onlineDeviceCount = computed(() => {
  return deviceList.value.filter(device => device.status === 'online').length
})

const offlineDeviceCount = computed(() => {
  return deviceList.value.filter(device => device.status === 'offline').length
})

const extendedLayouts = computed(() => [
  { mode: 1, count: 1, label: '1x1', gridStyle: 'grid-template-columns: 1fr; grid-template-rows: 1fr;' },
  { mode: 2, count: 4, label: '2x2', gridStyle: 'grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr;' },
  { mode: 3, count: 9, label: '3x3', gridStyle: 'grid-template-columns: repeat(3, 1fr); grid-template-rows: repeat(3, 1fr);' },
  { mode: 4, count: 16, label: '4x4', gridStyle: 'grid-template-columns: repeat(4, 1fr); grid-template-rows: repeat(4, 1fr);' },
  { mode: 5, count: 25, label: '5x5', gridStyle: 'grid-template-columns: repeat(5, 1fr); grid-template-rows: repeat(5, 1fr);' },
  { mode: 6, count: 36, label: '6x6', gridStyle: 'grid-template-columns: repeat(6, 1fr); grid-template-rows: repeat(6, 1fr);' }
])

// 方法
const initMap = () => {
  // 确保组件仍然挂载且地图容器存在
  if (!isComponentMounted || !document.getElementById('video-square-map')) {
    console.log('组件未挂载或地图容器不存在，跳过地图初始化')
    return
  }
  
  if (map) {
    try {
    map.remove()
    } catch (error) {
      console.warn('清理旧地图时出错:', error)
    }
    map = null
  }
  
  try {
  map = L.map('video-square-map').setView([39.9042, 116.4074], 13)
  
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
  }).addTo(map)
    
    mapInitialized = true
    console.log('地图初始化成功')
  } catch (error) {
    console.error('地图初始化失败:', error)
    mapInitialized = false
  }
}

const selectLayout = (mode) => {
  layoutMode.value = mode
  showExtendedLayoutDialog.value = false
  ElMessage.success(`已切换到${extendedLayouts.value.find(l => l.mode === mode)?.label}布局`)
}

const showExtendedLayoutDialogHandler = () => {
  showExtendedLayoutDialog.value = true
}

const openVideoDialogs = () => {
  // 实现打开视频弹窗逻辑
  ElMessage.info('打开视频弹窗功能')
}

const openCustomVideoDialogs = () => {
  // 实现自定义视频弹窗逻辑
  ElMessage.info('自定义视频布局功能')
}

const toggleFullscreen = () => {
  console.log('切换视频播放器全屏')
  
  // 检查是否有打开的视频播放器
  const hasOpenDialogs = videoDialogs.value.length > 0
  
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

const fitBounds = () => {
  if (map) {
    map.setView([39.9042, 116.4074], 13)
  }
  ElMessage.success('已重置地图视图')
}

const toggleMapView = () => {
  ElMessage.info('切换地图视图')
}

const refreshCameraLocations = () => {
  ElMessage.success('已刷新摄像头位置')
}

const toggleDarkMode = () => {
  isDarkMode.value = !isDarkMode.value
  ElMessage.info(isDarkMode.value ? '已切换到夜间模式' : '已切换到白天模式')
}

const handleSettings = () => {
  showSettings.value = true
}

const toggleTreeView = () => {
  isTreeView.value = true
}

const toggleListView = () => {
  isTreeView.value = false
}

const showDeviceStatsModal = () => {
  showDeviceStatsDialog.value = true
  currentStatsItem.value = {
    totalCount: deviceList.value.length,
    onlineCount: onlineDeviceCount.value,
    offlineCount: offlineDeviceCount.value
  }
}

const hideDeviceStatsModal = () => {
  showDeviceStatsDialog.value = false
}

const handleMapSearch = () => {
  // 实现地图搜索逻辑
}

const handleCameraClick = (camera) => {
  // 实现摄像头点击逻辑
  ElMessage.info(`点击了摄像头: ${camera.name}`)
}

const handleTreeNodeClick = (data) => {
  // 实现树节点点击逻辑
  ElMessage.info(`点击了节点: ${data.name}`)
}

// 视频弹窗相关方法
const closeVideoDialog = (id) => {
  const index = videoDialogs.value.findIndex(dialog => dialog.id === id)
  if (index > -1) {
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

const startDrag = ({ event, id }) => {
  // 实现拖拽逻辑
}

const handleWebRTCIframeLoad = () => {
  ElMessage.success('WebRTC流加载成功')
}

const handleWebRTCIframeError = () => {
  ElMessage.error('WebRTC流加载失败')
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

const retryWebRTCConnection = (camera) => {
  ElMessage.info(`重试连接摄像头: ${camera.name}`)
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
    
    // 初始化地图
    initMap()
    
    // 添加全屏状态监听
    document.addEventListener('fullscreenchange', handleFullscreenChange)
    document.addEventListener('webkitfullscreenchange', handleFullscreenChange)
    document.addEventListener('mozfullscreenchange', handleFullscreenChange)
    document.addEventListener('MSFullscreenChange', handleFullscreenChange)
    
    // 加载设备列表
    try {
    const response = await getDeviceList()
      if (response && response.data && isComponentMounted) {
      deviceList.value = response.data
      }
    } catch (apiError) {
      console.warn('API加载失败，使用模拟数据:', apiError)
    }
    
    // 模拟一些数据
    if (isComponentMounted) {
    deviceList.value = [
      { id: 1, name: '摄像头1', location: '大门入口', status: 'online', ip: '192.168.1.100', port: 554 },
      { id: 2, name: '摄像头2', location: '停车场', status: 'offline', ip: '192.168.1.101', port: 554 },
      { id: 3, name: '摄像头3', location: '大厅', status: 'online', ip: '192.168.1.102', port: 554 }
    ]
    
    ElMessage.success('VideoSquare加载完成')
    }
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
  console.log('VideoSquare组件开始卸载')
  isComponentMounted = false
  mapInitialized = false
  
  // 清理全屏状态监听
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.removeEventListener('mozfullscreenchange', handleFullscreenChange)
  document.removeEventListener('MSFullscreenChange', handleFullscreenChange)
  document.removeEventListener('keydown', handleEscKey)
  
  // 清理地图
  if (map) {
    try {
    map.remove()
      console.log('地图清理成功')
    } catch (error) {
      console.warn('地图清理时出错:', error)
    } finally {
    map = null
    }
  }
  
  console.log('VideoSquare组件卸载完成')
})
</script>

<style scoped>
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
  overflow: hidden;
}

.main-layout {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.map-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.map-container {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.leaflet-map-container {
  width: 100%;
  height: 100%;
}

/* 扩展布局对话框样式 */
.extended-layout-options h4 {
  margin: 0 0 20px 0;
  color: #333;
  font-weight: 600;
}

.layout-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 16px;
}

.layout-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
}

.layout-option:hover {
  border-color: #1A53FF;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(26, 83, 255, 0.2);
}

.layout-option.active {
  border-color: #1A53FF;
  background: #f0f8ff;
}

.layout-preview {
  display: grid;
  width: 60px;
  height: 60px;
  gap: 2px;
  border: 1px solid #ccc;
  border-radius: 4px;
  padding: 4px;
}

.layout-cell {
  background: #f0f0f0;
  border-radius: 2px;
}

.layout-option.active .layout-cell {
  background: #1A53FF;
}

.layout-label {
  font-size: 12px;
  font-weight: 500;
  color: #666;
}

.layout-option.active .layout-label {
  color: #1A53FF;
}

/* 设置面板样式 */
.settings-content h4 {
  margin: 0 0 16px 0;
  color: #333;
  font-weight: 600;
}

.setting-group {
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.setting-group:last-child {
  border-bottom: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .layout-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .layout-preview {
    width: 50px;
    height: 50px;
  }
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
}

/* 全屏状态下的视频网格 */
.video-dialog.fullscreen-active .video-grid,
.layout-dialog.fullscreen-active .video-grid {
  width: 100% !important;
  height: 100% !important;
  gap: 2px !important;
  padding: 0 !important;
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

/* 全屏状态下的PTZ面板 */
.video-dialog.fullscreen-active .ptz-panel,
.layout-dialog.fullscreen-active .ptz-panel {
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

/* 全屏状态下的空白占位符 */
.video-dialog.fullscreen-active .video-placeholder,
.layout-dialog.fullscreen-active .video-placeholder {
  background: #1a1a1a !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  color: rgba(255, 255, 255, 0.6) !important;
}
</style> 