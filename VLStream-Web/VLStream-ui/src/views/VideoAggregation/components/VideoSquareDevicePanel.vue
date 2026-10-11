<template>
  <aside v-loading="loading" class="device-list-panel square-device-panel">
    <div class="device-list-header">
      <strong class="device-list-title">{{ $tp('设备列表') }}</strong>
      <div class="device-list-actions">
        <button class="device-action-btn" @click="$emit('settings')" :title="$tp('设置')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
            <path d="M12,15.5A3.5,3.5 0 0,1 8.5,12A3.5,3.5 0 0,1 12,8.5A3.5,3.5 0 0,1 15.5,12A3.5,3.5 0 0,1 12,15.5M19.43,12.97C19.47,12.65 19.5,12.33 19.5,12C19.5,11.67 19.47,11.34 19.43,11.03L21.54,9.37C21.73,9.22 21.78,8.95 21.66,8.73L19.66,5.27C19.54,5.05 19.27,4.96 19.05,5.05L16.56,6.05C16.04,5.66 15.5,5.32 14.87,5.07L14.5,2.42C14.46,2.18 14.25,2 14,2H10C9.75,2 9.54,2.18 9.5,2.42L9.13,5.07C8.5,5.32 7.96,5.66 7.44,6.05L4.95,5.05C4.73,4.96 4.46,5.05 4.34,5.27L2.34,8.73C2.22,8.95 2.27,9.22 2.46,9.37L4.57,11.03C4.53,11.34 4.5,11.67 4.5,12C4.5,12.33 4.53,12.65 4.57,12.97L2.46,14.63C2.27,14.78 2.22,15.05 2.34,15.27L4.34,18.73C4.46,18.95 4.73,19.03 4.95,18.95L7.44,17.94C7.96,18.34 8.5,18.68 9.13,18.93L9.5,21.58C9.54,21.82 9.75,22 10,22H14C14.25,22 14.46,21.82 14.5,21.58L14.87,18.93C15.5,18.68 16.04,18.34 16.56,17.94L19.05,18.95C19.27,19.03 19.54,18.95 19.66,18.73L21.66,15.27C21.78,15.05 21.73,14.78 21.54,14.63L19.43,12.97Z"/>
          </svg>
        </button>
        <button class="device-action-btn" @click="toggleTreeView" :class="{ active: treeView }" :title="$tp('树形结构')">
          <img src="@/assets/组织-选中@2x.png" :alt="$tp('树形结构')" width="16" height="16">
        </button>
        <button class="device-action-btn" @click="toggleListView" :class="{ active: !treeView }" :title="$tp('列表结构')">
          <img src="@/assets/人员列表-未选中@2x.png" :alt="$tp('列表结构')" width="16" height="16">
        </button>
      </div>
    </div>
    <el-alert v-if="error" type="error" :title="$tp(error)" :closable="false" />
    <div class="square-filters">
      <el-select :model-value="selectedProtocols" multiple collapse-tags :placeholder="$tp('全部')"
        @update:model-value="$emit('protocols-change', $event)">
        <el-option v-for="protocol in availableProtocols" :key="protocol.value" :value="protocol.value" :label="$tp(protocol.label)" />
      </el-select>
      <el-select :model-value="selectedStatus" :placeholder="$tp('全部')" clearable
        @update:model-value="$emit('status-change', $event)">
        <el-option v-for="status in statuses" :key="status.value" :value="status.value" :label="$tp(status.label)" />
      </el-select>
      <el-input :model-value="keyword" :placeholder="$tp('搜索设备...')" clearable
        @update:model-value="$emit('search', $event)" />
      <div class="square-location-summary">{{ $tp('地图位置') }}: {{ locatedCount }} / {{ devices.length }}</div>
    </div>
    <div class="device-list-items">
      <el-empty v-if="!visibleDevices.length && !loading" :description="$tp('暂无数据')" :image-size="55" />
      <template v-for="group in groups" :key="group.id">
        <button v-if="treeView" class="tree-group-header" @click="toggleGroup(group.id)">
          <span>{{ collapsed.has(group.id) ? '▸' : '▾' }} {{ $tp(group.label) }}</span>
          <span>({{ group.children.length }})</span>
        </button>
        <template v-if="!treeView || !collapsed.has(group.id)">
          <button v-for="device in group.children" :key="device.id" class="square-device-item"
            @click="$emit('device-click', device)">
            <span class="device-name" data-i18n-ignore>{{ device.deviceName }}</span>
            <small data-i18n-ignore>{{ device.deviceId || device.ipAddress || device.id }}</small>
            <div class="device-details">
              <el-tag v-for="protocol in device.protocols" :key="protocol" size="small">{{ $tp(protocolLabel(protocol)) }}</el-tag>
              <span class="device-status" :class="device.status.toLowerCase()">{{ $tp(statusLabel(device.status)) }}</span>
            </div>
            <small v-if="!videoDeviceCoordinates(device)">{{ $tp('未上报位置') }}</small>
          </button>
        </template>
      </template>
    </div>
  </aside>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { translatePhrase } from '@/i18n'
