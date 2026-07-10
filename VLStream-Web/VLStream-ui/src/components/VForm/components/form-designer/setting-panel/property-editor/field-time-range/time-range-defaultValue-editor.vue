<template>
  <el-form-item :label="i18nt('designer.setting.defaultValue')">
    <el-time-picker
      v-model="optionModel.defaultValue"
      is-range
      :format="optionModel.format"
      value-format="HH:mm:ss"
      style="width: 100%"
      @change="emitDefaultValueChange"
    />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import propertyMixin from '~@/components/form-designer/setting-panel/property-editor/propertyMixin'
import { ElFormItem, ElTimePicker } from 'element-plus'
export default {
  name: 'TimeRangeDefaultValueEditor',
  components: {
    ElFormItem,
    ElTimePicker
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
