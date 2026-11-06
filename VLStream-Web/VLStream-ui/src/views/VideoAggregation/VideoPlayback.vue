<template>
  <div class="playback-workspace">
    <aside class="directory">
      <h2>{{ $tp('视频回放') }}</h2>
      <el-input v-model="search" clearable :placeholder="$tp('搜索设备名称 / ID')" />
      <VideoSquareClassificationTree :devices="devices" :visible-devices="visibleDevices" :selected-device-id="device?.id" show-tree
        @device-click="selectDevice" @filter-change="categoryIds = $event" />
      <el-alert v-if="catalogError" :title="catalogError" type="error" :closable="false" />
      <el-button v-if="catalogError" @click="loadCatalog">{{ $tp('重试') }}</el-button>
      <small class="directory-footer">{{ $tp('平台录像 · 保留 7 天') }}</small>
    </aside>
    <main v-loading="loading">
      <header><div><h2>{{ device?.deviceName || $tp('选择设备查看录像') }}</h2><small>{{ $tp('平台录像 · 北京时间') }}</small></div>
        <PlatformRecordingActions v-if="source" :protocol="source.protocol" :row-id="source.rowId" :online="source.state === 'ONLINE'" @changed="loadSources" />
      </header>
      <div class="filters">
        <el-select v-if="device?.sources?.length > 1" v-model="sourceKey" :aria-label="$tp('设备来源')" @change="loadSources">
          <el-option v-for="item in device.sources" :key="key(item)" :value="key(item)" :label="`${item.protocol} · ${item.deviceName || item.rowId}`" />
        </el-select>
        <el-select v-model="sourceId" :placeholder="$tp('选择录像通道')" @change="loadSegments">
          <el-option v-for="item in sources" :key="item.id" :value="item.id" :label="item.source?.channelName || `${item.source?.name || $tp('设备主码流')} ${item.stream_id}`" />
        </el-select>
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" :disabled-date="value => value.getTime() > Date.now()" @change="loadSegments" />
        <el-button :disabled="!source" @click="refresh">{{ $tp('刷新') }}</el-button>
        <el-tag v-if="recordSource" :type="recordSource.state === 'RECORDING' ? 'success' : 'info'">{{ $tp(recordingState(recordSource)) }}</el-tag>
      </div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <section class="screen">
        <video v-if="url" ref="video" :src="url" controls playsinline @loadedmetadata="mediaReady" @timeupdate="updateTime" @ended="next" @error="error = $tp('录像解码失败，请下载片段检查或选择其他录像')" />
        <div v-else class="empty"><span>▷</span><strong>{{ $tp(loadingMedia ? '正在加载录像' : device ? '当前没有可播放的录像' : '请选择左侧设备') }}</strong>
          <p v-if="device && !loadingMedia">{{ $tp('开启手动录制或定时计划后，已保存的片段会显示在这里。') }}</p>
        </div>
      </section>
      <section class="timeline">
        <header><strong>{{ currentTime || '00:00:00' }}</strong><small>{{ $tp('蓝色表示已保存录像，空白表示无录像') }}</small>
          <el-select v-model="speed" size="small" :aria-label="$tp('播放速度')" @change="setSpeed"><el-option v-for="rate in [0.5, 1, 1.5, 2, 4]" :key="rate" :value="rate" :label="`${rate}×`" /></el-select>
        </header>
        <div class="track" role="group" :aria-label="$tp('录像时间轴')">
          <button v-for="segment in segments" :key="segment.id" :style="segmentStyle(segment)" :class="{ active: active?.id === segment.id }" :title="range(segment)" :aria-label="range(segment)" @click="play(segment)" />
        </div>
        <div class="hours"><span v-for="hour in [0, 4, 8, 12, 16, 20, 24]" :key="hour">{{ String(hour).padStart(2, '0') }}:00</span></div>
      </section>
      <section><header><strong>{{ $tp('录像片段') }} <small>{{ segments.length }}</small></strong><el-button :disabled="!active || loadingMedia" @click="download">{{ $tp('下载当前片段') }}</el-button></header>
        <el-empty v-if="!segments.length" :image-size="40" :description="$tp('当天暂无录像')" />
        <div v-else class="segments"><button v-for="segment in segments" :key="segment.id" :class="{ selected: active?.id === segment.id }" @click="play(segment)">
          <span>▷ {{ range(segment) }}</span><small>{{ (Number(segment.size_bytes) / 1048576).toFixed(1) }} MB</small>
        </button></div>
      </section>
    </main>
  </div>
