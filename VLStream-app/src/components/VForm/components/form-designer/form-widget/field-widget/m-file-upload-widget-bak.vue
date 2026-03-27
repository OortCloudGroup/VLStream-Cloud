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
      :label-width="fieldLabelWidth"
      :label-align="field.options.labelAlign"
      :disabled="fieldDisabled"
      :required="field.options.required"
      :size="size"
      :rules="rules"
    >
      <template #input>
        <div class="preview-fileList">
          <template v-if="!slots[field.options.name]">
            <div v-for="(file, index) in fieldModel" :key="index" class="preview-cover" :class="{ readmode: field.options.readonly || isReadMode }">
              {{ file.name }}
              <span v-if="!isReadMode && !fieldDisabled" class="file-action el-icon-delete" @click="handleFileDelete(file, index)">
                <van-icon name="delete-o" />
              </span>
              <a v-if="!isReadMode" :href="file.url" target="_blank" class="file-link">
                <span class="file-action el-icon-download">
                  <van-icon name="down" />
                </span>
              </a>
            </div>
          </template>
          <slot v-else :name="field.options.name" v-bind="{ fieldModel, field }" />
          <template v-if="slots[slotButtonName]">
            <van-uploader
              v-if="!isReadMode"
              ref="fieldEditor"
              v-model="fileList"
              :disabled="fieldDisabled"
              :deletable="!field.options.disabled"
              :accept="acceptTypes"
              :max-count="field.options.limit"
              :max-size="uploadMaxSize"
              result-type="file"
              :preview-image="false"
              :multiple="!!field.options.multipleSelect"
              :before-read="beforeRead"
              :after-read="afterRead"
              @delete="handleFileDelete"
              @oversize="handleOversize"
            >
              <slot :name="slotButtonName" v-bind="{ fieldModel, field }" />
            </van-uploader>
          </template>
          <template v-else>
            <van-uploader
              v-if="!isReadMode"
              ref="fieldEditor"
              v-model="fileList"
              :disabled="fieldDisabled"
              :deletable="!field.options.disabled"
              :accept="acceptTypes"
              :max-count="field.options.limit"
              :max-size="uploadMaxSize"
              result-type="file"
              :preview-image="false"
              :multiple="!!field.options.multipleSelect"
              :before-read="beforeRead"
              :after-read="afterRead"
              @delete="handleFileDelete"
              @oversize="handleOversize"
            >
              <van-button icon="plus" native-type="button" type="primary">
                {{ field.options.uploadButtonText }}
              </van-button>
            </van-uploader>
          </template>
        </div>
      </template>
    </van-field>
  </form-item-wrapper>
</template>

<script>
import { computed, toRefs, reactive, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'
import { asyncUploadFile, mimeTypes, deepClone } from '~@/utils/util'

export default {
  componentName: 'FieldWidget',
  name: 'MFileUploadWidget',
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
      ImagePreview: {},
      fieldModel: null,
      fileList: [], // 上传文件列表

      noChangeEventFlag: true, // van-field组件没有change事件！！
      oldFieldValue: null, // field组件change之前的值
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
        let resultTypes = []
        props.field.options.fileTypes.forEach(ft => {
          let newType = ft.startsWith('.') ? ft : '.' + ft
          resultTypes.push(newType)
        })

        // 同时添加.扩展名和MIME类型,在android app中只能识别MIME类型才能显示选择文件的按钮，部分手机无法筛选文件
        props.field.options.fileTypes.forEach(ft => {
          let mimeType = mimeTypes[ft]
          if (mimeType) {
            resultTypes.push(mimeType)
          }
        })

        return resultTypes.join(',')
      }

      return '*'
    })

    const slotButtonName = computed(() => {
      return props.field.options.name + 'Button'
    })
    const slots = computed(() => {
      return proxy.$slots
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
      let fileTypeCheckResult = false

      let files = []
      if (Array.isArray(file)) {
        files = file
      } else {
        files.push(file)
      }

      for (let i = 0; i < files.length; i++) {
        let extFileName = files[i].name.substring(files[i].name.lastIndexOf('.') + 1)
        if (!!props.field.options && !!props.field.options.fileTypes) {
          let uploadFileTypes = props.field.options.fileTypes
          if (uploadFileTypes.length > 0) {
            fileTypeCheckResult = uploadFileTypes.some(ft => {
              return extFileName.toLowerCase() === ft.toLowerCase()
            })
          }
        }
        if (!fileTypeCheckResult) {
          proxy.$message.error(i18nt('render.hint.unsupportedFileType') + extFileName)
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
        data.fieldModel.push(defaultResult)
      }

      fieldMixin.syncUpdateFormModel(data.fieldModel)
      fieldMixin.emitFieldDataChange(data.fieldModel, oldValue)
    }

    const removeUploadFile = deleteFileIndex => {
      let file = data.fileList[deleteFileIndex]
      handleFileDelete(file, deleteFileIndex)
    }

    const handleFileDelete = (file, index) => {
      updateFieldModelAndEmitDataChangeForRemove(file, data.fileList, index)
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
        data.fileList.splice(deletedIndex, 1) // 手动删除列表中的文件，m-picture-upload不需要手动处理，删除用的是组件内部方法
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
      handleFileDelete,

      handleOversize,
      removeUploadFile,
      slotButtonName,
      slots,
      uploadMaxSize
    }
  }
}
</script>

<style scoped lang="scss">
.preview-fileList {
  display: flex;
  flex-direction: column;
  width: 100%;

  .preview-cover {
    box-sizing: border-box;
    width: 100%;
    line-height: 23px;
    max-height: 50px;
    overflow: hidden;
    color: #000000;
    font-size: 12px;
    text-align: left;
    overflow: hidden;
    padding: 4px 8px;
    margin-bottom: 5px;
    &:hover {
      background-color: #f5f7fa;
    }

    &.readmode {
      background-color: transparent;
    }
  }
  .file-link {
    text-decoration: none;
    color: black;
  }
  .file-action {
    float: right;
    display: inline-block;
    width: 23px;
    height: 23px;
    line-height: 23px;
    font-size: 14px;
    color: #409eff;
    vertical-align: middle;
    cursor: pointer;
  }
}
</style>
