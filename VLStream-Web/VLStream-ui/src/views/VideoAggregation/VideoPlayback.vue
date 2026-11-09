<template>
  <div class="playback-workspace">
    <aside class="directory">
      <h2>{{ $tp('视频回放') }}</h2>
      <el-input v-model="search" clearable :placeholder="$tp('搜索设备名称 / ID')" />
      <VideoSquareClassificationTree :devices="devices" :visible-devices="visibleDevices" :selected-device-id="device?.id" show-tree
        @device-click="filterDevice" @filter-change="changeCategory" />
      <el-alert v-if="catalogError" :title="catalogError" type="error" :closable="false" />
      <el-button v-if="catalogError" @click="loadCatalog">{{ $tp('重试') }}</el-button>
      <small class="directory-footer">{{ $tp('平台录像 · 按通道保留') }}</small>
    </aside>
    <main class="device-list">
      <div class="list-toolbar">
        <span>{{ $tp('设备列表') }} <small>{{ tableDevices.length }}</small></span>
        <el-input v-model="listSearch" clearable :placeholder="$tp('搜索设备名称 / ID')" :prefix-icon="Search" />
        <el-button :icon="Refresh" :aria-label="$tp('刷新')" @click="loadCatalog" />
      </div>
      <el-table :data="pageDevices" :row-class-name="({ row }) => device?.id === row.id ? 'selected-device' : ''" height="100%" @row-dblclick="selectDevice">
        <el-table-column type="index" :index="index => (page - 1) * pageSize + index + 1" :label="$tp('序号')" width="68" />
        <el-table-column prop="deviceName" :label="$tp('设备名称')" min-width="150" show-overflow-tooltip />
        <el-table-column prop="deviceId" :label="$tp('设备 ID')" min-width="190" show-overflow-tooltip />
        <el-table-column :label="$tp('设备来源')" min-width="140"><template #default="{ row }"><el-tag v-for="protocol in row.protocols" :key="protocol" size="small" class="protocol-tag">{{ protocol }}</el-tag></template></el-table-column>
        <el-table-column :label="$tp('状态')" width="100"><template #default="{ row }"><el-tag size="small" :type="row.status === 'ONLINE' ? 'success' : 'info'">{{ $tp(row.status === 'ONLINE' ? '在线' : row.status === 'OFFLINE' ? '离线' : '未知状态') }}</el-tag></template></el-table-column>
        <el-table-column :label="$tp('操作')" width="100" fixed="right"><template #default="{ row }"><el-button link type="primary" :icon="VideoPlay" @click="selectDevice(row)">{{ $tp('播放') }}</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]" :total="tableDevices.length" layout="total, prev, pager, next, sizes" />
    </main>
    <el-dialog v-model="playerOpen" :fullscreen="playerFullscreen" width="min(1360px, 94vw)" top="5vh" class="playback-dialog" :show-close="false" append-to-body @closed="closePlayer">
      <template #header><div class="dialog-heading"><span>{{ $tp('播放视频') }}<small>{{ device?.deviceName }}</small></span><div><el-button text :icon="FullScreen" :aria-label="$tp('全屏')" @click="playerFullscreen = !playerFullscreen" /><el-button text :icon="Close" :aria-label="$tp('关闭')" @click="playerOpen = false" /></div></div></template>
      <div v-loading="loading" class="playback-content">
        <div class="source-toolbar">
          <el-select v-if="device?.sources?.length > 1" v-model="sourceKey" :aria-label="$tp('设备来源')" @change="loadSources"><el-option v-for="item in device.sources" :key="key(item)" :value="key(item)" :label="`${item.protocol} · ${item.deviceName || item.rowId}`" /></el-select>
          <el-select v-model="sourceId" :placeholder="$tp('选择录像通道')" @change="loadSegments"><el-option v-for="item in sources" :key="item.id" :value="item.id" :label="item.source?.channelName || item.source?.name || $tp('设备主码流')" /></el-select>
          <el-tag v-if="recordSource" :type="recordSource.state === 'RECORDING' ? 'success' : 'info'">{{ $tp(recordingState(recordSource)) }}</el-tag>
          <PlatformRecordingActions v-if="source" :protocol="source.protocol" :row-id="source.rowId" :online="source.state === 'ONLINE'" @changed="loadSources" />
        </div>
        <div class="date-panels">
          <section class="date-panel years"><label>{{ $tp('年份') }}</label><div class="date-grid"><button v-for="year in years" :key="year" :class="{ chosen: selectedYear === year }" @click="chooseDate(year, selectedMonth, selectedDay)">{{ year }}</button></div></section>
          <section class="date-panel months"><label>{{ $tp('月份') }}</label><div class="date-grid"><button v-for="month in 12" :key="month" :disabled="isFuture(selectedYear, month, 1)" :class="{ chosen: selectedMonth === month }" @click="chooseDate(selectedYear, month, selectedDay)">{{ month }}</button></div></section>
          <section class="date-panel dates"><label>{{ $tp('日期') }} <small>{{ $tp('北京时间') }}</small></label><div class="date-grid"><button v-for="day in daysInMonth" :key="day" :disabled="isFuture(selectedYear, selectedMonth, day)" :class="{ chosen: selectedDay === day }" @click="chooseDate(selectedYear, selectedMonth, day)">{{ day }}</button></div></section>
        </div>
        <el-alert v-if="error" :title="error" type="error" :closable="false" />
        <div class="media-layout">
          <section class="clip-panel"><div class="clip-heading">{{ $tp('视频列表') }} <small>{{ segments.length }}</small></div>
            <div class="clip-list"><el-empty v-if="!segments.length" :image-size="40" :description="$tp('当天暂无录像')" />
              <button v-for="segment in segments" :key="segment.id" :class="['clip', { selected: active?.id === segment.id }]" @click="play(segment)">
                <span class="clip-cover"><img v-if="posters[segment.id]" :src="posters[segment.id]" alt="" /><el-icon v-else><VideoPlay /></el-icon><small>{{ (Number(segment.size_bytes) / 1048576).toFixed(1) }} MB</small></span>
                <span>{{ range(segment) }}</span>
              </button>
            </div>
          </section>
          <section class="player-panel">
            <div class="screen"><video v-if="url" ref="video" :src="url" controls playsinline @loadedmetadata="mediaReady" @loadeddata="capturePoster" @timeupdate="updateTime" @ended="next" @error="error = $tp('录像解码失败，请下载片段检查或选择其他录像')" /><div v-else class="empty"><el-icon><VideoPlay /></el-icon><strong>{{ $tp(loadingMedia ? '正在加载录像' : '当前没有可播放的录像') }}</strong><p>{{ $tp('开启手动录制或定时计划后，已保存的片段会显示在这里。') }}</p></div></div>
            <div class="player-toolbar"><span>{{ currentTime || '00:00:00' }}</span><el-select v-model="speed" size="small" :aria-label="$tp('播放速度')" @change="setSpeed"><el-option v-for="rate in [0.5, 1, 1.5, 2, 4]" :key="rate" :value="rate" :label="`${rate}×`" /></el-select><el-button text :disabled="!active || loadingMedia" @click="download">{{ $tp('下载当前片段') }}</el-button></div>
            <div class="track" role="group" :aria-label="$tp('录像时间轴')"><button v-for="segment in segments" :key="segment.id" :style="segmentStyle(segment)" :class="{ active: active?.id === segment.id }" :title="range(segment)" :aria-label="range(segment)" @click="play(segment)" /></div>
            <div class="hours"><span v-for="hour in [0, 4, 8, 12, 16, 20, 24]" :key="hour">{{ String(hour).padStart(2, '0') }}:00</span></div>
          </section>
        </div>
      </div>
    </el-dialog>
  </div>
