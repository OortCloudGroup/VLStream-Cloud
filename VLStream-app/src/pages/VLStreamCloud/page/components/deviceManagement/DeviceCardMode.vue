<template>
  <div class="card-wrap">
    <div class="card-list">
      <div v-for="row in rows" :key="row.id" class="device-card" @click="openDetail(row)">
        <div class="card-header">
          <div class="name">
            {{ row.deviceName }}
          </div>
        </div>

        <div class="info-row">
          <img src="@/assets/img/VLStreamCloud/spgc/sbId.png" alt="" />
          <div class="label">
            设备ID
          </div>
          <div class="value">
            {{ row.deviceId }}
          </div>
        </div>
        <div class="info-row">
          <img src="@/assets/img/VLStreamCloud/spgc/sbwz.png" alt="" />
          <div class="label">
            设备位置
          </div>
          <div class="value">
            {{ row.address }}
          </div>
        </div>

        <div class="card-footer">
          <div class="chips">
            <div class="chip" :class="{ offline: row.status != '1' }">
              {{ row.status == '1' ? '在线' : '离线' }}
            </div>
            <div v-if="row.deviceType" class="chip">
              {{ row.deviceType }}
            </div>
            <div v-if="row.heightPosition" class="chip">
              {{ row.heightPosition }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="fab" @click="onAdd">
      <span class="fab-plus">+</span>
    </div>

    <oort-popup v-model="showDetail" position="right" style="width: 100%;height: 100%;" teleport="body">
      <DeviceDetailTabs
        v-if="showDetail"
        :device-id="currentRow.id"
        @deleted="handleDeleted"
        @close="showDetail = false"
      />
    </oort-popup>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import OortPopup from '@/components/popup/oort_popup.vue'
import DeviceDetailTabs from '@/pages/VLStreamCloud/page/components/deviceManagement/DeviceDetailTabs.vue'
import { getDeviceList } from '@/api/VLStreamCloud/device'
import { useUserStore } from '@/store/modules/useraPaas'

const props = defineProps({
  tagName: {
    type: String,
    default: null
  }
})

const store = useUserStore()

const rows = ref([])

const fetchDevices = async() => {
  const params = {
    accessToken: store.userInfo?.accessToken
  }
  // 如果传入了 tagName，则添加到请求参数中
  if (props.tagName) {
    params.tag = props.tagName
  }
  const res = await getDeviceList(params)
  if (res.code === 200 && res.data && Array.isArray(res.data.records)) {
    rows.value = res.data.records
  }
}

onMounted(() => {
  fetchDevices()
})

// 监听 tagName 变化，重新获取设备列表
watch(
  () => props.tagName,
  () => {
    fetchDevices()
  }
)

const onAdd = () => {
  // eslint-disable-next-line no-console
  console.log('add device card')
}

const showDetail = ref(false)
const currentRow = ref({})

const openDetail = (row) => {
  currentRow.value = row
  showDetail.value = true
}

const handleDeleted = () => {
  showDetail.value = false
  fetchDevices()
}
</script>

<style lang="scss" scoped>
.card-wrap {
  position: relative;
}

.card-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.device-card {
  position: relative;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.04);
}

.card-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  gap: 10px;
}

.name {
  line-height: 25px;
  font-weight: 700;
  color: #333333;
  font-size: 16px;
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
  min-width: 80px;
  color: #7a7c85;
}

.value {
  flex: 1;
  color: #333333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.card-footer {
  display: flex;
  align-items: center;
  margin-top: 10px;
}

.chips {
  display: flex;
  gap: 8px;
}

.chip {
  background: rgba(47, 105, 248, 1);
  border-radius: 2px;
  line-height: 16px;
  color: #ffffff;
  font-size: 12px;
  padding: 4px 12px;
}

.chip.offline {
  background: rgba(216, 216, 216, 1);
  color: #ffffff;
}

.fab {
  position: fixed;
  right: 18px;
  bottom: 92px;
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: #2f69f8;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 24px rgba(47, 105, 248, 0.35);
  z-index: 20;
}

.fab-plus {
  font-size: 34px;
  line-height: 34px;
  color: #fff;
  font-weight: 500;
}
</style>
