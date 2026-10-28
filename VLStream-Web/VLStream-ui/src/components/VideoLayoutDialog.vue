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
            <div ref="videoAreaElement" class="video-area" :class="[`presentation-${presentationMode}`, { 'is-multi-screen': presentationMode !== 'bottom' && dialog.layoutCount > 1 }]" @playing.capture="rememberFrame" @loadeddata.capture="rememberFrame"
              @loadedmetadata.capture="rememberAspect" @resize.capture="rememberAspect" @emptied.capture="refreshMediaState" @volumechange.capture="refreshMediaState">
              <VideoSquareThumbnailStrip v-if="presentationMode === 'default'" :devices="thumbnailDevices" :selected-id="activeCamera?.id || ''"
                :frames="thumbnailFrames" @select="$emit('select-thumbnail', $event)" />
              <!--  -->
              <div ref="workspaceElement" class="video-grid" :class="[`layout-${getLayoutClass(dialog.layoutCount)}`, `variant-${dialog.layoutVariant || 'grid'}`]"
                :style="compactGridStyle"
                @wheel="scrollCarouselWheel">
                <div v-if="presentationMode === 'bottom'" ref="carouselElement" class="window-carousel-scroll"
                  tabindex="0" :aria-label="$tp('底部视频轮播')" @scroll="carouselOffset = $event.target.scrollLeft">
                  <div :style="{ width: carouselWidth + 'px', height: '1px' }"></div>
                </div>
                <div
                  v-for="index in dialog.layoutCount"
                  :key="windowKey(index - 1)"
                  class="video-window"
                  :class="{
                    'active': activeWindowIndex === index - 1,
                    'dragging': draggedWindowIndex === index - 1,
                    'drag-over': dragOverWindowIndex === index - 1,
                    'is-floating': !!floatingWindows[index - 1],
                    'is-moving': movingWindowIndex === index - 1
                  }"
                  :style="windowPlacement(index - 1)"
                  :data-window-index="index - 1"
                  :data-window-id="windowKey(index - 1)"
                  :data-camera-id="dialog.cameras[index - 1]?.id || ''"
                  :data-dialog-id="dialog.id"
                  tabindex="0"
                  :draggable="presentationMode !== 'bottom'"
                  @pointerdown="onWindowPointerDown($event, index - 1)"
                  @click="handleVideoWindowClick(index - 1)"
                  @dblclick="handleVideoWindowDoubleClick(index - 1)"
                  @keydown.enter.self="handleVideoWindowClick(index - 1)"
                  @keydown.space.prevent.self="handleVideoWindowClick(index - 1)"
                  @dragstart="handleDragStart($event, index - 1)"
                  @dragover.prevent="handleDragOver($event, index - 1)"
                  @dragenter.prevent="handleDragEnter($event, index - 1)"
                  @dragleave="handleDragLeave($event, index - 1)"
                  @drop="handleDrop($event, index - 1)"
                  @dragend="handleDragEnd"
                >
                  <div v-if="(dialog.cameras[index - 1] && presentationMode !== 'default') || presentationMode === 'bottom'" class="window-titlebar"
                    @click.stop @dblclick.stop>
                    <span v-if="presentationMode === 'bottom'" data-i18n-ignore :title="dialog.cameras[index - 1]?.name">{{ dialog.cameras[index - 1]?.name || $tp('空白窗口') }}</span>
                    <VideoSquarePlaybackControls v-if="presentationMode !== 'default'" :compact="presentationMode === 'bottom' || dialog.layoutCount > 1"
                      :overlay="presentationMode !== 'bottom' && dialog.layoutCount > 1"
                      :ptz-open="showPtz && activeWindowIndex === index - 1" :muted="windowMedia[index - 1]?.muted ?? true"
                      :can-mute="windowMedia[index - 1]?.canMute" :can-screenshot="windowMedia[index - 1]?.canScreenshot"
                      can-close :fullscreen="playerFullscreen" :busy="capturing"
                      @toggle-mute="runWindowAction(index - 1, toggleSpeaker)" @screenshot="runWindowAction(index - 1, captureActiveFrame)"
                      @toggle-ptz="runWindowAction(index - 1, () => showPtz = !showPtz)"
                      @toggle-fullscreen="runWindowAction(index - 1, () => togglePlayerFullscreen(index - 1))"
                      @close-video="$emit('remove-device', dialog.id, index - 1, windowKey(index - 1))" />
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
                        <div class="placeholder-text">{{ $tp('无信号') }}</div>

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

              <!-- MasterGo 257:28182 / 309:26171: controls follow the picture. -->
              <VideoSquarePlaybackControls v-if="presentationMode === 'default' && dialog.layoutCount > 0" class="default-playback-controls"
                :ptz-open="showPtz" :muted="mediaState.muted" :can-mute="mediaState.canMute"
                :can-screenshot="mediaState.canScreenshot" can-close :fullscreen="playerFullscreen" :busy="capturing"
                @toggle-mute="toggleSpeaker" @screenshot="captureActiveFrame" @toggle-ptz="showPtz = !showPtz"
                @toggle-fullscreen="togglePlayerFullscreen" @close-video="$emit('remove-device', dialog.id, activeWindowIndex, windowKey(activeWindowIndex))" />

              <div v-if="presentationMode === 'bottom'" class="window-carousel-navigation" :style="{ bottom: (carouselSize.height + 28) + 'px' }">
                <button v-if="dialog.layoutCount" type="button" :disabled="carouselOffset <= 0" :aria-label="$tp('上一组视频')" @click="scrollCarousel(-1)">‹</button>
                <button v-if="dialog.layoutCount" type="button" :disabled="carouselOffset >= carouselWidth - workspaceSize.width - 1" :aria-label="$tp('下一组视频')" @click="scrollCarousel(1)">›</button>
                <button type="button" @click="restoreWindowLayout(true)">{{ $tp('恢复布局') }}</button>
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
import { uniformWindowSize, clampVideoWindow, isWindowDragGesture, fitVideoPicture, packVideoWindows } from '@/utils/videoSquareWindowWorkspace.mjs'

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
const showPtz = ref(false)
const activeWindowIndex = ref(0)
const selectedSources = ref({})
const thumbnailFrames = ref({})
const mediaState = ref({muted:true,canMute:false,canScreenshot:false})
const windowMedia = ref({})
const videoAspects = ref({})
const videoInsets = ref({})
const rememberAspect = event => {
  const video = event.target
  const index = Number(video.closest?.('.video-window')?.dataset.windowIndex)
  if (video.tagName === 'VIDEO' && video.videoWidth && video.videoHeight && Number.isInteger(index)) {
    videoAspects.value = {...videoAspects.value, [index]:video.videoWidth / video.videoHeight}
    videoInsets.value = {...videoInsets.value, [index]:video.closest('.video-window').querySelector('.source-selector')?.offsetHeight || 0}
  }
  refreshMediaState()
}
const capturing = ref(false)
const playerFullscreen = ref(false)
const getActiveVideo = () => dialogElement.value?.querySelector(`[data-window-index="${activeWindowIndex.value}"] video`)
const refreshMediaState = () => {
  const video = getActiveVideo()
  mediaState.value = {muted:video?.muted ?? true,canMute:!!video,canScreenshot:canCaptureVideoFrame(video)}
  const states = {}
  dialogElement.value?.querySelectorAll('.video-window').forEach(element => {
    const media = element.querySelector('video')
    states[element.dataset.windowIndex] = {muted:media?.muted ?? true,canMute:!!media,canScreenshot:canCaptureVideoFrame(media)}
  })
  windowMedia.value = states
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
const fullscreenTarget = index => dialogElement.value?.querySelector(`[data-window-index="${Number.isInteger(index) ? index : activeWindowIndex.value}"]`) || dialogElement.value?.querySelector('.video-area')
const fullscreenChanged = () => { playerFullscreen.value=isVideoFullscreen(dialogElement.value?.querySelector('.video-area')) }
const togglePlayerFullscreen = async index => {
  const target = fullscreenTarget(index)
  const context = videoFullscreenContext(target)
  try {
    await toggleVideoFullscreen(target)
    fullscreenChanged()
  } catch(cause) {
    console.warn('视频广场全屏失败',{name:cause?.name,message:cause?.message,...context})
    ElMessage.warning(uiText('当前无法进入全屏'))
  }
}
watch(() => [activeWindowIndex.value, props.dialog.cameras[activeWindowIndex.value]?.id], () => nextTick(refreshMediaState))
watch(() => props.dialog.layoutCount, count => {
  if (activeWindowIndex.value >= count) activeWindowIndex.value = 0
  if (!count) showPtz.value = false
})
onMounted(() => { document.addEventListener('fullscreenchange',fullscreenChanged); nextTick(refreshMediaState) })
onBeforeUnmount(() => document.removeEventListener('fullscreenchange',fullscreenChanged))
const activeCamera = computed(() => props.dialog.cameras[activeWindowIndex.value])
const activeSource = computed(() => {
  const sources = activeCamera.value?.deviceData?.sources || []
  const selected = selectedSources.value[activeWindowIndex.value]
  return sources.find(source => selected && videoSourceKey(source) === videoSourceKey(selected)) || null
})

const workspaceElement = ref(null)
const windowKey = index => props.dialog.windowIds?.[index] || `${props.dialog.id}:legacy:${index}`
const videoAreaElement = ref(null)
const carouselElement = ref(null)
const workspaceSize = ref({width: 0, height: 0})
const availableGridSize = ref({width:0, height:0})
const carouselOffset = ref(0)
const floatingWindows = ref({})
const movingWindowIndex = ref(null)
const windowLayers = ref({})
let frontWindowLayer = 1
let windowDrag = null
let suppressWindowClickUntil = 0
let workspaceObserver
const windowSize = () => uniformWindowSize(workspaceSize.value.width, workspaceSize.value.height)
const carouselSize = computed(() => windowSize())
const dockedWindowOffset = index => windowSize().width * index
const carouselWidth = computed(() => dockedWindowOffset(props.dialog.layoutCount))
// Use the same browser layout-unit edge for both neighbours, avoiding fractional hairline seams.
const layoutEdge = value => Math.round(value * 64) / 64
const packedGrid = computed(() => props.dialog.layoutCount ? packVideoWindows({...availableGridSize.value, count:props.dialog.layoutCount,
  variant:props.dialog.layoutVariant || 'grid', aspects:Array.from({length:props.dialog.layoutCount}, (_, index) => videoAspects.value[index]),
  insets:Array.from({length:props.dialog.layoutCount}, (_, index) => (videoInsets.value[index] || 0) + (props.presentationMode === 'grid' && props.dialog.layoutCount === 1 ? 32 : 0))}) : {width:0,height:0,boxes:[]})
const compactGridStyle = computed(() => {
  if (!props.dialog.layoutCount) return {flex:'0 0 0px',height:'0px',width:'100%'}
  if (props.presentationMode === 'bottom') return undefined
  if (!availableGridSize.value.width) return undefined
  if (props.presentationMode === 'default') {
    const picture = fitVideoPicture(availableGridSize.value.width, availableGridSize.value.height, videoAspects.value[0])
    return {flex:'0 0 auto', height:picture.height + 'px'}
  }
  return {display:'block', flex:'0 0 auto', width:layoutEdge(packedGrid.value.width) + 'px', height:layoutEdge(packedGrid.value.height) + 'px', alignSelf:'center'}
})
const windowPlacement = index => {
  if (props.presentationMode === 'default') {
    const size = fitVideoPicture(workspaceSize.value.width, workspaceSize.value.height, videoAspects.value[index])
    return {width:`${size.width}px`, height:`${size.height}px`, justifySelf:'center', alignSelf:'start'}
  }
  if (props.presentationMode !== 'bottom') {
    const box = packedGrid.value.boxes[index]
    return {position:'absolute', left:`${layoutEdge(box.x)}px`, top:`${layoutEdge(box.y)}px`,
      width:`${layoutEdge(box.x + box.width) - layoutEdge(box.x)}px`, height:`${layoutEdge(box.y + box.height) - layoutEdge(box.y)}px`,
      right:'auto', bottom:'auto', gridArea:'auto', border:'0', margin:'0', padding:'0'}
  }
  const size = windowSize()
  const position = floatingWindows.value[index] || {
    x: dockedWindowOffset(index) - carouselOffset.value,
    y: Math.max(0, workspaceSize.value.height - size.height),
  }
  return {position:'absolute', left:`${position.x}px`, top:`${position.y}px`, width:`${size.width}px`, height:`${size.height}px`,
    right:'auto', bottom:'auto', gridArea:'auto', zIndex:windowLayers.value[index] || 1}
}
const focusVideoWindow = index => {
  if (props.presentationMode === 'bottom') windowLayers.value = {...windowLayers.value, [index]:++frontWindowLayer}
  handleVideoWindowClick(index)
}
const runWindowAction = (index, action) => { focusVideoWindow(index); action() }
const measureWorkspace = () => {
  if (!workspaceElement.value) return
  workspaceSize.value = {width:workspaceElement.value.clientWidth, height:workspaceElement.value.clientHeight}
  const area = videoAreaElement.value
  if (area) {
    const reserved = ['.square-thumbnail-strip', '.default-playback-controls'].reduce((sum, selector) => sum + (area.querySelector(selector)?.offsetHeight || 0), 0)
    availableGridSize.value = {width:area.clientWidth, height:Math.max(1, area.clientHeight - reserved)}
  }
  const positions = {}
  for (const [index, position] of Object.entries(floatingWindows.value)) {
    positions[index] = clampVideoWindow(position, windowSize(), workspaceSize.value)
  }
  floatingWindows.value = positions
}
const stopWindowDrag = () => {
  if (windowDrag?.moved) suppressWindowClickUntil = Date.now() + 250
  if (windowDrag?.handle.hasPointerCapture?.(windowDrag.pointerId)) windowDrag.handle.releasePointerCapture(windowDrag.pointerId)
  windowDrag = null
  movingWindowIndex.value = null
  window.removeEventListener('pointermove', moveWindowDrag)
  window.removeEventListener('pointerup', stopWindowDrag)
  window.removeEventListener('pointercancel', stopWindowDrag)
  window.removeEventListener('blur', stopWindowDrag)
}
const moveWindowDrag = event => {
  if (!windowDrag || event.pointerId !== windowDrag.pointerId) return
  if (!windowDrag.moved && !isWindowDragGesture(windowDrag.start, event)) return
  event.preventDefault()
  windowDrag.moved = true
  movingWindowIndex.value = windowDrag.index
  floatingWindows.value = {...floatingWindows.value, [windowDrag.index]:clampVideoWindow({
    x:windowDrag.origin.x + event.clientX - windowDrag.start.x,
    y:windowDrag.origin.y + event.clientY - windowDrag.start.y,
  }, windowSize(), workspaceSize.value)}
}
// 底部排列下整扇窗口都可拖：标题栏已被紧凑控制栏占满，只在控件/输入等交互元素上不启动拖动
const onWindowPointerDown = (event, index) => {
  focusVideoWindow(index)
  startWindowDrag(event, index)
}
const startWindowDrag = (event, index) => {
  if (props.presentationMode !== 'bottom' || event.button !== 0 || event.target.closest('button, input, select, textarea, a, [contenteditable="true"]')) return
  stopWindowDrag()
  measureWorkspace()
  focusVideoWindow(index)
  const area = workspaceElement.value.getBoundingClientRect()
  const cell = event.currentTarget.getBoundingClientRect()
  windowDrag = {index, pointerId:event.pointerId, handle:event.currentTarget, start:{x:event.clientX, y:event.clientY},
    origin:{x:cell.left - area.left, y:cell.top - area.top}, moved:false}
  event.currentTarget.setPointerCapture(event.pointerId)
  window.addEventListener('pointermove', moveWindowDrag, {passive:false})
  window.addEventListener('pointerup', stopWindowDrag)
  window.addEventListener('pointercancel', stopWindowDrag)
  window.addEventListener('blur', stopWindowDrag)
}
const restoreWindowLayout = (revive = false) => {
  if (revive === true && !props.dialog.layoutCount) emit('restore-windows', props.dialog.id)
  stopWindowDrag()
  floatingWindows.value = {}
  windowLayers.value = {}
  frontWindowLayer = 1
  carouselOffset.value = 0
  if (carouselElement.value) carouselElement.value.scrollLeft = 0
  nextTick(measureWorkspace)
}
// 切换展示方式或窗口数量时清掉拖动留下的自由位置，保证默认按当前布局重新贴底排列
const scrollCarousel = direction => carouselElement.value?.scrollBy({left:direction * Math.max(carouselSize.value.width, workspaceSize.value.width - 32), behavior:'smooth'})
const scrollCarouselWheel = event => {
  if (props.presentationMode !== 'bottom' || !carouselElement.value || carouselWidth.value <= workspaceSize.value.width) return
  const area = workspaceElement.value.getBoundingClientRect()
  if (event.clientY < area.bottom - carouselSize.value.height - 18 || event.target.closest('.is-floating')) return
  event.preventDefault()
  carouselElement.value.scrollLeft += Math.abs(event.deltaX) > Math.abs(event.deltaY) ? event.deltaX : event.deltaY
}
watch(() => [props.presentationMode, props.dialog.layoutRevision], () => restoreWindowLayout())
watch(() => props.dialog.windowIds?.slice(), (ids, oldIds) => {
  if (!ids || !oldIds) return
  stopWindowDrag()
  const reindex = values => Object.fromEntries(ids.flatMap((id, index) => {
    const previous = oldIds.indexOf(id)
    return previous >= 0 && values[previous] !== undefined ? [[index, values[previous]]] : []
  }))
  floatingWindows.value = reindex(floatingWindows.value)
  windowLayers.value = reindex(windowLayers.value)
  videoAspects.value = reindex(videoAspects.value)
  videoInsets.value = reindex(videoInsets.value)
  selectedSources.value = reindex(selectedSources.value)
  nextTick(measureWorkspace)
})
watch(() => props.thumbnailDevices, () => nextTick(measureWorkspace), {deep:true})
const observeWorkspace = () => {
  workspaceObserver?.disconnect()
  if (workspaceElement.value) workspaceObserver?.observe(workspaceElement.value)
  if (videoAreaElement.value) {
    workspaceObserver?.observe(videoAreaElement.value)
    videoAreaElement.value.querySelectorAll('.square-thumbnail-strip, .default-playback-controls').forEach(element => workspaceObserver?.observe(element))
  }
  measureWorkspace()
}
watch(() => props.presentationMode, () => nextTick(observeWorkspace))
watch(() => props.dialog.activeWindowIndex, index => {
  if (Number.isInteger(index) && index < props.dialog.layoutCount) {
    activeWindowIndex.value = index
    if (props.presentationMode === 'bottom' && !floatingWindows.value[index]) {
      carouselElement.value?.scrollTo({left:dockedWindowOffset(index), behavior:'smooth'})
    }
  }
})
onMounted(() => {
  workspaceObserver = new ResizeObserver(measureWorkspace)
  observeWorkspace()
})
onBeforeUnmount(() => { stopWindowDrag(); workspaceObserver?.disconnect() })
watch(videoAspects, () => nextTick(measureWorkspace), {deep:true})

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
  'restore-windows',
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
  if (Date.now() < suppressWindowClickUntil) return
  activeWindowIndex.value = windowIndex
  emit('video-window-click', props.dialog.id, windowIndex)
}

