<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="map-top-controls">
    <!-- controlbutton - in -->
    <div class="layout-button-group" v-floating-drag>
      <button type="button" class="floating-drag-handle" data-drag-handle aria-label="拖动工具栏" title="拖动工具栏">⠿</button>
      <!-- MasterGo 309:32968 / 309:32903 — 视频徽标 + 分隔线 -->
      <button type="button" class="video-badge" aria-label="选择视频" @click="$emit('open-devices')">
        <img :src="videoBadgeIcon" width="24" height="24" alt="" />
        <span class="video-badge-text">{{ $tp('视频') }}</span>
      </button>
      <span class="toolbar-divider" aria-hidden="true"></span>
      <template v-if="showPresentation">
        <el-button class="layout-btn presentation-btn" :type="presentationMode === 'default' ? 'primary' : 'default'"
          :title="$tp('默认展示')" :aria-label="$tp('默认展示')" :aria-pressed="presentationMode === 'default'" @click="$emit('presentation-select', 'default')">
          <span class="design-toolbar-icon default-presentation-icon" :style="{ maskImage: `url(${defaultPresentationIcon})` }"></span>
        </el-button>
        <el-button class="layout-btn presentation-btn" :type="presentationMode === 'bottom' ? 'primary' : 'default'"
          :title="$tp('底部排列')" :aria-label="$tp('底部排列')" :aria-pressed="presentationMode === 'bottom'" @click="$emit('presentation-select', 'bottom')">
          <span class="design-toolbar-icon" :style="{ maskImage: `url(${bottomPresentationIcon})` }"></span>
        </el-button>
      </template>
      <el-button
        :type="presentationMode === 'grid' && layoutMode === '1x1' ? 'primary' : 'default'"
        size="small"
        @click="handleLayoutSelect(1)"
        :title="$tp('单画面视频播放')"
        class="layout-btn"
      >
        <img :src="mode1Icon" alt="1x1" class="layout-mode-icon" />
      </el-button>
      <el-button
        :type="presentationMode === 'grid' && layoutMode === '2x2' ? 'primary' : 'default'"
        size="small"
        @click="handleLayoutSelect(4)"
        :title="$tp('四分屏视频播放')"
        class="layout-btn four-grid-btn"
      >
        <img :src="fourLayoutIcon" alt="" class="layout-mode-icon" />
      </el-button>
      <el-button
        :type="presentationMode === 'grid' && layoutMode === '3x3' ? 'primary' : 'default'"
        size="small"
        @click="handleLayoutSelect(6)"
        :title="$tp('六分屏视频播放')"
        class="layout-btn"
      >
        <img :src="mode3Icon" :alt="$tp('六分屏')" class="layout-mode-icon" />
      </el-button>
      <el-button
        :type="presentationMode === 'grid' && layoutMode === '4x4' ? 'primary' : 'default'"
        size="small"
        @click="handleLayoutSelect(8)"
        :title="$tp('八分屏视频播放')"
        class="layout-btn"
      >
        <img :src="mode4Icon" :alt="$tp('八分屏')" class="layout-mode-icon" />
      </el-button>
      <el-button
        :type="presentationMode === 'grid' && layoutMode === '5x5' ? 'primary' : 'default'"
        size="small"
        @click="handleLayoutSelect(9)"
        :title="$tp('九分屏视频播放')"
        class="layout-btn"
      >
        <img :src="mode5Icon" :alt="$tp('九分屏')" class="layout-mode-icon" />
      </el-button>
      <el-button
        :type="presentationMode === 'grid' && layoutMode === '6x6' ? 'primary' : 'default'"
        size="small"
        @click="handleLayoutSelect(16)"
        :title="$tp('十六分屏视频播放')"
        class="layout-btn"
      >
        <img :src="mode6Icon" :alt="$tp('十六分屏')" class="layout-mode-icon" />
      </el-button>
      <el-button
        size="small"
        @click="$emit('extended-layout')"
        :title="$tp('扩展布局选项')"
        class="layout-btn extended-layout-btn"
        :class="{ active: showExtendedLayout }"
      >
        <div class="vertical-dots">
          <div class="dot"></div>
          <div class="dot"></div>
          <div class="dot"></div>
        </div>
      </el-button>
      <span class="toolbar-divider" aria-hidden="true"></span>
      <el-button class="layout-btn" :aria-label="$tp(panelCollapsed ? '展开设备栏' : '收起设备栏')" :title="$tp(panelCollapsed ? '展开设备栏' : '收起设备栏')" :aria-expanded="!panelCollapsed" aria-controls="square-device-catalog" @click="$emit('collapse-panel', 'right')"><img :src="panelRightIcon" alt="" class="layout-mode-icon" /></el-button>
      <el-button
        size="small"
        @click="$emit('toggle-fullscreen')"
        :title="isFullscreen ? $tp('退出全屏') : $tp('全屏')"
        :type="isFullscreen ? 'primary' : 'default'"
        class="layout-btn"
      >
        <img :src="toolbarFullscreenIcon" alt="" class="layout-mode-icon" />
      </el-button>
      <span class="toolbar-divider" aria-hidden="true"></span>
      <el-button
        size="small"
        @click="$emit('close-all-videos')"
        :title="$tp(openWindowCount ? '收起所有视频' : '展开视频')"
        :aria-label="$tp(openWindowCount ? '收起所有视频' : '展开视频')"
        :aria-expanded="openWindowCount > 0"
        class="layout-btn close-all-videos-btn custom-layout-btn"
      >
        {{ $tp(openWindowCount ? '收起视频' : '展开视频') }}
        <img :src="chevronDownIcon" :style="{ transform: openWindowCount ? 'none' : 'rotate(180deg)' }" width="16" height="16" alt="" />
      </el-button>
    </div>

    <!-- device info - -->
  </div>
