<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<!-- eslint-disable no-prototype-builtins -->
<!--
/**
 * author: vformAdmin
 * email: vdpadmin@163.com
 * website: https://www.vform666.com
 * date: 2021.08.18
 * remark: if need to VForm , in info! !
 */
-->

<template>
  <el-form
    ref="renderForm"
    :label-position="labelPosition"
    :size="size"
    :class="[customClass]"
    class="render-form"
    :label-width="labelWidth"
    :validate-on-rule-change="false"
    :model="formDataModel"
    @submit.prevent
  >
    <template v-for="(widget, index) in widgetList">
      <template v-if="'container' === widget.category">
        <component
          :is="getContainerWidgetName(widget)"
          :key="widget.id"
          :widget="widget"
          :field="widget"
          :parent-list="widgetList"
          :index-of-parent-list="index"
          :parent-widget="null"
        >
          <!-- ! ! ! -->
          <template v-for="slot in Object.keys($slots)" #[slot]="scope">
            <slot :name="slot" v-bind="scope" />
          </template>
        </component>
      </template>
      <template v-else>
        <component
          :is="getWidgetName(widget)"
          :key="widget.id"
          :field="widget"
          :form-model="formDataModel"
          :designer="null"
          :parent-list="widgetList"
          :index-of-parent-list="index"
          :parent-widget="null"
        >
          <!-- ! ! ! -->
          <template v-for="slot in Object.keys($slots)" #[slot]="scope">
            <slot :name="slot" v-bind="scope" />
          </template>
        </component>
      </template>
    </template>
  </el-form>
</template>

<script>
// import ElForm from 'element-ui/packages/form/src/form.vue' /* Element UI */
import emitter from '~@/utils/emitter'
import './container-item/index'
import FieldComponents from '~@/components/form-designer/form-widget/field-widget/index'
import {
  generateId, deepClone, insertCustomCssToHead, insertGlobalFunctionsToHtml, getAllContainerWidgets,
  getAllFieldWidgets, traverseFieldWidgets, buildDefaultFormJson
} from '~@/utils/util'
import i18n, { changeLocale } from '~@/utils/i18n'
import { ElForm } from 'element-plus'

