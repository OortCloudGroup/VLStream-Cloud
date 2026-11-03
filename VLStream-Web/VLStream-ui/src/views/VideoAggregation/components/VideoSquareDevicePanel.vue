<template>
  <aside v-loading="loading" class="device-list-panel square-device-panel">
    <slot name="views" />
    <div class="device-list-header">
      <strong class="device-list-title">{{ $tp('数据展示') }}</strong>
      <span class="header-rule" aria-hidden="true"></span>
      <div class="device-list-actions">
        <button class="protocol-collapse" type="button" :aria-expanded="!protocolsCollapsed" @click="protocolsCollapsed = !protocolsCollapsed">
          {{ $tp(protocolsCollapsed ? '展开' : '收起') }}
          <img :src="collapseIcon" alt="" :class="{ 'is-collapsed': protocolsCollapsed }" />
        </button>
        <el-popover trigger="click" placement="bottom-end" :width="240">
          <template #reference><button class="device-action-btn" :aria-label="$tp('设置')"><img :src="settingsIcon" width="20" height="20" alt="" /></button></template>
          <div class="catalog-settings">
            <el-select :model-value="selectedStatus" :placeholder="$tp('全部')" clearable @update:model-value="$emit('status-change', $event)">
              <el-option v-for="status in statuses" :key="status.value" :value="status.value" :label="$tp(status.label)" />
            </el-select>
            <el-button @click="toggleTreeView">{{ $tp('树形结构') }}</el-button>
            <el-button @click="toggleListView">{{ $tp('列表结构') }}</el-button>
            <el-button @click="$emit('settings')">{{ $tp('显示设置') }}</el-button>
            <small>{{ $tp('地图位置') }}: {{ locatedCount }} / {{ devices.length }}</small>
          </div>
        </el-popover>
      </div>
    </div>
    <VideoSquareClassificationTree v-if="classificationEnabled" :devices="classificationDevices || devices" :visible-devices="visibleDevices" :selected-device-id="selectedId" filter-empty :show-tree="treeView" @filter-change="$emit('classification-change', $event)" @device-click="$emit('device-click', $event)" />
    <el-alert v-if="error" type="error" :title="$tp(error)" :closable="false" />
    <div class="square-filters">
      <el-checkbox-group v-show="!protocolsCollapsed" class="square-protocol-options" :model-value="selectedProtocols" @update:model-value="$emit('protocols-change', $event)">
        <div v-for="protocol in availableProtocols" :key="protocol.value" class="protocol-option">
          <el-checkbox :label="protocol.value">{{ $tp(protocol.label) }}{{ $tp('协议') }}</el-checkbox>
          <el-dropdown trigger="click" @command="value => $emit('protocols-change', value === 'all' ? availableProtocols.map(item => item.value) : [protocol.value])">
            <button class="protocol-menu" :aria-label="$tp(protocol.label) + $tp('协议')"><img :src="protocolArrowIcon" alt="" /></button>
            <template #dropdown><el-dropdown-menu><el-dropdown-item command="only">{{ $tp('仅显示此协议') }}</el-dropdown-item><el-dropdown-item command="all">{{ $tp('全部') }}</el-dropdown-item></el-dropdown-menu></template>
          </el-dropdown>
        </div>
      </el-checkbox-group>
      <el-input :model-value="keyword" :placeholder="$tp('搜索')" clearable @update:model-value="$emit('search', $event)">
        <template #prefix><svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="10" cy="10" r="6" stroke="currentColor"/><path d="m15 15 5 5" stroke="currentColor"/></svg></template>
      </el-input>
    </div>
    <div v-if="!classificationEnabled || !treeView" class="device-list-items">
      <el-empty v-if="!visibleDevices.length && !loading" :description="$tp('暂无数据')" :image-size="55" />
      <template v-for="group in groups" :key="group.id">
        <button v-if="treeView" class="tree-group-header" @click="toggleGroup(group.id)">
          <span class="device-group-label"><span class="tree-caret">{{ collapsed.has(group.id) ? '▸' : '▾' }}</span><img :src="folderIcon" width="18" height="18" alt="" />{{ $tp(group.label) }}</span><span>{{ group.children.length }}</span>
        </button>
        <template v-if="!treeView || !collapsed.has(group.id)">
          <button v-for="device in group.children" :key="device.id" class="square-device-item" :class="{ 'is-selected': device.id === selectedId }" :title="device.deviceName + ' · ' + $tp(statusLabel(device.status))" @click="$emit('device-click', device)">
            <img :src="cameraIcon" width="18" height="18" alt="" />
            <span class="device-name" data-i18n-ignore>{{ device.deviceName }}</span>
            <span class="device-state" :class="device.status.toLowerCase()">{{ $tp(statusLabel(device.status)) }}</span>
          </button>
        </template>
      </template>
    </div>
  </aside>
