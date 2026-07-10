<template>
  <el-form-item :label="i18nt('designer.setting.lowThreshold')">
    <el-input-number
      v-model="optionModel.lowThreshold"
      :min="1"
      :max="optionModel.highThreshold"
      class="hide-spin-button"
      style="width: 100%"
    />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElInputNumber } from 'element-plus'

export default {
  name: 'LowThresholdEditor',
  components: {
    ElFormItem,
    ElInputNumber
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
