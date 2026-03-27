import { inject, getCurrentInstance } from 'vue'
import { useI18n } from '~@/utils/i18n'

export function useDesignRef() {
  const refList = inject('refList')
  const { i18nt } = useI18n()
  const { proxy, props } = getCurrentInstance()
  let $current = proxy
  let widget = props.widget

  const methods = {
    getWidgetRef(widgetName, showError) {
      let foundRef = refList[widgetName]
      if (!foundRef && !!showError) {
        $current.$message.error(i18nt('render.hint.refNotFound') + widgetName)
      }
      return foundRef
    },

    initRefList() {
      if (refList !== null && !!widget.options.name) {
        refList[widget.options.name] = $current
      }
    },

    /* 该方法用于组件重名检查！！ */
    registerToRefList(oldRefName) {
      if (refList !== null && !!widget.options.name) {
        if (oldRefName) {
          delete refList[oldRefName]
        }
        refList[widget.options.name] = $current
      }
    }
  }
  return {
    ...methods
  }
}
