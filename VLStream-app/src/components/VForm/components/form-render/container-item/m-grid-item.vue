<!-- eslint-disable vue/no-v-for-template-key -->
<template>
  <container-item-wrapper :widget="widget">
    <van-row v-show="!widget.options.hidden" :key="widget.id" :ref="widget.id" :gutter="px2rem(widget.options.gutter)" class="grid-container" :class="[customClass]">
      <template v-for="(colWidget, colIdx) in widget.cols" :key="colIdx">
        <grid-col-item
          :widget="colWidget"
          :parent-list="widget.cols"
          :index-of-parent-list="colIdx"
          :parent-widget="widget"
          :col-height="px2rem(widget.options.colHeight)"
          :sub-form-row-id="subFormRowId"
          :sub-form-row-index="subFormRowIndex"
          :sub-form-col-index="subFormColIndex"
        >
          <!-- 递归传递插槽！！！ -->
          <template v-for="slot in Object.keys($slots)" #[slot]="scope">
            <slot :name="slot" v-bind="scope" />
          </template>
        </grid-col-item>
      </template>
    </van-row>
  </container-item-wrapper>
</template>

<script>
import { computed, onBeforeUnmount } from 'vue'

import { useEmitter } from '~@/utils/emitter'
import { useRef } from '../../../components/form-render/refMixin'
import { useContainer } from './containerItemMixin'
import { px2rem } from '~@/utils/util'
import ContainerItemWrapper from './container-item-wrapper'
import GridColItem from './m-grid-col-item'

export default {
  componentName: 'ContainerItem',
  name: 'MGridItem',
  components: {
    ContainerItemWrapper,
    GridColItem
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

    const refMixin = useRef()
    const containerMixin = useContainer()
    const emitterMixin = useEmitter()

    const customClass = computed(() => {
      return props.widget.options.customClass || ''
    })

    onBeforeUnmount(() => {
      containerMixin.unregisterFromRefList()
    })

    refMixin.initRefList()
    return {
      ...emitterMixin,
      ...containerMixin,
      customClass,
      px2rem
    }
  }
}
</script>

<style lang="scss" scoped></style>
