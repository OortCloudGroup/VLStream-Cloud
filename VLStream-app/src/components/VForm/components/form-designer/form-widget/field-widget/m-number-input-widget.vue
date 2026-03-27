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
      is-link
      :disabled="fieldDisabled"
      :required="field.options.required"
      readonly
      :clickable="!field.options.disabled && !field.options.readonly"
      :size="size"
      type="text"
      :placeholder="field.options.placeholder"
      @click="doShowKeyboard"
      @focus="handleFocusCustomEvent"
      @touchstart.stop.passive="doShowKeyboard"
    />
    <van-number-keyboard
      v-if="!field.options.disabled && !field.options.readonly && !isReadMode"
      v-model="fieldModel"
      :show="showKeyboard"
      :close-button-text="field.options.closeButtonText || i18nt('designer.setting.closeButtonDefaultText')"
      theme="custom"
      teleport="body"
      :extra-key="['00', '.']"
      @show="handleKeyboardShowEvent"
      @close="handleKeyboardCloseEvent"
      @blur="handleKeyboardBlurEvent"
    />
  </form-item-wrapper>
</template>

<script>
import { toRefs, reactive, watch, getCurrentInstance, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'
import { deepClone } from '~@/utils/util'

export default {
  componentName: 'FieldWidget',
  name: 'MNumberInputWidget',
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
    const { proxy } = getCurrentInstance()

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

    const doShowKeyboard = () => {
      if (!!props.designState) {
        data.showKeyboard = false
        return
      }

      data.showKeyboard = true
    }

    const handleKeyboardShowEvent = () => {
      data.oldFieldValue = deepClone(data.fieldModel)
    }

    const handleKeyboardCloseEvent = () => {
      // fieldMixin.syncUpdateFormModel(data.fieldModel * 1); //字符串转数字！！
      // fieldMixin.emitFieldDataChange(data.fieldModel * 1, data.oldFieldValue * 1);
      /* 主动触发表单的单个字段校验，用于清除字段可能存在的校验错误提示 */
      // proxy.dispatch('VmFormRender', 'fieldValidation', [fieldMixin.getPropName()]);
    }

    const handleKeyboardBlurEvent = () => {
      fieldMixin.syncUpdateFormModel(data.fieldModel * 1)
      fieldMixin.emitFieldDataChange(data.fieldModel * 1, data.oldFieldValue * 1)
      /* 主动触发表单的单个字段校验，用于清除字段可能存在的校验错误提示 */
      proxy.dispatch('VmFormRender', 'fieldValidation', [fieldMixin.getPropName()])

      data.showKeyboard = false
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

      doShowKeyboard,
      handleKeyboardBlurEvent,
      handleKeyboardCloseEvent,
      handleKeyboardShowEvent
    }
  }
}
</script>

<style scoped></style>
