<template>
  <div id="map-container" @click.capture="warnClick">
    <LMap
      :center="center"
      :zoom="zoom"
      :crs="crs"
      :max-zoom="maxZoom"
      :zoom-control="zoomControl"
      :style="{ width: '100%', height: height }"
      @ready="onMapInit"
      @click="onMapClick"
    >
      <WmsLayer :theme-options="themeOptions" :active-them-key="activeThemKey" />
      <MarkerList ref="markerListRef" :markers="markers" />
      <slot />
    </LMap>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import L from 'leaflet'
import { LMap } from '@vue-leaflet/vue-leaflet'
import 'leaflet-trackplayer'
import WmsLayer from './WmsLayer.vue'
import MarkerList from './MarkerList.vue'
import 'leaflet/dist/leaflet.css'
import cameraActiveIconImg from '@/assets/img/commandDispatch/start.png'
import carMove from '@/assets/img/maintenanceManagement/blue.png'
import endPointIconImg from '@/assets/img/commandDispatch/end.png'
import bus from '@/utils/bus'
const emits = defineEmits(['warnClick', 'mapClick'])
const track = ref(null)
const currentPointIndex = ref(0)
const isPlaying = ref(false)
const playTimer = ref(null)
const isInitialized = ref(false)
const markerListRef = ref(null)
// 记录起点/终点图层以便清理
const startMarkerLayer = ref(null)
const endMarkerLayer = ref(null)
const props = defineProps({ height: {
  type: String,
  default: 'calc(100vh - 106px)'
},
zoom: {
  type: Number,
  default: 15
},
zoomControl: { // 是否携带缩放按钮
  type: Boolean,
  default: false
},
center: {
  type: Array,
  default: () => [22.562051, 114.126417]
},
markers: {
  type: Array,
  default: () => []
},
activeThemKey: {
  type: String,
  default: 'standard'
},
maxZoom: { // 缩放到多少级
  type: Number,
  default: 17
},
isTrackPlayer: {
  type: Boolean,
  default: false
},
// 已行驶轨迹部分的颜色
passedLineColor: {
  type: String,
  default: '#3476FC'
},
// 未行驶轨迹部分的颜色
notPassedLineColor: {
  type: String,
  default: '#3476FC'
},
markerRotation: {
  type: Boolean,
  default: false
}
})
const leafLetMap = ref(null)
// 后台用EPSG:4326 需要转(WGS84 地理坐标)
const crs = ref(L.CRS.EPSG3857)

// 定义终点图标
const endPointIcon = L.icon({
  iconSize: [25, 25],
  iconUrl: new URL(endPointIconImg, import.meta.url).href,
  iconAnchor: [-7.5, 12.5]
})

