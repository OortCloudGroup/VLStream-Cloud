<template>
  <div class="schedule-mobile">
    <!-- 周期选择 -->
    <div class="repeat_row">
      <div class="repeat_tabs">
        <div
          v-for="item in repeatTabs"
          :key="item.value"
          class="repeat_tab"
          :class="{ act: formValue.repeat === item.value }"
          @click="updateRepeat(item.value)"
        >
          {{ item.label }}
        </div>
      </div>
    </div>

    <!-- 每隔输入框（仅隔天模式显示） -->
    <div v-if="formValue.repeat === 'next'" class="interval_row">
      <van-field
        v-model="intervalValue"
        label="每隔"
        type="number"
        placeholder="请输入"
        class="interval_input"
        @update:model-value="updateInterval"
      >
        <template #right-icon>
          <span class="interval_suffix">天</span>
        </template>
      </van-field>
    </div>

    <!-- 时间轴 -->
    <div class="timeline_wrap">
      <div v-for="day in dayRows" :key="day.key" class="timeline_row">
        <div
          class="row_label"
          :class="{ selected: selectedDayKey === day.key }"
          @click="handleRowLabelClick(day.key)"
        >
          {{ day.label }}
        </div>
        <div class="row_track_wrap">
          <div class="row_ticks">
            <div
              v-for="(t, index) in ticks"
              :key="t"
              class="row_tick"
              :style="tickStyle(index)"
            >
              <div class="row_tick_num">
                {{ t }}
              </div>
              <div class="row_tick_line" />
            </div>
          </div>
          <div
            class="row_track"
            @mousedown="handleTrackMouseDown($event, day.key)"
            @touchstart="handleTrackTouchStart($event, day.key)"
          >
            <div class="track_bg" />

            <div
              v-for="seg in segmentsByDay[day.key] || []"
              :key="seg.id"
              class="seg"
              :class="{ selected: selectedSegId === seg.id }"
              :style="segmentStyle(seg)"
              @mousedown.stop="handleSegmentMouseDown($event, day.key, seg)"
              @touchstart.stop="handleSegmentTouchStart($event, day.key, seg)"
              @dblclick.stop="handleSegmentDelete(day.key, seg.id)"
            >
              <div
                class="seg_resize_left"
                @mousedown.stop="handleResizeStart($event, day.key, seg, 'left')"
                @touchstart.stop="handleResizeTouchStart($event, day.key, seg, 'left')"
              />
              <div
                class="seg_resize_right"
                @mousedown.stop="handleResizeStart($event, day.key, seg, 'right')"
                @touchstart.stop="handleResizeTouchStart($event, day.key, seg, 'right')"
              />
              <div class="seg_delete" @click.stop="handleSegmentDelete(day.key, seg.id)">
                <van-icon name="cross" />
              </div>
            </div>

            <div
              v-if="formValue.repeat === 'week' || formValue.repeat === 'month'"
              class="row_copy"
              @click.stop="handleCopyClick(day.key)"
            >
              <img src="@/assets/img/VLStreamCloud/copy.png" alt="" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑时间段弹窗 -->
    <oort-popup
      v-model="editPop.visible"
      position="bottom"
      style="height: 50%;"
      round
      closeable
      @close="closeEditPop"
    >
      <div class="edit_pop_content">
        <div class="edit_pop_title">
          编辑时间段
        </div>

        <div class="edit_time_row">
          <div class="edit_time_item">
            <div class="edit_time_label">
              开始时间
            </div>
            <van-time-picker
              v-model="editPop.startTime"
              title=""
              :columns-type="['hour', 'minute']"
              :show-toolbar="false"
            />
          </div>
          <div class="edit_time_dash">
            -
          </div>
          <div class="edit_time_item">
            <div class="edit_time_label">
              结束时间
            </div>
            <van-time-picker
              v-model="editPop.endTime"
              title=""
              :columns-type="['hour', 'minute']"
              :show-toolbar="false"
            />
          </div>
        </div>

        <div class="edit_actions">
          <van-button class="edit_delete_btn" @click="handleEditDelete">
            删除
          </van-button>
          <van-button type="primary" class="edit_save_btn" @click="handleEditSave">
            保存
          </van-button>
        </div>
      </div>
    </oort-popup>

    <!-- 复制到...弹窗 -->
    <oort-popup
      v-model="copyVisible"
      position="bottom"
      style="height: 60%;"
      round
      closeable
      @close="copyVisible = false"
    >
      <div class="copy_pop_content">
        <div class="copy_pop_title">
          复制到…
        </div>

        <div class="copy_days">
          <van-checkbox-group v-model="copyDays" class="copy_checkbox_group">
            <div
              v-for="d in copyDaysOptions"
              :key="d.value"
              class="copy_day_item"
            >
              <van-checkbox :name="d.value">
                {{ d.label }}
              </van-checkbox>
            </div>
          </van-checkbox-group>
        </div>

        <div class="copy_actions">
          <van-button class="copy_cancel_btn" @click="copyVisible = false">
            取消
          </van-button>
          <van-button type="primary" class="copy_ok_btn" @click="handleCopyConfirm">
            确定
          </van-button>
        </div>
      </div>
    </oort-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch, onBeforeUnmount } from 'vue'
