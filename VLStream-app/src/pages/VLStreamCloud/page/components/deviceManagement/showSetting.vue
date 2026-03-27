<template>
  <div class="camera-settings">
    <!-- 场景 -->
    <van-field
      v-model="sceneValue"
      is-link
      readonly
      label="场景"
      class="settings-field"
      @click="showScenePicker = true"
    />
    <oort-popup v-model="showScenePicker" round position="bottom">
      <van-picker
        :columns="sceneColumns"
        @cancel="showScenePicker = false"
        @confirm="onSceneConfirm"
      />
    </oort-popup>

    <!-- 图像调节 -->
    <div class="section-title">
      图像调节
    </div>
    <div class="slider-group">
      <div class="slider-item">
        <span class="slider-label">亮度</span>
        <van-slider v-model="brightness" :max="100" />
        <span class="slider-value">{{ brightness }}</span>
      </div>
      <div class="slider-item">
        <span class="slider-label">对比度</span>
        <van-slider v-model="contrast" :max="100" />
        <span class="slider-value">{{ contrast }}</span>
      </div>
      <div class="slider-item">
        <span class="slider-label">饱和度</span>
        <van-slider v-model="saturation" :max="100" />
        <span class="slider-value">{{ saturation }}</span>
      </div>
      <div class="slider-item">
        <span class="slider-label">锐度</span>
        <van-slider v-model="sharpness" :max="100" />
        <span class="slider-value">{{ sharpness }}</span>
      </div>
    </div>

    <!-- 曝光 -->
    <div class="section-title">
      曝光
    </div>
    <div class="field-group">
      <!-- 曝光模式 -->
      <van-field
        v-model="exposureModeValue"
        is-link
        readonly
        label="曝光模式"
        class="settings-field"
        @click="showExposureModePicker = true"
      />
      <oort-popup v-model="showExposureModePicker" round position="bottom">
        <van-picker
          :columns="exposureModeColumns"
          @cancel="showExposureModePicker = false"
          @confirm="onExposureModeConfirm"
        />
      </oort-popup>

      <!-- 最大快门限制 -->
      <van-field
        v-model="maxShutterValue"
        is-link
        readonly
        label="最大快门限制"
        class="settings-field"
        @click="showMaxShutterPicker = true"
      />
      <oort-popup v-model="showMaxShutterPicker" round position="bottom">
        <van-picker
          :columns="maxShutterColumns"
          @cancel="showMaxShutterPicker = false"
          @confirm="onMaxShutterConfirm"
        />
      </oort-popup>

      <!-- 最小快门限制 -->
      <van-field
        v-model="minShutterValue"
        is-link
        readonly
        label="最小快门限制"
        class="settings-field"
        @click="showMinShutterPicker = true"
      />
      <oort-popup v-model="showMinShutterPicker" round position="bottom">
        <van-picker
          :columns="minShutterColumns"
          @cancel="showMinShutterPicker = false"
          @confirm="onMinShutterConfirm"
        />
      </oort-popup>

      <!-- 增益限制 -->
      <div class="slider-item">
        <span class="slider-label">增益限制</span>
        <van-slider v-model="gainLimit" :max="100" />
        <span class="slider-value">{{ gainLimit }}</span>
      </div>

      <!-- 低照度电子快门 -->
      <van-field
        v-model="lowLightShutterValue"
        is-link
        readonly
        label="低照度电子快门"
        class="settings-field"
        @click="showLowLightShutterPicker = true"
      />
      <oort-popup v-model="showLowLightShutterPicker" round position="bottom">
        <van-picker
          :columns="lowLightShutterColumns"
          @cancel="showLowLightShutterPicker = false"
          @confirm="onLowLightShutterConfirm"
        />
      </oort-popup>
    </div>

    <!-- 聚焦 -->
    <div class="section-title">
      聚焦
    </div>
    <div class="field-group">
      <!-- 聚焦模式 -->
      <van-field
        v-model="focusModeValue"
        is-link
        readonly
        label="聚焦模式"
        class="settings-field"
        @click="showFocusModePicker = true"
      />
      <oort-popup v-model="showFocusModePicker" round position="bottom">
        <van-picker
          :columns="focusModeColumns"
          @cancel="showFocusModePicker = false"
          @confirm="onFocusModeConfirm"
        />
      </oort-popup>

      <!-- 最小聚焦距离 -->
      <van-field
        v-model="minFocusValue"
        is-link
        readonly
        label="最小聚焦距离"
        class="settings-field"
        @click="showMinFocusPicker = true"
      />
      <oort-popup v-model="showMinFocusPicker" round position="bottom">
        <van-picker
          :columns="minFocusColumns"
          @cancel="showMinFocusPicker = false"
          @confirm="onMinFocusConfirm"
        />
      </oort-popup>
    </div>

    <!-- 日夜转换 -->
    <div class="section-title">
      日夜转换
    </div>
    <div class="field-group">
      <!-- 日夜转换 -->
      <van-field
        v-model="dayNightValue"
        is-link
        readonly
        label="日夜转换"
        class="settings-field"
        @click="showDayNightPicker = true"
      />
      <oort-popup v-model="showDayNightPicker" round position="bottom">
        <van-picker
          :columns="dayNightColumns"
          @cancel="showDayNightPicker = false"
          @confirm="onDayNightConfirm"
        />
      </oort-popup>

      <!-- 灵敏度 -->
      <van-field
        v-model="sensitivityValue"
        is-link
        readonly
        label="灵敏度"
        class="settings-field"
        @click="showSensitivityPicker = true"
      />
      <oort-popup v-model="showSensitivityPicker" round position="bottom">
        <van-picker
          :columns="sensitivityColumns"
          @cancel="showSensitivityPicker = false"
          @confirm="onSensitivityConfirm"
        />
      </oort-popup>

      <!-- 防补光过曝 -->
      <van-field
        v-model="antiOverexposeValue"
        is-link
        readonly
        label="防补光过曝"
        class="settings-field"
        @click="showAntiOverexposePicker = true"
      />
      <oort-popup v-model="showAntiOverexposePicker" round position="bottom">
        <van-picker
          :columns="antiOverexposeColumns"
          @cancel="showAntiOverexposePicker = false"
          @confirm="onAntiOverexposeConfirm"
        />
      </oort-popup>

      <!-- 红外灯模式 -->
      <van-field
        v-model="irModeValue"
        is-link
        readonly
        label="红外灯模式"
        class="settings-field"
        @click="showIrModePicker = true"
      />
      <oort-popup v-model="showIrModePicker" round position="bottom">
        <van-picker
          :columns="irModeColumns"
          @cancel="showIrModePicker = false"
          @confirm="onIrModeConfirm"
        />
      </oort-popup>

      <!-- 亮度限制 -->
      <div class="slider-item">
        <span class="slider-label">亮度限制</span>
        <van-slider v-model="brightnessLimit" :max="100" />
        <span class="slider-value">{{ brightnessLimit }}</span>
      </div>
    </div>

    <!-- 背光 -->
    <div class="section-title">
      背光
    </div>
    <div class="field-group">
      <!-- 背光补偿 -->
      <van-field
        v-model="backlightCompValue"
        is-link
        readonly
        label="背光补偿"
        class="settings-field"
        @click="showBacklightCompPicker = true"
      />
      <oort-popup v-model="showBacklightCompPicker" round position="bottom">
        <van-picker
          :columns="backlightCompColumns"
          @cancel="showBacklightCompPicker = false"
          @confirm="onBacklightCompConfirm"
        />
      </oort-popup>

      <!-- 宽动态 -->
      <van-field
        v-model="wideDynamicValue"
        is-link
        readonly
        label="宽动态"
        class="settings-field"
        @click="showWideDynamicPicker = true"
      />
      <oort-popup v-model="showWideDynamicPicker" round position="bottom">
        <van-picker
          :columns="wideDynamicColumns"
          @cancel="showWideDynamicPicker = false"
          @confirm="onWideDynamicConfirm"
        />
      </oort-popup>

      <!-- 强光抑制 -->
      <van-field
        v-model="strongLightValue"
        is-link
        readonly
        label="强光抑制"
        class="settings-field"
        @click="showStrongLightPicker = true"
      />
      <oort-popup v-model="showStrongLightPicker" round position="bottom">
        <van-picker
          :columns="strongLightColumns"
          @cancel="showStrongLightPicker = false"
          @confirm="onStrongLightConfirm"
        />
      </oort-popup>
    </div>

    <!-- 白平衡 -->
    <div class="section-title">
      白平衡
    </div>
    <div class="field-group">
      <!-- 白平衡 -->
      <van-field
        v-model="whiteBalanceValue"
        is-link
        readonly
        label="白平衡"
        class="settings-field"
        @click="showWhiteBalancePicker = true"
      />
      <oort-popup v-model="showWhiteBalancePicker" round position="bottom">
        <van-picker
          :columns="whiteBalanceColumns"
          @cancel="showWhiteBalancePicker = false"
          @confirm="onWhiteBalanceConfirm"
        />
      </oort-popup>
    </div>

    <!-- 图像增强 -->
    <div class="section-title">
      图像增强
    </div>
    <div class="field-group">
      <!-- 数字降噪 -->
      <van-field
        v-model="digitalNoiseReductionValue"
        is-link
        readonly
        label="数字降噪"
        class="settings-field"
        @click="showDigitalNoiseReductionPicker = true"
      />
      <oort-popup v-model="showDigitalNoiseReductionPicker" round position="bottom">
        <van-picker
          :columns="digitalNoiseReductionColumns"
          @cancel="showDigitalNoiseReductionPicker = false"
          @confirm="onDigitalNoiseReductionConfirm"
        />
      </oort-popup>

      <!-- 降噪等级 -->
      <div class="slider-item">
        <span class="slider-label">降噪等级</span>
        <van-slider v-model="noiseReductionLevel" :max="100" />
        <span class="slider-value">{{ noiseReductionLevel }}</span>
      </div>

      <!-- 透雾模式 -->
      <van-field
        v-model="dehazeModeValue"
        is-link
        readonly
        label="透雾模式"
        class="settings-field"
        @click="showDehazeModePicker = true"
      />
      <oort-popup v-model="showDehazeModePicker" round position="bottom">
        <van-picker
          :columns="dehazeModeColumns"
          @cancel="showDehazeModePicker = false"
          @confirm="onDehazeModeConfirm"
        />
      </oort-popup>

      <!-- 电子防抖 -->
      <van-field
        v-model="electronicStabilizationValue"
        is-link
        readonly
        label="电子防抖"
        class="settings-field"
        @click="showElectronicStabilizationPicker = true"
      />
      <oort-popup v-model="showElectronicStabilizationPicker" round position="bottom">
        <van-picker
          :columns="electronicStabilizationColumns"
          @cancel="showElectronicStabilizationPicker = false"
          @confirm="onElectronicStabilizationConfirm"
        />
      </oort-popup>
    </div>

    <!-- 视频调整 -->
    <div class="section-title">
      视频调整
    </div>
    <div class="field-group">
      <!-- 镜像 -->
      <van-field
        v-model="mirrorValue"
        is-link
        readonly
        label="镜像"
        class="settings-field"
        @click="showMirrorPicker = true"
      />
      <oort-popup v-model="showMirrorPicker" round position="bottom">
        <van-picker
          :columns="mirrorColumns"
          @cancel="showMirrorPicker = false"
          @confirm="onMirrorConfirm"
        />
      </oort-popup>

      <!-- PAL(50HZ) -->
      <van-field
        v-model="pal50hzValue"
        is-link
        readonly
        label="PAL（50HZ）"
        class="settings-field"
        @click="showPal50hzPicker = true"
      />
      <oort-popup v-model="showPal50hzPicker" round position="bottom">
        <van-picker
          :columns="pal50hzColumns"
          @cancel="showPal50hzPicker = false"
          @confirm="onPal50hzConfirm"
        />
      </oort-popup>
    </div>

    <!-- 其他 -->
    <div class="section-title">
      其他
    </div>
    <div class="field-group">
      <!-- 镜头初始化 -->
      <van-field
        v-model="lensInitializationValue"
        is-link
        readonly
        label="镜头初始化"
        class="settings-field"
        @click="showLensInitializationPicker = true"
      />
      <oort-popup v-model="showLensInitializationPicker" round position="bottom">
        <van-picker
          :columns="lensInitializationColumns"
          @cancel="showLensInitializationPicker = false"
          @confirm="onLensInitializationConfirm"
        />
      </oort-popup>

      <!-- 变倍限制 -->
      <van-field
        v-model="zoomLimitValue"
        is-link
        readonly
        label="变倍限制"
        class="settings-field"
        @click="showZoomLimitPicker = true"
      />
      <oort-popup v-model="showZoomLimitPicker" round position="bottom">
        <van-picker
          :columns="zoomLimitColumns"
          @cancel="showZoomLimitPicker = false"
          @confirm="onZoomLimitConfirm"
        />
      </oort-popup>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { getVlsCameraDisplaySetting, updateVlsCameraDisplaySetting, restoreDefaultVlsCameraDisplaySetting } from '@/api/VLStreamCloud/device'
