<template>
    <div class="event-recorded-videos" :aria-busy="state.status === 'loading'">
        <div v-if="state.status === 'loading'" class="event-video-placeholder">
            <span class="event-video-placeholder__text">{{ $tp('视频加载中') }}</span>
        </div>
        <div v-else-if="state.status === 'error'" class="event-video-placeholder" role="alert">
            <span class="event-video-placeholder__text">{{ $tp('视频加载失败，请重试') }}</span>
            <el-button class="event-video-retry" link type="primary" @click="refreshVideos">{{ $tp('重试') }}</el-button>
        </div>
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
            <div v-if="playbackFailed" class="event-video-alert" role="alert">{{ $tp('视频无法播放，请刷新视频后重试') }}</div>
            <div class="event-video-actions">
                <el-button link type="primary" @click="refreshVideos">{{ $tp('刷新视频') }}</el-button>
                <el-button link type="primary" @click="downloadEventVideo({ videoUrl: currentVideo.url, videoMediaId: currentVideo.videoMediaId })">{{ $tp('下载') }}</el-button>
            </div>
        </template>
        <div v-else class="event-video-placeholder">
            <svg class="event-video-placeholder__icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <rect x="2" y="5" width="20" height="14" rx="3" stroke="currentColor" stroke-width="1.6" />
                <path d="M9.5 9.5l5 2.5-5 2.5v-5z" fill="currentColor" />
                <path d="M3 3l18 18" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
            </svg>
            <span class="event-video-placeholder__text">{{ $tp('暂无视频') }}</span>
        </div>
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
.event-recorded-videos { display: flex; flex-direction: column; gap: 12px; width: 100%; }

/* 播放器：按 16:9 撑满可用宽度并水平居中，同时预留弹窗头部与操作区高度，避免出现纵向滚动 */
.event-recorded-video {
    display: block;
    width: min(100%, calc((90vh - 260px) * 16 / 9));
    height: auto;
    aspect-ratio: 16 / 9;
    margin-inline: auto;
    background: #0b0d12;
    border-radius: 8px;
    object-fit: contain;
    box-shadow: 0 6px 20px rgb(16 24 40 / 8%);
}

.event-video-choices, .event-video-actions { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; }

/* 弹窗全局“大按钮”规则（.el-dialog__body>div>div>.el-button）会把这里的按钮变成 170×48 的纯主色块，这里显式还原为克制的按钮样式 */
.event-video-choices .el-button {
    width: auto !important;
    min-width: 72px !important;
    height: 32px !important;
    margin-left: 0 !important;
    padding: 0 14px !important;
    border-radius: 6px !important;
}

.event-video-actions .el-button, .event-video-retry.el-button {
    width: auto !important;
    min-width: 0 !important;
    height: 32px !important;
    margin-left: 0 !important;
    padding: 0 12px !important;
    border: 1px solid var(--el-border-color) !important;
    border-radius: 6px !important;
    background: #fff !important;
    color: var(--el-color-primary) !important;
    transition: background-color .2s, border-color .2s, color .2s;
}

.event-video-actions .el-button:hover, .event-video-retry.el-button:hover {
    border-color: var(--el-color-primary-light-5) !important;
    background: var(--el-color-primary-light-9) !important;
    color: var(--el-color-primary) !important;
}

.event-video-alert {
    padding: 8px 12px;
    border-radius: 6px;
    background: var(--el-color-danger-light-9);
    color: var(--el-color-danger);
    font-size: 13px;
    text-align: center;
}

/* 加载中 / 失败 / 暂无视频 统一成同一尺寸的浅灰卡片，避免空态塌成一行文字 */
.event-video-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 10px;
    width: min(100%, calc((90vh - 260px) * 16 / 9));
    aspect-ratio: 16 / 9;
    margin-inline: auto;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    background: #f7f8fa;
    color: var(--el-text-color-secondary);
}

.event-video-placeholder__icon { width: 40px; height: 40px; color: var(--el-text-color-placeholder); }
.event-video-placeholder__text { font-size: 14px; }
</style>
