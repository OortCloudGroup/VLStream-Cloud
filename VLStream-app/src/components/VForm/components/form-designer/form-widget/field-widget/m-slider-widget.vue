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
    <van-field :name="field.options.name" :label="fieldLabel" :label-align="field.options.labelAlign" :label-width="fieldLabelWidth" :disabled="fieldDisabled" :size="size" :rules="rules">
      <template #input>
        <van-slider
          ref="fieldEditor"
          v-model="fieldModel"
          :disabled="fieldDisabled || field.options.readonly || isReadMode"
          :min="field.options.min"
          :max="field.options.max"
          :step="field.options.step"
          :range="field.options.range"
          :vertical="field.options.vertical"
          @change="handleChangeEvent"
        />
      </template>
    </van-field>
  </form-item-wrapper>
</template>

<script>
import { reactive, toRefs, watch, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'

export default {
  componentName: 'FieldWidget',
  name: 'MSliderWidget',
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
    const { i18nt } = useI18n()

    const data = reactive({
      fieldModel: null,
      fieldModel2: null,
      noChangeEventFlag: false,
      oldFieldValue: null, // field组件change之前的值

      rules: []
    })
    const fieldMixin = useField(data)

    watch(
      () => props.field.options.range,
      val => {
        if (val) {
          data.fieldModel = [props.field.options.min, props.field.options.max]
        } else {
          // this.fieldModel=this.field.options.min;
        }
      }
    )

    const initFieldModel = () => {
      fieldMixin.initFieldModel()
      if (props.field.options.range) {
        data.fieldModel = [props.field.options.min, props.field.options.max]
      } else {
        // this.fieldModel=this.field.options.min;
      }
    }

    onMounted(() => {
      fieldMixin.handleOnMounted()
    })

    onBeforeUnmount(() => {
      fieldMixin.unregisterFromRefList()
    })

    fieldMixin.registerToRefList()
    initFieldModel()
    fieldMixin.initEventHandler()
    fieldMixin.handleOnCreated()

    return {
      i18nt,
      ...toRefs(data),
      ...fieldMixin
    }
  }
}
</script>

<style scoped></style>
