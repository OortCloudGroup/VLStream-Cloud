<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div
    class="layout-dialog-overlay"
    :style="{ zIndex: 3000 + dialog.id }"
  >
    <div
      class="layout-dialog draggable-dialog"
      :style="{
        left: dialog.position.x + 'px',
        top: dialog.position.y + 'px',
        position: 'fixed',
        transform: 'none'
      }"
      @mousedown="bringToFront"
    >
      <div
        class="layout-dialog-header draggable-handle"
        @mousedown="startDrag"
      >
        <span class="layout-dialog-title">{{ getLayoutTitle(dialog.layoutCount) }} - 视频播放</span>
        <div class="dialog-controls">
          <button class="dialog-control-btn minimize-btn" @click="minimize" title="最小化">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 13H5v-2h14v2z"/>
            </svg>
          </button>
          <button class="dialog-control-btn close-btn" @click="close" title="关闭">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
          </button>
        </div>
      </div>

      <div class="layout-dialog-content" v-show="!dialog.minimized">
        <div class="dialog-main-content">
          <!--  -->
          <div class="video-player-section">
            <div class="video-area">
              <!--  -->
              <div class="video-grid" :class="`layout-${getLayoutClass(dialog.layoutCount)}`">
                <div
                  v-for="index in dialog.layoutCount"
                  :key="`window-${index}`"
                  class="video-window"
                  :class="{
                    'dragging': draggedWindowIndex === index - 1,
                    'drag-over': dragOverWindowIndex === index - 1
                  }"
                  :data-window-index="index - 1"
                  :data-dialog-id="dialog.id"
                  draggable="true"
                  @click="handleVideoWindowClick(index - 1)"
                  @dblclick="handleVideoWindowDoubleClick(index - 1)"
                  @dragstart="handleDragStart($event, index - 1)"
                  @dragover.prevent="handleDragOver($event, index - 1)"
                  @dragenter.prevent="handleDragEnter($event, index - 1)"
                  @dragleave="handleDragLeave($event, index - 1)"
                  @drop="handleDrop($event, index - 1)"
                  @dragend="handleDragEnd"
                >
                  <!-- layer - all can in and -->
                  <div class="drag-overlay"
                       @click.stop="handleVideoWindowClick(index - 1)"
                       @dblclick.stop="handleVideoWindowDoubleClick(index - 1)"
                       @dragstart.stop="handleDragStart($event, index - 1)"
                       @dragover.stop.prevent="handleDragOver($event, index - 1)"
                       @dragenter.stop.prevent="handleDragEnter($event, index - 1)"
                       @dragleave.stop="handleDragLeave($event, index - 1)"
                       @drop.stop="handleDrop($event, index - 1)"
                       @dragend.stop="handleDragEnd">
                  </div>
                  <!-- if data, -->
                  <template v-if="dialog.cameras[index - 1]">
                    <!-- OPlayer -->
                    <div
                      v-if="dialog.cameras[index - 1].deviceData && dialog.cameras[index - 1].deviceData.streamUrl"
                      :ref="el => setOPlayerContainer(index - 1, el)"
                      class="oplayer-container"
                    />

                    <div v-else class="video-placeholder">
                      <div class="placeholder-content">
                        <div class="placeholder-icon">📹</div>
                        <div class="placeholder-text">暂无视频流</div>
                        <div class="placeholder-details">
                          <div>设备: {{ dialog.cameras[index - 1].name }}</div>
                          <div>状态: 在线</div>
                        </div>

                        <!-- operation item -->
                        <div class="placeholder-actions">
                          <button class="action-btn primary" @click="retryWebRTCConnection(dialog.cameras[index - 1])">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
                            </svg>
                            重试连接
                          </button>
                          <button class="action-btn secondary" @click="copyStreamUrl(dialog.cameras[index - 1].deviceData?.streamUrl)">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M16 1H4C2.9 1 2 1.9 2 3v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/>
                            </svg>
                            复制地址
                          </button>
                          <button class="action-btn secondary" @click="openInVlc(dialog.cameras[index - 1].deviceData?.streamUrl)">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z"/>
                            </svg>
                            VLC播放
                          </button>
                        </div>
                      </div>
                    </div>
                  </template>

                  <!-- data null / empty -->
                  <template v-else>
                    <div class="video-placeholder empty-placeholder">
                      <div class="placeholder-content">
                        <div class="placeholder-icon">📺</div>
                        <div class="placeholder-text">空白窗口</div>
                        <div class="placeholder-details">
                          <div>位置: {{ index }}</div>
                          <div>状态: 待分配</div>
                        </div>

                        <!-- device item -->
                        <div class="placeholder-actions">
                          <button class="action-btn primary" @click="addDeviceToWindow(index - 1)">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
                            </svg>
                            添加设备
                          </button>
                        </div>
                      </div>
                    </div>
                  </template>
                </div>
              </div>

              <!-- recordingcontrol -->
              <div class="video-controls">
                <!-- recording -->
                <div v-if="dialog.recording?.isRecording" class="recording-status">
                  <div class="recording-indicator">
                    <span class="recording-dot"></span>
                    录制中 {{ dialog.recording.currentRecordTime }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- PTZcontrol -->
          <div class="ptz-control-section">
            <PTZControl
              :show-camera-management="true"
              @ptz-control="handlePTZControl"
              @zoom-control="handleZoomControl"
              @control-action="handleControlAction"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import PTZControl from './PTZControl.vue'
import { ensureWebRTCBackendConfig, WEBRTC_SERVER_BASE_URL } from '@/api/webrtc'
import { ensureOPlayer } from '@/utils/oplayer'
import { getStreamType } from '@/views/VideoAggregation/deviceUtils.js'

// Props
const props = defineProps({
  dialog: {
    type: Object,
    required: true
  }
})

// Emits
const emit = defineEmits([
  'close',
  'minimize',
  'bring-to-front',
  'start-drag',
  'webrtc-load',
  'webrtc-error',
  'rtsp-connected',
  'rtsp-disconnected',
  'rtsp-error',
  'ptz-control',
  'zoom-control',
  'control-action',
  'retry-webrtc',
  'copy-stream-url',
  'open-in-vlc',
  'add-device',
  'video-window-double-click',
  'video-window-swap'
])

// Get
const getLayoutClass = (count) => {
  const classes = {
    1: '1x1',
    4: '2x2',
    6: '3x3',
    8: '4x4',
    9: '5x5',
    16: '6x6',
    22: '6x6', // 6x6
    24: '6x6', // 6x6
    25: '6x6'  // 6x6
  }
  return classes[count] || '1x1'
}

// Get
const getLayoutTitle = (count) => {
  const titleMap = {
    1: '单画面',
    4: '四分屏',
    6: '六分屏',
    8: '八分屏',
    9: '九分屏',
    16: '十六分屏',
    22: '二十二画面',
    24: '二十四画面',
    25: '二十五画面'
  }
  return titleMap[count] || '视频播放'
}

// eventProcess
const close = () => {
  emit('close', props.dialog.id)
}

const minimize = () => {
  emit('minimize', props.dialog.id)
}

const bringToFront = () => {
  emit('bring-to-front', props.dialog.id)
}

const startDrag = (event) => {
  emit('start-drag', event, props.dialog.id)
}

const handlePTZControl = (data) => {
  emit('ptz-control', data)
}

const handleZoomControl = (data) => {
  emit('zoom-control', data)
}

const handleControlAction = (data) => {
  emit('control-action', data)
}

const retryWebRTCConnection = (camera) => {
  emit('retry-webrtc', camera)
}

const copyStreamUrl = (url) => {
  emit('copy-stream-url', url)
}

const openInVlc = (url) => {
  emit('open-in-vlc', url)
}

const addDeviceToWindow = (windowIndex) => {
  emit('add-device', props.dialog.id, windowIndex)
}


// OPlayer
const oplayerContainers = ref(new Map())
const oplayerInstances = ref(new Map())
const oplayerTasks = ref(new Map())

/**
 * record , .
 */
const setOPlayerContainer = (windowIndex, element) => {
  if (element) {
    oplayerContainers.value.set(windowIndex, element)
    return
  }

  cleanupOPlayer(windowIndex)
  oplayerContainers.value.delete(windowIndex)
}

/**
 * and task.
 */
const cleanupOPlayer = (windowIndex) => {
  oplayerTasks.value.delete(windowIndex)

  const player = oplayerInstances.value.get(windowIndex)
  if (player?.compInstance?.$destroy) {
    player.compInstance.$destroy()
  }
  oplayerInstances.value.delete(windowIndex)

  const container = oplayerContainers.value.get(windowIndex)
  if (container) {
    container.innerHTML = ''
  }
}

/**
 * current dialog full .
 */
const cleanupAllOPlayers = () => {
  const windowIndexes = new Set([
    ...oplayerInstances.value.keys(),
    ...oplayerContainers.value.keys()
  ])
  windowIndexes.forEach(cleanupOPlayer)
}

/**
 * from CameraRTC in Parse service and ID.
 */
const parseCameraRtcConfig = (streamUrl) => {
  const url = new URL(streamUrl)
  const cameraId = url.pathname.split('/').filter(Boolean).pop()
  if (!cameraId) {
    throw new Error('CameraRTC 地址中缺少摄像头ID')
  }

  return {
    cameraId,
    socketUrl: url.origin.replace(/^http/, 'ws')
  }
}

/**
 * Generate OPlayer parameter.
 */
const createOPlayerOptions = async (streamUrl, streamType) => {
  const playerConfig = {
    debuggerMode: false,
    autoSize: true,
    backgroundColor: '#000000',
    showHeader: true
  }

  if (streamType === 'cameraRTC') {
    const cameraRtcConfig = parseCameraRtcConfig(streamUrl)
    playerConfig.webRTCSocketURL = cameraRtcConfig.socketUrl
    return {
      playerConfig,
      playConfig: {
        type: 'cameraRTC',
        src: cameraRtcConfig.cameraId
      }
    }
  }

  if (streamType === 'rtsp') {
    await ensureWebRTCBackendConfig()
    playerConfig.rtspServerURL = WEBRTC_SERVER_BASE_URL
    return {
      playerConfig,
      playConfig: {
        type: 'rtsp',
        src: streamUrl,
        transport: 'tcp',
        timeout: 60,
        preferredMime: 'video/H264'
      }
    }
  }

  const playTypeMap = {
    flv: 'flv',
    hls: 'm3u8',
    video: 'mp4',
    http: 'mp4'
  }
  const playType = playTypeMap[streamType]
  if (!playType) {
    throw new Error(`暂不支持该视频流类型：${streamType}`)
  }

  return {
    playerConfig,
    playConfig: {
      type: playType,
      src: streamUrl
    }
  }
}

/**
 * in in OPlayer.
 */
const playOPlayerStream = async (camera, windowIndex) => {
  const taskId = Symbol(`oplayer-${windowIndex}`)
  oplayerTasks.value.set(windowIndex, taskId)

  try {
    const streamUrl = camera?.deviceData?.streamUrl
    if (!streamUrl) return

    await Promise.all([ensureOPlayer(), nextTick()])
    const streamType = getStreamType(streamUrl)
    const { playerConfig, playConfig } = await createOPlayerOptions(streamUrl, streamType)
    const container = oplayerContainers.value.get(windowIndex)
    if (!container || oplayerTasks.value.get(windowIndex) !== taskId) return

    cleanupOPlayer(windowIndex)
    oplayerTasks.value.set(windowIndex, taskId)

    const player = new window.OToolBox.OPlayer(container, playerConfig)
    oplayerInstances.value.set(windowIndex, player)
    player.play({
      ...playConfig,
      name: camera?.name || ''
    })
  } catch (error) {
    if (oplayerTasks.value.get(windowIndex) === taskId) {
      cleanupOPlayer(windowIndex)
      console.error('OPlayer 播放失败:', error)
      emit('webrtc-error', error)
    }
  }
}

/**
 * current data instance.
 */
const syncOPlayerPlayers = async () => {
  await nextTick()
  const cameras = props.dialog?.cameras || []

  cameras.forEach((camera, index) => {
    if (camera?.deviceData?.streamUrl) {
      playOPlayerStream(camera, index)
    } else {
      cleanupOPlayer(index)
    }
  })

  for (const windowIndex of oplayerInstances.value.keys()) {
    if (windowIndex >= cameras.length) {
      cleanupOPlayer(windowIndex)
    }
  }
}

watch(
  () => (props.dialog?.cameras || []).map(camera => camera
    ? `${camera?.id || camera?.deviceData?.id || ''}:${camera?.deviceData?.streamUrl || ''}`
    : 'empty'),
  syncOPlayerPlayers,
  { deep: true, immediate: true }
)

onMounted(syncOPlayerPlayers)
onBeforeUnmount(cleanupAllOPlayers)

//
const draggedWindowIndex = ref(null)
const dragOverWindowIndex = ref(null)

// Process
const handleVideoWindowClick = (windowIndex) => {
  emit('video-window-click', props.dialog.id, windowIndex)
}

// Process
const handleVideoWindowDoubleClick = (windowIndex) => {
  emit('video-window-double-click', props.dialog.id, windowIndex)
}

// start
const handleDragStart = (event, windowIndex) => {
  const dialog = props.dialog
  if (!dialog) {
    event.preventDefault()
    return
  }

  // Set
  draggedWindowIndex.value = windowIndex

  // Set data
  event.dataTransfer.effectAllowed = 'move'
  try {
    event.dataTransfer.setData('text/plain', JSON.stringify({
      dialogId: dialog.id,
      windowIndex,
      cameraData: dialog.cameras[windowIndex] || null
    }))
  } catch (e) {
    console.error('设置拖拽数据失败:', e)
    event.preventDefault()
    return
  }

  console.log('开始拖拽窗口:', dialog.id, windowIndex)
}

// finish
const handleDragEnd = (event) => {
  console.log('窗口拖拽结束')

  //
  draggedWindowIndex.value = null
  dragOverWindowIndex.value = null
}

//
const handleDragOver = (event, windowIndex) => {
  event.preventDefault()
  event.dataTransfer.dropEffect = 'move'
}

//
const handleDragEnter = (event, windowIndex) => {
  event.preventDefault()
  dragOverWindowIndex.value = windowIndex
}

//
const handleDragLeave = (event, windowIndex) => {
  // element
  const rect = event.currentTarget.getBoundingClientRect()
  const x = event.clientX
  const y = event.clientY

  if (x < rect.left || x > rect.right || y < rect.top || y > rect.bottom) {
    dragOverWindowIndex.value = null
  }
}

//
const handleDrop = (event, targetWindowIndex) => {
  event.preventDefault()
  event.stopPropagation()

  try {
    const data = event.dataTransfer.getData('text/plain')
    if (!data) {
      throw new Error('无拖拽数据')
    }
    const dragData = JSON.parse(data)
    const sourceDialogId = dragData.dialogId
    const sourceWindowIndex = dragData.windowIndex

    // if is , Process
    if (sourceDialogId === props.dialog.id && sourceWindowIndex === targetWindowIndex) {
      return
    }

    // Execute
    emit('video-window-swap', sourceDialogId, sourceWindowIndex, props.dialog.id, targetWindowIndex)

    console.log('窗口交换完成:', {
      from: { dialogId: sourceDialogId, windowIndex: sourceWindowIndex },
      to: { dialogId: props.dialog.id, windowIndex: targetWindowIndex }
    })

  } catch (e) {
    console.error('处理拖拽放置失败:', e)
  }

  //
  draggedWindowIndex.value = null
  dragOverWindowIndex.value = null
}
</script>

<style scoped>
/* dialog */
.layout-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none;
  z-index: 3000;
}

