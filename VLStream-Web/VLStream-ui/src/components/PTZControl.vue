<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="ptz-control-panel" :class="{ 'ptz-control-panel--square': isSquare }">
    <!-- PTZcontrol -->
    <div class="ptz-section">
      <h4 class="section-title ptz-title">PTZ</h4>
      <div class="ptz-wrapper">
        <!-- control -->
        <div class="ptz-control-area">
          <div class="ptz-circle-container">
            <!--  -->
            <div class="ptz-outer-ring">
              <!--  -->
              <div class="ptz-inner-circle">
                <!-- in -->
                <div class="ptz-center-icon">
                  <img :src="rotateIcon" :alt="$tp('旋转')" class="ptz-center-rotate-icon" />
                </div>
              </div>

              <!--  -->
              <div class="ptz-direction-arrows">
                <!--  -->
                <div class="ptz-arrow ptz-arrow-up" role="button" :tabindex="movementEnabled ? 0 : -1" :aria-disabled="!movementEnabled"
                  @click="ptzControl('up')" @pointerdown="startMovement('up', $event)" @pointerup="stopMovement" @pointercancel="stopMovement" @pointerleave="stopMovement"
                  @keydown="startMovement('up', $event)" @keyup="stopMovement">
                  <img :src="isSquare ? ptzArrowIcon : directionUpIcon" :alt="$tp('上')" class="ptz-direction-icon" />
                </div>

                <!--  -->
                <div class="ptz-arrow ptz-arrow-right" role="button" :tabindex="movementEnabled ? 0 : -1" :aria-disabled="!movementEnabled"
                  @click="ptzControl('right')" @pointerdown="startMovement('right', $event)" @pointerup="stopMovement" @pointercancel="stopMovement" @pointerleave="stopMovement"
                  @keydown="startMovement('right', $event)" @keyup="stopMovement">
                  <img :src="isSquare ? ptzArrowIcon : directionUpIcon" :alt="$tp('右')" class="ptz-direction-icon ptz-rotate-90" />
                </div>

                <!--  -->
                <div class="ptz-arrow ptz-arrow-down" role="button" :tabindex="movementEnabled ? 0 : -1" :aria-disabled="!movementEnabled"
                  @click="ptzControl('down')" @pointerdown="startMovement('down', $event)" @pointerup="stopMovement" @pointercancel="stopMovement" @pointerleave="stopMovement"
                  @keydown="startMovement('down', $event)" @keyup="stopMovement">
                  <img :src="isSquare ? ptzArrowIcon : directionUpIcon" :alt="$tp('下')" class="ptz-direction-icon ptz-rotate-180" />
                </div>

                <!--  -->
                <div class="ptz-arrow ptz-arrow-left" role="button" :tabindex="movementEnabled ? 0 : -1" :aria-disabled="!movementEnabled"
                  @click="ptzControl('left')" @pointerdown="startMovement('left', $event)" @pointerup="stopMovement" @pointercancel="stopMovement" @pointerleave="stopMovement"
                  @keydown="startMovement('left', $event)" @keyup="stopMovement">
                  <img :src="isSquare ? ptzArrowIcon : directionUpIcon" :alt="$tp('左')" class="ptz-direction-icon ptz-rotate-270" />
                </div>
              </div>

              <div v-if="isSquare" class="ptz-grid" aria-hidden="true">
                <span class="ptz-grid-line ptz-grid-h ptz-grid-line-1"></span>
                <span class="ptz-grid-line ptz-grid-h ptz-grid-line-2"></span>
                <span class="ptz-grid-line ptz-grid-v ptz-grid-line-3"></span>
                <span class="ptz-grid-line ptz-grid-v ptz-grid-line-4"></span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <slot name="movement-settings"></slot>
    <!-- control -->
    <div class="zoom-section">
      <h4 class="section-title">{{ $tp('光学变倍控制') }}</h4>
      <div class="zoom-wrapper">
        <div class="zoom-control-area">
          <div class="zoom-background-container">
            <div class="zoom-control-bar">
              <button class="zoom-control-btn zoom-out" :disabled="!zoomEnabled" @click="zoomControl('out')" :title="$tp('缩小')">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19 13H5v-2h14v2z"/>
                </svg>
              </button>
              <div class="zoom-indicator">
                <span>{{ $tp('变倍') }}</span>
              </div>
              <button class="zoom-control-btn zoom-in" :disabled="!zoomEnabled" @click="zoomControl('in')" :title="$tp('放大')">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- can controlbutton -->
    <div class="function-section">
      <div class="control-layout">
        <!-- control -->
        <div class="focus-control-group">
          <button class="focus-btn" :disabled="!supportsAction('focus-plus')" @click="controlAction('focus-plus')" :title="$tp('调焦+')">
            <img :src="focusPlusIcon" :alt="$tp('调焦+')" class="function-icon" />
          </button>
          <button class="focus-btn" :disabled="!supportsAction('focus-minus')" @click="controlAction('focus-minus')" :title="$tp('调焦-')">
            <img :src="focusMinusIcon" :alt="$tp('调焦-')" class="function-icon" />
          </button>
        </div>

        <!-- control -->
        <div class="zoom-control-group">
          <button class="zoom-btn" :disabled="!supportsAction('zoom-plus')" @click="controlAction('zoom-plus')" :title="$tp('聚集+')">
            <img :src="zoomPlusIcon" :alt="$tp('聚集+')" class="function-icon" />
          </button>
          <button class="zoom-btn" :disabled="!supportsAction('zoom-minus')" @click="controlAction('zoom-minus')" :title="$tp('聚集-')">
            <img :src="zoomMinusIcon" :alt="$tp('聚集-')" class="function-icon" />
          </button>
        </div>

        <!-- control -->
        <div class="iris-control-group">
          <button class="iris-btn" :disabled="!supportsAction('iris-plus')" @click="controlAction('iris-plus')" :title="$tp('光圈+')">
            <img :src="irisPlusIcon" :alt="$tp('光圈+')" class="function-icon" />
          </button>
          <button class="iris-btn" :disabled="!supportsAction('iris-minus')" @click="controlAction('iris-minus')" :title="$tp('光圈-')">
            <img :src="irisMinusIcon" :alt="$tp('光圈-')" class="function-icon" />
          </button>
        </div>
      </div>

      <!-- can button -->
      <div class="function-grid">
        <div class="function-buttons-container">
          <!-- : 、 、 Initialize 、menu -->
          <div class="function-row four-cols">
            <button class="function-btn" :disabled="!supportsAction('light')" @click="controlAction('light')" :title="$tp('灯光')">
              <img :src="lightIcon" :alt="$tp('灯光')" class="function-icon" />
            </button>
            <button class="function-btn" :disabled="!supportsAction('assist-focus')" @click="controlAction('assist-focus')" :title="$tp('辅助聚集')">
              <img :src="assistFocusIcon" :alt="$tp('辅助聚集')" class="function-icon" />
            </button>
            <button class="function-btn" :disabled="!supportsAction('lens-init')" @click="controlAction('lens-init')" :title="$tp('镜头初始化')">
              <img :src="lensInitIcon" :alt="$tp('镜头初始化')" class="function-icon" />
            </button>
            <button class="function-btn" :disabled="!supportsAction('menu')" @click="controlAction('menu')" :title="$tp('菜单')">
              <img :src="menuIcon" :alt="$tp('菜单')" class="function-icon" />
            </button>
          </div>

          <!-- : 、 3D 、 、 -->
          <div class="function-row four-cols">
            <button class="function-btn" :disabled="!supportsAction('manual-track')" @click="controlAction('manual-track')" :title="$tp('开启手动跟踪')">
              <img :src="manualTrackIcon" :alt="$tp('开启手动跟踪')" class="function-icon" />
            </button>
            <button class="function-btn" :disabled="!supportsAction('3d-position')" @click="controlAction('3d-position')" :title="$tp('开启3D定位')">
              <img :src="position3dIcon" :alt="$tp('开启3D定位')" class="function-icon" />
            </button>
            <button class="function-btn" :disabled="!supportsAction('auto-cruise')" @click="controlAction('auto-cruise')" :title="$tp('一键巡航')">
              <img :src="autoCruiseIcon" :alt="$tp('一键巡航')" class="function-icon" />
            </button>
            <button class="function-btn" :disabled="!supportsAction('auto-watch')" @click="controlAction('auto-watch')" :title="$tp('一键守望')">
              <img :src="autoWatchIcon" :alt="$tp('一键守望')" class="function-icon" />
            </button>
          </div>
        </div>

        <!-- : after button -->
        <div class="function-row camera-management-row" v-if="showCameraManagement">
          <button class="function-btn camera-management" :disabled="!supportsAction('camera-management')" @click="controlAction('camera-management')" :title="$tp('摄像机管理后台')">
            <img :src="cameraManagementIcon" :alt="$tp('摄像机管理后台')" class="function-icon" />
            <span class="camera-text">{{ $tp('摄像机管理后台') }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, defineProps, defineEmits } from 'vue'

