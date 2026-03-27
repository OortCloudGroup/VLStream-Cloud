<template>
  <div class="defense-time-mobile">
    <ScheduleTimelineSimpleMobile
      :form="formValue"
      :time-periods="timePeriodsValue"
      @update:form="handleFormUpdate"
      @update:time-periods="handleTimePeriodsUpdate"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive, watch, computed, ref } from 'vue'
import ScheduleTimelineSimpleMobile from './ScheduleTimelineSimpleMobile.vue'

type TimePeriodItem =
  | { start: string; end: string }
  | { interval: number }
  | { weekday: number }
  | { day: number }

type TimePeriods =
  | TimePeriodItem[]
  | TimePeriodItem[][]

interface ProtectionTime {
  frequency: string
  time_periods: TimePeriods
}

interface FormData {
  repeat: string
  interval: number
}

const props = defineProps<{
  protectionTime?: ProtectionTime
}>()

const emit = defineEmits<{
  'update:protection-time': [value: ProtectionTime]
}>()

const frequencyMap: Record<string, string> = {
  每天: 'day',
  隔天: 'next',
  每周: 'week',
  每月: 'month'
}

const frequencyMapReverse: Record<string, string> = {
  day: '每天',
  next: '隔天',
  week: '每周',
  month: '每月'
}

const defaultForm = reactive<FormData>({
  repeat: 'day',
  interval: 1
})

const formValue = computed<FormData>(() => {
  if (props.protectionTime) {
    const repeat = frequencyMap[props.protectionTime.frequency] || 'day'
    let interval = 1

    if (repeat === 'next' && props.protectionTime.time_periods) {
      const periods = props.protectionTime.time_periods as TimePeriodItem[]
      const intervalObj = periods.find((p: any) => (p as any).interval !== undefined) as { interval: number } | undefined
      if (intervalObj) {
        interval = intervalObj.interval
      }
    }

    return {
      repeat,
      interval
    }
  }
  return defaultForm
})

const timePeriodsCache = reactive<Record<string, TimePeriods | undefined>>({
  day: undefined,
  next: undefined,
  week: undefined,
  month: undefined
})

const normalizeNextTimePeriods = (val: TimePeriods | undefined, interval: number): TimePeriods => {
  const periods = (Array.isArray(val) ? (val as TimePeriodItem[]) : []) as TimePeriodItem[]
  const segPeriods = periods.filter((p: any) => (p as any).start && (p as any).end) as TimePeriodItem[]
  return [{ interval }, ...segPeriods]
}

const timePeriodsValue = computed<TimePeriods | undefined>(() => {
  const key = formValue.value.repeat
  return timePeriodsCache[key] ?? props.protectionTime?.time_periods
})

const isUpdatingFromEmit = ref(false)

const isTimePeriodsEqual = (a: TimePeriods | undefined, b: TimePeriods | undefined): boolean => {
  if (a === b) return true
  if (!a || !b) return false
  return JSON.stringify(a) === JSON.stringify(b)
}

const handleFormUpdate = (val: FormData) => {
  if (props.protectionTime) {
    const currentKey = frequencyMap[props.protectionTime.frequency] || 'day'
    timePeriodsCache[currentKey] = props.protectionTime.time_periods

    const nextFrequency = frequencyMapReverse[val.repeat] || '每天'
    let nextTimePeriods: TimePeriods = timePeriodsCache[val.repeat] ?? []

    if (val.repeat === 'next') {
      nextTimePeriods = normalizeNextTimePeriods(nextTimePeriods, val.interval)
      timePeriodsCache.next = nextTimePeriods
    }

    if (val.repeat === 'next' && (!Array.isArray(nextTimePeriods) || nextTimePeriods.length === 0)) {
      nextTimePeriods = [{ interval: val.interval }]
    }

    if (
      nextFrequency === props.protectionTime.frequency &&
      isTimePeriodsEqual(nextTimePeriods, props.protectionTime.time_periods)
    ) {
      return
    }

    isUpdatingFromEmit.value = true
    const updated: ProtectionTime = {
      frequency: nextFrequency,
      time_periods: nextTimePeriods
    }
    emit('update:protection-time', updated)
    setTimeout(() => {
      isUpdatingFromEmit.value = false
    }, 0)
  }
}

const handleTimePeriodsUpdate = (val: TimePeriods) => {
  if (props.protectionTime) {
    if (isTimePeriodsEqual(val, props.protectionTime.time_periods)) {
      return
    }
    const key = frequencyMap[props.protectionTime.frequency] || 'day'
    timePeriodsCache[key] = val
    isUpdatingFromEmit.value = true
    const updated: ProtectionTime = {
      frequency: props.protectionTime.frequency,
      time_periods: val
    }
    emit('update:protection-time', updated)
    setTimeout(() => {
      isUpdatingFromEmit.value = false
    }, 0)
  }
}

watch(
  () => props.protectionTime,
  (val) => {
    if (isUpdatingFromEmit.value) {
      return
    }
    if (val) {
      defaultForm.repeat = frequencyMap[val.frequency] || 'day'

      if (defaultForm.repeat === 'next' && val.time_periods) {
        const periods = val.time_periods as TimePeriodItem[]
        const intervalObj = periods.find((p: any) => (p as any).interval !== undefined) as { interval: number } | undefined
        if (intervalObj) {
          defaultForm.interval = intervalObj.interval
        }
      }

      const key = frequencyMap[val.frequency] || 'day'
      if (!isTimePeriodsEqual(timePeriodsCache[key], val.time_periods)) {
        timePeriodsCache[key] = val.time_periods
      }
    }
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
.defense-time-mobile {
  width: 100%;
}
</style>

