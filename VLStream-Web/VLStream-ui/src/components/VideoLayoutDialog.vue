<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div
    class="layout-dialog-overlay"
    :class="{ 'is-embedded': embedded }"
    :style="{ zIndex: embedded ? 'auto' : 3000 + dialog.id }"
  >
    <div
      ref="dialogElement"
      class="layout-dialog draggable-dialog"
      :class="{ 'is-maximized': maximized, 'is-minimized': dialog.minimized }"
      :style="{
        left: maximized ? '0px' : dialog.position.x + 'px',
        top: maximized ? '0px' : dialog.position.y + 'px',
        position: 'fixed',
        transform: 'none'
      }"
      @mousedown="bringToFront"
    >
      <div
        class="layout-dialog-header draggable-handle"
        @mousedown="startDrag"
      >
        <span class="layout-dialog-title">{{ getLayoutTitle(dialog.layoutCount) }} {{ $tp('- 视频播放') }}</span>
        <div class="dialog-controls" @mousedown.stop>
          <button class="dialog-panel-btn" @click="showPtz = !showPtz" :aria-expanded="showPtz">
            {{ showPtz ? $tp('收起PTZ') : $tp('显示PTZ') }}
          </button>
          <button class="dialog-control-btn" @click="maximized = !maximized" :title="maximized ? $tp('还原') : $tp('最大化')">
            <span aria-hidden="true">{{ maximized ? '❐' : '□' }}</span>
          </button>
          <button v-if="!embedded" class="dialog-control-btn minimize-btn" @click="minimize" :title="$tp('最小化')">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 13H5v-2h14v2z"/>
            </svg>
          </button>
          <button class="dialog-control-btn close-btn" @click="close" :title="$tp('关闭')">
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
            <div class="video-area" :class="`presentation-${presentationMode}`" @playing.capture="rememberFrame" @loadeddata.capture="rememberFrame"
              @loadedmetadata.capture="refreshMediaState" @emptied.capture="refreshMediaState" @volumechange.capture="refreshMediaState">
              <VideoSquareThumbnailStrip v-if="presentationMode !== 'grid'" :devices="thumbnailDevices" :selected-id="activeCamera?.id || ''"
                :frames="thumbnailFrames" @select="$emit('select-thumbnail', $event)" />
              <!--  -->
              <div class="video-grid" :class="[`layout-${getLayoutClass(dialog.layoutCount)}`, `variant-${dialog.layoutVariant || 'grid'}`]"
                :style="getLayoutClass(dialog.layoutCount) === 'custom' ? videoGridStyle(dialog.layoutCount, dialog.layoutVariant || 'grid') : undefined">
                <div
                  v-for="index in dialog.layoutCount"
                  :key="`window-${index}`"
                  class="video-window"
                  :class="{
                    'active': activeWindowIndex === index - 1,
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
                  <div v-if="dialog.cameras[index - 1]" class="window-titlebar" @click.stop>
                    <span data-i18n-ignore>{{ dialog.cameras[index - 1].name }}</span>
                    <button :title="$tp('关闭视频')" :aria-label="$tp('关闭视频')" @click.stop="$emit('remove-device', dialog.id, index - 1)">×</button>
                  </div>
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
                    <VideoSquareDevicePlayer v-if="dialog.cameras[index - 1].deviceData?.catalogSource === 'VIDEO_SQUARE'" :key="dialog.cameras[index - 1].id" :device="dialog.cameras[index - 1].deviceData" @source-change="selectedSources[index - 1] = $event" />
                    <VlsDevicePlayer v-else-if="dialog.cameras[index - 1].deviceData?.catalogSource === 'VLSTREAM'" :key="dialog.cameras[index - 1].id" :device="dialog.cameras[index - 1].deviceData" compact-status />
                    <div
                      v-else-if="dialog.cameras[index - 1].deviceData && dialog.cameras[index - 1].deviceData.streamUrl"
                      :ref="el => setOPlayerContainer(index - 1, el)"
                      class="oplayer-container"
                    />

                    <div v-else class="video-placeholder no-signal-placeholder" role="status">
                      <div class="placeholder-text">{{ $tp('无信号') }}</div>
                    </div>
                  </template>

                  <!-- data null / empty -->
                  <template v-else>
                    <div class="video-placeholder empty-placeholder">
                      <div class="placeholder-content">
                        <div class="placeholder-icon">📺</div>
                        <div class="placeholder-text">{{ $tp('空白窗口') }}</div>
                        <div class="placeholder-details">
                          <div>{{ $tp('位置:') }} {{ index }}</div>
                          <div>{{ $tp('状态: 待分配') }}</div>
                        </div>

                        <!-- device item -->
                        <div class="placeholder-actions">
                          <button class="action-btn primary" @click="addDeviceToWindow(index - 1)">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
                            </svg> {{ $tp('添加设备') }} </button>
                        </div>
                      </div>
                    </div>
                  </template>
                </div>
              </div>

              <!-- recordingcontrol -->
              <div v-if="dialog.recording?.isRecording" class="video-controls">
                <!-- recording -->
                <div v-if="dialog.recording?.isRecording" class="recording-status">
                  <div class="recording-indicator">
                    <span class="recording-dot"></span> {{ $tp('录制中') }} {{ dialog.recording.currentRecordTime }}
                  </div>
                </div>
              </div>
              <VideoSquarePlaybackControls :ptz-open="showPtz" :muted="mediaState.muted" :can-mute="mediaState.canMute"
                :can-screenshot="mediaState.canScreenshot" :can-close="!!activeCamera" :fullscreen="playerFullscreen" :busy="capturing"
                @toggle-mute="toggleSpeaker" @screenshot="captureActiveFrame" @toggle-ptz="showPtz = !showPtz"
                @toggle-fullscreen="togglePlayerFullscreen" @close-video="$emit('remove-device', dialog.id, activeWindowIndex)" />
              <VideoSquareFloatingPtz :source="activeSource" :visible="showPtz && active && !dialog.minimized"
                :view-only="activeCamera?.deviceData?.access?.readOnly === true || activeCamera?.deviceData?.access?.canControl === false || activeCamera?.deviceData?.shared === true"
                @close="showPtz = false" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { translatePhrase as uiText } from '@/i18n'