.layout-dialog {
  position: fixed;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  border: 1px solid #e4e7ed;
  min-width: 1400px;
  min-height: 900px;
  max-height: 95vh;
  pointer-events: auto;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.layout-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
  cursor: move;
  user-select: none;
}

.layout-dialog-title {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.dialog-controls {
  display: flex;
  gap: 8px;
}

.dialog-control-btn {
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

.dialog-control-btn:hover {
  background: #e4e7ed;
  color: #303133;
}

.dialog-control-btn.close-btn:hover {
  background: #f56c6c;
  color: white;
}

.layout-dialog-content {
  flex: 1;
  overflow: hidden;
}

.dialog-main-content {
  display: flex;
  gap: 20px;
  padding: 24px;
  height: calc(100% - 60px);
  align-items: flex-start;
  min-width: 1200px;
  overflow: hidden;
}

.video-player-section {
  display: flex;
  flex-direction: column;
  background: transparent;
  border-radius: 8px;
  overflow: hidden;
  width: 1140px;
  flex: 1;
  min-height: 0;
}

.video-area {
  width: 100%;
  height: 100%;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  flex: 1;
  min-height: 0;
  max-height: none;
}

.video-grid {
  display: grid;
  gap: 8px;
  width: 100%;
  flex: 1;
  min-height: 762px;
  height: calc(100% - 48px);
}

/*  */
.video-grid.layout-1x1 {
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
}

.video-grid.layout-2x2 {
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
}

/* 6 L : main 4 , 5 in and */
.video-grid.layout-3x3 {
  grid-template-columns: 1fr 1fr 1fr;
  grid-template-rows: 1fr 1fr 1fr;
}

.video-grid.layout-3x3 .video-window:nth-child(1) {
  grid-column: 1 / 3;
  grid-row: 1 / 3;
}

.video-grid.layout-3x3 .video-window:nth-child(2) {
  grid-column: 3 / 4;
  grid-row: 1 / 2;
}

.video-grid.layout-3x3 .video-window:nth-child(3) {
  grid-column: 3 / 4;
  grid-row: 2 / 3;
}

.video-grid.layout-3x3 .video-window:nth-child(4) {
  grid-column: 3 / 4;
  grid-row: 3 / 4;
}

.video-grid.layout-3x3 .video-window:nth-child(5) {
  grid-column: 1 / 2;
  grid-row: 3 / 4;
}

.video-grid.layout-3x3 .video-window:nth-child(6) {
  grid-column: 2 / 3;
  grid-row: 3 / 4;
}

/* 8 L : 3×3 main + 4 + 4 , 5 */
.video-grid.layout-4x4 {
  grid-template-columns: 1fr 1fr 1fr 1fr;
  grid-template-rows: 1fr 1fr 1fr 1fr;
}

.video-grid.layout-4x4 .video-window:nth-child(1) {
  grid-column: 1 / 4;
  grid-row: 1 / 4;
}

.video-grid.layout-4x4 .video-window:nth-child(2) {
  grid-column: 4 / 5;
  grid-row: 1 / 2;
}

.video-grid.layout-4x4 .video-window:nth-child(3) {
  grid-column: 4 / 5;
  grid-row: 2 / 3;
}

.video-grid.layout-4x4 .video-window:nth-child(4) {
  grid-column: 4 / 5;
  grid-row: 3 / 4;
}

.video-grid.layout-4x4 .video-window:nth-child(5) {
  grid-column: 4 / 5;
  grid-row: 4 / 5;
}

.video-grid.layout-4x4 .video-window:nth-child(6) {
  grid-column: 1 / 2;
  grid-row: 4 / 5;
}

.video-grid.layout-4x4 .video-window:nth-child(7) {
  grid-column: 2 / 3;
  grid-row: 4 / 5;
}

.video-grid.layout-4x4 .video-window:nth-child(8) {
  grid-column: 3 / 4;
  grid-row: 4 / 5;
}

.video-grid.layout-5x5 {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, 1fr);
}

.video-grid.layout-6x6 {
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(4, 1fr);
}

.video-window {
  background: transparent;
  border: 1px solid #ddd;
  border-radius: 0;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  width: 100%;
  height: 100%;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

.drag-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 10;
  pointer-events: auto;
  background: transparent;
}

/* - each sub */
.video-grid.layout-2x2 .video-window {
  height: 100%;
  width: 100%;
}

/* etc. - , etc. */
.video-grid.layout-4x4 .video-window:not(:first-child),
.video-grid.layout-3x3 .video-window:not(:first-child) {
  height: 100%;
  width: 100%;
}

.video-window.active {
  border: 2px solid #409eff;
}

/*  */
.video-window {
  transition: all 0.3s ease-in-out;
}

.video-window iframe,
.video-window .camera-rtc-container,
.video-window .oplayer-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
  overflow: hidden;
}

