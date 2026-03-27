<template>
  <div>
    <div class="device-info">
      <div class="card header-card">
        <div class="title-row">
          <span class="device-name">{{ device.deviceName || '' }}</span>
          <span class="privacy-tag">
            {{ privacyText }}
          </span>
        </div>
      </div>

      <div class="card info-card">
        <div
          v-for="item in infoList"
          :key="item.label"
          class="info-row"
        >
          <div class="info-label">
            <img :src="item.icon" alt="" class="info-icon" />
            <span>{{ item.label }}</span>
          </div>
          <div class="info-value">
            {{ item.value }}
          </div>
        </div>
      </div>
    </div>
    <div class="bottom-actions">
      <div class="bottom-action-item" @click="openConfigRecord">
        配置录像
      </div>
      <div class="bottom-action-item" @click="openSettingAi">
        配置AI算法
      </div>
      <div class="bottom-action-item" @click="openSettingCamera">
        设置摄像机
      </div>
    </div>

    <oort-popup v-model="showSettingCamera" position="right" style="width: 100%;height: 100%;">
      <settingCamera v-if="showSettingCamera" :device="device" @close="showSettingCamera = false" />
    </oort-popup>
    <oort-popup v-model="showSettingAi" position="right" style="width: 100%;height: 100%;">
      <settingAi v-if="showSettingAi" :device-id="device.deviceId" @close="showSettingAi = false" />
    </oort-popup>

    <oort-popup v-model="showConfigRecord" position="right" style="width: 100%;height: 100%;">
      <ConfigRecord
        v-if="showConfigRecord"
        :device-id="device.deviceId"
        @close="showConfigRecord = false"
      />
    </oort-popup>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import OortPopup from '@/components/popup/oort_popup.vue'
import settingCamera from '@/pages/VLStreamCloud/page/components/deviceManagement/settingCamera.vue'
import settingAi from '@/pages/VLStreamCloud/page/components/deviceManagement/settingAi.vue'
import ConfigRecord from '@/pages/VLStreamCloud/page/components/deviceManagement/ConfigRecord.vue'
import bz from '@/assets/img/VLStreamCloud/sbgl/bz.png'
import cjsj from '@/assets/img/VLStreamCloud/sbgl/cjsj.png'
import lj from '@/assets/img/VLStreamCloud/sbgl/lj.png'
import sbbq from '@/assets/img/VLStreamCloud/sbgl/sbbq.png'
import sbid from '@/assets/img/VLStreamCloud/sbgl/sbid.png'
import sbqh from '@/assets/img/VLStreamCloud/sbgl/sbqh.png'
import sbwz from '@/assets/img/VLStreamCloud/sbgl/sbwz.png'
import sbzb from '@/assets/img/VLStreamCloud/sbgl/sbzb.png'
import yysf from '@/assets/img/VLStreamCloud/sbgl/yysf.png'

const props = defineProps({
  device: {
    type: Object,
    default: () => ({})
  }
})

const device = computed(() => props.device || {})

const privacyText = computed(() => {
  const d = device.value || {}
  return d.isPublic === 1 ? '公开' : '私有'
})

const infoList = computed(() => {
  const d = device.value || {}
  const path = d.streamUrl || ''
  const coord =
      d.longitude != null && d.latitude != null
        ? `${d.longitude}，${d.latitude}`
        : ''

  return [
    { label: '路径', value: path || '--', icon: lj },
    { label: '设备ID', value: d.deviceId || '--', icon: sbid },
    { label: '设备坐标', value: coord || '--', icon: sbzb },
    { label: '设备类型', value: d.deviceType || '--', icon: sbbq },
    { label: '高度位置', value: d.heightPosition || '--', icon: sbwz },
    { label: '设备区划', value: d.region || '--', icon: sbqh },
    { label: '设备位置', value: d.address || '--', icon: sbwz },
    { label: '拥有算法', value: d.algorithmName || '--', icon: yysf },
    { label: '设备标签', value: d.tag || '--', icon: sbbq },
    { label: '备注', value: d.remark || '--', icon: bz },
    { label: '创建时间', value: d.createTime || '--', icon: cjsj }
  ]
})

const showSettingCamera = ref(false)
const showSettingAi = ref(false)
const showConfigRecord = ref(false)

const openSettingCamera = () => {
  showSettingCamera.value = true
}
const openSettingAi = () => {
  showSettingAi.value = true
}

const openConfigRecord = () => {
  showConfigRecord.value = true
}
</script>

<style scoped lang="scss">
.device-info {
  position: relative;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-radius: 8px;
  padding: 12px 16px 16px;
  margin-top: 16px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.04);
}

.card {
  padding: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 700;
  color: #333333;
}

.device-name {
  line-height: 24px;
}

.privacy-tag {
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  background:rgba(255, 141, 34, 0.12);
  border-radius: 4px;
  line-height: 18px;
  color:#ff8d22;
  font-size:12px;
  padding: 0 6px;
  font-weight: 400;
}

.info-card {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin-top: 4px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  margin-top: 8px;
  font-size: 14px;
}

.info-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  line-height: 22px;
  color: #7a7c85;
  font-size: 14px;
  min-width: 90px;
}

.info-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.info-value {
  flex: 1;
  line-height: 22px;
  font-size: 14px;
  color: #333333;
  white-space: normal;
  word-break: break-all;
  overflow-wrap: anywhere;
}

.bottom-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 16px;
  .bottom-action-item {
    background: rgba(216, 216, 216, 0);
    border: 1px solid;
    border-color: #1a53ff;
    border-radius: 20px;
    padding: 10px 20px;
    color: #1a53ff;
    font-size: 14px;
  }
}
</style>
