<template>
  <form-item-wrapper
    :designer="designer"
    :field="field"
    :design-state="designState"
    :title="field.options.label"
    :parent-widget="parentWidget"
    :parent-list="parentList"
    :index-of-parent-list="indexOfParentList"
    :sub-form-row-index="subFormRowIndex"
    :sub-form-col-index="subFormColIndex"
    :sub-form-row-id="subFormRowId"
  >
    <van-field
      :name="field.options.name"
      :label="fieldLabel"
      :label-align="field.options.labelAlign"
      :label-width="fieldLabelWidth"
      :disabled="fieldDisabled"
      :required="field.options.required"
      :size="size"
      :rules="rules"
    >
      <template #input>
        <slot v-if="useCustomPreview" :name="field.options.name" v-bind="{ fieldModel, field }" />
        <!-- 用按钮插槽和不用按钮插槽的必须是两个，无法在van-uploader内部切换,需要刷新页面 -->
        <template v-if="slots[slotButtonName]">
          <van-uploader
            ref="fieldEditor"
            v-model="fileList"
            :accept="acceptTypes"
            :after-read="afterRead"
            :before-read="beforeRead"
            :capture="capture"
            :disabled="fieldDisabled"
            :deletable="deletable"
            :readonly="isReadMode"
            :max-count="field.options.limit"
            :max-size="uploadMaxSize"
            :multiple="!!field.options.multipleSelect"
            :preview-options="previewOptions"
            :show-upload="!isReadMode"
            :preview-image="!useCustomPreview"
            @delete="handleFileDelete"
            @oversize="handleOversize"
          >
            <slot :name="slotButtonName" v-bind="{ fieldModel, field, formModel }" />
          </van-uploader>
        </template>
        <template v-else>
          <van-uploader
            ref="fieldEditor"
            v-model="fileList"
            :accept="acceptTypes"
            :after-read="afterRead"
            :before-read="beforeRead"
            :capture="capture"
            :disabled="fieldDisabled"
            :deletable="deletable"
            :readonly="isReadMode"
            :max-count="field.options.limit"
            :max-size="uploadMaxSize"
            :multiple="!!field.options.multipleSelect"
            :preview-options="previewOptions"
            :show-upload="!isReadMode"
            :preview-image="!useCustomPreview"
            @delete="handleFileDelete"
            @oversize="handleOversize"
          />
        </template>
      </template>
    </van-field>
  </form-item-wrapper>
</template>

