<template>
  <div class="device-time-strategy">
    <div class="config-container">
      <!-- 标签页 -->
      <div class="config-tabs">
        <div 
          class="tab-item" 
          :class="{ active: activeTab === 'time' }"
          @click="activeTab = 'time'"
        >
          时间策略
        </div>
        <div 
          class="tab-item" 
          :class="{ active: activeTab === 'event' }"
          @click="activeTab = 'event'"
        >
          事件策略
        </div>
      </div>

      <!-- 时间策略内容 -->
      <div v-if="activeTab === 'time'" class="config-tab-content">
        <!-- 录制设置 -->
        <div class="config-section">
          <h3 class="config-title">录制设置</h3>
          <div class="config-buttons">
            <el-button 
              :type="configForm.deleteAll ? 'danger' : ''" 
              :plain="!configForm.deleteAll"
              @click="toggleDeleteAll"
            >
              删除
            </el-button>
            <el-button 
              :type="configForm.deleteAllTime ? 'danger' : ''" 
              :plain="!configForm.deleteAllTime"
              @click="toggleDeleteAllTime"
            >
              删除全部
            </el-button>
          </div>
        </div>

        <!-- 录制设置选项 -->
        <div class="config-section">
          <div class="record-options">
            <el-button 
              :type="configForm.recordSettings.everyday ? 'primary' : ''" 
              :plain="!configForm.recordSettings.everyday"
              @click="setRecordType('everyday')"
            >
              每天
            </el-button>
            <el-button 
              :type="configForm.recordSettings.weekly ? 'primary' : ''" 
              :plain="!configForm.recordSettings.weekly"
              @click="setRecordType('weekly')"
            >
              每周
            </el-button>
            <el-button 
              :type="configForm.recordSettings.monthly ? 'primary' : ''" 
              :plain="!configForm.recordSettings.monthly"
              @click="setRecordType('monthly')"
            >
              每月
            </el-button>
            <el-button 
              :type="configForm.recordSettings.custom ? 'primary' : ''" 
              :plain="!configForm.recordSettings.custom"
              @click="setRecordType('custom')"
            >
              自定义
            </el-button>
          </div>
        </div>

        <!-- 时间表 -->
        <div class="config-section">
          <div class="schedule-container">
            <!-- 每天模式 -->
            <div v-if="configForm.recordSettings.everyday" class="daily-schedule">
              <h4 class="schedule-title">每天录制时间</h4>
              <!-- 时间轴标签 -->
              <div class="time-axis-daily">
                <div class="time-labels-daily">
                  <span v-for="labelIndex in timeLabelCount" :key="labelIndex" class="time-label-daily">
                    {{ (labelIndex - 1).toString().padStart(2, '0') }}
                  </span>
                </div>
              </div>
              <!-- 时间选择区域 -->
              <div class="time-selector-daily">
                <div 
                  class="time-track"
                  @mousedown="startDragDaily"
                  @mousemove="onDragDaily"
                  @mouseup="endDragDaily"
                  @mouseleave="endDragDaily"
                >
                  <div 
                    v-for="slot in timeSlotCount" 
                    :key="slot" 
                    class="time-segment"
                    :class="{ selected: isDailyTimeSelected(slot - 1) }"
                    :data-slot="slot - 1"
                  ></div>
                </div>
              </div>
            </div>

            <!-- 每周模式 -->
            <div v-if="configForm.recordSettings.weekly" class="weekly-schedule">
              <h4 class="schedule-title">每周录制时间</h4>
              <!-- 星期列表 -->
              <div class="week-rows">
                <div 
                  v-for="(dayName, dayKey) in weekDays" 
                  :key="dayKey" 
                  class="week-row"
                >
                  <div class="day-label-weekly">
                    <span>{{ dayName }}</span>
                  </div>
                  <div class="time-section-weekly">
                    <!-- 时间标签 -->
                    <div class="time-labels-weekly">
                      <span v-for="labelIndex in timeLabelCount" :key="labelIndex" class="time-label-weekly">
                        {{ (labelIndex - 1).toString().padStart(2, '0') }}
                      </span>
                    </div>
                    <!-- 时间轨道 -->
                    <div 
                      class="time-track"
                      @mousedown="startDragWeekly($event, dayKey)"
                      @mousemove="onDragWeekly($event, dayKey)"
                      @mouseup="endDragWeekly"
                      @mouseleave="endDragWeekly"
                    >
                      <div 
                        v-for="slot in timeSlotCount" 
                        :key="slot" 
                        class="time-segment"
                        :class="{ selected: isWeeklyTimeSelected(dayKey, slot - 1) }"
                        :data-slot="slot - 1"
                        :data-day="dayKey"
                      ></div>
                    </div>
                  </div>
                  <div class="copy-action-weekly">
                    <el-button 
                      size="small" 
                      text 
                      @click="showCopyDialog(dayKey)"
                      class="copy-btn"
                    >
                      <el-icon><DocumentCopy /></el-icon>
                    </el-button>
                  </div>
                </div>
              </div>
              
              <!-- 复制对话框 -->
              <el-dialog
                v-model="copyDialogVisible"
                title="复制时间设置"
                width="400px"
                :show-close="false"
              >
                <div class="copy-dialog-content">
                  <p>将 {{ sourceDay }} 的时间设置复制到：</p>
                  <div class="copy-day-options">
                    <el-checkbox-group v-model="copyTargetDays">
                      <el-checkbox 
                        v-for="(dayName, dayKey) in weekDays" 
                        :key="dayKey"
                        :label="dayKey"
                        :disabled="dayKey === sourceDayKey"
                        :class="{
                          'source-day': dayKey === sourceDayKey,
                          'has-time-setting': timeStrategyData.weeklyTimes[dayKey].length > 0
                        }"
                      >
                        {{ dayName }}
                        <span v-if="timeStrategyData.weeklyTimes[dayKey].length > 0" class="time-indicator">●</span>
                      </el-checkbox>
                    </el-checkbox-group>
                  </div>
                </div>
                <template #footer>
                  <div class="copy-dialog-footer">
                    <el-button @click="cancelCopy">取消</el-button>
                    <el-button type="primary" @click="confirmCopy">确定</el-button>
                  </div>
                </template>
              </el-dialog>
            </div>

            <!-- 每月录制时间 -->
            <div v-if="configForm.recordSettings.monthly" class="monthly-schedule">
              <h4 class="schedule-title">每月录制时间</h4>
              <div class="month-rows">
                <div 
                  v-for="day in monthDays" 
                  :key="day" 
                  class="month-row"
                >
                  <div class="day-label-weekly">
                    <span>{{ day.toString().padStart(2, '0') }}</span>
                  </div>
                  <div class="time-section-weekly">
                    <div class="time-labels-weekly">
                      <span v-for="labelIndex in timeLabelCount" :key="labelIndex" class="time-label-weekly">
                        {{ (labelIndex - 1).toString().padStart(2, '0') }}
                      </span>
                    </div>
                    <div 
                      class="time-track"
                      @mousedown="startDragMonthly($event, day)"
                      @mousemove="onDragMonthly($event, day)"
                      @mouseup="endDragMonthly"
                      @mouseleave="endDragMonthly"
                    >
                      <div 
                        v-for="slot in timeSlotCount" 
                        :key="slot" 
                        class="time-segment"
                        :class="{ selected: isMonthlyTimeSelected(day, slot - 1) }"
                        :data-slot="slot - 1"
                        :data-day="day"
                      ></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 自定义录制时间 -->
            <div v-if="configForm.recordSettings.custom" class="custom-schedule">
              <h4 class="schedule-title">自定义录制时间</h4>
              <div class="custom-actions">
                <el-date-picker
                  v-model="customDateValue"
                  type="date"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  placeholder="选择时间"
                  class="custom-date-picker"
                />
                <div class="custom-actions-buttons">
                  <el-button type="primary" @click="addCustomDate">添加时间</el-button>
                  <el-button @click="clearCustomDates">清除</el-button>
                </div>
              </div>
              <div v-if="!customDateList.length" class="custom-empty">没有添加自定义录制时间</div>
              <div v-else class="custom-rows">
                <div 
                  v-for="dateKey in customDateList" 
                  :key="dateKey" 
                  class="custom-row"
                >
                  <div class="day-label-weekly">
                    <span>{{ dateKey }}</span>
                  </div>
                  <div class="time-section-weekly">
                    <div class="time-labels-weekly">
                      <span v-for="labelIndex in timeLabelCount" :key="labelIndex" class="time-label-weekly">
                        {{ (labelIndex - 1).toString().padStart(2, '0') }}
                      </span>
                    </div>
                    <div 
                      class="time-track"
                      @mousedown="startDragCustom($event, dateKey)"
                      @mousemove="onDragCustom($event, dateKey)"
                      @mouseup="endDragCustom"
                      @mouseleave="endDragCustom"
                    >
                      <div 
                        v-for="slot in timeSlotCount" 
                        :key="slot" 
                        class="time-segment"
                        :class="{ selected: isCustomTimeSelected(dateKey, slot - 1) }"
                        :data-slot="slot - 1"
                        :data-day="dateKey"
                      ></div>
                    </div>
                  </div>
                  <div class="custom-action">
                    <el-button size="small" text @click="removeCustomDate(dateKey)">删除</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 事件策略内容 -->
      <div v-if="activeTab === 'event'" class="config-tab-content">
        <!-- 事件策略标签页 -->
        <div class="event-tabs">
          <div class="event-tab-item active">移动侦测</div>
          <div class="event-tab-item">遮挡报警</div>
        </div>

        <!-- 事件选项 -->
        <div class="config-section">
          <div class="event-options">
            <el-checkbox v-model="eventForm.motionDetection" label="启用移动侦测" />
            <el-checkbox v-model="eventForm.ptzUpload" label="启用PTZ运动报警上报" />
            <el-checkbox v-model="eventForm.autoAnalysis" label="启用动态分析" />
          </div>
        </div>

        <!-- 触发报警时 -->
        <div class="config-section">
          <h3 class="config-title">触发报警时</h3>
          <div class="alert-options">
            <el-radio-group v-model="eventForm.alertType">
              <el-radio label="video">录像</el-radio>
              <el-radio label="image">抓图</el-radio>
            </el-radio-group>
          </div>
        </div>

        <!-- 触发报警前后录制时间 -->
        <div class="config-section">
          <h3 class="config-title">触发报警前后录制时间</h3>
          <div class="time-inputs">
            <div class="time-input-group">
              <span class="time-label">前</span>
              <el-input 
                v-model="eventForm.preRecordTime" 
                placeholder="请输入" 
                style="width: 120px"
              />
              <span class="time-unit">秒</span>
              <span class="time-label" style="margin-left: 20px">后</span>
              <el-input 
                v-model="eventForm.postRecordTime" 
                placeholder="请输入" 
                style="width: 120px"
              />
              <span class="time-unit">秒</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 保存按钮 -->
      <div class="config-footer">
        <el-button 
          type="primary" 
          class="save-config-btn"
          @click="saveTimeStrategy"
        >
          保存
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'
import { weekDays } from './constants.js'
import { 
  parseTimeRanges, 
  isTimeSelected, 
  addTimeRange, 
  clearTimeSelection, 
  copyTimeConfig 
} from './deviceUtils.js'
import { getTimeStrategy } from '@/api/timeStrategy'

