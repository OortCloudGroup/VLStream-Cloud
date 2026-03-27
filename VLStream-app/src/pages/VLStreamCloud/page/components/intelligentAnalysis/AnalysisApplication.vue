<template>
  <div class="analysis-application">
    <nav-header-banner title="分析申请" :is-call-back="true" @call-back="onBack" />
    <div class="application-content">
      <van-form class="form">
        <!-- 分析名称 -->
        <van-field v-model="formData.name" placeholder="请输分析名称" class="addMargin">
          <template #left-icon>
            <img :src="fxmc" alt="" class="field-icon" />
          </template>
        </van-field>

        <div class="divider">
          <div class="half-pixel-line" />
        </div>

        <!-- 开始/结束时间 -->
        <div class="time-range-section">
          <img :src="rq" alt="" class="section-icon" />
          <div class="time-range-item">
            <div class="time-label-row">
              <span class="section-title">开始</span>
            </div>
            <div class="time-fields">
              <van-field
                v-model="formData.startDate"
                is-link
                readonly
                placeholder="日期时间"
                class="time-field"
                @click="showStartDatePicker = true"
              />
            </div>
          </div>
          <div class="time-arrow" />
          <div class="time-range-item">
            <div class="time-label-row">
              <span class="section-title">结束</span>
            </div>
            <div class="time-fields">
              <van-field
                v-model="formData.endDate"
                is-link
                readonly
                placeholder="日期时间"
                class="time-field"
                @click="showEndDatePicker = true"
              />
            </div>
          </div>
        </div>

        <oort-popup v-model="showStartDatePicker" position="bottom">
          <van-date-picker @confirm="onStartDateConfirm" @cancel="showStartDatePicker = false" />
        </oort-popup>
        <oort-popup v-model="showStartTimePicker" position="bottom">
          <van-time-picker @confirm="onStartTimeConfirm" @cancel="showStartTimePicker = false" />
        </oort-popup>
        <oort-popup v-model="showEndDatePicker" position="bottom">
          <van-date-picker @confirm="onEndDateConfirm" @cancel="showEndDatePicker = false" />
        </oort-popup>
        <oort-popup v-model="showEndTimePicker" position="bottom">
          <van-time-picker @confirm="onEndTimeConfirm" @cancel="showEndTimePicker = false" />
        </oort-popup>

        <div class="divider">
          <div class="half-pixel-line" />
        </div>

        <!-- 分析区域 -->
        <div class="section">
          <div class="section-header">
            <span class="section-label">
              <img :src="fxql" alt="" class="section-icon" />
              分析区域
            </span>
            <img :src="add" alt="" class="section-add" @click="onAddArea" />
          </div>
          <div class="tag-list">
            <div v-for="(item, index) in formData.areas" :key="index" class="tag">
              {{ item }}
              <img
                :src="del"
                alt=""
                class="tag-delete"
                @click="removeArea(index)"
              />
            </div>
          </div>
        </div>

        <!-- 分析区域选择弹窗（级联） -->
        <van-popup
          v-model:show="showAreaPopup"
          position="bottom"
          style="width: 100%;height: 60%;"
          round
        >
          <div class="area-popup-wrapper">
            <div class="area-popup-header">
              <span class="area-popup-cancel" @click="onAreaCancel">取消</span>
              <span class="area-popup-title">选择分析区域</span>
              <span class="area-popup-confirm" @click="onAreaConfirm">确定</span>
            </div>
            <div class="area-popup-content">
              <van-cascader
                v-model="tempAreaCascaderValue"
                :options="areaOptions"
                @change="onAreaChange"
                @finish="onAreaFinish"
              />
            </div>
          </div>
        </van-popup>

        <div class="divider">
          <div class="half-pixel-line" />
        </div>

        <!-- 摄像头 -->
        <div class="section">
          <div class="section-header">
            <span class="section-label">
              <img :src="sxt" alt="" class="section-icon" />
              摄像头
            </span>
            <img :src="add" alt="" class="section-add" @click="onAddCamera" />
          </div>
          <div class="tag-list">
            <div v-for="(item, index) in formData.cameras" :key="index" class="tag">
              {{ item }}
              <img
                :src="del"
                alt=""
                class="tag-delete"
                @click="removeCamera(index)"
              />
            </div>
          </div>
        </div>

        <!-- 摄像头选择弹窗 -->
        <van-popup
          v-model:show="showCameraPopup"
          position="bottom"
          style="width: 100%;height: 60%;"
          round
        >
          <div class="camera-popup-wrapper">
            <div class="camera-popup-header">
              <span class="camera-popup-cancel" @click="onCameraCancel">取消</span>
              <span class="camera-popup-title">选择摄像头</span>
              <span class="camera-popup-confirm" @click="onCameraConfirm">确定</span>
            </div>
            <div class="camera-popup-content">
              <van-checkbox-group v-model="tempCameras">
                <van-checkbox
                  v-for="item in deviceOptions"
                  :key="item.id"
                  :name="item.deviceName"
                  shape="square"
                  class="camera-checkbox"
                >
                  {{ item.deviceName }}
                </van-checkbox>
              </van-checkbox-group>
            </div>
          </div>
        </van-popup>

        <div class="divider">
          <div class="half-pixel-line" />
        </div>

        <!-- 分析图片 -->
        <div class="section image-section">
          <div class="image-header flexRowAC">
            <img :src="fxtp" alt="" class="image-title-icon" />
            <span class="image-title">分析图片 ({{ imgArr.length }}/5)</span>
          </div>
          <div class="image-cover flexRowAC">
            <div class="uploadBox flexRowAC" @click="showUploader = true">
              <img :src="sctp" alt="" />
              上传图片
            </div>
            <template v-if="imgArr && imgArr.length">
              <div
                v-for="(item, i) in imgArr"
                :key="i"
                class="img1 d"
              >
                <oort_image class="img1" :src="item.url" alt="" />
                <van-icon
                  class="del_img"
                  name="close"
                  @click.stop="imgArr.splice(i, 1)"
                />
              </div>
            </template>
          </div>
        </div>

        <div class="divider">
          <div class="half-pixel-line" />
        </div>

        <!-- 分析描述 -->
        <van-field
          v-model="formData.description"
          type="textarea"
          placeholder="请输入分析描述"
          rows="4"
          class="taskDesc"
        />

        <!-- 常用语 -->
        <div class="common_expressions">
          <CommonExpressions :content="formData.description" @selectContent="selectContent" />
        </div>
      </van-form>

      <!-- 上传图片弹窗 -->
      <oort-uploader
        ref="oort_uploader"
        v-model:show="showUploader"
        :file="imgArr"
        @click="showUploader = false"
        @getImgUrl="getImg"
      />

      <!-- 底部按钮 -->
      <div class="submit-section">
        <van-button type="primary" block round @click="submit">
          确认申请
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import OortPopup from '@/components/popup/oort_popup.vue'
import CommonExpressions from '@/components/commonExpressions.vue'
import OortUploader from '@/components/uploader/oort_uploader.vue'
import Oort_image from '@/components/image/oort_image.vue'
import { getDeviceList } from '@/api/VLStreamCloud/device'
import { saveVlsAnalysisk } from '@/api/VLStreamCloud/intelligentAnalysis'
import { definitionDataGroupList } from '@/api/VLStreamCloud/region'
import { useUserStore } from '@/store/modules/useraPaas'
import { showToast } from 'vant'
import fxmc from '@/assets/img/VLStreamCloud/fxsq/fxmc.png'
import rq from '@/assets/img/VLStreamCloud/fxsq/rq.png'
import fxql from '@/assets/img/VLStreamCloud/fxsq/fxql.png'
import sxt from '@/assets/img/VLStreamCloud/fxsq/sxt.png'
import fxtp from '@/assets/img/VLStreamCloud/fxsq/fxtp.png'
import add from '@/assets/img/VLStreamCloud/fxsq/add.png'
import sctp from '@/assets/img/VLStreamCloud/fxsq/sctp.png'
import del from '@/assets/img/VLStreamCloud/fxsq/del.png'

