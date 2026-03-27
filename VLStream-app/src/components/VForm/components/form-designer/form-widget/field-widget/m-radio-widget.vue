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
        <van-radio-group
          v-show="!field.options.readonly && !isReadMode"
          ref="fieldEditor"
          v-model="fieldModel"
          :direction="field.options.displayStyle === 'inline' ? 'horizontal' : 'vertical'"
          :disabled="fieldDisabled"
          @change="handleChangeEvent"
        >
          <van-radio
            v-for="(item, index) in field.options.optionItems"
            :key="index"
            :icon-size="px2rem(field.options.iconSize)"
            :disabled="item.disabled"
            :name="item.value"
            class="vform-van-radio"
          >
            {{
              item.label
            }}
          </van-radio>
        </van-radio-group>
        <template v-if="field.options.readonly || isReadMode">
          <span class="readonly-mode-field" :class="{ diabled: fieldDisabled }">{{ displayValue }}</span>
        </template>
      </template>
    </van-field>
  </form-item-wrapper>
</template>

<script>
import { toRefs, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'
import { px2rem } from '~@/utils/util'

export default {
  componentName: 'FieldWidget',
  name: 'MRadioWidget',
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

    const displayValue = computed(() => {
      let selectedItem = props.field.options.optionItems.find(item => item.value === data.fieldModel)
      if (selectedItem) return selectedItem.label
      else return ''
    })

    fieldMixin.registerToRefList()
    fieldMixin.initOptionItems()
    fieldMixin.initFieldModel()
    fieldMixin.initEventHandler()
    fieldMixin.buildFieldRules()

    fieldMixin.handleOnCreated()

    return {
      i18nt,
      ...fieldMixin,
      ...toRefs(data),

      displayValue
    }
  }
}
</script>

<style lang="scss" scoped>
.vform-van-radio.van-radio.van-radio--vertical {
  margin-bottom: 8px;
}

.vform-van-radio.van-radio.van-radio--horizontal {
  margin-bottom: 8px;
}

.readonly-mode-field {
  height: 28px;
}
</style>
