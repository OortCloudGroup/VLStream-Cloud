<template>
  <form-item-wrapper
    :designer="designer"
    :field="field"
    :design-state="designState"
    :parent-widget="parentWidget"
    :parent-list="parentList"
    :index-of-parent-list="indexOfParentList"
    :sub-form-row-index="subFormRowIndex"
    :sub-form-col-index="subFormColIndex"
    :sub-form-row-id="subFormRowId"
  >
    <van-field
      ref="fieldEditor"
      v-model="fieldModel"
      :name="field.options.name"
      :label="fieldLabel"
      :rules="rules"
      :label-width="fieldLabelWidth"
      :label-align="field.options.labelAlign"
      :input-align="field.options.inputAlign"
      :disabled="fieldDisabled"
      :readonly="field.options.readonly || isReadMode"
      :required="field.options.required"
      :size="size"
      :type="field.options.type"
      :placeholder="field.options.placeholder"
      :maxlength="field.options.maxLength"
      :show-word-limit="field.options.showWordLimit"
      :clearable="field.options.clearable"
      :left-icon="field.options.leftIcon"
      :right-icon="field.options.rightIcon"
      @focus="handleFocusCustomEvent"
      @blur="handleBlurCustomEvent"
      @change="handleChangeEvent(fieldModel)"
      @update:model-value="handleInputCustomEvent"
    >
      <template v-if="!!field.options.appendButton" #button>
        <van-button :disabled="field.options.appendButtonDisabled" size="small" :type="field.options.appendButtonType" :icon="field.options.appendButtonIcon" @click="emitAppendButtonClick">
          {{ field.options.appendButtonLabel }}
        </van-button>
      </template>
    </van-field>
  </form-item-wrapper>
</template>

<script>
import { toRefs, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'

import FormValidators from '~@/utils/validators'
export default {
  componentName: 'FieldWidget',
  name: 'MInputWidget',
  components: {
    FormItemWrapper
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
    // const { proxy } = getCurrentInstance()

    const data = reactive({
      fieldModel: null,
      noChangeEventFlag: true, // van-field组件没有change事件！！
      oldFieldValue: null, // field组件change之前的值

      rules: [
        {
          label: props.field.options.label,
          message: props.field.options.validationHint,
          trigger: 'onBlur',
          validator: FormValidators['mobilePhone']
        }
      ]
    })

    const fieldMixin = useField(data)

    onMounted(() => {
      fieldMixin.handleOnMounted()
    })

    onBeforeUnmount(() => {
      fieldMixin.unregisterFromRefList()
    })

    fieldMixin.registerToRefList()
    fieldMixin.initFieldModel()
    fieldMixin.initEventHandler()

    fieldMixin.buildFieldRules()
    fieldMixin.handleOnCreated()

    return {
      i18nt,
      ...fieldMixin,
      // ...emitterMixin,
      ...toRefs(data)
    }
  }
}
</script>

<style scoped></style>