import OortPopup from '@/components/popup/oort_popup.vue'
import { useUserStore } from '@/store/modules/useraPaas'

const props = defineProps<{
  deviceId?: string | number
}>()

const store = useUserStore()

const displayData = ref<any>(null)
const isInitializing = ref(false)
const hasLoaded = ref(false)

const safeNum = (v: any, fallback = 0) => {
  const n = Number(v)
  return Number.isFinite(n) ? n : fallback
}

// 场景
const showScenePicker = ref(false)
const sceneValue = ref('室内')
const sceneColumns = ref([
  { text: '室内', value: '室内' },
  { text: '室外', value: '室外' }
])
const onSceneConfirm = ({ selectedOptions }: any) => {
  sceneValue.value = selectedOptions[0].text
  showScenePicker.value = false
  updateDisplaySetting()
}

// 图像调节滑块
const brightness = ref(50)
const contrast = ref(50)
const saturation = ref(50)
const sharpness = ref(50)

// 曝光模式
const showExposureModePicker = ref(false)
const exposureModeValue = ref('自动')
const exposureModeColumns = ref([
  { text: '自动', value: '自动' },
  { text: '手动', value: '手动' }
])
const onExposureModeConfirm = ({ selectedOptions }: any) => {
  exposureModeValue.value = selectedOptions[0].text
  showExposureModePicker.value = false
  updateDisplaySetting()
}

