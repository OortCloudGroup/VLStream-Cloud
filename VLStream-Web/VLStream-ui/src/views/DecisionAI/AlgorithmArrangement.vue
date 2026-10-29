<template>
  <DeviceClassificationLayout protocol-type="VLSTREAM" readonly @filter-change="classificationFilter" :selected-device-keys="[]">
    <div class="algorithm-arrangement tenant_Page draHeaPB">
      <div class="tenant_content">
        <div class="arrangement-toolbar">
          <div><strong>{{ $tp('算法配置') }}</strong><span>{{ $tp('选择摄像机，统一部署算法模型') }}</span></div>
          <el-button type="primary" :disabled="!selectedDevices.length || selectedDevices.length > 20" @click="configure(selectedDevices)">{{ $tp('批量配置') }} ({{ selectedDevices.length }}/20)</el-button>
        </div>
        <el-alert :title="$tp('选择设备后，使用设备详情相同的模型选择页。选中的每个模型均下发到每台设备，逐项校验权限及在线状态，最终结果以设备回执为准。')" type="info" :closable="false" show-icon />
        <div class="arrangement-filters">
          <el-input v-model="query.keyword" clearable :placeholder="$tp('设备名称 / ID / 序列号')" @keyup.enter="search" />
          <el-button @click="search">{{ $tp('搜索') }}</el-button>
          <el-button @click="reset">{{ $tp('重置') }}</el-button>
          <span>{{ $tp('翻页或筛选会清空本页选择') }}</span>
        </div>
        <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
        <el-table ref="deviceTable" v-loading="loading" :data="devices" row-key="deviceId" stripe class="new_table" @selection-change="selectedDevices = $event">
          <el-table-column type="selection" width="48" />
          <el-table-column type="index" :label="$tp('序号')" width="60" />
          <el-table-column prop="deviceName" :label="$tp('设备名称')" min-width="150" show-overflow-tooltip />
          <el-table-column prop="deviceId" :label="$tp('设备ID')" min-width="180" show-overflow-tooltip />
          <el-table-column prop="deviceModel" :label="$tp('设备型号')" min-width="140" />
          <el-table-column :label="$tp('在线状态')" width="100"><template #default="{ row }"><el-tag :type="row.online ? 'success' : 'info'">{{ $tp(row.online ? '在线' : '离线') }}</el-tag></template></el-table-column>
          <el-table-column :label="$tp('设备上报模型')" min-width="170" show-overflow-tooltip><template #default="{ row }">{{ reportedModels(row) }}</template></el-table-column>
          <el-table-column :label="$tp('操作')" width="160" fixed="right"><template #default="{ row }">
            <el-button link type="primary" :disabled="!row.online" @click="playDevice(row)">{{ $tp('播放') }}</el-button>
            <el-button link type="primary" @click="configure([row])">{{ $tp('配置') }}</el-button>
          </template></el-table-column>
        </el-table>
        <div class="paginationBox"><el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :page-sizes="[10,20,50]" :total="total" layout="total, prev, pager, next, sizes" @current-change="loadDevices" @size-change="search" /></div>
      </div>
    </div>
    <DeviceModelDrawer v-model="showDeployment" :devices="targets" />
    <el-dialog
      v-model="videoDialogVisible"
      :title="$tp('视频播放')"
      width="65%"
      top="5vh"
      @close="handleVideoClose"
    >
      <div class="video-dialog-content">
        <div v-if="!currentStreamUrl" class="video-empty">{{ $tp('未配置视频流地址') }}</div>

        <iframe
          v-else-if="currentStreamType === 'youtube'"
          class="video-iframe"
          :src="youtubeEmbedUrl"
          frameborder="0"
          allow="autoplay; encrypted-media"
          allowfullscreen
        />

        <div
          v-else-if="currentStreamType === 'cameraRTC'"
          ref="cameraRtcContainer"
          class="video-element camera-rtc-player"
        ></div>

        <video
          v-else-if="currentStreamType === 'hls'"
          ref="hlsVideoPlayer"
          class="video-element"
          controls
          autoplay
          muted
          playsinline
        />

        <video
          v-else-if="currentStreamType === 'rtsp'"
          id="webrtcDirectPlayer"
          ref="webrtcVideoPlayer"
          class="video-element"
          controls
          autoplay
          muted
          playsinline
        />

        <video
          v-else
          ref="nativeVideoPlayer"
          class="video-element"
          :src="currentStreamUrl"
          controls
          autoplay
          muted
          playsinline
        />
      </div>
    </el-dialog>


  </DeviceClassificationLayout>
