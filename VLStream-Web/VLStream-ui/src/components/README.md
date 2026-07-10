# 组件使用文档

## DateRangePicker 日期范围选择器

一个基于 Element Plus `el-date-picker` 封装的日期范围选择器组件，提供了统一的配置和样式。

### 基础用法

```vue
<template>
  <DateRangePicker v-model="dateRange" />
</template>

<script setup>
import { ref } from 'vue'
import DateRangePicker from '@/components/DateRangePicker.vue'

const dateRange = ref([])
</script>
```

### 自定义配置

```vue
<template>
  <DateRangePicker
    v-model="dateRange"
    start-placeholder="开始日期"
    end-placeholder="结束日期"
    value-format="YYYY-MM-DD HH:mm:ss"
    format="YYYY-MM-DD HH:mm:ss"
    width="400px"
    @change="handleDateChange"
  />
</template>

<script setup>
import { ref } from 'vue'
import DateRangePicker from '@/components/DateRangePicker.vue'

const dateRange = ref([])

const handleDateChange = (value) => {
  console.log('日期范围改变:', value)
}
</script>
```

### Props 属性

| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| modelValue | Array | [] | v-model 绑定值 |
| rangeSeparator | String | '至' | 分隔符 |
| startPlaceholder | String | '开始日期' | 开始日期占位符 |
| endPlaceholder | String | '结束日期' | 结束日期占位符 |
| valueFormat | String | 'YYYY-MM-DD' | 值格式 |
| format | String | 'YYYY-MM-DD' | 显示格式 |
| width | String | '200px' | 组件宽度 |
| size | String | 'default' | 尺寸 (large/default/small) |
| disabled | Boolean | false | 是否禁用 |
| clearable | Boolean | true | 是否可清空 |
| shortcuts | Array | 默认快捷选项 | 快捷选项配置 |
| disabledDate | Function | null | 禁用日期函数 |

### Events 事件

| 事件名 | 说明 | 回调参数 |
|--------|------|----------|
| update:modelValue | 值改变时触发 | (value: Array) |
| change | 值改变时触发 | (value: Array) |

### 默认快捷选项

组件提供了以下默认快捷选项：
- 最近一周
- 最近一个月
- 最近三个月

### 自定义快捷选项

```vue
<template>
  <DateRangePicker
    v-model="dateRange"
    :shortcuts="customShortcuts"
  />
</template>

<script setup>
import { ref } from 'vue'
import DateRangePicker from '@/components/DateRangePicker.vue'

const dateRange = ref([])

const customShortcuts = [
  {
    text: '今天',
    value: (() => {
      const today = new Date()
      return [today, today]
    })()
  },
  {
    text: '昨天',
    value: (() => {
      const yesterday = new Date()
      yesterday.setTime(yesterday.getTime() - 3600 * 1000 * 24)
      return [yesterday, yesterday]
    })()
  }
]
</script>
```

### 禁用特定日期

```vue
<template>
  <DateRangePicker
    v-model="dateRange"
    :disabled-date="disabledDate"
  />
</template>

<script setup>
import { ref } from 'vue'
import DateRangePicker from '@/components/DateRangePicker.vue'

const dateRange = ref([])

// 禁用今天之后的日期
const disabledDate = (time) => {
  return time.getTime() > Date.now()
}
</script>
```

### 已应用页面

该组件已在以下页面中使用：
- 监控告警页面 (`/monitoring-alarm`)
- 标签管理页面 (`/tag-management`)
- 智能分析结果页面 (`/intelligent-analysis-result`)

### 默认配置

- 分隔符：`至`
- 开始占位符：`开始日期`
- 结束占位符：`结束日期`
- 值格式：`YYYY-MM-DD`
- 显示格式：`YYYY-MM-DD`
- 宽度：`200px`
- 内置快捷选项：最近一周/一个月/三个月

### 注意事项

1. 该组件基于 Element Plus 的 `el-date-picker` 组件封装
2. 支持所有 `el-date-picker` 的原生属性和事件
3. 默认提供中文的快捷选项和占位符
4. 可以通过 CSS 变量或深度选择器自定义样式 