.oplayer-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
  overflow: hidden;
}

.camera-rtc-player {
  width: 100%;
  height: 100%;
}

.rtsp-player-container {
  width: 100%;
  height: 100%;
  transition: width 0.3s ease-in-out, height 0.3s ease-in-out;
}

/*  */
.video-window.dragging {
  opacity: 0.8;
  transform: scale(0.95);
  z-index: 1000;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.3);
  border: 2px dashed #1A53FF;
  transition: all 0.2s ease;
  position: relative;
}

.video-window.drag-over {
  border: 2px dashed #67c23a;
  background-color: rgba(103, 194, 58, 0.1);
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
  transition: all 0.2s ease;
  z-index: 1001;
  position: relative;
}

.video-window.drop-target {
  border: 2px dashed #409eff;
  background-color: rgba(64, 158, 255, 0.1);
  transition: all 0.2s ease;
}

.video-window[draggable="true"] {
  cursor: grab;
}

.video-window[draggable="true"]:active {
  cursor: grabbing;
}

/* prompt / tip */
.video-window.dragging::before {
  content: "拖拽到目标位置";
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(26, 83, 255, 0.9);
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  z-index: 1001;
  pointer-events: none;
}

.video-window.drop-target::before {
  content: "释放以交换位置";
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(103, 194, 58, 0.9);
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  z-index: 1001;
  pointer-events: none;
}

