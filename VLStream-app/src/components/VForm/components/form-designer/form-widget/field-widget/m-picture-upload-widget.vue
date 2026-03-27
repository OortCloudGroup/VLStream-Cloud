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
        <div class="preview-fileList">
          <div v-for="(file, index) in fieldModel" :key="index" class="preview-cover" :class="{ readmode: field.options.readonly || isReadMode }">
            <img class="preview-cover_img" :src="file.url" />
            <span v-if="!isReadMode && !fieldDisabled" class="file-action el-icon-delete" @click="handleFileDelete(file, index)">
              <van-icon name="cross" color="#fff" size="10" />
            </span>
          </div>

          <div v-if="!isReadMode && !fieldDisabled" class="add_plus" @click="showUploaderFile=true">
            <van-icon name="photograph" size="24" color="#99999950" />
          </div>
        </div>
      </template>
    </van-field>
    <oort-uploader ref="oort_uploader" v-model:show="showUploaderFile" :file="fileList" @click="showUploaderFile=false" @getImgUrl="getImgFile" />
  </form-item-wrapper>
</template>

<script>
import { toRefs, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'
import OortUploader from '@/components/uploader/oort_uploader.vue'
import { deepClone } from '~@/utils/util'

export default {
  componentName: 'FieldWidget',
  name: 'MPictureUploadWidget',
  components: {
    FormItemWrapper,
    OortUploader
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
      showUploaderFile: false,
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
  flex-wrap: wrap;
  gap: 4px;
  width: 100%;

  .preview-cover {
    width: 68px;
    height: 68px;
    background-color: #f7f8fa;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    position: relative;
    &_img {
      width: 100%;
      min-height: 100%;
      height: auto;
    }
    &:hover {
      background-color: #f5f7fa;
    }

    &.readmode {
      background-color: transparent;
    }
  }
  .file-action {
    position: absolute;
    top: -0px;
    right: -0px;
    width: 14px;
    height: 14px;
    background-color: #000;
    cursor: pointer;
    border-bottom-left-radius: 80%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.add_plus {
  width: 68px;
    height: 68px;
  background-color: #f7f8fa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
</style>