const emit = defineEmits(['close'])
const store = useUserStore()

const formData = ref({
  name: '',
  startDate: '',
  startTime: '',
  endDate: '',
  endTime: '',
  areas: [],
  cameras: [],
  description: ''
})

const showStartDatePicker = ref(false)
const showStartTimePicker = ref(false)
const showEndDatePicker = ref(false)
const showEndTimePicker = ref(false)
const showUploader = ref(false)
const imgArr = ref<any[]>([])

// 分析区域列表（级联）
const areaOptions = ref<any[]>([])
const showAreaPopup = ref(false)
const areaCascaderValue = ref('')
const tempAreaCascaderValue = ref('')
const tempAreaSelectedOptions = ref<any[]>([])

// 设备列表
const deviceOptions = ref<any[]>([])
const showCameraPopup = ref(false)
const tempCameras = ref<string[]>([])

const fetchDevices = async() => {
  const params = {
    accessToken: store.userInfo?.accessToken
  }
  const res: any = await getDeviceList(params)
  if (res && res.code === 200 && res.data && Array.isArray(res.data.records)) {
    deviceOptions.value = res.data.records
  } else {
    deviceOptions.value = []
  }
}

// 获取分析区域列表（第一级）
const fetchAreas = async() => {
  const params = {
    accessToken: store.userInfo?.accessToken,
    group_type: 1,
    page: 1,
    pagesize: 99,
    puid: ''
  }
  const res: any = await definitionDataGroupList(params)
  if (res && res.code === 200 && res.data && Array.isArray(res.data.list)) {
    areaOptions.value = res.data.list.map((item: any) => ({
      value: item.uid,
      text: item.name,
      children: []
    }))
  } else {
    areaOptions.value = []
  }
}