// 最大快门限制
const showMaxShutterPicker = ref(false)
const maxShutterValue = ref('1/25')
const maxShutterColumns = ref([
  { text: '1/15', value: '1/15' },
  { text: '1/25', value: '1/25' },
  { text: '1/30', value: '1/30' },
  { text: '1/50', value: '1/50' }
])
const onMaxShutterConfirm = ({ selectedOptions }: any) => {
  maxShutterValue.value = selectedOptions[0].text
  showMaxShutterPicker.value = false
  updateDisplaySetting()
}

// 最小快门限制
const showMinShutterPicker = ref(false)
const minShutterValue = ref('1/3000')
const minShutterColumns = ref([
  { text: '1/1000', value: '1/1000' },
  { text: '1/2000', value: '1/2000' },
  { text: '1/3000', value: '1/3000' },
  { text: '1/4000', value: '1/4000' }
])
const onMinShutterConfirm = ({ selectedOptions }: any) => {
  minShutterValue.value = selectedOptions[0].text
  showMinShutterPicker.value = false
  updateDisplaySetting()
}

// 增益限制
const gainLimit = ref(50)

// 低照度电子快门
const showLowLightShutterPicker = ref(false)
const lowLightShutterValue = ref('关闭')
const lowLightShutterColumns = ref([
  { text: '关闭', value: '关闭' },
  { text: '开启', value: '开启' },
  { text: '自动', value: '自动' }
])
const onLowLightShutterConfirm = ({ selectedOptions }: any) => {
  lowLightShutterValue.value = selectedOptions[0].text
  showLowLightShutterPicker.value = false
  updateDisplaySetting()
}

