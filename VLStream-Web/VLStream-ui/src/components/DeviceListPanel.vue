<template>
  <div class="device-list-panel">
    <!-- 设备列表头部 -->
    <div class="device-list-header">
      <span class="device-list-title">设备列表</span>
      <div class="device-list-actions">
        <button class="device-action-btn" @click="$emit('settings')" title="设置">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
            <path d="M12,15.5A3.5,3.5 0 0,1 8.5,12A3.5,3.5 0 0,1 12,8.5A3.5,3.5 0 0,1 15.5,12A3.5,3.5 0 0,1 12,15.5M19.43,12.97C19.47,12.65 19.5,12.33 19.5,12C19.5,11.67 19.47,11.34 19.43,11.03L21.54,9.37C21.73,9.22 21.78,8.95 21.66,8.73L19.66,5.27C19.54,5.05 19.27,4.96 19.05,5.05L16.56,6.05C16.04,5.66 15.5,5.32 14.87,5.07L14.5,2.42C14.46,2.18 14.25,2 14,2H10C9.75,2 9.54,2.18 9.5,2.42L9.13,5.07C8.5,5.32 7.96,5.66 7.44,6.05L4.95,5.05C4.73,4.96 4.46,5.05 4.34,5.27L2.34,8.73C2.22,8.95 2.27,9.22 2.46,9.37L4.57,11.03C4.53,11.34 4.5,11.67 4.5,12C4.5,12.33 4.53,12.65 4.57,12.97L2.46,14.63C2.27,14.78 2.22,15.05 2.34,15.27L4.34,18.73C4.46,18.95 4.73,19.03 4.95,18.95L7.44,17.94C7.96,18.34 8.5,18.68 9.13,18.93L9.5,21.58C9.54,21.82 9.75,22 10,22H14C14.25,22 14.46,21.82 14.5,21.58L14.87,18.93C15.5,18.68 16.04,18.34 16.56,17.94L19.05,18.95C19.27,19.03 19.54,18.95 19.66,18.73L21.66,15.27C21.78,15.05 21.73,14.78 21.54,14.63L19.43,12.97Z"/>
          </svg>
        </button>
        <button class="device-action-btn" @click="toggleTreeView" :class="{ active: isTreeView }" title="树形结构">
          <img src="@/assets/组织-选中@2x.png" alt="树形结构" width="16" height="16">
        </button>
        <button class="device-action-btn" @click="toggleListView" :class="{ active: !isTreeView }" title="列表结构">
          <img src="@/assets/人员列表-未选中@2x.png" alt="列表结构" width="16" height="16">
        </button>
      </div>
      </div>
      
    <div class="device-list-content">
      <!-- 显示设备信息项 - 始终显示，不依赖勾选状态 -->
      <div class="selected-display-items">
        <div class="display-items-list">
          <div 
            v-for="item in selectedDisplayItems" 
            :key="item.id"
            class="display-item-tag"
          >
            <div class="tag-content">
              <el-checkbox 
                :model-value="item.checked" 
                @update:model-value="updateItemChecked(item, $event)"
                class="tag-checkbox"
              />
              <span class="tag-label">{{ item.label }}</span>
            </div>
            <el-popover
              placement="bottom"
              :width="400"
              trigger="click"
              :visible="currentStatsItem?.id === item.id && showDeviceStatsDialog"
              @hide="hideDeviceStatsModal"
              popper-class="device-stats-popover"
            >
              <template #reference>
        <button 
                  class="tag-stats-btn" 
                  @click="toggleDeviceStatsModal(item)"
                  title="展开/收起设备统计详情"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" :class="{ 'rotated': currentStatsItem?.id === item.id && showDeviceStatsDialog }">
                    <path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z"/>
                  </svg>
        </button>
              </template>
              
              <!-- Popover内容 -->
              <div class="device-stats-content">
                <div class="device-stats-expanded">
                  <div class="stats-header">
                    <div class="device-icon">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M17 10.5V7a1 1 0 0 0-1-1H4a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-3.5l4 4v-11l-4 4z"/>
                      </svg>
      </div>
                    <span class="device-type-title">{{ item.label }}</span>
    </div>
    
                  <div class="total-count">
                    <span class="count-label">总数</span>
                    <span class="count-number">{{ getDeviceCount(item.id) }}</span>
                  </div>

                  <!-- 位置分类统计 -->
                  <div class="location-stats">
                    <div v-for="location in locationCategories" :key="location.id" class="location-group">
                      <div class="location-title">{{ location.label }}</div>
                      <div class="status-options">
                        <div v-for="status in statusOptions" :key="`${location.id}-${status.id}`" class="status-item">
                          <el-checkbox 
                            v-model="item.locationStats[location.id][status.id]"
                            class="status-checkbox"
                          >
                            {{ status.label }}
                          </el-checkbox>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </el-popover>
          </div>
        </div>
    </div>
    
      <!-- 设备搜索框 -->
      <div class="device-search-container">
        <div class="device-search-wrapper">
          <input 
            v-model="searchKeyword"
            type="text" 
            placeholder="搜索摄像机"
            class="device-search-input"
            @keyup.enter="handleSearch"
          />
          <button class="device-search-btn" @click="handleSearch">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <path d="M15.5 14h-.79l-.28-.27a6.5 6.5 0 1 0-.7.7l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0a4.5 4.5 0 1 1 0-9 4.5 4.5 0 0 1 0 9z"/>
            </svg>
          </button>
        </div>
      </div>
      
      <!-- 设备列表 -->
      <div class="device-list-items">
      <!-- 列表视图 -->
        <template v-if="!isTreeView">
          <div 
            v-for="device in filteredDevices" 
            :key="device.id"
            class="device-item"
            @click="$emit('device-click', device)"
          >
            <div class="device-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M17 10.5V7a1 1 0 0 0-1-1H4a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-3.5l4 4v-11l-4 4z"/>
              </svg>
            </div>
            <div class="device-info">
              <div class="device-name">{{ device.deviceName }}</div>
              <div class="device-details">
                <span class="device-type-tag">{{ device.deviceType }}</span>
                <span class="device-status" :class="getDeviceStatusClass(device.status)">
                  {{ device.status }}
                </span>
                <span class="device-ip">{{ device.ipAddress }}</span>
              </div>
            </div>
            </div>
        </template>

        <!-- 树形视图 -->
        <template v-else>
          <div class="tree-view">
            <template v-for="group in treeData" :key="group.id">
              <div class="tree-group" @click="handleTreeNodeClick(group)">
                <div class="tree-group-header">
                  <svg class="tree-expand-icon" :class="{ 'expanded': group.expanded }" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M8.59 16.58L13.17 12L8.59 7.41L10 6L16 12L10 18L8.59 16.58Z"/>
                  </svg>
                  <svg class="tree-group-icon" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M10,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V8C22,6.89 21.1,6 20,6H12L10,4Z"/>
                  </svg>
                  <span class="tree-group-label">{{ group.label }}</span>
                  <span class="tree-group-count">({{ group.children?.length || 0 }})</span>
            </div>
            </div>
              <template v-if="group.expanded">
                <div 
                  v-for="device in group.children"
                  :key="device.id"
                  class="tree-device-item"
                  @click="$emit('device-click', device)"
                >
                  <div class="tree-device-indent"></div>
                  <svg class="tree-device-icon" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M17 10.5V7a1 1 0 0 0-1-1H4a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-3.5l4 4v-11l-4 4z"/>
                  </svg>
                  <div class="tree-device-info">
                    <div class="tree-device-name">{{ device.deviceName }}</div>
                    <div class="tree-device-details">
                      <span class="device-type-tag">{{ device.deviceType }}</span>
                      <span class="device-status" :class="getDeviceStatusClass(device.status)">{{ device.status }}</span>
            </div>
          </div>
        </div>
              </template>
            </template>
            </div>
        </template>
            </div>
          </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