import { TimePicker as VanTimePicker, Button as VanButton, CheckboxGroup as VanCheckboxGroup, Checkbox as VanCheckbox, Icon as VanIcon, Field as VanField, showToast } from 'vant'
import OortPopup from '@/components/popup/oort_popup.vue'

interface Seg {
  id: string
  start: number
  end: number
}

interface FormData {
  repeat: string
  interval: number
}

// API time_periods 类型，与 PC 版保持一致，方便共用逻辑
type TimePeriodItem =
  | { start: string; end: string }
  | { interval: number }
  | { weekday: number }
  | { day: number }

type TimePeriods =
  | TimePeriodItem[]
  | TimePeriodItem[][]

const props = defineProps<{
  form?: FormData
  timePeriods?: TimePeriods
}>()

const emit = defineEmits<{
  'update:form': [value: FormData]
  'update:time-periods': [value: TimePeriods]
}>()

const defaultForm = reactive<FormData>({
  repeat: 'day',
  interval: 1
})

const formValue = computed(() => props.form || defaultForm)

const segmentsByDay = reactive<Record<string, Seg[]>>({})

const timeToHours = (timeStr: string): number => {
  const [hours, minutes] = timeStr.split(':').map(Number)
  return hours + minutes / 60
}

const hoursToTime = (hours: number): string => {
  const h = Math.floor(hours)
  const m = Math.floor((hours - h) * 60)
  const s = 0
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const weekdayMap: Record<number, string> = {
  1: 'mon',
  2: 'tue',
  3: 'wed',
  4: 'thu',
  5: 'fri',
  6: 'sat',
  7: 'sun'
}

const weekdayMapReverse: Record<string, number> = {
  mon: 1,
  tue: 2,
  wed: 3,
  thu: 4,
  fri: 5,
  sat: 6,
  sun: 7
}

const convertTimePeriodsToSegments = (timePeriods: TimePeriods | undefined, repeat: string): Record<string, Seg[]> => {
  const result: Record<string, Seg[]> = {}

  if (!timePeriods) {
    return result
  }

  if (repeat === 'day' || repeat === 'next') {
    if (!Array.isArray(timePeriods) || (timePeriods.length > 0 && Array.isArray(timePeriods[0]))) {
      return result
    }
    const periods = timePeriods as TimePeriodItem[]
    const segments: Seg[] = periods
      .filter((p: any) => p.start && p.end)
      .map((p: any, index: number) => ({
        id: `s${index + 1}`,
        start: timeToHours(p.start),
        end: timeToHours(p.end)
      }))
    const key = repeat === 'day' ? 'day' : 'next'
    if (segments.length > 0) {
      result[key] = segments
    }
  } else if (repeat === 'week' || repeat === 'month') {
    if (!Array.isArray(timePeriods) || timePeriods.length === 0 || !Array.isArray(timePeriods[0])) {
      return result
    }
    const periods = timePeriods as TimePeriodItem[][]
    periods.forEach((dayPeriods) => {
      if (!Array.isArray(dayPeriods)) return

      if (repeat === 'week') {
        const weekdayObj = dayPeriods.find((p: any) => p.weekday) as { weekday: number } | undefined
        if (weekdayObj) {
          const weekdayKey = weekdayMap[weekdayObj.weekday]
          if (weekdayKey) {
            const segments: Seg[] = dayPeriods
              .filter((p: any) => p.start && p.end)
              .map((p: any, index: number) => ({
                id: `s${index + 1}`,
                start: timeToHours(p.start),
                end: timeToHours(p.end)
              }))
            if (segments.length > 0) {
              result[weekdayKey] = segments
            }
          }
        }
      } else {
        const dayObj = dayPeriods.find((p: any) => p.day !== undefined) as { day: number } | undefined
        if (dayObj) {
          const dayKey = `day${dayObj.day}`
          const segments: Seg[] = dayPeriods
            .filter((p: any) => p.start && p.end)
            .map((p: any, index: number) => ({
              id: `s${index + 1}`,
              start: timeToHours(p.start),
              end: timeToHours(p.end)
            }))
          if (segments.length > 0) {
            result[dayKey] = segments
          }
        }
      }
    })
  }

  return result
}

const convertSegmentsToTimePeriods = (segments: Record<string, Seg[]>, repeat: string, interval: number): TimePeriods => {
  if (repeat === 'day') {
    const daySegments = segments.day || []
    return daySegments.map(seg => ({
      start: hoursToTime(seg.start),
      end: hoursToTime(seg.end)
    }))
  }

  if (repeat === 'next') {
    const nextSegments = segments.next || []
    const periods: TimePeriodItem[] = [{ interval }]
    nextSegments.forEach(seg => {
      periods.push({
        start: hoursToTime(seg.start),
        end: hoursToTime(seg.end)
      })
    })
    return periods
  }

  if (repeat === 'week') {
    const result: TimePeriodItem[][] = []
    Object.keys(segments).forEach(key => {
      const weekdayNum = weekdayMapReverse[key]
      if (weekdayNum) {
        const dayPeriods: TimePeriodItem[] = [{ weekday: weekdayNum }]
        segments[key].forEach(seg => {
          dayPeriods.push({
            start: hoursToTime(seg.start),
            end: hoursToTime(seg.end)
          })
        })
        result.push(dayPeriods)
      }
    })
    return result
  }

  if (repeat === 'month') {
    const result: TimePeriodItem[][] = []
    Object.keys(segments).forEach(key => {
      if (key.startsWith('day')) {
        const dayNum = parseInt(key.replace('day', ''), 10)
        if (!Number.isNaN(dayNum)) {
          const dayPeriods: TimePeriodItem[] = [{ day: dayNum }]
          segments[key].forEach(seg => {
            dayPeriods.push({
              start: hoursToTime(seg.start),
              end: hoursToTime(seg.end)
            })
          })
          result.push(dayPeriods)
        }
      }
    })
    return result
  }

  return []
}

const isUpdatingFromProps = ref(false)

const getRepeatKeys = (repeat: string): string[] => {
  if (repeat === 'day') return ['day']
  if (repeat === 'next') return ['next']
  if (repeat === 'week') return ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun']
  if (repeat === 'month') return Array.from({ length: 31 }, (_, i) => `day${i + 1}`)
  return []
}

const isTimePeriodsEqual = (a: TimePeriods | undefined, b: TimePeriods | undefined): boolean => {
  if (a === b) return true
  if (!a || !b) return false
  return JSON.stringify(a) === JSON.stringify(b)
}

watch(
  () => [props.timePeriods, formValue.value.repeat],
  (newVal, oldVal) => {
    if (props.timePeriods !== undefined) {
      const newTimePeriods = newVal[0] as TimePeriods | undefined
      const newRepeat = newVal[1] as string
      const oldTimePeriods = oldVal?.[0] as TimePeriods | undefined
      const oldRepeat = oldVal?.[1] as string

      if (newRepeat !== oldRepeat) {
        isUpdatingFromProps.value = true
        const converted = convertTimePeriodsToSegments(props.timePeriods, formValue.value.repeat)
        getRepeatKeys(formValue.value.repeat).forEach(key => {
          delete segmentsByDay[key]
        })
        Object.assign(segmentsByDay, converted)
        setTimeout(() => {
          isUpdatingFromProps.value = false
        }, 0)
        return
      }

      if (!isTimePeriodsEqual(newTimePeriods, oldTimePeriods)) {
        const currentConverted = convertSegmentsToTimePeriods(
          segmentsByDay,
          formValue.value.repeat,
          formValue.value.interval
        )
        if (isTimePeriodsEqual(currentConverted, newTimePeriods)) {
          return
        }

        isUpdatingFromProps.value = true
        const converted = convertTimePeriodsToSegments(props.timePeriods, formValue.value.repeat)
        getRepeatKeys(formValue.value.repeat).forEach(key => {
          delete segmentsByDay[key]
        })
        Object.assign(segmentsByDay, converted)
        setTimeout(() => {
          isUpdatingFromProps.value = false
        }, 0)
      }
    }
  },
  { immediate: true, deep: true }
)

watch(
  () => [segmentsByDay, formValue.value.repeat, formValue.value.interval],
  () => {
    if (props.timePeriods !== undefined && !isUpdatingFromProps.value) {
      const converted = convertSegmentsToTimePeriods(
        segmentsByDay,
        formValue.value.repeat,
        formValue.value.interval
      )
      emit('update:time-periods', converted)
    }
  },
  { deep: true }
)

const selectedSegId = ref<string | null>(null)
const selectedDayKey = ref<string | null>(null)

// 编辑时间段弹窗状态
const editPop = reactive<{
  visible: boolean
  dayKey: string
  segId: string
  startTime: string[]
  endTime: string[]
  start: string
  end: string
}>({
  visible: false,
  dayKey: '',
  segId: '',
  startTime: ['00', '00'],
  endTime: ['00', '00'],
  start: '00:00:00',
  end: '00:00:00'
})

// 复制到...弹窗状态
const copyVisible = ref(false)
const copyDays = ref<string[]>([])
const sourceDayKey = ref<string>('')

const copyWeekDays = [
  { label: '周一', value: 'mon' },
  { label: '周二', value: 'tue' },
  { label: '周三', value: 'wed' },
  { label: '周四', value: 'thu' },
  { label: '周五', value: 'fri' },
  { label: '周六', value: 'sat' },
  { label: '周日', value: 'sun' }
]

// 根据模式动态返回复制选项
const copyDaysOptions = computed(() => {
  if (formValue.value.repeat === 'week') {
    return copyWeekDays
  } else if (formValue.value.repeat === 'month') {
    return Array.from({ length: 31 }, (_, i) => ({
      label: `${i + 1}`,
      value: `day${i + 1}`
    }))
  }
  return []
})

const repeatTabs = [
  { label: '每天', value: 'day' },
  { label: '隔天', value: 'next' },
  { label: '每周', value: 'week' },
  { label: '每月', value: 'month' }
]

const ticks = computed(() => [0, 4, 8, 12, 16, 20, 24])

const tickStyle = (index: number) => {
  const count = ticks.value.length - 1 || 1
  const leftPercent = (index / count) * 100
  return {
    left: `${leftPercent}%`,
    transform: 'translateX(-50%)'
  }
}

const dayRows = computed(() => {
  if (formValue.value.repeat === 'month') {
    return Array.from({ length: 31 }, (_, i) => ({
      key: `day${i + 1}`,
      label: `${String(i + 1).padStart(2, '0')}`
    }))
  }
  if (formValue.value.repeat === 'week') {
    return [
      { key: 'mon', label: '周一' },
      { key: 'tue', label: '周二' },
      { key: 'wed', label: '周三' },
      { key: 'thu', label: '周四' },
      { key: 'fri', label: '周五' },
      { key: 'sat', label: '周六' },
      { key: 'sun', label: '周日' }
    ]
  }
  if (formValue.value.repeat === 'next') {
    return [{ key: 'next', label: '隔天' }]
  }
  return [{ key: 'day', label: '每天' }]
})

const segmentStyle = (seg: Seg) => {
  const duration = seg.end - seg.start
  const left = `${(seg.start / 24) * 100}%`
  let widthPercent = (duration / 24) * 100
  if (duration <= 0 || duration < 0.1) {
    const minWidthPercent = (0.1 / 24) * 100
    widthPercent = Math.max(minWidthPercent, widthPercent)
  }
  return {
    left,
    width: `${widthPercent}%`
  }
}

const handleRowLabelClick = (dayKey: string) => {
  selectedDayKey.value = selectedDayKey.value === dayKey ? null : dayKey
  selectedSegId.value = null
}

// ===== 拖动逻辑，与 PC 版基本一致 =====
const dragState = ref<{
  isDragging: boolean
  mode: 'create' | 'move' | 'resize-left' | 'resize-right' | null
  dayKey: string
  segId: string | null
  startX: number
  startTime: number
  originalStart: number
  originalEnd: number
  moved: boolean
  segElement: HTMLElement | null
  trackElement: HTMLElement | null
}>({
  isDragging: false,
  mode: null,
  dayKey: '',
  segId: null,
  startX: 0,
  startTime: 0,
  originalStart: 0,
  originalEnd: 0,
  moved: false,
  segElement: null,
  trackElement: null
})

const getTimeFromMouseX = (e: MouseEvent, trackElement: HTMLElement): number => {
  const rect = trackElement.getBoundingClientRect()
  const x = e.clientX - rect.left
  const width = rect.width
  const time = Math.max(0, Math.min(24, (x / width) * 24))
  return Math.round(time * 100) / 100
}

const getTimeFromTouchX = (e: TouchEvent, trackElement: HTMLElement): number => {
  const rect = trackElement.getBoundingClientRect()
  const touch = e.touches[0] || e.changedTouches[0]
  const x = touch.clientX - rect.left
  const width = rect.width
  const time = Math.max(0, Math.min(24, (x / width) * 24))
  return Math.round(time * 100) / 100
}

const getClientX = (e: MouseEvent | TouchEvent): number => {
  if ('touches' in e && e.touches.length > 0) {
    return e.touches[0].clientX
  }
  if ('changedTouches' in e && e.changedTouches.length > 0) {
    return e.changedTouches[0].clientX
  }
  return (e as MouseEvent).clientX
}

let segIdCounter = 0
const generateSegId = () => {
  segIdCounter += 1
  return `seg_${Date.now()}_${segIdCounter}`
}

const checkOverlap = (dayKey: string, start: number, end: number, excludeId?: string): boolean => {
  const segments = segmentsByDay[dayKey] || []
  return segments.some(seg => {
    if (excludeId && seg.id === excludeId) return false
    return !(end <= seg.start || start >= seg.end)
  })
}

const handleTrackMouseDown = (e: MouseEvent, dayKey: string) => {
  if (e.button !== 0) return
  const target = e.target as HTMLElement
  if (target.closest('.seg')) return
  // 如果点击在复制按钮上，不处理
  if (target.closest('.row_copy')) return
  // 如果点击在删除按钮上，不处理
  if (target.closest('.seg_delete')) return

  selectedSegId.value = null
  selectedDayKey.value = null

  const trackElement = e.currentTarget as HTMLElement
  const startTime = getTimeFromMouseX(e, trackElement)

  dragState.value = {
    isDragging: true,
    mode: 'create',
    dayKey,
    segId: null,
    startX: e.clientX,
    startTime,
    originalStart: startTime,
    originalEnd: startTime,
    moved: false,
    segElement: null,
    trackElement
  }

  if (!segmentsByDay[dayKey]) {
    segmentsByDay[dayKey] = []
  }
  const newSeg: Seg = {
    id: generateSegId(),
    start: startTime,
    end: startTime
  }
  segmentsByDay[dayKey].push(newSeg)
  dragState.value.segId = newSeg.id

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
  e.preventDefault()
}

const handleTrackTouchStart = (e: TouchEvent, dayKey: string) => {
  const target = e.target as HTMLElement
  if (target.closest('.seg')) return
  // 如果点击在复制按钮上，不处理
  if (target.closest('.row_copy')) return
  // 如果点击在删除按钮上，不处理
  if (target.closest('.seg_delete')) return

  selectedSegId.value = null
  selectedDayKey.value = null

  const trackElement = e.currentTarget as HTMLElement
  const startTime = getTimeFromTouchX(e, trackElement)
  const clientX = getClientX(e)

  dragState.value = {
    isDragging: true,
    mode: 'create',
    dayKey,
    segId: null,
    startX: clientX,
    startTime,
    originalStart: startTime,
    originalEnd: startTime,
    moved: false,
    segElement: null,
    trackElement
  }

  if (!segmentsByDay[dayKey]) {
    segmentsByDay[dayKey] = []
  }
  const newSeg: Seg = {
    id: generateSegId(),
    start: startTime,
    end: startTime
  }
  segmentsByDay[dayKey].push(newSeg)
  dragState.value.segId = newSeg.id

  document.addEventListener('touchmove', handleMouseMove, { passive: false })
  document.addEventListener('touchend', handleMouseUp)
  e.preventDefault()
}

const handleSegmentMouseDown = (e: MouseEvent, dayKey: string, seg: Seg) => {
  if (e.button !== 0) return
  if (
    (e.target as HTMLElement).classList.contains('seg_resize_left') ||
    (e.target as HTMLElement).classList.contains('seg_resize_right')
  ) {
    return
  }

  selectedSegId.value = seg.id
  selectedDayKey.value = null

  const trackElement = (e.currentTarget as HTMLElement).closest('.row_track') as HTMLElement
  const startTime = getTimeFromMouseX(e, trackElement)

  dragState.value = {
    isDragging: true,
    mode: 'move',
    dayKey,
    segId: seg.id,
    startX: e.clientX,
    startTime,
    originalStart: seg.start,
    originalEnd: seg.end,
    moved: false,
    segElement: e.currentTarget as HTMLElement,
    trackElement
  }

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
  e.preventDefault()
  e.stopPropagation()
}

const handleSegmentTouchStart = (e: TouchEvent, dayKey: string, seg: Seg) => {
  if (
    (e.target as HTMLElement).classList.contains('seg_resize_left') ||
    (e.target as HTMLElement).classList.contains('seg_resize_right')
  ) {
    return
  }

  selectedSegId.value = seg.id
  selectedDayKey.value = null

  const trackElement = (e.currentTarget as HTMLElement).closest('.row_track') as HTMLElement
  const startTime = getTimeFromTouchX(e, trackElement)
  const clientX = getClientX(e)

  dragState.value = {
    isDragging: true,
    mode: 'move',
    dayKey,
    segId: seg.id,
    startX: clientX,
    startTime,
    originalStart: seg.start,
    originalEnd: seg.end,
    moved: false,
    segElement: e.currentTarget as HTMLElement,
    trackElement
  }

  document.addEventListener('touchmove', handleMouseMove, { passive: false })
  document.addEventListener('touchend', handleMouseUp)
  e.preventDefault()
  e.stopPropagation()
}

const handleResizeStart = (e: MouseEvent, dayKey: string, seg: Seg, side: 'left' | 'right') => {
  if (e.button !== 0) return

  const trackElement = (e.currentTarget as HTMLElement).closest('.row_track') as HTMLElement
  const startTime = getTimeFromMouseX(e, trackElement)

  dragState.value = {
    isDragging: true,
    mode: side === 'left' ? 'resize-left' : 'resize-right',
    dayKey,
    segId: seg.id,
    startX: e.clientX,
    startTime,
    originalStart: seg.start,
    originalEnd: seg.end,
    moved: false,
    segElement: null,
    trackElement
  }

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
  e.preventDefault()
  e.stopPropagation()
}

const handleResizeTouchStart = (e: TouchEvent, dayKey: string, seg: Seg, side: 'left' | 'right') => {
  const trackElement = (e.currentTarget as HTMLElement).closest('.row_track') as HTMLElement
  const startTime = getTimeFromTouchX(e, trackElement)
  const clientX = getClientX(e)

  dragState.value = {
    isDragging: true,
    mode: side === 'left' ? 'resize-left' : 'resize-right',
    dayKey,
    segId: seg.id,
    startX: clientX,
    startTime,
    originalStart: seg.start,
    originalEnd: seg.end,
    moved: false,
    segElement: null,
    trackElement
  }

  document.addEventListener('touchmove', handleMouseMove, { passive: false })
  document.addEventListener('touchend', handleMouseUp)
  e.preventDefault()
  e.stopPropagation()
}

const handleMouseMove = (e: MouseEvent | TouchEvent) => {
  if (!dragState.value.isDragging || !dragState.value.trackElement) return

  const currentTime = 'touches' in e || 'changedTouches' in e
    ? getTimeFromTouchX(e as TouchEvent, dragState.value.trackElement)
    : getTimeFromMouseX(e as MouseEvent, dragState.value.trackElement)

  const currentX = getClientX(e)

  if (!dragState.value.moved) {
    const movedX = Math.abs(currentX - dragState.value.startX) > 3
    const movedT = Math.abs(currentTime - dragState.value.startTime) > 0.02
    if (movedX || movedT) {
      dragState.value.moved = true
    }
  }

  const segments = segmentsByDay[dragState.value.dayKey] || []
  const seg = segments.find(s => s.id === dragState.value.segId)
  if (!seg) return

  if (dragState.value.mode === 'create') {
    const start = Math.min(dragState.value.startTime, currentTime)
    const end = Math.max(dragState.value.startTime, currentTime)
    seg.start = start
    seg.end = end
  } else if (dragState.value.mode === 'move') {
    const duration = dragState.value.originalEnd - dragState.value.originalStart
    const offset = currentTime - dragState.value.startTime
    const newStart = Math.max(0, Math.min(24 - duration, dragState.value.originalStart + offset))
    const newEnd = newStart + duration
    if (!checkOverlap(dragState.value.dayKey, newStart, newEnd, seg.id)) {
      seg.start = newStart
      seg.end = newEnd
    }
  } else if (dragState.value.mode === 'resize-left') {
    const newStart = Math.max(0, Math.min(currentTime, dragState.value.originalEnd - 0.1))
    if (newStart < seg.end && !checkOverlap(dragState.value.dayKey, newStart, seg.end, seg.id)) {
      seg.start = newStart
    }
  } else if (dragState.value.mode === 'resize-right') {
    const newEnd = Math.min(24, Math.max(currentTime, dragState.value.originalStart + 0.1))
    if (newEnd > seg.start && !checkOverlap(dragState.value.dayKey, seg.start, newEnd, seg.id)) {
      seg.end = newEnd
    }
  }
}

const handleMouseUp = () => {
  if (dragState.value.isDragging) {
    // 点击时间段：未发生有效拖动，则打开编辑弹窗
    if (dragState.value.mode === 'move' && dragState.value.segId && !dragState.value.moved) {
      const segments = segmentsByDay[dragState.value.dayKey] || []
      const seg = segments.find(s => s.id === dragState.value.segId)
      if (seg) {
        seg.start = dragState.value.originalStart
        seg.end = dragState.value.originalEnd
      }
      openEditPop(dragState.value.dayKey, dragState.value.segId)
    }

    // 如果是创建模式且时间段太小，删除它
    if (dragState.value.mode === 'create' && dragState.value.segId) {
      const segments = segmentsByDay[dragState.value.dayKey] || []
      const seg = segments.find(s => s.id === dragState.value.segId)
      if (seg && (seg.end - seg.start < 0.1)) {
        const index = segments.indexOf(seg)
        if (index > -1) {
          segments.splice(index, 1)
        }
      }
    }

    dragState.value = {
      isDragging: false,
      mode: null,
      dayKey: '',
      segId: null,
      startX: 0,
      startTime: 0,
      originalStart: 0,
      originalEnd: 0,
      moved: false,
      segElement: null,
      trackElement: null
    }
  }

  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
  document.removeEventListener('touchmove', handleMouseMove)
  document.removeEventListener('touchend', handleMouseUp)
}

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
  document.removeEventListener('touchmove', handleMouseMove)
  document.removeEventListener('touchend', handleMouseUp)
})