import VlsDevicePlayer from '@/components/VlsDevicePlayer.vue'
import VideoSquareDevicePlayer from '@/views/VideoAggregation/components/VideoSquareDevicePlayer.vue'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import VideoSquareFloatingPtz from '@/views/VideoAggregation/components/VideoSquareFloatingPtz.vue'
import VideoSquareThumbnailStrip from '@/views/VideoAggregation/components/VideoSquareThumbnailStrip.vue'
import VideoSquarePlaybackControls from '@/views/VideoAggregation/components/VideoSquarePlaybackControls.vue'
import { captureThumbnailFrame } from '@/utils/videoSquarePresentation.mjs'
import { canCaptureVideoFrame, captureVideoFrame } from '@/utils/videoSquareCapture.mjs'
import { isVideoFullscreen, toggleVideoFullscreen, videoFullscreenContext } from '@/utils/videoSquareFullscreen.mjs'
import { ElMessage } from 'element-plus'
import { videoSourceKey } from '@/utils/videoSquareCatalog'
import { ensureWebRTCBackendConfig, WEBRTC_SERVER_BASE_URL } from '@/api/webrtc'
import { ensureOPlayer, parseCameraRtcConfig } from '@/utils/oplayer'
import { getStreamType } from '@/views/VideoAggregation/deviceUtils.js'
import { videoGridStyle } from '@/utils/videoSquareLayout.mjs'

// Props
const props = defineProps({
  presentationMode: { type: String, default: 'grid' },
  thumbnailDevices: { type: Array, default: () => [] },
  embedded: Boolean,
  active: { type: Boolean, default: true },
  dialog: {
    type: Object,
    required: true
  }
})