// Props
const props = defineProps({
  devices: {
    type: Array,
    default: () => []
  },
  treeData: {
    type: Array,
    default: () => []
  },
  displaySettings: {
    type: Object,
    default: () => ({
      deviceTypes: ['bulletCamera', 'ballCamera'],
      showOnline: true,
      showOffline: true
    })
  }
})

// Emits
const emit = defineEmits([
  'settings',
  'device-click',
  'search'
])

// 响应式数据
const searchKeyword = ref('')
const isTreeView = ref(false)

// 所有可能的设备类型（内部状态，不会消失）
const allDeviceTypes = ref([
  { 
    id: 'bulletCamera', 
    label: '枪机', 
    checked: true, 
    expanded: true,
    locationStats: {
      'aerial': { 'online': true, 'offline': true },
      'ground': { 'online': true, 'offline': true },
      'underground': { 'online': true, 'offline': true }
    }
  },
  { 
    id: 'ballCamera', 
    label: '球机', 
    checked: true, 
    expanded: true,
    locationStats: {
      'aerial': { 'online': true, 'offline': true },
      'ground': { 'online': true, 'offline': true },
      'underground': { 'online': true, 'offline': true }
    }
  }
])

// 显示的设备类型选项（基于父组件的displaySettings控制）
const deviceTypeOptions = computed(() => {
  return allDeviceTypes.value.filter(deviceType => 
    props.displaySettings.deviceTypes.includes(deviceType.id)
  )
})

