<template>
  <div ref="mapRef" class="content_map_body" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { covertCurrentLocationURL } from '@/utils/converGatewayPath'
// 点聚合第三方插件
import 'leaflet.markercluster'
import 'leaflet.markercluster/dist/MarkerCluster.css'
import 'leaflet.markercluster/dist/MarkerCluster.Default.css'

import { getAddressFromCoordinates } from '@/utils/PointChangeAddress'
// import { bd09To3857 } from '@/utils/leafletUtil'
import Config from '@/config/index'

// 为leaflet-geoman添加类型声明
declare global {
  interface Window {
    mapBtnClick: (type: string, ...args: any[]) => void
  }
}

//  当type为WMS时，layer为必传
type BaseMapOptions = {
  type: 'WMS' | 'TILE' // WMS 和 TILE 两种类型 ，  wms, TILE 瓦片
  layer?: string // WMS 才需要layer参数 多个图层逗号分隔
  url: string
}

const props = defineProps({
  baseMapOptions: {
    type: Array as PropType<BaseMapOptions[]>,
    required: true,
    default: () => []
  },
  zoom: {
    type: Number,
    required: false,
    default: 12
  },
  center: {
    type: Array,
    required: false,
    default: () => [25.770160849566686, 113.01551542555586]
  },
  minZoom: {
    type: Number,
    required: false,
    default: 1
  },
  maxZoom: {
    type: Number,
    required: false,
    default: 18
  }
})

// 监听 zoom 和 center 变化
watch(() => props.zoom, () => {
  if (mapInstance.value) {
    if (props.zoom > 0) {
      mapInstance.value.setZoom(props.zoom)
    }
  }
})
watch(() => props.center, () => {
  if (mapInstance.value) {
    if (props.center && props.center.length > 0) {
      mapInstance.value.setView(props.center)
    }
  }
})

// 绘制工具相关变量
// const currentDrawType = ref('')

const featureGroup = reactive({
  Area: L.featureGroup(),
  Draw: L.featureGroup()
})

// 处理地图容器尺寸变化
const handleMapResize = () => {
  if (mapInstance.value) {
    setTimeout(() => {
      (mapInstance.value as any).invalidateSize()
    }, 100)
  }
}

const emit = defineEmits(['mapReady', 'mapClick', 'selectResult', 'mapMove'])

const mapRef = ref(null)
const mapInstance = ref(null)
// 存储当前底图图层的数组
const currentBaseLayers = ref([])

// 更新底图图层
const updateBaseLayers = () => {
  if (!mapInstance.value) return
  // 设置地图中心点
  mapInstance.value.setView(props.center)
  // 设置地图缩放级别
  mapInstance.value.setZoom(props.zoom)
  // 最大缩放
  mapInstance.value.setMaxZoom(props.maxZoom)
  // 最小缩放
  mapInstance.value.setMinZoom(props.minZoom)
  // 清除当前所有底图图层
  currentBaseLayers.value.forEach(layer => {
    mapInstance.value.removeLayer(layer)
  })
  currentBaseLayers.value = []

  // 设置地图中心点
  if (props.center && props.center.length > 0) {
    mapInstance.value.setView(props.center)
  }
  // 设置地图缩放级别
  if (props.zoom > 0) {
    mapInstance.value.setZoom(props.zoom)
  }

  // 添加新的底图图层
  if (props.baseMapOptions && props.baseMapOptions.length > 0) {
    props.baseMapOptions.forEach(item => {
      // 处理item.url 替换为 如果是http 开头 则不替换， 如果不是这加上 config.URL +
      item.url = item.url.startsWith('http') ? item.url : Config.URL + item.url
      let layer = null
      if (item.type === 'WMS') {
        layer = L.tileLayer.wms(item.url, {
          layers: item.layer,
          format: 'image/png',
          transparent: true,
          detectRetina: false
        })
      } else if (item.type === 'TILE') {
        layer = L.tileLayer(item.url, {
          format: 'image/png',
          transparent: true,
          detectRetina: false
        })
      }

      if (layer) {
        mapInstance.value.addLayer(layer)
        currentBaseLayers.value.push(layer)
      }
    })
  }
}

// 监听baseMapOptions变化
watch(() => props.baseMapOptions, () => {
  updateBaseLayers()
}, { deep: true })

