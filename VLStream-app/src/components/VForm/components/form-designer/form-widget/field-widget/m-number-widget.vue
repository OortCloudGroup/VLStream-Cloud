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
      :label-width="fieldLabelWidth"
      :rules="rules"
      :disabled="fieldDisabled"
      :required="field.options.required"
      readonly
      :clickable="!field.options.disabled && !field.options.readonly"
      :size="size"
      :placeholder="field.options.placeholder"
      @focus="handleFocusCustomEvent"
    >
      <template v-if="fieldModel.length > 0" #input>
        <van-stepper
          v-if="!field.options.disabled && !field.options.readonly && !isReadMode"
          v-model="fieldModel"
          @change="handleChangeEvent"
        />
      </template>
    </van-field>
  </form-item-wrapper>
</template>

<script>
import { toRefs, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'

export default {
  componentName: 'FieldWidget',
  name: 'MNumberWidget',
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
  setup() {
    // const refList = inject('refList')
    // const globalOptionData = inject('globalOptionData')
    // const globalModel = inject('globalModel')

    const { i18nt } = useI18n()
    // const { proxy } = getCurrentInstance()

    const data = reactive({
      fieldModel: null,
      noChangeEventFlag: true, // van-field组件没有change事件！！
      oldFieldValue: null, // field组件change之前的值

      rules: [],
      showKeyboard: false
    })
    const fieldMixin = useField(data)

    watch(
      () => data.fieldModel,
      val => {
        fieldMixin.handleInputCustomEvent(val)
      }
    )

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
      ...toRefs(data)
    }
  }
}
</script>

<style scoped></style>
