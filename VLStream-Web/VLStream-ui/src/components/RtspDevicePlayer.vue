<template>
  <div v-loading="loading" class="rtsp-device-player">
    <video v-show="!error" ref="video" autoplay muted playsinline controls @playing="onPlaying" />
    <div v-if="error" class="preview-error">
      <p>{{ $tp(error) }}</p>
      <el-button @click="connect(deviceId)">{{ $tp('重新连接') }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import request from '@/utils/wvpRequest'
import { ZLMRTCClient } from '@/components/rtcPlayer/js/ZLMRTCClient'
import { resolveZlmPlaybackUrl } from '@/utils/zlmPlaybackUrl'

const props = defineProps({ deviceId: { type: [Number, String], required: true } })
const video = ref(null)
const loading = ref(false)
const error = ref('')
let generation = 0
let player = null
let activeSession = null
let heartbeat = null
let connectionTimeout = null

function release(session) {
  if (!session) return
  return request({ url: `/rtsp/RtspDevice/${session.deviceId}/preview/${session.id}`, method: 'delete' })
    .catch(() => { /* The backend lease retries cleanup if the browser loses connectivity. */ })
}

function disconnect() {
  clearInterval(heartbeat)
  clearTimeout(connectionTimeout)
  heartbeat = null
  if (player) player.close()
  player = null
  if (video.value) video.value.srcObject = null
  const previous = activeSession
  activeSession = null
  return release(previous)
}

function onPlaying() {
  clearTimeout(connectionTimeout)
  loading.value = false
}

async function connect(deviceId) {
  const current = ++generation
  await disconnect()
  if (current !== generation) return
  loading.value = true
  error.value = ''
  try {
    const result = (await request({ url: `/rtsp/RtspDevice/${deviceId}/preview`, method: 'post', timeout: 35000 })).data
    const session = { deviceId, id: result.sessionId }
    if (current !== generation) { await release(session); return }
    activeSession = session
    const info = result.streamInfo || {}
    const url = location.protocol === 'https:' ? info.rtcs : info.rtc
    if (!url) throw new Error('WVP 未返回可用播放地址')
    player = new ZLMRTCClient.Endpoint({
      element: video.value, debug: false, zlmsdpUrl: resolveZlmPlaybackUrl(url),
      useCamera: false, audioEnable: true, videoEnable: true, recvOnly: true, usedatachannel: false
    })
    const fail = () => {
      if (current !== generation) return
      loading.value = false
      error.value = 'WebRTC 连接失败'
      disconnect()
    }
    player.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED, fail)
    player.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, fail)
    connectionTimeout = setTimeout(fail, 15000)
    heartbeat = setInterval(async () => {
      try {
        await request({ url: `/rtsp/RtspDevice/${deviceId}/preview/${session.id}`, method: 'put' })
      } catch (cause) {
        if (current === generation) fail()
      }
    }, 30000)
  } catch (cause) {
    if (current !== generation) return
    loading.value = false
    error.value = cause?.response?.data?.msg || cause?.message || '创建预览失败'
    disconnect()
  }
}

watch(() => props.deviceId, id => connect(id), { immediate: true, flush: 'post' })
onBeforeUnmount(() => { generation += 1; disconnect() })
</script>

<style scoped>
.rtsp-device-player { width: 100%; min-height: 240px; background: #000; }
video { display: block; width: 100%; max-height: 65vh; }
.preview-error { padding: 40px 16px; text-align: center; color: #fff; }
</style>
