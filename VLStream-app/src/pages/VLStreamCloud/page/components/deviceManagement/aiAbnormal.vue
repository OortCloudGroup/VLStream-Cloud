<template>
  <div class="audio-detection-page">
    <nav-header-banner title="音频异常侦测" :is-call-back="true" @call-back="onBack">
      <span class="header-save" @click="onSave">保存</span>
    </nav-header-banner>

    <div class="audio-content">
      <!-- 顶部 tab -->
      <div class="tab-header">
        <div
          v-for="tab in tabs"
          :key="tab.value"
          class="tab-item"
          :class="{ active: activeTab === tab.value }"
          @click="activeTab = tab.value"
        >
          {{ tab.label }}
          <div v-if="activeTab === tab.value" class="tab-underline" />
        </div>
      </div>

      <!-- tab 内容 -->
      <div class="tab-body">
        <!-- 异常检测 -->
        <div v-if="activeTab === 'detect'">
          <AiAbnormalDetect ref="detectRef" :device-id="props.deviceId" />
        </div>

        <!-- 布防时间 -->
        <div v-else-if="activeTab === 'time'">
          <AiDefenseTime ref="timeRef" :device-id="props.deviceId" />
        </div>

        <!-- 联动方式 -->
        <div v-else>
          <AiLinkageMode ref="linkageRef" :device-id="props.deviceId" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import NavHeaderBanner from '@/components/navHeaderBanner/index.vue'
import AiAbnormalDetect from './AiAbnormalDetect.vue'
import AiDefenseTime from './AiDefenseTime.vue'
import AiLinkageMode from './AiLinkageMode.vue'

const emit = defineEmits(['close'])

const props = defineProps<{
  deviceId?: string | number
}>()

const tabs = [
  { label: '异常检测', value: 'detect' },
  { label: '布防时间', value: 'time' },
  { label: '联动方式', value: 'linkage' }
]

const activeTab = ref('detect')

const detectRef = ref<InstanceType<typeof AiAbnormalDetect> | null>(null)
const timeRef = ref<InstanceType<typeof AiDefenseTime> | null>(null)
const linkageRef = ref<InstanceType<typeof AiLinkageMode> | null>(null)

const onSave = async() => {
  if (activeTab.value === 'detect' && detectRef.value && detectRef.value.save) {
    await detectRef.value.save()
  }
  if (activeTab.value === 'time' && timeRef.value && timeRef.value.save) {
    await timeRef.value.save()
  }
  if (activeTab.value === 'linkage' && linkageRef.value && linkageRef.value.save) {
    await linkageRef.value.save()
  }
}

const onBack = () => {
  emit('close')
}
</script>

<style scoped lang="scss">
.audio-detection-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f7f8fa;
}

.audio-content {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 16px;
  background: #F9FAFF;
}

.tab-header {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 64px;
  margin-bottom: 16px;
  background: #ffffff;
}

.tab-item {
  position: relative;
  line-height: 18px;
  color:#aeaeba;
  font-size: 14px;
  padding-bottom: 6px;

  &.active {
    font-weight:500;
    color:#2f69f8;
  }
}

.tab-underline {
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  border-radius: 999px;
  background:#2f69f8;
}

.tab-body {
  background: #F9FAFF;
  padding: 0 12px 20px;
}

.header-save {
  font-size: 16px;
  color: #fff;
  line-height: 1;
}
</style>