const handleSegmentDelete = (dayKey: string, segId: string) => {
  const segments = segmentsByDay[dayKey]
  if (segments) {
    const index = segments.findIndex(s => s.id === segId)
    if (index > -1) {
      segments.splice(index, 1)
    }
  }
  if (selectedSegId.value === segId) {
    selectedSegId.value = null
  }
  if (editPop.visible && editPop.segId === segId) {
    closeEditPop()
  }
}

// 编辑时间段相关函数
const closeEditPop = () => {
  editPop.visible = false
  editPop.dayKey = ''
  editPop.segId = ''
}

const openEditPop = (dayKey: string, segId: string) => {
  const seg = (segmentsByDay[dayKey] || []).find(s => s.id === segId)
  if (!seg) return

  selectedSegId.value = segId
  editPop.dayKey = dayKey
  editPop.segId = segId

  const startTimeStr = hoursToTime(seg.start)
  const endTimeStr = hoursToTime(seg.end)

  editPop.start = startTimeStr
  editPop.end = endTimeStr

  // 转换为时间选择器格式 [hour, minute]
  const [startHour, startMin] = startTimeStr.split(':')
  const [endHour, endMin] = endTimeStr.split(':')

  editPop.startTime = [startHour, startMin]
  editPop.endTime = [endHour, endMin]

  editPop.visible = true
}

