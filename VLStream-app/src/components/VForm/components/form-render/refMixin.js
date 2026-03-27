import { inject, getCurrentInstance } from 'vue'
import { useI18n } from '~@/utils/i18n'

export function useRef() {
  const i18nt = useI18n()
  const refList = inject('refList')
  const { proxy, props } = getCurrentInstance()

  const methods = {
    getComponentByContainer(con) {
      if (con.type === 'grid') {
        // grid-item跟VueGridLayout全局注册组件重名，故特殊处理！！
        return 'vf-grid-item'
      }

      return con.type + '-item'
    },

    getFormRef() {
      /* 获取VFrom引用，必须在VForm组件created之后方可调用 */
      return refList['v_form_ref']
    },

    getWidgetRef(widgetName, showError) {
      let foundRef = refList[widgetName]
      if (!foundRef && !!showError) {
        proxy.$message.error(i18nt('render.hint.refNotFound') + widgetName)
      }
      return foundRef
    },

    initRefList() {
      if (props.subFormRowIndex === -1 || props.subFormRowIndex === undefined) {
        // 容器组件未被嵌套于多行子表单之内
        if (refList !== null && !!props.widget.options.name) {
          refList[props.widget.options.name] = proxy
        }
      } else {
        // 容器组件被嵌套于多行子表单之内
        if (refList !== null && !!props.widget.options.name) {
          refList[props.widget.options.name + '@row' + props.subFormRowId] = proxy
        }
      }
    }
  }
  return {
    ...methods
  }
}
