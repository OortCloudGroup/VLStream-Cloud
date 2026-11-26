<template>
    <nav class="square-thumbnail-strip" :aria-label="$tp('视频缩略图')"
        @pointerenter="marquee.setPaused('pointer', true)" @pointerleave="marquee.setPaused('pointer', false)"
        @pointercancel="marquee.setPaused('pointer', false)" @focusin="marquee.trackFocus" @focusout="marquee.trackFocus">
        <button type="button" class="strip-arrow" :aria-label="$tp('向左滚动缩略图')" :disabled="!canLeft"
            @pointerenter="hoverScroll.start(-1)" @pointerleave="hoverScroll.stop" @pointercancel="hoverScroll.stop" @blur="hoverScroll.stop" @click="scrollPage(-1)">‹</button>
        <div ref="scrollElement" class="square-thumbnail-track" @scroll="syncScroll">
        <div ref="laneElement" class="square-thumbnail-lane" :style="laneStyle">
        <button v-for="(device, index) in devices" :key="device.id" :ref="element => observeThumbnail(device.id, element)"
            type="button" class="square-thumbnail" :style="thumbnailPosition(index)"
            :class="{ active: device.id === selectedId }" :aria-pressed="device.id === selectedId"
            :aria-label="device.deviceName || device.name" data-i18n-ignore-attributes @click="$emit('select', device.id)">
            <img v-if="device.status !== 'OFFLINE' && frames[device.id]" :src="frames[device.id]" alt="" />
            <span v-else class="thumbnail-unavailable">{{ $tp(unavailablePreviewIds.has(device.id) ? '无信号' : '未预览') }}</span>
            <span :ref="element => emit('preview-target', device.id, element)" :data-thumbnail-id="device.id"
                class="thumbnail-live-player" @playing.capture="mutePreview" @loadedmetadata.capture="mutePreview"></span>
            <span class="thumbnail-name" data-i18n-ignore>{{ device.deviceName || device.name }}</span>
        </button>
        <span v-if="!devices.length" class="thumbnail-empty">{{ $tp('暂无设备') }}</span>
        </div>
        </div>
        <button type="button" class="strip-arrow" :aria-label="$tp('向右滚动缩略图')" :disabled="!canRight"
            @pointerenter="hoverScroll.start(1)" @pointerleave="hoverScroll.stop" @pointercancel="hoverScroll.stop" @blur="hoverScroll.stop" @click="scrollPage(1)">›</button>
    </nav>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useHoverScroll } from '@/composables/useHoverScroll'
import { useVideoMarquee, videoStripLoopWidth, videoStripWrappedCount } from '@/composables/useVideoMarquee.mjs'
import { canPreviewVideoSource } from '@/utils/videoSquareCatalog'
const props = defineProps({ devices: { type: Array, default: () => [] }, selectedId: { type: [String, Number], default: '' }, frames: { type: Object, default: () => ({}) }, autoPreview: Boolean })
const emit = defineEmits(['select', 'preview-target', 'live-previews'])
const scrollElement = ref(null)
const laneElement = ref(null)
const PREVIEW_SETTLE_MS = 400
const MIN_VISIBLE_RATIO = 0.01
const pageVisible = ref(typeof document === 'undefined' || !document.hidden)
const canObserveThumbnails = typeof IntersectionObserver !== 'undefined'
const thumbnailElements = new Map()
const thumbnailIds = new WeakMap()
const visibleThumbnailIds = ref(new Set())
let thumbnailObserver

function observeThumbnail(id, element) {
    const previous = thumbnailElements.get(id)
    if (previous === element) return
    if (previous) {
        thumbnailObserver?.unobserve(previous)
        thumbnailIds.delete(previous)
        thumbnailElements.delete(id)
        visibleThumbnailIds.value.delete(id)
    }
    if (element) {
        thumbnailElements.set(id, element)
        thumbnailIds.set(element, id)
        thumbnailObserver?.observe(element)
    }
}

function updateThumbnailVisibility(entries) {
    for (const entry of entries) {
        if (!thumbnailIds.has(entry.target)) continue
        const id = thumbnailIds.get(entry.target)
        if (entry.isIntersecting && entry.intersectionRatio >= MIN_VISIBLE_RATIO) {
            visibleThumbnailIds.value.add(id)
        } else {
            visibleThumbnailIds.value.delete(id)
        }
    }
}