/* WebRTC */
.video-grid.layout-1x1 .webrtc-iframe-player {
  min-height: 650px;
  width: 100% !important;
  height: 100% !important;
}

/* can correct */
.video-window iframe,
.video-window .webrtc-iframe-player,
.video-window .rtsp-player-container {
  width: 100%;
  height: 100%;
}

.video-grid.layout-2x2 .webrtc-iframe-player {
  min-height: 300px;
}

.video-grid.layout-3x3 .webrtc-iframe-player {
  min-height: 200px;
}

.video-grid.layout-4x4 .webrtc-iframe-player,
.video-grid.layout-5x5 .webrtc-iframe-player,
.video-grid.layout-6x6 .webrtc-iframe-player {
  min-height: 150px;
}

/* WebRTC iframe ( ) */
.webrtc-iframe {
  border: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
  box-sizing: border-box !important;
  width: 100% !important;
  height: 100% !important;
}

.youtube-iframe-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.youtube-iframe-container iframe {
  width: 100% !important;
  height: 100% !important;
  border: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
  box-sizing: border-box !important;
}

.webrtc-iframe-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.webrtc-iframe-container iframe {
  width: 100% !important;
  height: 100% !important;
  border: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
  box-sizing: border-box !important;
}

/* info */
.video-grid.layout-4x4 .player-info,
.video-grid.layout-5x5 .player-info,
.video-grid.layout-6x6 .player-info {
  font-size: 10px;
}

