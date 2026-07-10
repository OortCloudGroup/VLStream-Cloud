<template>
  <div class="ptz-panel">
    <div class="ptz-header">
      <h3>PTZ控制</h3>
      <div class="ptz-status">
        <span class="status-dot" :class="{ online: isConnected }"></span>
        {{ isConnected ? '已连接' : '未连接' }}
      </div>
    </div>
    
    <div class="ptz-content">
      <!-- 方向控制 -->
      <div class="control-section">
        <h4>方向控制</h4>
        <div class="direction-controls">
          <div class="direction-row">
            <button class="direction-btn" @click="$emit('ptz-control', 'up')" title="向上">
              <el-icon><ArrowUp /></el-icon>
            </button>
          </div>
          <div class="direction-row">
            <button class="direction-btn" @click="$emit('ptz-control', 'left')" title="向左">
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <div class="center-area">
              <button class="center-btn" @click="$emit('ptz-control', 'home')" title="回中">
                <el-icon><Aim /></el-icon>
              </button>
            </div>
            <button class="direction-btn" @click="$emit('ptz-control', 'right')" title="向右">
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
          <div class="direction-row">
            <button class="direction-btn" @click="$emit('ptz-control', 'down')" title="向下">
              <el-icon><ArrowDown /></el-icon>
            </button>
          </div>
        </div>
      </div>
      
      <!-- 缩放控制 -->
      <div class="control-section">
        <h4>缩放控制</h4>
        <div class="zoom-controls">
          <button class="zoom-btn" @click="$emit('zoom-control', 'in')" title="放大">
            <el-icon><ZoomIn /></el-icon>
            <span>放大</span>
          </button>
          <button class="zoom-btn" @click="$emit('zoom-control', 'out')" title="缩小">
            <el-icon><ZoomOut /></el-icon>
            <span>缩小</span>
          </button>
        </div>
      </div>
      
      <!-- 预设位置 -->
      <div class="control-section">
        <h4>预设位置</h4>
        <div class="preset-controls">
          <div class="preset-grid">
            <button 
              v-for="i in 9" 
              :key="i"
              class="preset-btn"
              @click="$emit('control-action', { type: 'preset', value: i })"
              :title="`预设位置 ${i}`"
            >
              {{ i }}
            </button>
          </div>
          <div class="preset-actions">
            <button class="preset-action-btn" @click="$emit('control-action', { type: 'set-preset', value: currentPreset })">
              <el-icon><Star /></el-icon>
              设置预设
            </button>
            <button class="preset-action-btn" @click="$emit('control-action', { type: 'clear-preset', value: currentPreset })">
              <el-icon><Delete /></el-icon>
              清除预设
            </button>
          </div>
        </div>
      </div>
      
      <!-- 高级控制 -->
      <div class="control-section">
        <h4>高级控制</h4>
        <div class="advanced-controls">
          <div class="control-group">
            <label>速度控制</label>
            <div class="speed-controls">
              <button class="speed-btn" @click="$emit('control-action', { type: 'speed', value: 'slow' })">慢速</button>
              <button class="speed-btn active" @click="$emit('control-action', { type: 'speed', value: 'normal' })">正常</button>
              <button class="speed-btn" @click="$emit('control-action', { type: 'speed', value: 'fast' })">快速</button>
            </div>
          </div>
          
          <div class="control-group">
            <label>自动巡航</label>
            <div class="cruise-controls">
              <button class="cruise-btn" @click="$emit('control-action', { type: 'cruise', value: 'start' })">
                <el-icon><VideoPlay /></el-icon>
                开始巡航
              </button>
              <button class="cruise-btn" @click="$emit('control-action', { type: 'cruise', value: 'stop' })">
                <el-icon><VideoPause /></el-icon>
                停止巡航
              </button>
            </div>
          </div>
          
          <div class="control-group">
            <label>扫描模式</label>
            <div class="scan-controls">
              <button class="scan-btn" @click="$emit('control-action', { type: 'scan', value: 'start' })">
                <el-icon><Refresh /></el-icon>
                开始扫描
              </button>
              <button class="scan-btn" @click="$emit('control-action', { type: 'scan', value: 'stop' })">
                <el-icon><Close /></el-icon>
                停止扫描
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 状态信息 -->
      <div class="status-section">
        <h4>状态信息</h4>
        <div class="status-info">
          <div class="status-item">
            <span class="status-label">当前预设:</span>
            <span class="status-value">{{ currentPreset || '无' }}</span>
          </div>
          <div class="status-item">
            <span class="status-label">连接状态:</span>
            <span class="status-value" :class="{ online: isConnected }">
              {{ isConnected ? '已连接' : '未连接' }}
            </span>
          </div>
          <div class="status-item">
            <span class="status-label">控制模式:</span>
            <span class="status-value">{{ controlMode }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { 
  ArrowUp, 
  ArrowDown, 
  ArrowLeft, 
  ArrowRight,
  ZoomIn,
  ZoomOut,
  Aim,
  Star,
  Delete,
  VideoPlay,
  VideoPause,
  Refresh,
  Close
} from '@element-plus/icons-vue'

// Props
const props = defineProps({
  showCameraManagement: {
    type: Boolean,
    default: true
  },
  isConnected: {
    type: Boolean,
    default: false
  },
  currentPreset: {
    type: Number,
    default: null
  },
  controlMode: {
    type: String,
    default: '手动'
  }
})

// Emits
const emit = defineEmits([
  'ptz-control',
  'zoom-control',
  'control-action'
])
</script>

<style scoped>
.ptz-panel {
  width: 100%;
  height: 100%;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ptz-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #e8e8e8;
  border-bottom: 1px solid #d9d9d9;
}

.ptz-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.ptz-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #606266;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
}