const handleEditDelete = () => {
  if (!editPop.dayKey || !editPop.segId) return
  handleSegmentDelete(editPop.dayKey, editPop.segId)
  closeEditPop()
}

const handleEditSave = () => {
  if (!editPop.dayKey || !editPop.segId) return

  // 从时间选择器的当前值读取时间
  const [startHour, startMin] = editPop.startTime
  const [endHour, endMin] = editPop.endTime

  const startTimeStr = `${String(startHour).padStart(2, '0')}:${String(startMin).padStart(2, '0')}:00`
  const endTimeStr = `${String(endHour).padStart(2, '0')}:${String(endMin).padStart(2, '0')}:00`

  const start = timeToHours(startTimeStr)
  const end = timeToHours(endTimeStr)
  if (!(start >= 0 && start <= 24 && end >= 0 && end <= 24)) {
    showToast('时间范围不合法')
    return
  }
  if (end <= start) {
    showToast('结束时间必须大于开始时间')
    return
  }
  if (checkOverlap(editPop.dayKey, start, end, editPop.segId)) {
    showToast('时间段重叠，请调整时间')
    return
  }

  const seg = (segmentsByDay[editPop.dayKey] || []).find(s => s.id === editPop.segId)
  if (!seg) return
  seg.start = start
  seg.end = end
  closeEditPop()
}