// Import PNG
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
import ptzArrowIcon from '@/assets/mastergo/video-square/ptz-arrow.svg'

// props
const props = defineProps({
  holdControls: Boolean,
  movementEnabled: { type: Boolean, default: true },
  zoomEnabled: { type: Boolean, default: true },
  supportedActions: { type: Array, default: null },
  showCameraManagement: {
    type: Boolean,
    default: false
  },
  variant: { type: String, default: 'default' }
})

// 视频广场浮层使用 MasterGo 设计稿布局
const isSquare = computed(() => props.variant === 'square')

// emits
const emit = defineEmits(['ptz-control', 'zoom-control', 'control-action', 'movement-start', 'movement-stop'])

// PTZcontrol method
const ptzControl = (direction) => {
  if (!props.holdControls && props.movementEnabled) emit('ptz-control', direction)
}

// control method
const zoomControl = (action) => {
  if (props.zoomEnabled) emit('zoom-control', action)
}

// can control method
const controlAction = (action) => {
  if (supportsAction(action)) emit('control-action', action)
}
const supportsAction = action => props.supportedActions === null || props.supportedActions.includes(action)
const startMovement = (direction, event) => {
  if (!props.holdControls || !props.movementEnabled || event.repeat) return
  if (event.type === 'keydown' && !['Enter', ' '].includes(event.key)) return
  if (event.type === 'pointerdown' && event.button !== 0) return
  event.preventDefault()
  emit('movement-start', direction)
}
const stopMovement = () => { if (props.holdControls) emit('movement-stop') }
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

