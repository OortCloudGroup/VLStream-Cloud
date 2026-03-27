/**
 * 工作台页面
 */
<template>
  <div>
    <van-pull-refresh v-model="isLoading" class="workbench-body" @refresh="onRefresh">
      <div class="workbench-container">
        <div class="top-bg" :style="{ backgroundImage: `url(${bgImg})` }" />
        <!-- 顶部标题区域 -->
        <div class="header-section">
          <h1 class="page-title">
            工作台
          </h1>

          <!-- 天气和个人信息卡片 -->
          <div class="weather-card">
            <div class="weather-title">
              <img src="@/assets/img/VLStreamCloud/qt.png" alt="" />
              <div>
                <div class="temperature">
                  {{ weatherTemp }}
                </div>
                <div class="date">
                  {{ currentDate }} {{ currentTime }} {{ currentDay }}
                </div>
              </div>
            </div>
            <div class="weather-info">
              <div class="greeting">
                {{ userName }}，{{ greetingText }}!
              </div>
            </div>

            <!-- 状态卡片区域 -->
            <div class="status-cards">
              <div class="status-card pending">
                <div class="status-number">
                  {{ pendingCount }}
                </div>
                <div class="status-label">
                  待审批
                </div>
              </div>
              <div class="status-card approved">
                <div class="status-number">
                  {{ approvedCount }}
                </div>
                <div class="status-label">
                  已审批
                </div>
              </div>
              <div class="status-card all">
                <div class="status-number">
                  {{ allCount }}
                </div>
                <div class="status-label">
                  所有申请
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 常用功能区域 -->
        <div class="functions-section">
          <div class="section-header">
            <div class="section-title">
              常用功能
            </div>
          </div>
          <div class="functions-grid">
            <div
              v-for="(item, index) in functionList"
              :key="index"
              class="function-item"
              @click="handleFunctionClick(item)"
            >
              <img class="function-icon" :src="item.iconSvg" alt="" />
              <div class="function-label">
                {{ item.label }}
              </div>
            </div>
          </div>
        </div>

        <!-- 热门设备区域 -->
        <div class="popular-devices-section">
          <div class="section-header">
            <div class="section-title">
              热门设备
            </div>
            <div class="more-link" @click="handleMoreDevices">
              更多 >
            </div>
          </div>
          <div class="devices-list">
            <div
              v-for="(device, index) in popularDevices"
              :key="index"
              class="device-item"
            >
              <img class="device-icon" :src="sxjImg" alt="" />
              <div class="device-name">
                {{ device.name }}
              </div>
              <div class="play-btn" @click="handlePlay(device)">
                <img class="play-icon" :src="bfImg" alt="" />
              </div>
            </div>
          </div>
        </div>

        <!-- 我的待审批区域 -->
        <div class="approval-section">
          <div class="section-header">
            <div class="section-title">
              我的待审批
            </div>
            <div class="more-link" @click="handleMoreApprovals">
              更多 >
            </div>
          </div>
          <div class="approval-table">
            <div class="approval-header">
              <div class="header-cell device-name-header">
                设备名称
              </div>
              <div class="header-cell applicant-header">
                申请人
              </div>
              <div class="header-cell operation-header">
                操作
              </div>
            </div>
            <div
              v-for="(item, index) in approvalList"
              :key="index"
              class="approval-item"
            >
              <div class="approval-cell device-name-cell">
                {{ item.deviceName }}
              </div>
              <div class="approval-cell applicant-cell">
                {{ item.applicant }}
              </div>
              <div class="approval-cell operation-cell">
                <span class="approve-btn" @click="handleApprove(item)">
                  同意
                </span>
                <span class="reject-btn" @click="handleReject(item)">
                  拒绝
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/useraPaas'
import { getDeviceList } from '@/api/VLStreamCloud/device'
import { listTodoProcess, listFinishedProcess, listOwnProcess } from '@/api/processui'
import bgImg from '@/assets/img/VLStreamCloud/bg.png'
import sxjImg from '@/assets/img/VLStreamCloud/sxj.png'
import bfImg from '@/assets/img/VLStreamCloud/bf.png'
import spgc from '@/assets/img/VLStreamCloud/spgc.png'
import cjzl from '@/assets/img/VLStreamCloud/cjzl.png'
import bqgl from '@/assets/img/VLStreamCloud/bqgl.png'
import czrz from '@/assets/img/VLStreamCloud/czrz.png'
import fwzx from '@/assets/img/VLStreamCloud/fwzx.png'
import sbgl from '@/assets/img/VLStreamCloud/sbgl.png'
import sfbp from '@/assets/img/VLStreamCloud/sfbp.png'
import sfbz from '@/assets/img/VLStreamCloud/sfbz.png'
import sfcs from '@/assets/img/VLStreamCloud/sfcs.png'
import sfxl from '@/assets/img/VLStreamCloud/sfxl.png'
import sjgl from '@/assets/img/VLStreamCloud/sjgl.png'
import sphf from '@/assets/img/VLStreamCloud/sphf.png'

