<template>
  <div class="track-list-container">
    <div class="time-box">
      <van-cell v-model:show="show" title="请选择日期" :value="date" @click="show = true" />
      <van-calendar v-model:show="show" :type="props.type" :min-date="minDate" @confirm="onConfirm" />
    </div>
    <div class="map-box">
      <MapComponent
        is-track-player
        zoom="15"
        height="300px"
        :center="center"
        :active-them-key="activeThemKey"
        :markers="gpsMarkers"
        passed-line-color="#3476FC"
        not-passed-line-color="#3476FC"
      />
      <div v-if="isPlay" class="replay-btn" @click="startTrack">
        <img class="play" src="@/assets/img/commandDispatch/paly_guiji.png" />
        轨迹回放
      </div>

      <div v-else class="replay-btn" @click="pauseTrack">
        <img class="play" src="@/assets/img/commandDispatch/pause_guiji.png" />
        暂停
      </div>
      <div class="clockIn">
        <div>
          <img src="@/assets/img/maintenanceManagement/blue2.png" class="clockInImg" />
          已打卡
        </div>
        <div>
          <img src="@/assets/img/maintenanceManagement/red2.png" class="clockInImg" />
          未打卡
        </div>
      </div>
    </div>
    <div class="list-container">
      <van-steps direction="vertical" :active="currentIndex" class="custom-steps">
        <van-step v-for="(item, index) in trajectoryData" :key="index" class="track-step">
          <!-- 步骤内容 -->
          <div class="step-content">
            <div class="location">
              {{ item.address }}
            </div>
            <!-- <div class="address">
              {{ item.address }}
            </div> -->
            <div class="arrive-time">
              到达时间：{{ formatDateToHM(item.report_at * 1000) }}
            </div>
            <!-- <div v-if="item.stayTime" class="stay-time">
              停留时间：{{ item.stayTime }}
            </div>
            <div v-if="item.noStay" class="no-stay">
              未停留
            </div> -->
          </div>
        </van-step>
      </van-steps>
    </div>

    <div class="timeline-controller">
      <van-slider
        v-model="progressValue"
        :min="0"
        :max="100"
        bar-height="4px"
        active-color="#3476FC"
        inactive-color="#CCCCCC"
        @change="onProgressChange"
      >
        <template #button>
          <div class="custom-thumb" />
        </template>
      </van-slider>
      <div class="timeline-labels">
        <span>起点</span>
        <span>终点</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import MapComponent from '@/components/trajectory/MapComponent.vue'