// Dialog viewport state
const dialogElement = ref(null)
const maximized = ref(false)
const showPtz = ref(true)
const activeWindowIndex = ref(0)
const selectedSources = ref({})
const thumbnailFrames = ref({})
const mediaState = ref({muted:true,canMute:false,canScreenshot:false})
const capturing = ref(false)
const playerFullscreen = ref(false)
const getActiveVideo = () => dialogElement.value?.querySelector(`[data-window-index="${activeWindowIndex.value}"] video`)
const refreshMediaState = () => {
  const video = getActiveVideo()
  mediaState.value = {muted:video?.muted ?? true,canMute:!!video,canScreenshot:canCaptureVideoFrame(video)}
}
const rememberFrame = event => {
  refreshMediaState()
  const windowElement = event.target?.closest?.('.video-window')
  const camera = props.dialog.cameras[Number(windowElement?.dataset.windowIndex)]
  if (!camera || event.target?.tagName !== 'VIDEO') return
  if (!event.target.paused) event.target.controls=false
  const frame = captureThumbnailFrame(event.target)
  if (frame) thumbnailFrames.value = {...thumbnailFrames.value, [camera.id]:frame}
}
const toggleSpeaker = async () => {
  const video = getActiveVideo()
  if (!video) return
  video.muted = !video.muted
  if (!video.muted && video.paused) {
    try { await video.play() } catch (cause) { console.warn('视频广场音频播放失败',cause); ElMessage.warning(uiText('当前视频无法播放声音')) }
  }
  if (!video.paused) video.controls=false
  refreshMediaState()
}
const captureActiveFrame = async () => {
  if (capturing.value) return
  capturing.value = true
  try { await captureVideoFrame(getActiveVideo(), {fileName:`video-frame-${Date.now()}.png`}) }
  catch (cause) { console.warn('视频广场截屏失败',cause); ElMessage.warning(uiText('当前视频无法截屏')) }
  finally { capturing.value=false; refreshMediaState() }
}
const fullscreenChanged = () => { playerFullscreen.value=isVideoFullscreen(dialogElement.value?.querySelector('.video-area')) }
const togglePlayerFullscreen = async () => {
  const area = dialogElement.value?.querySelector('.video-area')
  const context = videoFullscreenContext(area)
  try {
    await toggleVideoFullscreen(area)
    fullscreenChanged()
  } catch(cause) {
    console.warn('视频广场全屏失败',{name:cause?.name,message:cause?.message,...context})
    ElMessage.warning(uiText('当前无法进入全屏'))
  }
}
watch(() => [activeWindowIndex.value, props.dialog.cameras[activeWindowIndex.value]?.id], () => nextTick(refreshMediaState))
watch(() => props.dialog.layoutCount, count => { if(activeWindowIndex.value>=count)activeWindowIndex.value=0 })
onMounted(() => { document.addEventListener('fullscreenchange',fullscreenChanged); nextTick(refreshMediaState) })
onBeforeUnmount(() => document.removeEventListener('fullscreenchange',fullscreenChanged))
const activeCamera = computed(() => props.dialog.cameras[activeWindowIndex.value])
const activeSource = computed(() => {
  const sources = activeCamera.value?.deviceData?.sources || []
  const selected = selectedSources.value[activeWindowIndex.value]
  return sources.find(source => selected && videoSourceKey(source) === videoSourceKey(selected)) || null
})

// Clamp after viewport changes and native dialog resizing, without restarting streams.
const fitDialog = () => {
  if (maximized.value || !dialogElement.value) return
  const bounds = dialogElement.value.getBoundingClientRect()
  const position = props.dialog.position
  position.x = Math.max(0, Math.min(position.x, window.innerWidth - bounds.width))
  position.y = Math.max(0, Math.min(position.y, window.innerHeight - bounds.height))
}
let dialogObserver
onMounted(() => {
  dialogObserver = new ResizeObserver(fitDialog)
  dialogObserver.observe(dialogElement.value)
  window.addEventListener('resize', fitDialog)
  fitDialog()
})
watch(maximized, () => nextTick(fitDialog))
onBeforeUnmount(() => {
  dialogObserver?.disconnect()
  window.removeEventListener('resize', fitDialog)
})

