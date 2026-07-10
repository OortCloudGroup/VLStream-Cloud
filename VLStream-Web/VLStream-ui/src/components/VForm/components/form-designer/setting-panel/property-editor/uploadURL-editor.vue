<template>
  <div>
    <el-form-item label-width="0">
      <el-divider class="custom-divider">
        {{ i18nt('designer.setting.uploadSetting') }}
      </el-divider>
    </el-form-item>
    <el-form-item :label="i18nt('designer.setting.uploadURL')">
      <el-input v-model="optionModel.uploadURL" type="text" placeholder="请输入上传地址，默认为系统文件上传地址" />
    </el-form-item>
  </div>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElDivider, ElInput } from 'element-plus'
export default {
  name: 'UploadURLEditor',
  components: {
    ElFormItem,
    ElDivider,
    ElInput
  },
  mixins: [i18n],
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  }
}
</script>

<style scoped>

</style>
