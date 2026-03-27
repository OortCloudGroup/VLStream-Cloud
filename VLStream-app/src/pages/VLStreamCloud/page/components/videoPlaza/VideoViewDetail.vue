<template>
  <div class="view-video">
    <nav-header-banner title="查看视频" :is-call-back="true" @call-back="onBack" />

    <div class="view-body">
      <!-- 顶部大图 -->
      <img class="hero-img" :src="heroImg" alt="" />

      <!-- 信息卡片 -->
      <div class="info-card">
        <div class="title-row">
          <span class="device-name">{{ device.deviceName || '--' }}</span>
          <span class="privacy-tag">
            {{ privacyText }}
          </span>
        </div>

        <div class="info-row">
          <div class="label">
            <img :src="sbId" alt="" class="info-icon" />
            <span>设备ID</span>
          </div>
          <div class="value">
            {{ device.deviceId }}
          </div>
        </div>

        <div class="info-row">
          <div class="label">
            <img :src="sblx" alt="" class="info-icon" />
            <span>设备类型</span>
          </div>
          <div class="value">
            {{ device.deviceType || '--' }}
          </div>
        </div>

        <div class="info-row">
          <div class="label">
            <img :src="yysf" alt="" class="info-icon" />
            <span>拥有算法</span>
          </div>
          <div class="value">
            {{ device.algorithmName || device.algorithmId || '--' }}
          </div>
        </div>

        <div class="info-row">
          <div class="label">
            <img :src="sbbq" alt="" class="info-icon" />
            <span>设备标签</span>
          </div>
          <div class="value">
            {{ tagsText }}
          </div>
        </div>

        <div class="info-row">
          <div class="label">
            <img :src="sbwz" alt="" class="info-icon" />
            <span>设备位置</span>
          </div>
          <div class="value">
            {{ device.address || '--' }}
          </div>
        </div>
      </div>
    </div>

    <!-- 底部按钮 -->
    <div class="footer-bar">
      <van-button type="primary" block round class="apply-btn" @click="onApply">
        申请使用
      </van-button>
    </div>

    <VideoApplyForm
      v-model:show="showApplyForm"
      :device-info-id="device.id"
      @submit="handleSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import NavHeaderBanner from '@/components/navHeaderBanner/index.vue'
import VideoApplyForm from './VideoApplyForm.vue'
import sbId from '@/assets/img/VLStreamCloud/spgc/sbId.png'
import sblx from '@/assets/img/VLStreamCloud/spgc/sblx.png'
import yysf from '@/assets/img/VLStreamCloud/spgc/yysf.png'
import sbbq from '@/assets/img/VLStreamCloud/spgc/sbbq.png'
import sbwz from '@/assets/img/VLStreamCloud/spgc/sbwz.png'

interface DeviceInfo {
  deviceName?: string
  deviceId: string
  deviceType?: string
  algorithmId?: string
  algorithmName?: string
  tag?: string
  selectedTags?: string[]
  isPublic?: number
  heightPosition?: string
  address?: string
  poster?: string
  imagePath?: string
  [key: string]: any
}

const props = defineProps<{
  device: DeviceInfo
}>()

const emit = defineEmits(['apply', 'close'])

const heroImg = computed(() => props.device.imagePath || '')

const privacyText = computed(() => {
  const d = props.device || {}
  return d.isPublic === 1 ? '公开' : '私有'
})

const tagsText = computed(() => {
  const d = props.device || {}
  if (Array.isArray(d.selectedTags) && d.selectedTags.length) {
    return d.selectedTags.join('、')
  }
  if (d.tag) {
    const arr = String(d.tag)
      .split(',')
      .map((s: string) => s.trim())
      .filter((s: string) => !!s)
    return arr.length ? arr.join('、') : '--'
  }
  return '--'
})

const showApplyForm = ref(false)

const onApply = () => {
  showApplyForm.value = true
}

const handleSubmit = (payload: { applicant: string; tenant: string; desc: string }) => {
  emit('apply', {
    device: props.device,
    ...payload
  })
}

const onBack = () => {
  emit('close')
}
</script>

<style scoped lang="scss">
.view-video {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f9faff;
}

.view-body {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.hero-img {
  width: 100%;
  height: 210px;
  object-fit: cover;
}

.info-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 12px;
  margin: 16px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 12px;
}

.device-name {
  line-height: 25px;
  font-weight: 700;
  color: #333333;
  font-size: 16px;
}

.privacy-tag {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  box-sizing: border-box;
  background: rgba(255, 141, 34, 0.12);
  border-radius: 4px;
  line-height: 18px;
  color: #ff8d22;
  font-size: 12px;
  padding: 0 6px;
}

.info-row {
  display: flex;
  margin-top: 4px;
  font-size: 14px;
  line-height: 20px;
  img {
    width: 20px;
    height: 20px;
    margin-right: 4px;
  }
}

.label {
  display: inline-flex;
  min-width: 110px;
  color: #7a7c85;
  gap: 4px;
}

.info-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.value {
  flex: 1;
  color: #333333;
}

.footer-bar {
  padding: 12px 16px 22px;
  background: #f9faff;
}

.apply-btn {
  font-size: 16px;
}
</style>
