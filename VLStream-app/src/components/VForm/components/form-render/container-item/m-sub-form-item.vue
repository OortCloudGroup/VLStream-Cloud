<template>
  <container-item-wrapper :widget="widget">
    <div v-show="!widget.options.hidden" :key="widget.id" class="sub-form-container">
      <van-row class="header-row">
        <div class="action-header-column">
          <span class="action-label">{{ i18nt('render.hint.subFormAction') }}</span>
          <van-button
            v-if="!isReadMode"
            :disabled="widgetDisabled || actionDisabled || insertDisabled"
            round
            type="info"
            size="mini"
            class="action-button"
            :title="i18nt('render.hint.subFormAddActionHint')"
            @click="addSubFormRow"
          >
            {{ i18nt('render.hint.subFormAddAction') }}<i class="el-icon-plus el-icon-right" />
          </van-button>
        </div>
      </van-row>

      <div v-for="(subFormRowId, sfrIdx) in rowIdData" :key="subFormRowId" class="sub-form-row">
        <div class="sub-form-action-column hide-label">
          <div class="action-button-column">
            <span v-if="widget.options.showRowNumber" class="row-number-span">#{{ sfrIdx + 1 }}</span>
          </div>
          <div>
            <van-button
              v-show="!isReadMode"
              :disabled="widgetDisabled || actionDisabled || insertDisabled"
              class="round-button"
              round
              plain
              type="info"
              size="mini"
              icon="plus"
              :title="i18nt('render.hint.insertSubFormRow')"
              @click="insertSubFormRow(sfrIdx + 1)"
            />
            <van-button
              v-show="!isReadMode"
              :disabled="widgetDisabled || actionDisabled || deleteDisabled"
              class="round-button"
              round
              plain
              type="warning"
              size="mini"
              icon="minus"
              :title="i18nt('render.hint.deleteSubFormRow')"
              @click="deleteSubFormRow(sfrIdx)"
            />
          </div>
        </div>
        <div v-if="!leftActionColumn && widget.options.showRowNumber" class="row-no-column">
          <span v-if="widget.options.showRowNumber" class="row-number-span">#{{ sfrIdx + 1 }}</span>
        </div>
        <div class="grid-sub-form-data-row">
          <!-- eslint-disable-next-line vue/no-v-for-template-key -->
          <template v-for="(subWidget, swIdx) in widget.widgetList" :key="fieldSchemaData[sfrIdx][swIdx].id">
            <template v-if="'container' === subWidget.category">
              <component
                :is="subWidget.type + '-item'"
                :widget="fieldSchemaData[sfrIdx][swIdx]"
                :parent-list="widget.widgetList"
                :index-of-parent-list="swIdx"
                :parent-widget="widget"
                :sub-form-row-id="subFormRowId"
                :sub-form-row-index="sfrIdx"
                :sub-form-col-index="swIdx"
              >
                <!-- 子表单暂不支持插槽！！！ -->
                <!-- 递归传递插槽！！！ -->
                <template v-for="slot in Object.keys($slots)" #[slot]="scope">
                  <slot :name="slot" v-bind="scope" />
                </template>
              </component>
            </template>
            <template v-else>
              <component
                :is="subWidget.type + '-widget'"
                :field="fieldSchemaData[sfrIdx][swIdx]"
                :parent-list="widget.widgetList"
                :index-of-parent-list="swIdx"
                :parent-widget="widget"
                :sub-form-row-id="subFormRowId"
                :sub-form-row-index="sfrIdx"
                :sub-form-col-index="swIdx"
              >
                <!-- 子表单暂不支持插槽！！！ -->
                <!-- 递归传递插槽！！！ -->
                <template v-for="slot in Object.keys($slots)" #[slot]="scope">
                  <slot :name="slot" v-bind="scope" />
                </template>
              </component>
            </template>
          </template>
        </div>
      </div>
    </div>
  </container-item-wrapper>
</template>

