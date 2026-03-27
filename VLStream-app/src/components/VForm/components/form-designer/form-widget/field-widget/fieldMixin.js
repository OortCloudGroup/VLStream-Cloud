import { deepClone, getDSByName, overwriteObj, runDataSourceRequest, translateOptionItems } from '~@/utils/util'
import FormValidators from '~@/utils/validators'
import { inject, ref, toRefs, computed, getCurrentInstance, nextTick } from 'vue'

import { useEmitter } from '~@/utils/emitter'
import { useI18n } from '~@/utils/i18n'
// import { px2rem } from '~@/utils/util'
import eventBus from '~@/utils/event-bus'

export function useField(data) {
  const { i18nt } = useI18n()
  const emitter = useEmitter(data)
  const { proxy, props } = getCurrentInstance()

  const refList = inject('refList')
  const getFormConfig = inject('getFormConfig')
  const globalOptionData = inject('globalOptionData')
  const globalModel = inject('globalModel')
  const getOptionData = inject('getOptionData')
  const getGlobalDsv = inject('getGlobalDsv')
  const getReadMode = inject('getReadMode')
  const getSubFormFieldFlag = inject('getSubFormFieldFlag')
  const getSubFormName = inject('getSubFormName')
  const getDSResultCache = inject('getDSResultCache')

  data.fieldReadonlyFlag = ref(false)
  data.dataSetLoadedFlag = ref(false)

  const fieldEditor = ref(null)

  const fieldLabel = computed(() => {
    return !!props.field.options.labelHidden ? '' : props.field.options.label
  })

  const fieldLabelWidth = computed(() => {
    if (formConfig.value.labelPosition === 'top') {
      return '100%'
    }

    if (!!props.field.options.labelHidden) return 0

    // if (!!props.field.options.labelWidth) return px2rem(props.field.options.labelWidth)

    // if (!!getFormConfig().labelWidth) return px2rem(formConfig.value.labelWidth)
    return '80px'
  })

  const labelAlign = computed(() => {
    if (props.field.options.labelAlign) {
      return props.field.options.labelAlign
    }

    return formConfig.value.labelAlign || 'left'
  })

  const formConfig = computed(() => {
    return getFormConfig()
  })

  const subFormName = computed(() => {
    return !!getSubFormName ? getSubFormName() : ''
  })

  const subFormItemFlag = computed(() => {
    return !!getSubFormFieldFlag ? getSubFormFieldFlag() : false
  })

  const formModel = computed({
    cache: false,
    get() {
      return globalModel.formModel
    }
  })

  const isReadMode = computed(() => {
    // return getReadMode() || data.fieldReadonlyFlag.value
    return !!getReadMode() ? true : data.fieldReadonlyFlag.value
  })

  const optionLabel = computed(() => {
    if (data.fieldModel === null) {
      return '--'
    } else {
      let resultContent = '--'
      props.field.options.optionItems.forEach(oItem => {
        if (oItem.value === data.fieldModel || methods.findInArray(data.fieldModel, oItem.value) !== -1) {
          resultContent = resultContent === '--' ? oItem.label : resultContent + ' ' + oItem.label
        }
      })

      return resultContent
    }
  })

  const size = computed(() => {
    if (!!props.field.options.size) return props.field.options.size
    return formConfig.value.size || ''
  })

  const disabledInDesign = computed(() => {
    return !!props.designState
  })

  const fieldDisabled = computed(() => {
    return !!props.designState || props.field.options.disabled
  })

  const clearable = computed(() => {
    if (!props.field.options.clearable || isReadMode.value) {
      return false
    }
    if (Array.isArray(data.fieldModel)) {
      return data.fieldModel && data.fieldModel.length > 0
    } else {
      return data.fieldModel !== null && data.fieldModel !== ''
    }
  })

  const methods = {
    /**
     * 动态增加自定义css样式
     * @param className
     */
    addCssClass(className) {
      if (!props.field.options.customClass) {
        props.field.options.customClass = [className]
      } else {
        props.field.options.customClass.push(className)
      }
    },
    buildFieldRules() {
      if (!props.field.formItemFlag) {
        return
      }

      data.rules.splice(0, data.rules.length) // 清空已有
      if (!!props.field.options.required) {
        data.rules.push({
          message: props.field.options.requiredHint || i18nt('render.hint.fieldRequired'),
          required: true /* 去掉change事件触发校验，change事件触发时formModel数据尚未更新，导致radio/checkbox必填校验出错！！ */,
          // trigger: ['blur', 'change'],
          trigger: 'onBlur'
        })
      }

      if (!!props.field.options.validation) {
        let vldName = props.field.options.validation
        if (!!FormValidators[vldName]) {
          data.rules.push({
            label: props.field.options.label,
            message: props.field.options.validationHint,
            trigger: 'onBlur',
            validator: FormValidators[vldName]
          })
        } else {
          data.rules.push({
            label: props.field.options.label,
            message: props.field.options.validationHint,
            regExp: vldName,
            trigger: 'onChange',
            validator: FormValidators['regExp']
          })
        }
      }

      if (!!props.field.options.onValidate) {
        let customFn = (value, rule) => {
          let tmpFunc = new Function('rule', 'value', props.field.options.onValidate)
          // return tmpFunc.call(this, rule, value)  //此处value应该替换为this.fieldModel？？
          return tmpFunc.call(props, rule, data.fieldModel)
        }
        data.rules.push({
          label: props.field.options.label,
          message: props.field.options.validationHint,
          trigger: 'onChange',
          validator: customFn
        })
      }
      // if (!!props.field.options.onValidate) {
      //   //let customFn = new Function('rule', 'value', 'callback', props.field.options.onValidate)
      //   let customFn = (rule, value, callback) => {
      //     let tmpFunc = new Function('rule', 'value', 'callback', props.field.options.onValidate);
      //     return tmpFunc.call(proxy, rule, value, callback);
      //   };
      //   data.rules.push({
      //     validator: customFn,
      //     trigger: ['blur', 'change'],
      //     label: props.field.options.label,
      //   });
      // }
    },

    clearFieldRules() {
      if (!props.field.formItemFlag) {
        return
      }

      data.rules.splice(0, data.rules.length) // 清空已有
    },

    clearSelectedOptions() {
      // 清空已选选项
      if (props.field.type !== 'm-checkbox' && props.field.type !== 'm-radio' && props.field.type !== 'm-select') {
        return
      }

      if (props.field.type === 'm-checkbox' || (props.field.type === 'm-select' && props.field.options.multiple)) {
        data.fieldModel = []
      } else {
        data.fieldModel = ''
      }
    },

    /**
     * 清除字段校验提示
     */

    clearValidate() {
      if (!!props.designState) {
        return
      }
      methods.getFormRef().clearValidate(methods.getPropName())
    },

    /**
     * 禁用字段值变动触发表单校验
     */

    disableChangeValidate() {
      if (!data.rules) {
        return
      }

      data.rules.forEach(rule => {
        if (!!rule.trigger) {
          rule.trigger.splice(0, rule.trigger.length)
        }
      })
    },

    disableOption(optionValue) {
      methods.disableOptionOfList(props.field.options.optionItems, optionValue)
    },

    disableOptionOfList(optionList, optionValue) {
      if (!!optionList && optionList.length > 0) {
        optionList.forEach(opt => {
          if (opt.value === optionValue) {
            opt.disabled = true
          }
        })
      }
    },

    emitAppendButtonClick() {
      if (!!props.designState) {
        // 设计状态不触发点击事件
        return
      }

      if (!!props.field.options.onAppendButtonClick) {
        let customFn = new Function(props.field.options.onAppendButtonClick)
        customFn.call(proxy)
      } else {
        /* 必须调用mixins中的dispatch方法逐级向父组件发送消息！！ */
        emitter.dispatch('VmFormRender', 'appendButtonClick', [proxy])
      }
    },

    // --------------------- 组件内部方法 end ------------------//

    // --------------------- 事件处理 begin ------------------//

    emitFieldDataChange(newValue, oldValue) {
      emitter.emit$('field-value-changed', [newValue, oldValue])
      /* 必须用dispatch向指定父组件派发消息！！ */
      emitter.dispatch('VmFormRender', 'fieldChange', [props.field.options.name, newValue, oldValue, subFormName.value, props.subFormRowIndex])

      // console.log('handleChangeEvent--------', this.parentWidget)
      if (!!props.parentWidget && props.parentWidget.type === 'items-item') {
        emitter.dispatch('ItemsItem', 'itemsItemChange', [props.indexOfParentList, props.field.options.name, newValue])
      }
      eventBus.$emit('field-value-changed', props.field)
    },

    /**
     * 启用字段值变动触发表单校验
     */

    enableChangeValidate() {
      if (!data.rules) {
        return
      }

      data.rules.forEach(rule => {
        if (!!rule.trigger) {
          rule.trigger.push('blur')
          rule.trigger.push('change')
        }
      })
    },

    enableOption(optionValue) {
      methods.enableOptionOfList(props.field.options.optionItems, optionValue)
    },

    enableOptionOfList(optionList, optionValue) {
      if (!!optionList && optionList.length > 0) {
        optionList.forEach(opt => {
          if (opt.value === optionValue) {
            opt.disabled = false
          }
        })
      }
    },
    /**
     * 执行数据源请求
     * @param dsName
     * @param localDsv
     */

    async executeDataSource(dsName, localDsv) {
      let ds = getDSByName(formConfig.value, dsName)
      // eslint-disable-next-line no-new-object
      let newDsv = new Object({})
      overwriteObj(newDsv, getGlobalDsv() || {})
      overwriteObj(newDsv, localDsv)
      return await runDataSourceRequest(ds, newDsv, methods.getFormRef(), false, proxy.$message)
    },

    findInArray(arrayObject, element) {
      if (!Array.isArray(arrayObject)) {
        return -1
      }

      let foundIdx = -1
      arrayObject.forEach((aItem, aIdx) => {
        if (aItem === element) {
          foundIdx = aIdx
        }
      })

      return foundIdx
    },

    focus() {
      if (!!methods.getFieldEditor() && !!methods.getFieldEditor().focus) {
        methods.getFieldEditor().focus()
      }
    },
    getFieldEditor() {
      // 获取内置的el表单组件
      return fieldEditor.value
    },

    // --------------------- 事件处理 end ------------------//

    // --------------------- 以下为组件支持外部调用的API方法 begin ------------------//
    /* 提示：用户可自行扩充这些方法！！！ */
    getFormRef() {
      /* 获取VFrom引用，必须在VForm组件created之后方可调用 */
      return refList['v_form_ref']
    },

    /**
     * 返回选择项
     * @returns {*}
     */

    getOptionItems() {
      return props.field.options.optionItems
    },

    // --------------------- 组件内部方法 begin ------------------//

    getPropName() {
      if (subFormItemFlag.value && !props.designState) {
        return subFormName.value + '.' + props.subFormRowIndex + '.' + props.field.options.name + ''
      } else {
        return props.field.options.name
      }
    },

    /**
     * 返回选项类字段的当前选中项的label值
     */
    getSelectedLabel() {
      // TODO: 待实现！！
    },

    getValue() {
      return data.fieldModel
    },

    getWidgetRef(widgetName, showError) {
      let foundRef = refList[widgetName]
      if (!foundRef && !!showError) {
        proxy.$message.error(i18nt('render.hint.refNotFound') + widgetName)
      }
      return foundRef
    },

    handleBlurCustomEvent(event) {
      if (!!props.designState) {
        // 设计状态不触发事件
        return
      }

      if (!!props.field.options.onBlur) {
        let customFn = new Function('event', props.field.options.onBlur)
        customFn.call(proxy, event)
      }

      emitter.dispatch('VmFormRender', 'fieldValidation', [methods.getPropName()])
    },

    handleButtonWidgetClick() {
      if (!!props.designState) {
        // 设计状态不触发点击事件
        return
      }

      if (!!props.field.options.onClick) {
        let customFn = new Function(props.field.options.onClick)
        customFn.call(proxy)
      } else {
        emitter.dispatch('VmFormRender', 'buttonClick', [proxy])
      }
    },

    handleChangeEvent(value) {
      if (!!props.designState) {
        // 设计状态不触发事件
        return
      }

      methods.syncUpdateFormModel(value)
      methods.emitFieldDataChange(value, props.oldFieldValue)

      // number组件一般不会触发focus事件，故此处需要手工赋值oldFieldValue！！
      props.oldFieldValue = deepClone(value) /* oldFieldValue需要在initFieldModel()方法中赋初值!! */

      /* 主动触发表单的单个字段校验，用于清除字段可能存在的校验错误提示 */
      emitter.dispatch('VmFormRender', 'fieldValidation', [methods.getPropName()])
    },

    handleFocusCustomEvent(event) {
      if (!!props.designState) {
        // 设计状态不触发事件
        return
      }

      props.oldFieldValue = deepClone(data.fieldModel) // 保存修改change之前的值

      if (!!props.field.options.onFocus) {
        let customFn = new Function('event', props.field.options.onFocus)
        customFn.call(proxy, event)
      }
    },

    handleInputCustomEvent(value) {
      if (!!props.designState) {
        // 设计状态不触发事件
        return
      }
      methods.syncUpdateFormModel(value)

      /* 主动触发表单的单个字段校验，用于清除字段可能存在的校验错误提示 */
      emitter.dispatch('VmFormRender', 'fieldValidation', [methods.getPropName()])

      if (!!props.field.options.onInput) {
        let customFn = new Function('value', props.field.options.onInput)
        customFn.call(proxy, value)
      }
    },

    handleOnChange(val, oldVal) {
      // 自定义onChange事件
      if (!!props.designState) {
        // 设计状态不触发事件
        return
      }

      if (!!props.field.options.onChange) {
        let changeFn = new Function('value', 'oldValue', 'subFormData', 'rowId', props.field.options.onChange)
        changeFn.call(proxy, val, oldVal, null, null)
      }
    },

    handleOnChangeForSubForm(val, oldVal, subFormData, rowId) {
      // 子表单自定义onChange事件
      if (!!props.designState) {
        // 设计状态不触发事件
        return
      }

      if (!!props.field.options.onChange) {
        let changeFn = new Function('value', 'oldValue', 'subFormData', 'rowId', props.field.options.onChange)
        changeFn.call(proxy, val, oldVal, subFormData, rowId)
      }
    },

    handleOnConfirm(value) {
      if (!!props.field.options.onConfirm) {
        let customFunc = new Function('value', props.field.options.onConfirm)
        customFunc.call(proxy, value)
      }
    },

    handleOnCreated() {
      if (!!props.designState) {
        // 设计状态不触发事件
        return
      }

      if (!!props.field.options.onCreated) {
        let customFunc = new Function(props.field.options.onCreated)
        customFunc.call(proxy)
      }
    },

    handleOnMounted() {
      if (!!props.designState) {
        // 设计状态不触发事件
        return
      }

      if (!!props.field.options.onMounted) {
        let mountFunc = new Function(props.field.options.onMounted)
        mountFunc.call(proxy)
      }
    },

    initDSVSearch() {
      if (props.field.options.filterable && props.field.options.remote) {
        let gDsv = getGlobalDsv() || {}
        gDsv.search = ''
      }
    },
    initDefaultValue() {
      if (props.designState === true) {
        return
      }
      if (proxy && proxy.initDisplayValue) proxy.initDisplayValue()
    },

    initEventHandler() {
      emitter.on$('setFormData', newFormData => {
        // console.log('formModel of globalModel----------', globalModel.formModel)
        if (!subFormItemFlag.value) {
          methods.setValue(newFormData[props.field.options.name])
        }
      })

      emitter.on$('field-value-changed', values => {
        if (!!subFormItemFlag.value) {
          let subFormData = formModel.value[subFormName.value]
          methods.handleOnChangeForSubForm(values[0], values[1], subFormData, props.subFormRowId)
        } else {
          methods.handleOnChange(values[0], values[1])
        }
      })

      /* 监听从数据集加载选项事件 */
      emitter.on$('loadOptionItemsFromDataSet', dsName => {
        methods.loadOptionItemsFromDataSet(dsName)
        data.dataSetLoadedFlag = true
      })

      emitter.on$('reloadOptionItems', widgetNames => {
        if (widgetNames.length === 0 || widgetNames.indexOf(props.field.options.name) > -1) {
          methods.initOptionItems(true)
        }
      })
    },
    initFieldModel() {
      if (!props.field.formItemFlag) {
        return
      }

      if (!!subFormItemFlag.value && !props.designState) {
        // SubForm子表单组件需要特殊处理！！
        let subFormData = formModel.value[subFormName.value]

        if (
          (subFormData === undefined || subFormData[props.subFormRowIndex] === undefined || subFormData[props.subFormRowIndex][props.field.options.name] === undefined) &&
          props.field.options.defaultValue !== undefined
        ) {
          data.fieldModel = props.field.options.defaultValue
          subFormData[props.subFormRowIndex][props.field.options.name] = props.field.options.defaultValue
        } else if (subFormData[props.subFormRowIndex][props.field.options.name] === undefined) {
          data.fieldModel = null
          subFormData[props.subFormRowIndex][props.field.options.name] = null
        } else {
          data.fieldModel = subFormData[props.subFormRowIndex][props.field.options.name]
        }

        /* 主动触发子表单内field-widget的onChange事件！！ */
        setTimeout(() => {
          // 延时触发onChange事件, 便于更新计算字段！！
          methods.handleOnChangeForSubForm(data.fieldModel, props.oldFieldValue, subFormData, props.subFormRowId)
        }, 800)
        props.oldFieldValue = deepClone(data.fieldModel)

        methods.initFileList() // 处理图片上传、文件上传字段

        return
      }

      if (formModel.value && formModel.value[props.field.options.name] === undefined && props.field.options.defaultValue !== undefined) {
        data.fieldModel = props.field.options.defaultValue
      } else if (!!formModel.value && formModel.value[props.field.options.name] === undefined) {
        // 如果formModel为空对象，则初始化字段值为null!!
        formModel.value[props.field.options.name] = null
      } else {
        data.fieldModel = formModel.value[props.field.options.name]
      }
      props.oldFieldValue = deepClone(data.fieldModel)
      methods.initFileList() // 处理图片上传、文件上传字段
    },
    initFileList() {
      // 初始化上传组件的已上传文件列表
      if ((props.field.type !== 'm-picture-upload' && props.field.type !== 'm-file-upload') || props.designState === true) {
        return
      }

      if (!!data.fieldModel) {
        if (Array.isArray(data.fieldModel)) {
          data.fileList = deepClone(data.fieldModel)
          data.uploadBtnHidden = data.fileList.length >= props.field.options.limit
        } else {
          data.fileList.splice(0, 0, deepClone(data.fieldModel))
          data.uploadBtnHidden = props.field.options.limit <= 1
        }
      }
    },

    /**
     * 初始化选项数据
     *
     * @param keepSelected 是否保留已选项
     * @returns 无返回值
     */

    async initOptionItems(keepSelected) {
      if (props.designState) {
        return
      }
      if (props.field.type === 'm-radio' || props.field.type === 'm-checkbox' || props.field.type === 'm-select' || props.field.type === 'm-cascader') {
        /* 首先处理数据源选项加载 */
        if (!!props.field.options.dsEnabled) {
          props.field.options.optionItems.splice(0, props.field.options.optionItems.length) // 清空原有选项
          let curDSName = props.field.options.dsName
          let curDSetName = props.field.options.dataSetName
          let curDS = getDSByName(formConfig.value, curDSName)
          if (!!curDS && !curDSetName) {
            let gDsv = getGlobalDsv() || {}
            // console.log('Global DSV is: ', gDsv)
            // eslint-disable-next-line no-new-object
            let localDsv = new Object({})
            overwriteObj(localDsv, gDsv)
            localDsv['widgetName'] = props.field.options.name
            let dsResult = null
            try {
              dsResult = await runDataSourceRequest(curDS, localDsv, methods.getFormRef(), false, proxy.$message)
              methods.reloadOptions(dsResult)
            } catch (err) {
              proxy.$message.error(err.message)
            }
          } else if (!!curDS && !!curDSetName && !data.dataSetLoadedFlag) {
            methods.loadOptionItemsFromDataSet(curDSName)
          }

          return
        }

        /* 异步更新option-data之后globalOptionData不能获取到最新值，改用provide的getOptionData()方法 */
        const newOptionItems = getOptionData()
        // eslint-disable-next-line no-prototype-builtins
        if (!!newOptionItems && newOptionItems.hasOwnProperty(props.field.options.name)) {
          if (!!keepSelected) {
            methods.reloadOptions(newOptionItems[props.field.options.name])
          } else {
            methods.loadOptions(newOptionItems[props.field.options.name])
          }
        }
      }
    },

    /**
     * 是否子表单内嵌的字段组件
     * @returns {boolean}
     */

    isSubFormField() {
      return subFormItemFlag.value
    },

    /**
     * 是否子表单内嵌的字段组件
     * @returns {boolean}
     */

    isSubFormItem() {
      return subFormItemFlag.value
    },
    loadOptionItemsFromDataSet(dsName) {
      if (props.designState) {
        return
      }

      if (props.field.type !== 'm-radio' && props.field.type !== 'm-checkbox' && props.field.type !== 'm-select' && props.field.type !== 'm-cascader') {
        return
      }

      if (!props.field.options.dsEnabled || !props.field.options.dsName || !props.field.options.dataSetName || props.field.options.dsName !== dsName) {
        return
      }

      const dataCache = getDSResultCache()
      const dSetName = props.field.options.dataSetName
      if (!!dataCache && !!dataCache[dsName] && !!dataCache[dsName][dSetName]) {
        props.field.options.optionItems.splice(0, props.field.options.optionItems.length) // 清空原有选项
        methods.reloadOptions(dataCache[dsName][dSetName])
      }
    },
    /**
     * 加载选项，并清空字段值
     * @param options
     */

    loadOptions(options) {
      /*
      props.field.options.optionItems = deepClone(options)
      */
      methods.clearSelectedOptions() // 清空已选选项
      methods.reloadOptions(options)
    },

    refreshDefaultValue() {
      if (props.designState === true && props.field.options.defaultValue !== undefined) {
        data.fieldModel = props.field.options.defaultValue
      }
    },

    registerToRefList(oldRefName) {
      if (refList !== null && !!props.field.options.name) {
        if (subFormItemFlag.value && !props.designState) {
          // 处理子表单元素（且非设计状态）
          if (!!oldRefName) {
            delete refList[oldRefName + '@row' + props.subFormRowId]
          }
          refList[props.field.options.name + '@row' + props.subFormRowId] = proxy
        } else {
          if (!!oldRefName) {
            delete refList[oldRefName]
          }
          refList[props.field.options.name] = proxy
        }
      }
    },

    /**
     * 重新加载选项，不清空字段值
     * @param options
     */

    /**
     * 重新加载选项(不清空字段值)
     *
     * @param options 选项数组
     */

    reloadOptions(options) {
      // props.field.options.optionItems = deepClone(options)

      let optionItems = translateOptionItems(options, props.field.type, props.field.options.labelKey || 'label', props.field.options.valueKey || 'value')
      props.field.options.optionItems.splice(0)
      props.field.options.optionItems.push(...optionItems)
    },

    remoteDataSourceQuery() {
      const searchPromise = this.executeDataSource(props.field.options.dsName, { search: data.searchVal })
      searchPromise.then(data => {
        let curDSetName = props.field.options.dataSetName
        let searchResult = []
        if (curDSetName) {
          searchResult = data[curDSetName]
        } else {
          searchResult = data
        }
        if (!Array.isArray(searchResult)) {
          console.error('remoteDataSourceQuery返回值必须是数组')
          return
        }
        props.field.options.optionItems = searchResult.map(x => ({ label: x[props.field.options.labelKey || 'label'], value: x[props.field.options.valueKey || 'value'] }))
      })
    },
    remoteQuery(keyword) {
      if (!!props.designState) {
        // 设计状态不触发事件
        return
      }

      if (!!props.field.options.onRemoteQuery) {
        let remoteFn = new Function('keyword', props.field.options.onRemoteQuery)
        remoteFn.call(proxy, keyword)
      }
    },

    /**
     * 动态移除自定义css样式
     * @param className
     */

    removeCssClass(className) {
      if (!props.field.options.customClass) {
        return
      }

      let foundIdx = -1
      props.field.options.customClass.map((cc, idx) => {
        if (cc === className) {
          foundIdx = idx
        }
      })
      if (foundIdx > -1) {
        props.field.options.customClass.splice(foundIdx, 1)
      }
    },

    resetField() {
      let defaultValue = props.field.options.defaultValue
      methods.setValue(defaultValue)
      nextTick(() => {
        //
      })

      // 清空上传组件文件列表
      if (props.field.type === 'm-picture-upload' || props.field.type === 'm-file-upload') {
        data.fileList.splice(0, data.fileList.length)
      }
    },

    setAppendButtonDisabled(flag) {
      props.field.options.appendButtonDisabled = flag
    },
    setAppendButtonVisible(flag) {
      props.field.options.appendButton = flag
    },
    setDisabled(flag) {
      props.field.options.disabled = flag
    },

    setHidden(flag) {
      props.field.options.hidden = flag

      if (!!flag) {
        // 清除组件校验规则
        methods.clearFieldRules()
      } else {
        // 重建组件校验规则
        methods.buildFieldRules()
      }
    },

    setLabel(newLabel) {
      props.field.options.label = newLabel
    },

    /**
     * 设置或取消设置字段只读查看模式
     * @param readonlyFlag
     */
    setReadMode(readonlyFlag = true) {
      data.fieldReadonlyFlag.value = readonlyFlag
    },

    setReadonly(flag) {
      props.field.options.readonly = flag
    },

    setRequired(flag) {
      props.field.options.required = flag
      methods.buildFieldRules()

      if (!props.designState && !flag) {
        // 清除必填校验提示
        methods.clearValidate()
      }
    },

    setToolbar(customToolbar) {
      data.customToolbar = customToolbar
    },
    setUploadData(name, value) {
      data.uploadData[name] = value
    },
    setUploadHeader(name, value) {
      data.uploadHeaders[name] = value
    },

    /*
      注意：VmFormRender的setFormData方法不会触发子表单内field-widget的setValue方法，
      因为setFormData方法调用后，子表单内所有field-widget组件已被清空，接收不到setFormData事件！！
    * */
    setValue(newValue) {
      /* if ((props.field.type === 'picture-upload') || (props.field.type === 'file-upload')) {
        data.fileList = newValue
      } else */
      if (!!props.field.formItemFlag) {
        let oldValue = deepClone(data.fieldModel)
        if (proxy.setFieldModel) {
          proxy.setFieldModel(newValue)
        } else {
          data.fieldModel = newValue
        }
        methods.initFileList()
        methods.initDefaultValue()

        methods.syncUpdateFormModel(newValue)
        methods.emitFieldDataChange(newValue, oldValue)
      }
    },
    setWidgetOption(optionName, optionValue) {
      // 通用组件选项修改API
      // eslint-disable-next-line no-prototype-builtins
      if (props.field.options.hasOwnProperty(optionName)) {
        props.field.options[optionName] = optionValue
        // TODO: 是否重新构建组件？？有些属性修改后必须重新构建组件才能生效，比如字段校验规则。
      }
    },
    syncUpdateFormModel(value) {
      if (!!props.designState) {
        return
      }
      if (!!subFormItemFlag.value) {
        let subFormData = formModel.value[subFormName.value] || [{}]
        let subFormDataRow = subFormData[props.subFormRowIndex]
        if (!!subFormDataRow) {
          // 重置表单后subFormDataRow为undefined，应跳过！！
          subFormDataRow[props.field.options.name] = value
        }
      } else {
        formModel.value[props.field.options.name] = value
      }
    },
    unregisterFromRefList() {
      // 销毁组件时注销组件ref
      if (refList !== null && !!props.field.options.name) {
        let oldRefName = props.field.options.name
        if (subFormItemFlag.value && !props.designState) {
          // 处理子表单元素（且非设计状态）
          delete refList[oldRefName + '@row' + props.subFormRowId]
        } else {
          delete refList[oldRefName]
        }
      }
    }
  }

  // --------------------- 以上为组件支持外部调用的API方法 end ------------------//

  return {
    clearable,
    fieldEditor,
    getDSResultCache,
    getFormConfig,
    getGlobalDsv,
    getOptionData,
    getReadMode,
    getSubFormFieldFlag,
    getSubFormName,
    globalModel,
    globalOptionData,
    refList,

    ...toRefs(data),

    disabledInDesign,
    fieldDisabled,

    // computed 计算函数
    fieldLabel,
    fieldLabelWidth,
    formConfig,
    formModel,
    isReadMode,
    labelAlign,
    methods, // 在组件中可以使用fieldMixin.methods.xxx覆盖methods中的方法
    optionLabel,
    size,
    subFormItemFlag,
    subFormName,
    ...emitter,
    ...methods
  }
}