</template>
<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Search, Refresh, VideoPlay, FullScreen, Close } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { translatePhrase as t } from '@/i18n'
import { getVideoDeviceCatalog } from '@/api/videoDeviceCatalog'
import { matchesVideoDevice } from '@/utils/videoSquareCatalog'
import { platformRecording as api, recordingError, recordingState } from '@/api/platformRecording'
import VideoSquareClassificationTree from './components/VideoSquareClassificationTree.vue'
import PlatformRecordingActions from '@/components/PlatformRecordingActions.vue'
const route = useRoute()
const playerOpen = ref(false), playerFullscreen = ref(false), listSearch = ref(''), focusedDeviceId = ref(null)
const page = ref(1), pageSize = ref(10), posters = ref({})
const tableDevices = computed(() => visibleDevices.value.filter(item => (!focusedDeviceId.value || item.id === focusedDeviceId.value) && matchesVideoDevice(item, listSearch.value)))
const pageDevices = computed(() => tableDevices.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
function filterDevice(value) { focusedDeviceId.value = value.id; device.value = value; page.value = 1 }
function changeCategory(ids) { categoryIds.value = ids; focusedDeviceId.value = null; page.value = 1 }
function closePlayer() { generation++; clearMedia(); posters.value = {}; playerFullscreen.value = false }

const devices = ref([]), device = ref(null), search = ref(''), categoryIds = ref(null)
const catalogError = ref(''), error = ref(''), loading = ref(false), loadingMedia = ref(false)
const sourceKey = ref(''), sources = ref([]), sourceId = ref(''), segments = ref([]), active = ref(null)
const video = ref(null), url = ref(''), speed = ref(1), currentTime = ref('')
const date = ref(new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date()))
const selectedYear = computed(() => Number(date.value.slice(0, 4)))
const selectedMonth = computed(() => Number(date.value.slice(5, 7)))
const selectedDay = computed(() => Number(date.value.slice(8, 10)))
const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
const years = Array.from({ length: 6 }, (_, i) => Number(today.slice(0, 4)) - 5 + i)
const daysInMonth = computed(() => new Date(selectedYear.value, selectedMonth.value, 0).getDate())
const dateString = (y, m, d) => `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
const isFuture = (y, m, d) => dateString(y, m, d) > today
function chooseDate(y, m, d) {
  const nextDate = dateString(y, m, Math.min(d, new Date(y, m, 0).getDate()))
  const value = nextDate > today ? today : nextDate
  if (date.value !== value) { date.value = value; loadSegments() }
}
function capturePoster() {
  if (!active.value || !video.value?.videoWidth) return
  try {
    const canvas = document.createElement('canvas'); canvas.width = 240; canvas.height = 135
    canvas.getContext('2d').drawImage(video.value, 0, 0, 240, 135)
    posters.value[active.value.id] = canvas.toDataURL('image/jpeg', 0.7)
  } catch { /* Thumbnail failure must not interrupt playback. */ }
}
async function warmPosters(items, ticket) {
  // Bound background downloads to the first three clips; never prefetch an entire day.
  for (const item of items.slice(0, 3)) {
    if (disposed || ticket !== generation || !playerOpen.value) return
    let objectUrl, preview
    try {
      const blob = await api.file(item.id)
      if (disposed || ticket !== generation || !playerOpen.value) return
      objectUrl = URL.createObjectURL(blob)
      preview = document.createElement('video'); preview.muted = true; preview.preload = 'auto'
      await new Promise((resolve, reject) => {
        const timeout = setTimeout(() => reject(new Error('Thumbnail timeout')), 8000)
        const finish = () => { clearTimeout(timeout); resolve() }
        preview.onerror = () => { clearTimeout(timeout); reject(new Error('Thumbnail unavailable')) }
        preview.onloadeddata = () => { preview.currentTime = Math.min(0.1, preview.duration / 2); }
        preview.onseeked = finish
        preview.src = objectUrl
      })
      if (disposed || ticket !== generation || !playerOpen.value) return
      const canvas = document.createElement('canvas'); canvas.width = 240; canvas.height = 135
      canvas.getContext('2d').drawImage(preview, 0, 0, 240, 135)
      posters.value[item.id] = canvas.toDataURL('image/jpeg', 0.7)
    } catch { /* A missing thumbnail does not make a recording unavailable. */ }
    finally { if (preview) { preview.removeAttribute('src'); preview.load() } if (objectUrl) URL.revokeObjectURL(objectUrl) }
  }
}
const key = item => `${item.protocol}:${item.rowId}`
const source = computed(() => device.value?.sources?.find(item => key(item) === sourceKey.value))
const recordSource = computed(() => sources.value.find(item => item.id === sourceId.value))
const visibleDevices = computed(() => devices.value.filter(item => matchesVideoDevice(item, search.value) && (!categoryIds.value || categoryIds.value.includes(item.id))))
const dayStart = computed(() => new Date(`${date.value}T00:00:00+08:00`).getTime())
watch([listSearch, search, pageSize], () => { page.value = 1 })
watch(tableDevices, values => { page.value = Math.min(page.value, Math.max(1, Math.ceil(values.length / pageSize.value))) })
let generation = 0, mediaGeneration = 0, catalogGeneration = 0, disposed = false, poll, autoPlay = false, polling = false
const clock = time => new Intl.DateTimeFormat('en-GB', { timeZone: 'Asia/Shanghai', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(new Date(Number(time)))
const range = segment => `${clock(segment.start_ms)} – ${clock(segment.end_ms)}`
function segmentStyle(segment) {
  const from = Math.max(0, Number(segment.start_ms) - dayStart.value), to = Math.min(86400000, Number(segment.end_ms) - dayStart.value)
  return { insetInlineStart: `${from / 864000}%`, width: `${Math.max(.12, (to - from) / 864000)}%` }
}
function clearMedia() {
  mediaGeneration++; video.value?.pause(); if (url.value) URL.revokeObjectURL(url.value)
  url.value = ''; active.value = null; currentTime.value = ''; loadingMedia.value = false
}
async function loadCatalog() {
  const ticket = ++catalogGeneration; catalogError.value = ''
  try {
    const result = await getVideoDeviceCatalog()
    if (disposed || ticket !== catalogGeneration) return
    devices.value = result.devices
    const target = devices.value.find(item => item.sources?.some(s => s.protocol === route.query.protocol && String(s.rowId) === String(route.query.rowId)))
    if (target) await selectDevice(target, `${route.query.protocol}:${route.query.rowId}`)
  } catch (failure) { if (!disposed && ticket === catalogGeneration) catalogError.value = recordingError(failure) }
}
async function selectDevice(value, preferred) { device.value = value; playerOpen.value = true; posters.value = {}; sourceKey.value = typeof preferred === 'string' ? preferred : key(value.sources[0]); await loadSources() }
async function loadSources() {
  const ticket = ++generation; clearMedia(); segments.value = []; sources.value = []; sourceId.value = ''; error.value = ''
  if (!source.value) return
  loading.value = true
  try {
    const rows = await api.sources(source.value.protocol, source.value.rowId)
    if (disposed || ticket !== generation) return
    sources.value = rows; sourceId.value = rows[0]?.id || ''; loading.value = false
    await loadSegments()
  } catch (failure) { if (ticket === generation) error.value = recordingError(failure) }
  finally { if (ticket === generation) loading.value = false }
}
async function loadSegments() {
  const ticket = ++generation; clearMedia(); segments.value = []; error.value = ''; loading.value = false
  if (!sourceId.value || !date.value) return
  loading.value = true
  try {
    const rows = await api.segments(sourceId.value, date.value)
    if (disposed || ticket !== generation) return
    segments.value = rows
    warmPosters(rows, ticket)
    if (rows.length) await play(rows[0], false)
  } catch (failure) { if (ticket === generation) error.value = recordingError(failure) }
  finally { if (ticket === generation) loading.value = false }
}
async function play(segment, shouldPlay = true) {
  clearMedia(); const ticket = mediaGeneration; loadingMedia.value = true; error.value = ''; autoPlay = shouldPlay
  try {
    const blob = await api.file(segment.id)
    if (disposed || ticket !== mediaGeneration) return
    if (!(blob instanceof Blob) || blob.type.includes('json')) throw new Error(t('录像文件不可用'))
    url.value = URL.createObjectURL(blob); active.value = segment; currentTime.value = clock(segment.start_ms)
  } catch (failure) { if (ticket === mediaGeneration) error.value = recordingError(failure) }
  finally { if (ticket === mediaGeneration) loadingMedia.value = false }
}
function setSpeed() { if (video.value) video.value.playbackRate = speed.value }
function mediaReady() { setSpeed(); if (autoPlay) video.value?.play().catch(() => {}) }
function updateTime() {
  if (active.value) {
    currentTime.value = clock(Number(active.value.start_ms) + (video.value?.currentTime || 0) * 1000)
    if (video.value?.currentTime > 0.2 && video.value.currentTime < 1) capturePoster()
  }
}
function next() { const index = segments.value.findIndex(item => item.id === active.value?.id); if (index >= 0 && index + 1 < segments.value.length) play(segments.value[index + 1]) }
function download() { if (url.value && active.value) { const a = document.createElement('a'); a.href = url.value; a.download = `${active.value.id}.mp4`; a.click() } }
async function refresh() {
  if (!playerOpen.value || !source.value || loading.value || polling) return
  const ticket = generation; polling = true
  try {
    const rows = await api.sources(source.value.protocol, source.value.rowId)
    if (disposed || ticket !== generation) return
    sources.value = rows
    if (!sourceId.value && rows.length) { sourceId.value = rows[0].id; await loadSegments(); return }
    if (!sourceId.value) return
    const items = await api.segments(sourceId.value, date.value)
    if (disposed || ticket !== generation) return
    segments.value = items
  } catch (failure) { if (!disposed && ticket === generation) error.value = recordingError(failure) }
  finally { polling = false }
}
watch(() => [route.query.protocol, route.query.rowId], loadCatalog)
onMounted(() => { loadCatalog(); poll = setInterval(refresh, 10000) })
onBeforeUnmount(() => { disposed = true; generation++; catalogGeneration++; clearInterval(poll); clearMedia() })
</script>
<style scoped>
.playback-workspace { display:grid; grid-template-columns:260px minmax(0,1fr); height:calc(100vh - 96px); min-height:640px; background:#fff; border-radius:16px; overflow:hidden; color:#202b40; }
.directory { padding:24px 16px 16px; border-inline-end:1px solid #edf0f5; display:flex; flex-direction:column; gap:20px; overflow:auto; }
h2 { margin:0; font-size:18px; font-weight:600; }
.directory :deep(.square-classification) { padding-inline:0; }
.directory :deep(.classification-tree) { max-height:none; }
.directory-footer { margin-top:auto; padding-top:16px; }
small { color:#8792a5; }
.device-list { padding:24px; display:flex; flex-direction:column; gap:18px; min-width:0; min-height:0; }
.list-toolbar { display:flex; gap:12px; align-items:center; }
.list-toolbar > span { margin-inline-end:auto; white-space:nowrap; }
.list-toolbar .el-input { max-width:320px; }
.list-toolbar :deep(.el-input__wrapper) { border-radius:18px; }
.device-list :deep(.el-table) { flex:1; --el-table-header-bg-color:#f6f7fa; --el-table-row-hover-bg-color:#edf3ff; font-size:13px; }
.device-list :deep(.el-table__cell) { padding:15px 0; }
.device-list :deep(.selected-device td.el-table__cell) { background:#e8f0ff; }
.device-list .el-pagination { justify-content:flex-end; }
.protocol-tag { margin-inline-end:4px; }
.dialog-heading { display:flex; align-items:center; justify-content:space-between; color:#333; width:100%; flex:1; }
.dialog-heading small { margin-inline-start:18px; }
.dialog-heading .el-button + .el-button { margin-left:0; }
.playback-content { display:flex; flex-direction:column; gap:20px; }
.source-toolbar { display:flex; align-items:center; gap:12px; flex-wrap:wrap; }
.source-toolbar .el-select { width:220px; }
.source-toolbar .recording-actions { margin-inline-start:auto; }
.date-panels { display:grid; grid-template-columns:220px 200px minmax(300px,1fr); gap:28px; max-width:1120px; }
.date-panel label { display:block; margin-bottom:12px; font-size:14px; color:#333; }
.date-panel label small { margin-inline-start:8px; }
.date-grid { display:grid; border:1px solid #e5e8ee; border-radius:4px; overflow:hidden; background:white; }
.years .date-grid { grid-template-columns:repeat(3,1fr); }
.months .date-grid { grid-template-columns:repeat(4,1fr); }
.dates .date-grid { grid-template-columns:repeat(11,1fr); }
.date-grid button { min-height:36px; background:#fff; border:0; border-bottom:1px solid #f0f2f6; border-right:1px solid #f0f2f6; color:#9098a6; cursor:pointer; font:inherit; font-size:13px; }
.date-grid button.chosen { background:#e5efff; color:#2278ff; }
.date-grid button:hover:not(:disabled) { background:#edf3ff; color:#2278ff; }
.date-grid button:disabled { color:#d2d6df; cursor:not-allowed; }
.media-layout { display:grid; grid-template-columns:210px minmax(0,1fr); gap:32px; }
.clip-panel { min-height:0; }
.clip-heading { margin-bottom:14px; color:#333; font-size:14px; }
.clip-list { height: min(52vh, 580px); overflow:auto; padding-inline-end:10px; }
.clip { width:100%; display:flex; flex-direction:column; gap:9px; margin-bottom:18px; padding:0; background:none; border:0; color:#999; cursor:pointer; font-size:12px; }
.clip-cover { position:relative; display:grid; place-items:center; aspect-ratio:1.52; width:100%; background:#202a39; border:2px solid transparent; overflow:hidden; }
.clip-cover img { width:100%; height:100%; object-fit:cover; }
.clip-cover .el-icon { font-size:36px; color:#7b8da8; }
.clip-cover small { position:absolute; right:6px; bottom:6px; padding:2px 5px; color:#fff; background:#0007; }
.clip.selected { color:#2278ff; }
.clip.selected .clip-cover { border-color:#2278ff; }
.player-panel { min-width:0; padding-top:32px; }
.screen { display:grid; place-items:center; aspect-ratio:2.06; background:#111725; overflow:hidden; }
.screen video { width:100%; height:100%; max-height:58vh; object-fit:contain; }
.empty { text-align:center; padding:24px; color:#a4b2c9; }
.empty > .el-icon { display:block; margin:0 auto 15px; font-size:44px; }
.empty strong { font-weight:400; }
.empty p { font-size:12px; line-height:1.7; }
.player-toolbar { display:flex; align-items:center; gap:14px; margin:12px 0; }
.player-toolbar > span { margin-inline-end:auto; font-variant-numeric:tabular-nums; }
.player-toolbar .el-select { width:85px; }
.track { height:16px; background:#e4e9f1; position:relative; overflow:hidden; border-radius:3px; }
.track button { position:absolute; top:2px; bottom:2px; min-width:2px; border:0; background:#6a99ff; cursor:pointer; }
.track button.active { background:#2278ff; }
.hours { display:flex; justify-content:space-between; font-size:10px; color:#929cab; margin-top:5px; }
@media(max-width:1100px) { .date-panels { grid-template-columns:150px 160px minmax(240px,1fr); gap:16px; } .media-layout { grid-template-columns:160px minmax(0,1fr); gap:20px; } }
@media(max-width:720px) { .playback-workspace { grid-template-columns:200px minmax(0,1fr); } .device-list { padding:12px; } .date-panels { grid-template-columns:1fr 1fr; } .dates { grid-column:1 / -1; } .media-layout { grid-template-columns:110px minmax(0,1fr); gap:12px; } .source-toolbar .el-select { width:170px; } }
</style>
<style>
.el-dialog.playback-dialog { background:#f1f4f8; border-radius:6px; padding:22px 24px; }
.playback-dialog .el-dialog__body { padding-top:12px; }
</style>
