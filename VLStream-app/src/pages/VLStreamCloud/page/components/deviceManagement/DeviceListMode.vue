<template>
  <div class="list-wrap">
    <div class="table-card">
      <div class="table-head">
        <div class="col name">
          设备名称
        </div>
        <div class="col id">
          设备ID
        </div>
        <div class="col">
          状态
        </div>
      </div>

      <div class="table-body">
        <div
          v-for="row in rows"
          :key="row.id"
          class="table-row"
          @click="openDetail(row)"
        >
          <div class="col">
            {{ row.deviceName }}
          </div>
          <div class="col">
            {{ row.deviceId }}
          </div>
          <div class="col">
            <van-tag
              v-if="row.status == '1'"
              type="success"
              plain
              class="status-tag online"
            >
              在线
            </van-tag>
            <van-tag v-else type="default" plain class="status-tag offline">
              离线
            </van-tag>
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
import DeviceDetailTabs from '../deviceManagement/DeviceDetailTabs.vue'
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

const showDetail = ref(false)
const currentRow = ref({})

const openDetail = (row) => {
  currentRow.value = row
  showDetail.value = true
}

const onAdd = () => {
}

const handleDeleted = () => {
  showDetail.value = false
  fetchDevices()
}

</script>

<style lang="scss" scoped>
.list-wrap {
  position: relative;
}

.table-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.04);
  margin-top: 16px;
}

.table-head {
  display: grid;
  grid-template-columns: 1.2fr 1fr 0.8fr;
  padding: 10px 12px;
  background:#fafafa;
  line-height:18px;
  color:#333333;
  font-size:14px;
}

.table-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr 0.8fr;
  padding: 20px 12px;
  border-top: 1px solid rgba(235, 237, 240, 1);
  color:#333333;
  font-size:14px;
  .col{
    display: flex;
    align-items: center;
  }
}

.status-tag {
  border-radius:4px;
  padding: 0 6px;
  font-size: 12px;
  line-height:18px;
}

.status-tag.online {
  color: rgba(0, 186, 82, 1);
  border-color: rgba(0, 186, 82, 0.15);
  background: rgba(0, 186, 82, 0.08);
}

.status-tag.offline {
  color: rgba(153, 153, 153, 1);
  border-color: rgba(153, 153, 153, 0.15);
  background: rgba(153, 153, 153, 0.08);
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

