<template>
    <div class="event-recorded-videos" :aria-busy="state.status === 'loading'">
        <span v-if="state.status === 'loading'">{{ $tp('视频加载中') }}</span>
        <template v-else-if="state.status === 'error'">
            <span role="alert">{{ $tp('视频加载失败，请重试') }}</span>
            <el-button class="event-video-retry" link type="primary" @click="refreshVideos">{{ $tp('重试') }}</el-button>
        </template>
        <template v-else-if="currentVideo">
            <div v-if="state.videos.length > 1" class="event-video-choices" :aria-label="$tp('录制视频')">
                <el-button v-for="(video, index) in state.videos" :key="video.key"
                    :type="selectedIndex === index ? 'primary' : 'default'" size="small"
                    :aria-pressed="selectedIndex === index" @click="selectedIndex = index">
                    {{ $tp('视频') }} {{ index + 1 }}
                </el-button>
            </div>
            <video ref="videoRef" :key="currentVideo.url" :src="currentVideo.url"
                :data-event-id="state.eventId" :aria-label="$tp('录制视频')"
                class="event-recorded-video" controls playsinline preload="metadata" @error="playbackFailed = true" />
            <div v-if="playbackFailed" role="alert">{{ $tp('视频无法播放，请刷新视频后重试') }}</div>
            <div class="event-video-actions">
                <el-button link type="primary" @click="refreshVideos">{{ $tp('刷新视频') }}</el-button>
                <el-button link type="primary" @click="downloadEventVideo({ videoUrl: currentVideo.url, videoMediaId: currentVideo.videoMediaId })">{{ $tp('下载') }}</el-button>
            </div>
        </template>
        <span v-else>{{ $tp('暂无视频') }}</span>
    </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { eventDetail } from '@/api/smartCity/events'
import { downloadEventVideo } from '@/api/eventMedia'
import { useUserStore } from '@/store/modules/useraPaas'
import { createEventVideoLoader, releaseEventVideo } from './eventVideos.mjs'

const props = defineProps({
    eventId: { type: [String, Number], default: '' },
    active: { type: Boolean, default: false }
})
const store = useUserStore()
const identity = computed(() => `${store.token || ''}:${store.tenantId || store.userInfo?.tenantId || store.userInfo?.tenant_id || ''}:${store.userInfo?.user_id || store.userInfo?.userId || store.userInfo?.id || ''}`)
const state = ref({ status: 'idle', eventId: '', videos: [] })
const selectedIndex = ref(0)
const videoRef = ref(null)
const playbackFailed = ref(false)
const currentVideo = computed(() => state.value.videos[selectedIndex.value])
let preferredVideoKey
const loader = createEventVideoLoader(eventDetail, () => identity.value, nextState => {
    releaseEventVideo(videoRef.value)
    state.value = nextState
    selectedIndex.value = Math.max(0, nextState.videos.findIndex(video => video.key === preferredVideoKey))
    playbackFailed.value = false
})
const refreshVideos = () => {
    preferredVideoKey = currentVideo.value?.key || preferredVideoKey
    if (props.active && props.eventId !== '') return loader.load(props.eventId)
}
watch(selectedIndex, () => {
    releaseEventVideo(videoRef.value)
    playbackFailed.value = false
}, { flush: 'sync' })
watch([() => props.active, () => props.eventId, identity], ([active, eventId]) => {
    preferredVideoKey = undefined
    if (active && eventId !== '') loader.load(eventId)
    else loader.invalidate()
}, { immediate: true, flush: 'sync' })
onBeforeUnmount(() => loader.invalidate())
</script>

<style scoped>
.event-recorded-videos { width: 100%; max-width: 560px; }
.event-recorded-video { display: block; width: 100%; max-height: 340px; background: #000; border-radius: 4px; }
.event-video-choices, .event-video-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-block: 6px; }
.event-video-choices .el-button, .event-video-actions .el-button { margin-left: 0; }
.event-video-choices .el-button { width: auto; min-width: 72px; height: 28px; padding: 0 10px; }
.event-video-actions .el-button, .event-video-retry.el-button { width: auto; min-width: 0; height: auto; padding: 4px 0; color: var(--el-color-primary); background: transparent; border: 0; }
</style>
