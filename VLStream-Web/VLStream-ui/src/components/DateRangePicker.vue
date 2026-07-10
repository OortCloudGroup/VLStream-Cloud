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

// 定义 props
const props = defineProps({
  // v-model 绑定值
  modelValue: {
    type: Array,
    default: () => []
  },
  // 分隔符
  rangeSeparator: {
    type: String,
    default: '至'
  },
  // 开始日期占位符
  startPlaceholder: {
    type: String,
    default: '开始日期'
  },
  // 结束日期占位符
  endPlaceholder: {
    type: String,
    default: '结束日期'
  },
  // 值格式
  valueFormat: {
    type: String,
    default: 'YYYY-MM-DD'
  },
  // 显示格式
  format: {
    type: String,
    default: 'YYYY-MM-DD'
  },
  // 宽度
  width: {
    type: String,
    default: '420px'
  },
  // 尺寸
  size: {
    type: String,
    default: 'default',
    validator: (value) => ['large', 'default', 'small'].includes(value)
  },
  // 是否禁用
  disabled: {
    type: Boolean,
    default: false
  },
  // 是否可清空
  clearable: {
    type: Boolean,
    default: true
  },
  // 快捷选项
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
  // 禁用日期函数
  disabledDate: {
    type: Function,
    default: null
  }
})

// 定义 emits
const emit = defineEmits(['update:modelValue', 'change'])

// 处理值更新
const handleUpdate = (value) => {
  emit('update:modelValue', value)
  emit('change', value)
}

// 调试：打印当前宽度值
// console.log('DateRangePicker width:', props.width)
</script>

<style scoped>
.date-range-picker-wrapper {
  width: v-bind(width);
  min-width: v-bind(width);
  max-width: v-bind(width);
}

/* 确保内部组件占满容器宽度 */
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