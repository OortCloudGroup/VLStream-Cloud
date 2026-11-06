<template>
  <div class="location-picker">
    <div class="location-toolbar">
      <el-input v-model="query" :placeholder="$tp(hasGeocoding ? '输入地址或经度,纬度' : '输入经度,纬度定位')" @keyup.enter="locate" clearable />
      <el-button :loading="searching" @click="locate">{{ $tp(hasGeocoding ? '搜索' : '定位') }}</el-button>
      <el-button type="primary" :disabled="!selected" @click="confirm">{{ $tp('确定') }}</el-button>
    </div>
    <div v-if="results.length" class="location-results"><el-button v-for="(result, index) in results" :key="index" text @click="chooseResult(result)">{{ result.display_name }}</el-button></div>
    <el-input v-model="address" :placeholder="$tp('地址（可手动填写）')" @input="selectionGeneration++" />
    <div ref="container" class="location-map" role="region" :aria-label="$tp('位置选择地图')" />
    <div class="location-status">{{ selected ? `${selected[0].toFixed(7)}, ${selected[1].toFixed(7)}` : $tp('请在地图上点击选择位置') }}</div>
    <el-alert v-if="tileError" :title="$tp('底图加载失败，请检查网络；可输入经纬度定位')" type="warning" :closable="false" />
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { translatePhrase as uiText } from '@/i18n'
import { hasGeocoding, searchAddress, reverseGeocode } from '@/utils/osmGeocoding'

const props = defineProps({ autoInitialize: { type: Boolean, default: true }, position: { default: null }, toponym: { type: String, default: '' } })
const emit = defineEmits(['update-value'])
const container = ref(null), selected = ref(null), query = ref(''), address = ref(''), tileError = ref(false)
const searching = ref(false), results = ref([])
let map, marker, observer, generation = 0, selectionGeneration = 0
function validPosition(value) {
  return Array.isArray(value) && value.length === 2 && value.every(item =>
    ['string', 'number'].includes(typeof item) && String(item).trim() !== '' && Number.isFinite(Number(item))) &&
    Math.abs(Number(value[0])) <= 180 && Math.abs(Number(value[1])) <= 90
}
function selectPosition(position, preserveAddress = false) {
  if (!map || !validPosition(position)) return
  selected.value = position.map(Number)
  const selection = ++selectionGeneration, current = generation
  if (!preserveAddress) address.value = ''
  if (!preserveAddress && hasGeocoding) {
    reverseGeocode({ lng: selected.value[0], lat: selected.value[1] }).then(value => {
      if (current === generation && selection === selectionGeneration && map) address.value = value
    }).catch(() => {
      if (current === generation && selection === selectionGeneration && map) ElMessage.warning(uiText('地址查询失败，可手动填写地址'))
    })
  }
  const point = [selected.value[1], selected.value[0]]
  if (marker) marker.setLatLng(point)
  else {
    marker = L.marker(point, { draggable: true, icon: L.divIcon({
      className: 'location-marker', html: '<span style="display:block;width:20px;height:20px;background:#245bff;border:3px solid white;border-radius:50%;box-shadow:0 1px 5px #555"></span>',
      iconSize: [26, 26], iconAnchor: [13, 13]
    }) }).addTo(map)
    marker.on('dragend', () => { const point = marker.getLatLng(); selectPosition([point.lng, point.lat]) })
  }
}
async function initialize() {
  const current = generation
  await nextTick()
  if (current !== generation || !container.value?.isConnected) return
  if (map) { map.invalidateSize(); return }
  address.value = props.toponym || ''
  tileError.value = false
  const initial = validPosition(props.position) ? props.position.map(Number) : null
  map = L.map(container.value).setView(initial ? [initial[1], initial[0]] : [35.8617, 104.1954], initial ? 15 : 4)
  L.tileLayer(import.meta.env.VITE_OSM_TILE_URL || 'https://tile.openstreetmap.de/{z}/{x}/{y}.png', {
    maxZoom: 19, attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a> contributors'
  }).on('tileerror', () => { tileError.value = true }).addTo(map)
  L.control.scale({ imperial: false }).addTo(map)
  map.on('click', event => selectPosition([event.latlng.lng, event.latlng.lat]))
  if (initial) selectPosition(initial, true)
  observer = new ResizeObserver(() => map?.invalidateSize())
  observer.observe(container.value)
  map.invalidateSize()
}
async function locate() {
  if (!map || searching.value) return
  const point = query.value.split(/[,，\s]+/).filter(Boolean)
  if (validPosition(point)) {
    results.value = []
    selectPosition(point)
    map.setView([Number(point[1]), Number(point[0])], 16)
    return
  }
  if (!hasGeocoding || !query.value.trim()) return ElMessage.warning(uiText('请输入有效的经度,纬度'))
  const current = generation, selection = ++selectionGeneration
  searching.value = true
  try {
    const items = await searchAddress(query.value.trim())
    if (current !== generation || selection !== selectionGeneration || !map) return
    results.value = items.filter(item => validPosition([item.lon, item.lat]))
    if (!results.value.length) ElMessage.info(uiText('未找到匹配的地址'))
  } catch {
    if (current === generation) ElMessage.warning(uiText('地址查询失败，可在地图上点选'))
  } finally { if (current === generation) searching.value = false }
}
function chooseResult(result) {
  selectPosition([result.lon, result.lat], true)
  address.value = result.display_name || ''
  results.value = []
  map?.setView([Number(result.lat), Number(result.lon)], 16)
}
function confirm() {
  if (!selected.value || !map) return
  emit('update-value', { show: false, lng: selected.value[0], lat: selected.value[1], address: address.value.trim(), detailedStreet: '' })
}
function destroy() {
  generation++
  selectionGeneration++
  results.value = []; searching.value = false
  observer?.disconnect(); observer = null
  map?.remove(); map = null; marker = null
  selected.value = null; query.value = ''; address.value = ''; tileError.value = false
}
// Keep existing protocol dialogs compatible while sharing the Leaflet implementation.
defineExpose({ initialize, destroy, inGaDeMap: initialize, Destruction: destroy })
onMounted(() => { if (props.autoInitialize) initialize() })
onBeforeUnmount(destroy)
</script>

<style scoped>
.location-toolbar { display: flex; gap: 8px; margin-bottom: 12px; }
.location-toolbar .el-input { flex: 1; min-width: 0; }
.location-toolbar .el-button + .el-button { margin-left: 0; }
.location-map { width: 100%; height: min(550px, 55vh); min-height: 280px; margin-top: 12px; }
.location-status { margin-top: 8px; color: var(--el-text-color-secondary); font-size: 12px; }
.location-results { max-height: 150px; overflow: auto; margin-bottom: 8px; }
.location-results .el-button { display: block; width: 100%; height: auto; white-space: normal; text-align: left; margin: 0; }
</style>
