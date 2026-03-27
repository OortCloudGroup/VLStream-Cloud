import { inject, computed, getCurrentInstance, onMounted } from 'vue'
import { useRef } from '../refMixin'
// import { useEmitter } from '~@/utils/emitter'
import { useI18n } from '~@/utils/i18n'
import { deepClone, traverseFieldWidgetsOfContainer, traverseWidgetsOfContainer } from '~@/utils/util'

export function useContainer(data = {}) {
  const { props, proxy } = getCurrentInstance()
  const refList = inject('refList')
  const globalModel = inject('globalModel')
  const refMixin = useRef()
  const { i18nt } = useI18n()

  const customClass = computed(() => {
    return props.widget.options.customClass || ''
  })

  const formModel = computed({
    cache: false,
    get: () => {
      return globalModel.formModel
    }
  })

  onMounted(() => {
    methods.callSetHidden()
  })

  const methods = {
    activeTab(tabIndex) {
      // tabIndex从0计数
      if (tabIndex >= 0 && tabIndex < props.widget.tabs.length) {
        props.widget.tabs.forEach((tp, idx) => {
          tp.options.active = idx === tabIndex
          if (idx === tabIndex) {
            data.activeTabName = tp.options.name
          }
        })
      }
    },

    // validateField(fieldName) { //逐行校验子表单字段
    //   //TODO:
    // },
    //
    // validateSubForm() { //逐行校验子表单全部字段
    //   //TODO:
    // },

    /**
     * 动态增加自定义css样式
     * @param className
     */

    addCssClass(className) {
      if (!props.widget.options.customClass) {
        props.widget.options.customClass = [className]
      } else {
        props.widget.options.customClass.push(className)
      }
    },

    /* 主动触发setHidden()方法，以清空被隐藏容器内字段组件的校验规则！！ */
    callSetHidden() {
      if (proxy.widget.options.hidden === true) {
        methods.setHidden(true)
      }
    },

    cloneWidgetSchema(widget) {
      return deepClone(widget)
      /**
       * 注意：在v-for循环中，必须保证克隆对象与原对象完全一致，修改克隆对象任何属性，
       * 都会触发组件的beforeDestroy事件钩子！！！
       */

      // let newWidgetSchema = deepClone(widget)
      // newWidgetSchema.id = widget.type + generateId()
      // return newWidgetSchema
    },
    disableGridSubForm() {
      if (data.rowIdData.length > 0) {
        data.rowIdData.forEach((dataRow, rIdx) => {
          methods.disableGridSubFormRow(rIdx)
        })
      }

      // 禁用3个操作按钮
      data.actionDisabled = true
    },

    disableGridSubFormRow(rowIndex) {
      let gsfFWList = []
      let fieldListFn = fw => {
        gsfFWList.push(fw)
      }
      traverseFieldWidgetsOfContainer(props.widget, fieldListFn)

      gsfFWList.forEach(fw => {
        let swRefName = fw.options.name + '@row' + data.rowIdData[rowIndex]
        let foundSW = refMixin.getWidgetRef(swRefName)
        if (!!foundSW && !!foundSW.setDisabled) {
          foundSW.setDisabled(true)
        }
      })
    },

    disableSubForm() {
      if (data.rowIdData.length > 0) {
        data.rowIdData.forEach((dataRow, rIdx) => {
          methods.disableSubFormRow(rIdx)
        })
      }

      // 禁用3个操作按钮
      data.actionDisabled = true
    },

    disableSubFormRow(rowIndex) {
      props.widget.widgetList.forEach(subWidget => {
        let swRefName = subWidget.options.name + '@row' + data.rowIdData[rowIndex]
        let foundSW = refMixin.getWidgetRef(swRefName)
        if (!!foundSW && !!foundSW.setDisabled) {
          foundSW.setDisabled(true)
        }
      })
    },

    disableTab(tabIndex) {
      if (tabIndex >= 0 && tabIndex < props.widget.tabs.length) {
        props.widget.tabs[tabIndex].options.disabled = true
      }
    },

    enableGridSubForm() {
      if (data.rowIdData.length > 0) {
        data.rowIdData.forEach((dataRow, rIdx) => {
          methods.enableGridSubFormRow(rIdx)
        })
      }

      // 启用3个操作按钮
      data.actionDisabled = false
    },

    enableGridSubFormRow(rowIndex) {
      let gsfFWList = []
      let fieldListFn = fw => {
        gsfFWList.push(fw)
      }
      traverseFieldWidgetsOfContainer(props.widget, fieldListFn)

      gsfFWList.forEach(fw => {
        let swRefName = fw.options.name + '@row' + data.rowIdData[rowIndex]
        let foundSW = refMixin.getWidgetRef(swRefName)
        if (!!foundSW && !!foundSW.setDisabled) {
          foundSW.setDisabled(false)
        }
      })
    },
    enableSubForm() {
      if (data.rowIdData.length > 0) {
        data.rowIdData.forEach((dataRow, rIdx) => {
          methods.enableSubFormRow(rIdx)
        })
      }

      // 启用3个操作按钮
      data.actionDisabled = false
    },

    enableSubFormRow(rowIndex) {
      props.widget.widgetList.forEach(subWidget => {
        let swRefName = subWidget.options.name + '@row' + data.rowIdData[rowIndex]
        let foundSW = refMixin.getWidgetRef(swRefName)
        if (!!foundSW && !!foundSW.setDisabled) {
          foundSW.setDisabled(false)
        }
      })
    },

    enableTab(tabIndex) {
      if (tabIndex >= 0 && tabIndex < props.widget.tabs.length) {
        props.widget.tabs[tabIndex].options.disabled = false
      }
    },

    /**
     * 获取子表单的行数
     */

    getSubFormRowCount() {
      return !data.rowIdData ? 0 : data.rowIdData.length
    },

    getSubFormValues() {
      if (props.widget.type === 'm-sub-form') {
        // TODO: 逐行校验子表单！！
        return formModel.value[props.widget.options.name]
      } else {
        proxy.$message.error(i18nt('render.hint.nonSubFormType'))
      }
    },

    hideTab(tabIndex) {
      if (tabIndex >= 0 && tabIndex < props.widget.tabs.length) {
        props.widget.tabs[tabIndex].options.hidden = true
      }
    },

    /**
     * 动态移除自定义css样式
     * @param className
     */

    removeCssClass(className) {
      if (!props.widget.options.customClass) {
        return
      }

      let foundIdx = -1
      props.widget.options.customClass.map((cc, idx) => {
        if (cc === className) {
          foundIdx = idx
        }
      })
      if (foundIdx > -1) {
        props.widget.options.customClass.splice(foundIdx, 1)
      }
    },

    resetSubForm() {
      // 重置subForm数据为空
      if (props.widget.type === 'm-sub-form') {
        let subFormModel = formModel.value[props.widget.options.name]
        if (!!subFormModel) {
          subFormModel.splice(0, subFormModel.length)
          data.rowIdData.splice(0, data.rowIdData.length)
        }

        if (props.widget.options.showBlankRow) {
          if (methods.addSubFormRow) methods.addSubFormRow()
        }
      }
    },

    /**
     * 禁用或启用容器组件（包含容器内部的所有组件）
     * @param flag
     */

    setDisabled(flag) {
      const fwHandler = fw => {
        const fwName = fw.options.name
        const fwRef = refMixin.getWidgetRef(fwName)
        if (!!fwRef && !!fwRef.setDisabled) {
          fwRef.setDisabled(flag)
        }
      }
      const cwHandler = cw => {
        if (cw.id === props.widget.id) {
          // 避免死循环！！！
          return
        }

        const cwName = cw.options.name
        const cwRef = refMixin.getWidgetRef(cwName)
        if (!!cwRef && !!cwRef.setDisabled) {
          cwRef.setDisabled(flag)
        }
      }
      traverseWidgetsOfContainer(props.widget, fwHandler, cwHandler)

      // 注意：单行子表单、多行子表单容器的setDisabled方法由单行子表单、多行子表单组件自己实现！！
    },

    setGridSubFormRowDisabled(rowId, disabledFlag) {
      const fwHandler = fw => {
        const fwName = fw.options.name + '@row' + rowId
        const fwRef = refMixin.getWidgetRef(fwName)
        if (!!fwRef && !!fwRef.setDisabled) {
          fwRef.setDisabled(disabledFlag)
        }
      }
      const cwHandler = cw => {
        const cwName = cw.options.name + '@row' + rowId
        const cwRef = refMixin.getWidgetRef(cwName)
        if (!!cwRef && !!cwRef.setDisabled) {
          cwRef.setDisabled(disabledFlag)
        }
      }
      traverseWidgetsOfContainer(props.widget, fwHandler, cwHandler)
    },

    // --------------------- 以下为组件支持外部调用的API方法 begin ------------------//
    /* 提示：用户可自行扩充这些方法！！！ */

    setHidden(flag) {
      props.widget.options.hidden = flag

      const fwHandler = fw => {
        let fwName = fw.options.name
        let fwRef = refMixin.getWidgetRef(fwName)
        if (flag && !!fwRef && !!fwRef.clearFieldRules) {
          fwRef.clearFieldRules()
        }

        if (!flag && !!fwRef && !!fwRef.buildFieldRules) {
          fwRef.buildFieldRules()
        }
      }

      let sfArray = []
      const cwHandler = cw => {
        if (cw.type === 'm-sub-form') {
          sfArray.push(cw)
        }
      }
      traverseWidgetsOfContainer(props.widget, fwHandler, cwHandler)

      sfArray.forEach(sf => {
        const sfRef = refMixin.getWidgetRef(sf.options.name)
        if (!!sfRef) {
          const rowIds = sfRef.getRowIdData()
          const sfwHandler = sfw => {
            if (!!rowIds && rowIds.length > 0) {
              rowIds.forEach(rid => {
                const sfwName = sfw.options.name + '@row' + rid
                const sfwRef = refMixin.getWidgetRef(sfwName)
                if (flag && !!sfwRef && !!sfwRef.clearFieldRules) {
                  sfwRef.clearFieldRules()
                }

                if (!flag && !!sfwRef && !!sfwRef.buildFieldRules) {
                  sfwRef.buildFieldRules()
                }
              })
            }
          }

          traverseFieldWidgetsOfContainer(sfRef.widget, sfwHandler)
        }
      })
    },

    setSubFormValues() {
      // 在sub-form-item、grid-sub-form-item中实现！！
    },

    setWidgetOption(optionName, optionValue) {
      // 通用组件选项修改API
      // eslint-disable-next-line no-prototype-builtins
      if (props.widget.options.hasOwnProperty(optionName)) {
        props.widget.options[optionName] = optionValue
      }
    },
    showTab(tabIndex) {
      if (tabIndex >= 0 && tabIndex < props.widget.tabs.length) {
        props.widget.tabs[tabIndex].options.hidden = false
      }
    },
    unregisterFromRefList() {
      // 销毁容器组件时注销组件ref
      if (refList !== null && !!proxy.widget.options.name) {
        let oldRefName = proxy.widget.options.name
        delete refList[oldRefName]
      }
    }

    // --------------------- 以上为组件支持外部调用的API方法 end ------------------//
  }

  return {
    customClass,
    formModel,
    ...methods,
    ...refMixin
  }
}
