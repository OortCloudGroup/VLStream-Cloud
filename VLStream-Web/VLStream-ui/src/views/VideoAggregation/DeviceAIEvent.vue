<template>
  <div class="device-ai-event">
    <div class="ai-event-container">
      <!-- 设备信息头部 -->
      <div class="device-info-header">
        <h2 class="device-title">{{ deviceInfo?.deviceName || 'XXX摄像机' }}</h2>
        <div class="device-details">
          <div class="detail-item">
            <span class="detail-label">设备型号</span>
            <span class="detail-value">{{ deviceInfo?.deviceId || 'N931' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">设备ID</span>
            <span class="detail-value">{{ deviceInfo?.deviceId || '616891' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">设备类型</span>
            <span class="detail-value">{{ deviceInfo?.tag || '97球机' }}</span>
          </div>
        </div>
      </div>

      <!-- 模型调用按钮 -->
      <div class="model-section">
        <h3 class="section-title">本地模型</h3>
        <el-button type="primary" class="model-call-btn" @click="openModelMarket">
          <el-icon><ShoppingBag /></el-icon>
          模型超市
        </el-button>
      </div>

      <!-- AI检测配置 -->
      <div class="ai-config-section">
        <div class="ai-config-left">
          <!-- 检测类型列表 -->
          <div class="detection-list">
            <div class="detection-item" v-for="(item, index) in detectionItems" :key="index">
              <span class="detection-name">{{ item.name }}</span>
              <el-button 
                size="small" 
                text 
                class="remove-btn"
                @click="removeDetection(index)"
              >
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </div>
          
          <!-- 添加检测项目 -->
          <div class="add-detection">
            <el-button 
              type="primary" 
              size="small" 
              @click="showAddDetection"
              class="add-detection-btn"
            >
              <el-icon><Plus /></el-icon>
              添加检测项目
            </el-button>
          </div>
        </div>

        <div class="ai-config-right">
          <!-- 配置标签页 -->
          <div class="config-tabs">
            <el-tabs v-model="activeTab" class="config-tab-container">
              <!-- 异常检测标签页 -->
              <el-tab-pane label="异常检测" name="detection">
                <div class="detection-config">
                  <!-- 音频输入异常 -->
                  <div class="config-item">
                    <el-checkbox v-model="aiConfig.audioInput" class="config-checkbox">
                      音频输入异常
                    </el-checkbox>
                  </div>
                  
                  <!-- 声强增升 -->
                  <div class="config-item">
                    <el-checkbox v-model="aiConfig.soundEnhancement" class="config-checkbox">
                      声强增升
                    </el-checkbox>
                  </div>
                  
                  <!-- 灵敏度1 -->
                  <div class="config-item">
                    <div class="slider-container">
                      <label class="slider-label">灵敏度</label>
                      <div class="slider-wrapper">
                        <el-slider 
                          v-model="aiConfig.sensitivity1" 
                          :min="0" 
                          :max="100"
                          class="custom-slider"
                        />
                        <span class="slider-value">{{ aiConfig.sensitivity1 }}</span>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 声音强度阈值 -->
                  <div class="config-item">
                    <div class="slider-container">
                      <label class="slider-label">声音强度阈值</label>
                      <div class="slider-wrapper">
                        <el-slider 
                          v-model="aiConfig.soundThreshold" 
                          :min="0" 
                          :max="100"
                          class="custom-slider"
                        />
                        <span class="slider-value">{{ aiConfig.soundThreshold }}</span>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 声强降降 -->
                  <div class="config-item">
                    <el-checkbox v-model="aiConfig.soundDecrease" class="config-checkbox">
                      声强降降
                    </el-checkbox>
                  </div>
                  
                  <!-- 灵敏度2 -->
                  <div class="config-item">
                    <div class="slider-container">
                      <label class="slider-label">灵敏度</label>
                      <div class="slider-wrapper">
                        <el-slider 
                          v-model="aiConfig.sensitivity2" 
                          :min="0" 
                          :max="100"
                          class="custom-slider"
                        />
                        <span class="slider-value">{{ aiConfig.sensitivity2 }}</span>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 实时音量 -->
                  <div class="config-item">
                    <div class="realtime-audio">
                      <label class="audio-label">实时音量</label>
                      <div class="audio-waveform">
                        <canvas ref="waveformCanvas" class="waveform-canvas"></canvas>
                      </div>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
              
              <!-- 布防时间标签页 -->
              <el-tab-pane label="布防时间" name="schedule">
                <div class="schedule-config">
                  <!-- 布防模式 -->
                  <div class="schedule-mode">
                    <div class="mode-item">
                      <el-radio-group v-model="scheduleConfig.mode" class="mode-options">
                        <el-radio label="always">全天布防</el-radio>
                        <el-radio label="custom">自定义时间</el-radio>
                        <el-radio label="disable">关闭布防</el-radio>
                      </el-radio-group>
                    </div>
                  </div>
                  
                  <!-- 自定义时间设置 -->
                  <div v-if="scheduleConfig.mode === 'custom'" class="custom-schedule">
                    <!-- 星期选择 -->
                    <div class="week-selector">
                      <label class="schedule-label">布防日期</label>
                      <div class="week-options">
                        <el-checkbox-group v-model="scheduleConfig.selectedDays" class="day-checkboxes">
                          <el-checkbox 
                            v-for="day in weekDays" 
                            :key="day.value"
                            :label="day.value"
                            class="day-checkbox"
                          >
                            {{ day.label }}
                          </el-checkbox>
                        </el-checkbox-group>
                      </div>
                    </div>
                    
                    <!-- 时间段设置 -->
                    <div class="time-periods">
                      <label class="schedule-label">布防时间段</label>
                      <div class="time-period-list">
                        <div 
                          v-for="(period, index) in scheduleConfig.timePeriods" 
                          :key="index"
                          class="time-period-item"
                        >
                          <div class="time-inputs">
                            <el-time-picker
                              v-model="period.startTime"
                              placeholder="开始时间"
                              format="HH:mm"
                              value-format="HH:mm"
                              class="time-input"
                            />
                            <span class="time-separator">至</span>
                            <el-time-picker
                              v-model="period.endTime"
                              placeholder="结束时间"
                              format="HH:mm"
                              value-format="HH:mm"
                              class="time-input"
                            />
                          </div>
                          <el-button 
                            type="danger" 
                            text 
                            size="small"
                            @click="removeTimePeriod(index)"
                            v-if="scheduleConfig.timePeriods.length > 1"
                          >
                            删除
                          </el-button>
                        </div>
                      </div>
                      
                      <!-- 添加时间段 -->
                      <div class="add-time-period">
                        <el-button 
                          type="primary" 
                          text 
                          size="small"
                          @click="addTimePeriod"
                          :disabled="scheduleConfig.timePeriods.length >= 4"
                        >
                          <el-icon><Plus /></el-icon>
                          添加时间段
                        </el-button>
                        <span class="period-hint" v-if="scheduleConfig.timePeriods.length >= 4">
                          最多可添加4个时间段
                        </span>
                      </div>
                    </div>
                    
                    <!-- 时间段可视化 -->
                    <div class="time-visualization">
                      <label class="schedule-label">时间段预览</label>
                      <div class="time-timeline">
                        <div class="timeline-hours">
                          <div 
                            v-for="hour in 24" 
                            :key="hour"
                            class="hour-mark"
                            :class="{ 
                              active: isHourInPeriods(hour - 1),
                              'even-hour': (hour - 1) % 2 === 0 
                            }"
                          >
                            <span class="hour-label" v-if="(hour - 1) % 4 === 0">
                              {{ (hour - 1).toString().padStart(2, '0') }}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 布防状态提示 -->
                  <div class="schedule-status">
                    <div class="status-info">
                      <el-icon class="status-icon">
                        <el-icon v-if="scheduleConfig.mode === 'always'" style="color: #67c23a;">
                          <Check />
                        </el-icon>
                        <el-icon v-else-if="scheduleConfig.mode === 'custom'" style="color: #e6a23c;">
                          <Clock />
                        </el-icon>
                        <el-icon v-else style="color: #f56c6c;">
                          <Close />
                        </el-icon>
                      </el-icon>
                      <span class="status-text">
                        {{ getScheduleStatusText() }}
                      </span>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
              
              <!-- 联动方式标签页 -->
              <el-tab-pane label="联动方式" name="linkage">
                <div class="linkage-config">
                  <!-- 常规联动 -->
                  <div class="linkage-group">
                    <h5 class="group-title">常规联动</h5>
                    <el-checkbox-group v-model="linkageConfig.normal" class="linkage-options">
                      <el-checkbox label="general">常规</el-checkbox>
                      <el-checkbox label="mail">邮件</el-checkbox>
                      <el-checkbox label="upload">上传</el-checkbox>
                    </el-checkbox-group>
                  </div>
                  
                  <!-- 报警输出 -->
                  <div class="linkage-group">
                    <h5 class="group-title">报警输出</h5>
                    <div class="channel-config">
                      <el-tag 
                        v-for="channel in linkageConfig.alarmOutput.channels" 
                        :key="channel"
                        closable
                        @close="removeAlarmChannel(channel)"
                      >
                        {{ channel }}
                      </el-tag>
                      <el-input 
                        v-model="newAlarmChannel"
                        placeholder="A->1"
                        size="small"
                        style="width: 80px; margin-left: 8px;"
                        @keyup.enter="addAlarmChannel"
                      />
                    </div>
                  </div>
                  
                  <!-- 录像联动 -->
                  <div class="linkage-group">
                    <h5 class="group-title">录像联动</h5>
                    <div class="channel-config">
                      <el-tag 
                        v-for="channel in linkageConfig.recordLinkage.channels" 
                        :key="channel"
                        closable
                        @close="removeRecordChannel(channel)"
                      >
                        {{ channel }}
                      </el-tag>
                      <el-input 
                        v-model="newRecordChannel"
                        placeholder="A1"
                        size="small"
                        style="width: 60px; margin-left: 8px;"
                        @keyup.enter="addRecordChannel"
                      />
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="ai-footer">
        <el-button type="primary" @click="saveAIConfig" class="save-btn">保存</el-button>
      </div>
    </div>

    <!-- 添加检测项目对话框 -->
    <el-dialog
      v-model="addDetectionDialogVisible"
      title="添加检测项目"
      width="500px"
    >
      <div class="add-detection-content">
        <el-form :model="newDetectionForm" label-width="100px">
          <el-form-item label="检测类型">
            <el-select v-model="newDetectionForm.type" placeholder="请选择检测类型">
              <el-option label="区域入侵侦测" value="区域入侵侦测" />
              <el-option label="越界侦测" value="越界侦测" />
              <el-option label="离开区域侦测" value="离开区域侦测" />
              <el-option label="物品遗留侦测" value="物品遗留侦测" />
              <el-option label="物品拿取侦测" value="物品拿取侦测" />
              <el-option label="人脸识别" value="人脸识别" />
              <el-option label="车牌识别" value="车牌识别" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="检测名称">
            <el-input v-model="newDetectionForm.name" placeholder="请输入检测名称" />
          </el-form-item>
          
          <el-form-item label="灵敏度">
            <el-slider v-model="newDetectionForm.sensitivity" :min="1" :max="10" />
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <el-button @click="addDetectionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddDetection">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ShoppingBag, Close, Plus, Check, Clock } from '@element-plus/icons-vue'

const props = defineProps({
  deviceInfo: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['save', 'cancel', 'open-model-market'])

// 响应式数据
const detectionItems = ref([
  { name: '音频异常侦测' },
  { name: '区域入侵侦测' },
  { name: '越界侦测' },
  { name: '离开区域侦测' },
  { name: '物品遗留侦测' },
  { name: '物品拿取侦测' }
])

// 当前激活的标签页
const activeTab = ref('detection')

// 实时音量波形相关
const waveformCanvas = ref(null)
let animationId = null
let waveformData = []

// AI配置数据
const aiConfig = reactive({
  audioInput: true,
  soundEnhancement: true,
  sensitivity1: 50,
  soundThreshold: 50,
  soundDecrease: true,
  sensitivity2: 50
})

// 星期数据
const weekDays = [
  { label: '周一', value: '1' },
  { label: '周二', value: '2' },
  { label: '周三', value: '3' },
  { label: '周四', value: '4' },
  { label: '周五', value: '5' },
  { label: '周六', value: '6' },
  { label: '周日', value: '0' }
]

// 布防时间配置
const scheduleConfig = reactive({
  mode: 'custom', // always, custom, disable
  selectedDays: ['1', '2', '3', '4', '5'], // 工作日
  timePeriods: [
    { startTime: '08:00', endTime: '18:00' }
  ]
})

// 联动方式配置
const linkageConfig = reactive({
  normal: ['general'],
  alarmOutput: {
    channels: ['A->1']
  },
  recordLinkage: {
    channels: ['A1']
  }
})

// 新增通道输入
const newAlarmChannel = ref('')
const newRecordChannel = ref('')

// 添加检测项目相关
const addDetectionDialogVisible = ref(false)
const newDetectionForm = reactive({
  type: '',
  name: '',
  sensitivity: 5
})

// 方法
const removeDetection = (index) => {
  detectionItems.value.splice(index, 1)
  ElMessage.success('检测项目已删除')
}

// 布防时间相关方法
const addTimePeriod = () => {
  if (scheduleConfig.timePeriods.length < 4) {
    scheduleConfig.timePeriods.push({
      startTime: '09:00',
      endTime: '17:00'
    })
  }
}

const removeTimePeriod = (index) => {
  if (scheduleConfig.timePeriods.length > 1) {
    scheduleConfig.timePeriods.splice(index, 1)
  }
}

// 判断某个小时是否在时间段内
const isHourInPeriods = (hour) => {
  if (scheduleConfig.mode !== 'custom') return false
  
  return scheduleConfig.timePeriods.some(period => {
    if (!period.startTime || !period.endTime) return false
    
    const startHour = parseInt(period.startTime.split(':')[0])
    const endHour = parseInt(period.endTime.split(':')[0])
    
    if (startHour <= endHour) {
      return hour >= startHour && hour < endHour
    } else {
      // 跨天的情况
      return hour >= startHour || hour < endHour
    }
  })
}

// 获取布防状态描述
const getScheduleStatusText = () => {
  switch (scheduleConfig.mode) {
    case 'always':
      return '全天24小时布防'
    case 'custom':
      if (scheduleConfig.timePeriods.length === 0) {
        return '未设置布防时间'
      }
      const dayCount = scheduleConfig.selectedDays.length
      const periodCount = scheduleConfig.timePeriods.length
      return `${dayCount}天/周，${periodCount}个时间段布防`
    case 'disable':
      return '布防已关闭'
    default:
      return '未知状态'
  }
}

const removeAlarmChannel = (channel) => {
  const index = linkageConfig.alarmOutput.channels.indexOf(channel)
  if (index > -1) {
    linkageConfig.alarmOutput.channels.splice(index, 1)
  }
}

const addAlarmChannel = () => {
  const channel = newAlarmChannel.value.trim()
  if (channel && !linkageConfig.alarmOutput.channels.includes(channel)) {
    linkageConfig.alarmOutput.channels.push(channel)
    newAlarmChannel.value = ''
  }
}

const removeRecordChannel = (channel) => {
  const index = linkageConfig.recordLinkage.channels.indexOf(channel)
  if (index > -1) {
    linkageConfig.recordLinkage.channels.splice(index, 1)
  }
}

const addRecordChannel = () => {
  const channel = newRecordChannel.value.trim()
  if (channel && !linkageConfig.recordLinkage.channels.includes(channel)) {
    linkageConfig.recordLinkage.channels.push(channel)
    newRecordChannel.value = ''
  }
}

const showAddDetection = () => {
  // 重置表单
  newDetectionForm.type = ''
  newDetectionForm.name = ''
  newDetectionForm.sensitivity = 5
  addDetectionDialogVisible.value = true
}

const confirmAddDetection = () => {
  if (!newDetectionForm.type) {
    ElMessage.warning('请选择检测类型')
    return
  }
  
  const name = newDetectionForm.name || newDetectionForm.type
  
  // 检查是否已存在
  const exists = detectionItems.value.some(item => item.name === name)
  if (exists) {
    ElMessage.warning('该检测项目已存在')
    return
  }
  
  detectionItems.value.push({
    name: name,
    type: newDetectionForm.type,
    sensitivity: newDetectionForm.sensitivity
  })
  
  addDetectionDialogVisible.value = false
  ElMessage.success('检测项目添加成功')
}

const openModelMarket = () => {
  emit('open-model-market')
}

const saveAIConfig = () => {
  const configData = {
    deviceId: props.deviceInfo.id,
    detectionItems: detectionItems.value,
    aiConfig: { ...aiConfig },
    scheduleConfig: { ...scheduleConfig },
    linkageConfig: { ...linkageConfig }
  }
  
  emit('save', configData)
  ElMessage.success('AI配置保存成功')
}

const handleCancel = () => {
  emit('cancel')
}

// 实时音量波形图绘制
const drawWaveform = () => {
  if (!waveformCanvas.value) return
  
  const canvas = waveformCanvas.value
  const ctx = canvas.getContext('2d')
  const width = canvas.width
  const height = canvas.height
  
  // 清除画布
  ctx.clearRect(0, 0, width, height)
  
  // 生成随机波形数据（模拟实时音量）
  if (waveformData.length > 100) {
    waveformData.shift()
  }
  waveformData.push(Math.sin(Date.now() / 200) * 0.5 + Math.random() * 0.3)
  
  // 绘制波形
  ctx.strokeStyle = '#1A53FF'
  ctx.lineWidth = 2
  ctx.beginPath()
  
  const step = width / waveformData.length
  
  waveformData.forEach((value, index) => {
    const x = index * step
    const y = height / 2 + value * height * 0.3
    
    if (index === 0) {
      ctx.moveTo(x, y)
    } else {
      ctx.lineTo(x, y)
    }
  })
  
  ctx.stroke()
  
  // 继续动画
  animationId = requestAnimationFrame(drawWaveform)
}

// 初始化波形图
const initWaveform = () => {
  if (waveformCanvas.value) {
    const canvas = waveformCanvas.value
    canvas.width = canvas.offsetWidth
    canvas.height = canvas.offsetHeight
    
    // 初始化波形数据
    waveformData = Array.from({ length: 50 }, () => Math.random() * 0.2)
    
    drawWaveform()
  }
}

// 组件挂载时初始化
onMounted(() => {
  setTimeout(() => {
    initWaveform()
  }, 100)
})

// 组件卸载时清理
onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
})
</script>

<style scoped lang="scss">
.device-ai-event {
  padding: 20px;
  
  .ai-event-container {
    max-width: 1200px;
    margin: 0;
  }
  
  .device-info-header {
    background: #f8f9fa;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 30px;
    
    .device-title {
      margin: 0 0 16px 0;
      font-size: 20px;
      font-weight: 600;
      color: #333;
    }
    
    .device-details {
      display: flex;
      gap: 30px;
      
      .detail-item {
        display: flex;
        gap: 8px;
        
        .detail-label {
          color: #666;
          font-weight: 500;
        }
        
        .detail-value {
          color: #333;
        }
      }
    }
  }
  
  .model-section {
    margin-bottom: 30px;
    
    .section-title {
      margin: 0 0 16px 0;
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
    
    .model-call-btn {
      .el-icon {
        margin-right: 8px;
      }
    }
  }
  
  .ai-config-section {
    display: flex;
    gap: 30px;
    margin-bottom: 30px;
    
    .ai-config-left {
      width: 300px;
      flex-shrink: 0;
      
      .detection-list {
        border: 1px solid #ebeef5;
        border-radius: 6px;
        padding: 16px;
        margin-bottom: 16px;
        min-height: 200px;
        
        .detection-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 8px 0;
          border-bottom: 1px solid #f5f5f5;
          
          &:last-child {
            border-bottom: none;
          }
          
          .detection-name {
            color: #333;
            font-size: 14px;
          }
          
          .remove-btn {
            color: #f56c6c;
            
            &:hover {
              background-color: #fef0f0;
            }
          }
        }
      }
      
      .add-detection {
        .add-detection-btn {
          width: 100%;
          
          .el-icon {
            margin-right: 8px;
          }
        }
      }
    }
    
    .ai-config-right {
      flex: 1;
      
      .config-tabs {
        .config-tab-container {
          :deep(.el-tabs__header) {
            margin: 0 0 20px 0;
            
            .el-tabs__nav {
              border: none;
            }
            
            .el-tabs__item {
              padding: 12px 20px;
              font-size: 14px;
              font-weight: 500;
              color: #666;
              border-bottom: 2px solid transparent;
              
              &.is-active {
                color: #1A53FF;
                border-bottom-color: #1A53FF;
              }
              
              &:hover {
                color: #1A53FF;
              }
            }
            
            .el-tabs__active-bar {
              display: none;
            }
          }
          
          :deep(.el-tabs__content) {
            padding: 0;
          }
        }
        
        .detection-config {
          .config-item {
            margin-bottom: 24px;
            
            .config-checkbox {
              :deep(.el-checkbox__label) {
                font-size: 14px;
                color: #333;
                font-weight: 500;
              }
              
              :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
                background-color: #1A53FF;
                border-color: #1A53FF;
              }
            }
            
            .slider-container {
              .slider-label {
                display: block;
                margin-bottom: 12px;
                font-size: 14px;
                color: #333;
                font-weight: 500;
              }
              
              .slider-wrapper {
                display: flex;
                align-items: center;
                gap: 16px;
                
                .custom-slider {
                  flex: 1;
                  
                  :deep(.el-slider__runway) {
                    height: 6px;
                    background-color: #f0f2f5;
                  }
                  
                  :deep(.el-slider__bar) {
                    background-color: #1A53FF;
                  }
                  
                  :deep(.el-slider__button) {
                    width: 20px;
                    height: 20px;
                    border: 2px solid #1A53FF;
                    background-color: white;
                  }
                }
                
                .slider-value {
                  min-width: 30px;
                  text-align: right;
                  font-size: 14px;
                  color: #333;
                  font-weight: 500;
                }
              }
            }
            
            .realtime-audio {
              .audio-label {
                display: block;
                margin-bottom: 12px;
                font-size: 14px;
                color: #333;
                font-weight: 500;
              }
              
              .audio-waveform {
                .waveform-canvas {
                  width: 100%;
                  height: 80px;
                  background-color: #f8f9fa;
                  border-radius: 8px;
                  border: 1px solid #e9ecef;
                }
              }
            }
          }
        }
        
        .schedule-config {
          .schedule-mode {
            margin-bottom: 32px;
            
            .mode-item {
              .mode-options {
                display: flex;
                gap: 24px;
                
                :deep(.el-radio) {
                  margin-right: 0;
                  
                  .el-radio__input.is-checked .el-radio__inner {
                    background-color: #1A53FF;
                    border-color: #1A53FF;
                  }
                  
                  .el-radio__label {
                    font-size: 14px;
                    font-weight: 500;
                  }
                }
              }
            }
          }
          
          .custom-schedule {
            .schedule-label {
              display: block;
              margin-bottom: 16px;
              font-size: 14px;
              color: #333;
              font-weight: 500;
            }
            
            .week-selector {
              margin-bottom: 32px;
              
              .week-options {
                .day-checkboxes {
                  display: flex;
                  gap: 16px;
                  flex-wrap: wrap;
                  
                  .day-checkbox {
                    margin-right: 0;
                    
                    :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
                      background-color: #1A53FF;
                      border-color: #1A53FF;
                    }
                    
                    :deep(.el-checkbox__label) {
                      font-size: 14px;
                      color: #333;
                      font-weight: 500;
                    }
                  }
                }
              }
            }
            
            .time-periods {
              margin-bottom: 32px;
              
              .time-period-list {
                margin-bottom: 16px;
                
                .time-period-item {
                  display: flex;
                  align-items: center;
                  gap: 16px;
                  margin-bottom: 12px;
                  padding: 16px;
                  background: #f8f9fa;
                  border-radius: 8px;
                  border: 1px solid #e9ecef;
                  
                  .time-inputs {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    flex: 1;
                    
                    .time-input {
                      width: 120px;
                      
                      :deep(.el-input__wrapper) {
                        border-radius: 6px;
                      }
                    }
                    
                    .time-separator {
                      color: #666;
                      font-size: 14px;
                      font-weight: 500;
                    }
                  }
                }
              }
              
              .add-time-period {
                display: flex;
                align-items: center;
                gap: 12px;
                
                .period-hint {
                  font-size: 12px;
                  color: #999;
                }
              }
            }
            
            .time-visualization {
              margin-bottom: 24px;
              
              .time-timeline {
                .timeline-hours {
                  display: flex;
                  border: 1px solid #e9ecef;
                  border-radius: 8px;
                  overflow: hidden;
                  
                  .hour-mark {
                    flex: 1;
                    height: 40px;
                    position: relative;
                    border-right: 1px solid #f0f2f5;
                    transition: all 0.3s;
                    
                    &:last-child {
                      border-right: none;
                    }
                    
                    &.even-hour {
                      background-color: #fafbfc;
                    }
                    
                    &.active {
                      background-color: #1A53FF;
                      color: white;
                    }
                    
                    .hour-label {
                      position: absolute;
                      bottom: 2px;
                      left: 2px;
                      font-size: 10px;
                      color: #666;
                      font-weight: 500;
                    }
                    
                    &.active .hour-label {
                      color: white;
                    }
                  }
                }
              }
            }
          }
          
          .schedule-status {
            padding: 16px;
            background: #f8f9fa;
            border-radius: 8px;
            border: 1px solid #e9ecef;
            
            .status-info {
              display: flex;
              align-items: center;
              gap: 12px;
              
              .status-icon {
                font-size: 18px;
              }
              
              .status-text {
                font-size: 14px;
                color: #333;
                font-weight: 500;
              }
            }
          }
        }
        
        .linkage-config {
          .linkage-group {
            margin-bottom: 20px;
            
            .group-title {
              margin: 0 0 12px 0;
              font-size: 14px;
              font-weight: 500;
              color: #333;
            }
            
            .linkage-options {
              display: flex;
              gap: 16px;
            }
            
            .channel-config {
              display: flex;
              align-items: center;
              flex-wrap: wrap;
              gap: 8px;
            }
          }
        }
      }
    }
  }
  
  .ai-footer {
    display: flex;
    justify-content: center;
    padding: 30px 0;
    border-top: 1px solid #ebeef5;
    
    .save-btn {
      width: 200px;
      height: 44px;
      font-size: 16px;
      font-weight: 500;
      background-color: #1A53FF;
      border-color: #1A53FF;
      border-radius: 22px;
      
      &:hover {
        background-color: #3d70ff;
        border-color: #3d70ff;
      }
    }
  }
  
  // 添加检测项目对话框样式
  .add-detection-content {
    .el-form-item {
      margin-bottom: 20px;
    }
  }
}

// 响应式设计
@media (max-width: 1024px) {
  .device-ai-event {
    .ai-config-section {
      flex-direction: column;
      
      .ai-config-left {
        width: 100%;
      }
    }
    
    .device-details {
      flex-direction: column;
      gap: 12px;
    }
    
    .time-grid {
      grid-template-columns: repeat(8, 1fr) !important;
    }
  }
}

@media (max-width: 768px) {
  .device-ai-event {
    .time-grid {
      grid-template-columns: repeat(6, 1fr) !important;
    }
    
    .linkage-options {
      flex-direction: column !important;
      gap: 8px !important;
    }
    
    .period-options {
      flex-direction: column !important;
      gap: 8px !important;
    }
  }
}
</style> 