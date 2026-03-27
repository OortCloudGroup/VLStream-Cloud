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
  <div
    v-if="!!field.formItemFlag && (!field.options.hidden || designState === true)"
    class="field-wrapper"
    :class="[field.type, field.options.name, selected ? 'selected' : '', labelAlign, customClass, field.options.required ? 'required' : '', showInputBorder]"
    @click.stop="selectField(field)"
  >
    <slot />
    <template v-if="!!designer">
      <div v-if="designer.selectedId === field.id" class="field-action">
        <i :title="i18nt('designer.hint.selectParentWidget')" @click.stop="selectParentWidget(field)"><svg-icon icon-class="el-back" /></i>
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
export default {
  name: 'FormItemWrapper'
}
</script>
<script setup>
import SvgIcon from '~@/components/svg-icon'
import { useI18n } from '~@/utils/i18n'
import { inject, computed, nextTick } from 'vue'

const props = defineProps({
  designState: {
    default: false,
    type: Boolean
  },
  designer: Object,
  field: Object,
  indexOfParentList: Number,
  parentList: Array,

  parentWidget: Object,

  rules: Array,
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
})

const { i18nt, i18n2t } = useI18n()
const getFormConfig = inject('getFormConfig')

const formConfig = computed(() => {
  return getFormConfig()
})

const selected = computed(() => {
  return !!props.designer && props.field.id === props.designer.selectedId
})

// const label = computed(() => {
//   if (props.field.options.labelHidden) {
//     return ''
//   }

//   return props.field.options.label
// })

// const labelWidth = computed(() => {
//   if (props.field.options.labelHidden) {
//     return props.designState ? 5 : 0 // 设计期间标签最小宽度5像素，以便于鼠标点击可选中组件！！
//   }

//   if (props.field.options.labelWidth) {
//     return props.field.options.labelWidth
//   }

//   if (props.designer) {
//     return props.designer.formConfig.labelWidth
//   } else {
//     return formConfig.value.labelWidth
//   }
// })

const labelAlign = computed(() => {
  if (props.field.options.labelAlign) {
    return props.field.options.labelAlign
  }

  if (props.designer) {
    return props.designer.formConfig.labelAlign || 'left'
  } else {
    return formConfig.value.labelAlign || 'left'
  }
})

const customClass = computed(() => {
  return props.field.options.customClass ? props.field.options.customClass.join(' ') : ''
})

// const subFormName = computed(() => {
//   return props.parentWidget ? props.parentWidget.options.name : ''
// })

// const subFormItemFlag = computed(() => {
//   return props.parentWidget ? props.parentWidget.type === 'm-sub-form' : false
// })

const showInputBorder = computed(() => {
  return formConfig.value.inputBorder ? 'cell-value-border' : ''
})

const selectField = field => {
  if (props.designer) {
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
  if (props.parentList) {
    let nextSelected = null
    if (props.parentList.length === 1) {
      if (props.parentWidget) {
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

// const getPropName = () => {
//   if (subFormItemFlag.value && !props.designState) {
//     return subFormName.value + '.' + props.subFormRowIndex + '.' + props.field.options.name + ''
//   } else {
//     return props.field.options.name
//   }
// }
</script>

<style lang="scss" scoped>
@import '../../../../styles/global.scss';

.design-time-bottom-margin {
  margin-bottom: 5px;
}

.field-wrapper {
    border-radius: 8px;
    margin: 6px 12px 0;
    overflow: hidden;

  position: relative;
  /* margin-bottom:2px; */

  .field-action {
    position: absolute;
    bottom: 0;
    right: 0px;
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
    /*bottom: -22px;   拖拽手柄位于组件下方，有时无法正常拖动，原因未明？？ */
    left: 0px;
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
      opacity: 1;
      background: var(--color-primary);
    }
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

:deep(.van-cell):last-child:after {
  display: initial;
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
    /* border: 2px solid var(--color-primary); */
    transform: scaley(1);
  }
}

:deep(.left) .van-field__label {
  text-align: left;
}

:deep(.center) .van-field__label {
  text-align: center;
}

:deep(.right) .van-field__label {
  text-align: right;
}

.cell-value-border.m-input,
.cell-value-border.m-textarea {
  :deep(.van-field__body) {
    /* border: 1px solid #dcdfe6; */
    border-radius: 4px;
    padding: 4px 8px;
    box-sizing: border-box;
  }
}
</style>

<style lang="scss">
.van-cell {
  position: relative;

  &:after {
    position: absolute;
    box-sizing: border-box;
    content: ' ';
    pointer-events: none;
    right: 0px;
    bottom: 0px;
    left: 0px;
    /* border-bottom: 1px solid rgb(173, 104, 104); */
    transform: scaley(0.5);
  }
}
</style>