// 聚焦模式
const showFocusModePicker = ref(false)
const focusModeValue = ref('半自动')
const focusModeColumns = ref([
  { text: '半自动', value: '半自动' },
  { text: '自动', value: '自动' },
  { text: '手动', value: '手动' },
  { text: '连续自动', value: '连续自动' }
])
const onFocusModeConfirm = ({ selectedOptions }: any) => {
  focusModeValue.value = selectedOptions[0].text
  showFocusModePicker.value = false
  updateDisplaySetting()
}

// 最小聚焦距离
const showMinFocusPicker = ref(false)
const minFocusValue = ref('1.5m')
const minFocusColumns = ref([
  { text: '0.5m', value: '0.5m' },
  { text: '1.0m', value: '1.0m' },
  { text: '1.5m', value: '1.5m' },
  { text: '2.0m', value: '2.0m' }
])
const onMinFocusConfirm = ({ selectedOptions }: any) => {
  minFocusValue.value = selectedOptions[0].text
  showMinFocusPicker.value = false
  updateDisplaySetting()
}

// 日夜转换
const showDayNightPicker = ref(false)
const dayNightValue = ref('自动')
const dayNightColumns = ref([
  { text: '自动', value: '自动' },
  { text: '白天', value: '白天' },
  { text: '夜晚', value: '夜晚' },
  { text: '定时', value: '定时' }
])
const onDayNightConfirm = ({ selectedOptions }: any) => {
  dayNightValue.value = selectedOptions[0].text
  showDayNightPicker.value = false
  updateDisplaySetting()
}

