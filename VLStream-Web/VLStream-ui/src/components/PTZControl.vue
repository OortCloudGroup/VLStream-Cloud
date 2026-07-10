<template>
  <div class="ptz-control-panel">
    <!-- PTZ控制 -->
    <div class="ptz-section">
      <h4 class="section-title ptz-title">PTZ</h4>
      <div class="ptz-wrapper">
        <!-- 圆形方向控制 -->
        <div class="ptz-control-area">
          <div class="ptz-circle-container">
            <!-- 外圆环 -->
            <div class="ptz-outer-ring">
              <!-- 内圆 -->
              <div class="ptz-inner-circle">
                <!-- 中心旋转箭头图标 -->
                <div class="ptz-center-icon">
                  <img :src="rotateIcon" alt="旋转" class="ptz-center-rotate-icon" />
                </div>
              </div>
              
              <!-- 方向箭头 -->
              <div class="ptz-direction-arrows">
                <!-- 上箭头 -->
                <div class="ptz-arrow ptz-arrow-up" @click="ptzControl('up')">
                  <img :src="directionUpIcon" alt="上" class="ptz-direction-icon" />
                </div>
                
                <!-- 右箭头 -->
                <div class="ptz-arrow ptz-arrow-right" @click="ptzControl('right')">
                  <img :src="directionUpIcon" alt="右" class="ptz-direction-icon ptz-rotate-90" />
                </div>
                
                <!-- 下箭头 -->
                <div class="ptz-arrow ptz-arrow-down" @click="ptzControl('down')">
                  <img :src="directionUpIcon" alt="下" class="ptz-direction-icon ptz-rotate-180" />
                </div>
                
                <!-- 左箭头 -->
                <div class="ptz-arrow ptz-arrow-left" @click="ptzControl('left')">
                  <img :src="directionUpIcon" alt="左" class="ptz-direction-icon ptz-rotate-270" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 光学变倍控制 -->
    <div class="zoom-section">
      <h4 class="section-title">光学变倍控制</h4>
      <div class="zoom-wrapper">
        <div class="zoom-control-area">
          <div class="zoom-background-container">
            <div class="zoom-control-bar">
              <button class="zoom-control-btn zoom-out" @click="zoomControl('out')" title="缩小">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19 13H5v-2h14v2z"/>
                </svg>
              </button>
              <div class="zoom-indicator">
                <span>变倍</span>
              </div>
              <button class="zoom-control-btn zoom-in" @click="zoomControl('in')" title="放大">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 功能控制按钮 -->
    <div class="function-section">
      <div class="control-layout">
        <!-- 调焦控制组 -->
        <div class="focus-control-group">
          <button class="focus-btn" @click="controlAction('focus-plus')" title="调焦+">
            <img :src="focusPlusIcon" alt="调焦+" class="function-icon" />
          </button>
          <button class="focus-btn" @click="controlAction('focus-minus')" title="调焦-">
            <img :src="focusMinusIcon" alt="调焦-" class="function-icon" />
          </button>
        </div>
        
        <!-- 聚集控制组 -->
        <div class="zoom-control-group">
          <button class="zoom-btn" @click="controlAction('zoom-plus')" title="聚集+">
            <img :src="zoomPlusIcon" alt="聚集+" class="function-icon" />
          </button>
          <button class="zoom-btn" @click="controlAction('zoom-minus')" title="聚集-">
            <img :src="zoomMinusIcon" alt="聚集-" class="function-icon" />
          </button>
        </div>
        
        <!-- 光圈控制组 -->
        <div class="iris-control-group">
          <button class="iris-btn" @click="controlAction('iris-plus')" title="光圈+">
            <img :src="irisPlusIcon" alt="光圈+" class="function-icon" />
          </button>
          <button class="iris-btn" @click="controlAction('iris-minus')" title="光圈-">
            <img :src="irisMinusIcon" alt="光圈-" class="function-icon" />
          </button>
        </div>
      </div>
      
      <!-- 其他功能按钮 -->
      <div class="function-grid">
        <div class="function-buttons-container">
          <!-- 第三行：灯光、辅助聚集、镜头初始化、菜单 -->
          <div class="function-row four-cols">
            <button class="function-btn" @click="controlAction('light')" title="灯光">
              <img :src="lightIcon" alt="灯光" class="function-icon" />
            </button>
            <button class="function-btn" @click="controlAction('assist-focus')" title="辅助聚集">
              <img :src="assistFocusIcon" alt="辅助聚集" class="function-icon" />
            </button>
            <button class="function-btn" @click="controlAction('lens-init')" title="镜头初始化">
              <img :src="lensInitIcon" alt="镜头初始化" class="function-icon" />
            </button>
            <button class="function-btn" @click="controlAction('menu')" title="菜单">
              <img :src="menuIcon" alt="菜单" class="function-icon" />
            </button>
          </div>
          
          <!-- 第四行：开启手动跟踪、开启3D定位、一键巡航、一键守望 -->
          <div class="function-row four-cols">
            <button class="function-btn" @click="controlAction('manual-track')" title="开启手动跟踪">
              <img :src="manualTrackIcon" alt="开启手动跟踪" class="function-icon" />
            </button>
            <button class="function-btn" @click="controlAction('3d-position')" title="开启3D定位">
              <img :src="position3dIcon" alt="开启3D定位" class="function-icon" />
            </button>
            <button class="function-btn" @click="controlAction('auto-cruise')" title="一键巡航">
              <img :src="autoCruiseIcon" alt="一键巡航" class="function-icon" />
            </button>
            <button class="function-btn" @click="controlAction('auto-watch')" title="一键守望">
              <img :src="autoWatchIcon" alt="一键守望" class="function-icon" />
            </button>
          </div>
        </div>
        
        <!-- 第五行：摄像机管理后台按钮 -->
        <div class="function-row camera-management-row" v-if="showCameraManagement">
          <button class="function-btn camera-management" @click="controlAction('camera-management')" title="摄像机管理后台">
            <img :src="cameraManagementIcon" alt="摄像机管理后台" class="function-icon" />
            <span class="camera-text">摄像机管理后台</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