const onStartDateConfirm = ({ selectedValues }: any) => {
  formData.value.startDate = Array.isArray(selectedValues) ? selectedValues.join('-') : selectedValues
  showStartDatePicker.value = false
}

const onStartTimeConfirm = ({ selectedValues }: any) => {
  formData.value.startTime = selectedValues.join(':')
  showStartTimePicker.value = false
}

const onEndDateConfirm = ({ selectedValues }: any) => {
  formData.value.endDate = Array.isArray(selectedValues) ? selectedValues.join('-') : selectedValues
  showEndDatePicker.value = false
}

const onEndTimeConfirm = ({ selectedValues }: any) => {
  formData.value.endTime = selectedValues.join(':')
  showEndTimePicker.value = false
}

const getImg = (file: any[]) => {
  imgArr.value = file
}

const removeArea = (index: number) => {
  formData.value.areas.splice(index, 1)
}

const onAddArea = () => {
  tempAreaCascaderValue.value = areaCascaderValue.value
  tempAreaSelectedOptions.value = []
  showAreaPopup.value = true
}

// 级联点击时按当前节点
const onAreaChange = async({ selectedOptions }: any) => {
  tempAreaSelectedOptions.value = selectedOptions || []

  const opts = selectedOptions || []
  if (!opts.length) return
  const last = opts[opts.length - 1] as any
  if (!last || (Array.isArray(last.children) && last.children.length)) {
    // 已经有子级了，不再重复请求
    return
  }

  const parentValue = last.value
  if (!parentValue) return

  const params = {
    accessToken: store.userInfo?.accessToken,
    group_type: 1,
    page: 1,
    pagesize: 99,
    puid: parentValue
  }
  const res: any = await definitionDataGroupList(params)
  if (res && res.code === 200 && res.data && Array.isArray(res.data.list) && res.data.list.length > 0) {
    last.children = res.data.list.map((item: any) => ({
      value: item.uid,
      text: item.name
    }))
  } else {
    delete last.children
  }
}

// 级联选择完成时保存完整路径
const onAreaFinish = ({ selectedOptions }: any) => {
  tempAreaSelectedOptions.value = selectedOptions || []
}

// 取消选择区域
const onAreaCancel = () => {
  showAreaPopup.value = false
  tempAreaCascaderValue.value = ''
  tempAreaSelectedOptions.value = []
}

// 确认选择区域
const onAreaConfirm = () => {
  showAreaPopup.value = false
  const names = (tempAreaSelectedOptions.value || []).map((opt: any) => opt.text)
  const text = names.join('/')
  formData.value.areas = text ? [text] : []
  areaCascaderValue.value = tempAreaCascaderValue.value
}

const removeCamera = (index: number) => {
  formData.value.cameras.splice(index, 1)
}

const onAddCamera = () => {
  // 打开摄像头选择弹窗
  tempCameras.value = [...formData.value.cameras]
  showCameraPopup.value = true
}

const selectContent = (content: string) => {
  formData.value.description = content
}

const onCameraConfirm = () => {
  formData.value.cameras = [...tempCameras.value] as any
  showCameraPopup.value = false
}

const onCameraCancel = () => {
  showCameraPopup.value = false
}

const submit = async() => {
  const hasStartDate = !!formData.value.startDate
  const hasStartTime = !!formData.value.startTime
  const hasEndDate = !!formData.value.endDate
  const hasEndTime = !!formData.value.endTime

  // 默认开始时间 00:00:00，结束时间 23:59:59
  const startDateTime = hasStartDate
    ? `${formData.value.startDate} ${hasStartTime ? formData.value.startTime : '00:00:00'}`
    : ''

  const endDateTime = hasEndDate
    ? `${formData.value.endDate} ${hasEndTime ? formData.value.endTime : '23:59:59'}`
    : ''

  // 时间范围
  const timeRange =
    startDateTime && endDateTime
      ? `${startDateTime} ~ ${endDateTime}`
      : ''

  // 分析区域
  const regionInfo = formData.value.areas.join('，')

  // 设备ID
  const selectedNames = formData.value.cameras || []
  const selectedIds = (deviceOptions.value as any[])
    .filter((item: any) => (selectedNames as any[]).includes(item.deviceName))
    .map((item: any) => item.id)
  const deviceIds = selectedIds.join('，')

  // 图片列表
  const images = imgArr.value
    .map((item: any) => item.url || item.path || '')
    .filter((v: string) => v)
    .join(',')

  const payload = {
    id: 0,
    tenantId: '',
    analysisName: formData.value.name || '',
    analysisType: '',
    deviceIds,
    regionInfo,
    timeRange,
    images,
    requestStatus: 'processing', // 新申请默认处理中
    progress: 0,
    resultPath: '',
    startTime: startDateTime,
    completeTime: endDateTime,
    errorMessage: '',
    description: formData.value.description || ''
  }

  const res: any = await saveVlsAnalysisk(payload)
  if (res && res.code === 200) {
    showToast('申请提交成功')
    emit('close')
  }
}

