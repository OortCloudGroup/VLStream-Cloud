<template>
  <div class="playback-detail">
    <nav-header-banner title="查看回放" :is-call-back="true" @call-back="onBack" />

    <div class="detail-body">
      <!-- 日期选择 -->
      <div class="date-row">
        <div class="date-item" @click="onPick('year')">
          {{ currentYear }}年
          <van-icon name="arrow-down" />
        </div>
        <div class="date-item" @click="onPick('month')">
          {{ currentMonth }}月
          <van-icon name="arrow-down" />
        </div>
        <div class="date-item" @click="onPick('day')">
          {{ currentDay }}日
          <van-icon name="arrow-down" />
        </div>
      </div>

      <!-- 底部弹出日期选择 -->
      <van-popup v-model:show="showDatePicker" round position="bottom" teleport="body">
        <van-date-picker
          v-model="pickerValues"
          :columns-type="pickerColumnsType"
          @confirm="onDateConfirm"
          @cancel="showDatePicker = false"
        />
      </van-popup>

      <!-- 顶部播放区域 -->
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
            <img :src="playImg" alt="" class="player-icon" />
          </div>
        </div>
      </div>

      <!-- 视频列表 -->
      <div class="list-title">
        视频列表
      </div>
      <div class="clip-list">
        <div
          v-for="clip in clips"
          :key="clip.id"
          class="clip-item"
          :class="{ active: activeClipId === clip.id }"
          @click="playClip(clip)"
        >
          <div class="thumb-wrap">
            <img :src="clip.thumb" class="thumb-img" alt="" />
            <div class="thumb-play">
              <img :src="playImg" alt="" class="thumb-play-icon" />
            </div>
            <div class="thumb-duration">
              {{ clip.duration }}
            </div>
          </div>

          <div class="clip-info">
            <div class="clip-time">
              {{ clip.start }} - {{ clip.end }}
            </div>
            <div class="clip-device-name">
              {{ clip.deviceName }}
            </div>
            <div
              v-if="activeClipId === clip.id"
              class="playing-tag"
            >
              <img src="@/assets/img/VLStreamCloud/bfz.png" alt="" class="playing-tag-icon" />
              <span>播放中</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useUserStore } from '@/store/modules/useraPaas'
import type { DatePickerColumnType } from 'vant'
import NavHeaderBanner from '@/components/navHeaderBanner/index.vue'
import playImg from '@/assets/img/VLStreamCloud/play.png'
import { getPlayback, getPlaybackStream } from '@/api/VLStreamCloud/videoPlayback'

const props = defineProps<{
  deviceId: number | string
}>()

const emit = defineEmits(['close'])
const store = useUserStore()
const onBack = () => {
  emit('close')
}

const today = new Date()
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth() + 1)
const currentDay = ref(today.getDate())

const activeClipId = ref<number | string>('')
const videoSrc = ref<string>('')
const videoPlayer = ref<HTMLVideoElement | null>(null)

// 底部日期选择器
const showDatePicker = ref(false)
const pickerValues = ref<string[]>([])

const pad2 = (value: number | string) => String(value).padStart(2, '0')

const pickerColumnsType = computed<DatePickerColumnType[]>(() => ['year', 'month', 'day'])

type ClipItem = {
  id: number | string
  thumb: string
  deviceName: string
  start: string
  end: string
  duration: string
  raw?: any
}

const clips = ref<ClipItem[]>([])

const extractTime = (dateTimeStr?: any) => {
  const s = String(dateTimeStr || '')
  const m = s.match(/(\d{2}:\d{2}:\d{2})/)
  return m?.[1] || '--'
}

const formatDuration = (seconds: any) => {
  const s = Number(seconds)
  if (!Number.isFinite(s) || s <= 0) return '--'
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = Math.floor(s % 60)
  if (h > 0) return `${pad2(h)}:${pad2(m)}:${pad2(sec)}`
  return `${pad2(m)}:${pad2(sec)}`
}

const buildRangeParams = () => {
  const dateStr = `${currentYear.value}-${pad2(currentMonth.value)}-${pad2(currentDay.value)}`
  return {
    startTime: `${dateStr} 00:00:00`,
    endTime: `${dateStr} 23:59:59`
  }
}

