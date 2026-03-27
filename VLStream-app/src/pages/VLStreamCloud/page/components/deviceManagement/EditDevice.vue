<template>
  <div class="edit-device">
    <nav-header-banner title="编辑设备" :is-call-back="true" @call-back="onBack">
      <span class="header-save" @click="onSave">保存</span>
    </nav-header-banner>
    <div class="edit_content">
      <van-form class="form">
        <!-- 基本信息 -->
        <div class="block-title">
          基本信息
        </div>

        <!-- 设备名称 -->
        <van-field
          v-model="form.name"
          placeholder="请输入设备名称"
          label-align="top"
          class="field-item"
        >
          <template #label>
            <div class="field-label">
              <img :src="fxmc" alt="必填" class="required-icon" />
              <span>设备名称</span>
            </div>
          </template>
        </van-field>

        <!-- 视频流路径（必填） -->
        <van-field
          v-model="form.streamUrl"
          placeholder="请输入视频流路径"
          label-align="top"
          class="field-item"
        >
          <template #label>
            <div class="field-label">
              <img :src="lj" alt="必填" class="required-icon" />
              <span>视频流路径</span>
            </div>
          </template>
        </van-field>

        <!-- 设备标签 -->
        <van-field label-align="top" class="field-item" readonly>
          <template #label>
            <div class="section-header">
              <div class="field-label">
                <img :src="sbbq" alt="必填" class="required-icon" />
                <span>设备标签</span>
              </div>
              <img :src="add" class="section-add" alt="" @click="openTagPopup" />
            </div>
          </template>
          <template #input>
            <div class="tag-list">
              <div
                v-for="(tag, index) in form.tags"
                :key="tag"
                class="tag"
              >
                {{ tag }}
                <img
                  src="@/assets/img/patrolManagement/delete.png"
                  alt=""
                  class="tag-delete"
                  @click.stop="removeTag(index)"
                />
              </div>
            </div>
          </template>
        </van-field>

        <!-- 是否公开 -->
        <van-field label-align="top" class="field-item" readonly>
          <template #label>
            <div class="field-label">
              <img :src="sbbq" alt="必填" class="required-icon" />
              <span>是否公开</span>
            </div>
          </template>
          <template #input>
            <div class="switch-value">
              <span :class="{ active: !form.isPublic }">不公开</span>
              <van-switch v-model="form.isPublic" size="20" />
              <span :class="{ active: form.isPublic }">公开</span>
            </div>
          </template>
        </van-field>

        <!-- 更多信息 -->
        <div class="block-title more-title">
          更多信息
        </div>

        <!-- 设备ID -->
        <van-field
          :model-value="form.deviceId"
          placeholder="请输入设备ID"
          label-align="top"
          class="field-item"
        >
          <template #label>
            <div class="field-label">
              <img :src="sbid" alt="必填" class="required-icon" />
              <span>设备ID</span>
            </div>
          </template>
        </van-field>

        <!-- 图片路径 / 图片上传 -->
        <div class="section image-section">
          <div class="image-header flexRowAC">
            <img :src="fxtp" alt="" class="image-title-icon" />
            <span class="image-title">图片路径</span>
          </div>
          <div class="image-cover flexRowAC">
            <div class="uploadBox flexRowAC" @click="showUploader = true">
              <img :src="sctp" alt="" />
              上传图片
            </div>
            <template v-if="imgArr && imgArr.length">
              <div class="img1 d">
                <oort_image class="img1" :src="imgArr[0].url" alt="" />
                <van-icon
                  class="del_img"
                  name="close"
                  @click.stop="removeImage"
                />
              </div>
            </template>
          </div>
        </div>

        <!-- 经纬度坐标 -->
        <van-field label-align="top" class="field-item">
          <template #label>
            <div class="field-label">
              <img :src="sbzb" alt="必填" class="required-icon" />
              <span>经纬度坐标</span>
            </div>
          </template>
          <template #input>
            <div class="coord-row" @click="openSelectPoint">
              <span class="coord-text">{{ form.longitude || '输入经度' }}</span>
              <span class="coord-split">-</span>
              <span class="coord-text">{{ form.latitude || '输入纬度' }}</span>
            </div>
          </template>
        </van-field>

        <!-- 区划选择 -->
        <van-field label-align="top" class="field-item" readonly @click="openRegionPicker">
          <template #label>
            <div class="section-header">
              <div class="field-label">
                <img :src="sbqh" alt="必填" class="required-icon" />
                <span>区划选择</span>
              </div>
              <van-icon name="arrow" class="section-arrow" />
            </div>
          </template>
          <template #input>
            <div class="tag-list">
              <div
                v-for="(area, index) in form.regions"
                :key="area"
                class="tag"
              >
                {{ area }}
                <img
                  src="@/assets/img/patrolManagement/delete.png"
                  alt=""
                  class="tag-delete"
                  @click.stop="removeRegion(index)"
                />
              </div>
            </div>
          </template>
        </van-field>

        <!-- 详细地址 -->
        <van-field
          v-model="form.address"
          placeholder="请输入详细地址"
          label-align="top"
          class="field-item"
        >
          <template #label>
            <div class="field-label">
              <img :src="sbwz" alt="必填" class="required-icon" />
              <span>详细地址</span>
            </div>
          </template>
        </van-field>

        <!-- 备注 -->
        <van-field
          v-model="form.remark"
          type="textarea"
          placeholder="请输入备注"
          rows="4"
          class="field-item"
        />

        <!-- 常用语 -->
        <div class="common_expressions">
          <CommonExpressions :content="form.remark" @selectContent="onSelectContent" />
        </div>
      </van-form>
    </div>
    <oort-popup
      v-model="showSelectPoint"
      style="width: 100%;height: 80%;"
      position="bottom"
      round
      title="选择位置"
    >
      <SelectPointFormMap
        v-if="showSelectPoint"
        is-around-search
        @confirm="onSelectPointConfirm"
        @cancel="showSelectPoint = false"
      />
    </oort-popup>
    <van-popup
      v-model:show="showTagPopup"
      position="bottom"
      style="width: 100%;height: 50%;"
      round
    >
      <div class="tag-picker-wrapper">
        <div class="tag-picker-header">
          <span class="tag-picker-cancel" @click="onTagCancel">取消</span>
          <span class="tag-picker-title">选择设备标签</span>
          <span class="tag-picker-confirm" @click="onTagConfirm">确定</span>
        </div>
        <div class="tag-popup-content">
          <div class="tag-list">
            <div
              v-for="item in tagOptions"
              :key="item"
              class="tag-chip"
              :class="{ active: tempTags.includes(item) }"
              @click="toggleTag(item)"
            >
              {{ item }}
            </div>
          </div>
        </div>
      </div>
    </van-popup>

    <!-- 区划选择（中国省市区） -->
    <van-popup v-model:show="showRegionPicker" round position="bottom">
      <van-cascader
        v-model="regionCascaderValue"
        title="请选择所在地区"
        :options="regionOptions"
        @close="showRegionPicker = false"
        @finish="onRegionFinish"
      />
    </van-popup>

    <!-- 图片上传 -->
    <oort-uploader
      ref="oort_uploader"
      v-model:show="showUploader"
      :file="imgArr"
      @click="showUploader = false"
      @getImgUrl="getImg"
    />
  </div>