const initMap = () => {
  // const { zoom, center, minZoom, maxZoom } = props
  mapInstance.value = L.map(mapRef.value, {
    // center: [22.557418320604025, 114.115564440425],// 深圳
    // center: center, // 彬州
    // zoom: zoom,
    // minZoom: minZoom,
    // maxZoom: maxZoom,
    zoomControl: false,
    fullscreenControl: false,
    crs: L.CRS.EPSG3857,
    attributionControl: false
  }).fitWorld()
  mapInstance.value.on('locationerror', (e) => { console.log('locationerror-----', e) })

  ;(mapInstance.value as any)?.whenReady(() => {
    emit('mapReady')
    // 初始化底图
    updateBaseLayers()
  })
  mapInstance.value.on('click', (e) => {
    console.log('map click-----', e)
    emit('mapClick', e)
    if (isDrawing.value) {
      markerDrawToMap(e)
    }
  })

  // 地图移动触发移动事件并返回地图中心点
  mapInstance.value.on('move', () => {
    const center = mapInstance.value.getCenter()
    emit('mapMove', {
      center: {
        lng: center.lng,
        lat: center.lat
      },
      zoom: mapInstance.value.getZoom()
    })
  })

  ;(mapInstance.value as any).on('zoomend', () => {
    // console.log('缩放结束，最终层级：', mapInstance.value.getZoom())
    // 优化：只在层级变化时进行添加/移除，避免重复操作
    const zoom = (mapInstance.value as any).getZoom()
    const group = featureGroup.Area
    if (!group) return
    const shouldShow = zoom > 15
    const isShown = (mapInstance.value as any).hasLayer(group)
    if (shouldShow && !isShown) {
      (mapInstance.value as any).addLayer(group)
    } else if (!shouldShow && isShown) {
      (mapInstance.value as any).removeLayer(group)
    }
  })

  // // 添加 114.126417,22.562051  测试中心点 中心点
  // L.marker(bd09To3857([114.126417, 22.562051]), {
  //   icon: L.icon({
  //     iconUrl: gpIcon,
  //     iconSize: [24, 24]
  //   })
  // }).addTo(mapInstance.value)
  // setTimeout(() => {
  //   mapInstance.value.panTo([25.76847430996533, 113.02884578704835])
  // }, 1000)
}

const markerDrawToMap = (e) => {
  featureGroup.Draw.clearLayers()
  const latlng = e.latlng
  const marker = L.marker(latlng, {
    icon: L.icon({
      iconUrl: point_icon,
      iconSize: [24, 36],
      iconAnchor: [12, 36]
    })
  })
  featureGroup.Draw.addLayer(marker)
  // 获取地址,并显示在下方
  getAddressFromCoordinates(latlng.lng, latlng.lat).then(res => {
    // 创建divIcon作为label
    const label = L.marker([latlng.lat, latlng.lng], {
      icon: L.divIcon({
        className: 'segment-label',
        html: `<div style="background: rgba(0,0,0,0.7); color: #fff; padding: 2px 6px; border-radius: 3px; font-size: 10px;min-width: 180px;">${res.address}(${latlng.lat.toFixed(5)},${latlng.lng.toFixed(5)})</div>`,
        iconSize: [80, 24],
        iconAnchor: [40, 0]
      }),
      interactive: false
    })
    featureGroup.Draw.addLayer(label)
    emit('selectResult', { type: 'point', result: { latitude: latlng.lat, longitude: latlng.lng,
      address: res.address, province: res.province, city: res.city, county: res.county, street: res.street }})
  })
}

/** 创建要素图层组
 */
const createFeatureGroup = ({ id }) => {
  featureGroup[id] = L.featureGroup()
  featureGroup[id].addTo(mapInstance.value)
}

import defaultIcon from '@/assets/img/ditu/default.png'

const highLightIcon = {} // 高亮图标的集合
const defaultFeatureIcon = {} // 默认图标的集合

/** 将geoJSON图层添加到要素组图层
 * @param {Object} params 参数
 * @param {string} params.id 要素组id
 * @param {Array} params.geoJSONArray 要素数组，每个元素对象包含如下属性：
 *   @param {Object} geoJSON geojson数据
 *   @param {string} icon 要素图标
 *   @param {Function} popup 要素弹窗
 *   @param {Object} Options  { minWidth: clacPXToVW(238), maxWidth: clacPXToVW(238) 参考leaflet.js的Popup 弹出窗口  } 要素弹窗参数
 *   @param {Object|Function} style 要素样式
 *   @param {Function} onEachFeature 要素点击事件
 *   @param {Function} coordsToLatLng 要素坐标转换
 *   @param {Function} pointToLayer 要素点转换
 */