import { VIDEO_PROTOCOLS, videoDeviceCoordinates, videoDeviceGroups } from '@/utils/videoSquareCatalog'

const props = defineProps({
  devices: { type: Array, default: () => [] },
  visibleDevices: { type: Array, default: () => [] },
  protocols: { type: Array, default: () => [] },
  selectedProtocols: { type: Array, default: () => [] },
  selectedStatus: { type: String, default: '' },
  keyword: { type: String, default: '' },
  loading: Boolean,
  error: { type: String, default: '' },
})
defineEmits(['device-click', 'settings', 'search', 'protocols-change', 'status-change'])
const treeView = ref(false)
const toggleTreeView = () => { treeView.value = true; ElMessage.success(translatePhrase('已切换到树形结构')) }
const toggleListView = () => { treeView.value = false; ElMessage.success(translatePhrase('已切换到列表结构')) }
const collapsed = ref(new Set())
const groups = computed(() => videoDeviceGroups(props.visibleDevices))
const availableProtocols = computed(() => VIDEO_PROTOCOLS.filter(protocol => props.protocols.includes(protocol.value)))
const locatedCount = computed(() => props.devices.filter(videoDeviceCoordinates).length)
const statuses = [
  { value: 'ONLINE', label: '在线' }, { value: 'OFFLINE', label: '离线' }, { value: 'UNKNOWN', label: '未知状态' },
]
const protocolLabel = value => VIDEO_PROTOCOLS.find(protocol => protocol.value === value)?.label || value
const statusLabel = value => statuses.find(status => status.value === value)?.label || '未知状态'
function toggleGroup(id) {
  if (collapsed.value.has(id)) collapsed.value.delete(id)
  else collapsed.value.add(id)
}
</script>

<style scoped>
.square-device-panel {
  position: fixed;
  top: 130px;
  right: 50px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  width: 300px;
  height: 600px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 1001;
}
.device-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(248, 249, 250, 0.8);
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}
.device-list-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.device-list-actions {
  display: flex;
  gap: 8px;
}
.device-action-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(0, 0, 0, 0.7);
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  transition: all 0.2s ease;
  padding: 2px;
}
.device-action-btn:hover {
  background: rgba(0, 0, 0, 0.9);
}
.device-action-btn.active {
  background: rgba(24, 144, 255, 0.8);
}
.square-filters { padding: 12px 18px; display: grid; gap: 10px; }
.square-filters .el-select { width: 100%; }
.square-location-summary { font-size: 12px; color: var(--el-text-color-secondary); }
.device-list-items { overflow: auto; flex: 1; padding: 0 16px 16px; }
.tree-group-header { display: flex; justify-content: space-between; align-items: center; width: 100%; padding: 14px 6px; color: inherit; background: transparent; border: 0; font-size: 15px; font-weight: 600; cursor: pointer; }
.square-device-item { text-align: start; width: 100%; padding: 12px; display: grid; gap: 5px; color: inherit; background: transparent; border: 1px solid var(--el-border-color-lighter); border-radius: 8px; margin-bottom: 8px; cursor: pointer; }
.square-device-item:hover { border-color: var(--el-color-primary); }
.device-name { font-weight: 600; overflow-wrap: anywhere; }
.square-device-item small { color: var(--el-text-color-secondary); overflow-wrap: anywhere; }
.device-details { display: flex; flex-wrap: wrap; gap: 5px; align-items: center; }
.device-status { margin-inline-start: auto; font-size: 12px; }
.online { color: #269528; } .offline { color: #c33; } .unknown { color: #996800; }
</style>