const router = useRouter()
const store = useUserStore()

// 用户名
const userName = ref(store.userInfo.userName || '用户')

// 下拉刷新状态
const isLoading = ref(false)

// 时间与天气相关
const now = ref(new Date())
const weatherTemp = ref('--°C')
let clockTimer = null

const weekLabels = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
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

const startClock = () => {
  updateNow()
  if (clockTimer) {
    clearInterval(clockTimer)
  }
  clockTimer = setInterval(updateNow, 1000)
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

const fetchWeather = async() => {
  try {
    let latitude = 39.9042
    let longitude = 116.4074

    try {
      const coords = await getCurrentPosition()
      latitude = coords.latitude
      longitude = coords.longitude
    } catch (error) {
      // 使用默认坐标（北京）
    }

    const url = `https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}&current=temperature_2m`
    const response = await fetch(url)
    if (!response.ok) return
    const data = await response.json()
    const temperature = data?.current?.temperature_2m
    if (typeof temperature === 'number') {
      weatherTemp.value = `${Math.round(temperature)}°C`
    }
  } catch (error) {
    // 忽略天气请求错误
  }
}

// 功能列表
const functionList = ref([
  {
    label: '视频广场',
    iconSvg: spgc
  },
  {
    label: '场景治理',
    iconSvg: cjzl
  },
  {
    label: '事件管理',
    iconSvg: sjgl
  },
  {
    label: '算法超市',
    iconSvg: sfcs
  },
  {
    label: '算法标注',
    iconSvg: sfbz
  },
  {
    label: '标签管理',
    iconSvg: bqgl
  },
  {
    label: '视频回放',
    iconSvg: sphf
  },
  {
    label: '设备管理',
    iconSvg: sbgl
  },
  {
    label: '算法训练',
    iconSvg: sfxl
  },
  {
    label: '算法编排',
    iconSvg: sfbp
  },
  {
    label: '操作日志',
    iconSvg: czrz
  },
  {
    label: '服务中心',
    iconSvg: fwzx
  }
])

// 热门设备列表
const popularDevices = ref([])

// 获取热门设备
const fetchPopularDevices = async() => {
  const params = {
    accessToken: store.userInfo?.accessToken,
    page: 1,
    size: 10
  }
  const res = await getDeviceList(params)
  if (res.code === 200 && res.data && Array.isArray(res.data.records)) {
    popularDevices.value = res.data.records.map((item) => ({
      id: item.id,
      name: item.deviceName,
      ...item
    }))
  }
}

// 审批统计与列表
const pendingCount = ref(0) // 待审批数量
const approvedCount = ref(0) // 已审批数量
const allCount = ref(0) // 所有申请数量

// 我的待审批列表
const approvalList = ref([])

const PROCESS_COMMON_PARAMS = {
  pageNum: 1,
  pageSize: 10,
  category: '2029435351850999810',
  categoryType: 'wfAppAll'
}

// 获取审批统计和列表
const fetchApprovalData = async() => {
  try {
    const [todoRes, finishedRes, ownRes] = await Promise.all([
      listTodoProcess(PROCESS_COMMON_PARAMS),
      listFinishedProcess(PROCESS_COMMON_PARAMS),
      listOwnProcess(PROCESS_COMMON_PARAMS)
    ])

    if (todoRes && todoRes.code === 200) {
      pendingCount.value = todoRes.total || (todoRes.data && todoRes.data.total) || 0
    } else {
      pendingCount.value = 0
    }

    if (finishedRes && finishedRes.code === 200) {
      approvedCount.value = finishedRes.total || (finishedRes.data && finishedRes.data.total) || 0
    } else {
      approvedCount.value = 0
    }

    if (ownRes && ownRes.code === 200) {
      allCount.value = ownRes.total || (ownRes.data && ownRes.data.total) || 0
      const rows = ownRes.rows || (ownRes.data && ownRes.data.rows) || []
      approvalList.value = rows.map(row => ({
        id: row.procInsId || row.hisProcInsId || row.taskId,
        deviceName: row.procDefName || row.workOrderName || '-',
        applicant: row.startUserName || '-'
      }))
    } else {
      allCount.value = 0
      approvalList.value = []
    }
  } catch (e) {
    pendingCount.value = 0
    approvedCount.value = 0
    allCount.value = 0
    approvalList.value = []
  }
}

onBeforeUnmount(() => {
  if (clockTimer) {
    clearInterval(clockTimer)
    clockTimer = null
  }
})

onMounted(() => {
  startClock()
  fetchWeather()
  fetchPopularDevices()
  fetchApprovalData()
})

// 处理功能点击
const handleFunctionClick = (item) => {
  const videoTabMap = {
    视频广场: 'square',
    场景治理: 'govern',
    视频回放: 'playback',
    设备管理: 'device',
    算法超市: 'algo'
  }

  const topTab = videoTabMap[item.label]

  if (topTab) {
    router.push({
      path: '/video',
      query: {
        topTab
      }
    })
    return
  }
}

// 处理播放
const handlePlay = (device) => {
  // eslint-disable-next-line no-console
  console.log('播放设备:', device.name)
}

// 处理更多设备
const handleMoreDevices = () => {
  // eslint-disable-next-line no-console
  console.log('查看更多设备')
}

// 处理更多审批
const handleMoreApprovals = () => {
  // eslint-disable-next-line no-console
  console.log('查看更多审批')
}

// 处理同意
const handleApprove = (item) => {
  // eslint-disable-next-line no-console
  console.log('同意审批:', item)
}

// 处理拒绝
const handleReject = (item) => {
  // eslint-disable-next-line no-console
  console.log('拒绝审批:', item)
}

// 下拉刷新
const onRefresh = async() => {
  await Promise.all([
    fetchWeather(),
    fetchPopularDevices(),
    fetchApprovalData()
  ])
  isLoading.value = false
}
</script>

<style lang="scss" scoped>
.workbench-body {
  height: calc(100vh - 50px);
  overflow: scroll;
  background: #fff;
  scrollbar-width: none;
  -ms-overflow-style: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.workbench-container {
  min-height: 100%;
  background: #F9FAFF;
  padding: 0 20px;
  padding-bottom: 20px;
  position: relative;
}

.top-bg {
  position: absolute;
  top: -15px;
  left: -20px;
  right: -20px;
  height: 200px;
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  z-index: 0;
}

.header-section,
.functions-section,
.popular-devices-section,
.approval-section {
  position: relative;
  z-index: 1;
}

.page-title {
  font-weight: 700;
  color: #ffffff;
  font-size: 18px;
  padding: 38px 0 21px 0;
}

.weather-card {
  height: 190px;
  background: #ffffff;
  border-radius: 12px;
  padding: 12px;
  .weather-title{
    display: flex;
    gap: 12px;
    margin-bottom: 10px;
    img{
      width: 27px;
      height: 26.38px;
    }

    .temperature{
      line-height: 21px;
      font-weight: 700;
      color: #323233;
      font-size: 18px;
      letter-spacing: 0.96px;
    }

    .date{
      line-height: 14px;
      font-weight: 700;
      color: #999999;
      font-size: 12px;
      letter-spacing: 0.64px;
    }
  }
}

.weather-icon {
  width: 48px;
  height: 48px;
  margin-right: 16px;
  display: flex;
  align-items: center;
  justify-content: center;

  svg {
    width: 100%;
    height: 100%;
  }
}

.weather-info {
  flex: 1;
  margin-bottom: 10px;

  .greeting {
    line-height: 22px;
    font-weight: 500;
    color: #323233;
    font-size: 16px;
  }
}

.status-cards {
  display: flex;
  gap: 8px;
}

.status-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
  justify-content: center;
  height: 88px;
  background: #fddedb;
  border-radius: 4px;

  &.pending {
    .status-number {
      color: rgba(254, 69, 90, 1);
    }
    background: rgba(253, 222, 219, 0.2);
  }

  &.approved {
    .status-number {
      color: rgba(30, 225, 0, 1);
    }
    background: rgba(30, 225, 0, 0.2);
  }

  &.all {
    .status-number {
      color: rgba(47, 105, 248, 1);
    }
    background: rgba(229, 243, 254, 0.2);
  }
}

.status-number {
  font-size:28px;
  font-weight: 700;
}

.status-label {
  font-size: 14px;
  color: #000000;
}

.section-title {
  line-height: 24px;
  color: #969799;
  font-size: 12px;
}

.functions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  background-color: #FFFFFF;
  border-radius: 12px;
  padding: 16px 0;
}

