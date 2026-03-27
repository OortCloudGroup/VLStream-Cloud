<template>
  <div class="audio-body">
    <!-- 图像调节模块 -->
    <div class="section-title">
      图像调节
    </div>
    <div class="card-container">
      <!-- 音频输入异常 -->
      <van-checkbox-group v-model="audioCheckList" shape="square" class="checkbox-group">
        <van-checkbox name="audioInput">
          音频输入异常
        </van-checkbox>
      </van-checkbox-group>

      <!-- 声强陡升 -->
      <van-checkbox-group v-model="audioCheckList" shape="square" class="checkbox-group">
        <van-checkbox name="audioRise">
          声强陡升
        </van-checkbox>
      </van-checkbox-group>
      <div class="slider-item">
        <span class="slider-label">灵敏度</span>
        <van-slider v-model="riseSensitivity" :max="100" />
        <span class="slider-value">{{ riseSensitivity }}</span>
      </div>
      <div class="slider-item">
        <span class="slider-label">声音强度阈值</span>
        <van-slider v-model="riseThreshold" :max="100" />
        <span class="slider-value">{{ riseThreshold }}</span>
      </div>

      <!-- 声强陡降 -->
      <van-checkbox-group v-model="audioCheckList" shape="square" class="checkbox-group">
        <van-checkbox name="audioDrop">
          声强陡降
        </van-checkbox>
      </van-checkbox-group>
      <div class="slider-item">
        <span class="slider-label">灵敏度</span>
        <van-slider v-model="dropSensitivity" :max="100" />
        <span class="slider-value">{{ dropSensitivity }}</span>
      </div>
    </div>

    <!-- 异常检测波形图模块 -->
    <div class="section-title">
      异常检测
    </div>
    <div class="card-container wave-container">
      <svg viewBox="0 0 800 150" class="wave-svg">
        <path
          d="M0,75 C100,20 150,130 200,75 S300,130 350,75 S400,130 450,75 C500,20 550,75 600,75 L800,75"
          fill="none"
          stroke="#1989fa"
          stroke-width="2"
        />
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getVlsAudioAnomalyDetectionSetting, saveVlsAudioAnomalyDetectionSetting } from '@/api/VLStreamCloud/device'
import { useUserStore } from '@/store/modules/useraPaas'

// props：设备ID
const props = defineProps<{
  deviceId?: string | number
}>()

const store = useUserStore()

// 原始数据（用于保存时带上 id、tenantId 等）
const originData = ref<any | null>(null)

// 音频检测复选框列表（默认全选）
const audioCheckList = ref<string[]>(['audioInput', 'audioRise', 'audioDrop'])

// 声强陡升滑块值
const riseSensitivity = ref<number>(50)
const riseThreshold = ref<number>(50)

// 声强陡降滑块值
const dropSensitivity = ref<number>(50)

// 拉取音频异常侦测设置
const fetchSetting = async() => {
  if (!props.deviceId) return
  const params: any = {
    accessToken: store.userInfo?.accessToken,
    deviceId: props.deviceId
  }
  const res: any = await getVlsAudioAnomalyDetectionSetting(params)
  if (res && res.code === 200) {
    const data = res.data
    originData.value = data

    const checks: string[] = []
    if (data.audioInputAnomalyEnabled === 1) checks.push('audioInput')
    if (data.soundRiseEnabled === 1) checks.push('audioRise')
    if (data.soundDropEnabled === 1) checks.push('audioDrop')
    audioCheckList.value = checks.length ? checks : []

    riseSensitivity.value = Number(data.soundRiseSensitivity ?? riseSensitivity.value)
    riseThreshold.value = Number(data.soundIntensityThreshold ?? riseThreshold.value)
    dropSensitivity.value = Number(data.soundDropSensitivity ?? dropSensitivity.value)
  }
}

// 保存音频异常侦测设置
const save = async() => {
  if (!props.deviceId) return
  const base = originData.value || {}
  const payload: any = {
    id: base.id,
    tenantId: base.tenantId || '',
    deviceId: base.deviceId || props.deviceId,
    audioInputAnomalyEnabled: audioCheckList.value.includes('audioInput') ? 1 : 0,
    soundRiseEnabled: audioCheckList.value.includes('audioRise') ? 1 : 0,
    soundRiseSensitivity: Number(riseSensitivity.value) || 0,
    soundIntensityThreshold: Number(riseThreshold.value) || 0,
    soundDropEnabled: audioCheckList.value.includes('audioDrop') ? 1 : 0,
    soundDropSensitivity: Number(dropSensitivity.value) || 0,
    remark: base.remark || ''
  }
  const params = {
    ...payload,
    accessToken: store.userInfo?.accessToken
  }
  const res = await saveVlsAudioAnomalyDetectionSetting(params)
  if (res && res.code === 200) {
    await fetchSetting()
  }
}

onMounted(() => {
  fetchSetting()
})

defineExpose({
  save
})
</script>

<style scoped lang="scss">
.audio-body {
  padding: 0 16px 16px 16px;
}

// 模块标题样式
.section-title {
  color:#969799;
  font-size:12px;
  margin: 16px 0 6px 0;
}

// 白色卡片容器
.card-container {
  background-color: #fff;
  border-radius: 12px;
  padding: 16px 16px 0 16px;
  margin-bottom: 20px;
}

// 复选框组样式
.checkbox-group {
  --van-checkbox-color: #1989fa;
  --van-checkbox-icon-size: 18px;
  --van-checkbox-label-font-size: 16px;
  margin-bottom: 25px;
}

// 滑块项样式
.slider-item {
  display: flex;
  align-items: center;
  padding-bottom: 25px;

  &:last-child {
    border-bottom: none;
  }

  .slider-label {
    width: 100px;
    font-size: 14px;
    color: #666666;
  }

  .van-slider {
    flex: 1;
  }

  .slider-value {
    width: 40px;
    text-align: right;
    font-size: 16px;
    color: #646566;
  }

  :deep(.van-slider__button){
    width: 14px;
    height: 14px;
  }
}

// 波形图容器
.wave-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
}

.wave-svg {
  width: 100%;
  height: auto;
}
</style>

