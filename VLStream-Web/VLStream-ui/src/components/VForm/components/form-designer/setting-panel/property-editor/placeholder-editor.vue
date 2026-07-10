<template>
  <el-form-item :label="i18nt('designer.setting.placeholder')">
    <el-input v-model="optionModel.placeholder" type="text" />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import propertyMixin from '~@/components/form-designer/setting-panel/property-editor/propertyMixin'
import { ElFormItem, ElInput } from 'element-plus'

export default {
  name: 'PlaceholderEditor',
  components: {
    ElFormItem,
    ElInput
  },
  mixins: [i18n, propertyMixin],
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  }
}
</script>

<style scoped>

</style>
