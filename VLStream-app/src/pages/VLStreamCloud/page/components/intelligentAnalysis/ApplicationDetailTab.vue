<template>
  <div class="application-detail-tab">
    <div class="detail-card">
      <div class="card-header">
        <div class="task-title">
          {{ task.title }}
        </div>
      </div>

      <img :src="statusImg" alt="" class="status-img" />

      <div class="info-row">
        <div class="label">
          <img :src="fxlx" alt="" class="info-icon" />
          <span>分析类型</span>
        </div>
        <div class="value">
          {{ task.type }}
        </div>
      </div>

      <div class="info-row">
        <div class="label">
          <img :src="fxql" alt="" class="info-icon" />
          <span>分析区域</span>
        </div>
        <div class="value">
          {{ task.area }}
        </div>
      </div>

      <div class="info-row">
        <div class="label">
          <img :src="fxtp" alt="" class="info-icon" />
          <span>分析图片</span>
        </div>
        <div class="thumb-grid">
          <img
            v-for="(img, idx) in analysisImages"
            :key="idx"
            :src="img"
            class="thumb-item"
            alt=""
          />
        </div>
      </div>

      <div class="info-row">
        <div class="label">
          <img :src="sxt" alt="" class="info-icon" />
          <span>摄像头</span>
        </div>
        <div class="value multi-line">
          {{ camerasText }}
        </div>
      </div>

      <div class="info-row">
        <div class="label">
          <img :src="sqsj" alt="" class="info-icon" />
          <span>分析时段</span>
        </div>
        <div class="value">
          {{ periodText }}
        </div>
      </div>

      <div class="info-row">
        <div class="label">
          <img :src="fxlx" alt="" class="info-icon" />
          <span>描述</span>
        </div>
        <div class="value">
          {{ descriptionText }}
        </div>
      </div>

      <div class="info-row">
        <div class="label">
          <img :src="sqsj" alt="" class="info-icon" />
          <span>申请时间</span>
        </div>
        <div class="value">
          {{ applyTimeText }}
        </div>
      </div>

      <div class="info-row">
        <div class="label">
          <img :src="sqsj" alt="" class="info-icon" />
          <span>完成时间</span>
        </div>
        <div class="value">
          {{ completeTimeText }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import fxlx from '@/assets/img/VLStreamCloud/znfx/fxlx.png'
import fxql from '@/assets/img/VLStreamCloud/znfx/fxql.png'
import fxtp from '@/assets/img/VLStreamCloud/znfx/fxtp.png'
import sqsj from '@/assets/img/VLStreamCloud/znfx/sqsj.png'
import sxt from '@/assets/img/VLStreamCloud/znfx/sxt.png'
import ywc from '@/assets/img/VLStreamCloud/ywc.png'
import ysp from '@/assets/img/VLStreamCloud/ysp.png'

interface TaskInfo {
  id: number | string
  title: string
  type: string
  area: string
  thumb: string
  cameras: string
  applyTime: string
  status: string
  period?: string
  description?: string
  completeTime?: string
  images?: string
  timeRange?: string
  startTime?: string
  requestStatus?: string
  deviceIds?: string
  cameraName?: string
  [key: string]: any
}

const props = defineProps<{
  task: TaskInfo
}>()

// 状态图标：已完成 / 已失败
const statusImg = computed(() => {
  const t: any = props.task || {}
  if (t.status === 'failed' || t.requestStatus === 'failed') {
    return ysp
  }
  return ywc
})

// 分析图片
const analysisImages = computed(() => {
  const t: any = props.task || {}
  const imageStr: string = t.images || t.thumb || ''
  if (!imageStr) return ''
  const arr = String(imageStr)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  return arr.length ? arr : ''
})

// 摄像头
const camerasText = computed(() => {
  const t: any = props.task || {}
  return t.cameraName || '--'
})

// 分析时段
const periodText = computed(() => {
  const t: any = props.task || {}
  if (t.timeRange) return t.timeRange
  const start = t.startTime || ''
  const end = t.completeTime || ''
  if (start && end) return `${start} ~ ${end}`
  return '--'
})

// 描述
const descriptionText = computed(() => {
  const t: any = props.task || {}
  return t.description || '智能分析描述'
})

// 申请时间
const applyTimeText = computed(() => {
  const t: any = props.task || {}
  return t.applyTime || t.startTime || '--'
})

// 完成时间
const completeTimeText = computed(() => {
  const t: any = props.task || {}
  return t.completeTime || t.applyTime || '--'
})
</script>

<style scoped lang="scss">
.application-detail-tab {
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 16px 0;
  background: #f9faff;
}

.detail-card {
  position: relative;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.task-title {
  line-height: 24px;
  font-weight: 700;
  color: #333333;
  font-size: 16px;
}

.status-img {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 38px;
  height: 38px;
  object-fit: contain;
}

.info-row {
  display: flex;
  align-items: flex-start;
  margin-top: 8px;
  font-size: 14px;
}

.label {
  min-width: 100px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  line-height: 22px;
  color: #7a7c85;
  font-size: 14px;
}

.info-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.value {
  flex: 1;
  line-height: 22px;
  color: #333333;
  font-size: 14px;
}

.value.multi-line {
  line-height: 1.5;
}

.thumb-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.thumb-item {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 8px;
  object-fit: cover;
}
</style>
