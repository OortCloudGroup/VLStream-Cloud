<template>
  <div class="osd-settings">
    <!-- 显示设置（复选框组 - 方形） -->
    <div class="show-setting">
      <div class="section-title">
        显示设置
      </div>
      <van-checkbox-group v-model="showChecked" shape="square" class="checkbox-group">
        <van-checkbox name="showDate">
          显示日期
        </van-checkbox>
        <van-checkbox name="showName">
          显示名称
        </van-checkbox>
        <van-checkbox name="showWeek">
          显示星期
        </van-checkbox>
      </van-checkbox-group>
    </div>

    <!-- 通道名称 -->
    <van-field
      v-model="channelName"
      label="通道名称"
      placeholder="请输入通道名称"
      class="settings-field"
    />

    <!-- 时间格式 - 下拉选择 -->
    <van-field
      v-model="timeFormatValue"
      is-link
      readonly
      label="时间格式"
      class="settings-field"
      @click="showTimeFormatPicker = true"
    />
    <oort-popup v-model="showTimeFormatPicker" round position="bottom">
      <van-picker
        :columns="timeFormatColumns"
        @cancel="showTimeFormatPicker = false"
        @confirm="onTimeFormatConfirm"
      />
    </oort-popup>

    <!-- 日期格式 - 下拉选择 -->
    <van-field
      v-model="dateFormatValue"
      is-link
      readonly
      label="日期格式"
      class="settings-field"
      @click="showDateFormatPicker = true"
    />
    <oort-popup v-model="showDateFormatPicker" round position="bottom">
      <van-picker
        :columns="dateFormatColumns"
        @cancel="showDateFormatPicker = false"
        @confirm="onDateFormatConfirm"
      />
    </oort-popup>

    <!-- 字符叠加（复选框组+输入框 - 方形） -->
    <div class="zfdj">
      <div class="section-title">
        字符叠加
      </div>
      <div class="osd-overlay-group">
        <van-checkbox-group v-model="overlayChecked" shape="square" class="overlay-checkbox-group">
          <div v-for="(item, idx) in 3" :key="idx" class="overlay-item">
            <van-checkbox :name="`overlay${idx+1}`" />
            <span class="overlay-label">{{ idx+1 }}</span>
            <van-field
              v-model="overlayText[`text${idx+1}`]"
              placeholder="请输入"
              class="overlay-input"
            />
          </div>
        </van-checkbox-group>
      </div>
    </div>

    <!-- OSD属性 - 下拉选择 -->
    <van-field
      v-model="osdAttrValue"
      is-link
      readonly
      label="OSD属性"
      class="settings-field"
      @click="showOsdAttrPicker = true"
    />
    <oort-popup v-model="showOsdAttrPicker" round position="bottom">
      <van-picker
        :columns="osdAttrColumns"
        @cancel="showOsdAttrPicker = false"
        @confirm="onOsdAttrConfirm"
      />
    </oort-popup>

    <!-- OSD字体 - 下拉选择 -->
    <van-field
      v-model="osdFontValue"
      is-link
      readonly
      label="OSD字体"
      class="settings-field"
      @click="showOsdFontPicker = true"
    />
    <oort-popup v-model="showOsdFontPicker" round position="bottom">
      <van-picker
        :columns="osdFontColumns"
        @cancel="showOsdFontPicker = false"
        @confirm="onOsdFontConfirm"
      />
    </oort-popup>

    <!-- OSD颜色 - 下拉选择 -->
    <van-field
      v-model="osdColorValue"
      is-link
      readonly
      label="OSD颜色"
      class="settings-field"
      @click="showOsdColorPicker = true"
    />
    <oort-popup v-model="showOsdColorPicker" round position="bottom">
      <van-picker
        :columns="osdColorColumns"
        @cancel="showOsdColorPicker = false"
        @confirm="onOsdColorConfirm"
      />
    </oort-popup>

    <!-- 对齐方式 - 下拉选择 -->
    <van-field
      v-model="alignValue"
      is-link
      readonly
      label="对齐方式"
      class="settings-field"
      @click="showAlignPicker = true"
    />
    <oort-popup v-model="showAlignPicker" round position="bottom">
      <van-picker
        :columns="alignColumns"
        @cancel="showAlignPicker = false"
        @confirm="onAlignConfirm"
      />
    </oort-popup>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { getVlsCameraOsdSetting, updateVlsCameraOsdSetting, restoreDefaultVlsCameraOsdSetting } from '@/api/VLStreamCloud/device'
