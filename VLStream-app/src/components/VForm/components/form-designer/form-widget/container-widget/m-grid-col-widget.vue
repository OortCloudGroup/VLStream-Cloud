<template>
  <van-col v-bind="layoutProps" :key="widget.id" class="grid-cell" :class="[selected ? 'selected' : '', customClass]" :style="colHeightStyle" @click.stop="selectWidget(widget)">
    <draggable
      :list="widget.widgetList"
      item-key="id"
      v-bind="{ group: 'dragGroup', ghostClass: 'ghost', animation: 200 }"
      handle=".drag-handler"
      :move="checkContainerMove"
      class="m-grid-col"
      style="min-height: 30px"
      :component-data="{ inSubFormFlag: getSubFormFieldFlag() }"
      @end="evt => onContainerDragEnd(evt, widget.widgetList)"
      @add="evt => onContainerDragAdd(evt, widget.widgetList)"
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
        </transition-group>
      </template>
    </draggable>

    <div v-if="designer.selectedId === widget.id && widget.type === 'm-grid-col'" class="grid-col-action">
      <i :title="i18nt('designer.hint.selectParentWidget')" @click.stop="selectParentWidget(widget)"><svg-icon icon-class="el-back" /></i>
      <i v-if="!!parentList && parentList.length > 1" :title="i18nt('designer.hint.moveUpWidget')" @click.stop="moveUpWidget()"><svg-icon icon-class="el-move-up" /></i>
      <i v-if="!!parentList && parentList.length > 1" :title="i18nt('designer.hint.moveDownWidget')" @click.stop="moveDownWidget()"><svg-icon icon-class="el-move-down" /></i>
      <i :title="i18nt('designer.hint.cloneWidget')" @click.stop="cloneGridCol(widget)"><svg-icon icon-class="el-clone" /></i>
      <i :title="i18nt('designer.hint.remove')" @click.stop="removeWidget"><svg-icon icon-class="el-delete" /></i>
    </div>

    <div v-if="designer.selectedId === widget.id && widget.type === 'm-grid-col'" class="grid-col-handler">
      <i>{{ i18nt('designer.widgetLabel.' + widget.type) }}</i>
    </div>
  </van-col>
</template>

<script>
import { computed, toRefs, reactive, nextTick, watch } from 'vue'

// import { px2rem } from '~@/utils/util'
import { useI18n } from '~@/utils/i18n'
import { useContainer } from '~@/components/form-designer/form-widget/container-widget/containerMixin'
import { useDesignRef } from '~@/components/form-designer/refMixinDesign'
// import ContainerWrapper from '~@/components/form-designer/form-widget/container-widget/container-wrapper'
import FieldComponents from '~@/components/form-designer/form-widget/field-widget/index'
import SvgIcon from '~@/components/svg-icon'

