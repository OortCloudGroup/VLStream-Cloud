<template>
  <div class="device-manage">
    <!-- 筛选栏 -->
    <div class="filters">
      <div class="filter-item" @click="onFilter('tree')">
        <span>设备树</span>
        <van-icon name="arrow-down" class="filter-icon" />
      </div>
      <!-- 分组 -->
      <!-- <div class="filter-item" @click="onFilter('group')">
        <span>分组</span>
        <van-icon name="arrow-down" class="filter-icon" />
      </div> -->
      <div class="filter-item" @click="onFilter('tag')">
        <span>标签</span>
        <van-icon name="arrow-down" class="filter-icon" />
      </div>
    </div>

    <DeviceListMode v-if="activeMode === 'list'" :tag-name="tagName" />
    <DeviceMapMode v-else-if="activeMode === 'map'" />
    <DeviceCardMode v-else :tag-name="tagName" />

    <!-- 顶部筛选弹出面板 -->
    <VideoFilterPanel
      v-model:show="showFilterPanel"
      :default-tab="filterType"
      @confirm="onFilterConfirm"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import DeviceListMode from './DeviceListMode.vue'
import DeviceMapMode from './DeviceMapMode.vue'
import DeviceCardMode from './DeviceCardMode.vue'
import VideoFilterPanel from '../Filter/VideoFilterPanel.vue'

defineProps({
  activeMode: {
    type: String,
    default: 'list'
  }
})

const showFilterPanel = ref(false)
const filterType = ref('tree')
const tagName = ref(null)

const onFilter = (type) => {
  filterType.value = type
  showFilterPanel.value = true
}

const onFilterConfirm = (filters) => {
  if (filters.filterType === 'tag' && filters.filterValue) {
    tagName.value = filters.filterValue
  } else if (filters.filterType === 'tree' && filters.filterValue) {
    tagName.value = filters.filterValue
  } else {
    tagName.value = null
  }
  showFilterPanel.value = false
}
</script>

<style lang="scss" scoped>
.device-manage {
  position: relative;
  z-index: 1;
  margin-top: 16px;
}

.filters {
  display: flex;
  justify-content: space-between;
}

.filter-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  line-height: 18px;
  font-weight: 500;
  color: #333333;
  font-size: 14px;
}

.filter-icon {
  color: rgba(51, 51, 51, 0.35);
}
</style>

