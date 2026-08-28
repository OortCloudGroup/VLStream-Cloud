<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <form-item-wrapper
    :designer="designer"
    :field="field"
    :rules="rules"
    :design-state="designState"
    :parent-widget="parentWidget"
    :parent-list="parentList"
    :index-of-parent-list="indexOfParentList"
    :sub-form-row-index="subFormRowIndex"
    :sub-form-col-index="subFormColIndex"
    :sub-form-row-id="subFormRowId"
  >
    <!-- el-upload :name="field.options.name" after, will and failed! Delete ! ! -->
    <el-upload
      ref="fieldEditor"
      :disabled="field.options.disabled"
      :action="realUploadURL"
      :headers="uploadHeaders"
      :data="uploadData"
      :with-credentials="field.options.withCredentials"
      :multiple="field.options.multipleSelect"
      :file-list="fileList"
      :show-file-list="field.options.showFileList"
      list-type="picture-card"
      :class="{'hideUploadDiv': uploadBtnHidden}"
      :limit="field.options.limit"
      :on-exceed="handlePictureExceed"
      :before-upload="beforePictureUpload"
      :on-preview="handlePictureCardPreview"
      :on-success="handlePictureUpload"
      :on-error="handleUploadError"
    >
      <template #file="{ file }">
        <el-image
          ref="imageRef"
          style="width: 100%; height: 100%"
          :src="file.url"
          :preview-src-list="previewList"
          :initial-index="previewIndex"
          fit="cover"
          preview-teleported
        />
        <!-- successfully -->
        <label class="el-upload-list__item-status-label">
          <i class="el-icon--upload-success" style="color: #FFF"><svg-icon class="" icon-class="el-check" /></i>
        </label>
        <!-- operationbutton -->
        <span class="el-upload-list__item-actions">
          <!--  -->
          <span
            class="el-upload-list__item-preview"
            @click="handlePictureCardPreview(file)"
          >
            <svg-icon icon-class="el-zoom-in" />
          </span>
          <!-- Delete -->
          <span
            class="el-upload-list__item-delete"
            @click="handlePictureRemove(file)"
          >
            <svg-icon icon-class="el-delete" />
          </span>
        </span>
      </template>
      <template #tip>
        <div
          v-if="!!field.options.uploadTip"
          class="el-upload__tip"
        >
          {{ field.options.uploadTip }}
        </div>
      </template>
      <div class="uploader-icon">
        <svg-icon icon-class="el-plus" />
      </div>
    </el-upload>
  </form-item-wrapper>
</template>

<script>
import FormItemWrapper from './form-item-wrapper'
import emitter from '~@/utils/emitter'
import i18n from '~@/utils/i18n'
import { deepClone, evalFn } from '~@/utils/util'
import fieldMixin from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import SvgIcon from '~@/components/svg-icon/index'
import { ElImage, ElUpload } from 'element-plus'
import { apaasRequestHeaders, apaasServiceUrl } from '@/utils/apaasApiBase'

