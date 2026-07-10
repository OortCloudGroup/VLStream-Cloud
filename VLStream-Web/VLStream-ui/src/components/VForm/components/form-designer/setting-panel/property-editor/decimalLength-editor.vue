<template>
  <el-form-item label="小数位数">
    <el-input
      v-model="optionModel.decimalLength"
      type="number"
      class="hide-spin-button"
      min="1"
      style="width: 100%"
      @input="inputNumberHandler"
    />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import propertyMixin from '~@/components/form-designer/setting-panel/property-editor/propertyMixin'
import { ElFormItem, ElInput } from 'element-plus'

export default {
  name: 'DecimalLengthEditor',
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
