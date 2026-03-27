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
    <div :class="[!!designState ? 'slot-wrapper-design' : 'slot-wrapper-render']">
      <slot :name="field.options.name" :form-model="formModel" />
      <div v-if="!!designState" class="slot-title">
        {{ field.options.label }}
      </div>
    </div>
  </static-content-wrapper>
</template>

<script>
import { onMounted, toRefs, reactive, onBeforeUnmount } from 'vue'
import StaticContentWrapper from './static-content-wrapper'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'

export default {
  componentName: 'FieldWidget',
  name: 'MSlotWidget', // 必须固定为FieldWidget，用于接收父级组件的broadcast事件
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

    const data = reactive({})
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
      ...toRefs(data),
      ...fieldMixin
    }
  }
}
</script>

<style lang="scss" scoped>
.slot-wrapper-design {
  width: 100%;
  min-height: 26px;
  background: linear-gradient(45deg, #ccc 25%, #eee 0, #eee 50%, #ccc 0, #ccc 75%, #eee 0);
  background-size: 20px 20px;
  text-align: center;

  .slot-title {
    font-size: 13px;
  }
}

.slot-wrapper-render {
}
</style>
