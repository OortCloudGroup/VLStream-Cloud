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
          <span class="classification-node"><span data-i18n-ignore>{{ data.categoryName }}</span><small>{{ data.deviceCount }}</small></span>
        </template>
      </el-tree>
    </div>
    <div v-if="filtering" class="classification-loading" role="status">{{ $tp('加载中...') }}</div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { getClassificationTree, getDeviceClassification } from '@/api/device/classification'
import { categorySubtreeIds, createClassificationReader } from '@/utils/videoSquareClassification.mjs'
import { translatePhrase } from '@/i18n'
const props = defineProps({ devices: { type: Array, default: () => [] }, showTree: Boolean })
const emit = defineEmits(['filter-change'])
const tabs = [{ value: 'REGION', label: '区域' }, { value: 'GROUP', label: '分组' }, { value: 'TAG', label: '标签' }]
const activeType = ref('REGION')
const tree = ref([])
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
const displayTree = computed(() => [{ id: '__all__', categoryName: translatePhrase('全部'), deviceCount: props.devices.length, children: tree.value }])
async function loadTree() {
  const current = ++treeGeneration
  loading.value = true
  error.value = ''
  try {
    const response = await getClassificationTree(activeType.value, 'ALL')
    if (!mounted || current !== treeGeneration) return
    tree.value = response.data?.tree || []
  } catch (failure) {
    if (!mounted || current !== treeGeneration) return
    tree.value = []
    error.value = failure?.message || translatePhrase('获取分类失败')
  } finally {
    if (mounted && current === treeGeneration) loading.value = false
  }
}
async function selectNode(node) {
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
  if (selectedId.value != null) selectNode({ id: selectedId.value })
})
onMounted(loadTree)
onBeforeUnmount(() => { mounted = false; generation++; treeGeneration++; reader.clear() })
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
.classification-error { color: #c33; display: flex; gap: 8px; align-items: center; overflow-wrap: anywhere; }
.classification-loading { color: #778da2; font-size: 12px; padding-bottom: 8px; }
</style>
