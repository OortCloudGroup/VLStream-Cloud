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
        <van-checkbox-group
          v-show="!field.options.readonly && !isReadMode"
          v-model="fieldModel"
          :direction="field.options.displayStyle === 'inline' ? 'horizontal' : 'vertical'"
          :disabled="fieldDisabled"
          @change="handleChangeEvent"
        >
          <van-checkbox
            v-for="(item, index) in field.options.optionItems"
            :key="index"
            shape="square"
            :disabled="item.disabled"
            :icon-size="px2rem(field.options.iconSize)"
            :name="item.value"
            class="vform-van-checkbox"
          >
            {{ item.label }}
          </van-checkbox>
        </van-checkbox-group>
        <template v-if="field.options.readonly || isReadMode">
          <span class="readonly-mode-field" :class="{ diabled: fieldDisabled }">{{ displayValue }}</span>
        </template>
      </template>
    </van-field>
  </form-item-wrapper>
</template>
<script>
import { reactive, toRefs, computed, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'
import { px2rem } from '~@/utils/util'

export default {
  componentName: 'FieldWidget',
  name: 'MCheckboxWidget',
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
      fieldModel: [],
      noChangeEventFlag: true, // van-field组件没有change事件！！
      oldFieldValue: [], // field组件change之前的值
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
      let checkedItems = props.field.options.optionItems.filter(item => {
        return data.fieldModel.indexOf(item.value) >= 0
      })
      return checkedItems.map(item => item.label).toString()
    })

    /* 注意：子组件mounted在父组件created之后、父组件mounted之前触发，故子组件mounted需要用到的prop
        需要在父组件created中初始化！！ */
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
.vform-van-checkbox.van-checkbox.van-checkbox--vertical {
  margin-bottom: 8px;
}

.vform-van-checkbox.van-checkbox.van-checkbox--horizontal {
  margin-bottom: 8px;
}

.readonly-mode-field {
  height: 28px;
}
</style>
