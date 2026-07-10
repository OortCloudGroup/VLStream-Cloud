<template>
  <div class="cron-builder">
    <div class="cron-row" v-for="def in segmentDefs" :key="def.key">
      <div class="cron-label">{{ def.label }}</div>
      <el-select
        v-model="segments[def.key].mode"
        class="cron-mode"
        size="small"
        :disabled="disabled"
      >
        <el-option
          v-for="mode in getModeOptions(def)"
          :key="mode.value"
          :label="mode.label"
          :value="mode.value"
        />
      </el-select>
      <div class="cron-control">
        <template v-if="segments[def.key].mode === 'specific'">
          <el-select
            v-model="segments[def.key].specific"
            class="cron-select"
            size="small"
            multiple
            collapse-tags
            collapse-tags-tooltip
            filterable
            :disabled="disabled"
            placeholder="请选择"
          >
            <el-option
              v-for="option in getOptions(def)"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </template>
        <template v-else-if="segments[def.key].mode === 'range'">
          <el-input-number
            v-model="segments[def.key].range.start"
            size="small"
            :min="def.min"
            :max="def.max"
            :disabled="disabled"
          />
          <span class="cron-separator">-</span>
          <el-input-number
            v-model="segments[def.key].range.end"
            size="small"
            :min="def.min"
            :max="def.max"
            :disabled="disabled"
          />
        </template>
        <template v-else-if="segments[def.key].mode === 'interval'">
          <el-input-number
            v-model="segments[def.key].interval.start"
            size="small"
            :min="def.min"
            :max="def.max"
            :disabled="disabled"
          />
          <span class="cron-separator">/</span>
          <el-input-number
            v-model="segments[def.key].interval.step"
            size="small"
            :min="1"
            :max="def.max"
            :disabled="disabled"
          />
        </template>
        <template v-else>
          <span class="cron-hint">{{ getModeHint(def, segments[def.key].mode) }}</span>
        </template>
      </div>
    </div>
    <div class="cron-preview">
      <span class="preview-label">Cron表达式</span>
      <el-input :model-value="previewExpression" readonly class="preview-input" />
    </div>
    <div class="cron-note">提示：日与周只能指定其中一个，另一个会自动设置为“?”。</div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch, nextTick } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const weekOptions = [
  { label: '周日', value: 1 },
  { label: '周一', value: 2 },
  { label: '周二', value: 3 },
  { label: '周三', value: 4 },
  { label: '周四', value: 5 },
  { label: '周五', value: 6 },
  { label: '周六', value: 7 }
]

const segmentDefs = [
  { key: 'second', label: '秒', min: 0, max: 59 },
  { key: 'minute', label: '分', min: 0, max: 59 },
  { key: 'hour', label: '时', min: 0, max: 23 },
  { key: 'day', label: '日', min: 1, max: 31, supportsNone: true },
  { key: 'month', label: '月', min: 1, max: 12 },
  { key: 'week', label: '周', min: 1, max: 7, supportsNone: true, options: weekOptions },
  { key: 'year', label: '年', min: 1970, max: 2099 }
]

const segmentOrder = ['second', 'minute', 'hour', 'day', 'month', 'week', 'year']
const defMap = Object.fromEntries(segmentDefs.map(def => [def.key, def]))

const modeOptions = [
  { label: '每', value: 'every' },
  { label: '指定', value: 'specific' },
  { label: '区间', value: 'range' },
  { label: '间隔', value: 'interval' }
]

const noneOption = { label: '不指定', value: 'none' }
const optionsCache = new Map()
const isSyncing = ref(false)

const createSegmentState = (def, overrides = {}) => ({
  mode: overrides.mode ?? 'every',
  specific: Array.isArray(overrides.specific) ? [...overrides.specific] : [],
  range: {
    start: overrides.rangeStart ?? def.min,
    end: overrides.rangeEnd ?? def.max
  },
  interval: {
    start: overrides.intervalStart ?? def.min,
    step: overrides.intervalStep ?? 1
  }
})

