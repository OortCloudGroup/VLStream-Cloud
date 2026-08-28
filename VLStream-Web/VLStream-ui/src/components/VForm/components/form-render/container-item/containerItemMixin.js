/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import { traverseFieldWidgetsOfContainer } from '~@/utils/util'

export default {
  inject: ['getFormConfig', 'getGlobalDsv'],
  computed: {
    customClass() {
      return this.widget.options.customClass || ''
    },

    formModel: {
      cache: false,
      get() {
        return this.globalModel.formModel
      }
    }

  },
  mounted() {
    this.callSetHidden()
  },

  methods: {
    unregisterFromRefList() { // component componentref
      if ((this.refList !== null) && !!this.widget.options.name) {
        let oldRefName = this.widget.options.name
        delete this.refList[oldRefName]
      }
    },

    /* main setHidden() method , null / empty fieldcomponent Validate ! ! */
    callSetHidden() {
      if (this.widget.options.hidden === true) {
        this.setHidden(true)
      }
    },

    // --------------------- to component API method begin ------------------//
    /* prompt / tip: user method ! ! ! */

    setHidden(flag) {
      this.widget.options.hidden = flag

      /* after, need to fieldcomponent Validate */
      let clearRulesFn = (fieldWidget) => {
        let fwName = fieldWidget.options.name
        let fwRef = this.getWidgetRef(fwName)
        if (flag && !!fwRef && !!fwRef.clearFieldRules) {
          fwRef.clearFieldRules()
        }

        if (!flag && !!fwRef && !!fwRef.buildFieldRules) {
          fwRef.buildFieldRules()
        }
      }

      traverseFieldWidgetsOfContainer(this.widget, clearRulesFn)
    },

    activeTab(tabIndex) { // tabIndexfrom 0
      if ((tabIndex >= 0) && (tabIndex < this.widget.tabs.length)) {
        this.widget.tabs.forEach((tp, idx) => {
          tp.options.active = idx === tabIndex
          if (idx === tabIndex) {
            this.activeTabName = tp.options.name
          }
        })
      }
    },

    disableTab(tabIndex) {
      if ((tabIndex >= 0) && (tabIndex < this.widget.tabs.length)) {
        this.widget.tabs[tabIndex].options.disabled = true
      }
    },

    enableTab(tabIndex) {
      if ((tabIndex >= 0) && (tabIndex < this.widget.tabs.length)) {
        this.widget.tabs[tabIndex].options.disabled = false
      }
    },

    hideTab(tabIndex) {
      if ((tabIndex >= 0) && (tabIndex < this.widget.tabs.length)) {
        this.widget.tabs[tabIndex].options.hidden = true
      }
    },

    showTab(tabIndex) {
      if ((tabIndex >= 0) && (tabIndex < this.widget.tabs.length)) {
        this.widget.tabs[tabIndex].options.hidden = false
      }
    },

    setWidgetOption(optionName, optionValue) { // component item Update API
      // eslint-disable-next-line no-prototype-builtins
      if (this.widget.options.hasOwnProperty(optionName)) {
        this.widget.options[optionName] = optionValue
      }
    },

    /**
     * Get sub form
     */
    getSubFormRowCount() {
      return !this.rowIdData ? 0 : this.rowIdData.length
    },

    disableSubFormRow(rowIndex) {
      this.widget.widgetList.forEach(subWidget => {
        let swRefName = subWidget.options.name + '@row' + this.rowIdData[rowIndex]
        let foundSW = this.getWidgetRef(swRefName)
        if (!!foundSW) {
          foundSW.setDisabled(true)
        }
      })
    },

    enableSubFormRow(rowIndex) {
      this.widget.widgetList.forEach(subWidget => {
        let swRefName = subWidget.options.name + '@row' + this.rowIdData[rowIndex]
        let foundSW = this.getWidgetRef(swRefName)
        if (!!foundSW) {
          foundSW.setDisabled(false)
        }
      })
    },

    disableSubForm() {
      if (this.rowIdData.length > 0) {
        this.rowIdData.forEach((dataRow, rIdx) => {
          this.disableSubFormRow(rIdx)
        })
      }

      // 3 operationbutton
      this.actionDisabled = true
    },

    enableSubForm() {
      if (this.rowIdData.length > 0) {
        this.rowIdData.forEach((dataRow, rIdx) => {
          this.enableSubFormRow(rIdx)
        })
      }

      // 3 operationbutton
      this.actionDisabled = false
    },

    resetSubForm() { // subFormdata is empty
      if (this.widget.type === 'sub-form') {
        let subFormModel = this.formModel[this.widget.options.name]
        if (!!subFormModel) {
          subFormModel.splice(0, subFormModel.length)
          this.rowIdData.splice(0, this.rowIdData.length)
        }

        if (this.widget.options.showBlankRow) {
          this.addSubFormRow()
        }
      }
    },

    getSubFormValues() {
      if (this.widget.type === 'sub-form') {
        // TODO: Validate sub form! !
        return this.formModel[this.widget.options.name]
      } else {
        this.$message.error(this.i18nt('render.hint.nonSubFormType'))
      }
    },

    // validateField(fieldName) { // Validate sub formfield
    //   //TODO:
    // },
    //
    // validateSubForm() { // Validate sub form full field
    //   //TODO:
    // },

    /**
     * Customcss
     * @param className
     */
    addCssClass(className) {
      if (!this.widget.options.customClass) {
        this.widget.options.customClass = [className]
      } else {
        this.widget.options.customClass.push(className)
      }
    },

    /**
     * Customcss
     * @param className
     */
    removeCssClass(className) {
      if (!this.widget.options.customClass) {
        return
      }

      let foundIdx = -1
      this.widget.options.customClass.map((cc, idx) => {
        if (cc === className) {
          foundIdx = idx
        }
      })
      if (foundIdx > -1) {
        this.widget.options.customClass.splice(foundIdx, 1)
      }
    }

    // --------------------- to component API method end ------------------//

  }

}
