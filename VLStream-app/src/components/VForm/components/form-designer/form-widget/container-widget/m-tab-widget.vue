<!-- eslint-disable vue/no-mutating-props -->
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
  <container-wrapper :designer="designer" :widget="widget" :class="{ selected: selected }" :parent-widget="parentWidget" :parent-list="parentList" :index-of-parent-list="indexOfParentList">
    <div :key="widget.id" class="tab-container" @click.stop="selectWidget(widget)">
      <van-tabs v-model="widget.options.active" :type="tabDisplayType" :color="widget.options.colorStyle" :ellipsis="widget.options.textEllipsis" :swipeable="true">
        <van-tab v-for="(tab, index) in widget.tabs" :key="index" :title="tab.options.label" style="min-height: 28px" :name="tab.options.name" @click.stop="selectWidget(widget)">
          <draggable
            :list="tab.widgetList"
            item-key="id"
            v-bind="{ group: 'dragGroup', ghostClass: 'ghost', animation: 200 }"
            handle=".drag-handler"
            :move="checkContainerMove"
            class="m-tab"
            style="min-height: 30px"
            :component-data="{ inSubFormFlag: getSubFormFieldFlag() }"
            @add="evt => onContainerDragAdd(evt, tab.widgetList)"
            @update="onContainerDragUpdate"
          >
            <template #item="{ element: subWidget, index: swIdx }">
              <transition-group name="fade" tag="div" class="form-widget-list">
                <template v-if="'container' === subWidget.category">
                  <component
                    :is="subWidget.type + '-widget'"
                    :key="subWidget.id"
                    :widget="subWidget"
                    :designer="designer"
                    :parent-list="tab.widgetList"
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
                    :parent-list="tab.widgetList"
                    :index-of-parent-list="swIdx"
                    :parent-widget="widget"
                    :design-state="true"
                  />
                </template>
              </transition-group>
            </template>
          </draggable>
        </van-tab>
      </van-tabs>
    </div>
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
  name: 'MTabWidget',
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

    const tabDisplayType = computed(() => {
      return props.widget.options.displayType === 'border-card' ? 'card' : 'line'
    })

    const onTabClick = evt => {
      let paneName = evt.name
      props.widget.tabs.forEach(tp => {
        tp.options.active = tp.options.name === paneName
      })
    }

    designRefMixin.initRefList()
    return {
      i18nt,
      ...containerMixin,

      customClass,
      onTabClick,
      selected,

      tabDisplayType
    }
  }
}
</script>

<style lang="scss" scoped>
.tab-container {
  /* //padding: 5px; */
  margin: 0px;

  .form-widget-list {
    min-height: 28px;
  }
}
</style>
