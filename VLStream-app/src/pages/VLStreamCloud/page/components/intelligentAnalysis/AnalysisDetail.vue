<template>
  <div class="analysis-detail">
    <nav-header-banner title="查看视频" :is-call-back="true" @call-back="onBack" />

    <div class="detail-body">
      <!-- 视频播放器 -->
      <div class="player-wrap">
        <video
          v-if="videoSrc"
          ref="videoPlayer"
          :src="videoSrc"
          class="player-video"
          controls
          @loadedmetadata="onVideoLoaded"
        />
        <div v-else class="player-placeholder">
          <img v-if="heroImg" :src="heroImg" alt="" class="player-img" />
          <div class="player-overlay">
            <img :src="playImg" alt="播放" class="player-icon" />
          </div>
        </div>
      </div>

      <!-- Tab 切换 -->
      <van-tabs v-model:active="activeTab" class="content-tabs" line-width="28">
        <van-tab title="视频列表" name="video">
          <VideoListTab :task="task" @play="onPlay" />
        </van-tab>
        <van-tab title="申请详情" name="detail">
          <ApplicationDetailTab :task="task" />
        </van-tab>
      </van-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import NavHeaderBanner from '@/components/navHeaderBanner/index.vue'
import playImg from '@/assets/img/VLStreamCloud/play.png'
import VideoListTab from './VideoListTab.vue'
import ApplicationDetailTab from './ApplicationDetailTab.vue'

interface TaskInfo {
  id: number
  title: string
  type: string
  area: string
  thumb: string
  cameras: string
  applyTime: string
  status: string
}

const _props = defineProps<{
  task: TaskInfo
}>()

const emit = defineEmits(['close'])

const heroImg = ref<string>('')
const activeTab = ref<'video' | 'detail'>('video')

const videoSrc = ref<string>('')
const videoPlayer = ref<HTMLVideoElement | null>(null)

// 接收子组件 VideoListTab 抛出的播放地址和封面图
const onPlay = (payload: any) => {
  let src = ''
  let thumb = ''

  if (typeof payload === 'string') {
    src = payload
  } else if (payload && typeof payload === 'object') {
    src = payload.src || ''
    thumb = payload.thumb || ''
  }

  // 释放旧的 blob URL
  if (videoSrc.value && videoSrc.value.startsWith('blob:')) {
    URL.revokeObjectURL(videoSrc.value)
  }
  videoSrc.value = src

  // hero 图使用当前片段缩略图（默认就是第一个列表图片）
  if (thumb) {
    heroImg.value = thumb
  }
}

const onVideoLoaded = () => {
  if (videoPlayer.value) {
    videoPlayer.value.play().catch(() => {
      // 自动播放失败时，用户可手动点击播放
    })
  }
}

onBeforeUnmount(() => {
  if (videoSrc.value && videoSrc.value.startsWith('blob:')) {
    URL.revokeObjectURL(videoSrc.value)
  }
})

const onBack = () => {
  emit('close')
}
</script>

<style scoped lang="scss">
.analysis-detail {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f9faff;
}

.detail-body {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.player-wrap {
  position: relative;
  width: 100%;
  height: 188px;
}

.player-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

.player-placeholder {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
}

.player-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.player-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 2;
}

.player-icon {
  width: 60px;
  height: 60px;
  object-fit: contain;
  opacity: 0.9;
}

.content-tabs {
  --van-tabs-bottom-bar-color: #2f69f8;
  padding: 0 16px;

  :deep(.van-tabs__nav) {
    justify-content: flex-start;
    padding-left: 0;
    padding-right: 0;
    background: transparent;
  }

  :deep(.van-tab) {
    flex: 0 0 auto;
    line-height: 24px;
    font-weight: 500;
    font-size: 16px;
  }

  :deep(.van-tab--active) {
    line-height: 24px;
    color: #2f69f8;
    font-size: 16px;
  }
}
</style>