// 灵敏度
const showSensitivityPicker = ref(false)
const sensitivityValue = ref('2')
const sensitivityColumns = ref([
  { text: '1', value: '1' },
  { text: '2', value: '2' },
  { text: '3', value: '3' },
  { text: '4', value: '4' }
])
const onSensitivityConfirm = ({ selectedOptions }: any) => {
  sensitivityValue.value = selectedOptions[0].text
  showSensitivityPicker.value = false
  updateDisplaySetting()
}

// 防补光过曝
const showAntiOverexposePicker = ref(false)
const antiOverexposeValue = ref('关闭')
const antiOverexposeColumns = ref([
  { text: '关闭', value: '关闭' },
  { text: '开启', value: '开启' }
])
const onAntiOverexposeConfirm = ({ selectedOptions }: any) => {
  antiOverexposeValue.value = selectedOptions[0].text
  showAntiOverexposePicker.value = false
  updateDisplaySetting()
}

// 红外灯模式
const showIrModePicker = ref(false)
const irModeValue = ref('自动')
const irModeColumns = ref([
  { text: '自动', value: '自动' },
  { text: '开启', value: '开启' },
  { text: '关闭', value: '关闭' }
])
const onIrModeConfirm = ({ selectedOptions }: any) => {
  irModeValue.value = selectedOptions[0].text
  showIrModePicker.value = false
  updateDisplaySetting()
}

// 亮度限制
const brightnessLimit = ref(50)

// 背光补偿
const showBacklightCompPicker = ref(false)
const backlightCompValue = ref('关闭')
const backlightCompColumns = ref([
  { text: '关闭', value: '关闭' },
  { text: '开启', value: '开启' },
  { text: '自动', value: '自动' }
])
const onBacklightCompConfirm = ({ selectedOptions }: any) => {
  backlightCompValue.value = selectedOptions[0].text
  showBacklightCompPicker.value = false
  updateDisplaySetting()
}

