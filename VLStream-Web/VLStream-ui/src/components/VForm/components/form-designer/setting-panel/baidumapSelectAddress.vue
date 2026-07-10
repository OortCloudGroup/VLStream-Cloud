<template>
  <div class="map_body">
    <div ref="mapContainer" class="map_container" />
    <div class="button_select">
      <div>{{ address?"详细地址：" + address: '' }} {{ isEdit?'经纬度坐标' + centerPoint.center:'当前坐标：'+ centerPoint.center }}</div>
      <el-button type="primary" size="large" @click="baiduMapSelectAddress">
        确定
      </el-button>
    </div>
  </div>
</template>

<script setup>

import { ref, onMounted } from 'vue'
import { ElButton } from 'element-plus'

const props = defineProps({
  center: {
    type: Object,
    default: null
  },
  level: {
    type: Number,
    default: 13
  },
  // 是否编辑
  isEdit: {
    type: Boolean,
    default: true
  },
  areaType: {
    type: Number,
    default: 1
  }
})

const address = ref('')
const centerPoint = ref(props.center)
const mapContainer = ref(null)

const init = async() => {
  let mapLevel = props.level || 13
  let map = new window.BMapGL.Map(mapContainer.value)
  window.baiduMap = map
  let marker1 = null
  if (props.center) {
    try {
      let centerPointObj = null
      if (typeof props.center === 'object') {
        centerPointObj = JSON.parse(props.center.center)
      } else {
        centerPointObj = JSON.parse(props.center)
      }
      let tPoint = new window.BMapGL.Point(centerPointObj.lng, centerPointObj.lat)
      map.centerAndZoom(tPoint, mapLevel)
      marker1 = new window.BMapGL.Marker(tPoint)
      map.addOverlay(marker1)
      setTimeout(() => { map.panTo(tPoint) }, 1500)
    } catch (error) {
      console.error('error_init', error)
      map.centerAndZoom(new window.BMapGL.Point(116.331398, 39.897445), mapLevel)
    }
  } else {
    map.centerAndZoom(new window.BMapGL.Point(116.331398, 39.897445), mapLevel)
  }

  map.enableScrollWheelZoom(true)
  let geoc = new window.BMapGL.Geocoder()
  map.addEventListener('click', (e) => {
    let pt = e.latlng
    marker1 && marker1.setPosition(new window.BMapGL.Point(pt.lng, pt.lat))
    centerPoint.value = JSON.stringify({ lng: pt.lng, lat: pt.lat })
    geoc.getLocation(pt, (rs) => {
      let addComp = rs.addressComponents
      // 根据选择的精度显示
      if (props.areaType === 4) {
        address.value = addComp.province + addComp.city + addComp.district + addComp.street + addComp.streetNumber
      }
      if (props.areaType === 5) {
        address.value = addComp.district + addComp.street + addComp.streetNumber
      }

      // { label: '省', value: 1 },
      //   { label: '省-市', value: 2 },
      //   { label: '省-市-区', value: 3 },
      //   { label: '省-市-区-详细地址', value: 4 },
      //   { label: '详细地址', value: 5 }
    })
  })
}

const emits = defineEmits(['update:locationCenter'])
const baiduMapSelectAddress = () => {
  emits('update:center', centerPoint.value)
  emits('change', { address: address.value, center: centerPoint.value })
}

onMounted(() => {
  init()
})

</script>

<style lang="scss" scoped>
    .map_body {
        width: 100%;
        height: 100%;
        display: flex;
        flex-direction: column;
        .map_container {
            width:100%;
            min-height: 500px;
            height:  calc(100% - 60px);
        }
        .button_select {
           height: 80px;
           display: flex;
           flex-direction: row;
           justify-content: space-between;
           align-items: center;
           div {
            color: var(--el-color-primary);
            font-size: 16px;
           }
        }
    }
    </style>