</template>
<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import Hls from 'hls.js'
import { translatePhrase as uiText } from '@/i18n'
import DeviceClassificationLayout from '@/components/DeviceClassificationLayout/index.vue'
import DeviceModelDrawer from '@/views/VideoAggregation/components/DeviceModelDrawer.vue'
import { getMqttDevicePage, getMqttDeviceStreams } from '@/api/vlstreamMqttDevice'
import { captureComputeIdentity } from '@/utils/computeIdentityRequest'
import { ensureWebRTCBackendConfig, WEBRTC_SERVER_BASE_URL } from '@/api/webrtc'
import { getStreamType, getYouTubeEmbedUrl } from '@/views/VideoAggregation/deviceUtils.js'
import { CAMERA_RTC_SOCKET_URL, ensureOPlayer } from '@/utils/oplayer'

const query = ref({ pageNum: 1, pageSize: 10, keyword: '', protocolType: 'VLSTREAM' })
const devices = ref([]), selectedDevices = ref([]), total = ref(0), loading = ref(false), loadError = ref('')
const deviceTable = ref(null), showDeployment = ref(false), targets = ref([])
let listSequence = 0, identityTimer
let identity = captureComputeIdentity()
const errorMessage = ex => ex?.data?.msg || ex?.response?.data?.msg || ex?.message || uiText('设备服务不可用')
function identityCurrent() { try { identity.assertCurrent(); return true } catch { return false } }
function reportedModels(row) {
  try {
    const models = JSON.parse(row.modelsJson || 'null')
    return Array.isArray(models) ? models.map(model => model.modelName || model.modelId).join('、') || uiText('无模型') : uiText('未上报')
  } catch { return uiText('未上报') }
}
async function loadDevices() {
  const sequence = ++listSequence
  const requestIdentity = captureComputeIdentity()
  selectedDevices.value = []; deviceTable.value?.clearSelection()
  loading.value = true; loadError.value = ''
  try {
    const result = await getMqttDevicePage({ ...query.value })
    requestIdentity.assertCurrent()
    if (sequence !== listSequence) return
    if (!Array.isArray(result?.rows)) throw new Error('设备列表格式不正确')
    devices.value = result.rows; total.value = Number(result.total || 0)
  } catch (ex) { if (sequence === listSequence && identityCurrent()) { devices.value = []; total.value = 0; loadError.value = errorMessage(ex) } }
  finally { if (sequence === listSequence) loading.value = false }
}
function search() { query.value.pageNum = 1; loadDevices() }
function reset() { query.value = { pageNum: 1, pageSize: 10, keyword: '', protocolType: 'VLSTREAM' }; loadDevices() }
function classificationFilter(filter) { Object.assign(query.value, filter, { pageNum: 1 }); loadDevices() }
function configure(rows) { if (!identityCurrent() || !rows.length || rows.length > 20) return; targets.value = rows.map(row => ({ ...row })); showDeployment.value = true }
async function playDevice(device) {
  const requestIdentity = captureComputeIdentity()
  try {
    const result = await getMqttDeviceStreams(device.id)
    requestIdentity.assertCurrent()
    const stream = (result?.data || []).find(item => item.available && item.sourceUrl)
    if (!stream) throw new Error('设备未上报可用视频源')
    await handlePlay({ ...device, streamUrl: stream.sourceUrl })
  } catch (ex) { if (identityCurrent()) ElMessage.error(errorMessage(ex)) }
}
const videoDialogVisible = ref(false)
const currentVideoDevice = ref(null)
const youtubeEmbedUrl = ref('')
const webrtcVideoPlayer = ref(null)
const hlsVideoPlayer = ref(null)
const nativeVideoPlayer = ref(null)
const hlsInstance = ref(null)
const webrtcPlayer = ref(null)
const cameraRtcContainer = ref(null)
const cameraRtcPlayer = shallowRef(null)
const webrtcPlaySeq = ref(0)

const getDeviceStreamUrl = (device) => {
  return (
    device?.originalRtspUrl ||
    device?.streamUrl ||
    device?.rtspUrl ||
    device?.rtsp ||
    device?.url ||
    ''
  )
}

