<template>
  <div class="debug-panel" v-if="showDebugInfo">
    <div class="debug-header">
      <h3>调试信息</h3>
      <button class="close-btn" @click="$emit('close')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
        </svg>
      </button>
    </div>
    
    <div class="debug-content">
      <!-- 系统状态 -->
      <div class="debug-section">
        <h4>系统状态</h4>
        <div class="status-grid">
          <div class="status-item">
            <span class="status-label">加载状态:</span>
            <span class="status-value" :class="{ loading: loading }">
              {{ loading ? '加载中...' : '已就绪' }}
            </span>
          </div>
          <div class="status-item">
            <span class="status-label">设备总数:</span>
            <span class="status-value">{{ deviceList.length }}</span>
          </div>
          <div class="status-item">
            <span class="status-label">在线设备:</span>
            <span class="status-value online">{{ onlineDeviceCount }}</span>
          </div>
          <div class="status-item">
            <span class="status-label">离线设备:</span>
            <span class="status-value offline">{{ offlineDeviceCount }}</span>
          </div>
        </div>
      </div>
      
      <!-- WebRTC配置 -->
      <div class="debug-section">
        <h4>WebRTC配置</h4>
        <div class="config-info">
          <div class="config-item">
            <span class="config-label">服务器地址:</span>
            <span class="config-value">{{ webrtcConfig.serverUrl }}</span>
          </div>
          <div class="config-item">
            <span class="config-label">服务状态:</span>
            <span class="config-value" :class="{ available: webrtcConfig.available }">
              {{ webrtcConfig.available ? '可用' : '不可用' }}
            </span>
          </div>
          <div class="config-item">
            <span class="config-label">启用状态:</span>
            <span class="config-value" :class="{ enabled: webrtcConfig.enabled }">
              {{ webrtcConfig.enabled ? '已启用' : '已禁用' }}
            </span>
          </div>
        </div>
      </div>
      
      <!-- 设备列表 -->
      <div class="debug-section">
        <h4>设备列表 ({{ deviceList.length }})</h4>
        <div class="device-list">
          <div 
            v-for="device in deviceList.slice(0, 10)" 
            :key="device.id"
            class="device-item"
            :class="{ online: isDeviceOnline(device) }"
          >
            <div class="device-info">
              <span class="device-name">{{ device.deviceName || `设备${device.id}` }}</span>
              <span class="device-status">{{ device.status || '未知' }}</span>
            </div>
            <div class="device-streams">
              <span v-if="device.streamUrl" class="stream-badge rtsp">RTSP</span>
              <span v-if="device.webrtcUrl" class="stream-badge webrtc">WebRTC</span>
            </div>
          </div>
          <div v-if="deviceList.length > 10" class="more-devices">
            还有 {{ deviceList.length - 10 }} 个设备...
          </div>
        </div>
      </div>
      
      <!-- 流信息 -->
      <div class="debug-section">
        <h4>流信息 ({{ realCameraStreams.length }})</h4>
        <div class="stream-list">
          <div 
            v-for="stream in realCameraStreams.slice(0, 5)" 
            :key="stream.id"
            class="stream-item"
          >
            <div class="stream-info">
              <span class="stream-name">{{ stream.name || `流${stream.id}` }}</span>
              <span class="stream-type">{{ getStreamType(stream) }}</span>
            </div>
            <div class="stream-status">
              <span class="status-dot" :class="{ active: isStreamActive(stream) }"></span>
              {{ isStreamActive(stream) ? '活跃' : '非活跃' }}
            </div>
          </div>
          <div v-if="realCameraStreams.length > 5" class="more-streams">
            还有 {{ realCameraStreams.length - 5 }} 个流...
          </div>
        </div>
      </div>
      
      <!-- 布局信息 -->
      <div class="debug-section">
        <h4>布局信息</h4>
        <div class="layout-info">
          <div class="layout-item">
            <span class="layout-label">当前布局:</span>
            <span class="layout-value">{{ layoutMode }}</span>
          </div>
          <div class="layout-item">
            <span class="layout-label">布局模式:</span>
            <span class="layout-value">{{ getLayoutDescription(layoutMode) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { isCameraRtcStream } from '@/utils/oplayer'
import { WEBRTC_SERVER_BASE_URL } from '@/api/webrtc'

// Props
const props = defineProps({
  showDebugInfo: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  deviceList: {
    type: Array,
    default: () => []
  },
  realCameraStreams: {
    type: Array,
    default: () => []
  },
  layoutMode: {
    type: String,
    default: '2x2'
  },
  webrtcConfig: {
    type: Object,
    default: () => ({
      serverUrl: WEBRTC_SERVER_BASE_URL,
      available: false,
      enabled: true
    })
  }
})

// Emits
const emit = defineEmits(['close'])

// 计算属性
const onlineDeviceCount = computed(() => {
  return props.deviceList.filter(device => 
    device.status && device.status.trim() === '在线'
  ).length
})

const offlineDeviceCount = computed(() => {
  return props.deviceList.filter(device => 
    !device.status || device.status.trim() !== '在线'
  ).length
})

// 方法
const isDeviceOnline = (device) => {
  return device.status && (
    device.status.trim() === '在线' || 
    device.status.toLowerCase() === 'online'
  )
}

const getStreamType = (stream) => {
  if (stream.webrtcUrl) return 'WebRTC'
  if (isCameraRtcStream(stream.streamUrl)) return 'CameraRTC'
  if (stream.streamUrl) return 'RTSP'
  if (stream.rtspUrl) return 'RTSP'
  return '未知'
}

const isStreamActive = (stream) => {
  return stream.webrtcUrl || stream.streamUrl || stream.rtspUrl
}

const getLayoutDescription = (mode) => {
  const descriptions = {
    '1x1': '单屏布局',
    '2x2': '四分屏布局',
    '3x3': '九分屏布局',
    '4x4': '十六分屏布局',
    '5x5': '二十五分屏布局',
    '6x6': '三十六分屏布局'
  }
  return descriptions[mode] || '未知布局'
}
</script>

<style scoped>
.debug-panel {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 400px;
  max-height: 80vh;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.debug-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f5f5f5;
  border-bottom: 1px solid #e4e7ed;
}

.debug-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.close-btn {
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

.close-btn:hover {
  background: #f0f0f0;
  color: #303133;
}

.debug-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.debug-section {
  margin-bottom: 24px;
}

.debug-section:last-child {
  margin-bottom: 0;
}

.debug-section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 8px;
}

.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 12px;
}

.status-label {
  color: #606266;
  font-weight: 500;
}

.status-value {
  font-weight: 600;
  color: #303133;
}

.status-value.loading {
  color: #e6a23c;
}

.status-value.online {
  color: #67c23a;
}

.status-value.offline {
  color: #f56c6c;
}

.config-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.config-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 12px;
}