/* PTZcontrol */
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

/* PTZ */
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

/* - 200px */
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

/* - 85px */
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

/* in */
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

/*  */
.ptz-direction-arrows {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
}

/*  */
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

/*  */
.ptz-direction-icon {
  width: 48px;
  height: 48px;
  object-fit: contain;
  transition: transform 0.2s ease;
}

/*  */
.ptz-rotate-90 {
  transform: rotate(90deg);
}

.ptz-rotate-180 {
  transform: rotate(180deg);
}

.ptz-rotate-270 {
  transform: rotate(270deg);
}

/*  */
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

/*  */
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

/*  */
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

/*  */
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
/* control */
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

/* can control */
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

/* after button */
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

/* 视频广场浮层：MasterGo 设计稿 (309:26907) 像素级布局 */
.ptz-control-panel--square {
  display: flex;
  flex-direction: column;
  gap: 20px;
  align-items: center;
  width: 100%;
  height: auto;
  padding: 0;
  background: transparent;
  border-radius: 0;
  box-shadow: none;
  overflow: visible;
}

/* 摇杆区 240x240 */
.ptz-control-panel--square .ptz-section {
  margin: 0 0 4px 0;
  align-items: center;
}

.ptz-control-panel--square .ptz-title {
  display: none;
}

.ptz-control-panel--square .ptz-wrapper {
  padding: 0;
  justify-content: center;
}

.ptz-control-panel--square .ptz-outer-ring {
  background: rgba(34, 120, 255, 0.12);
}

.ptz-control-panel--square .ptz-inner-circle {
  width: 102px;
  height: 102px;
  box-shadow: none;
}

.ptz-control-panel--square .ptz-center-rotate-icon {
  width: 38px;
  height: 38px;
}

/* 4 个方向按钮 48x48：上(96,10) 下(96,182) 左(10,96) 右(182,96) */
.ptz-control-panel--square .ptz-arrow,
.ptz-control-panel--square .ptz-direction-icon {
  width: 48px;
  height: 48px;
}

.ptz-control-panel--square .ptz-arrow {
  transform: none;
}

.ptz-control-panel--square .ptz-arrow-up {
  top: 10px;
  left: 96px;
}

.ptz-control-panel--square .ptz-arrow-right {
  top: 96px;
  right: auto;
  left: 182px;
}

.ptz-control-panel--square .ptz-arrow-down {
  top: 182px;
  bottom: auto;
  left: 96px;
}

.ptz-control-panel--square .ptz-arrow-left {
  top: 96px;
  left: 10px;
}

/* 摇杆网格线：y=82.5 / y=154.5 横线，x=84 / x=156 竖线 */
.ptz-control-panel--square .ptz-grid {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  overflow: hidden;
  pointer-events: none;
}

.ptz-control-panel--square .ptz-grid-line {
  position: absolute;
  background: rgba(34, 120, 255, 0.12);
}

.ptz-control-panel--square .ptz-grid-h {
  left: 0;
  width: 100%;
  height: 1px;
}

.ptz-control-panel--square .ptz-grid-v {
  top: 0;
  width: 1px;
  height: 100%;
}

.ptz-control-panel--square .ptz-grid-line-1 { top: 82.5px; }

.ptz-control-panel--square .ptz-grid-line-2 { top: 154.5px; }