// 宽动态
const showWideDynamicPicker = ref(false)
const wideDynamicValue = ref('关闭')
const wideDynamicColumns = ref([
  { text: '关闭', value: '关闭' },
  { text: '开启', value: '开启' },
  { text: '自动', value: '自动' }
])
const onWideDynamicConfirm = ({ selectedOptions }: any) => {
  wideDynamicValue.value = selectedOptions[0].text
  showWideDynamicPicker.value = false
  updateDisplaySetting()
}

// 强光抑制
const showStrongLightPicker = ref(false)
const strongLightValue = ref('关闭')
const strongLightColumns = ref([
  { text: '关闭', value: '关闭' },
  { text: '开启', value: '开启' },
  { text: '自动', value: '自动' }
])
const onStrongLightConfirm = ({ selectedOptions }: any) => {
  strongLightValue.value = selectedOptions[0].text
  showStrongLightPicker.value = false
  updateDisplaySetting()
}

// 白平衡
const showWhiteBalancePicker = ref(false)
const whiteBalanceValue = ref('自动白平衡')
const whiteBalanceColumns = ref([
  { text: '自动白平衡', value: '自动白平衡' },
  { text: '晴天', value: '晴天' },
  { text: '阴天', value: '阴天' },
  { text: '白炽灯', value: '白炽灯' },
  { text: '荧光灯', value: '荧光灯' }
])
const onWhiteBalanceConfirm = ({ selectedOptions }: any) => {
  whiteBalanceValue.value = selectedOptions[0].text
  showWhiteBalancePicker.value = false
  updateDisplaySetting()
}

// 数字降噪
const showDigitalNoiseReductionPicker = ref(false)
const digitalNoiseReductionValue = ref('普通模式')
const digitalNoiseReductionColumns = ref([
  { text: '普通模式', value: '普通模式' },
  { text: '增强模式', value: '增强模式' },
  { text: '关闭', value: '关闭' }
])
const onDigitalNoiseReductionConfirm = ({ selectedOptions }: any) => {
  digitalNoiseReductionValue.value = selectedOptions[0].text
  showDigitalNoiseReductionPicker.value = false
  updateDisplaySetting()
}

// 降噪等级
const noiseReductionLevel = ref(50)

// 透雾模式
const showDehazeModePicker = ref(false)
const dehazeModeValue = ref('关闭')
const dehazeModeColumns = ref([
  { text: '关闭', value: '关闭' },
  { text: '自动', value: '自动' },
  { text: '手动', value: '手动' }
])
const onDehazeModeConfirm = ({ selectedOptions }: any) => {
  dehazeModeValue.value = selectedOptions[0].text
  showDehazeModePicker.value = false
  updateDisplaySetting()
}

// 电子防抖
const showElectronicStabilizationPicker = ref(false)
const electronicStabilizationValue = ref('关闭')
const electronicStabilizationColumns = ref([
  { text: '关闭', value: '关闭' },
  { text: '开启', value: '开启' }
])
const onElectronicStabilizationConfirm = ({ selectedOptions }: any) => {
  electronicStabilizationValue.value = selectedOptions[0].text
  showElectronicStabilizationPicker.value = false
  updateDisplaySetting()
}

// 镜像
const showMirrorPicker = ref(false)
const mirrorValue = ref('关闭')
const mirrorColumns = ref([
  { text: '关闭', value: '关闭' },
  { text: '水平镜像', value: '水平镜像' },
  { text: '垂直镜像', value: '垂直镜像' }
])
const onMirrorConfirm = ({ selectedOptions }: any) => {
  mirrorValue.value = selectedOptions[0].text
  showMirrorPicker.value = false
  updateDisplaySetting()
}

// PAL(50HZ)
const showPal50hzPicker = ref(false)
const pal50hzValue = ref('关闭')
const pal50hzColumns = ref([
  { text: '关闭', value: '关闭' },
  { text: '开启', value: '开启' }
])
const onPal50hzConfirm = ({ selectedOptions }: any) => {
  pal50hzValue.value = selectedOptions[0].text
  showPal50hzPicker.value = false
  updateDisplaySetting()
}

