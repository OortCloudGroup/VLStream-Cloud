<template>
  <div class="square-classification">
    <el-tabs v-model="activeType" stretch @tab-change="changeType">
      <el-tab-pane v-for="tab in tabs" :key="tab.value" :name="tab.value" :label="$tp(tab.label)" />
    </el-tabs>
    <div v-if="error" class="classification-error" role="alert">
      <span>{{ error }}</span><el-button text size="small" @click="refresh">{{ $tp('重试') }}</el-button>
    </div>
    <div v-show="showTree" v-loading="loading" class="classification-tree">
      <el-tree :data="displayTree" :props="{ label: 'categoryName', children: 'children' }" node-key="id"
        default-expand-all highlight-current :expand-on-click-node="false" @node-click="selectNode">
        <template #default="{ data }">
          <span :draggable="!!data.device" @dragstart="dragCatalogDevice($event, data.device)" class="classification-node" :class="{ 'is-device': data.device, 'is-selected': data.device?.id === selectedDeviceId }"><span class="classification-label"><img :src="data.device ? cameraIcon : folderIcon" width="18" height="18" alt="" /><span data-i18n-ignore>{{ data.categoryName }}</span></span><span v-if="data.device" class="classification-device-status" :class="data.device.status.toLowerCase()">{{ $tp(data.device.status === 'ONLINE' ? '在线' : data.device.status === 'OFFLINE' ? '离线' : '未知状态') }}</span><small v-else>{{ data.deviceCount }}</small></span>
        </template>
      </el-tree>
    </div>
    <div v-if="filtering" class="classification-loading" role="status">{{ $tp('加载中...') }}</div>
  </div>
</template>

<script setup>
import { dragCatalogDevice } from "@/utils/videoSquareDeviceDrag.mjs"
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import folderIcon from '@/assets/mastergo/video-square/folder.svg'
import cameraIcon from '@/assets/mastergo/video-square/device-camera.svg'
import { getClassificationTree, getDeviceClassification } from '@/api/device/classification'
import { categorySubtreeIds, createClassificationReader, countClassificationTree } from '@/utils/videoSquareClassification.mjs'
import { translatePhrase } from '@/i18n'
const props = defineProps({ devices: { type: Array, default: () => [] }, visibleDevices: Array, selectedDeviceId: [String, Number], showTree: Boolean, filterEmpty: Boolean })
const emit = defineEmits(['filter-change', 'device-click'])
const tabs = [{ value: 'REGION', label: '区域' }, { value: 'GROUP', label: '分组' }, { value: 'TAG', label: '标签' }]
const activeType = ref('REGION')
const tree = ref([])
const countedTree = ref([])
let countGeneration = 0
const selectedId = ref(null)
const loading = ref(false)
const filtering = ref(false)
const error = ref('')
let generation = 0
let treeGeneration = 0
let mounted = true
const reader = createClassificationReader(async (protocol, rowId) => {
  const response = await getDeviceClassification(protocol, rowId)
  return response.data
})
const displayTree = computed(() => {
  const visible = new Map((props.visibleDevices || props.devices).map(device => [device.id, device]))
  const assigned = new Set()
  const leaf = (id, parent) => ({ id: `device:${parent}:${id}`, categoryName: visible.get(id).deviceName, device: visible.get(id) })
  const visit = nodes => nodes.map(node => {
    const ids = node.deviceIds || []
    ids.forEach(id => assigned.add(id))
    return { ...node, children: [...visit(node.children || []), ...ids.filter(id => visible.has(id)).map(id => leaf(id, node.id))] }
  })
  const children = visit(countedTree.value)
  const unassigned = [...visible.keys()].filter(id => !assigned.has(id)).map(id => leaf(id, '__all__'))
  return [{ id: '__all__', categoryName: translatePhrase('全部'), deviceCount: props.devices.length, children: [...children, ...unassigned] }]
})
async function updateCounts() {
  const current = ++countGeneration
  const isCurrent = () => mounted && current === countGeneration
  countedTree.value = []
  try {
    const next = await countClassificationTree(tree.value, props.devices, activeType.value, reader, props.filterEmpty, isCurrent)
    if (isCurrent()) countedTree.value = next
  } catch (failure) {
    if (isCurrent()) error.value = failure?.message || translatePhrase('获取分类失败')
  }
}
async function loadTree() {
  const current = ++treeGeneration
  loading.value = true
  error.value = ''
  try {
    const response = await getClassificationTree(activeType.value, 'ALL')
    if (!mounted || current !== treeGeneration) return
    tree.value = response.data?.tree || []
    await updateCounts()
  } catch (failure) {
    if (!mounted || current !== treeGeneration) return
    tree.value = []
    countedTree.value = []
    error.value = failure?.message || translatePhrase('获取分类失败')
  } finally {
    if (mounted && current === treeGeneration) loading.value = false
  }
}
async function selectNode(node) {
  if (node.device) { emit('device-click', node.device); return }
  const current = ++generation
  selectedId.value = node.id === '__all__' ? null : node.id
  error.value = ''
  if (selectedId.value == null) {
    filtering.value = false
    emit('filter-change', null)
    return
  }
  filtering.value = true
  emit('filter-change', [])
  try {
    const ids = await reader.matches(props.devices, activeType.value, categorySubtreeIds(tree.value, selectedId.value), () => mounted && generation === current)
    if (mounted && current === generation) emit('filter-change', ids)
  } catch (failure) {
    if (mounted && current === generation) error.value = failure?.message || translatePhrase('获取分类设备失败')
  } finally {
    if (mounted && current === generation) filtering.value = false
  }
}
async function changeType() {
  ++countGeneration
  countedTree.value = []
  ++generation
  selectedId.value = null
  filtering.value = false
  emit('filter-change', null)
  await loadTree()
}
async function refresh() {
  reader.clear()
  await loadTree()
  if (selectedId.value != null) await selectNode({ id: selectedId.value })
}
watch(() => props.devices.map(device => `${device.id}=${(device.sources || []).map(source => `${source.protocol}:${source.rowId}`).join(',')}`).join(';'), () => {
  updateCounts()
  if (selectedId.value != null) selectNode({ id: selectedId.value })
})
onMounted(loadTree)
watch(() => props.filterEmpty, updateCounts)
onBeforeUnmount(() => { mounted = false; generation++; treeGeneration++; countGeneration++; reader.clear() })
</script>