const unavailablePreviewIds = computed(() => new Set(props.devices
    .filter(device => device.status === 'OFFLINE' || !device.sources?.some(source =>
        source.protocol === 'VLSTREAM' || canPreviewVideoSource(source)))
    .map(device => device.id)))
const eligiblePreviewIds = computed(() => props.autoPreview && pageVisible.value
    ? new Set(props.devices.filter(device => !unavailablePreviewIds.value.has(device.id) &&
        (!canObserveThumbnails || visibleThumbnailIds.value.has(device.id))).map(device => device.id))
    : new Set())
const livePreviewIds = ref(new Set())
let settleTimer
watch([eligiblePreviewIds, () => props.selectedId], ([ids, selectedId], [previous, previousSelected]) => {
    if (previousSelected === selectedId && previous?.size === ids.size && [...ids].every(id => previous.has(id))) return
    clearTimeout(settleTimer)
    const current = livePreviewIds.value
    const remaining = new Set([...current].filter(id => ids.has(id)))
    // A visible main player is already running and can become a thumbnail immediately.
    if (ids.has(selectedId)) remaining.add(selectedId)
    if (remaining.size !== current.size || [...remaining].some(id => !current.has(id))) livePreviewIds.value = remaining
    // Leaving the viewport releases immediately. Only starting newcomers waits
    // for scrolling and catalogue updates to settle.
    if (remaining.size < ids.size) {
        settleTimer = setTimeout(() => { livePreviewIds.value = ids }, PREVIEW_SETTLE_MS)
    }
}, { immediate: true })
watch(livePreviewIds, ids => emit('live-previews', ids))
const syncVisibility = () => { pageVisible.value = !document.hidden }
const canLeft = ref(false)
const canRight = ref(false)
const hoverScroll = useHoverScroll(scrollElement)
const viewportWidth = ref(0)
const thumbnailWidth = ref(0)
const thumbnailGap = ref(0)
const loopWidth = computed(() => videoStripLoopWidth(props.devices.length, thumbnailWidth.value, viewportWidth.value, thumbnailGap.value))
const marquee = useVideoMarquee(scrollElement, loopWidth)
const laneStyle = computed(() => marquee.period.value ? { width: `${marquee.period.value + viewportWidth.value}px` } : undefined)
const wrappedCount = computed(() => videoStripWrappedCount(thumbnailWidth.value, marquee.offset.value, marquee.period.value, thumbnailGap.value))
const wrappedStyle = computed(() => ({ transform: `translateX(${marquee.period.value}px)` }))
const thumbnailPosition = index => index < wrappedCount.value ? wrappedStyle.value : undefined
const updateEdges = () => {
    const el = scrollElement.value
    canLeft.value = !!el && el.scrollLeft > 1
    canRight.value = !!el && el.scrollLeft < el.scrollWidth - el.clientWidth - 1
}
const syncScroll = () => {
    marquee.syncOffset()
    updateEdges()
}
const measureStrip = () => {
    const track = scrollElement.value
    const lane = laneElement.value
    if (!track || !lane) return
    viewportWidth.value = track.clientWidth
    thumbnailWidth.value = lane.querySelector('.square-thumbnail')?.getBoundingClientRect().width || 0
    thumbnailGap.value = parseFloat(getComputedStyle(lane).columnGap) || 0
    nextTick(syncScroll)
}
const scrollPage = direction => scrollElement.value?.scrollBy({ left: direction * scrollElement.value.clientWidth * 0.8, behavior: 'smooth' })
let observer
onMounted(() => {
    observer = new ResizeObserver(measureStrip)
    observer.observe(scrollElement.value)
    if (laneElement.value) observer.observe(laneElement.value)
    if (canObserveThumbnails) {
        // The viewport observer accounts for overflow clipping and marquee transforms.
        thumbnailObserver = new IntersectionObserver(updateThumbnailVisibility, { threshold: MIN_VISIBLE_RATIO })
        for (const element of thumbnailElements.values()) thumbnailObserver.observe(element)
    }
    measureStrip()
    document.addEventListener('visibilitychange', syncVisibility)
})
watch(() => props.devices.length, () => nextTick(measureStrip))
onBeforeUnmount(() => {
    observer?.disconnect()
    thumbnailObserver?.disconnect()
    clearTimeout(settleTimer)
    document.removeEventListener('visibilitychange', syncVisibility)
    emit('live-previews', new Set())
})
function mutePreview(event) {
    if (event.target instanceof HTMLMediaElement) event.target.muted = true
}
</script>

