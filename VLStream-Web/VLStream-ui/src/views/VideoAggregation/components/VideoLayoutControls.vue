<template>
  <div class="video-layout-controls">
    <!-- 布局模式选择 -->
    <div class="layout-mode-selector">
      <div class="layout-buttons">
        <button 
          class="layout-btn"
          :class="{ active: layoutMode === 1 }"
          @click="selectLayout(1)"
          title="单屏模式"
        >
          <img v-if="typeof mode1Icon === 'string'" :src="mode1Icon" alt="1x1" />
          <component v-else :is="mode1Icon" />
        </button>
        <button 
          class="layout-btn"
          :class="{ active: layoutMode === 2 }"
          @click="selectLayout(2)"
          title="2x2模式"
        >
          <img v-if="typeof mode2Icon === 'string'" :src="mode2Icon" alt="2x2" />
          <component v-else :is="mode2Icon" />
        </button>
        <button 
          class="layout-btn"
          :class="{ active: layoutMode === 3 }"
          @click="selectLayout(3)"
          title="3x3模式"
        >
          <img v-if="typeof mode3Icon === 'string'" :src="mode3Icon" alt="3x3" />
          <component v-else :is="mode3Icon" />
        </button>
        <button 
          class="layout-btn"
          :class="{ active: layoutMode === 4 }"
          @click="selectLayout(4)"
          title="4x4模式"
        >
          <img v-if="typeof mode4Icon === 'string'" :src="mode4Icon" alt="4x4" />
          <component v-else :is="mode4Icon" />
        </button>
        <button 
          class="layout-btn"
          :class="{ active: layoutMode === 5 }"
          @click="selectLayout(5)"
          title="5x5模式"
        >
          <img v-if="typeof mode5Icon === 'string'" :src="mode5Icon" alt="5x5" />
          <component v-else :is="mode5Icon" />
        </button>
        <button 
          class="layout-btn"
          :class="{ active: layoutMode === 6 }"
          @click="selectLayout(6)"
          title="6x6模式"
        >
          <img v-if="typeof mode6Icon === 'string'" :src="mode6Icon" alt="6x6" />
          <component v-else :is="mode6Icon" />
        </button>
      </div>
      
      <!-- 扩展布局按钮 -->
      <button 
        v-if="showExtendedLayout"
        class="extended-layout-btn"
        @click="showExtendedLayoutDialog"
        title="更多布局选项"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
        </svg>
      </button>
    </div>

    <!-- 设备统计信息 -->
    <div class="device-stats">
      <div class="stat-item">
        <span class="stat-label">在线设备:</span>
        <span class="stat-value online">{{ onlineDeviceCount }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">离线设备:</span>
        <span class="stat-value offline">{{ offlineDeviceCount }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">总设备:</span>
        <span class="stat-value total">{{ deviceList.length }}</span>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <button 
        class="action-btn"
        @click="openVideoDialogs"
        title="打开视频弹窗"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <path d="M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5l-1 1v2h8v-2l-1-1h5c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 12H3V5h18v10z"/>
        </svg>
        视频弹窗
      </button>
      
      <button 
        class="action-btn"
        @click="openCustomVideoDialogs"
        title="自定义视频布局"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/>
        </svg>
        自定义布局
      </button>
      
      <button 
        class="action-btn"
        @click="toggleFullscreen"
        :title="isFullscreen ? '退出全屏' : '全屏显示'"
      >
        <svg v-if="!isFullscreen" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/>
        </svg>
        <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <path d="M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z"/>
        </svg>
        {{ isFullscreen ? '退出全屏' : '全屏' }}
      </button>
    </div>
  </div>
</template>

<script setup>
// Props
const props = defineProps({
  layoutMode: {
    type: Number,
    default: 1
  },
  showExtendedLayout: {
    type: Boolean,
    default: true
  },
  isFullscreen: {
    type: Boolean,
    default: false
  },
  deviceList: {
    type: Array,
    default: () => []
  },
  onlineDeviceCount: {
    type: Number,
    default: 0
  },
  offlineDeviceCount: {
    type: Number,
    default: 0
  },
  mode1Icon: {
    type: [Object, String],
    default: () => ({})
  },
  mode2Icon: {
    type: [Object, String],
    default: () => ({})
  },
  mode3Icon: {
    type: [Object, String],
    default: () => ({})
  },
  mode4Icon: {
    type: [Object, String],
    default: () => ({})
  },
  mode5Icon: {
    type: [Object, String],
    default: () => ({})
  },
  mode6Icon: {
    type: [Object, String],
    default: () => ({})
  }
})

// Emits
const emit = defineEmits([
  'select-layout',
  'show-extended-layout',
  'open-video-dialogs',
  'open-custom-video-dialogs',
  'toggle-fullscreen'
])

// 方法
const selectLayout = (mode) => {
  emit('select-layout', mode)
}

const showExtendedLayoutDialog = () => {
  emit('show-extended-layout')
}

const openVideoDialogs = () => {
  emit('open-video-dialogs')
}

const openCustomVideoDialogs = () => {
  emit('open-custom-video-dialogs')
}

const toggleFullscreen = () => {
  emit('toggle-fullscreen')
}
</script>

<style scoped>
.video-layout-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #e0e0e0;
  position: relative;
  z-index: 100;
}

.layout-mode-selector {
  display: flex;
  align-items: center;
  gap: 12px;
}

.layout-buttons {
  display: flex;
  gap: 4px;
}

.layout-btn {
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
}

.layout-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
  transform: translateY(-1px);
}

.layout-btn.active {
  background: #1A53FF;
  border-color: #1A53FF;
  color: white;
}

.layout-btn img {
  width: 20px;
  height: 20px;
  object-fit: contain;
  filter: brightness(0) saturate(100%);
  opacity: 0.6;
}

.layout-btn:hover img {
  opacity: 0.8;
  filter: brightness(0) saturate(100%) invert(27%) sepia(51%) saturate(2878%) hue-rotate(224deg) brightness(104%) contrast(97%);
}

.layout-btn.active img {
  opacity: 1;
  filter: brightness(0) saturate(100%) invert(100%);
}

.extended-layout-btn {
  width: 40px;
  height: 40px;
  background: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  transition: all 0.2s;
}

.extended-layout-btn:hover {
  background: #e9ecef;
  border-color: #1A53FF;
  color: #1A53FF;
}

.device-stats {
  display: flex;
  gap: 20px;
  align-items: center;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.stat-label {
  color: #666;
}

.stat-value {
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.stat-value.online {
  background: #e8f5e8;
  color: #52c41a;
}

.stat-value.offline {
  background: #fff2f0;
  color: #ff4d4f;
}

.stat-value.total {
  background: #f0f8ff;
  color: #1A53FF;
}

.action-buttons {
  display: flex;
  gap: 12px;
  align-items: center;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}

.action-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
  transform: translateY(-1px);
}

.action-btn:active {
  transform: translateY(0);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .video-layout-controls {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .layout-mode-selector {
    justify-content: center;
  }
  
  .device-stats {
    justify-content: center;
  }
  
  .action-buttons {
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .video-layout-controls {
    padding: 12px 16px;
  }
  
  .layout-btn,
  .extended-layout-btn {
    width: 36px;
    height: 36px;
  }
  
  .device-stats {
    gap: 12px;
  }
  
  .action-btn {
    padding: 6px 12px;
    font-size: 13px;
  }
}
</style> 