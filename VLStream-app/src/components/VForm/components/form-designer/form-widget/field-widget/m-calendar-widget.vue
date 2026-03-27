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
      readonly
      clickable
      is-link
      :label="fieldLabel"
      :label-align="labelAlign"
      :label-width="fieldLabelWidth"
      :placeholder="field.options.placeholder"
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
    <van-calendar v-model:show="showCalendarFlag" :default-date="defaultDate" :poppable="true" :min-date="minDate" :max-date="maxDate" @confirm="handleConfirm" />
  </form-item-wrapper>
</template>

<script>
import { reactive, toRefs, computed, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'
import { parseDate } from '~@/utils/date-util'
import clearIcon from '../components/clear.vue'
import dayjs from 'dayjs'

export default {
  componentName: 'FieldWidget',
  name: 'MCalendarWidget',
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
      fieldModel: null,
      noChangeEventFlag: true, // van-field组件没有change事件！！

      oldFieldValue: null, // field组件change之前的值

      rules: [],
      showCalendarFlag: false
    })
    const fieldMixin = useField(data)

    onMounted(() => {
      fieldMixin.handleOnMounted()
    })

    onBeforeUnmount(() => {
      fieldMixin.unregisterFromRefList()
    })

    const minDate = computed(() => {
      if (!props.field.options.minDate) {
        return new Date(new Date().getFullYear() - 10, 0, 1)
      }
      return parseDate(props.field.options.minDate, 'YYYY-MM-DD').toDate()
    })
    const maxDate = computed(() => {
      if (!props.field.options.maxDate) {
        return new Date(new Date().getFullYear() + 10, 11, 31)
      }
      return parseDate(props.field.options.maxDate, 'YYYY-MM-DD').toDate()
    })

    const showCalendar = e => {
      if (!!props.designState) {
        data.showCalendarFlag = false
        return
      }

      if (props.field.options.readonly || fieldMixin.isReadMode.value || fieldMixin.fieldDisabled.value) return

      if (!!props.designState) {
        data.showCalendarFlag = false
        return
      }
      if (!data.fieldModel) {
        data.defaultDate = dayjs().toDate()
      } else {
        data.defaultDate = dayjs(data.fieldModel).toDate()
      }

      data.showCalendarFlag = true
      fieldMixin.handleFocusCustomEvent(e)
    }

    const handleConfirm = value => {
      data.fieldModel = dayjs(value).format(props.field.options.format)

      fieldMixin.syncUpdateFormModel(data.fieldModel)
      fieldMixin.handleOnConfirm(data.fieldModel)
      fieldMixin.handleChangeEvent(data.fieldModel)
      fieldMixin.handleBlurCustomEvent()
      data.showCalendarFlag = false
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

      handleConfirm,
      maxDate,
      minDate,

      showCalendar
    }
  }
}
</script>

<style scoped></style>
