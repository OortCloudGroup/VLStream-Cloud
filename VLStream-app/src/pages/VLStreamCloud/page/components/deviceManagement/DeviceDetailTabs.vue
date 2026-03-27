<template>
  <div class="device-detail">
    <nav-header-banner title="设备详情" :is-call-back="true" @call-back="onBack">
      <div class="header-actions">
        <span class="action edit" @click="onEdit">编辑</span>
        <span class="action delete" @click="onDelete">删除</span>
      </div>
    </nav-header-banner>

    <div class="detail-body">
      <img class="hero" :src="heroImg" alt="" />

      <div class="tab-header">
        <div
          class="tab-item"
          :class="{ active: activeTab === 'ptz' }"
          @click="activeTab = 'ptz'"
        >
          PTZ
          <div v-if="activeTab === 'ptz'" class="tab-underline" />
        </div>
        <div
          class="tab-item"
          :class="{ active: activeTab === 'info' }"
          @click="activeTab = 'info'"
        >
          设备信息
          <div v-if="activeTab === 'info'" class="tab-underline" />
        </div>
      </div>

      <div class="tab-body">
        <PTZPanel v-if="activeTab === 'ptz'" />
        <DeviceInfoPanel v-else :device="device" />
      </div>
    </div>

    <oort-popup v-model="showEdit" position="right" style="width: 100%;height: 100%;" teleport="body">
      <EditDevice
        v-if="showEdit"
        :device-id="deviceId"
        @saved="handleSaved"
        @close="showEdit = false"
      />
    </oort-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import OortPopup from '@/components/popup/oort_popup.vue'
import NavHeaderBanner from '@/components/navHeaderBanner/index.vue'
import PTZPanel from './PTZPanel.vue'
import DeviceInfoPanel from './DeviceInfoPanel.vue'
import EditDevice from './EditDevice.vue'
import { getDeviceDetail, removeDevice } from '@/api/VLStreamCloud/device'
import { useUserStore } from '@/store/modules/useraPaas'

const props = defineProps({
  deviceId: {
    type: [Number, String],
    required: true
  }
})

const store = useUserStore()
const activeTab = ref('ptz')
const showEdit = ref(false)
const device = ref({})

const heroImg = computed(() => {
  const d = device.value || {}
  return d.imagePath || ''
})

const onEdit = () => {
  showEdit.value = true
}

const emit = defineEmits(['close', 'deleted'])

const onBack = () => {
  emit('close')
}

const onDelete = () => {
  showConfirmDialog({
    title: '删除设备',
    message: '确定要删除该设备吗？',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
    .then(async() => {
      const params = {
        accessToken: store.userInfo?.accessToken,
        ids: [props.deviceId]
      }
      const res = await removeDevice(params)
      if (res.code === 200) {
        showToast('删除成功')
        emit('deleted')
        emit('close')
      }
    })
    .catch(() => {})
}

const fetchDeviceDetail = async(id) => {
  if (!id) return
  const params = {
    accessToken: store.userInfo?.accessToken,
    id: id
  }
  const res = await getDeviceDetail(params)
  if (res.code === 200 && res.data) {
    device.value = res.data
  }
}

const handleSaved = () => {
  fetchDeviceDetail(props.deviceId)
}

onMounted(() => {
  fetchDeviceDetail(props.deviceId)
})

watch(
  () => props.deviceId,
  (val) => {
    fetchDeviceDetail(val)
  }
)
</script>

<style scoped lang="scss">
.device-detail {
  height: 100vh;
  background: #f6f8ff;
  display: flex;
  flex-direction: column;
}

.detail-body {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;

  .action {
    font-size: 16px;
    color: #fff;
    line-height: 1;

    &.delete {
      color: #fff;
    }
  }
}

.hero {
  width: 100%;
  height: 188px;
}

.tab-header {
  height: 48px;
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 0 16px;
  background: #fff;
}

.tab-item {
  position: relative;
  color:#333333;
  font-size:14px;
  padding-bottom: 6px;
  cursor: pointer;

  &.active {
    color:#2f69f8;
    font-weight:500;
  }
}

.tab-underline {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 0;
  width: 32px;
  height: 3px;
  border-radius: 999px;
  background: #1c6cff;
}

.tab-body {
  flex: 1;
  background: #f6f8ff;
  padding: 12px 12px 24px;
}
</style>
