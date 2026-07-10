<template>
  <div class="preset-panel-container">
    <!-- 预制点列表 -->
    <div 
      class="preset-container"
      :class="{ 
        'has-osd-settings': showOSDSettings
      }"
    >
      <div class="preset-list">
        <div 
          v-for="preset in presetPoints" 
          :key="preset.id"
          class="preset-item"
          :class="{ 'selected': selectedPresetId === preset.id }"
          @click="handlePresetItemClick(preset)"
        >
          <span class="preset-name">{{ preset.name }}</span>
          <el-icon 
            class="preset-settings-icon" 
            @click.stop="toggleOSDSettings(preset)"
            v-if="selectedPresetId === preset.id"
          >
            <Setting />
          </el-icon>
        </div>
      </div>
    </div>

    <!-- OSD显示设置区域 - 点击齿轮后显示 -->
    <div v-if="showOSDSettings" class="osd-display-container">
      <div class="osd-display-settings">
        <!-- 显示选项 -->
        <div class="display-options">
          <div class="option-row">
            <el-checkbox v-model="osdSettings.showName">显示名称</el-checkbox>
            <el-checkbox v-model="osdSettings.showDate">显示日期</el-checkbox>
            <el-checkbox v-model="osdSettings.showWeek">显示星期</el-checkbox>
          </div>
        </div>

        <!-- 通道名称 -->
        <div class="channel-name-section">
          <label class="channel-label">通道名称</label>
          <el-input 
            v-model="osdSettings.channelName" 
            placeholder="请输入通道名称"
            class="channel-input"
          />
        </div>

        <!-- 时间格式 -->
        <div class="time-format-section">
          <label class="format-label">时间格式</label>
          <el-select v-model="osdSettings.timeFormat" class="format-select">
            <el-option label="24小时制" value="24h" />
            <el-option label="12小时制" value="12h" />
          </el-select>
        </div>

        <!-- 日期格式 -->
        <div class="date-format-section">
          <label class="format-label">日期格式</label>
          <el-select v-model="osdSettings.dateFormat" class="format-select">
            <el-option label="XXXX年XX月XX日" value="yyyy-mm-dd" />
            <el-option label="XX/XX/XXXX" value="mm-dd-yyyy" />
            <el-option label="XXXX-XX-XX" value="yyyy-mm-dd-dash" />
          </el-select>
        </div>

        <!-- 字符叠加 -->
        <div class="text-overlay-section">
          <label class="overlay-label">字符叠加</label>
          <div class="overlay-inputs">
            <div class="overlay-row">
              <el-checkbox :value="true">1</el-checkbox>
              <el-input 
                v-model="osdSettings.textOverlay1" 
                placeholder="请输入"
                class="overlay-input"
              />
            </div>
            <div class="overlay-row">
              <el-checkbox :value="true">2</el-checkbox>
              <el-input 
                v-model="osdSettings.textOverlay2" 
                placeholder="请输入"
                class="overlay-input"
              />
            </div>
            <div class="overlay-row">
              <el-checkbox :value="true">3</el-checkbox>
              <el-input 
                v-model="osdSettings.textOverlay3" 
                placeholder="请输入"
                class="overlay-input"
              />
            </div>
          </div>
        </div>

        <!-- 保存按钮 -->
        <div class="osd-save-section">
          <el-button type="primary" @click="saveOSDSettings" class="save-btn">
            保存OSD设置
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, toRefs } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'

// Props
const props = defineProps({
  presetPoints: {
    type: Array,
    default: () => []
  },
  showOsdFeatures: {
    type: Boolean,
    default: true
  }
})

// 解构props以便在模板中使用
const { showOsdFeatures } = toRefs(props)

// Emits
const emit = defineEmits(['preset-click', 'osd-settings-save'])

// 响应式数据
const selectedPresetId = ref(null)
const showOSDSettings = ref(false)

// OSD设置参数
const osdSettings = ref({
  showName: true,
  showDate: true,
  showWeek: true,
  channelName: 'IP4pme',
  timeFormat: '24h',
  dateFormat: 'yyyy-mm-dd',
  textOverlay1: '',
  textOverlay2: '',
  textOverlay3: ''
})

// 处理预置点项目点击
const handlePresetItemClick = (preset) => {
  // 如果点击的是已选中的项目，则取消选中；否则选中当前项目
  if (selectedPresetId.value === preset.id) {
    selectedPresetId.value = null
    showOSDSettings.value = false
  } else {
    selectedPresetId.value = preset.id
    showOSDSettings.value = false // 先隐藏OSD设置面板
  }
  
  // 触发预置点点击事件
  emit('preset-click', preset)
}

// 切换OSD设置显示
const toggleOSDSettings = (preset) => {
  showOSDSettings.value = !showOSDSettings.value
  console.log('切换OSD设置:', preset.name, showOSDSettings.value)
}

// 保存OSD设置
const saveOSDSettings = () => {
  console.log('保存OSD设置:', osdSettings.value)
  ElMessage.success('OSD设置已保存')
  emit('osd-settings-save', osdSettings.value)
}
</script>

<style scoped>
.preset-panel-container {
  display: flex;
  align-items: flex-start;
}

/* 预制点列表 */
.preset-container {
  width: 200px;
  height: 762px;
  background: white;
  border-radius: 0 6px 6px 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  margin: 0 20px 0 0;
  display: flex;
  flex-direction: column;
  border-left: none; /* 移除左边框，与PTZ无缝连接 */
}

/* 当显示OSD设置时，预制点列表移除右侧圆角 */
.preset-container.has-osd-settings {
  border-radius: 0;
  margin: 0;
}

.preset-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.preset-item {
  padding: 12px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s ease;
  border-bottom: 1px solid #f5f7fa;
  cursor: pointer;
}

.preset-item:hover {
  background-color: #f5f7fa;
}

.preset-item.selected {
  background-color: #e1f3ff;
  border-color: #b3d8ff;
}

.preset-item:last-child {
  border-bottom: none;
}

.preset-name {
  font-size: 14px;
  color: #606266;
  flex: 1;
  user-select: none;
}

.preset-item.selected .preset-name {
  color: #1A53FF;
  font-weight: 500;
}

.preset-name:hover {
  color: #409eff;
}

.preset-settings-icon {
  color: #909399;
  cursor: pointer;
  font-size: 16px;
  margin-left: 8px;
  transition: all 0.3s ease;
}

.preset-settings-icon:hover {
  color: #1A53FF;
  transform: scale(1.1);
}

/* OSD显示设置区域 */
.osd-display-container {
  width: 384px;
  height: 762px;
  background: white;
  border-radius: 0 6px 6px 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin: 0 20px 0 0;
  overflow-y: auto;
}

.osd-display-settings {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.display-options {
  margin-bottom: 16px;
}

.option-row {
  display: flex;
  flex-direction: row;
  gap: 16px;
  align-items: center;
}

.channel-name-section,
.time-format-section,
.date-format-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.channel-label,
.format-label,
.overlay-label {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}

.channel-input,
.format-select {
  width: 100%;
}

.text-overlay-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overlay-inputs {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overlay-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.overlay-input {
  flex: 1;
}

.osd-save-section {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.save-btn {
  background: #1A53FF;
  color: white;
  border: none;
  padding: 8px 24px;
  border-radius: 4px;
  min-width: 120px;
}

.save-btn:hover {
  background: #4084ff;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .preset-panel-container {
    flex-direction: column;
  }
  
  .preset-container,
  .osd-display-container {
    width: 100%;
    margin: 0 0 20px 0;
  }
}
</style> 