// 复制相关函数
const handleCopyClick = (dayKey: string) => {
  if (copyVisible.value) {
    return
  }
  sourceDayKey.value = dayKey
  copyDays.value = []
  copyVisible.value = true
}

const handleCopyConfirm = () => {
  if (!sourceDayKey.value || copyDays.value.length === 0) {
    copyVisible.value = false
    return
  }

  const sourceSegments = segmentsByDay[sourceDayKey.value] || []
  if (sourceSegments.length === 0) {
    copyVisible.value = false
    return
  }

  // 复制到选中的每个目标天
  copyDays.value.forEach(targetDayKey => {
    // 跳过源天本身
    if (targetDayKey === sourceDayKey.value) {
      return
    }

    // 先删除目标天的所有原有时间段
    segmentsByDay[targetDayKey] = []

    // 复制每个时间段
    sourceSegments.forEach(sourceSeg => {
      // 创建新的时间段，生成新的ID
      const newSeg: Seg = {
        id: generateSegId(),
        start: sourceSeg.start,
        end: sourceSeg.end
      }
      if (!segmentsByDay[targetDayKey]) {
        segmentsByDay[targetDayKey] = []
      }
      segmentsByDay[targetDayKey].push(newSeg)
    })
  })

  copyVisible.value = false
  copyDays.value = []
  sourceDayKey.value = ''
}

