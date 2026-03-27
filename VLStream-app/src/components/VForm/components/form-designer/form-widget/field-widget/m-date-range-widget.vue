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
      v-model="displayDateRange"
      :name="field.options.name"
      clickable
      readonly
      is-link
      :label="fieldLabel"
      :label-align="field.options.labelAlign"
      :label-width="fieldLabelWidth"
      :placeholder="placeHolder"
      :disabled="fieldDisabled || field.options.readonly"
      :required="field.options.required"
      :rules="rules"
      :size="size"
      @click="showCalendar"
    >
      <template #right-icon>
        <clearIcon v-if="clearable" />
      </template>
    </van-field>
    <van-calendar v-model:show="showPickerFlag" type="range" :default-date="defaultDate" :min-date="minDate" :max-date="maxDate" :poppable="true" @cancel="handleCancel" @confirm="handleConfirm" />
  </form-item-wrapper>
</template>

<script>
import { reactive, toRefs, computed, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'

import { formatDateRange, parseDate } from '~@/utils/date-util'
import clearIcon from '../components/clear.vue'
import dayjs from 'dayjs'

export default {
  componentName: 'FieldWidget',
  name: 'MDateRangeWidget',
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
    // const refList = inject('refList')
    // const globalOptionData = inject('globalOptionData')
    // const globalModel = inject('globalModel')

    const { i18nt } = useI18n()

    const data = reactive({
      defaultDate: null,
      fieldModel: [],
      noChangeEventFlag: true, // van-field组件没有change事件！！

      oldFieldValue: null, // field组件change之前的值

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

    const displayDateRange = computed(() => {
      if (data.fieldModel && data.fieldModel.length === 2) {
        return data.fieldModel[0] + '~' + data.fieldModel[1]
      } else {
        return ''
      }
    })

    // eslint-disable-next-line vue/return-in-computed-property
    const placeHolder = computed(() => {
      if (props.field.options.startPlaceholder && props.field.options.endPlaceholder) {
        return `${props.field.options.startPlaceholder} ~ ${props.field.options.endPlaceholder}`
      } else if (props.field.options.startPlaceholder) {
        return props.field.options.startPlaceholder
      } else if (props.field.options.endPlaceholder) {
        return props.field.options.endPlaceholder
      }
    })

    const minDate = computed(() => {
      if (!props.field.options.minDate) {
        return null
      }
      return parseDate(props.field.options.minDate, 'YYYY-MM-DD').toDate()
    })

    const maxDate = computed(() => {
      if (!props.field.options.maxDate) {
        return null
      }
      return parseDate(props.field.options.maxDate, 'YYYY-MM-DD').toDate()
    })

    const showCalendar = e => {
      if (!!props.designState) {
        data.showPickerFlag = false
        return
      }

      if (props.field.options.readonly || fieldMixin.isReadMode.value || fieldMixin.fieldDisabled.value) return
      if (!data.fieldModel) {
        data.defaultDate = [dayjs().toDate(), dayjs().add(1, 'd').toDate()]
      } else {
        data.defaultDate = [parseDate(data.fieldModel[0], props.field.options.format).toDate(), parseDate(data.fieldModel[1], props.field.options.format).toDate()]
      }

      data.showPickerFlag = true
      fieldMixin.handleFocusCustomEvent(e)
    }

    const handleConfirm = value => {
      data.fieldModel = formatDateRange(value, props.field.options.format)

      fieldMixin.syncUpdateFormModel(data.fieldModel)
      fieldMixin.handleOnConfirm(data.fieldModel)
      fieldMixin.handleChangeEvent(data.fieldModel)
      fieldMixin.handleBlurCustomEvent()

      data.showPickerFlag = false
    }
    const handleCancel = () => {
      data.showPickerFlag = false
    }

    /* 注意：子组件mounted在父组件created之后、父组件mounted之前触发，故子组件mounted需要用到的prop
         需要在父组件created中初始化！！ */
    fieldMixin.registerToRefList()
    fieldMixin.initFieldModel()
    fieldMixin.initEventHandler()
    fieldMixin.buildFieldRules()

    fieldMixin.handleOnCreated()

    return {
      i18nt,
      ...fieldMixin,
      ...toRefs(props),
      ...toRefs(data),

      displayDateRange,
      handleCancel,
      handleConfirm,
      maxDate,
      minDate,
      placeHolder,
      showCalendar
    }
  }
}
</script>