// 导入PNG图标
import focusPlusIcon from '@/assets/调焦+.png'
import focusMinusIcon from '@/assets/调焦-.png'
import zoomPlusIcon from '@/assets/聚集+.png'
import zoomMinusIcon from '@/assets/聚集-.png'
import irisPlusIcon from '@/assets/光圈+.png'
import irisMinusIcon from '@/assets/光圈-.png'
import lightIcon from '@/assets/灯光.png'
import assistFocusIcon from '@/assets/辅助聚集.png'
import lensInitIcon from '@/assets/镜头初始化.png'
import menuIcon from '@/assets/菜单.png'
import manualTrackIcon from '@/assets/开启手动跟踪.png'
import position3dIcon from '@/assets/开启3D定位.png'
import autoCruiseIcon from '@/assets/一键巡航.png'
import autoWatchIcon from '@/assets/一键守望.png'
import cameraManagementIcon from '@/assets/摄像机管理后台.png'
import directionUpIcon from '@/assets/directionUpIcon.png'
import rotateIcon from '@/assets/rotateIcon.png'

// 定义props
const props = defineProps({
  showCameraManagement: {
    type: Boolean,
    default: false
  }
})

// 定义emits
const emit = defineEmits(['ptz-control', 'zoom-control', 'control-action'])

// PTZ控制方法
const ptzControl = (direction) => {
  emit('ptz-control', direction)
}

// 变倍控制方法
const zoomControl = (action) => {
  emit('zoom-control', action)
}

// 功能控制方法
const controlAction = (action) => {
  emit('control-action', action)
}
</script>