import bus from '@/utils/bus'
import { formatDateToHM } from '@/utils/time'
// 定义 props
const props = defineProps({
  type: {
    type: String,
    default: 'range' // range：日期范围  single：单选日期
  },
  trajectoryData: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['dateChange'])
const currentIndex = ref(0)
const show = ref(false)
const startDate = ref('')
const endDate = ref('')
const selectedDate = ref('')
const progressValue = ref(0) // 进度条值
const isPlay = ref(true)
const date = ref('')

// 计算地图中心点
const center = computed(() => {
  if (!props.trajectoryData || props.trajectoryData.length === 0) {
    return [22.562051, 114.126417] // 默认中心点：巴登社区中心点
  }

  // 取起点和终点的中心作为地图中心
  const startPoint = props.trajectoryData[0]
  const endPoint = props.trajectoryData[props.trajectoryData.length - 1]

  const centerLat = (startPoint.lat + endPoint.lat) / 2
  const centerLng = (startPoint.lng + endPoint.lng) / 2

  return [centerLat, centerLng]
})

// 计算GP标记点
const gpsMarkers = computed(() => {
  if (!props.trajectoryData || props.trajectoryData.length === 0) {
    return []
  }

  return props.trajectoryData.map((item, index) => ({
    id: index,
    position: [item.lat, item.lng],
    title: item.address || '未知位置',
    content: item.report_at || '',
    address: item.address,
    report_at: formatDateToHM(item.report_at * 1000)
  }))
})

// 计算当前激活的主题键
const activeThemKey = computed(() => {
  return 'standard'
})

const formatDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
// 处理日期选择确认事件
const onConfirm = (selectedDates) => {
  show.value = false
  // 日期范围
  if (props.type === 'range') {
    startDate.value = formatDate(selectedDates[0])
    endDate.value = formatDate(selectedDates[1])
    date.value = `${startDate.value} 至 ${endDate.value}`
    emit('dateChange', {
      startDate: startDate.value,
      endDate: endDate.value,
      type: 'range'
    })
  } else {
    // 单选日期
    selectedDate.value = formatDate(selectedDates)
    date.value = selectedDate.value
    emit('dateChange', {
      selectedDate: selectedDate.value,
      type: 'single'
    })
  }
}

// 处理进度条变化
const onProgressChange = (value) => {
  const stepIndex = Math.round((value / 100) * (props.trajectoryData.length - 1))
  currentIndex.value = stepIndex

  // 发送进度变化事件到地图组件
  bus.$emit('trackProgressChange', {
    progress: value,
    currentIndex: stepIndex,
    totalPoints: props.trajectoryData.length
  })
}
const startTrack = () => {
  isPlay.value = false
  // 重置进度条
  progressValue.value = 0
  currentIndex.value = 0
  bus.$emit('playbackTrack')
}
const pauseTrack = () => {
  isPlay.value = true
  bus.$emit('pauseTrack')
}
onMounted(() => {
  // 初始化日期
  const today = formatDate(new Date())
  if (props.type === 'range') {
    // 日期范围选择
    startDate.value = today
    endDate.value = today
    date.value = `${startDate.value} 至 ${endDate.value}`
  } else {
    // 单选日期
    selectedDate.value = today
    date.value = selectedDate.value
  }

  // 初始化进度条
  if (props.trajectoryData && props.trajectoryData.length > 0) {
    progressValue.value = 0
    currentIndex.value = 0
  }

  // 监听轨迹播放完成事件
  bus.$on('trackPlaybackComplete', () => {
    isPlay.value = true // 播放完成，按钮变回播放状态
  })

  // 监听进度更新事件
  bus.$on('updateProgress', (data) => {
    progressValue.value = data.progress
    currentIndex.value = data.currentIndex
  })
})
// 监听轨迹数据变化，空数据时重置播放控件，并通知地图清理
watch(() => props.trajectoryData, (newVal) => {
  if (!newVal || newVal.length === 0) {
    // 重置 UI 控件
    isPlay.value = true
    progressValue.value = 0
    currentIndex.value = 0
    // 通知地图暂停并重置进度
    bus.$emit('pauseTrack')
    bus.$emit('updateProgress', { progress: 0, currentIndex: 0 })
  }
}, { deep: true })
const minDate = computed(() => {
  const now = new Date()
  return new Date(now.getFullYear() - 2, now.getMonth(), now.getDate())
})
onUnmounted(() => {
  // 清理事件监听
  bus.$off('trackPlaybackComplete')
  bus.$off('updateProgress')
})
</script>

<style scoped lang="scss">
track-list-container {
  background-color: #f7f7f7;
  position: relative;
}

.track-item {
  display: flex;
  align-items: flex-start;
  position: relative;
  z-index: 2;
}

.time-box {
  padding: 8px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f7f7f7;
  position: relative;
  height: 40px;
  /* position: absolute;
  top: 29.78vw;
  left: 50%;
  transform: translate(-50%, -50%); */
}

.time-start {
  position: absolute;
  left: 10px;
  width: 142px !important;
}

.time-end {
  position: absolute;
  right: 10px;
  width: 142px !important;
}

.time-start,
.time-end {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 42.22vw;
  height: 36px;
  background: #FFF;
  border-radius: px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 10px;
  margin-top: 4px;
}

green-dot .dot {
  background-color: green;
}

.blue-dot .dot {
  background-color: blue;
}

.location {
  font-weight: bold;
  font-size: 16px;
  margin-bottom: 4px;
}

.address {
  color: #666;
  margin-bottom: 4px;
}

.arrive-time {
  color: green;
  margin-bottom: 2px;
}

.stay-time {
  color: green;
}

.no-stay {
  color: green;
}

.highlight {
  border-radius: 4px;

  .content {
    background-color: #e6f7ff;
  }
}

.dot {
  margin-top: px;
}

.track-item {
  .content {
    padding: 16px;
  }
}

.timeline {
  margin-top: 20px;
}

.timeline-bar {
  width: 100%;
  height: 4px;
  background-color: #ccc;
  position: relative;
  border-radius: 2px;
}

.timeline-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 5px;
  color: #999;
}

