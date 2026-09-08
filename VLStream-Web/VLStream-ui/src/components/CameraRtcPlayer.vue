<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="camera-rtc-player">
    <video ref="videoElement" class="camera-rtc-video" autoplay muted playsinline controls />
    <div v-if="status.state !== 'playing'" class="camera-rtc-status" :class="`is-${status.state}`">
      <span v-if="isPending" class="camera-rtc-spinner" />
      <span class="camera-rtc-message">{{ status.message }}</span>
      <el-button v-if="status.state === 'failed'" type="primary" size="small" @click="retryNow"> 重新连接 </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { CameraRtcSession, parseCameraRtcTurnUrls } from '@/utils/cameraRtcSession'

const props = defineProps({
  deviceId: { type: String, required: true },
  socketUrl: { type: String, required: true },
  disconnectGraceMs: { type: Number, default: 8000 },
  maxRetries: { type: Number, default: 5 },
})

const emit = defineEmits(['status-change', 'diagnostic'])
const videoElement = ref(null)
const status = reactive({
  state: 'connecting',
  message: '正在初始化播放器…',
  retryCount: 0,
})
const isPending = computed(() => ['connecting', 'negotiating', 'recovering', 'retrying'].includes(status.state))
const extraTurnUrls = parseCameraRtcTurnUrls(import.meta.env.VITE_CAMERA_RTC_TURN_URLS)
let session = null

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
onMounted(createSession)
onBeforeUnmount(() => {
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