import { useUserStore } from '@/store/modules/useraPaas'
import OortPopup from '@/components/popup/oort_popup.vue'

const props = defineProps<{
  deviceId?: string | number
}>()

const store = useUserStore()

// 存储当前 OSD 设置数据
const osdData = ref<any>(null)
const isInitializing = ref(false)
const hasLoaded = ref(false)

// ********** 显示设置 - 方形复选框组 **********
const showChecked = ref<string[]>([])

// ********** 通道名称 **********
const channelName = ref('')

// ********** 时间格式 - 下拉选择 **********
const showTimeFormatPicker = ref(false)
const timeFormatValue = ref('')
const timeFormatColumns = ref([
  { text: '24小时制', value: '24小时制' },
  { text: '12小时制', value: '12小时制' }
])
const onTimeFormatConfirm = ({ selectedOptions }: { selectedOptions: any[] }) => {
  timeFormatValue.value = selectedOptions[0]?.text || selectedOptions[0]?.value || ''
  showTimeFormatPicker.value = false
  updateOsdSetting()
}

// ********** 日期格式 - 下拉选择 **********
const showDateFormatPicker = ref(false)
const dateFormatValue = ref('')
const dateFormatColumns = ref([
  { text: 'XXXX年XX月XX日', value: 'XXXX年XX月XX日' },
  { text: 'XXXX-XX-XX', value: 'XXXX-XX-XX' },
  { text: 'XX/XX/XXXX', value: 'XX/XX/XXXX' }
])
const onDateFormatConfirm = ({ selectedOptions }: { selectedOptions: any[] }) => {
  dateFormatValue.value = selectedOptions[0]?.text || selectedOptions[0]?.value || ''
  showDateFormatPicker.value = false
  updateOsdSetting()
}

// ********** 字符叠加 - 方形复选框组+输入框 **********
const overlayChecked = ref<string[]>([])
const overlayText = ref({
  text1: '',
  text2: '',
  text3: ''
})

// ********** OSD属性 - 下拉选择 **********
const showOsdAttrPicker = ref(false)
const osdAttrValue = ref('')
const osdAttrColumns = ref([
  { text: '不透明、不闪烁', value: '不透明、不闪烁' },
  { text: '透明、不闪烁', value: '透明、不闪烁' },
  { text: '不透明、闪烁', value: '不透明、闪烁' },
  { text: '透明、闪烁', value: '透明、闪烁' }
])
const onOsdAttrConfirm = ({ selectedOptions }: { selectedOptions: any[] }) => {
  osdAttrValue.value = selectedOptions[0]?.text || selectedOptions[0]?.value || ''
  showOsdAttrPicker.value = false
  updateOsdSetting()
}

// ********** OSD字体 - 下拉选择 **********
const showOsdFontPicker = ref(false)
const osdFontValue = ref('')
const osdFontColumns = ref([
  { text: '自适应', value: '自适应' },
  { text: '宋体', value: '宋体' },
  { text: '黑体', value: '黑体' }
])
const onOsdFontConfirm = ({ selectedOptions }: { selectedOptions: any[] }) => {
  osdFontValue.value = selectedOptions[0]?.text || selectedOptions[0]?.value || ''
  showOsdFontPicker.value = false
  updateOsdSetting()
}

// ********** OSD颜色 - 下拉选择 **********
const showOsdColorPicker = ref(false)
const osdColorValue = ref('')
const osdColorColumns = ref([
  { text: '黑白自动', value: '黑白自动' },
  { text: '白色', value: '白色' },
  { text: '黑色', value: '黑色' }
])
const onOsdColorConfirm = ({ selectedOptions }: { selectedOptions: any[] }) => {
  osdColorValue.value = selectedOptions[0]?.text || selectedOptions[0]?.value || ''
  showOsdColorPicker.value = false
  updateOsdSetting()
}