.function-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    margin-bottom: 12px;
}

.function-icon {
  width: 40px;
  height: 40px;
  margin-bottom: 8px;
  display: block;
  object-fit: contain;
}

.function-label {
  line-height: 16px;
  color: #323233;
  font-size: 12px;
  letter-spacing: 0.82px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  margin-top: 24px;
}

.more-link {
  line-height: 16px;
  color: #1a53ff;
  font-size: 12px;
}

// 热门设备区域
.devices-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  background-color: #FFFFFF;
  border-radius: 12px;
  padding: 0 16px;
}

.device-item {
  display: flex;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid #ebedf0;

  &:last-child {
    border-bottom: none;
  }
}

.device-icon {
  width: 15.99px;
  height: 15px;
  margin-right: 8px;
  object-fit: contain;
}

.device-name {
  flex: 1;
  line-height: 19px;
  color: #333333;
  font-size: 14px;
  letter-spacing: 0.75px;
}

.play-btn {
  display: flex;
  .play-icon {
    width: 58px;
    height: 30px;
    border-radius: 4px;
}
}

// 我的待审批区域
.approval-section {
  border-radius: 12px;
  margin-bottom: 20px;
}

.approval-table {
  display: flex;
  flex-direction: column;
}

.approval-header {
  display: flex;
  padding: 10px 12px;
  border-bottom: 1px solid #ebedf0;
  background: rgba(250, 250, 250, 1);
  line-height: 18px;
  color: #333333;
  font-size: 14px;
}

.header-cell {
  &.device-name-header {
    flex: 1;
    text-align: left;
  }

  &.applicant-header {
    flex: 1;
    text-align: center;
  }

  &.operation-header {
    flex: 1;
    text-align: right;
  }
}

.approval-item {
  display: flex;
  align-items: center;
  padding: 22px 12px;
  background: #ffffff;
  border-bottom: 1px solid #ebedf0;

  &:last-child {
    border-bottom: none;
  }
}

.approval-cell {
  font-size: 14px;
  color: #323233;

  &.device-name-cell {
    flex: 1;
    text-align: left;
  }

  &.applicant-cell {
    flex: 1;
    text-align: center;
  }

  &.operation-cell {
    flex: 1;
    text-align: right;
    display: flex;
    justify-content: flex-end;
    gap: 16px;
  }
}

.approve-btn {
  color: rgba(26, 83, 255, 1);
  font-size: 14px;
  cursor: pointer;

  &:active {
    opacity: 0.7;
  }
}

.reject-btn {
  color: rgba(246, 46, 46, 1);
  font-size: 14px;
  cursor: pointer;

  &:active {
    opacity: 0.7;
  }
}

</style>
