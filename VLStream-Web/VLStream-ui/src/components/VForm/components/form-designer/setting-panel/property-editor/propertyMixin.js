/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

export default {
  methods: {
    hasConfig(configName) {
      if (!this.designer || !this.designer.selectedWidget) {
        return false
      }

      return this.designer.hasConfig(this.selectedWidget, configName)
    },

    emitDefaultValueChange() {
      if (!!this.designer) {
        if (!!this.designer.formWidget) {
          let fieldWidget = this.designer.formWidget.getWidgetRef(this.designer.selectedWidget.options.name)
          if (!!fieldWidget && !!fieldWidget.refreshDefaultValue) {
            fieldWidget.refreshDefaultValue()
          }
        }
      }
    },

    inputNumberHandler(value) {
      value = value.replace(/[^0-9]/gi, '')
    },

    onRemoteChange(val) {
      if (!!val) {
        this.optionModel.filterable = true
        this.optionModel.allowCreate = false
      }
    },

    onMultipleSelected(val) {
      if (val) {
        this.optionModel.defaultValue = [] // null / empty value !!
      } else {
        if (!!this.optionModel.defaultValue && (this.optionModel.defaultValue.length > 0)) {
          this.optionModel.defaultValue = this.optionModel.defaultValue[0]
        } else {
          this.optionModel.defaultValue = ''
        }
      }
    }

  }
}
