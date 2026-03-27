<!-- eslint-disable vue/no-mutating-props -->
<!--
/**
 * author: vformAdmin
 * email: vdpadmin@163.com
 * website: https://www.vform666.com
 * date: 2021.08.18
 * remark: 如果要分发VForm源码，需在本文件顶部保留此文件头信息！！
 */
-->

<template>
  <van-form
    ref="renderForm"
    :label-position="labelPosition"
    :class="[customClass, readModeFlag ? 'readonly-mode-form' : '']"
    class="render-form"
    :label-width="labelWidth"
    :submit-on-enter="false"
    :model="formDataModel"
    :style="formStyle"
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
          <!-- 递归传递插槽！！！ -->
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
          <!-- 递归传递插槽！！！ -->
          <template v-for="slot in Object.keys($slots)" #[slot]="scope">
            <slot :name="slot" v-bind="scope" />
          </template>
        </component>
      </template>
    </template>
  </van-form>
</template>

<script>
import { createVNode, computed, render, provide, reactive, ref, toRefs, getCurrentInstance, onMounted, nextTick } from 'vue'
// import ElForm from 'element-ui/packages/form/src/form.vue'  /* 用于源码调试Element UI */
import { useEmitter } from '~@/utils/emitter'
import { useI18n, changeLocale } from '~@/utils/i18n'

import './container-item/index'
import FieldComponents from '~@/components/form-designer/form-widget/field-widget/index.js'
import {
  generateId,
  deepClone,
  insertCustomCssToHead,
  insertGlobalFunctionsToHtml,
  getAllContainerWidgets,
  getAllFieldWidgets,
  // traverseFieldWidgets,
  buildDefaultFormJson,
  getDSByName,
  runDataSourceRequest,
  getFieldWidgetByName,
  overwriteObj,
  getDefaultFormConfig,
  getContainerWidgetByName,
  traverseAllWidgets,
  traverseFieldWidgetsOfContainer,
  cloneFormConfigWithoutEventHandler
} from '~@/utils/util'