const defaultSegments = () => ({
  second: createSegmentState(defMap.second, { mode: 'specific', specific: [0] }),
  minute: createSegmentState(defMap.minute, { mode: 'specific', specific: [0] }),
  hour: createSegmentState(defMap.hour, { mode: 'specific', specific: [0] }),
  day: createSegmentState(defMap.day, { mode: 'every' }),
  month: createSegmentState(defMap.month, { mode: 'every' }),
  week: createSegmentState(defMap.week, { mode: 'none' }),
  year: createSegmentState(defMap.year, { mode: 'every' })
})

const segments = reactive(defaultSegments())

const applySegmentState = (target, source) => {
  target.mode = source.mode
  target.specific = [...source.specific]
  target.range.start = source.range.start
  target.range.end = source.range.end
  target.interval.start = source.interval.start
  target.interval.step = source.interval.step
}

const resetSegments = () => {
  const defaults = defaultSegments()
  Object.keys(defaults).forEach(key => {
    applySegmentState(segments[key], defaults[key])
  })
}

const clamp = (value, min, max) => Math.min(Math.max(value, min), max)

const weekTokenMap = {
  SUN: 1,
  MON: 2,
  TUE: 3,
  WED: 4,
  THU: 5,
  FRI: 6,
  SAT: 7
}

const parseTokenToNumber = (token, def) => {
  if (token === null || token === undefined) return null
  const normalized = String(token).trim().toUpperCase()
  if (!normalized) return null
  if (def.key === 'week') {
    if (weekTokenMap[normalized]) {
      return weekTokenMap[normalized]
    }
  }
  const parsed = Number.parseInt(normalized, 10)
  if (Number.isNaN(parsed)) return null
  return clamp(parsed, def.min, def.max)
}

const parseSegment = (value, def) => {
  const raw = String(value || '').trim()
  if (!raw) {
    return createSegmentState(def, { mode: def.supportsNone ? 'none' : 'every' })
  }
  const normalized = raw.toUpperCase()
  if (normalized === '?' && def.supportsNone) {
    return createSegmentState(def, { mode: 'none' })
  }
  if (normalized === '*' || normalized === '?') {
    return createSegmentState(def, { mode: 'every' })
  }
  if (normalized.includes('/')) {
    const [startToken, stepToken] = normalized.split('/')
    const startValue = startToken === '*' ? def.min : parseTokenToNumber(startToken, def)
    const stepValue = Number.parseInt(stepToken, 10)
    if (startValue === null || Number.isNaN(stepValue)) {
      return createSegmentState(def, { mode: 'every' })
    }
    return createSegmentState(def, {
      mode: 'interval',
      intervalStart: clamp(startValue, def.min, def.max),
      intervalStep: clamp(Math.max(stepValue, 1), 1, def.max)
    })
  }
  if (normalized.includes('-')) {
    const [startToken, endToken] = normalized.split('-')
    const startValue = parseTokenToNumber(startToken, def)
    const endValue = parseTokenToNumber(endToken, def)
    if (startValue === null || endValue === null) {
      return createSegmentState(def, { mode: 'every' })
    }
    return createSegmentState(def, {
      mode: 'range',
      rangeStart: startValue,
      rangeEnd: endValue
    })
  }
  const list = normalized.split(',')
  const values = list
    .map(token => parseTokenToNumber(token, def))
    .filter(value => value !== null)
  if (values.length > 0) {
    return createSegmentState(def, { mode: 'specific', specific: values })
  }
  return createSegmentState(def, { mode: 'every' })
}

const normalizeSpecificValues = (def, values) => {
  const normalized = values
    .map(value => parseTokenToNumber(value, def))
    .filter(value => value !== null)
  const unique = Array.from(new Set(normalized))
  return unique.sort((a, b) => a - b)
}

const buildSegmentValue = (def, state) => {
  switch (state.mode) {
    case 'every':
      return '*'
    case 'none':
      return def.supportsNone ? '?' : '*'
    case 'specific': {
      const values = normalizeSpecificValues(def, state.specific)
      return values.length > 0 ? values.join(',') : '*'
    }
    case 'range': {
      let start = clamp(state.range.start, def.min, def.max)
      let end = clamp(state.range.end, def.min, def.max)
      if (start > end) {
        const temp = start
        start = end
        end = temp
      }
      return `${start}-${end}`
    }
    case 'interval': {
      const start = clamp(state.interval.start, def.min, def.max)
      const step = clamp(Math.max(state.interval.step, 1), 1, def.max)
      return `${start}/${step}`
    }
    default:
      return '*'
  }
}

