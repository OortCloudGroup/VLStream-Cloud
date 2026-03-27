<template>
  <div id="map-container">
    <LMap
      :center="center"
      :zoom="zoom"
      :crs="crs"
      :zoom-control="zoomControl"
      :style="{ width: '100%', height: height }"
      @ready="onMapInit"
    >
      <WmsLayer :theme-options="themeOptions" />
      <MarkerList :markers="markers" />

      <!-- 已经过的轨迹 -->
      <LPolyline :lat-lngs="passedPoints" :options="passedPolylineStyle" />
      <!-- 未经过的轨迹 -->
      <LPolyline :lat-lngs="upcomingPoints" :options="upcomingPolylineStyle" />
      <slot />
    </LMap>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import L from 'leaflet'
import { LMap, LPolyline } from '@vue-leaflet/vue-leaflet'
import WmsLayer from './WmsLayer.vue'
import MarkerList from './MarkerList.vue'
import 'leaflet/dist/leaflet.css'
const props = defineProps({ height: {
  type: String,
  default: 'calc(100vh - 106px)'
},
zoom: {
  type: Number,
  default: 12
},
zoomControl: {
  type: Boolean,
  default: false
},
center: {
  type: Array,
  default: () => [22.53, 114.06]
},
markers: {
  type: Array,
  default: () => []
}
})
const leafLetMap = ref(null)
// 后台用EPSG:4326 需要转(WGS84 地理坐标)
const crs = ref(L.CRS.EPSG3857)
// 地图相关配置

const themeOptions = ref({
  standard: {
    name: '标准地图',
    url: 'http://oort.oortcloudsmart.com:21410/bus/geoserver/geoserver/test/wms?',
    layer: 'test:chenzhou-standard'
  },
  dark: {
    name: '暗黑主题',
    url: 'http://oort.oortcloudsmart.com:21410/bus/geoserver/geoserver/test/wms?',
    layer: 'test:chenzhou-dark'
  },
  satellite: {
    name: '卫星影像',
    url: 'http://oort.oortcloudsmart.com:21410/bus/geoserver/geoserver/test/wms?',
    layer: 'test:chenzhou-satellite'
  }
})
const onMapInit = (map) => {
  leafLetMap.value = map
  if (!props.zoomControl) {
    leafLetMap.value.zoomControl.remove()
  }
}

const pathCoordinates = ref([
  [25.65, 112.9],
  [25.68, 112.95],
  [25.7, 113.0],
  [25.72, 113.05],
  [25.75, 113.1],
  [25.78, 113.3]
])

// 定时器 ID
// eslint-disable-next-line @typescript-eslint/no-unused-vars
let playbackInterval = null
// 当前标记点位置
const currentPoint = ref(pathCoordinates.value[0])
// 是否暂停
const isPaused = ref(false)

// 是否正在回放
const isPlaying = ref(false)

// 当前播放索引
const currentIndex = ref(0)
// 路径坐标数组，格式为 [纬度, 经度]
// 路径颜色
// const pathColor = ref('#FF0000')

// 已经过的轨迹点
const passedPoints = computed(() => {
  return pathCoordinates.value.slice(0, currentIndex.value + 1)
})

// 未经过的轨迹点
const upcomingPoints = computed(() => {
  return pathCoordinates.value.slice(currentIndex.value)
})

// 已经过轨迹的样式
const passedPolylineStyle = computed(() => ({
  weight: 8,
  color: '#00FF00', // 绿色
  opacity: 0.8,
  lineCap: 'round',
  lineJoin: 'round'
}))

// 未经过轨迹的样式
const upcomingPolylineStyle = computed(() => ({
  weight: 8,
  color: '#FF0000', // 红色
  opacity: 0.8,
  lineCap: 'round',
  lineJoin: 'round'
}))
// 停止回放函数
const stopPlayback = () => {
  if (playbackInterval) {
    clearInterval(playbackInterval)
    playbackInterval = null
  }
  isPlaying.value = false
  isPaused.value = false
}
// 开始回放函数
const startPlayback = () => {
  if (!isPlaying.value && pathCoordinates.value.length > 0) {
    isPlaying.value = true
    isPaused.value = false
    currentIndex.value = 0
    currentPoint.value = pathCoordinates.value[0]
    playbackInterval = setInterval(() => {
      if (
        !isPaused.value &&
        currentIndex.value < pathCoordinates.value.length - 1
      ) {
        currentIndex.value++
        currentPoint.value = pathCoordinates.value[currentIndex.value]
      } else if (currentIndex.value >= pathCoordinates.value.length - 1) {
        stopPlayback()
      }
    }, 1000)
  }
}

onMounted(() => {
  startPlayback()
})

</script>
