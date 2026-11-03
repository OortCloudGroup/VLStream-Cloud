<template>
  <div v-loading="loading" class="vls-device-player" :class="{ 'is-compact-status': compactStatus }">
    <CameraRtcPlayer v-if="cameraConfig" :device-id="cameraConfig.cameraId" :socket-url="cameraConfig.socketUrl" :compact-status="compactStatus" />
    <RtcPlayer v-else-if="rtcUrl" :video-url="rtcUrl" :compact-status="compactStatus" @status-change="handleRtcStatus" />
    <el-empty v-else-if="!loading" :description="compactStatus ? $tp('无信号') : error || $tp('设备没有上报可用视频流')" />
    <div v-if="compactStatus && rtcUrl && rtcStatus === 'failed'" class="rtc-no-signal" role="status">{{ $tp('无信号') }}</div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import CameraRtcPlayer from '@/components/CameraRtcPlayer.vue'
import RtcPlayer from '@/components/rtcPlayer/index.vue'
import { getMqttDeviceStreams, getMqttDeviceDetail, createMqttDevicePreview,
  getMqttSquareStatus, getMqttSquareStreams, createMqttSquarePreview } from '@/api/vlstreamMqttDevice'
import { monitorDevicePreview } from '@/utils/devicePreviewGuard'
import { CAMERA_RTC_SOCKET_URL, parseCameraRtcConfig } from '@/utils/oplayer'
import { squareCameraRtcConfig } from '@/utils/squareCameraRtc.mjs'

const props = defineProps({ device: { type: Object, required: true }, compactStatus: { type: Boolean, default: false }, square: { type: Boolean, default: false } })
const loading = ref(false)
const error = ref('')
const cameraConfig = ref(null)
const rtcUrl = ref('')
const rtcStatus = ref('')
const handleRtcStatus = status => { if (['playing', 'failed'].includes(status?.state)) rtcStatus.value = status.state }
let generation = 0
let stopMonitor = () => {}
watch(() => [props.device.id, props.device.online], async ([id, online]) => {
  stopMonitor()
  const current = ++generation
  cameraConfig.value = null
  rtcUrl.value = ''
  rtcStatus.value = ''
  error.value = ''
  loading.value = true
  try {
    if (online !== true) {
      error.value = '设备离线，不允许播放'
      return
    }
    const getStatus = props.square ? getMqttSquareStatus : getMqttDeviceDetail
    const device = (await getStatus(id))?.data?.device
    if (current !== generation) return
    if (device?.online !== true) {
      error.value = '设备离线，不允许播放'
      return
    }
    stopMonitor = monitorDevicePreview(async () => (await getStatus(id))?.data?.device, () => {
      generation += 1
      cameraConfig.value = null
      rtcUrl.value = ''
      rtcStatus.value = ''
      loading.value = false
      error.value = '设备离线或状态无法确认，已停止播放'
    })
    const streams = (await (props.square ? getMqttSquareStreams(id) : getMqttDeviceStreams(id)))?.data || []
    if (current !== generation) return
    const preferred = streams.find(stream => stream.defaultStream) || streams[0]
    if (!preferred) return
    const preview = (await (props.square ? createMqttSquarePreview(id, preferred.id) : createMqttDevicePreview(id, preferred.id)))?.data || {}
    if (current !== generation) return
    if (preview.playMode === 'cameraRTC' && (preview.cameraId || preview.url)) {
      cameraConfig.value = preview.cameraId ? squareCameraRtcConfig(preview.cameraId, CAMERA_RTC_SOCKET_URL) : parseCameraRtcConfig(preview.url)
    } else {
      rtcUrl.value = location.protocol === 'https:' ? (preview.rtcs || preview.webrtcUrl) : (preview.rtc || preview.webrtcUrl)
      if (!rtcUrl.value) error.value = 'WVP 未返回可用播放地址'
    }
  } catch (cause) {
    if (current === generation) error.value = cause?.response?.data?.msg || cause?.message || '创建预览失败'
  } finally {
    if (current === generation) loading.value = false
  }
}, { immediate: true })
onBeforeUnmount(() => { generation += 1; stopMonitor() })
</script>

<style scoped>
.vls-device-player { width: 100%; height: 100%; min-height: 180px; background: #000; }
.vls-device-player.is-compact-status { position: relative; min-height: 0; }
.is-compact-status :deep(.el-empty) { height: 100%; box-sizing: border-box; }
.is-compact-status :deep(.el-empty__image) { display: none; }
.rtc-no-signal { position: absolute; inset: 0; display: grid; place-items: center; background: #000; color: #fff; pointer-events: none; }
</style>