</template>

<script setup>
import { reactive, onMounted, ref } from 'vue'
import { useCascaderAreaData } from '@vant/area-data'
import CommonExpressions from '@/components/commonExpressions.vue'
import { getDeviceDetail, updateDevice } from '@/api/VLStreamCloud/device'
import { getVlsTagManagement } from '@/api/VLStreamCloud/tag'
import OortUploader from '@/components/uploader/oort_uploader.vue'
import Oort_image from '@/components/image/oort_image.vue'
import lj from '@/assets/img/VLStreamCloud/sbgl/lj.png'
import sbbq from '@/assets/img/VLStreamCloud/sbgl/sbbq.png'
import sbid from '@/assets/img/VLStreamCloud/sbgl/sbid.png'
import sbqh from '@/assets/img/VLStreamCloud/sbgl/sbqh.png'
import sbwz from '@/assets/img/VLStreamCloud/sbgl/sbwz.png'
import sbzb from '@/assets/img/VLStreamCloud/sbgl/sbzb.png'
import add from '@/assets/img/VLStreamCloud/sbgl/add.png'
import fxmc from '@/assets/img/VLStreamCloud/fxsq/fxmc.png'
import fxtp from '@/assets/img/VLStreamCloud/fxsq/fxtp.png'
import sctp from '@/assets/img/VLStreamCloud/fxsq/sctp.png'
import OortPopup from '@/components/popup/oort_popup.vue'
import SelectPointFormMap from '@/components/map/selectPointFormMap.vue'
import { useUserStore } from '@/store/modules/useraPaas'