.video-grid.layout-6x6 .player-overlay {
  padding: 4px;
}

/* WebRTC */
.webrtc-iframe-player {
  background: #000;
  border-radius: 0;
  contain: layout style paint;
}

/* WebRTC iframe Process */
.video-window .webrtc-iframe-player {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

/* WebRTC iframe */
.webrtc-iframe-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.webrtc-iframe-container iframe {
  width: 100%;
  height: 100%;
  border: none;
}

/* YouTube iframe */
.youtube-iframe-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.youtube-iframe-container iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.video-window .video-player-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

/* dialogAdd */
.video-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  position: absolute;
  top: 0;
  left: 0;
}

/* null / empty */
.video-placeholder.empty-placeholder {
  background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 100%);
  border: 2px dashed #404040;
}

.video-placeholder.empty-placeholder .placeholder-icon {
  color: #666666;
  font-size: 36px;
}

.video-placeholder.empty-placeholder .placeholder-text {
  color: #999999;
  font-size: 16px;
}

.video-placeholder.empty-placeholder .placeholder-details {
  color: #666666;
  font-size: 12px;
}

/* WebRTC direct player */
.webrtc-direct-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
  overflow: hidden;
}

.webrtc-direct-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

/* RTSP */
.rtsp-player-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
  overflow: hidden;
}

