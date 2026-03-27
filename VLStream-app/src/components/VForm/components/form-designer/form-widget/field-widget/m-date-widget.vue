<template>
  <form-item-wrapper
    :designer="designer"
    :field="field"
    :design-state="designState"
    :title="field.options.label"
    :parent-widget="parentWidget"
    :parent-list="parentList"
    :index-of-parent-list="indexOfParentList"
    :sub-form-row-index="subFormRowIndex"
    :sub-form-col-index="subFormColIndex"
    :sub-form-row-id="subFormRowId"
  >
    <van-field
      v-model="fieldModel"
      :name="field.options.name"
      clickable
      readonly
      is-link
      :label="fieldLabel"
      :label-width="fieldLabelWidth"
      :placeholder="field.options.placeholder"
      :disabled="fieldDisabled || field.options.readonly"
      :required="field.options.required"
      :rules="rules"
      :size="size"
      @click="showDatePicker"
    >
      <template v-if="field.options.datetype === 'datetime'" #input>
        <div :class="{ 'disableDiv': fieldDisabled || field.options.readonly }">
          {{ formatDisplayDate(fieldModel) }}
        </div>
      </template>
      <template #right-icon>
        <clearIcon v-if="clearable && !(fieldDisabled || field.options.readonly)" />
      </template>
    </van-field>
    <van-popup v-model:show="showPickerFlag" round position="bottom" teleport="body">
      <van-picker
        v-if="field.options.datetype=== 'datetime'"
        v-model="currentDate"
        :columns="columns"
        @confirm="handleConfirm"
        @cancel="handleCancel"
      />
      <van-date-picker
        v-else
        v-model="currentDate"
        :columns-type="columnType"
        :title="field.options.placeholder"
        :min-date="minDate"
        :max-date="maxDate"
        @confirm="handleConfirm"
        @cancel="handleCancel"
      />
    </van-popup>
  </form-item-wrapper>
</template>

<script>
import { reactive, toRefs, onMounted, onBeforeUnmount, computed } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'
import { formatDate, parseDate } from '~@/utils/date-util'
import clearIcon from '../components/clear.vue'
import dayjs from 'dayjs'