const addGeoJSONToFeatureGroup = ({ id, geoJSONArray = [], clearOther = false }) => {
  // 是否清理其他
  if (clearOther) {
    for (let key in featureGroup) {
      if (key !== id) {
        featureGroup[key].clearLayers()
      }
    }
  }
  if (!featureGroup[id]) {
    createFeatureGroup({ id })
  }
  // 清理图层
  featureGroup[id].clearLayers()
  if (!geoJSONArray || geoJSONArray.length === 0) {
    return
  }

  // 原生聚合
  const markerClusterGroup = L.markerClusterGroup({ maxClusterRadius: 40 }) // 聚合的像素半径， 值越大，聚合的距离就越大即聚合的点就越多
  geoJSONArray.forEach(item => {
    if (!item.popup) {
      item.popup = getHtmlDefaultTemplate
    }
    if (!item.style) {
      item.style = { color: 'green', weight: 2, opacity: 0.7, fillOpacity: 0.1 }
    }
    // 统一处理 icon的地址
    item.icon = covertCurrentLocationURL(item.icon) || defaultIcon
    item.highlightIcon = covertCurrentLocationURL(item.highlightIcon) || defaultIcon
    if (!item.icon) {
      item.icon = defaultIcon
    }
    if (!item.highlightIcon) {
      item.highlightIcon = defaultIcon
    }
    const geoJSONLayer = L.geoJSON(null, {
      coordsToLatLng: function(coords) {
        if (item.coordsToLatLng) {
          item.coordsToLatLng(coords)
        } else {
          let jd = coords[0]
          let wd = coords[1]
          // 校验合法的经纬度 使用最大矩形范围（经度73.66°~135.08°，纬度3.86°~53.55°）快速筛选
          if (jd > 73.66 && jd < 135.08 && wd > 3.86 && wd < 53.55) {
            return L.latLng(wd, jd)
          } else {
            return L.CRS.EPSG3857.unproject(L.point(coords[0], coords[1]))
          }
        }
      },
      pointToLayer: (feature, latlng) => {
        if (item.pointToLayer) {
          return item.pointToLayer(feature, latlng)
        } else {
          let nameSpanhtmlStr = ''
          if (feature.properties.name) {
            nameSpanhtmlStr = '<span>' + feature.properties.name + '</span>'
          }
          let myIcon = L.divIcon({
            html: '<div class="self_marker_icon"><img src="' + item.icon + '" alt="">' + nameSpanhtmlStr + '</div>',
            iconSize: [30, 30]
          })
          let t = L.marker(latlng, {
            icon: myIcon || L.icon({
              iconUrl: item.icon,
              iconSize: [30, 30]
            })
          })
          return t
        }
      },
      highlightIcon: item?.highlightIcon,
      icon: item?.icon,
      highlightStyle: item?.highlightStyle,
      style: item?.style,
      onEachFeature: (feature, layer) => {
        if (item.onEachFeature) {
          item.onEachFeature(feature, layer)
        }
        //  存储唯一标识
        layer.featureId = feature.properties.id
        // 保存高亮图标
        highLightIcon[feature.properties.id] = item.highlightIcon
        // 默认高亮图标
        defaultFeatureIcon[feature.properties.id] = item.icon
        // 线 多边形的popup
        // 高亮图标
        // layer.on('mouseover', () => {
        //   layer.setIcon && layer.setIcon(L.icon({
        //     iconUrl: item.highlightIcon || defaultIcon
        //   }))
        // })
        // layer.on('mouseout', () => {
        //   layer.setIcon && layer.setIcon(L.icon({
        //     iconUrl: item.icon
        //   }))
        // })
        layer.on('click', () => {
          // 触发点击要素时间
          bus.$emit('featureClick', layer)
          const feature = layer.feature
          let nameSpanhtmlStr = ''
          if (feature.properties.name) {
            nameSpanhtmlStr = '<span>' + feature.properties.name + '</span>'
          }
          let layerHighlightIcon = covertCurrentLocationURL(feature.properties.highLightIcon) || defaultIcon
          let myIcon = L.divIcon({
            html: '<div class="self_marker_icon"><img src="' + layerHighlightIcon + '" alt="">' + nameSpanhtmlStr + '</div>',
            iconSize: [30, 30]
          })

          layer.setIcon && layer.setIcon(myIcon)
          layer.setStyle && layer.setStyle(item.highlightStyle)
        })
        layer.on('popupclose', () => {
          bus.$emit('featurePopupClose', layer)
          // 点击其他popupclose 关闭  恢复原icon
          const feature = layer.feature
          let nameSpanhtmlStr = ''
          if (feature.properties.name) {
            nameSpanhtmlStr = '<span>' + feature.properties.name + '</span>'
          }
          let iconT = covertCurrentLocationURL(feature.properties.icon) || defaultIcon
          let myIcon = L.divIcon({
            html: '<div class="self_marker_icon"><img src="' + iconT + '" alt="">' + nameSpanhtmlStr + '</div>',
            iconSize: [30, 30]
          })
          layer.setIcon && layer.setIcon(myIcon)
          layer.setStyle && layer.setStyle(item.style)
        })
      }
    })
    geoJSONLayer.addData(item.geoJSON)
    geoJSONLayer.addTo(markerClusterGroup)
    markerClusterGroup.addTo(featureGroup[id])
  })
}