const updateRepeat = (value: string) => {
  const newForm = {
    ...formValue.value,
    repeat: value
  }
  if (props.form) {
    emit('update:form', newForm)
  } else {
    defaultForm.repeat = value
  }
}

const intervalValue = computed({
  get: () => String(formValue.value.interval || 1),
  set: (val) => {
    updateInterval(val)
  }
})

const updateInterval = (value: string | number) => {
  const numValue = Number(value)
  if (isNaN(numValue) || numValue < 1) {
    return
  }
  const newForm = {
    ...formValue.value,
    interval: numValue
  }
  if (props.form) {
    emit('update:form', newForm)
  } else {
    defaultForm.interval = numValue
  }
}
</script>

<style scoped lang="scss">
.schedule-mobile {
  border-radius: 12px;
}

.repeat_row {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}

.interval_row {
  margin-bottom: 12px;
  background: #fff;
  height: 48px;
}

.interval_input {
  :deep(.van-field__label) {
    width: 50px;
    display: flex;
    align-items: center;
    font-size: 14px;
    color: #666666;
    margin: 0;
  }

  :deep(.van-field__control) {
    text-align: right;
    font-size: 14px;
  }

  :deep(.van-field__right-icon) {
    padding-left: 12px;
    border-left: 1px solid #e3e3e3;
    margin-left: 12px;
  }
}

