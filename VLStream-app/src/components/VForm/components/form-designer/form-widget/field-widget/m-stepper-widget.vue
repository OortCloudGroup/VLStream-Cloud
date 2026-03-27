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
      :name="field.options.name"
      :label="fieldLabel"
      :label-align="field.options.labelAlign"
      readonly
      :label-width="fieldLabelWidth"
      :disabled="fieldDisabled"
      :required="field.options.required"
      :size="size"
      :rules="rules"
    >
      <template #input>
        <van-stepper
          v-show="!field.options.readonly && !isReadMode"
          v-model="fieldModel"
          :disabled="fieldDisabled"
          :button-size="px2rem(field.options.stepButtonSize)"
          :min="field.options.min"
          :max="field.options.max"
          :step="field.options.step"
          :decimal-length="field.options.precision"
          @focus="handleFocusCustomEvent"
          @blur="handleBlurCustomEvent"
          @change="handleChangeEvent"
        />
        <template v-if="field.options.readonly || isReadMode">
          <span class="readonly-mode-field" :class="{ diabled: fieldDisabled }">{{ fieldModel }}</span>
        </template>
      </template>
    </van-field>
  </form-item-wrapper>
</template>

<script>
import { toRefs, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'
import { px2rem } from '~@/utils/util'

export default {
  componentName: 'FieldWidget',
  name: 'MStepperWidget',
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

    const data = reactive({
      fieldModel: null,
      noChangeEventFlag: true, // van-field组件没有change事件！！
      oldFieldValue: null, // field组件change之前的值
      px2rem: px2rem,

      rules: []
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
      ...toRefs(data)
    }
  }
}
</script>

<style scoped lang="scss">
.readonly-mode-field {
  height: 28px;
}
</style>