<script>
import { computed, reactive, toRefs, inject, provide, nextTick, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'

import { useI18n } from '~@/utils/i18n'
import { useEmitter } from '~@/utils/emitter'
import { useRef } from '../../../components/form-render/refMixin'
import { useContainer } from './containerItemMixin'
// import { px2rem } from '~@/utils/util'

import { deepClone, generateId } from '../../../utils/util'

import ContainerItemWrapper from './container-item-wrapper'
import FieldComponents from '~@/components/form-designer/form-widget/field-widget/index'

export default {
  componentName: 'ContainerItem',
  name: 'MSubFormItem',
  components: {
    ContainerItemWrapper,
    ...FieldComponents
  },
  props: {
    widget: Object
  },
  setup(props) {
    // const refList = inject('refList')
    const globalModel = inject('globalModel')
    const sfRefList = inject('sfRefList')
    const getReadMode = inject('getReadMode')

    const { proxy } = getCurrentInstance()

    provide('getSubFormFieldFlag', () => true)
    provide('getSubFormName', () => props.widget.options.name)

    const { i18nt } = useI18n()
    const data = reactive({
      actionDisabled: false,
      deleteDisabled: false,
      fieldSchemaData: [],
      insertDisabled: false,
      rowIdData: []
    })

    const refMixin = useRef()
    const emitterMixin = useEmitter(data)
    const containerMixin = useContainer(data)

    const isReadMode = computed(() => {
      return getReadMode()
    })

    const leftActionColumn = computed(() => {
      return (props.widget.options.actionColumnPosition || 'left') === 'left'
    })

    const getLabelAlign = (widget, subWidget) => {
      return subWidget.options.labelAlign || widget.options.labelAlign
    }

    const widgetDisabled = computed(() => {
      return !!props.widget.options.disabled
    })

    // const disabledClass = computed(() => {
    //   return !!widgetDisabled.value ? 'sub-form-disabled' : ''
    // })

    // const readModeClass = computed(() => {
    //   return !!getReadMode() ? 'sub-form-read-mode' : ''
    // })

    const registerSubFormToRefList = () => {
      if (props.widget.type === 'm-sub-form') {
        sfRefList[props.widget.options.name] = proxy
      }
    }
    const initRowIdData = initFlag => {
      if (props.widget.type === 'm-sub-form') {
        data.rowIdData.splice(0, data.rowIdData.length) // 清除数组必须用splice，length=0不会响应式更新！！
        let subFormModel = containerMixin.formModel.value[props.widget.options.name]

        if (!!subFormModel && subFormModel.length > 0) {
          subFormModel.forEach(() => {
            data.rowIdData.push('id' + generateId())
          })

          if (!!initFlag) {
            // 注意：事件触发需延期执行，SumFormDataChange事件处理代码中可能存在尚未创建完成的组件！！
            setTimeout(() => {
              handleSubFormRowChange(subFormModel)
            }, 800)
          }
        }
      }
    }
    const addToRowIdData = () => {
      data.rowIdData.push('id' + generateId())
    }
    const insertToRowIdData = rowIndex => {
      data.rowIdData.splice(rowIndex, 0, 'id' + generateId())
    }
    const deleteFromRowIdData = rowIndex => {
      data.rowIdData.splice(rowIndex, 1)
    }
    const getRowIdData = () => {
      return data.rowIdData
    }
    const getWidgetRefOfSubForm = (widgetName, rowIndex) => {
      let realWidgetName = widgetName + '@row' + data.rowIdData[rowIndex]
      return refMixin.getWidgetRef(realWidgetName)
    }
    const initFieldSchemaData = () => {
      // 初始化fieldSchemaData！！！
      if (props.widget.type !== 'm-sub-form') {
        return
      }

      let rowLength = data.rowIdData.length
      data.fieldSchemaData.splice(0, data.fieldSchemaData.length) // 清除数组必须用splice，length=0不会响应式更新！！
      if (rowLength > 0) {
        for (let i = 0; i < rowLength; i++) {
          let fieldSchemas = []
          props.widget.widgetList.forEach(swItem => {
            fieldSchemas.push(cloneFieldSchema(swItem))
          })
          data.fieldSchemaData.push(fieldSchemas)
        }

        // if (!data.fieldSchemaData) {
        //   nextTick(() => {
        //     containerMixin.disableSubForm();
        //   });
        // }
      }
    }
    const addToFieldSchemaData = rowIndex => {
      let fieldSchemas = []
      props.widget.widgetList.forEach(swItem => {
        fieldSchemas.push(cloneFieldSchema(swItem))
      })

      if (rowIndex === undefined) {
        data.fieldSchemaData.push(fieldSchemas)
      } else {
        data.fieldSchemaData.splice(rowIndex, 0, fieldSchemas)
      }
    }
    const deleteFromFieldSchemaData = rowIndex => {
      data.fieldSchemaData.splice(rowIndex, 1)
    }
    const cloneFieldSchema = fieldWidget => {
      let newFieldSchema = deepClone(fieldWidget)
      newFieldSchema.id = fieldWidget.type + generateId()
      return newFieldSchema
    }
    const initEventHandler = () => {
      if (props.widget.type !== 'm-sub-form') {
        return
      }

      emitterMixin.on$('setFormData', newFormData => {
        initRowIdData(false)
        initFieldSchemaData()

        let subFormData = newFormData[props.widget.options.name] || []
        setTimeout(() => {
          // 延时触发SubFormRowChange事件, 便于更新计算字段！！
          handleSubFormRowChange(subFormData)
        }, 800)
      })
    }
    const handleSubFormFirstRowAdd = () => {
      if (props.widget.type !== 'm-sub-form') {
        return
      }

      if (!!props.widget.options.showBlankRow && data.rowIdData.length === 1) {
        let oldSubFormData = containerMixin.formModel.value[props.widget.options.name] || []

        // 确认组件创建成功后触发事件!!
        nextTick(() => {
          handleSubFormRowAdd(oldSubFormData, data.rowIdData[0])
          handleSubFormRowChange(oldSubFormData)
        })
      }
    }

    const addSubFormRow = () => {
      let newSubFormDataRow = {}
      props.widget.widgetList.forEach(subFormItem => {
        if (!!subFormItem.formItemFlag) {
          newSubFormDataRow[subFormItem.options.name] = subFormItem.options.defaultValue
        }
      })

      let oldSubFormData = containerMixin.formModel.value[props.widget.options.name] || []
      oldSubFormData.push(newSubFormDataRow)

      addToRowIdData()
      addToFieldSchemaData()

      // 确认组件创建成功后触发事件!!
      nextTick(() => {
        handleSubFormRowAdd(oldSubFormData, data.rowIdData[oldSubFormData.length - 1])
        handleSubFormRowChange(oldSubFormData)
      })
    }
    const insertSubFormRow = beforeFormRowIndex => {
      let newSubFormDataRow = {}
      props.widget.widgetList.forEach(subFormItem => {
        if (!!subFormItem.formItemFlag) {
          newSubFormDataRow[subFormItem.options.name] = subFormItem.options.defaultValue
        }
      })

      let oldSubFormData = containerMixin.formModel.value[props.widget.options.name] || []
      oldSubFormData.splice(beforeFormRowIndex, 0, newSubFormDataRow)
      insertToRowIdData(beforeFormRowIndex)
      addToFieldSchemaData(beforeFormRowIndex)

      // 确认组件创建成功后触发事件!!
      nextTick(() => {
        handleSubFormRowInsert(oldSubFormData, data.rowIdData[beforeFormRowIndex])
        handleSubFormRowChange(oldSubFormData)
      })
    }
    const deleteSubFormRow = formRowIndex => {
      proxy
        .$showConfirmDialog({
          cancelButtonText: i18nt('render.hint.cancel'),
          className: 'van-dialog-confirm-dialog',

          confirmButtonText: i18nt('render.hint.confirm'),
          message: i18nt('render.hint.deleteSubFormRow') + '?',

          overlayClass: 'van-dialog-confirm-overlay',
          showCancelButton: true,
          showConfirmButton: true,
          title: i18nt('render.hint.prompt')
        })
        .then(() => {
          let oldSubFormData = containerMixin.formModel.value[props.widget.options.name] || []
          let deletedDataRow = deepClone(oldSubFormData[formRowIndex])
          oldSubFormData.splice(formRowIndex, 1)
          deleteFromRowIdData(formRowIndex)
          deleteFromFieldSchemaData(formRowIndex)

          // 确认组件创建成功后触发事件!!
          nextTick(() => {
            handleSubFormRowDelete(oldSubFormData, deletedDataRow, formRowIndex)
            handleSubFormRowChange(oldSubFormData)
          })
        })
    }
    const handleSubFormRowChange = subFormData => {
      if (!!props.widget.options.onSubFormRowChange) {
        let customFunc = new Function('subFormData', props.widget.options.onSubFormRowChange)
        customFunc.call(proxy, subFormData)
      }
    }
    const handleSubFormRowAdd = (subFormData, newRowId) => {
      if (!!props.widget.options.onSubFormRowAdd) {
        let customFunc = new Function('subFormData', 'newRowId', props.widget.options.onSubFormRowAdd)
        customFunc.call(proxy, subFormData, newRowId)
      }
    }
    const handleSubFormRowInsert = (subFormData, newRowId) => {
      if (!!props.widget.options.onSubFormRowInsert) {
        let customFunc = new Function('subFormData', 'newRowId', props.widget.options.onSubFormRowInsert)
        customFunc.call(proxy, subFormData, newRowId)
      }
    }
    const handleSubFormRowDelete = (subFormData, deletedDataRow, deletedRowIndex) => {
      if (!!props.widget.options.onSubFormRowDelete) {
        let customFunc = new Function('subFormData', 'deletedDataRow', 'deletedRowIndex', props.widget.options.onSubFormRowDelete)
        customFunc.call(proxy, subFormData, deletedDataRow, deletedRowIndex)
      }
    }

    const setDisabled = flag => {
      if (!!flag) {
        containerMixin.disableSubForm()
      } else {
        containerMixin.enableSubForm()
      }
    }

    /**
     * 设置单行子表单是否禁止新增、插入记录
     * @param flag
     */
    const setInsertDisabled = flag => {
      data.insertDisabled = flag
    }

    /**
     * 设置单行子表单是否禁止删除记录
     * @param flag
     */
    const setDeleteDisabled = flag => {
      data.deleteDisabled = flag
    }

    /**
     * 单独给子表单赋值
     * 注意：该方法仅触发组件的onChange事件以及子表单的onFormRowChange事件，不会触发表单的onFormDataChange等其他事件！！
     * @param subFormValues
     */
    const setSubFormValues = subFormValues => {
      globalModel.formModel[props.widget.options.name] = subFormValues
      initRowIdData(false)
      initFieldSchemaData()

      setTimeout(() => {
        // 延时触发SubFormRowChange事件, 便于更新计算字段！！
        handleSubFormRowChange(subFormValues)
      }, 800)
    }

    /**
     * 单独为子表单某行的字段组件赋值
     * @param fieldName
     * @param fieldValue
     * @param rowIndex
     */
    const setSubFormFieldValue = (fieldName, fieldValue, rowIndex) => {
      const subFormData = globalModel.formModel[props.widget.options.name]
      subFormData[rowIndex][fieldName] = fieldValue

      handleSubFormRowChange(subFormData)
    }

    refMixin.initRefList()
    registerSubFormToRefList()
    initRowIdData(true)
    initFieldSchemaData()
    initEventHandler()

    onMounted(() => {
      handleSubFormFirstRowAdd()
    })

    onBeforeUnmount(() => {
      containerMixin.unregisterFromRefList()
    })

    return {
      i18nt,
      ...toRefs(props),
      ...toRefs(data),
      ...emitterMixin,
      ...containerMixin,

      addSubFormRow,
      addToFieldSchemaData,
      addToRowIdData,
      cloneFieldSchema,
      deleteFromFieldSchemaData,
      deleteFromRowIdData,
      deleteSubFormRow,
      getLabelAlign,
      getRowIdData,
      getWidgetRefOfSubForm,
      handleSubFormFirstRowAdd,
      handleSubFormRowAdd,
      handleSubFormRowChange,
      handleSubFormRowDelete,
      handleSubFormRowInsert,
      initEventHandler,
      initFieldSchemaData,
      initRowIdData,
      insertSubFormRow,
      insertToRowIdData,
      isReadMode,
      leftActionColumn,
      registerSubFormToRefList,
      setDeleteDisabled,
      setDisabled,
      setInsertDisabled,
      setSubFormFieldValue,
      setSubFormValues,
      widgetDisabled
    }
  }
}
</script>

<style lang="scss" scoped>
.sub-form-container {
  box-sizing: border-box;
  text-align: left; //IE浏览器强制居左对齐

  :deep(.el-row.header-row) {
    padding: 0;
    display: flex;
  }

  :deep(div.sub-form-row) {
    padding: 0;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    border: 0px none;
    border-top: 1px solid #e1e2e3;

    .row-number-span {
      margin-right: 10px;
      font-size: 14px;
    }
  }
}

div.action-header-column {
  display: inline-block;
  //width: 120px;
  width: 100%;
  border-top: 1px solid #e1e2e3;
  background: #f1f2f3;
  padding: 8px;
  box-sizing: border-box;

  .action-label {
    margin-right: 12px;
    font-size: 14px;
  }

  .action-button {
    padding-left: 8px;
    padding-right: 8px;
    margin-right: 5px;
    float: right;
  }
}

div.field-header-column {
  display: inline-block;
  //overflow: hidden;
  //white-space: nowrap;  //文字超出长度不自动换行
  //text-overflow: ellipsis;  //文字超出长度显示省略号
  // border: 1px solid #e1e2e3;
  background: #f1f2f3;
  padding: 8px;

  span.custom-label i {
    margin: 0 3px;
  }
}

div.field-header-column.is-required:before {
  content: '*';
  color: #f56c6c;
  margin-right: 4px;
}

div.label-center-left {
  text-align: left;
}

div.label-center-align {
  text-align: center;
}

div.label-right-align {
  text-align: right;
}

div.sub-form-action-column {
  display: inline-flex;
  justify-content: space-between;
  align-items: center;
  text-align: center;
  // width: 120px;
  width: 100%;
  background-color: #f5f5f5;
  padding: 4px 10px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }

  :deep(.el-button) {
    font-size: 18px;
    padding: 0;
    background: #dcdfe6;
    border: 4px solid #dcdfe6;
  }
}

div.grid-sub-form-data-row {
  display: inline-block;
  width: 100%;
  // border-left: 1px solid #e1e2e3;
  // border-right: 1px solid #e1e2e3;
}

div.sub-form-action-column.hide-label {
  :deep(.el-form-item__label) {
    display: none;
  }

  :deep(.round-button) {
    width: 24px;
    height: 24px;
    line-height: 24px;
  }
}

div.row-no-column {
  display: flex;
  align-items: center;
  width: 50px;
  border-radius: 10px;
  background: #f1f2f3;
  padding: 5px 0;
  margin: 0 6px;
}

div.sub-form-table-column {
  display: inline-block;
  border: 1px solid #e1e2e3;
  padding: 8px;

  :deep(.el-form-item) {
    margin-left: 4px;
    margin-right: 4px;
    margin-bottom: 0;
  }

  :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }
}

div.sub-form-table-column.hide-label {
  :deep(.el-form-item__label) {
    display: none;
  }
}
</style>

<style>
.van-dialog-confirm-overlay {
  z-index: 5001 !important;
}
.van-dialog-confirm-dialog {
  z-index: 5002 !important;
}
</style>