<style scoped>
/* MasterGo 257:28769 — 区域/分组/标签 等宽页签，高 32，字号 16，激活 #2278FF + 2px 下划线 */
.square-classification { padding-inline: 20px; flex-shrink: 0; }
.square-classification :deep(.el-tabs__header) { margin: 0 0 16px; }
.square-classification :deep(.el-tabs__item) { height: 32px; line-height: 32px; font-size: 16px; color: #3d3d3d; }
.square-classification :deep(.el-tabs__item.is-active) { color: #2278ff; }
.square-classification :deep(.el-tabs__nav-wrap::after) { height: 1px; background: #e8e8e8; }
.square-classification :deep(.el-tabs__active-bar) { height: 2px; background-color: #2278ff; }
/* MasterGo 257:28614 — 目录树 7 行 × 32px，选中行 rgba(34,120,255,.12) */
.classification-tree { max-height: 224px; overflow: auto; padding-bottom: 8px; }
.classification-tree :deep(.el-tree-node__content) { height: 32px; }
.classification-tree :deep(.el-tree-node.is-current > .el-tree-node__content) { background: rgba(34, 120, 255, 0.12); }
.classification-node { display: flex; gap: 8px; justify-content: space-between; width: 100%; padding-inline-end: 8px; overflow: hidden; }
.classification-node > span { overflow: hidden; text-overflow: ellipsis; font-size: 14px; color: #333333; }
.classification-node small { font-size: 14px; color: #999999; }
.classification-label { display: flex; align-items: center; gap: 8px; min-width: 0; }
.classification-label > span { overflow: hidden; text-overflow: ellipsis; }
.classification-node.is-device { justify-content: flex-start; gap: 8px; }
.classification-node.is-selected { color: #2278ff; }
.classification-device-status { font-size: 12px; line-height: 18px; padding: 0 6px; border-radius: 4px; white-space: nowrap; }
.classification-device-status.online { color: #2278ff; background: rgb(34 120 255 / 12%); }
.classification-device-status.offline { color: #999; background: #f5f5f5; }
.classification-device-status.unknown { color: #ff8d22; background: rgb(255 141 34 / 12%); }
.classification-error { color: #c33; display: flex; gap: 8px; align-items: center; overflow-wrap: anywhere; }
.classification-loading { color: #778da2; font-size: 12px; padding-bottom: 8px; }
</style>
