<template>
  <section v-loading="loading || leaseState.status === 'loading'" class="shared-video-player">
    <el-select v-model="streamId" :placeholder="$tp('选择视频流')" :disabled="loading" @change="start">
      <el-option v-for="stream in streams" :key="stream.id" :value="stream.id" :label="stream.streamName || stream.channelId || stream.id" />
    </el-select>
    <el-alert v-if="error || leaseState.error" :title="error || leaseState.error" type="error" :closable="false" />
    <el-button v-if="error || leaseState.error" :disabled="!streamId || loading" @click="start">{{ $tp('重新连接') }}</el-button>
    <p v-if="mediaStatus && !error && !leaseState.error" role="status">{{ $tp(mediaStatus) }}</p>
    <video v-if="leaseState.status === 'active'" ref="video" controls autoplay playsinline :aria-label="$tp('共享视频播放')" />
    <el-empty v-else-if="!loading && !error && !leaseState.error && !streams.length" :description="$tp('暂无可用视频流')" />
    <p>{{ $tp('仅可查看视频，无设备控制权限') }}</p>
  </section>
</template>

<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { deviceSharingApi } from '@/api/videoDeviceSharing'
import { createSharedVideoLease } from '@/utils/sharedVideoLease.mjs'
import { createSharedCameraRtcSession } from '@/utils/sharedCameraRtc.mjs'
import { CameraRtcSession } from '@/utils/cameraRtcSession'
import { getWvpBaseURL, getSharedCameraRtcAuthHeaders } from '@/utils/wvpRequest'
import { ZLMRTCClient } from '@/components/rtcPlayer/js/ZLMRTCClient'
const props = defineProps({ deviceRef: { type: String, required: true }, api: { type: Object, default: () => deviceSharingApi } })
const streams = ref([]), streamId = ref(''), loading = ref(false), error = ref(''), video = ref(null)
const leaseState = ref({ status: 'idle', error: '', lease: null })
const mediaStatus = ref('')
const session = createSharedVideoLease({ api: props.api, allowCameraRtc: import.meta.env.VITE_SHARED_CAMERA_RTC_READY === 'true', onChange: value => { leaseState.value = value } })
let generation = 0, endpoint = null, cameraSession = null
const stopMedia = () => { cameraSession?.stop(); cameraSession=null; endpoint?.close(); endpoint = null; mediaStatus.value=''; if(video.value){video.value.pause();video.value.srcObject=null} }
watch(leaseState, async state => {
  stopMedia()
  if (state.status !== 'active') return
  await nextTick()
  if (leaseState.value !== state || !video.value) return
  try {
    if(state.lease.playMode==='cameraRTC') {
      cameraSession=createSharedCameraRtcSession({CameraRtcSession,lease:state.lease,wvpBase:getWvpBaseURL(),origin:location.origin,videoElement:video.value,
        getAuthHeaders:getSharedCameraRtcAuthHeaders,
        onStatus:status=>{
          if(leaseState.value.lease?.sessionId!==state.lease.sessionId)return
          mediaStatus.value=status.state==='playing'?'':status.message
          if(status.state==='failed'){error.value=status.message;session.stop()}
        }})
      cameraSession.start()
      return
    }
    const candidate = location.protocol === 'https:' ? state.lease.streamInfo?.rtcs : state.lease.streamInfo?.rtc || state.lease.streamInfo?.rtcs
    const url = new URL(candidate)
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || (location.protocol === 'https:' && url.protocol !== 'https:')) throw new Error('服务未返回可用的安全共享播放地址')
    endpoint = new ZLMRTCClient.Endpoint({ element: video.value, zlmsdpUrl: url.href, debug: false,
      useCamera: false, audioEnable: true, videoEnable: true, recvOnly: true, usedatachannel: false })
    const failed = () => { if(leaseState.value.lease?.sessionId!==state.lease.sessionId)return; error.value = '共享视频连接失败，请重新选择视频流'; session.stop() }
    endpoint.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, failed)
    endpoint.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED, failed)
  } catch (cause) { error.value = cause.message; session.stop() }
})
const start = () => { error.value = ''; if (streamId.value) return session.start(props.deviceRef, streamId.value) }
watch(() => props.deviceRef, async ref => {
  const current = ++generation
  session.stop(); streams.value = []; streamId.value = ''; error.value = ''; loading.value = true
  try {
    const response = (await props.api.streams(ref))?.data
    if (current !== generation) return
    if (!Array.isArray(response)) throw new Error('共享视频流响应无效')
    streams.value = response
    streamId.value = (response.find(stream => stream.defaultStream) || response[0])?.id || ''
    if (streamId.value) await start()
  } catch (cause) { if (current === generation) error.value = cause?.response?.data?.msg || cause.message || '读取共享视频失败' }
  finally { if (current === generation) loading.value = false }
}, { immediate: true })
onBeforeUnmount(() => { generation++; session.stop(); stopMedia() })
</script>

<style scoped>
.shared-video-player { display: flex; flex-direction: column; gap: 12px; min-height: 220px; }
.shared-video-player video { width: 100%; max-height: 65vh; background: #111; }
.shared-video-player p { color: #778da2; margin: 0; }
</style>