.status-dot.online {
  background: #67c23a;
}

.ptz-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.control-section {
  margin-bottom: 24px;
}

.control-section:last-child {
  margin-bottom: 0;
}

.control-section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 8px;
}

/* 方向控制 */
.direction-controls {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.direction-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.direction-btn {
  width: 48px;
  height: 48px;
  border: 2px solid #d9d9d9;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  color: #606266;
}

.direction-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
  transform: scale(1.05);
}

.direction-btn:active {
  background: #1A53FF;
  border-color: #1A53FF;
  color: white;
}

.center-area {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.center-btn {
  width: 32px;
  height: 32px;
  border: 2px solid #d9d9d9;
  background: white;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  color: #606266;
}

.center-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
  transform: scale(1.1);
}

.center-btn:active {
  background: #1A53FF;
  border-color: #1A53FF;
  color: white;
}

/* 缩放控制 */
.zoom-controls {
  display: flex;
  gap: 12px;
}

.zoom-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px;
  border: 2px solid #d9d9d9;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #606266;
}

.zoom-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
  transform: translateY(-2px);
}

.zoom-btn:active {
  background: #1A53FF;
  border-color: #1A53FF;
  color: white;
}

.zoom-btn span {
  font-size: 12px;
  font-weight: 500;
}

/* 预设位置 */
.preset-controls {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.preset-btn {
  width: 100%;
  height: 40px;
  border: 2px solid #d9d9d9;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #606266;
  transition: all 0.2s ease;
}

.preset-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
  transform: scale(1.05);
}

.preset-btn:active {
  background: #1A53FF;
  border-color: #1A53FF;
  color: white;
}

.preset-actions {
  display: flex;
  gap: 8px;
}

.preset-action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  color: #606266;
  transition: all 0.2s ease;
}

.preset-action-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
}

.preset-action-btn:active {
  background: #1A53FF;
  border-color: #1A53FF;
  color: white;
}

/* 高级控制 */
.advanced-controls {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.control-group label {
  font-size: 12px;
  font-weight: 500;
  color: #606266;
}

.speed-controls {
  display: flex;
  gap: 4px;
}

.speed-btn {
  flex: 1;
  padding: 6px 8px;
  border: 1px solid #d9d9d9;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 11px;
  color: #606266;
  transition: all 0.2s ease;
}

.speed-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
}

.speed-btn.active {
  background: #1A53FF;
  border-color: #1A53FF;
  color: white;
}

.cruise-controls,
.scan-controls {
  display: flex;
  gap: 8px;
}

.cruise-btn,
.scan-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 11px;
  color: #606266;
  transition: all 0.2s ease;
}

.cruise-btn:hover,
.scan-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
}

.cruise-btn:active,
.scan-btn:active {
  background: #1A53FF;
  border-color: #1A53FF;
  color: white;
}

/* 状态信息 */
.status-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.status-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: white;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
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

.status-value.online {
  color: #67c23a;
}

/* 深色模式样式 */
.map-area.dark-mode .ptz-panel {
  background: #1a1a1a;
}

.map-area.dark-mode .ptz-header {
  background: #2a2a2a;
  border-bottom-color: #333;
}

.map-area.dark-mode .ptz-header h3 {
  color: #ffffff;
}

.map-area.dark-mode .ptz-status {
  color: #a0a0a0;
}

.map-area.dark-mode .control-section h4 {
  color: #ffffff;
  border-bottom-color: #333;
}

.map-area.dark-mode .direction-btn,
.map-area.dark-mode .center-btn,
.map-area.dark-mode .zoom-btn,
.map-area.dark-mode .preset-btn,
.map-area.dark-mode .preset-action-btn,
.map-area.dark-mode .speed-btn,
.map-area.dark-mode .cruise-btn,
.map-area.dark-mode .scan-btn {
  background: #2a2a2a;
  border-color: #555;
  color: #ccc;
}

.map-area.dark-mode .direction-btn:hover,
.map-area.dark-mode .center-btn:hover,
.map-area.dark-mode .zoom-btn:hover,
.map-area.dark-mode .preset-btn:hover,
.map-area.dark-mode .preset-action-btn:hover,
.map-area.dark-mode .speed-btn:hover,
.map-area.dark-mode .cruise-btn:hover,
.map-area.dark-mode .scan-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
}

.map-area.dark-mode .speed-btn.active {
  background: #1A53FF;
  border-color: #1A53FF;
  color: white;
}

.map-area.dark-mode .control-group label {
  color: #a0a0a0;
}

.map-area.dark-mode .status-item {
  background: #2a2a2a;
  border-color: #333;
}

.map-area.dark-mode .status-label {
  color: #a0a0a0;
}

.map-area.dark-mode .status-value {
  color: #ffffff;
}

.map-area.dark-mode .status-section {
  border-top-color: #333;
}
</style> 