// Process
const handleVideoWindowDoubleClick = (windowIndex) => {
  if (props.presentationMode === 'bottom' || Date.now() < suppressWindowClickUntil) return
  emit('video-window-double-click', props.dialog.id, windowIndex)
}

// start
const handleDragStart = (event, windowIndex) => {
  if (props.presentationMode === 'bottom') { event.preventDefault(); return }
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
.video-area > .video-grid { order: 1; z-index:0; }
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
  gap: 0;
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

/* 单窗口全屏：只有被点击的那个播放窗口铺满屏幕，需覆盖 windowPlacement 的内联定位与尺寸 */
.video-window:fullscreen,
.video-window:-webkit-full-screen {
  left: 0 !important;
  top: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  max-width: none !important;
  max-height: none !important;
  transform: none !important;
  transition: none;
  border: 0;
  border-radius: 0;
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
  gap: 0 !important;
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

/* MasterGo 309:29344: transparent playback controls at the top of each picture. */
.video-area:not(.presentation-bottom) .window-titlebar { display:flex; inset:8px 16px auto; padding:0; background:transparent; cursor:default; pointer-events:none; }
.video-area:not(.presentation-bottom) .window-titlebar :deep(.square-playback-controls) { pointer-events:auto; }
.video-area:not(.presentation-bottom) .video-window:has(.window-titlebar) { padding-top:0; }
.video-area:not(.presentation-bottom) .video-window:has(.window-titlebar) > .no-signal-placeholder { padding-top:56px; box-sizing:border-box; }
.video-area:not(.presentation-bottom) .video-window:has(.window-titlebar) :deep(.protocol-preview-entry),
.video-area:not(.presentation-bottom) .video-window:has(.window-titlebar) :deep(.is-compact-status .preview-error),
.video-area:not(.presentation-bottom) .video-window:has(.window-titlebar) :deep(.is-compact-status .camera-rtc-status),
.video-area:not(.presentation-bottom) .video-window:has(.window-titlebar) :deep(.rtc-no-signal) { top:56px; }

/* Bottom mode uses the same mounted players for docking and free placement. */
.video-area.presentation-bottom > .video-grid { display:block; overflow:hidden; }
.video-area.presentation-bottom .video-window { display:flex; flex-direction:column; padding:0; border:1px solid #2278ff; transition:none; }
.video-area.presentation-bottom .video-window.active { border:1px solid #2278ff; box-shadow:0 0 0 1px #2278ff; }
.video-area.presentation-bottom .video-window.is-floating { box-shadow:0 5px 18px rgb(0 0 0 / 24%); }
.video-area.presentation-bottom .window-titlebar { position:relative; inset:auto; display:flex; flex:0 0 auto; flex-wrap:wrap; min-height:32px; box-sizing:border-box; padding:2px 4px 2px 8px; gap:4px; cursor:grab; touch-action:none; }
.video-area.presentation-bottom .window-titlebar > span { flex:1 1 48px; }
.video-area.presentation-bottom .window-titlebar :deep(.is-compact) { min-width:0; max-width:100%; flex-wrap:wrap; }
.video-area.presentation-bottom .is-moving .window-titlebar { cursor:grabbing; }
.video-area.presentation-bottom .video-window > :deep(.square-device-player),
.video-area.presentation-bottom .video-window > :deep(.vls-device-player),
.video-area.presentation-bottom .video-window > .oplayer-container,
.video-area.presentation-bottom .video-window > .video-placeholder { flex:1 1 0; height:0; min-height:0; }
.window-carousel-scroll { position:absolute; inset:auto 0 0; height:18px; overflow-x:auto; overflow-y:hidden; overscroll-behavior:contain; }
.window-carousel-navigation { order:2; display:flex; justify-content:center; align-items:center; flex-wrap:wrap; flex:0 0 auto; gap:8px; padding:6px 0; }
/* 底部排列：导航浮在底边，网格铺满整个画面区，窗口一排才能贴到最底下 */
.video-area.presentation-bottom .window-carousel-navigation { position:absolute; right:12px; bottom:20px; z-index:13; padding:0; gap:6px; }
.window-carousel-navigation button { border:1px solid #d7e0ec; border-radius:4px; background:white; color:#345; min-width:32px; min-height:28px; cursor:pointer; }
.window-carousel-navigation button:disabled { opacity:.4; cursor:default; }
.window-carousel-navigation button:focus-visible { outline:2px solid #2278ff; }
.video-window :deep(.protocol-preview-entry),
.video-window :deep(.is-compact-status .preview-error),
.video-window :deep(.is-compact-status .camera-rtc-status),
.video-window :deep(.rtc-no-signal) { display:flex; flex-direction:column; align-items:center; justify-content:center; text-align:center; }
.video-window :deep(.protocol-preview-entry p),
.video-window :deep(.is-compact-status .preview-error p) { margin:0; }
.video-area.presentation-bottom .video-window { display:block; padding:0; border:0; background:#111; }
.video-area.presentation-bottom .window-titlebar { position:absolute; inset:0 0 auto; min-height:32px; background:transparent; z-index:20; }
.video-area.presentation-bottom .window-titlebar > span { flex:1 1 28px; max-width:64px; text-shadow:0 1px 3px #000; }
.video-area.presentation-bottom .window-titlebar :deep(.is-compact) { flex:1; }
.video-area.presentation-bottom .video-window > :deep(.square-device-player),
.video-area.presentation-bottom .video-window > :deep(.vls-device-player),
.video-area.presentation-bottom .video-window > .oplayer-container,
.video-area.presentation-bottom .video-window > .video-placeholder { height:100%; }
.video-area.presentation-bottom .video-window :deep(.protocol-preview-entry),
.video-area.presentation-bottom .video-window :deep(.is-compact-status .preview-error) { top:40px; }
.is-embedded { backdrop-filter:none; background:transparent; }
.is-embedded .video-window { border:0; }
.video-area:not(.is-multi-screen) .video-window { display:flex; flex-direction:column; padding:0; }
.video-area:not(.is-multi-screen) .window-titlebar { position:relative; inset:auto; display:flex; flex:0 0 32px; min-height:32px; padding:2px 4px; box-sizing:border-box; background:#2278ff; pointer-events:auto; cursor:grab; }
.video-area:not(.is-multi-screen) .window-titlebar :deep(.square-playback-controls) { flex:1; padding:0; gap:4px; width:auto; margin:0; }
.video-area:not(.is-multi-screen) .window-titlebar :deep(.playback-control) { height:26px; background:transparent; flex:0 0 24px; min-width:24px; }
.video-area:not(.is-multi-screen) .window-titlebar :deep(.close-control) { margin-left:auto; background:#dc362e; }
.video-area:not(.is-multi-screen) .video-window > :deep(.square-device-player),
.video-area:not(.is-multi-screen) .video-window > :deep(.vls-device-player) { flex:1 1 0; height:0; }
.video-area:not(.is-multi-screen) .video-window :deep(.protocol-preview-entry),
.video-area:not(.is-multi-screen) .video-window :deep(.is-compact-status .preview-error) { top:0; }
.video-area.is-multi-screen .window-titlebar { background:rgba(0,0,0,.3); border-radius:4px; }
.video-area .window-titlebar { flex-wrap:nowrap; }
.video-area .window-titlebar :deep(.square-playback-controls) { flex-wrap:nowrap; min-width:0; overflow-x:auto; overflow-y:hidden; scrollbar-width:thin; }
.video-area.is-multi-screen .window-titlebar { inset:4px 4px auto; max-height:44px; }
.video-area.is-multi-screen .video-window :deep(.protocol-preview-entry),
.video-area.is-multi-screen .video-window :deep(.is-compact-status .preview-error),
.video-area.is-multi-screen .video-window :deep(.is-compact-status .camera-rtc-status),
.video-area.is-multi-screen .video-window :deep(.rtc-no-signal) { top:34px; }
.video-area:not(.is-multi-screen) .video-window :deep(.is-compact-status .camera-rtc-status),
.video-area:not(.is-multi-screen) .video-window :deep(.rtc-no-signal) { top:0; }
.video-area > :deep(.default-playback-controls) { order:2; background:#fff; }
.video-area > :deep(.default-playback-controls .playback-control:disabled) { opacity:1; }
.video-area > :deep(.default-playback-controls [data-action="ptz"]) { order:3; }
.video-area > :deep(.default-playback-controls [data-action="ai-hosting"]) { order:4; }
.video-area.presentation-default .video-window:has(.window-titlebar) :deep(.protocol-preview-entry),
.video-area.presentation-default .video-window:has(.window-titlebar) :deep(.is-compact-status .preview-error),
.video-area.presentation-default .video-window:has(.window-titlebar) :deep(.is-compact-status .camera-rtc-status),
.video-area.presentation-default .video-window:has(.window-titlebar) :deep(.rtc-no-signal) { top:0; }
.video-area.presentation-default .video-window:has(.window-titlebar) > .no-signal-placeholder { padding-top:0; }

/* Current selection 309:28524: continuous 356px windows, 56px blue header, 20px card inset. */
.video-area.presentation-bottom > .video-grid { margin:0; width:100%; overflow:visible; }
.video-area.presentation-bottom .video-window,
.video-area.presentation-bottom .video-window.active { border:0; border-radius:0; touch-action:none; user-select:none; cursor:grab; }
.video-area.presentation-bottom .video-window.is-moving { cursor:grabbing; }
.video-area.presentation-bottom .window-titlebar { flex:0 0 56px; min-height:56px; padding:8px 12px 8px 16px; }
.video-area.presentation-bottom .window-titlebar > span { display:none; }
.video-area.presentation-bottom .window-titlebar :deep(.square-playback-controls) { gap:8px; justify-content:space-between; flex-wrap:nowrap; }
.video-area.presentation-bottom .window-titlebar :deep(.playback-control) { flex:0 0 40px; width:40px; height:40px; }
.video-area.presentation-bottom .window-titlebar :deep(.close-control) { margin-left:0; border-radius:50%; }
.video-area.presentation-bottom .window-titlebar :deep([data-action="ai-hosting"]) { order:3; }
.video-area.presentation-bottom .window-titlebar :deep([data-action="ptz"]) { order:4; }
.video-area.presentation-bottom .window-titlebar :deep(svg),
.video-area.presentation-bottom .window-titlebar :deep(.playback-control::before) { width:24px; height:24px; }
.video-area.presentation-bottom .window-carousel-scroll { bottom:0; height:12px; z-index:30; }
.video-window:hover::after, .video-window:focus-visible::after {
  content:'';
  position:absolute;
  inset:0;
  box-sizing:border-box;
  border:2px solid #f5222d;
  border-radius:inherit;
  z-index:30;
  pointer-events:none;
}
/* Empty-slot actions stay reachable even in the small cells of 8/16/25-window layouts. */
.video-area.presentation-default .video-grid { gap:4px; }
.video-area:not(.presentation-default) .video-grid .video-window { border:0; border-radius:0; transition:none; container-type:size; container-name:video-slot; }
.video-area:not(.presentation-default) .video-grid .empty-placeholder { border:0; box-sizing:border-box; }
.video-area:not(.presentation-default) .video-grid .empty-placeholder .placeholder-content { max-height:100%; box-sizing:border-box; padding:4px; }
.empty-placeholder .placeholder-content { display:flex; align-items:center; justify-content:center; width:100%; height:100%; }
.empty-placeholder .placeholder-text { margin:0; }
.empty-placeholder .placeholder-actions { position:absolute; right:8px; bottom:8px; margin:0; }
@container video-slot (max-height:200px) {
  .empty-placeholder .placeholder-icon, .empty-placeholder .placeholder-details { display:none; }
  .empty-placeholder .placeholder-text { margin:0; font-size:12px; }
  .empty-placeholder .action-btn { min-height:28px; white-space:nowrap; }
}
@container video-slot (max-height:70px) {
  .empty-placeholder .placeholder-content { width:100%; height:100%; padding:2px; }
  .empty-placeholder .placeholder-actions { right:2px; bottom:2px; height:24px; }
  .empty-placeholder .action-btn { min-height:0; height:100%; max-height:28px; max-width:100%; padding:0 2px; font-size:10px; }
}
</style>
