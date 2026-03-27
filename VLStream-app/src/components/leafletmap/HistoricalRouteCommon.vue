<template>
  <div class="track-list-container">
    <div class="time-box">
      <div class="time-start" @click="showCalendar('start')">
        {{ startDate }}
      </div>
      <div>-</div>
      <div class="time-end" @click="showCalendar('end')">
        {{ endDate }}
      </div>
    </div>
    <div class="map-box">
      <MapComponent
        is-track-player
        zoom="15"
        height="300px"
        :center="center"
        :active-them-key="activeThemKey"
        :markers="gpsMarkers"
      />
      <div v-if="isPlay" class="replay-btn" @click="startTrack">
        <img class="play" src="@/assets/img/commandDispatch/paly_guiji.png" />
        轨迹回放
      </div>

      <div v-else class="replay-btn" @click="pauseTrack">
        <img class="play" src="@/assets/img/commandDispatch/pause_guiji.png" />
        暂停
      </div>
    </div>
    <div class="list-container">
      <van-steps direction="vertical" :active="currentIndex" class="custom-steps">
        <van-step v-for="(item, index) in trackList" :key="index" class="track-step">
          <!-- 步骤内容 -->
          <div class="step-content">
            <div class="location">
              {{ item.location }}
            </div>
            <div class="address">
              {{ item.address }}
            </div>
            <div class="arrive-time">
              到达时间：{{ item.arriveTime }}
            </div>
            <div v-if="item.stayTime" class="stay-time">
              停留时间：{{ item.stayTime }}
            </div>
            <div v-if="item.noStay" class="no-stay">
              未停留
            </div>
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

    <van-calendar v-model:show="show" type="range" @confirm="onConfirm" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import MapComponent from '@/components/leafletmap/MapComponent.vue'
import bus from '@/utils/bus'

// 定义 props
const props = defineProps({
  trajectoryData: {
    type: Array,
    default: () => []
  },
  type: {
    type: String,
    default: 'single'
  }
})

const currentIndex = ref(1)
const show = ref(false)
const startDate = ref('')
const endDate = ref('')
let selectedType = ref('')
const progressValue = ref(50) // 进度条值
const isPlay = ref(true)

// 计算 GPS 标记点
const gpsMarkers = computed(() => {
  if (!props.trajectoryData || props.trajectoryData.length === 0) {
    return []
  }

  return props.trajectoryData.map((point, index) => ({
    position: [point.latitude || point.lat || 0, point.longitude || point.lng || 0],
    title: `轨迹点 ${index + 1}`,
    timestamp: point.timestamp || point.time || '',
    ...point
  }))
})

// 计算地图中心点
const center = computed(() => {
  if (gpsMarkers.value.length > 0) {
    return gpsMarkers.value[0].position
  }
  return [25.7705, 113.0147] // 默认中心点
})

// 格式化日期函数
const formatDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
// 处理日期选择确认事件
const onConfirm = (date) => {
  show.value = false
  if (selectedType.value === 'start') {
    startDate.value = formatDate(date[0])
  } else if (selectedType.value === 'end') {
    endDate.value = formatDate(date[1])
  }
}
const showCalendar = (type) => {
  selectedType.value = type
  show.value = true
}
// 模拟轨迹数据
const trackList = ref([
  {
    dotColor: 'green',
    location: '11极富大厦B座',
    address: '广东省深圳市福田区松岭路56号',
    arriveTime: '2025.10.12 10:25:22',
    noStay: true,
    highlight: false
  },
  {
    dotColor: 'blue',
    location: '极富大厦B座',
    address: '广东省深圳市福田区松岭路56号',
    arriveTime: '2025.10.12 10:25:22',
    stayTime: '20分钟',
    highlight: true
  },
  {
    dotColor: 'blue',
    location: '极富大厦B座',
    address: '广东省深圳市福田区松岭路56号',
    arriveTime: '2025.10.12 10:25:22',
    stayTime: '20分钟',
    highlight: false
  },
  {
    dotColor: 'red',
    location: '极富大厦B座',
    address: '广东省深圳市福田区松岭路56号',
    arriveTime: '2025.10.12 10:25:22',
    stayTime: '20分钟',
    highlight: false
  }
])

// 处理进度条变化
const onProgressChange = (value) => {
  // 根据进度值更新当前步骤索引
  const stepIndex = Math.round((value / 100) * (trackList.value.length - 1))
  currentIndex.value = stepIndex
}
const startTrack = () => {
  isPlay.value = false
  bus.$emit('playbackTrack')
}
const pauseTrack = () => {
  isPlay.value = true
  bus.$emit('pauseTrack')
}
onMounted(() => {
  // 初始化开始和结束日期
  startDate.value = formatDate(new Date())
  endDate.value = formatDate(new Date())
})
</script>

<style scoped lang="scss">
track-list-container {
  background-color: #f7f7f7;
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
}

.time-start,
.time-end {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 152px;
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
  img{
    margin-right: 8px;
  }
}

.list-container {
  /* height: calc(100vh - 300px - 45px - 70px); */
  /* overflow: auto; */
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
</style>