// 定义起点图标
const startPointIcon = L.icon({
  iconSize: [25, 25],
  iconUrl: new URL(cameraActiveIconImg, import.meta.url).href,
  iconAnchor: [31.5, 12.5]
})
const onMapInit = (map) => {
  leafLetMap.value = map
  if (!props.zoomControl) {
    leafLetMap.value.zoomControl.remove()
  }
  // 修正容器尺寸导致的缩放计算偏差
  setTimeout(() => {
    try { leafLetMap.value.invalidateSize(true) } catch (e) { /* noop */ }
  }, 0)
  initTrackPlayer()
}
let markerIcon = L.icon({
  iconSize: [25, 25],
  iconUrl: new URL(carMove, import.meta.url).href,
  iconAnchor: [12.5, 22.5],
  rotationAngle: 90,
  rotationOrigin: 'center'
})
// 轨迹坐标
const pathCoordinates = computed(() => {
  if (props.markers && props.markers.length > 0) {
    return props.markers.map(marker => marker.position)
  }
  return []
})
// 初始化轨迹TrackPlayer播放器
const initTrackPlayer = () => {
  if (!leafLetMap.value || !props.isTrackPlayer || pathCoordinates.value.length === 0) {
    return
  }
  if (track.value) {
    leafLetMap.value.removeLayer(track.value)
  }
  // 保持原有样式
  track.value = new L.TrackPlayer(pathCoordinates.value, {
    markerIcon,
    passedLineColor: props.passedLineColor,
    notPassedLineColor: props.notPassedLineColor,
    markerVisible: true,
    markerRotation: props.markerRotation,
    weight: 8,
    lineOpacity: 0.8,
    panTo: false,
    play: false
  }).addTo(leafLetMap.value)

  // 初始化时设置进度为0
  if (track.value && track.value.setProgress) {
    setTimeout(() => {
      track.value.setProgress(0)
    }, 100)
  }

  // 添加起点和终点标记
  if (pathCoordinates.value.length > 0) {
    const startCoordinates = pathCoordinates.value[0]
    const endCoordinates = pathCoordinates.value[pathCoordinates.value.length - 1]
    // 先移除旧的起终点图层
    if (startMarkerLayer.value) {
      leafLetMap.value.removeLayer(startMarkerLayer.value)
      startMarkerLayer.value = null
    }
    if (endMarkerLayer.value) {
      leafLetMap.value.removeLayer(endMarkerLayer.value)
      endMarkerLayer.value = null
    }
    // 新增起终点图层并保存引用
    startMarkerLayer.value = L.marker(startCoordinates, { icon: startPointIcon }).addTo(leafLetMap.value)
    endMarkerLayer.value = L.marker(endCoordinates, { icon: endPointIcon }).addTo(leafLetMap.value)
  }
  // 只在首次初始化时调整地图视图
  if (!isInitialized.value) {
    fitMapToTrack()
    isInitialized.value = true
  }
}
// 清理轨迹与起终点
const clearTrackLayers = () => {
  if (!leafLetMap.value) return
  if (track.value) {
    leafLetMap.value.removeLayer(track.value)
    track.value = null
  }
  if (startMarkerLayer.value) {
    leafLetMap.value.removeLayer(startMarkerLayer.value)
    startMarkerLayer.value = null
  }
  if (endMarkerLayer.value) {
    leafLetMap.value.removeLayer(endMarkerLayer.value)
    endMarkerLayer.value = null
  }
  // 兜底：移除所有 Polyline 图层（已行驶/未行驶轨迹线）
  leafLetMap.value.eachLayer((layer) => {
    try {
      if (layer instanceof L.Polyline) {
        leafLetMap.value.removeLayer(layer)
      }
    } catch (e) {
      // 忽略非 Polyline 或第三方图层判断异常
    }
  })
  // 重置播放状态
  isPlaying.value = false
  currentPointIndex.value = 0
  if (playTimer.value) {
    clearTimeout(playTimer.value)
    playTimer.value = null
  }
}
// 调整地图视图以包含所有轨迹点
const fitMapToTrack = () => {
  if (!leafLetMap.value || pathCoordinates.value.length === 0) {
    return
  }
  // 边界
  const bounds = L.latLngBounds(pathCoordinates.value)
  bounds.pad(0.05)
  leafLetMap.value.fitBounds(bounds, {
    padding: [20, 20],
    maxZoom: props.maxZoom
  })
}
watch(() => props.markers, () => {
  if (!leafLetMap.value) return
  // 如果没有任何标记点，则清空地图上的轨迹与起终点
  if (!props.markers || props.markers.length === 0) {
    clearTrackLayers()
    isInitialized.value = false
    return
  }
  // 有数据则重建轨迹
  isInitialized.value = false
  initTrackPlayer()
}, { deep: true })
// 跳跃到下一个点
const jumpToNextPoint = () => {
  if (currentPointIndex.value < pathCoordinates.value.length) {
    currentPointIndex.value++
    // 标记当前点已被访问
    if (markerListRef.value && props.markers[currentPointIndex.value - 1]) {
      const currentMarker = props.markers[currentPointIndex.value - 1]
      markerListRef.value.markAsVisited(currentMarker.id)
    }
    // // 更新 TrackPlayer 进度
    // if (track.value && track.value.setProgress) {
    //   const progress = Math.min(currentPointIndex.value / (pathCoordinates.value.length - 1), 1)
    //   track.value.setProgress(progress)
    // }

    // 更新 TrackPlayer 进度
    if (track.value && track.value.jumpTo) {
      const progress = Math.min(currentPointIndex.value / (pathCoordinates.value.length - 1), 1)
      track.value.jumpTo(progress)
    }

    // 更新进度条
    const progressValue = Math.min((currentPointIndex.value / (pathCoordinates.value.length - 1)) * 100, 100)
    bus.$emit('updateProgress', {
      progress: progressValue,
      currentIndex: currentPointIndex.value
    })
    // 继续跳跃到下一个点
    if (currentPointIndex.value < pathCoordinates.value.length && isPlaying.value) {
      playTimer.value = setTimeout(jumpToNextPoint, 1000) // 每秒跳跃一个点
    } else if (currentPointIndex.value >= pathCoordinates.value.length) {
      // 播放完成
      isPlaying.value = false
      currentPointIndex.value = 0
      // 通知父组件播放完成，更新按钮状态
      bus.$emit('trackPlaybackComplete')
    }
  }
}

const startTrack = () => {
  if (pathCoordinates.value.length === 0) return
  isPlaying.value = true
  currentPointIndex.value = 0
  // 重置标记访问状态
  if (markerListRef.value) {
    markerListRef.value.resetVisitedStatus()
  }
  if (track.value && track.value.setProgress) {
    track.value.setProgress(0)
  }
  // 播放
  jumpToNextPoint()
}

const pauseTrack = () => {
  isPlaying.value = false
  if (playTimer.value) {
    clearTimeout(playTimer.value)
    playTimer.value = null
  }
}

// 更新轨迹进度
const updateTrackProgress = (currentIndex) => {
  if (pathCoordinates.value.length === 0) return

  // 更新当前点索引
  currentPointIndex.value = currentIndex

  // 标记已访问的标记点
  if (markerListRef.value) {
    for (let i = 0; i <= currentIndex && i < props.markers.length; i++) {
      const marker = props.markers[i]
      if (marker) {
        markerListRef.value.markAsVisited(marker.id)
      }
    }
  }

  // 更新 TrackPlayer 进度
  if (track.value && track.value.setProgress) {
    const progress = Math.min(currentIndex / (pathCoordinates.value.length - 1), 1)
    track.value.setProgress(progress)
  }
}

const warnClick = () => {
  emits('warnClick')
}

// 地图点击，向外抛出经纬度
const onMapClick = (e) => {
  try {
    const { latlng } = e
    if (latlng && typeof latlng.lat === 'number' && typeof latlng.lng === 'number') {
      emits('mapClick', latlng)
    }
  } catch (error) {
    // noop
  }
}

onMounted(() => {
  bus.$on('playbackTrack', () => {
    startTrack()
  })
  bus.$on('pauseTrack', () => {
    pauseTrack()
  })
  bus.$on('trackProgressChange', (data) => {
    if (pathCoordinates.value.length > 0 && data.currentIndex < pathCoordinates.value.length) {
      updateTrackProgress(data.currentIndex)
    }
  })
})

onUnmounted(() => {
  bus.$off('playbackTrack')
  bus.$off('pauseTrack')
  bus.$off('trackProgressChange')

  // 清理定时器
  if (playTimer.value) {
    clearTimeout(playTimer.value)
    playTimer.value = null
  }
  // 清理图层
  clearTrackLayers()
})

</script>
<style>
.leaflet-touch .leaflet-control-attribution{
  display: none !important;
}
</style>
