/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

export default {
  methods: {
    initRefList() {
      if ((this.refList !== null) && !!this.widget.options.name) {
        this.refList[this.widget.options.name] = this
      }
    },

    getWidgetRef(widgetName, showError) {
      let foundRef = this.refList[widgetName]
      if (!foundRef && !!showError) {
        this.$message.error(this.i18nt('render.hint.refNotFound') + widgetName)
      }
      return foundRef
    },

    getFormRef() { /* 获取VFrom引用，必须在VForm组件created之后方可调用 */
      return this.refList['v_form_ref']
    },

    getComponentByContainer(con) {
      if (con.type === 'grid') { // grid-item VueGridLayout full component , Process ! !
        return 'vf-grid-item'
      }

      return con.type + '-item'
    }

  }
}