</template>

<script setup>
import vFloatingDrag from '@/utils/floatingDrag'
import defaultPresentationIcon from '@/assets/mastergo/video-square/presentation-default.svg'
import bottomPresentationIcon from '@/assets/mastergo/video-square/presentation-bottom.svg'
import fourLayoutIcon from '@/assets/mastergo/video-square/layout-four.svg'
import toolbarFullscreenIcon from '@/assets/mastergo/video-square/toolbar-fullscreen.svg'
import chevronDownIcon from '@/assets/mastergo/video-square/chevron-down.svg'
import videoBadgeIcon from '@/assets/mastergo/video-square/video-badge.svg'
import panelRightIcon from '@/assets/mastergo/video-square/panel-right.svg'

// Import PNG
// 六画面按钮应有“大块 + 左下角小格”图案（见 VIDEO_LAYOUT_SPECS['6']），九画面按钮应为均匀 3×3；
// 现有两个资源文件的图案与此相反，这里按实际图案对调引用。
import mode1Icon from '@/assets/mastergo/video-square/layout-one.svg'
import mode3Icon from '@/assets/mastergo/video-square/layout-nine.svg'
import mode4Icon from '@/assets/mastergo/video-square/layout-eight.svg'
import mode5Icon from '@/assets/mastergo/video-square/layout-six.svg'
import mode6Icon from '@/assets/mastergo/video-square/layout-sixteen.svg'

// props
const props = defineProps({
  panelSide: { type: String, default: 'left' },
  panelCollapsed: Boolean,
  showPresentation: Boolean,
  presentationMode: { type: String, default: 'grid' },
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
  openWindowCount: {
    type: Number,
    default: 0
  }
})

// emits
const emit = defineEmits([
  'open-devices',
  'collapse-panel',
  'layout-select',
  'extended-layout',
  'custom-layout',
  'close-all-videos',
  'toggle-fullscreen',
  'presentation-select'
])

// Process
const handleLayoutSelect = (count) => {
  console.log('VideoLayoutControls: 布局选择按钮被点击，count:', count)
  emit('layout-select', count)
}


</script>

