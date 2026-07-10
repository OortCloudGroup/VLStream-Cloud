<template>
  <div>
    <el-form-item :label="i18nt('designer.setting.htmlContent')" />
    <el-form-item label-width="0">
      <el-input v-model="optionModel.htmlContent" type="textarea" :rows="5" class="html-content-editor" />
    </el-form-item>
  </div>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElInput } from 'element-plus'

export default {
  name: 'HtmlContentEditor',
  components: {
    ElFormItem,
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
  .html-content-editor {
    font-size: 13px;
  }

</style>