</template>
<script setup>
import { computed, ref } from 'vue'
import settingsIcon from '@/assets/mastergo/video-square/panel-settings.svg'
import collapseIcon from '@/assets/mastergo/video-square/chevron-up.svg'
import protocolArrowIcon from '@/assets/mastergo/video-square/protocol-arrow.svg'
import folderIcon from '@/assets/mastergo/video-square/folder.svg'
import cameraIcon from '@/assets/mastergo/video-square/device-camera.svg'
import { ElMessage } from 'element-plus'
import { translatePhrase } from '@/i18n'
import { VIDEO_PROTOCOLS, videoDeviceCoordinates, videoDeviceGroups, sortVideoDevices } from '@/utils/videoSquareCatalog'
import CustomAvailabilityBadge from '@/components/CustomAvailabilityBadge.vue'
import { savedVideoDeviceAddress } from '@/utils/videoDeviceAddress.mjs'
import VideoSquareClassificationTree from './VideoSquareClassificationTree.vue'

const props = defineProps({
  selectedId: { type: [String, Number], default: '' },
  now: { type: Number, default: () => Date.now() },
  devices: { type: Array, default: () => [] },
  classificationDevices: { type: Array, default: undefined },
  classificationEnabled: { type: Boolean, default: true },
  visibleDevices: { type: Array, default: () => [] },
  protocols: { type: Array, default: () => [] },
  selectedProtocols: { type: Array, default: () => [] },
  selectedStatus: { type: String, default: '' },
  keyword: { type: String, default: '' },
  loading: Boolean,
  error: { type: String, default: '' },
})
defineEmits(['device-click', 'settings', 'search', 'protocols-change', 'status-change', 'classification-change'])
const treeView = ref(true)
const protocolsCollapsed = ref(false)
const toggleTreeView = () => { treeView.value = true; ElMessage.success(translatePhrase('已切换到树形结构')) }
const toggleListView = () => { treeView.value = false; ElMessage.success(translatePhrase('已切换到列表结构')) }
const collapsed = ref(new Set())
const groups = computed(() => treeView.value ? videoDeviceGroups(props.visibleDevices) :
  [{ id: 'list', children: sortVideoDevices(props.visibleDevices) }])
const availableProtocols = computed(() => VIDEO_PROTOCOLS.filter(protocol => props.protocols.includes(protocol.value)))
const locatedCount = computed(() => props.devices.filter(videoDeviceCoordinates).length)
const statuses = [
  { value: 'ONLINE', label: '在线' }, { value: 'OFFLINE', label: '离线' }, { value: 'UNKNOWN', label: '未知状态' },
]
const protocolLabel = value => VIDEO_PROTOCOLS.find(protocol => protocol.value === value)?.label || value
const statusLabel = value => statuses.find(status => status.value === value)?.label || '未知状态'
const unknownReason = device => device.protocols?.includes('DAHUA')
  ? '暂无可靠的在线状态上报' : '暂无有效的在线探测证据'
function toggleGroup(id) {
  if (collapsed.value.has(id)) collapsed.value.delete(id)
  else collapsed.value.add(id)
}
</script>

