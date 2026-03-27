<template>
  <div class="marker-list">
    <LMarker
      v-for="worker in markers"
      :key="worker.id"
      :lat-lng="worker.position"
      :icon="getMarkerIcon(worker.id)"
      @click="setActive(worker.id)"
      @popupclose="resetIcon(worker.id)"
    >
      <l-popup>
        <WorkerPopup :marker="worker" />
      </l-popup>
    </LMarker>
  </div>
</template>

<script setup>
import { defineProps, ref } from 'vue'
import L from 'leaflet'
import { LMarker, LPopup } from '@vue-leaflet/vue-leaflet'
import WorkerPopup from './WorkerPopup.vue'
import blueIcon from '@/assets/img/maintenanceManagement/blue.png'
import redIcon from '@/assets/img/maintenanceManagement/red.png'
import mapactive from '@/assets/img/smartGraden/map_active.png'
defineProps({
  markers: {
    type: Array,
    required: true
  }
})
// 自定义图标
const customIcon = ref(L.icon({
  // 图标图片的 URL，需替换为实际图片路径
  iconUrl: redIcon,
  // 图标阴影图片的 URL，不需要阴影可设为 null
  shadowUrl: null,
  // 图标大小
  iconSize: [18, 26],
  // 阴影大小
  shadowSize: [24, 24],
  // 图标锚点位置，即图标在地图上的定位点
  iconAnchor: [12, 24],
  // 阴影锚点位置
  shadowAnchor: [12, 24],
  // 弹出框锚点位置，相对于图标锚点的偏移量
  popupAnchor: [0, -24]
}))

// 蓝色图标（已访问过的标记）
const visitedIcon = ref(L.icon({
  // 图标图片的 URL，需替换为实际图片路径
  iconUrl: blueIcon,
  // 图标阴影图片的 URL，不需要阴影可设为 null
  shadowUrl: null,
  // 图标大小
  iconSize: [18, 26],
  // 阴影大小
  shadowSize: [24, 24],
  // 图标锚点位置，即图标在地图上的定位点
  iconAnchor: [12, 24],
  // 阴影锚点位置
  shadowAnchor: [12, 24],
  // 弹出框锚点位置，相对于图标锚点的偏移量
  popupAnchor: [0, -24]
}))

// active 状态图标
const activeIcon = ref(L.icon({
  // active 状态图标图片的 URL，需替换为实际图片路径
  iconUrl: mapactive,
  // 图标阴影图片的 URL，不需要阴影可设为 null
  shadowUrl: null,
  // 图标大小
  iconSize: [24, 24],
  // 阴影大小
  shadowSize: [24, 24],
  // 图标锚点位置，即图标在地图上的定位点
  iconAnchor: [12, 24],
  // 阴影锚点位置
  shadowAnchor: [12, 12],
  // 弹出框锚点位置，相对于图标锚点的偏移量
  popupAnchor: [1, -24]
}))
// 跟踪当前激活的标记 ID
const activeMarkerId = ref(null)
// 跟踪已访问过的标记 ID
const visitedMarkerIds = ref(new Set())

// 判断标记是否处于 active 状态
const isActive = (id) => {
  return activeMarkerId.value === id
}

// 判断标记是否已被访问过
const isVisited = (id) => {
  return visitedMarkerIds.value.has(id)
}

// 获取标记的图标（支持传入自定义 icon）
const getMarkerIcon = (id) => {
  // 优先使用 marker 自带的 icon
  try {
    const marker = (Array.isArray(props.markers) ? props.markers : []).find(m => m && m.id === id)
    if (marker && marker.icon) {
      return marker.icon
    }
  } catch (e) { /* noop */ }

  if (isActive(id)) {
    return activeIcon.value
  } else if (isVisited(id)) {
    return visitedIcon.value
  } else {
    return customIcon.value
  }
}

// 设置激活的标记
const setActive = (id) => {
  activeMarkerId.value = id
  // 标记为已访问
  visitedMarkerIds.value.add(id)
}

// 重置图标的方法
const resetIcon = (id) => {
  if (isActive(id)) {
    activeMarkerId.value = null
  }
}

// 暴露方法给父组件调用
const markAsVisited = (id) => {
  visitedMarkerIds.value.add(id)
}

// 重置所有访问状态
const resetVisitedStatus = () => {
  visitedMarkerIds.value.clear()
}

defineExpose({
  markAsVisited,
  resetVisitedStatus
})
</script>

