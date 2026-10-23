<template>
  <section v-loading="loading" class="square-catalog-table">
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div class="table-tools">
      <el-input :model-value="keyword" :placeholder="$tp('搜索设备...')" clearable @update:model-value="$emit('search', $event)" />
      <el-dropdown @command="exportRows">
        <el-button :disabled="!devices.length">{{ $tp('导出') }}</el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="all">{{ $tp('导出全部') }}</el-dropdown-item>
            <el-dropdown-item command="selected" :disabled="!selectedDevices.length">{{ $tp('导出所选') }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <el-table :data="pageDevices" row-key="id" height="100%" :empty-text="$tp('暂无数据')" @selection-change="selectedKeys = $event.map(row => row.id)">
      <el-table-column type="selection" width="45" reserve-selection />
      <el-table-column type="index" :label="$tp('序号')" width="70" />
      <el-table-column prop="deviceName" :label="$tp('设备名称')" min-width="160" />
      <el-table-column :label="$tp('设备ID')" min-width="210">
        <template #default="{ row }">{{ row.deviceId || row.ipAddress || row.id }}</template>
      </el-table-column>
      <el-table-column :label="$tp('协议')" min-width="130">
        <template #default="{ row }">
          <el-tag v-for="protocol in row.protocols" :key="protocol" size="small">{{ protocolLabel(protocol) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$tp('状态')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ONLINE' ? 'success' : row.status === 'OFFLINE' ? 'info' : 'warning'" size="small">
            {{ $tp(row.status === 'ONLINE' ? '在线' : row.status === 'OFFLINE' ? '离线' : '未知状态') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$tp('地址')" min-width="220">
        <template #default="{ row }">{{ savedVideoDeviceAddress(row) || (videoDeviceCoordinates(row) ? $tp('地图位置') : $tp('未上报位置')) }}</template>
      </el-table-column>
      <el-table-column :label="$tp('公开范围')" min-width="110">
        <template #default="{row}">{{ row.protocols?.includes('VLSTREAM') ? $tp(({PUBLIC:'公开',PRIVATE:'私有'})[row.sources?.find(source=>source.protocol==='VLSTREAM')?.visibility || row.visibility] || '未确认') : '—' }}</template>
      </el-table-column>
      <el-table-column :label="$tp('操作')" width="140" fixed="right">
        <template #default="{ row }"><el-button type="primary" link @click="$emit('play', row)">{{ $tp('播放') }}</el-button><el-button v-if="row.protocols?.includes('VLSTREAM')" type="primary" link @click="$emit('sharing', row)">{{ $tp('共享') }}</el-button></template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="devices.length" :page-sizes="[10, 20, 50]" layout="total, prev, pager, next, sizes" small />
  </section>
</template>

<script setup>
import { VIDEO_PROTOCOLS, videoDeviceCoordinates } from '@/utils/videoSquareCatalog'
import { savedVideoDeviceAddress } from '@/utils/videoDeviceAddress.mjs'
import { computed, ref, watch } from 'vue'
import { videoCatalogCsv } from '@/utils/videoSquareExport.mjs'
import { translatePhrase } from '@/i18n'
const props = defineProps({ devices: { type: Array, default: () => [] }, loading: Boolean, error: { type: String, default: '' }, keyword: { type: String, default: '' } })
defineEmits(['play', 'search', 'sharing'])
const currentPage = ref(1)
const pageSize = ref(10)
const selectedKeys = ref([])
const selectedDevices = computed(() => props.devices.filter(device => selectedKeys.value.includes(device.id)))
const pageDevices = computed(() => props.devices.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value))
watch([() => props.keyword, () => props.devices.length, pageSize], () => { currentPage.value = 1 })
const protocolLabel = value => VIDEO_PROTOCOLS.find(protocol => protocol.value === value)?.label || value
function exportRows(mode) {
  const devices = mode === 'selected' ? selectedDevices.value : props.devices
  if (!devices.length) return
  const url = URL.createObjectURL(new Blob([videoCatalogCsv(devices, translatePhrase)], { type: 'text/csv;charset=utf-8;' }))
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `video-devices-${new Date().toISOString().slice(0, 10)}.csv`
  anchor.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
</script>

<style scoped>
.square-catalog-table { flex: 1; min-height: 0; min-width: 0; padding: 20px; display: flex; flex-direction: column; gap: 12px; }
.square-catalog-table :deep(th.el-table__cell) { background: #f7f8fa; color: #606775; font-weight: 400; }
.square-catalog-table :deep(td.el-table__cell) { padding-block: 16px; }
.square-catalog-table :deep(.el-tag) { margin-inline-end: 4px; }
.table-tools { display: flex; justify-content: flex-end; gap: 20px; }
.table-tools .el-input { width: min(340px, 100%); }
.square-catalog-table .el-pagination { align-self: flex-end; max-width: 100%; overflow-x: auto; flex-shrink: 0; }
</style>
