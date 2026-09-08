<template>
  <div v-loading="loading" class="vls-device-player">
    <CameraRtcPlayer v-if="cameraConfig" :device-id="cameraConfig.cameraId" :socket-url="cameraConfig.socketUrl" />
    <RtcPlayer v-else-if="rtcUrl" :video-url="rtcUrl" />
    <el-empty v-else-if="!loading" :description="error || '设备没有上报可用视频流'" />
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import CameraRtcPlayer from '@/components/CameraRtcPlayer.vue'
import RtcPlayer from '@/components/rtcPlayer/index.vue'
import { getMqttDeviceStreams, createMqttDevicePreview } from '@/api/vlstreamMqttDevice'
import { parseCameraRtcConfig } from '@/utils/oplayer'

const props = defineProps({ device: { type: Object, required: true } })
const loading = ref(false)
const error = ref('')
const cameraConfig = ref(null)
const rtcUrl = ref('')
let generation = 0
watch(() => props.device.id, async id => {
  const current = ++generation
  cameraConfig.value = null
  rtcUrl.value = ''
  error.value = ''
  loading.value = true
  try {
    const streams = (await getMqttDeviceStreams(id))?.data || []
    if (current !== generation) return
    const preferred = streams.find(stream => stream.defaultStream) || streams[0]
    if (!preferred) return
    const preview = (await createMqttDevicePreview(id, preferred.id))?.data || {}
    if (current !== generation) return
    if (preview.playMode === 'cameraRTC' && preview.url) {
      cameraConfig.value = parseCameraRtcConfig(preview.url)
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
onBeforeUnmount(() => { generation += 1 })
</script>

<style scoped>
.vls-device-player { width: 100%; height: 100%; min-height: 180px; background: #000; }
</style>
