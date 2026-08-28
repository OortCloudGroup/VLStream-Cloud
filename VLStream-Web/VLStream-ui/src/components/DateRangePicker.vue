<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <div class="date-range-picker-wrapper" :style="{ width: width }">
    <el-date-picker
      :model-value="modelValue"
      @update:model-value="handleUpdate"
      type="daterange"
      :range-separator="rangeSeparator"
      :start-placeholder="startPlaceholder"
      :end-placeholder="endPlaceholder"
      :value-format="valueFormat"
      :format="format"
      :style="{ width: '100%' }"
      :size="size"
      :disabled="disabled"
      :clearable="clearable"
      :shortcuts="shortcuts"
      :disabled-date="disabledDate"
    />
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

// props
const props = defineProps({
  // v-model value
  modelValue: {
    type: Array,
    default: () => []
  },
  //
  rangeSeparator: {
    type: String,
    default: '至'
  },
  // start
  startPlaceholder: {
    type: String,
    default: '开始日期'
  },
  // finish
  endPlaceholder: {
    type: String,
    default: '结束日期'
  },
  // value
  valueFormat: {
    type: String,
    default: 'YYYY-MM-DD'
  },
  //
  format: {
    type: String,
    default: 'YYYY-MM-DD'
  },
  //
  width: {
    type: String,
    default: '420px'
  },
  //
  size: {
    type: String,
    default: 'default',
    validator: (value) => ['large', 'default', 'small'].includes(value)
  },
  // whether
  disabled: {
    type: Boolean,
    default: false
  },
  // whether null / empty
  clearable: {
    type: Boolean,
    default: true
  },
  // item
  shortcuts: {
    type: Array,
    default: () => [
      {
        text: '最近一周',
        value: (() => {
          const end = new Date()
          const start = new Date()
          start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
          return [start, end]
        })()
      },
      {
        text: '最近一个月',
        value: (() => {
          const end = new Date()
          const start = new Date()
          start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
          return [start, end]
        })()
      },
      {
        text: '最近三个月',
        value: (() => {
          const end = new Date()
          const start = new Date()
          start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
          return [start, end]
        })()
      }
    ]
  },
  //
  disabledDate: {
    type: Function,
    default: null
  }
})

// emits
const emit = defineEmits(['update:modelValue', 'change'])

// Process value new
const handleUpdate = (value) => {
  emit('update:modelValue', value)
  emit('change', value)
}

// : current value
// console.log('DateRangePicker width:', props.width)
</script>

<style scoped>
.date-range-picker-wrapper {
  width: v-bind(width);
  min-width: v-bind(width);
  max-width: v-bind(width);
}

/* component */
:deep(.el-date-editor) {
  width: 100% !important;
  min-width: 100% !important;
  max-width: 100% !important;
}

:deep(.el-date-editor--daterange) {
  width: 100% !important;
  min-width: 100% !important;
  max-width: 100% !important;
}

:deep(.el-input) {
  width: 100% !important;
  min-width: 100% !important;
  max-width: 100% !important;
}

:deep(.el-input__wrapper) {
  width: 100% !important;
  min-width: 100% !important;
  max-width: 100% !important;
}

:deep(.el-range-editor) {
  width: 100% !important;
  min-width: 100% !important;
  max-width: 100% !important;
}

:deep(.el-range-editor.el-input__wrapper) {
  width: 100% !important;
  min-width: 100% !important;
  max-width: 100% !important;
}
</style>