// ********** 对齐方式 - 下拉选择 **********
const showAlignPicker = ref(false)
const alignValue = ref('')
const alignColumns = ref([
  { text: '自适应', value: '自适应' },
  { text: '左对齐', value: '左对齐' },
  { text: '右对齐', value: '右对齐' },
  { text: '居中', value: '居中' }
])
const onAlignConfirm = ({ selectedOptions }: { selectedOptions: any[] }) => {
  alignValue.value = selectedOptions[0]?.text || selectedOptions[0]?.value || ''
  showAlignPicker.value = false
  updateOsdSetting()
}

// 获取 OSD 设置
const fetchOsdSetting = async() => {
  if (!props.deviceId) return

  hasLoaded.value = false
  isInitializing.value = true
  try {
    const params: any = {
      accessToken: store.userInfo?.accessToken,
      deviceId: props.deviceId
    }
    const res: any = await getVlsCameraOsdSetting(params)
    if (res && res.code === 200) {
      const data = res.data
      osdData.value = data

      // 填充表单数据
      showChecked.value = []
      if (data.showName === 1) showChecked.value.push('showName')
      if (data.showDate === 1) showChecked.value.push('showDate')
      if (data.showWeek === 1) showChecked.value.push('showWeek')

      channelName.value = data.channelName || ''
      timeFormatValue.value = data.timeFormat || ''
      dateFormatValue.value = data.dateFormat || ''

      overlayChecked.value = []
      if (data.overlay1Enabled === 1) overlayChecked.value.push('overlay1')
      if (data.overlay2Enabled === 1) overlayChecked.value.push('overlay2')
      if (data.overlay3Enabled === 1) overlayChecked.value.push('overlay3')

      overlayText.value.text1 = data.overlay1Text || ''
      overlayText.value.text2 = data.overlay2Text || ''
      overlayText.value.text3 = data.overlay3Text || ''

      osdAttrValue.value = data.osdProperty || ''
      osdFontValue.value = data.osdFont || ''
      osdColorValue.value = data.osdColor || ''
      alignValue.value = data.alignMode || ''
    }
  } finally {
    isInitializing.value = false
    await nextTick()
    hasLoaded.value = true
  }
}

// 恢复默认 OSD 设置
const restoreDefault = async() => {
  if (!props.deviceId) return
  const params: any = {
    accessToken: store.userInfo?.accessToken,
    deviceId: props.deviceId
  }
  const res = await restoreDefaultVlsCameraOsdSetting(params)
  if (res && res.code === 200) {
    await fetchOsdSetting()
  }
}

// 更新 OSD 设置
const updateOsdSetting = async() => {
  // 初始化回填阶段不触发更新
  if (!osdData.value || isInitializing.value || !hasLoaded.value) return
  const payload: any = {
    id: osdData.value.id,
    tenantId: osdData.value.tenantId || '',
    deviceId: Number(osdData.value.deviceId) || Number(props.deviceId),
    showName: showChecked.value.includes('showName') ? 1 : 0,
    showDate: showChecked.value.includes('showDate') ? 1 : 0,
    showWeek: showChecked.value.includes('showWeek') ? 1 : 0,
    channelName: channelName.value || '',
    timeFormat: timeFormatValue.value || '',
    dateFormat: dateFormatValue.value || '',
    overlay1Enabled: overlayChecked.value.includes('overlay1') ? 1 : 0,
    overlay1Text: overlayText.value.text1 || '',
    overlay2Enabled: overlayChecked.value.includes('overlay2') ? 1 : 0,
    overlay2Text: overlayText.value.text2 || '',
    overlay3Enabled: overlayChecked.value.includes('overlay3') ? 1 : 0,
    overlay3Text: overlayText.value.text3 || '',
    osdProperty: osdAttrValue.value || '',
    osdFont: osdFontValue.value || '',
    osdColor: osdColorValue.value || '',
    alignMode: alignValue.value || '',
    remark: osdData.value.remark || ''
  }

  const params = {
    ...payload,
    accessToken: store.userInfo?.accessToken
  }

  await updateVlsCameraOsdSetting(params)
}

