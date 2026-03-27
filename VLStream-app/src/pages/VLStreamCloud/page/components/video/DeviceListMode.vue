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
        <div class="col status">
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
          <div class="col name">
            {{ row.name }}
          </div>
          <div class="col id">
            {{ row.deviceId }}
          </div>
          <div class="col status">
            <van-tag
              v-if="row.status === 'online'"
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

    <oort-popup v-model="showDetail" position="right" style="width: 100%;height: 100%;">
      <DeviceDetailTabs
        v-if="showDetail"
        :device="currentRow"
        @edit="openEdit"
      />
    </oort-popup>

    <oort-popup v-model="showEdit" position="right" style="width: 100%;height: 100%;">
      <EditDevice v-if="showEdit" :device="currentRow" @close="showEdit = false" />
    </oort-popup>

    <oort-popup v-model="showConfig" position="right" style="width: 100%;height: 100%;">
      <ConfigRecord v-if="showConfig" @close="showConfig = false" />
    </oort-popup>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import OortPopup from '@/components/popup/oort_popup.vue'
import EditDevice from './EditDevice.vue'
import ConfigRecord from './ConfigRecord.vue'
import DeviceDetailTabs from './DeviceDetailTabs.vue'

const rows = ref([
  { id: 1, name: '海康云台', deviceId: '1354841', status: 'online' },
  { id: 2, name: '海康云台', deviceId: '1354841', status: 'offline' },
  { id: 3, name: '海康云台', deviceId: '1354841', status: 'online' },
  { id: 4, name: '海康云台', deviceId: '1354841', status: 'online' },
  { id: 5, name: '海康云台', deviceId: '1354841', status: 'online' },
  { id: 6, name: '海康云台', deviceId: '1354841', status: 'online' },
  { id: 7, name: '海康云台', deviceId: '1354841', status: 'online' },
  { id: 8, name: '海康云台', deviceId: '1354841', status: 'online' }
])

const showDetail = ref(false)
const showEdit = ref(false)
const currentRow = ref({})
const showConfig = ref(false)

const openDetail = (row) => {
  currentRow.value = row
  showDetail.value = true
}

const openEdit = (row) => {
  currentRow.value = row
  showEdit.value = true
}

const onAdd = () => {
  showConfig.value = true
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
}

.table-head {
  display: grid;
  grid-template-columns: 1.2fr 1fr 0.8fr;
  padding: 16px 18px;
  background: rgba(250, 250, 250, 1);
  font-size: 16px;
  font-weight: 600;
  color: rgba(51, 51, 51, 0.65);
}

.table-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr 0.8fr;
  padding: 22px 18px;
  border-top: 1px solid rgba(235, 237, 240, 1);
  font-size: 18px;
  color: #222;
}

.col.status {
  display: flex;
  justify-content: flex-end;
}

.status-tag {
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 14px;
  font-weight: 600;
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