.replay-btn {
  position: absolute;
  z-index: 1000;
  right: 16px;
  bottom: 16px;
  background: #3476FC;
  border-radius: 20px;
  display: flex;
  align-items: center;
  font-family: SourceHanSansSC-Medium;
  font-size: 14px;
  color: #FFFFFF;
  font-weight: 500;
  padding: 2px;
  padding-right: 28px;

  img {
    margin-right: 8px;
  }
}

.list-container {
  height: calc(100vh - 300px - 45px - 70px);
  overflow: auto;
  border-radius: 12px 12px 0px 0px;
}

.play {
  width: 36px;
}

.map-box {
  position: relative;
}

.track-list {
  position: relative;
  padding-left: 20px;

  .track-item-container {
    position: relative;
  }
}

.custom-steps {
  padding-top: 14px;
}

/* 自定义步骤条连接线为虚线 */
:deep(.van-steps--vertical .van-steps__line) {
  border-left-style: dashed !important;
  border-left-width: 0.5px;
  background-color: rgba(52, 118, 252, 0);
  transform: scaleX(0.8);
}

:deep(.van-step__line) {
  border-left-style: dashed !important;
  border-left-color: rgba(52, 118, 252, 1);
  height: calc(100% - 22px);
  margin-top: 14px;
  background-color: rgba(0, 0, 0, 0) !important;
}

:deep(.van-step__title--active) {
  color: #333333 !important;
  background-color: rgba(2, 122, 255, 0.08);
  border-radius: 8px;
  padding: 12px;
  margin-top: -15px;
}

:deep(.van-step__title) {
  color: #333333 !important;
  font-family: SourceHanSansSC-Regular;
  font-size: 14px;
  color: #333333;
  letter-spacing: 0.43px;
  font-weight: 400;
}

/* 隐藏最后一个步骤的连接线 */
:deep(.van-step:last-child .van-step__line) {
  display: none !important;
}

/* 修改步骤点为同心圆样式 */
:deep(.van-step__circle),
:deep(.van-step__icon--active) {
  width: 12px !important;
  height: 12px !important;
  border: 3px solid #3476FC !important;
  background-color: white !important;
  border-radius: 50% !important;
  position: relative;
  box-sizing: border-box;
}

:deep(.van-step__icon--active) {
  border: 3px solid #03D56D !important;
}

:deep(.van-step:last-child .van-step__circle) {
  border: 3px solid #FF0250 !important;
}

:deep(.van-icon-checked:before) {
  display: none;
}

:deep(.van-step--active .van-step__circle) {
  border-color: #3476FC !important;
  background-color: white !important;
}

/* 激活状态的内部圆点 */
:deep(.van-step--active .van-step__circle)::after {
  background-color: #3476FC;
}

/* 时间轴控制器样式 */
.timeline-controller {
  margin: 0 16px;
  border-radius: 8px;
}

.timeline-labels {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #999;
  font-size: 12px;
}

/* 自定义滑块按钮样式 */
.custom-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #FFFFFF;
  border: 1.6px solid rgba(52, 118, 252, 1);
  box-shadow: 0px 1px 4px 0px rgba(52, 118, 252, 0.39);
  box-sizing: border-box;
}

:deep(.van-cell__title) {
  width: 90px !important;
  min-width: 90px !important;
  max-width: 90px !important;
  flex-shrink: 0 !important;
}

:deep(.van-cell__value) {
  flex: 1;
  text-align: right;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.clockIn {
  position: absolute;
  // width: 150px;
  background: #ffffffb3;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-family: SourceHanSansSC-Regular;
  font-size: 12px;
  color: #333333;
  font-weight: 400;
  right: 16px;
  top: 5px;
  z-index: 999;
  padding: 8px;
  gap: 10px;
}
.clockInImg{
  width: 16px;
  margin-right: 5px;
}
</style>