// 镜头初始化
const showLensInitializationPicker = ref(false)
const lensInitializationValue = ref('关闭')
const lensInitializationColumns = ref([
  { text: '关闭', value: '关闭' },
  { text: '开启', value: '开启' }
])
const onLensInitializationConfirm = ({ selectedOptions }: any) => {
  lensInitializationValue.value = selectedOptions[0].text
  showLensInitializationPicker.value = false
  updateDisplaySetting()
}

// 变倍限制
const showZoomLimitPicker = ref(false)
const zoomLimitValue = ref('2')
const zoomLimitColumns = ref([
  { text: '1', value: '1' },
  { text: '2', value: '2' },
  { text: '3', value: '3' },
  { text: '4', value: '4' }
])
const onZoomLimitConfirm = ({ selectedOptions }: any) => {
  zoomLimitValue.value = selectedOptions[0].text
  showZoomLimitPicker.value = false
  updateDisplaySetting()
}

const payload = computed(() => ({
  id: displayData.value?.id,
  tenantId: displayData.value?.tenantId ?? '',
  deviceId: Number(displayData.value?.deviceId) || Number(props.deviceId),
  scene: sceneValue.value || '',
  brightness: safeNum(brightness.value, 0),
  contrast: safeNum(contrast.value, 0),
  saturation: safeNum(saturation.value, 0),
  sharpness: safeNum(sharpness.value, 0),
  exposureMode: exposureModeValue.value || '',
  maxShutterLimit: maxShutterValue.value || '',
  minShutterLimit: minShutterValue.value || '',
  gainLimit: safeNum(gainLimit.value, 0),
  lowLightElectronicShutter: lowLightShutterValue.value || '',
  focusMode: focusModeValue.value || '',
  minFocusDistance: minFocusValue.value || '',
  dayNightSwitch: dayNightValue.value || '',
  sensitivity: safeNum(sensitivityValue.value, 0),
  antiFillLightOverExposure: antiOverexposeValue.value || '',
  infraredLampMode: irModeValue.value || '',
  brightnessLimit: safeNum(brightnessLimit.value, 0),
  backlightCompensation: backlightCompValue.value || '',
  wideDynamic: wideDynamicValue.value || '',
  strongLightSuppression: strongLightValue.value || '',
  whiteBalance: whiteBalanceValue.value || '',
  digitalNoiseReduction: digitalNoiseReductionValue.value || '',
  noiseReductionLevel: safeNum(noiseReductionLevel.value, 0),
  defogMode: dehazeModeValue.value || '',
  electronicStabilization: electronicStabilizationValue.value || '',
  mirrorMode: mirrorValue.value || '',
  pal50hz: pal50hzValue.value || '',
  lensInitialization: lensInitializationValue.value || '',
  zoomLimit: safeNum(zoomLimitValue.value, 0),
  remark: displayData.value?.remark ?? ''
}))