export default {
  name: 'PictureUploadWidget',
  componentName: 'FieldWidget',
  components: {
    ElImage,
    ElUpload,
    FormItemWrapper,
    SvgIcon
  }, // to FieldWidget, component broadcastevent
  mixins: [emitter, fieldMixin, i18n],
  props: {
    field: Object,
    parentWidget: Object,
    parentList: Array,
    indexOfParentList: Number,
    designer: Object,

    designState: {
      type: Boolean,
      default: false
    },

    subFormRowIndex: { /* 子表单组件行索引，从0开始计数 */
      type: Number,
      default: -1
    },
    subFormColIndex: { /* 子表单组件列索引，从0开始计数 */
      type: Number,
      default: -1
    },
    subFormRowId: { /* 子表单组件行Id，唯一id且不可变 */
      type: String,
      default: ''
    }

  },
  data() {
    return {
      oldFieldValue: null, // fieldcomponentchange before value
      fieldModel: [],
      rules: [],

      uploadHeaders: apaasRequestHeaders,
      uploadData: {
        key: '' //
        // token: '', // token

        // policy: '', // and policy
        // authorization: '', // and
      },
      fileList: [], //
      fileListBeforeRemove: [], // Delete before
      uploadBtnHidden: false,

      previewIndex: 1 //
    }
  },
  computed: {
    previewList() {
      return this.fileList.map(el => el.url)
    },

    realUploadURL() {
      let uploadURL = this.field.options.uploadURL
      if (!!uploadURL && ((uploadURL.indexOf('DSV.') > -1) || (uploadURL.indexOf('DSV[') > -1))) {
        let DSV = this.getGlobalDsv()
        console.log('test DSV: ', DSV) // DSV ! ! !
        return evalFn(this.field.options.uploadURL, DSV)
      }
      // if is empty to fastdfs
      if (!uploadURL) {
        return config.URL + config.gateWay + 'apaas-fastdfsservice/fastdfs/v1/uploadFile'
      }
      return this.field.options.uploadURL
    }

  },
  beforeCreate() {
    /* can method and property! ! */
  },

  created() {
    /* : sub componentmounted in componentcreated after、 componentmounted before , sub componentmounted need to prop
         需要在父组件created中初始化！！ */
    this.initFieldModel()
    this.registerToRefList()
    this.initEventHandler()
    this.buildFieldRules()

    this.handleOnCreated()
  },

  mounted() {
    this.handleOnMounted()
  },

  beforeUnmount() {
    this.unregisterFromRefList()
  },

  methods: {
    handlePictureExceed() {
      let uploadLimit = this.field.options.limit
      this.$message.warning(this.i18nt('render.hint.uploadExceed').replace('${uploadLimit}', uploadLimit))
    },

    beforePictureUpload(file) {
      let fileTypeCheckResult = false
      if (!!this.field.options && !!this.field.options.fileTypes) {
        let uploadFileTypes = this.field.options.fileTypes
        if (uploadFileTypes.length > 0) {
          fileTypeCheckResult = uploadFileTypes.some((ft) => {
            return file.type === 'image/' + ft
          })
        }
      }
      if (!fileTypeCheckResult) {
        this.$message.error(this.i18nt('render.hint.unsupportedFileType') + file.type)
        return false
      }

      let fileSizeCheckResult = false
      let uploadFileMaxSize = 5 // 5MB
      if (!!this.field.options && !!this.field.options.fileMaxSize) {
        uploadFileMaxSize = this.field.options.fileMaxSize
      }
      fileSizeCheckResult = file.size / 1024 / 1024 <= uploadFileMaxSize
      if (!fileSizeCheckResult) {
        this.$message.error(this.$('render.hint.fileSizeExceed') + uploadFileMaxSize + 'MB')
        return false
      }

      this.uploadData.key = file.name
      return this.handleOnBeforeUpload(file)
    },

    handleOnBeforeUpload(file) {
      if (!!this.field.options.onBeforeUpload) {
        let bfFunc = new Function('file', this.field.options.onBeforeUpload)
        let result = bfFunc.call(this, file)
        if (typeof result === 'boolean') {
          return result
        } else {
          return true
        }
      }

      return true
    },

    updateFieldModelAndEmitDataChangeForUpload(fileList, customResult, defaultResult) {
      let oldValue = deepClone(this.fieldModel)
      if (!!customResult && !!customResult.name && !!customResult.url) {
        this.fieldModel.push({
          name: customResult.name,
          url: customResult.url
        })
      } else if (!!defaultResult && !!defaultResult.name && !!defaultResult.url) {
        this.fieldModel.push({
          name: defaultResult.name,
          url: defaultResult.url
        })
      } else {
        this.fieldModel = deepClone(fileList)
      }

      this.syncUpdateFormModel(this.fieldModel)
      this.emitFieldDataChange(this.fieldModel, oldValue)
    },

    handlePictureUpload(res, file, fileList) {
      if (file.status === 'success') {
        let customResult = null
        if (!!this.field.options.onUploadSuccess) {
          let customFn = new Function('result', 'file', 'fileList', this.field.options.onUploadSuccess)
          customResult = customFn.call(this, res, file, fileList)
        } else {
          // to fastfds
          if (res.code === 200) {
            customResult = { name: file.name, url: res.data.url }
          } else {
            this.$message.error(res.msg)
          }
        }
        this.updateFieldModelAndEmitDataChangeForUpload(fileList, customResult, res)
        this.fileList = deepClone(fileList)
        this.uploadBtnHidden = fileList.length >= this.field.options.limit
      }
    },

    updateFieldModelAndEmitDataChangeForRemove(file) {
      let oldValue = deepClone(this.fieldModel)
      let foundFileIdx = -1
      this.fileListBeforeRemove.map((fi, idx) => { /* 跟element-ui不同，element-plus删除文件时this.fileList数组对应元素已被删除！！ */
        if ((fi.name === file.name) && ((fi.url === file.url) || (!!fi.uid && fi.uid === file.uid))) { /* 这个判断有问题？？ */
          foundFileIdx = idx
        }
      })
      if (foundFileIdx > -1) {
        this.fieldModel.splice(foundFileIdx, 1)
      }

      this.syncUpdateFormModel(this.fieldModel)
      this.emitFieldDataChange(this.fieldModel, oldValue)
    },

    handleBeforeRemove(fileList) {
      /* Delete before ! ! */
      this.fileListBeforeRemove = deepClone(fileList)
    },

    handlePictureRemove(file) {
      this.handleBeforeRemove(this.fileList) // Custom #file slot, need to handleBeforeRemove, @before-remove and @remove
      this.fileList.splice(this.fileList.indexOf(file), 1) // Delete
      this.updateFieldModelAndEmitDataChangeForRemove(file)
      let fileList = deepClone(this.fileList) // , userCustom fileList Update , component data
      this.uploadBtnHidden = fileList.length >= this.field.options.limit

      if (!!this.field.options.onFileRemove) {
        let customFn = new Function('file', 'fileList', this.field.options.onFileRemove)
        customFn.call(this, file, fileList)
      }
    },

    handleUploadError(err, file, fileList) {
      if (!!this.field.options.onUploadError) {
        let customFn = new Function('error', 'file', 'fileList', this.field.options.onUploadError)
        customFn.call(this, err, file, fileList)
      } else {
        this.$message({
          message: this.i18nt('render.hint.uploadError') + err,
          duration: 3000,
          type: 'error'
        })
      }
    },

    handlePictureCardPreview({ url }) {
      // Set to current
      this.previewIndex = this.previewList.indexOf(url)
      // <el-image> component img ( event in img )
      this.$refs['imageRef'].$el.children[0].click()
    }

  }
}
</script>

<style lang="scss" scoped>
  @import "../../../../styles/global.scss"; /* form-item-wrapper已引入，还需要重复引入吗？ */

  .full-width-input {
    width: 100% !important;
  }

  .hideUploadDiv {
    :deep(div.el-upload--picture-card) { /* 隐藏最后的图片上传按钮 */
      display: none;
    }

    :deep(div.el-upload--text) { /* 隐藏最后的文件上传按钮 */
      display: none;
    }

    :deep(div.el-upload__tip) { /* 隐藏最后的文件上传按钮提示 */
      display: none;
    }
  }

  .uploader-icon {
    height: 100%;
    display: flex;
    color: #8c939d;
    font-size: 28px;
    justify-content: center;
    align-items: center;
  }

</style>

