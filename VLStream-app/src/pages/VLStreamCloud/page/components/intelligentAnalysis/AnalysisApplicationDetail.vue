<template>
  <div class="analysis-application-detail">
    <nav-header-banner title="分析申请" :is-call-back="true" @call-back="onBack">
      <span class="header-cancel" @click="onCancel">取消申请</span>
    </nav-header-banner>

    <div class="detail-body">
      <div class="detail-card">
        <div class="card-header">
          <div class="task-title">
            {{ task.title }}
          </div>
        </div>

        <img
          v-if="task.status === 'processing'"
          :src="fxz"
          alt=""
          class="status-img"
        />

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
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { showConfirmDialog } from 'vant'
import NavHeaderBanner from '@/components/navHeaderBanner/index.vue'
import fxlx from '@/assets/img/VLStreamCloud/znfx/fxlx.png'
import fxql from '@/assets/img/VLStreamCloud/znfx/fxql.png'
import fxtp from '@/assets/img/VLStreamCloud/znfx/fxtp.png'
import sqsj from '@/assets/img/VLStreamCloud/znfx/sqsj.png'
import sxt from '@/assets/img/VLStreamCloud/znfx/sxt.png'
import fxz from '@/assets/img/VLStreamCloud/fxz.png'
import { cancelVlsAnalysisk } from '@/api/VLStreamCloud/intelligentAnalysis'

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
  // 透传后端字段，便于详情展示
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

const emit = defineEmits(['cancel', 'close'])

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

// 取消申请
const onCancel = async() => {
  try {
    await showConfirmDialog({
      title: '取消申请',
      message: '确认取消当前分析申请？'
    })
  } catch {
    return
  }

  const id = props.task?.id
  if (!id) {
    emit('cancel', props.task)
    return
  }

  await cancelVlsAnalysisk({ id })
  emit('cancel', props.task)
}

const onBack = () => {
  emit('close')
}
</script>

<style scoped lang="scss">
.analysis-application-detail {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f9faff;
}

.detail-body {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 12px 16px 20px;
}

.header-cancel {
  font-size: 16px;
  color: #fff;
  line-height: 1;
}

.detail-card {
  position: relative;
  background:#ffffff;
  border-radius:8px;
  padding: 12px;
}

.status-img {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 38px;
  height: 38px;
  object-fit: contain;
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

.status-stamp {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px dashed;
  font-size: 12px;
}

.status-stamp.processing {
  border-color: rgba(251, 146, 60, 0.6);
  color: #f97316;
  background: rgba(251, 146, 60, 0.1);
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
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
}

.thumb-item {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 8px;
  object-fit: cover;
}
</style>
