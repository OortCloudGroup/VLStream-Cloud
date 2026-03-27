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
      @click="showDatetimePicker"
    >
      <template #right-icon>
        <clearIcon v-if="clearable" />
      </template>
    </van-field>
    <van-popup v-model:show="showDatePickerFlag" round position="bottom" teleport="body">
      <van-picker-group
        :title="field.options.label"
        :tabs="[i18nt('render.hint.datePlaceholder'), i18nt('render.hint.timePlaceholder')]"
        :confirm-button-text="i18nt('render.hint.confirm')"
        :cancel-button-text="i18nt('render.hint.cancel')"
        @confirm="handleTimeConfirm"
        @cancel="handleCancel"
      >
        <van-date-picker v-model="currentDate" :columns-type="dateColumnType" :min-date="minDate" :max-date="maxDate" />
        <van-time-picker v-model="currentTime" :columns-type="timeColumnType" />
      </van-picker-group>
    </van-popup>
  </form-item-wrapper>
</template>

<script>
import { reactive, toRefs, onMounted, onBeforeUnmount, computed } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'
import { formatDateTime, parseDate } from '~@/utils/date-util'
import clearIcon from '../components/clear.vue'
import dayjs from 'dayjs'

export default {
  componentName: 'FieldWidget',
  name: 'MDatetimeWidget',
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
      currentDate: [], // new Date(),
      currentTime: [],
      fieldModel: '',

      noChangeEventFlag: true, // van-field组件没有change事件！！

      oldFieldValue: '', // field组件change之前的值
      rules: [],
      showDatePickerFlag: false,
      showTimePickerFlag: false
    })

    const fieldMixin = useField(data)

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

    onMounted(() => {
      fieldMixin.handleOnMounted()
    })

    onBeforeUnmount(() => {
      fieldMixin.unregisterFromRefList()
    })

    const dateColumnType = computed(() => {
      let type = ['year', 'month', 'day']
      return type
    })

    const timeColumnType = computed(() => {
      let type = ['hour', 'minute', 'second']
      let typeColumns = (props.field.options.format || 'yyyy-MM-dd HH:mm:ss')
        .replace(/\:|\/|年|月|日|时|分|秒/g, '-')
        .replace(/\s/, '-')
        .replace(/--/g, '-')
        .replace(/\-$/, '')
        .split('-')
      typeColumns.splice(0, 3)
      return type.splice(0, typeColumns.length)
    })

    const showDatetimePicker = e => {
      if (!!props.designState) {
        data.showDatePickerFlag = false
        return
      }

      if (props.field.options.readonly || fieldMixin.isReadMode.value || fieldMixin.fieldDisabled.value) return

      if (!data.fieldModel) {
        data.currentDate = dayjs().add(1, 'M').toArray().splice(0, 3)
        data.currentTime = dayjs().add(1, 'M').toArray().splice(3, timeColumnType.value.length)
      } else {
        data.currentDate = dayjs(data.fieldModel).add(1, 'M').toArray().splice(0, 3)
        data.currentTime = dayjs(data.fieldModel).add(1, 'M').toArray().splice(3, timeColumnType.value.length)
      }
      data.showDatePickerFlag = true
      fieldMixin.handleFocusCustomEvent(e)
    }

    const handleDateConfirm = () => {
      // data.showTimePickerFlag = true;
      // data.showDatePickerFlag = false;
    }

    const handleCancel = () => {
      data.showDatePickerFlag = false
      data.showTimePickerFlag = false
    }

    const handleTimeConfirm = () => {
      data.fieldModel = formatDateTime([...data.currentDate, ...data.currentTime], props.field.options.format)

      fieldMixin.syncUpdateFormModel(data.fieldModel)
      fieldMixin.handleOnConfirm(data.fieldModel)
      fieldMixin.handleChangeEvent(data.fieldModel)
      fieldMixin.handleBlurCustomEvent()

      data.showDatePickerFlag = false
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
      ...toRefs(data),

      dateColumnType,
      handleCancel,
      handleDateConfirm,
      handleTimeConfirm,

      maxDate,
      minDate,
      showDatetimePicker,

      timeColumnType
    }
  }
}
</script>

<style scoped></style>