<style scoped>
.ptz-control-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
  height: 100%;
  box-sizing: border-box;
  align-items: center;
  width: 320px;
  background: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* PTZ控制区域 */
.ptz-section {
  margin-bottom: 24px;
  margin-top: -20px;
  overflow: visible !important;
  height: auto !important;
  max-height: none !important;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  position: relative;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
  text-align: left;
}

/* PTZ标题特殊样式 */
.ptz-section .ptz-title {
  position: relative;
  transform: translateY(15px) translateX(-24px);
  margin: 0 0 12px 0 !important;
}

.ptz-wrapper {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  padding: 8px 0;
  overflow: visible;
  height: auto;
}

.ptz-control-area {
  display: flex;
  justify-content: flex-start;
}

.ptz-circle-container {
  position: relative;
  z-index: 1;
}

/* 外圆环 - 200px */
.ptz-outer-ring {
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: #E3EAFF;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-left: 0;
}

/* 内圆 - 85px */
.ptz-inner-circle {
  width: 85px;
  height: 85px;
  border-radius: 50%;
  background: white;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 中心旋转箭头图标 */
.ptz-center-icon {
  display: flex;
  justify-content: center;
  align-items: center;
}

.ptz-center-rotate-icon {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

/* 方向箭头容器 */
.ptz-direction-arrows {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
}

/* 箭头通用样式 */
.ptz-arrow {
  position: absolute;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  justify-content: center;
  align-items: center;
}

.ptz-arrow:hover {
  opacity: 0.8;
}

/* 方向图标样式 */
.ptz-direction-icon {
  width: 48px;
  height: 48px;
  object-fit: contain;
  transition: transform 0.2s ease;
}

/* 旋转样式 */
.ptz-rotate-90 {
  transform: rotate(90deg);
}

.ptz-rotate-180 {
  transform: rotate(180deg);
}

.ptz-rotate-270 {
  transform: rotate(270deg);
}

/* 上箭头 */
.ptz-arrow-up {
  top: 8px;
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 40px;
}

.ptz-arrow-up:hover .ptz-direction-icon {
  transform: scale(1.1);
}

/* 右箭头 */
.ptz-arrow-right {
  top: 50%;
  right: 8px;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
}

.ptz-arrow-right:hover .ptz-direction-icon {
  transform: rotate(90deg) scale(1.1);
}

/* 下箭头 */
.ptz-arrow-down {
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 40px;
}

.ptz-arrow-down:hover .ptz-direction-icon {
  transform: rotate(180deg) scale(1.1);
}

/* 左箭头 */
.ptz-arrow-left {
  top: 50%;
  left: 8px;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
}

.ptz-arrow-left:hover .ptz-direction-icon {
  transform: rotate(270deg) scale(1.1);
}
/* 光学变倍控制区域 */
.zoom-section {
  margin-bottom: 6px;
  margin-top: -30px;
  width: 100%;
  overflow: visible !important;
  height: auto !important;
  max-height: none !important;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.zoom-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px 0;
  overflow: visible;
  height: auto;
}

.zoom-control-area {
  display: flex;
  justify-content: flex-start;
}

.zoom-background-container {
  background: #E3EAFF;
  border-radius: 20px;
  padding: 8px 12px;
  width: 296px;
  height: 48px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-left: 0;
}

.zoom-control-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  background: transparent;
  border: none;
  width: 100%;
  height: 100%;
}

.zoom-control-btn {
  width: 36px;
  height: 36px;
  border-radius: 16px;
  background: transparent;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  color: #1A53FF;
}

.zoom-control-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.05);
}

.zoom-indicator {
  flex: 1;
  height: 36px;
  background: transparent;
  border-radius: 16px;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 6px;
  font-size: 13px;
  color: #1A53FF;
  font-weight: 500;
}