.config-label {
  color: #606266;
  font-weight: 500;
}

.config-value {
  font-weight: 600;
  color: #303133;
}

.config-value.available {
  color: #67c23a;
}

.config-value.enabled {
  color: #67c23a;
}

.device-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 200px;
  overflow-y: auto;
}

.device-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 12px;
  border-left: 3px solid #f56c6c;
}

.device-item.online {
  border-left-color: #67c23a;
}

.device-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.device-name {
  font-weight: 500;
  color: #303133;
}

.device-status {
  font-size: 10px;
  color: #909399;
}

.device-streams {
  display: flex;
  gap: 4px;
}

.stream-badge {
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  color: white;
}

.stream-badge.rtsp {
  background: #1A53FF;
}

.stream-badge.webrtc {
  background: #67c23a;
}

.more-devices {
  text-align: center;
  padding: 8px;
  color: #909399;
  font-size: 12px;
  font-style: italic;
}

.stream-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 150px;
  overflow-y: auto;
}

.stream-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 12px;
}

.stream-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stream-name {
  font-weight: 500;
  color: #303133;
}

.stream-type {
  font-size: 10px;
  color: #909399;
}

.stream-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  color: #909399;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f56c6c;
}

.status-dot.active {
  background: #67c23a;
}

.more-streams {
  text-align: center;
  padding: 8px;
  color: #909399;
  font-size: 12px;
  font-style: italic;
}

.layout-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.layout-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 12px;
}

.layout-label {
  color: #606266;
  font-weight: 500;
}

.layout-value {
  font-weight: 600;
  color: #303133;
}

/* 深色模式样式 */
.map-area.dark-mode .debug-panel {
  background: #1a1a1a;
  border: 1px solid #333;
}

.map-area.dark-mode .debug-header {
  background: #2a2a2a;
  border-bottom-color: #333;
}

.map-area.dark-mode .debug-header h3 {
  color: #ffffff;
}

.map-area.dark-mode .close-btn {
  color: #ccc;
}

.map-area.dark-mode .close-btn:hover {
  background: #333;
  color: #ffffff;
}

.map-area.dark-mode .debug-section h4 {
  color: #ffffff;
  border-bottom-color: #333;
}

.map-area.dark-mode .status-item,
.map-area.dark-mode .config-item,
.map-area.dark-mode .device-item,
.map-area.dark-mode .stream-item,
.map-area.dark-mode .layout-item {
  background: #2a2a2a;
}

.map-area.dark-mode .status-label,
.map-area.dark-mode .config-label,
.map-area.dark-mode .layout-label {
  color: #a0a0a0;
}

.map-area.dark-mode .status-value,
.map-area.dark-mode .config-value,
.map-area.dark-mode .layout-value {
  color: #ffffff;
}

.map-area.dark-mode .device-name,
.map-area.dark-mode .stream-name {
  color: #ffffff;
}

.map-area.dark-mode .device-status,
.map-area.dark-mode .stream-type,
.map-area.dark-mode .stream-status {
  color: #aaa;
}

.map-area.dark-mode .more-devices,
.map-area.dark-mode .more-streams {
  color: #666;
}
</style> 