export default {
  name: 'VFormRender',
  componentName: 'VFormRender',
  components: {
    ElForm,
    ...FieldComponents
  },
  mixins: [emitter, i18n],
  provide() {
    return {
      refList: this.widgetRefList,
      sfRefList: this.subFormRefList, // SubForm
      getFormConfig: () => this.formJsonObj.formConfig, /* 解决provide传递formConfig属性的响应式更新问题！！ */
      getGlobalDsv: () => this.globalDsv, // full data variable
      globalOptionData: this.optionData,
      getOptionData: () => this.optionData, /* 该方法用于在异步更新option-data之后重新获取到最新值 */
      globalModel: {
        formModel: this.formDataModel
      },
      previewState: this.previewState
    }
  },
  props: {
    formJson: { // prop formJSONconfiguration
      type: Object,
      default: () => buildDefaultFormJson()
    },
    formData: { // prop formdata
      type: Object,
      default: () => ({})
    },
    optionData: { // prop item data
      type: Object,
      default: () => ({})
    },
    previewState: { // whether form
      type: Boolean,
      default: false
    },
    globalDsv: { // full data variable
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      formJsonObj: this.formJson,

      formDataModel: {
        //
      },

      widgetRefList: {},
      subFormRefList: {},
      formId: null, // form Id, page v-form-rendercomponent! !

      externalComponents: {} // componentinstancecollection
    }
  },
  computed: {
    formConfig() {
      return this.formJsonObj.formConfig
    },

    widgetList() {
      return this.formJsonObj.widgetList
    },

    labelPosition() {
      if (!!this.formConfig && !!this.formConfig.labelPosition) {
        return this.formConfig.labelPosition
      }

      return 'left'
    },

    labelWidth() {
      if (!!this.formConfig && !!this.formConfig.labelWidth) {
        return this.formConfig.labelWidth + 'px'
      }

      return '140px'
    },

    size() {
      if (!!this.formConfig && !!this.formConfig.size) {
        return this.formConfig.size
      }

      return 'default'
    },

    customClass() {
      return !!this.formConfig && !!this.formConfig.customClass ? this.formConfig.customClass : ''
    }

  },
  watch: {
    //
  },
  created() {
    this.buildFormModel(!this.formJsonObj ? null : this.formJsonObj.widgetList)
    this.initFormObject()
  },
  mounted() {
    this.initLocale()
    this.handleOnMounted()
  },
  methods: {
    initFormObject(insertHtmlCodeFlag = true) {
      this.formId = 'vfRender' + generateId()
      if (!!insertHtmlCodeFlag) {
        this.insertCustomStyleAndScriptNode()
      }
      this.addFieldChangeEventHandler()
      this.addFieldValidateEventHandler()
      this.registerFormToRefList()
      this.handleOnCreated()
    },

    getContainerWidgetName(widget) {
      if (widget.type === 'grid') { // grid-item VueGridLayout full component , Process ! !
        return 'vf-grid-item'
      }
      return widget.type + '-item'
    },

    getWidgetName(widget) {
      return widget.type + '-widget'
    },

    initLocale() {
      let curLocale = localStorage.getItem('v_form_locale') || 'zh-CN'
      this.changeLanguage(curLocale)
    },

    insertCustomStyleAndScriptNode() {
      if (!!this.formConfig && !!this.formConfig.cssCode) {
        insertCustomCssToHead(this.formConfig.cssCode,
          !!this.previewState ? '' : this.formId)
      }

      if (!!this.formConfig && !!this.formConfig.functions) {
        insertGlobalFunctionsToHtml(this.formConfig.functions,
          !!this.previewState ? '' : this.formId)
      }
    },

    buildFormModel(widgetList) {
      if (!!widgetList && (widgetList.length > 0)) {
        widgetList.forEach((wItem) => {
          this.buildDataFromWidget(wItem)
        })
      }
    },

    buildDataFromWidget(wItem) {
      if (wItem.category === 'container') {
        if (wItem.type === 'grid') {
          if (!!wItem.cols && (wItem.cols.length > 0)) {
            wItem.cols.forEach((childItem) => {
              this.buildDataFromWidget(childItem)
            })
          }
        } else if (wItem.type === 'table') {
          if (!!wItem.rows && (wItem.rows.length > 0)) {
            wItem.rows.forEach((rowItem) => {
              if (!!rowItem.cols && (rowItem.cols.length > 0)) {
                rowItem.cols.forEach((colItem) => {
                  this.buildDataFromWidget(colItem)
                })
              }
            })
          }
        } else if (wItem.type === 'tab') {
          if (!!wItem.tabs && (wItem.tabs.length > 0)) {
            wItem.tabs.forEach((tabItem) => {
              if (!!tabItem.widgetList && (tabItem.widgetList.length > 0)) {
                tabItem.widgetList.forEach((childItem) => {
                  this.buildDataFromWidget(childItem)
                })
              }
            })
          }
        } else if (wItem.type === 'sub-form') {
          let subFormName = wItem.options.name
          // eslint-disable-next-line no-prototype-builtins
          if (!this.formData.hasOwnProperty(subFormName)) {
            let subFormDataRow = {}
            if (wItem.options.showBlankRow) {
              wItem.widgetList.forEach(subFormItem => {
                if (!!subFormItem.formItemFlag) {
                  subFormDataRow[subFormItem.options.name] = subFormItem.options.defaultValue
                }
              })

              this.formDataModel[subFormName] = [subFormDataRow]
            } else {
              this.formDataModel[subFormName] = []
            }
          } else {
            let initialValue = this.formData[subFormName]
            this.formDataModel[subFormName] = deepClone(initialValue)
          }
        } else if ((wItem.type === 'grid-col') || (wItem.type === 'table-cell')) {
          if (!!wItem.widgetList && (wItem.widgetList.length > 0)) {
            wItem.widgetList.forEach((childItem) => {
              this.buildDataFromWidget(childItem)
            })
          }
        } else if (wItem.type === 'items') { // component
          // wItem.items[0].widgetList.forEach((item) => {
          //   this.buildDataFromWidget(item)
          // })
        } else { // Custom component
          if (!!wItem.widgetList && (wItem.widgetList.length > 0)) {
            wItem.widgetList.forEach((childItem) => {
              this.buildDataFromWidget(childItem)
            })
          }
        }
      } else if (!!wItem.formItemFlag) {
        // eslint-disable-next-line no-prototype-builtins
        if (!this.formData.hasOwnProperty(wItem.options.name)) {
          this.formDataModel[wItem.options.name] = wItem.options.defaultValue
        } else {
          let initialValue = this.formData[wItem.options.name]
          this.formDataModel[wItem.options.name] = deepClone(initialValue)
        }
      }
    },

    addFieldChangeEventHandler() {
      this.off$('fieldChange') // event
      this.on$('fieldChange', (fieldName, newValue, oldValue, subFormName, subFormRowIndex) => {
        this.handleFieldDataChange(fieldName, newValue, oldValue, subFormName, subFormRowIndex)
        this.$emit('formChange', fieldName, newValue, oldValue, this.formDataModel, subFormName, subFormRowIndex)
      })
    },

    addFieldValidateEventHandler() {
      this.off$('fieldValidation') // event
      this.on$('fieldValidation', (fieldName) => {
        if (this.$refs.renderForm) {
          this.$refs.renderForm.validateField(fieldName)
          // Customcomponent ( component CustomValidate method )
          this.broadcast('ItemsItem', 'selfFieldValidation', fieldName)
        }
      })
    },

    registerFormToRefList() {
      this.widgetRefList['v_form_ref'] = this
    },

    handleFieldDataChange(fieldName, newValue, oldValue, subFormName, subFormRowIndex) {
      if (!!this.formConfig && !!this.formConfig.onFormDataChange) {
        let customFunc = new Function('fieldName', 'newValue', 'oldValue', 'formModel', 'subFormName', 'subFormRowIndex',
          this.formConfig.onFormDataChange)
        customFunc.call(this, fieldName, newValue, oldValue, this.formDataModel, subFormName, subFormRowIndex)
      }
    },

    handleOnCreated() {
      if (!!this.formConfig && !!this.formConfig.onFormCreated) {
        let customFunc = new Function(this.formConfig.onFormCreated)
        customFunc.call(this)
      }
    },

    handleOnMounted() {
      if (!!this.formConfig && !!this.formConfig.onFormMounted) {
        let customFunc = new Function(this.formConfig.onFormMounted)
        customFunc.call(this)
      }
    },

    findWidgetAndSetDisabled(widgetName, disabledFlag) {
      let foundW = this.getWidgetRef(widgetName)
      if (!!foundW) {
        foundW.setDisabled(disabledFlag)
      } else { // , can is sub form in component
        this.findWidgetOfSubFormAndSetDisabled(widgetName, disabledFlag)
      }
    },

    findWidgetOfSubFormAndSetDisabled(widgetName, disabledFlag) {
      this.findWidgetNameInSubForm(widgetName).forEach(wn => {
        let sw = this.getWidgetRef(wn)
        if (!!sw) {
          sw.setDisabled(disabledFlag)
        }
      })
    },

    findWidgetAndSetHidden(widgetName, hiddenFlag) {
      let foundW = this.getWidgetRef(widgetName)
      if (!!foundW) {
        foundW.setHidden(hiddenFlag)
      } else { // , can is sub form in component
        this.findWidgetOfSubFormAndSetHidden(widgetName, hiddenFlag)
      }
    },

    findWidgetOfSubFormAndSetHidden(widgetName, hiddenFlag) {
      this.findWidgetNameInSubForm(widgetName).forEach(wn => {
        let sw = this.getWidgetRef(wn)
        if (!!sw) {
          sw.setHidden(hiddenFlag)
        }
      })
    },

    findWidgetNameInSubForm(widgetName) {
      let result = []
      let subFormName = null
      let handlerFn = (field, parent) => {
        if (!!field.options && (field.options.name === widgetName)) {
          subFormName = parent.options.name
        }
      }
      traverseFieldWidgets(this.widgetList, handlerFn)

      if (!!subFormName) {
        let subFormRef = this.getWidgetRef(subFormName)
        if (!!subFormRef) {
          let rowIds = subFormRef.getRowIdData()
          if (!!rowIds && (rowIds.length > 0)) {
            rowIds.forEach(rid => {
              result.push(widgetName + '@row' + rid)
            })
          }
        }
      }

      return result
    },

    // --------------------- to component API method begin ------------------//
    /* prompt / tip: user method ! ! ! */

    changeLanguage(langName) {
      changeLocale(langName)
    },

    getNativeForm() { // Get form
      return this.$refs['renderForm']
    },

    getFormRef() {
      return this
    },

    getWidgetRef(widgetName, showError = false) {
      let foundRef = this.widgetRefList[widgetName]
      if (!foundRef && !!showError) {
        this.$message.error(this.i18nt('render.hint.refNotFound') + widgetName)
      }
      return foundRef
    },

    clearFormDataModel() {
      for (let pkey in this.formDataModel) {
        delete this.formDataModel[pkey]
      }
    },

    /**
       * Load formJSON
       * @param newFormJson
       */
    setFormJson(newFormJson) {
      if (!!newFormJson) {
        if ((typeof newFormJson === 'string') || (newFormJson.constructor === Object)) {
          let newFormJsonObj = null
          if (typeof newFormJson === 'string') {
            newFormJsonObj = JSON.parse(newFormJson)
          } else {
            newFormJsonObj = newFormJson
          }

          if (!newFormJsonObj.formConfig || !newFormJsonObj.widgetList) {
            this.$message.error('Invalid format of form json.')
            return
          }

          /* formDataModel in widgetList value Initialize , to widgetList value sub componentstart ! ! ! */
          // this.formDataModel = {} // null / empty formdataobject ( bug, will formValidate failed! ! )
          this.clearFormDataModel() // , will formValidate failed, object only null / empty objectproperty! !
          this.buildFormModel(newFormJsonObj.widgetList)

          this.formJsonObj['formConfig'] = newFormJsonObj.formConfig
          this.formJsonObj['widgetList'] = newFormJsonObj.widgetList

          this.insertCustomStyleAndScriptNode() /* 必须先插入表单全局函数，否则VForm内部引用全局函数会报错！！！ */
          this.$nextTick(() => {
            this.initFormObject(false)
            this.handleOnMounted()
          })
        } else {
          this.$message.error('Set form json failed.')
        }
      }
    },

    /**
       * new Load item data
       * @param widgetNames new Load component component array, new Load all item field
       */
    reloadOptionData(widgetNames) {
      let eventParams = []
      if (!!widgetNames && (typeof widgetNames === 'string')) {
        eventParams = [widgetNames]
      } else if (!!widgetNames && Array.isArray(widgetNames)) {
        eventParams = [...widgetNames]
      }
      this.broadcast('FieldWidget', 'reloadOptionItems', eventParams)
    },

    getFormData(needValidation = true) {
      if (!needValidation) {
        return this.formDataModel
      }

      let callback = function nullFunc() {}
      let promise = new window.Promise(function(resolve, reject) {
        callback = function(formData, error) {
          !error ? resolve(formData) : reject(error)
        }
      })

      let promiseArr = []
      Object.values(this.widgetRefList).forEach(item => {
        if (item.doValidate) {
          promiseArr.push(item.doValidate())
        }
      })

      Promise.all(promiseArr).then(results => {
        const allValid = results.every(result => result === true)
        this.$refs['renderForm'].validate((valid) => {
          if (valid && allValid) {
            callback(this.formDataModel)
          } else {
            callback(this.formDataModel, this.i18nt('render.hint.validationFailed'))
          }
        })
      })

      return promise
    },

    setFormData(formData) { // Set formdata
      Object.keys(this.formDataModel).forEach(propName => {
        // eslint-disable-next-line no-prototype-builtins
        if (!!formData && formData.hasOwnProperty(propName)) {
          this.formDataModel[propName] = deepClone(formData[propName])
        }
      })

      // notificationSubFormcomponent: formdata new event! !
      this.broadcast('ContainerItem', 'setFormData', this.formDataModel)
      // notificationFieldWidgetcomponent: formdata new event! !
      this.broadcast('FieldWidget', 'setFormData', this.formDataModel)
    },

    getFieldValue(fieldName) { // fieldGet value
      let fieldRef = this.getWidgetRef(fieldName)
      if (!!fieldRef && !!fieldRef.getValue) {
        return fieldRef.getValue()
      }

      if (!fieldRef) { // if is sub formfield
        let result = []
        this.findWidgetNameInSubForm(fieldName).forEach(wn => {
          let sw = this.getWidgetRef(wn)
          if (!!sw && !!sw.getValue) {
            result.push(sw.getValue())
          }
        })

        return result
      }
    },

    setFieldValue(fieldName, fieldValue) { // new field value
      let fieldRef = this.getWidgetRef(fieldName)
      if (!!fieldRef && !!fieldRef.setValue) {
        fieldRef.setValue(fieldValue)
      }

      if (!fieldRef) { // if is sub formfield
        this.findWidgetNameInSubForm(fieldName).forEach(wn => {
          let sw = this.getWidgetRef(wn)
          if (!!sw && !!sw.setValue) {
            sw.setValue(fieldValue)
          }
        })
      }
    },

    getSubFormValues(subFormName, needValidation = true) {
      let foundSFRef = this.subFormRefList[subFormName]
      // if (!foundSFRef) {
      //   return this.formDataModel[subFormName]
      // }
      return foundSFRef.getSubFormValues(needValidation)
    },

    // Set form
    setFormItemAuth(data) {
      data && data.forEach(item => {
        let foundW = this.getWidgetRef(item.id)
        if (!!foundW) {
          if (!!foundW.setDisabled) {
            foundW.setDisabled(item.readonly)
          }
          if (!!foundW.setRequired) {
            foundW.setRequired(item.required)
          }
          if (!!foundW.setHidden) {
            foundW.setHidden(item.hidden)
          }
        }
      })
    },

    disableForm() {
      let wNameList = Object.keys(this.widgetRefList)
      wNameList.forEach(wName => {
        let foundW = this.getWidgetRef(wName)
        if (!!foundW) {
          if (!!foundW.widget && (foundW.widget.type === 'sub-form')) {
            foundW.disableSubForm()
          } else {
            //! !foundW.setDisabled && foundW.setDisabled(true)
            if (!!foundW.setDisabled) {
              foundW.setDisabled(true)
            }
          }
        }
      })
    },

    enableForm() {
      let wNameList = Object.keys(this.widgetRefList)
      wNameList.forEach(wName => {
        let foundW = this.getWidgetRef(wName)
        if (!!foundW) {
          if (!!foundW.widget && (foundW.widget.type === 'sub-form')) {
            foundW.enableSubForm()
          } else {
            //! !foundW.setDisabled && foundW.setDisabled(false)
            if (!!foundW.setDisabled) {
              foundW.setDisabled(false)
            }
          }
        }
      })
    },

    resetForm() { // form
      let subFormNames = Object.keys(this.subFormRefList)
      subFormNames.forEach(sfName => {
        if (!!this.subFormRefList[sfName].resetSubForm) {
          this.subFormRefList[sfName].resetSubForm()
        }
      })

      let wNameList = Object.keys(this.widgetRefList)
      wNameList.forEach(wName => {
        let foundW = this.getWidgetRef(wName)
        if (!!foundW && !foundW.subFormItemFlag && !!foundW.resetField) { // sub formfield! !
          foundW.resetField()
        }
      })

      this.$nextTick(() => {
        this.clearValidate() /* 清除resetField方法触发的校验错误提示 */
      })
    },

    clearValidate(props) {
      this.$refs.renderForm.clearValidate(props)
    },

    /**
       * Validate form
       * @param callback
       */
    validateForm(callback) {
      this.$refs['renderForm'].validate((valid) => {
        callback(valid)
      })
    },

    validateFields() {
      //
    },

    disableWidgets(widgetNames) {
      if (!!widgetNames) {
        if (typeof widgetNames === 'string') {
          this.findWidgetAndSetDisabled(widgetNames, true)
        } else if (Array.isArray(widgetNames)) {
          widgetNames.forEach(wn => {
            this.findWidgetAndSetDisabled(wn, true)
          })
        }
      }
    },

    enableWidgets(widgetNames) {
      if (!!widgetNames) {
        if (typeof widgetNames === 'string') {
          this.findWidgetAndSetDisabled(widgetNames, false)
        } else if (Array.isArray(widgetNames)) {
          widgetNames.forEach(wn => {
            this.findWidgetAndSetDisabled(wn, false)
          })
        }
      }
    },

    hideWidgets(widgetNames) {
      if (!!widgetNames) {
        if (typeof widgetNames === 'string') {
          this.findWidgetAndSetHidden(widgetNames, true)
        } else if (Array.isArray(widgetNames)) {
          widgetNames.forEach(wn => {
            this.findWidgetAndSetHidden(wn, true)
          })
        }
      }
    },

    showWidgets(widgetNames) {
      if (!!widgetNames) {
        if (typeof widgetNames === 'string') {
          this.findWidgetAndSetHidden(widgetNames, false)
        } else if (Array.isArray(widgetNames)) {
          widgetNames.forEach(wn => {
            this.findWidgetAndSetHidden(wn, false)
          })
        }
      }
    },

    /**
       * Get all fieldcomponent
       * @returns {*[]}
       */
    getFieldWidgets() {
      return getAllFieldWidgets(this.formJsonObj.widgetList)
    },

    /**
       * Get all component
       * @returns {*[]}
       */
    getContainerWidgets() {
      return getAllContainerWidgets(this.formJsonObj.widgetList)
    },

    /**
       * component , getEC() method Get component, in VForm component method
       * @param componentName component
       * @param externalComponent componentinstance
       */
    addEC(componentName, externalComponent) {
      this.externalComponents[componentName] = externalComponent
    },

    /**
       * Check componentwhether Get
       * @param componentName component
       * @returns {boolean}
       */
    hasEC(componentName) {
      // eslint-disable-next-line no-prototype-builtins
      return this.externalComponents.hasOwnProperty(componentName)
    },

    /**
       * Get componentinstance
       * @param componentName
       * @returns {*}
       */
    getEC(componentName) {
      return this.externalComponents[componentName]
    },

    /**
       * Get globalDsvobject
       * @returns {*}
       */
    getGlobalDsv() {
      return this.globalDsv
    }

    // --------------------- to component API method end ------------------//

  }
}
</script>

<style lang="scss" scoped>
  .el-form :deep(.el-row) {
    padding: 8px;
  }
</style>
