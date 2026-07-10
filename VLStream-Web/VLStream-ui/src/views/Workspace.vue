<template>
  <div class="workspace">
    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 顶部问候区域 -->
      <div class="top-greeting">
        <!-- 左侧问候和天气 -->
        <div class="greeting-weather">
          <div class="user-greeting">
            <h1>{{ userName }}，{{ greetingText }}！</h1>
          </div>
          <div class="weather-section">
            <div class="weather-icon">
              <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                <circle cx="24" cy="24" r="8" fill="#FFD700"/>
                <path d="M24 4v4M24 40v4M44 24h-4M8 24H4M38.485 9.515l-2.828 2.828M12.343 35.657l-2.828 2.828M38.485 38.485l-2.828-2.828M12.343 12.343l-2.828-2.828" stroke="#FFD700" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <div class="weather-temp">{{ weatherTemp }}</div>
            <div class="weather-info">
              <div class="date">{{ currentDate }} {{ currentTime }}</div>
              <div class="day">{{ currentDay }}</div>
            </div>
          </div>
        </div>
        
        <!-- 中间统计数据卡片 -->
        <div class="stats-section">
          <div class="stat-card pending">
            <div class="stat-number">12</div>
            <div class="stat-label">待审批</div>
            <div class="stat-change positive">相较昨日 ▲8</div>
          </div>
          <div class="stat-card approved">
            <div class="stat-number">4</div>
            <div class="stat-label">已审批</div>
            <div class="stat-change negative">相较昨日 ▼2</div>
          </div>
          <div class="stat-card total">
            <div class="stat-number">76</div>
            <div class="stat-label">所有申请</div>
            <div class="stat-change positive">相较昨日 ▲10</div>
          </div>
        </div>
        
        <!-- 右侧VLStream Cloud卡片 -->
        <div class="vlstream-card">
          <div class="card-content">
            <div class="card-text">
              <h3>VLStream Cloud</h3>
              <el-button type="primary" size="small" class="guide-btn" @click="openGuide">
                查看引导手册
              </el-button>
            </div>
            <div class="card-illustration">
              <!-- 装饰性图案 -->
              <div class="illustration-elements">
                <div class="element person1"></div>
                <div class="element person2"></div>
                <div class="element shape1"></div>
                <div class="element shape2"></div>
                <div class="element shape3"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间功能区域 -->
      <div class="content-section">
        <!-- 常用功能 -->
        <div class="functions-panel">
          <div class="panel-header">
            <h3>常用功能</h3>
          </div>
          <div class="functions-grid">
            <div class="function-card" @click="navigateTo('video-square')">
              <div class="function-icon video">
                <el-icon size="28">
                  <VideoCamera />
                </el-icon>
              </div>
              <div class="function-text">视频汇聚</div>
            </div>
            <div class="function-card" @click="navigateTo('scene-governance')">
              <div class="function-icon scene">
                <el-icon size="28">
                  <Setting />
                </el-icon>
              </div>
              <div class="function-text">场景治理</div>
            </div>
            <div class="function-card" @click="navigateTo('event-management')">
              <div class="function-icon event">
                <el-icon size="28">
                  <Document />
                </el-icon>
              </div>
              <div class="function-text">事件管理</div>
            </div>
            <div class="function-card" @click="navigateTo('algorithm-management')">
              <div class="function-icon algorithm">
                <el-icon size="28">
                  <ShoppingCart />
                </el-icon>
              </div>
              <div class="function-text">算法超市</div>
            </div>
            <div class="function-card">
              <div class="function-icon standard">
                <el-icon size="28">
                  <Star />
                </el-icon>
              </div>
              <div class="function-text">算法标准</div>
            </div>
            <div class="function-card" @click="navigateTo('tag-management')">
              <div class="function-icon label">
                <el-icon size="28">
                  <PriceTag />
                </el-icon>
              </div>
              <div class="function-text">标签管理</div>
            </div>
            <div class="function-card" @click="navigateTo('device-management')">
              <div class="function-icon device">
                <el-icon size="28">
                  <Monitor />
                </el-icon>
              </div>
              <div class="function-text">设备管理</div>
            </div>
            <div class="function-card" @click="navigateTo('video-playback')">
              <div class="function-icon recovery">
                <el-icon size="28">
                  <View />
                </el-icon>
              </div>
              <div class="function-text">视频回放</div>
            </div>
            <div class="function-card">
              <div class="function-icon log">
                <el-icon size="28">
                  <Document />
                </el-icon>
              </div>
              <div class="function-text">操作日志</div>
            </div>
            <div class="function-card" @click="navigateTo('algorithm-training')">
              <div class="function-icon training">
                <el-icon size="28">
                  <Cpu />
                </el-icon>
              </div>
              <div class="function-text">算法训练</div>
            </div>
            <div class="function-card" @click="navigateTo('algorithm-orchestration')">
              <div class="function-icon orchestration">
                <el-icon size="28">
                  <Connection />
                </el-icon>
              </div>
              <div class="function-text">算法编排</div>
            </div>
            <div class="function-card" @click="navigateTo('open-service-center')">
              <div class="function-icon service">
                <el-icon size="28">
                  <Plus />
                </el-icon>
              </div>
              <div class="function-text">开放服务</div>
            </div>
          </div>
        </div>

        <!-- 热门设备 -->
        <div class="devices-panel">
          <div class="panel-header">
            <h3>热门设备</h3>
            <el-link type="primary" class="more-link" @click="gotoMoreDevice">更多</el-link>
          </div>
          <div class="devices-list">
            <div class="device-item" v-for="device in hotDevices" :key="device.id">
              <div class="device-status online"></div>
              <div class="device-info">
                <el-icon color="#1890ff" size="20">
                  <VideoCamera />
                </el-icon>
                <span class="device-name">{{ device.deviceName }}</span>
              </div>
              <el-button type="primary" size="small" plain @click="handlePlay(device)">播放</el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 我的审批表格 -->
      <div class="approval-panel">
        <div class="panel-header">
          <h3>我的审批</h3>
          <el-link type="primary" class="more-link">更多</el-link>
        </div>
        <div class="table-container">
          <el-table :data="approvalData" stripe>
            <el-table-column prop="deviceName" label="设备名称" width="140" />
            <el-table-column prop="status" label="状态" width="120">
              <template #default="scope">
                <el-tag
                    :type="scope.row.status === 1 ? 'success' : 'danger'"
                    size="small">
                  {{ scope.row.status === 1 ? '在线' : '离线' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deviceId" label="设备ID" width="140" />
            <el-table-column prop="deviceType" label="设备类型" width="120" />
            <el-table-column prop="position" label="设备位置" width="180" />
            <el-table-column prop="algorithm" label="拥有算法" min-width="200" />
            <el-table-column prop="applicant" label="申请人" width="120" />
            <el-table-column label="操作" width="220">
              <template #default="scope">
                <el-button type="primary" size="small" text>详情</el-button>
                <el-button type="success" size="small" text>同意</el-button>
                <el-button type="danger" size="small" text>拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </div>

  <!-- 热门设备 - 简易WebRTC播放弹窗 -->
  <el-dialog
    v-model="showPlayer"
    width="900px"
    :title="currentPlayDevice?.deviceName ? `${currentPlayDevice.deviceName} - 摄像头预览` : '摄像头预览'"
    append-to-body
    @close="handlePlayerClose"
  >
    <div class="workspace-player">
      <video
        ref="webrtcVideoEl"
        class="workspace-webrtc-video"
        controls
        autoplay
        muted
        playsinline
      ></video>
    </div>
  </el-dialog>
</template>

<script setup>
import {computed, nextTick, onBeforeUnmount, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {
  Connection,
  Cpu,
  Document,
  Monitor,
  Plus,
  PriceTag,
  Setting,
  ShoppingCart,
  Star,
  VideoCamera,
  View
} from '@element-plus/icons-vue'
import {getDeviceList} from "@/api/device";
import { WEBRTC_SERVER_BASE_URL } from "@/api/webrtc";
import { ElMessage } from "element-plus";

const router = useRouter()

// 分页相关
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
const currentTime = computed(() => {
  const date = now.value
  return `${pad2(date.getHours())}:${pad2(date.getMinutes())}`
})
const currentDay = computed(() => {
  const date = now.value
  return weekLabels[date.getDay()]
})

const greetingText = computed(() => {
  const hour = now.value.getHours()
  if (hour >= 5 && hour < 12) return '早上好'
  if (hour >= 12 && hour < 14) return '中午好'
  if (hour >= 14 && hour < 18) return '下午好'
  if (hour >= 18 && hour < 23) return '晚上好'
  return '夜深了'
})

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
  if (clockTimer) {
    clearInterval(clockTimer)
  }
  clockTimer = setInterval(updateNow, 1000)
}

const startWeatherRefresh = () => {
  fetchWeather()
  if (weatherTimer) {
    clearInterval(weatherTimer)
  }
  weatherTimer = setInterval(fetchWeather, 30 * 60 * 1000)
}


// WebRTC
const showPlayer = ref(false)
const webrtcVideoEl = ref(null)
const currentPlayDevice = ref(null)
let webrtcPlayer = null


// 热门设备数据
const hotDevices = ref([])

// 审批数据
const approvalData = ref([
  {
    deviceName: '海康云台',
    status: '待机器人',
    deviceId: '65131984',
    deviceType: '球机',
    position: '水产大厦19K',
    algorithm: '人流密度、客流量、物品识别',
    applicant: '郑明慧'
  },
  {
    deviceName: '海康云台',
    status: '待机器人',
    deviceId: '65131984',
    deviceType: '球机',
    position: '水产大厦19K',
    algorithm: '人流密度、客流量、物品识别',
    applicant: '郑明慧'
  },
  {
    deviceName: '海康云台',
    status: '待机器人',
    deviceId: '65131984',
    deviceType: '球机',
    position: '水产大厦19K',
    algorithm: '人流密度、客流量、物品识别',
    applicant: '郑明慧'
  },
  {
    deviceName: '海康云台',
    status: '待机器人',
    deviceId: '65131984',
    deviceType: '球机',
    position: '水产大厦19K',
    algorithm: '人流密度、客流量、物品识别',
    applicant: '郑明慧'
  },
  {
    deviceName: '海康云台',
    status: '待机器人',
    deviceId: '65131984',
    deviceType: '球机',
    position: '水产大厦19K',
    algorithm: '人流密度、客流量、物品识别',
    applicant: '郑明慧'
  },
  {
    deviceName: '海康云台',
    status: '待机器人',
    deviceId: '65131984',
    deviceType: '球机',
    position: '水产大厦19K',
    algorithm: '人流密度、客流量、物品识别',
    applicant: '郑明慧'
  }
])

const WEBRTC_STREAMER_BASE = WEBRTC_SERVER_BASE_URL
const WEBRTC_SCRIPT_URLS = [
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

    const script = document.createElement("script")
    script.src = src
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`Failed to load ${src}`))
    document.head.appendChild(script)
  })
}