const props = defineProps({
  deviceId: {
    type: [Number, String],
    required: false
  },
  device: {
    type: Object,
    default: () => ({})
  }
})

const store = useUserStore()
const emit = defineEmits(['close', 'saved'])

const form = reactive({
  name: '',
  streamUrl: '',
  tags: [],
  isPublic: false,
  deviceId: '',
  imageUrl: '',
  longitude: '',
  latitude: '',
  regions: [],
  address: '',
  remark: ''
})

const deviceData = reactive({})

const showSelectPoint = ref(false)
const showTagPopup = ref(false)
const tagOptions = ref([])
const tempTags = ref([])

// 图片上传
const showUploader = ref(false)
const imgArr = ref([])

// 区划选择（省/市/区）
const showRegionPicker = ref(false)
const regionFieldValue = ref('')
const regionCascaderValue = ref('')
const regionOptions = useCascaderAreaData()

const openRegionPicker = () => {
  showRegionPicker.value = true
}

const onRegionFinish = ({ selectedOptions }) => {
  showRegionPicker.value = false
  const text = (selectedOptions || []).map((opt) => opt.text).join('/')
  regionFieldValue.value = text
  form.regions = text ? [text] : []
}

// 获取标签列表
const fetchTags = async() => {
  const params = {
    accessToken: store.userInfo?.accessToken,
    parentId: 1
  }
  const res = await getVlsTagManagement(params)
  if (res && res.code === 200 && res.data && Array.isArray(res.data.records)) {
    const names = res.data.records
      .map((item) => String(item.tagName || '').trim())
      .filter((name) => !!name)
    tagOptions.value = names
  }
}

// 处理图片选择结果
const getImg = (files) => {
  if (Array.isArray(files) && files.length) {
    const first = files[0]
    const url = first.url || first.path || ''
    imgArr.value = url ? [{ url }] : []
    form.imageUrl = url
  } else {
    imgArr.value = []
    form.imageUrl = ''
  }
}

const removeImage = () => {
  imgArr.value = []
  form.imageUrl = ''
}

const loadDevice = async() => {
  const id = props.deviceId || props.device?.id
  if (!id) return
  const params = {
    accessToken: store.userInfo?.accessToken,
    id: id
  }
  const res = await getDeviceDetail(params)
  if (res.code === 200 && res.data) {
    const d = res.data
    Object.assign(deviceData, d)
    form.name = d.deviceName || ''
    form.streamUrl = d.streamUrl || ''
    if (d.tag) {
      form.tags = Array.isArray(d.tag) ? d.tag : (d.tag.split(',').map(t => t.trim()).filter(t => t))
    } else if (d.selectedTags && Array.isArray(d.selectedTags)) {
      form.tags = d.selectedTags
    } else {
      form.tags = []
    }
    form.isPublic = d.isPublic === 1
    form.deviceId = d.deviceId || ''
    form.imageUrl = d.imagePath || ''
    imgArr.value = form.imageUrl ? [{ url: form.imageUrl }] : []
    form.longitude = d.longitude ?? ''
    form.latitude = d.latitude ?? ''
    form.regions = d.region ? [d.region] : []
    regionFieldValue.value = d.region || ''
    form.address = d.address || ''
    form.remark = d.remark || ''
  }
}

onMounted(() => {
  loadDevice()
  fetchTags()
})

const removeTag = (index) => {
  form.tags.splice(index, 1)
}

const removeRegion = (index) => {
  form.regions.splice(index, 1)
}

const onSelectContent = (content) => {
  form.remark = content
}