/* 功能控制区域 */
.function-section {
  margin-bottom: 0;
  margin-top: -20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.control-layout {
  display: flex;
  gap: 16px;
  width: 296px;
  align-items: flex-start;
  justify-content: space-between;
  position: relative;
}

.focus-control-group {
  position: relative;
}

.zoom-control-group {
  position: relative;
  z-index: 1;
}

.iris-control-group {
  position: relative;
}

.focus-control-group,
.zoom-control-group,
.iris-control-group {
  width: 92px;
  height: 137px;
  background: #F0F2F5;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  align-items: center;
  padding: 16px 8px;
  gap: 8px;
}

.focus-btn,
.iris-btn,
.zoom-btn {
  width: 100%;
  height: 60px;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.focus-btn:hover,
.iris-btn:hover,
.zoom-btn:hover {
  background: rgba(227, 234, 255, 0.5);
  transform: translateY(-1px);
}

.function-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 296px;
  margin-top: 16px;
  align-items: center;
}

.function-buttons-container {
  width: 296px;
  height: 148px;
  background: #F0F2F5;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  align-items: center;
  padding: 16px;
  gap: 8px;
}

.function-buttons-container .function-row.four-cols {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  width: 100%;
}

.function-buttons-container .function-btn {
  width: 100%;
  height: 48px;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  padding: 4px;
}

.function-buttons-container .function-btn:hover {
  background: rgba(227, 234, 255, 0.5);
  transform: translateY(-1px);
}

.function-row {
  display: flex;
  gap: 8px;
}

.function-row.two-cols {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.function-row.three-cols {
  justify-content: space-between;
}

.function-row.four-cols {
  justify-content: space-between;
}

.function-btn {
  width: 100%;
  height: 36px;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  padding: 4px;
}

.function-btn:hover {
  background: rgba(227, 234, 255, 0.5);
  transform: translateY(-1px);
}

.function-icon {
  width: 42px;
  height: 42px;
  object-fit: contain;
}

.camera-text {
  font-size: 12px;
  color: #303133;
  font-weight: 500;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
  text-align: left;
}

.ptz-direction-btn {
  position: absolute;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: white;
  border: 1px solid #e0e6ed;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  color: #4A90E2;
}

.ptz-direction-btn:hover {
  background: #f8faff;
  border-color: #4A90E2;
  transform: scale(1.1);
  box-shadow: 0 3px 8px rgba(74, 144, 226, 0.3);
}

.ptz-direction-btn:active {
  transform: scale(0.95);
}

.ptz-up {
  top: -16px;
  left: 50%;
  transform: translateX(-50%);
}

.ptz-down {
  bottom: -16px;
  left: 50%;
  transform: translateX(-50%);
}

.ptz-left {
  left: -16px;
  top: 50%;
  transform: translateY(-50%);
}

.ptz-right {
  right: -16px;
  top: 50%;
  transform: translateY(-50%);
}

.ptz-center-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #f8faff;
  border: 1px solid #e0e6ed;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  color: #4A90E2;
}

.ptz-center-btn:hover {
  background: #e8f2ff;
  border-color: #4A90E2;
  transform: scale(1.05);
}

/* 摄像机管理后台按钮行 */
.camera-management-row {
  justify-content: center;
  margin-top: 12px;
}

.camera-management {
  background: #F0F2F5 !important;
  border: none !important;
  color: #333 !important;
  padding: 6px 16px !important;
  border-radius: 8px !important;
  display: flex !important;
  align-items: center !important;
  gap: 6px !important;
  font-size: 12px !important;
  font-weight: 500 !important;
  transition: all 0.3s ease !important;
  width: 296px !important;
  height: 44px !important;
}

.camera-management:hover {
  background: rgba(227, 234, 255, 0.5) !important;
  transform: translateY(-1px) !important;
}

.camera-management:active {
  transform: translateY(0) !important;
}

.camera-text {
  white-space: nowrap;
  font-size: 12px;
}
</style> 