// 监听字段变化
watch(showChecked, () => {
  if (!isInitializing.value && hasLoaded.value) {
    updateOsdSetting()
  }
}, { deep: true })

watch(channelName, () => {
  if (!isInitializing.value && hasLoaded.value) {
    updateOsdSetting()
  }
})

watch(overlayChecked, () => {
  if (!isInitializing.value && hasLoaded.value) {
    updateOsdSetting()
  }
}, { deep: true })

watch(() => overlayText.value.text1, () => {
  if (!isInitializing.value && hasLoaded.value) {
    updateOsdSetting()
  }
})

watch(() => overlayText.value.text2, () => {
  if (!isInitializing.value && hasLoaded.value) {
    updateOsdSetting()
  }
})

watch(() => overlayText.value.text3, () => {
  if (!isInitializing.value && hasLoaded.value) {
    updateOsdSetting()
  }
})

onMounted(() => {
  fetchOsdSetting()
})

defineExpose({
  restoreDefault
})
</script>

<style scoped lang="scss">
.osd-settings {
  min-height: 100vh;
  box-sizing: border-box;
  background: #ffffff;
  padding: 12px;
  border-radius: 7px;
}

// 模块标题样式
.show-setting{
  display: flex;
  justify-content: space-between;
}
.section-title {
  color:#666666;
  font-size:14px;
  white-space: nowrap;
}

// 显示设置 - 复选框组样式
.checkbox-group {
  margin-bottom: 8px;
  --van-checkbox-label-font-size: 16px;
  --van-checkbox-color: #1a53ff;
  --van-checkbox-icon-size: 18px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  direction: rtl;
  gap: 8px;

  :deep(.van-checkbox) {
    direction: ltr !important;
    justify-content: flex-end !important;
  }
}

// 通用下拉/输入框样式
.settings-field {
  --van-field-label-width: 100px;
  --van-field-label-font-size: 16px;
  --van-field-input-font-size: 16px;
  --van-field-label-color: #323233;
  --van-field-input-color: #323233;
  --van-cell-border-color: transparent;
  border-radius: 12px;
  margin-bottom: 8px;
  padding: 14px 16px;
}

// 字符叠加模块容器
.osd-overlay-group {
  border-radius: 12px;
  margin-bottom: 8px;
  overflow: hidden;
}

// 字符叠加 - 复选框组+输入框样式
.overlay-checkbox-group {
  --van-checkbox-color: #1989fa;
  --van-checkbox-icon-size: 18px;
  --van-checkbox-label-font-size: 16px;

  .overlay-item {
    display: flex;
    align-items: center;
    border-bottom: 1px solid #f0f0f0;
    box-sizing: border-box;

    &:last-child {
      border-bottom: none;
    }

    .overlay-label {
      font-size: 16px;
      color: #323233;
      margin: 0 16px;
      width: 20px;
    }

    .overlay-input {
      flex: 1;
      --van-field-padding: 0;
      --van-field-border: none;
      --van-field-placeholder-color: #c8c9cc;
      --van-field-input-font-size: 16px;
    }
  }
}

// 适配Vant Picker弹窗样式
:deep(.van-picker) {
  --van-picker-title-font-size: 16px;
  --van-picker-item-font-size: 18px;
}

:deep(.van-cell){
  background: transparent;
  padding-left: 0;
  padding-right: 0;
  .van-field__label{
    color: #666666;
    font-size: 14px;
  }
}

.zfdj{
  display: flex;
  justify-content: space-between;
  .section-title{
    padding-top: 10px;
    padding-right: 20px;
  }
}

:deep(.van-field__control){
  text-align: right;
  color: #333333;
  font-size: 14px;
}
</style>