// Emits
const emit = defineEmits([
  'select-thumbnail',
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
  'remove-device',
  'video-window-click',
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
    22: 'extended',
    24: 'extended',
    25: 'extended'
  }
  return classes[count] || 'custom'
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
  return titleMap[count] || uiText('{value0}画面', { value0: count })
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
  if (props.embedded || maximized.value) return
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
  activeWindowIndex.value = windowIndex
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
  box-sizing: border-box;
  width: min(1530px, calc(100vw - 32px));
  height: min(900px, calc(100dvh - 32px));
  min-width: min(480px, calc(100vw - 32px));
  min-height: min(280px, calc(100dvh - 32px));
  max-width: calc(100vw - 32px);
  max-height: calc(100dvh - 32px);
  resize: both;
  pointer-events: auto;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.layout-dialog.is-maximized,
.layout-dialog.fullscreen-active {
  width: 100vw !important;
  height: 100dvh !important;
  max-width: 100vw !important;
  max-height: 100dvh !important;
  resize: none;
  border-radius: 0;
}

.layout-dialog.is-minimized {
  height: auto !important;
  min-height: 0 !important;
  resize: none;
}

.dialog-panel-btn {
  border: 0;
  background: transparent;
  color: #606266;
  cursor: pointer;
  white-space: nowrap;
}

.layout-dialog-header {
  flex: 0 0 auto;
  gap: 8px;
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
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.dialog-main-content {
  display: flex;
  gap: 12px;
  padding: 12px;
  box-sizing: border-box;
  height: 100%;
  align-items: stretch;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.video-player-section {
  display: flex;
  flex-direction: column;
  background: transparent;
  border-radius: 8px;
  overflow: hidden;
  min-width: 0;
  flex: 1;
  min-height: 0;
}

.video-area {
  position: relative;
  width: 100%;
  height: 100%;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  flex: 1;
  min-height: 0;
  max-height: none;
}
.video-area > .square-thumbnail-strip { order: 0; }
.video-area > .video-grid { order: 1; }
.video-area > .video-controls { order: 2; }
.video-area > :deep(.square-playback-controls) { order: 2; }
.video-area.presentation-bottom > .square-thumbnail-strip { order: 3; padding: 12px 2px 4px; }
.video-area.presentation-bottom > .square-thumbnail-strip :deep(.square-thumbnail) { flex-basis: clamp(140px, 20vw, 280px); width: clamp(140px, 20vw, 280px); aspect-ratio: 16 / 9; }
.video-area.presentation-default .window-titlebar,
.video-area.presentation-bottom .window-titlebar { display: none; }
.video-area.presentation-default .video-window,
.video-area.presentation-bottom .video-window { border-color: transparent; }
.video-area:fullscreen { width: 100vw; height: 100vh; padding: 12px; border-radius: 0; background: #111; }

.video-grid {
  position: relative;
  display: grid;
  gap: 4px;
  width: 100%;
  flex: 1;
  min-width: 0;
  min-height: 0;
}

/*  */
.video-grid.layout-1x1 {
  grid-template-columns: minmax(0, 1fr);
  grid-template-rows: minmax(0, 1fr);
}

.video-grid.layout-2x2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(0, 1fr));
}

/* 6 L : main 4 , 5 in and */
.video-grid.layout-3x3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-template-rows: repeat(3, minmax(0, 1fr));
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  grid-template-rows: repeat(4, minmax(0, 1fr));
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
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-template-rows: repeat(3, minmax(0, 1fr));
}

.video-grid.layout-6x6 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  grid-template-rows: repeat(4, minmax(0, 1fr));
}

.video-grid.layout-extended {
  grid-template-columns: repeat(5, minmax(0, 1fr));
  grid-template-rows: repeat(5, minmax(0, 1fr));
}