const currentStreamUrl = computed(() => getDeviceStreamUrl(currentVideoDevice.value))
const currentStreamType = computed(() => getStreamType(currentStreamUrl.value))

// WebRTC ( RTSP )
let WEBRTC_STREAMER_BASE = WEBRTC_SERVER_BASE_URL
// configuration after Generate , Initialize value .
const getWebRtcScriptUrls = () => [
  `${WEBRTC_STREAMER_BASE}/libs/adapter.min.js`,
  `${WEBRTC_STREAMER_BASE}/webrtcstreamer.js`
]
let webrtcScriptLoader = null

const loadScriptTag = (src) => {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${src}"]`)) {
      resolve()
      return
    }

    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`Failed to load ${src}`))
    document.head.appendChild(script)
  })
}

const ensureWebRtcStreamerScripts = async () => {
  if (webrtcScriptLoader) return webrtcScriptLoader
  await ensureWebRTCBackendConfig()
  WEBRTC_STREAMER_BASE = WEBRTC_SERVER_BASE_URL
  webrtcScriptLoader = Promise.all(getWebRtcScriptUrls().map(loadScriptTag)).catch((error) => {
    webrtcScriptLoader = null
    throw error
  })
  return webrtcScriptLoader
}

const cleanupWebRTCStream = async () => {
  if (webrtcVideoPlayer.value) {
    webrtcVideoPlayer.value.srcObject = null
    webrtcVideoPlayer.value.onerror = null
    webrtcVideoPlayer.value.onloadeddata = null
  }

  if (webrtcPlayer.value) {
    try {
      if (typeof webrtcPlayer.value.disconnect === 'function') {
        await webrtcPlayer.value.disconnect()
      } else if (typeof webrtcPlayer.value.stop === 'function') {
        await webrtcPlayer.value.stop()
      }
    } catch (error) {
      console.error('Failed to stop WebRTC stream:', error)
    } finally {
      webrtcPlayer.value = null
    }
  }
}

const cleanupHLSStream = () => {
  if (hlsInstance.value) {
    try {
      hlsInstance.value.destroy()
    } catch (error) {
      console.warn('Failed to destroy HLS instance:', error)
    } finally {
      hlsInstance.value = null
    }
  }
  if (hlsVideoPlayer.value) {
    hlsVideoPlayer.value.pause?.()
    hlsVideoPlayer.value.removeAttribute?.('src')
    hlsVideoPlayer.value.load?.()
  }
}

const cleanupNativeStream = () => {
  if (!nativeVideoPlayer.value) return
  nativeVideoPlayer.value.pause?.()
  nativeVideoPlayer.value.removeAttribute?.('src')
  nativeVideoPlayer.value.load?.()
}

const cleanupCameraRTCPlayer = () => {
  if (cameraRtcPlayer.value?.compInstance?.$destroy) {
    cameraRtcPlayer.value.compInstance.$destroy()
  }
  cameraRtcPlayer.value = null

  if (cameraRtcContainer.value) {
    cameraRtcContainer.value.innerHTML = ''
  }
}

const stopPlayback = async () => {
  cleanupHLSStream()
  cleanupNativeStream()
  cleanupCameraRTCPlayer()
  await cleanupWebRTCStream()
}

const playWithWebRTC = async (rtspUrl) => {
  await ensureWebRtcStreamerScripts()
  if (!window.WebRtcStreamer) {
    throw new Error('WebRtcStreamer library not available')
  }

  await nextTick()
  const videoEl = webrtcVideoPlayer.value
  if (!videoEl) {
    throw new Error('Video element not ready')
  }

  await cleanupWebRTCStream()
  webrtcPlayer.value = new window.WebRtcStreamer(videoEl.id, WEBRTC_STREAMER_BASE)
  if (typeof webrtcPlayer.value.connect !== 'function') {
    throw new Error('WebRtcStreamer.connect is not available')
  }
  webrtcPlayer.value.connect(rtspUrl, '', 'rtptransport=tcp&timeout=60')
}

