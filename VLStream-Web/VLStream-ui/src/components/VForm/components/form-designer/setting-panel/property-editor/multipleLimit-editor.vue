<template>
  <el-form-item :label="i18nt('designer.setting.multipleLimit')">
    <el-input-number
      v-model="optionModel.multipleLimit"
      :min="0"
      class="hide-spin-button"
      style="width: 100%"
    />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElInputNumber } from 'element-plus'
export default {
  name: 'MultipleLimitEditor',
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
