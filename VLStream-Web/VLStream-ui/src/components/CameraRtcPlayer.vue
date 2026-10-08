<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div ref="playerElement" class="camera-rtc-player">
    <video ref="videoElement" class="camera-rtc-video" autoplay muted playsinline @play="updateControls" @pause="updateControls" @volumechange="updateControls" />
    <div v-if="status.state !== 'playing'" class="camera-rtc-status" :class="`is-${status.state}`">
      <span v-if="isPending" class="camera-rtc-spinner" />
      <span class="camera-rtc-message">{{ translateUiMessage(status.message) }}</span>
      <el-button v-if="status.state === 'failed'" type="primary" size="small" @click="retryNow"> {{ $tp('重新连接') }} </el-button>
    </div>
    <div v-if="status.state === 'playing'" class="camera-rtc-controls" role="group" :aria-label="$tp('播放控制')">
      <el-button size="small" @click="togglePlayback">{{ $tp(paused ? '播放' : '暂停') }}</el-button>
      <el-button size="small" @click="toggleMute">{{ $tp(muted ? '取消静音' : '静音') }}</el-button>
      <input class="camera-rtc-volume" type="range" min="0" max="1" step="0.05" :value="volume" :aria-label="$tp('音量')" @input="setVolume" />
      <el-button size="small" @click="toggleFullscreen">{{ $tp(fullscreen ? '退出全屏' : '全屏') }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { CameraRtcSession, parseCameraRtcTurnUrls } from '@/utils/cameraRtcSession'
import { translateUiMessage } from '@/i18n'

const props = defineProps({
  deviceId: { type: String, required: true },
  socketUrl: { type: String, required: true },
  disconnectGraceMs: { type: Number, default: 8000 },
  maxRetries: { type: Number, default: -1 },
})

const emit = defineEmits(['status-change', 'diagnostic'])
const videoElement = ref(null)
const playerElement = ref(null)
const paused = ref(true)
const muted = ref(true)
const volume = ref(1)
const fullscreen = ref(false)
const status = reactive({
  state: 'connecting',
  message: '正在初始化播放器…',
  retryCount: 0,
})
const isPending = computed(() => ['connecting', 'negotiating', 'recovering', 'retrying'].includes(status.state))
const extraTurnUrls = parseCameraRtcTurnUrls(import.meta.env.VITE_CAMERA_RTC_TURN_URLS)
let session = null

function updateControls() {
  if (!videoElement.value) return
  paused.value = videoElement.value.paused
  muted.value = videoElement.value.muted
  volume.value = videoElement.value.volume
}

async function togglePlayback() {
  if (!videoElement.value) return
  if (!videoElement.value.paused) videoElement.value.pause()
  else {
    try { await videoElement.value.play() }
    catch (error) { emit('diagnostic', { event: 'playback-control-error', error: error.message }) }
  }
}

function toggleMute() {
  if (videoElement.value) videoElement.value.muted = !videoElement.value.muted
}

function setVolume(event) {
  if (!videoElement.value) return
  videoElement.value.volume = Number(event.target.value)
  videoElement.value.muted = videoElement.value.volume === 0
}

function updateFullscreen() {
  fullscreen.value = document.fullscreenElement === playerElement.value
}

async function toggleFullscreen() {
  try {
    if (document.fullscreenElement === playerElement.value) await document.exitFullscreen()
    else await playerElement.value?.requestFullscreen()
  } catch (error) { emit('diagnostic', { event: 'fullscreen-control-error', error: error.message }) }
}

function createSession() {
  session?.stop()
  if (!videoElement.value) return
  session = new CameraRtcSession({
    videoElement: videoElement.value,
    deviceId: props.deviceId,
    socketUrl: props.socketUrl,
    disconnectGraceMs: props.disconnectGraceMs,
    maxRetries: props.maxRetries,
    extraTurnUrls,
    onStatus(nextStatus) {
      Object.assign(status, nextStatus)
      emit('status-change', nextStatus)
    },
    onDiagnostic(event) {
      emit('diagnostic', event)
    },
  })
  session.start()
}

function retryNow() {
  session?.retryNow()
}

watch(() => [props.deviceId, props.socketUrl], createSession)
onMounted(() => {
  document.addEventListener('fullscreenchange', updateFullscreen)
  createSession()
})
onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', updateFullscreen)
  session?.stop()
  session = null
})
</script>

<style scoped>
.camera-rtc-player {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 320px;
  overflow: hidden;
  background: #000;
}

.camera-rtc-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

.camera-rtc-status {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: #fff;
  background: rgba(0, 0, 0, 0.72);
}

.camera-rtc-controls {
  position: absolute;
  bottom: 0;
  inset-inline: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: rgba(0, 0, 0, 0.72);
}

.camera-rtc-volume {
  width: 90px;
  margin-inline-end: auto;
}

.camera-rtc-message {
  max-width: 80%;
  text-align: center;
}

.camera-rtc-spinner {
  width: 38px;
  height: 38px;
  border: 4px solid rgba(255, 255, 255, 0.25);
  border-top-color: #409eff;
  border-radius: 50%;
  animation: camera-rtc-spin 0.9s linear infinite;
}

.camera-rtc-status.is-failed {
  color: #f8a7a7;
}

@keyframes camera-rtc-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