.video-window {
  min-width: 0;
  min-height: 0;
  box-sizing: border-box;
  background: #000;
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
  pointer-events: none;
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

.video-placeholder.no-signal-placeholder { background: #111; }
.video-placeholder.no-signal-placeholder .placeholder-text { margin: 0; color: #cbd5e1; }

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

/* Every player gets the cell's available height, including the source selector.
   Native media then contains the complete frame with centered letterboxing. */
.video-window :deep(.square-device-player),
.video-window :deep(.vls-device-player),
.video-window :deep(.rtsp-device-player),
.video-window :deep(.square-protocol-player),
.video-window :deep(.camera-rtc-player),
.video-window :deep(.direct-player),
.video-window :deep(#rtcPlayer),
.video-window :deep(.rtsp-player-container),
.video-window :deep(.video-container:not(.fullscreen)),
.video-window :deep(.o-player),
.video-window :deep(.o-player-item) {
  position: relative;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  max-height: none;
  box-sizing: border-box;
}

/* RtspPlayer receives legacy pixel props; constrain its outer box to the cell.
   Its inner fullscreen container keeps its own viewport sizing. */
.video-window :deep(.rtsp-player-container) {
  width: 100% !important;
  height: 100% !important;
}
.video-window :deep(#webRtcPlayerBox) { max-height: none; }

.video-window :deep(.square-device-player) { overflow: hidden; }
.video-window :deep(.source-selector) { flex: 0 0 auto; }
.video-window :deep(.square-device-player > .vls-device-player),
.video-window :deep(.square-device-player > .rtsp-device-player),
.video-window :deep(.square-device-player > .square-protocol-player) {
  height: 0;
  flex: 1 1 0;
}

.video-window :deep(video),
.video-window :deep(canvas),
.video-window :deep(iframe) {
  display: block;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  max-height: none;
  object-fit: contain;
  object-position: center;
  background: #000;
}

.video-window :deep(.preview-error),
.video-window :deep(.camera-rtc-status),
.video-window :deep(.protocol-preview-entry) {
  position: absolute;
  inset: 0;
  margin: 0;
  padding: 8px;
  overflow: auto;
  box-sizing: border-box;
  font-size: 12px;
}

.video-window :deep(.camera-rtc-status) { justify-content: safe center; }
.video-window :deep(.camera-rtc-status > *) { flex-shrink: 0; }
.video-window :deep(.camera-rtc-controls) { flex-wrap: wrap; }
.video-window .video-placeholder {
  overflow: auto;
  align-items: safe center;
  justify-content: safe center;
}

@media (max-width: 900px) {
  .dialog-main-content { gap: 6px; padding: 6px; }
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

<style scoped>
.layout-dialog-overlay.is-embedded { position: relative; inset: auto; width: 100%; height: 100%; min-height: 0; }
.is-embedded .layout-dialog:not(.is-maximized):not(.fullscreen-active) { position: relative !important; inset: auto !important; width: 100%; height: 100%; min-width: 0; min-height: 0; max-width: none; max-height: none; border: 0; border-radius: 0; box-shadow: none; resize: none; }
.is-embedded .layout-dialog-header { display: none; }
.is-embedded .layout-dialog-header { background: #fff; cursor: default; }
.is-embedded .dialog-main-content { padding: 0; gap: 0; }
.is-embedded .video-area { box-shadow: none; border-radius: 0; }
</style>

<style scoped>
.window-titlebar { position: absolute; inset: 0 0 auto; z-index: 12; display: flex; align-items: center; justify-content: space-between; padding: 4px 8px; gap: 8px; background: #2278ff; color: #fff; font-size: 12px; }
.window-titlebar span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.window-titlebar button { flex: none; background: transparent; color: #fff; border: 0; cursor: pointer; font-size: 20px; line-height: 20px; }
.video-window:has(.window-titlebar) { padding-top: 28px; }
</style>

<style scoped>
.video-grid.variant-two-overlay .video-window:nth-child(2) { position: absolute; width: 30%; height: 30%; right: 12px; bottom: 12px; z-index: 15; border: 2px solid #2278ff; }
.video-grid.variant-three-top .video-window:first-child { grid-column: span 2; }
.video-grid.variant-three-left .video-window:first-child { grid-row: span 2; }
</style>
