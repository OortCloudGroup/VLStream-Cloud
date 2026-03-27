<template>
  <static-content-wrapper
    :designer="designer"
    :field="field"
    :design-state="designState"
    :display-style="field.options.displayStyle"
    :parent-widget="parentWidget"
    :parent-list="parentList"
    :index-of-parent-list="indexOfParentList"
    :sub-form-row-index="subFormRowIndex"
    :sub-form-col-index="subFormColIndex"
    :sub-form-row-id="subFormRowId"
  >
    <van-button
      ref="fieldEditor"
      :type="field.options.type"
      :size="size"
      :plain="field.options.plain"
      :round="field.options.round"
      :icon="field.options.icon"
      :disabled="fieldDisabled"
      native-type="button"
      @click="handleButtonWidgetClick"
    >
      {{ field.options.label }}
    </van-button>
    <!-- 新版chrome内核点击按钮在设计模式中不会触发组件选中，因此加了个mask层 -->
    <div v-if="designState" class="design-mask" />
  </static-content-wrapper>
</template>

<script>
import { reactive, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import StaticContentWrapper from './static-content-wrapper'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
export default {
  componentName: 'FieldWidget',
  name: 'MButtonWidget',
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
  setup() {
    const { i18nt } = useI18n()
    const fieldMixin = useField(reactive({}))

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
      ...fieldMixin
    }
  }
}
</script>

<style lang="scss" scoped>
@import '../../../../styles/global.scss'; /* static-content-wrapper已引入，还需要重复引入吗？ */

.design-mask {
  position: absolute;
  top: 0px;
  bottom: 0px;
  left: 0px;
  right: 0px;
}
</style>