</template>
<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { translatePhrase as t } from '@/i18n'
import { getVideoDeviceCatalog } from '@/api/videoDeviceCatalog'
import { matchesVideoDevice } from '@/utils/videoSquareCatalog'
import { platformRecording as api, recordingError, recordingState } from '@/api/platformRecording'
import VideoSquareClassificationTree from './components/VideoSquareClassificationTree.vue'
import PlatformRecordingActions from '@/components/PlatformRecordingActions.vue'
const route = useRoute()
const devices = ref([]), device = ref(null), search = ref(''), categoryIds = ref(null)
const catalogError = ref(''), error = ref(''), loading = ref(false), loadingMedia = ref(false)
const sourceKey = ref(''), sources = ref([]), sourceId = ref(''), segments = ref([]), active = ref(null)
const video = ref(null), url = ref(''), speed = ref(1), currentTime = ref('')
const date = ref(new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date()))
const key = item => `${item.protocol}:${item.rowId}`
const source = computed(() => device.value?.sources?.find(item => key(item) === sourceKey.value))
const recordSource = computed(() => sources.value.find(item => item.id === sourceId.value))
const visibleDevices = computed(() => devices.value.filter(item => matchesVideoDevice(item, search.value) && (!categoryIds.value || categoryIds.value.includes(item.id))))
const dayStart = computed(() => new Date(`${date.value}T00:00:00+08:00`).getTime())
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
async function selectDevice(value, preferred) { device.value = value; sourceKey.value = preferred || key(value.sources[0]); await loadSources() }
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
function updateTime() { if (active.value) currentTime.value = clock(Number(active.value.start_ms) + (video.value?.currentTime || 0) * 1000) }
function next() { const index = segments.value.findIndex(item => item.id === active.value?.id); if (index >= 0 && index + 1 < segments.value.length) play(segments.value[index + 1]) }
function download() { if (url.value && active.value) { const a = document.createElement('a'); a.href = url.value; a.download = `${active.value.id}.mp4`; a.click() } }
async function refresh() {
  if (!source.value || loading.value || polling) return
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
.playback-workspace { display: grid; grid-template-columns: 260px minmax(0, 1fr); height: calc(100vh - 96px); min-height: 640px; background: #fff; border-radius: 16px; overflow: hidden; color: #202b40; }
.directory { padding: 24px 16px 16px; border-inline-end: 1px solid #edf0f5; display: flex; flex-direction: column; gap: 20px; overflow: auto; }
h2 { margin: 0; font-size: 18px; font-weight: 600; }
.directory :deep(.square-classification) { padding-inline: 0; }
.directory :deep(.classification-tree) { max-height: none; }
.directory-footer { margin-top: auto; padding-top: 16px; }
small { color: #8792a5; }
main { padding: 24px; display: flex; flex-direction: column; gap: 18px; overflow: auto; }
header, .filters { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
header > div > small { display: block; margin-top: 6px; }
.filters { justify-content: flex-start; flex-wrap: wrap; }
.filters .el-select { width: 190px; }
.screen { min-height: 280px; flex: 1; background: #111725; border-radius: 10px; display: grid; place-items: center; overflow: hidden; }
.screen video { width: 100%; height: 100%; min-height: 0; max-height: 56vh; object-fit: contain; }
.empty { color: #b0bacd; text-align: center; padding: 28px; }
.empty > span { display: block; font-size: 54px; color: #687892; margin-bottom: 14px; }
.empty p { font-size: 13px; }
.timeline header { margin-bottom: 14px; }
.timeline strong { font-variant-numeric: tabular-nums; }
.timeline header small { margin-inline-end: auto; }
.timeline .el-select { width: 80px; }
.track { height: 28px; background: #edf1f7; position: relative; border-radius: 4px; overflow: hidden; }
.track button { position: absolute; top: 4px; bottom: 4px; background: #648cff; border: 0; cursor: pointer; min-width: 2px; }
.track button.active, .track button:hover { background: #2055ed; }
.hours { display: flex; justify-content: space-between; color: #8792a5; font-size: 11px; margin-top: 8px; }
.segments { max-height: 150px; overflow: auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 6px; margin-top: 12px; }
.segments button { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 12px; background: #f7f9fc; border: 1px solid transparent; border-radius: 4px; cursor: pointer; color: #44516b; }
.segments button.selected { border-color: #638aff; background: #eef3ff; color: #2458ef; }
section :deep(.el-empty) { padding: 8px 0; }
@media (max-width: 1000px) { .playback-workspace { grid-template-columns: 220px minmax(0, 1fr); } main { padding: 16px; } }
@media (max-width: 720px) { .playback-workspace { display: flex; flex-direction: column; height: auto; } .directory { max-height: 260px; } main { overflow: visible; } .timeline header small { display: none; } }
</style>