export default {
  componentName: 'ContainerWidget',
  name: 'MGridColWidget',
  components: {
    ...FieldComponents,
    SvgIcon
  },
  props: {
    colHeight: {
      default: null,
      type: String
    },
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

    const data = reactive({
      layoutProps: {
        // md: props.widget.options.md || 12,
        // sm: props.widget.options.sm || 12,
        // xs: props.widget.options.xs || 12,
        offset: props.widget.options.offset || 0,
        pull: props.widget.options.pull || 0,
        push: props.widget.options.push || 0,
        span: props.widget.options.span || 0
      }
    })

    const selected = computed(() => {
      return props.widget.id === props.designer.selectedId
    })

    const customClass = computed(() => {
      return props.widget.options.customClass || ''
    })

    const colHeightStyle = computed(() => {
      return !!props.colHeight ? { height: props.colHeight + 'px' } : {}
    })

    watch(
      () => props.designer.formConfig.layoutType,
      val => {
        if (!!props.widget.options.responsive) {
          if (val === 'H5') {
            data.layoutProps.span = props.widget.options.xs || 12
          } else if (val === 'Pad') {
            data.layoutProps.span = props.widget.options.sm || 12
          } else {
            data.layoutProps.span = props.widget.options.md || 12
          }
        } else {
          data.layoutProps.span = props.widget.options.span || 12
        }
      }
    )

    watch(
      () => props.widget.options.responsive,
      val => {
        let lyType = props.designer.formConfig.layoutType
        if (!!val) {
          if (lyType === 'H5') {
            data.layoutProps.span = props.widget.options.xs || 12
          } else if (lyType === 'Pad') {
            data.layoutProps.span = props.widget.options.sm || 12
          } else {
            data.layoutProps.span = props.widget.options.md || 12
          }
        } else {
          data.layoutProps.span = props.widget.options.span || 12
        }
      }
    )

    watch(
      () => props.widget.options.span,
      val => {
        data.layoutProps.span = val
      }
    )

    watch(
      () => props.widget.options.md,
      val => {
        data.layoutProps.span = val
      }
    )

    watch(
      () => props.widget.options.sm,
      val => {
        data.layoutProps.span = val
      }
    )

    watch(
      () => props.widget.options.xs,
      val => {
        data.layoutProps.span = val
      }
    )

    watch(
      () => props.widget.options.offset,
      val => {
        data.layoutProps.offset = val
      }
    )

    watch(
      () => props.widget.options.push,
      val => {
        data.layoutProps.push = val
      }
    )

    watch(
      () => props.widget.options.pull,
      val => {
        data.layoutProps.pull = val
      }
    )

    const initLayoutProps = () => {
      if (!!props.widget.options.responsive) {
        let lyType = props.designer.formConfig.layoutType
        if (lyType === 'H5') {
          data.layoutProps.span = props.widget.options.xs || 12
        } else if (lyType === 'Pad') {
          data.layoutProps.span = props.widget.options.sm || 12
        } else {
          data.layoutProps.span = props.widget.options.md || 12
        }
      } else {
        data.layoutProps.spn = props.widget.options.span
      }
    }

    // const onGridDragEnd = (evt, subList) => {
    //   //
    // }
    const selectWidget = widget => {
      props.designer.setSelected(widget)
    }

    const checkContainerMove = evt => {
      return props.designer.checkWidgetMove(evt)
    }

    const selectParentWidget = () => {
      if (props.parentWidget) {
        props.designer.setSelected(props.parentWidget)
      } else {
        props.designer.clearSelected()
      }
    }

    const moveUpWidget = () => {
      props.designer.moveUpWidget(props.parentList, props.indexOfParentList)
    }

    const moveDownWidget = () => {
      props.designer.moveDownWidget(props.parentList, props.indexOfParentList)
    }

    const cloneGridCol = widget => {
      props.designer.cloneGridCol(widget, props.parentWidget)
    }

    const removeWidget = () => {
      if (!!props.parentList) {
        let nextSelected = null
        if (props.parentList.length === 1) {
          if (!!props.parentWidget) {
            nextSelected = props.parentWidget
          }
        } else if (props.parentList.length === 1 + props.indexOfParentList) {
          nextSelected = props.parentList[props.indexOfParentList - 1]
        } else {
          nextSelected = props.parentList[props.indexOfParentList + 1]
        }

        nextTick(() => {
          // eslint-disable-next-line vue/no-mutating-props
          props.parentList.splice(props.indexOfParentList, 1)
          // if (!!nextSelected) {
          props.designer.setSelected(nextSelected)
          // }

          props.designer.emitHistoryChange()
        })
      }
    }

    designRefMixin.initRefList()
    initLayoutProps()
    return {
      i18nt,
      ...toRefs(data),

      ...containerMixin,
      ...designRefMixin,

      checkContainerMove,
      cloneGridCol,
      colHeightStyle,

      customClass,
      moveDownWidget,

      moveUpWidget,
      removeWidget,
      selectParentWidget,

      selectWidget,
      selected
    }
  }
}
</script>

<style lang="scss" scoped>
.grid-cell {
  min-height: 38px;
  //margin: 6px 0;  /* 设置了margin，栅格列的offset、push、pull会失效！！ */
  // padding: 3px;
  position: relative;

  &.selected {
    &:after {
      position: absolute;
      box-sizing: border-box;
      content: ' ';
      pointer-events: none;
      top: 0px;
      right: 0;
      bottom: 0;
      left: 0;
      border: 1px solid var(--color-primary);
      transform: scaley(1);
    }
  }

  .form-widget-list {
    min-height: 28px;
  }

  .grid-col-action {
    position: absolute;
    bottom: 0;
    right: 0px;
    height: 28px;
    line-height: 28px;
    background: var(--color-primary);
    z-index: 999;

    i {
      font-size: 14px;
      color: #fff;
      margin: 0 5px;
      cursor: pointer;
    }
  }

  .grid-col-handler {
    position: absolute;
    top: 0px;
    left: 0px;
    height: 22px;
    line-height: 22px;
    background: var(--color-primary);
    z-index: 9;

    i {
      font-size: 14px;
      font-style: normal;
      color: #fff;
      margin: 4px;
      cursor: default;
    }
  }
}
</style>
