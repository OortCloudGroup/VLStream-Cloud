<template>
  <div class="workspace">
    <div class="main-content">
      <!-- 顶部问候 + 引导 -->
      <div class="top-greeting">
        <div class="greeting-stats-card">
          <div class="greeting-weather">
            <div class="user-greeting">
              <h1>{{ userName }}，{{ greetingText }}！</h1>
            </div>
            <div class="weather-section">
              <img class="weather-icon" src="@/assets/img/workbench/weather.png" alt="weather" />
              <div class="weather-content">
                <div class="weather-temp">{{ weatherTemp }}</div>
                <div class="weather-info">
                  <div class="date">{{ currentDate }}</div>
                  <div class="day">{{ currentDay }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="stats-section">
            <div class="stat-card">
              <div class="stat-label">待审批</div>
              <div class="stat-value-row">
                <span class="stat-number">12</span>
                <span class="stat-change">
                  相较昨日 <em class="up">▲ 8</em>
                </span>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-label">已审批</div>
              <div class="stat-value-row">
                <span class="stat-number">4</span>
                <span class="stat-change">
                  相较昨日 <em class="down">▼ 2</em>
                </span>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-label">所有申请</div>
              <div class="stat-value-row">
                <span class="stat-number">76</span>
                <span class="stat-change">
                  相较昨日 <em class="down">▼ 10</em>
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="vlstream-card">
          <div class="card-content">
            <div class="card-text">
              <h3>VLStream Cloud</h3>
              <el-button type="primary" size="small" class="guide-btn" @click="openGuide">
                查看引导手册
              </el-button>
            </div>
            <img class="guide-illustration" src="@/assets/img/workbench/guide_illustration.png" alt="guide" />
          </div>
        </div>
      </div>

      <!-- 中间功能区域 -->
      <div class="content-section">
        <div class="functions-panel">
          <div class="panel-header">
            <h3>常用功能</h3>
          </div>
          <div class="functions-grid">
            <div
              v-for="item in functionList"
              :key="item.key"
              class="function-card"
              @click="item.path && navigateTo(item.path)"
            >
              <img class="function-icon" :src="item.icon" :alt="item.label" />
              <div class="function-text">{{ item.label }}</div>
            </div>
          </div>
        </div>

        <div class="devices-panel">
          <div class="panel-header">
            <h3>热门设备</h3>
            <el-link type="primary" class="more-link" @click="gotoMoreDevice">更多</el-link>
          </div>
          <div class="devices-list">
            <div v-for="device in hotDevices" :key="device.id" class="device-item">
              <div class="device-info">
                <img class="device-cam" src="@/assets/img/workbench/device_cam.png" alt="device" />
                <span class="device-name">{{ device.deviceName }}</span>
              </div>
              <button class="play-btn" type="button" @click="handlePlay(device)">
                <img class="play-icon" src="@/assets/img/workbench/play_btn.png" alt="play" />
                <span>播放</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 我的待审核 -->
      <div class="approval-panel">
        <div class="panel-header">
          <h3>我的待审核</h3>
          <el-link type="primary" class="more-link">更多</el-link>
        </div>
        <div class="table-container">
          <TableSelf
            class="approval-table new_table"
            header-cell-class-name="approval_header_cell"
            :data="approvalData"
            current-row-key="deviceId"
          >
            <el-table-column prop="deviceName" label="设备名称" :width="clacPXToVW(140)" />
            <el-table-column prop="tag" label="标签" :width="clacPXToVW(120)">
              <template #default="scope">
                <span class="tag-pill">{{ scope.row.tag }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="deviceId" label="设备ID" :width="clacPXToVW(140)" />
            <el-table-column prop="deviceType" label="设备类型" :width="clacPXToVW(120)" />
            <el-table-column prop="position" label="设备位置" :width="clacPXToVW(160)" />
            <el-table-column prop="algorithm" label="拥有算法" show-overflow-tooltip />
            <el-table-column prop="applicant" label="申请人" />
            <el-table-column label="操作" fixed="right" align="right" :width="clacPXToVW(220)">
              <template #default="scope">
                <div class="operate-box flexRowAC">
                  <div class="new_table_svg_group" @click="handleDetail(scope.row)">
                    <oort-svg-icon width="14" height="14" name="detail_icon" class="new_table_svg_group_svg" />
                    <span>详情</span>
                  </div>
                  <div class="new_table_svg_group" @click="handleEdit(scope.row)">
                    <oort-svg-icon width="14" height="14" name="edit_icon" class="new_table_svg_group_svg" />
                    <span>编辑</span>
                  </div>
                  <div class="new_table_svg_group" @click="handleDelete(scope.row)">
                    <oort-svg-icon color="red" width="14" height="14" name="delete_icon" class="new_table_svg_group_svg" />
                    <span>删除</span>
                  </div>
                </div>
              </template>
            </el-table-column>
          </TableSelf>
        </div>
      </div>
    </div>
  </div>

  <!-- 热门设备播放弹窗 -->
  <el-dialog
    v-model="showPlayer"
    class="workspace-player-dialog"
    top="10vh"
    width="900px"
    draggable
    :title="currentPlayDevice?.deviceName ? `${currentPlayDevice.deviceName} - 摄像头预览` : '摄像头预览'"
    append-to-body
    @close="handlePlayerClose"
  >
    <template #header="{ titleId, titleClass }">
      <div class="workspace-player-header">
        <span :id="titleId" :class="titleClass">
          {{ currentPlayDevice?.deviceName ? `${currentPlayDevice.deviceName} - 摄像头预览` : '摄像头预览' }}
        </span>
        <el-icon class="workspace-fullscreen-button" title="全屏" @click="toggleFullscreen">
          <FullScreen />
        </el-icon>
      </div>
    </template>
    <div ref="workspacePlayerRef" class="workspace-player">
      <div ref="oplayerContainer" class="oplayer-container" />
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { FullScreen } from '@element-plus/icons-vue'
import { getDeviceList } from '@/api/device'
import { ensureWebRTCBackendConfig, WEBRTC_SERVER_BASE_URL } from '@/api/webrtc'
import { ElLoading, ElMessage } from 'element-plus'
import { clacPXToVW } from '@/utils/index'
import { ensureOPlayer } from '@/utils/oplayer'
import { getStreamType } from '@/views/VideoAggregation/deviceUtils.js'

import iconVideoPlaza from '@/assets/img/workbench/video_plaza.png'
import iconSceneGovernance from '@/assets/img/workbench/scene_governance.png'
import iconEventManagement from '@/assets/img/workbench/event_management.png'
import iconAlgoMarket from '@/assets/img/workbench/algo_market.png'
import iconAlgoTunnel from '@/assets/img/workbench/algo_tunnel.png'
import iconLabelManagement from '@/assets/img/workbench/label_management.png'
import iconDeviceManagement from '@/assets/img/workbench/device_management.png'
import iconVideoPlayback from '@/assets/img/workbench/video_playback.png'
import iconOperationLog from '@/assets/img/workbench/operation_log.png'
import iconAlgoTraining from '@/assets/img/workbench/algo_training.png'
import iconAlgoOrchestration from '@/assets/img/workbench/algo_orchestration.png'

const router = useRouter()

const currentPage = ref(1)
const pageSize = ref(10)
const userName = ref('用户')
const now = ref(new Date())
const weatherTemp = ref('--\u00b0C')
let clockTimer = null
let weatherTimer = null

const weekLabels = [
  '\u661f\u671f\u65e5',
  '\u661f\u671f\u4e00',
  '\u661f\u671f\u4e8c',
  '\u661f\u671f\u4e09',
  '\u661f\u671f\u56db',
  '\u661f\u671f\u4e94',
  '\u661f\u671f\u516d'
]
const pad2 = (value) => String(value).padStart(2, '0')
const currentDate = computed(() => {
  const date = now.value
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
})
const currentDay = computed(() => weekLabels[now.value.getDay()])

const greetingText = computed(() => {
  const hour = now.value.getHours()
  if (hour >= 5 && hour < 12) return '早上好'
  if (hour >= 12 && hour < 14) return '中午好'
  if (hour >= 14 && hour < 18) return '下午好'
  if (hour >= 18 && hour < 23) return '晚上好'
  return '夜深了'
})

const functionList = [
  { key: 'video-plaza', label: '视频广场', path: 'video-square', icon: iconVideoPlaza },
  { key: 'scene-governance', label: '场景治理', path: 'scene-governance', icon: iconSceneGovernance },
  { key: 'event-management', label: '事件管理', path: 'event-management', icon: iconEventManagement },
  { key: 'algo-market', label: '算法超市', path: 'algorithm-management', icon: iconAlgoMarket },
  { key: 'algo-tunnel', label: '算法隧道', path: '', icon: iconAlgoTunnel },
  { key: 'label-management', label: '标签管理', path: 'tag-management', icon: iconLabelManagement },
  { key: 'device-management', label: '设备管理', path: 'device-management', icon: iconDeviceManagement },
  { key: 'video-playback', label: '视频回放', path: 'video-playback', icon: iconVideoPlayback },
  { key: 'operation-log', label: '操作日志', path: '', icon: iconOperationLog },
  { key: 'algo-training', label: '算法自主训练', path: 'algorithm-training', icon: iconAlgoTraining },
  { key: 'algo-orchestration', label: '算法编排', path: 'algorithm-orchestration', icon: iconAlgoOrchestration }
]

const updateNow = () => {
  now.value = new Date()
}

const getCurrentPosition = () => new Promise((resolve, reject) => {
  if (!navigator.geolocation) {
    reject(new Error('Geolocation not supported'))
    return
  }
  navigator.geolocation.getCurrentPosition(
    position => resolve(position.coords),
    error => reject(error),
    { enableHighAccuracy: false, timeout: 5000, maximumAge: 600000 }
  )
})

const fetchWeather = async () => {
  try {
    let coords = null
    try {
      coords = await getCurrentPosition()
    } catch (error) {
      coords = null
    }

    const latitude = coords?.latitude ?? 39.9042
    const longitude = coords?.longitude ?? 116.4074
    const url = `https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}&current=temperature_2m`
    const response = await fetch(url)
    if (!response.ok) {
      throw new Error(`Weather request failed (${response.status})`)
    }
    const data = await response.json()
    const temperature = data?.current?.temperature_2m
    if (typeof temperature === 'number') {
      weatherTemp.value = `${Math.round(temperature)}\u00b0C`
    }
  } catch (error) {
    console.warn('Weather fetch failed:', error)
  }
}

const startClock = () => {
  updateNow()
  if (clockTimer) clearInterval(clockTimer)
  clockTimer = setInterval(updateNow, 1000)
}

const startWeatherRefresh = () => {
  fetchWeather()
  if (weatherTimer) clearInterval(weatherTimer)
  weatherTimer = setInterval(fetchWeather, 30 * 60 * 1000)
}

const showPlayer = ref(false)
const oplayerContainer = ref(null)
const oplayerInstance = shallowRef(null)
const currentPlayDevice = ref(null)
const workspacePlayerRef = ref(null)
let activePlaybackTask = null

const hotDevices = ref([])

const approvalData = ref([])

/**
 * 切换工作台播放器全屏状态。
 */
const toggleFullscreen = async () => {
  const playerElement = workspacePlayerRef.value
  if (!playerElement) return

  try {
    if (document.fullscreenElement) {
      await document.exitFullscreen()
    } else {
      await playerElement.requestFullscreen?.()
    }
  } catch (error) {
    console.error('切换播放器全屏失败:', error)
    ElMessage.error('切换全屏失败')
  }
}

/**
 * 释放工作台播放器资源。
 */
const cleanupOPlayer = () => {
  activePlaybackTask = null

  if (oplayerInstance.value?.compInstance?.$destroy) {
    oplayerInstance.value.compInstance.$destroy()
  }
  oplayerInstance.value = null

  if (oplayerContainer.value) {
    oplayerContainer.value.innerHTML = ''
  }
}

const handlePlayerClose = () => {
  cleanupOPlayer()
  showPlayer.value = false
}

const gotoMoreDevice = async () => {
  await router.push('/device-management')
}

/**
 * 根据流类型生成工作台 OPlayer 参数。
 */
const createWorkspacePlayerOptions = async (streamUrl) => {
  const streamType = getStreamType(streamUrl)
  const playerConfig = {
    debuggerMode: false,
    autoSize: true,
    backgroundColor: '#000000',
    showHeader: false
  }

  if (streamType === 'cameraRTC') {
    const url = new URL(streamUrl)
    const cameraId = url.pathname.split('/').filter(Boolean).pop()
    if (!cameraId) {
      throw new Error('CameraRTC 地址中缺少摄像头ID')
    }

    playerConfig.webRTCSocketURL = url.origin.replace(/^http/, 'ws')
    return {
      playerConfig,
      playConfig: { type: 'cameraRTC', src: cameraId }
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
    playConfig: { type: playType, src: streamUrl }
  }
}

/**
 * 播放热门设备的视频流。
 */
const handlePlay = async (device) => {
  const streamUrl = device?.streamUrl || device?.originalRtspUrl || device?.rtspUrl || device?.url
  if (!streamUrl) {
    ElMessage.warning('暂无可用流地址')
    return
  }

  cleanupOPlayer()
  const playbackTask = Symbol('workspace-oplayer')
  activePlaybackTask = playbackTask
  currentPlayDevice.value = device
  showPlayer.value = true

  const loading = ElLoading.service({
    lock: true,
    text: '正在启动视频播放...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    await Promise.all([ensureOPlayer(), nextTick()])
    const { playerConfig, playConfig } = await createWorkspacePlayerOptions(streamUrl)
    const container = oplayerContainer.value
    if (activePlaybackTask !== playbackTask) return
    if (!container) throw new Error('播放器容器未准备好')

    const player = new window.OToolBox.OPlayer(container, playerConfig)
    oplayerInstance.value = player
    player.play({
      ...playConfig,
      name: device?.deviceName || device?.name || ''
    })
  } catch (error) {
    if (activePlaybackTask !== playbackTask) return
    console.error('播放失败:', error)
    ElMessage.error(`播放失败: ${error.message || error}`)
    cleanupOPlayer()
    showPlayer.value = false
  } finally {
    loading.close()
  }
}

const navigateTo = (path) => {
  if (!path) return
  router.push('/' + path)
}

const openGuide = () => {
  const guideUrl = `${import.meta.env.BASE_URL}vlstream-cloud-guide.pdf`
  // window.open(guideUrl, '_blank', 'noopener')
}

const handleDetail = () => {
  ElMessage.info('详情功能开发中')
}

const handleEdit = () => {
  ElMessage.info('编辑功能开发中')
}

const handleDelete = () => {
  ElMessage.info('删除功能开发中')
}

onMounted(async () => {
  startClock()
  startWeatherRefresh()
  try {
    const cached = localStorage.getItem('userInfo')
    if (cached) {
      const info = JSON.parse(cached)
      userName.value = info.userName || info.username || userName.value
    }

    const response = await getDeviceList({
      page: currentPage.value,
      size: pageSize.value
    })
    hotDevices.value = (response.data.records || []).slice(0, 5)
  } catch (e) {
    console.warn('读取用户信息失败，使用默认名称', e)
  }
})

onBeforeUnmount(() => {
  if (clockTimer) {
    clearInterval(clockTimer)
    clockTimer = null
  }
  if (weatherTimer) {
    clearInterval(weatherTimer)
    weatherTimer = null
  }
  cleanupOPlayer()
})
</script>

<style scoped>
.workspace-player {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #000;
  overflow: hidden;
}

.oplayer-container {
  width: 100%;
  height: 100%;
  background: #000;
}

.workspace {
  padding: 20px 24px;
  background-color: #f4f7f9;
  min-height: calc(100vh - 60px);
}

.main-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  width: 1320px;
  margin: 0 auto;
}

.top-greeting {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  height: 160px;
}

.greeting-stats-card {
  flex: 1;
  background: #fff;
  border-radius: 10px;
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 76px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.greeting-weather {
  flex-shrink: 0;
  min-width: 240px;
}

.user-greeting h1 {
  margin: 0 0 27px;
  font-size: 16px;
  font-weight: bold;
  letter-spacing: 0.85px;
  color: #323233;
}

.weather-section {
  display: flex;
  align-items: center;
  gap: 25px;
}

.weather-icon {
  width: 55px;
  height: 64px;
  flex-shrink: 0;
}

.weather-content{
  display: flex;
  flex-direction: column;
  gap: 11px;
}

.weather-temp {
  font-size: 24px;
  font-weight: bold;
  letter-spacing: 1.28px;
  color: #323233;
}

.weather-info{
  display: flex;
  align-items: center;
  gap: 18px;
}

.weather-info .date {
  font-size: 14px;
  line-height: 20px;
  color: #999999;
}

.weather-info .day {
  font-size: 14px;
  letter-spacing: 0.75px;
  color: #999999;
}

.stats-section {
  flex: 1;
  display: flex;
  justify-content: space-around;
  align-items: center;
  gap: 24px;
}

.stat-card {
  flex: 1;
  text-align: left;
}

.stat-label {
  font-size: 14px;
  line-height: 22px;
  letter-spacing: 0.75px;
  color: #646566;
  margin-bottom: 10px;
}

.stat-value-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.stat-number {
  font-size: 40px;
  font-weight: bold;
  line-height: normal;
  letter-spacing: 0px;
  color: #323233;
}

.stat-change {
  font-size: 14px;
  line-height: 24px;
  letter-spacing: 0.75px;
  color: #969799;
  font-style: normal;
  white-space: nowrap;
}

.stat-change em {
  font-style: normal;
  font-weight: 500;
  margin-left: 2px;
}

.stat-change .up {
  color: #f56c6c;
}

.stat-change .down {
  color: #67c23a;
}

.vlstream-card {
  width: 428px;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
  padding: 24px;
  background-color: #fff;
}

.card-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
  border-radius: 4px;
  background: url(@/assets/img/workbench/guide_bg.png);
}

.card-text{
  margin-left: 24px;
}

.card-text h3 {
  font-size: 18px;
  font-weight: bold;
  line-height: normal;
  letter-spacing: 0.96px;
  color: #323233;
  margin-bottom: 10px;
}

.guide-btn {
  width: 88px;
  height: 24px;
  background: #2278FF;
  border: none;
  border-radius: 1px;
  padding: 3px 6px;
  font-size: 12px;
  font-weight: 600;
  color: #FFFFFF;
}

.guide-btn:hover {
  background: #0040e6;
}

.guide-illustration {
  width: 174px;
  height: auto;
  object-fit: contain;
  flex-shrink: 0;
  mix-blend-mode: multiply;
  margin-right: 14px;
  margin-bottom: -6px;
}

.content-section {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  height: 276px;
}

.functions-panel {
  flex: 1;
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  line-height: 22px;
  color: #333333;
  font-weight: normal;
  display: flex;
  align-items: center;
  gap: 6px;
}

.panel-header h3::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 18px;
  background: #2278FF;
}

.more-link {
  font-size: 13px;
  text-decoration: none;
}

.functions-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
}

.function-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.function-card:hover {
  background-color: #f5f8ff;
  transform: translateY(-2px);
}

.function-icon {
  width: 40px;
  height: 40px;
  object-fit: contain;
  margin-bottom: 16px;
}

.function-text {
  font-size: 14px;
  font-weight: normal;
  line-height: normal;
  text-align: center;
  letter-spacing: 0.96px;
  color: #323233;
  white-space: nowrap;
}

.devices-panel {
  display: flex;
  flex-direction: column;
  width: 428px;
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.devices-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.device-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
}

.device-item:last-child {
  border-bottom: none;
}

.device-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.device-cam {
  width: 20px;
  height: 20px;
  object-fit: contain;
  flex-shrink: 0;
}

.device-name {
  color: #1a1a1a;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.play-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 28px;
  padding: 0 12px;
  border: none;
  border-radius: 4px;
  background: #1a53ff;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.play-icon {
  width: 14px;
  height: 14px;
  object-fit: contain;
  filter: brightness(0) invert(1);
}

.play-btn:hover {
  background: #0040e6;
}

.approval-panel {
  flex: 1;
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.table-container {
  border-radius: 4px;
  overflow: hidden;
}

.tag-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  color: #1a53ff;
  background: #edf3ff;
  border: 1px solid #b8d0ff;
  line-height: 20px;
}

.operate-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.operate-box .new_table_svg_group {
  color: #1a53ff;
  cursor: pointer;
}

.operate-box .new_table_svg_group:last-child {
  color: #ff2525;
}

:deep(.approval-table) {
  font-size: 14px;
  color: #333;
}

:deep(.approval_header_cell) {
  background-color: #f5f8fc !important;
  color: #333 !important;
  font-weight: 600 !important;
  border-bottom: 1px solid #eef2f6 !important;
}

:deep(.approval-table .el-table__row:hover > td) {
  background-color: #f5f9ff !important;
}

:deep(.approval-table .el-table__body tr.current-row > td) {
  background-color: #eef5ff !important;
}

:deep(.approval-table td.el-table__cell) {
  border-bottom: 1px solid #f0f2f5;
}

:deep(.approval-table::before) {
  display: none;
}
</style>

<style>
.el-dialog.workspace-player-dialog {
  margin: 0 auto !important;
  border: none !important;
  padding: 0 !important;
}

.el-dialog.workspace-player-dialog .el-dialog__body {
  padding: 0 !important;
}

.el-dialog.workspace-player-dialog .el-dialog__header {
  position: relative;
}

.el-dialog.workspace-player-dialog .workspace-player-header {
  display: flex;
  align-items: center;
  width: 100%;
  padding-right: 72px;
  box-sizing: border-box;
}

.el-dialog.workspace-player-dialog .workspace-fullscreen-button {
  position: absolute;
  top: 50%;
  right: 52px;
  margin: 0;
  transform: translateY(-50%);
  font-size: 18px;
  color: var(--el-color-info);
  cursor: pointer;
}

.el-dialog.workspace-player-dialog .workspace-fullscreen-button:hover {
  color: var(--el-color-primary);
}
</style>
