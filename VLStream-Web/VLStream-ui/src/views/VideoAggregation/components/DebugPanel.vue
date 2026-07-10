<template>
  <div class="debug-panel">
    <!-- 面板头部 -->
    <div class="debug-header">
      <div class="header-title">
        <h3>调试信息面板</h3>
        <span class="debug-status" :class="{ loading: loading }">
          {{ loading ? '加载中...' : '运行中' }}
        </span>
      </div>
      <div class="header-controls">
        <button class="control-btn" @click="refreshDebugInfo" title="刷新">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
          </svg>
        </button>
        <button class="control-btn close-btn" @click="closePanel" title="关闭">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- 调试内容 -->
    <div class="debug-content">
      <!-- 系统状态 -->
      <div class="debug-section">
        <h4>系统状态</h4>
        <div class="status-grid">
          <div class="status-item">
            <label>加载状态:</label>
            <span :class="loading ? 'loading' : 'ready'">
              {{ loading ? '加载中' : '就绪' }}
            </span>
          </div>
          <div class="status-item">
            <label>布局模式:</label>
            <span>{{ layoutModeText }}</span>
          </div>
          <div class="status-item">
            <label>设备总数:</label>
            <span>{{ deviceList.length }}</span>
          </div>
          <div class="status-item">
            <label>活动流:</label>
            <span>{{ activeStreamCount }}</span>
          </div>
        </div>
      </div>

      <!-- 设备列表 -->
      <div class="debug-section">
        <h4>设备列表 ({{ deviceList.length }})</h4>
        <div class="device-debug-list">
          <div
            v-for="device in deviceList"
            :key="device.id"
            class="device-debug-item"
            :class="{ online: device.status === 'online', offline: device.status === 'offline' }"
          >
            <div class="device-basic-info">
              <div class="device-name">{{ device.name }}</div>
              <div class="device-id">ID: {{ device.id }}</div>
              <div class="device-location">{{ device.location }}</div>
            </div>
            <div class="device-technical-info">
              <div class="info-row">
                <label>状态:</label>
                <span class="status-badge" :class="device.status">
                  {{ device.status === 'online' ? '在线' : '离线' }}
                </span>
              </div>
              <div class="info-row">
                <label>IP:</label>
                <span>{{ device.ip || 'N/A' }}</span>
              </div>
              <div class="info-row">
                <label>端口:</label>
                <span>{{ device.port || 'N/A' }}</span>
              </div>
              <div class="info-row">
                <label>协议:</label>
                <span>{{ device.protocol || 'RTSP' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 实时流状态 -->
      <div class="debug-section">
        <h4>实时流状态 ({{ activeStreamCount }})</h4>
        <div class="stream-debug-list">
          <div
            v-for="(stream, cameraId) in realCameraStreams"
            :key="cameraId"
            class="stream-debug-item"
          >
            <div class="stream-info">
              <div class="stream-id">摄像头 ID: {{ cameraId }}</div>
              <div class="stream-url">流地址: {{ stream.url || 'N/A' }}</div>
              <div class="stream-status">
                状态: 
                <span class="status-badge active">活动</span>
              </div>
              <div class="stream-quality">
                质量: {{ stream.quality || '标清' }}
              </div>
            </div>
          </div>
          <div v-if="activeStreamCount === 0" class="no-streams">
            暂无活动流
          </div>
        </div>
      </div>

      <!-- WebRTC配置 -->
      <div class="debug-section">
        <h4>WebRTC 配置</h4>
        <div class="config-content">
          <pre class="config-json">{{ formattedWebRTCConfig }}</pre>
        </div>
      </div>

      <!-- 性能监控 -->
      <div class="debug-section">
        <h4>性能监控</h4>
        <div class="performance-grid">
          <div class="perf-item">
            <label>内存使用:</label>
            <span>{{ memoryUsage }}MB</span>
          </div>
          <div class="perf-item">
            <label>CPU使用:</label>
            <span>{{ cpuUsage }}%</span>
          </div>
          <div class="perf-item">
            <label>网络延迟:</label>
            <span>{{ networkLatency }}ms</span>
          </div>
          <div class="perf-item">
            <label>帧率:</label>
            <span>{{ frameRate }}fps</span>
          </div>
        </div>
      </div>

      <!-- 操作日志 -->
      <div class="debug-section">
        <h4>操作日志</h4>
        <div class="log-container">
          <div
            v-for="(log, index) in logs"
            :key="index"
            class="log-item"
            :class="log.type"
          >
            <span class="log-time">{{ log.time }}</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

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
    type: Object,
    default: () => ({})
  },
  layoutMode: {
    type: Number,
    default: 1
  },
  webrtcConfig: {
    type: Object,
    default: () => ({})
  }
})

// Emits
const emit = defineEmits(['close'])

// 本地状态
const memoryUsage = ref(0)
const cpuUsage = ref(0)
const networkLatency = ref(0)
const frameRate = ref(0)
const logs = ref([])

// 计算属性
const activeStreamCount = computed(() => {
  return Object.keys(props.realCameraStreams).length
})