// 隐藏或者收起要素图层
const toggleFeatureGroup = (type) => {
  if ((mapInstance.value as any).hasLayer(featureGroup[type])) {
    (mapInstance.value as any).removeLayer(featureGroup[type])
  } else {
    (mapInstance.value as any).addLayer(featureGroup[type])
  }
}

// 清除指定图层的所有标记点
const clearFeatureGroup = (type) => {
  if (featureGroup[type]) {
    featureGroup[type].clearLayers()
  }
}

import { getHtmlDefaultTemplate } from '@/utils/getBindPopupHtmlStr.ts'

/**
 * 移动到指定要素
 * @param {Object} param0 参数对象
 * @param {Object} param0.geometry 要素的几何信息
 * @param {string} param0.geometry.type 要素类型 Point, MultiPolygon
 * @param {Array} param0.geometry.coordinates 要素坐标 点：[经度, 纬度] 面：[[[经度, 纬度], [经度, 纬度], [经度, 纬度], [经度, 纬度]]]
 * @param {Object} param0.properties 要素属性
 * @param {string} param0.properties.id 要素id  必需标识要素的唯一性
 * @param {string} param0.properties.name 要素名称
 */
const moveToFeature = ({ geometry, properties }) => {
  // WGS84 GCJ02 BD09
  let jd = null
  let wd = null
  // 点
  if (geometry.type === 'Point') {
    let jwd = geometry.coordinates
    jd = jwd[0]
    wd = jwd[1]
  }
  // 面
  if (geometry.type === 'MultiPolygon') {
    let jwd = geometry.coordinates[0][0][0]
    jd = jwd[0]
    wd = jwd[1]
  }
  if (jd && wd) {
    // 将经纬度转换为3857坐标系
    // 校验合法的经纬度 使用最大矩形范围（经度73.66°~135.08°，纬度3.86°~53.55°）快速筛选
    if (jd < 73.66 || jd > 135.08 || wd < 3.86 || wd > 53.55) {
      let pt = L.CRS.EPSG3857.unproject(L.point(jd, wd))
      mapInstance.value.panTo(pt, { animate: true, duration: 1 })
    } else {
      mapInstance.value.panTo([wd, jd], { animate: true, duration: 1 })
    }
  }
  // TODO 高亮元素的操作
  highlightFeature(properties.id)
}

const highlightFeature = (featureId) => {
  // 先重置所有要素的样式
  resetHighlight()
  // 遍历geoJSONLayer的每个图层
  for (let key in featureGroup) {
    // 三层  图层组下面 图层 ，图层下面的要素
    featureGroup[key].eachLayer(function(layer) {
      layer.eachLayer(function(layerItem) {
        if (layerItem.featureId === featureId) {
          layerItem.setStyle && layerItem.setStyle(layerItem?.options?.highlightStyle || {})
          if (layerItem?.options?.highlightIcon || highLightIcon[layerItem.featureId]) {
            layerItem.setIcon && layerItem.setIcon(L.icon({
              iconUrl: layerItem?.options?.highlightIcon || highLightIcon[layerItem.featureId],
              iconSize: [30, 30]
            }))
          } else {
            layerItem.setIcon && layerItem.setIcon(L.icon({
              iconUrl: defaultIcon,
              iconSize: [30, 30]
            }))
          }
        }
      })
    })
  }
}
// // 重置所有要素的样式
const resetHighlight = () => {
  for (let key in featureGroup) {
    featureGroup[key].eachLayer(function(layer) {
      layer.eachLayer(function(layerItem) {
        layerItem.setStyle && layerItem.setStyle(layerItem?.defaultOptions?.style || {})
        layerItem.setIcon && layerItem.setIcon(L.icon({
          iconUrl: defaultFeatureIcon[layerItem.featureId],
          iconSize: [30, 30]
        }))
      })
    })
  }
}

// const paintArea = (gridname, coordinates, color, centroid) => {
//   // console.log('gridname-----', gridname)
//   // console.log('coordinates-----', coordinates)
//   // console.log('color-----', color)
//   // console.log('centroid-----', centroid)
//   let latlngs = coordinates.map(item => { return bd09To3857(item) })
//   // 虚线 颜色 透明度
//   L.polygon(latlngs, {
//     color: color,
//     weight: 2,
//     opacity: 1,
//     fillColor: '#ffffff00',
//     fillOpacity: 0.1,
//     dashArray: '6, 6'
//   }).addTo(featureGroup.Area)