const onSave = async() => {
  const id = props.deviceId || props.device?.id
  if (!id) {
    emit('close')
    return
  }

  const d = deviceData || {}

  const payload = {
    ...d,
    deviceName: form.name || d.deviceName,
    streamUrl: form.streamUrl || d.streamUrl,
    imagePath: form.imageUrl || d.imagePath,
    address: form.address || d.address,
    longitude: form.longitude ?? d.longitude,
    latitude: form.latitude ?? d.latitude,
    region: form.regions && form.regions.length ? form.regions[0] : d.region,
    remark: form.remark || d.remark,
    isPublic: form.isPublic ? 1 : 0,
    tag: form.tags && form.tags.length ? form.tags.join(',') : '',
    selectedTags: form.tags && form.tags.length ? form.tags : []
  }

  const res = await updateDevice(id, payload)
  if (res.code === 200) {
    emit('saved')
    emit('close')
  }
}

const openSelectPoint = () => {
  showSelectPoint.value = true
}

const onSelectPointConfirm = (result) => {
  if (!result) return
  form.longitude = result.longitude ?? ''
  form.latitude = result.latitude ?? ''
  if (!form.address) {
    form.address = result.address || ''
  }
  showSelectPoint.value = false
}

const openTagPopup = () => {
  tempTags.value = [...form.tags]
  showTagPopup.value = true
}

const toggleTag = (item) => {
  const index = tempTags.value.indexOf(item)
  if (index === -1) {
    tempTags.value.push(item)
  } else {
    tempTags.value.splice(index, 1)
  }
}

const onTagConfirm = () => {
  form.tags = [...tempTags.value]
  showTagPopup.value = false
}

const onTagCancel = () => {
  showTagPopup.value = false
}

const onBack = () => {
  emit('close')
}
</script>

<style scoped lang="scss">
.edit-device {
  height: 100vh;
  overflow: hidden;
}

.header-save {
  font-size: 16px;
  color: #fff;
  line-height: 1;
}

.edit_content {
  height: calc(100vh - 48px);
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding-left: 16px;
  padding-right: 16px;
  background: #F9FAFF;
}

:deep(.form) {
  background: transparent;
}

.block-title {
  padding:  16px 0 8px;
  line-height: 24px;
  font-weight: 500;
  color: #969799;
  font-size: 12px;
}

.more-title {
  margin-top: 8px;
}

.field-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #999;
}

.section {
  padding: 16px;
  background: #fff;
  margin-top: 8px;
}

.section-header {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-label {
  font-size: 14px;
  color: #999;
}

.section-add{
  width: 24px;
  height: 24px;
}

.section-arrow {
  color: #999;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 4px 8px;
  background: #f0f0f0;
  border-radius: 4px;
  font-size: 12px;
  color: #333;
  display: flex;
  align-items: center;
  column-gap: 6px;
}

.tag-delete {
  width: 14px;
  height: 14px;
}

.switch-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.switch-label {
  font-size: 14px;
  color: #999;
}

.switch-value {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #999;
}

.switch-value span.active {
  color: #2f69f8;
}

.coord-section {
  padding: 16px;
}

.coord-label {
  font-size: 14px;
  color: #999;
  margin-bottom: 8px;
}

.coord-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #333;
}

.coord-split {
  color: #999;
}

.tag-picker-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.tag-picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  font-size: 14px;
}

.tag-picker-title {
  font-weight: 600;
  color: #323233;
}

.tag-picker-cancel,
.tag-picker-confirm {
  color: #1989fa;
}

.tag-popup-content {
  padding: 12px 16px 16px;
}

.tag-popup-content .tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-chip {
  padding: 4px 16px;
  border-radius: 4px;
  background: #f9faff;
  color: #333333;
  font-size: 14px;
}

.tag-chip.active {
  color: #2f69f8;
  background: rgba(47, 105, 248, 0.12);
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

.image-header {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 12px;
}

.image-title-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.common_expressions {
  background-color: #fff;
  border-top: 1px solid #d8d8d8;
  margin-bottom: 16px;
  padding-left: 16px;
  padding-right: 16px;
  padding-bottom: 12px;
}

.required-icon {
  width: 20px;
  height: 20px;
  margin-right: 4px;
}

:deep(.van-field__body) {
  margin-left: 28px;
}
</style>

