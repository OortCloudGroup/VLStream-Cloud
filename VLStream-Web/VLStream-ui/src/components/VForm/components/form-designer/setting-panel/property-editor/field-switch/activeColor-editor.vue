<template>
  <el-form-item :label="i18nt('designer.setting.activeColor')">
    <el-color-picker v-model="optionModel.activeColor" />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElColorPicker } from 'element-plus'
export default {
  name: 'ActiveColorEditor',
  components: {
    ElFormItem,
    ElColorPicker
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
