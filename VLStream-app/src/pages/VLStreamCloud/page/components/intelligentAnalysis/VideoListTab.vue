<template>
  <div class="video-list-tab">
    <div class="clip-list">
      <div
        v-for="clip in clips"
        :key="clip.id"
        class="clip-item"
        :class="{ active: activeClipId === clip.id }"
        @click="playClip(clip)"
      >
        <div class="thumb-wrap">
          <img v-if="clip.thumb" :src="clip.thumb" class="thumb-img" alt="" />
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
          <div v-if="clip.camera" class="clip-camera">
            {{ clip.camera }}
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
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useUserStore } from '@/store/modules/useraPaas'
import playImg from '@/assets/img/VLStreamCloud/play.png'
import { getPlayback, getPlaybackStream } from '@/api/VLStreamCloud/intelligentAnalysis'

interface TaskInfo {
  id: number | string
  title: string
  type: string
  area: string
  thumb: string
  cameras: string
  applyTime: string
  status: string
  // 透传后端字段，便于取设备 ID
  deviceId?: number | string
  deviceIds?: string
  cameraName?: string
  [key: string]: any
}

const props = defineProps<{
  task: TaskInfo
}>()

const emit = defineEmits(['play'])

const store = useUserStore()

const activeClipId = ref<number | string>('')

type ClipItem = {
  id: number | string
  thumb: string
  camera: string
  start: string
  end: string
  duration: string
  raw?: any
}

const clips = ref<ClipItem[]>([])

const pad2 = (value: number | string) => String(value).padStart(2, '0')

// 从完整时间字符串中提取 HH:mm:ss
const extractTime = (dateTimeStr?: any) => {
  const s = String(dateTimeStr || '')
  const m = s.match(/(\d{2}:\d{2}:\d{2})/)
  return m?.[1] || '--'
}

// 将秒数格式化为 mm:ss 或 HH:mm:ss
const formatDuration = (seconds: any) => {
  const s = Number(seconds)
  if (!Number.isFinite(s) || s <= 0) return '--'
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = Math.floor(s % 60)
  if (h > 0) return `${pad2(h)}:${pad2(m)}:${pad2(sec)}`
  return `${pad2(m)}:${pad2(sec)}`
}

// 构造当天 00:00:00 ~ 23:59:59 的时间范围
const buildRangeParams = () => {
  const today = new Date()
  const year = today.getFullYear()
  const month = pad2(today.getMonth() + 1)
  const day = pad2(today.getDate())
  const dateStr = `${year}-${month}-${day}`
  return {
    startTime: `${dateStr} 00:00:00`,
    endTime: `${dateStr} 23:59:59`
  }
}

// 获取当前任务设备的回放片段列表
const fetchPlayback = async() => {
  const rawDeviceIds = String((props.task as any).deviceIds || '')
  const idList = rawDeviceIds
    .split(/[，,]/)
    .map((s: string) => Number(s.trim()))
    .filter((n: number) => Number.isFinite(n) && n > 0)

  if (!idList.length) {
    clips.value = []
    activeClipId.value = ''
    return
  }

  const { startTime, endTime } = buildRangeParams()
  const allRecords: any[] = []

  for (const deviceIdNum of idList) {
    const params = {
      accessToken: store.userInfo?.accessToken,
      deviceId: deviceIdNum,
      startTime,
      endTime
    }

    const res: any = await getPlayback(params)
    if (res.code === 200 && Array.isArray(res.data)) {
      allRecords.push(...res.data)
    }
  }

  if (!allRecords.length) {
    clips.value = []
    activeClipId.value = ''
    return
  }

  clips.value = allRecords.map((r: any, idx: number) => ({
    id: r.id ?? idx,
    thumb: r.thumbnailPath || '',
    camera: r.deviceName || (props.task.cameraName as string) || '',
    start: extractTime(r.recordStartTime),
    end: extractTime(r.recordEndTime),
    duration: formatDuration(r.duration),
    raw: r
  }))

  activeClipId.value = clips.value.length ? clips.value[0].id : ''

  // 默认自动播放第一个片段
  if (clips.value.length) {
    playClip(clips.value[0])
  }
}

// 点击片段时把地址抛给父组件
const playClip = async(clip: ClipItem) => {
  activeClipId.value = clip.id
  if (!clip.id) return

  try {
    const params = { accessToken: store.userInfo?.accessToken }
    const res: any = await getPlaybackStream(clip.id, params)
    if (res && res instanceof Blob) {
      const src = URL.createObjectURL(res)
      emit('play', { src, thumb: clip.thumb })
    }
  } catch (e) {
    // 获取失败则不播放
  }
}

onMounted(() => {
  fetchPlayback()
})

watch(
  () => props.task && (props.task as any).deviceId,
  () => {
    fetchPlayback()
  }
)
</script>

<style scoped lang="scss">
.video-list-tab {
  background: #f9faff;
  margin-top: 16px;
}

.clip-list {
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
  flex-shrink: 0;
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
  border-radius: 4px;
  font-size: 8px;
  background: rgba(0, 0, 0, 0.3);
  color: #fff;
}

.clip-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-top: 6px;
  gap: 4px;
}

.clip-time {
  line-height: 18px;
  font-weight: 500;
  color: #333333;
  font-size: 14px;
}

.clip-camera {
  font-size: 13px;
  color: #6b7280;
}

.playing-tag {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 64px;
  height: 22px;
  background: rgba(47, 105, 248, 0.12);
  border-radius: 4px;
  line-height: 16px;
  font-weight: 500;
  color: #2f69f8;
  font-size: 12px;
  padding: 5px 6px;
  box-sizing: border-box;

  .playing-tag-icon {
    width: 12px;
    height: 12px;
  }
}
</style>