//   // gridname 添加到图上 字体为白色
//   L.marker(bd09To3857(centroid), {
//     icon: L.divIcon({
//       className: 'my-div-icon',
//       html: `<div style="color: white; font-size: 12px;">${gridname}</div>`
//     })
//   }).addTo(featureGroup.Area)
// }

// const paintAllArea = (data) => {
//   if (data) {
//     data.forEach(item => {
//       paintArea(item.gridname, item.coordinates, item.color, item.centroid)
//     })
//   }
// }

// 添加一段轨迹
import { antPath } from 'leaflet-ant-path'
import start_icon from '@/assets/img/ditu/start.png'
import end_icon from '@/assets/img/ditu/end.png'
import point_icon from '@/assets/img/ditu/gj_location.png'

/**
 * 在地图上添加一段轨迹路径，并可选地添加起点终点标记、轨迹动画等效果
 * @param {Array} route 轨迹点数组，格式为经纬度坐标数组
 * @param {Object} options 配置项
 * @param {String} options.layerGroupId 图层组ID，默认为'trace'
 * @param {Boolean} options.pointDance 是否开启轨迹点跳动动画，默认为false
 * @param {Number} options.pointDanceTimeer 轨迹点跳动动画的间隔时间（毫秒），默认为1500
 * @param {Boolean} options.isLoop 是否循环播放轨迹动画，默认为false
 */
const addPathToMap = (route, { layerGroupId = 'trace', pointDance = false, pointDanceTimeer = 1500, isLoop = false, showPointNumber = false }) => {
  if (!featureGroup[layerGroupId]) {
    createFeatureGroup({ id: layerGroupId })
  }
  // 清理图层
  featureGroup[layerGroupId].clearLayers()
  // 将route 转换为 3857 坐标系
  // let route3857 = route.map(item => { return bd09To3857(item) })
  let startMarker = L.marker(route[0], {
    icon: L.icon({
      iconUrl: start_icon,
      iconSize: [24, 24]
    })
  })
  let endMarker = L.marker(route[route.length - 1], {
    icon: L.icon({
      iconUrl: end_icon,
      iconSize: [24, 24]
    })
  })

  const bluePath = antPath(route, {
    'delay': 2000,
    'dashArray': [10, 50],
    'weight': 8,
    'color': '#279c62', // 线条背景色
    'pulseColor': '#ffffff',
    'paused': false,
    'reverse': false,
    'hardwareAccelerated': true
  })
  featureGroup[layerGroupId].addLayer(bluePath)

  // console.log('featureGroup[layerGroupId]---------', featureGroup[layerGroupId])
  featureGroup[layerGroupId].addLayer(startMarker)
  featureGroup[layerGroupId].addLayer(endMarker)
  if (showPointNumber) {
  // 添加中间路径点数字标记（从2开始）
    route.slice(1, -1).forEach((point, index) => {
      const pointNumber = index + 2 // 从2开始计数
      const pointMarker = L.marker(point, {
        icon: L.divIcon({
          className: 'path-point-marker',
          html: `<div class="path-point-number">${pointNumber}</div>`,
          iconSize: [16, 16],
          iconAnchor: [12, 12]
        })
      })
      featureGroup[layerGroupId].addLayer(pointMarker)
    })
  }
  // 计算轨迹的边界范围
  const bounds = L.latLngBounds(route)
  // 为边界添加一些内边距，确保轨迹不会贴边显示
  const paddedBounds = bounds.pad(0.1) // 添加10%的内边距
  // 将地图缩放到轨迹的完整范围
  mapInstance.value.fitBounds(paddedBounds, {
    animate: true,
    duration: 1,
    maxZoom: props.maxZoom || 18, // 限制最大缩放级别，避免过度放大
    padding: [20, 20] // 添加额外的内边距
  })

  // 轨迹上的点跳动
  if (pointDance) {
    const marker = L.marker(route[0], { icon: L.icon({ iconUrl: point_icon, iconSize: [24, 36] }) })
    featureGroup[layerGroupId].addLayer(marker)

    let index = 0
    let timer = setInterval(() => {
      index++
      route[index] && marker.setLatLng(route[index])
      if (index >= route.length) {
        // 移除标记
        featureGroup[layerGroupId].removeLayer(marker)
        clearInterval(timer)
        if (isLoop) {
          index = 0
          timer = setInterval(() => {
            marker.setLatLng(route[index])
            index++
          }, pointDanceTimeer)
        }
      }
    }, pointDanceTimeer)
  }
}

