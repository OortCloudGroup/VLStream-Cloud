<template>
  <container-item-wrapper :widget="widget" :class="[widget.type]">
    <van-cell-group :title="widget.options.label" :border="widget.options.border" :inset="widget.options.inset">
      <template v-for="(subWidget, swIdx) in widget.widgetList">
        <template v-if="'container' === subWidget.category">
          <component
            :is="subWidget.type + '-item'"
            :key="swIdx"
            :widget="subWidget"
            :parent-list="widget.widgetList"
            :index-of-parent-list="swIdx"
            :parent-widget="widget"
            :sub-form-row-id="subFormRowId"
            :sub-form-row-index="subFormRowIndex"
            :sub-form-col-index="subFormColIndex"
          >
            <!-- 递归传递插槽！！！ -->
            <template v-for="slot in Object.keys($slots)" #[slot]="scope">
              <slot :name="slot" v-bind="scope" />
            </template>
          </component>
        </template>
        <template v-else>
          <component
            :is="subWidget.type + '-widget'"
            :key="swIdx"
            :field="subWidget"
            :designer="null"
            :parent-list="widget.widgetList"
            :index-of-parent-list="swIdx"
            :parent-widget="widget"
            :sub-form-row-id="subFormRowId"
            :sub-form-row-index="subFormRowIndex"
            :sub-form-col-index="subFormColIndex"
          >
            <!-- 递归传递插槽！！！ -->
            <template v-for="slot in Object.keys($slots)" #[slot]="scope">
              <slot :name="slot" v-bind="scope" />
            </template>
          </component>
        </template>
      </template>
    </van-cell-group>
  </container-item-wrapper>
</template>

<script>
import { toRefs, computed, onBeforeUnmount } from 'vue'

import { useEmitter } from '~@/utils/emitter'
import { useI18n } from '~@/utils/i18n'
import { useRef } from '~@/components/form-render/refMixin'
import { useContainer } from './containerItemMixin'
// import { useDesignRef } from '~@/components/form-designer/refMixinDesign'

import ContainerItemWrapper from '~@/components/form-render/container-item/container-item-wrapper'
import FieldComponents from '~@/components/form-designer/form-widget/field-widget'

export default {
  componentName: 'ContainerItem',
  name: 'MCellGroupItem',
  components: {
    ContainerItemWrapper,
    ...FieldComponents
  },
  props: {
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
    },
    widget: Object
  },
  setup(props) {
    // const refList = inject('refList')
    // const globalModel = inject('globalModel')
    // const sfRefList = inject('sfRefList')

    const { i18nt } = useI18n()
    const refMixin = useRef()
    const emitterMixin = useEmitter()

    const containerMixin = useContainer()

    const customClass = computed(() => {
      return props.widget.options.customClass || ''
    })

    onBeforeUnmount(() => {
      containerMixin.unregisterFromRefList()
    })

    refMixin.initRefList()

    return {
      i18nt,
      ...toRefs(props),
      ...emitterMixin,
      ...containerMixin,
      customClass
    }
  }
}
</script>

<style scoped>
.container-wrapper.m-cell-group {
  box-sizing: border-box;
  padding-bottom: 10px;
}
</style>
