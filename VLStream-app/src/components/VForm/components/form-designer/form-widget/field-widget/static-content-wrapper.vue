<template>
  <div class="field-wrapper" :class="[selected ? 'selected' : '', customClass]" :style="{ display: displayStyle }">
    <div v-show="!field.options.hidden || designState === true" class="static-content-item" :style="{ display: displayStyle }" @click.stop="selectField(field)">
      <slot />
    </div>

    <template v-if="!!designer">
      <div v-if="designer.selectedId === field.id" class="field-action">
        <i :title="i18nt('designer.hint.selectParentWidget')" @click.stop="selectParentWidget(field)">
          <svg-icon icon-class="el-back" />
        </i>
        <i v-if="!!parentList && parentList.length > 1" :title="i18nt('designer.hint.moveUpWidget')" @click.stop="moveUpWidget(field)"><svg-icon icon-class="el-move-up" /></i>
        <i v-if="!!parentList && parentList.length > 1" :title="i18nt('designer.hint.moveDownWidget')" @click.stop="moveDownWidget(field)"><svg-icon icon-class="el-move-down" /></i>
        <i :title="i18nt('designer.hint.remove')" @click.stop="removeFieldWidget">
          <svg-icon icon-class="el-delete" />
        </i>
      </div>

      <div v-if="designer.selectedId === field.id" class="drag-handler background-opacity">
        <i :title="i18nt('designer.hint.dragHandler')"><svg-icon icon-class="el-drag-move" /></i>
        <i>{{ i18n2t(`designer.widgetLabel.${field.type}`, `extension.widgetLabel.${field.type}`) }}</i>
        <i v-if="field.options.hidden === true"><svg-icon icon-class="el-hide" /></i>
      </div>
    </template>
  </div>
</template>

<script>
import { computed, nextTick, toRefs } from 'vue'

import SvgIcon from '~@/components/svg-icon'
import { useI18n } from '~@/utils/i18n'

export default {
  name: 'StaticContentWrapper',
  components: {
    SvgIcon
  },
  props: {
    designState: {
      default: false,
      type: Boolean
    },
    designer: Object,
    displayStyle: {
      default: 'block',
      type: String
    },
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
    const { i18nt, i18n2t } = useI18n()

    const selected = computed(() => {
      return !!props.designer && props.field.id === props.designer.selectedId
    })

    const customClass = computed(() => {
      return !!props.field.options.customClass ? props.field.options.customClass.join(' ') : ''
    })

    const selectField = field => {
      if (!!props.designer) {
        props.designer.setSelected(field)
        props.designer.emitEvent('field-selected', props.parentWidget) // 发送选中组件的父组件对象
      }
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
      props.designer.emitHistoryChange()
    }

    const moveDownWidget = () => {
      props.designer.moveDownWidget(props.parentList, props.indexOfParentList)
      props.designer.emitHistoryChange()
    }

    const removeFieldWidget = () => {
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

    return {
      i18n2t,
      i18nt,
      ...toRefs(props),

      customClass,
      moveDownWidget,

      moveUpWidget,
      removeFieldWidget,
      selectField,
      selectParentWidget,
      selected
    }
  }
}
</script>

<style lang="scss" scoped>
@import '../../../../styles/global.scss';

.field-wrapper {
  position: relative;
  /* background-color: white; */
  border-radius: 8px;
  padding: 4px 0;
  margin: 6px 12px 0;

  .field-action {
    position: absolute;
    //bottom: -24px;
    bottom: 0;
    right: -2px;
    height: 22px;
    line-height: 22px;
    background: var(--color-primary);
    z-index: 9;

    i {
      font-size: 14px;
      color: #fff;
      margin: 0 5px;
      cursor: pointer;
    }
  }

  .drag-handler {
    position: absolute;
    top: 0;
    /* bottom: -22px;  拖拽手柄位于组件下方，有时无法正常拖动，原因未明？？ */
    left: -1px;
    height: 20px;
    line-height: 20px;
    /* background: var(--color-primary); */
    z-index: 9;

    i {
      font-size: 12px;
      font-style: normal;
      color: #fff;
      margin: 4px;
      cursor: move;
    }

    &:hover {
      //opacity: 1;
      background: var(--color-primary);
    }
  }
}

.field-wrapper.selected,
.static-content-item.selected {
  &:after {
    position: absolute;
    box-sizing: border-box;
    content: ' ';
    pointer-events: none;
    top: 0px;
    right: 0;
    bottom: 0;
    left: 0;
    border: 2px solid var(--color-primary);
    transform: scaley(1);
  }
}

.static-content-item {
  min-height: 20px;
  display: flex; /* 垂直居中 */
  align-items: center; /* 垂直居中 */

  :deep(.el-divider--horizontal) {
    margin: 0;
  }
}
</style>