/* WebRTC iframe */
.webrtc-iframe-player {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  background: #000;
}

.webrtc-iframe {
  border: none;
  background: #000;
  position: absolute;
  top: 0;
  left: 0;
}

.player-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: 8px;
  background: linear-gradient(180deg, rgba(0,0,0,0.8) 0%, transparent 50%);
  color: white;
  pointer-events: none;
  z-index: 2;
}

.player-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.player-title {
  font-weight: 500;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.8);
}

.player-status {
  color: #67c23a;
  font-size: 11px;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.8);
}

/* WebRTC */
.rtsp-webrtc-player {
  width: 100%;
  height: 100%;
}

/* RTSP */
.rtsp-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 100%);
  color: white;
}

.rtsp-fallback .rtsp-info-content {
  text-align: center;
  padding: 20px;
  max-width: 250px;
}

.rtsp-fallback .rtsp-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 8px;
  color: white;
}

.rtsp-fallback .rtsp-note {
  font-size: 12px;
  color: #f56c6c;
  margin-bottom: 8px;
}

/*  */
.video-grid.layout-4x4 .rtsp-info-content,
.video-grid.layout-5x5 .rtsp-info-content,
.video-grid.layout-6x6 .rtsp-info-content {
  padding: 8px;
  max-width: 120px;
}

