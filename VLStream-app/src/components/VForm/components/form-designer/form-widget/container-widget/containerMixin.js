import { computed, getCurrentInstance, nextTick, inject } from 'vue'

export function useContainer() {
  const { props } = getCurrentInstance()

  const getFormConfig = inject('getFormConfig')
  const getSubFormFieldFlag = inject('getSubFormFieldFlag')
  const formConfig = computed(() => {
    return getFormConfig()
  })

  const methods = {
    appendTableCol(widget) {
      props.designer.appendTableCol(widget)
    },

    appendTableRow(widget) {
      props.designer.appendTableRow(widget)
    },

    /* draggable组件的move钩子是在内部子组件被拖放到其他draggable组件时触发！！ */

    checkContainerMove(evt) {
      return props.designer.checkWidgetMove(evt)
    },

    cloneContainer(widget) {
      if (!!props.parentList) {
        let newCon = props.designer.cloneContainer(widget)
        props.parentList.splice(props.indexOfParentList + 1, 0, newCon)
        props.designer.setSelected(newCon)

        props.designer.emitHistoryChange()
      }
    },
    getSubFormFieldFlag,

    moveDownWidget() {
      props.designer.moveDownWidget(props.parentList, props.indexOfParentList)
      props.designer.emitHistoryChange()
    },

    moveUpWidget() {
      props.designer.moveUpWidget(props.parentList, props.indexOfParentList)
      props.designer.emitHistoryChange()
    },
    onContainerDragAdd(evt, subList) {
      const newIndex = evt.newIndex
      if (!!subList[newIndex]) {
        props.designer.setSelected(subList[newIndex])
      }

      props.designer.emitHistoryChange()
      props.designer.emitEvent('field-selected', this.widget)
    },

    onContainerDragEnd() {},

    onContainerDragUpdate() {
      props.designer.emitHistoryChange()
    },

    removeWidget() {
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
          props.parentList.splice(props.indexOfParentList, 1)
          // if (!!nextSelected) {
          props.designer.setSelected(nextSelected)
          // }

          props.designer.emitHistoryChange()
        })
      }
    },

    selectParentWidget() {
      if (props.parentWidget) {
        props.designer.setSelected(props.parentWidget)
      } else {
        props.designer.clearSelected()
      }
    },

    selectWidget(widget) {
      props.designer.setSelected(widget)
    },
    setWidgetOption(optionName, optionValue) {
      // 通用组件选项修改API
      // eslint-disable-next-line no-prototype-builtins
      if (props.widget.options.hasOwnProperty(optionName)) {
        props.widget.options[optionName] = optionValue
      }
    }
  }

  return {
    formConfig,
    ...methods
  }
}
