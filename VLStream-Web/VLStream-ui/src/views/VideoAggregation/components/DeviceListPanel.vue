<template>
  <div class="device-list-panel">
    <!-- 面板头部 -->
    <div class="panel-header">
      <div class="header-title">
        <h3>设备列表</h3>
      </div>
      <div class="header-controls">
        <button class="control-btn" @click="handleSettings" title="设置">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.07-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.74,8.87 C2.62,9.08,2.66,9.34,2.86,9.48l2.03,1.58C4.84,11.36,4.82,11.69,4.82,12s0.02,0.64,0.07,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- 视图切换按钮 -->
    <div class="view-toggle">
      <button 
        class="toggle-btn" 
        :class="{ active: isTreeView }" 
        @click="toggleTreeView"
        title="树形视图"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <path d="M22 11V3h-7v3H9V3H2v8h7V8h2v10h4v3h7v-8h-7v3h-2V8h2v3h7z"/>
        </svg>
        树形
      </button>
      <button 
        class="toggle-btn" 
        :class="{ active: !isTreeView }" 
        @click="toggleListView"
        title="列表视图"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z"/>
        </svg>
        列表
      </button>
    </div>

    <!-- 设备统计弹窗触发按钮 -->
    <div class="stats-section">
      <button class="stats-btn" @click="showDeviceStatsModal" title="设备统计">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z"/>
        </svg>
        设备统计
      </button>
    </div>

    <!-- 搜索框 -->
    <div class="search-section">
      <el-input
        :model-value="mapSearchKeyword"
        placeholder="搜索设备..."
        prefix-icon="Search"
        clearable
        @input="handleSearchInput"
        @clear="handleSearchClear"
      />
    </div>

    <!-- 设备列表内容 -->
    <div class="device-content">
      <!-- 树形视图 -->
      <div v-if="isTreeView" class="tree-view">
        <el-tree
          :data="treeData"
          :props="{ children: 'children', label: 'name' }"
          :expand-on-click-node="false"
          node-key="id"
          @node-click="handleTreeNodeClick"
        >
          <template #default="{ data }">
            <div class="tree-node">
              <span class="node-icon">
                <svg v-if="data.type === 'location'" width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
                </svg>
                <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M21 6.5l-4-4h-5l-4 4v5l4 4h5l4-4v-5zm-1 4.5l-3.5 3.5h-4L9 10.5v-4L12.5 3h4L20 6.5v4.5z"/>
                </svg>
              </span>
              <span class="node-label">{{ data.name }}</span>
              <span v-if="data.count" class="node-count">({{ data.count }})</span>
            </div>
          </template>
        </el-tree>
      </div>

      <!-- 列表视图 -->
      <div v-else class="list-view">
        <div class="filter-section">
          <!-- 位置分类筛选 -->
          <el-select
            v-model="selectedLocationCategory"
            placeholder="选择位置"
            clearable
            style="margin-bottom: 12px; width: 100%;"
            @change="filterDevices"
          >
            <el-option
              v-for="category in locationCategories"
              :key="category.value"
              :label="category.label"
              :value="category.value"
            />
          </el-select>

          <!-- 状态筛选 -->
          <el-select
            v-model="selectedStatus"
            placeholder="选择状态"
            clearable
            style="width: 100%;"
            @change="filterDevices"
          >
            <el-option
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>

        <!-- 设备列表 -->
        <div class="device-list">
          <div
            v-for="camera in filteredCameras"
            :key="camera.id"
            class="device-item"
            :class="{ 
              active: selectedDisplayItems.includes(camera.id),
              online: camera.status === 'online',
              offline: camera.status === 'offline'
            }"
            @click="handleCameraClick(camera)"
          >
            <div class="device-info">
              <div class="device-name">{{ camera.name }}</div>
              <div class="device-location">{{ camera.location }}</div>
              <div class="device-status">
                <span 
                  class="status-indicator" 
                  :class="camera.status"
                ></span>
                {{ camera.status === 'online' ? '在线' : '离线' }}
              </div>
            </div>
            
            <!-- 实时流状态 -->
            <div v-if="realCameraStreams[camera.id]" class="stream-status">
              <span class="stream-indicator active"></span>
              <small>直播中</small>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 设备统计弹窗 -->
    <el-dialog
      :model-value="showDeviceStatsDialog"
      title="设备统计信息"
      width="600px"
      @update:model-value="handleDialogClose"
    >
      <div v-if="currentStatsItem" class="stats-content">
        <!-- 统计图表区域 -->
        <div class="stats-charts">
          <div class="chart-item">
            <h4>设备状态分布</h4>
            <div class="chart-placeholder">
              <!-- 这里可以集成图表库如ECharts -->
              <div class="simple-chart">
                <div class="chart-bar online" style="height: 70%;">
                  <span>在线: {{ currentStatsItem.onlineCount }}</span>
                </div>
                <div class="chart-bar offline" style="height: 30%;">
                  <span>离线: {{ currentStatsItem.offlineCount }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 详细统计信息 -->
        <div class="stats-details">
          <div class="detail-item">
            <label>总设备数量:</label>
            <span>{{ currentStatsItem.totalCount }}</span>
          </div>
          <div class="detail-item">
            <label>在线设备:</label>
            <span class="online">{{ currentStatsItem.onlineCount }}</span>
          </div>
          <div class="detail-item">
            <label>离线设备:</label>
            <span class="offline">{{ currentStatsItem.offlineCount }}</span>
          </div>
          <div class="detail-item">
            <label>在线率:</label>
            <span>{{ ((currentStatsItem.onlineCount / currentStatsItem.totalCount) * 100).toFixed(1) }}%</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

// Props
const props = defineProps({
  isTreeView: {
    type: Boolean,
    default: false
  },
  selectedDisplayItems: {
    type: Array,
    default: () => []
  },
  currentStatsItem: {
    type: Object,
    default: null
  },
  showDeviceStatsDialog: {
    type: Boolean,
    default: false
  },
  mapSearchKeyword: {
    type: String,
    default: ''
  },
  realCameraStreams: {
    type: Object,
    default: () => ({})
  },
  treeData: {
    type: Array,
    default: () => []
  },
  locationCategories: {
    type: Array,
    default: () => []
  },
  statusOptions: {
    type: Array,
    default: () => []
  }
})

// Emits
const emit = defineEmits([
  'settings',
  'toggle-tree-view',
  'toggle-list-view',
  'show-device-stats-modal',
  'hide-device-stats-modal',
  'map-search',
  'camera-click',
  'tree-node-click'
])

// 本地状态
const selectedLocationCategory = ref('')
const selectedStatus = ref('')

// 模拟设备数据
const cameras = ref([
  { id: 1, name: '摄像头1', location: '大门入口', status: 'online' },
  { id: 2, name: '摄像头2', location: '停车场', status: 'offline' },
  { id: 3, name: '摄像头3', location: '大厅', status: 'online' },
  // 更多设备数据...
])

// 过滤后的设备列表
const filteredCameras = computed(() => {
  let filtered = cameras.value

  // 按位置筛选
  if (selectedLocationCategory.value) {
    filtered = filtered.filter(camera => 
      camera.location === selectedLocationCategory.value
    )
  }

  // 按状态筛选
  if (selectedStatus.value) {
    filtered = filtered.filter(camera => 
      camera.status === selectedStatus.value
    )
  }

  // 按搜索关键词筛选
  if (props.mapSearchKeyword) {
    filtered = filtered.filter(camera =>
      camera.name.toLowerCase().includes(props.mapSearchKeyword.toLowerCase()) ||
      camera.location.toLowerCase().includes(props.mapSearchKeyword.toLowerCase())
    )
  }

  return filtered
})

// 方法
const handleSettings = () => {
  emit('settings')
}

const toggleTreeView = () => {
  emit('toggle-tree-view')
}

const toggleListView = () => {
  emit('toggle-list-view')
}

const showDeviceStatsModal = () => {
  emit('show-device-stats-modal')
}

const hideDeviceStatsModal = () => {
  emit('hide-device-stats-modal')
}

const handleSearchInput = (value) => {
  emit('map-search', value)
}

const handleSearchClear = () => {
  emit('map-search', '')
}

const handleDialogClose = (value) => {
  if (!value) {
    emit('hide-device-stats-modal')
  }
}

const handleCameraClick = (camera) => {
  emit('camera-click', camera)
}

const handleTreeNodeClick = (data) => {
  emit('tree-node-click', data)
}

const filterDevices = () => {
  // 触发过滤逻辑
}
</script>

<style scoped>
.device-list-panel {
  position: absolute;
  top: 80px;
  right: 20px;
  width: 300px;
  height: calc(100vh - 120px);
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  z-index: 500;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.header-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.header-controls {
  display: flex;
  gap: 8px;
}

.control-btn {
  width: 32px;
  height: 32px;
  background: none;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  transition: all 0.2s;
}

.control-btn:hover {
  border-color: #1A53FF;
  color: #1A53FF;
}

.view-toggle {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.toggle-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  cursor: pointer;
  font-size: 12px;
  color: #666;
  transition: all 0.2s;
}

.toggle-btn:first-child {
  border-radius: 6px 0 0 6px;
  border-right: none;
}

.toggle-btn:last-child {
  border-radius: 0 6px 6px 0;
}

.toggle-btn.active {
  background: #1A53FF;
  border-color: #1A53FF;
  color: white;
}

.stats-section {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.stats-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}

.stats-btn:hover {
  background: #e9ecef;
  border-color: #1A53FF;
  color: #1A53FF;
}

.search-section {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.device-content {
  flex: 1;
  overflow: hidden;
}

.tree-view,
.list-view {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.node-icon {
  color: #666;
}

.node-label {
  font-size: 14px;
  color: #333;
}

.node-count {
  font-size: 12px;
  color: #999;
}

.filter-section {
  margin-bottom: 16px;
}

.device-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.device-item {
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.device-item:hover {
  border-color: #1A53FF;
  box-shadow: 0 2px 8px rgba(26, 83, 255, 0.1);
}

.device-item.active {
  border-color: #1A53FF;
  background: #f0f8ff;
}

.device-item.offline {
  opacity: 0.7;
}

.device-info {
  margin-bottom: 8px;
}

.device-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.device-location {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.device-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-indicator.online {
  background: #52c41a;
}

.status-indicator.offline {
  background: #ff4d4f;
}

.stream-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.stream-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.stream-indicator.active {
  background: #1A53FF;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

/* 统计弹窗样式 */
.stats-content {
  padding: 16px 0;
}

.stats-charts {
  margin-bottom: 24px;
}

.chart-item h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #333;
}

.chart-placeholder {
  height: 200px;
  background: #f8f9fa;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.simple-chart {
  display: flex;
  align-items: end;
  gap: 20px;
  height: 150px;
}

.chart-bar {
  width: 60px;
  display: flex;
  align-items: end;
  justify-content: center;
  padding: 8px;
  border-radius: 4px;
  color: white;
  font-size: 12px;
  font-weight: 500;
}

.chart-bar.online {
  background: #52c41a;
}

.chart-bar.offline {
  background: #ff4d4f;
}

.stats-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 4px;
}

.detail-item label {
  font-weight: 500;
  color: #666;
}

.detail-item span {
  font-weight: 600;
}

.detail-item span.online {
  color: #52c41a;
}

.detail-item span.offline {
  color: #ff4d4f;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .device-list-panel {
    width: 280px;
  }
}

@media (max-width: 768px) {
  .device-list-panel {
    position: fixed;
    top: 60px;
    right: 10px;
    left: 10px;
    width: auto;
    height: calc(100vh - 80px);
  }
}
</style> 