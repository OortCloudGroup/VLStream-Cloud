<template>
  <aside v-loading="loading" class="device-list-panel square-device-panel">
    <slot name="views" />
    <!-- MasterGo 257:28769 / 309:33869 — 目录面板只有 区域/分组/标签 页签 + 搜索框 + 目录树 -->
    <VideoSquareClassificationTree v-if="classificationEnabled" :devices="classificationDevices || devices" show-tree
      @filter-change="$emit('classification-change', $event)">
      <template #toolbar>
        <el-input class="square-search" :model-value="keyword" :placeholder="$tp('搜索设备...')" clearable
          @update:model-value="$emit('search', $event)" />
      </template>
    </VideoSquareClassificationTree>
    <el-alert v-if="error" type="error" :title="$tp(error)" :closable="false" />
  </aside>
</template>

<script setup>
import VideoSquareClassificationTree from './VideoSquareClassificationTree.vue'

// 设备目录面板按设计只保留「页签 → 搜索 → 目录树」；筛选、设备卡片等由上层工具栏/缩略图行承担。
// 保留原有 props/emits 以免调用方收到未知属性告警。
defineProps({
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
</script>

<style scoped>
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
/* MasterGo 309:33869 — 搜索框 r4 + 1px #C6CBD4，占位文字 12px #808695 */
.square-search { display: block; margin-bottom: 16px; }
.square-search :deep(.el-input__wrapper) { border-radius: 4px; box-shadow: 0 0 0 1px #c6cbd4 inset; }
.square-search :deep(.el-input__inner::placeholder) { color: #808695; font-size: 12px; }
.square-device-panel > .el-alert { margin: 0 20px 12px; }
@media (max-width: 1280px) {
  .square-device-panel { width: 280px; flex-basis: 280px; }
}
@media (max-width: 700px) {
  .square-device-panel { width: 100%; flex: 0 0 380px; border-inline-end: 0; border-bottom: 1px solid #e4e7ed; }
}
</style>