const onBack = () => {
  emit('close')
}

onMounted(() => {
  fetchDevices()
  fetchAreas()
})
</script>

<style scoped lang="scss">
.analysis-application {
  height: 100vh;
  overflow: hidden;
  background: #f3f3f3;
  display: flex;
  flex-direction: column;
}

.application-content {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 80px;
}

.form {
  background: #fff;
}

.addMargin {
  margin: 0;
}

.divider {
  padding: 0 16px;
}

.half-pixel-line {
  height: 0.5px;
  background: #ebedf0;
}

.time-range-section {
  padding: 16px;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-icon {
  width: 18px;
  height: 18px;
  object-fit: contain;
}

.section-title {
  font-size: 14px;
  color: #969799;
}

.time-fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.time-field {
  padding: 0;
}

.time-arrow {
  position: relative;
  width: 32px;
  height: 40px;
  margin: 0 24px;

  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 1px;
    height: 32px;
    background: #b0bcc8;
    transform: translate(-50%, -50%) rotate(60deg);
  }
}

:deep(.time-field .van-field__right-icon) {
  display: none;
}

.section {
  padding: 16px;
  background: #fff;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-label {
  font-size: 14px;
  color: #323233;
  display: flex;
  align-items: center;
  gap: 6px;
}

.section-add {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  margin-left: 26px;
  gap: 8px;
}

.tag {
  padding: 6px 10px;
  background: #f0f0f0;
  border-radius: 4px;
  font-size: 12px;
  color: #323233;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tag-delete {
  width: 14px;
  height: 14px;
  object-fit: contain;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.image-slot {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background: #f7f8fa;
}

.uploaded-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.upload-icon {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.upload-text {
  font-size: 12px;
  color: #969799;
}

.uploadBox {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 68px;
  height: 68px;
  background: #f7f7f7;
  border-radius: 4px;
  margin-right: 8px;
  font-size: 12px;
  color: #989eb5;
  text-align: center;
  font-weight: 400;

  > img {
    width: 20px;
    height: 20px;
    margin: 0 !important;
  }
}

.image-cover {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  flex-wrap: wrap;

  .d {
    position: relative;
  }

  img.img1 {
    flex-shrink: 0;
    width: 68px;
    height: 68px;
    background-size: cover;
  }

  .del_img {
    position: absolute;
    top: 0;
    right: 0;
    color: red;
  }
}

.image-delete {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 16px;
  height: 16px;
  object-fit: contain;
}

.taskDesc :deep(.van-field__body) {
  margin-left: 26px !important;
}

.common_expressions {
  background-color: #fff;
  margin: 10px 10px 10px 25px;
  border-top: 1px solid #d8d8d8;
}

.camera-popup-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.camera-popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  font-size: 14px;
}

.camera-popup-cancel,
.camera-popup-confirm {
  color: #1989fa;
}

.camera-popup-title {
  font-weight: 600;
  color: #323233;
}

.camera-popup-content {
  padding: 12px 16px 24px;
  overflow-y: auto;
}

.camera-checkbox {
  margin-bottom: 8px;
}

.area-popup-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.area-popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  font-size: 14px;
}

.area-popup-cancel,
.area-popup-confirm {
  color: #1989fa;
}

.area-popup-title {
  font-weight: 600;
  color: #323233;
}

.area-popup-content {
  padding: 12px 16px 24px;
  overflow-y: auto;
  flex: 1;
}

// 隐藏 van-cascader 的默认头部
:deep(.area-popup-content .van-cascader__header) {
  display: none;
}

.submit-section {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  padding: 12px 16px;
  background: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
}

.field-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.image-header{
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 12px;
}

.image-title-icon{
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.time-range-item{
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  :deep(.van-cell__right-icon){
    display: none;
  }
}
</style>