.interval_suffix {
  color:#999999;
  font-size:14px;
}

.repeat_tabs {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  border: 1px solid #2278ff;
  background: #fff;
  overflow: hidden;
}

.repeat_tab {
  padding: 6px 18px;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  background: transparent;
  margin: 2px;

  &.act {
    color: #2278ff;
    background: rgba(34, 120, 255, 0.12);
    border-radius: 999px;
  }
}

.timeline_wrap {
  margin-top: 10px;
  border-radius: 12px;
}

.timeline_row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 20px 12px 12px;
  margin-bottom: 12px;
  background:#f0f2f5;
  border-radius: 8px;
}

.row_label {
  width: 25px;
  color: #999999;
  font-size: 12px;
  margin-top: 35px;
}

.row_track_wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.row_ticks {
  position: relative;
  width: 100%;
  height: 22px;
  margin-bottom: 4px;
}

.row_tick {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;

  .row_tick_num {
    font-size: 12px;
    color: #999;
    line-height: 1;
    margin-bottom: 2px;
  }

  .row_tick_line {
    width: 1px;
    height: 6px;
    background: #d1d5db;
  }
}

.row_track {
  position: relative;
  height: 32px;
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
  cursor: crosshair;
  user-select: none;
  touch-action: none;
  -webkit-touch-callout: none;
}