const buildCronExpression = () => {
  const parts = segmentOrder.map(key => buildSegmentValue(defMap[key], segments[key]))
  return parts.join(' ')
}

const previewExpression = computed(() => buildCronExpression())

const getOptions = (def) => {
  if (def.options) return def.options
  if (optionsCache.has(def.key)) return optionsCache.get(def.key)
  const list = []
  for (let i = def.min; i <= def.max; i += 1) {
    list.push({ label: String(i), value: i })
  }
  optionsCache.set(def.key, list)
  return list
}

const getModeOptions = (def) => (def.supportsNone ? [...modeOptions, noneOption] : modeOptions)

const getModeHint = (def, mode) => {
  if (mode === 'every') return `每${def.label}`
  if (mode === 'none') return '不指定'
  return ''
}

const syncExpression = () => {
  if (isSyncing.value) return
  const nextValue = previewExpression.value
  const currentValue = String(props.modelValue || '').trim()
  if (!nextValue || nextValue === currentValue) return
  emit('update:modelValue', nextValue)
  emit('change', nextValue)
}

const syncFromModel = (value) => {
  const text = String(value || '').trim()
  const tokens = text ? text.split(/\s+/) : []
  const normalizedTokens = tokens.length === 6 ? [...tokens, '*'] : tokens
  isSyncing.value = true
  if (!normalizedTokens.length) {
    resetSegments()
  } else {
    segmentOrder.forEach((key, index) => {
      const def = defMap[key]
      const parsed = parseSegment(normalizedTokens[index] || '*', def)
      applySegmentState(segments[key], parsed)
    })
  }
  if (segments.day.mode !== 'none' && segments.week.mode !== 'none') {
    segments.week.mode = 'none'
  }
  if (segments.day.mode === 'none' && segments.week.mode === 'none') {
    segments.day.mode = 'every'
  }
  nextTick(() => {
    isSyncing.value = false
    if (!text) {
      syncExpression()
    }
  })
}

const ensureDayWeekRule = () => {
  if (segments.day.mode === 'none' && segments.week.mode === 'none') {
    segments.day.mode = 'every'
  }
}

watch(
  () => segments.day.mode,
  (mode) => {
    if (isSyncing.value) return
    if (mode !== 'none' && segments.week.mode !== 'none') {
      segments.week.mode = 'none'
    }
    ensureDayWeekRule()
  }
)

watch(
  () => segments.week.mode,
  (mode) => {
    if (isSyncing.value) return
    if (mode !== 'none' && segments.day.mode !== 'none') {
      segments.day.mode = 'none'
    }
    ensureDayWeekRule()
  }
)

watch(segments, syncExpression, { deep: true })
watch(() => props.modelValue, syncFromModel, { immediate: true })
</script>

<style scoped>
.cron-builder {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cron-row {
  display: grid;
  grid-template-columns: 40px 110px 1fr;
  gap: 12px;
  align-items: center;
}

.cron-label {
  font-size: 14px;
  color: #606266;
  text-align: right;
}

.cron-mode {
  width: 110px;
}

.cron-control {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.cron-select {
  min-width: 240px;
  flex: 1;
}

.cron-separator {
  color: #909399;
  font-size: 14px;
}

.cron-hint {
  font-size: 12px;
  color: #909399;
}

.cron-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.preview-label {
  font-size: 12px;
  color: #606266;
  min-width: 72px;
}

.preview-input {
  flex: 1;
}

.cron-note {
  font-size: 12px;
  color: #909399;
}

@media (max-width: 768px) {
  .cron-row {
    grid-template-columns: 1fr;
  }

  .cron-label {
    text-align: left;
  }

  .cron-mode {
    width: 100%;
  }
}
</style>
