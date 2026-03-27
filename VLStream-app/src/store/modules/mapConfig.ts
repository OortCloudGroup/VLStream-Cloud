import { defineStore } from 'pinia'
import { ref } from 'vue'
import config from '@/config'

export interface BaseMapOption {
  url: string
  type: 'WMS' | 'TILE'
  layer?: string
}

export interface MapConfig {
  baseMapOptions: BaseMapOption[]
  center: [number, number]
  zoom: number
  minZoom: number
  maxZoom: number
}

export const useMapConfigStore = defineStore('mapConfig', () => {
  // 底图配置
  const baseMapOptions = ref<BaseMapOption[]>([
    {
      url: '/' + config.gateWay + 'apaas-tile-server/china-dark/{z}/{x}/{y}.png',
      type: 'TILE',
      layer: 'ChinaMap_World'
    }
  ])

  // 地图中心点 -北京中心点
  const center = ref<[number, number]>([39.914889, 116.403874])

  // 缩放级别
  const zoom = ref(5)
  const minZoom = ref(1)
  const maxZoom = ref(10)

  const baseMapArray = ref([])
  // 设置底图配置
  const setBaseMapArray = (options) => {
    baseMapOptions.value = options
    baseMapArray.value = options
  }

  // 设置地图中心点
  const setCenter = (newCenter: [number, number]) => {
    center.value = newCenter
  }

  // 设置缩放级别
  const setZoom = (newZoom: number) => {
    zoom.value = newZoom
  }

  // 设置最小缩放级别
  const setMinZoom = (newMinZoom: number) => {
    minZoom.value = newMinZoom
  }

  // 设置最大缩放级别
  const setMaxZoom = (newMaxZoom: number) => {
    maxZoom.value = newMaxZoom
  }

  // 更新整个地图配置
  const updateMapConfig = (config: Partial<MapConfig>) => {
    if (config.baseMapOptions) {
      baseMapOptions.value = config.baseMapOptions
    }
    if (config.center) {
      center.value = config.center
    }
    if (config.zoom !== undefined) {
      zoom.value = config.zoom
    }
    if (config.minZoom !== undefined) {
      minZoom.value = config.minZoom
    }
    if (config.maxZoom !== undefined) {
      maxZoom.value = config.maxZoom
    }
  }

  // 重置为默认配置
  const resetToDefault = () => {
    // 重置为默认配置
    baseMapOptions.value = [
      {
        url: '/' + config.gateWay + 'apaas-tile-server/china-dark/{z}/{x}/{y}.png',
        type: 'TILE',
        layer: 'ChinaMap_World'
      }
    ]
    center.value = [39.914889, 116.403874]
    zoom.value = 5
    minZoom.value = 1
    maxZoom.value = 10
  }

  return {
    baseMapArray,
    // 状态
    baseMapOptions,
    center,
    zoom,
    minZoom,
    maxZoom,

    // 方法
    setBaseMapArray,
    setCenter,
    setZoom,
    setMinZoom,
    setMaxZoom,
    updateMapConfig,
    resetToDefault
  }
})
