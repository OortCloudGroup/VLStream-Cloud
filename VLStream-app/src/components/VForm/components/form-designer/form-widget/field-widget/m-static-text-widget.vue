<template>
  <static-content-wrapper
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
    <div ref="fieldEditor" :style="!!field.options.fontSize ? `font-size: ${px2rem(field.options.fontSize)};text-align: ${field.options.textAlign};color: ${field.options.textColor};font-weight: ${field.options.fontWeight};` : ''">
      {{ field.options.textContent }}
    </div>
  </static-content-wrapper>
</template>

<script>
import { reactive, toRefs, onMounted, onBeforeUnmount } from 'vue'
import StaticContentWrapper from './static-content-wrapper'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import { px2rem } from '~@/utils/util'

export default {
  componentName: 'FieldWidget',
  name: 'MStaticTextWidget', // 必须固定为FieldWidget，用于接收父级组件的broadcast事件
  components: {
    StaticContentWrapper
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
      px2rem: px2rem
    })
    const fieldMixin = useField(data)

    onMounted(() => {
      fieldMixin.handleOnMounted()
    })

    onBeforeUnmount(() => {
      fieldMixin.unregisterFromRefList()
    })

    fieldMixin.registerToRefList()
    fieldMixin.initEventHandler()
    fieldMixin.handleOnCreated()

    return {
      i18nt,
      ...toRefs(props),
      ...toRefs(data),
      ...fieldMixin
    }
  }
}
</script>

<style lang="scss" scoped>
@import '../../../../styles/global.scss'; /* static-content-wrapper已引入，还需要重复引入吗？ */
</style>