const locationCategories = ref([
  { id: 'aerial', label: '高空' },
  { id: 'ground', label: '地面' },
  { id: 'underground', label: '地下' }
])

const statusOptions = ref([
  { id: 'online', label: '在线' },
  { id: 'offline', label: '离线' }
])

const selectedDisplayItems = computed(() => {
  // 显示所有在displaySettings中允许的设备类型，不管是否勾选
  return allDeviceTypes.value.filter(item => 
    props.displaySettings.deviceTypes.includes(item.id)
  )
})

const showDeviceStatsDialog = ref(false)
const currentStatsItem = ref(null)

const filteredDevices = computed(() => {
  let devices = props.devices
  
  // 根据设备类型勾选状态过滤
  const checkedDeviceTypes = allDeviceTypes.value.filter(item => item.checked).map(item => item.id)
  devices = devices.filter(device => {
    const deviceType = getDeviceTypeFromName(device.deviceName, device)
    // 根据设备类型映射到对应的ID
    const typeId = deviceType === '枪机' || deviceType === '摄像头' ? 'bulletCamera' : 
                   deviceType === '球机' ? 'ballCamera' : null
    return typeId && checkedDeviceTypes.includes(typeId)
  })
  
  // 根据搜索关键词过滤
  if (searchKeyword.value.trim()) {
    devices = devices.filter(device => 
      device.deviceName.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }
  
  return devices
})

// 根据设备名称或类型判断设备类型
const getDeviceTypeFromName = (deviceName, device) => {
  if (device.deviceType) {
    return device.deviceType
  }
  
  // 根据设备名称判断类型
  const name = deviceName.toLowerCase()
  if (name.includes('枪机') || name.includes('摄像头') || name.includes('camera')) {
    return '枪机'
  } else if (name.includes('球机') || name.includes('球形') || name.includes('dome')) {
    return '球机'
  }
  
  // 默认返回枪机
  return '枪机'
}

// 方法
const updateItemChecked = (item, checked) => {
  const index = allDeviceTypes.value.findIndex(option => option.id === item.id)
  if (index !== -1) {
    allDeviceTypes.value[index].checked = checked
  }
}

const toggleTreeView = () => {
  isTreeView.value = true
  ElMessage.success('已切换到树形结构')
}

const toggleListView = () => {
  isTreeView.value = false
  ElMessage.success('已切换到列表结构')
}

const handleSearch = () => {
  console.log('设备搜索:', searchKeyword.value)
  emit('search', searchKeyword.value)
  
  if (filteredDevices.value.length === 0) {
    ElMessage.warning('未找到匹配的设备')
  } else {
    ElMessage.success(`找到 ${filteredDevices.value.length} 个匹配的设备`)
  }
}

const toggleDeviceStatsModal = (item) => {
  if (showDeviceStatsDialog.value && currentStatsItem.value?.id === item.id) {
    hideDeviceStatsModal()
  } else {
    currentStatsItem.value = item
    showDeviceStatsDialog.value = true
    
    // 等待DOM更新后应用深色模式样式 - 多次尝试确保生效
    nextTick(() => {
      applyDarkModeToPopover()
    })
    
    // 延迟100ms再次尝试，确保Element Plus弹窗完全渲染
    setTimeout(() => {
      applyDarkModeToPopover()
    }, 100)
    
    // 延迟300ms最后一次尝试
    setTimeout(() => {
      applyDarkModeToPopover()
    }, 300)
  }
}

// 强制为弹窗应用深色模式样式
const applyDarkModeToPopover = () => {
  const mapArea = document.querySelector('.map-area')
  if (!mapArea) return
  
  const isDarkMode = mapArea.classList.contains('dark-mode')
  
  // 查找所有可能的弹窗元素
  const popover = document.querySelector('.device-stats-popover') || 
                  document.querySelector('.el-popper.device-stats-popover') ||
                  document.querySelector('.el-popper[data-popper-placement]') ||
                  document.querySelector('.el-popper') ||
                  document.querySelector('[data-popper-placement]') ||
                  document.querySelector('[role="tooltip"]')
  
  if (popover && isDarkMode) {
    // 强制应用深色模式样式到弹窗元素本身
    popover.style.setProperty('background', 'rgba(40, 44, 52, 0.95)', 'important')
    popover.style.setProperty('border', 'none', 'important')
    popover.style.setProperty('box-shadow', '0 4px 12px rgba(0, 0, 0, 0.3)', 'important')
    popover.style.setProperty('color', '#e8f4fd', 'important')
    
    // 强制应用弹窗内部所有可能的背景元素
    const innerElements = [
      popover,
      popover.querySelector('.device-stats-content'),
      popover.querySelector('.device-stats-expanded'),
      popover.querySelector('.el-popper__content'),
      popover.querySelector('.el-popover__content')
    ]
    
    innerElements.forEach(element => {
      if (element) {
        element.style.setProperty('background', 'rgba(40, 44, 52, 0.95)', 'important')
        element.style.setProperty('background-color', 'rgba(40, 44, 52, 0.95)', 'important')
        element.style.setProperty('color', '#e8f4fd', 'important')
      }
    })
    
    // 强制应用内部元素样式
    const content = popover.querySelector('.device-stats-content')
    if (content) {
      content.style.setProperty('color', '#e8f4fd', 'important')
      content.style.setProperty('background', 'rgba(40, 44, 52, 0.95)', 'important')
      content.style.setProperty('background-color', 'rgba(40, 44, 52, 0.95)', 'important')
      content.style.setProperty('border', '1px solid rgba(100, 149, 237, 0.3)', 'important')
      content.style.setProperty('border-radius', '8px', 'important')
      content.style.setProperty('padding', '12px', 'important')
    }
    
    const typeTitle = popover.querySelector('.device-type-title')
    if (typeTitle) typeTitle.style.setProperty('color', '#e8f4fd', 'important')
    
    const deviceIcon = popover.querySelector('.device-icon')
    if (deviceIcon) deviceIcon.style.setProperty('color', '#7db8ff', 'important')
    
    const totalCount = popover.querySelector('.total-count')
    if (totalCount) {
      totalCount.style.setProperty('background', 'rgba(30, 34, 42, 0.6)', 'important')
      totalCount.style.setProperty('background-color', 'rgba(30, 34, 42, 0.6)', 'important')
      totalCount.style.setProperty('border-bottom', '1px solid rgba(100, 149, 237, 0.3)', 'important')
    }
    
    // 强制应用location-stats区域背景
    const locationStats = popover.querySelector('.location-stats')
    if (locationStats) {
      locationStats.style.setProperty('background', 'rgba(40, 44, 52, 0.95)', 'important')
      locationStats.style.setProperty('background-color', 'rgba(40, 44, 52, 0.95)', 'important')
    }
    
    const countLabel = popover.querySelector('.count-label')
    if (countLabel) countLabel.style.setProperty('color', '#b0c4de', 'important')
    
    const countNumber = popover.querySelector('.count-number')
    if (countNumber) countNumber.style.setProperty('color', '#7db8ff', 'important')
    
    const locationTitles = popover.querySelectorAll('.location-title')
    locationTitles.forEach(title => {
      title.style.setProperty('color', '#e8f4fd', 'important')
    })
    
    // 强制应用复选框样式
    const checkboxLabels = popover.querySelectorAll('.el-checkbox__label')
    checkboxLabels.forEach(label => {
      label.style.setProperty('color', '#e8f4fd', 'important')
    })
    
    const checkboxInners = popover.querySelectorAll('.el-checkbox__inner')
    checkboxInners.forEach(inner => {
      inner.style.setProperty('background-color', 'rgba(30, 34, 42, 0.8)', 'important')
      inner.style.setProperty('border-color', 'rgba(100, 149, 237, 0.5)', 'important')
    })
    
    const checkedInners = popover.querySelectorAll('.el-checkbox__input.is-checked .el-checkbox__inner')
    checkedInners.forEach(inner => {
      inner.style.setProperty('background-color', '#7db8ff', 'important')
      inner.style.setProperty('border-color', '#7db8ff', 'important')
    })
    
    // 强制处理所有div元素的背景
    const allDivs = popover.querySelectorAll('div')
    allDivs.forEach(div => {
      // 跳过特殊背景的元素
      if (!div.classList.contains('total-count') && 
          !div.classList.contains('el-checkbox__inner')) {
        const computedStyle = window.getComputedStyle(div)
        if (computedStyle.backgroundColor === 'rgb(255, 255, 255)' || 
            computedStyle.backgroundColor === 'white' ||
            computedStyle.backgroundColor === 'rgba(255, 255, 255, 1)') {
          div.style.setProperty('background', 'rgba(40, 44, 52, 0.95)', 'important')
          div.style.setProperty('background-color', 'rgba(40, 44, 52, 0.95)', 'important')
        }
      }
    })
  } else if (popover && !isDarkMode) {
    // 移除深色模式样式，恢复正常样式
    popover.style.background = ''
    popover.style.border = ''
    popover.style.boxShadow = ''
    popover.style.color = ''
    
    const content = popover.querySelector('.device-stats-content')
    if (content) content.style.color = ''
    
    const typeTitle = popover.querySelector('.device-type-title')
    if (typeTitle) typeTitle.style.color = ''
    
    const deviceIcon = popover.querySelector('.device-icon')
    if (deviceIcon) deviceIcon.style.color = ''
    
    const totalCount = popover.querySelector('.total-count')
    if (totalCount) {
      totalCount.style.background = ''
      totalCount.style.borderBottom = ''
    }
    
    const countLabel = popover.querySelector('.count-label')
    if (countLabel) countLabel.style.color = ''
    
    const countNumber = popover.querySelector('.count-number')
    if (countNumber) countNumber.style.color = ''
    
    const locationTitles = popover.querySelectorAll('.location-title')
    locationTitles.forEach(title => {
      title.style.color = ''
    })
    
    const checkboxLabels = popover.querySelectorAll('.el-checkbox__label')
    checkboxLabels.forEach(label => {
      label.style.color = ''
    })
    
    const checkboxInners = popover.querySelectorAll('.el-checkbox__inner')
    checkboxInners.forEach(inner => {
      inner.style.backgroundColor = ''
      inner.style.borderColor = ''
    })
  }
}

const hideDeviceStatsModal = () => {
  showDeviceStatsDialog.value = false
  currentStatsItem.value = null
}

const getDeviceCount = (deviceTypeId) => {
  if (deviceTypeId === 'bulletCamera') {
    return props.devices.filter(device => device.deviceType === '摄像头' || device.deviceType === '枪机').length
  } else if (deviceTypeId === 'ballCamera') {
    return props.devices.filter(device => device.deviceType === '球机').length
  }
  return 0
}

const getDeviceStatusClass = (status) => {
  return {
    'online': status === '在线',
    'offline': status === '离线'
  }
}

const handleTreeNodeClick = (group) => {
  group.expanded = !group.expanded
}

const handleGlobalClick = (event) => {
  const popover = event.target.closest('.el-popover, .tag-stats-btn')
  if (!popover && showDeviceStatsDialog.value) {
    hideDeviceStatsModal()
  }
}

// 强制应用树形结构样式
const applyTreeStructureStyles = () => {
  const mapArea = document.querySelector('.map-area')
  if (!mapArea) return
  
  const isDarkMode = mapArea.classList.contains('dark-mode')
  
  // 查找树形结构元素和列表结构元素
  const treeDeviceNames = document.querySelectorAll('.tree-device-name')
  const treeDeviceDetails = document.querySelectorAll('.tree-device-details')
  const treeGroupLabels = document.querySelectorAll('.tree-group-label')
  // 列表结构元素
  const deviceNames = document.querySelectorAll('.device-name')
  const deviceDetails = document.querySelectorAll('.device-details')
  const deviceIps = document.querySelectorAll('.device-ip')
  
  if (isDarkMode) {
    // 深色模式样式
    treeDeviceNames.forEach(name => {
      name.style.setProperty('color', '#ffffff', 'important')
      name.style.setProperty('font-weight', '500', 'important')
    })
    
    treeDeviceDetails.forEach(detail => {
      detail.style.setProperty('color', '#a0a0a0', 'important')
    })
    
    treeGroupLabels.forEach(label => {
      label.style.setProperty('color', '#ffffff', 'important')
      label.style.setProperty('font-weight', '600', 'important')
    })
    
    // 深色模式列表结构样式
    deviceNames.forEach(name => {
      name.style.setProperty('color', '#ffffff', 'important')
      name.style.setProperty('font-weight', '500', 'important')
    })
    
    deviceDetails.forEach(detail => {
      detail.style.setProperty('color', '#a0a0a0', 'important')
    })
    
    deviceIps.forEach(ip => {
      ip.style.setProperty('color', '#a0a0a0', 'important')
    })
    
    console.log('应用树形结构和列表结构深色模式样式')
  } else {
    // 浅色模式样式
    treeDeviceNames.forEach(name => {
      name.style.setProperty('color', '#303133', 'important')
      name.style.setProperty('font-weight', '500', 'important')
    })
    
    treeDeviceDetails.forEach(detail => {
      detail.style.setProperty('color', '#909399', 'important')
    })
    
    treeGroupLabels.forEach(label => {
      label.style.setProperty('color', '#606266', 'important')
      label.style.setProperty('font-weight', '600', 'important')
    })
    
    // 浅色模式列表结构样式
    deviceNames.forEach(name => {
      name.style.setProperty('color', '#303133', 'important')
      name.style.setProperty('font-weight', '500', 'important')
    })
    
    deviceDetails.forEach(detail => {
      detail.style.setProperty('color', '#909399', 'important')
    })
    
    deviceIps.forEach(ip => {
      ip.style.setProperty('color', '#909399', 'important')
    })
    
    console.log('恢复树形结构和列表结构浅色模式样式')
  }
}

onMounted(() => {
  document.addEventListener('click', handleGlobalClick)
  
  // 监听弹窗DOM变化，自动应用深色模式样式
  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      mutation.addedNodes.forEach((node) => {
        if (node.nodeType === 1) { // Element node
          // 检查是否是弹窗元素
          if (node.classList && (
            node.classList.contains('el-popper') || 
            node.classList.contains('device-stats-popover') ||
            node.hasAttribute('data-popper-placement')
          )) {
            setTimeout(() => {
              applyDarkModeToPopover()
            }, 50)
          }
          
          // 检查子元素中是否有弹窗
          const popovers = node.querySelectorAll && node.querySelectorAll('.el-popper, .device-stats-popover, [data-popper-placement]')
          if (popovers && popovers.length > 0) {
            setTimeout(() => {
              applyDarkModeToPopover()
            }, 50)
          }
          
          // 检查是否有树形结构或列表结构元素
          const structureElements = node.querySelectorAll && node.querySelectorAll('.tree-device-name, .tree-device-details, .tree-group-label, .device-name, .device-details, .device-ip')
          if (structureElements && structureElements.length > 0) {
            setTimeout(() => {
              applyTreeStructureStyles()
            }, 50)
          }
        }
      })
    })
  })
  
  // 监听body的变化
  observer.observe(document.body, {
    childList: true,
    subtree: true
  })
  
  // 组件卸载时清理观察器
  onUnmounted(() => {
    observer.disconnect()
  })
  
  // 初始化时应用树形结构样式
  nextTick(() => {
    applyTreeStructureStyles()
  })
  
  // 延迟一点再次确保样式应用
  setTimeout(() => {
    applyTreeStructureStyles()
  }, 200)
})