const fetchDisplaySetting = async() => {
  if (!props.deviceId) return
  isInitializing.value = true
  hasLoaded.value = false
  try {
    const params: any = {
      accessToken: store.userInfo?.accessToken,
      deviceId: props.deviceId
    }
    const res: any = await getVlsCameraDisplaySetting(params)
    if (res && res.code === 200) {
      const data = res.data
      if (!data) return
      displayData.value = data

      sceneValue.value = data.scene || sceneValue.value
      brightness.value = safeNum(data.brightness, brightness.value)
      contrast.value = safeNum(data.contrast, contrast.value)
      saturation.value = safeNum(data.saturation, saturation.value)
      sharpness.value = safeNum(data.sharpness, sharpness.value)

      exposureModeValue.value = data.exposureMode || exposureModeValue.value
      maxShutterValue.value = data.maxShutterLimit || maxShutterValue.value
      minShutterValue.value = data.minShutterLimit || minShutterValue.value
      gainLimit.value = safeNum(data.gainLimit, gainLimit.value)
      lowLightShutterValue.value = data.lowLightElectronicShutter || lowLightShutterValue.value

      focusModeValue.value = data.focusMode || focusModeValue.value
      minFocusValue.value = data.minFocusDistance || minFocusValue.value

      dayNightValue.value = data.dayNightSwitch || dayNightValue.value
      sensitivityValue.value = String(data.sensitivity ?? sensitivityValue.value)
      antiOverexposeValue.value = data.antiFillLightOverExposure || antiOverexposeValue.value
      irModeValue.value = data.infraredLampMode || irModeValue.value
      brightnessLimit.value = safeNum(data.brightnessLimit, brightnessLimit.value)

      backlightCompValue.value = data.backlightCompensation || backlightCompValue.value
      wideDynamicValue.value = data.wideDynamic || wideDynamicValue.value
      strongLightValue.value = data.strongLightSuppression || strongLightValue.value

      whiteBalanceValue.value = data.whiteBalance || whiteBalanceValue.value
      digitalNoiseReductionValue.value = data.digitalNoiseReduction || digitalNoiseReductionValue.value
      noiseReductionLevel.value = safeNum(data.noiseReductionLevel, noiseReductionLevel.value)
      dehazeModeValue.value = data.defogMode || dehazeModeValue.value
      electronicStabilizationValue.value = data.electronicStabilization || electronicStabilizationValue.value

      mirrorValue.value = data.mirrorMode || mirrorValue.value
      pal50hzValue.value = data.pal50hz || pal50hzValue.value
      lensInitializationValue.value = data.lensInitialization || lensInitializationValue.value
      zoomLimitValue.value = String(data.zoomLimit ?? zoomLimitValue.value)
    }
  } finally {
    isInitializing.value = false
    await nextTick()
    hasLoaded.value = true
  }
}

// 恢复默认显示设置
const restoreDefault = async() => {
  if (!props.deviceId) return
  const params: any = {
    accessToken: store.userInfo?.accessToken,
    deviceId: props.deviceId
  }
  const res = await restoreDefaultVlsCameraDisplaySetting(params)
  if (res && res.code === 200) {
    await fetchDisplaySetting()
  }
}

const updateDisplaySetting = async() => {
  if (isInitializing.value) return
  if (!props.deviceId) return

  const params: any = {
    ...payload.value,
    accessToken: store.userInfo?.accessToken
  }
  await updateVlsCameraDisplaySetting(params)
}

watch(
  payload,
  () => {
    if (!isInitializing.value && hasLoaded.value) updateDisplaySetting()
  },
  { deep: true }
)

onMounted(() => {
  fetchDisplaySetting()
})

defineExpose({
  restoreDefault
})
</script>

<style scoped lang="scss">
.camera-settings {
  min-height: 100vh;
}

.section-title {
  margin: 24px 0 10px 0;
  color:#969799;
  font-size:12px;
}

.slider-group,
.field-group {
  background-color: #fff;
  border-radius: 7px;
  padding: 8px 0;
}

.slider-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  .slider-label {
    width: 100px;
    color: #969799;
    font-size: 14px;
  }

  .van-slider {
    flex: 1;
  }

  .slider-value {
    width: 40px;
    text-align: right;
    color: #333333;
    font-size: 14px;
  }

  :deep(.van-slider__button){
    width: 14px;
    height: 14px;
  }
}

.settings-field {
  --van-field-label-width: 140px;
  --van-field-label-font-size: 14px;
  --van-field-input-font-size: 14px;
  --van-field-label-color: #666666;
  --van-field-input-color: #323233;
  --van-cell-border-color: #f0f0f0;
  background-color: #fff;
  border-radius: 7px;
  margin-bottom: 8px;
}

:deep(.van-field__control){
  text-align: right;
  color: #333333;
  font-size: 14px;
}

:deep(.van-field__label){
  color: #969799;
  font-size: 14px;
}
</style>
