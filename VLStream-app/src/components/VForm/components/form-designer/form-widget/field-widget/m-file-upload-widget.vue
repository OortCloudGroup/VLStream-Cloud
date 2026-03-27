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
          <van-button v-if="!isReadMode && !fieldDisabled" icon="plus" size="small" style="" type="primary" @click="showUploaderFile=true">
            上传文件
          </van-button>
        </div>
      </template>
    </van-field>
    <oort-file-upload
      ref="oort_uploader"
      v-model:show="showUploaderFile"
      :file="fileList"
      :upload-u-r-l="field.options.uploadURL ? field.options.uploadURL : defaultUploadURL"
      @click="showUploaderFile=false"
      @getImgUrl="getImgFile"
    />
  </form-item-wrapper>
</template>

<script>
import { toRefs, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'
import OortFileUpload from '@/components/uploader/oort_fileUpload'
import { deepClone } from '~@/utils/util'
import config from '@/config/index'

export default {
  componentName: 'FieldWidget',
  name: 'MFileUploadWidget',
  components: {
    FormItemWrapper,
    OortFileUpload
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

    const data = reactive({
      defaultUploadURL: config.URL + config.gateWay + 'apaas-fastdfsservice/fastdfs/v1/uploadFile',
      showUploaderFile: false,
      accept: '*/*',
      ImagePreview: {},
      fieldModel: [],
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

    const getImgFile = (file) => {
      data.fieldModel = file
      updateFieldModelAndEmitDataChangeForRemove(null, file, -1)
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

    const handleFileDelete = (file, index) => {
      updateFieldModelAndEmitDataChangeForRemove(file, data.fileList, index)
      data.uploadBtnHidden = data.fieldModel.length >= props.field.options.limit
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
      handleFileDelete,
      getImgFile
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
    background-color: #f9f9f9;
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
