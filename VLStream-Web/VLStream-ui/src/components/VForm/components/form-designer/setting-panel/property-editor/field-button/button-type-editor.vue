<template>
  <el-form-item :label="i18nt('designer.setting.displayType')">
    <el-select v-model="optionModel.type">
      <el-option label="default" value="" />
      <el-option label="primary" value="primary" />
      <el-option label="success" value="success" />
      <el-option label="warning" value="warning" />
      <el-option label="danger" value="danger" />
      <el-option label="info" value="info" />
      <el-option label="text" value="text" />
    </el-select>
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import propertyMixin from '~@/components/form-designer/setting-panel/property-editor/propertyMixin'
import { ElFormItem, ElButton, ElSelect, ElOption } from 'element-plus'
export default {
  name: 'ButtonTypeEditor',
  components: {
    ElFormItem,
    ElButton,
    ElSelect,
    ElOption
  },
  mixins: [i18n, propertyMixin],
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  }
}
</script>

<style lang="scss" scoped>

</style>