onUnmounted(() => {
  document.removeEventListener('click', handleGlobalClick)
})
</script>

<style scoped>
.device-list-panel {
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

/* 深色模式下的设备列表面板 */
:deep(.map-area.dark-mode) .device-list-panel,
.map-area.dark-mode .device-list-panel {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3) !important;
  backdrop-filter: blur(8px) !important;
}

:deep(.map-area.dark-mode) .device-list-header {
  background: rgba(30, 34, 42, 0.95) !important;
  border-bottom: 1px solid rgba(100, 149, 237, 0.3) !important;
}

:deep(.map-area.dark-mode) .device-list-title {
  color: #ffffff !important;
}

:deep(.map-area.dark-mode) .device-action-btn {
  background: rgba(40, 44, 52, 0.8) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  color: #ffffff !important;
}

:deep(.map-area.dark-mode) .device-action-btn:hover {
  background: rgba(26, 83, 255, 0.2) !important;
  border-color: #1A53FF !important;
}

:deep(.map-area.dark-mode) .device-action-btn.active {
  background: #1A53FF !important;
  border-color: #1A53FF !important;
}

:deep(.map-area.dark-mode) .selected-display-items {
  background: rgba(30, 34, 42, 0.8) !important;
  border-bottom: 1px solid rgba(100, 149, 237, 0.3) !important;
}