.track_bg {
  position: absolute;
  inset: 0;
}

.seg {
  position: absolute;
  top: 0;
  height: 100%;
  background: #27b4ff;
  border-radius: 4px;
  overflow: visible;
}

.seg_resize_left,
.seg_resize_right {
  position: absolute;
  top: 0;
  width: 4px;
  height: 100%;
  background: rgba(255, 255, 255, 0.8);
}

.seg_resize_left {
  left: 0;
}

.seg_resize_right {
  right: 0;
}

.seg_delete {
  position: absolute;
  top: 0;
  right: 2px;
  width: 15px;
  height: 15px;
  display: none;
  align-items: center;
  justify-content: center;
  background: rgba(255, 77, 79, 0.9);
  border-radius: 50%;
  cursor: pointer;
  opacity: 1;
  transition: opacity 0.2s;
  z-index: 20;
  pointer-events: auto;

  &:hover{
    display: flex;
  }

  .van-icon {
    font-size: 8px;
    color: #fff;
  }
}

.row_copy {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 100;
  pointer-events: auto;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 4px;
  padding: 2px;

  img {
    width: 18px;
    height: 18px;
    pointer-events: none;
  }
}

// 编辑弹窗样式
.edit_pop_content {
  padding: 20px 16px;
}

.edit_pop_title {
  font-size: 18px;
  font-weight: 600;
  color: #222;
  text-align: center;
  margin-bottom: 24px;
}

.edit_time_row {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  gap: 12px;
  margin-bottom: 32px;
}

.edit_time_item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.edit_time_item :deep(.van-picker__toolbar) {
  display: none !important;
}

.edit_time_label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.edit_time_dash {
  color: #999;
  font-size: 16px;
  margin-top: 40px;
}

.edit_actions {
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.edit_delete_btn,
.edit_save_btn {
  flex: 1;
  height: 44px;
}

.edit_delete_btn {
  color: #ff4d4f;
  border-color: #ff4d4f;
}

// 复制弹窗样式
.copy_pop_content {
  padding: 20px 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.copy_pop_title {
  font-size: 18px;
  font-weight: 600;
  color: #222;
  text-align: center;
  margin-bottom: 20px;
}

.copy_days {
  flex: 1;
  overflow-y: auto;
  padding: 8px 16px;
}

:deep(.copy_checkbox_group) {
  display: grid !important;
  grid-template-columns: repeat(3, 1fr) !important;
  gap: 12px 8px !important;
  width: 100% !important;
}

.copy_day_item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-width: 0;
}

.copy_actions {
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  margin-top: auto;
}

.copy_cancel_btn,
.copy_ok_btn {
  flex: 1;
  height: 44px;
}

.copy_cancel_btn {
  color: #ff4d4f;
  border-color: #ff4d4f;
}
</style>