.ptz-control-panel--square .ptz-grid-line-3 { left: 84px; }

.ptz-control-panel--square .ptz-grid-line-4 { left: 156px; }

/* 光学变倍控制 296x76 */
.ptz-control-panel--square .zoom-section {
  width: 100%;
  margin: 0;
  align-items: flex-start;
}

.ptz-control-panel--square .zoom-section .section-title {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 400;
  line-height: 24px;
  color: #000000;
}

.ptz-control-panel--square .zoom-wrapper {
  width: 100%;
  padding: 0;
  justify-content: flex-start;
}

.ptz-control-panel--square .zoom-control-area {
  width: 100%;
}

.ptz-control-panel--square .zoom-background-container {
  position: relative;
  width: 100%;
  height: 40px;
  padding: 0;
  margin-left: 0;
  border-radius: 20px;
  background: rgba(34, 120, 255, 0.12);
}

.ptz-control-panel--square .zoom-control-bar {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
}

.ptz-control-panel--square .zoom-control-btn {
  position: absolute;
  top: 8px;
  width: 24px;
  height: 24px;
  padding: 0;
  border-radius: 0;
}

/* 16x2.4 横杠 / 16x16 加号：24 视口内的 14 单位图形按 16px 呈现 */
.ptz-control-panel--square .zoom-control-btn svg {
  width: 27.4286px;
  height: 27.4286px;
}

.ptz-control-panel--square .zoom-out { left: 26px; }

.ptz-control-panel--square .zoom-in { left: 248px; }

.ptz-control-panel--square .zoom-indicator {
  position: absolute;
  top: 11px;
  left: 133px;
  height: 19px;
  margin: 0;
  font-size: 14px;
  font-weight: 400;
  line-height: 19px;
  color: #2278FF;
}

/* 三张功能小卡 92x137 */
.ptz-control-panel--square .function-section {
  width: 100%;
  margin: 0;
  gap: 10px;
}

.ptz-control-panel--square .control-layout {
  width: 100%;
  gap: 10px;
}

.ptz-control-panel--square .focus-control-group,
.ptz-control-panel--square .zoom-control-group,
.ptz-control-panel--square .iris-control-group {
  position: relative;
  padding: 0;
  gap: 0;
  background: #FFFFFF;
}

.ptz-control-panel--square .focus-control-group::after,
.ptz-control-panel--square .zoom-control-group::after,
.ptz-control-panel--square .iris-control-group::after {
  content: '';
  position: absolute;
  top: 68px;
  left: 22px;
  width: 48px;
  height: 1px;
  background: #F0F2F5;
}

.ptz-control-panel--square .focus-btn,
.ptz-control-panel--square .iris-btn,
.ptz-control-panel--square .zoom-btn {
  position: absolute;
  left: 22px;
  width: 48px;
  height: 48px;
  padding: 0;
  border-radius: 0;
}

.ptz-control-panel--square .focus-control-group > :first-child,
.ptz-control-panel--square .zoom-control-group > :first-child,
.ptz-control-panel--square .iris-control-group > :first-child {
  top: 12px;
}

.ptz-control-panel--square .focus-control-group > :last-child,
.ptz-control-panel--square .zoom-control-group > :last-child,
.ptz-control-panel--square .iris-control-group > :last-child {
  top: 77px;
}

/* 八图标卡 & 摄像机管理后台卡 */
.ptz-control-panel--square .function-grid {
  width: 100%;
  margin: 0;
  gap: 10px;
}

.ptz-control-panel--square .function-icon {
  width: 48px;
  height: 48px;
}

.ptz-control-panel--square .function-buttons-container {
  width: 100%;
  height: 148px;
  padding: 16px 22px;
  gap: 20px;
  justify-content: flex-start;
  background: #FFFFFF;
}

.ptz-control-panel--square .function-buttons-container .function-row.four-cols {
  grid-template-columns: repeat(4, 48px);
  gap: 20px;
}

.ptz-control-panel--square .function-buttons-container .function-btn {
  width: 48px;
  height: 48px;
  padding: 0;
}

.ptz-control-panel--square .camera-management-row {
  width: 100%;
  margin: 0;
}

.ptz-control-panel--square .camera-management {
  width: 100% !important;
  height: 53px !important;
  padding: 0 !important;
  gap: 8px !important;
  background: #FFFFFF !important;
}

.ptz-control-panel--square .camera-text {
  font-size: 16px;
  font-weight: 400;
  color: #333333;
}
</style>
<style scoped>
.ptz-arrow[aria-disabled="true"], button:disabled { opacity: .4; cursor: not-allowed; }
.ptz-arrow { touch-action: none; }
</style>