<style scoped>
.saved-device-address { white-space: normal; overflow-wrap: anywhere; }
.protocol-collapse { padding: 0; border: 0; background: transparent; color: #2278ff; font-size: 12px; cursor: pointer; }
.square-protocol-options { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.square-protocol-options .el-checkbox { margin-right: 0; }
/* MasterGo 257:28228 — 面板列 340 = 20 + 300 + 20，分隔线 #E8E8E8 */
.square-device-panel {
  position: relative;
  order: -1;
  background: #fff;
  border-inline-end: 1px solid #e8e8e8;
  width: 340px;
  flex: 0 0 340px;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 2;
}
.device-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
  gap: 8px;
  flex-wrap: wrap;
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
/* MasterGo 309:33869 — 搜索框 r4 + 1px #C6CBD4，占位文字 12px #808695 */
.square-filters { padding: 12px 20px; display: grid; gap: 10px; }
.square-filters .el-select { width: 100%; }
.square-filters :deep(.el-input__wrapper) { border-radius: 4px; box-shadow: 0 0 0 1px #c6cbd4 inset; }
.square-filters :deep(.el-input__inner::placeholder) { color: #808695; font-size: 12px; }
.square-location-summary { font-size: 12px; color: var(--el-text-color-secondary); }
.device-list-items { overflow: auto; flex: 1; padding: 0 20px 20px; }
.tree-group-header { display: flex; justify-content: space-between; align-items: center; width: 100%; padding: 14px 6px; color: inherit; background: transparent; border: 0; font-size: 15px; font-weight: 600; cursor: pointer; }
.square-device-item { text-align: start; width: 100%; padding: 12px 8px; display: grid; gap: 5px; color: inherit; background: transparent; border: 0; border-bottom: 1px solid #eef0f4; border-radius: 0; cursor: pointer; }
.square-device-item:hover { background: #e3eeff; }
.device-name { font-weight: 600; overflow-wrap: anywhere; }
.square-device-item small { color: var(--el-text-color-secondary); overflow-wrap: anywhere; }
.device-details { display: flex; flex-wrap: wrap; gap: 5px; align-items: center; }
.source-statuses { display: flex; flex-wrap: wrap; gap: 8px; }
.device-status { margin-inline-start: auto; font-size: 12px; }
.online { color: #269528; } .offline { color: #c33; } .unknown { color: #996800; }
@media (max-width: 1280px) {
  .square-device-panel { width: 280px; flex-basis: 280px; }
}
@media (max-width: 700px) {
  .square-device-panel { width: 100%; flex: 0 0 380px; border-inline-end: 0; border-bottom: 1px solid #e4e7ed; }
  .square-filters { grid-template-columns: repeat(2, minmax(0, 1fr)); padding: 8px 16px; }
  .square-location-summary { align-self: center; }
  .device-list-header { padding: 8px 16px; }
}
</style>

<style scoped>
.device-list-actions { align-items: center; gap: 20px; }
.header-rule { flex: 1; min-width: 4px; border-top: 1px dashed #e8e8e8; }
.protocol-collapse { display: flex; gap: 8px; align-items: center; font-size: 14px; white-space: nowrap; }
.protocol-collapse img { width: 16px; height: 16px; }
.protocol-collapse img.is-collapsed { transform: rotate(180deg); }
.device-action-btn, .device-action-btn:hover { width: 20px; height: 20px; padding: 0; background: transparent; color: #333; border-radius: 0; }
.catalog-settings { display: grid; gap: 8px; }
.catalog-settings .el-button { margin: 0; }
.protocol-option { display: flex; align-items: center; gap: 4px; min-width: 0; }
.protocol-menu { display: flex; align-items: center; justify-content: center; width: 16px; height: 20px; border: 0; background: transparent; padding: 0; cursor: pointer; }
.protocol-menu img { width: 16px; height: 16px; }
.protocol-option :deep(.el-checkbox__label) { font-size: 14px; padding-left: 8px; color: #333; }
.protocol-option :deep(.el-checkbox.is-checked .el-checkbox__label) { color: #2278ff; }
.protocol-option :deep(.el-checkbox__inner) { width: 16px; height: 16px; border-radius: 0; border-color: #ddd; }
.protocol-option :deep(.el-checkbox__inner::after) { left: 5px; top: 2px; }
.protocol-option :deep(.el-checkbox__input.is-checked .el-checkbox__inner) { background: #026fff; border-color: #026fff; }
.device-group-label { display: flex; align-items: center; gap: 8px; min-width: 0; }
.tree-caret { width: 12px; color: #a0a7b2; }
.device-name { font-size: 14px; font-weight: 400; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.device-state { flex: none; border-radius: 4px; padding: 0 6px; line-height: 18px; font-size: 12px; }
.device-state.online { color: #2278ff; background: rgb(34 120 255 / 12%); }
.device-state.offline { color: #999; background: #f5f5f5; }
.device-state.unknown { color: #ff8d22; background: rgb(255 141 34 / 12%); }
</style>