<style scoped>
.design-toolbar-icon { display: block; width: 24px; height: 24px; background: currentColor; mask-size: contain; mask-position: center; mask-repeat: no-repeat; }
.default-presentation-icon { width: 18px; height: 18px; }
.close-all-videos-btn :deep(span) { gap: 2px; }
/* MasterGo 309:32968 — 绿色「视频」徽标：高 24、r4、padding 0 8、gap 4、文字 12px 白 */
.video-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex: 0 0 auto;
  height: 24px;
  padding: 0 8px;
  border-radius: 4px;
  background: #36D94B;
  color: #FFFFFF;
}
.video-badge-icon {
  display: block;
  width: 18px;
  height: 18px;
  background: currentColor;
  mask: url('@/assets/mastergo/video-square/view-video.svg') center / contain no-repeat;
  -webkit-mask: url('@/assets/mastergo/video-square/view-video.svg') center / contain no-repeat;
}
.video-badge-text { font-size: 12px; line-height: 18px; }

/* MasterGo 309:32903 — 1×21.5 rgba(0,0,0,.2) 竖分隔线 */
.toolbar-divider { flex: 0 0 auto; width: 1px; height: 21.5px; background: rgba(0, 0, 0, 0.2); }

/* 视频/地图/列表 三种模式统一为一条工具栏：按钮组居中、设备统计靠右，不再换行成两行 */
.map-top-controls {
  position: absolute;
  top: 20px;
  left: 0;
  right: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  z-index: 1500;
  pointer-events: none;
}

/* 窄屏兜底：放不下时退回换行，避免统计条被压出内容区 */
@media (max-width: 900px) {
  .map-top-controls { display: flex; flex-wrap: wrap; justify-content: center; }
  .layout-button-group { grid-column: auto; margin-inline: auto; }
  .device-stats { grid-column: auto; margin-right: auto; }
}

/* MasterGo 309:30012 — bg #F2F7FF / r4 / padding 9 16 / gap 10 / 无边框阴影 */
.layout-button-group {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #F2F7FF;
  border-radius: 4px;
  padding: 9px 16px;
  border: 0;
  box-shadow: none;
  backdrop-filter: none;
  flex: 0 0 auto;
  grid-column: 2;
  justify-self: center;
  margin-inline: 0;
  pointer-events: auto;
}

/* button */
:deep(.map-area.dark-mode) .layout-button-group,
.map-area.dark-mode .layout-button-group {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
  backdrop-filter: blur(8px) !important;
}

/* MasterGo 303:30018 等 — 布局图标统一 24×24，色值 #718096 */
.layout-btn {
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 2px;
  background: transparent;
  color: #718096;
  box-shadow: none;
  transition: color 0.2s ease;
  min-width: 24px;
  width: 24px;
  height: 24px;
}

.layout-btn:hover,
.layout-btn:focus {
  background: transparent;
  border-color: transparent;
  color: #2278FF;
  box-shadow: none;
  transform: none;
}

.layout-btn:first-child,
.layout-btn:last-child {
  margin: 0;
}

.layout-mode-icon {
  width: 24px;
  height: 24px;
}

/* 文字按钮不套用图标按钮的 24px 固定宽度，避免文字被裁切 */
.layout-btn.custom-layout-btn {
  width: auto;
  min-width: 24px;
  padding: 0;
  font-size: 12px;
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

/*  */
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

/* in , , button */
.four-grid-btn.is-primary {
  background-color: #e6f3ff !important;
  border-color: #1A53FF !important;
}

.four-grid-btn.is-primary .grid-cell {
  background-color: #1A53FF;
}

/*  */
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

/* device info */
.device-stats {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px 14px;
  width: max-content;
  max-width: 100%;
  min-height: 36px;
  grid-column: 3;
  justify-self: end;
  margin-right: 0;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  padding: 6px 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

/* device */
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

/* button */
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

/* button */
:deep(.map-area.dark-mode) .extended-layout-btn {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}
</style>