.video-grid.layout-4x4 .rtsp-title,
.video-grid.layout-5x5 .rtsp-title,
.video-grid.layout-6x6 .rtsp-title {
  font-size: 10px;
  margin-bottom: 4px;
}

.video-grid.layout-4x4 .rtsp-note,
.video-grid.layout-5x5 .rtsp-note,
.video-grid.layout-6x6 .rtsp-note {
  font-size: 8px;
  margin-bottom: 8px;
}

.video-grid.layout-4x4 .rtsp-btn,
.video-grid.layout-5x5 .rtsp-btn,
.video-grid.layout-6x6 .rtsp-btn {
  padding: 2px 6px;
  font-size: 8px;
}

.video-grid.layout-6x6 .rtsp-actions {
  gap: 4px;
}

/* Load */
.video-grid.layout-4x4 .loading-content,
.video-grid.layout-5x5 .loading-content,
.video-grid.layout-6x6 .loading-content {
  padding: 8px;
}

.video-grid.layout-4x4 .loading-title,
.video-grid.layout-5x5 .loading-title,
.video-grid.layout-6x6 .loading-title {
  font-size: 10px;
  margin-bottom: 4px;
}

.video-grid.layout-4x4 .loading-note,
.video-grid.layout-5x5 .loading-note,
.video-grid.layout-6x6 .loading-note {
  font-size: 8px;
}

/* full */
.video-area.fullscreen-active {
  width: 100vw !important;
  height: 100vh !important;
  background: #000 !important;
  padding: 0 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  z-index: 9999 !important;
}

/* full */
.video-area.fullscreen-active .video-grid {
  width: 100vw !important;
  height: 100vh !important;
  gap: 2px !important;
  max-width: 100vw !important;
  max-height: 100vh !important;
}

/* full */
.video-area.fullscreen-active .video-window {
  border: 1px solid #333 !important;
  min-height: auto !important;
  min-width: auto !important;
}

/* control */
.video-controls {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 6px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  margin-bottom: 4px;
  flex-shrink: 0;
  height: 40px;
}

/* PTZcontrol */
.ptz-control-section {
  flex: 0 0 320px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  padding: 0;
  overflow: hidden;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/*  */
.placeholder-content {
  text-align: center;
  padding: 20px;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.placeholder-text {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
  color: #303133;
}

.placeholder-details {
  font-size: 12px;
  color: #606266;
  margin-bottom: 16px;
}

.placeholder-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s ease;
}

.action-btn.primary {
  background: #409eff;
  color: white;
}

.action-btn.primary:hover {
  background: #337ecc;
}

.action-btn.secondary {
  background: #f4f4f5;
  color: #606266;
}

.action-btn.secondary:hover {
  background: #e9e9eb;
}

/* recording */
.recording-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.recording-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #f56c6c;
  font-size: 12px;
  font-weight: 500;
}

.recording-dot {
  width: 8px;
  height: 8px;
  background: #f56c6c;
  border-radius: 50%;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

/* WebRTC and YouTubeinfo */
.webrtc-info,
.youtube-info {
  position: absolute;
  bottom: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 10px;
}
</style>
