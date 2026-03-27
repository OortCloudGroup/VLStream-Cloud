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
      @click="showTimePicker"
    >
      <template #right-icon>
        <clearIcon v-if="clearable" />
      </template>
    </van-field>
    <van-popup v-model:show="showPickerFlag" round position="bottom" teleport="body">
      <van-time-picker v-model="defaultDate" :columns-type="columnType" :title="field.options.placeholder" @cancel="handleCancel" @confirm="handleConfirm" />
    </van-popup>
  </form-item-wrapper>
</template>

<script>
import { toRefs, computed, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'

import { formatTime } from '~@/utils/date-util'
import clearIcon from '../components/clear.vue'
import dayjs from 'dayjs'

export default {
  componentName: 'FieldWidget',
  name: 'MTimeWidget',
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
      defaultDate: [],
      fieldModel: null,
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

    const columnType = computed(() => {
      let type = ['hour', 'minute', 'second']

      let typeColumns = (props.field.options.format || '--')
        .replace(/\:|时|分|秒/g, '-')
        .replace(/\-$/, '')
        .split('-')
      return type.splice(0, typeColumns.length)
    })

    const showTimePicker = e => {
      if (!!props.designState) {
        data.showPickerFlag = false
        return
      }

      if (props.field.options.readonly || fieldMixin.isReadMode.value || fieldMixin.fieldDisabled.value) return

      if (!data.fieldModel) {
        data.defaultDate = dayjs().toArray().splice(3, columnType.value.length)
      } else {
        data.defaultDate = dayjs('2000-01-01 ' + data.fieldModel)
          .toArray()
          .splice(3, columnType.value.length)
      }
      data.showPickerFlag = true
      fieldMixin.handleFocusCustomEvent(e)
    }
    const handleConfirm = value => {
      let selectedValues = [1970, 1, 1]
      value.selectedValues.forEach(x => {
        selectedValues.push(Number(x))
      })

      data.fieldModel = formatTime(selectedValues, props.field.options.format)

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

      props,
      showTimePicker
    }
  }
}
</script>

<style scoped></style>