export default {
  componentName: 'VmFormRender',
  name: 'VmFormRender',
  components: {
    // ElForm,

    ...FieldComponents
  },
  props: {
    disabledMode: {
      default: false,
      // 表单禁止编辑模式
      type: Boolean
    },
    dynamicCreation: {
      default: false,
      // 是否弹窗、抽屉动态创建的VmFormRender
      type: Boolean
    },
    formData: {
      default: () => ({}),
      // prop传入的表单数据
      type: Object
    },
    formJson: {
      default: () => buildDefaultFormJson(),
      // prop传入的表单JSON配置
      type: Object
    },
    globalDsv: {
      default: () => ({}),
      // 全局数据源变量
      type: Object
    },
    optionData: {
      default: () => ({}),
      // prop传入的选项数据
      type: Object
    },
    parentForm: {
      default: null,
      type: Object
    },
    previewState: {
      default: false,
      // 是否表单预览状态
      type: Boolean
    },
    renderConfig: {
      default: () => {
        return {
          languageName: 'zh-CN' // 界面语言，默认显示中文
        }
      },
      // 渲染配置对象
      type: Object
    }
  },
  setup(props) {
    const { i18nt } = useI18n()
    const { proxy } = getCurrentInstance()

    const renderForm = ref(null)

    const getDefaultFormJson = () => {
      // 判断props.formJson为空对象
      if (!props.formJson || Object.keys(props.formJson).length === 0) {
        return buildDefaultFormJson()
      } else if (!props.formJson.formConfig) {
        // eslint-disable-next-line vue/no-mutating-props
        props.formJson.formConfig = buildDefaultFormJson().formConfig
      } else if (!props.formJson.widgetList) {
        // eslint-disable-next-line vue/no-mutating-props
        props.formJson.widgetList = []
      }
      return props.formJson
    }

    const data = reactive({
      childFormRef: null, // 保存子级VmFormRender组件的ref

      dialogOrDrawerRef: null, // 保存子级VmFormRender的包裹弹窗组件或抽屉组件的ref

      dsResultCache: {}, // 数据源请求结果缓存
      externalComponents: {}, // 外部组件实例集合
      formDataModel: {
        //
      },

      formId: null, // 表单唯一Id，用于区分页面上的多个v-form-render组件！！
      formJsonObj: getDefaultFormJson(),
      readModeFlag: false, // 是否只读查看模式
      subFormRefList: {},

      widgetRefList: {}
    })
    const emitterMixin = useEmitter(data)

    provide('refList', data.widgetRefList)
    provide('sfRefList', data.subFormRefList) // 收集SubForm引用
    provide('getFormConfig', () => data.formJsonObj.formConfig) /* 解决provide传递formConfig属性的响应式更新问题！！ */
    provide('getGlobalDsv', () => props.globalDsv) // 全局数据源变量
    provide('globalOptionData', props.optionData)
    provide('getOptionData', () => props.optionData) /* 该方法用于在异步更新option-data之后重新获取到最新值 */
    provide('globalModel', {
      formModel: data.formDataModel
    })
    provide('previewState', props.previewState)
    provide('getReadMode', () => data.readModeFlag)
    provide('getSubFormFieldFlag', () => false)
    provide('getSubFormName', () => '')
    provide('getDSResultCache', () => data.dsResultCache)

    const formConfig = computed(() => {
      return data.formJsonObj.formConfig
    })

    const widgetList = computed(() => {
      return data.formJsonObj.widgetList
    })

    const labelPosition = computed(() => {
      if (!!formConfig.value && !!formConfig.value.labelPosition) {
        return formConfig.value.labelPosition
      }

      return 'left'
    })

    const labelWidth = computed(() => {
      if (!!formConfig.value && !!formConfig.value.labelWidth) {
        return formConfig.value.labelWidth + 'px'
      }

      return '80px'
    })

    const size = computed(() => {
      if (!!formConfig.value && !!formConfig.value.size) {
        return formConfig.value.size
      }

      return 'default'
    })

    const formStyle = computed(() => {
      return {
        backgroundColor: formConfig.value.background || '#f9f9f9',
        padding: (formConfig.value.padding || 0) + 'px'
      }
    })

    const formFieldRadius = computed(() => {
      return (formConfig.value.radius || 0) + 'px'
    })

    const customClass = computed(() => {
      return !!formConfig.value && !!formConfig.value.customClass ? formConfig.value.customClass : ''
    })

    onMounted(() => {
      initLocale()
      initDataSetRequest()
      handleOnMounted()
    })

    const initFormObject = (insertHtmlCodeFlag = true) => {
      data.formId = 'vfRender' + generateId()
      if (!!insertHtmlCodeFlag && !props.dynamicCreation) {
        // 弹窗、抽屉动态创建的VmFormRender不重新插入全局CSS和全局函数节点！！
        insertCustomStyleAndScriptNode()
      }
      addFieldChangeEventHandler()
      addFieldValidateEventHandler()
      registerFormToRefList()
      handleOnCreated()

      if (!!props.disabledMode) {
        // 禁止表单编辑
        nextTick(() => {
          disableForm()
        })
      }
    }

    const getContainerWidgetName = widget => {
      if (widget.type === 'grid') {
        // grid-item跟VueGridLayout全局注册组件重名，故特殊处理！！
        return 'vf-grid-item'
      }

      return widget.type + '-item'
    }

    const getWidgetName = widget => {
      return widget.type + '-widget'
    }

    const initLocale = () => {
      let curLocale = localStorage.getItem('vm_form_locale') || 'zh-CN'
      changeLanguage(curLocale)
    }

    const insertCustomStyleAndScriptNode = () => {
      if (!!formConfig.value && !!formConfig.value.cssCode) {
        insertCustomCssToHead(formConfig.value.cssCode, !!props.previewState ? '' : data.formId)
      }

      if (!!formConfig.value && !!formConfig.value.functions) {
        insertGlobalFunctionsToHtml(formConfig.value.functions, !!props.previewState ? '' : data.formId)
      }
    }

    const buildFormModel = widgetList => {
      if (!!widgetList && widgetList.length > 0) {
        widgetList.forEach(wItem => {
          buildDataFromWidget(wItem)
        })
      }
    }

    const buildDataFromWidget = wItem => {
      if (wItem.category === 'container') {
        if (wItem.type === 'm-grid') {
          if (!!wItem.cols && wItem.cols.length > 0) {
            wItem.cols.forEach(childItem => {
              buildDataFromWidget(childItem)
            })
          }
        } else if (wItem.type === 'table') {
          if (!!wItem.rows && wItem.rows.length > 0) {
            wItem.rows.forEach(rowItem => {
              if (!!rowItem.cols && rowItem.cols.length > 0) {
                rowItem.cols.forEach(colItem => {
                  buildDataFromWidget(colItem)
                })
              }
            })
          }
        } else if (wItem.type === 'm-tab') {
          if (!!wItem.tabs && wItem.tabs.length > 0) {
            wItem.tabs.forEach(tabItem => {
              if (!!tabItem.widgetList && tabItem.widgetList.length > 0) {
                tabItem.widgetList.forEach(childItem => {
                  buildDataFromWidget(childItem)
                })
              }
            })
          }
        } else if (wItem.type === 'm-sub-form') {
          let gridSubFormName = wItem.options.name
          // eslint-disable-next-line no-prototype-builtins
          if (!props.formData.hasOwnProperty(gridSubFormName)) {
            let gsfFWList = []
            let fieldListFn = fw => {
              gsfFWList.push(fw)
            }
            traverseFieldWidgetsOfContainer(wItem, fieldListFn)

            let gridSubFormDataRow = {}
            if (wItem.options.showBlankRow) {
              gsfFWList.forEach(gridSubFormItem => {
                gridSubFormDataRow[gridSubFormItem.options.name] = gridSubFormItem.options.defaultValue
              })
              data.formDataModel[gridSubFormName] = [gridSubFormDataRow]
            } else {
              data.formDataModel[gridSubFormName] = []
            }
          } else {
            let initialValue = data.formData[gridSubFormName]
            data.formDataModel[gridSubFormName] = deepClone(initialValue)
          }
        } else if (wItem.type === 'm-grid-col' || wItem.type === 'table-cell') {
          if (!!wItem.widgetList && wItem.widgetList.length > 0) {
            wItem.widgetList.forEach(childItem => {
              buildDataFromWidget(childItem)
            })
          }
        } else {
          // 自定义容器组件
          if (!!wItem.widgetList && wItem.widgetList.length > 0) {
            wItem.widgetList.forEach(childItem => {
              buildDataFromWidget(childItem)
            })
          }
        }
      } else if (!!wItem.formItemFlag) {
        // eslint-disable-next-line no-prototype-builtins
        if (!props.formData.hasOwnProperty(wItem.options.name)) {
          data.formDataModel[wItem.options.name] = deepClone(wItem.options.defaultValue) // 设置字段默认值
        } else {
          let initialValue = props.formData[wItem.options.name]
          data.formDataModel[wItem.options.name] = deepClone(initialValue)
        }
      }
    }

    const addFieldChangeEventHandler = () => {
      emitterMixin.off$('fieldChange') // 移除原有事件监听
      emitterMixin.on$('fieldChange', ([fieldName, newValue, oldValue, subFormName, subFormRowIndex]) => {
        handleFieldDataChange(fieldName, newValue, oldValue, subFormName, subFormRowIndex)
        proxy.$emit('formChange', fieldName, newValue, oldValue, data.formDataModel, subFormName, subFormRowIndex)
      })
    }

    const addFieldValidateEventHandler = () => {
      emitterMixin.off$('fieldValidation') // 移除原有事件监听
      emitterMixin.on$('fieldValidation', fieldName => {
        if (!!renderForm.value) {
          renderForm.value.validate(fieldName).catch(() => {})
        }
      })
    }

    const registerFormToRefList = () => {
      data.widgetRefList['v_form_ref'] = proxy
    }

    const handleFieldDataChange = (fieldName, newValue, oldValue, subFormName, subFormRowIndex) => {
      if (!!formConfig.value && !!formConfig.value.onFormDataChange) {
        let customFunc = new Function('fieldName', 'newValue', 'oldValue', 'formModel', 'subFormName', 'subFormRowIndex', formConfig.value.onFormDataChange)
        customFunc.call(proxy, fieldName, newValue, oldValue, data.formDataModel, subFormName, subFormRowIndex)
      }
    }

    const handleOnCreated = () => {
      if (!!formConfig.value && !!formConfig.value.onFormCreated) {
        let customFunc = new Function(formConfig.value.onFormCreated)
        customFunc.call(proxy)
      }
    }

    const handleOnMounted = () => {
      if (!!formConfig.value && !!formConfig.value.onFormMounted) {
        let customFunc = new Function(formConfig.value.onFormMounted)
        customFunc.call(proxy)
      }
    }

    const findWidgetAndSetDisabled = (widgetName, disabledFlag) => {
      let foundW = getWidgetRef(widgetName)
      if (!!foundW && !!foundW.setDisabled) {
        foundW.setDisabled(disabledFlag)
      } else {
        // 没找到，可能是子表单中的组件
        findWidgetOfSubFormAndSetDisabled(widgetName, disabledFlag)
      }
    }

    const findWidgetOfSubFormAndSetDisabled = (widgetName, disabledFlag) => {
      const widgetSchema = getFieldWidgetByName(data.formJsonObj.widgetList, widgetName, true)
      // eslint-disable-next-line no-prototype-builtins
      if (!!widgetSchema && !!widgetSchema.options && widgetSchema.options.hasOwnProperty('disabled')) {
        widgetSchema.options.disabled = disabledFlag
      }

      findWidgetNameInSubForm(widgetName).forEach(wn => {
        let sw = getWidgetRef(wn)
        if (!!sw && !!sw.setDisabled) {
          sw.setDisabled(disabledFlag)
        }
      })
    }

    const findWidgetAndSetRequired = (widgetName, required) => {
      let foundW = getWidgetRef(widgetName)
      if (!!foundW && !!foundW.setRequired) {
        foundW.setRequired(required)
      } else {
        // 没找到，可能是子表单中的组件
        findWidgetOfSubFormAndSetRequired(widgetName, required)
      }
    }

    const findWidgetAndSetHidden = (widgetName, hiddenFlag) => {
      let foundW = getWidgetRef(widgetName)
      if (!!foundW && !!foundW.setDisabled) {
        foundW.setHidden(hiddenFlag)
      } else {
        // 没找到，可能是子表单中的组件
        findWidgetOfSubFormAndSetHidden(widgetName, hiddenFlag)
      }
    }

    const findWidgetOfSubFormAndSetRequired = (widgetName, required) => {
      const widgetSchema = getFieldWidgetByName(data.formJsonObj.widgetList, widgetName, true)
      // eslint-disable-next-line no-prototype-builtins
      if (!!widgetSchema && !!widgetSchema.options && widgetSchema.options.hasOwnProperty('required')) {
        widgetSchema.options.required = required
      }

      findWidgetNameInSubForm(widgetName).forEach(wn => {
        let sw = getWidgetRef(wn)
        if (!!sw && !!sw.setRequired) {
          sw.setRequired(required)
        }
      })
    }

    const findWidgetOfSubFormAndSetHidden = (widgetName, hiddenFlag) => {
      const widgetSchema = getFieldWidgetByName(data.formJsonObj.widgetList, widgetName, true)
      // eslint-disable-next-line no-prototype-builtins
      if (!!widgetSchema && !!widgetSchema.options && widgetSchema.options.hasOwnProperty('hidden')) {
        widgetSchema.options.hidden = hiddenFlag
      }

      findWidgetNameInSubForm(widgetName).forEach(wn => {
        let sw = getWidgetRef(wn)
        if (!!sw && !!sw.setDisabled) {
          sw.setHidden(hiddenFlag)
        }
      })
    }

    const findWidgetNameInSubForm = widgetName => {
      let result = []
      let subFormName = getSubFormNameOfWidget(widgetName)

      if (!!subFormName) {
        let subFormRef = getWidgetRef(subFormName)
        if (!!subFormRef) {
          let rowIds = subFormRef.getRowIdData()
          if (!!rowIds && rowIds.length > 0) {
            rowIds.forEach(rid => {
              result.push(widgetName + '@row' + rid)
            })
          }
        }
      }

      return result
    }

    const getSubFormNameOfWidget = widgetName => {
      let subFormName = null
      Object.keys(data.subFormRefList).forEach(sfName => {
        const fwHandler = fw => {
          if (fw.options.name === widgetName) {
            subFormName = sfName
          }
        }

        const sfRef = data.subFormRefList[sfName]
        traverseFieldWidgetsOfContainer(sfRef.widget, fwHandler)
      })

      return subFormName
    }

    const initDataSetRequest = () => {
      let dsNameSet = new Set()
      traverseAllWidgets(widgetList.value, fw => {
        if (!!fw.options.dsEnabled && !!fw.options.dsName && !!fw.options.dataSetName) {
          dsNameSet.add(fw.options.dsName)
        }
      })

      if (dsNameSet.size > 0) {
        dsNameSet.forEach(async dsName => {
          let curDS = getDSByName(formConfig.value, dsName)
          if (!!curDS) {
            // eslint-disable-next-line no-new-object
            let localDsv = new Object({})
            overwriteObj(localDsv, props.globalDsv || {})
            let dsResult = null
            try {
              dsResult = await runDataSourceRequest(curDS, localDsv, proxy, false, proxy.$message)
              data.dsResultCache[dsName] = dsResult
              emitterMixin.broadcast('FieldWidget', 'loadOptionItemsFromDataSet', dsName) // 注意：跟Vue2不同，事件参数不需要包含在数组中传递！！
            } catch (err) {
              proxy.$message.error(err.message)
            }
          }
        })
      }
    }

    // --------------------- 以下为组件支持外部调用的API方法 begin ------------------//
    /* 提示：用户可自行扩充这些方法！！！ */

    const changeLanguage = langName => {
      changeLocale(langName)
    }

    const getLanguageName = () => {
      return localStorage.getItem('vm_form_locale') || 'zh-CN'
    }

    const getNativeForm = () => {
      // 获取原生form引用
      return renderForm
    }

    const getFormRef = () => {
      return proxy
    }

    const getWidgetRef = (widgetName, showError = false) => {
      let foundRef = data.widgetRefList[widgetName]
      if (!foundRef && !!showError) {
        proxy.$message.error(i18nt('render.hint.refNotFound') + widgetName)
      }
      return foundRef
    }

    const clearFormDataModel = () => {
      for (let pkey in data.formDataModel) {
        delete data.formDataModel[pkey]
      }
    }

    const getFormJson = () => {
      return data.formJsonObj
    }

    /**
     * 动态加载表单JSON
     * @param newFormJson
     */
    const setFormJson = newFormJson => {
      if (!!newFormJson) {
        if (typeof newFormJson === 'string' || newFormJson.constructor === Object) {
          let newFormJsonObj = null
          if (typeof newFormJson === 'string') {
            newFormJsonObj = JSON.parse(newFormJson)
          } else {
            newFormJsonObj = newFormJson
          }

          if (!newFormJsonObj.formConfig || !newFormJsonObj.widgetList) {
            proxy.$message.error('Invalid format of form json.')
            return
          }

          /* formDataModel必须在widgetList赋值完成初始化，因为widgetList赋值意味着子组件开始创建！！！ */
          // data.formDataModel = {}  //清空表单数据对象（有bug，会导致表单校验失败！！）
          clearFormDataModel() // 上行代码有问题，会导致表单校验失败，故保留原对象引用只清空对象属性！！
          buildFormModel(newFormJsonObj.widgetList)

          data.formJsonObj['formConfig'] = newFormJsonObj.formConfig
          data.formJsonObj['widgetList'] = newFormJsonObj.widgetList

          insertCustomStyleAndScriptNode() /* 必须先插入表单全局函数，否则VForm内部引用全局函数会报错！！！ */
          nextTick(() => {
            initFormObject(false)
            initDataSetRequest()
            handleOnMounted()
          })
        } else {
          proxy.$message.error('Set form json failed.')
        }
      }
    }

    /**
     * 重新加载选项数据
     * @param widgetNames 指定重新加载的组件名称或组件名数组，不传则重新加载所有选项字段
     */
    const reloadOptionData = widgetNames => {
      let eventParams = []
      if (!!widgetNames && typeof widgetNames === 'string') {
        eventParams = [widgetNames]
      } else if (!!widgetNames && Array.isArray(widgetNames)) {
        eventParams = [...widgetNames]
      }
      emitterMixin.broadcast('FieldWidget', 'reloadOptionItems', eventParams)
    }

    const getFormData = (needValidation = true) => {
      if (!needValidation) {
        return data.formDataModel
      }

      let callback = function nullFunc() {}
      let promise = new window.Promise(function(resolve, reject) {
        callback = function(formData, error) {
          !error ? resolve(formData) : reject(error)
        }
      })

      renderForm.value
        .validate()
        .then(async() => {
          let customValid = await doCustomValidation()
          if (customValid === true) {
            callback(data.formDataModel)
          } else {
            callback(data.formDataModel, customValid === false ? i18nt('render.hint.validationFailed') : customValid)
          }
        })
        .catch(() => {
          callback(data.formDataModel, i18nt('render.hint.validationFailed'))
        })

      return promise
    }

    const setFormData = formData => {
      // 设置表单数据
      Object.keys(data.formDataModel).forEach(propName => {
        // eslint-disable-next-line no-prototype-builtins
        if (!!formData && formData.hasOwnProperty(propName)) {
          data.formDataModel[propName] = deepClone(formData[propName])
        }
      })
      // 通知SubForm组件：表单数据更新事件！！
      emitterMixin.broadcast('ContainerItem', 'setFormData', data.formDataModel)
      // 通知FieldWidget组件：表单数据更新事件！！
      emitterMixin.broadcast('FieldWidget', 'setFormData', data.formDataModel)
    }

    const getFieldValue = fieldName => {
      // 单个字段获取值
      let fieldRef = getWidgetRef(fieldName)
      if (!!fieldRef && !!fieldRef.getValue) {
        return fieldRef.getValue()
      }

      if (!fieldRef) {
        // 如果是子表单字段
        let result = []
        findWidgetNameInSubForm(fieldName).forEach(wn => {
          let sw = getWidgetRef(wn)
          if (!!sw && !!sw.getValue) {
            result.push(sw.getValue())
          }
        })

        return result
      }
    }

    const setFieldValue = (fieldName, fieldValue) => {
      // 单个更新字段值
      let fieldRef = getWidgetRef(fieldName)
      if (!!fieldRef && !!fieldRef.setValue) {
        fieldRef.setValue(fieldValue)
      }

      if (!fieldRef) {
        // 如果是子表单字段
        findWidgetNameInSubForm(fieldName).forEach(wn => {
          let sw = getWidgetRef(wn)
          if (!!sw && !!sw.setValue) {
            sw.setValue(fieldValue)
          }
        })
      }
    }

    const getSubFormValues = (subFormName, needValidation = true) => {
      let foundSFRef = data.subFormRefList[subFormName]
      return foundSFRef.getSubFormValues(needValidation)
    }

    const setSubFormValues = (subFormName, subFormValues) => {
      let foundSFRef = data.subFormRefList[subFormName]
      return foundSFRef.setSubFormValues(subFormValues)
    }

    const disableForm = () => {
      let wNameList = Object.keys(data.widgetRefList)
      wNameList.forEach(wName => {
        let foundW = getWidgetRef(wName)
        if (!!foundW) {
          if (!!foundW.widget && foundW.widget.type === 'm-sub-form') {
            foundW.disableSubForm()
          } else {
            !!foundW.setDisabled && foundW.setDisabled(true)
          }
        }
      })
    }

    const enableForm = () => {
      let wNameList = Object.keys(data.widgetRefList)
      wNameList.forEach(wName => {
        let foundW = getWidgetRef(wName)
        if (!!foundW) {
          if (!!foundW.widget && foundW.widget.type === 'm-sub-form') {
            foundW.enableSubForm()
          } else {
            !!foundW.setDisabled && foundW.setDisabled(false)
          }
        }
      })
    }

    const resetForm = () => {
      // 重置表单
      let subFormNames = Object.keys(data.subFormRefList)
      subFormNames.forEach(sfName => {
        if (!!data.subFormRefList[sfName].resetSubForm) {
          data.subFormRefList[sfName].resetSubForm()
        }
      })

      let wNameList = Object.keys(data.widgetRefList)
      wNameList.forEach(wName => {
        let foundW = getWidgetRef(wName)
        if (!!foundW && !foundW.subFormItemFlag && !!foundW.resetField) {
          // 跳过子表单字段！！
          foundW.resetField()
        }
      })

      nextTick(() => {
        clearValidate() /* 清除resetField方法触发的校验错误提示 */
      })
    }

    const clearValidate = props => {
      renderForm.value.resetValidation(props)
    }

    /**
     * 清空表单所有组件（包含全局函数、自定义CSS代码）
     */
    const setBlankFormJson = () => {
      let blankFormJson = {
        formConfig: getDefaultFormConfig(),
        widgetList: []
      }
      setFormJson(blankFormJson)
    }

    /**
     * 校验表单
     * @param callback 回调函数
     */
    const validateForm = callback => {
      renderForm.value
        .validate()
        .then(async() => {
          let customValid = await doCustomValidation()
          callback(customValid)
        })
        .catch(() => {
          callback(false)
        })
    }

    /**
     * 执行表单自定义校验代码
     */
    const doCustomValidation = async() => {
      if (!formConfig.value.onFormValidate) {
        return true
      }

      const AsyncFunction = async function() {}.constructor
      let customFn = new AsyncFunction('formModel', formConfig.value.onFormValidate)
      let result = await customFn.call(proxy, data.formDataModel)
      return result === undefined ? true : result
    }

    const validateFields = () => {
      // TODO
    }

    const disableWidgets = widgetNames => {
      if (!!widgetNames) {
        if (typeof widgetNames === 'string') {
          findWidgetAndSetDisabled(widgetNames, true)
        } else if (Array.isArray(widgetNames)) {
          widgetNames.forEach(wn => {
            findWidgetAndSetDisabled(wn, true)
          })
        }
      }
    }

    const enableWidgets = widgetNames => {
      if (!!widgetNames) {
        if (typeof widgetNames === 'string') {
          findWidgetAndSetDisabled(widgetNames, false)
        } else if (Array.isArray(widgetNames)) {
          widgetNames.forEach(wn => {
            findWidgetAndSetDisabled(wn, false)
          })
        }
      }
    }

    const hideWidgets = widgetNames => {
      if (!!widgetNames) {
        if (typeof widgetNames === 'string') {
          findWidgetAndSetHidden(widgetNames, true)
        } else if (Array.isArray(widgetNames)) {
          widgetNames.forEach(wn => {
            findWidgetAndSetHidden(wn, true)
          })
        }
      }
    }

    const showWidgets = widgetNames => {
      if (!!widgetNames) {
        if (typeof widgetNames === 'string') {
          findWidgetAndSetHidden(widgetNames, false)
        } else if (Array.isArray(widgetNames)) {
          widgetNames.forEach(wn => {
            findWidgetAndSetHidden(wn, false)
          })
        }
      }
    }

    /**
     * 获取所有字段组件
     * @param staticWidgetsIncluded 是否包含按钮等静态组件，默认不包含
     * @returns {*[]}
     */
    const getFieldWidgets = (staticWidgetsIncluded = false) => {
      return getAllFieldWidgets(data.formJsonObj.widgetList, staticWidgetsIncluded)
    }

    /**
     * 获取所有容器组件
     * @returns {*[]}
     */
    const getContainerWidgets = () => {
      return getAllContainerWidgets(data.formJsonObj.widgetList)
    }

    /**
     * 增加外部组件引用，可通过getEC()方法获取外部组件，以便在VForm内部调用外部组件方法
     * @param componentName 外部组件名称
     * @param externalComponent 外部组件实例
     */
    const addEC = (componentName, externalComponent) => {
      data.externalComponents[componentName] = externalComponent
    }

    /**
     * 判断外部组件是否可获取
     * @param componentName 外部组件名称
     * @returns {boolean}
     */
    const hasEC = componentName => {
      // eslint-disable-next-line no-prototype-builtins
      return data.externalComponents.hasOwnProperty(componentName)
    }

    /**
     * 获取外部组件实例
     * @param componentName
     * @returns {*}
     */
    const getEC = componentName => {
      return data.externalComponents[componentName]
    }

    /**
     * 设置或取消设置表单为只读查看模式
     * @param readonlyFlag
     */
    const setReadMode = (readonlyFlag = true) => {
      data.readModeFlag = readonlyFlag
    }

    /**
     * 获取表单当前是否只读查看模式
     * @returns {boolean}
     */
    const getReadMode = () => {
      return data.readModeFlag
    }

    /**
     * 获取globalDsv对象
     * @returns {*}
     */
    const getGlobalDsv = () => {
      return props.globalDsv
    }

    /**
     * 执行数据源请求
     * @param dsName
     * @param localDsv
     */
    const executeDataSource = async(dsName, localDsv = {}) => {
      let ds = getDSByName(data.formJsonObj.formConfig, dsName)
      // eslint-disable-next-line no-new-object
      let newDsv = new Object({})
      overwriteObj(newDsv, props.globalDsv)
      overwriteObj(newDsv, localDsv)
      return await runDataSourceRequest(ds, newDsv, proxy, false, proxy.$message)
    }

    /**
     * 获取父级VmFormRender组件实例
     * @returns {object}
     */
    const getParentFormRef = () => {
      return props.parentForm
    }

    /**
     * 当显示多级嵌套弹窗或抽屉时，获取最顶层VmFormRender组件实例
     * @returns {object}
     */
    const getTopFormRef = () => {
      if (!props.parentForm) {
        return proxy
      }

      let topFormRef = props.parentForm
      while (topFormRef.parentForm) {
        topFormRef = topFormRef.parentForm
      }

      return topFormRef
    }

    const setChildFormRef = childFormRef => {
      data.childFormRef = childFormRef
    }

    const getChildFormRef = () => {
      return data.childFormRef
    }

    /**
     * 是否弹窗、抽屉组件动态创建的v-form-render
     * @returns {boolean}
     */
    const isDynamicCreation = () => {
      return props.dynamicCreation
    }

    const setDialogOrDrawerRef = ddRef => {
      data.dialogOrDrawerRef = ddRef
    }

    /**
     * 获取子级VmFormRender的包裹弹窗组件或抽屉组件实例ref
     * @returns {object}
     */
    const getDialogOrDrawerRef = () => {
      return data.dialogOrDrawerRef
    }

    /**
     * 显示弹窗表单，动态创建v-form-render组件，option-data、global-dsv等属性继承父级表单
     * @param dialogName
     * @param formData
     * @param extraData
     */
    const showDialog = (dialogName, formData = {}, extraData = {}) => {
      let topFormRef = getTopFormRef()
      let dialogCon = getContainerWidgetByName(topFormRef.widgetList, dialogName)
      if (!dialogName || dialogCon.type !== 'vf-dialog') {
        proxy.$message.error(i18nt('render.hint.refNotFound') + dialogName)
        return
      }
      let dFormJson = {
        formConfig: cloneFormConfigWithoutEventHandler(topFormRef.formConfig),
        widgetList: deepClone(dialogCon.widgetList)
      }

      let wrapperDivId = generateId() + ''
      let dialogInstance = createVNode(DynamicDialog, {
        extraData: extraData,
        formData: formData || {},
        formJson: dFormJson,
        globalDsv: topFormRef.globalDsv,
        optionData: topFormRef.optionData,
        options: dialogCon.options,
        parentFormRef: proxy,
        wrapperId: wrapperDivId
      })
      dialogInstance.appContext = proxy.$root.$.appContext // 非常重要， 覆盖应用上下文！！

      let wrapperDiv = document.createElement('div')
      wrapperDiv.id = 'vf-dynamic-dialog-wrapper' + wrapperDivId
      document.body.appendChild(wrapperDiv)
      render(dialogInstance, wrapperDiv)
      document.body.appendChild(dialogInstance.el)
      dialogInstance.component.ctx.show()
    }

    const showDrawer = (drawerName, formData = {}, extraData = {}) => {
      let topFormRef = getTopFormRef()
      let drawerCon = getContainerWidgetByName(topFormRef.widgetList, drawerName)
      if (!drawerCon || drawerCon.type !== 'vf-drawer') {
        proxy.$message.error(i18nt('render.hint.refNotFound') + drawerName)
        return
      }
      let dFormJson = {
        formConfig: cloneFormConfigWithoutEventHandler(topFormRef.formConfig),
        widgetList: deepClone(drawerCon.widgetList)
      }

      let wrapperDivId = generateId() + ''
      let drawerInstance = createVNode(DynamicDrawer, {
        extraData: extraData,
        formData: formData || {},
        formJson: dFormJson,
        globalDsv: topFormRef.globalDsv,
        optionData: topFormRef.optionData,
        options: drawerCon.options,
        parentFormRef: proxy,
        wrapperId: wrapperDivId
      })
      drawerInstance.appContext = proxy.$root.$.appContext // 非常重要， 覆盖应用上下文！！

      let wrapperDiv = document.createElement('div')
      wrapperDiv.id = 'vf-dynamic-drawer-wrapper' + wrapperDivId
      document.body.appendChild(wrapperDiv)
      render(drawerInstance, wrapperDiv)
      document.body.appendChild(drawerInstance.el)
      drawerInstance.component.ctx.show()
    }

    /**
     * 判断表单是否处于设计器预览状态
     * @return {boolean}
     */
    const isPreviewState = () => {
      return props.previewState
    }
    /**
     * 一次性设置formJson、formData和禁用表单
     * @param formJson
     * @param formData
     * @param disabledFlag
     */
    const setFormJsonAndData = (formJson, formData, disabledFlag = null) => {
      setFormJson(formJson)
      $nextTick(() => {
        if (!!formData) {
          setFormData(formData)
          $nextTick(() => {
            if (!!disabledFlag) {
              disableForm()
            }
          })
        }
      })
    }

    /**
     * 批量设置字段是否必填
     * @param widgetNames
     * @param required true/false
     */
    const setWidgetsRequired = (widgetNames, required) => {
      if (!!widgetNames) {
        if (typeof widgetNames === 'string') {
          findWidgetAndSetRequired(widgetNames, required)
        } else if (Array.isArray(widgetNames)) {
          widgetNames.forEach(wn => {
            findWidgetAndSetRequired(wn, required)
          })
        }
      }
    }

    // --------------------- 以上为组件支持外部调用的API方法 end ------------------//

    buildFormModel(!data.formJsonObj ? null : data.formJsonObj.widgetList)
    initFormObject()

    return {
      i18nt,
      ...toRefs(props),
      ...toRefs(data),
      ...emitterMixin,
      // #endregion

      // #region methods
      addEC,
      addFieldChangeEventHandler,
      addFieldValidateEventHandler,
      buildDataFromWidget,
      buildFormModel,

      // --------------------- 以下为组件支持外部调用的API方法 begin ------------------//
      /* 提示：用户可自行扩充这些方法！！！ */
      changeLanguage,
      clearFormDataModel,
      clearValidate,
      // #endregion

      // #region computed
      customClass,
      disableForm,
      disableWidgets,
      enableForm,
      enableWidgets,
      executeDataSource,
      findWidgetAndSetDisabled,
      findWidgetAndSetHidden,

      findWidgetAndSetRequired,
      findWidgetNameInSubForm,
      findWidgetOfSubFormAndSetDisabled,
      findWidgetOfSubFormAndSetHidden,
      findWidgetOfSubFormAndSetRequired,
      formConfig,
      formFieldRadius,
      formStyle,
      getChildFormRef,
      getContainerWidgetName,
      getContainerWidgets,
      getDialogOrDrawerRef,
      getEC,
      getFieldValue,
      getFieldWidgets,
      getFormData,
      getFormJson,
      getFormRef,
      getGlobalDsv,
      getLanguageName,
      getNativeForm,
      getParentFormRef,
      getReadMode,
      getSubFormNameOfWidget,
      getSubFormValues,
      getTopFormRef,
      getWidgetName,
      getWidgetRef,
      handleFieldDataChange,
      handleOnCreated,
      handleOnMounted,
      hasEC,
      hideWidgets,
      initDataSetRequest,
      initFormObject,
      initLocale,
      insertCustomStyleAndScriptNode,
      isDynamicCreation,
      isPreviewState,
      labelPosition,
      labelWidth,
      registerFormToRefList,
      reloadOptionData,

      // #region ref
      renderForm,
      resetForm,
      setBlankFormJson,
      setChildFormRef,
      setDialogOrDrawerRef,
      setFieldValue,
      setFormData,
      setFormJson,
      setFormJsonAndData,
      setReadMode,
      setSubFormValues,
      setWidgetsRequired,
      showDialog,
      showDrawer,
      showWidgets,
      size,
      validateFields,
      validateForm,
      widgetList
      // #endregion

      // --------------------- 以上为组件支持外部调用的API方法 end ------------------//
    }
  }
}
</script>

<style lang="scss" scoped>
.van-form {
  min-height: 100%;
  box-sizing: border-box;
  background: #efefef;
  padding: 10px;
}

.render-form.van-form {
  height: 100%;
  background: #efefef;
  box-sizing: border-box;
  padding: 10px;

  & > :deep(.field-wrapper):first-of-type {
    border-top-left-radius: v-bind(formFieldRadius);
    border-top-right-radius: v-bind(formFieldRadius);
    overflow: hidden;
  }
  & > :deep(.field-wrapper):last-of-type {
    border-bottom-left-radius: v-bind(formFieldRadius);
    border-bottom-right-radius: v-bind(formFieldRadius);
    overflow: hidden;
  }
}
</style>
