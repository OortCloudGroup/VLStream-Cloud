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
      :label-width="fieldLabelWidth"
      :disabled="fieldDisabled"
      :required="field.options.required"
      :size="size"
      :rules="rules"
    >
      <template #input>
        <div class="field-rate-control">
          <van-rate
            ref="fieldEditor"
            v-model="fieldModel"
            size="25"
            :count="field.options.max"
            :allow-half="field.options.allowHalf"
            :disabled="fieldDisabled"
            :readonly="field.options.readonly || isReadMode"
            @change="handleChangeEvent"
          />
          <span v-if="field.options.showScore" class="score">{{ fieldModel }}</span>
        </div>
      </template>
    </van-field>
  </form-item-wrapper>
</template>

<script>
import { toRefs, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'

export default {
  componentName: 'FieldWidget',
  name: 'MRateWidget',
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

    const data = reactive({
      fieldModel: null,
      noChangeEventFlag: false,
      oldFieldValue: null, // field组件change之前的值

      rules: []
    })
    const fieldMixin = useField(data)

    onMounted(() => {
      fieldMixin.handleOnMounted()

      if (props.field.options.required) {
        data.rules.push({
          label: props.field.options.label,
          message: props.field.options.requiredHint || i18nt('render.hint.fieldRequired'),
          trigger: 'onBlur',
          validator: () => {
            return data.fieldModel > 0
          }
        })
      }
    })

    onBeforeUnmount(() => {
      fieldMixin.unregisterFromRefList()
    })

    fieldMixin.registerToRefList()
    fieldMixin.initFieldModel()
    fieldMixin.initEventHandler()
    // fieldMixin.buildFieldRules()

    fieldMixin.handleOnCreated()

    return {
      i18nt,
      ...fieldMixin,
      ...toRefs(data)
    }
  }
}
</script>

<style lang="scss" scoped>
.field-rate-control {
  display: inline-flex;
  align-items: center;
}
.score {
  display: inline-block;
  height: 25px;
  line-height: 25px;
  margin-left: 10px;
}
</style>
