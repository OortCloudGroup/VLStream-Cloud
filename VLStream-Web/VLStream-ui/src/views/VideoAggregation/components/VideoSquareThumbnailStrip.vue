<template>
    <nav class="square-thumbnail-strip" :aria-label="$tp('视频缩略图')">
        <button type="button" class="strip-arrow" :aria-label="$tp('向左滚动缩略图')" :disabled="!canLeft"
            @pointerenter="hoverScroll.start(-1)" @pointerleave="hoverScroll.stop" @pointercancel="hoverScroll.stop" @blur="hoverScroll.stop" @click="scrollPage(-1)">‹</button>
        <div ref="scrollElement" class="square-thumbnail-track" @scroll="updateEdges">
        <button v-for="device in devices" :key="device.id" type="button" class="square-thumbnail"
            :class="{ active: device.id === selectedId }" :aria-pressed="device.id === selectedId"
            :aria-label="device.deviceName || device.name" @click="$emit('select', device.id)">
            <VideoSquareDevicePlayer v-if="autoPreview && device.id !== selectedId && device.status !== 'OFFLINE'" class="thumbnail-live-player" :device="device" @playing.capture="mutePreview" @loadedmetadata.capture="mutePreview" />
            <img v-else-if="frames[device.id]" :src="frames[device.id]" alt="" />
            <span v-else class="thumbnail-unavailable">{{ $tp(device.status === 'OFFLINE' ? '无信号' : '未预览') }}</span>
            <span class="thumbnail-name" data-i18n-ignore>{{ device.deviceName || device.name }}</span>
        </button>
        <span v-if="!devices.length" class="thumbnail-empty">{{ $tp('暂无设备') }}</span>
        </div>
        <button type="button" class="strip-arrow" :aria-label="$tp('向右滚动缩略图')" :disabled="!canRight"
            @pointerenter="hoverScroll.start(1)" @pointerleave="hoverScroll.stop" @pointercancel="hoverScroll.stop" @blur="hoverScroll.stop" @click="scrollPage(1)">›</button>
    </nav>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useHoverScroll } from '@/composables/useHoverScroll'
import VideoSquareDevicePlayer from './VideoSquareDevicePlayer.vue'
const props = defineProps({ devices: { type: Array, default: () => [] }, selectedId: { type: [String, Number], default: '' }, frames: { type: Object, default: () => ({}) }, autoPreview: Boolean })
const scrollElement = ref(null)
const canLeft = ref(false)
const canRight = ref(false)
const hoverScroll = useHoverScroll(scrollElement)
const updateEdges = () => {
    const el = scrollElement.value
    canLeft.value = !!el && el.scrollLeft > 1
    canRight.value = !!el && el.scrollLeft < el.scrollWidth - el.clientWidth - 1
}
const scrollPage = direction => scrollElement.value?.scrollBy({ left: direction * scrollElement.value.clientWidth * 0.8, behavior: 'smooth' })
let observer
onMounted(() => {
    observer = new ResizeObserver(updateEdges)
    observer.observe(scrollElement.value)
    updateEdges()
})
watch(() => props.devices.length, () => nextTick(updateEdges))
onBeforeUnmount(() => observer?.disconnect())
defineEmits(['select'])
function mutePreview(event) {
    if (event.target instanceof HTMLMediaElement) event.target.muted = true
}
</script>

<style scoped>
.square-thumbnail-strip { display: flex; flex: 0 0 auto; min-width: 0; max-width: 100%; gap: 8px; padding: 4px 2px 12px; overflow: hidden; }
.square-thumbnail-track { display:flex; flex:1; min-width:0; gap:20px; overflow-x:auto; overscroll-behavior:contain; scrollbar-width:none; }
.square-thumbnail-track::-webkit-scrollbar { display:none; }
.strip-arrow { flex:0 0 28px; align-self:stretch; border:0; border-radius:6px; padding:0; background:#171d26; color:#99aec7; font-size:28px; cursor:pointer; transition:background .15s,color .15s; }
.strip-arrow:hover:not(:disabled), .strip-arrow:focus-visible { background:#20334e; color:#70aaff; }
.strip-arrow:disabled { opacity:.25; cursor:default; }
.square-thumbnail { position: relative; flex: 0 0 clamp(90px, 12vw, 140px); width: clamp(90px, 12vw, 140px); aspect-ratio: 1; overflow: hidden; padding: 0; border: 2px solid transparent; border-radius: 8px; background: #171b22; cursor: pointer; color: #a7b0bd; }
.square-thumbnail.active { border-color: #026fff; }
.square-thumbnail:hover,.square-thumbnail:focus-visible { outline: 2px solid #f5222d; outline-offset: -2px; }
.square-thumbnail img { width: 100%; height: 100%; display: block; object-fit: cover; }
.thumbnail-unavailable { font-size: 12px; }
.thumbnail-name { position: absolute; inset: auto 0 0; padding: 5px 6px; background: rgb(0 0 0 / 55%); color: #fff; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; overflow: hidden; }
.thumbnail-empty { padding: 12px; color: #778da2; font-size: 12px; }
.thumbnail-live-player { position: absolute; inset: 0; pointer-events: none; }
.thumbnail-live-player :deep(.source-selector), .thumbnail-live-player :deep(.player-overlay), .thumbnail-live-player :deep(.oplayer-controller) { display: none; }
/* Live previews and the selected camera's captured frame use the same square viewport. */
.thumbnail-live-player :deep(.direct-player),
.thumbnail-live-player :deep(.oplayer-container) { width: 100%; height: 100%; min-height: 0; }
.thumbnail-live-player :deep(video) { display: block; width: 100%; height: 100%; max-height: none; object-fit: cover; }
.thumbnail-name { z-index: 2; }
</style>