const layoutModeText = computed(() => {
  const modes = {
    1: '单屏模式',
    2: '2x2模式',
    3: '3x3模式',
    4: '4x4模式',
    5: '5x5模式',
    6: '6x6模式'
  }
  return modes[props.layoutMode] || '未知模式'
})

const formattedWebRTCConfig = computed(() => {
  return JSON.stringify(props.webrtcConfig, null, 2)
})

// 方法
const closePanel = () => {
  emit('close')
}

const refreshDebugInfo = () => {
  // 刷新性能数据
  updatePerformanceMetrics()
  
  // 添加日志
  addLog('info', '调试信息已刷新')
}

const updatePerformanceMetrics = () => {
  // 模拟性能数据更新
  memoryUsage.value = Math.floor(Math.random() * 100) + 50
  cpuUsage.value = Math.floor(Math.random() * 50) + 10
  networkLatency.value = Math.floor(Math.random() * 100) + 20
  frameRate.value = Math.floor(Math.random() * 10) + 25
}

const addLog = (type, message) => {
  const log = {
    type,
    message,
    time: new Date().toLocaleTimeString()
  }
  logs.value.unshift(log)
  
  // 保持日志数量在合理范围内
  if (logs.value.length > 50) {
    logs.value = logs.value.slice(0, 50)
  }
}

// 定时器
let performanceTimer = null

// 生命周期
onMounted(() => {
  // 初始化性能监控
  updatePerformanceMetrics()
  
  // 启动定时更新
  performanceTimer = setInterval(() => {
    updatePerformanceMetrics()
  }, 5000)
  
  // 添加初始化日志
  addLog('info', '调试面板已启动')
})

onUnmounted(() => {
  if (performanceTimer) {
    clearInterval(performanceTimer)
  }
})
</script>

<style scoped>
.debug-panel {
  position: fixed;
  top: 20px;
  left: 20px;
  width: 400px;
  max-height: calc(100vh - 40px);
  background: white;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  z-index: 3000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.debug-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
}

.header-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.debug-status {
  font-size: 12px;
  color: #52c41a;
  font-weight: 500;
}

.debug-status.loading {
  color: #1A53FF;
}

.header-controls {
  display: flex;
  gap: 8px;
}

.control-btn {
  width: 32px;
  height: 32px;
  background: none;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  transition: all 0.2s;
}

.control-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
}

.close-btn:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.debug-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.debug-section {
  margin-bottom: 24px;
}

.debug-section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.status-item label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.status-item span {
  font-size: 14px;
  font-weight: 600;
}

.status-item span.loading {
  color: #1A53FF;
}

.status-item span.ready {
  color: #52c41a;
}

.device-debug-list {
  max-height: 300px;
  overflow-y: auto;
}

.device-debug-item {
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  margin-bottom: 8px;
  background: #f8f9fa;
}

.device-debug-item.online {
  border-left: 3px solid #52c41a;
}

.device-debug-item.offline {
  border-left: 3px solid #ff4d4f;
  opacity: 0.8;
}

.device-basic-info {
  margin-bottom: 8px;
}

.device-name {
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.device-id,
.device-location {
  font-size: 12px;
  color: #666;
}

.device-technical-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.info-row label {
  color: #666;
  font-weight: 500;
}

.status-badge {
  padding: 2px 6px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-badge.online,
.status-badge.active {
  background: #e8f5e8;
  color: #52c41a;
}

.status-badge.offline {
  background: #fff2f0;
  color: #ff4d4f;
}

.stream-debug-list {
  max-height: 200px;
  overflow-y: auto;
}

.stream-debug-item {
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  margin-bottom: 8px;
  background: #f0f8ff;
}

.stream-info div {
  font-size: 12px;
  margin-bottom: 4px;
}

.stream-id {
  font-weight: 600;
  color: #333;
}

.stream-url {
  color: #666;
  word-break: break-all;
}

.no-streams {
  text-align: center;
  color: #999;
  font-style: italic;
  padding: 20px;
}

.config-content {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.config-json {
  margin: 0;
  font-size: 11px;
  color: #333;
  white-space: pre-wrap;
  word-break: break-all;
}

.performance-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.perf-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 4px;
  font-size: 12px;
}

.perf-item label {
  color: #666;
  font-weight: 500;
}

.perf-item span {
  font-weight: 600;
  color: #333;
}

.log-container {
  max-height: 200px;
  overflow-y: auto;
  background: #f8f9fa;
  border-radius: 6px;
  padding: 8px;
}

.log-item {
  display: flex;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 4px;
  margin-bottom: 2px;
  font-size: 11px;
  font-family: monospace;
}

.log-item.info {
  background: #e6f7ff;
  color: #1890ff;
}

.log-item.warning {
  background: #fff7e6;
  color: #fa8c16;
}

.log-item.error {
  background: #fff2f0;
  color: #f5222d;
}

.log-time {
  color: #666;
  font-weight: 500;
  flex-shrink: 0;
}

.log-message {
  flex: 1;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .debug-panel {
    left: 10px;
    right: 10px;
    width: auto;
  }
  
  .status-grid,
  .performance-grid {
    grid-template-columns: 1fr;
  }
  
  .device-technical-info {
    grid-template-columns: 1fr;
  }
}
</style> 