<style scoped>
.square-thumbnail-strip { display: flex; flex: 0 0 auto; min-width: 0; max-width: 100%; gap: 8px; padding: 4px 2px 12px; overflow: hidden; }
.square-thumbnail-track { flex:1; min-width:0; overflow-x:auto; overscroll-behavior:contain; scrollbar-width:none; }
.square-thumbnail-track::-webkit-scrollbar { display:none; }
.square-thumbnail-lane { display:flex; width:max-content; min-width:100%; gap:20px; overflow:clip; }
.strip-arrow { flex:0 0 28px; align-self:stretch; border:0; border-radius:6px; padding:0; background:#171d26; color:#99aec7; font-size:28px; cursor:pointer; transition:background .15s,color .15s; }
.strip-arrow:hover:not(:disabled), .strip-arrow:focus-visible { background:#20334e; color:#70aaff; }
.strip-arrow:disabled { opacity:.25; cursor:default; }
.square-thumbnail { position: relative; flex: 0 0 clamp(90px, 12vw, 140px); width: clamp(90px, 12vw, 140px); aspect-ratio: 1; overflow: hidden; padding: 0; border: 2px solid transparent; border-radius: 8px; background: #171b22; cursor: pointer; color: #a7b0bd; }
.square-thumbnail.active { border-color: #026fff; }
.square-thumbnail:hover,.square-thumbnail:focus-visible { outline: 2px solid #f5222d; outline-offset: -2px; }
.square-thumbnail img { width: 100%; height: 100%; display: block; object-fit: cover; }
.thumbnail-unavailable { font-size: 12px; }
.thumbnail-name { position: absolute; z-index: 1; inset: auto 0 0; padding: 5px 6px; background: rgb(0 0 0 / 55%); color: #fff; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; overflow: hidden; }
.thumbnail-empty { padding: 12px; color: #778da2; font-size: 12px; }
.thumbnail-live-player { position: absolute; z-index: 0; inset: 0; pointer-events: none; }
.square-thumbnail:has(.square-device-player) > .thumbnail-unavailable { visibility: hidden; }
.thumbnail-live-player :deep(.source-selector), .thumbnail-live-player :deep(.player-overlay), .thumbnail-live-player :deep(.oplayer-controller) { display: none; }
/* Live previews and the selected camera's captured frame use the same square viewport. */
.thumbnail-live-player :deep(.direct-player),
.thumbnail-live-player :deep(.oplayer-container) { width: 100%; height: 100%; min-height: 0; }
.thumbnail-live-player :deep(.rtsp-player-container) { max-width: 100%; max-height: 100%; }
.thumbnail-live-player :deep(video) { display: block; width: 100%; height: 100%; max-height: none; object-fit: cover; }
.thumbnail-live-player :deep(.el-loading-mask) {
    z-index: 1;
    background: rgb(8 12 18 / 86%);
    backdrop-filter: none;
}
.thumbnail-live-player :deep(.el-loading-spinner) {
    inset: 0;
    margin: 0;
    display: grid;
    place-items: center;
}
/* Keep a full ring visible even if a slow frame stalls the animation. */
.thumbnail-live-player :deep(.el-loading-spinner .circular) {
    box-sizing: border-box;
    width: 28px;
    height: 28px;
    border: 3px solid #65768d;
    border-top-color: #83c7ff;
    border-radius: 50%;
    animation: thumbnail-spin 1s linear infinite;
}
.thumbnail-live-player :deep(.el-loading-spinner .path) { display: none; }
@keyframes thumbnail-spin {
    to { transform: rotate(360deg); }
}
@media (prefers-reduced-motion: reduce) {
    .thumbnail-live-player :deep(.el-loading-spinner .circular) { animation: none; }
}
.thumbnail-name { z-index: 2; }
</style>