<script>
import { computed, toRefs, reactive, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'
import { asyncUploadFile, deepClone } from '~@/utils/util'

export default {
  componentName: 'FieldWidget',
  name: 'MPictureUploadWidget',
  components: {
    FormItemWrapper
  },
  props: {
    designState: {
      default: false,
      type: Boolean
    },
    designer: Object,
    field: Object,
    indexOfParentList: Number,
    parentList: Array,

    parentWidget: Object,

    subFormColIndex: {
      default: -1,
      /* 子表单组件列索引，从0开始计数 */
      type: Number
    },
    subFormRowId: {
      default: '',
      /* 子表单组件行Id，唯一id且不可变 */
      type: String
    },
    subFormRowIndex: {
      default: -1,
      /* 子表单组件行索引，从0开始计数 */
      type: Number
    }
  },
  setup(props) {
    const { i18nt } = useI18n()
    const { proxy } = getCurrentInstance()

    const data = reactive({
      fieldModel: null,
      fileList: [], // 上传文件列表
      noChangeEventFlag: true, // van-field组件没有change事件！！

      oldFieldValue: null, // field组件change之前的值
      previewOptions: {
        getContainer: () => {
          return document.querySelector('.el-dialog__wrapper')
        }
      },
      rules: [],
      uploadBtnHidden: false,

      uploadData: {
        key: '' // 七牛云上传文件名
        // token: '',  //七牛云上传token

        // policy: '',  //又拍云上传policy
        // authorization: '',  //又拍云上传签名
      },

      uploadHeaders: {}
    })
    const fieldMixin = useField(data)

    onMounted(() => {
      fieldMixin.handleOnMounted()
    })

    onBeforeUnmount(() => {
      fieldMixin.unregisterFromRefList()
    })

    const uploadMaxSize = computed(() => {
      return (props.field.options.fileMaxSize || 5) * 1048576 // MBytes
    })

    const acceptTypes = computed(() => {
      if (!!props.field.options.fileTypes && props.field.options.fileTypes.length > 0) {
        let resultTypes = ''
        props.field.options.fileTypes.forEach(ft => {
          let newType = ft.startsWith('.') ? ft + ',' : '.' + ft + ','
          resultTypes = resultTypes + newType
        })

        return resultTypes
      }

      return 'image/*'
    })

    const capture = computed(() => {
      if (props.field.options.capture) {
        if (props.field.options.capture === 'album') {
          return undefined
        } else {
          return props.field.options.capture
        }
      }

      return undefined
    })

    const slotButtonName = computed(() => {
      return props.field.options.name + 'Button'
    })
    const slots = computed(() => {
      return proxy.$slots
    })

    const useCustomPreview = computed(() => {
      return !!proxy.$slots[props.field.options.name]
    })

    // 允许删除： 禁用或者只读状态禁止删除
    const deletable = computed(() => {
      if (fieldMixin.isReadMode.value || fieldMixin.fieldDisabled.value) {
        return false
      }
      return true
    })

    const handleOnBeforeUpload = file => {
      // eslint-disable-next-line no-async-promise-executor
      return new Promise(async(resolve, reject) => {
        if (!!props.field.options.onBeforeUpload) {
          let bfFunc = new Function('file', props.field.options.onBeforeUpload)
          let result
          try {
            result = await bfFunc.call(proxy, file)
            if (typeof result === 'boolean') {
              result ? resolve(file) : reject()
            } else {
              resolve(result || file)
            }
          } catch (e) {
            uploadErrorCallBack(e, file, data.fileList)
            reject()
          }
        } else {
          resolve(file)
        }
      })
    }

    const beforeRead = file => {
      // 检查上传图片类型
      let fileTypeCheckResult = false

      let files = []
      if (Array.isArray(file)) {
        files = file
      } else {
        files.push(file)
      }

      for (let i = 0; i < files.length; i++) {
        if (!!props.field.options && !!props.field.options.fileTypes) {
          let uploadFileTypes = props.field.options.fileTypes
          if (uploadFileTypes.length > 0) {
            fileTypeCheckResult = uploadFileTypes.some(ft => {
              return files[i].type === 'image/' + ft
            })
          }
        }

        if (!fileTypeCheckResult) {
          proxy.$message.error(i18nt('render.hint.unsupportedFileType') + file.type)
          return false
        }
      }

      return handleOnBeforeUpload(file)
    }

    const afterRead = file => {
      let gDSV = fieldMixin.getGlobalDsv()
      let files = []
      if (Array.isArray(file)) {
        files = file
      } else files.push(file)

      // 多文件上传采用多次上传单文件的方式,如果想一次上传多文件可以修改这里代码，直接给asyncUploadFile传数组
      files.forEach(fileItem => {
        asyncUploadFile(fileItem, props.field.options.uploadURL, gDSV, data.uploadHeaders, data.uploadData)
          .then(result => {
            uploadSuccessCallBack(result, fileItem)
          })
          .catch(error => uploadErrorCallBack(error, fileItem))
      })
    }

    const uploadSuccessCallBack = (result, file) => {
      let customResult = null
      if (!!props.field.options.onUploadSuccess) {
        let customFn = new Function('result', 'file', 'fileList', props.field.options.onUploadSuccess)
        customResult = customFn.call(proxy, result, file, data.fileList)
      }

      updateFieldModelAndEmitDataChangeForUpload(data.fileList, customResult, result)
      data.uploadBtnHidden = data.fieldModel.length >= props.field.options.limit
    }

    const uploadErrorCallBack = (error, file) => {
      if (!!props.field.options.onUploadError) {
        let customFn = new Function('error', 'file', 'fileList', props.field.options.onUploadError)
        customFn.call(proxy, error, file, data.fileList)
      } else {
        proxy.$message({
          duration: 3000,
          message: i18nt('render.hint.uploadError') + error,
          type: 'error'
        })
      }
    }

    const updateFieldModelAndEmitDataChangeForUpload = (fileList, customResult, defaultResult) => {
      data.fieldModel = data.fieldModel || []
      let oldValue = deepClone(data.fieldModel)
      if (!!customResult && !!customResult.name && !!customResult.url) {
        data.fieldModel.push(customResult)
      } else if (!!defaultResult && !!defaultResult.name && !!defaultResult.url) {
        data.fieldModel.push(defaultResult)
      } else if (!!defaultResult && Array.isArray(defaultResult)) {
        defaultResult.forEach(item => {
          data.fieldModel.push(item)
        })
      } else if (!!defaultResult && !!defaultResult.data && !!defaultResult.data.name && !!defaultResult.data.url) {
        this.fieldModel.push({
          name: defaultResult.data.name,
          url: defaultResult.data.url
        })
      } else {
        this.fieldModel = deepClone(fileList)
      }

      fieldMixin.syncUpdateFormModel(data.fieldModel)
      fieldMixin.emitFieldDataChange(data.fieldModel, oldValue)
    }

    const removeUploadFile = deleteFileIndex => {
      let file = data.fileList[deleteFileIndex]
      handleFileDelete(file, deleteFileIndex)
    }

    const handleFileDelete = (file, item) => {
      updateFieldModelAndEmitDataChangeForRemove(file, data.fileList, item.index)

      data.uploadBtnHidden = data.fieldModel.length >= props.field.options.limit

      if (!!props.field.options.onFileRemove) {
        let customFn = new Function('file', 'fileList', props.field.options.onFileRemove)
        customFn.call(proxy, file, data.fileList)
      }
    }

    const updateFieldModelAndEmitDataChangeForRemove = (file, fileList, deletedIndex) => {
      let oldValue = deepClone(data.fieldModel)
      if (deletedIndex > -1) {
        data.fieldModel.splice(deletedIndex, 1)
      }
      fieldMixin.syncUpdateFormModel(data.fieldModel)
      fieldMixin.emitFieldDataChange(data.fieldModel, oldValue)
    }

    const handleOversize = () => {
      let uploadFileMaxSize = props.field.options.fileMaxSize || 5
      proxy.$message.error(i18nt('render.hint.fileSizeExceed') + uploadFileMaxSize + 'MB')
    }

    fieldMixin.registerToRefList()
    fieldMixin.initFieldModel()
    fieldMixin.initEventHandler()
    fieldMixin.buildFieldRules()

    fieldMixin.handleOnCreated()
    return {
      i18nt,
      ...toRefs(data),
      ...fieldMixin,

      acceptTypes,
      afterRead,
      beforeRead,
      capture,
      deletable,
      handleFileDelete,
      handleOversize,

      removeUploadFile,
      slotButtonName,
      slots,
      uploadMaxSize,
      useCustomPreview
    }
  }
}
</script>

<style>
.van-image-preview.preview-z-index {
  z-index: 5001 !important;
}
</style>