:deep(.map-area.dark-mode) .display-item-tag {
  background: rgba(40, 44, 52, 0.8) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  color: #ffffff !important;
}

:deep(.map-area.dark-mode) .device-search-container {
  background: rgba(30, 34, 42, 0.8) !important;
  border-bottom: 1px solid rgba(100, 149, 237, 0.3) !important;
}

:deep(.map-area.dark-mode) .device-search-input {
  background: rgba(40, 44, 52, 0.8) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  color: #ffffff !important;
}

:deep(.map-area.dark-mode) .device-search-input::placeholder {
  color: #a0a0a0 !important;
}

:deep(.map-area.dark-mode) .device-item {
  background: rgba(40, 44, 52, 0.5) !important;
  border-bottom: 1px solid rgba(100, 149, 237, 0.2) !important;
  color: #ffffff !important;
}

:deep(.map-area.dark-mode) .device-item:hover {
  background: rgba(26, 83, 255, 0.1) !important;
}

:deep(.map-area.dark-mode) .device-name {
  color: #ffffff !important;
}

:deep(.map-area.dark-mode) .device-type-tag {
  background: rgba(26, 83, 255, 0.2) !important;
  color: #7db8ff !important;
  border: 1px solid rgba(26, 83, 255, 0.4) !important;
}

