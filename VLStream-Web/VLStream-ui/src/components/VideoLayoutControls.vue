<template>
  <div class="map-top-controls">
    <!-- 布局控制按钮组 - 居中 -->
    <div class="layout-button-group">
      <el-button 
        :type="layoutMode === '1x1' ? 'primary' : 'default'" 
        size="small"
        @click="handleLayoutSelect(1)"
        title="单画面视频播放"
        class="layout-btn"
      >
        <img :src="mode1Icon" alt="1x1" class="layout-mode-icon" />
      </el-button>
      <el-button 
        :type="layoutMode === '2x2' ? 'primary' : 'default'" 
        size="small"
        @click="handleLayoutSelect(4)"
        title="四分屏视频播放"
        class="layout-btn four-grid-btn"
      >
        <div class="four-grid">
          <div class="grid-cell"></div>
          <div class="grid-cell"></div>
          <div class="grid-cell"></div>
          <div class="grid-cell"></div>
        </div>
      </el-button>
      <el-button 
        :type="layoutMode === '3x3' ? 'primary' : 'default'" 
        size="small"
        @click="handleLayoutSelect(6)"
        title="六分屏视频播放"
        class="layout-btn"
      >
        <img :src="mode3Icon" alt="六分屏" class="layout-mode-icon" />
      </el-button>
      <el-button 
        :type="layoutMode === '4x4' ? 'primary' : 'default'" 
        size="small"
        @click="handleLayoutSelect(8)"
        title="八分屏视频播放"
        class="layout-btn"
      >
        <img :src="mode4Icon" alt="八分屏" class="layout-mode-icon" />
      </el-button>
      <el-button 
        :type="layoutMode === '5x5' ? 'primary' : 'default'" 
        size="small"
        @click="handleLayoutSelect(9)"
        title="九分屏视频播放"
        class="layout-btn"
      >
        <img :src="mode5Icon" alt="九分屏" class="layout-mode-icon" />
      </el-button>
      <el-button 
        :type="layoutMode === '6x6' ? 'primary' : 'default'" 
        size="small"
        @click="handleLayoutSelect(16)"
        title="十六分屏视频播放"
        class="layout-btn"
      >
        <img :src="mode6Icon" alt="十六分屏" class="layout-mode-icon" />
      </el-button>
      <el-button 
        size="small"
        @click="$emit('extended-layout')"
        title="扩展布局选项"
        class="layout-btn extended-layout-btn"
        :class="{ active: showExtendedLayout }"
      >
        <div class="vertical-dots">
          <div class="dot"></div>
          <div class="dot"></div>
          <div class="dot"></div>
        </div>
      </el-button>
      <el-button 
        size="small"
        @click="$emit('custom-layout')"
        title="自定义视频播放"
        class="layout-btn"
      >
        自定义
      </el-button>
      <el-button 
        size="small"
        @click="$emit('toggle-fullscreen')"
        :title="isFullscreen ? '退出全屏' : '全屏'"
        :type="isFullscreen ? 'primary' : 'default'"
        class="layout-btn"
      >
        <el-icon><FullScreen /></el-icon>
      </el-button>
    </div>

    <!-- 设备统计信息 - 右侧 -->
    <div class="device-stats">
      <div class="stat-item total">
        <div class="stat-icon">
          <img src="@/assets/设备-总设备@3x.png" alt="总设备" width="32" height="32">
        </div>
        <div class="stat-content">
          <span class="stat-label">总设备：</span>
          <span class="stat-value">{{ deviceCount }}</span>
        </div>
      </div>
      
      <div class="stat-item online">
        <div class="stat-icon">
          <img src="@/assets/设备-在线@3x.png" alt="在线" width="32" height="32">
        </div>
        <div class="stat-content">
          <span class="stat-label">在线：</span>
          <span class="stat-value">{{ onlineCount }}</span>
        </div>
      </div>
      
      <div class="stat-item offline">
        <div class="stat-icon">
          <img src="@/assets/设备-离线@3x.png" alt="离线" width="32" height="32">
        </div>
        <div class="stat-content">
          <span class="stat-label">离线：</span>
          <span class="stat-value">{{ offlineCount }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { FullScreen } from '@element-plus/icons-vue'

// 导入布局模式PNG图标
import mode1Icon from '@/assets/mode1_default.png'
import mode3Icon from '@/assets/mode3_default.png'
import mode4Icon from '@/assets/mode4_default.png'
import mode5Icon from '@/assets/mode5_default.png'
import mode6Icon from '@/assets/mode6_default.png'

// 定义props
const props = defineProps({
  layoutMode: {
    type: String,
    default: '2x2'
  },
  showExtendedLayout: {
    type: Boolean,
    default: false
  },
  isFullscreen: {
    type: Boolean,
    default: false
  },
  deviceCount: {
    type: Number,
    default: 0
  },
  onlineCount: {
    type: Number,
    default: 0
  },
  offlineCount: {
    type: Number,
    default: 0
  }
})

// 定义emits
const emit = defineEmits([
  'layout-select',
  'extended-layout',
  'custom-layout',
  'toggle-fullscreen'
])

// 处理布局选择
const handleLayoutSelect = (count) => {
  console.log('VideoLayoutControls: 布局选择按钮被点击，count:', count)
  emit('layout-select', count)
}


</script>

<style scoped>
/* 地图顶部控制栏 - 布局按钮居中，设备统计右侧 */
.map-top-controls {
  position: absolute;
  top: 20px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  z-index: 1500;
}

.layout-button-group {
  display: flex;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  padding: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  /* 绝对居中定位 */
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

/* 深色模式下的布局按钮组 */
:deep(.map-area.dark-mode) .layout-button-group,
.map-area.dark-mode .layout-button-group {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
  backdrop-filter: blur(8px) !important;
}

.layout-btn {
  margin: 0 2px;
  border-radius: 6px;
  transition: all 0.3s ease;
  min-width: 40px;
  height: 36px;
}

.layout-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.layout-btn:first-child {
  margin-left: 0;
}

.layout-btn:last-child {
  margin-right: 0;
}

.layout-mode-icon {
  width: 20px;
  height: 20px;
}

.vertical-dots {
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: center;
}

.vertical-dots .dot {
  width: 4px;
  height: 4px;
  background-color: #666;
  border-radius: 50%;
}

/* 四宫格样式 */
.four-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 2px;
  width: 15px;
  height: 15px;
}

.grid-cell {
  background-color: #778da2;
  border-radius: 1px;
  transition: background-color 0.3s ease;
}

/* 选中状态时，四宫格变成蓝色，按钮背景变成浅蓝色 */
.four-grid-btn.is-primary {
  background-color: #e6f3ff !important;
  border-color: #1A53FF !important;
}

.four-grid-btn.is-primary .grid-cell {
  background-color: #1A53FF;
}

/* 深色模式下的四宫格 */
:deep(.map-area.dark-mode) .grid-cell {
  background-color: #a0a0a0;
}

:deep(.map-area.dark-mode) .four-grid-btn.is-primary {
  background-color: rgba(125, 184, 255, 0.2) !important;
  border-color: #7db8ff !important;
}

:deep(.map-area.dark-mode) .four-grid-btn.is-primary .grid-cell {
  background-color: #7db8ff;
}

/* 设备统计信息样式 */
.device-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 420px;
  height: 36px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  padding: 6px 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

/* 深色模式下的设备统计样式 */
:deep(.map-area.dark-mode) .device-stats,
.map-area.dark-mode .device-stats {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  color: #e8f4fd !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
  backdrop-filter: blur(8px) !important;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon img {
  width: 32px;
  height: 32px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
  white-space: nowrap;
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
}

:deep(.map-area.dark-mode) .stat-label {
  color: #a0a0a0 !important;
}

:deep(.map-area.dark-mode) .stat-value {
  color: #ffffff !important;
}

:deep(.map-area.dark-mode) .stat-value {
  color: #e8f4fd;
}

/* 扩展布局按钮样式 */
.extended-layout-btn {
  background: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  color: #000000 !important;
}

.extended-layout-btn:hover {
  background: rgba(255, 255, 255, 0.2) !important;
  border-color: rgba(255, 255, 255, 0.3) !important;
  color: #000000 !important;
}

.extended-layout-btn.active {
  background: #1A53FF !important;
  border-color: #1A53FF !important;
  color: #ffffff !important;
}

.extended-layout-btn:hover .dot {
  background-color: #000000;
}

.extended-layout-btn.active .dot {
  background-color: #ffffff;
}

/* 深色模式下的扩展布局按钮 */
:deep(.map-area.dark-mode) .extended-layout-btn {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}
</style> 