const fetchPlayback = async() => {
  const deviceIdNum = Number(props.deviceId)
  if (!deviceIdNum) {
    clips.value = []
    activeClipId.value = ''
    return
  }
  const { startTime, endTime } = buildRangeParams()
  const params = {
    accessToken: store.userInfo?.accessToken,
    deviceId: deviceIdNum,
    startTime,
    endTime
  }
  const res: any = await getPlayback(params)
  if (res.code !== 200 || !Array.isArray(res.data)) {
    clips.value = []
    activeClipId.value = ''
    return
  }
  clips.value = res.data.map((r: any, idx: number) => ({
    id: r.id ?? idx,
    thumb: r.thumbnailPath || '',
    deviceName: r.deviceName,
    start: extractTime(r.recordStartTime),
    end: extractTime(r.recordEndTime),
    duration: formatDuration(r.duration),
    raw: r
  }))
  activeClipId.value = clips.value.length ? clips.value[0].id : ''
  // 自动播放第一个视频
  if (clips.value.length > 0) {
    playClip(clips.value[0])
  }
}

const heroImg = computed(() => {
  const active = clips.value.find(c => c.id === activeClipId.value)
  return active?.thumb || ''
})

const playClip = async(clip: ClipItem) => {
  activeClipId.value = clip.id
  if (!clip.id) {
    videoSrc.value = ''
    return
  }

  try {
    // 调用接口获取视频流
    const params = { accessToken: store.userInfo?.accessToken }
    const res: any = await getPlaybackStream(clip.id, params)
    if (res && res instanceof Blob) {
      if (videoSrc.value && videoSrc.value.startsWith('blob:')) {
        URL.revokeObjectURL(videoSrc.value)
      }
      videoSrc.value = URL.createObjectURL(res)

      // 等待视频元素加载后自动播放
      if (videoPlayer.value) {
        videoPlayer.value.load()
      }
    } else {
      videoSrc.value = ''
    }
  } catch (error) {
    videoSrc.value = ''
  }
}

const onVideoLoaded = () => {
  if (videoPlayer.value) {
    videoPlayer.value.play().catch(() => {
      // 自动播放失败，用户需要手动点击播放
    })
  }
}

const onPick = (_type: 'year' | 'month' | 'day') => {
  pickerValues.value = [
    String(currentYear.value),
    pad2(currentMonth.value),
    pad2(currentDay.value)
  ]
  showDatePicker.value = true
}

const onDateConfirm = ({ selectedValues }: any) => {
  if (!Array.isArray(selectedValues) || selectedValues.length === 0) {
    showDatePicker.value = false
    return
  }
  currentYear.value = Number(selectedValues[0])
  currentMonth.value = Number(selectedValues[1])
  currentDay.value = Number(selectedValues[2])
  showDatePicker.value = false
  fetchPlayback()
}

onMounted(() => {
  fetchPlayback()
})

watch(
  () => props.deviceId,
  () => {
    fetchPlayback()
  }
)

onBeforeUnmount(() => {
  // 清理 Blob URL，避免内存泄漏
  if (videoSrc.value && videoSrc.value.startsWith('blob:')) {
    URL.revokeObjectURL(videoSrc.value)
  }
})
</script>

<style scoped lang="scss">
.playback-detail {
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

.date-row {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 10px 16px 8px;
  color:#333333;
  font-size:14px;
}

.date-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.player-wrap {
  position: relative;
  width: 100%;
  height: 210px;
  background: #000;
}

.player-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.player-placeholder {
  position: relative;
  width: 100%;
  height: 100%;
}

.player-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.player-overlay {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.player-icon {
  width: 38px;
  height: 38px;
  object-fit: contain;
}

.list-title {
  line-height: 24px;
  font-weight: 500;
  color: #969799;
  font-size: 12px;
  padding: 20px 16px 10px;
}

.clip-list {
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.clip-item {
  display: flex;
  gap: 12px;
}

.thumb-wrap {
  position: relative;
  width: 120px;
  height: 88px;
  border: 2px solid rgba(47, 105, 248, 0.2);
  border-radius: 8px;
  overflow: hidden;
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-play {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-play-icon {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.thumb-duration {
  position: absolute;
  right: 5px;
  bottom: 5px;
  padding: 1px 3px;
  border-radius:4px;
  font-size:8px;
  background:rgba(0, 0, 0, 0.3);
  color: #fff;
}

.clip-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 4px;
}

.clip-time {
  line-height: 18px;
  font-weight: 500;
  color: #333333;
  font-size: 14px;
}

.clip-device-name {
  line-height: 22px;
  color:#7a7c85;
  font-size:14px;
}
.playing-tag {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width:64px;
  height:22px;
  background: rgba(47, 105, 248, 0.12);
  border-radius: 4px;
  line-height: 16px;
  font-weight: 500;
  color: #2f69f8;
  font-size: 12px;
  padding: 5px 6px;
  box-sizing: border-box;
  .playing-tag-icon{
    width: 12px;
    height: 12px;
  }
}
</style>

