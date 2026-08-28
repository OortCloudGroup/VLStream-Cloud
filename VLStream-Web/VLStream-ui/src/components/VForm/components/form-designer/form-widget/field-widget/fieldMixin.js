/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import { deepClone } from '~@/utils/util'
import FormValidators from '~@/utils/validators'
import eventBus from '~@/utils/event-bus'

export default {
  inject: ['refList', 'getFormConfig', 'getGlobalDsv', 'globalOptionData', 'globalModel', 'getOptionData'],

  computed: {
    formConfig() {
      return this.getFormConfig()
    },

    widgetSize() {
      return this.field.options.size || 'default'
    },

    subFormName() {
      return !!this.parentWidget ? this.parentWidget.options.name : ''
    },

    subFormItemFlag() {
      return !!this.parentWidget ? this.parentWidget.type === 'sub-form' : false
    },

    formModel: {
      cache: false,
      get() {
        return this.globalModel.formModel
      }
    }

  },

  methods: {

    // --------------------- component method begin ------------------//
    getPropName() {
      if (this.subFormItemFlag && !this.designState) {
        return this.subFormName + '.' + this.subFormRowIndex + '.' + this.field.options.name + ''
      } else {
        return this.field.options.name
      }
    },

    initFieldModel() {
      if (!this.field.formItemFlag) {
        return
      }

      if (!!this.subFormItemFlag && !this.designState) { // SubForm sub formcomponent need to Process ! !
        let subFormData = this.formModel[this.subFormName]
        if (((subFormData === undefined) || (subFormData[this.subFormRowIndex] === undefined) ||
            (subFormData[this.subFormRowIndex][this.field.options.name] === undefined)) &&
            (this.field.options.defaultValue !== undefined)) {
          this.fieldModel = this.field.options.defaultValue
          subFormData[this.subFormRowIndex][this.field.options.name] = this.field.options.defaultValue
        } else if (subFormData[this.subFormRowIndex][this.field.options.name] === undefined) {
          this.fieldModel = null
          subFormData[this.subFormRowIndex][this.field.options.name] = null
        } else {
          this.fieldModel = subFormData[this.subFormRowIndex][this.field.options.name]
        }

        /* main sub form field-widget onChangeevent! ! */
        setTimeout(() => { // onChangeevent, new field! !
          this.handleOnChangeForSubForm(this.fieldModel, this.oldFieldValue, subFormData, this.subFormRowId)
        }, 800)
        this.oldFieldValue = deepClone(this.fieldModel)

        this.initFileList() // Process 、 field

        return
      }

      if ((this.formModel[this.field.options.name] === undefined) &&
          (this.field.options.defaultValue !== undefined)) {
        this.fieldModel = this.field.options.defaultValue
      } else if (this.formModel[this.field.options.name] === undefined) { // if formModel is emptyobject, Initialize field value to null!!
        this.formModel[this.field.options.name] = null
      } else {
        this.fieldModel = this.formModel[this.field.options.name]
      }
      this.oldFieldValue = deepClone(this.fieldModel)
      this.initFileList() // Process 、 field
    },

    initFileList() { // Initialize component already
      if (((this.field.type !== 'picture-upload') && (this.field.type !== 'file-upload')) || (this.designState === true)) {
        return
      }

      if (!!this.fieldModel) {
        if (Array.isArray(this.fieldModel)) {
          this.fileList = deepClone(this.fieldModel)
        } else {
          this.fileList.splice(0, 0, deepClone(this.fieldModel))
        }
      }
    },

    initEventHandler() {
      this.on$('setFormData', (newFormData) => {
        console.log('formModel of globalModel----------', this.globalModel.formModel)
        if (!this.subFormItemFlag) {
          this.setValue(newFormData[this.field.options.name])
        }
      })

      this.on$('field-value-changed', (values) => {
        if (!!this.subFormItemFlag) {
          let subFormData = this.formModel[this.subFormName]
          this.handleOnChangeForSubForm(values[0], values[1], subFormData, this.subFormRowId)
        } else {
          this.handleOnChange(values[0], values[1])
        }
      })

      this.on$('reloadOptionItems', (widgetNames) => {
        if ((widgetNames.length === 0) || (widgetNames.indexOf(this.field.options.name) > -1)) {
          this.initOptionItems(true)
        }
      })
    },

    handleOnCreated() {
      if (!!this.field.options.onCreated) {
        let customFunc = new Function(this.field.options.onCreated)
        customFunc.call(this)
      }
    },

    handleOnMounted() {
      if (!!this.field.options.onMounted) {
        let mountFunc = new Function(this.field.options.onMounted)
        mountFunc.call(this)
      }
    },

    registerToRefList(oldRefName) {
      if ((this.refList !== null) && !!this.field.options.name) {
        if (this.subFormItemFlag && !this.designState) { // Process sub formelement ( non- )
          if (!!oldRefName) {
            delete this.refList[oldRefName + '@row' + this.subFormRowId]
          }
          this.refList[this.field.options.name + '@row' + this.subFormRowId] = this
        } else {
          if (!!oldRefName) {
            delete this.refList[oldRefName]
          }
          this.refList[this.field.options.name] = this
        }
      }
    },

    unregisterFromRefList() { // component componentref
      if ((this.refList !== null) && !!this.field.options.name) {
        let oldRefName = this.field.options.name
        if (this.subFormItemFlag && !this.designState) { // Process sub formelement ( non- )
          delete this.refList[oldRefName + '@row' + this.subFormRowId]
        } else {
          delete this.refList[oldRefName]
        }
      }
    },

    initOptionItems(keepSelected) {
      if (this.designState) {
        return
      }

      if ((this.field.type === 'radio') || (this.field.type === 'checkbox') ||
          (this.field.type === 'select') || (this.field.type === 'cascader')) {
        /* new option-data afterglobalOptionData can Get new value , provide getOptionData() method */
        const newOptionItems = this.getOptionData()
        // eslint-disable-next-line no-prototype-builtins
        if (!!newOptionItems && newOptionItems.hasOwnProperty(this.field.options.name)) {
          if (!!keepSelected) {
            this.reloadOptions(newOptionItems[this.field.options.name])
          } else {
            this.loadOptions(newOptionItems[this.field.options.name])
          }
        }
      }
    },

    refreshDefaultValue() {
      if ((this.designState === true) && (this.field.options.defaultValue !== undefined)) {
        this.fieldModel = this.field.options.defaultValue
      }
    },

    clearFieldRules() {
      if (!this.field.formItemFlag) {
        return
      }

      this.rules.splice(0, this.rules.length) // null / empty already
    },

    buildFieldRules() {
      if (!this.field.formItemFlag && this.field.options.hidden) {
        return
      }
      // if is component, component Validate , component , in Validate
      if (this.parentWidget && this.parentWidget.type === 'items-item') {
        return
      }
      if (!this.rules) {
        this.rules = []
      }
      this.rules.splice(0, this.rules.length) // null / empty already
      if (!!this.field.options.required) {
        this.rules.push({
          required: true,
          // trigger: ['blur', 'change'],
          trigger: ['blur'], /* 去掉change事件触发校验，change事件触发时formModel数据尚未更新，导致radio/checkbox必填校验出错！！ */
          message: this.field.options.requiredHint || this.i18nt('render.hint.fieldRequired')
        })
      }

      if (!!this.field.options.validation) {
        let vldName = this.field.options.validation
        if (!!FormValidators[vldName]) {
          this.rules.push({
            validator: FormValidators[vldName],
            trigger: ['blur', 'change'],
            label: this.field.options.label,
            errorMsg: this.field.options.validationHint
          })
        } else {
          this.rules.push({
            validator: FormValidators['regExp'],
            trigger: ['blur', 'change'],
            regExp: vldName,
            label: this.field.options.label,
            errorMsg: this.field.options.validationHint
          })
        }
      }

      if (!!this.field.options.onValidate) {
        let customFn = (rule, value, callback) => {
          let tmpFunc = new Function('rule', 'value', 'callback', this.field.options.onValidate)
          return tmpFunc.call(this, rule, value, callback)
        }
        this.rules.push({
          validator: customFn,
          trigger: ['blur', 'change'],
          label: this.field.options.label
        })
      }
    },

    /**
     * field value formValidate
     */
    disableChangeValidate() {
      if (!this.rules) {
        return
      }

      this.rules.forEach(rule => {
        if (!!rule.trigger) {
          rule.trigger.splice(0, rule.trigger.length)
        }
      })
    },

    /**
     * field value formValidate
     */
    enableChangeValidate() {
      if (!this.rules) {
        return
      }

      this.rules.forEach(rule => {
        if (!!rule.trigger) {
          rule.trigger.push('blur')
          rule.trigger.push('change')
        }
      })
    },

    disableOptionOfList(optionList, optionValue) {
      if (!!optionList && (optionList.length > 0)) {
        optionList.forEach(opt => {
          if (opt.value === optionValue) {
            opt.disabled = true
          }
        })
      }
    },

    enableOptionOfList(optionList, optionValue) {
      if (!!optionList && (optionList.length > 0)) {
        optionList.forEach(opt => {
          if (opt.value === optionValue) {
            opt.disabled = false
          }
        })
      }
    },

    // --------------------- component method end ------------------//

    // --------------------- eventProcess begin ------------------//

    emitFieldDataChange(newValue, oldValue) {
      this.emit$('field-value-changed', [newValue, oldValue])
      // full event
      eventBus.$emit('field-value-changed', this.field, newValue)
      /* dispatch component ! ! */
      this.dispatch('VFormRender', 'fieldChange',
        [this.field.options.name, newValue, oldValue, this.subFormName, this.subFormRowIndex])

      // if component is component component component changeevent
      // console.log('handleChangeEvent--------', this.parentWidget)
      if (this.parentWidget && this.parentWidget.type === 'items-item') {
        // console.log('itemsItemChange--------', [this.indexOfParentList, this.field.options.name, newValue])
        this.dispatch('ItemsItem', 'itemsItemChange', [this.indexOfParentList, this.field.options.name, newValue])
      }
    },

    syncUpdateFormModel(value) {
      if (!!this.designState) {
        return
      }

      if (!!this.subFormItemFlag) {
        let subFormData = this.formModel[this.subFormName] || [{}]
        let subFormDataRow = subFormData[this.subFormRowIndex]
        if (!!subFormDataRow) { // form aftersubFormDataRow to undefined, ! !
          subFormDataRow[this.field.options.name] = value
        }
      } else {
        this.formModel[this.field.options.name] = value
      }
    },

    handleChangeEvent(value) {
      this.syncUpdateFormModel(value)
      this.emitFieldDataChange(value, this.oldFieldValue)
      // numbercomponent will focusevent, need to value oldFieldValue! !
      this.oldFieldValue = deepClone(value) /* oldFieldValue需要在initFieldModel()方法中赋初值!! */

      /* main form fieldValidate , field can in Validate prompt / tip */
      this.dispatch('VFormRender', 'fieldValidation', [this.getPropName()])
      // if component is component component component changeevent
    },

    handleFocusCustomEvent(event) {
      this.oldFieldValue = deepClone(this.fieldModel) // Update change before value

      if (!!this.field.options.onFocus) {
        let customFn = new Function('event', this.field.options.onFocus)
        customFn.call(this, event)
      }
    },

    handleBlurCustomEvent(event) {
      if (!!this.field.options.onBlur) {
        let customFn = new Function('event', this.field.options.onBlur)
        customFn.call(this, event)
      }
    },

    handleInputCustomEvent(value) {
      this.syncUpdateFormModel(value)

      /* main form fieldValidate , field can in Validate prompt / tip */
      this.dispatch('VFormRender', 'fieldValidation', [this.getPropName()])

      if (!!this.field.options.onInput) {
        let customFn = new Function('value', this.field.options.onInput)
        customFn.call(this, value)
      }
    },

    emitAppendButtonClick() {
      if (!!this.designState) { // event
        return
      }

      if (!!this.field.options.onAppendButtonClick) {
        let customFn = new Function(this.field.options.onAppendButtonClick)
        customFn.call(this)
      } else {
        /* mixins in dispatch method component ! ! */
        this.dispatch('VFormRender', 'appendButtonClick', [this])
      }
    },

    handleOnChange(val, oldVal) { // CustomonChangeevent
      if (!!this.field.options.onChange) {
        let changeFn = new Function('value', 'oldValue', this.field.options.onChange)
        changeFn.call(this, val, oldVal)
      }
    },

    handleOnChangeForSubForm(val, oldVal, subFormData, rowId) { // sub formCustomonChangeevent
      if (!!this.field.options.onChange) {
        let changeFn = new Function('value', 'oldValue', 'subFormData', 'rowId', this.field.options.onChange)
        changeFn.call(this, val, oldVal, subFormData, rowId)
      }
    },

    handleButtonWidgetClick() {
      if (!!this.designState) { // event
        return
      }

      if (!!this.field.options.onClick) {
        let changeFn = new Function(this.field.options.onClick)
        changeFn.call(this)
      } else {
        this.dispatch('VFormRender', 'buttonClick', [this])
      }
    },

    remoteQuery(keyword) {
      if (!!this.field.options.onRemoteQuery) {
        let remoteFn = new Function('keyword', this.field.options.onRemoteQuery)
        remoteFn.call(this, keyword)
      }
    },

    // --------------------- eventProcess end ------------------//

    // --------------------- to component API method begin ------------------//
    /* prompt / tip: user method ! ! ! */

    getFormRef() { /* 获取VFrom引用，必须在VForm组件created之后方可调用 */
      return this.refList['v_form_ref']
    },

    getWidgetRef(widgetName, showError) {
      let foundRef = this.refList[widgetName]
      if (!foundRef && !!showError) {
        this.$message.error(this.i18nt('render.hint.refNotFound') + widgetName)
      }
      return foundRef
    },

    getFieldEditor() { // Get elformcomponent
      return this.$refs['fieldEditor']
    },

    /*
      注意：VFormRender的setFormData方法不会触发子表单内field-widget的setValue方法，
      因为setFormData方法调用后，子表单内所有field-widget组件已被清空，接收不到setFormData事件！！
    * */
    setValue(newValue) {
      /* if ((this.field.type === 'picture-upload') || (this.field.type === 'file-upload')) {
        this.fileList = newValue
      } else */ if (!!this.field.formItemFlag) {
        let oldValue = deepClone(this.fieldModel)
        this.fieldModel = newValue
        this.initFileList()

        this.syncUpdateFormModel(newValue)
        this.emitFieldDataChange(newValue, oldValue)
      }
    },

    getValue() {
      /* if ((this.field.type === 'picture-upload') || (this.field.type === 'file-upload')) {
        return this.fileList
      } else {
      }*/
      return this.fieldModel
    },

    resetField() {
      let defaultValue = this.field.options.defaultValue
      this.setValue(defaultValue)
      this.$nextTick(() => {
        //
      })

      // null / empty component
      if ((this.field.type === 'picture-upload') || (this.field.type === 'file-upload')) {
        this.$refs['fieldEditor'].clearFiles()
        this.fileList.splice(0, this.fileList.length)
      }
    },

    setWidgetOption(optionName, optionValue) { // component item Update API
      // eslint-disable-next-line no-prototype-builtins
      if (this.field.options.hasOwnProperty(optionName)) {
        this.field.options[optionName] = optionValue
        // TODO: whether new Build component? ? propertyUpdate after new Build component can , fieldValidate .
      }
    },

    setReadonly(flag) {
      this.field.options.readonly = flag
    },

    setDisabled(flag) {
      this.field.options.disabled = flag
    },

    setAppendButtonVisible(flag) {
      this.field.options.appendButton = flag
    },

    setAppendButtonDisabled(flag) {
      this.field.options.appendButtonDisabled = flag
    },

    setHidden(flag) {
      this.field.options.hidden = flag

      if (!!flag) { // componentValidate
        this.clearFieldRules()
      } else { // componentValidate
        this.buildFieldRules()
      }
    },

    setRequired(flag) {
      this.field.options.required = flag
      this.buildFieldRules()
    },

    setLabel(newLabel) {
      this.field.options.label = newLabel
    },

    focus() {
      if (!!this.getFieldEditor() && !!this.getFieldEditor().focus) {
        this.getFieldEditor().focus()
      }
    },

    clearSelectedOptions() { // null / empty already item
      if ((this.field.type !== 'checkbox') && (this.field.type !== 'radio') && (this.field.type !== 'select')) {
        return
      }

      if ((this.field.type === 'checkbox') ||
          ((this.field.type === 'select') && this.field.options.multiple)) {
        this.fieldModel = []
      } else {
        this.fieldModel = ''
      }
    },

    /**
     * Load item , null / empty field value
     * @param options
     */
    loadOptions(options) {
      this.field.options.optionItems = deepClone(options)
      // this.clearSelectedOptions() // null / empty already item
    },

    /**
     * new Load item , null / empty field value
     * @param options
     */
    reloadOptions(options) {
      this.field.options.optionItems = deepClone(options)
    },

    disableOption(optionValue) {
      this.disableOptionOfList(this.field.options.optionItems, optionValue)
    },

    enableOption(optionValue) {
      this.enableOptionOfList(this.field.options.optionItems, optionValue)
    },

    /**
     * item
     * @returns {*}
     */
    getOptionItems() {
      return this.field.options.optionItems
    },

    setUploadHeader(name, value) {
      this.uploadHeaders[name] = value
    },

    setUploadData(name, value) {
      this.uploadData[name] = value
    },

    setToolbar(customToolbar) {
      this.customToolbar = customToolbar
    },

    /**
     * whether sub form component
     * @returns {boolean}
     */
    isSubFormItem() {
      return !!this.parentWidget ? this.parentWidget.type === 'sub-form' : false
    },

    /**
     * Customcss
     * @param className
     */
    addCssClass(className) {
      if (!this.field.options.customClass) {
        this.field.options.customClass = [className]
      } else {
        this.field.options.customClass.push(className)
      }
    },

    /**
     * Customcss
     * @param className
     */
    removeCssClass(className) {
      if (!this.field.options.customClass) {
        return
      }

      let foundIdx = -1
      this.field.options.customClass.map((cc, idx) => {
        if (cc === className) {
          foundIdx = idx
        }
      })
      if (foundIdx > -1) {
        this.field.options.customClass.splice(foundIdx, 1)
      }
    }

    // --------------------- to component API method end ------------------//

  }
}