const props = defineProps({
  deviceInfo: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['save', 'cancel'])

// 响应式数据
const activeTab = ref('time')

// 配置表单数据
const configForm = reactive({
  deleteAll: false,
  deleteAllTime: false,
  recordSettings: {
    everyday: true,
    weekly: false,
    monthly: false,
    custom: false
  }
})

// 事件表单数据
const eventForm = reactive({
  motionDetection: true,
  ptzUpload: false,
  autoAnalysis: true,
  alertType: 'video',
  preRecordTime: '5',
  postRecordTime: '10'
})

// 时间策略数据
const createWeeklyTimes = () => ({
  monday: [],
  tuesday: [],
  wednesday: [],
  thursday: [],
  friday: [],
  saturday: [],
  sunday: []
})

const monthDays = Array.from({ length: 31 }, (_, index) => index + 1)

const createMonthlyTimes = () => {
  const times = {}
  monthDays.forEach(day => {
    times[day] = []
  })
  return times
}

const timeStrategyData = reactive({
  dailyTimes: [],
  weeklyTimes: createWeeklyTimes(),
  monthlyTimes: createMonthlyTimes(),
  customTimes: {}
})

// 拖拽相关
const timeSlotCount = 48
const slotsPerHour = 2
const timeLabelCount = 25

const isDragging = ref(false)
const dragStartSlot = ref(null)

// 复制对话框相关
const copyDialogVisible = ref(false)
const sourceDayKey = ref('')
const copyTargetDays = ref([])
const customDateValue = ref('')
const customDateList = ref([])

// 计算属性
const sourceDay = computed(() => {
  return weekDays[sourceDayKey.value] || ''
})

// 方法
const toggleDeleteAll = () => {
  configForm.deleteAll = !configForm.deleteAll
  if (configForm.deleteAll) {
    // 清除所有时间设置
    timeStrategyData.dailyTimes = []
    timeStrategyData.weeklyTimes = createWeeklyTimes()
    timeStrategyData.monthlyTimes = createMonthlyTimes()
    timeStrategyData.customTimes = {}
    customDateList.value = []
    customDateValue.value = ''
  }
}

const toggleDeleteAllTime = () => {
  configForm.deleteAllTime = !configForm.deleteAllTime
  if (configForm.deleteAllTime) {
    toggleDeleteAll()
  }
}

const setRecordType = (type) => {
  // 重置所有类型
  Object.keys(configForm.recordSettings).forEach(key => {
    configForm.recordSettings[key] = false
  })
  
  // 设置选中的类型
  configForm.recordSettings[type] = true
}

// 每日时间选择
const isDailyTimeSelected = (slotIndex) => {
  return isTimeSelected(timeStrategyData.dailyTimes, slotIndex)
}

const startDragDaily = (event) => {
  const slotIndex = Number(event.target.dataset.slot)
  if (Number.isNaN(slotIndex)) return
  isDragging.value = true
  dragStartSlot.value = slotIndex
  
  if (isDailyTimeSelected(slotIndex)) {
    removeDailyTimeRange(slotIndex, slotIndex)
  } else {
    timeStrategyData.dailyTimes = addTimeRange(timeStrategyData.dailyTimes, slotIndex, slotIndex)
  }
}

const onDragDaily = (event) => {
  if (!isDragging.value || !event.target.dataset.slot) return
  
  const currentSlot = Number(event.target.dataset.slot)
  if (Number.isNaN(currentSlot) || dragStartSlot.value === null) return
  const startSlot = Math.min(dragStartSlot.value, currentSlot)
  const endSlot = Math.max(dragStartSlot.value, currentSlot)
  
  const isAddMode = !isDailyTimeSelected(dragStartSlot.value)
  
  if (isAddMode) {
    timeStrategyData.dailyTimes = addTimeRange(timeStrategyData.dailyTimes, startSlot, endSlot)
  } else {
    removeDailyTimeRange(startSlot, endSlot)
  }
}

const endDragDaily = () => {
  isDragging.value = false
  dragStartSlot.value = null
}

const removeDailyTimeRange = (startSlot, endSlot) => {
  timeStrategyData.dailyTimes = timeStrategyData.dailyTimes.filter(range => {
    return !(range.start <= endSlot && range.end >= startSlot)
  })
}

const isWeeklyTimeSelected = (dayKey, slotIndex) => {
  return isTimeSelected(timeStrategyData.weeklyTimes[dayKey], slotIndex)
}

const startDragWeekly = (event, dayKey) => {
  const slotIndex = Number(event.target.dataset.slot)
  if (Number.isNaN(slotIndex)) return
  isDragging.value = true
  dragStartSlot.value = slotIndex
  
  if (isWeeklyTimeSelected(dayKey, slotIndex)) {
    removeWeeklyTimeRange(dayKey, slotIndex, slotIndex)
  } else {
    timeStrategyData.weeklyTimes[dayKey] = addTimeRange(timeStrategyData.weeklyTimes[dayKey], slotIndex, slotIndex)
  }
}

const onDragWeekly = (event, dayKey) => {
  if (!isDragging.value || !event.target.dataset.slot) return
  
  const currentSlot = Number(event.target.dataset.slot)
  if (Number.isNaN(currentSlot) || dragStartSlot.value === null) return
  const startSlot = Math.min(dragStartSlot.value, currentSlot)
  const endSlot = Math.max(dragStartSlot.value, currentSlot)
  
  const isAddMode = !isWeeklyTimeSelected(dayKey, dragStartSlot.value)
  
  if (isAddMode) {
    timeStrategyData.weeklyTimes[dayKey] = addTimeRange(timeStrategyData.weeklyTimes[dayKey], startSlot, endSlot)
  } else {
    removeWeeklyTimeRange(dayKey, startSlot, endSlot)
  }
}

const endDragWeekly = () => {
  isDragging.value = false
  dragStartSlot.value = null
}

const removeWeeklyTimeRange = (dayKey, startSlot, endSlot) => {
  timeStrategyData.weeklyTimes[dayKey] = timeStrategyData.weeklyTimes[dayKey].filter(range => {
    return !(range.start <= endSlot && range.end >= startSlot)
  })
}

const isMonthlyTimeSelected = (day, slotIndex) => {
  const ranges = timeStrategyData.monthlyTimes[day] || []
  return isTimeSelected(ranges, slotIndex)
}

const startDragMonthly = (event, day) => {
  const slotIndex = Number(event.target.dataset.slot)
  if (Number.isNaN(slotIndex)) return
  if (!timeStrategyData.monthlyTimes[day]) {
    timeStrategyData.monthlyTimes[day] = []
  }
  isDragging.value = true
  dragStartSlot.value = slotIndex

  if (isMonthlyTimeSelected(day, slotIndex)) {
    removeMonthlyTimeRange(day, slotIndex, slotIndex)
  } else {
    timeStrategyData.monthlyTimes[day] = addTimeRange(timeStrategyData.monthlyTimes[day], slotIndex, slotIndex)
  }
}

const onDragMonthly = (event, day) => {
  if (!isDragging.value || !event.target.dataset.slot) return

  const currentSlot = Number(event.target.dataset.slot)
  if (Number.isNaN(currentSlot) || dragStartSlot.value === null) return
  const startSlot = Math.min(dragStartSlot.value, currentSlot)
  const endSlot = Math.max(dragStartSlot.value, currentSlot)

  const isAddMode = !isMonthlyTimeSelected(day, dragStartSlot.value)

  if (isAddMode) {
    timeStrategyData.monthlyTimes[day] = addTimeRange(timeStrategyData.monthlyTimes[day], startSlot, endSlot)
  } else {
    removeMonthlyTimeRange(day, startSlot, endSlot)
  }
}

const endDragMonthly = () => {
  isDragging.value = false
  dragStartSlot.value = null
}

const removeMonthlyTimeRange = (day, startSlot, endSlot) => {
  if (!timeStrategyData.monthlyTimes[day]) return
  timeStrategyData.monthlyTimes[day] = timeStrategyData.monthlyTimes[day].filter(range => {
    return !(range.start <= endSlot && range.end >= startSlot)
  })
}

const ensureCustomDate = (date) => {
  if (!timeStrategyData.customTimes[date]) {
    timeStrategyData.customTimes[date] = []
  }
}

const isCustomTimeSelected = (date, slotIndex) => {
  const ranges = timeStrategyData.customTimes[date] || []
  return isTimeSelected(ranges, slotIndex)
}

const startDragCustom = (event, date) => {
  const slotIndex = Number(event.target.dataset.slot)
  if (Number.isNaN(slotIndex)) return
  ensureCustomDate(date)
  isDragging.value = true
  dragStartSlot.value = slotIndex

  if (isCustomTimeSelected(date, slotIndex)) {
    removeCustomTimeRange(date, slotIndex, slotIndex)
  } else {
    timeStrategyData.customTimes[date] = addTimeRange(timeStrategyData.customTimes[date], slotIndex, slotIndex)
  }
}

const onDragCustom = (event, date) => {
  if (!isDragging.value || !event.target.dataset.slot) return

  const currentSlot = Number(event.target.dataset.slot)
  if (Number.isNaN(currentSlot) || dragStartSlot.value === null) return
  const startSlot = Math.min(dragStartSlot.value, currentSlot)
  const endSlot = Math.max(dragStartSlot.value, currentSlot)

  const isAddMode = !isCustomTimeSelected(date, dragStartSlot.value)

  if (isAddMode) {
    timeStrategyData.customTimes[date] = addTimeRange(timeStrategyData.customTimes[date], startSlot, endSlot)
  } else {
    removeCustomTimeRange(date, startSlot, endSlot)
  }
}

const endDragCustom = () => {
  isDragging.value = false
  dragStartSlot.value = null
}

const removeCustomTimeRange = (date, startSlot, endSlot) => {
  if (!timeStrategyData.customTimes[date]) return
  timeStrategyData.customTimes[date] = timeStrategyData.customTimes[date].filter(range => {
    return !(range.start <= endSlot && range.end >= startSlot)
  })
}

const showCopyDialog = (dayKey) => {
  sourceDayKey.value = dayKey
  copyTargetDays.value = []
  copyDialogVisible.value = true
}

const cancelCopy = () => {
  copyDialogVisible.value = false
  sourceDayKey.value = ''
  copyTargetDays.value = []
}

const confirmCopy = () => {
  if (copyTargetDays.value.length === 0) {
    ElMessage.warning('请选择要复制到的日期')
    return
  }
  
  const sourceConfig = timeStrategyData.weeklyTimes[sourceDayKey.value]
  
  copyTargetDays.value.forEach(targetDay => {
    timeStrategyData.weeklyTimes[targetDay] = [...sourceConfig]
  })
  
  ElMessage.success(`已复制到 ${copyTargetDays.value.length} 个日期`)
  cancelCopy()
}

// 将小时数组转换为时间范围对象
const addCustomDate = () => {
  if (!customDateValue.value) {
    ElMessage.warning('Please select a date')
    return
  }

  const dateKey = customDateValue.value
  if (timeStrategyData.customTimes[dateKey]) {
    ElMessage.warning('Date already exists')
    return
  }

  timeStrategyData.customTimes[dateKey] = []
  customDateList.value = [...customDateList.value, dateKey].sort()
  customDateValue.value = ''
}

const removeCustomDate = (dateKey) => {
  if (!timeStrategyData.customTimes[dateKey]) return
  delete timeStrategyData.customTimes[dateKey]
  customDateList.value = customDateList.value.filter(date => date !== dateKey)
}

const clearCustomDates = () => {
  timeStrategyData.customTimes = {}
  customDateList.value = []
  customDateValue.value = ''
}

const normalizeTimeSlots = (values) => {
  if (!Array.isArray(values) || values.length === 0) return []
  const numbers = values
    .map(value => Number(value))
    .filter(value => Number.isFinite(value))
  if (numbers.length === 0) return []
  const hasHalfHour = numbers.some(value => !Number.isInteger(value) || value > 23)
  if (hasHalfHour) {
    const normalized = numbers.map(value => (
      Number.isInteger(value) ? value : Math.round(value * slotsPerHour)
    ))
    return Array.from(new Set(normalized))
      .filter(value => value >= 0 && value < timeSlotCount)
      .sort((a, b) => a - b)
  }
  const expanded = []
  numbers.forEach(hour => {
    const base = hour * slotsPerHour
    expanded.push(base, base + 1)
  })
  return Array.from(new Set(expanded))
    .filter(value => value >= 0 && value < timeSlotCount)
    .sort((a, b) => a - b)
}

const convertSlotsToTimeRanges = (slots) => {
  if (!Array.isArray(slots) || slots.length === 0) return []
  
  const sortedSlots = [...slots].sort((a, b) => a - b)
  const ranges = []
  let start = sortedSlots[0]
  let end = sortedSlots[0]
  
  for (let i = 1; i < sortedSlots.length; i++) {
    if (sortedSlots[i] === end + 1) {
      end = sortedSlots[i]
    } else {
      ranges.push({ start, end })
      start = sortedSlots[i]
      end = sortedSlots[i]
    }
  }
  
  ranges.push({ start, end })
  return ranges
}

const convertTimeRangesToSlots = (timeRanges) => {
  if (!Array.isArray(timeRanges)) return []
  
  const slots = []
  timeRanges.forEach(range => {
    for (let slot = range.start; slot <= range.end; slot += 1) {
      if (!slots.includes(slot)) {
        slots.push(slot)
      }
    }
  })
  
  return slots.sort((a, b) => a - b)
}

const loadTimeStrategy = async () => {
  if (!props.deviceInfo.id) return
  
  try {
    const response = await getTimeStrategy(props.deviceInfo.id)
    
    if (response.code === 200 && response.data) {
      const strategy = response.data
      const validTypes = ['everyday', 'weekly', 'monthly', 'custom']
      const recordType = validTypes.includes(strategy.strategyType) ? strategy.strategyType : 'everyday'
      setRecordType(recordType)

      timeStrategyData.dailyTimes = []
      timeStrategyData.weeklyTimes = createWeeklyTimes()
      timeStrategyData.monthlyTimes = createMonthlyTimes()
      timeStrategyData.customTimes = {}
      customDateList.value = []
      customDateValue.value = ''

      if (strategy.dailyTimes) {
        const dailySlots = normalizeTimeSlots(strategy.dailyTimes)
        timeStrategyData.dailyTimes = convertSlotsToTimeRanges(dailySlots)
      }
      
      if (strategy.weeklyTimes) {
        Object.keys(strategy.weeklyTimes).forEach(day => {
          if (timeStrategyData.weeklyTimes[day] !== undefined) {
            const weeklySlots = normalizeTimeSlots(strategy.weeklyTimes[day])
            timeStrategyData.weeklyTimes[day] = convertSlotsToTimeRanges(weeklySlots)
          }
        })
      }

      if (recordType === 'monthly') {
        const monthlySource = strategy.monthlyTimes || strategy.weeklyTimes || {}
        monthDays.forEach(day => {
          const monthlySlots = normalizeTimeSlots(monthlySource[day])
          timeStrategyData.monthlyTimes[day] = convertSlotsToTimeRanges(monthlySlots)
        })
      }

      if (recordType === 'custom') {
        const customSource = strategy.customTimes || strategy.weeklyTimes || {}
        Object.keys(customSource).forEach(dateKey => {
          const customSlots = normalizeTimeSlots(customSource[dateKey])
          timeStrategyData.customTimes[dateKey] = convertSlotsToTimeRanges(customSlots)
        })
        customDateList.value = Object.keys(timeStrategyData.customTimes).sort()
      }
      
      console.log('Loaded time strategy:', strategy)
      console.log('Normalized time strategy:', timeStrategyData)
    }
  } catch (error) {
    console.error('Failed to load time strategy:', error)
  }
}



// 保存时间策略
const saveTimeStrategy = async () => {
  try {
    const recordType = Object.keys(configForm.recordSettings).find(key => configForm.recordSettings[key]) || 'everyday'
    
    const dailySlots = convertTimeRangesToSlots(timeStrategyData.dailyTimes)
    const weeklySlots = {}
    Object.keys(timeStrategyData.weeklyTimes).forEach(day => {
      weeklySlots[day] = convertTimeRangesToSlots(timeStrategyData.weeklyTimes[day])
    })
    const monthlySlots = {}
    monthDays.forEach(day => {
      monthlySlots[day] = convertTimeRangesToSlots(timeStrategyData.monthlyTimes[day] || [])
    })
    const customSlots = {}
    Object.keys(timeStrategyData.customTimes).forEach(dateKey => {
      customSlots[dateKey] = convertTimeRangesToSlots(timeStrategyData.customTimes[dateKey])
    })

    let dailyTimes = []
    let weeklyTimes = {}
    if (recordType === 'everyday') {
      dailyTimes = dailySlots
    } else if (recordType === 'weekly') {
      weeklyTimes = weeklySlots
    } else if (recordType === 'monthly') {
      weeklyTimes = monthlySlots
    } else if (recordType === 'custom') {
      weeklyTimes = customSlots
    } else {
      dailyTimes = dailySlots
      weeklyTimes = weeklySlots
    }
    
    const strategyData = {
      deviceId: props.deviceInfo.id,
      strategyType: recordType, // everyday, weekly, monthly, custom
      dailyTimes,
      weeklyTimes
    }
    
    console.log('Saving time strategy:', strategyData)
    
    emit('save', strategyData)
  } catch (error) {
    console.error('Failed to save time strategy:', error)
    ElMessage.error('Failed to save time strategy.')
  }
}



const handleCancel = () => {
  emit('cancel')
}

// 组件挂载时加载时间策略
onMounted(() => {
  loadTimeStrategy()
})
</script>

<style scoped lang="scss">
.device-time-strategy {
  padding: 20px;
  
  .config-container {
    max-width: 1200px;
    margin: 0;
  }
  
  .config-tabs {
    display: flex;
    margin-bottom: 30px;
    border-bottom: 1px solid #e4e7ed;
    
    .tab-item {
      padding: 12px 24px;
      cursor: pointer;
      border-bottom: 2px solid transparent;
      transition: all 0.3s;
      
      &:hover {
        color: #409eff;
      }
      
      &.active {
        color: #409eff;
        border-bottom-color: #409eff;
      }
    }
  }
  
  .config-section {
    margin-bottom: 30px;
    
    .config-title {
      margin: 0 0 16px 0;
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
    
    .config-buttons {
      display: flex;
      gap: 12px;
    }
    
    .record-options {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }
  }
  
  // 每日时间选择样式
  .daily-schedule {
    .schedule-title {
      margin: 0 0 20px 0;
      font-size: 14px;
      font-weight: 500;
      color: #666;
    }
    
    .time-axis-daily {
      margin-bottom: 8px;
      
      .time-labels-daily {
        display: flex;
        justify-content: space-between;
        padding: 0 8px;
        
        .time-label-daily {
          font-size: 12px;
          color: #909399;
        }
      }
    }
    
    .time-selector-daily {
      .time-track {
        display: flex;
        height: 30px;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        overflow: hidden;
        cursor: pointer;
        
        .time-segment {
          flex: 1;
          height: 100%;
          border-right: 1px solid #f0f0f0;
          transition: background-color 0.2s;
          
          &:last-child {
            border-right: none;
          }
          
          &:hover {
            background-color: #e6f3ff;
          }
          
          &.selected {
            background-color: #409eff;
          }
        }
      }
    }
  }
  
  // 每周时间选择样式
  .weekly-schedule,
  .monthly-schedule,
  .custom-schedule {
    .schedule-title {
      margin: 0 0 20px 0;
      font-size: 14px;
      font-weight: 500;
      color: #666;
    }
  }

  .week-rows,
  .month-rows,
  .custom-rows {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .month-rows,
  .custom-rows {
    max-height: 520px;
    overflow-y: auto;
    padding-right: 4px;
  }

  .week-row,
  .month-row,
  .custom-row {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .day-label-weekly {
      width: 80px;
      text-align: center;
      font-weight: 500;
      color: #333;
    }
    
    .time-section-weekly {
      flex: 1;
      
      .time-labels-weekly {
        display: flex;
        justify-content: space-between;
        padding: 0 8px;
        margin-bottom: 4px;
        
        .time-label-weekly {
          font-size: 10px;
          color: #c0c4cc;
        }
      }
      
      .time-track {
        display: flex;
        height: 24px;
        border: 1px solid #dcdfe6;
        border-radius: 3px;
        overflow: hidden;
        cursor: pointer;
        
        .time-segment {
          flex: 1;
          height: 100%;
          border-right: 1px solid #f5f5f5;
          transition: background-color 0.2s;
          
          &:last-child {
            border-right: none;
          }
          
          &:hover {
            background-color: #e6f3ff;
          }
          
          &.selected {
            background-color: #409eff;
          }
        }
      }
    }
    
    .copy-action-weekly {
      width: 40px;
      display: flex;
      justify-content: center;
      
      .copy-btn {
        padding: 4px;
        min-height: auto;
        
        .el-icon {
          font-size: 14px;
        }
      }
    }
  }

  .month-row {
    .day-label-weekly {
      width: 60px;
    }
  }

  .custom-row {
    .day-label-weekly {
      width: 140px;
      text-align: left;
    }
  }

  .custom-action {
    width: 80px;
    display: flex;
    justify-content: center;
  }

  .custom-actions {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 16px;
  }

  .custom-actions-buttons {
    display: flex;
    gap: 8px;
  }

  .custom-date-picker {
    min-width: 200px;
  }

  .custom-empty {
    font-size: 12px;
    color: #909399;
  }

  .event-tabs {
    display: flex;
    gap: 1px;
    margin-bottom: 20px;
    
    .event-tab-item {
      padding: 8px 16px;
      background: #f5f7fa;
      cursor: pointer;
      
      &.active {
        background: #409eff;
        color: white;
      }
    }
  }
  
  .event-options {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  
  .alert-options {
    .el-radio-group {
      display: flex;
      gap: 20px;
    }
  }
  
  .time-inputs {
    .time-input-group {
      display: flex;
      align-items: center;
      gap: 8px;
      
      .time-label {
        font-weight: 500;
        color: #333;
      }
      
      .time-unit {
        color: #666;
      }
    }
  }
  
  .config-footer {
    display: flex;
    justify-content: flex-start;
    gap: 12px;
    padding: 20px 0;
    border-top: 1px solid #ebeef5;
    margin-top: 30px;
    
    .save-config-btn {
      width: 170px !important;
      height: 48px !important;
      font-size: 16px;
      font-weight: 500;
    }
  }
  
  // 复制对话框样式
  .copy-dialog-content {
    .copy-day-options {
      margin-top: 16px;
      
      .el-checkbox {
        width: 100%;
        margin: 8px 0;
        
        &.source-day {
          color: #909399;
          
          :deep(.el-checkbox__label) {
            color: #909399;
          }
        }
        
        &.has-time-setting {
          .time-indicator {
            color: #67c23a;
            margin-left: 8px;
          }
        }
      }
    }
  }
  
  .copy-dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .device-time-strategy {
    .week-row,
    .month-row,
    .custom-row {
      flex-direction: column;
      align-items: stretch;
      gap: 8px;
      
      .day-label-weekly {
        width: auto;
        text-align: left;
      }
      
      .copy-action-weekly,
      .custom-action {
        width: auto;
        justify-content: flex-start;
      }
    }
    
    .record-options {
      flex-direction: column;
      
      .el-button {
        justify-content: flex-start;
      }
    }
    
    .time-input-group {
      flex-wrap: wrap;
      gap: 8px;
    }
  }
}
</style> 
