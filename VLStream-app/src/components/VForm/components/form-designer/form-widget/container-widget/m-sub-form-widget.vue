<!--
/**
 * author: vformAdmin
 * email: vdpadmin@163.com
 * website: https://www.vform666.com
 * date: 2021.08.18
 * remark: 如果要分发VForm源码，需在本文件顶部保留此文件头信息！！
 */
-->

<template>
  <container-wrapper :designer="designer" :widget="widget" :parent-widget="parentWidget" :parent-list="parentList" :index-of-parent-list="indexOfParentList" :class="{ selected: selected }">
    <div class="sub-form-container" @click.stop="selectWidget(widget)">
      <draggable
        :list="widget.widgetList"
        item-key="id"
        v-bind="{ group: 'dragGroup', ghostClass: 'ghost', animation: 200 }"
        :component-data="{ inSubFormFlag: getSubFormFieldFlag() }"
        handle=".drag-handler"
        :move="checkContainerMove"
        class="sub-form"
        style="min-height: 68px"
        @add="evt => onSubFormDragAdd(evt, widget.widgetList)"
        @end="onSubFormDragEnd"
        @update="onContainerDragUpdate"
      >
        <template #item="{ element: subWidget, index: swIdx }">
          <div class="sub-form-table-column" :style="{ width: subWidget.options.columnWidth }">
            <template v-if="'container' === subWidget.category">
              <component
                :is="subWidget.type + '-widget'"
                :key="subWidget.id"
                :widget="subWidget"
                :designer="designer"
                :parent-list="widget.widgetList"
                :index-of-parent-list="swIdx"
                :parent-widget="widget"
                :design-state="true"
                :sub-form-item-flag="true"
              />
            </template>
            <template v-else>
              <component
                :is="subWidget.type + '-widget'"
                :key="subWidget.id"
                :field="subWidget"
                :designer="designer"
                :parent-list="widget.widgetList"
                :index-of-parent-list="swIdx"
                :parent-widget="widget"
                :design-state="true"
                :sub-form-item-flag="true"
              />
            </template>
          </div>
        </template>
      </draggable>
    </div>
  </container-wrapper>
</template>

<script>
import { computed, provide } from 'vue'

import { useI18n } from '~@/utils/i18n'
import { useContainer } from '~@/components/form-designer/form-widget/container-widget/containerMixin'
import { useDesignRef } from '~@/components/form-designer/refMixinDesign'
import ContainerWrapper from '~@/components/form-designer/form-widget/container-widget/container-wrapper'
import FieldComponents from '~@/components/form-designer/form-widget/field-widget/index'

export default {
  componentName: 'ContainerWidget',
  name: 'MSubFormWidget',
  components: {
    ContainerWrapper,
    ...FieldComponents
  },
  props: {
    designer: Object,
    indexOfParentList: Number,
    parentList: Array,
    parentWidget: Object,
    widget: Object
  },
  setup(props) {
    // const refList = inject('refList')

    provide('getSubFormFieldFlag', () => true)
    provide('getSubFormName', () => props.widget.options.name)

    const { i18nt } = useI18n()
    const containerMixin = useContainer()
    const designRefMixin = useDesignRef()

    const selected = computed(() => {
      return !!props.designer && props.widget.id === props.designer.selectedId
    })

    const customClass = computed(() => {
      return props.widget.options.customClass || ''
    })

    const onSubFormDragAdd = (evt, subList) => {
      const newIndex = evt.newIndex
      if (!!subList[newIndex]) {
        props.designer.setSelected(subList[newIndex])
      }

      props.designer.emitHistoryChange()
      props.designer.emitEvent('field-selected', props.widget)
    }

    const onSubFormDragEnd = () => {}

    designRefMixin.initRefList()
    return {
      i18nt,
      ...designRefMixin,
      ...containerMixin,

      customClass,
      onSubFormDragAdd,

      onSubFormDragEnd,
      selected
    }
  }
}
</script>

<style lang="scss" scoped>
.sub-form-container {
  box-sizing: border-box;
  border: 1px dashed #336699;

  :deep(.sub-form) {
    min-height: 68px;
  }
}
</style>
