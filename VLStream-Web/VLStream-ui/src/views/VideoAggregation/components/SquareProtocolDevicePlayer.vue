<template>
  <div v-loading="loading" class="square-protocol-player">
    <video v-show="!error && !directUrl" ref="video" autoplay muted playsinline controls @playing="onPlaying" />
    <Hikvision v-if="directRtsp" :rtsp="directRtsp" />
    <CameraRtcPlayer v-if="directCamera" :device-id="directCamera.cameraId" :socket-url="directCamera.socketUrl" />
    <div v-show="directUrl && !directRtsp && !directCamera" ref="directContainer" class="direct-player" />
    <div v-if="error" class="preview-error" role="status">
      <p>{{ $tp(error) }}</p>
      <el-button @click="connect">{{ $tp('重新连接') }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import request from '@/utils/wvpRequest'
import { getCustomDevice } from '@/api/customDevice'
import { startPlay } from '@/api/wvp/push'
import { ZLMRTCClient } from '@/components/rtcPlayer/js/ZLMRTCClient'
import { resolveZlmPlaybackUrl } from '@/utils/zlmPlaybackUrl'
import { ensureOPlayer, createOPlayerOptions, getCustomStreamType } from '@/utils/customPlayer'
import Hikvision from '@/components/Hikvision/index.vue'
import CameraRtcPlayer from '@/components/CameraRtcPlayer.vue'
import { parseCameraRtcConfig } from '@/utils/oplayer'

const props = defineProps({ source: { type: Object, required: true }, deviceName: { type: String, default: '' } })
const video = ref(null), directContainer = ref(null), directUrl = ref(''), directRtsp = ref('')
const directCamera = ref(null)
const loading = ref(false), error = ref('')
let generation = 0, player = null, lease = null, heartbeat = null, connectionTimeout = null
const release = session => session ? request({ url: `/custom/device/${session.deviceId}/preview/${session.id}`, method: 'delete' })
  .catch(() => { /* Expiry retries cleanup without stopping another viewer or recording. */ }) : Promise.resolve()

function disconnect() {
  clearInterval(heartbeat); clearTimeout(connectionTimeout)
  heartbeat = null
  if (player?.close) player.close()
  if (player?.compInstance?.$destroy) player.compInstance.$destroy()
  player = null
  if (video.value) video.value.srcObject = null
  if (directContainer.value) directContainer.value.innerHTML = ''
  directUrl.value = ''; directRtsp.value = ''; directCamera.value = null
  const previous = lease; lease = null
  return release(previous)
}

function onPlaying() { clearTimeout(connectionTimeout); loading.value = false }

async function connect() {
  const current = ++generation, source = props.source
  await disconnect()
  if (current !== generation) return
  loading.value = true; error.value = ''
  try {
    let response
    if (source.protocol === 'CUSTOM') {
      try {
        response = (await request({ url: `/custom/device/${source.rowId}/preview`, method: 'post',
          params: { lease: true }, timeout: 35000 })).data || {}
      } catch (cause) {
        // Older WVP returns only ZLM StreamContent. Direct HTTP sources used the
        // original query-authorized details in device management; keep that guard.
        if ((cause?.response?.data?.msg || cause?.message) !== '仅RTSP/RTMP流需要通过ZLM预览') throw cause
        if (current !== generation) return
        const device = (await getCustomDevice(source.rowId)).data || {}
        const type = getCustomStreamType(device.streamUrl)
        if (!['cameraRTC', 'hls', 'flv', 'video', 'http'].includes(type)) throw cause
        response = { playMode: 'DIRECT', url: device.streamUrl }
      }
      const session = response.sessionId ? { deviceId: source.rowId, id: response.sessionId } : null
      if (current !== generation) { await release(session); return }
      lease = session
    } else {
      response = (await request({ url: `/onvif/device/${source.rowId}/square-playback`, method: 'post' })).data || {}
      if (current !== generation) return
      if (response.playMode === 'PUSH') {
        response = (await startPlay(response.streamId)).data || {}
        if (current !== generation) return
      }
    }
    if (response.playMode === 'DIRECT') {
      if (!response.url) throw new Error('设备未配置可播放地址')
      directUrl.value = response.url
      const streamType = getCustomStreamType(response.url)
      if (streamType === 'cameraRTC') {
        directCamera.value = parseCameraRtcConfig(response.url); loading.value = false
      } else if (streamType === 'rtsp') {
        directRtsp.value = response.url; loading.value = false
      } else {
        await Promise.all([ensureOPlayer(), nextTick()])
        if (current !== generation) return
        const { playerConfig, playConfig } = createOPlayerOptions(response.url)
        player = new window.OToolBox.OPlayer(directContainer.value, playerConfig)
        player.play({ ...playConfig, name: props.deviceName })
        loading.value = false
      }
    } else {
      const info = response.streamInfo || response
      const url = location.protocol === 'https:' ? info.rtcs : info.rtc
      if (!url) throw new Error('WVP 未返回可用播放地址')
      player = new ZLMRTCClient.Endpoint({ element: video.value, debug: false,
        zlmsdpUrl: resolveZlmPlaybackUrl(url), useCamera: false, audioEnable: true, videoEnable: true,
        recvOnly: true, usedatachannel: false })
      const fail = () => {
        if (current !== generation) return
        error.value = 'WebRTC 连接失败'; loading.value = false; disconnect()
      }
      player.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED, fail)
      player.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, fail)
      connectionTimeout = setTimeout(fail, 15000)
    }
    if (lease) {
      const session = lease
      heartbeat = setInterval(async () => {
        try { await request({ url: `/custom/device/${session.deviceId}/preview/${session.id}`, method: 'put' }) }
        catch { if (current === generation) { error.value = '预览会话已结束，请重新播放'; loading.value = false; disconnect() } }
      }, 30000)
    }
  } catch (cause) {
    if (current !== generation) return
    error.value = cause?.response?.data?.msg || cause?.message || '创建预览失败'
    loading.value = false; disconnect()
  }
}

watch(() => [props.source?.protocol, props.source?.rowId, props.source?.playType,
  props.source?.availability?.sourceRevision].join(':'), connect, { immediate: true, flush: 'post' })
onBeforeUnmount(() => { generation += 1; disconnect() })
</script>

<style scoped>
.square-protocol-player { width: 100%; min-height: 240px; background: #000; }
video { display: block; width: 100%; max-height: 65vh; }
.direct-player { width: 100%; min-height: 240px; }
.preview-error { color: white; padding: 40px 16px; text-align: center; }
</style>