:deep(.map-area.dark-mode) .device-status.online {
  color: #7db8ff !important;
}

:deep(.map-area.dark-mode) .device-status.offline {
  color: #f78989 !important;
}

:deep(.map-area.dark-mode) .device-ip {
  color: #a0a0a0 !important;
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

.device-list-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.selected-display-items {
  padding: 12px;
  border-bottom: 1px solid #e4e7ed;
  background: rgba(248, 249, 250, 0.8);
}

.display-items-list {
  display: flex;
  gap: 16px;
  align-items: center;
  overflow-x: auto;
}

.display-item-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  background: transparent;
  border: none;
  border-radius: 0;
  padding: 0;
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.display-item-tag:hover {
  background: transparent;
}

.tag-content {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tag-label {
  font-weight: 500;
}

.tag-stats-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px;
  border-radius: 3px;
  color: #909399;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tag-stats-btn:hover {
  background: rgba(144, 147, 153, 0.1);
  color: #606266;
}

.tag-stats-btn svg.rotated {
  transform: rotate(180deg);
}

.device-search-container {
  padding: 12px;
  border-bottom: 1px solid #e4e7ed;
  background: rgba(248, 249, 250, 0.8);
}

.device-search-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.device-search-input {
  flex: 1;
  height: 36px;
  padding: 0 40px 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  color: #606266;
  outline: none;
  transition: border-color 0.2s;
}

.device-search-input:focus {
  border-color: #1A53FF;
}

.device-search-input::placeholder {
  color: #c0c4cc;
}

.device-search-btn {
  position: absolute;
  right: 8px;
  width: 24px;
  height: 24px;
  background: none;
  border: none;
  cursor: pointer;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.device-search-btn:hover {
  background: #f5f7fa;
  color: #1A53FF;
}

.device-list-items {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.device-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid #f0f0f0;
}

.device-item:hover {
  background: rgba(26, 83, 255, 0.05);
}

.device-item:last-child {
  border-bottom: none;
}

.device-icon {
  width: 32px;
  height: 32px;
  background: #f8f9fa;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1A53FF;
  flex-shrink: 0;
}

.device-info {
  flex: 1;
  min-width: 0;
}

.device-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.device-details {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.device-type-tag {
  background: #e1f3ff;
  color: #1A53FF;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 11px;
}

.device-status {
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 500;
}

.device-status.online {
  background: #f0f9ff;
  color: #10b981;
}

.device-status.offline {
  background: #fef2f2;
  color: #f56565;
}

.device-ip {
  color: #909399;
  font-size: 11px;
}

.tree-view {
  padding: 8px 0;
}

.tree-group {
  cursor: pointer;
}

.tree-group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  transition: background-color 0.2s;
}

.tree-group-header:hover {
  background: rgba(26, 83, 255, 0.05);
}

.tree-expand-icon {
  transition: transform 0.2s;
  color: #909399;
}

.tree-expand-icon.expanded {
  transform: rotate(90deg);
}

.tree-group-icon {
  color: #1A53FF;
}

.tree-group-label {
  font-weight: 500;
  color: #303133;
  flex: 1;
}

.tree-group-count {
  font-size: 12px;
  color: #909399;
}

.tree-device-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid #f8f9fa;
}

.tree-device-item:hover {
  background: rgba(26, 83, 255, 0.05);
}

.tree-device-indent {
  width: 24px;
  flex-shrink: 0;
}

.tree-device-icon {
  color: #1A53FF;
  flex-shrink: 0;
}

.tree-device-info {
  flex: 1;
  min-width: 0;
}

.tree-device-name {
  font-size: 13px;
  color: #303133;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tree-device-details {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 设备统计弹窗样式 */
.device-stats-content {
  color: #303133;
  font-size: 14px;
}

.device-stats-expanded {
  padding: 12px;
  background: white;
  border-radius: 6px;
}

.stats-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
  color: #303133;
}

.device-icon {
  color: #1A53FF;
}

.device-type-title {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.total-count {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.count-label {
  font-size: 13px;
  color: #606266;
}

.count-number {
  font-size: 16px;
  font-weight: 600;
  color: #1A53FF;
}

.location-stats {
  padding: 12px 16px;
}

.location-group {
  margin-bottom: 12px;
}

.location-group:last-child {
  margin-bottom: 0;
}

.location-title {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.status-options {
  display: flex;
  gap: 16px;
}

.status-item {
  display: flex;
  align-items: center;
}

.status-checkbox {
  font-size: 12px;
}

/* 深色模式下的弹窗样式 */
:deep(.map-area.dark-mode) .device-stats-popover {
  background: rgba(40, 44, 52, 0.95) !important;
  border: 1px solid rgba(100, 149, 237, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
  color: #e8f4fd !important;
}

:deep(.map-area.dark-mode) .device-stats-content {
  color: #e8f4fd;
}

:deep(.map-area.dark-mode) .device-stats-expanded {
  background: transparent;
  color: #e8f4fd;
}

:deep(.map-area.dark-mode) .stats-header {
  color: #e8f4fd;
}

:deep(.map-area.dark-mode) .device-icon {
  color: #7db8ff;
}

:deep(.map-area.dark-mode) .device-type-title {
  color: #e8f4fd;
}

:deep(.map-area.dark-mode) .total-count {
  background: rgba(30, 34, 42, 0.6);
  border-bottom: 1px solid rgba(100, 149, 237, 0.3);
}

:deep(.map-area.dark-mode) .count-label {
  color: #b0c4de;
}

:deep(.map-area.dark-mode) .count-number {
  color: #7db8ff;
}

:deep(.map-area.dark-mode) .location-title {
  color: #e8f4fd;
}
</style> 