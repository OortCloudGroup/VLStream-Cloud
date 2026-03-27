<template>
  <container-wrapper
    :class="[selected ? 'selected' : '', widget.type]"
    :designer="designer"
    :widget="widget"
    :parent-widget="parentWidget"
    :parent-list="parentList"
    :index-of-parent-list="indexOfParentList"
    @click.stop="selectWidget(widget)"
  >
    <van-cell-group :title="widget.options.label" :border="widget.options.border" :inset="widget.options.inset">
      <draggable
        :list="widget.widgetList"
        item-key="id"
        v-bind="{ group: 'dragGroup', ghostClass: 'ghost', animation: 200 }"
        handle=".drag-handler"
        :component-data="{ inSubFormFlag: getSubFormFieldFlag() }"
        :move="checkContainerMove"
        class="m-cell-group"
        style="min-height: 30px"
        @add="evt => onContainerDragAdd(evt, widget.widgetList)"
        @update="onContainerDragUpdate"
      >
        <template #item="{ element: subWidget, index: swIdx }">
          <template v-if="'container' === subWidget.category">
            <component
              :is="subWidget.type + '-widget'"
              :key="subWidget.id"
              :widget="subWidget"
              :designer="designer"
              :parent-list="widget.widgetList"
              :index-of-parent-list="swIdx"
              :parent-widget="widget"
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
            />
          </template>
        </template>
      </draggable>
    </van-cell-group>
  </container-wrapper>
</template>

<script>
import { computed } from 'vue'

import { useI18n } from '~@/utils/i18n'
import { useContainer } from '~@/components/form-designer/form-widget/container-widget/containerMixin'
import { useDesignRef } from '~@/components/form-designer/refMixinDesign'
import ContainerWrapper from '~@/components/form-designer/form-widget/container-widget/container-wrapper'
import FieldComponents from '~@/components/form-designer/form-widget/field-widget/index'

export default {
  componentName: 'ContainerWidget',
  name: 'MCellGroupWidget',
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
    const { i18nt } = useI18n()
    const containerMixin = useContainer()
    const designRefMixin = useDesignRef()

    const selected = computed(() => {
      return props.widget.id === props.designer.selectedId
    })

    const customClass = computed(() => {
      return props.widget.options.customClass || ''
    })

    designRefMixin.initRefList()
    return {
      i18nt,
      ...containerMixin,
      ...designRefMixin,

      customClass,
      selected
    }
  }
}
</script>

<style lang="scss" scoped>
div.form-widget-list {
  min-height: 28px;
}

.container-wrapper.m-cell-group {
  box-sizing: border-box;
  padding-bottom: 10px;
}

:deep(.van-cell-group--inset) .field-wrapper:first-of-type {
  border-radius: 8px;
  &.selected::after {
    border-top-left-radius: 8px;
    border-top-right-radius: 8px;
  }
}
:deep(.van-cell-group--inset) .field-wrapper:last-of-type {
  border-radius: 8px;
  &.selected::after {
    border-bottom-left-radius: 8px;
    border-bottom-right-radius: 8px;
  }
}
</style>