const ensureWebRtcStreamerScripts = async () => {
  if (webrtcScriptLoader) {
    return webrtcScriptLoader
  }

  webrtcScriptLoader = Promise.all(WEBRTC_SCRIPT_URLS.map(loadScriptTag)).catch(error => {
    webrtcScriptLoader = null
    throw error
  })

  return webrtcScriptLoader
}

const cleanupWebrtcPlayer = async () => {
  if (webrtcPlayer) {
    try {
      if (typeof webrtcPlayer.disconnect === "function") {
        await webrtcPlayer.disconnect()
      } else if (typeof webrtcPlayer.stop === "function") {
        await webrtcPlayer.stop()
      }
    } catch (error) {
      console.warn("停止 WebRTC 播放失败:", error)
    }
    webrtcPlayer = null
  }

  if (webrtcVideoEl.value) {
    webrtcVideoEl.value.srcObject = null
  }
}

const handlePlayerClose = async () => {
  await cleanupWebrtcPlayer()
  showPlayer.value = false
}

const gotoMoreDevice = async () => {
  await router.push('/device-management')
}

const handlePlay = async (device) => {
  const streamUrl = device?.streamUrl || device?.originalRtspUrl || device?.rtspUrl || device?.url
  if (!streamUrl) {
    ElMessage.warning("暂无可用流地址")
    return
  }

  currentPlayDevice.value = device
  showPlayer.value = true

  try {
    await ensureWebRtcStreamerScripts()
    await nextTick()

    const videoEl = webrtcVideoEl.value
    if (!videoEl) {
      throw new Error("播放器未准备好")
    }

    if (!window.WebRtcStreamer) {
      throw new Error("WebRtcStreamer 未加载")
    }

    if (!videoEl.id) {
      videoEl.id = `workspace-webrtc-${device.id || Date.now()}`
    }

    await cleanupWebrtcPlayer()

    webrtcPlayer = new window.WebRtcStreamer(videoEl.id, WEBRTC_STREAMER_BASE)
    webrtcPlayer.onconnected = () => ElMessage.success("WebRTC连接成功")
    webrtcPlayer.onerror = () => ElMessage.error("WebRTC连接失败")

    if (typeof webrtcPlayer.connect === "function") {
      webrtcPlayer.connect(streamUrl, "", "rtptransport=tcp&timeout=60")
    } else if (typeof webrtcPlayer.play === "function") {
      webrtcPlayer.play(streamUrl)
    } else {
      throw new Error("WebRtcStreamer 缺少 connect/play 方法")
    }
  } catch (error) {
    console.error("播放失败:", error)
    ElMessage.error(`播放失败: ${error.message || error}`)
    showPlayer.value = false
  }
}