// 初始化事件
import bus from '@/utils/bus'
const initEvent = () => {
  // 移动到指定要素
  bus.$on('moveToFeature', moveToFeature)
  // 创建要素组
  bus.$on('createFeatureGroup', createFeatureGroup)
  // 隐藏或者收起要素图层
  bus.$on('toggleFeatureGroup', toggleFeatureGroup)
  // 添加要素组
  bus.$on('addGeoJSONToFeatureGroup', addGeoJSONToFeatureGroup)
  // 清除指定图层的所有标记点
  bus.$on('clearFeatureGroup', clearFeatureGroup)

  bus.$on('clickLeftInitMap', clickLeftInitMap)

  // 监听地图容器尺寸变化事件
  bus.$on('mapResize', handleMapResize)

  // 监听轨迹添加事件
  bus.$on('addPathToMap', addPathToMap)

  // 绘制相关事件
  bus.$on('startDraw', startDraw)
}
const clickLeftInitMap = () => {
  initMap()
}
// 注销事件
const unInitEvent = () => {
  bus.$off('moveToFeature', moveToFeature)
  bus.$off('createFeatureGroup', createFeatureGroup)
  bus.$off('toggleFeatureGroup', toggleFeatureGroup)
  bus.$off('clearFeatureGroup', clearFeatureGroup)

  // 注销地图容器尺寸变化事件
  bus.$off('mapResize', handleMapResize)

  // 注销轨迹添加事件
  bus.$off('addPathToMap', addPathToMap)
}

const isDrawing = ref(false)
/**
 * 处理绘制结束后的结果，根据不同类型的图层（线、多边形、圆、点、多多边形）计算并格式化结果，
 * 并通过事件将结果传递给父组件。
 * @param {Object} e - 事件对象，包含绘制的图层和类型等信息
 * @param {string} type - 绘制的类型 点 point、线 polyline、矩形 rectangle、多边形 polygon、圆 circle
 * @param {boolean} drawIsContinue - 是否继续绘制
 * @param {boolean} drawISReturnResult - 是否返回结果
 */
const startDraw = () => {
  isDrawing.value = true
  if (mapInstance.value) {
    mapInstance.value.addLayer(featureGroup.Draw)
  }
}

defineExpose({
  addPathToMap,
  startDraw
})

onMounted(() => {
  initMap()
  initEvent()
  // 监听地图容器尺寸变化
  if (mapRef.value) {
    const resizeObserver = new ResizeObserver(() => {
      handleMapResize()
    })
    resizeObserver.observe(mapRef.value)
    ;(mapRef.value as any)._resizeObserver = resizeObserver
  }
})

onUnmounted(() => {
  unInitEvent()
  // 清理 ResizeObserver
  if (mapRef.value && (mapRef.value as any)._resizeObserver) {
    (mapRef.value as any)._resizeObserver.disconnect()
  }
})

</script>

<style lang="scss" scoped>
.content_map_body {
  width: 100%;
  height: 100%;
  border-radius: 0px!important;
  z-index: 1;
}
</style>
<style>
  /* leaflet 的 bindPopup 弹窗样式 调整 */
.leaflet-popup-content {
  margin: 0 !important;
}
.leaflet-popup-content-wrapper, .leaflet-popup-tip {
  background: #031e42 !important;
}

.infoBox::before {
  content: '';
  position: absolute;
  bottom: -9px;
  left: 50%;
  transform: translateX(-50%);
  border-width: 10px 10px 0;
  border-style: solid;
  border-color: #027Aff transparent transparent transparent;
}
.BMapLib_Drawing_label {
  display: none !important;
}

/* 区域标签样式 */
.area-label {
  background: transparent !important;
  border: none !important;
}

.area-label div {
  pointer-events: none;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.8);
}

/* 隐藏默认的英文提示 */
.leaflet-pm-tooltip {
  display: none !important;
}

/* 自定义中文提示样式 */
.custom-chinese-tooltip {
  background: rgba(0, 0, 0, 0.8) !important;
  color: white !important;
  border: none !important;
  border-radius: 4px !important;
  padding: 8px 12px !important;
  font-size: 14px !important;
  font-weight: bold !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3) !important;
}

