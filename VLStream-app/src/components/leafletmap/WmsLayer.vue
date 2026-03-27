<template>
  <div>
    <LWmsTileLayer
      v-for="(theme, index) in [themeOptions[activeThemKey]]"
      :key="theme.layer + index"
      :url="theme.url"
      :layers="theme.layer"
    />
  </div>
</template>

<script setup>
import { defineProps, ref } from 'vue'
import { LWmsTileLayer } from '@vue-leaflet/vue-leaflet'

defineProps({
  activeThemKey: {
    type: String,
    default: 'standard'
  }
})
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
</script>