const navigateTo = (path) => {
  router.push('/' + path)
}

const openGuide = () => {
  const guideUrl = `${import.meta.env.BASE_URL}vlstream-cloud-guide.pdf`
  // window.open(guideUrl, '_blank', 'noopener')
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

    const params = {
      page: currentPage.value,
      size: pageSize.value
    }

    const response = await getDeviceList(params)
    hotDevices.value = response.data.records || []
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
  cleanupWebrtcPlayer()
})
</script>

<style scoped>
.workspace-player {
  width: 100%;
  height: 500px;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.workspace-webrtc-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

.workspace {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.main-content {
  max-width: 1600px;
  margin: 0 auto;
}

/* 顶部问候区域 */
.top-greeting {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  height: 200px;
}

/* 左侧问候和天气 */
.greeting-weather {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.user-greeting h1 {
  margin: 0 0 24px 0;
  color: #1a1a1a;
  font-size: 28px;
  font-weight: 700;
}

.weather-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.weather-icon {
  flex-shrink: 0;
}

.weather-temp {
  font-size: 40px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1;
}

.weather-info {
  margin-left: 8px;
}

.weather-info .date {
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.weather-info .day {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

/* 中间统计数据区域 */
.stats-section {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-around;
  align-items: center;
}

.stat-card {
  text-align: center;
  flex: 1;
}

.stat-number {
  font-size: 48px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 8px;
  line-height: 1;
}

.stat-label {
  color: #666;
  font-size: 16px;
  margin-bottom: 8px;
  font-weight: 500;
}

.stat-change {
  font-size: 14px;
  font-weight: 500;
}

.stat-change.positive {
  color: #52c41a;
}

.stat-change.negative {
  color: #ff4d4f;
}

/* 右侧VLStream Cloud卡片 */
.vlstream-card {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 32px;
  color: white;
  position: relative;
  overflow: hidden;
}

.card-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  z-index: 2;
}

.card-text h3 {
  margin: 0 0 16px 0;
  font-size: 24px;
  font-weight: 700;
  color: #666;
}

.guide-btn {
  background: #1A53FF;
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
}

.guide-btn:hover {
  background: #0040e6;
}

/* 装饰性插图 */
.card-illustration {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 200px;
  height: 120px;
  pointer-events: none;
}

.illustration-elements {
  position: relative;
  width: 100%;
  height: 100%;
}

.element {
  position: absolute;
  border-radius: 50%;
}

.element.person1 {
  width: 40px;
  height: 40px;
  background: #FFE4B5;
  right: 60px;
  bottom: 30px;
  border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
}

.element.person2 {
  width: 35px;
  height: 35px;
  background: #87CEEB;
  right: 20px;
  bottom: 20px;
  border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
}

.element.shape1 {
  width: 60px;
  height: 20px;
  background: #FFD700;
  right: 80px;
  bottom: 80px;
  border-radius: 10px;
  transform: rotate(-15deg);
}

.element.shape2 {
  width: 25px;
  height: 25px;
  background: #FF6B6B;
  right: 30px;
  bottom: 70px;
  border-radius: 50%;
}

.element.shape3 {
  width: 45px;
  height: 15px;
  background: #4ECDC4;
  right: 100px;
  bottom: 50px;
  border-radius: 8px;
  transform: rotate(20deg);
}

/* 中间功能区域 */
.content-section {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
}

.functions-panel {
  flex: 2.5;
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.panel-header h3 {
  margin: 0;
  color: #1a1a1a;
  font-size: 18px;
  font-weight: 600;
}

.more-link {
  font-size: 14px;
  text-decoration: none;
}

.functions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.function-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.function-card:hover {
  background-color: #f8faff;
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #e6f7ff;
}

.function-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.function-icon.video { background-color: #e6f7ff; color: #1890ff; }
.function-icon.scene { background-color: #f6ffed; color: #52c41a; }
.function-icon.event { background-color: #fff7e6; color: #fa8c16; }
.function-icon.algorithm { background-color: #f9f0ff; color: #722ed1; }
.function-icon.standard { background-color: #fff1f0; color: #f5222d; }
.function-icon.label { background-color: #feffe6; color: #fadb14; }
.function-icon.device { background-color: #fafafa; color: #8c8c8c; }
.function-icon.recovery { background-color: #e6f7ff; color: #1890ff; }
.function-icon.log { background-color: #f5f5f5; color: #595959; }
.function-icon.training { background-color: #f6ffed; color: #52c41a; }
.function-icon.orchestration { background-color: #fff7e6; color: #fa8c16; }
.function-icon.service { background-color: #e6f7ff; color: #1890ff; }

.function-text {
  font-size: 14px;
  color: #1a1a1a;
  text-align: center;
  font-weight: 500;
}

.devices-panel {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.devices-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.device-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.device-item:last-child {
  border-bottom: none;
}

.device-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #52c41a;
  margin-right: 12px;
  flex-shrink: 0;
}

.device-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.device-name {
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 500;
}

/* 审批表格 */
.approval-panel {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.table-container {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table) {
  border-radius: 8px;
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #fafafa;
  color: #1a1a1a;
  font-weight: 600;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.el-table td) {
  border-bottom: 1px solid #f5f5f5;
}

:deep(.el-table .el-table__row:hover > td) {
  background-color: #f8faff;
}

:deep(.el-button--text) {
  padding: 4px 8px;
  font-size: 13px;
}

:deep(.el-tag) {
  border-radius: 4px;
  font-size: 12px;
}
</style> 