.custom-chinese-tooltip::before {
  border-top-color: rgba(0, 0, 0, 0.8) !important;
}
</style>
<style lang="scss">
.info_window_sign {
  width: 100%;
  min-width: 270px;
  min-height: 120px;
  border: 1px solid #027Aff;
  background: #031e42;
  padding: 8px 16px;
  border-radius: 5px;
}
.info_window {
  width: 100%;
  min-width: 220px;
  min-height: 180px;
  border: 1px solid #027Aff;
  background: #031e42;
  padding: 8px 16px;
  border-radius: 5px;
  user-select: none;
}
.topIconStyle {
  color: #fff;
  display: flex;
  align-items: center;
  margin: 10px 0;
  img {
    width: 25px;
    height: auto;
    margin: 0 4px;
  }
}
.info_window_item {
  width: 96%;
  display: flex;
  flex-direction: row;
  align-items: center;
  min-height: 32px;
  img {
    width: 18px;
    height: auto;
    margin: 0 4px;
  }
  span {
    flex: 1;
    font-size: 14px;
    color: #FFFFFF;
  }
}
.anchorBL {
  display: none !important;
}
.map_btns_group {
  display: flex;
  flex-direction: row;
  align-items: center;
  height: 48px;
  justify-content: space-around;
  cursor: pointer;
}
.map_btns_group2 {
  display: flex;
  flex-direction: row;
  align-self: start;
  height: 48px;
  margin-top: 10px;
}
.lineStyle {
  border-top: 1px solid #0278ff59;
  margin: 10px 0;
}
.map_btns_group_item {
  margin: 0 4px;
  background: #027Aff;
  border-radius: 4px;
  width: 100px;
  height: 32px;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  span {
    font-size: 12px;
    color: #fff;
  }
}
.map_user_container {
  display: flex;
  min-width: 100px;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
.map_user_name {
  color: #fff;
  font-size: 10px;
  padding: 1px 2px;
  border-radius: 2px;
  margin: 0 2px;
  border: 1px solid #0378FF;
  background: #0378FF;
}
//地图绘图工具标签样式
.screenshot {
  position: absolute;
  background: #fff;
  border: 1px solid #e9e9e9;
  border-radius: 2px;
  color: #333;
  padding: 0 4px;
  font-size: 12px;
}
.screenshot .rectWH {
  width: 110px;
  height: 30px;
  line-height: 30px;
  padding: 2px 0;
}
.screenshot .rectWH .wh {
  display: inline-block;
  width: 45px;
}
.screenshot .rectWH .wh #rectWidth,
.screenshot .rectWH .wh #rectHeight {
  width: 33px;
  cursor: text;
}
.screenshot .rectWH .wh #rectWidthInput,
.screenshot .rectWH .wh #rectHeightInput {
  display: none;
  border: 0;
  width: 34px;
  height: 30px;
  margin-top: -1px;
  outline: 0;
  user-select: text;
  appearance: listitem;
  -webkit-user-select: text;
  -webkit-appearance: listitem;
  padding: 0;
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  color: inherit;
}
.screenshot .rectWH .multiple {
  color: #999;
}
.screenshot .rectWH span {
  display: inline-block;
  width: 10px;
  text-align: center;
}
.screenshot .unit {
  display: block;
  color: #999;
  width: 12px;
  float: right;
}
.operateWindow {
  position: absolute;
  width: 270px;
  height: 35px;
  -webkit-user-select: none;
}
.operateWindow div #confirmOperate {
  float: left;
  width: 50px;
  height: 30px;
  margin-left: 15px;
  background: #fff url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADwAAAA8CAYAAAA6/NlyAAAAAXNSR0IArs4c6QAAAwxJREFUaAXtmE1oE0EUx2fWNCkpfkA9iXeN0tbqYVNUFISCJ1GoiPhxELWtx3oQRBA8iR48iEkPJYIeilK8qChFxJOJSlNsxYhFFKviIVQqqTUm+5wpmTIpm+xHNpspfXt5szOzb/6//+zO7iwheKAD6AA6gA6gA+gAOoAOoAPoADqADqADK88BupyQhzI7V+d/zd4AQg4x4T81CgNn9clHThiWDfDdpL7mN5l7CgBRAcjEzxMSiPR1jX8WdVZRs+qgQrsZLNfFZrqZUNjtRKPywJVgBSSlxpQo24lKA1vBEkoTvfrESzugoo+ywCXYJ/IzK0TzSCkZXq9vPi3X2SkruWhJsF1mEBy2VY8cO0zvF83aq9UpB1xPWG6EUsD1hlUK2A9YZYD9glUC2E9Yx8CPP+4PTc/8iIQg+PWk/irLE9Ry+A3Ltdp+D8ded3R+yU5PFYqFdM6Y+xZPtp/3AJZ/G3v+6qmmyzYwFI0YE7exlCxkgHEtlmy/Xi15pTZpZhc3AnLfWt6zch6zsm1gArB1aQIAY+BWsm0Q4LLtPBKsrzMrtNsWyr7lXoiLyiLAmXhy5M5z2Bsoqzc5aTQsl2QbuInQXgb9yYSDbdPgaCaVHeGLmlk7r1MBlutw9KU1NNa5IZ//NwpAtvCLlx6U0GctYXrgRMfbnNxW+lPh+wIlaxBl2zPMLzi1Pf1dC4T2MLAxkUCObKb35f7AaCK9bZ2oVwmWa3I0wwJi8M2OtcXC34fsXt4l6soiJePNwaZuCIfn2T8oJWZW6HMFzC9m0GEG/YBBd4tkcqSUfmDnM9X2s263ePI4TsuugflA9971BLOzmWF2Kx90MnA937NWOmoCXoCGnlXZ1PsEW8iOWw3G2xsJuzC+HZFWfdhtS+OptpsMur9a30bDcm2OVulKMOx5hb7o5Dmi0auV+7j/LVMpp5t6T4DFwP36xAWiaRfFuYgqzOyiFlHwMrJNxRFCjEvsFm+hRLvdGt10xc0PNy81YS50AB1AB9ABdAAdQAfQAXQAHUAH0AF0YCU78B/Xn4einv7Y7QAAAABJRU5ErkJggg==) no-repeat top center;
  background-size: contain;
  box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.3);
  border-radius: 2px;
}
.operateWindow div #cancelOperate {
  float: left;
  width: 50px;
  height: 30px;
  margin-left: 8px;
  background: #fff url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADwAAAA8CAYAAAA6/NlyAAAAAXNSR0IArs4c6QAAAupJREFUaAXtmTtvFDEQx2eWQ4ICISRaegq6hI4CCSEkQgkRgi6QkhRwCOiOGlIAouIhUhGSAqVFQgr1HWW+AvcBIo7myDB25GjX571drx8NY+m0a589M7//+rVeAEmigCggCogCooAoIAqIAqKAKCAKiAKiwP+nAIYi051LZ2D65w0Q3ODfGI7hU/wy2gm1a9rTzYUHgPgYgE6y/U9w4dQzHOxOzf++18K3wUx9BXsAdzmY0/zfefhLX+nW4r2Zeh0KaHnhJTd7DUTn2P5Zvu/D3n6/g6mjJuHA6slWE3KA70Khuf06C/moalrl0PY3W2VOSQzgscN+ELSGJXrosMs9m345y1sWhgPzmGVf5PDXCVp34zpYgAkcL547fLUuCgbWExTiagxoDevsxppnAj1cws/DvdZ0jorBs7Sxqccsj13Ou2wSz7SruD36YOrb11awm6Ndu51v3hWcr42j+l2hc8GqQKMCK4O+0DlhkwD7QOeGTQbcDhq+8dx+TdV1pMMJKsKYtW1H79JlBw3du1y1fJ8MVjlJCqwceEInhc0C7AGdHFbFErzxUEaakl5/kcdsfeJ1mt5igjFru8wCrGfj+glKxcTbUOzr7m9HGDmffgyrV7z67aKN07gjsxv45pM+4YZ1NtoLhw90MuAG2AkU+IIDzQ6dBLgRVr31bI2eMPT93NDRx3Ar2NJsTMuLK3BA7xncFUv0Me1y4jMkKnV9YU3jnNDRgHlJWefjF/exjDqpwOI6bg9/GEj7mgs6CnAorIHPAR0MzN14jdfZVyboyhXhN2/mluY92Up9zjRC94qruDn8brdrmw+fpXmH5HTWAVbZ4dn749zZe0qOo1tnBM7CcGD1RcBOHWGNmQboWX+mYYtrOLD6/FFOgbDGVC10QVV/pkHLa/gYHlzuHX7+0F8ExnyUOgg9Si3HTrcvXgHVjZFO8Eq9gVs/N8r/y70oIAqIAqKAKCAKiAKigCggCogCooAo0EGBf6iFZapSQC/BAAAAAElFTkSuQmCC) no-repeat top center;
  background-size: contain;
  box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.3);
  border-radius: 2px;
}
.trackIcon {
  width: 25px;
  margin-right: 5px;
}
.leaflet-control-container {
  display: none !important;
}
.path-point-number {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background-color: #ff0000;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.3);
}
</style>
<style lang="scss" scoped>
.content_map_body {
  width: 100%;
  height: 100%;
  border-radius: 10px;
}
</style>

<style lang="scss">
  /* 强制divIcon背景透明 */
.leaflet-div-icon {
  background-color: transparent !important;
  border: none !important;
}
  /* 自定义icon样式 */
.self_marker_icon {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: transparent;
  img {
    width: 30px;
    height: 30px;
  }
  span {
    font-size: 12px;
    color: #fff;
    padding: 0px 8px;
    background-color: var(--el-color-primary);
    border-radius: 2px;
    word-break: keep-all;
  }
}

</style>