const playWithCameraRTC = async (device) => {
  await ensureOPlayer()
  await nextTick()

  const container = cameraRtcContainer.value
  if (!container) {
    throw new Error('CameraRTC container not ready')
  }

  const deviceId = device?.deviceId || device?.id
  if (!deviceId) {
    throw new Error('CameraRTC requires deviceId')
  }

  cleanupCameraRTCPlayer()
  cameraRtcPlayer.value = new window.OToolBox.OPlayer(container, {
    debuggerMode: false,
    autoSize: true,
    backgroundColor: '#000000',
    showHeader: true,
    webRTCSocketURL: CAMERA_RTC_SOCKET_URL
  })
  cameraRtcPlayer.value.play({
    type: 'cameraRTC',
    src: String(deviceId),
    name: device?.deviceName || device?.name || ''
  })
}

const initHlsPlayer = async (streamUrl) => {
  await nextTick()
  const videoEl = hlsVideoPlayer.value
  if (!videoEl) return

  cleanupHLSStream()

  if (Hls.isSupported()) {
    const hls = new Hls({
      lowLatencyMode: true,
      enableWorker: true
    })
    hls.loadSource(streamUrl)
    hls.attachMedia(videoEl)
    hlsInstance.value = hls
    videoEl.play?.().catch(() => {})
    return
  }

  if (videoEl.canPlayType('application/vnd.apple.mpegurl')) {
    videoEl.src = streamUrl
    videoEl.play?.().catch(() => {})
    return
  }

  throw new Error('HLS not supported in this browser')
}

const playNative = async () => {
  await nextTick()
  nativeVideoPlayer.value?.play?.().catch(() => {})
}

const handlePlay = async (row) => {
  const deviceForPlay = { ...(row || {}) }
  const streamUrl = getDeviceStreamUrl(deviceForPlay)
  if (!streamUrl || !String(streamUrl).trim()) {
    ElMessage.warning(uiText('未配置视频流地址'))
    return
  }

  await stopPlayback()

  currentVideoDevice.value = deviceForPlay
  youtubeEmbedUrl.value = ''
  videoDialogVisible.value = true

  const seq = ++webrtcPlaySeq.value
  await nextTick()
  if (seq !== webrtcPlaySeq.value) return

  try {
    const type = getStreamType(streamUrl)
    if (type === 'youtube') {
      const embedUrl = getYouTubeEmbedUrl(streamUrl)
      if (!embedUrl) {
        throw new Error('Invalid YouTube URL')
      }
      youtubeEmbedUrl.value = embedUrl
      return
    }

    if (type === 'cameraRTC') {
      await playWithCameraRTC(deviceForPlay)
      return
    }

    if (type === 'rtsp') {
      await playWithWebRTC(streamUrl)
      return
    }

    if (type === 'hls') {
      await initHlsPlayer(streamUrl)
      return
    }

    await playNative()
  } catch (error) {
    console.error('Play failed:', error)
    ElMessage.error(uiText('播放失败: {value0}', { value0: error.message || error }))
  }
}

const handleVideoClose = async () => {
  await stopPlayback()
  youtubeEmbedUrl.value = ''
  currentVideoDevice.value = null
}


watch(videoDialogVisible, visible => { if (!visible) stopPlayback() })
onMounted(() => {
  loadDevices()
  identityTimer = setInterval(() => {
    if (identityCurrent()) return
    listSequence++; devices.value = []; selectedDevices.value = []
    targets.value = []; total.value = 0; loading.value = false; showDeployment.value = false
    videoDialogVisible.value = false; stopPlayback(); identity = captureComputeIdentity()
    loadError.value = uiText('登录身份或租户已变化，请重新加载设备；已发送任务请回原账号核实')
  }, 500)
})
onUnmounted(() => { listSequence++; clearInterval(identityTimer); stopPlayback() })
</script>
<style scoped>
.algorithm-arrangement { padding:20px; }
.arrangement-toolbar,.arrangement-filters { display:flex; align-items:center; gap:12px; margin-bottom:18px; flex-wrap:wrap; }
.arrangement-toolbar { justify-content:space-between; }
.arrangement-toolbar strong { font-size:18px; margin-right:18px; }
.arrangement-toolbar span,.arrangement-filters span { color:#909399; font-size:13px; }
.arrangement-filters { margin-top:18px; }
.arrangement-filters .el-input { width:260px; }
.arrangement-filters .el-select { width:300px; }
.paginationBox { display:flex; justify-content:flex-end; margin-top:20px; }
.video-dialog-content { height:60vh; background:#000; display:flex; align-items:center; justify-content:center; }
.video-element { width:100%; height:100%; border:0; }
.video-empty { color:#fff; }
</style>