export default {
  componentName: 'FieldWidget',
  name: 'MDateWidget',
  components: {
    FormItemWrapper,
    clearIcon
  },
  props: {
    designState: {
      default: false,
      type: Boolean
    },
    designer: Object,
    field: Object,
    indexOfParentList: Number,
    parentList: Array,

    parentWidget: Object,

    subFormColIndex: {
      default: -1,
      /* 子表单组件列索引，从0开始计数 */
      type: Number
    },
    subFormRowId: {
      default: '',
      /* 子表单组件行Id，唯一id且不可变 */
      type: String
    },
    subFormRowIndex: {
      default: -1,
      /* 子表单组件行索引，从0开始计数 */
      type: Number
    }
  },
  setup(props) {
    const minDate = computed(() => {
      if (!props.field.options.minDate) {
        return undefined
      }
      return parseDate(props.field.options.minDate, 'YYYY-MM-DD').toDate()
    })
    const maxDate = computed(() => {
      if (!props.field.options.maxDate) {
        return undefined
      }
      return parseDate(props.field.options.maxDate, 'YYYY-MM-DD').toDate()
    })

    const getDateValues = () => {
      const minDateValue = minDate.value ? dayjs(minDate.value) : dayjs().subtract(15, 'day')
      const maxDateValue = maxDate.value ? dayjs(maxDate.value) : dayjs().add(15, 'day')
      const dateRange = maxDateValue.diff(minDateValue, 'day') + 1 // 计算日期范围

      return Array.from({ length: dateRange }, (_, i) => {
        const date = minDateValue.add(i, 'day') // 根据minDate计算日期
        const weekDay = date.format('d') // 获取周几，0为周日，1为周一，依此类推
        const weekDayText = ['日', '一', '二', '三', '四', '五', '六'][weekDay] // 转换为中文星期

        let displayDate
        if (date.isSame(dayjs(), 'day')) {
          displayDate = '今天'
        } else if (date.isSame(dayjs().add(1, 'day'), 'day')) {
          displayDate = '明天'
        } else if (date.isSame(dayjs().subtract(1, 'day'), 'day')) {
          displayDate = '昨天'
        } else {
          displayDate = `${date.format('M月D日')} 星期${weekDayText}`
        }

        return {
          text: displayDate,
          value: date.format('YYYY-MM-DD')
        }
      })
    }

    const getHourValues = () => {
      return Array.from({ length: 24 }, (_, i) => {
        return {
          text: i < 10 ? `0${i}` : `${i}`,
          value: i
        }
      })
    }

    const getMinuteValues = () => {
      return Array.from({ length: 60 }, (_, i) => {
        return {
          text: i < 10 ? `0${i}` : `${i}`,
          value: i
        }
      })
    }

    const columns = computed(() => {
      return [getDateValues(), getHourValues(), getMinuteValues()
      ]
    })

    // const refList = inject('refList')
    // const globalOptionData = inject('globalOptionData')
    // const globalModel = inject('globalModel')

    const { i18nt } = useI18n()

    const data = reactive({
      // 默认显示为今天的日期和时间
      currentDate: [], // new Date()
      fieldModel: '',
      noChangeEventFlag: true, // van-field组件没有change事件！！

      oldFieldValue: '', // field组件change之前的值

      rules: [],
      showPickerFlag: false
    })
    const fieldMixin = useField(data)

    onMounted(() => {
      fieldMixin.handleOnMounted()
    })

    onBeforeUnmount(() => {
      fieldMixin.unregisterFromRefList()
    })

    const columnType = computed(() => {
      let type = ['year', 'month', 'day']
      let typeColumns = (props.field.options.format || 'yyyy-MM-dd')
        .replace(/\/|年|月|日/g, '-')
        .replace(/\-$/, '')
        .split('-')
      return type.splice(0, typeColumns.length)
    })

    onMounted(() => {
      fieldMixin.handleOnMounted()
    })

    onBeforeUnmount(() => {
      fieldMixin.unregisterFromRefList()
    })

    const showDatePicker = e => {
      if (!!props.designState) {
        data.showPickerFlag = false
        return
      }

      if (props.field.options.readonly || fieldMixin.isReadMode.value || fieldMixin.fieldDisabled.value) return

      // 区分date-type 的显示 datetime 显示日期时间 其他的照旧
      // console.log(dayjs().add(1, 'M').toArray().splice(0, 3))
      if (props.field.options.datetype === 'datetime') {
        let dateStr = ''
        if (!data.fieldModel) {
          dateStr = dayjs().format('YYYY-MM-DD HH:mm')
        } else {
          dateStr = dayjs(data.fieldModel).format('YYYY-MM-DD HH:mm')
        }
        const dateArr = dateStr.split(' ')
        const timeArr = dateArr[1].split(':')
        data.currentDate = [dateArr[0], parseInt(timeArr[0]), parseInt(timeArr[1])]
      } else {
        if (!data.fieldModel) {
          data.currentDate = dayjs().add(1, 'M').toArray().splice(0, 3)
        } else {
          data.currentDate = dayjs(data.fieldModel).add(1, 'M').toArray().splice(0, 3)
        }
      }

      data.showPickerFlag = true
      fieldMixin.handleFocusCustomEvent(e)
    }

    const formatDisplayDate = date => {
      if (!date) {
        return ''
      }
      date = dayjs(date)

      const weekDay = date.format('d') // 获取周几，0为周日，1为周一，依此类推
      const weekDayText = ['日', '一', '二', '三', '四', '五', '六'][weekDay] // 转换为中文星期

      let displayDate
      if (date.isSame(dayjs(), 'day')) {
        displayDate = '今天'
      } else if (date.isSame(dayjs().add(1, 'day'), 'day')) {
        displayDate = '明天'
      } else if (date.isSame(dayjs().subtract(1, 'day'), 'day')) {
        displayDate = '昨天'
      } else {
        displayDate = `${date.format('M月D日')} 星期${weekDayText}`
      }
      const displayHour = date.format('HH')
      const displayMinute = date.format('mm')

      return displayDate + ' ' + displayHour + ':' + displayMinute
    }

    const handleConfirm = value => {
      if (props.field.options.datetype === 'datetime') {
        const [dateString, hour, minute] = value.selectedValues
        const formattedDate = dayjs(dateString).set('hour', hour).set('minute', minute).format('YYYY-MM-DD HH:mm', props.field.options.format)
        data.fieldModel = formattedDate
      } else {
        data.fieldModel = formatDate(value.selectedValues, props.field.options.format)
      }

      fieldMixin.syncUpdateFormModel(data.fieldModel)
      fieldMixin.handleOnConfirm(data.fieldModel)
      fieldMixin.handleChangeEvent(data.fieldModel)
      fieldMixin.handleBlurCustomEvent()
      data.showPickerFlag = false
    }

    const handleCancel = () => {
      data.showPickerFlag = false
    }

    fieldMixin.registerToRefList()
    fieldMixin.initFieldModel()
    fieldMixin.initEventHandler()
    fieldMixin.buildFieldRules()

    fieldMixin.handleOnCreated()

    return {
      i18nt,
      ...fieldMixin,
      ...toRefs(data),

      columnType,
      handleCancel,
      handleConfirm,
      maxDate,
      minDate,
      showDatePicker,
      columns,
      formatDisplayDate
    }
  }
}
</script>

<style lang="scss" scoped>
  .disableDiv {
    color: var(--van-field-input-disabled-text-color) !important;
    cursor: not-allowed !important;
    opacity: 1 !important;
  }
</style>
